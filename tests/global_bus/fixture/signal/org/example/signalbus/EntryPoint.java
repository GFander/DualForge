package org.example.signalbus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
@Mod("signalbus")
public class EntryPoint {
 public static Object busReceipt;
 public EntryPoint(){var bus=FMLJavaModLoadingContext.get().getModEventBus();busReceipt=bus;MinecraftForge.EVENT_BUS.register(this);System.out.println("[DF4C3] CONSTRUCTED signal");}
 @SubscribeEvent public void started(ServerStartedEvent event){System.out.println("[DF4C3] HANDLER signal");}
}
