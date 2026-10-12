# Forge → NeoForge 兼容层开发手册

**版本**：v1.1（架构冻结版·最终）
**基础环境**：Minecraft 1.21.1 + NeoForge 21.1.256 + FancyModLoader 4.0.45 + Java 21
**项目定位**：以 NeoForge 为唯一宿主的 Forge 模组运行时兼容层，为未来 Fabric 适配预留插件化接口。
**当前状态**：架构冻结，事实验证未冻结。

```
Spike-000      ✅ 完成
Spike-001a     ✅ 完成
Spike-001b-0   ✅ 完成
Spike-001b-1   ✅ 完成
Spike-001b-3   ✅ 完成
Spike-001b-4   ⏭ 已被 001b-1 覆盖
Spike-001b-5   ✅ 完成
Spike-001b-6   ✅ 完成
Spike-001c-0   ✅ 完成
Spike-001c-1   ✅ 完成
Spike-001c-2   ✅ 完成（VALID / 层 c ABI Break）
Spike-002-0a   ✅ 完成（层 c PASS；层 d FAIL）
Spike-002-0b Step 1     ✅ 完成
Spike-002-0b-1          ✅ 完成
Spike-002-0b-1b         ✅ 完成
Spike-002-0b Step 2-A v1     ⛔ STOP（前置配置缺口）
Spike-002-0b Step 2-A v1.1   ✅ 完成（PASS）
Spike-002-0c            ✅ 完成（PASS-002-0c-1：G-1 条件下的历史基线）
Spike-002-0d            ✅ 完成（VALID-002-0d-1：G-1 清零，Run T 到达 Done）
Spike-002-0e            ✅ 完成（VALID-002-0e-1：MinecraftForge P1）
Spike-002-0f            ✅ 完成（PASS：EVENT_BUS静态契约）
Spike-002-0g            ✅ 完成（PASS：局部链接越过P1，进入Neo register，终点S1）
Spike-002-0h            ✅ 完成（PASS：零订阅候选条件分支；原Forge运行仍未验证）
Spike-002-0i            ⛔ STOP（原Forge夹具前置缺件，register未运行）
Spike-002-0i-1          ✅ 完成（PASS：同一裸对象夹具原Forge register正常返回）
Spike-002-0j            ✅ 完成（VALID：固定零订阅探针空注册保真，Done）
```

**版本标签**：v1.1 + Spike-000 回写 + Spike-001a 回写 + Spike-001b-0 回写
+ Spike-001b-3 回写 + Spike-001b-5 回写 + Spike-001c-2 / 002-0a / 002-0b（Step 1 / 1 / 1b / 2-A v1.1）回写
+ Spike-002-0c 回写（含 G-1 认定）+ Spike-002-0d 回写（2026-10-11，含证据勘误）
+ Spike-002-0e / 0f / 0g / 0h 回写（2026-10-12，单智能体串行流程）。
+ Spike-002-0i / 0i-1 / 0j 回写（受控原Forge行为及限定空注册语义）。
回写只改动事实标签与状态、新增 §3.6 / §3.7 / §3.8，**不改动架构**。

**版本历史**：

```
v1.0  源码验证版
   ↓
v1.1  架构冻结版  ← 当前
   ↓
v1.2  实验验证版
   ↓
v2.0  第一阶段实现版
```


## 一、项目定位与核心原则

### 1.1 定位

本项目是**兼容层**，不是统一加载器。Forge 模组通过 FancyModLoader 官方 SPI 注入 NeoForge 原生生命周期，**不修改 NeoForge 核心源码**。

### 1.2 核心原则

| 原则 | 说明 |
|---|---|
| **单一宿主** | NeoForge 是唯一底层补丁基准，Forge 模组冲突在适配层消化 |
| **官方 SPI 优先** | 通过 `IModFileCandidateLocator`、`IModFileReader`、`IModLanguageLoader` 接入 |
| **共享优先，隔离可选** | 默认共享 Game Layer；仅对冲突模组启用旁路 `URLClassLoader` |
| **低侵入优先** | API Shim > 字节码转换 > Mixin 补丁 |
| **Vertical Slice 先行** | 先跑通最小 Forge 模组闭环，再横向扩展 |
| **诊断即生命线** | 结构化错误报告，拒绝裸 `NoSuchMethodError` |
| **架构冻结，事实验证未冻结** | 不再扩展架构，进入源码实验阶段 |
| **从正例不反推必要条件** | 一个成功案例不构成必要条件；必要条件必须由负对照实验确认 |
| **单路径结论限定单路径** | 一个已验证路径上的结论，不得外推为系统级事实 |


## 二、总体架构

```
┌─────────────────────────────────────────────────────┐
│                 Compatibility Core                   │
│  Mod Discovery │ Lifecycle │ Mapping │ Diagnostics   │
│  Transformer Pipeline │ Cache │ Capability System    │
└──────────────────────┬──────────────────────────────┘
                       │
          Compatibility Plugin API
                       │
        ┌──────────────┼──────────────┐
        │              │              │
   Forge Adapter   Fabric Adapter  Future Adapter
   ┌────┴────┐     ┌────┴────┐
   │ Locator │     │ Shim    │
   │ Reader  │     │ Trans.  │
   │ Parser  │     │ Event   │
   │ Loader  │     │ Registry│
   │ Shim    │     │ Lifecycle│
   └────┬────┘     └────┬────┘
        │              │
        └──────┬───────┘
               ↓
    NeoForge FancyModLoader
    (Discovery → Validation → Scan → Layer → Container)
               ↓
         Minecraft 1.21.1
```

**两层身份区分**：

```
SERVICE / BOOT 层族
    │
    └── Compatibility Core (LIBRARY)
Game Layer
    │
    └── Forge Mods (MOD)
```


## 三、FancyModLoader SPI 接入

### 3.1 SPI / API 分类（源码 + 实验已验证）

| SPI / API | 角色 | 加载方式 |
|---|---|---|
| `IModFileCandidateLocator` | 候选发现扩展点 | **ServiceLoader** [F]；实际 provider Layer = SERVICE [E] |
| `IModFileReader` | ModFile 创建扩展点 | **ServiceLoader** [F]；实际 provider Layer = SERVICE [E] |
| `ModFileInfoParser` | Mod metadata 解析器 | **由 Reader 显式使用；加载方式待 21.1.256 源码确认** |
| `IModLanguageLoader` | ModContainer 创建扩展点 | **ServiceLoader** [F]；内置 provider Layer = **BOOT** [E]；兼容层 provider = **SERVICE** [E]（Spike-001b-3） |

> **Provider SPI 与 Internal/Parser API 必须区分。** `ModFileInfoParser` 是 `@FunctionalInterface`，天然属于 Reader 内部选择 Parser 的机制，不是 ServiceLoader provider。

**`IModLanguageLoader` 的解析链（Spike-001b-0 源码分析确认）** `[F]`：

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
          不通过 → ModLoadingException(fml.modloadingissue.language.missingversion)
          通过   → wrapper.modLanguageProvider()
        ↓
ModFile.loaders : List<IModLanguageLoader>
        ↓
IModInfo.getLoader() = owningFile.getFile().getLoaders().getFirst()
```

**关键事实（均为 [F]，来自 `loader-4.0.45.jar` 字节码）**：

- `[F]` `modLoader` / `loaderVersion` 在 **`ModFileInfo` 构造阶段**解析；`loaderVersion` 被解析为 `org.apache.maven.artifact.versioning.VersionRange`。
- `[F]` 实际 **provider 查找与版本验证**发生在 `LanguageProviderLoader.findLanguage`，**不在** `loadMod`，也不在 `ModFileInfo` 构造期。
- `[F]` `IModInfo.getLoader()` 返回类型为 `net.neoforged.neoforgespi.language.IModLanguageLoader`，取值为 `ModFile.getLoaders().getFirst()`。
- `[F]` `LanguageProviderLoader` 以 `name()` 为键、用 `Map.put`，**无重名检测**；同名 provider 静默互相覆盖，覆盖顺序由 `ServiceLoader` 枚举顺序决定。
- `[F]` `IModLanguageLoader` **不实现** `IOrderedProvider`，`getPriority()` 不适用。
- `[F]` `IModLanguageLoader` **不在** `TransformerDiscovererConstants.SERVICES` 白名单内。
- `[F]` `VersionSupportMatrix.overrideVersions` **不包含**任何 `languageloader:*` 项。
- `[H]` 在当前已定位的 `VersionSupportMatrix` 路径中，未发现可绕过 `javafml` `provider.version()` 检查的 override。**该结论限定于 `findLanguage()` 这条已定位路径。**
- `[E]` **`IModLanguageLoader` 的实际 provider Layer（Spike-001b-3 实测）**：
  内置 `FMLJavaModLanguageProvider` / `LowCodeModLanguageProvider` / `MinecraftModLanguageProvider`
  均来自 **`BOOT`** 层（module `fml_loader`，CodeSource = `loader-4.0.45.jar`）；
  兼容层 provider 来自 **`SERVICE`** 层（module `forgecompat`，CodeSource = `mods/forgecompat.jar`）。
  > 判定手段是 `IModuleLayerManager.getLayer(L).get()` 与 `class.getModule().getLayer()` 的
  > **对象同一性比对（`==`）**，**不是**由 Java ModuleLayer 直接推断 ModLauncher Layer。
  > 该观测发生时 `GAME` 层尚不存在，`PLUGIN` 层已存在（含 `neoforge.coremods`）。
- `[E]` **同名 provider 覆盖行为（Spike-001b-3 实测）**：`LanguageProviderLoader` 按 `ServiceLoader`
  流顺序执行 `Map.put(name, wrapper)`，后写入者胜出。实测顺序
  `javafml@4.0.45 → lowcodefml@4.0.45 → minecraft@4.0.45 → javafml@9999`，
  故 `languageProviderMap["javafml"]` 最终由兼容层 provider 占据（**情况 A**）；两次运行顺序一致。
- `[F]` `findLanguage` 的两条错误分支文案长度不同：分支①（provider 未找到）
  `"Missing language {} version {} wanted by {}"`；分支②（`VersionSupportMatrix`）
  `"Missing language {} version {} wanted by {}, found {}"`。
  **可用作运行时区分两分支的日志判据。**
- `[F]` `findLanguage` 的调用发生在 `FMLLoader.completeScan → stage2Validation → validateLanguages
  → identifyLanguage`，**不是** `beginScanning` 阶段。stage1 里
  `"Loading mod file ... with languages [...]"` 只读取 `requiredLanguageLoaders()` 用于日志，
  **不触发** `findLanguage`。
- `[F]` 分支② 抛出的 `ModLoadingException` **不带 cause**；`validateLanguages` 只取 issue、丢弃异常对象。
  `findLanguage` 的**原始栈不可恢复**。

**兼容层 JAR 的 ModFile Type（已由 Spike-001a-0 修正）**：`IModFileReader` / `IModFileCandidateLocator` 这类 provider 的实际提升路径**不是** Plugin Layer，而是 ModLauncher 层的 `ModDirTransformerDiscoverer` 把它们提前提升进 **SERVICE Layer**。详见 §3.6 与 §3.7。

### 3.2 IModFile 创建流程（已纠正）

```java
// 正确流程
SecureJar secureJar = SecureJar.from(jarContents, new ForgeJarMetadata(jarContents));
IModFile modFile = IModFile.create(secureJar, new ForgeModFileInfoParser());
```

**关键事实**：

- `IModFile.create()` 的第一个参数是 `SecureJar`，**不是** `JarContents`。[F]
- `JarContents → SecureJar` 的转换由 `ForgeModReader` **自己完成**。[F]
- `ModJarMetadata` 是 NeoForge 的实现，不可直接假设适用于 Forge 模组；需要实现 `ForgeJarMetadata` 或验证其可复用性。[H]

### 3.3 Class Scanning 与 Entrypoint 识别的阶段切割

```
Class scanning
     ↓
ModFileScanData
     ↓
Language-specific interpretation
     ↓
Entrypoint extraction
     ↓
