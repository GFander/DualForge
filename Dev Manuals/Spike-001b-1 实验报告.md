# Spike-001b-1 实验报告

> **结论（先行）**：**四节点全部打开，001b-1 PASS。**
> `findLanguage("javafml", [52,))` 被真实调用，且经日志形态判定**进入了 VersionSupportMatrix 分支**
> （provider 已找到，版本比较失败），日志原文含 `, found 4.0.45`。
> 失败形态：`ModLoadingException` → `Mod loading has failed` → **服务端未能启动**（未达 `Done`）。
>
> 按 Spike_001b-1.md §十一：本 Spike **不进入 001b-2**，报告原样交回架构决策方。

---

## 一、实验环境（精确版本）

```
Minecraft:            1.21.1
NeoForge:             21.1.256
FancyModLoader:       4.0.45
Java:                 21
OS:                   Windows 11 (amd64) version 10.0
启动方式:             CLI（官方 installer --installServer 产物 + java @argfile nogui）
客户端/服务端:        Server（Dedicated Server，launch target = forgeserver）

编译依赖 Forge 版本:  1.21.1-52.1.0
```

Java 取证：`openjdk version "21.0.10" 2026-01-20 LTS` / `Microsoft-13106404 (build 21.0.10+7-LTS)`。
运行标签：`spike001b-1`。
`mods/` 内容：`forgecompat.jar`（本次更新）+ `forgeprobe.jar`（未改动）。

---

## 二、实验目标

**唯一目标**：让 `ModFile.identifyLanguage()` 调用 `LanguageProviderLoader.findLanguage()`。

不要求 `findLanguage()` 成功返回。**即使最后出现 `fml.modloadingissue.language.missingversion`，本 Spike 仍然可以 PASS。**

本 Spike 观察的是四个节点是否被依次打开：

```
getMods()                        → 非空
requiredLanguageLoaders()        → 非空
identifyLanguage()               → 被调用
findLanguage("javafml", [52,))   → 被调用
```

---

## 三、实验输入

### forgecompat.jar（修改）

- 路径：`D:\DualForge\spike001a\compat\build\forgecompat.jar`
- SHA-256: `14efd54fa91e415c5815972b81d16c2895dd9ed52a427154db1261efa532e334`
- 文件大小: `13215` 字节
- class 文件版本: major = 65（Java 21）
- 001a-0 版本留档：`D:\DualForge\spike001a\compat\build\forgecompat-001a0.jar`
  （SHA-256 `68d74a50745d6712888605e4abbbefac2827b91aa7d9b25a967fd0ecae6a8b5a`）

**JAR tree（`jar tf forgecompat.jar` 完整输出）**：

```
META-INF/
META-INF/MANIFEST.MF
META-INF/services/
META-INF/services/net.neoforged.neoforgespi.locating.IModFileCandidateLocator
META-INF/services/net.neoforged.neoforgespi.locating.IModFileReader
com/
com/example/
com/example/forgecompat/
com/example/forgecompat/ForgeJarMetadata.class
com/example/forgecompat/ForgeModFileInfo$ForgeModInfo.class
com/example/forgecompat/ForgeModFileInfo$MapConfigurable.class
com/example/forgecompat/ForgeModFileInfo.class
com/example/forgecompat/ForgeModFileInfoParser.class
com/example/forgecompat/ForgeModLocator.class
com/example/forgecompat/ForgeModReader.class
```

**与 001a-0 版本的 diff（逐条目 SHA-256 比对）**：

| JAR 条目 | 001a-0 | 001b-1 | 判定 |
|---|---|---|---|
| `META-INF/MANIFEST.MF` | `7b8130925631a919…` | `7b8130925631a919…` | **相同** |
| `META-INF/services/…IModFileReader` | `394a0017ea04610a…` | `394a0017ea04610a…` | **相同** |
| `META-INF/services/…IModFileCandidateLocator` | `2e232a7c8c1ed34a…` | `2e232a7c8c1ed34a…` | **相同** |
| `ForgeModLocator.class` | `09766038b2a78900…` | `09766038b2a78900…` | **相同** |
| `ForgeModReader.class` | `741a3d90b9aaacf5…` | `741a3d90b9aaacf5…` | **相同** |
| `ForgeJarMetadata.class` | `d261617e8d0694d2…` | `d261617e8d0694d2…` | **相同** |
| `ForgeModFileInfoParser.class` | `0626764eae0b7b3b…` | `0626764eae0b7b3b…` | **相同** |
| `ForgeModFileInfo.class` | `9768811e628bf62e…` | `1cf3ec65decfa409…` | **变更** |
| `ForgeModFileInfo$MapConfigurable.class` | `e6c5293dfd539291…` | `09e12625ad844f9d…` | 变更（源码未改；因外层类新增 nest member 而重编译） |
| `ForgeModFileInfo$ForgeModInfo.class` | 不存在 | `07018cf4126664ef…` | **新增**（`ForgeModFileInfo.java` 内的嵌套类，非新增源文件） |

