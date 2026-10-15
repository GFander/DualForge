package demo.shelves;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
@Mod("tabboundary")
public class EntryPoint {
 public static Object busReceipt;
 public EntryPoint(){var bus=FMLJavaModLoadingContext.get().getModEventBus();busReceipt=bus;Items.attach(bus);System.out.println("[DF4C2] BEFORE TAB");Shelves.attach(bus);System.out.println("[DF4C2] CONSTRUCTED");}
}
