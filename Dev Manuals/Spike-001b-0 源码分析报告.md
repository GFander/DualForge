# Spike-001b-0 源码分析报告

> **报告性质**：源码分析报告（**非实验报告**）。本子 Spike **不产生实验数据**、**不做运行验证**、
> **不修改任何代码**。按 Spike_001a.md §六，本报告**不填 Failure Chain**，只记录源码路径。
>
> **唯一问题**：在 exact `loader-4.0.45.jar` 中，从 `mods.toml` 的 `modLoader` 字段到
> `IModLanguageLoader` 实例的完整调用链是什么？

---

## 一、分析对象

| 项 | 值 |
|---|---|
| 主分析工件 | `neoforge-server/libraries/net/neoforged/fancymodloader/loader/4.0.45/loader-4.0.45.jar` |
| 该工件 SHA-256 | `8390398e67df23e72d85ed809685be2752c8ec545b7cbb6ad4b70fdc81e4dcd4` |
| 大小 | `505696` 字节 |
| 辅助工件 | `cpw/mods/modlauncher/11.0.5/modlauncher-11.0.5.jar`（SHA-256 `fd9b9fc7cf043d2264ea0113eac6b258f0a71fece5b00e831bfbdf9726c78a24`） |
| 分析手段 | `javap -p -c -constants`（JDK 21.0.10）反汇编**已发布字节码** |
| 未使用 | 任何 current/main 分支源码；任何其它 FML 版本 |

> 全部结论均标注来源类与方法；未由字节码直接支撑的推断一律进入 [H]。

---

## 二、主调用链（一句话版）

```
IConfigurable.getConfigElement("modLoader")                 → String
IConfigurable.getConfigElement("loaderVersion")             → String
        ↓ MavenVersionAdapter.createFromVersionSpec(String)
   VersionRange
        ↓
IModFileInfo.LanguageSpec(languageName, acceptedVersions)   ← record，两个字段
        ↓ (ModFileInfo 构造器写入 languageSpecs)
ModFileInfo.requiredLanguageLoaders()                       → List<LanguageSpec>
        ↓ (ModFile.identifyLanguage())
FMLLoader.getLanguageLoadingProvider()                      → LanguageProviderLoader
        ↓ LanguageProviderLoader.findLanguage(ModFile, languageName, VersionRange)
   ┌─ map.get(languageName) → ModLanguageWrapper(IModLanguageLoader, ArtifactVersion)
   │      不存在 → ModLoadingException(fml.modloadingissue.language.missingversion)
   └─ VersionSupportMatrix.testVersionSupportMatrix(range, name, "languageloader", ...)
          不通过 → ModLoadingException(fml.modloadingissue.language.missingversion)  ← 版本校验点
          通过   → wrapper.modLanguageProvider()
        ↓
ModFile.loaders : List<IModLanguageLoader>
        ↓
ModInfo.getLoader() = owningFile.getFile().getLoaders().getFirst()
```

---

## 三、六个必答项

### 3.1 `ModFileInfoParser` / `ModFileInfo.getMods()`

| 定位 | 结论 |
|---|---|
| `net.neoforged.fml.loading.moddiscovery.ModFileParser.readModList(IModFile)` | [F] NeoForge 自带解析器：先 `findResource("META-INF","neoforge.mods.toml")`；不存在则 `LOGGER.warn(..., "Mod file {} is missing {} file")`；存在则用 **NightConfig** `FileConfig` 加载 → `NightConfigWrapper` → `new ModFileInfo(modFile, config, ...)` |
| `readModList` 首条日志 | [F] `LOGGER.debug(SCAN, "Considering mod file candidate {}", modFile.getFilePath())` —— 即 Spike-000 / 001a 报告引用的那一行 |
| `ModFileInfo` 构造器 | [F] 只读**文件级**（非 per-mod）两个键：`modLoader`（String；缺失 → `InvalidModFileException("Missing modLoader")`）与 `loaderVersion`（String → `VersionRange`；缺失 → `InvalidModFileException("Missing loaderVersion")`），随后 `languageSpecs = new ArrayList<>(List.of(new LanguageSpec(modLoader, loaderVersionRange)))` —— **一个文件恰好一个 LanguageSpec** |
| `ModFileInfo.requiredLanguageLoaders()` | [F] 直接返回字段 `languageSpecs` |
| `ModFileInfo.getMods()` | [F] 直接返回字段 `mods`（由 `[[mods]]` 列表构建） |
| 其它强约束 | [F] `license` 缺失或 blank → `InvalidModFileException("Missing license")`；`showAsResourcePack` / `showAsDataPack` / `services` / `properties` 均有默认值 |

