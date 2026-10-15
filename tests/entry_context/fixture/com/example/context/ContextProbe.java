package com.example.context;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("contextprobe")
public class ContextProbe {
    public static Object capturedBus;

    public ContextProbe() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        capturedBus = bus;
        System.out.println("[DF007C3] CONTEXT CONSTRUCTED contextprobe");
    }
}
