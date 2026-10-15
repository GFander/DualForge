package com.example.dependencyb;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod("dependency_b")
public class DependencyB {
    public DependencyB(IEventBus bus) {
        System.out.println("[DF007C2] B CONSTRUCTOR");
        MinecraftForge.EVENT_BUS.register(this);
        System.out.println("[DF007C2] B REGISTER RETURNED");
    }
    @SubscribeEvent public void onDependency(DependencyEvent event) { }
}
