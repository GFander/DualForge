package com.example.registrydriver;

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

/** Queries only public APIs after registration. Never creates, posts or registers an Item. */
@Mod("registrydriver")
public final class RegistryDriver {
    private final PrintStream original = System.out;
    private final AtomicInteger supplierCalls = new AtomicInteger();
    private boolean registerEventStack;
    private boolean deferredStack;
    private boolean officialLifecycleStack;
    private boolean queried;

    public RegistryDriver() {
        // Observe the existing Forge supplier's println, without triggering that supplier.
        System.setOut(new PrintStream(original) {
            @Override public void println(String text) {
                original.println(text);
                if (!"[DF005] SUPPLIER REACHED".equals(text)) return;
                var frames = StackWalker.getInstance(StackWalker.Option.SHOW_HIDDEN_FRAMES).walk(s -> s.skip(1).toList());
                if (!frames.getFirst().getClassName().equals("com.example.registryprobe.RegistryProbe")
                        || !frames.getFirst().getMethodName().equals("lambda$static$0")) {
                    throw new IllegalStateException("Supplier marker did not originate at the original lambda");
                }
                supplierCalls.incrementAndGet();
                for (var frame : frames) {
                    original.println("[DF005] FRAME " + frame.getClassName() + "." + frame.getMethodName());
                    registerEventStack |= frame.getClassName().equals("net.neoforged.neoforge.registries.RegisterEvent")
                            && frame.getMethodName().equals("register");
                    deferredStack |= frame.getClassName().equals("net.neoforged.neoforge.registries.DeferredRegister")
                            && frame.getMethodName().equals("addEntries");
                    officialLifecycleStack |= frame.getClassName().equals("net.neoforged.fml.ModLoader")
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
        boolean enabled = Boolean.getBoolean("DF005_REGISTER");
        var container = (FMLModContainer) ModList.get().getModContainerById("registryprobe").orElseThrow();
        var bus = container.getEventBus();
        if (bus == NeoForge.EVENT_BUS) throw new IllegalStateException("Global bus used as MOD bus");
        System.out.println("[DF005] CONTAINER MODBUS=" + System.identityHashCode(bus)
                + " GLOBALBUS=" + System.identityHashCode(NeoForge.EVENT_BUS));
        var id = ResourceLocation.fromNamespaceAndPath("registryprobe", "ice_ether");
        boolean contains = BuiltInRegistries.ITEM.containsKey(id);
        var optional = BuiltInRegistries.ITEM.getOptional(id);
        if (contains != enabled || optional.isPresent() != enabled || supplierCalls.get() != (enabled ? 1 : 0)) {
            throw new IllegalStateException("Actual registry/supplier disagrees with switch: " + id);
        }
        System.out.println("[DF005] QUERY phase=ServerStartedEvent id=" + id + " contains=" + contains
                + " optional=" + optional.isPresent() + " suppliers=" + supplierCalls.get());
        if (enabled) {
            Item value = optional.orElseThrow();
            if (value == Items.AIR || value.getClass() != Item.class || !id.equals(BuiltInRegistries.ITEM.getKey(value))) {
                throw new IllegalStateException("Wrong actual Item, key or AIR fallback");
            }
            Class<?> entry = Class.forName("com.example.registryprobe.RegistryProbe", false,
                    FMLLoader.getGameLayer().findLoader("registryprobe"));
            Object holder = entry.getField("ICE_ETHER").get(null);
            Object held = entry.getMethod("registeredItem").invoke(null);
            if (!(holder instanceof net.neoforged.neoforge.registries.DeferredHolder<?, ?>) || held != value) {
                throw new IllegalStateException("Holder type or get() identity mismatch");
            }
            if (!registerEventStack || !deferredStack || !officialLifecycleStack) {
                throw new IllegalStateException("Missing actual official registration stack");
            }
            System.out.println("[DF005] VERIFIED Item key=" + BuiltInRegistries.ITEM.getKey(value)
                    + " holder=" + holder.getClass().getName() + " sameObject=true air=false");
        }
        System.out.println("[DF005] QUERY PASS");
    }
}
