package com.example.dependencya;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod("dependency_a")
public class DependencyA {
    public DependencyA(IEventBus bus) {
        System.out.println("[DF007C2] A CONSTRUCTOR");
        MinecraftForge.EVENT_BUS.register(this);
        System.out.println("[DF007C2] A REGISTER RETURNED");
    }
    @SubscribeEvent public void onDependency(DependencyEvent event) { }
}
