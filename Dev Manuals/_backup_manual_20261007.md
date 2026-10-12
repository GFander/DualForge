# Forge → NeoForge 兼容层开发手册

**版本**：v1.1（架构冻结版·最终）
**基础环境**：Minecraft 1.21.1 + NeoForge 21.1.256 + FancyModLoader（随 NeoForge 捆绑） + Java 21
**项目定位**：以 NeoForge 为唯一宿主的 Forge 模组运行时兼容层，为未来 Fabric 适配预留插件化接口。
**当前状态**：架构冻结，事实验证未冻结。

```
Spike-000   ✅ 完成
Spike-001a  ✅ 完成
Spike-001b  ▶ 已进入 / 待执行
Spike-001c  ⏸ 未开始
```

> **版本标签**：`v1.1 + Spike-000 回写 + Spike-001a 回写`。
> 回写只改动事实标签与新增 §3.6 / §3.7，**不改动架构**。

**版本历史**：
```
v1.0  源码验证版
   ↓
v1.1  架构冻结版  ← 当前
   ↓
Spike-000 ~ 005
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
Plugin Layer
    │
    └── Compatibility Core (LIBRARY)
Game Layer
    │
    └── Forge Mods (MOD)
```


## 三、FancyModLoader SPI 接入

### 3.1 SPI / API 分类（源码已验证）

| SPI / API | 角色 | 加载方式 |
|---|---|---|
| `IModFileCandidateLocator` | 候选发现扩展点 | **ServiceLoader** [F] |
| `IModFileReader` | ModFile 创建扩展点 | **ServiceLoader** [F] |
| `ModFileInfoParser` | Mod metadata 解析器 | **由 Reader 显式使用；加载方式待 21.1.256 源码确认** |
| `IModLanguageLoader` | ModContainer 创建扩展点 | **ServiceLoader** [H]（加载方式与校验位置已由 001b-0 逐字确认；**具体层名待 001b-3 运行侧观测**） |

> **Provider SPI 与 Internal/Parser API 必须区分。** `ModFileInfoParser` 是 `@FunctionalInterface`，天然属于 Reader 内部选择 Parser 的机制，不是 ServiceLoader provider。

**兼容层 JAR 的 ModFile Type（已由 Spike-001a-0 修正）**：Spike-001a-0 实测证明，
`IModFileReader` / `IModFileCandidateLocator` 这类 provider 的**实际**提升路径**不是** Plugin Layer，
而是 ModLauncher 层的 `ModDirTransformerDiscoverer` 把它们提前提升进 **SERVICE Layer**。
详见 §3.6 与 §3.7。

`IModLanguageLoader` 的实际层级（SERVICE / PLUGIN / 其他）**仍属 [H]**。
Spike-001b-0 已从字节码确认：它由 `LanguageProviderLoader`（在 `FMLLoader.completeScan` 第一条语句构造）
经 `ServiceLoaderUtil.loadServices` 加载，且**不可能**命中 mods/ 目录在本次运行中新建的 PLUGIN 层内容；
**具体层名留待 001b-3 运行侧观测**。
**不得**因为 Spike-001a-0 证明了 reader/locator 走 SERVICE 层，就推断 language loader 也走 SERVICE 层。

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

**`Scanner` 负责“发现类/注解信息”，谁把这些信息解释成 Forge entrypoint 是另一层职责。** FancyModLoader 当前源码路径为 `modInfo.getLoader().loadMod(modInfo, modFile.getScanResult(), gameLayer)`，即先取得 `scanResult`，再交给 language loader。因此 `findForgeModEntrypoints()` 留到 `ForgeLanguageLoader` 是合理的。

**Forge `@Mod` 的识别方式必须通过 Spike 验证。**[H]

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

| 类型 | 说明 |
|---|---|
| `net.neoforged.bus.api.IEventBus` | Mod 事件总线 [F] |
| `ModContainer` | 当前容器 [F] |
| `FMLModContainer` | 当前容器 [F] |
| `Dist` | 物理端 [F] |

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
                   visitLayer(layer, handled::add);   // 连同该层的 parent 一起标记
               }