FMLModContainer
```

**兼容层不应重复实现 NeoForge 的通用 class scan 机制；但不得预设 NeoForge 的 scanner 会识别 Forge 的 `net.minecraftforge.fml.common.Mod` 注解。**

**`Scanner` 负责"发现类/注解信息"，谁把这些信息解释成 Forge entrypoint 是另一层职责。** FancyModLoader 当前源码路径为 `modInfo.getLoader().loadMod(modInfo, modFile.getScanResult(), gameLayer)`，即先取得 `scanResult`，再交给 language loader。因此 `findForgeModEntrypoints()` 留到 `ForgeLanguageLoader` 是合理的。

**Forge `@Mod` 的识别方式必须通过 Spike 验证。**[H]

`[E]` **Spike-001c-0 实测（关键）**：NeoForge 21.1.256 的**内置 scanner 确实扫描了 Forge 原生 JAR**，
并把 Forge 注解连同 metadata 放进了 `ModFileScanData`：

| 项 | 实测值 |
|---|---|
| `scanData.getAnnotations().size()` | `1` |
| `annotationType().getClassName()` | `net.minecraftforge.fml.common.Mod` |
| `clazz().getClassName()` | `com.example.forgeprobe.ForgeProbe` |
| `targetType()` / `memberName()` | `TYPE` / `com.example.forgeprobe.ForgeProbe` |
| `annotationData()` | `{value=forgeprobe}` |
| 对照：`net.neoforged.fml.common.Mod` | **NO** |
| `getClasses().size()` | `1`（`Lcom/example/forgeprobe/ForgeProbe;`） |

`[F]` `AnnotationData` 是 record，访问器为
**`annotationType()` / `targetType()` / `clazz()` / `memberName()` / `annotationData()`**
（**没有** `getClassType()` / `getAnnotationData()`）；`annotationType()` / `clazz()` 返回 `org.objectweb.asm.Type`。

> **纪律不变**：以上只证明"**scanner 扫描到了这个类与这条注解**"，
> **不等于** "language loader 把该 `@Mod` 解释为 entrypoint"。两者不是同一件事。

### 3.4 ModuleLayer 注入

- `ModFile` **不负责**将自己加入 `ModuleLayer`。[F]
- `ModValidator.getModResources()` 收集所有 `ModFile.getSecureJar()`，传递给 `IModuleLayerManager.Layer.GAME`。[F]
- `FMLModContainer` 通过 `gameLayer.findModule(info.getOwningFile().moduleName())` 获取模块。[F]

**必须验证四个身份一致**：

```
SecureJar.name()
IModInfo.moduleName()
ModuleDescriptor.name()
GameLayer.findModule(...)
```

**验证的是 Jar metadata identity → Mod metadata identity → Java Module identity → Layer identity 的一致性，而非仅仅四个字符串。**[H]

### 3.5 构造函数注入白名单（绑定精确版本）

**经源码验证的 NeoForge 21.1.256 FMLModContainer constructor injection types**：

白名单容器：`Map.of(Class<?>, Object) × 4`（`FMLModContainer.constructMod` 内）

| # | 键（`Class`） | 值（注入对象） | 证据 |
|---|---|---|---|
| 1 | `net.neoforged.bus.api.IEventBus` | mod event bus（`FMLModContainer.eventBus` 字段） | `[F]` **raw bytecode 直接证明** |
| 2 | `net.neoforged.fml.ModContainer` | `this` | `[F]` raw bytecode 直接证明 |
| 3 | `net.neoforged.fml.javafmlmod.FMLModContainer` | `this` | `[F]` raw bytecode 直接证明 |
| 4 | `net.neoforged.api.distmarker.Dist` | `FMLLoader.getDist()` | `[F]` raw bytecode 直接证明 |

**判定机制：精确匹配**（`Map.get(Object)` + `Set.contains(Object)`，`equals()` 语义）；
方法体内**不存在** `Class.isAssignableFrom` / `Class.isInstance`。
（`[F]` Spike-002-0b Step 1 + raw bytecode 补证）

**运行时验证**（`[E]`）：

- 002-0a：空壳 Forge `IEventBus` 使层 c 通过；层 d 抛
  `RuntimeException: Mod constructor has unsupported argument interface
  net.minecraftforge.eventbus.api.IEventBus`
- 002-0b Step 2-A v1.1：把描述符参数类型重写为
  `net.neoforged.bus.api.IEventBus` 后，层 d 解除（层 e marker 命中）

> 四项键的 raw bytecode 依据见 `Spike-002-0b Step1 取证报告.md` §三（offset 59–84）。
> 第四键（`IEventBus`）的原始行本轮独立复核：**`offset 64: ldc #203 // class net/neoforged/bus/api/IEventBus`**
> （完整 dump 存档 `logs\spike002-0b-2a-artifacts\3-IEventBus-rawbytecode-FMLModContainer.constructMod.txt`）。

`[F]` **`FMLModContainer` 自身的构造函数（Spike-001c-1 取证）**：**唯一且 public**——

```java
public FMLModContainer(IModInfo, List<String>, ModFileScanData, ModuleLayer)
```

`[E]` Spike-001c-1 实测：在真实 `loadMod(...)` 中用 `scanData` 提取的 Forge entrypoint
（`com.example.forgeprobe.ForgeProbe`）调用该构造函数，**正常返回**
（`net.neoforged.fml.javafmlmod.FMLModContainer@…`），未抛异常；
`constructMod()` 未被调用（该方法是 `protected`）。

> `FMLModContainer` 构造成功 **≠** Forge Mod 可被加载 —— 容器构造与 `constructMod()` 必须分开评估。

> **具体签名必须绑定实验所使用的 NeoForge 21.1.256 精确版本，不得用 current/main 分支源码替代。** 实验环境已冻结为 `NeoForge = 21.1.256`。

**不支持 `FMLJavaModLoadingContext`，也不支持 `net.minecraftforge.eventbus.api.IEventBus`（Forge 命名空间的 IEventBus）。**[F] Forge 1.21.1 新增 `FMLJavaModLoadingContext` 参数（Forge PR #10074），这是真实 ABI 断层。[F] **Forge 的 `net.minecraftforge.eventbus.api.IEventBus` 与 NeoForge 的 `net.neoforged.bus.api.IEventBus` 不是同一个 Java 类型**，`FMLModContainer` 能否在不进行 ABI 转换的情况下实例化参数类型属于 Forge 命名空间的构造器，是 Spike-001c 的核心问题。[H]

### 3.6 Service Provider 加载机制（Spike-001a-0 回写）

**加载入口（[F]，`loader-4.0.45.jar` 字节码）**

```
ServiceLoaderUtil.loadServices(ILaunchContext ctx, Class<T> service[, Collection<T> builtIns])
  └─ ctx.loadServices(service)                        // ILaunchContext
       └─ LaunchContext.loadServices(Class<T>)        // net.neoforged.fml.loading.LaunchContext
            └─ for (Layer layer : reversed(Layer.values())) {
                   if (handled.contains(layer)) continue;
                   ModuleLayer ml = moduleLayerManager.getLayer(layer).orElse(null);
                   if (ml == null) continue;
                   result = concat(result, ServiceLoader.load(ml, service).stream());
                   visitLayer(layer, handled::add);
               }
```

- `[F]` `IModuleLayerManager.Layer` 枚举序为 `BOOT(0) → SERVICE(1) → PLUGIN(2) → GAME(3)`。
- `[F]` `LaunchContext.loadServices` **逆序**取"最高的已存在层"，因此实际会搜索 **该层 + 其全部 parent 层**。
- `[F]` 结果按 `IOrderedProvider.getPriority()` 降序排序（仅当 service 为 `IOrderedProvider` 时）。
- `[E]` 该日志行是后续所有 Spike 判定 provider 是否可见的**首要证据**。

**当前已验证的 Service 层提升通道（[F]，Spike-001a-0 首次定位）**

```
ModLauncher TransformationServicesHandler.discoverServices(DiscoveryData)
  ├─ BOOT 层 ServiceLoader 取 ITransformerDiscoveryService 实现
  │    loader-4.0.45.jar 提供两个：
  │      net.neoforged.fml.loading.ModDirTransformerDiscoverer
  │      net.neoforged.fml.loading.ClasspathTransformerDiscoverer
  └─ ModDirTransformerDiscoverer.candidates(gameDir, launchTarget)
       └─ scan(gameDir): Files.walk(<gameDir>/mods, 1)
            .filter(shouldLoadInServiceLayer)
            → NamedPath(name, path)
       → ModuleLayerHandler.addToLayer(Layer.SERVICE, namedPath)
```

```java
// net.neoforged.fml.loading.TransformerDiscovererConstants  [F]
public static final Set<String> SERVICES = Set.of(
    cpw.mods.modlauncher.api.ITransformationService.class.getName(),
    net.neoforged.neoforgespi.locating.IModFileCandidateLocator.class.getName(),
    net.neoforged.neoforgespi.locating.IModFileReader.class.getName(),
    net.neoforged.neoforgespi.locating.IDependencyLocator.class.getName(),
    net.neoforged.neoforgespi.earlywindow.GraphicsBootstrapper.class.getName(),
    net.neoforged.neoforgespi.earlywindow.ImmediateWindowProvider.class.getName()
);

public static boolean shouldLoadInServiceLayer(JarContents jar) {
    return JarMetadata.from(jar).providers().stream()
        .map(SecureJar.Provider::serviceName)
        .anyMatch(SERVICES::contains);
}
```

- `[F]` **判据只读 `META-INF/services/`**，不读 manifest 的 `FMLModType`。
- `[E]` 该提升发生在 **LaunchContext 创建之前**，因此被提升的 JAR 会出现在
  `Located paths when launch context was created` 列表中。
- `[E]` 实测：`Found additional transformation services from discovery services: [...\mods\forgecompat.jar]`；该行在无此 JAR 的运行中为空列表。
- `[D]` 对 `IModFileReader` / `IModFileCandidateLocator` 而言，这就是**官方预留的第三方注入点**。

**时间顺序（[E]，Spike-001a-0）**

```
ModLauncher discoverServices          → 提升 mods/forgecompat.jar 进 SERVICE 层
LaunchContext 构造                     → locatedPaths 含 forgecompat.jar
FMLServiceProvider.beginScanning
  └─ FMLLoader.beginModScan
       └─ new ModDiscoverer(ctx, ...)   → 此刻即 loadServices(locator) 与 loadServices(reader)
            └─ ModDiscoverer.discoverMods() → 逐个 locator 扫描并 read
FMLLoader.completeScan                 → 构建 PLUGIN / GAME 层（LIBRARY 型 mod file 进 PLUGIN）
```

- `[F]` reader/locator 的服务查找**先于** `ModDiscoverer` 的普通 mod-file 扫描本身，因此一个 JAR 不可能通过"被 `ModDiscoverer` 的普通 mod-file 扫描找到"来提供 reader。
- `[H]` **这一结论仅限定于 `ModDiscoverer` 的普通 mod-file 扫描路径。** 其它早期 classpath / service discovery 机制（例如 `ClasspathTransformerDiscoverer`）是否也能提供 reader/locator，尚未单独验证。

#### 3.6.1 第三方类转换通道与 ④→⑤→⑥ 静态链（Spike-002-0b-1 / 002-0b-1b / 002-0b-2A v1.1 回写）

**两条候选通道**

| 通道 | SPI | 白名单 | provider 来源 Layer |
|---|---|---|---|
| **A** | `cpw.mods.modlauncher.api.ITransformationService#transformers()` | **含** | **SERVICE** `[F]` |
| **B** | `cpw.mods.modlauncher.serviceapi.ILaunchPluginService` | **不含** | **BOOT**（`Launcher` 构造期）`[F]` |

> `[F]` FML 4.0.45 中**不存在** `ClassProcessor` / `ClassProcessorProvider` 类（002-0b-1 §三）。

**通道 B 静态不可达**（`[F]`，**非**"不在白名单"外推；002-0b-1b §2.3）

依据来自 `ModuleLayerHandler.addToLayer` 的**全量调用点枚举**：

- 只有 `Launcher`（PLUGIN / GAME 资源）与 `TransformationServicesHandler`
  （`ITransformerDiscoveryService` 候选 → SERVICE 层）
- **无一以 BOOT 层为目标**；且 `addToLayer` 对已构建层抛
  `IllegalStateException("Layer already populated")`
- `mods/` JAR 的唯一层提升入口是 `ModDirTransformerDiscoverer.candidates()`，
  **终点固定为 SERVICE 层**

⇒ 第三方 `mods/` JAR 经通道 B 注册，**静态链不可达**。

**④→⑤→⑥ 静态链**（`[F]`，含一处 `[F-j]`）

