package com.example.blockmatrix;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

@Mod("blockmatrix")
public final class RenamedBlockProbe {
    public static final DeferredRegister<Block> BLOCK_DEPOT = DeferredRegister.create(ForgeRegistries.BLOCKS, "blockmatrix");
    public static final DeferredRegister<Item> ITEM_DEPOT = DeferredRegister.create(ForgeRegistries.ITEMS, "blockmatrix");
    public static final RegistryObject<Block> CUBE = BLOCK_DEPOT.register("solid_cube", () -> {
        System.out.println("[DF006B] BLOCK SUPPLIER REACHED");
        return new Block(BlockBehaviour.Properties.of());
    });
    public static final RegistryObject<BlockItem> CUBE_ITEM = ITEM_DEPOT.register("solid_cube", () -> {
        System.out.println("[DF006B] BLOCKITEM SUPPLIER REACHED");
        return new BlockItem(CUBE.get(), new Item.Properties());
    });
    public RenamedBlockProbe(IEventBus modBus) {
        System.out.println("[DF006B] CONSTRUCTOR REACHED");
        System.out.print("[DF006B] INJECTED MODBUS=");
        System.out.println(System.identityHashCode(modBus));
        if (Boolean.getBoolean("DF006B_REGISTER")) {
            BLOCK_DEPOT.register(modBus);
            ITEM_DEPOT.register(modBus);
            System.out.println("[DF006B] ATTACH RETURNED");
        } else {
            System.out.println("[DF006B] ATTACH SKIPPED");
        }
    }
    public static Block cubeValue() { return CUBE.get(); }
    public static BlockItem cubeItemValue() { return CUBE_ITEM.get(); }
}
