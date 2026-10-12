package com.example.identitycheck;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod("identityprobe")
public class IdentityProbe {
    public IdentityProbe(IEventBus bus) {
        System.out.println("=================================");
        System.out.println("IDENTITY PROBE CONSTRUCTOR REACHED");
        System.out.println("BUS = " + bus);
        System.out.println("=================================");
        MinecraftForge.EVENT_BUS.register(this);
        System.out.println("IDENTITY PROBE REGISTER RETURNED");
    }

    @SubscribeEvent
    public void onIdentity(IdentityEvent event) { }
}
