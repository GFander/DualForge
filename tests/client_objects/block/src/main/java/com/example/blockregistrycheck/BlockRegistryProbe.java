package com.example.blockregistrycheck;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

@Mod("blockregistryprobe")
public final class BlockRegistryProbe {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, "blockregistryprobe");
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "blockregistryprobe");
    public static final RegistryObject<Block> ICE_ETHER_BLOCK = BLOCKS.register("ice_ether_block", () -> {
        System.out.println("[DF006B] BLOCK SUPPLIER REACHED");
        return new Block(BlockBehaviour.Properties.of());
    });
    public static final RegistryObject<BlockItem> ICE_ETHER_BLOCK_ITEM = ITEMS.register("ice_ether_block", () -> {
        System.out.println("[DF006B] BLOCKITEM SUPPLIER REACHED");
        return new BlockItem(ICE_ETHER_BLOCK.get(), new Item.Properties());
    });
    public BlockRegistryProbe(IEventBus modBus) {
        System.out.println("[DF006B] CONSTRUCTOR REACHED");
        System.out.print("[DF006B] INJECTED MODBUS=");
        System.out.println(System.identityHashCode(modBus));
        if (Boolean.getBoolean("DF006B_REGISTER")) {
            BLOCKS.register(modBus);
            ITEMS.register(modBus);
            System.out.println("[DF006B] ATTACH RETURNED");
        } else {
            System.out.println("[DF006B] ATTACH SKIPPED");
        }
    }
    public static Block iceEtherBlockValue() { return ICE_ETHER_BLOCK.get(); }
    public static BlockItem iceEtherBlockItemValue() { return ICE_ETHER_BLOCK_ITEM.get(); }
}