**结论**：`META-INF/services/` 与 `MANIFEST.MF` **逐字节未变**；未新增任何 `IModLanguageLoader` 声明；
未新增源文件。实际变更的**源文件只有 `ForgeModFileInfo.java` 一个**。

> **与说明书的偏差（如实记录）**：Spike_001b-1.md §5.2 允许的变更点之一是
> "`ForgeModFileInfoParser` 的 `[[mods]]` 解析逻辑"，但 001a-0 的解析器**已经**把
> `[[mods]]` 首项解析为 `{modId, version, displayName}`（本次日志 `parsed keys=[modLoader, loaderVersion, license, mods, dependencies]` 亦可证），
> 因此**无需改动**，其 `.class` 哈希保持不变。变更集合是允许集合的**子集**。

### forgeprobe.jar（未改动）

- SHA-256: `a93a823c988bfd6a8f6ea132d189d9de732b0e5951f4bf74980d51bb63374e22`
- 文件大小: `1772` 字节
- **与 Spike-001a-0 逐字节相同**（哈希一致）

### ForgeModFileInfoParser.java

实际参与编译的源码（`D:\DualForge\spike001a\compat\src\main\java\com\example\forgecompat\ForgeModFileInfoParser.java`），**本次未改动**：

```java
package com.example.forgecompat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
        return new ForgeModFileInfo(file, license, parsed);
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
```

### ForgeModFileInfo.java

实际参与编译的源码（`D:\DualForge\spike001a\compat\src\main\java\com\example\forgecompat\ForgeModFileInfo.java`），**本次唯一变更的源文件**：

```java
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
```

---

## 四、四节点打开证据

### 节点 1：`getMods()` 非空

- **entered: YES**
- evidence:
  ```
  [FORGECOMPAT] ForgeModFileInfo.buildMods: modId=forgeprobe version=1.0.0 displayName=Forge Probe
  [FORGECOMPAT] ForgeModFileInfo built: mods=1 languageSpecs=[LanguageSpec[languageName=javafml, acceptedVersions=[52,)]]
  [FORGECOMPAT] CALLED ForgeModFileInfo.getMods -> 1 entry(ies)
  ```
  下游可见证据（`latest.log`，发现阶段的 Mod List **已包含 forgeprobe**）：
  ```
  [main/INFO] [net.neoforged.fml.loading.moddiscovery.ModDiscoverer/]:
       Mod List:
  		Name Version (Mod Id)

  		Forge Probe 1.0.0 (forgeprobe)
  		Minecraft 1.21.1 (minecraft)
  		NeoForge 21.1.256 (neoforge)
  ```
  以及 `ModValidator/SCAN: Found 4 mod files with 3 mods`（对比 001a-0 为 `2 mods`）。
- 若 NO，错误: 不适用

### 节点 2：`requiredLanguageLoaders()` 非空

- **entered: YES**
- evidence:
  ```
  [FORGECOMPAT] ForgeModFileInfo.buildLanguageSpecs: LanguageSpec[languageName=javafml, acceptedVersions=[52,)]
  [FORGECOMPAT] CALLED ForgeModFileInfo.requiredLanguageLoaders -> [LanguageSpec[languageName=javafml, acceptedVersions=[52,)]]
  ```
- `languageSpecs` 列表内容: `[LanguageSpec[languageName=javafml, acceptedVersions=[52,)]]`（长度 1）
- 该列表被 NeoForge 以 DEBUG 打印，可交叉验证（`debug.log`）：
  ```
  [main/DEBUG] [net.neoforged.fml.loading.moddiscovery.ModFile/LOADING]: Loading mod file D:\DualForge\spike000\neoforge-server\mods\forgeprobe.jar with languages [LanguageSpec[languageName=javafml, acceptedVersions=[52,)]]
  ```

### 节点 3：`identifyLanguage()` 被调用

