package com.example.registryprobe;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.world.item.Item;

/** Original Forge input: the property only gates attachment to the injected MOD bus. */
@Mod("registryprobe")
public final class RegistryProbe {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "registryprobe");
    public static final RegistryObject<Item> ICE_ETHER = ITEMS.register("ice_ether", () -> {
        System.out.println("[DF005] SUPPLIER REACHED");
        return new Item(new Item.Properties());
    });

    public RegistryProbe(IEventBus modBus) {
        System.out.println("[DF005] CONSTRUCTOR REACHED");
        System.out.print("[DF005] INJECTED MODBUS=");
        System.out.println(System.identityHashCode(modBus));
        if (Boolean.getBoolean("DF005_REGISTER")) {
            ITEMS.register(modBus);
            System.out.println("[DF005] ATTACH RETURNED");
        } else {
            System.out.println("[DF005] ATTACH SKIPPED");
        }
    }

    public static Item registeredItem() { return ICE_ETHER.get(); }
}
