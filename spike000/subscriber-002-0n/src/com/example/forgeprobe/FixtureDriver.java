package com.example.forgeprobe;

import net.neoforged.bus.api.BusBuilder;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.IEventBus;

public final class FixtureDriver {
    public static void main(String[] args) {
        String value = System.getProperty("forgecompat.0n.post");
        if (!"true".equals(value) && !"false".equals(value)) {
            throw new IllegalStateException("forgecompat.0n.post must be explicitly true or false");
        }
        boolean post = Boolean.parseBoolean(value);
        IEventBus bus = BusBuilder.builder().build();
        SubscriberFixture listener = new SubscriberFixture();
        ProbeEvent event = new ProbeEvent();
        bus.register(listener);
        System.out.println("[SUBSCRIBER-FIXTURE] register returned hits=" + listener.hits);
        if (listener.hits != 0) throw new IllegalStateException("unexpected invocation before post");
        if (post) {
            Event returned = bus.post(event);
            System.out.println("[SUBSCRIBER-FIXTURE] post returned sameEvent=" + (returned == event));
            if (returned != event) throw new IllegalStateException("post did not return same event");
        } else {
            System.out.println("[SUBSCRIBER-FIXTURE] post skipped");
        }
        int expected = post ? 1 : 0;
        System.out.println("[SUBSCRIBER-FIXTURE] FINAL hits=" + listener.hits + " expected=" + expected);
        if (listener.hits != expected) throw new IllegalStateException("handler count mismatch");
    }
}
