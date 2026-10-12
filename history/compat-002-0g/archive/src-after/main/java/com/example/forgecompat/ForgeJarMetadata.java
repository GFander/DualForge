package com.example.forgecompat;

import java.lang.module.ModuleDescriptor;

import cpw.mods.jarhandling.JarContents;
import cpw.mods.jarhandling.JarMetadata;

/**
 * v1.1 手册 §3.2 的 [H]：
 *   "ModJarMetadata 是 NeoForge 的实现，不可直接假设适用于 Forge 模组；
 *    需要实现 ForgeJarMetadata 或验证其可复用性。"
 *
 * Spike-001a-0 走"验证可复用性"这条路：
 *   cpw.mods.jarhandling.JarMetadata.from(JarContents) 是 securejarhandler 的通用入口
 *   （不是 NeoForge 私有实现），本类先作为它的显式包装，用于观察 ModuleLayer 行为。
 *   本阶段不做任何 Forge 特有的 descriptor 定制。
 */
public final class ForgeJarMetadata implements JarMetadata {

    private final JarMetadata delegate;

    public ForgeJarMetadata(JarContents contents) {
        System.out.println("[FORGECOMPAT] CALLED ForgeJarMetadata.<init> for " + contents.getPrimaryPath());
        this.delegate = JarMetadata.from(contents);
        System.out.println("[FORGECOMPAT] ForgeJarMetadata: delegate name=" + delegate.name()
                + " version=" + delegate.version());
    }

    @Override
    public String name() {
        return delegate.name();
    }

    @Override
    public String version() {
        return delegate.version();
    }

    @Override
    public ModuleDescriptor descriptor() {
        return delegate.descriptor();
    }

    @Override
    public String toString() {
        return "ForgeJarMetadata[" + name() + "]";
    }
}