- **entered: YES**
- evidence:
  - `ModFile.identifyLanguage()` 内部即以 `modFileInfo.requiredLanguageLoaders().stream().map(...)` 调用
    `FMLLoader.getLanguageLoadingProvider().findLanguage(...)`（001b-0 字节码已确认这是唯一实施方式）。
  - 本节点的**直接证据**是节点 4 的日志：只有 `identifyLanguage()` 才会走到 `findLanguage`，
    而 `findLanguage` 的两条 `LOGGER.error` 只可能从该处触发。
  - 时序证据（`debug.log` 行号）：
    ```
    146: [10:27:59.323] ModValidator/SCAN: Found 4 mod files with 3 mods
    147: [10:27:59.323] TransformationServiceDecorator/MODLAUNCHER: End scan trigger - transformation service fml
    148: [10:27:59.331] ServiceLoaderUtil/CORE: Found 3 implementations of IModLanguageLoader:
    152: [10:27:59.332] LanguageProviderLoader/CORE: Found language provider javafml, version 4.0.45
    155: [10:27:59.333] LanguageProviderLoader/LOADING: Missing language javafml version [52,) wanted by forgeprobe.jar, found 4.0.45
    ```
    即：`findLanguage` 发生在 `LanguageProviderLoader` 构造完成**之后**，符合
    `FMLLoader.completeScan`（第一步构造 loader）→ `ModValidator.stage2Validation()`（第一步
    `validateLanguages()` → `identifyLanguage()`）的调用顺序。

### 节点 4：`findLanguage("javafml", [52,))` 被调用

- **entered: YES**
- evidence（`latest.log` + `stdout`，ERROR 级，原文）：
  ```
  [0710月2026 10:27:59.333] [main/ERROR] [net.neoforged.fml.loading.LanguageProviderLoader/LOADING]: Missing language javafml version [52,) wanted by forgeprobe.jar, found 4.0.45
  ```
- **执行分支判定（关键）**：
  - [ ] 进入 `map.get("javafml") == null` 分支（provider 未找到）
  - [x] **进入 `VersionSupportMatrix` 分支（provider 找到，版本校验）**
  - [ ] 进入其它分支（需描述）

  **判定依据（日志形态，非猜测）**：`findLanguage` 的两条错误分支文案不同：

  ```
  分支① provider 未找到      : "Missing language {} version {} wanted by {}"                       ← 3 个占位符，无 "found"
  分支② VersionSupportMatrix : "Missing language {} version {} wanted by {}, found {}"             ← 4 个占位符，含 "found"
  ```

  实际日志为 `... wanted by forgeprobe.jar, **found 4.0.45**` → 命中**分支②**。
  这与分支② 的构造 `ModLoadingIssue.error(..., languageName, range, wrapper.version())` 一致
  （第三参数为实际 loader 版本，而非分支① 的字面量 `"-"`）。

- **provider 已被找到的独立证据**（`debug.log`，`LanguageProviderLoader/CORE`）：
  ```
  [10:27:59.331] [main/DEBUG] [net.neoforged.fml.util.ServiceLoaderUtil/CORE]: Found 3 implementations of IModLanguageLoader:
  [10:27:59.332] [main/DEBUG] [net.neoforged.fml.loading.LanguageProviderLoader/CORE]: Found language provider javafml, version 4.0.45
  [10:27:59.332] [main/DEBUG] [net.neoforged.fml.loading.LanguageProviderLoader/CORE]: Found language provider lowcodefml, version 4.0.45
  [10:27:59.332] [main/DEBUG] [net.neoforged.fml.loading.LanguageProviderLoader/CORE]: Found language provider minecraft, version 4.0.45
  ```
  即 `languageProviderMap` 中**存在**键 `"javafml"`，映射到版本 `4.0.45` 的 provider。

- 返回结果 / 抛出的异常：**未返回**。`findLanguage` 在分支② 内抛出
  `net.neoforged.fml.ModLoadingException`，携带 issue
  `fml.modloadingissue.language.missingversion`。
- 若抛异常，完整 stack trace: 见 §九。

---

## 五、版本校验结果

```
loader name         = javafml
loader version      = 4.0.45
accepted range      = [52,)
comparison result   = FAIL
validation location = LanguageProviderLoader.findLanguage
                      (调用方：ModFile.identifyLanguage
                        ← ModValidator.validateLanguages
                        ← ModValidator.stage2Validation
                        ← FMLLoader.completeScan)
issue key（若有）   = fml.modloadingissue.language.missingversion
```

补充字段（均由证据直接支撑）：

