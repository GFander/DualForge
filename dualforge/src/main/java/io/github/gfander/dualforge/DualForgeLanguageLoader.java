package io.github.gfander.dualforge;

import java.util.ArrayList;
import java.lang.annotation.ElementType;
import java.util.List;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.javafmlmod.FMLModContainer;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforgespi.language.IModInfo;
import net.neoforged.neoforgespi.language.IModLanguageLoader;
import net.neoforged.neoforgespi.language.ModFileScanData;

/**
 * Spike-002-0b Step 2-A v1.1 §0.3 前置 1（IModLanguageLoader observer）.
 * name()="javafml", version()="9999"; Forge entries use the host FMLModContainer.
 */
public final class DualForgeLanguageLoader implements IModLanguageLoader {
    public static final String NAME = "javafml";
    public static final String VERSION = "9999";

    static {
        System.out.println("[DUALFORGE-LANG] <clinit> DualForgeLanguageLoader");
    }

    public DualForgeLanguageLoader() {
        System.out.println("[DUALFORGE-LANG] INSTANTIATED DualForgeLanguageLoader");
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
        List<String> entrypoints = forgeEntrypoints(info, scanData);
        System.out.println("[DUALFORGE-LANG] loadMod called for " + info.getModId() + " entrypoints=" + entrypoints);
        if (entrypoints.isEmpty()) {
            List<IModLanguageLoader> candidates = FMLLoader.getLanguageLoadingProvider()
                    .applyForEach(l -> l)
                    .filter(l -> l != this)
                    .filter(l -> "javafml".equals(l.name()))
                    .filter(l -> "net.neoforged.fml.javafmlmod.FMLJavaModLanguageProvider".equals(l.getClass().getName()))
                    .toList();
            if (candidates.size() != 1) {
                throw new IllegalStateException("[DUALFORGE-LANG] builtin javafml not unique in languageProviders: candidates=" + candidates.size());
            }
            IModLanguageLoader builtin = candidates.get(0);
            System.out.println("[DUALFORGE-LANG] delegate -> builtin javafml for " + info.getModId());
            return builtin.loadMod(info, scanData, layer);
        }
        return new FMLModContainer(info, entrypoints, scanData, layer);
    }

    static List<String> forgeEntrypoints(IModInfo info, ModFileScanData scanData) {
        // Reuse 003a admission: native metadata must remain on the builtin path.
        if (!ForgeModLocator.isForgeCandidate(info.getOwningFile().getFile().getFilePath())) return List.of();
        List<String> entrypoints = new ArrayList<>();
        for (ModFileScanData.AnnotationData annotation : scanData.getAnnotations()) {
            if (annotation.targetType() == ElementType.TYPE
                    && annotation.annotationType().getClassName().equals("net.minecraftforge.fml.common.Mod")
                    && info.getModId().equals(annotation.annotationData().get("value"))) {
                entrypoints.add(annotation.clazz().getClassName());
            }
        }
        return entrypoints;
    }
}
