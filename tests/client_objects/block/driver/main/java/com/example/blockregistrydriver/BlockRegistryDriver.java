package com.example.blockregistrydriver;

import java.io.PrintStream;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.javafmlmod.FMLModContainer;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/** Observes actual supplier stacks and registry/holder identities; creates or registers no objects. */
@Mod("blockregistrydriver")
public final class BlockRegistryDriver {
    private final PrintStream original = System.out;
    private final int[] calls = new int[2];
    private final int[] sequence = new int[2];
    private final boolean[][] official = new boolean[2][3];
    private int clock;
    private boolean queried;
    private Block blockAvailableToItem;
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("blockregistryprobe", "ice_ether_block");

    public BlockRegistryDriver() {
        System.setOut(new PrintStream(original) {
            @Override public void println(String text) {
                original.println(text);
                int index;
                if ("[DF006B] BLOCK SUPPLIER REACHED".equals(text)) index = 0;
                else if ("[DF006B] BLOCKITEM SUPPLIER REACHED".equals(text)) index = 1;
                else return;
                var frames = StackWalker.getInstance(StackWalker.Option.SHOW_HIDDEN_FRAMES).walk(s -> s.skip(1).toList());
                if (!frames.getFirst().getClassName().equals("com.example.blockregistrycheck.BlockRegistryProbe"))
                    throw new IllegalStateException("Supplier source is not original Forge entry");
                calls[index]++; sequence[index] = ++clock;
                for (var frame : frames) {
                    original.println("[DF006B] FRAME role=" + index + " " + frame.getClassName() + "." + frame.getMethodName());
                    official[index][0] |= frame.getClassName().equals("net.neoforged.neoforge.registries.RegisterEvent") && frame.getMethodName().equals("register");
                    official[index][1] |= frame.getClassName().equals("net.neoforged.neoforge.registries.DeferredRegister") && frame.getMethodName().equals("addEntries");
                    official[index][2] |= frame.getClassName().equals("net.neoforged.fml.ModLoader") && frame.getMethodName().equals("postEventWrapContainerInModOrder");
                }
                if (index == 1) {
                    try {
                        blockAvailableToItem = BuiltInRegistries.BLOCK.getOptional(ID).orElseThrow();
                        if (entry().getMethod("iceEtherBlockValue").invoke(null) != blockAvailableToItem)
                            throw new IllegalStateException("Block holder unavailable or wrong at Item supplier");
                        original.println("[DF006B] ITEM_PHASE blockRegistered=true holderBound=true sequence=" + sequence[index]);
                    } catch (ReflectiveOperationException ex) { throw new IllegalStateException(ex); }
                }
            }
        });
        NeoForge.EVENT_BUS.register(this);
    }
    private static Class<?> entry() throws ClassNotFoundException {
        return Class.forName("com.example.blockregistrycheck.BlockRegistryProbe", false,
                FMLLoader.getGameLayer().findLoader("blockregistryprobe"));
    }
    @SubscribeEvent
    public void onStarted(ServerStartedEvent event) throws ReflectiveOperationException {
        if (queried) throw new IllegalStateException("Duplicate query");
        queried = true; System.setOut(original);
        boolean enabled = Boolean.getBoolean("DF006B_REGISTER");
        var container = (FMLModContainer) ModList.get().getModContainerById("blockregistryprobe").orElseThrow();
        if (container.getEventBus() == NeoForge.EVENT_BUS) throw new IllegalStateException("Global bus substituted for MOD bus");
        System.out.println("[DF006B] CONTAINER MODBUS=" + System.identityHashCode(container.getEventBus()) + " GLOBALBUS=" + System.identityHashCode(NeoForge.EVENT_BUS));
        var old = ResourceLocation.fromNamespaceAndPath("blockregistryprobe", "sample_block");
        if (BuiltInRegistries.BLOCK.containsKey(old) || BuiltInRegistries.ITEM.containsKey(old))
            throw new IllegalStateException("Legacy sample_block still registered");
        System.out.println("[DF006B] LEGACY_IDS_ABSENT sample_block");
        boolean block = BuiltInRegistries.BLOCK.containsKey(ID), item = BuiltInRegistries.ITEM.containsKey(ID);
        if (block != enabled || item != enabled || BuiltInRegistries.BLOCK.getOptional(ID).isPresent() != enabled
                || BuiltInRegistries.ITEM.getOptional(ID).isPresent() != enabled || calls[0] != (enabled ? 1 : 0) || calls[1] != (enabled ? 1 : 0))
            throw new IllegalStateException("Actual registry or supplier result disagrees with switch");
        System.out.println("[DF006B] QUERY block=" + block + " item=" + item + " blockSuppliers=" + calls[0] + " itemSuppliers=" + calls[1]);
        if (enabled) {
            Block b = BuiltInRegistries.BLOCK.getOptional(ID).orElseThrow();
            var i = BuiltInRegistries.ITEM.getOptional(ID).orElseThrow();
            if (b == Blocks.AIR || b.getClass() != Block.class || i == Items.AIR || i.getClass() != BlockItem.class
                    || ((BlockItem) i).getBlock() != b || blockAvailableToItem != b
                    || !ID.equals(BuiltInRegistries.BLOCK.getKey(b)) || !ID.equals(BuiltInRegistries.ITEM.getKey(i)))
                throw new IllegalStateException("Wrong actual Block/BlockItem association or key");
            Class<?> clazz = entry();
            var bh = clazz.getField("ICE_ETHER_BLOCK").get(null); var ih = clazz.getField("ICE_ETHER_BLOCK_ITEM").get(null);
            if (!(bh instanceof DeferredHolder<?, ?>) || !(ih instanceof DeferredHolder<?, ?>) || bh == ih
                    || clazz.getMethod("iceEtherBlockValue").invoke(null) != b || clazz.getMethod("iceEtherBlockItemValue").invoke(null) != i)
                throw new IllegalStateException("Holder/get identity failure");
            if (!((DeferredHolder<?, ?>) bh).getKey().registry().equals(BuiltInRegistries.BLOCK.key().location())
                    || !((DeferredHolder<?, ?>) ih).getKey().registry().equals(BuiltInRegistries.ITEM.key().location()))
                throw new IllegalStateException("Holder assigned to wrong registry");
            if (sequence[0] != 1 || sequence[1] != 2) throw new IllegalStateException("Unexpected supplier ordering or extra instance");
            for (boolean[] row : official) for (boolean ok : row) if (!ok) throw new IllegalStateException("Official lifecycle stack absent");
            System.out.println("[DF006B] VERIFIED blockKey=" + BuiltInRegistries.BLOCK.getKey(b) + " itemKey=" + BuiltInRegistries.ITEM.getKey(i)
                    + " sameBlock=true holdersGet=true holderRegistries=true blockBeforeItem=true");
        }
        System.out.println("[DF006B] QUERY PASS");
    }
}
