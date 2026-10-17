package io.github.gfander.dualforge;

import java.util.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.IExtensionPoint;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.javafmlmod.FMLModContainer;
import net.neoforged.fml.loading.FMLLoader;

/** Constructor-only bridge; client types are resolved in the existing GAME layer. */
public final class ForgeClientEventBridge {
    private static final StackWalker WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
    private enum Phase { INITIALIZING, REGISTERING, REGISTERED, CONFIGURING, COMPLETE, UNCERTAIN }
    private static final Map<ModContainer,Progress> STATES = new WeakHashMap<>();
    private static final class Progress {
        final Object target; final ForgeClientEventPlan plan;
        Phase phase = Phase.INITIALIZING;
        Progress(Object target, ForgeClientEventPlan plan) { this.target = target; this.plan = plan; }
    }
    private ForgeClientEventBridge() { }

    public static void begin(Object target, String fingerprint) {
        var caller = WALKER.walk(f -> f.skip(1).findFirst().orElseThrow());
        ModContainer owner = owner(caller);
        require(target != null && target.getClass() == caller.getDeclaringClass(), "ENTRY_CONSTRUCTOR_OBJECT");
        var plan = ForgeClientEventPlan.create(owner.getModInfo(), caller.getClassName().replace('.', '/'));
        require(plan.fingerprint.equals(fingerprint), "PLAN_FINGERPRINT");
        validateListeners(target.getClass(), plan);
        synchronized (STATES) {
            require(!STATES.containsKey(owner), "DUPLICATE_INITIALIZATION");
            STATES.put(owner, new Progress(target, plan));
        }
    }

    public static void register(Object target, String fingerprint) {
        var caller = WALKER.walk(f -> f.skip(1).findFirst().orElseThrow());
        ModContainer owner = owner(caller);
        synchronized (STATES) {
            Progress p = progress(owner, caller.getDeclaringClass(), fingerprint);
            require(p.target == target, "FOREIGN_LISTENER_OBJECT");
            try {
                Class<?> type = host("net.neoforged.neoforge.common.NeoForge");
                Object bus = type.getField("EVENT_BUS").get(null);
                require(bus instanceof IEventBus && bus != owner.getEventBus()
                    && bus.getClass().getName().equals("net.neoforged.bus.EventBus")
                    && bus.getClass().getModule() == IEventBus.class.getModule(), "HOST_GLOBAL_BUS_IDENTITY");
                registerOnBus(owner, p, (IEventBus) bus);
            } catch (ReflectiveOperationException failure) {
                p.phase = Phase.UNCERTAIN; throw new IllegalStateException("Client global bus ABI unavailable", failure);
            }
        }
    }

    public static void configure(Object factory, String fingerprint) {
        var caller = WALKER.walk(f -> f.skip(1).findFirst().orElseThrow());
        ModContainer owner = owner(caller);
        synchronized (STATES) {
            Progress p = progress(owner, caller.getDeclaringClass(), fingerprint);
            Class<?> type = host(ForgeClientEventPlan.FACTORY.replace('/', '.'));
            require(factory != null && type.isInstance(factory)
                && factory.getClass().isHidden() && factory.getClass().getModule() == caller.getDeclaringClass().getModule(),
                "CONFIG_FACTORY_IDENTITY");
            installConfig(owner, p, factory, type);
        }
    }

