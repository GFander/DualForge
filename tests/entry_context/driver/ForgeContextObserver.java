package com.example.contextobserver;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLModContainer;

@Mod("forgecontextobserver")
public class ForgeContextObserver {
    public ForgeContextObserver() {
        MinecraftForge.EVENT_BUS.addListener(this::started);
    }

    private void started(ServerStartedEvent event) {
        try {
            Object previous = null;
            for (String[] entry : new String[][]{{"contextprobe", "com.example.context.ContextProbe"}, {"contextpeer", "com.example.contextpeer.ContextPeer"}}) {
                var container = (FMLModContainer) ModList.get().getModContainerById(entry[0]).orElseThrow();
                Object receipt = container.getMod().getClass().getField("capturedBus").get(null);
                if (receipt == null || receipt != container.getEventBus() || receipt == MinecraftForge.EVENT_BUS || receipt == previous)
                    throw new AssertionError("Wrong native Mod Bus for " + entry[0]);
                previous = receipt;
                System.out.println("[DF007C3-NATIVE] BUS_IDENTITY owner=" + entry[0] + " exact=true global=false");
            }
            System.out.println("[DF007C3-NATIVE] CONTEXT PASS owners=2 distinct=true");
        } catch (ReflectiveOperationException error) {
            throw new AssertionError(error);
        }
    }
}
