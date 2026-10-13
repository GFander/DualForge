package com.example.multiregistrydriver;

import java.io.PrintStream;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.javafmlmod.FMLModContainer;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Queries only after registration and observes supplier output; it never registers an Item. */
@Mod("multiregistrydriver")
public final class MultiRegistryDriver {
    private final PrintStream original = System.out;
    private final AtomicInteger[] supplierCalls = {new AtomicInteger(), new AtomicInteger()};
    private final boolean[] registerEventStack = new boolean[2];
    private final boolean[] deferredStack = new boolean[2];
    private final boolean[] officialLifecycleStack = new boolean[2];
    private boolean queried;

    public MultiRegistryDriver() {
        System.setOut(new PrintStream(original) {
            @Override public void println(String text) {
                original.println(text);
                int index;
                if ("[DF006] ALPHA SUPPLIER REACHED".equals(text)) index = 0;
                else if ("[DF006] BETA SUPPLIER REACHED".equals(text)) index = 1;
                else return;
                var frames = StackWalker.getInstance(StackWalker.Option.SHOW_HIDDEN_FRAMES).walk(s -> s.skip(1).toList());
                String expectedMethod = index == 0 ? "lambda$static$0" : "createSecondItem";
                if (!frames.getFirst().getClassName().equals("com.example.multiregistrycheck.MultiRegistryProbe")
                        || !frames.getFirst().getMethodName().equals(expectedMethod)) {
                    throw new IllegalStateException("Supplier marker did not originate at the original supplier: " + index);
                }
                supplierCalls[index].incrementAndGet();
                for (var frame : frames) {
                    original.println("[DF006] FRAME item=" + (index == 0 ? "alpha_item" : "beta_item") + " "
                            + frame.getClassName() + "." + frame.getMethodName());
                    registerEventStack[index] |= frame.getClassName().equals("net.neoforged.neoforge.registries.RegisterEvent")
                            && frame.getMethodName().equals("register");
                    deferredStack[index] |= frame.getClassName().equals("net.neoforged.neoforge.registries.DeferredRegister")
                            && frame.getMethodName().equals("addEntries");
                    officialLifecycleStack[index] |= frame.getClassName().equals("net.neoforged.fml.ModLoader")
                            && frame.getMethodName().equals("postEventWrapContainerInModOrder");
                }
            }
        });
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onStarted(ServerStartedEvent event) throws ReflectiveOperationException {
        if (queried) throw new IllegalStateException("Duplicate query");
        queried = true;
        System.setOut(original);
        boolean enabled = Boolean.getBoolean("DF006_REGISTER");
        var container = (FMLModContainer) ModList.get().getModContainerById("multiregistryprobe").orElseThrow();
        var bus = container.getEventBus();
        if (bus == NeoForge.EVENT_BUS) throw new IllegalStateException("Global bus used as MOD bus");
        System.out.println("[DF006] CONTAINER MODBUS=" + System.identityHashCode(bus)
                + " GLOBALBUS=" + System.identityHashCode(NeoForge.EVENT_BUS));
        Class<?> entry = Class.forName("com.example.multiregistrycheck.MultiRegistryProbe", false,
                FMLLoader.getGameLayer().findLoader("multiregistryprobe"));
        String[] paths = {"alpha_item", "beta_item"};
        String[] fields = {"PRIMARY", "SECONDARY"};
        String[] getters = {"alphaValue", "betaValue"};
        Item[] values = new Item[2];
        Object[] holders = new Object[2];
        for (int index = 0; index < paths.length; index++) {
            var id = ResourceLocation.fromNamespaceAndPath("multiregistryprobe", paths[index]);
            boolean contains = BuiltInRegistries.ITEM.containsKey(id);
            var optional = BuiltInRegistries.ITEM.getOptional(id);
            if (contains != enabled || optional.isPresent() != enabled || supplierCalls[index].get() != (enabled ? 1 : 0)) {
                throw new IllegalStateException("Actual registry/supplier disagrees with switch: " + id);
            }
            System.out.println("[DF006] QUERY phase=ServerStartedEvent id=" + id + " contains=" + contains
                    + " optional=" + optional.isPresent() + " suppliers=" + supplierCalls[index].get());
            if (!enabled) continue;
            values[index] = optional.orElseThrow();
            if (values[index] == Items.AIR || values[index].getClass() != Item.class
                    || !id.equals(BuiltInRegistries.ITEM.getKey(values[index]))) {
                throw new IllegalStateException("Wrong actual Item, key or AIR fallback: " + id);
            }
            holders[index] = entry.getField(fields[index]).get(null);
            Object held = entry.getMethod(getters[index]).invoke(null);
            if (!(holders[index] instanceof net.neoforged.neoforge.registries.DeferredHolder<?, ?>)
                    || held != values[index]) {
                throw new IllegalStateException("Holder type or get() identity mismatch: " + id);
            }
            if (!registerEventStack[index] || !deferredStack[index] || !officialLifecycleStack[index]) {
                throw new IllegalStateException("Missing actual official registration stack: " + id);
            }
            System.out.println("[DF006] VERIFIED Item key=" + BuiltInRegistries.ITEM.getKey(values[index])
                    + " holder=" + holders[index].getClass().getName() + " sameObject=true air=false");
        }
        if (enabled) {
            if (values[0] == values[1] || holders[0] == holders[1]) {
                throw new IllegalStateException("The two entries overwrite or alias one another");
            }
            System.out.println("[DF006] DISTINCT items=true holders=true");
        }
        System.out.println("[DF006] QUERY PASS");
    }
}