    // Native API/state-machine core. Reflection-based isolated tests do not claim
    // to validate the caller/Module Layer path of the public constructor API.
    private static void registerOnBus(ModContainer owner, Progress p, IEventBus bus) {
        require(STATES.get(owner) == p && p.phase == Phase.INITIALIZING, "DUPLICATE_OR_UNORDERED_REGISTRATION");
        p.phase = Phase.REGISTERING;
        try {
            bus.register(p.target);
            p.phase = Phase.REGISTERED;
        } catch (RuntimeException | Error failure) { p.phase = Phase.UNCERTAIN; throw failure; }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void installConfig(ModContainer owner, Progress p, Object factory, Class<?> type) {
        require(STATES.get(owner) == p && p.phase == Phase.REGISTERED, "DUPLICATE_OR_UNORDERED_CONFIG");
        require(IExtensionPoint.class.isAssignableFrom(type) && type.isInstance(factory), "CONFIG_EXTENSION_TYPE");
        p.phase = Phase.CONFIGURING;
        try {
            require(owner.getCustomExtension((Class) type).isEmpty(), "CONFIG_EXTENSION_CONFLICT");
            owner.registerExtensionPoint((Class) type, (IExtensionPoint) factory);
            require(owner.getCustomExtension((Class) type).orElseThrow() == factory, "CONFIG_EXTENSION_PUBLICATION");
            p.phase = Phase.COMPLETE;
        } catch (RuntimeException | Error failure) { p.phase = Phase.UNCERTAIN; throw failure; }
    }

    private static Progress progress(ModContainer owner, Class<?> entry, String fingerprint) {
        Progress p = STATES.get(owner);
        require(p != null && p.target.getClass() == entry && p.plan.fingerprint.equals(fingerprint), "FOREIGN_OR_UNBOUND_PLAN");
        return p;
    }
    private static ModContainer owner(StackWalker.StackFrame caller) {
        require(caller.getMethodName().equals("<init>"), "ENTRY_CONSTRUCTOR_ONLY");
        ForgePlatformDependencyResolver.Host.current().validate();
        require(net.neoforged.fml.loading.FMLEnvironment.dist != null
            && net.neoforged.fml.loading.FMLEnvironment.dist.isClient(), "CLIENT_DIST_REQUIRED");
        ModContainer owner = ModLoadingContext.get().getActiveContainer();
        require(owner instanceof FMLModContainer && owner.getModInfo().getLoader() instanceof DualForgeLanguageLoader,
            "ACTIVE_FORGE_OWNER");
        Class<?> entry = caller.getDeclaringClass(); var info = owner.getModInfo();
        require(entry.getModule().isNamed() && entry.getModule().getLayer() == FMLLoader.getGameLayer()
            && entry.getModule().getName().equals(info.getOwningFile().moduleName())
            && DualForgeLanguageLoader.forgeEntrypoints(info, info.getOwningFile().getFile().getScanResult())
                .equals(List.of(entry.getName())), "ENTRY_MODULE_OWNER");
        return owner;
    }
    private static Class<?> host(String name) {
        Class<?> type = Class.forName(FMLLoader.getGameLayer().findModule("neoforge").orElseThrow(), name);
        require(type != null, "HOST_CLASS_MISSING " + name); return type;
    }
    private static void validateListeners(Class<?> entry, ForgeClientEventPlan plan) {
        require(entry.getSuperclass() == Object.class && entry.getInterfaces().length == 0
            && entry.getDeclaredFields().length == 0 && entry.getDeclaredMethods().length == 5, "LIVE_ENTRY_STRUCTURE");
        for (var method : entry.getDeclaredMethods()) {
            if (method.getName().equals(ForgeClientEventPlan.ADAPTER)) {
                require(method.isSynthetic() && method.getModifiers() == (java.lang.reflect.Modifier.PRIVATE
                    | java.lang.reflect.Modifier.STATIC | 0x1000), "LIVE_CONFIG_ADAPTER"); continue;
            }
            String suffix = plan.listeners.get(method.getName());
            require(suffix != null && method.getModifiers() == java.lang.reflect.Modifier.PUBLIC
                && method.getReturnType() == void.class && method.getParameterCount() == 1
                && method.getParameterTypes()[0] == host((ForgeClientEventPlan.HOST + suffix).replace('/', '.')),
                "LIVE_LISTENER_IDENTITY");
            var annotations = method.getDeclaredAnnotations();
            require(annotations.length == 1 && annotations[0].annotationType() == net.neoforged.bus.api.SubscribeEvent.class,
                "LIVE_SUBSCRIBE_IDENTITY");
            var annotation = (net.neoforged.bus.api.SubscribeEvent) annotations[0];
            require(annotation.priority() == net.neoforged.bus.api.EventPriority.NORMAL && !annotation.receiveCanceled(),
                "LIVE_SUBSCRIBE_OPTIONS");
        }
    }
    private static void require(boolean value, String why) { if (!value) throw new IllegalStateException("Client event bridge rejected: " + why); }
}