```
loader implementation class = net.neoforged.fml.javafmlmod.FMLJavaModLanguageProvider
                              （001b-0 字节码 + 本次 debug.log "Found language provider javafml"）
provider 来源 JAR           = loader-4.0.45.jar
issue 渲染文本              = "Mod File ...\forgeprobe.jar needs language provider javafml:52 or above to load
                               We have found 4.0.45"
```

---

## 六、Failure Chain（更新）

> 以实际调用证据填写。

```
Discovery:       YES    （locator 与 reader 均处理了 forgeprobe.jar）
Metadata:        YES    （ForgeModFileInfoParser.build 执行，parsed keys 完整）
Validation:      YES    （stage1Validation + stage2Validation 均执行；
                         validateLanguages 捕获了 missingversion）
Scan:            UNKNOWN（本轮未取得针对 forgeprobe.jar 的 ModFileScanData 直接证据）
LanguageLoader:  YES    （findLanguage 被调用，进入 VersionSupportMatrix 分支后抛异常）
Module:          NO     （未进入；forgeprobe 的 ModFile 在 validateLanguages 中被移出候选集）
Container:       NO
Constructor:     NO     （FORGE PROBE CONSTRUCTOR REACHED 零命中）
```

---

## 七、First Confirmed Failure（若有）

**阶段**: `LanguageLoader` —— 具体为 `LanguageProviderLoader.findLanguage` 的
**VersionSupportMatrix 版本校验分支**。

**证据**:
1. `[main/ERROR] [LanguageProviderLoader/LOADING]: Missing language javafml version [52,) wanted by forgeprobe.jar, found 4.0.45`
2. `[main/DEBUG] [LanguageProviderLoader/CORE]: Found language provider javafml, version 4.0.45`
   （证明 provider **已找到**，排除分支①）
3. 崩溃报告 `-- Mod loading issue --` 段：
   `Failure message: Mod File ...\forgeprobe.jar needs language provider javafml:52 or above to load / We have found 4.0.45`

**异常**: `net.neoforged.fml.ModLoadingException`（issue：`fml.modloadingissue.language.missingversion`），
**严重度 ERROR**；最终以 `net.neoforged.neoforge.logging.CrashReportExtender$ModLoadingCrashException: Mod loading has failed`
终止启动。

**调用链**:

```
FMLLoader.beginModScan
  └─ ModDiscoverer.discoverMods
       └─ ModValidator.stage1Validation
            └─ validateFiles → ModFile.identifyMods
                 └─ 打印 "Loading mod file ... with languages [LanguageSpec[javafml, [52,)]]"
                    （此处仅**读取** requiredLanguageLoaders 用于日志，不做解析）
FMLLoader.completeScan
  ├─ new LanguageProviderLoader(ctx)            ← 载入 javafml@4.0.45
  └─ ModValidator.stage2Validation
       └─ validateLanguages
            └─ ModFile.identifyLanguage
                 └─ FMLLoader.getLanguageLoadingProvider()
                      .findLanguage(this, "javafml", VersionRange("[52,)"))
                        ├─ languageProviderMap.get("javafml") != null      → 不走分支①
                        └─ VersionSupportMatrix.testVersionSupportMatrix(...) == false
                             → LOGGER.error("... found 4.0.45")              ← **首失败点**
                             → throw ModLoadingException(missingversion)
       └─ catch (ModLoadingException e) { issues.addAll(e.getIssues()); it.remove(); }
FMLLoader.completeScan → loadingModList.hasErrors() == true → 跳过 mixinConfigs
ModLoader.gatherAndInitializeMods → "Error during pre-loading phase" → ModLoadingCrashException
```

**原因分类（若为 findLanguage 异常）**：
  - [ ] provider 未找到
  - [x] **VersionSupportMatrix 校验失败**
  - [ ] ServiceLoader / provider 实例化失败
  - [ ] 其它

---

## 八、SPI 调用证据

**IModFileReader**:

- 沿用 001a-0 状态。本次 `debug.log` 仍为 `Found 3 implementations of IModFileReader`，
  `100 - com.example.forgecompat.ForgeModReader from forgecompat.jar` 仍列首位。
- `ForgeModReader.read` 对本轮每个 JAR 均被调用（含 NeoForge 自带 JAR 与 4 个 jar-in-jar 内容），
  仅 `mods/forgeprobe.jar` 返回非 null。

**IModLanguageLoader**:

- 本次实验是否触发了 `findLanguage`？**是**（见 §四 节点 4）。
- 触发了哪个 provider？`javafml` → `net.neoforged.fml.javafmlmod.FMLJavaModLanguageProvider`，
  `version() = 4.0.45`，来自 `loader-4.0.45.jar`。

  > 注意：**`findLanguage` 的返回值未产生** —— 版本校验失败先于返回发生。
  > 因此本次**没有**观测到 `IModInfo.getLoader()` 的实际返回（该对象在 001b-1 中始终为 `null`）。

- provider Layer 是否可观测？**仅部分可观测**。
  `ServiceLoaderUtil/CORE: Found 3 implementations of IModLanguageLoader` 这一行由
  `LaunchContext.loadServices` 产出，但日志**不打印命中层名**。
  因此本次**仍未**确定 `IModLanguageLoader` 的 provider 具体来自哪一层（维持 001b-0 的 [H]）。

---

## 九、完整异常

### 9.1 直接抛出点（`findLanguage` 内）**没有可用的 stack trace**

这是本 Spike 必须记录的一条负面事实：

- `findLanguage` 抛出的是 `ModLoadingException`（由 `ModLoadingIssue.error(...).withAffectedModFile(file)` 构造），
  **未附加任何 cause**。
- 该异常在 `ModValidator.validateLanguages()` 中被 `catch (ModLoadingException e)` 捕获，
  仅执行 `issues.addAll(e.getIssues())`，**异常对象本身被丢弃**，因此其 stack trace **不被保留**。
- 后续的 `ModLoadingException: Loading errors encountered` 是
  `ModLoader.gatherAndInitializeMods` 依据累积 issues **重新构造**的新异常，其栈从
  `ModLoader.java:110` 开始，**不包含** `findLanguage` 的原始帧。
- 崩溃报告亦确认：`Exception message: <No associated exception found>`。

> 因此本 Spike 能提供的"完整异常链"只有：**日志行 + issue 渲染文本 + 重新构造后的栈**。
> 这不是输出截断，而是 NeoForge 21.1.256 自身的处理结果（[F]）。

### 9.2 完整异常（`latest.log` 原文，未截断）