```
FMLModContainer.<init>
  ├─ ModuleLayer.findModule(info.getOwningFile().moduleName())      [F]
  └─ Class.forName(Module, entrypointName)                          [F]
───────── JDK 契约 ─────────
  Class.forName(Module,String) 使用 module.getClassLoader()         [F-j]
───────── 再入点 ─────────
  ModuleClassLoader.loadClass → findClass → loadFromModule
    → readerToClass → getClassBytes                                 [F]  ← byte[] 形成点
    → maybeTransformClassBytes(bytes, name, null)                   [F]
    → ClassTransformer.transform(bytes, name, "classloading")       [F]  ← 同一实例字段
      → performVote
        → ITransformer.transform(T, ctx)                            [F]  ← ⑥（通道 A）
        → ILaunchPluginService.processClassWithFlags(...)           [F]  ← ⑥（通道 B）
```

**同一性论证**（`[F]`）：④ 的 `ModuleLayer` 实参来自 `FMLLoader.getGameLayer()`；
该层由 `ModuleLayer.defineModules(configuration, parents, cl)` 以**同一个
`TransformingClassLoader` 实例**定义整层模块 ⇒ ⑤ 的 `this` 与 ④ 的 module loader
为**同一对象**；⑥ 又挂在该实例的 `classTransformer` 字段上。
报告判定：**同一静态调用路径 = YES**。

**实际 `T`**（`[F]`）

```
通道 A:  ITransformer<ClassNode>   （PRE_CLASS / CLASS 级）
         MethodNode（METHOD 级）/ FieldNode（FIELD 级）
通道 B:  ILaunchPluginService.processClassWithFlags(Phase, ClassNode, Type, String)
```

⇒ **ASM 解析由 ModLauncher 代劳**；第三方 transformer 操作的是**已解析树**，不是 `byte[]`。

**`targets()` 匹配规则**（`[F]`）

- ASM `Type` 归一化 + name / descriptor `equals`（**精确匹配**）
- 四张独立 `TargetType` 子表（PRE_CLASS / 字段 / 方法 / CLASS）
- 空 `targets()` 静默跳过

**ForgeProbe 满足静态匹配前提 = YES**（`[F]`，002-0b-1b §7.4）

**时序事实**（`[F]`）—— `Launcher.run` 字节码顺序：

```
discoverServices(14) → triggerScanCompletion(114) → 资源入层(~140–191)
  → initialiseServiceTransformers(200, ⑥ 注册)
  → buildTransformingClassLoader(240) → LaunchServiceHandler.launch(288)
```

④ 的 entrypoint 类加载发生在 launch 之后的 FML 启动流程里
⇒ **⑥ 的注册先于 ④ 的类加载**。

**标签扩展：`[F-j]` = JDK Spec Fact** —— 由 JDK 规范保证的事实，**非本项目字节码证明**，
仅用于标记跨 JDK / VM 边界的连接点。本项目中仅 ④→⑤ 的
`Class.forName(Module,String)` 一跳使用该标签。

**运行时确认**（`[E]`，Spike-002-0b Step 2-A v1.1）：

- Run T 中 `offered: com/example/forgeprobe/ForgeProbe` → `REWRITE hits=1`
  → 层 d 解除 → `FORGE PROBE CONSTRUCTOR REACHED` 命中；
- ⇒ ④→⑤→⑥ 的**静态路径**在运行时**被实际穿过**（002-0b-1 证据表 **C 项**首次运行时确认）；
- ⇒ 注入的 EventBus 实现类 = `net.neoforged.bus.EventBus`（`BUS = net.neoforged.bus.EventBus@…`）。
### 3.7 兼容层 JAR 的当前已验证形态与未验证变量（Spike-001a-0 回写）

**当前已验证可工作的实验形态（[E]，对应 `spike001a/compat/build/forgecompat.jar`）**

```
META-INF/MANIFEST.MF
    Manifest-Version: 1.0
    Automatic-Module-Name: forgecompat
    FMLModType: LIBRARY

META-INF/services/net.neoforged.neoforgespi.locating.IModFileReader
    com.example.forgecompat.ForgeModReader
META-INF/services/net.neoforged.neoforgespi.locating.IModFileCandidateLocator
    com.example.forgecompat.ForgeModLocator
```

当前已验证的实验形态包含以下三项：

1. `[F]` 在 `META-INF/services/` 中声明 `TransformerDiscovererConstants.SERVICES` 白名单内的 SPI，
   是当前已验证的 Service Layer promotion 判据。仅声明 manifest 而不声明这些 services 的 JAR
   （如 `forgeprobe.jar`），不会通过该判据获得 SERVICE Layer 提升。
2. `[E]` 当前实验使用的兼容层 JAR 使用 `Automatic-Module-Name: forgecompat`，并成功进入 SERVICE Layer。
   `Automatic-Module-Name` 对本实验 JAR 的模块身份是当前已验证条件；
   **其是否为所有兼容层 JAR 的必要条件，尚未通过负对照实验确认。[H]**
3. `[E]` `FMLModType: LIBRARY` + `Automatic-Module-Name` 的组合照抄 NeoForge 自带
   `net.neoforged.neoforge-coremods-<ver>.jar` 的 manifest 形态；本形态已实测可用。

**提升后的行为（[E]）**

- 兼容层 JAR 会出现在 `Located paths when launch context was created` 中，
  随后被内置 `ModsFolderLocator` **跳过**：
  `Skipping ...\mods\forgecompat.jar because it was already located earlier`
  → 兼容层 JAR 自己不会再作为 mod 被二次读入。
- `[E]` 兼容层 JAR 的 `IModFileReader` 以 **priority 100** 先于所有内置 reader（0 / -1000）
  处理**每一个** JAR，包括 NeoForge 自带 JAR 与 jar-in-jar 内容。
  实现必须以"不匹配即返回 `null`"兜底，否则会污染其它 mod。

**尚未分离的变量（[H]，需专门对照实验）**

- `[H]` 保留 services 声明、移除 `FMLModType: LIBRARY` 后，提升行为是否不变；
  以及该 JAR 是否仍会被当作 library mod file 接受。
- `[H]` 保留 `FMLModType: LIBRARY`、移除 services 声明后，是否必然失去提升。
- `[H]` 保留 services 声明与 `FMLModType: LIBRARY`、移除 `Automatic-Module-Name` 后，
  `SecureJar.name()` / `ModuleDescriptor.name()` 是否仍可产生，以及提升是否仍成立。

**Spike-001a-0 未做上述三个负对照。因此本节只记录"当前已验证可工作的实验形态"，不宣称任何一项是必要条件。**


### 3.8 IModFileInfo 契约的隐式约束（Spike-001b-3 / 001b-5 回写）

> **本节性质**：Spike-001b-3 首次暴露第一层契约；Spike-001b-5 补全第二层契约，
> 并揭示第三层隐含约束。本节**只记录已由字节码或实验证据支撑的事实**；
> 未由证据支撑的推论一律进入 `[H]`。
> **不改动架构**，只记录 `IModFileInfo` / `IModFile` / `IModInfo` 三层隐式类型契约。

#### 3.8.1 三层隐式契约概览

NeoForge 21.1.256 / FML 4.0.45 的 mod 加载链中，至少存在三层**未被 Javadoc 记录**的类型强转契约：

```
第一层（最早暴露，Spike-001b-3）:
    ModSorter.sort(ModSorter.java:112)
        ModInfo modInfo = (ModInfo) imodInfo;

第二层（Spike-001b-5 新发现，触发更早）:
    LoadingModList.<init>(LoadingModList.java:54-58)
        modFiles 字段类型 = List<ModFileInfo>
        对 IModFile.getModFileInfo() 的返回值执行 ModFileInfo.class::cast

第三层（由 Spike-001b-5 的 ModFileInfo 构造签名反推）:
    ModFileInfo(ModFile owningFile, IConfigurable config, ...)
        owningFile 参数是 ModFile（具体类），不是 IModFile
```

即：**兼容层不能只实现 SPI 接口，必须构造 NeoForge 的具体实现类。**

#### 3.8.2 第一层契约：`IModInfo` → `ModInfo`

**`[F]` 源码事实**：

```
net.neoforged.fml.loading.ModSorter.sort(ModSorter.java:112):
    ModInfo modInfo = (ModInfo) imodInfo;
```

`ModSorter.sort` 内部对 `IModInfo` 的实例做强制类型转换为
`net.neoforged.fml.loading.moddiscovery.ModInfo`。

**`[F]` `ModInfo` 的精确结构（`loader-4.0.45.jar` 字节码）**：

| 项 | 值 |
|---|---|
| class | `net.neoforged.fml.loading.moddiscovery.ModInfo` |
| package | `net.neoforged.fml.loading.moddiscovery` |
| final / non-final | **non-final** |
| 构造函数 | `public ModInfo(ModFileInfo owningFile, IConfigurable config)`（**唯一**） |
| 构造函数可见性 | **public** |
| factory 方法 | **不存在** |
| 实现接口 | `net.neoforged.neoforgespi.language.IModInfo`、`net.neoforged.neoforgespi.language.IConfigurable` |

> 附带 `[F]`：`ModSorter` 内共 **7 处** `checkcast ModInfo`
> （`addDependency` ×2、`lambda$sort$6` ×2、`lambda$sort$0/1/2(ModFile)` ×3）。

**`[E]` 实验事实**：

- Spike-001b-3：`ForgeModInfo`（`IModInfo` 的自定义实现）在 `ModSorter.sort` 抛 CCE。
- Spike-001b-5：`getMods()` 返回真实 `ModInfo` 后，`ModSorter.sort` 的 `ModInfo::cast` **通过**。

**`[D]` 设计约束**：

> 兼容层 `IModFileInfo.getMods()` 返回的每个 `IModInfo` **必须是 `ModInfo` 或其子类**。
> **不能返回自定义 `IModInfo` 实现。**

#### 3.8.3 第二层契约：`IModFileInfo` → `ModFileInfo`

**`[F]` 源码事实（`loader-4.0.45.jar` 字节码）**：

`LoadingModList` 的字段：

```java
private final List<ModFileInfo> modFiles;     // 具体类，非 IModFileInfo
private final List<ModInfo>     sortedList;   // 具体类，非 IModInfo
```

`LoadingModList.<init>` 内部：

```
line ~54: ModFile::getModFileInfo
line ~55: ldc ModFileInfo.class → invokedynamic → ModFileInfo.class::cast
line ~58: ldc ModInfo.class     → invokedynamic → ModInfo.class::cast
```

**`[F]` 触发点**：

```
ModSorter.sort(ModSorter.java:82)
    ↓
LoadingModList.of(...) → new LoadingModList(...)
    ↓
LoadingModList.<init> 对 IModFile.getModFileInfo() 的返回值执行 ModFileInfo.class::cast
```

**`[E]` 实验事实**：

Spike-001b-5：兼容层 `ForgeModFileInfo`（自定义 `IModFileInfo` 实现）在 `LoadingModList.<init>` 抛 CCE：

```
ClassCastException:
Cannot cast com.example.forgecompat.ForgeModFileInfo
        to net.neoforged.fml.loading.moddiscovery.ModFileInfo
    at LoadingModList.<init>(LoadingModList.java:56)
    at LoadingModList.of(LoadingModList.java:70)
    at ModSorter.sort(ModSorter.java:82)
```

**`[D]` 设计约束**：

> 兼容层 `IModFile.getModFileInfo()` 返回的 `IModFileInfo` **必须是 `ModFileInfo` 或其子类**。
> **不能返回自定义 `IModFileInfo` 实现。**

**`[E]` 契约"搬家"效应**：

Spike-001b-3 满足第一层契约后，Spike-001b-5 暴露第二层契约。
说明这是**一组契约**而非单一检查点，每层契约都会拒绝自定义实现。

#### 3.8.4 第三层契约：`ModFileInfo` 构造参数的 `ModFile` 具体类型

**`[F]` 源码事实**：

`ModFileInfo` 构造函数签名包含具体类参数（共 2 个 public 重载）：

```java
public ModFileInfo(ModFile owningFile, IConfigurable config, Consumer<IModFileInfo> consumer);
public ModFileInfo(ModFile owningFile, IConfigurable config, Consumer<IModFileInfo> consumer,
                   List<IModFileInfo.LanguageSpec> languageSpecs);
                  ↑
                  ModFile（具体类），非 IModFile
```

**`[E]` 实验事实**：

Spike-001b-5 的独立编译探针 `spike001b5/probe/` 验证了以下代码 `javac --release 21` **exit 0**：

```java
SecureJar secureJar = ...;
ModFileDiscoveryAttributes attributes = ModFileDiscoveryAttributes.DEFAULT;

ModFile modFile = new ModFile(secureJar, parser, attributes);
ModFileInfo modFileInfo = new ModFileInfo(modFile, config, info -> { });
ModInfo modInfo = new ModInfo(modFileInfo, config);
```

