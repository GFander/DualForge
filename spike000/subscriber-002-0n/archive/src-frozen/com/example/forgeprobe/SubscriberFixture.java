package com.example.forgeprobe;

public class SubscriberFixture {
    public int hits;

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public void onProbe(ProbeEvent event) {
        hits++;
        System.out.println("[SUBSCRIBER-FIXTURE] HANDLER REACHED hits=" + hits);
    }
}
