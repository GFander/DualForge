package io.github.gfander.dualforge;

import java.lang.reflect.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.*;
import java.util.jar.JarFile;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Real native bus/events with constructor-free Screen and fixture objects.
 * This is API isolation, never a client launch, GUI, mod construction or input simulation. */
public final class NativeApiHarness {
    static int assertions;
    record Sample(String prefix, ClassLoader loader, Class<?> type, Class<?> logic, Object target,
                  Object bus, Class<?> busAPI, Class<?> eventAPI, Class<?> screen, Object screenObject) { }
    static void check(boolean ok, String why) { if (!ok) throw new AssertionError(why); assertions++; }

    public static void main(String[] args) throws Exception {
        Sample s = load("forge", Path.of(args[0]), null);
        s.busAPI.getMethod("register", Object.class).invoke(s.bus, s.target);
        exercise(s);
        Class<?> factoryType = Class.forName("net.minecraftforge.client.ConfigScreenHandler$ConfigScreenFactory");
        int[] calls = {0}; Function<Object,Object> function = parent -> { check(parent == s.screenObject, "Forge factory parent identity"); calls[0]++; return parent; };
        Object factory = factoryType.getConstructor(Function.class).newInstance(function);
        check(calls[0] == 0, "Forge factory stays lazy");
        Object callback = factoryType.getMethod("screenFunction").invoke(factory);
        @SuppressWarnings("unchecked") BiFunction<Object,Object,Object> invoke = (BiFunction<Object,Object,Object>) callback;
        check(invoke.apply(null, s.screenObject) == s.screenObject && calls[0] == 1, "actual Forge config record invokes original Function once");
        System.out.println("NATIVE FORGE SCREEN API PASS assertions=" + assertions + "; GUI=NOT_RUN; CONSTRUCTION=NOT_RUN");
    }