```

- `[F]` `IModuleLayerManager.Layer` 枚举序为 `BOOT(0) → SERVICE(1) → PLUGIN(2) → GAME(3)`。
- `[F]` `LaunchContext.loadServices` **逆序**取"最高的已存在层"，因此实际会搜索
  **该层 + 其全部 parent 层**（`visitLayer` 会连带标记 parent，正说明这一点）。
- `[F]` 结果按 `IOrderedProvider.getPriority()` **降序**排序；带 marker `CORE` 以 DEBUG 级别
  输出 `Found {n} implementations of {service}` 及每个 provider 的
  `优先级 - 实现类 from 来源 jar`。
- `[E]` 该日志行是后续所有 Spike 判定 provider 是否可见的**首要证据**。

**Service 层提升通道（[F]，Spike-001a-0 首次定位）**

```
ModLauncher TransformationServicesHandler.discoverServices(DiscoveryData)
  ├─ BOOT 层 ServiceLoader 取 ITransformerDiscoveryService 实现
  │    loader-4.0.45.jar 提供两个：
  │      net.neoforged.fml.loading.ModDirTransformerDiscoverer
  │      net.neoforged.fml.loading.ClasspathTransformerDiscoverer
  └─ ModDirTransformerDiscoverer.candidates(gameDir, launchTarget)
       └─ scan(gameDir): Files.walk(<gameDir>/mods, 1)
            .filter(shouldLoadInServiceLayer)          // 判据见下
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
- `[F]` 该提升发生在 **LaunchContext 创建之前**，因此被提升的 JAR 会出现在
  `Located paths when launch context was created` 列表中。
- `[E]` 实测：`Found additional transformation services from discovery services: [...\mods\forgecompat.jar]`；
  该行在无此 JAR 的运行中为空列表。
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

> `[F]` 即：**reader/locator 的服务查找先于 locator 扫描本身**，所以一个 JAR 不可能
> 通过"被自己扫描到"来提供 reader；它必须走 §3.6 的提前提升通道。


### 3.7 兼容层 JAR 身份约束（Spike-001a-0 回写）

**已实测通过的身份形态（[E]，对应 `spike001a/compat/build/forgecompat.jar`）**

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

三条约束，缺一不可（前两条为 [F]，第三条为 [E]）：

1. `[F]` **必须在 `META-INF/services/` 中声明白名单内的 SPI**（见 §3.6）。
   只放 manifest 不声明 services 的 JAR（如 `forgeprobe.jar`）不会被提升进 SERVICE 层。
2. `[F]` **必须声明 `Automatic-Module-Name`**，否则该 JAR 无法作为自动模块进入 ModuleLayer，
   `SecureJar.name()` 与 `ModuleDescriptor.name()` 将无从产生。
3. `[E]` `FMLModType: LIBRARY` + `Automatic-Module-Name` 的组合照抄 NeoForge 自带
   `net.neoforged.neoforge-coremods-<ver>.jar` 的 manifest 形态；本形态已实测可用。

**提升后的行为（[E]）**

- 兼容层 JAR 会出现在 `Located paths when launch context was created` 中，
  随后被内置 `ModsFolderLocator` **跳过**：
  `Skipping ...\mods\forgecompat.jar because it was already located earlier`
  → 兼容层 JAR 自己不会再作为 mod 被二次读入。
- `[E]` 兼容层 JAR 的 `IModFileReader` 以 **priority 100** 先于所有内置 reader（0 / -1000）
  处理**每一个** JAR，包括 NeoForge 自带 JAR 与 jar-in-jar 内容。实现必须以
  "不匹配即返回 `null`"兜底，否则会污染其它 mod。

**尚未分离的变量（[H]，需专门对照实验）**

- `[H]` 保留 services 声明、移除 `FMLModType: LIBRARY` 后，提升行为是否不变；
  以及该 JAR 是否仍会被当作 library mod file 接受。
