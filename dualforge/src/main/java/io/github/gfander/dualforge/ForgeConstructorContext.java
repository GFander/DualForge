package io.github.gfander.dualforge;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.javafmlmod.FMLModContainer;

/** Constructor-only source-ABI view of the current container's actual host Mod Bus. */
public final class ForgeConstructorContext {
    private static final StackWalker WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
    private final ModContainer owner;
    private final Class<?> entry;
    private final BusView view;

    private ForgeConstructorContext(ModContainer owner, Class<?> entry) {
        this.owner = owner;
        this.entry = entry;
        this.view = new BusView(owner);
    }

    public static ForgeConstructorContext get() {
        var caller = WALKER.walk(frames -> frames.skip(1).findFirst().orElseThrow());
        require("<init>".equals(caller.getMethodName()), "outside entry constructor");
        ForgePlatformDependencyResolver.Host.current().validate();
        ModContainer owner = ModLoadingContext.get().getActiveContainer();
        validateCaller(caller.getDeclaringClass(), owner);
        System.out.println("[DUALFORGE-CONTEXT] BOUND modId=" + owner.getModId() + " entry=" + caller.getClassName());
        return new ForgeConstructorContext(owner, caller.getDeclaringClass());
    }

    public net.minecraftforge.eventbus.api.IEventBus getModEventBus() {
        var caller = WALKER.walk(frames -> frames.skip(1).findFirst().orElseThrow());
        require("<init>".equals(caller.getMethodName()) && caller.getDeclaringClass() == entry
                && ModLoadingContext.get().getActiveContainer() == owner, "foreign, reentrant or escaped context");
        validateCaller(entry, owner);
        return view;
    }

    private static void validateCaller(Class<?> caller, ModContainer container) {
        require(container instanceof FMLModContainer
                && container.getModInfo().getLoader() instanceof DualForgeLanguageLoader, "native or unowned active container");
        var info = container.getModInfo();
        require(caller.getModule().isNamed() && info.getOwningFile().moduleName().equals(caller.getModule().getName())
                && DualForgeLanguageLoader.forgeEntrypoints(info, info.getOwningFile().getFile().getScanResult()).contains(caller.getName()),
                "caller does not belong to active Forge metadata/module");
        require(container.getEventBus() != null, "active container has no Mod Bus");
    }

    // Internal bridge validation returns a verified container, never a public raw-bus accessor.
    static ModContainer registryOwner(net.minecraftforge.eventbus.api.IEventBus source, Class<?> helper) {
        require(WALKER.getCallerClass() == ForgeRegistryBridge.class, "unowned bridge caller");
        require(source instanceof BusView, "unrecognized source BusView");
        BusView view = (BusView) source;
        ModContainer active = ModLoadingContext.get().getActiveContainer();
        require(active == view.owner && active.getEventBus() == view.delegate, "foreign or escaped registry context");
        require(helper.getModule().isNamed() && helper.getModule().getName().equals(active.getModInfo().getOwningFile().moduleName()), "foreign helper module");
        var entries = DualForgeLanguageLoader.forgeEntrypoints(active.getModInfo(), active.getModInfo().getOwningFile().getFile().getScanResult());
        boolean constructing = WALKER.walk(frames -> frames.anyMatch(f -> f.getMethodName().equals("<init>")
                && f.getDeclaringClass().getModule() == helper.getModule() && entries.contains(f.getClassName())));
        require(constructing, "registry bridge outside owned entry constructor");
        require(active instanceof FMLModContainer && active.getModInfo().getLoader() instanceof DualForgeLanguageLoader, "native registry owner");
        return active;
    }

    // The existing source interface remains a marker shell. This view adds no Forge
    // register/post/listener API and creates no bus; its delegate is the real host bus.
    private static final class BusView implements net.minecraftforge.eventbus.api.IEventBus {
        private final ModContainer owner;
        private final net.neoforged.bus.api.IEventBus delegate;
        private BusView(ModContainer owner) { this.owner = owner; this.delegate = owner.getEventBus(); }
    }

    private static void require(boolean valid, String reason) {
        if (!valid) throw new IllegalStateException("Forge constructor context rejected: " + reason);
    }
}
