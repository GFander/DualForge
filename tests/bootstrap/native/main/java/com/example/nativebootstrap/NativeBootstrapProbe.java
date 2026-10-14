package com.example.nativebootstrap;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Native NeoForge coexistence input; it never references or registers Forge objects. */
@Mod("nativebootstrap")
public final class NativeBootstrapProbe {
    public NativeBootstrapProbe() {
        System.out.println("[DF007-NATIVE] CONSTRUCTOR");
        FMLLoader.getLanguageLoadingProvider().forEach(provider -> {
            var type = provider.getClass();
            System.out.println("[DF007-NATIVE] PROVIDER class=" + type.getName() + " module="
                    + type.getModule().getName() + " version=" + provider.version());
        });
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void started(ServerStartedEvent event) { System.out.println("[DF007-NATIVE] SERVER_STARTED"); }
}
