package com.example.forgecompat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import net.neoforged.neoforgespi.ILaunchContext;
import net.neoforged.neoforgespi.locating.IDiscoveryPipeline;
import net.neoforged.neoforgespi.locating.IModFileCandidateLocator;
import net.neoforged.neoforgespi.locating.IncompatibleFileReporting;
import net.neoforged.neoforgespi.locating.ModFileDiscoveryAttributes;

/**
 * Spike-001a 最小实现范围（Spike_001a.md §2.2）：
 *   ForgeModLocator：只认 mods/forgeprobe.jar（不做通用扫描）。
 *
 * 方法体取值依据 v1.1 手册 §4.1，包括 §4.1 的修正：
 *   ModFileDiscoveryAttributes.DEFAULT 不包含 locator，必须 .withLocator(this)。
 */
public class ForgeModLocator implements IModFileCandidateLocator {

    static {
        System.out.println("[FORGECOMPAT] <clinit> ForgeModLocator");
    }

    public ForgeModLocator() {
        System.out.println("[FORGECOMPAT] INSTANTIATED ForgeModLocator");
    }

    @Override
    public void findCandidates(ILaunchContext context, IDiscoveryPipeline pipeline) {
        System.out.println("[FORGECOMPAT] CALLED ForgeModLocator.findCandidates");
        Path probe = Paths.get("mods", "forgeprobe.jar");
        if (!Files.exists(probe)) {
            System.out.println("[FORGECOMPAT] ForgeModLocator: candidate NOT found at " + probe.toAbsolutePath());
            return;
        }
        System.out.println("[FORGECOMPAT] ForgeModLocator: adding candidate " + probe.toAbsolutePath());
        pipeline.addPath(
                probe,
                ModFileDiscoveryAttributes.DEFAULT.withLocator(this),
                IncompatibleFileReporting.WARN_ALWAYS);
    }

    @Override
    public int getPriority() {
        return 100;
    }

    @Override
    public String toString() {
        return "forgecompat locator (mods/forgeprobe.jar only)";
    }
}
