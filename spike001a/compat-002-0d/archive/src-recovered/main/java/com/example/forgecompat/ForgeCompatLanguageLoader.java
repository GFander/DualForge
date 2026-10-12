package com.example.forgecompat;

import java.util.ArrayList;
import java.util.List;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.javafmlmod.FMLModContainer;
import net.neoforged.neoforgespi.language.IModInfo;
import net.neoforged.neoforgespi.language.IModLanguageLoader;
import net.neoforged.neoforgespi.language.ModFileScanData;

/**
 * Spike-002-0b Step 2-A v1.1 §0.3 前置 1（IModLanguageLoader observer）.
 * name()="javafml", version()="9999"; loadMod returns a real FMLModContainer and never throws.
 */
public final class ForgeCompatLanguageLoader implements IModLanguageLoader {
    public static final String NAME = "javafml";
    public static final String VERSION = "9999";

    static {
        System.out.println("[FORGECOMPAT-LANG] <clinit> ForgeCompatLanguageLoader");
    }

    public ForgeCompatLanguageLoader() {
        System.out.println("[FORGECOMPAT-LANG] INSTANTIATED ForgeCompatLanguageLoader");
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public String version() {
        return VERSION;
    }

    @Override
    public ModContainer loadMod(IModInfo info, ModFileScanData scanData, ModuleLayer layer) {
        List<String> entrypoints = new ArrayList<>();
        for (ModFileScanData.AnnotationData annotation : scanData.getAnnotations()) {
            if (annotation.annotationType().getClassName().equals("net.minecraftforge.fml.common.Mod")) {
                entrypoints.add(annotation.clazz().getClassName());
            }
        }
        System.out.println("[FORGECOMPAT-LANG] loadMod called for " + info.getModId() + " entrypoints=" + entrypoints);
        return new FMLModContainer(info, entrypoints, scanData, layer);
    }
}
