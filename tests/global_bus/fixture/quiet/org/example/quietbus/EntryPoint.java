package org.example.quietbus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.common.MinecraftForge;
@Mod("quietbus")
public class EntryPoint {
 public static Object busReceipt;
 public EntryPoint(){var bus=FMLJavaModLoadingContext.get().getModEventBus();busReceipt=bus;MinecraftForge.EVENT_BUS.register(this);System.out.println("[DF4C3] CONSTRUCTED quiet");}
}
