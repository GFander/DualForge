package org.example.combinedbus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
@Mod("catalogbus")
public class EntryPoint {
 public static Object busReceipt;
 public EntryPoint(){var bus=FMLJavaModLoadingContext.get().getModEventBus();busReceipt=bus;Items.attach(bus);System.out.println("[DF4C3] BEFORE TAB");Shelves.attach(bus);MinecraftForge.EVENT_BUS.register(this);System.out.println("[DF4C3] CONSTRUCTED combined");}
 @SubscribeEvent public void started(ServerStartedEvent event){System.out.println("[DF4C3] HANDLER combined");}
}