> **对 Spike-001a 兼容层的直接含义**（[D] 材料，不在本子 Spike 落地）：
> 001a-0 的 `ForgeModFileInfo.requiredLanguageLoaders()` 返回 `List.of()`，
> 因此 `findLanguage` 从未被调用。001b-1 若要让链路继续，必须返回**非空** `LanguageSpec`。

### 3.2 `IModInfo.getLoader()` 的返回类型

```java
// net.neoforged.neoforgespi.language.IModInfo   [F]
IModLanguageLoader getLoader();   // 返回类型 = net.neoforged.neoforgespi.language.IModLanguageLoader
```

```java
// net.neoforged.fml.loading.moddiscovery.ModInfo   [F]
public IModLanguageLoader getLoader() {
    return owningFile.getFile().getLoaders().getFirst();
}
```

| 定位 | 结论 |
|---|---|
| 返回类型 | [F] `net.neoforged.neoforgespi.language.IModLanguageLoader`（**不是** `ModContainer`，也不是 `FMLModContainer`） |
| 取值来源 | [F] `ModInfo.owningFile` → `ModFileInfo.getFile()` → `ModFile.getLoaders()` → **`List.getFirst()`** |
| 含义 | [F] **整个 `ModFile` 共享同一个 loader 实例**；`IModInfo.getLoader()` 取的是文件级唯一 loader，与"该 mod 自己声明了什么"无关 |
| 与 001b-2 纪律的对应 | [F] "loader name" = `IModLanguageLoader.name()`；"loader version" = `IModLanguageLoader.version()`；"implementation class" 需调用方自行 `getClass()`。三者是**三个独立方法**，接口不保证任何一致性 |

### 3.3 `IModLanguageLoader` 的定义与加载方式

```java
// net.neoforged.neoforgespi.language.IModLanguageLoader   [F]（完整方法表）
public interface IModLanguageLoader {
    String name();
    String version();
    ModContainer loadMod(IModInfo info, ModFileScanData data, ModuleLayer layer) throws ModLoadingException;
    default void validate(IModFile file, Collection<ModContainer> modContainers, IIssueReporting reporter) { }
}
```

| 定位 | 结论 |
|---|---|
| 接口方法 | [F] 上表四项；**不含 `getPriority()`** |
| 是否 `IOrderedProvider` | [F] **否**——声明为 `public interface IModLanguageLoader {`，无 `extends`。直接后果见 §3.5 |
| 加载方式 | [F] `LanguageProviderLoader` 构造器 → `ServiceLoaderUtil.loadServices(ctx, IModLanguageLoader.class)` |
| 该调用落到的层 | [F] `ServiceLoaderUtil.loadServices` → `LaunchContext.loadServices` → `ServiceLoader.load(ModuleLayer, Class)`（层选择规则见手册 §3.6） |
| 构造时机 | [F] `FMLLoader.completeScan(ctx, mixinConfigs)` 的**第 0 条语句**：`languageProviderLoader = new LanguageProviderLoader(ctx);`（其后才是 `modValidator.stage2Validation()` 等） |
| 内置实现（[E]，引自 Spike-001a-0 日志） | `FMLJavaModLanguageProvider` → `javafml`；`LowCodeModLanguageProvider` → `lowcodefml`；`MinecraftModLanguageProvider` → `minecraft`；三者均来自 `loader-4.0.45.jar`，`version()` 均为 `4.0.45` |

