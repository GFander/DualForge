package com.example.forgecompat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.neoforged.fml.loading.moddiscovery.ModFile;
import net.neoforged.fml.loading.moddiscovery.ModFileInfo;
import net.neoforged.neoforgespi.language.IModFileInfo;
import net.neoforged.neoforgespi.locating.IModFile;
import net.neoforged.neoforgespi.locating.InvalidModFileException;
import net.neoforged.neoforgespi.locating.ModFileInfoParser;

/**
 * Spike-001a 最小实现范围（Spike_001a.md §2.2）：
 *   ForgeModFileInfoParser：解析 mods.toml，返回 IModFileInfo。
 *   不实现：features 声明。
 *
 * 本阶段用的是**最小字段扫描器**（不是完整 TOML 解析器），只提取
 * modLoader / loaderVersion / license / [[mods]] 首项字段 / [[dependencies.*]] 表。
 * 完整 mods.toml 解析属于 001a-1 之后的决策范围。
 */
public class ForgeModFileInfoParser implements ModFileInfoParser {

    static {
        System.out.println("[FORGECOMPAT] <clinit> ForgeModFileInfoParser");
    }

    public ForgeModFileInfoParser() {
        System.out.println("[FORGECOMPAT] INSTANTIATED ForgeModFileInfoParser");
    }

    @Override
    public IModFileInfo build(IModFile file) throws InvalidModFileException {
        System.out.println("[FORGECOMPAT] CALLED ForgeModFileInfoParser.build for " + file.getFilePath());

        Path toml;
        try {
            toml = file.findResource("META-INF", "mods.toml");
        } catch (RuntimeException e) {
            throw new InvalidModFileException("META-INF/mods.toml not found in " + file.getFilePath(), null);
        }

        String text;
        try {
            text = Files.readString(toml);
        } catch (IOException e) {
            throw new InvalidModFileException("Unable to read META-INF/mods.toml: " + e, null);
        }

        Map<String, Object> parsed = parseForgeModsToml(text);
        String license = String.valueOf(parsed.getOrDefault("license", ""));
        System.out.println("[FORGECOMPAT] ForgeModFileInfoParser: parsed keys=" + parsed.keySet());
        ModFile modFile = (ModFile) file;
        return new ModFileInfo(modFile, ForgeModFileInfo.configOf(parsed), info -> { });
    }

    /**
     * Minimal line scanner for the Forge mods.toml subset used by the Spike-001a probe.
     * Handles: top-level scalars, [[mods]], [[dependencies.<ownerModId>]].
     */
    static Map<String, Object> parseForgeModsToml(String text) {
        Map<String, Object> root = new LinkedHashMap<>();
        List<Map<String, Object>> mods = new ArrayList<>();
        List<Map<String, Object>> dependencies = new ArrayList<>();
        Map<String, Object> currentMod = null;
        Map<String, Object> currentDependency = null;
        String section = "";

        for (String rawLine : text.split("\\R")) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            if (line.startsWith("[[") && line.endsWith("]]")) {
                String name = line.substring(2, line.length() - 2).trim();
                if (name.equals("mods")) {
                    currentMod = new LinkedHashMap<>();
                    mods.add(currentMod);
                    section = "mods";
                } else if (name.startsWith("dependencies.")) {
                    currentDependency = new LinkedHashMap<>();
                    currentDependency.put("owner", name.substring("dependencies.".length()));
                    dependencies.add(currentDependency);
                    section = "dependencies";
                } else {
                    section = name;
                }
                continue;
            }
            int eq = line.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String key = line.substring(0, eq).trim();
            String value = unquote(line.substring(eq + 1).trim());
            switch (section) {
                case "mods" -> currentMod.put(key, value);
                case "dependencies" -> currentDependency.put(key, value);
                default -> root.put(key, value);
            }
        }

        if (!mods.isEmpty()) {
            root.put("mods", mods);
        }
        if (!dependencies.isEmpty()) {
            root.put("dependencies", dependencies);
        }
        return root;
    }

    private static String unquote(String value) {
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }
}
