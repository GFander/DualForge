package com.example.blockregistrycheck;
import java.io.Serializable;
import java.util.function.Supplier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
public final class SupplierForms {
    public static Supplier<Block> captured(String marker) {
        return () -> { System.out.println(marker); return new Block(BlockBehaviour.Properties.of()); };
    }
    public static Supplier<Block> alternate() {
        return (Supplier<Block> & Serializable) () -> { System.out.println("alternative"); return new Block(BlockBehaviour.Properties.of()); };
    }
}