- `[H]` 保留 `FMLModType: LIBRARY`、移除 services 声明后，是否必然失去提升。
  Spike-001a-0 **未做**这两个负对照，故 §3.7 第 3 条只能记为 [E]（形态已实测可用），
  不能升级为"`FMLModType` 是必要条件"。


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

**ISOLATED 实现约束**：不能在 `ModuleLayer` 中插入子层，必须使用模块系统之外的 `URLClassLoader` 旁路。真正决定能否看到类的核心机制是 **ClassLoader delegation**，不是 JPMS exports。**禁止使用“导出 Minecraft/NeoForge/Shim API”的表述，应描述为“Parent Delegation Policy + API 白名单”。**[D]

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


## 十、测试矩阵

| 测试 | 验证目标 | 依赖 SPI | 预期 |
|---|---|---|---|
| **T0** | **Compatibility Core 自身身份验证** | `FMLModType` + `Automatic-Module-Name` + ServiceLoader | **provider 在正确 Layer 可见** |
| T1 | 兼容层被 EarlyServiceDiscovery 加载 | `IModFileCandidateLocator` | 日志输出初始化 |
| T2 | 发现 Forge JAR | Locator + Pipeline | 报告候选 |
| T3 | 元数据解析 | `IModFileReader` + Parser | 输出 modId |
| T4 | Lifecycle 闭环 | `FMLModContainer` | 构造函数被调用 |
| T5 | 字节码转换 | `ClassProcessorProvider` | SRG 引用重映射 |
| T6 | 事件桥接 | EventBus Shim | 监听器被调用 |
| T7 | 隔离加载 | `URLClassLoader` 旁路 | 不污染 Game Layer |

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

**T0 的必要性**：整个系统存在两层身份——Plugin Layer（Compatibility Core）与 Game Layer（Forge Mods）。如果出现 `ServiceLoader 找不到 ForgeModReader`，必须首先区分是 **provider 没注册 / 注册了但进入错误 Layer / 所依赖类不在该 Layer 可见范围**。T0 能极大降低排查成本。


## 十一、Spike 开发路线（冻结版）

| Spike | 核心问题 | PASS 条件 | 不处理 |
|---|---|---|---|
| **000** | Native Forge JAR 在哪一阶段失败？ | 完整记录 Discovery → Metadata → Validation → Scan → LanguageLoader → Module → Container → Constructor 的实际失败链 | 任何兼容代码 |
| **001a** | Forge JAR 是否正确进入 Game Layer？ | `SecureJar.name()`、`IModInfo.moduleName()`、`ModuleDescriptor.name()`、`GameLayer.findModule()` 四者一致 | javafml provider、构造函数 |
| **001b** | javafml 如何兼容？ | 明确同名 provider 策略、版本检查时机、最终 `version()` 要求 | ModuleLayer、构造函数 |
| **001c** | FMLModContainer 能否消费 Forge @Mod entrypoint？ | 1. Forge `@Mod` entrypoint 被正确识别；2. `FMLModContainer` 成功获得 entrypoint；3. **constructor resolution 的实际行为已记录**；4. 若构造器因 Forge `IEventBus` / `FMLJavaModLoadingContext` 类型不匹配失败，**完整记录其 ABI 断层**。**不要求本 Spike 成功构造 ForgeProbe。** | FMLJavaModLoadingContext、事件、注册表 |
| **002** | Forge constructor context / ABI compatibility | **Forge constructor 所需上下文类型的 ABI 兼容路径已被证明；`FMLJavaModLoadingContext` / Forge `IEventBus` 的 Shim 接管点明确；不要求 Registry/Event 功能本身在本 Spike 中完成。** | Registry/Event |
| **003** | Registry 是否兼容？ | `DeferredRegister` 注册成功 | Mapping |
| **004** | Event 是否兼容？ | `@SubscribeEvent` 被触发 | Mapping |
| **005** | Namespace Mapping 是否可行？ | SRG → Mojmap 引用正确重映射 | 其它兼容层 |
| **006** | 真实 Forge Mod | JEI/Create 基本可用 | 全功能 |

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