**`[D]` 设计约束**：

> 兼容层 `IModFileReader.read()` 返回的 `IModFile` **必须是 `ModFile` 或其子类**。
> `ModFile` 的 `ModFileInfoParser` 参数**必须返回 `ModFileInfo`（具体类）**。

**`[F]` + `[E]` 内部 `new ModInfo`（Spike-001b-6 已证实）**：

`[F]` `ModFileInfo` 构造函数内部**确实**会生成 `ModInfo` 实例 ——
位于 `private IModInfo lambda$new$2(IConfigurable)`，即对 config 中**每个 mod 条目**
执行 `new ModInfo(this, entryConfig)`。

`[E]` 因此走真实 `ModFileInfo` 构造路径**一次越过两道 cast**（001b-6 实测）：

```
ForgeModReader.read()
    ↓ new ModFile(secureJar, parser, attributes)
ForgeModFileInfoParser.build()
    ↓ return new ModFileInfo(modFile, config, ...)
    ↓ 内部 new ModInfo(this, config)
LoadingModList.<init> 的 ModFileInfo::cast ✓
ModSorter.sort 的 ModInfo::cast ✓
```

> `[E]` 实测佐证：兼容层 `ForgeModFileInfoParser.build()` 返回真实 `ModFileInfo` 后，
> `stderr` 为 0 字节，两次 CCE（`…ForgeModInfo → ModInfo`、`…ForgeModFileInfo → ModFileInfo`）
> **全部消失**，流程推进到 `ModLoader.buildModContainerFromTOML` 并真实调用 `loadMod`。
> 另注：自定义 `MapConfigurable` 形态的 `IConfigurable` **未被 `ModFileInfo` 拒绝**。

#### 3.8.5 可访问性前提

**`[F]` 源码事实**：

| 类 | 构造函数可见性 | 可第三方调用 |
|---|---|---|
| `ModFile` | public | ✅ |
| `ModFileInfo` | public | ✅ |
| `ModInfo` | public | ✅ |

**`[F]` 模块系统事实**：

- `loader-4.0.45.jar` 的 `MANIFEST.MF` 为 `Automatic-Module-Name: fml_loader`
- **无 `module-info.class`** → 自动模块 → 所有包导出
- 结论：`net.neoforged.fml.loading.moddiscovery` 包**对兼容层可访问**

**`[E]` 实验事实**：

- Spike-001b-5 的编译探针**未使用**反射 / MethodHandles / Unsafe / 字节码注入
- **正常 Java 路径存在（`NORMAL JAVA PATH = YES`）**

#### 3.8.6 未解决事项（`[H]`）

1. ~~`[H]` 一次越过两道 cast 的可行性~~ → **`[E]` 已由 Spike-001b-6 证实成立**（见 §3.8.4）
2. ~~`[H]` `ModFileInfo` 对 `IConfigurable` 的隐含需求~~ → **部分转为 `[E]`**：
   自定义 `MapConfigurable` 形态**未被拒绝**；`ModFileInfo` 的硬性要求仅为
   `modLoader` / `loaderVersion` / `license`（缺失抛 `InvalidModFileException`）与每个 `[[mods]]` 条目的 `modId`
3. `[H]` **仍开放**：Spike-001c-0 / 001c-1 已越过 `ModSorter`、`LoadingModList`、
   `ModLoader.buildModContainerFromTOML` 并成功构造 `FMLModContainer`；
   **后续是否还有更多具体类型 cast，仍未穷尽**（`constructMod()` 之后完全未观测）
4. `[H]` **仍开放**：兼容层兼容多个 Forge mod 时，`LoadingModList` 的 `List<ModFileInfo>`
   是否要求所有元素同类（例如不允许部分来自内置、部分来自兼容层）
5. `[H]` **新开放**：`FMLModContainer` 构造函数**未解析 entrypoint 类的字节码**
   （否则 Forge 命名空间的构造参数类型应在此暴露）；entrypoint 类解析与 `@Mod` 解释
   很可能发生在 `constructMod()` 内 —— 属 001c-2 范围

#### 3.8.7 与既有章节的关系

- **§3.2（IModFile 创建流程）**：**不冲突**。Spike-001b-5 证明的
  `IModFile.create(SecureJar, parser)` 仍成立，但 **parser 的返回值约束由本节补充**。
- **§3.3（Class Scanning 与 Entrypoint 识别）**：**不冲突**。类型契约与扫描阶段切割正交。
- **§3.6（Service Provider 加载机制）**：**不冲突**。类型契约与 provider 加载路径正交。
- **§3.7（兼容层 JAR 当前已验证形态）**：**不冲突**。类型契约不改变兼容层 JAR 的 manifest / services 要求。

#### 3.8.8 对开发路线的直接影响

| 项 | 变更前假设 | 变更后约束 |
|---|---|---|
| `ForgeModReader.read()` 返回值 | `IModFile`（接口） | **`ModFile`（具体类）** |
| `ForgeModFileInfoParser.build()` 返回值 | `IModFileInfo`（接口） | **`ModFileInfo`（具体类）** |
| `ModFileInfoParser` 传入参数 | 自定义 `IModFileInfo` | `ModFileInfo` **必须是 `ModFileInfo` 具体类** |
| `ForgeModFileInfo` / `ForgeModInfo` | 兼容层核心组件 | **删除或降级为工具类** |

**对 Plan A / Plan B 的影响**：

- `[D]` **Plan A（同名 `javafml` provider）的实现路径需要修正**：
  - 原假设：兼容层实现 `IModFileInfo` / `IModInfo` 接口
  - 修正后：兼容层**必须构造 NeoForge 的具体实现类**（`ModFile` / `ModFileInfo` / `ModInfo`）
- `[E]` **正常 Java 路径存在**（`NORMAL JAVA PATH = YES`）——未构成 Plan A 的排除条件
- `[H]` 但 `ModFileInfo` 构造器是否有其他隐含需求，以及后续是否还有更多 CCE，**未验证**

---

> 本节完成于 **Spike-001b-5**，待 **Spike-001b-6** 后可能追加 §3.8.6 的部分
> `[H]` → `[E]` / `[F]` 升级。

### 3.8.9 FMLModContainer 五层失败点结构（Spike-001c-2 / 002-0a / 002-0b Step 1 / 002-0b-2A v1.1 回写）

`FMLModContainer.constructMod()` 的失败点由早到晚分为**五层**：

| 层 | 名称 | 典型异常 | 首次观测 | 最终状态 |
|---|---|---|---|---|
| **a** | entrypoint 类加载 | `ClassNotFoundException` / `NoClassDefFoundError` | 001c-1 | **PASS** |
| **b** | `@Mod` 注解解释 | `ModLoadingException` + issue key | 未独立观测 | UNKNOWN |
| **c** | 构造函数签名解析 | `NoClassDefFoundError`（Forge 命名空间参数类型） | 001c-2 **FAIL** | **PASS**（002-0a） |
| **d** | 参数注入 | `RuntimeException: Mod constructor has unsupported argument` | 002-0a **FAIL** | **PASS**（002-0b-2A v1.1） |
| **e** | 构造函数体 | 用户代码异常 | 002-0b-2A v1.1 **进入**（marker） | **已进入** |

**层 a 的实际机制**（`[F]`）：`FMLModContainer.<init>` 内通过 `Class.forName(Module, name)`
完成 entrypoint 类加载（`[F-j]`：`Class.forName(Module,String)` 使用 `module.getClassLoader()`）。

**层 c 与层 d 的分离**（`[E]`）：

- 001c-2：层 c **FAIL**（`NoClassDefFoundError: net.minecraftforge.eventbus.api.IEventBus`）
- 002-0a：补充**空壳** `IEventBus` 类型后，**层 c PASS**，失败点推进至**层 d**
  （`Mod constructor has unsupported argument`）
- 002-0b Step 1：`[F]` 层 d 的判定机制 = **精确匹配**（白名单不含 Forge 命名空间类型）
- 002-0b Step 2-A v1.1：把构造函数的参数类型描述符重写为
  `net.neoforged.bus.api.IEventBus` 后，**层 d PASS**，层 e 进入

**层 e 进入证据**（`[E]`）：

```
[E] 002-0b Step 2-A v1.1 Run T：
stdout 出现 "FORGE PROBE CONSTRUCTOR REACHED"
BUS = net.neoforged.bus.EventBus@a9b007c
（marker 命中 → 立即 STOP；marker 之后内容不进入分析）
```

**五层状态总览（截至 002-0b Step 2-A v1.1）**：

```
a: PASS      b: UNKNOWN      c: PASS      d: PASS      e: 已进入
```

**结论**：五层结构完整确立；a / c / d 有明确 PASS 证据；e 已进入但内容未观察；b 未独立观测。
**层 e 之后的生命周期（DeferredRegister / FMLCommonSetupEvent / ModLoadingContext / …）
属 002-0c 及以上范围。**

> **纪律**：五层必须**分称**，禁止把任一层的失败笼统写作"ABI 断层"。
> `CLEAR ≠ 修复成功`：002-0a 只证明"一个具体类型的缺失被补上"。

### 3.8.10 层 e 之后：G-1 条件下的历史生命周期基线（Spike-002-0c 回写，2026-10-07）

> 变量 = 0（复用 002-0b-2A v1.1 探针与前置）；观察范围 = marker 之后（取消"层 e 进入即 STOP"）。

**[E] 观测（PASS-002-0c-1）**：

- 前置生效：`javafml` 兼容 provider 被调用（`offered` / `REWRITE hits=1` / `post-check ctorCount=1 oldAbsent=true newPresent=true`）；
- 层 e：`FORGE PROBE CONSTRUCTOR REACHED` 命中，`BUS = net.neoforged.bus.EventBus@f2931ef`；
- 层 e 之后：日志未观测到 `FMLLoadCompleteEvent`；实际调用链到达 **Registry initialization**（`ModLoader.runInitTask` → `CommonModLoader.begin`），
  首个失败 = `RegistryManager.postNewRegistryEvent`（`RegistryManager.java:94`）抛 `IllegalStateException`：
  11 项 `neoforge:*` 注册表 "created but not registered to NewRegistryEvent"；
- 进程：`EXITED_EARLY` / exitCode 0 / 12.1s；`Done` 零命中；`FMLLoadCompleteEvent / FMLCommonSetupEvent /
  FMLDedicatedServerSetupEvent / FMLServerAboutToStartEvent / FMLServerStartingEvent / FMLServerStartedEvent`
  在 stdout / latest / debug 三份日志逐名零命中。

**[E] 复现性**：002-0b-2A v1.1（17:29）与 002-0c（18:05）两轮独立运行末段逐行一致（仅时间戳与 BUS 地址不同）。

**[F]/[E] 失败归因链（G-1）**：

- `[F]` `neoforge` 容器 entrypoint = `net.neoforged.neoforge.common.NeoForgeMod`（`@net.neoforged.fml.common.Mod`；
  构造器 `(IEventBus, Dist, ModContainer)`）；
- `[F]` `NeoForgeMod.<init>` → `NeoForgeRegistriesSetup.setup(bus)`；其 `NewRegistryEvent` 监听器注册的
  11 项注册表与失败清单逐一对应；
- `[E]` 兼容 provider 对 `neoforge` 容器返回 `entrypoints=[]`（stdout line 69；v1.1 同；control(rewrite=false) 亦同）；
- `[E]` 截至 002-0c，`NeoForge mod loading` 仅出现在无兼容 provider 的已检查运行（baseline / forgeprobe / spike001a-0），
  在当时已检查的兼容 provider 激活运行中零命中；002-0d 修复后的观测见 §3.8.11；
- `⇒ [D]` **G-1 认定**：Plan A 同名 `javafml` 覆盖使兼容 provider 同时接管 `neoforge` 系统容器并返回空
  entrypoints → NeoForge 核心 mod 未被构造 → 其注册表注册永不执行 → Registry initialization 失败。
  **属前置缺陷（v1.1 前置集），非 Forge↔NeoForge ABI 断层，且与 ForgeProbe 无关。**

**[F] 说明性注记（防误读）**：

- 崩溃报告 `JVM Flags: 2 total; -Xmx3G -Xms1G` 不含 `-D` 属性属报告显示行为；转换器强制要求
  `-Dforgecompat.2a.rewrite` 显式设置（未设置即抛异常），`rewriteEnabled=true` 反证其已生效。
