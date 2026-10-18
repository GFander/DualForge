package org.example.jeibaseline;

import java.util.*;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartedEvent;

/** Separate diagnostic mod: reads real FML ownership and scan data after startup. */
@Mod("jeibaselineobserver")
public final class NativeBaselineObserver {
    public NativeBaselineObserver() {
        MinecraftForge.EVENT_BUS.addListener(this::started);
    }

    private void started(ServerStartedEvent event) {
        try {
            var owner = ModList.get().getModContainerById("jei").orElseThrow();
            Object entry = owner.getMod();
            if (!entry.getClass().getName().equals("mezz.jei.forge.JustEnoughItems"))
                throw new AssertionError("Wrong actual JEI instance");
            var plugins = owner.getModInfo().getOwningFile().getFile().getScanResult().getAnnotations().stream()
                .filter(a -> a.annotationType().getClassName().equals("mezz.jei.api.JeiPlugin"))
                .map(a -> a.clazz().getClassName()).sorted().toList();
            if (plugins.size() != 5) throw new AssertionError("Incomplete FML plugin scan");
            Class<?> service = Class.forName("mezz.jei.common.platform.IPlatformHelper", false,
                entry.getClass().getClassLoader());
            var providers = ServiceLoader.load(service, entry.getClass().getClassLoader()).stream()
                .map(p -> p.type().getName()).sorted().toList();
            if (!providers.equals(List.of("mezz.jei.forge.platform.PlatformHelper")))
                throw new AssertionError("Wrong service provider discovery " + providers);
            int recipes = event.getServer().getRecipeManager().getRecipes().size();
            if (recipes <= 0) throw new AssertionError("No host recipe data");
            System.out.println("JEI_NATIVE_OBSERVER_PASS entry=" + entry.getClass().getName()
                + " module=" + entry.getClass().getModule().getName()
                + " plugins=" + plugins + " providers=" + providers + " hostRecipes=" + recipes
                + "; JEI_CLIENT_PLUGIN_REGISTRATION=NOT_RUN; JEI_RUNTIME=NOT_RUN");
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("Native JEI observer failed", failure);
        }
    }
}