```
[0710月2026 10:28:06.212] [main/FATAL] [net.neoforged.fml.ModLoader/CORE]: Error during pre-loading phase: Mod File D:\DualForge\spike000\neoforge-server\mods\forgeprobe.jar needs language provider javafml:52 or above to load
We have found 4.0.45
[0710月2026 10:28:06.337] [main/ERROR] [net.minecraft.CrashReport/]: Negative index in crash report handler (0/18)
[0710月2026 10:28:06.350] [main/FATAL] [net.neoforged.neoforge.server.loading.ServerModLoader/]: Crash report saved to .\crash-reports\crash-2026-10-07_10.28.06-fml.txt
[0710月2026 10:28:06.351] [main/ERROR] [net.minecraft.server.Main/FATAL]: Failed to start the minecraft server
net.neoforged.fml.ModLoadingException: Loading errors encountered:
	- Mod File D:\DualForge\spike000\neoforge-server\mods\forgeprobe.jar needs language provider javafml:52 or above to load
	  We have found 4.0.45

	at MC-BOOTSTRAP/fml_loader@4.0.45/net.neoforged.fml.ModLoader.gatherAndInitializeMods(ModLoader.java:110) ~[loader-4.0.45.jar%2359!/:4.0]
	at TRANSFORMER/neoforge@21.1.256/net.neoforged.neoforge.internal.CommonModLoader.begin(CommonModLoader.java:49) ~[neoforge-21.1.256-universal.jar%23121!/:?]
	at TRANSFORMER/neoforge@21.1.256/net.neoforged.neoforge.server.loading.ServerModLoader.load(ServerModLoader.java:34) ~[neoforge-21.1.256-universal.jar%23121!/:?]
	at TRANSFORMER/minecraft@1.21.1/net.minecraft.server.Main.main(Main.java:124) ~[server-1.21.1-20240808.144430-srg.jar%23120!/:?]
	at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:103) ~[?:?]
	at java.base/java.lang.reflect.Method.invoke(Method.java:580) ~[?:?]
	at MC-BOOTSTRAP/fml_loader@4.0.45/net.neoforged.fml.loading.targets.CommonLaunchHandler.runTarget(CommonLaunchHandler.java:136) ~[loader-4.0.45.jar%2359!/:4.0]
	at MC-BOOTSTRAP/fml_loader@4.0.45/net.neoforged.fml.loading.targets.CommonLaunchHandler.serverService(CommonLaunchHandler.java:128) ~[loader-4.0.45.jar%2359!/:4.0]
	at MC-BOOTSTRAP/fml_loader@4.0.45/net.neoforged.fml.loading.targets.CommonServerLaunchHandler.runService(CommonServerLaunchHandler.java:32) ~[loader-4.0.45.jar%2359!/:4.0]
	at MC-BOOTSTRAP/fml_loader@4.0.45/net.neoforged.fml.loading.targets.CommonLaunchHandler.lambda$launchService$4(CommonLaunchHandler.java:118) ~[loader-4.0.45.jar%2359!/:4.0]
	at MC-BOOTSTRAP/cpw.mods.modlauncher@11.0.5/cpw.mods.modlauncher.LaunchServiceHandlerDecorator.launch(LaunchServiceHandlerDecorator.java:30) [modlauncher-11.0.5.jar%2364!/:?]
	at MC-BOOTSTRAP/cpw.mods.modlauncher@11.0.5/cpw.mods.modlauncher.LaunchServiceHandler.launch(LaunchServiceHandler.java:53) [modlauncher-11.0.5.jar%2364!/:?]
	at MC-BOOTSTRAP/cpw.mods.modlauncher@11.0.5/cpw.mods.modlauncher.LaunchServiceHandler.launch(LaunchServiceHandler.java:71) [modlauncher-11.0.5.jar%2364!/:?]
	at MC-BOOTSTRAP/cpw.mods.modlauncher@11.0.5/cpw.mods.modlauncher.Launcher.run(Launcher.java:103) [modlauncher-11.0.5.jar%2364!/:?]
	at MC-BOOTSTRAP/cpw.mods.modlauncher@11.0.5/cpw.mods.modlauncher.Launcher.main(Launcher.java:74) [modlauncher-11.0.5.jar%2364!/:?]
	at MC-BOOTSTRAP/cpw.mods.modlauncher@11.0.5/cpw.mods.modlauncher.BootstrapLaunchConsumer.accept(BootstrapLaunchConsumer.java:26) [modlauncher-11.0.5.jar%2364!/:?]
	at MC-BOOTSTRAP/cpw.mods.modlauncher@11.0.5/cpw.mods.modlauncher.BootstrapLaunchConsumer.accept(BootstrapLaunchConsumer.java:23) [modlauncher-11.0.5.jar%2364!/:?]
	at cpw.mods.bootstraplauncher@2.0.2/cpw.mods.bootstraplauncher.BootstrapLauncher.run(BootstrapLauncher.java:210) [bootstraplauncher-2.0.2.jar:?]
	at cpw.mods.bootstraplauncher@2.0.2/cpw.mods.bootstraplauncher.BootstrapLauncher.main(BootstrapLauncher.java:69) [bootstraplauncher-2.0.2.jar:?]
```

**无 `Caused by` / `Suppressed` 段。**

### 9.3 崩溃报告（`crash-reports/crash-2026-10-07_10.28.06-fml.txt`，issue 段原文）

```
Description: Mod loading failures have occurred; consult the issue messages for more details

net.neoforged.neoforge.logging.CrashReportExtender$ModLoadingCrashException: Mod loading has failed

-- Mod loading issue --
Details:
	Mod file: <No mod information provided>
	Failure message: Mod File D:\DualForge\spike000\neoforge-server\mods\forgeprobe.jar needs language provider javafml:52 or above to load
		We have found 4.0.45
	Mod version: <No mod information provided>
	Mod issues URL: <No issues URL found>
	Exception message: <No associated exception found>
```

同文件 `FML Language Providers:` 段原文：

```
	FML Language Providers: 
		javafml@4.0
		lowcodefml@4.0
		minecraft@4.0
```

---

## 十、原始日志

- latest.log 路径: `D:\DualForge\spike000\logs\spike001b-1\spike001b-1.log`（7095 字节）
- debug.log 路径: `D:\DualForge\spike000\logs\spike001b-1\spike001b-1.debug.log`（65125 字节）
- stdout 路径: `D:\DualForge\spike000\logs\spike001b-1\spike001b-1.stdout.log`（16351 字节）
- stderr 路径: `D:\DualForge\spike000\logs\spike001b-1\spike001b-1.stderr.log`（0 字节）
- crash-report 路径（若存在）: `D:\DualForge\spike000\logs\spike001b-1\crash-reports\crash-2026-10-07_10.28.06-fml.txt`（3590 字节）
- 运行元数据: `D:\DualForge\spike000\logs\spike001b-1\meta.json`
  （`status = EXITED_EARLY`，`exitCode = 0`，`durationSeconds = 12.1`，`doneLine = null`）