### 3.4 `IModLanguageLoader` 的实际层级

**结论：不是 mods/ 目录贡献的 PLUGIN 层内容；可归入该时刻已存在的层（SERVICE / BOOT 层族）。**

逐步推理（每步均为 [F]）：

1. `LanguageProviderLoader` 在 `FMLLoader.completeScan` 的**第一句**被构造（§3.3）。
2. `completeScan` 由 ModLauncher 的 `TransformationServiceDecorator.onCompleteScan(...)` /
   `TransformationServicesHandler.triggerScanCompletion(...)` 调用。
3. mods/ 目录里 LIBRARY 型 mod file 进入 PLUGIN 层的**唯一**通道是：
   `ModValidator.getPluginResources()` → `ITransformationService.Resource(Layer.PLUGIN, candidatePlugins)`
   → 由 `FMLLoader.beginModScan` 返回给 ModLauncher。
4. ModLauncher 在 `TransformationServicesHandler.buildTransformingClassLoader(...)` 中消费这些 Resource：
   `buildLayer(Layer.GAME, ...)`，随后 `updateLayer(Layer.PLUGIN, ...)`。
5. `buildTransformingClassLoader` 在调用序列上**晚于** `triggerScanCompletion`（= `completeScan`）：
   `initializeTransformationServices`（含 `runScanningTransformationServices` = `beginScanning`）
   → `triggerScanCompletion` → 之后才是 `buildTransformingClassLoader`。
6. `LaunchContext.loadServices` 逆序遍历 `Layer.values()` 取**最高的已存在层**，并连带搜索其 parent。
7. 因此在 (1) 发生的时刻，mods/ 目录贡献的 PLUGIN 内容**尚不存在**；该次查找只能命中当时已存在的层。

**附加结论（[F]）**：`IModLanguageLoader` **不在** `TransformerDiscovererConstants.SERVICES` 六项白名单内
（白名单 = `ITransformationService` / `IModFileCandidateLocator` / `IModFileReader` /
`IDependencyLocator` / `GraphicsBootstrapper` / `ImmediateWindowProvider`）。
因此"只在 `META-INF/services/` 里声明 `IModLanguageLoader`"的 `mods/` JAR
**不会**被 `ModDirTransformerDiscoverer` 提升（对照手册 §3.6）。

> `[H]` PLUGIN 层是否在此时已被预创建为**空层**，本轮未观测；
> 若是，则"最高已存在层"是 PLUGIN，但 provider 仍来自其 parent（SERVICE / BOOT）——结论不变。

### 3.5 `LanguageProviderLoader`（language provider registry）

```java
// net.neoforged.fml.loading.LanguageProviderLoader   [F]
private final List<IModLanguageLoader> languageProviders;
private final Map<String, ModLanguageWrapper> languageProviderMap;

LanguageProviderLoader(ILaunchContext ctx) {                    // 包级私有
    languageProviderMap = new HashMap<>();
    languageProviders = ServiceLoaderUtil.loadServices(ctx, IModLanguageLoader.class);
    ImmediateWindowHandler.updateProgress("Loading language providers");
    languageProviders.forEach(loader -> {
        String version = loader.version();
        if (version == null || version.isBlank()) {
            LOGGER.error(CORE, "Found unversioned language provider {}", loader.name());
            throw new RuntimeException(loader.name() + " ...");   // 非 ModLoadingIssue
        }
        LOGGER.debug(CORE, "Found language provider {}, version {}", loader.name(), version);
        languageProviderMap.put(loader.name(),
            new ModLanguageWrapper(loader, new DefaultArtifactVersion(version)));
    });
}

public IModLanguageLoader findLanguage(ModFile file, String languageName, VersionRange range) { ... }

private record ModLanguageWrapper(IModLanguageLoader modLanguageProvider, ArtifactVersion version) { }
```

