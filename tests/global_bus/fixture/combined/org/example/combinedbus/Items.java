package org.example.combinedbus;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.*;
import net.minecraftforge.eventbus.api.IEventBus;
public final class Items {
 public static final DeferredRegister<Item> DECLARATIONS=DeferredRegister.create(ForgeRegistries.ITEMS,"catalogbus");
 public static final RegistryObject<Item> FIRST=DECLARATIONS.register("shared",Items::first);
 public static final RegistryObject<Item> SECOND=DECLARATIONS.register("second",()->{System.out.println("[DF4C3] SUPPLIER second");return new Item(new Item.Properties());});
 private Items(){}
 private static Item first(){System.out.println("[DF4C3] SUPPLIER first");return new Item(new Item.Properties());}
 public static void attach(IEventBus bus){if(!"false".equals(System.getenv("DF4C3_REGISTER")))DECLARATIONS.register(bus);}
}
