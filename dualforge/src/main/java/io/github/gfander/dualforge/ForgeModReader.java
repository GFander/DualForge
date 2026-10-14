package io.github.gfander.dualforge;

import java.util.Optional;

import cpw.mods.jarhandling.JarContents;
import cpw.mods.jarhandling.SecureJar;
import net.neoforged.neoforgespi.locating.IModFile;
import net.neoforged.neoforgespi.locating.IModFileReader;
import net.neoforged.neoforgespi.locating.ModFileDiscoveryAttributes;

/**
 * Spike-001a 最小实现范围（Spike_001a.md §2.2）：
 *   ForgeModReader：识别 META-INF/mods.toml，调用 IModFile.create(SecureJar, parser)。
 *   不实现：自定义 IModFile 实现。
 *
 * 流程取值依据 v1.1 手册 §3.2：
 *   SecureJar secureJar = SecureJar.from(jarContents, new ForgeJarMetadata(jarContents));
 *   IModFile  modFile   = IModFile.create(secureJar, new ForgeModFileInfoParser());
 */
public class ForgeModReader implements IModFileReader {

    public static final String MODS_TOML = "META-INF/mods.toml";

    static {
        System.out.println("[DUALFORGE] <clinit> ForgeModReader");
    }

    public ForgeModReader() {
        System.out.println("[DUALFORGE] INSTANTIATED ForgeModReader");
    }

    @Override
    public IModFile read(JarContents jar, ModFileDiscoveryAttributes attributes) {
        System.out.println("[DUALFORGE] CALLED ForgeModReader.read for " + jar.getPrimaryPath());
        // Native locators also invoke this Reader; apply the same identity guard there.
        if (!ForgeModLocator.isForgeCandidate(jar.getPrimaryPath())) return null;

        Optional<java.net.URI> toml = jar.findFile(MODS_TOML);
        if (toml.isEmpty()) {
            System.out.println("[DUALFORGE] ForgeModReader: no " + MODS_TOML + " -> returning null");
            return null;
        }

        System.out.println("[DUALFORGE] ForgeModReader: found " + MODS_TOML + " -> building SecureJar + IModFile");
        ForgeJarMetadata metadata = new ForgeJarMetadata(jar);
        SecureJar secureJar = SecureJar.from(jar, metadata);
        IModFile modFile = IModFile.create(secureJar, new ForgeModFileInfoParser());
        metadata.bind(modFile);
        System.out.println("[DUALFORGE] ForgeModReader: returning IModFile " + modFile);
        return modFile;
    }

    @Override
    public int getPriority() {
        return 100;
    }

    @Override
    public String toString() {
        return "dualforge reader (META-INF/mods.toml)";
    }
}