- `javafml@null` 为 ModLoader 崩溃报告 callable 的显示值（`Package.getImplementationVersion()` 为空），
  功能 `version()="9999"`（冻结 jar 字节码）——"null" 不代表无版本或未覆盖。
- `build\forgecompat.jar`（`95B7398A…`）为 001b-1 源码重建（per-entry diff = 0），与归档
  `forgecompat-001b1.jar`（`14EFD54F…`）仅 zip 字节差异；002-0c 运行使用归档 v1.1 jar。

**处置**：`[D]` 002-0d = G-1 前置修复（决策侧说明书；修复后重跑零变量基线）；002-0e = 方向 2 探针 #1
（`MinecraftForge.EVENT_BUS.register(this)`，位置 = marker 之后）。详见《Spike-002-0c 裁判审查与裁定报告》。

### 3.8.11 G-1 修复与重建基线（Spike-002-0d 回写，2026-10-11）

`[E]` Run C 复现 G-1（Registry initialization 失败）；Run T 在 Forge entrypoints 为空时委托内置 javafml，出现 `NeoForge mod loading`，G-1 同链 FATAL 在 stdout/latest/debug 中均缺席，并到达 `Done (0.340s)! For help, type "help"`。meta 为 `DONE_STOPPED_GRACEFULLY / exitCode 0 / 24.7s`。`[D]` 裁判确认 VALID-002-0d-1，仅适用于本探针路径。

`[D]` 新基线 compat：`spike001a\compat-002-0d\build-treatment\forgecompat-002-0d.jar`，SHA-256 `4CD8195F7AA03B8B595F646CDEAC16454D03D5E3862B5E2F388C820CF2FA2FEF`，21180 字节、25 条目。原 probe 固定为 `A93A823C988BFD6A8F6EA132D189D9DE732B0E5951F4BF74980D51BB63374E22`；JVM 属性仍为 `-Dforgecompat.2a.rewrite=true`。

002-0c 的 PASS 保留，其事实基线限定为「存在 G-1 的历史输入下，FML 停在 Registry initialization」。该轮正式纳入分析的失败此前已见于 002-0b-2A v1.1 的未分析尾段；不得称为首次产生的新断层。002-0d 的 Run T 建立 G-1 修复后的生命周期基线，不推导一般 Forge API 兼容。

`[F]` `ModInfo.getLoader()` 经 ModFile.loaders 返回语言 provider map 选择的 compat；`[E]` neoforge 的 compat.loadMod 调用日志与该调用点闭合。委托 builtin.loadMod 不把 loader 归属改回 builtin。根据已核验的 validate 分组路径，不能声称内置 validate 因委托而恢复运行；该未修复事项继续登记。未额外测量委托返回后的对象同一性。

依据：《Spike-002-0d 执行报告》《Spike-002-0d 裁判审查与裁定报告》§三–六、《Spike-002-0d 执行报告勘误附记（2026-10-11）》；原始日志位于 `spike000\logs\spike002-0d-control` / `spike002-0d-treatment`，字节码证据位于 `spike002-0d-artifacts`。

### 3.8.12 首个API调用的链接与语义边界（002-0e 至 0h，2026-10-12）

`[E]` 002-0e K1：C到Done，T唯一新增`MinecraftForge.EVENT_BUS.register(this)`在ForgeProbe.java:14以NCDFE MinecraftForge失败；正式VALID-002-0e-1。未证register调用，P2未观测。

`[F]` 002-0f：Neo候选为`net.neoforged.neoforge.common.NeoForge.EVENT_BUS`，字段类型和register接口owner均属Neo命名空间；单改类owner不形成一致引用。限定该句需要getstatic owner+descriptor及invokeinterface owner三处耦合变换。模块声明不证跨层访问；总线语义等价未证。

`[E]` 002-0g K1：相同0e probe及同一新实验compat jar，只切换局部规则。C复现P1且dump同0e；T三操作数变化，实际栈进入`net.neoforged.bus.EventBus.register(EventBus.java:135)`，新终点为“has no @SubscribeEvent methods”的IllegalArgumentException（S1），PASS-002-0g-1。两轮均EXITED_EARLY/0/10.1s，无Done；exit0不替代判据。已证该路径链接及入口越过P1，**未证监听器登记成功**。

局部jar `F0D51E46533A135A9ECA0FA859805E04E9FBB22E8759B1A29524319E37B4C739` /22432B（compat-002-0g/build）只支持该限定实验规则；不是任意Mod或零API probe的正式兼容基线。0d有效生命周期基线继续保留。构造器打印BUS地址不代表全局EVENT_BUS对象同一性。

`[F]` 002-0h：Forge registerObject的候选map.isEmpty为true时offset124正常返回；Neo register零匹配计数条件抛异常。`[H]` 固定probe在原Forge实际环境的反射集合/初始化/调用返回仍未运行验证，不能由静态return直接认定S1可安全吞掉。下一步先补原Forge受控运行事实，再选语义适配；不通过添加Neo原生监听器制造兼容成功。

依据：0e独立裁定、0f/0g顺序复核报告与0h静态报告。按用户2026-10-12指示，0f起由root顺序执行和复核，**不称独立智能体审查**。完整日志、原始字节码、前后源码及哈希索引均保留，架构与手册版本不变。

### 3.8.13 固定零订阅形态的语义返回（002-0i / 0i-1 / 0j）

`[E]` 0i原Forge夹具因日志上下文缺SecureModuleClassLoader，在bus创建前STOP，未调用register；记录保留。0i-1只补冻结的既存securemodules2.2.21及声明unsafe依赖，复用同一两个class、原builder选项，在原Forge eventbus6.2.27中确认裸对象声明方法/非Object公共方法/SubscribeEvent方法均0，register前后marker及退出0齐备。仅证该夹具调用正常返回，不是假装原ForgeProbe完整原宿主加载。

`[D]` 据0h条件分支与0i-1运行，0j为固定ForgeProbe裸对象形态试验保真空返回：精确class名、直接父类Object、声明方法0、非Object公共方法0全部成立才return；否则直接Neo register，不吞异常、不补监听器。

`[E]` 0j K1同一probe/同一新jar只切换emptyRegistration开关。C复现S1且dump同0g T；T实际命中profile/空返回，S1缺席，Done(0.257s)、DONE_STOPPED_GRACEFULLY/0/22.7s，确认VALID-002-0j-1。T没有登记监听器，不能把这个结果写成事件订阅或分发兼容。

新限定jar `D724FB489A3A1F1180A5FBA09DFF2FECD01FBD6F8AE33EE727851AB211FA44AB` /23861B/26条目，路径compat-002-0j/build。0e probe D612…仍1848B/未改。新源码只修改transformer、增加空注册helper；原15内容文件不变。C/T字节码仅调用节点变化（接口→static两个参数）和2byte布局差异，四次打印/ctor参数/全局字段不变。

这是**固定无监听器探针的一条API路径生命周期通过**，不是任意Forge Mod兼容基线或通用IEventBus适配。真实订阅方法、注解/事件类型契约、优先级/取消/线程/实际事件送达仍未验证；应进入下一单变量探针，不泛化这个class名规则。


## 四、组件设计

### 4.1 ForgeModLocator

```java
public class ForgeModLocator implements IModFileCandidateLocator {
    @Override
    public void findCandidates(ILaunchContext context, IDiscoveryPipeline pipeline) {
        // Spike-001: 只认固定的 forgeprobe.jar
        Path probe = Paths.get("mods", "forgeprobe.jar");
        if (Files.exists(probe)) {
            pipeline.addPath(
                probe,
                ModFileDiscoveryAttributes.DEFAULT.withLocator(this),
                IncompatibleFileReporting.WARN_ALWAYS
            );
        }
    }
}
```

> **`ModFileDiscoveryAttributes.DEFAULT` 不包含 locator，必须通过 `.withLocator(this)` 显式设置**，否则 `ForgeModReader` 中的 `attributes.locator() instanceof ForgeModLocator` 永远为 false。此修正属于 Spike-001 实现细节，不影响 v1.1 架构冻结。

> **Spike-001 前禁止将固定路径 Locator 泛化为生产实现。** 正式版本必须根据 `ILaunchContext`、Mod 搜索路径及 `ModFileDiscoveryAttributes` 设计 Forge 候选发现策略。

### 4.2 ForgeModReader

```java
public class ForgeModReader implements IModFileReader {
    @Override
    public @Nullable IModFile read(JarContents jar, ModFileDiscoveryAttributes attributes) {
        if (!(attributes.locator() instanceof ForgeModLocator)) {
            return null;
        }
        SecureJar secureJar = SecureJar.from(jar, new ForgeJarMetadata(jar));
        return IModFile.create(secureJar, new ForgeModFileInfoParser());
    }
}
```

### 4.3 ForgeModFileInfoParser

```java
public class ForgeModFileInfoParser implements ModFileInfoParser {
    @Override
    public IModFileInfo build(IModFile file) throws InvalidModFileException {
        // 解析 mods.toml: modLoader, loaderVersion, modId, version, dependencies
        // 返回 IModFileInfo，getLoader() 必须返回兼容的 loader 名称
        return new ForgeModFileInfo(file, parsedData);
    }
}
```

**Spike-001b-1 起 `ForgeModFileInfo` 必须做到（修正后）**：

- `getMods()` 返回**非空**列表（含 1 个 `IModInfo`）
- `requiredLanguageLoaders()` 返回**非空** `LanguageSpec` 列表
  - `languageName = "javafml"`
  - `acceptedVersions = VersionRange("[52,)")`

> 若 `requiredLanguageLoaders()` 仍为空，则 `findLanguage` 不会被调用（Spike-001b-0 §3.1 已验证）。

### 4.4 ForgeLanguageLoader（待 Spike-001b 决定具体方案）

```java
public class ForgeLanguageLoader implements IModLanguageLoader {
    @Override public String name() { return /* Plan A: "javafml" / Plan B: "forge_compat" */; }
    @Override public String version() { return /* 待 Spike-001b 决定 */; }
    @Override
    public ModContainer loadMod(IModInfo info, ModFileScanData scanData, ModuleLayer layer) {
        List<String> entrypoints = findForgeModEntrypoints(scanData);
        return new FMLModContainer(info, entrypoints, scanData, layer);
        // 注意：FMLModContainer 能否直接复用需 Spike-001c 验证
    }
}
```

**`findForgeModEntrypoints()` 必须严格依据 Forge 1.21.1 `@Mod` scan data 格式实现，不得凭历史 API 记忆假设。**


## 五、类加载模型

| 模式 | 默认 | 类型空间 | 适用场景 |
|---|---|---|---|
| **SHARED** | ✅ | 与 Game Layer 共享 | 绝大多数 Forge 模组 |
| **ISOLATED** | 可选 | 独立 `URLClassLoader` + 明确 Parent Delegation Policy + API 白名单 + 禁止冲突库向 Parent 泄漏 | 携带冲突库的模组 |
| **BRIDGED** | 实验 | 双 ClassLoader + Adapter 对象转换 | 跨隔离模组类型交互 |

**ISOLATED 实现约束**：不能在 `ModuleLayer` 中插入子层，必须使用模块系统之外的 `URLClassLoader` 旁路。真正决定能否看到类的核心机制是 **ClassLoader delegation**，不是 JPMS exports。**禁止使用"导出 Minecraft/NeoForge/Shim API"的表述，应描述为"Parent Delegation Policy + API 白名单"。**[D]

**BRIDGED 的核心问题**：ClassLoader 隔离真正麻烦的不是 `A` 找不到 `B`，而是 `A` 的 `com.example.Foo` ≠ `B` 的 `com.example.Foo`——即使二者字节内容完全一样。[H]


## 六、映射与转换策略（延后）

**Spike-001 至 Spike-005 禁止引入：ASM、Mapping、Mixin、Registry、Event、Isolation。**

### 6.1 命名空间处理

```java
public interface MappingProvider {
    MappingNamespace sourceNamespace();
    MappingNamespace targetNamespace();
    Optional<MethodMapping> mapMethod(String owner, String name, String descriptor);
    Optional<FieldMapping> mapField(String owner, String name, String descriptor);
}
```

- **NeoForge 1.21.1 游戏运行时以 Mojang 官方命名空间为主要目标；不能因此假设 Forge 模组、构建工具链或附加转换资源中不存在 SRG 名称。**[H]
- Forge 模组可能基于 SRG、Mojmap 或混合状态。
- 映射键使用 **name + descriptor** 组合，避免静默失败。
- `NamespaceDetector` 置信度 ≥ 0.95 自动采用，0.7-0.95 警告并尝试，< 0.7 明确失败。[D]

