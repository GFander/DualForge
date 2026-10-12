package com.example.dispatchcontrol;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
public final class UnclaimedMarker {
    public UnclaimedMarker(IEventBus bus) { }
    @SubscribeEvent public void onDispatch(RejectedEvent event) { System.out.println("UNCLAIMED MARKER"); }
}
