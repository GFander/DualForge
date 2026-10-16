package org.example.combinedbus;
import net.minecraft.world.item.*;
import net.minecraftforge.registries.RegistryObject;
public final class Consumer {
 private Consumer(){}
 public static Item first(){return Items.FIRST.get();}
 public static RegistryObject<Item> receipt(){return Items.SECOND;}
 public static CreativeModeTab tab(){return Shelves.SHELF.get();}
 public static RegistryObject<CreativeModeTab> tabReceipt(){return Shelves.SHELF;}
}