| 定位 | 结论 |
|---|---|
| registry 结构 | [F] `Map<String name, ModLanguageWrapper>` —— **以 loader 的 `name()` 为键** |
| 重名处理 | [F] 使用 `Map.put`，**无重复检测、无报错**；后写入者静默覆盖先写入者 |
| 排序 | [F] `IModLanguageLoader` 不实现 `IOrderedProvider`，因此 `ServiceLoaderUtil.loadServices` 的 **priority 降序排序不适用**（该分支要求 `IOrderedProvider.class.isAssignableFrom(serviceClass)`）。provider 顺序 = `ServiceLoader.stream()` 枚举顺序，**不受 `getPriority()` 控制** |
| 无版本 provider | [F] `version()` 为 null/blank → 直接 `RuntimeException`（不是 ModLoadingIssue） |
| 查找失败 | [F] `map.get(name) == null` → `ModLoadingException(fml.modloadingissue.language.missingversion, name, range, "-")` + `withAffectedModFile(file)` |
| 对外接口 | [F] `forEach(Consumer)`、`applyForEach(Function)` 暴露内部 provider 列表 |

### 3.6 `loaderVersion` 的版本范围校验位置

```java
// net.neoforged.fml.loading.LanguageProviderLoader.findLanguage   [F]
String fileName = file.getFileName();
ModLanguageWrapper wrapper = languageProviderMap.get(languageName);
if (wrapper == null) { /* 见 §3.5 末行 */ }
if (!VersionSupportMatrix.testVersionSupportMatrix(range, languageName, "languageloader",
        (name, r) -> r.containsVersion(wrapper.version()))) {
    LOGGER.error(LOADING, "Missing language {} version {} wanted by {}, found {}",
                 languageName, range, fileName, wrapper.version());
    throw new ModLoadingException(
        ModLoadingIssue.error("fml.modloadingissue.language.missingversion",
                              languageName, range, wrapper.version())
            .withAffectedModFile(file));
}
return wrapper.modLanguageProvider();
```

```java
// net.neoforged.fml.loading.VersionSupportMatrix   [F]
private static final HashMap<String, List<ArtifactVersion>> overrideVersions = new HashMap<>();

static {
    ArtifactVersion mcVersion = new DefaultArtifactVersion(FMLLoader.versionInfo().mcVersion());
    if (MavenVersionAdapter.createFromVersionSpec("[1.21.1]").containsVersion(mcVersion)) {
        add("mod.minecraft", "1.21");
        add("mod.neoforge", "21.0.166");
    }
}

public static boolean testVersionSupportMatrix(VersionRange range, String name, String type,
                                               BiPredicate<String, VersionRange> predicate) {
    if (predicate.test(name, range)) return true;                 // 主判据
    List<ArtifactVersion> overrides = overrideVersions.get(type + ":" + name);
    return overrides != null && overrides.stream().anyMatch(range::containsVersion);
}
```

| 定位 | 结论 |
|---|---|
| 校验位置 | [F] `LanguageProviderLoader.findLanguage(...)`（**不在** `loadMod`，也不在 `ModFileInfo` 构造期） |
| 触发路径 | [F] `ModValidator` → `ModFile.identifyLanguage()` → `lambda$identifyLanguage$10` → `FMLLoader.getLanguageLoadingProvider().findLanguage(modFile, spec.languageName(), spec.acceptedVersions())` |
| 主判据 | [F] `acceptedVersions.containsVersion(new DefaultArtifactVersion(loader.version()))` |
| 覆盖表 | [F] `overrideVersions` **仅两项**：`mod:minecraft` → `1.21`、`mod:neoforge` → `21.0.166`。**不存在任何 `languageloader:*` 项**，即**无绕过路径** |
| 失败形态 | [F] `ModLoadingException`，issue key `fml.modloadingissue.language.missingversion`，`affectedModFile` = 出问题的 ModFile |
| 该 issue 的 en_us 文案 | [F] `"Mod File {101} needs language provider {0}:{1,vr} to load\n§7We have found {2,i18n,fml.messages.artifactversion}"` |
| `VersionRange` 的产生点 | [F] `ModFileInfo` 构造器：`config.getConfigElement("loaderVersion").map(MavenVersionAdapter::createFromVersionSpec).orElseThrow(...)`；`MavenVersionAdapter.createFromVersionSpec` 直接转调 `org.apache.maven.artifact.versioning.VersionRange.createFromVersionSpec`，解析失败 → `RuntimeException("Failed to parse spec")` |

