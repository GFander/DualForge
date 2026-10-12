package com.example.forgeprobe;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod("forgeprobe")
public class ForgeProbe {
    public ForgeProbe(IEventBus bus) {
        System.out.println("=================================");
        System.out.println("FORGE PROBE CONSTRUCTOR REACHED");
        System.out.println("BUS = " + bus);
        System.out.println("=================================");
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onProbe(ProbeEvent event) { }
}
