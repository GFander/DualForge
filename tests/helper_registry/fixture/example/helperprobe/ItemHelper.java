package example.helperprobe;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
public final class ItemHelper {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "helperprobe");
    public static final RegistryObject<Item> ALPHA = ITEMS.register("alpha_item", () -> {
        System.out.println("[DF4B] SUPPLIER alpha");
        return new Item(new Item.Properties());
    });
    public static final RegistryObject<Item> BETA = ITEMS.register("beta_item", ItemHelper::makeBeta);
    private ItemHelper() {}
    private static Item makeBeta() {
        System.out.println("[DF4B] SUPPLIER beta");
        return new Item(new Item.Properties());
    }
    public static RegistryObject<Item> alphaHolder() { return ALPHA; }
    public static void attach(IEventBus bus) {
        if (!"false".equals(System.getenv("DF4B_REGISTER"))) ITEMS.register(bus);
    }
}
