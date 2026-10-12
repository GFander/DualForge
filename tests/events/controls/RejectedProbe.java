package com.example.dispatchcontrol;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.common.MinecraftForge;
@Mod("dispatchprobe")
public final class RejectedProbe {
    public RejectedProbe(IEventBus bus) { MinecraftForge.EVENT_BUS.register(this); }
    @SubscribeEvent public void onRejected(RejectedEvent event) {
        System.out.println("REJECTED PROFILE MARKER");
        System.out.flush();
    }
}
