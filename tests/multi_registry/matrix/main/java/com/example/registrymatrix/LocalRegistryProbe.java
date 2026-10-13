package com.example.registrymatrix;

import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Compile-only layout control: the initializer keeps the register in a local alias. */
@Mod("matrixregistryprobe")
public final class LocalRegistryProbe {
    public static final DeferredRegister<Item> DEPOT;
    public static final RegistryObject<Item> FIRST_TOKEN;
    public static final RegistryObject<Item> SECOND_TOKEN;

    static {
        DeferredRegister<Item> queue = DeferredRegister.create(ForgeRegistries.ITEMS, "matrixregistryprobe");
        DEPOT = queue;
        FIRST_TOKEN = queue.register("gamma_item", () -> {
            System.out.println("[DF006-MATRIX] GAMMA SUPPLIER REACHED");
            return new Item(new Item.Properties());
        });
        SECOND_TOKEN = queue.register("delta_item", LocalRegistryProbe::makeDelta);
    }

    public LocalRegistryProbe(IEventBus modBus) {
        System.out.println("[DF006] CONSTRUCTOR REACHED");
        System.out.print("[DF006] INJECTED MODBUS=");
        System.out.println(System.identityHashCode(modBus));
        if (Boolean.getBoolean("DF006_REGISTER")) {
            DEPOT.register(modBus);
            System.out.println("[DF006] ATTACH RETURNED");
        } else {
            System.out.println("[DF006] ATTACH SKIPPED");
        }
    }

    private static Item makeDelta() {
        System.out.println("[DF006-MATRIX] DELTA SUPPLIER REACHED");
        return new Item(new Item.Properties());
    }

    public static Item firstEntry() { return FIRST_TOKEN.get(); }
    public static Item secondEntry() { return SECOND_TOKEN.get(); }
}