---

## 四、必答流程图（逐格填）

```
mods.toml
    ↓
modLoader = "javafml"
    ↓
loader name = javafml
      [F] 由 IConfigurable 文件级键取出，原样存入 LanguageSpec.languageName；
          在此步骤中它只是一个字符串，无任何校验
    ↓
loaderVersion = "[52,)"
    ↓
VersionRange / validation 发生在哪一步？
      解析：ModFileInfo 构造器（MavenVersionAdapter.createFromVersionSpec）   [F]
      校验：LanguageProviderLoader.findLanguage（VersionSupportMatrix 判据）  [F]
      时机：ModFile.identifyLanguage()（ModValidator 阶段）                  [F]
    ↓
IModLanguageLoader 实例从哪来？
      来源：LanguageProviderLoader 构造器
            → ServiceLoaderUtil.loadServices(ctx, IModLanguageLoader.class)   [F]
      时机：FMLLoader.completeScan 第一条语句                                 [F]
      层：该时刻已存在的层（SERVICE / BOOT 层族），
          不是 mods/ 目录在本次运行中新建的 PLUGIN 内容                       [F]
      version() 取值：BuiltInLanguageLoader.version()
            = JarVersionLookupHandler.getVersion(class)（JAR 版本元数据）      [F]
            实测 4.0.45（FML 工件版本，不是 Forge 的 loaderVersion 语义）      [E]
```

---

## 五、`loaderVersion="[52,)"` 的预期解析结果（**源码推导，非实验**）

| 字段 | 值 | 来源 |
|---|---|---|
| loader name | `javafml` | [F] `FMLJavaModLanguageProvider.name()` 返回字面量 `"javafml"` |
| loader version | `4.0.45` | [F] `BuiltInLanguageLoader.version()`；[E] Spike-001a-0 日志 `Found language provider javafml, version 4.0.45` |
| accepted range | `[52,)` | [F] 由 `mods.toml` 的 `loaderVersion` 经 `VersionRange.createFromVersionSpec` 解析 |
| comparison result | **FAIL**（预期） | [F] 4.0.45 < 52 → `containsVersion` 为 false；`overrideVersions` 无 `languageloader:javafml` 项 |
| validation location | `LanguageProviderLoader.findLanguage` | [F] 见 §3.6 |

> **本表是源码推导，不是实验观测。** 001b-4 的职责是用实际运行确认或推翻它。
> 按 Spike_001b.md §八，本报告**不据此提出任何方案**。

**语义落差的来源（两个数字各自的事实，均为 [F]）**：

- Forge `loaderVersion="[52,)"` 指向 **Forge mod-loader ABI 版本**（52 系列）。
- NeoForge `javafml.version()` 返回 **FML 工件版本**（4.0.45，取自 JAR 版本元数据）。
- 二者是**两套不同编号体系**，取值范围不相交。

---

## 六、对 Plan A / Plan B 的中性输入（**不预选**）

以下只是本子 Spike 采集到的**机制事实**，用于后续决策，不构成任何方案倾向。

