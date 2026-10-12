package com.example.forgecompat;

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
        System.out.println("[FORGECOMPAT] <clinit> ForgeModReader");
    }

    public ForgeModReader() {
        System.out.println("[FORGECOMPAT] INSTANTIATED ForgeModReader");
    }

    @Override
    public IModFile read(JarContents jar, ModFileDiscoveryAttributes attributes) {
        System.out.println("[FORGECOMPAT] CALLED ForgeModReader.read for " + jar.getPrimaryPath());

        Optional<java.net.URI> toml = jar.findFile(MODS_TOML);
        if (toml.isEmpty()) {
            System.out.println("[FORGECOMPAT] ForgeModReader: no " + MODS_TOML + " -> returning null");
            return null;
        }

        System.out.println("[FORGECOMPAT] ForgeModReader: found " + MODS_TOML + " -> building SecureJar + IModFile");
        SecureJar secureJar = SecureJar.from(jar, new ForgeJarMetadata(jar));
        IModFile modFile = IModFile.create(secureJar, new ForgeModFileInfoParser());
        System.out.println("[FORGECOMPAT] ForgeModReader: returning IModFile " + modFile);
        return modFile;
    }

    @Override
    public int getPriority() {
        return 100;
    }

    @Override
    public String toString() {
        return "forgecompat reader (META-INF/mods.toml)";
    }
}
