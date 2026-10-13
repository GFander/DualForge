package com.example.forgecompat;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

import net.neoforged.neoforgespi.language.IConfigurable;
import net.neoforged.neoforgespi.language.IModFileInfo;
import net.neoforged.neoforgespi.language.IModInfo;
import net.neoforged.neoforgespi.language.IModLanguageLoader;
import net.neoforged.neoforgespi.language.MavenVersionAdapter;
import net.neoforged.neoforgespi.locating.ForgeFeature;
import net.neoforged.neoforgespi.locating.IModFile;

/**
 * Forge JAR 的 IModFileInfo 最小实现。
 *
 * <p>Spike-001a-0 阶段：{@code getMods()} 与 {@code requiredLanguageLoaders()} 均返回空列表，
 * 因此 {@code ModFile.identifyLanguage()} 的 stream 为空、{@code findLanguage} 从未被调用。
 *
 * <p><b>Spike-001b-1 变更（唯一变更点）</b>：
 * <ul>
 *   <li>{@code getMods()} 由 {@code [[mods]]} 首项构造 1 个 {@link ForgeModInfo}（节点 1）</li>
 *   <li>{@code requiredLanguageLoaders()} 由 {@code modLoader} / {@code loaderVersion}
 *       构造 1 个 {@link LanguageSpec}（节点 2）</li>
 * </ul>
 *
 * <p><b>刻意不做的</b>：不注册任何 {@code IModLanguageLoader}；{@link ForgeModInfo#getLoader()}
 * 有意返回 {@code null}。本 Spike 的目标是让 {@code findLanguage} 被调用，不是让它成功。
 *
 * <p>{@code modLoader} / {@code loaderVersion} 的取值原样来自 {@code mods.toml}，
 * 未被改写（单因子纪律）。
 */
public final class ForgeModFileInfo implements IModFileInfo {

    private final IModFile file;
    private final String license;
    private final Map<String, Object> parsedToml;
    private final IConfigurable config;
    private final List<IModInfo> mods;
    private final List<LanguageSpec> languageSpecs;

    public ForgeModFileInfo(IModFile file, String license, Map<String, Object> parsedToml) {
        this.file = file;
        this.license = license;
        this.parsedToml = parsedToml;
        this.config = new MapConfigurable(parsedToml);
        this.mods = buildMods();
        this.languageSpecs = buildLanguageSpecs();
        System.out.println("[FORGECOMPAT] ForgeModFileInfo built: mods=" + mods.size()
                + " languageSpecs=" + languageSpecs);
    }

    /** Spike-001b-1 节点 1：{@code [[mods]]} 首项 -> 非空 IModInfo 列表。 */
    private List<IModInfo> buildMods() {
        Object raw = parsedToml.get("mods");
        if (!(raw instanceof List<?> list) || list.isEmpty()) {
            System.out.println("[FORGECOMPAT] ForgeModFileInfo.buildMods: no [[mods]] entry -> empty");
            return List.of();
        }
        if (!(list.get(0) instanceof Map<?, ?> entry)) {
            System.out.println("[FORGECOMPAT] ForgeModFileInfo.buildMods: first entry is not a table -> empty");
            return List.of();
        }
        String modId = str(entry, "modId", "");
        String version = str(entry, "version", "0.0NONE");
        String displayName = str(entry, "displayName", modId);
        System.out.println("[FORGECOMPAT] ForgeModFileInfo.buildMods: modId=" + modId
                + " version=" + version + " displayName=" + displayName);
        return List.of(new ForgeModInfo(this, modId, displayName, version, config));
    }

    private static String str(Map<?, ?> entry, String key, String fallback) {
        Object value = entry.get(key);
        return value == null ? fallback : String.valueOf(value);
    }

    /** Spike-001b-1 节点 2：{@code modLoader} / {@code loaderVersion} -> 非空 LanguageSpec 列表。 */
    private List<LanguageSpec> buildLanguageSpecs() {
        Object modLoader = parsedToml.get("modLoader");
        Object loaderVersion = parsedToml.get("loaderVersion");
        if (modLoader == null || loaderVersion == null) {
            System.out.println("[FORGECOMPAT] ForgeModFileInfo.buildLanguageSpecs: modLoader=" + modLoader
                    + " loaderVersion=" + loaderVersion + " -> empty");
            return List.of();
        }
        LanguageSpec spec = new LanguageSpec(
                String.valueOf(modLoader),
                MavenVersionAdapter.createFromVersionSpec(String.valueOf(loaderVersion)));
        System.out.println("[FORGECOMPAT] ForgeModFileInfo.buildLanguageSpecs: " + spec);
        return List.of(spec);
    }

