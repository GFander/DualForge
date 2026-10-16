package sample.fnfruntime;

import com.google.gson.GsonBuilder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/** Separate native read-only observer. Never registers FnF objects or invokes Tab callbacks. */
@Mod("fnfruntimeobserver")
public final class NeoRegistryObserver {
    private boolean queried;
    public NeoRegistryObserver(IEventBus bus) {
        bus.addListener(this::registration);
        NeoForge.EVENT_BUS.addListener(this::started);
    }
    private void registration(RegisterEvent event) {
        String key = event.getRegistryKey().location().toString();
        if (!Set.of("minecraft:item", "minecraft:creative_mode_tab").contains(key)) return;
        System.out.println("[DF4C4-QUERY] REGISTER_EVENT " + key);
        for (var frame : Thread.currentThread().getStackTrace())
            System.out.println("[DF4C4-QUERY] REGISTER_STACK " + frame);
    }
    private void started(ServerStartedEvent event) {
        try {
            if (queried) throw new AssertionError("Repeated ServerStarted observation");
            queried = true;
            boolean present = ModList.get().isLoaded("fnf");
            Map<String, Object> items = new TreeMap<>();
            Map<String, Object> holders = new TreeMap<>();
            for (var id : BuiltInRegistries.ITEM.keySet()) {
                if (id.getNamespace().equals("fnf")) {
                    var item = BuiltInRegistries.ITEM.getOptional(id).orElseThrow();
                    items.put(id.toString(), new LinkedHashMap<>(Map.of("class", item.getClass().getName(),
                            "registryId", BuiltInRegistries.ITEM.getKey(item).toString())));
                }
            }
            var tabs = BuiltInRegistries.CREATIVE_MODE_TAB.keySet().stream()
                    .filter(id -> id.getNamespace().equals("fnf")).map(Object::toString).sorted().toList();
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("fnfPresent", present);
            result.put("items", items);
            result.put("creativeTabs", tabs);
            if (present) {
                var owner = ModList.get().getModContainerById("fnf").orElseThrow();
                var module = getClass().getModule().getLayer().findModule(owner.getModInfo().getOwningFile().moduleName()).orElseThrow();
                Class<?> helper = Class.forName(module, "net.natevg.fnf.item.ItemRegistries");
                Object register = helper.getField("ITEMS").get(null);
                for (Object holder : (Collection<?>) register.getClass().getMethod("getEntries").invoke(register)) {
                    String id = holder.getClass().getMethod("getId").invoke(holder).toString();
                    Object value = holder.getClass().getMethod("get").invoke(holder);
                    if (!items.containsKey(id) || BuiltInRegistries.ITEM.getKey((Item)value) == null)
                        throw new AssertionError("Unregistered Holder " + id);
                    if (BuiltInRegistries.ITEM.getOptional(net.minecraft.resources.ResourceLocation.parse(id)).orElseThrow() != value)
                        throw new AssertionError("Holder identity " + id);
                    holders.put(id, true);
                }
                if (items.size() != 67 || !items.keySet().equals(holders.keySet())) throw new AssertionError("67 Item/Holder identity");
                Class<?> shelves = Class.forName(module, "net.natevg.fnf.item.FnFCreativeModeTabs");
                Object tabHolder = shelves.getField("FNF_ITEM_TAB").get(null);
                Object tab = tabHolder.getClass().getMethod("get").invoke(tabHolder);
                String tabId = tabHolder.getClass().getMethod("getId").invoke(tabHolder).toString();
                if (!tabs.equals(List.of("fnf:fnf_item_tab")) || !tabId.equals(tabs.getFirst())
                        || BuiltInRegistries.CREATIVE_MODE_TAB.getOptional(net.minecraft.resources.ResourceLocation.parse(tabId)).orElseThrow() != tab)
                    throw new AssertionError("Tab registry/Holder identity");
                Object modBus = owner.getClass().getMethod("getEventBus").invoke(owner);
                var field = NeoForge.EVENT_BUS.getClass().getDeclaredField("listeners");
                field.setAccessible(true);
                var table = (Map<?, ?>)field.get(NeoForge.EVENT_BUS);
                Class<?> entry = Class.forName(module, "net.natevg.fnf.FoodNFuel");
                long sourceObjects = table.keySet().stream().filter(k -> k.getClass() == entry).count();
                if (sourceObjects != 0 || modBus == NeoForge.EVENT_BUS) throw new AssertionError("Global/Mod bus identity");
                result.put("holderIds", holders.keySet());
                result.put("allHolderIdentity", true);
                result.put("tabHolderIdentity", true);
                result.put("nativeGlobalSourceObjects", sourceObjects);
                result.put("modBusIdentity", System.identityHashCode(modBus));
                result.put("globalBusIdentity", System.identityHashCode(NeoForge.EVENT_BUS));
                result.put("sourceModule", entry.getModule().getName());
                result.put("sourceModuleLayer", entry.getModule().getLayer() == getClass().getModule().getLayer());
            } else if (!items.isEmpty() || !tabs.isEmpty()) throw new AssertionError("Unexpected FnF objects in Control");
            result.put("foodConsumption", "NOT RUN");
            result.put("furnaceBurning", "NOT RUN");
            result.put("clientUI", "NOT RUN");
            result.put("tabCallbacks", "NOT RUN");
            result.put("persistenceFunctionality", "NOT RUN");
            Files.writeString(Path.of("fnf-runtime-observation.json"), new GsonBuilder().setPrettyPrinting().create().toJson(result) + "\n");
            System.out.println("[DF4C4-QUERY] COMPLETE present=" + present + " items=" + items.size() + " tabs=" + tabs.size());
        } catch (ReflectiveOperationException | java.io.IOException failure) {
            throw new AssertionError("Read-only observation failed", failure);
        }
    }
}