| 事实 | 标签 |
|---|---|
| `languageProviderMap` 以 `name()` 为键、用 `Map.put`，**无重名检测** → 同名 provider 静默互相覆盖 | [F] |
| `IModLanguageLoader` **不是** `IOrderedProvider` → provider 顺序不受 `getPriority()` 控制，而是 `ServiceLoader` 枚举顺序 | [F] |
| `IModLanguageLoader` **不在** `TransformerDiscovererConstants.SERVICES` 白名单 → 单独声明它不会让 `mods/` JAR 被提前提升 | [F] |
| 若某 JAR 因**其它**白名单服务（如 `IModFileReader`）已被提升进 SERVICE 层，则它同时声明的 `IModLanguageLoader` 也会被同一 `ServiceLoader` 看见 | [H]（推断，需运行侧对照） |
| 任何 provider 若 `version()` 为 null/blank，会导致 `RuntimeException`（不是 ModLoadingIssue） | [F] |
| `loadMod(IModInfo, ModFileScanData, ModuleLayer)` 是后续（001c）唯一入口，本 Spike 不触碰 | [F] |

---

## 七、事实标签

### [F] Source Fact

1. `[F]` `ModFileInfo` 构造器从配置文件**文件级**读取 `modLoader` 与 `loaderVersion`，
   并生成**恰好一个** `IModFileInfo.LanguageSpec(languageName, acceptedVersions)`。
2. `[F]` `loaderVersion` 字符串在 `ModFileInfo` 构造期被 `MavenVersionAdapter.createFromVersionSpec`
   转成 `org.apache.maven.artifact.versioning.VersionRange`。
3. `[F]` `ModFileInfo.requiredLanguageLoaders()` 返回该 `languageSpecs` 列表；`getMods()` 返回 `mods` 字段。
4. `[F]` `ModFile.identifyLanguage()` 对每个 `LanguageSpec` 调用
   `FMLLoader.getLanguageLoadingProvider().findLanguage(this, spec.languageName(), spec.acceptedVersions())`，
   结果存入 `ModFile.loaders`。
5. `[F]` `IModInfo.getLoader()` 返回 `ModFile.getLoaders().getFirst()`，
   类型为 `net.neoforged.neoforgespi.language.IModLanguageLoader`。
6. `[F]` `LanguageProviderLoader` 在 `FMLLoader.completeScan` 第一条语句被构造，内部通过
   `ServiceLoaderUtil.loadServices(ctx, IModLanguageLoader.class)` 取得全部 provider，并按 `name()` 建 `Map`。
7. `[F]` `IModLanguageLoader` 不继承 `IOrderedProvider`，也不含 `getPriority()`。
8. `[F]` 版本校验发生在 `LanguageProviderLoader.findLanguage`，
   主判据为 `VersionRange.containsVersion(loader 版本)`，并经 `VersionSupportMatrix` 覆盖表二次尝试。
9. `[F]` `VersionSupportMatrix.overrideVersions` 仅含 `mod:minecraft`→`1.21` 与 `mod:neoforge`→`21.0.166`，
   **不含**任何 `languageloader:*` 项。
10. `[F]` 校验失败抛 `ModLoadingException(fml.modloadingissue.language.missingversion)`，并带 `withAffectedModFile(file)`。
11. `[F]` `FMLJavaModLanguageProvider.name()` 返回字面量 `"javafml"`。
12. `[F]` `BuiltInLanguageLoader.version()` 通过 `JarVersionLookupHandler.getVersion(getClass())`
    从 JAR 版本元数据取值。
13. `[F]` `TransformerDiscovererConstants.SERVICES` 六项白名单**不含** `IModLanguageLoader`。
14. `[F]` mods/ 目录 LIBRARY 型 mod file 的 PLUGIN 层注入发生在 ModLauncher
    `buildTransformingClassLoader`，**晚于** `triggerScanCompletion`（= `completeScan`）。
15. `[F]` `IModLanguageLoader` 的方法表为 `name()` / `version()` /
    `loadMod(IModInfo, ModFileScanData, ModuleLayer)` / `default validate(...)`。

