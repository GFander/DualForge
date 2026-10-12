package com.example.dispatchcheck;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.common.MinecraftForge;
@Mod("dispatchprobe")
public final class DispatchProbe {
    public DispatchProbe(IEventBus bus) {
        System.out.println("[DF004] CONSTRUCTOR REACHED");
        String value = System.getProperty("DF004_REGISTER");
        if (!"true".equals(value) && !"false".equals(value)) throw new IllegalStateException("Missing DF004_REGISTER");
        System.out.println("[DF004] REGISTER SWITCH=" + value);
        if (Boolean.parseBoolean(value)) {
            MinecraftForge.EVENT_BUS.register(this);
            System.out.println("[DF004] REGISTER RETURNED");
        } else {
            System.out.println("[DF004] REGISTER SKIPPED");
        }
    }
    @SubscribeEvent
    public void onDispatch(DispatchEvent event) {
        System.out.println("[DF004] CALLBACK REACHED");
    }
}
