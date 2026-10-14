package com.example.forgecompat.probe;

import java.util.function.Consumer;

import cpw.mods.jarhandling.JarContents;
import cpw.mods.jarhandling.SecureJar;
import net.neoforged.fml.loading.moddiscovery.ModFile;
import net.neoforged.fml.loading.moddiscovery.ModFileInfo;
import net.neoforged.fml.loading.moddiscovery.ModInfo;
import net.neoforged.neoforgespi.language.IConfigurable;
import net.neoforged.neoforgespi.language.IModFileInfo;
import net.neoforged.neoforgespi.language.IModInfo;
import net.neoforged.neoforgespi.locating.ModFileDiscoveryAttributes;
import net.neoforged.neoforgespi.locating.ModFileInfoParser;

/**
 * Spike-001b-5-1 **编译可行性探测**（不参与 forgecompat.jar 打包）。
 *
 * <p>目的：验证第三方兼容层编译单元能否用**正常 Java 路径**构造
 * {@code net.neoforged.fml.loading.moddiscovery.ModInfo}。
 *
 * <p>本文件只做编译验证；不实现任何兼容逻辑。
 */
public final class ModInfoContractProbe {

    private ModInfoContractProbe() {
    }

    /** A1：直接构造（含前置的 ModFile / ModFileInfo 构造链）。 */
    public static ModInfo a1_directConstruction(JarContents jar, ModFileInfoParser parser, IConfigurable config) {
        SecureJar secureJar = SecureJar.from(jar);
        ModFile modFile = new ModFile(secureJar, parser, ModFileDiscoveryAttributes.DEFAULT);
        ModFileInfo modFileInfo = new ModFileInfo(modFile, config, (Consumer<IModFileInfo>) info -> { });
        return new ModInfo(modFileInfo, config);
    }

    /** A1b：ModFileInfo 的第二条 public 构造重载。 */
    public static ModFileInfo a1b_modFileInfoWithLanguageSpecs(JarContents jar, ModFileInfoParser parser,
                                                              IConfigurable config,
                                                              java.util.List<IModFileInfo.LanguageSpec> specs) {
        SecureJar secureJar = SecureJar.from(jar);
        ModFile modFile = new ModFile(secureJar, parser, ModFileDiscoveryAttributes.DEFAULT);
        return new ModFileInfo(modFile, config, (Consumer<IModFileInfo>) info -> { }, specs);
    }

    /** A2：继承 ModInfo 是否可编译。 */
    public static final class ModInfoSubclass extends ModInfo {
        public ModInfoSubclass(ModFileInfo owningFile, IConfigurable config) {
            super(owningFile, config);
        }
    }

    /**
     * Spike-001b-6-1：**完整链**编译探针。
     * {@code ModFile → (ModFileInfo) modFile.getModFileInfo() → getMods() → instanceof ModInfo}
     */
    public static void b6_fullChain(JarContents jar, ModFileInfoParser parser, IConfigurable config,
                                    java.util.List<IModInfo> out) {
        SecureJar secureJar = SecureJar.from(jar);
        ModFile modFile = new ModFile(secureJar, parser, ModFileDiscoveryAttributes.DEFAULT);
        ModFileInfo modFileInfo = (ModFileInfo) modFile.getModFileInfo();
        if (modFileInfo == null) {
            throw new AssertionError("ModFileInfo is null");
        }
        for (IModInfo info : modFileInfo.getMods()) {
            if (!(info instanceof ModInfo)) {
                throw new AssertionError("Not a ModInfo: " + info.getClass());
            }
            out.add(info);
        }
    }
}
