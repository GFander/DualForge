package org.example.combinedbus;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraftforge.registries.*;
import net.minecraftforge.eventbus.api.IEventBus;
public final class Shelves {
 public static final DeferredRegister<CreativeModeTab> DECLARATIONS=DeferredRegister.create(Registries.CREATIVE_MODE_TAB,"catalogbus");
 public static final RegistryObject<CreativeModeTab> SHELF=DECLARATIONS.register("shared",Shelves::make);
 private Shelves(){}
 private static CreativeModeTab make(){System.out.println("[DF4C3] SUPPLIER tab");return CreativeModeTab.builder().title(Component.translatable("tab.catalogbus.shared")).icon(Shelves::icon).displayItems(Shelves::display).build();}
 private static ItemStack icon(){System.out.println("[DF4C3] CALLBACK icon");return new ItemStack(Items.FIRST.get());}
 private static void display(CreativeModeTab.ItemDisplayParameters p,CreativeModeTab.Output output){System.out.println("[DF4C3] CALLBACK display");output.accept(Consumer.first());output.accept(Consumer.receipt().get());}
 public static void attach(IEventBus bus){if(!"false".equals(System.getenv("DF4C3_REGISTER")))DECLARATIONS.register(bus);}
}
