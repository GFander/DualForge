package org.example.publisher.platform;

public final class Services {
    public static final EventPort EVENT = java.util.ServiceLoader.load(EventPort.class).findFirst().orElseThrow();
}
