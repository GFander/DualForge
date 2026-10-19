package io.github.gfander.dualforge;

import java.lang.ref.WeakReference;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.javafmlmod.FMLModContainer;
import net.neoforged.fml.loading.FMLLoader;
import static io.github.gfander.dualforge.ForgeDualBusPlan.require;

/** Actual host references and owner provenance. No bus is constructed here. */
final class ForgeBusIdentity {
    final WeakReference<ModContainer> owner;
    final Module module;
    final ClassLoader loader;
    final Object file;
    final ModuleLayer layer;
    final IEventBus game, mod;
    ForgeBusIdentity(ModContainer owner,Class<?> entry) {
        this.owner=new WeakReference<>(owner);module=entry.getModule();loader=entry.getClassLoader();file=owner.getModInfo().getOwningFile();layer=FMLLoader.getGameLayer();
        verifyOwner(owner,entry);
        mod=owner.getEventBus();
        try {game=(IEventBus)host("net.neoforged.neoforge.common.NeoForge","neoforge").getField("EVENT_BUS").get(null);}
        catch(ReflectiveOperationException e){throw new IllegalStateException("HOST_GAME_BUS_UNAVAILABLE",e);}
        require(game!=null && mod!=null && game!=mod,"UNKNOWN_BUS_ORIGIN");
        require(game.getClass()==mod.getClass() && game.getClass().getModule()==IEventBus.class.getModule(),"UNKNOWN_BUS_ORIGIN");
    }
    static ModContainer active(Class<?> entry) {
        ForgePlatformDependencyResolver.Host.current().validate();
        ModContainer owner=ModLoadingContext.get().getActiveContainer();
        require(owner instanceof FMLModContainer && owner.getModInfo().getLoader() instanceof DualForgeLanguageLoader,"WRONG_MOD_CONTAINER");
        verifyOwner(owner,entry);return owner;
    }
    static void verifyOwner(ModContainer owner,Class<?> entry) {
        var info=owner.getModInfo();var module=entry.getModule();
        require(module.isNamed() && module.getLayer()==FMLLoader.getGameLayer() && module.getName().equals(info.getOwningFile().moduleName()),"WRONG_MODULE");
        require(entry.getClassLoader()==module.getClassLoader(),"WRONG_CLASSLOADER");
        require(DualForgeLanguageLoader.forgeEntrypoints(info,info.getOwningFile().getFile().getScanResult()).equals(java.util.List.of(entry.getName())),"WRONG_MODFILE");
    }
    void sameOwner(Class<?> caller) {
        ModContainer value=owner.get();require(value!=null,"WRONG_MOD_CONTAINER");
        require(value.getModInfo().getOwningFile()==file,"WRONG_MODFILE");
        require(caller.getModule()==module && module.getLayer()==layer && layer==FMLLoader.getGameLayer(),"WRONG_MODULE");
        require(caller.getClassLoader()==loader,"WRONG_CLASSLOADER");
        require(value.getEventBus()==mod,"FOREIGN_MOD_BUS");
    }
    void route(IEventBus actual,boolean modEvent) {
        require(actual==game || actual==mod,"UNKNOWN_BUS_ORIGIN");
        require(actual==(modEvent?mod:game),"WRONG_EVENT_DOMAIN");
    }
    static Class<?> host(String name,String moduleName) {
        // FML lives in the parent layer; never resolve an arbitrary same-name class via context loader.
        if(moduleName.equals("fml_loader")) {
            try {Class<?> c=Class.forName(name,false,ModContainer.class.getClassLoader());require(c.getModule()==ModContainer.class.getModule(),"WRONG_MODULE");return c;}
            catch(ClassNotFoundException e){throw new IllegalStateException("HOST_FML_CLASS_MISSING",e);}
        }
        Class<?> c=Class.forName(FMLLoader.getGameLayer().findModule(moduleName).orElseThrow(),name);require(c!=null,"HOST_CLASS_MISSING");return c;
    }
}