### Spike-001b 的决策树

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
| R-METADATA | ForgeJarMetadata 的具体实现 | 模块名、包列表、服务提供者 | 🟡 待验证 |
| R-SCAN-DATA | Forge 1.21.1 @Mod scan data 的确切结构 | entrypoint 生成方式 | 🟡 待验证 |
| R-CONTAINER-ABI | FMLModContainer 是否可直接复用 | 构造函数注入语义；Forge `IEventBus` ABI 断层 | 🟡 待验证 |
| R-CLASSPROCESSOR | ClassProcessorProvider 在 21.1.256 的确切 API | 待源码确认 | 🟡 待确认 |
| R-OBJECT-MODEL | Forge/Neo 对象身份与泛型兼容 | 长期隐患，BRIDGED 模式 | 🟢 暂不处理 |


## 十三、开发环境与依赖（精确版本冻结）

| 组件 | 版本 |
|---|---|
| Minecraft | **1.21.1**（精确） |
| NeoForge | **21.1.256**（精确） |
| FancyModLoader | **随 NeoForge 21.1.256 捆绑，版本从启动日志读取** |
| Java | **21** |
| **编译依赖 Forge** | **1.21.1-52.1.0**（精确） |

> **21.1.256 满足精确 patch 级冻结要求。** FancyModLoader 的实际版本必须从基线启动日志开头提取，不得凭推测填写。**Forge 版本与 NeoForge 版本是两个独立字段，不能混为一谈。** Forge 版本用于编译依赖，NeoForge 版本用于运行时宿主。[D]

> **Forge 1.21.1-52.1.0 是 `52.1` 分支的 RB 1（Release Build 1），是该分支的起点版本。** 后续 52.1.1 到 52.1.16 均为增量修补。选择分支起点版本可避免后期 patch 引入的无关行为干扰，同时与 `mods.toml` 中 `loaderVersion="[52,)"` 的语义一致。**不得因为官网有更新的 52.1.16 就改用最新版。** Spike-000 的目标是定位失败阶段，不是测试“最新 Forge 是否能工作”。[D]

> 实验环境必须做成**不可变基线**。在 Spike-000 完成前，不得修改上述任何版本。


## 十四、Spike-000 执行原则

**立即执行 Spike-000，不写任何兼容层代码。**

> **Spike-000 的完整执行流程、结构化输出格式、纪律约束，见《Spike-000 执行任务说明书》。** 本节仅保留手册层面的核心原则。

### 14.1 Probe 设计（最小形态）

`forgeprobe.jar`：
- `META-INF/mods.toml`（`modLoader="javafml"`, `loaderVersion="[52,)"`, `modId="forgeprobe"`）
- `META-INF/MANIFEST.MF`（建议声明 `Automatic-Module-Name: forgeprobe`，**仅作为实验辅助条件，不构成兼容层最终实现要求**）
- `com/example/forgeprobe/ForgeProbe.class`（`@Mod("forgeprobe")` + `public ForgeProbe(IEventBus bus)`，**使用 Forge 命名空间的 IEventBus**）

**编译依赖必须为 Forge 1.21.1-52.1.0，禁止使用 NeoForge 依赖。**

**`ForgeProbe` 是“故意保持 Forge 原生 ABI”的测试样本，不是“最小 NeoForge 可兼容样本”。** 不要因看到 `ForgeProbe(IEventBus)` 就误以为这个 `IEventBus` 应该是 NeoForge 的。[D]

### 14.2 mods.toml 本身就是重要观察点

NeoForge 从 20.5 起明确使用 `META-INF/neoforge.mods.toml`，而 Forge 使用 `META-INF/mods.toml`。[F]

**预期观察**：NeoForge 默认 metadata reader 无法将 Forge 的 `META-INF/mods.toml` 作为原生 NeoForge mod metadata 接受。[H]

**具体首次失败阶段仍必须由实际日志和调用链确认。** 不得因为理论判断与实验现象吻合，就将其写成“源码验证事实”。

**千万不要在 Spike-000 前给 Probe 加 `neoforge.mods.toml`，否则实验失去意义。**[D]

### 14.3 执行原则