### 6.2 转换引擎

```java
public interface ClassTransformer {
    TransformResult transform(ClassContext context, byte[] input);
}
```

| 转换类型 | 执行模式 |
|---|---|
| 简单重映射 | `ClassRemapper` / `SimpleRemapper` |
| 复杂补丁 | ASM Tree API |
| 无操作 | 原字节码 |

**Tree API 仅用于需要搜索指令、修改控制流、根据上下文决定插入位置的场景。**[D]


## 七、事件与注册表兼容

### 7.1 事件总线

```java
public final class ForgeEventBusShim implements IEventBus {
    private final IEventBus neoForgeBus;
    @Override
    public void register(Object target) {
        // 运行时扫描 @SubscribeEvent，映射事件类型，注册到 NeoForge 总线
    }
}
```

- 优先运行时 Shim，而非大量修改注解。[D]
- 事件类包名重映射（`net.minecraftforge.event` → `net.neoforged.neoforge.event`）仍需字节码层完成。[H]

### 7.2 注册表

- `DeferredRegister` Shim 内部调用 NeoForge 的注册机制。[D]
- `RegistryObject` Shim 包装 NeoForge 的 `DeferredHolder`。[D]
- 注册时机必须与 NeoForge 生命周期对齐。[D]

### 7.3 兼容对象模型（长期隐患）

进入大型 Forge Mod 后，会逐渐出现 `Forge Event / Registry / Capability / Network / Config / Dist / ModLoadingContext / ResourceLocation / ItemStack / Entity` 等。最危险的并非静态方法映射，而是 **对象身份 + 泛型 + 回调 + 生命周期**。这需要：

```
Forge object → adapter → Neo object → callback → Forge object
```

**BRIDGED 模式就是为这一层准备的，但当前 Spike 阶段绝对不应提前解决。**[D] 保持 Spike 001~005 禁止 ASM/Mapping/Registry/Event/Isolation 不动。


## 八、未来 Fabric 适配预留

### 8.1 插件化转换器

```java
public interface TransformerPlugin {
    String name();
    void registerJarTransformers(TransformerRegistrar registrar, TransformerContext context);
    void registerPatches(PatchRegistrar registrar, TransformerContext context);
}
```

通过 `META-INF/services/` 注册。未来新增 `FabricTransformerPlugin`，复用现有基础设施。[D]

### 8.2 Shim 注册

所有 Shim 通过 `ServiceLoader` 注册，Forge 与 Fabric 实现彼此隔离。[D]

### 8.3 转换器排序

支持 `OrderingHint.EARLY/LATE` 和 `registerAfter`，确保 Forge 与 Fabric 管线独立排序。[D]


## 九、诊断系统

必须结构化报告以下错误：

| 错误 | 报告内容 |
|---|---|
| `NoSuchMethodError` | Mod ID、阶段、类、引用、预期映射、转换器、建议操作 |
| `ClassNotFoundException` | 缺失类、来源、是否被隔离 |
| `VerifyError` | 字节码转换阶段、转换器名称 |
| `MixinApplyError` | Mixin 目标、优先级、冲突补丁 |
| `IllegalAccessError` | 访问目标、AT/AW 状态 |

**诊断输出应包含 Compatibility Profile**：

```
Create
 ├─ Registry    ✓
 ├─ Event       ✓
 ├─ Network     ✓
 ├─ Mixin       ⚠ (3 patches applied)
 └─ Rendering   ✗ (not yet supported)
```

### 9.1 已观测的诊断约束（Spike-001b-1 回写）

- `[F]` `fml.modloadingissue.language.missingversion` 是 **ERROR 级 issue**，
  会导致**启动中止**（`ModLoadingCrashException: Mod loading has failed`），
  **不是**"WARNING + 跳过"（与 Spike-000 的 `minecraft_forge` 形态相反）。
- `[D]` **诊断系统设计约束**：`findLanguage` 类失败点的**原始异常栈被 NeoForge 丢弃** ——
  `ModValidator.validateLanguages` 捕获 `ModLoadingException` 后只执行 `issues.addAll(e.getIssues())`
  并丢弃异常对象；崩溃报告中亦为 `Exception message: <No associated exception found>`。
  因此结构化诊断**只能依赖日志文案分支判定（见 §3.1）**，**不能依赖 stack trace**。


## 十、测试矩阵

| 测试 | 验证目标 | 依赖 SPI | 预期 |
|---|---|---|---|
| **T0** | **Compatibility Core 自身身份验证** | `META-INF/services/` 白名单（ServiceLoader） | **provider 可见、来源 JAR、实际 Layer、模块身份** |
| T1 | 兼容层被 EarlyServiceDiscovery 加载 | `IModFileCandidateLocator` | 日志输出初始化 |
| T2 | 发现 Forge JAR | Locator + Pipeline | 报告候选 |
| T3 | 元数据解析 | `IModFileReader` + Parser | 输出 modId |
| T4 | Lifecycle 闭环 | `FMLModContainer` | 构造函数被调用 |
| T5 | 字节码转换 | `cpw.mods.modlauncher.api.ITransformationService#transformers()` | SRG / 描述符重映射 |
| T6 | 事件桥接 | EventBus Shim | 监听器被调用 |
| T7 | 隔离加载 | `URLClassLoader` 旁路 | 不污染 Game Layer |

> `[F]` FML 4.0.45 中不存在 `ClassProcessor` / `ClassProcessorProvider` 类。
> 第三方类转换接入点为 `ITransformationService#transformers()`（通道 A，见 §3.6.1）；
> 通道 B `ILaunchPluginService` 对第三方 `mods/` JAR 静态不可达。

> **T0 不假设 `FMLModType` 或 `Automatic-Module-Name` 是必要条件；仅记录其存在与否及对运行结果的影响。**
> `FMLModType` 与 `Automatic-Module-Name` 是 T0 的**观测变量**，不是 T0 的**依赖**。

**T0 的时间关系**：

> **T0 为独立的基础设施 Smoke Test，不属于 Spike-000 的执行前置条件。Spike-000 必须在无 Compatibility Core 的条件下首先完成；待 Compatibility Core 建立后，T0 纳入所有后续 Spike 的回归测试。**[D]

时序：

```
Spike-000
    ↓
确定 Native Forge JAR 第一失败点
    ↓
001a / 001b / 001c
    ↓
建立 Compatibility Core
    ↓
T0 成为持续性基础设施测试
```

**T0 的必要性**：整个系统存在两层身份——**SERVICE/BOOT 层族（Compatibility Core）**与 **Game Layer（Forge Mods）**。如果出现 `ServiceLoader` 找不到 `ForgeModReader`，必须首先区分是 **provider 没注册 / 注册了但进入错误 Layer / 所依赖类不在该 Layer 可见范围**。T0 能极大降低排查成本。


## 十一、Spike 开发路线（冻结版）

| Spike | 核心问题 | PASS 条件 | 不处理 |
|---|---|---|---|
| **000** | Native Forge JAR 在哪一阶段失败？ | 完整记录 Discovery → Metadata → Validation → Scan → LanguageLoader → Module → Container → Constructor 的实际失败链 | 任何兼容代码 |
| **001a** | Forge JAR 能否由自定义 Reader 接管并进入 ModValidator？ | Forge JAR → `IModFileReader` → `IModFile` → `ModValidator` 输入 | javafml provider、构造函数、Game Layer 注入 |
| **001b** | javafml 如何解析、验证与选择？ | 1. `modLoader` / `loaderVersion` → `LanguageSpec` 链已确认；2. `IModLanguageLoader` provider 来源机制已确认；3. 实际 provider Layer 已观测；4. 同名 provider 覆盖行为已观测；5. `version()` 与 `loaderVersion` 的比较规则及实际结果已确认；6. 不预设 Plan A / Plan B | ModuleLayer、构造函数 |
| **001c** | FMLModContainer 能否消费 Forge @Mod entrypoint？ | 1. Forge `@Mod` entrypoint 被正确识别；2. `FMLModContainer` 成功获得 entrypoint；3. constructor resolution 的实际行为已记录；4. 若构造器因 Forge `IEventBus` / `FMLJavaModLoadingContext` 类型不匹配失败，完整记录其 ABI 断层。**不要求本 Spike 成功构造 ForgeProbe。** | FMLJavaModLoadingContext、事件、注册表 |
| **002** | Forge constructor context / ABI compatibility | Forge constructor 所需上下文类型的 ABI 兼容路径已被证明；`FMLJavaModLoadingContext` / Forge `IEventBus` 的 Shim 接管点明确；不要求 Registry/Event 功能本身在本 Spike 中完成 | Registry/Event |
| **003** | Registry 是否兼容？ | `DeferredRegister` 注册成功 | Mapping |
| **004** | Event 是否兼容？ | `@SubscribeEvent` 被触发 | Mapping |
| **005** | Namespace Mapping 是否可行？ | SRG → Mojmap 引用正确重映射 | 其它兼容层 |
| **006** | 真实 Forge Mod | JEI/Create 基本可用 | 全功能 |

**Spike-001b 子链实际状态**：

| 子 Spike | 问题 | 结果 |
|---|---|---|
| 001b-0 | `modLoader` → `LanguageSpec` → `findLanguage` → provider 的源码路径定位 | ✅ 完成（源码分析） |
| 001b-1 | 四节点打开：`getMods` → `requiredLanguageLoaders` → `identifyLanguage` → `findLanguage` | ✅ **PASS**（进入分支②，`[52,)` vs `4.0.45` FAIL） |
| 001b-3 | provider 实际 ModLauncher Layer + 同名覆盖行为 | ✅ **PASS**（内置 = `BOOT`；覆盖 = **情况 A**） |
| 001b-4 | `[52,)` vs `4.0.45` 的解析/校验结果 | ⏭ 已被 001b-1 覆盖，不再单独执行 |
| 001b-5 | `ModSorter` 隐式类型契约 + 正常 Java 路径可行性 | ✅ **PASS**（契约两层；正常路径 = **YES**） |
| 001b-2 | `findLanguage` 成功返回后的下一步行为 | ⏭ 已被 001b-6 / 001c 链覆盖 |
| 001b-6 | 验证 §3.8.4 的 `[H]`：走真实 `ModFileInfo` 是否一次越过两道 cast | ✅ **PASS**（两道 cast 均 PASS；`loadMod` 被真实调用） |

**Spike-001c 子链实际状态**：

| 子 Spike | 问题 | 结果 |
|---|---|---|
| 001c-0 | 只读 dump `ModFileScanData`，判定 Forge `@Mod` 是否可得（五层判据 A–E） | ✅ **PASS**（A–E 全 PASS） |
| 001c-1 | 用真实 entrypoint 数据构造 `FMLModContainer` | ✅ **PASS**（构造正常返回；`constructMod()` 未进入） |
| 001c-2 | ForgeProbe 实例化 / Forge `@Mod` constructor ABI | ✅ **完成（VALID / 层 c ABI Break）**；由 002-0a / 002-0b 接续 |

**Spike 子链状态（截至 Spike-002-0j）**：

```
Spike-000              ✅ 完成
Spike-001a             ✅ 完成
Spike-001b-0           ✅ 完成
Spike-001b-1           ✅ 完成
Spike-001b-3           ✅ 完成
Spike-001b-4           ⏭ 已被 001b-1 覆盖
Spike-001b-5           ✅ 完成
Spike-001b-6           ✅ 完成
Spike-001c-0           ✅ 完成
Spike-001c-1           ✅ 完成
Spike-001c-2           ✅ 完成（VALID / 层 c ABI Break）
Spike-002-0a           ✅ 完成（层 c PASS；层 d FAIL）
Spike-002-0b Step 1    ✅ 完成（白名单 = 精确匹配）
Spike-002-0b-1         ✅ 完成（VALID / Chain Not Fully Traceable）
Spike-002-0b-1b        ✅ 完成（PASS / 通道 A 可达）
Spike-002-0b Step 2-A v1     ⛔ STOP（前置配置缺口）
Spike-002-0b Step 2-A v1.1   ✅ 完成（PASS：层 d 解除 + 层 e 进入）
Spike-002-0c           ✅ 完成（PASS-002-0c-1：G-1 条件下的历史基线）
Spike-002-0d           ✅ 完成（VALID-002-0d-1：G-1 清零，Run T 到达 Done）
Spike-002-0e           ✅ 完成（VALID：P1首失败坐标）
Spike-002-0f           ✅ 完成（PASS：静态契约）
Spike-002-0g           ✅ 完成（PASS：进入register，S1）
Spike-002-0h           ✅ 完成（PASS：条件静态语义，原Forge运行未证）
Spike-002-0i           ⛔ STOP（夹具前置缺件）
Spike-002-0i-1         ✅ 完成（PASS：原Forge裸对象register返回）
Spike-002-0j           ✅ 完成（VALID：限定零订阅API路径Done）
```

