package com.example.forgecompat;

import cpw.mods.modlauncher.api.IEnvironment;
import cpw.mods.modlauncher.api.ITransformationService;
import cpw.mods.modlauncher.api.ITransformer;

import java.util.List;
import java.util.Set;

/**
 * Spike-002-0b Step 2-A: minimal ITransformationService (channel A).
 * name() is unique; the four ITransformationService default methods are NOT overridden.
 */
public final class ForgeCompatTransformationService implements ITransformationService {
    public static final String NAME = "forgecompat-transform";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void initialize(IEnvironment environment) {
        System.out.println("[FORGECOMPAT-XFORM] initialize() called, name=" + NAME);
    }

    @Override
    public void onLoad(IEnvironment environment, Set<String> otherServices) {
        System.out.println("[FORGECOMPAT-XFORM] onLoad() called, otherServices=" + otherServices);
    }

    @Override
    public List<? extends ITransformer<?>> transformers() {
        System.out.println("[FORGECOMPAT-XFORM] transformers() called");
        return List.of(new ForgeProbeConstructorTransformer());
    }
}