- jar 元数据: `D:\DualForge\spike001a\compat\build\jar-metadata-001b1.txt`
- 运行脚本: `D:\DualForge\spike000\run-server.ps1`（`-Label spike001b-1`）

> **本次运行未达到 `Done`**（对比 Spike-000 / 001a-0 均达到 `Done` 并优雅关闭）。
> 服务端在 `ModLoader.gatherAndInitializeMods` 阶段终止，进程退出码仍为 `0`。

---

## 十一、事实标签

### [F] Source Fact

1. `[F]` `ModValidator.stage2Validation()` 的**第一条语句**即 `validateLanguages()`；后者遍历
   `candidateMods` 调用 `ModFile.identifyLanguage()`，并用
   `catch (ModLoadingException e) { issues.addAll(e.getIssues()); it.remove(); }` 与
   `catch (RuntimeException e) { issues.add(error("fml.modloadingissue.technical_error")…); it.remove(); }` 兜底。
2. `[F]` `FMLLoader.completeScan` 第一条语句是 `new LanguageProviderLoader(ctx)`，
   **之后**才调用 `modValidator.stage2Validation()`。因此 `findLanguage` 必然在语言 provider 载入之后发生。
3. `[F]` `ModFile.identifyMods()`（`ModValidator.validateFiles` → stage1）打印
   `"Loading mod file {} with languages {}"`，该行**只读取** `requiredLanguageLoaders()` 用于日志，
   **不触发** `findLanguage`。这是 001b-0 未区分开的一处细节，本次由时序证据澄清。
4. `[F]` `LanguageProviderLoader.findLanguage` 两条错误文案长度不同：
   分支①（provider 未找到）为 `"Missing language {} version {} wanted by {}"`；
   分支②（`VersionSupportMatrix` 不通过）为 `"Missing language {} version {} wanted by {}, found {}"`。
   **可用作区分两分支的日志判据。**
5. `[F]` 分支② 抛出的 `ModLoadingException` **不带 cause**；`validateLanguages` 只取 issue、
   丢弃异常对象，故 `findLanguage` 的原始栈**不可恢复**。
6. `[F]` `LoadingModList.hasErrors()` 为 true 时，`completeScan` 跳过 `mixinConfigs.forEach(...)`。
7. `[F]` 最终终止由 `ModLoader.gatherAndInitializeMods`（`ModLoader.java:110`）重新抛出
   `ModLoadingException`，并由 `CrashReportExtender$ModLoadingCrashException` 渲染为 FML 崩溃报告。

### [E] Experimental Fact

1. `[E]` **四节点全部打开**（详见 §四），001b-1 达到 PASS 判据。
2. `[E]` `findLanguage` 实际进入**分支②（VersionSupportMatrix）**，日志含 `, found 4.0.45`。
3. `[E]` provider lookup **成功**：`Found language provider javafml, version 4.0.45`。
4. `[E]` 版本比较结果 `FAIL`：`[52,)` 不包含 `4.0.45`。
5. `[E]` 发现阶段的 `Mod List` **首次出现 `Forge Probe 1.0.0 (forgeprobe)`**（3 mods），
   说明非空 `getMods()` 使 forgeprobe 进入 mod 列表。
6. `[E]` 服务端**未能启动**（未达 `Done`），生成 FML 崩溃报告；`stderr` 为空。
7. `[E]` `forgeprobe.jar` 的 SHA-256 与 001a-0 完全一致；`META-INF/services/` 与 `MANIFEST.MF`
   逐条目哈希未变；未注册任何新 provider。
8. `[E]` `ForgeProbe` 构造函数输出（`FORGE PROBE CONSTRUCTOR REACHED`）**零命中**。

### [H] Hypothesis

1. `[H]` `IModLanguageLoader` 的 provider 具体所在层仍未被日志证实（`LaunchContext.loadServices`
   不打印命中层名）。001b-0 §3.4 的"不是 mods/ 新建的 PLUGIN 内容"结论本轮**未被推翻**，也**未被直接观测**。
2. `[H]` 若 `findLanguage` 成功返回，后续是否立即进入 `ModSorter` / Module 阶段，本轮无法判断
   （版本校验在 `validateLanguages` 内、`ModSorter.sort` 之前即失败）。
3. `[H]` 若把 `loaderVersion` 改为 `[3,)`（NeoForge 自带 JAR 的写法），`findLanguage` 应能返回 provider；
   **本轮未做该对照**，属 001b-2 范围。
