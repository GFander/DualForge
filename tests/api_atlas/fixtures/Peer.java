package research.fixture;

import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;

public class Peer {
    public static RegistryObject<Item> call() { return AtlasProbe.getToken(); }
}
