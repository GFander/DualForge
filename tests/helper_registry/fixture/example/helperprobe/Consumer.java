package example.helperprobe;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;
public final class Consumer {
    private Consumer() {}
    public static Item alpha() { return ItemHelper.alphaHolder().get(); }
    public static Item beta() { return ItemHelper.BETA.get(); }
    public static RegistryObject<Item> receipt() { return ItemHelper.alphaHolder(); }
}
