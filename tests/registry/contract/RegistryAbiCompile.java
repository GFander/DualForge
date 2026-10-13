package com.example.registrycontract;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.world.item.Item;
/** Gate 0 native API compile experiment, not the final mod fixture. */
public final class RegistryAbiCompile {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "registryprobe");
    public static final RegistryObject<Item> ICE_ETHER = ITEMS.register("ice_ether", () -> new Item(new Item.Properties()));
    public RegistryAbiCompile(IEventBus bus) { ITEMS.register(bus); }
    public static Item item() { return ICE_ETHER.get(); }
}
