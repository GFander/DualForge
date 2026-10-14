package com.example.dropindriver;

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

/** Post-registration observation only. No register(), event posting or Item construction. */
@Mod("dropindriver")
public final class DropInDriver {
    public DropInDriver() { NeoForge.EVENT_BUS.register(this); }

    @SubscribeEvent
    public void started(ServerStartedEvent event) throws ReflectiveOperationException {
        var owner = (FMLModContainer) ModList.get().getModContainerById("dropinprobe").orElseThrow();
        if (owner.getEventBus() == NeoForge.EVENT_BUS) throw new IllegalStateException("Wrong MOD bus");
        Class<?> entry = Class.forName("com.example.dropin.DropInItems", false,
                FMLLoader.getGameLayer().findLoader("dropinprobe"));
        Item[] items = new Item[2];
        String[] ids = {"ice_ether", "raw_ice_ether"};
        String[] fields = {"ICE_ETHER", "RAW_ICE_ETHER"};
        String[] accessors = {"iceValue", "rawValue"};
        for (int i = 0; i < ids.length; i++) {
            var key = ResourceLocation.fromNamespaceAndPath("dropinprobe", ids[i]);
            items[i] = BuiltInRegistries.ITEM.getOptional(key).orElseThrow();
            Object holder = entry.getField(fields[i]).get(null);
            if (!(holder instanceof net.neoforged.neoforge.registries.DeferredHolder<?, ?> deferred)
                    || deferred.get() != items[i] || entry.getMethod(accessors[i]).invoke(null) != items[i]
                    || items[i] == Items.AIR || items[i].getClass() != Item.class
                    || !BuiltInRegistries.ITEM.getKey(items[i]).equals(key)) {
                throw new IllegalStateException("Registry/Holder/object identity disagrees: " + key);
            }
            System.out.println("[DF007] VERIFIED id=" + key + " holderGet=true sameObject=true air=false");
        }
        if (items[0] == items[1]) throw new IllegalStateException("Aliased items");
        System.out.println("[DF007] QUERY PASS distinctItems=true officialModBus=true");
    }
}
