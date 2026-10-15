package com.example.contextpeer;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("contextpeer")
public class ContextPeer {
    public static Object capturedBus;

    public ContextPeer() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        capturedBus = bus;
        System.out.println("[DF007C3] CONTEXT CONSTRUCTED contextpeer");
    }
}
