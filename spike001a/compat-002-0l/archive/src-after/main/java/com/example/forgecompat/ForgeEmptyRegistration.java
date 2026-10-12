package com.example.forgecompat;

import java.lang.reflect.Method;
import net.neoforged.bus.api.IEventBus;

public final class ForgeEmptyRegistration {
    private ForgeEmptyRegistration() { }
    public static void register(IEventBus bus, Object target) {
        if (target != null) {
            Class<?> type = target.getClass();
            if ("com.example.forgeprobe.ForgeProbe".equals(type.getName())
                    && type.getSuperclass() == Object.class
                    && type.getDeclaredMethods().length == 0) {
                int customPublic = 0;
                for (Method method : type.getMethods()) {
                    if (method.getDeclaringClass() != Object.class) customPublic++;
                }
                if (customPublic == 0) {
                    System.out.println("[FORGECOMPAT-EMPTY] legacy-empty-noop target=" + type.getName()
                            + " declaredMethods=0 customPublic=0");
                    return;
                }
            }
        }
        System.out.println("[FORGECOMPAT-EMPTY] delegate to NeoForge register");
        bus.register(target);
        System.out.println("[FORGECOMPAT-SUBSCRIBER] register returned target="
                + target.getClass().getName() + " declaredMethods="
                + target.getClass().getDeclaredMethods().length);
    }
}
