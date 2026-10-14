package io.github.gfander.dualforge;

import java.lang.module.ModuleDescriptor;

import cpw.mods.jarhandling.JarContents;
import cpw.mods.jarhandling.JarMetadata;
import net.neoforged.fml.loading.moddiscovery.ModJarMetadata;
import net.neoforged.neoforgespi.locating.IModFile;

/**
 * v1.1 手册 §3.2 的 [H]：
 *   "ModJarMetadata 是 NeoForge 的实现，不可直接假设适用于 Forge 模组；
 *    需要实现 ForgeJarMetadata 或验证其可复用性。"
 *
 * Spike-001a-0 走"验证可复用性"这条路：
 *   cpw.mods.jarhandling.JarMetadata.from(JarContents) 是 securejarhandler 的通用入口
 *   （不是 NeoForge 私有实现），本类先作为它的显式包装，用于观察 ModuleLayer 行为。
 * 007B binds mismatched default JAR names to the host's parsed module identity.
 * Matching historical descriptors stay on the original delegate path.
 */
public final class ForgeJarMetadata implements JarMetadata {

    private final JarMetadata delegate;
    private final JarContents contents;
    private JarMetadata activeMetadata;

    public ForgeJarMetadata(JarContents contents) {
        System.out.println("[DUALFORGE] CALLED ForgeJarMetadata.<init> for " + contents.getPrimaryPath());
        this.contents = contents;
        this.delegate = JarMetadata.from(contents);
        this.activeMetadata = delegate;
        System.out.println("[DUALFORGE] ForgeJarMetadata: delegate name=" + delegate.name()
                + " version=" + delegate.version());
    }

    void bind(IModFile file) {
        String requiredModule = file.getModFileInfo().moduleName();
        if (!delegate.name().equals(requiredModule)) {
            if (!delegate.descriptor().isAutomatic()) {
                throw new IllegalStateException("007B unsupported explicit Forge module identity: declared="
                        + delegate.name() + " metadata=" + requiredModule);
            }
            // FMLModContainer resolves this exact metadata-owned module. Use the official
            // implementation rather than changing the input JAR or constructing a custom layer.
            ModJarMetadata hostMetadata = new ModJarMetadata(contents);
            hostMetadata.setModFile(file);
            activeMetadata = hostMetadata;
            System.out.println("[DUALFORGE] MODULE_IDENTITY original=" + delegate.name()
                    + " metadata=" + requiredModule + " provider=official-ModJarMetadata");
        }
    }

    @Override
    public String name() {
        return activeMetadata.name();
    }

    @Override
    public String version() {
        return activeMetadata.version();
    }

    @Override
    public ModuleDescriptor descriptor() {
        return activeMetadata.descriptor();
    }

    @Override
    public String toString() {
        return "ForgeJarMetadata[" + name() + "]";
    }
}
