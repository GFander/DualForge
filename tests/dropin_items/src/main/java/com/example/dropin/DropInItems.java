package com.example.dropin;

import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Ordinary Forge registration: no test business properties or diagnostic markers. */
@Mod("dropinprobe")
public final class DropInItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "dropinprobe");
    public static final RegistryObject<Item> ICE_ETHER = ITEMS.register("ice_ether", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> RAW_ICE_ETHER = ITEMS.register("raw_ice_ether", DropInItems::createRaw);

    public DropInItems(IEventBus modBus) { ITEMS.register(modBus); }

    private static Item createRaw() { return new Item(new Item.Properties()); }
    public static Item iceValue() { return ICE_ETHER.get(); }
    public static Item rawValue() { return RAW_ICE_ETHER.get(); }
}