> 母任务上下文见《Spike_002-0b.md 母说明书》。

### 单因子实验链

```
001c 发现 ABI 断层
   ↓
002   修 ABI
   ↓
003   Registry
   ↓
004   Event
   ↓
005   Mapping
```

### Spike-001c 的设计理由

`FMLModContainer` 白名单中的 `IEventBus` 是 `net.neoforged.bus.api.IEventBus`。真正的 Forge 1.21.1 Probe 若写成 `import net.minecraftforge.eventbus.api.IEventBus`，其参数类型与 NeoForge 的 `IEventBus` **不是同一个 Java 类型**。001c 的目标是**证明问题在哪里**，而非**必须成功**；`002` 才负责解决 ABI 兼容。[D]

### Spike-001b 决策树

```
loaderVersion="[52,)"
        │
        ▼
什么时候检查？
        │
   ┌────┴────┐
   ▼         ▼
findLanguage  loadMod
   │         │
   └────┬────┘
        ▼
最终需要什么 version()
        │
   ┌────┴────┐
   ▼         ▼
Plan A:    Plan B:
javafml    forge_compat
+ 兼容      + 兼容
provider    provider
```

**不在 Spike-001b 之前预设 Plan A 或 Plan B。**[D]


## 十二、风险表

| 编号 | 风险 | 验证重点 | 状态 |
|---|---|---|---|
| R-GAME-LAYER | Game Layer 构建时序 | `SecureJar` → `ModFile` → `LoadingModList` → `getModResources()` → Game Layer → `findModule()` | 🔴 高风险 |
| R-LOADER-NAME | Loader Name 重写 | 同名 provider 策略；Plan A/B 选择 | 🔴 高风险 |
| R-MODULE-IDENTITY | 四重身份一致性 | `SecureJar.name()` / `IModInfo.moduleName()` / `ModuleDescriptor.name()` / `GameLayer.findModule()` | 🟡 待验证 |
| R-METADATA | `ForgeJarMetadata` 的具体实现 | 模块名、包列表、服务提供者 | 🟡 待验证 |
| R-SCAN-DATA | Forge 1.21.1 @Mod scan data 的确切结构 | entrypoint 生成方式 | 🟡 待验证 |
| R-CONTAINER-ABI | `FMLModContainer` 是否可直接复用 | 构造函数注入语义；Forge `IEventBus` ABI 断层 | 🟡 待验证 |
| R-CLASSPROCESSOR | 第三方类转换接入点 | **已确认**：`ITransformationService#transformers()`（通道 A，SERVICE 层可达）；通道 B 对第三方静态不可达 | 🟢 已确认（002-0b-1b） |
| R-OBJECT-MODEL | Forge/Neo 对象身份与泛型兼容 | 长期隐患，BRIDGED 模式 | 🟢 暂不处理 |


## 十三、开发环境与依赖（精确版本冻结）

| 组件 | 版本 |
|---|---|
| Minecraft | **1.21.1**（精确） |
| NeoForge | **21.1.256**（精确） |
| FancyModLoader | **4.0.45**（从 `--fml.fmlVersion` 参数或 `fancymodloader/loader/<ver>/` 目录读取） |
| Java | **21** |
| **编译依赖 Forge** | **1.21.1-52.1.0**（精确） |

> **21.1.256 满足精确 patch 级冻结要求。** FancyModLoader 的实际版本必须从 `--fml.fmlVersion` 参数或工件目录提取，不得凭推测填写。**Forge 版本与 NeoForge 版本是两个独立字段，不能混为一谈。** Forge 版本用于编译依赖，NeoForge 版本用于运行时宿主。[D]

> **Forge 1.21.1-52.1.0 是 `52.1` 分支的 RB 1（Release Build 1），是该分支的起点版本。** 后续 52.1.1 到 52.1.16 均为增量修补。选择分支起点版本可避免后期 patch 引入的无关行为干扰，同时与 `mods.toml` 中 `loaderVersion="[52,)"` 的语义一致。**不得因为官网有更新的 52.1.16 就改用最新版。** Spike-000 的目标是定位失败阶段，不是测试"最新 Forge 是否能工作"。[D]

> 实验环境必须做成**不可变基线**。在 Spike-001b 完成前，不得修改上述任何版本。

> `[E]` **加入非空 `getMods()` 后**（Spike-001b-1），ForgeProbe **首次出现在发现阶段的 Mod List**：
> `Forge Probe 1.0.0 (forgeprobe)`；`ModValidator/SCAN: Found 4 mod files with 3 mods`
> （对照 Spike-001a-0 为 `2 mods`）。


## 十四、Spike-000 执行原则（历史记录）

> **Spike-000 已于 2026-10-07 完成。** 本节保留其执行原则，作为后续 Spike 的方法论参考。

> **Spike-000 的完整执行流程、结构化输出格式、纪律约束，见《Spike-000 执行任务说明书》。** 本节仅保留手册层面的核心原则。

### 14.1 Probe 设计（最小形态）

`forgeprobe.jar`：

- `META-INF/mods.toml`（`modLoader="javafml"`, `loaderVersion="[52,)"`, `modId="forgeprobe"`）
- `META-INF/MANIFEST.MF`（建议声明 `Automatic-Module-Name: forgeprobe`，**仅作为实验辅助条件，不构成兼容层最终实现要求**）
- `com/example/forgeprobe/ForgeProbe.class`（`@Mod("forgeprobe")` + `public ForgeProbe(IEventBus bus)`，**使用 Forge 命名空间的 IEventBus**）

**编译依赖必须为 Forge 1.21.1-52.1.0，禁止使用 NeoForge 依赖。**

**`ForgeProbe` 是"故意保持 Forge 原生 ABI"的测试样本，不是"最小 NeoForge 可兼容样本"。** 不要因看到 `ForgeProbe(IEventBus)` 就误以为这个 `IEventBus` 应该是 NeoForge 的。[D]

### 14.2 mods.toml 本身就是重要观察点

NeoForge 从 20.5 起明确使用 `META-INF/neoforge.mods.toml`，而 Forge 使用 `META-INF/mods.toml`。[F]

**实测观察**：Spike-000 已确认 `META-INF/mods.toml` 触发 `IncompatibleModReason.MINECRAFT_FORGE`，在 Discovery 阶段被**静默跳过**（WARNING + `ModLoadingIssue`），而非在 Metadata 阶段失败。[E]

### 14.3 执行原则

1. 放入 NeoForge 21.1.256 的 `mods/` 目录。
2. 启动游戏，**完整记录从 Discovery 到失败的日志**。
3. **不修复任何错误。**
4. 先记录：**错误 → 调用栈 → 源码入口 → 对应流水线阶段 → 为什么原生 NeoForge 无法继续**。

### 14.4 失败链记录表

| 阶段 | 预期入口 | 001a-0 | 001b-1 | 001b-5 |
|---|---|---|---|---|
| Discovery | `IModFileCandidateLocator` / Pipeline | 进入 | 进入 | 进入 |
| Metadata | `IModFileReader` / Parser | 未进入 | 进入 | 进入 |
| Validation | `ModValidator` | 未进入 | 进入 | 进入 |
| Scan | `BackgroundScanHandler` | 未进入 | UNKNOWN | UNKNOWN |
| LanguageLoader | `LanguageProviderLoader` | 未进入 | 进入（fail） | 进入（pass：9999 覆盖） |
| ModSorter | `ModSorter.sort` | 未进入 | 未进入 | 进入（fail：`LoadingModList` cast） |
| Module | Game Layer | 未进入 | 未进入 | 未进入 |
| Container | `FMLModContainer` | 未进入 | 未进入 | 未进入 |
| Constructor | ForgeProbe ctor | 未进入 | 未进入 | 未进入 |

**按五层失败点结构续记**（Spike-001c-2 / 002-0a / 002-0b Step 1 / 002-0b-1b / 002-0b-2A v1.1）：

| 阶段 | 001c-2 | 002-0a | 002-0b S1 | 002-0b-1b | 002-0b-2A v1.1 |
|---|---|---|---|---|---|
| Discovery / Metadata / Validation | YES | YES | YES | YES | YES |
| Scan | YES | YES | YES | YES | YES |
| LanguageLoader | YES | YES | YES | YES | YES |
| FMLModContainer 构造 | PASS | PASS | PASS | PASS | PASS |
| `constructMod` 调用 | YES | YES | YES | YES | YES |
| entrypoint 类加载（层 a） | PASS | PASS | PASS | PASS | PASS |
| `@Mod` 注解解释（层 b） | UNKNOWN | UNKNOWN | UNKNOWN | UNKNOWN | UNKNOWN |
| 构造函数签名解析（层 c） | **FAIL** | **PASS** | PASS | PASS | PASS |
| 参数注入（层 d） | NOT REACHED | **FAIL** | FAIL（精确匹配机制） | FAIL | **PASS** |
| 构造函数体（层 e） | NOT REACHED | NOT REACHED | NOT REACHED | NOT REACHED | **进入（marker）** |

**offered / REWRITE 运行时观测**（002-0b-2A v1.1）：
Run T（treatment）：`offered = 1`（`com/example/forgeprobe/ForgeProbe`）、`REWRITE hits = 1`；
Run C（control）：`offered = 1`、`REWRITE = 0`。

**G-1 修复 K1 续记（002-0d）**：

| 观测 | Run C | Run T |
|---|---|---|
| ForgeProbe marker / BUS | 命中 / 命中 | 命中 / 命中 |
| 委托 builtin 日志 | 无 | 命中（neoforge） |
| `NeoForge mod loading` | 无 | 命中 |
| Registry initialization 同链 FATAL | 命中 | stdout/latest/debug 均缺席 |
| Done | 无 | 命中 |
| meta status / exitCode / duration | EXITED_EARLY / 0 / 12.1s | DONE_STOPPED_GRACEFULLY / 0 / 24.7s |

本表仅列实测，不给日志未独立观测的生命周期事件追加 PASS 标签。

> **注意**：不要因为日志里出现了某个类名，就直接认定"已经进入该阶段"。应根据实际调用链/异常栈确认。
> **class loaded ≠ method invoked ≠ stage entered ≠ operation succeeded**
>
> 对 `ModSorter` 尤其如此：**被调用 ≠ 成功 ≠ Module 阶段进入**。
> Spike-001b-3 亦到达 `ModSorter` 并在 `(ModInfo)` 强转处失败（未单列一列）。


## 附录 A：事实标签体系

| 标签 | 含义 | 示例 |
|---|---|---|
| **[F]** | Source Fact | `[F] IModFileReader is resolved through ServiceLoader` |
| **[E]** | Experimental Fact | `[E] Forge mods.toml reaches / does not reach metadata parser` |
| **[H]** | Hypothesis | `[H] Forge @Mod may be present in ScanData but not interpreted as entrypoint` |
| **[D]** | Design Decision | `[D] Compatibility Core 采用已验证的 Service Provider 注入路径` |
| **[F-j]** | **JDK Spec Fact** —— 由 JDK 规范保证的事实，**非本项目字节码证明** | `[F-j] Class.forName(Module,String) 使用 module.getClassLoader()` |

**`[F-j]` 使用规则**：

- 仅用于跨越 JDK / VM 边界的连接点
- **不得**跨越边界反推实现细节
- 若 JDK 版本升级，`[F-j]` 项需**重新核验**
- 本项目目前**仅** ④→⑤ 的 `Class.forName(Module,String)` 一跳使用该标签

**证据链升级路径**：

```
[H] 理论假设
    ↓ 实验
[E] 真实观察
    ↓ 源码核对
[F] 源码事实
    ↓ 架构决策
[D] Design Decision
```

**核心纪律**：

- **一个成功案例不构成必要条件。** 必要条件必须由负对照实验确认。
- 未由字节码直接支撑的推断一律进入 `[H]`，不得记为 `[F]`。
- 一个已验证路径上的结论，不得外推为系统级事实。

**证据归档与交接纪律（002-0d 收尾）**：