### [E] Experimental Fact

本报告**不产生**新的实验事实。仅引用 Spike-001a-0 已有观测（不重复判定）：

1. `[E]` `Found 3 implementations of IModLanguageLoader` → `FMLJavaModLanguageProvider` /
   `LowCodeModLanguageProvider` / `MinecraftModLanguageProvider`，均 `from loader-4.0.45.jar`。
2. `[E]` `Found language provider javafml, version 4.0.45`（lowcodefml / minecraft 同版本）。
3. `[E]` 001a-0 中 `forgeprobe.jar` 的 `ModFile` 被标记 `with languages []`，
   即 `requiredLanguageLoaders()` 为空 → `findLanguage` **未**被调用。

### [H] Hypothesis

1. `[H]` 若某 `mods/` JAR 因声明 `IModFileReader` 而被提升进 SERVICE 层，
   它同时声明的 `IModLanguageLoader` 也会被 `LanguageProviderLoader` 看见。
   （依据 `ServiceLoader` 语义 + §3.4 的层结论；本轮未做该对照。）
2. `[H]` `LanguageProviderLoader` 构造时刻 PLUGIN 层是否已被预创建为空层，本轮未观测；
   若已预创建，则 `loadServices` 命中的"最高层"是 PLUGIN，但 provider 仍来自其 parent（SERVICE / BOOT）。
3. `[H]` `loaderVersion="[52,)"` 会在 001b-4 中产生
   `fml.modloadingissue.language.missingversion`（§五 的推导尚未由运行确认）。
4. `[H]` 同名 provider 的覆盖顺序在实际运行中是否稳定，取决于 `ServiceLoader` 枚举顺序；
   本轮未观测其在多层场景下的行为。

### [D] Design Decision

1. `[D]` 手册 §3.1 中 `IModLanguageLoader` 的层级标签维持 `[H]`；
   本报告已把其中**可判定部分**升级为 [F]（"不是 mods/ 新建的 PLUGIN 内容"）。
   **具体层名（SERVICE 还是 PLUGIN）仍需运行侧观测**，留给 001b-3。
2. `[D]` 兼容层若要提供 `IModLanguageLoader`，其 JAR **必须**能进入
   `LanguageProviderLoader` 构造时刻**已存在**的层；仅靠"放进 `mods/` + 声明 `IModLanguageLoader`"不成立（[F]13）。
3. `[D]` 同名静默覆盖、且无 priority 控制（[F]6–7）是 Plan A / Plan B 决策的**硬约束**，
   建议在 001b-3 的观测清单中显式覆盖。

---

## 八、本轮未回答 / 未观测（明确边界）

| 项 | 状态 |
|---|---|
| `LanguageProviderLoader` 构造时 `LaunchContext.loadServices` 命中的**具体层名** | 未观测（[H]2） |
| `mods/` JAR 能否通过"旁路服务提升"间接提供 `IModLanguageLoader` | 未验证（[H]1） |
| `loaderVersion="[52,)"` 的**实际**运行结果 | 未验证，留给 001b-4 |
| 同名 provider 覆盖的实际行为 | 未验证（[H]4） |
| `loadMod` / `FMLModContainer` / entrypoint / ABI | 本 Spike **禁止触碰**，未分析 |

---

## 九、禁止事项确认

- [x] 未修改 `forgeprobe.jar`
- [x] 未添加 `neoforge.mods.toml`
- [x] 未修改 NeoForge 核心
- [x] 未引入 ASM / Mapping / Mixin / Registry / Event / Isolation
- [x] 未修改 `FMLJavaModLoadingContext` ABI
- [x] 未提出修复方案
- [x] 未预设 Plan A 或 Plan B
- [x] 未修改任何代码（本子 Spike 为纯源码分析）
- [x] 本报告不含 Failure Chain（按 Spike_001a.md §六，001b-0 只记录源码路径）