1. 放入 NeoForge 21.1.256 的 `mods/` 目录。
2. 启动游戏，**完整记录从 Discovery 到失败的日志**。
3. **不修复任何错误**。即使出现 `Unknown language loader` 也不要马上写 `ForgeLanguageLoader`；即使出现 `module not found` 也不要马上改 `ForgeJarMetadata`。
4. 先记录：**错误 → 调用栈 → 源码入口 → 对应流水线阶段 → 为什么原生 NeoForge 无法继续**。

### 14.4 失败链记录表

| 阶段 | 预期入口 | 实际结果 | 错误 | 是否进入下一阶段 |
|---|---|---|---|---|
| Discovery | `IModFileCandidateLocator` / Pipeline | ? | ? | ? |
| Metadata | `IModFileReader` / Parser | ? | ? | ? |
| Validation | `ModValidator` | ? | ? | ? |
| Scan | `BackgroundScanHandler` | ? | ? | ? |
| LanguageLoader | `LanguageProviderLoader` | ? | ? | ? |
| Module | Game Layer | ? | ? | ? |
| Container | `FMLModContainer` | ? | ? | ? |
| Constructor | ForgeProbe ctor | ? | ? | ? |

> **注意**：不要因为日志里出现了某个类名，就直接认定“已经进入该阶段”。应根据实际调用链/异常栈确认。
> **class loaded ≠ method invoked ≠ stage entered ≠ operation succeeded**

**Spike-000 的输出直接决定 Spike-001a 从哪里开始接管。**


## 附录 A：事实标签体系

| 标签 | 含义 | 示例 |
|---|---|---|
| **[F]** | Source Fact | `[F] IModFileReader is resolved through ServiceLoader` |
| **[E]** | Experimental Fact | `[E] Forge mods.toml reaches / does not reach metadata parser` |
| **[H]** | Hypothesis | `[H] Forge @Mod may be present in ScanData but not interpreted as entrypoint` |
| **[D]** | Design Decision | `[D] Compatibility Core lives in Plugin Layer` |

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


## 附录 B：最终验收清单

| 项目 | 状态 |
|---|---|
| 架构边界 | ✅ 冻结 |
| SPI 分层 | ✅ |
| Plugin/Game Layer | ✅ |
| Discovery → Container 模型 | ✅ |
| Class scanning / entrypoint 分离 | ✅ |
| Module identity | ✅ |
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


## 附录 C：配套文档索引

| 文档 | 用途 |
|---|---|
| **《Forge → NeoForge 兼容层开发手册 v1.1》**（本文档） | 架构冻结基线，所有设计决策的事实来源 |
| **《Spike-000 执行任务说明书》** | 可直接交给 Codex 的可执行任务，包含完整流程、纪律约束、结构化输出格式 |
| **《Spike-000 实验报告》** | Spike-000 实验结论（Discovery 红线 = `MINECRAFT_FORGE` 拦截），[E]/[F]/[H]/[D] 全量标签 |
| **《Spike-000 正式实验结论与 Spike-001a 执行定义》**（`Spike_001a.md`） | Spike-000 结论冻结 + Spike-001a 四步拆分 + 001a 任务说明书 |
| **《Spike-001a-0 实验报告》** | 001a-0 结论（provider 进入 SERVICE 层）、`ModDirTransformerDiscoverer` 机制定位 |
| **《Spike-001b 任务说明书与 v1.1 标签修正》**（`Spike_001b.md`） | v1.1 标签修正汇总 + Spike-001b 子 Spike 拆分（001b-0 ~ 001b-5） |

---

**v1.1 已冻结。架构设计阶段结束。不再增加架构。开始 Spike-000。**

**下一份真正有价值的材料**：

```
forgeprobe.jar
    +
NeoForge 21.1.256
    +
编译依赖 Forge 1.21.1-52.1.0
    +
完整启动日志（基线 + Spike-000）
    +
完整异常栈
    +
失败链记录表
```

**然后根据真实结果判断：Discovery → Metadata → Validation → Scan → LanguageLoader → Module → Container → Constructor 究竟哪一格第一次变成红色。**
