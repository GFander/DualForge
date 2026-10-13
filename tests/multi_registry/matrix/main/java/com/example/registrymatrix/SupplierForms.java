package com.example.registrymatrix;

import java.io.Serializable;
import java.util.function.Supplier;
import net.minecraft.world.item.Item;

/** Compile-only BootstrapMethods evidence; it is never installed as a mod entry. */
public final class SupplierForms {
    private SupplierForms() {}

    public static Supplier<Item> plainLambda() {
        return () -> new Item(new Item.Properties());
    }

    public static Supplier<Item> staticReference() {
        return SupplierForms::makeItem;
    }

    public static Supplier<Item> capturedLambda(String marker) {
        return () -> {
            System.out.println(marker);
            return new Item(new Item.Properties());
        };
    }

    public static Supplier<Item> serializableLambda() {
        return (Supplier<Item> & Serializable) () -> new Item(new Item.Properties());
    }

    private static Item makeItem() { return new Item(new Item.Properties()); }
}
