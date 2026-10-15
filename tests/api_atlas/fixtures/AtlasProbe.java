package research.fixture;

import net.minecraft.world.item.Item;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryObject;

@Mod("atlasprobe")
public class AtlasProbe {
    public static RegistryObject<Item> token;

    public static RegistryObject<Item> getToken() { return token; }

    public static IForgeRegistry<Item> registry() { return ForgeRegistries.ITEMS; }

    public static DeferredRegister<Item> create() {
        return DeferredRegister.create(ForgeRegistries.ITEMS, "atlasprobe");
    }

    public static Item read() { return token.get(); }
}
