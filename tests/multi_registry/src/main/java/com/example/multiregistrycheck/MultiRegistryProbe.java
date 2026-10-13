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
    public static final RegistryObject<Item> PRIMARY = CATALOG.register("alpha_item", () -> {
        System.out.println("[DF006] ALPHA SUPPLIER REACHED");
        return new Item(new Item.Properties());
    });
    public static final RegistryObject<Item> SECONDARY = CATALOG.register("beta_item", MultiRegistryProbe::createSecondItem);

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

    private static Item createSecondItem() {
        System.out.println("[DF006] BETA SUPPLIER REACHED");
        return new Item(new Item.Properties());
    }

    public static Item alphaValue() { return PRIMARY.get(); }
    public static Item betaValue() { return SECONDARY.get(); }
}
