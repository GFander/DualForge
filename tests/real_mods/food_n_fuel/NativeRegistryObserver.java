package io.github.gfander.dualforge.observation;

import com.google.gson.GsonBuilder;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.RegisterEvent;

/** Optional native-Forge diagnostic: queries existing objects, never creates/registers items. */
@Mod("df007cobserver")
public final class NativeRegistryObserver {
    public NativeRegistryObserver() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::registerEvent);
        MinecraftForge.EVENT_BUS.addListener(this::started);
    }

    private void registerEvent(RegisterEvent event) {
        System.out.println("DF007C_NATIVE_REGISTER_EVENT " + event.getRegistryKey());
        for (var frame : Thread.currentThread().getStackTrace())
            System.out.println("DF007C_NATIVE_REGISTER_STACK " + frame);
    }

    private void started(ServerStartedEvent event) {
        try {
            Class<?> api = Class.forName("net.minecraftforge.registries.IForgeRegistry");
            Object registry = Class.forName("net.minecraftforge.registries.ForgeRegistries").getField("ITEMS").get(null);
            Class<?> location = Class.forName("net.minecraft.resources.ResourceLocation");
            Method value = api.getMethod("getValue", location);
            Set<?> keys = (Set<?>) api.getMethod("getKeys").invoke(registry);
            Map<String, Object> items = new TreeMap<>();
            Map<String, Object> holders = new TreeMap<>();
            Object register = Class.forName("net.natevg.fnf.item.ItemRegistries").getField("ITEMS").get(null);
            for (Object holder : (Collection<?>) register.getClass().getMethod("getEntries").invoke(register)) {
                Object key = holder.getClass().getMethod("getId").invoke(holder);
                holders.put(key.toString(), holder.getClass().getMethod("get").invoke(holder));
            }
            for (Object key : keys) {
                if (!key.toString().startsWith("fnf:")) continue;
                Object item = value.invoke(registry, key);
                Map<String, Object> facts = new LinkedHashMap<>();
                facts.put("class", item.getClass().getName());
                facts.put("holderIdentical", holders.get(key.toString()) == item);
                Class<?> stackClass = Class.forName("net.minecraft.world.item.ItemStack");
                Object stack = stackClass.getConstructor(Class.forName("net.minecraft.world.level.ItemLike")).newInstance(item);
                Class<?> living = Class.forName("net.minecraft.world.entity.LivingEntity");
                Object food = item.getClass().getMethod("getFoodProperties", stackClass, living).invoke(item, stack, null);
                facts.put("foodProperties", food == null ? null : food.toString());
                if (item.getClass().getName().equals("net.natevg.fnf.item.custom.FuelItem")) {
                    facts.put("burnTimeApiResult", item.getClass().getMethod("getBurnTime", stackClass,
                         Class.forName("net.minecraft.world.item.crafting.RecipeType")).invoke(item, stack, null));
                }
                items.put(key.toString(), facts);
            }
            Object tabHolder = Class.forName("net.natevg.fnf.item.FnFCreativeModeTabs").getField("FNF_ITEM_TAB").get(null);
            Object tab = tabHolder.getClass().getMethod("get").invoke(tabHolder);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("entryClass", "net.natevg.fnf.FoodNFuel");
            result.put("runtimeModuleName", Class.forName("net.natevg.fnf.FoodNFuel").getModule().getName());
            result.put("items", items);
            result.put("registryCount", items.size());
            result.put("holderCount", holders.size());
            result.put("allHolderIdentity", items.size() == holders.size() && items.values().stream()
                    .allMatch(f -> Boolean.TRUE.equals(((Map<?, ?>)f).get("holderIdentical"))));
            result.put("creativeTabId", tabHolder.getClass().getMethod("getId").invoke(tabHolder).toString());
            result.put("creativeTabClass", tab.getClass().getName());
            result.put("foodConsumption", "NOT RUN");
            result.put("furnaceBurning", "NOT RUN");
            result.put("clientDisplay", "NOT RUN");
            Files.writeString(Path.of("fnf-observation.json"), new GsonBuilder().setPrettyPrinting().serializeNulls().create().toJson(result) + "\n");
            System.out.println("DF007C_NATIVE_OBSERVATION_COMPLETE items=" + items.size());
        } catch (Exception failure) {
            failure.printStackTrace();
            throw new IllegalStateException("Native observation failed", failure);
        }
    }
}