    static Sample load(String host, Path fixture, Path transformed) throws Exception {
        String prefix = host.equals("forge") ? "net.minecraftforge" : "net.neoforged.neoforge";
        String eventbus = host.equals("forge") ? "net.minecraftforge.eventbus.api." : "net.neoforged.bus.api.";
        String name = "org.example.gesture.ClientInput";
        byte[] replacement = null;
        if (transformed != null) {
            ClassNode n = new ClassNode(); new ClassReader(Files.readAllBytes(transformed.resolve(name.replace('.', '/') + ".class"))).accept(n, 0);
            // Diagnostic accessor copies the actual generated native factory callsite.
            // No original JAR is modified; this enriched in-memory class is not a game input.
            InvokeDynamicInsnNode nativeFactory = (InvokeDynamicInsnNode) n.methods.stream().flatMap(m -> Arrays.stream(m.instructions.toArray()))
                .filter(op -> op instanceof InvokeDynamicInsnNode d && d.name.equals("createScreen")).findFirst().orElseThrow();
            MethodNode accessor = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "df009c$factory",
                "(Ljava/util/function/Function;)Lnet/neoforged/neoforge/client/gui/IConfigScreenFactory;", null, null);
            accessor.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0)); accessor.instructions.add(nativeFactory.clone(new HashMap<>()));
            accessor.instructions.add(new InsnNode(Opcodes.ARETURN)); accessor.maxLocals = 1; accessor.maxStack = 1; n.methods.add(accessor);
            ClassWriter writer = new ClassWriter(0); n.accept(writer); replacement = writer.toByteArray();
        }
        byte[] definition = replacement;
        Object forgeTransformer = host.equals("forge") ? Class.forName("net.minecraftforge.eventbus.EventSubclassTransformer").getConstructor().newInstance() : null;
        URLClassLoader loader = new URLClassLoader(new URL[]{fixture.toUri().toURL()}, NativeApiHarness.class.getClassLoader()) {
            @Override protected Class<?> loadClass(String requested, boolean resolve) throws ClassNotFoundException {
                if (forgeTransformer != null && requested.startsWith("net.minecraftforge.client.event.ScreenEvent")) {
                    synchronized (getClassLoadingLock(requested)) {
                        Class<?> type = findLoadedClass(requested);
                        if (type == null) {
                            String resource = requested.replace('.', '/') + ".class";
                            try (var in = getParent().getResourceAsStream(resource)) {
                                if (in == null) throw new ClassNotFoundException(requested);
                                ClassNode node = new ClassNode(); new ClassReader(in.readAllBytes()).accept(node, 0);
                                forgeTransformer.getClass().getMethod("transform", ClassNode.class, org.objectweb.asm.Type.class)
                                    .invoke(forgeTransformer, node, org.objectweb.asm.Type.getObjectType(requested.replace('.', '/')));
                                ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS); node.accept(writer);
                                byte[] bytes = writer.toByteArray(); type = defineClass(requested, bytes, 0, bytes.length);
                            } catch (Exception failure) { throw new ClassNotFoundException(requested, failure); }
                        }
                        if (resolve) resolveClass(type); return type;
                    }
                }
                return super.loadClass(requested, resolve);
            }
            @Override protected Class<?> findClass(String requested) throws ClassNotFoundException {
                if (definition != null && requested.equals(name)) return defineClass(requested, definition, 0, definition.length);
                return super.findClass(requested);
            }
        };
        Thread.currentThread().setContextClassLoader(loader);
        Class<?> type = Class.forName(name, true, loader);
        // Explicitly bypass fixture construction to isolate event API without Minecraft initialization.
        Class<?> unsafeType = Class.forName("sun.misc.Unsafe"); Field field = unsafeType.getDeclaredField("theUnsafe"); field.setAccessible(true);
        Object target = unsafeType.getMethod("allocateInstance", Class.class).invoke(field.get(null), type);
        Object screenObject = unsafeType.getMethod("allocateInstance", Class.class).invoke(field.get(null),
            Class.forName("org.example.gesture.GestureScreen", false, loader));
        Class<?> builder = Class.forName(eventbus + "BusBuilder"); Object instance = builder.getMethod("builder").invoke(null);
        Object bus = builder.getMethod("build").invoke(instance);
        return new Sample(prefix, loader, type, Class.forName("org.example.gesture.GestureLogic", true, loader), target,
            bus, Class.forName(eventbus + "IEventBus"), Class.forName(eventbus + "Event"), Class.forName("net.minecraft.client.gui.screens.Screen", false, loader), screenObject);
    }

    static void exercise(Sample s) throws Exception {
        String[] suffixes = {"$MouseButtonPressed$Pre", "$MouseButtonReleased$Pre", "$MouseDragged$Pre", "$MouseScrolled$Post"};
        int[] calls = (int[]) s.logic.getField("calls").get(null); double[] data = (double[]) s.logic.getField("data").get(null);
        for (int kind = 0; kind < 4; kind++) {
            Class<?> type = Class.forName(s.prefix + ".client.event.ScreenEvent" + suffixes[kind], true, s.loader);
            for (boolean cancel : new boolean[]{false, true}) {
                s.logic.getField("cancel").setBoolean(null, cancel); int previous = calls[kind];
                Object event = event(s, type, kind, 1);
                Object result = s.busAPI.getMethod("post", s.eventAPI).invoke(s.bus, event);
                check(calls[kind] == previous + 1, "one native callback kind=" + kind);
                check(data[0] == -3.75 && data[1] == 8.5 && s.logic.getField("lastScreen").get(null) == s.screenObject, "screen/coordinates forwarded");
                check(data[2] == (kind == 3 ? -2.75 : 1), "button or fractional scroll delta forwarded");
                boolean canceled = kind < 3 && (boolean) type.getMethod("isCanceled").invoke(event);
                check(canceled == (kind < 3 && cancel), "actual cancellation matches callback");
                check(s.prefix.contains("minecraftforge") ? result.equals(canceled) : result == event, "native post return ABI");
                if (kind == 2) check(Class.forName("org.example.gesture.GestureLog", true, s.loader).getField("last").get(null).equals("drag 1 5.5 -8.25"), "drag deltas preserved");
            }
            if (kind < 3) {
                int previous = calls[kind]; Object event = event(s, type, kind, 0);
                type.getMethod("setCanceled", boolean.class).invoke(event, true);
                s.busAPI.getMethod("post", s.eventAPI).invoke(s.bus, event);
                check(calls[kind] == previous, "default receiveCanceled=false");
                event = event(s, type, kind, 99); s.busAPI.getMethod("post", s.eventAPI).invoke(s.bus, event);
                check(calls[kind] == previous, "unknown button unchanged business refusal");
            } else {
                boolean cancelable = s.prefix.contains("minecraftforge") ? (boolean) type.getMethod("isCancelable").invoke(event(s, type, kind, 0))
                    : Class.forName("net.neoforged.bus.api.ICancellableEvent").isAssignableFrom(type);
                check(!cancelable, "scroll Post remains noncancellable");
            }
        }
        s.busAPI.getMethod("unregister", Object.class).invoke(s.bus, s.target);
        int previous = calls[0]; s.busAPI.getMethod("post", s.eventAPI).invoke(s.bus,
            event(s, Class.forName(s.prefix + ".client.event.ScreenEvent$MouseButtonPressed$Pre", true, s.loader), 0, 0));
        check(calls[0] == previous, "native unregister removes listener");
    }
    private static Object event(Sample s, Class<?> type, int kind, int button) throws Exception {
        if (kind == 2) return type.getConstructor(s.screen, double.class, double.class, int.class, double.class, double.class)
            .newInstance(s.screenObject, -3.75, 8.5, button, 5.5, -8.25);
        if (kind == 3) return type.getConstructor(s.screen, double.class, double.class, double.class, double.class)
            .newInstance(s.screenObject, -3.75, 8.5, 4.25, -2.75);
        return type.getConstructor(s.screen, double.class, double.class, int.class).newInstance(s.screenObject, -3.75, 8.5, button);
    }
}