- 构建时保存完整实际命令参数、工具版本、退出码和输出；模板不能冒充历史逐字命令。002-0d 的 control/treatment 命令记录限制详见勘误附记。
- harness 每轮以带哈希的快照作机械 diff 基线；新快照不追溯填补旧 diff 缺口。
- 硬编码共享 dump 每轮立即另存并记录哈希，下一轮覆盖前保全；源码修改前后全量归档。
- 实践报告由独立裁判审查后回写；交接带任务标识、明确回执、冻结文件指纹。应急代行须披露，不把平台错误原因或日志捕获乱序原因记成已证明事实。

2026-10-12用户变更工作方式：不使用子代理，改为同一智能体按执行、复核、裁定、下一说明书顺序处理。此后注明顺序复核而不冒称独立审查；事实标签、冻结输入与单因子纪律保持。旧独立裁定原样保留。


## 附录 B：最终验收清单

| 项目 | 状态 |
|---|---|
| 架构边界 | ✅ 冻结 |
| SPI 接入机制 | ✅ |
| SPI 完整分层 | ✅（Reader/Locator = SERVICE [E]；LanguageLoader 内置 = **BOOT** [E]；兼容层 provider = SERVICE [E]） |
| SERVICE/BOOT 层族 / Game Layer | 🟡（SERVICE promotion 已验证；Forge Mod → Game Layer module identity 待后续 Spike） |
| Discovery → Container 模型 | ✅ |
| Class scanning / entrypoint 分离 | ✅ |
| Module identity | 🟡 待 001c 前置验证 |
| IModFileInfo / IModInfo 具体类型契约 | ✅ 两层均已定型（§3.8）；**001b-6 实测「一次越过」成立** |
| Forge 注解在 `ModFileScanData` 中可得 | ✅ 001c-0 实测（五层判据 A–E 全 PASS） |
| `FMLModContainer` 可构造性 | ✅ 001c-1 实测（构造函数 public，正常返回） |
| 层 c / 层 d 分离 | ✅ 已证（001c-2 FAIL → 002-0a FAIL 于层 d） |
| 白名单判定机制 | ✅ 精确匹配（`Map.get` + `Set.contains`） |
| 第三方类转换通道 A 可达性 | ✅ 可达（SERVICE 层） |
| 第三方类转换通道 B 可达性 | ✅ 静态不可达（BOOT 层发现源） |
| ④→⑤→⑥ 静态链 | ✅ 同一静态路径 |
| ForgeProbe 静态目标资格 | ✅ YES |
| 层 a / c / d 五层结构 | ✅ 完整确立 |
| 层 d 解除 | ✅ 002-0b-2A v1.1 PASS |
| 层 e 进入 | ✅ marker 命中 |
| 通道 A 可达性（运行时） | ✅ offered + REWRITE 确认（002-0b-2A v1.1） |
| ④→⑤→⑥ 运行时穿过 | ✅ 002-0b-2A v1.1 |
| 参数注入 ABI 完全兼容 | ⏸ 仅本探针路径 |
| 单语句全局EVENT_BUS链接 | ✅ 002-0g限定规则越过P1，进入Neo register |
| 固定零订阅对象空注册返回 | ✅ 0i-1原Forge夹具返回；0j限定保真路径Done |
| 有订阅方法对象/事件分发兼容 | ⏸ 未验证；不能由空注册返回推导 |
| 层 e 之后的生命周期 | ✅ 002-0c：G-1 历史终点；002-0d Run T：G-1 清零并到达 Done（仅本探针路径） |
| Constructor ABI 分析 | ✅ |
| Shared / Isolated / Bridged | ✅ |
| Mapping 延后 | ✅ |
| Registry/Event 延后 | ✅ |
| Fabric 预留 | ✅ |
| Diagnostics | ✅ |
| T0 时序 | ✅ |
| Spike 单因子链 | ✅ |
| 事实标签体系 | ✅ |
| Spike-000 Probe | ✅ |
| 4.1 attributes 传递 | ⚠️ 已修正 |
| 精确 NeoForge 版本 | ✅ 21.1.256 |
| 精确 Forge 编译依赖 | ✅ 52.1.0 |
| 精确 FML 版本 | ✅ 4.0.45 |


## 附录 C：配套文档索引

| 文档 | 用途 |
|---|---|
| **《Forge → NeoForge 兼容层开发手册 v1.1》**（本文档） | 架构冻结基线，所有设计决策的事实来源 |
| **《Spike-000 执行任务说明书》** | 可直接交给 Codex 的可执行任务，包含完整流程、纪律约束、结构化输出格式 |
| **《Spike-000 实验报告》** | Spike-000 实验结论（Discovery 红线 = `MINECRAFT_FORGE` 拦截），[E]/[F]/[H]/[D] 全量标签 |
| **《Spike-000 正式实验结论与 Spike-001a 执行定义》**（`Spike_001a.md`） | Spike-000 结论冻结 + Spike-001a 四步拆分 + 001a 任务说明书 |
| **《Spike-001a-0 实验报告》** | 001a-0 结论（provider 进入 SERVICE 层）、`ModDirTransformerDiscoverer` 机制定位 |
| **《Spike-001b-0 源码分析报告》** | `modLoader` → `LanguageSpec` → `findLanguage` → provider 完整调用链、`VersionSupportMatrix` 无 override 路径 |
| **《Spike-001b 任务说明书与 v1.1 标签修正》**（`Spike_001b.md`） | v1.1 标签修正汇总 + Spike-001b 子 Spike 拆分（001b-0 ~ 001b-5） |
| **《Spike-001b-1 执行任务说明书》**（独立文档） | 001b-1 可直接交付 Codex 的可执行任务 |
| **《Spike-001b-1 实验报告》** | 001b-1 四节点打开结论（`missingversion`、分支② 判定、原始栈不可恢复） |
| **《Spike-001b-3 执行任务说明书》**（`Spike_001b-3.md`） | Layer 概念修正版任务书（问题 A / B、情况 A/B/C 判定、撤销要求） |
| **《Spike-001b-3 实验报告》** | 内置 provider = **BOOT**、兼容 provider = SERVICE；同名覆盖 = **情况 A**（两次运行一致） |
| **《Spike-001b-5 执行任务说明书》** | ModSorter 隐式类型契约取证任务书（A/B/C 路径分级、清理要求） |
| **《Spike-001b-5 实验报告》** | 两层具体类型契约、`NORMAL JAVA PATH = YES`、第三层新 CCE（`LoadingModList`） |
| **《Spike-001b-6 执行任务说明书》**（`Spike_001b-6.md`） | 两道 checkcast 验证任务书 |
| **《Spike-001b-6 实验报告》** | 两道 cast 均 PASS；`loadMod` 被真实调用；一次越过两道 cast 成立 |
| **《Spike-001c-0 执行任务说明书》**（`Spike_001c-0.md`） | 只读 dump `ModFileScanData`（五层判据 A–E） |
| **《Spike-001c-0 实验报告》** | 五层判据 A–E 全 PASS；NeoForge scanner 扫到 Forge `@Mod`，metadata 正确 |
| **《Spike-001c-1 执行任务说明书》**（`Spike_001c-1.md`） | 构造 `FMLModContainer`（含边界与观测桩要求） |
| **《Spike-001c-1 实验报告》** | entrypoint 提取 PASS；`FMLModContainer` 构造 PASS；`constructMod()` 未进入 |
| **《Spike-001c-2 执行任务说明书》**（`Spike_001c-2.md`） | 进入 `constructMod()` 并做五层失败点分类 |
| **《Spike-001c-2 实验报告》** | VALID / 层 c ABI Break（`NoClassDefFoundError: net.minecraftforge…IEventBus`） |
| **《Spike-002-0a 执行任务说明书》**（`Spike_002-0a.md`） | 空壳 `IEventBus` 类型存在性探针 |
| **《Spike-002-0a 实验报告》** | CLEAR：层 c PASS、层 d FAIL（两道独立的门） |
| **《Spike-002-0b Step 1 取证报告》** | `[F]` 白名单判定 = 精确匹配（含 raw bytecode 四条） |
| **《Spike-002-0b-1 取证报告》** | `[F]` 候选通道 2 条；`ClassProcessor` 不存在 |
| **《Spike-002-0b-1b 取证报告》** | `[F]` ④→⑤→⑥ 同一静态路径；实际 `T` = `ClassNode`；通道 A 可达 |
| **《Spike-002-0b Step 2-A 执行报告（v1，STOP）》**（`Spike-002-0b Step2-A 执行报告.md`） | v1 冻结版执行报告：STOP / 前置配置缺口（语言门失败；未达层 c/d） |
| **《Spike-002-0b Step2-A v1.1 执行报告（PASS）》**（`Spike-002-0b Step2-A v1.1 执行报告.md`） | v1.1 冻结版执行报告：**PASS —— 层 d 解除 + 层 e 进入**（marker 命中后 STOP） |
| **《Spike_002-0b.md 母说明书》** | 母任务背景、拆分理由、子阶段关系图、门槛与教训归档 |
| **《Spike_002-0d.md》** | G-1 委托修复冻结说明书 |
| **《Spike-002-0d 执行报告》** | Run C 复现 G-1；Run T 到达 Done |
| **《Spike-002-0d 裁判审查与裁定报告》** | VALID-002-0d-1；新基线与 getLoader/validate 归属裁定 |
| **《Spike-002-0d 执行报告勘误附记（2026-10-11）》** | 原文来源、synthetic 成员、命令缺口及协作事实校准 |
| **《Spike_002-0e.md》** / **《Spike-002-0e 冻结审查记录（2026-10-11）》** | 首个 Forge API 调用单语句探针；v1.0 冻结契约及审查记录 |
| **《Spike-002-0e 执行报告》** / **《Spike-002-0e 裁判审查与裁定报告》** | VALID，P1完整异常链 |
| **《Spike_002-0f.md》** / **《Spike-002-0f 静态取证报告》** / **《Spike-002-0f 顺序复核与裁定报告》** | 三处成员引用契约与候选资格 |
| **《Spike_002-0g.md》** / **《Spike-002-0g 执行报告》** / **《Spike-002-0g 顺序复核与裁定报告》** | K1局部链接修复与register实际入口、S1 |
| **《Spike_002-0h.md》** / **《Spike-002-0h 静态取证报告》** | 原Forge零候选条件分支与运行未知项 |
| **《Spike_002-0i.md》** / **《Spike-002-0i 执行与顺序裁定报告》** | 原Forge裸对象夹具初次STOP，前置缺件 |
| **《Spike_002-0i-1.md》** / **《Spike-002-0i-1 执行与顺序裁定报告》** | 夹具前置恢复、原register正常返回 |
| **《Spike_002-0j.md》** / **《Spike-002-0j 执行与顺序裁定报告》** | 限定空注册保真K1与Done，VALID |

---

**v1.1 架构已冻结；Spike-002-0j 已完成。架构不再扩展，仅允许根据实验结果回写 [F]/[E]/[H]/[D] 标签与已验证事实。**

**当前阶段**：

```
Spike-002-0b Step 1           ✅ 完成
Spike-002-0b-1                ✅ 完成
Spike-002-0b-1b               ✅ 完成
Spike-002-0b Step 2-A v1      ⛔ STOP（前置配置缺口）
Spike-002-0b Step 2-A v1.1    ✅ 完成（PASS）
Spike-002-0c                  ✅ 完成（PASS-002-0c-1：G-1 条件下的历史基线）
Spike-002-0d                  ✅ 完成（VALID-002-0d-1：G-1 清零，Done）
Spike-002-0e                  ✅ 完成（VALID：P1）
Spike-002-0f                  ✅ 完成（PASS：静态契约）
Spike-002-0g                  ✅ 完成（PASS：链接越过P1，register入口/S1）
Spike-002-0h                  ✅ 完成（PASS：条件分支；原Forge运行未验证）
Spike-002-0i                  ⛔ STOP（夹具前置缺件）
Spike-002-0i-1                ✅ 完成（PASS：原Forge裸对象register返回）
Spike-002-0j                  ✅ 完成（VALID：限定零订阅API路径Done）
```

**下一步**：先取证真实Forge订阅方法所需的注解/事件类型二进制契约，再冻结下一单变量探针；不选择额外业务API、不把空注册成功推为事件分发兼容。
（详见002-0j执行与顺序裁定报告。）

> `FMLModContainer` 构造成功 **≠** Forge Mod 可被加载；也 **≠** `constructMod()` 能通过。
