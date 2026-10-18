package io.github.gfander.dualforge;

import java.lang.reflect.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import net.neoforged.fml.ModContainer;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Actual original initialization/Screen constructor with an explicitly injected,
 * constructor-free Minecraft singleton. No window, Screen.init, render or mouse API. */
public final class OriginalInitContract {
    private static int assertions;
    public static void main(String[] args) throws Exception {
        Path game = Path.of(args[2]).toAbsolutePath(); Files.createDirectories(game.resolve("config"));
        Class<?> minecraft = Class.forName("net.minecraft.client.Minecraft");
        Class<?> unsafe = Class.forName("sun.misc.Unsafe"); Field handle = unsafe.getDeclaredField("theUnsafe"); handle.setAccessible(true);
        Object allocator = handle.get(null);
        Object mock = unsafe.getMethod("allocateInstance", Class.class).invoke(allocator, minecraft);
        Field singleton = minecraft.getDeclaredField("instance"); singleton.setAccessible(true); Object previous = singleton.get(null);
        Field directory = minecraft.getField("gameDirectory"); directory.setAccessible(true);
        directory.set(mock, game.toFile()); singleton.set(null, mock);
        String entry = "yalter.mousetweaks.forge.MouseTweaksForge";
        ClassNode node = new ClassNode(); new ClassReader(Files.readAllBytes(Path.of(args[1]).resolve(entry.replace('.', '/') + ".class"))).accept(node, 0);
        List<InvokeDynamicInsnNode> sites = Arrays.stream(node.methods.stream().filter(m -> m.name.equals("<init>")).findFirst().orElseThrow().instructions.toArray())
            .filter(op -> op instanceof InvokeDynamicInsnNode).map(op -> (InvokeDynamicInsnNode) op).toList();
        check(sites.size() == 2 && sites.get(0).name.equals("apply") && sites.get(1).name.equals("createScreen"), "original Function followed by native adapter callsite");
        MethodNode access = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "df009c$originalFactory",
            "()Lnet/neoforged/neoforge/client/gui/IConfigScreenFactory;", null, null);
        for (var site : sites) access.instructions.add(site.clone(new HashMap<>()));
        access.instructions.add(new InsnNode(Opcodes.ARETURN)); access.maxStack = 1; node.methods.add(access);
        ClassWriter writer = new ClassWriter(0); node.accept(writer); byte[] replacement = writer.toByteArray();
        try (var loader = new URLClassLoader(new URL[]{Path.of(args[0]).toUri().toURL()}, OriginalInitContract.class.getClassLoader()) {
            @Override protected Class<?> findClass(String name) throws ClassNotFoundException {
                if (name.equals(entry)) return defineClass(name, replacement, 0, replacement.length);
                return super.findClass(name);
            }
        }) {
            Class<?> main = Class.forName("yalter.mousetweaks.Main", false, loader);
            main.getMethod("initialize").invoke(null);
            Object config = main.getField("config").get(null); check(config != null, "actual Main.initialize produces original Config");
            Field ready = main.getDeclaredField("initialized"); ready.setAccessible(true); check(ready.getBoolean(null), "actual initialization guard set");
            Field path = Arrays.stream(config.getClass().getDeclaredFields()).filter(f -> f.getType() == String.class && !Modifier.isStatic(f.getModifiers())).findFirst().orElseThrow(); path.setAccessible(true);
            Path actual = Path.of((String) path.get(config)).toAbsolutePath();
            check(actual.startsWith(game) && Files.isRegularFile(actual), "original Config.read creates only the isolated game config");
            byte[] contents = Files.readAllBytes(actual);
            main.getMethod("initialize").invoke(null);
            check(main.getField("config").get(null) == config && Arrays.equals(contents, Files.readAllBytes(actual)), "original once-only initialization preserved");
            Class<?> type = Class.forName(entry, true, loader);
            Object factory = type.getMethod("df009c$originalFactory").invoke(null);
            Class<?> api = Class.forName("net.neoforged.neoforge.client.gui.IConfigScreenFactory");
            Class<?> screen = Class.forName("net.minecraft.client.gui.screens.Screen");
            Class<?> originalScreen = Class.forName("yalter.mousetweaks.ConfigScreen", false, loader);
            Object parent = unsafe.getMethod("allocateInstance", Class.class).invoke(allocator, originalScreen);
            Object created = api.getMethod("createScreen", ModContainer.class, screen).invoke(factory, null, parent);
            check(created != parent && created.getClass() == originalScreen, "native SAM calls actual original ConfigScreen constructor");
            Field back = originalScreen.getDeclaredField("previous"); back.setAccessible(true);
            check(back.get(created) == parent, "actual original parent object retained");
            Object second = api.getMethod("createScreen", ModContainer.class, screen).invoke(factory, null, parent);
            check(second != created && back.get(second) == parent, "each factory call creates a new original Screen");
            System.out.println("ORIGINAL INIT MODEL PASS assertions=" + assertions + " config=" + actual
                + "; INJECTED_MINECRAFT_SINGLETON; ENTRY_CONSTRUCTOR=NOT_RUN; GUI=NOT_RUN");
        } finally { singleton.set(null, previous); }
    }
    private static void check(boolean value, String why) { if (!value) throw new AssertionError(why); assertions++; }
}