    @Override
    public List<IModInfo> getMods() {
        System.out.println("[FORGECOMPAT] CALLED ForgeModFileInfo.getMods -> " + mods.size() + " entry(ies)");
        return mods;
    }

    @Override
    public List<LanguageSpec> requiredLanguageLoaders() {
        System.out.println("[FORGECOMPAT] CALLED ForgeModFileInfo.requiredLanguageLoaders -> " + languageSpecs);
        return languageSpecs;
    }

    @Override
    public boolean showAsResourcePack() {
        return false;
    }

    @Override
    public boolean showAsDataPack() {
        return false;
    }

    @Override
    public Map<String, Object> getFileProperties() {
        return parsedToml;
    }

    @Override
    public String getLicense() {
        return license;
    }

    @Override
    public String moduleName() {
        return file.getSecureJar().name();
    }

    @Override
    public String versionString() {
        Object version = parsedToml.get("version");
        return version == null ? null : String.valueOf(version);
    }

    @Override
    public List<String> usesServices() {
        return List.of();
    }

    @Override
    public IModFile getFile() {
        return file;
    }

    @Override
    public IConfigurable getConfig() {
        return config;
    }

    @Override
    public String toString() {
        return "ForgeModFileInfo[" + file.getFilePath() + "]";
    }

    /**
     * Spike-001b-1 的最小 IModInfo。
     *
     * <p>{@link #getLoader()} 有意返回 {@code null}：Spike_001b-1.md §3.2 禁止
     * 注册任何 IModLanguageLoader，因此本 Spike 内不存在可返回的 loader 实例。
     */
    private static final class ForgeModInfo implements IModInfo {

        private final IModFileInfo owningFile;
        private final String modId;
        private final String displayName;
        private final ArtifactVersion version;
        private final IConfigurable config;

        ForgeModInfo(IModFileInfo owningFile, String modId, String displayName, String version,
                     IConfigurable config) {
            this.owningFile = owningFile;
            this.modId = modId;
            this.displayName = displayName;
            this.version = new DefaultArtifactVersion(version);
            this.config = config;
        }

        @Override
        public IModFileInfo getOwningFile() {
            return owningFile;
        }

        /** 本 Spike 有意返回 null —— 不注册 IModLanguageLoader。 */
        @Override
        public IModLanguageLoader getLoader() {
            return null;
        }

        @Override
        public String getModId() {
            return modId;
        }

        @Override
        public String getDisplayName() {
            return displayName;
        }

        @Override
        public String getDescription() {
            return "";
        }

        @Override
        public ArtifactVersion getVersion() {
            return version;
        }

        @Override
        public List<? extends ModVersion> getDependencies() {
            return List.of();
        }

        @Override
        public List<? extends ForgeFeature.Bound> getForgeFeatures() {
            return List.of();
        }

        @Override
        public String getNamespace() {
            return modId;
        }

        @Override
        public Map<String, Object> getModProperties() {
            return Map.of();
        }

        @Override
        public Optional<URL> getUpdateURL() {
            return Optional.empty();
        }

        @Override
        public Optional<URL> getModURL() {
            return Optional.empty();
        }

        @Override
        public Optional<String> getLogoFile() {
            return Optional.empty();
        }

        @Override
        public boolean getLogoBlur() {
            return true;
        }

        @Override
        public IConfigurable getConfig() {
            return config;
        }

        @Override
        public String toString() {
            return "ForgeModInfo[" + modId + " " + version + "]";
        }
    }

    /** IConfigurable backed by the parsed mods.toml map. */
    private static final class MapConfigurable implements IConfigurable {

        private final Map<String, Object> data;

        MapConfigurable(Map<String, Object> data) {
            this.data = data;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> Optional<T> getConfigElement(String... path) {
            Object current = data;
            for (String key : path) {
                if (!(current instanceof Map)) {
                    return Optional.empty();
                }
                current = ((Map<String, Object>) current).get(key);
                if (current == null) {
                    return Optional.empty();
                }
            }
            return Optional.of((T) current);
        }

        @Override
        @SuppressWarnings("unchecked")
        public List<? extends IConfigurable> getConfigList(String... path) {
            Object current = getConfigElement(path).orElse(null);
            if (!(current instanceof List<?> list)) {
                return List.of();
            }
            return list.stream()
                    .filter(Map.class::isInstance)
                    .map(entry -> new MapConfigurable((Map<String, Object>) entry))
                    .toList();
        }
    }
}
