package com.example.multiregistrycheck;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.world.item.Item;

/** Original Forge input; only attachment to the injected MOD bus is conditional. */
@Mod("multiregistryprobe")
public final class MultiRegistryProbe {
    public static final DeferredRegister<Item> CATALOG = DeferredRegister.create(ForgeRegistries.ITEMS, "multiregistryprobe");
    public static final RegistryObject<Item> ICE_ETHER = CATALOG.register("ice_ether", () -> {
        System.out.println("[DF006] ICE_ETHER SUPPLIER REACHED");
        return new Item(new Item.Properties());
    });
    public static final RegistryObject<Item> RAW_ICE_ETHER = CATALOG.register("raw_ice_ether", MultiRegistryProbe::createRawIceEther);

    public MultiRegistryProbe(IEventBus modBus) {
        System.out.println("[DF006] CONSTRUCTOR REACHED");
        System.out.print("[DF006] INJECTED MODBUS=");
        System.out.println(System.identityHashCode(modBus));
        if (Boolean.getBoolean("DF006_REGISTER")) {
            CATALOG.register(modBus);
            System.out.println("[DF006] ATTACH RETURNED");
        } else {
            System.out.println("[DF006] ATTACH SKIPPED");
        }
    }

    private static Item createRawIceEther() {
        System.out.println("[DF006] RAW_ICE_ETHER SUPPLIER REACHED");
        return new Item(new Item.Properties());
    }

    public static Item iceEtherValue() { return ICE_ETHER.get(); }
    public static Item rawIceEtherValue() { return RAW_ICE_ETHER.get(); }
}