4. `[H]` `Scan` 阶段对 `forgeprobe.jar` 是否执行过 class scan 仍未被证据覆盖（标 UNKNOWN）。

### [D] Design Decision

1. `[D]` 按 Spike_001b-1.md §十一，"四节点全开 + 明确进入 `VersionSupportMatrix` 且因 `version()`
   不满足 accepted range 而抛 `missingversion`" → **对应 001b-4**。是否进入由架构决策方裁定。
2. `[D]` 手册 §九（诊断系统）可记录本观察：NeoForge 21.1.256 在 `validateLanguages` 中
   **丢弃原始异常栈**，导致 `findLanguage` 的失败点只能用日志文案区分。这直接影响
   "结构化错误报告"的设计输入。
3. `[D]` 手册 §3.1 的 `IModLanguageLoader` 解析链可补一处：`identifyLanguage()` 的调用发生在
   `completeScan → stage2Validation → validateLanguages`，而非 `beginScanning` 阶段。

---

## 十二、结论

### 四节点打开状态

```
getMods()                     PASS
requiredLanguageLoaders()     PASS
identifyLanguage()            PASS
findLanguage()                PASS
```

**四个节点全部 PASS ⇒ 001b-1 PASS。**

### findLanguage 执行分支判定

```
provider lookup 结果   = 成功（languageProviderMap 中存在 "javafml" → 4.0.45）
version validation 结果 = FAIL（[52,) 不包含 4.0.45；VersionSupportMatrix 无 languageloader:* override）
issue key              = fml.modloadingissue.language.missingversion
```

### Confidence: **HIGH**

### Evidence

1. 节点 1/2：兼容层自身 `[FORGECOMPAT]` 标记 + NeoForge 侧
   `with languages [LanguageSpec[languageName=javafml, acceptedVersions=[52,)]]`。
2. 节点 3/4：`LanguageProviderLoader/LOADING` 的 ERROR 行，且**含 `, found 4.0.45`**——
   按字节码，该形态只可能来自分支②。
3. provider 已存在的独立证据：`Found language provider javafml, version 4.0.45`。
4. 结束形态：FML 崩溃报告 + `ModLoadingCrashException: Mod loading has failed`。
5. 输入未变：`forgeprobe.jar` 哈希一致；`services` / `MANIFEST.MF` 哈希一致；未注册新 provider。

### Hypotheses still unverified

1. `IModLanguageLoader` 的 provider 具体层名（§八）。
2. `findLanguage` 成功返回后的下一步行为（001b-2 / 001b-4 范围）。
3. `Scan` 阶段是否已执行。
4. `[52,)` 之外的其它范围写法能否通过（未做对照）。

---

## 十三、禁止事项确认

- [x] 未注册新的 `IModLanguageLoader`（`META-INF/services/` 逐字节未变）
- [x] 未修改 `modLoader` / `loaderVersion`（`mods.toml` 与 `forgeprobe.jar` 均未改动）
- [x] 未修改 `forgeprobe.jar`（SHA-256 `a93a823c…374e22` 与 001a-0 一致）
- [x] 未添加 `neoforge.mods.toml`
- [x] 未修改 NeoForge 核心（`neoforge-server/` 仅 `mods/` 与运行期产物变化）
- [x] 未引入 ASM / Mapping / Mixin / Registry / Event / Isolation
- [x] 未修改 `FMLJavaModLoadingContext` ABI
- [x] 未触碰 `FMLModContainer`
- [x] 未触碰 ForgeProbe 构造函数（构造函数输出零命中）
- [x] 未因 `missingversion` 出现而跳到 001b-4（本报告只做分支判定，未启动后续 Spike）
- [x] 未在报告中提出修复方案

---

## 附录：与 Spike_001b-1.md 的偏差清单（如实记录）

| 项 | 说明书要求 | 实际执行 | 影响 |
|---|---|---|---|
| 变更源文件 | 允许 `ForgeModFileInfoParser` / `ForgeModFileInfo` 三处变更点 | **仅变更 `ForgeModFileInfo.java`**（parser 无需改动，其 `.class` 哈希未变） | 变更集合是允许集合的**子集**，不违反单因子纪律 |
| 新增类文件 | 未规定 | 新增 `ForgeModFileInfo$ForgeModInfo.class`（**嵌套类**，非新增源文件） | 不影响 `services` / `manifest` 约束 |
| 服务端结束形态 | 未规定 | **未达 `Done`**，生成 FML 崩溃报告（001a-0 达 `Done`） | 属预期内的观测结果，不影响四节点判定 |
