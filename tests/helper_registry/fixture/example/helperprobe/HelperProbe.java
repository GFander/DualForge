package example.helperprobe;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
@Mod("helperprobe")
public class HelperProbe {
    public HelperProbe() {
        var bus = FMLJavaModLoadingContext.get().getModEventBus();
        ItemHelper.attach(bus);
        System.out.println("[DF4B] CONSTRUCTED");
    }
}
