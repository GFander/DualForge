package io.github.gfander.dualforge;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.loading.FMLLoader;

/** Constructor-only semantic bridge. The host Global Bus and registered object never escape. */
public final class ForgeGlobalEventBridge {
    private static final StackWalker WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
    private enum State { REGISTERING, REGISTERED, EMPTY_SOURCE_TRANSLATION, UNCERTAIN }
    private static final Map<ModContainer,State> STATES = new WeakHashMap<>();
    private ForgeGlobalEventBridge() { }

    public static void register(Object target, String fingerprint) {
        var caller = WALKER.walk(f -> f.skip(1).findFirst().orElseThrow());
        require(caller.getMethodName().equals("<init>") && target != null && target.getClass() == caller.getDeclaringClass(), "ENTRY_CONSTRUCTOR_OBJECT");
        ModContainer owner = ForgeConstructorContext.globalOwner(caller.getDeclaringClass());
        var info = owner.getModInfo();
        String entry = caller.getClassName().replace('.', '/');
        var plan = ForgeRegistryTransformationPlan.create(info, entry);
        require(plan.global != null && plan.entry.equals(entry) && plan.fingerprint.equals(fingerprint), "COMBINED_PLAN_FINGERPRINT");
        require(target.getClass().getModule().getLayer() == FMLLoader.getGameLayer(), "WRONG_GAME_LAYER");
        validateTarget(target, plan.global);
        try {
            Object bus = globalBus(owner);
            String outcome = invoke(owner, bus, target, plan.global);
            System.out.println("[DUALFORGE-GLOBAL-BRIDGE] REGISTERED modId=" + owner.getModId()
                    + " object=" + System.identityHashCode(target) + " globalBus=" + System.identityHashCode(bus)
                    + " listeners=" + plan.global.listeners() + " outcome=" + outcome + " plan=" + fingerprint);
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Global bus fixed host ABI unavailable", failure);
        }
    }
    // Called only after ownership/plan/target/host validation above. Tests may exercise this
    // state machine in isolation; doing so does not validate the public caller path.
    private static String invoke(ModContainer owner, Object bus, Object target, ControlledForgeGlobalBus global)
            throws ReflectiveOperationException {
        synchronized (STATES) {
            State state = STATES.get(owner);
            require(state != State.UNCERTAIN, "GLOBAL_REGISTRATION_UNCERTAIN");
            require(state != State.REGISTERING, "GLOBAL_REGISTRATION_IN_PROGRESS");
            require(state == null, "DUPLICATE_GLOBAL_REGISTRATION");
            STATES.put(owner, State.REGISTERING);
            try {
                net.neoforged.bus.api.IEventBus.class.getMethod("register", Object.class).invoke(bus, target);
                require(global.listeners() == 1, "UNEXPECTED_EMPTY_NATIVE_RETURN");
                STATES.put(owner, State.REGISTERED); return "NATIVE_RETURN";
            } catch (InvocationTargetException failure) {
                if (global.listeners() == 0 && emptyRejection(failure.getCause(), target)) {
                    STATES.put(owner, State.EMPTY_SOURCE_TRANSLATION); return "EMPTY_SOURCE_TRANSLATION";
                } else {
                    STATES.put(owner, State.UNCERTAIN);
                    throw new IllegalStateException("Global bus official register failed", failure.getCause());
                }
            } catch (ReflectiveOperationException | RuntimeException | Error failure) {
                STATES.put(owner, State.UNCERTAIN); throw failure;
            }
        }
    }
    private static Object globalBus(ModContainer owner) throws ReflectiveOperationException {
        Class<?> host = Class.forName(FMLLoader.getGameLayer().findModule("neoforge").orElseThrow(), "net.neoforged.neoforge.common.NeoForge");
        Object bus = host.getField("EVENT_BUS").get(null);
        require(bus instanceof net.neoforged.bus.api.IEventBus && bus != owner.getEventBus()
                && bus.getClass().getName().equals("net.neoforged.bus.EventBus")
                && bus.getClass().getModule() == net.neoforged.bus.api.IEventBus.class.getModule(), "HOST_GLOBAL_BUS_IDENTITY");
        ControlledForgeGlobalBus.verifyHostBus();
        return bus;
    }
    private static void validateTarget(Object target, ControlledForgeGlobalBus global) {
        Class<?> type = target.getClass();
        require(type.getSuperclass() == Object.class && type.getInterfaces().length == 0, "TARGET_INHERITANCE");
        var methods = type.getDeclaredMethods();
        require(methods.length == global.listeners(), "TARGET_LISTENER_COUNT");
        if (global.listeners() == 1) {
            var m = methods[0]; var annotations = m.getDeclaredAnnotations();
            require(m.getName().equals(global.listener) && m.getModifiers() == java.lang.reflect.Modifier.PUBLIC
                    && m.getReturnType() == void.class && m.getParameterCount() == 1
                    && m.getParameterTypes()[0] == Class.forName(FMLLoader.getGameLayer().findModule("neoforge").orElseThrow(), ControlledForgeGlobalBus.HOST_EVENT.replace('/', '.'))
                    && annotations.length == 1 && annotations[0].annotationType() == net.neoforged.bus.api.SubscribeEvent.class,
                    "MAPPED_LISTENER_IDENTITY");
            var annotation = (net.neoforged.bus.api.SubscribeEvent) annotations[0];
            require(annotation.priority() == net.neoforged.bus.api.EventPriority.NORMAL && !annotation.receiveCanceled(), "MAPPED_LISTENER_OPTIONS");
        }
    }
    private static boolean emptyRejection(Throwable failure, Object target) {
        String expected = "%s has no @SubscribeEvent methods, but register was called anyway.\nThe event bus only recognizes listener methods that have the @SubscribeEvent annotation.\n".formatted(target.getClass());
        if (failure == null || failure.getClass() != IllegalArgumentException.class || !expected.equals(failure.getMessage())) return false;
        var stack = failure.getStackTrace();
        return stack.length > 0 && stack[0].getClassName().equals("net.neoforged.bus.EventBus") && stack[0].getMethodName().equals("register");
    }
    private static void require(boolean valid, String reason) { if (!valid) throw new IllegalStateException("Global bus bridge rejected: " + reason); }
}
