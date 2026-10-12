# Spike-001a-0 实验报告

> 子 Spike 唯一问题：**ForgeModReader 是否被 ServiceLoader 发现/实例化？**
> PASS：日志或断点明确证明 ForgeModReader 被实例化 · STOP：未加载则停止，检查 META-INF/services 注册
>
> **结论：PASS（置信度 HIGH）。**
> 但 PASS 的实现机制与手册 §3.1 的预期**不一致**——不是"进入 Plugin Layer"，而是 ModLauncher 层的
> `ITransformerDiscoveryService` 把该 JAR 提前提升进 **SERVICE 层**。详见第六节。

---

## 一、实验环境（精确版本）

```
Minecraft:            1.21.1
NeoForge:             21.1.256
FancyModLoader:       4.0.45
Java:                 21（OpenJDK 21.0.10，Microsoft Build，21.0.10+7-LTS）
OS:                   Windows 11 (amd64) version 10.0
启动方式:             CLI（官方 installer --installServer 产物 + java @argfile nogui）
客户端/服务端:        Server（Dedicated Server，launch target = forgeserver）

编译依赖 Forge 版本:  1.21.1-52.1.0        （forgeprobe.jar 的编译依赖，本子 Spike 未改动）
兼容层编译依赖:       NeoForge FML 4.0.45 / securejarhandler 3.0.8 / modlauncher 11.0.5
```

运行标签：`spike001a-0`

**本次 `mods/` 目录内容**（唯一变量，相对 Spike-000 的 forgeprobe 运行）：

```
forgecompat.jar    ← 新增：Spike-001a 最小兼容层工件
forgeprobe.jar     ← 不变：与 Spike-000 同一个文件（SHA-256 已核对一致）
```

服务端工作目录、`server.properties`、`eula.txt`、`user_jvm_args.txt`（`-Xmx3G -Xms1G`）、
JVM 参数、启动参数、Java/NeoForge/FML 版本**全部与 Spike-000 两次运行相同**。

---

## 二、实验目标

**唯一问题（Spike_001a.md §三 001a-0）**：`ForgeModReader` 是否被 ServiceLoader 发现/实例化？

- PASS：日志或断点明确证明 `ForgeModReader` 被实例化
- STOP：未加载则停止，检查 `META-INF/services` 注册

**禁止目标**（Spike_001a.md §一）：让 ForgeProbe 构造函数被调用；进入
LanguageLoader / Module / Container；修改 `FMLJavaModLoadingContext` ABI；写任何
ASM / Mapping / Mixin / Registry / Event 代码。

---

## 三、实验输入

### 3.1 forgeprobe.jar（未改动）

- SHA-256: `a93a823c988bfd6a8f6ea132d189d9de732b0e5951f4bf74980d51bb63374e22`
- 大小: `1772` 字节
- 与 Spike-000 使用的文件**逐字节相同**（哈希一致），未做任何修改
- 内容与 Spike-000 报告第三节完全一致，此处不重复

### 3.2 forgecompat.jar（本子 Spike 新增）

- 构建路径: `D:\DualForge\spike001a\compat\build\forgecompat.jar`
- SHA-256: `68d74a50745d6712888605e4abbbefac2827b91aa7d9b25a967fd0ecae6a8b5a`
- 文件大小: `10553` 字节
- class 文件版本: major = 65（Java 21）
- 运行期存放路径: `D:\DualForge\spike000\neoforge-server\mods\forgecompat.jar`

**JAR tree（`jar tf forgecompat.jar` 完整输出）：**

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
com/example/forgecompat/ForgeModFileInfo$MapConfigurable.class
com/example/forgecompat/ForgeModFileInfo.class
com/example/forgecompat/ForgeModFileInfoParser.class
com/example/forgecompat/ForgeModLocator.class
com/example/forgecompat/ForgeModReader.class
```

**META-INF/MANIFEST.MF（JAR 内实际内容，已解包核对）：**

```
Manifest-Version: 1.0
Automatic-Module-Name: forgecompat
FMLModType: LIBRARY
Created-By: 21.0.10 (Microsoft)
```

> `Created-By` 由 JDK `jar` 工具自动追加。`FMLModType: LIBRARY` 与
> `Automatic-Module-Name` 的取值方式，照抄 NeoForge 自带的
> `net.neoforged.neoforge-coremods-21.1.256.jar` 的 manifest 形态
> （已解包核对：`Manifest-Version` / `Automatic-Module-Name: neoforge.coremods` / `FMLModType: LIBRARY`）。

**META-INF/services/net.neoforged.neoforgespi.locating.IModFileReader（原文）：**

```
com.example.forgecompat.ForgeModReader
```

**META-INF/services/net.neoforged.neoforgespi.locating.IModFileCandidateLocator（原文）：**

```
com.example.forgecompat.ForgeModLocator
```

**源码（4 个类，实际参与编译，位于 `D:\DualForge\spike001a\compat\src\main\java\com\example\forgecompat\`）：**

| 文件 | 实现内容 | 依据 |
|---|---|---|
| `ForgeModLocator.java` | 只认 `mods/forgeprobe.jar`；`DEFAULT.withLocator(this)` | v1.1 §4.1（含 §4.1 的修正） |
| `ForgeModReader.java` | 识别 `META-INF/mods.toml` → `SecureJar.from(jar, new ForgeJarMetadata(jar))` → `IModFile.create(secureJar, new ForgeModFileInfoParser())` | v1.1 §3.2 |
| `ForgeJarMetadata.java` | 包装 `cpw.mods.jarhandling.JarMetadata.from(JarContents)` | v1.1 §3.2 的 [H]：走"验证其可复用性"这条路 |
| `ForgeModFileInfoParser.java` + `ForgeModFileInfo.java` | 最小字段扫描器解析 `mods.toml`，返回 `IModFileInfo` | Spike_001a.md §2.2 |

每个类带 `[FORGECOMPAT]` 前缀的 `System.out.println` 标记：`<clinit>`（类初始化）、
`INSTANTIATED`（构造器）、`CALLED`（方法进入）。这些标记是 001a-0 的"断点"。

**`getMods()` 返回空列表是有意选择**（`ForgeModFileInfo`）：
`IModInfo.getLoader()` 必须返回一个 `IModLanguageLoader`，而 Spike_001a.md §2.2 明确写
"ForgeLanguageLoader：不实现"。解析出的 `mods.toml` 字段改由
`getConfig()` / `getFileProperties()` 暴露。该选择与 NeoForge 自身对 LIBRARY 型 JAR 的
处理一致（`JarModsDotTomlModFileReader.manifestParser` 产出的 `DefaultModFileInfo.getMods()` 同样是空列表）。

**编译依赖（`build.gradle` 关键行，原始内容）：**

```
compileOnly 'net.neoforged.fancymodloader:loader:4.0.45'
compileOnly 'cpw.mods:securejarhandler:3.0.8'
compileOnly 'cpw.mods:modlauncher:11.0.5'
compileOnly 'org.slf4j:slf4j-api:2.0.9'
```

**实际编译命令（执行偏差，如实记录）：**

```
C:\Users\Albert\.jdks\ms-21.0.10\bin\javac.exe --release 21 -encoding UTF-8 ^
  -classpath "D:\DualForge\spike000\neoforge-server\libraries\net\neoforged\fancymodloader\loader\4.0.45\loader-4.0.45.jar;D:\DualForge\spike000\neoforge-server\libraries\cpw\mods\securejarhandler\3.0.8\securejarhandler-3.0.8.jar;D:\DualForge\spike000\neoforge-server\libraries\cpw\mods\modlauncher\11.0.5\modlauncher-11.0.5.jar;D:\DualForge\spike000\neoforge-server\libraries\org\slf4j\slf4j-api\2.0.9\slf4j-api-2.0.9.jar" ^
  -d "D:\DualForge\spike001a\compat\build\classes" <4 个 .java 文件>
```

`javac` 退出码 0，无 warning / 无 error。

---

## 四、基线对照

本子 Spike 不重跑基线，直接引用已冻结的 Spike-000 结果作为对照：

| 运行 | `mods/` | `IModFileReader` 实现数 | `forgeprobe.jar` 结局 |
|---|---|---|---|
| Spike-000 baseline | 空 | 2 | —（不存在） |
| Spike-000 forgeprobe | `forgeprobe.jar` | 2 | `fml.modloadingissue.brokenfile.minecraft_forge`，跳过 |
| **Spike-001a-0** | `forgeprobe.jar` + `forgecompat.jar` | **3** | **被接受为 `IModFile`** |

对照锚点（同一条日志在三次运行中的取值）：

```
Spike-000 baseline : Found additional transformation services from discovery services:
Spike-000 forgeprobe: Found additional transformation services from discovery services:
Spike-001a-0        : Found additional transformation services from discovery services: [D:\DualForge\spike000\neoforge-server\mods\forgecompat.jar]
```

---

## 五、001a-0 实验结果

结果: **PASS**

日志路径:

- `D:\DualForge\spike000\logs\spike001a-0\spike001a-0.log`
- `D:\DualForge\spike000\logs\spike001a-0\spike001a-0.debug.log`
- `D:\DualForge\spike000\logs\spike001a-0\spike001a-0.stdout.log`
- `D:\DualForge\spike000\logs\spike001a-0\spike001a-0.stderr.log`（0 字节）
- `D:\DualForge\spike000\logs\spike001a-0\meta.json`

**PASS 直接证据（stdout，实例化标记）：**

```
[FORGECOMPAT] <clinit> ForgeModLocator
[FORGECOMPAT] INSTANTIATED ForgeModLocator
[FORGECOMPAT] <clinit> ForgeModReader
[FORGECOMPAT] INSTANTIATED ForgeModReader
```

**PASS 直接证据（`debug.log`，ServiceLoader 名录）：**

```
[main/DEBUG] [net.neoforged.fml.util.ServiceLoaderUtil/CORE]: Found 3 implementations of IModFileReader:
[main/DEBUG] [net.neoforged.fml.util.ServiceLoaderUtil/CORE]: 	     100 - com.example.forgecompat.ForgeModReader from forgecompat.jar
[main/DEBUG] [net.neoforged.fml.util.ServiceLoaderUtil/CORE]: 	       0 - net.neoforged.fml.loading.moddiscovery.readers.JarModsDotTomlModFileReader from loader-4.0.45.jar
[main/DEBUG] [net.neoforged.fml.util.ServiceLoaderUtil/CORE]: 	   -1000 - net.neoforged.fml.loading.moddiscovery.readers.NestedLibraryModReader from loader-4.0.45.jar
```

```
[main/DEBUG] [net.neoforged.fml.util.ServiceLoaderUtil/CORE]: Found 5 implementations of IModFileCandidateLocator:
[main/DEBUG] [net.neoforged.fml.util.ServiceLoaderUtil/CORE]: 	    1000 - [built-in] net.neoforged.fml.loading.moddiscovery.locators.ProductionServerProvider from loader-4.0.45.jar
[main/DEBUG] [net.neoforged.fml.util.ServiceLoaderUtil/CORE]: 	    1000 - [built-in] net.neoforged.fml.loading.moddiscovery.locators.PathBasedLocator from loader-4.0.45.jar
[main/DEBUG] [net.neoforged.fml.util.ServiceLoaderUtil/CORE]: 	     100 - com.example.forgecompat.ForgeModLocator from forgecompat.jar
[main/DEBUG] [net.neoforged.fml.util.ServiceLoaderUtil/CORE]: 	       0 - net.neoforged.fml.loading.moddiscovery.locators.ModsFolderLocator from loader-4.0.45.jar
[main/DEBUG] [net.neoforged.fml.util.ServiceLoaderUtil/CORE]: 	       0 - net.neoforged.fml.loading.moddiscovery.locators.MavenDirectoryLocator from loader-4.0.45.jar
```

> 计数对照：Spike-000 为 `2 implementations of IModFileReader` / `4 implementations of IModFileCandidateLocator`；
> 本子 Spike 为 **3 / 5**，增量正是 `forgecompat.jar` 的两个 provider。

其他关键行:

```
[main/INFO] [net.minecraft.server.dedicated.DedicatedServer/]: Done (0.323s)! For help, type "help"
```

异常（若有）: **无异常、无堆栈、无 crash-report**（`logs/spike001a-0/crash-reports` 不存在）。
进程退出码 `0`，状态 `DONE_STOPPED_GRACEFULLY`。

---

## 六、机制定位：这个 provider 为什么能被加载到

**这是本子 Spike 最重要的发现，且与 v1.1 手册 §3.1 的预期不同。**

### 6.1 实际发生的顺序

```
ModLauncher 启动（Launcher.main）
  └─ TransformationServicesHandler.discoverServices(DiscoveryData)
       ├─ 从 BOOT 层 ServiceLoader 取 ITransformerDiscoveryService 实现
       │    → 来自 loader-4.0.45.jar 的两个实现：
       │         net.neoforged.fml.loading.ModDirTransformerDiscoverer
       │         net.neoforged.fml.loading.ClasspathTransformerDiscoverer
       │      （证据：loader-4.0.45.jar!/META-INF/services/cpw.mods.modlauncher.serviceapi.ITransformerDiscoveryService）
       ├─ ModDirTransformerDiscoverer.candidates(gameDir, launchTarget)
       │    └─ scan(gameDir)：Files.walk(<gameDir>/mods, 1)
       │         .filter(shouldLoadInServiceLayer)
       │         → 命中 mods/forgecompat.jar
       │         → 产出 NamedPath("forgecompat.jar", <path>)
       ├─ 记录日志（本次实证）：
       │    "Found additional transformation services from discovery services: [...\mods\forgecompat.jar]"
       └─ moduleLayerHandler.addToLayer(Layer.SERVICE, namedPath)   ← 提升进 SERVICE 层
            └─ buildLayer(Layer.SERVICE)

  ↓ （SERVICE 层此时已含 forgecompat.jar）

  └─ LaunchContext 构造：收集所有层中已解析模块的 location 作为 locatedPaths
       → "Located paths when launch context was created: [ ... , ...\mods\forgecompat.jar , ... ]"

  ↓

FMLServiceProvider.beginScanning → FMLLoader.beginModScan → new ModDiscoverer(...)
  └─ ServiceLoaderUtil.loadServices(ctx, IModFileReader.class)
       └─ LaunchContext.loadServices(...)：取**最高的已存在层**做 ServiceLoader.load(layer, service)
            → 找到 com.example.forgecompat.ForgeModReader  ← **001a-0 的 PASS 点**
  └─ ServiceLoaderUtil.loadServices(ctx, IModFileCandidateLocator.class)
       → 找到 com.example.forgecompat.ForgeModLocator
```

### 6.2 决定性判据（字节码）

`net.neoforged.fml.loading.TransformerDiscovererConstants`（`loader-4.0.45.jar`）：

```java
public static final Set<String> SERVICES = Set.of(
    cpw.mods.modlauncher.api.ITransformationService.class.getName(),
    net.neoforged.neoforgespi.locating.IModFileCandidateLocator.class.getName(),  // ← ForgeModLocator
    net.neoforged.neoforgespi.locating.IModFileReader.class.getName(),           // ← ForgeModReader
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

`net.neoforged.fml.loading.ModDirTransformerDiscoverer`：

```java
private static void scan(Path gameDir) {
    Path modsDir = gameDir.resolve(FMLPaths.MODSDIR.relative()).toAbsolutePath().normalize();
    if (!Files.exists(modsDir)) return;
    Files.walk(modsDir, 1)
         .filter(ModDirTransformerDiscoverer::shouldLoadInServiceLayer)
         .forEachOrdered(p -> found.add(new NamedPath(p.getFileName().toString(), p)));
}

private static boolean shouldLoadInServiceLayer(Path path) {
    return Files.isRegularFile(path)
        && path.toString().endsWith(".jar")
        && Files.size(path) != 0
        && TransformerDiscovererConstants.shouldLoadInServiceLayer(path);   // ← 判据在上面
}
```

**因此：一个放在 `mods/` 里的 JAR，只要在 `META-INF/services/` 中声明了
`IModFileReader` 或 `IModFileCandidateLocator`（以及另外 4 种白名单类型之一），
就会在 ModLauncher 启动早期被提升进 SERVICE 层，其 provider 会被
`ServiceLoaderUtil` 在 Discovery 阶段正常加载。**
这解释了 `forgeprobe.jar`（无 `META-INF/services/`）为何不被提升，而 `forgecompat.jar` 被提升。

### 6.3 对 v1.1 手册 §3.1 的修正

| 原表述 | 修正后 |
|---|---|
| "放在 `mods/` 目录中的 provider JAR 需要声明 `FMLModType: LIBRARY` 才能进入 Plugin Layer" | `[E]`+`[F]` 对 `IModFileReader` / `IModFileCandidateLocator` 而言，实际生效的机制是 **`META-INF/services/` 中的白名单服务声明 + `ModDirTransformerDiscoverer` 提升进 SERVICE 层**，发生在 Plugin Layer 存在之前 |
| "`IModFileReader` 通过 ServiceLoader 加载 `[F]`" | 进一步精确为：通过 `ILaunchContext.loadServices` → `LaunchContext.loadServices` → 对**最高的已存在 ModuleLayer** 调 `ServiceLoader.load(ModuleLayer, Class)` |
| "兼容层必须声明 `FMLModType: LIBRARY`" | `[H]` 本实验的 JAR **同时**带 `FMLModType: LIBRARY` 与 services 声明，**未能把两者的作用分离**。按字节码，`shouldLoadInServiceLayer` 只读 `META-INF/services/`，不读 `FMLModType`；但 `FMLModType` 的运行时必要性**未在本轮中单独验证** |

### 6.4 兼容层在 SERVICE 层的可见性

`forgecompat.jar` 被提升后出现在 `Located paths when launch context was created` 列表中
（与该列表里的 `loader-4.0.45.jar`、`modlauncher-11.0.5.jar` 等并列），
因此后续 `ModsFolderLocator` 扫描 `mods/` 时直接把它**跳过了**：

```
[main/DEBUG] [net.neoforged.fml.loading.moddiscovery.ModDiscoverer/]: Skipping D:\DualForge\spike000\neoforge-server\mods\forgecompat.jar because it was already located earlier
```

即：兼容层 JAR 自己**不会**再作为 mod 被二次读入。

---

## 七、Failure Chain

> 判定原则不变：`class loaded ≠ method invoked ≠ stage entered ≠ operation succeeded`。
> 本节只填写到本子 Spike **已被证据覆盖**的阶段。

### Discovery
- entered: **YES**
- evidence:
  - `Trying locator forgecompat locator (mods/forgeprobe.jar only)`
  - `[FORGECOMPAT] CALLED ForgeModLocator.findCandidates`
  - `[FORGECOMPAT] ForgeModLocator: adding candidate D:\...\mods\forgeprobe.jar`
  - `[main/INFO] ... Found mod file "forgeprobe.jar" [locator: forgecompat locator (mods/forgeprobe.jar only)]`
  - `Locator forgecompat locator (mods/forgeprobe.jar only) found 1 mods, 0 warnings, 0 errors`
- error: **无**。Spike-000 的 `Skipping jar. ... is for Minecraft Forge ...` 与
  `fml.modloadingissue.brokenfile.minecraft_forge` 在本轮**全部消失**
  （对三份日志全文检索 `minecraft_forge` / `Skipping jar` / `brokenfile`：**零命中**）

### Metadata
- entered: **YES**
- evidence:
  - `[FORGECOMPAT] CALLED ForgeModFileInfoParser.build for D:\...\mods\forgeprobe.jar`
  - `[FORGECOMPAT] ForgeModFileInfoParser: parsed keys=[modLoader, loaderVersion, license, mods, dependencies]`
- error: 无

### Validation
- entered: **YES**
- evidence:
  - `[main/DEBUG] [net.neoforged.fml.loading.moddiscovery.ModFile/LOADING]: Loading mod file D:\...\mods\forgeprobe.jar with languages []`
    （该行由 `ModFile.identifyLanguage()` 输出，调用方为 `ModValidator`）
  - `[main/DEBUG] [net.neoforged.fml.loading.moddiscovery.ModValidator/SCAN]: Found 4 mod files with 2 mods`
- error: 无

### Scan（class scan / ModFileScanData）
- entered: **UNKNOWN**
- evidence: 本轮日志中**没有**任何针对 `forgeprobe.jar` 的 `ModFileScanData` / 注解扫描记录。
  由于该 `IModFileInfo.getMods()` 为空，`forgeprobe.jar` 未进入语言加载，其类扫描是否发生
  **无法由本轮证据判定**，记为 UNKNOWN（不记为 NO，也不记为 YES）。
- error: 无相关记录

### LanguageLoader
- entered: **NO**
- evidence: `Loading mod file ...\forgeprobe.jar with languages []` —— languages 为空列表。
  原因是 `ForgeModFileInfo.getMods()` 返回空列表（见第三节的有意选择），
  因此不存在需要语言加载的 `IModInfo`。日志中未出现任何为 `forgeprobe` 选择 `javafml` 的记录。
- error: 无

### Module
- entered: **NO（无证据）**
- evidence: 未进入 LanguageLoader → 无 `FMLModContainer` → 无 `gameLayer.findModule(...)` 调用证据。
- error: 无

### Container
- entered: **NO**
- evidence: 日志中不存在 `forgeprobe` 对应的 `FMLModContainer` 创建记录。
- error: 无

### Constructor
- entered: **NO**
- evidence: `FORGE PROBE CONSTRUCTOR REACHED` / `BUS = ` 在 stdout 及全部日志中**零命中**。
  `ForgeProbe.class` 从未被加载。
- error: 无

---

## 八、First Confirmed Failure

**本子 Spike 内未观察到失败。**

001a-0 的 STOP 条件（"未加载则停止"）**未触发**；`ForgeModReader` 已被 ServiceLoader
发现并实例化。因此本报告**没有 001a-0 意义上的 first confirmed failure 可报**。

Spike-000 报告的第一条红线（Discovery → `IModFile` 创建边界的 `MINECRAFT_FORGE` 拦截）
在本轮中**没有再出现**：`minecraft_forge` 在三份日志中零命中。

**该红线是否已被彻底越过、以及越过后下一个失败点在哪里，超出 001a-0 的判定范围，
由架构决策方依据本报告附录 A 的原始证据判断。**

---

## 九、SPI 调用证据

与 Spike-000 不同，本子 Spike **有** SPI 调用证据。

| SPI | 证据 |
|---|---|
| `IModFileReader` | `ServiceLoaderUtil/CORE: Found 3 implementations of IModFileReader` · `100 - com.example.forgecompat.ForgeModReader from forgecompat.jar` |
| `IModFileCandidateLocator` | `ServiceLoaderUtil/CORE: Found 5 implementations of IModFileCandidateLocator` · `100 - com.example.forgecompat.ForgeModLocator from forgecompat.jar` |
| 提供者来源层 | `ModDirTransformerDiscoverer`（`cpw.mods.modlauncher.serviceapi.ITransformerDiscoveryService` 实现）→ `Layer.SERVICE` |
| 加载路径 | `FMLServiceProvider.beginScanning` → `FMLLoader.beginModScan` → `new ModDiscoverer(...)` → `ServiceLoaderUtil.loadServices(ctx, Class)` → `LaunchContext.loadServices` → `ServiceLoader.load(ModuleLayer, Class)` |
| 被排除项 | `mods/forgeprobe.jar` **未**被提升（不在 `Found additional transformation services from discovery services` 列表中），因为它没有 `META-INF/services/` 声明 |

---

## 十、Module Identity

本子 Spike 首次拿到部分身份信息，但**四重一致性仍无法判定**：

| 项 | 值 | 证据 |
|---|---|---|
| `SecureJar.name()` | `forgeprobe` | `[FORGECOMPAT] ForgeJarMetadata: delegate name=forgeprobe version=null` |
| `IModInfo.moduleName()` | **N/A** | `getMods()` 为空列表，不存在 `IModInfo` |
| `ModuleDescriptor.name()` | **UNKNOWN** | 本轮无日志打印该值 |
| `GameLayer.findModule(...)` | **未发生** | 未进入 Module 阶段 |
| **四重一致性** | **UNKNOWN / N/A** | 前两项之一缺失、后两项未发生 |

> 补充：`SecureJar.name() = "forgeprobe"` 来自 `forgeprobe.jar` manifest 的
> `Automatic-Module-Name: forgeprobe`，说明 `JarMetadata.from()` 这条通用路径
> 在 Forge JAR 上**行为正常**（v1.1 §3.2 的 [H] 得到部分回答，但仍需 001a-1 的正规判定）。

---

## 十一、完整异常

### 本子 Spike **未产生任何 Java 异常**，不存在 stack trace / `Caused by` 链。

- 服务端退出码 `0`，达到 `Done (0.323s)!`，经控制台 `stop` 优雅关闭
- **未产生 crash-report**
- `stderr` 文件 0 字节
- 实验关注的 3 个错误文本（`minecraft_forge`、`Skipping jar`、`brokenfile`）**零命中**

**完整相关日志（不截断，按时间顺序）：**

```
[0710月2026 04:50:20.429] [main/INFO] [cpw.mods.modlauncher.Launcher/MODLAUNCHER]: ModLauncher running: args [--launchTarget, forgeserver, --fml.neoForgeVersion, 21.1.256, --fml.fmlVersion, 4.0.45, --fml.mcVersion, 1.21.1, --fml.neoFormVersion, 20240808.144430, nogui]
[0710月2026 04:50:20.435] [main/INFO] [cpw.mods.modlauncher.Launcher/MODLAUNCHER]: JVM identified as Microsoft OpenJDK 64-Bit Server VM 21.0.10+7-LTS
[0710月2026 04:50:20.438] [main/INFO] [cpw.mods.modlauncher.Launcher/MODLAUNCHER]: ModLauncher 11.0.5+main.901c6ea8 starting: java version 21.0.10 by Microsoft; OS Windows 11 arch amd64 version 10.0
[0710月2026 04:50:20.629] [main/DEBUG] [cpw.mods.modlauncher.TransformationServicesHandler/MODLAUNCHER]: Found additional transformation services from discovery services: [D:\DualForge\spike000\neoforge-server\mods\forgecompat.jar]
[0710月2026 04:50:20.647] [main/INFO] [net.neoforged.fml.loading.ImmediateWindowHandler/]: ImmediateWindowProvider not loading because launch target is forgeserver
[0710月2026 04:50:20.708] [main/INFO] [mixin/]: SpongePowered MIXIN Subsystem Version=0.8.7 ... Service=ModLauncher Env=SERVER
[0710月2026 04:50:20.718] [main/DEBUG] [net.neoforged.fml.loading.LaunchContext/SCAN]: Located paths when launch context was created: [ ... , D:\DualForge\spike000\neoforge-server\mods\forgecompat.jar , ... ]
[0710月2026 04:50:20.740] [main/DEBUG] [net.neoforged.fml.util.ServiceLoaderUtil/CORE]: Found 5 implementations of IModFileCandidateLocator:
[0710月2026 04:50:20.747] [main/DEBUG] [net.neoforged.fml.util.ServiceLoaderUtil/CORE]: 	     100 - com.example.forgecompat.ForgeModLocator from forgecompat.jar
[0710月2026 04:50:20.749] [main/DEBUG] [net.neoforged.fml.util.ServiceLoaderUtil/CORE]: Found 3 implementations of IModFileReader:
[0710月2026 04:50:20.749] [main/DEBUG] [net.neoforged.fml.util.ServiceLoaderUtil/CORE]: 	     100 - com.example.forgecompat.ForgeModReader from forgecompat.jar
[0710月2026 04:50:20.750] [main/DEBUG] [net.neoforged.fml.loading.moddiscovery.ModDiscoverer/SCAN]: Scanning for mods and other resources to load. We know 5 ways to find mods
[0710月2026 04:50:20.934] [main/INFO] [net.neoforged.fml.loading.moddiscovery.ModDiscoverer/SCAN]: Found mod file "server-1.21.1-20240808.144430-srg.jar" [locator: production server provider +net.neoforged:neoforge:21.1.256:server]
[0710月2026 04:50:20.956] [main/INFO] [net.neoforged.fml.loading.moddiscovery.ModDiscoverer/SCAN]: Found mod file "neoforge-21.1.256-universal.jar" [locator: PathBasedLocator[name=neoforge, ...], reader: mod manifest]
[0710月2026 04:50:20.956] [main/DEBUG] [net.neoforged.fml.loading.moddiscovery.ModDiscoverer/SCAN]: Trying locator forgecompat locator (mods/forgeprobe.jar only)
[0710月2026 04:50:20.963] [main/INFO] [net.neoforged.fml.loading.moddiscovery.ModDiscoverer/SCAN]: Found mod file "forgeprobe.jar" [locator: forgecompat locator (mods/forgeprobe.jar only)]
[0710月2026 04:50:20.963] [main/DEBUG] [net.neoforged.fml.loading.moddiscovery.ModDiscoverer/SCAN]: Locator forgecompat locator (mods/forgeprobe.jar only) found 1 mods, 0 warnings, 0 errors and skipped 0 candidates
[0710月2026 04:50:20.964] [main/DEBUG] [net.neoforged.fml.loading.moddiscovery.ModDiscoverer/SCAN]: Trying locator {mods folder locator at D:\DualForge\spike000\neoforge-server\mods}
[0710月2026 04:50:20.965] [main/DEBUG] [net.neoforged.fml.loading.moddiscovery.ModDiscoverer/]: Skipping D:\DualForge\spike000\neoforge-server\mods\forgecompat.jar because it was already located earlier
[0710月2026 04:50:20.969] [main/INFO] [net.neoforged.fml.loading.moddiscovery.ModDiscoverer/SCAN]: Found mod file "forgeprobe.jar" [locator: {mods folder locator at D:\DualForge\spike000\neoforge-server\mods}]
[0710月2026 04:50:20.969] [main/DEBUG] [net.neoforged.fml.loading.moddiscovery.ModDiscoverer/SCAN]: Locator {mods folder locator at D:\DualForge\spike000\neoforge-server\mods} found 1 mods, 0 warnings, 0 errors and skipped 1 candidates
[0710月2026 04:50:20.973] [main/DEBUG] [net.neoforged.fml.loading.UniqueModListBuilder/]: Found 2 mods for first modid forgeprobe, selecting most recent based on version data
[0710月2026 04:50:20.973] [main/DEBUG] [net.neoforged.fml.loading.UniqueModListBuilder/]: Selected file forgeprobe.jar for modid forgeprobe with version 0.0NONE
[0710月2026 04:50:21.054] [main/DEBUG] [net.neoforged.fml.loading.moddiscovery.ModFile/LOADING]: Loading mod file D:\DualForge\spike000\neoforge-server\mods\forgeprobe.jar with languages []
[0710月2026 04:50:21.057] [main/DEBUG] [net.neoforged.fml.loading.moddiscovery.ModValidator/SCAN]: Found 4 mod files with 2 mods
[0710月2026 04:50:21.364] [main/INFO] [cpw.mods.modlauncher.LaunchServiceHandler/MODLAUNCHER]: Launching target 'forgeserver' with arguments [nogui]
[0710月2026 04:50:27.206] [modloading-worker-0/INFO] [net.neoforged.neoforge.common.NeoForgeMod/NEOFORGE-MOD]: NeoForge mod loading, version 21.1.256, for MC 1.21.1
[0710月2026 04:50:29.520] [Server thread/INFO] [net.minecraft.server.dedicated.DedicatedServer/]: Done (0.323s)! For help, type "help"
[0710月2026 04:50:41.001] [Server thread/INFO] [net.minecraft.server.MinecraftServer/]: Stopping the server
[0710月2026 04:50:41.113] [Server thread/INFO] [net.minecraft.server.MinecraftServer/]: ThreadedAnvilChunkStorage: All dimensions are saved
```

> 上段是"相关行"的按序摘录。其中两行因原始长度过大而在本报告中以 `...` 省略：
> `Located paths when launch context was created`（单行含 60+ 个 jar 路径）与
> `SpongePowered MIXIN Subsystem ...`（单行含长 union URL）。
> **这两行的完整原文在 §十二 给出的原始日志文件中，未做任何修改。**

**stdout 中兼容层标记的完整序列（不截断）：**

```
[FORGECOMPAT] <clinit> ForgeModLocator
[FORGECOMPAT] INSTANTIATED ForgeModLocator
[FORGECOMPAT] <clinit> ForgeModReader
[FORGECOMPAT] INSTANTIATED ForgeModReader
[FORGECOMPAT] CALLED ForgeModReader.read for D:\...\libraries\net\neoforged\neoforge\21.1.256\neoforge-21.1.256-universal.jar
[FORGECOMPAT] ForgeModReader: no META-INF/mods.toml -> returning null
[FORGECOMPAT] CALLED ForgeModLocator.findCandidates
[FORGECOMPAT] ForgeModLocator: adding candidate D:\...\mods\forgeprobe.jar
[FORGECOMPAT] CALLED ForgeModReader.read for D:\...\mods\forgeprobe.jar
[FORGECOMPAT] ForgeModReader: found META-INF/mods.toml -> building SecureJar + IModFile
[FORGECOMPAT] CALLED ForgeJarMetadata.<init> for D:\...\mods\forgeprobe.jar
[FORGECOMPAT] ForgeJarMetadata: delegate name=forgeprobe version=null
[FORGECOMPAT] <clinit> ForgeModFileInfoParser
[FORGECOMPAT] INSTANTIATED ForgeModFileInfoParser
[FORGECOMPAT] CALLED ForgeModFileInfoParser.build for D:\...\mods\forgeprobe.jar
[FORGECOMPAT] ForgeModFileInfoParser: parsed keys=[modLoader, loaderVersion, license, mods, dependencies]
[FORGECOMPAT] ForgeModReader: returning IModFile Mod File: D:\...\mods\forgeprobe.jar
（……同一文件被 mods-folder locator 再读一次，产生第 2 个 IModFile……）
[FORGECOMPAT] CALLED ForgeModReader.read for            ← 4 次，jar-in-jar / 嵌套内容（getPrimaryPath() 为空）
[FORGECOMPAT] ForgeModReader: no META-INF/mods.toml -> returning null
```

---

## 十二、原始日志

- 最新日志: `D:\DualForge\spike000\logs\spike001a-0\spike001a-0.log`
- debug 日志: `D:\DualForge\spike000\logs\spike001a-0\spike001a-0.debug.log`
- 启动器 stdout: `D:\DualForge\spike000\logs\spike001a-0\spike001a-0.stdout.log`
- 启动器 stderr: `D:\DualForge\spike000\logs\spike001a-0\spike001a-0.stderr.log`（0 字节）
- **crash-report 路径（若存在）: 不存在**
- 运行元数据: `D:\DualForge\spike000\logs\spike001a-0\meta.json`
- 运行脚本（已向后兼容扩展 `-ExtraModsJars`，Spike-000 用法不变）: `D:\DualForge\spike000\run-server.ps1`
- 兼容层工程: `D:\DualForge\spike001a\compat\`（`src/`、`build.gradle`、`build/forgecompat.jar`）
- 服务端目录: `D:\DualForge\spike000\neoforge-server`（官方 installer 产物，未修改核心）

**启动命令（与 Spike-000 完全相同）：**

```
C:\Users\Albert\.jdks\ms-21.0.10\bin\java.exe @user_jvm_args.txt @libraries/net/neoforged/neoforge/21.1.256/win_args.txt nogui
```

---

## 十三、事实标签

### [F] Source Fact

1. `[F]` `TransformerDiscovererConstants.SERVICES`（`loader-4.0.45.jar`）恰为 6 个类型：
   `ITransformationService`、**`IModFileCandidateLocator`**、**`IModFileReader`**、
   `IDependencyLocator`、`GraphicsBootstrapper`、`ImmediateWindowProvider`。
2. `[F]` `TransformerDiscovererConstants.shouldLoadInServiceLayer(JarContents)` 的判据是
   `JarMetadata.from(jar).providers()` 中任一 provider 的 serviceName 落入上述集合；
   **不读取 manifest 的 `FMLModType`**。
3. `[F]` `ModDirTransformerDiscoverer.implements cpw.mods.modlauncher.serviceapi.ITransformerDiscoveryService`，
   其 `candidates(gameDir, launchTarget)` → `scan(gameDir)` 遍历 `<gameDir>/mods`（`Files.walk(..., 1)`），
   对命中 `shouldLoadInServiceLayer` 的 `*.jar` 产出 `NamedPath`。
   （该实现由 `loader-4.0.45.jar!/META-INF/services/cpw.mods.modlauncher.serviceapi.ITransformerDiscoveryService` 注册，
   同文件另注册 `ClasspathTransformerDiscoverer`。）
4. `[F]` ModLauncher `TransformationServicesHandler.discoverServices(DiscoveryData)`
   把发现到的候选经 `ModuleLayerHandler.addToLayer(Layer.SERVICE, NamedPath)` 放入 **SERVICE** 层。
5. `[F]` `LaunchContext.loadServices(Class)` 对 `Layer.values()` **逆序**遍历，
   取第一个存在且未被 `visitLayer` 标记过的层做 `ServiceLoader.load(ModuleLayer, Class)`，
   并同时标记该层及其全部 parent（说明该 API 会连带搜索父层）。
6. `[F]` `ModLauncher` 的 `IModuleLayerManager.Layer` 枚举为 `BOOT(0) → SERVICE(1) → PLUGIN(2) → GAME(3)`。
7. `[F]` `ModDiscoverer` 的构造器内部即完成
   `ServiceLoaderUtil.loadServices(ctx, IModFileCandidateLocator.class)` 与
   `loadServices(ctx, IModFileReader.class)`，先于 locator 扫描执行。
8. `[F]` `UniqueModListBuilder.getModId(ModFile)`：`getModFileInfo().getMods()` 非空则用首个 mod 的 modId，
   **为空则回退到 `SecureJar.name()`**；这也解释了下文 `modid forgeprobe` 的来源。

### [E] Experimental Fact

1. `[E]` **001a-0 PASS**：`ForgeModReader` 与 `ForgeModLocator` 均被实例化并被 ServiceLoader 列出。
2. `[E]` `Found additional transformation services from discovery services: [ ...\mods\forgecompat.jar ]`
   —— 该行在 baseline / Spike-000-forgeprobe 两次运行中均为**空列表**。
3. `[E]` `mods/forgecompat.jar` 出现在 `Located paths when launch context was created` 中，
   证明其已成为 SERVICE 层的模块。
4. `[E]` `IModFileReader` 实现数 2 → 3；`IModFileCandidateLocator` 实现数 4 → 5；
   增量即本次兼容层的两个 provider，优先级均为 100。
5. `[E]` `ForgeModReader.read()` 以最高优先级（100）先于内置 reader 处理**每一个** JAR，
   包括 NeoForge 自带 `neoforge-21.1.256-universal.jar` 与 4 个路径为空（jar-in-jar）的内容；
   对这些一律返回 `null`。
6. `[E]` `SecureJar.name()` 在 Forge JAR 上得到 `forgeprobe`
   （来自 `Automatic-Module-Name`），`JarMetadata.from()` 这条通用路径未报错。
7. `[E]` Spike-000 的失败标志在本轮**全部消失**：三份日志中
   `minecraft_forge` / `Skipping jar` / `brokenfile` **零命中**。
8. `[E]` 服务端仍为退出码 0、`Done (0.323s)`、无 crash-report、`stderr` 0 字节。
9. `[E]` 兼容层 JAR 自身不会再被当作 mod 读入：
   `Skipping ...\mods\forgecompat.jar because it was already located earlier`。
10. `[E]` `ForgeProbe` 构造函数输出（`FORGE PROBE CONSTRUCTOR REACHED`）**仍然零命中**。

### [H] Hypothesis

1. `[H]` 若从 `forgecompat.jar` 中移除 `FMLModType: LIBRARY`（保留 services 声明），
   provider 仍应被提升进 SERVICE 层（依据 [F]2 的判据），但该 JAR 之后是否被当作
   library mod file 接受会改变。**本轮未做该对照，未验证。**
2. `[H]` 若移除 `META-INF/services/` 中的两个声明（保留 `FMLModType: LIBRARY`），
   provider 将**不会**被提升，001a-0 会转为 FAIL。本轮未做该对照。
3. `[H]` `forgeprobe.jar` 的类扫描（`ModFileScanData`）可能已发生但未在日志中留下可辨识记录；
   本轮无法区分"未发生"与"发生但无日志"。
4. `[H]` 双 `IModFile`（兼容层 locator 与 mods-folder locator 各造一个）之所以未触发
   `fml.modloadingissue.duplicate_mod`，是因为 `UniqueModListBuilder` 按 modId 去重后
   选中了同一文件；若两者 modId 不同，结果可能改变。

### [D] Design Decision

1. `[D]` v1.1 手册 §3.1 中"provider JAR 需要 `FMLModType: LIBRARY` 才能进入 Plugin Layer"的表述，
   对 `IModFileReader` / `IModFileCandidateLocator` 应修正为
   "在 `META-INF/services/` 中声明 `TransformerDiscovererConstants.SERVICES` 白名单内的服务，
   由 `ModDirTransformerDiscoverer` 提升进 SERVICE 层"。是否需要同时保留 `FMLModType`，
   待架构决策方决定是否补一个分离对照实验。
2. `[D]` 兼容层 JAR 的 `getPriority()` 取值（本轮取 100）会决定它是否先于内置 reader 处理所有 JAR，
   这是一个需要显式设计的策略点。
3. `[D]` `IModInfo` 的铸造被推迟到 `ForgeLanguageLoader` 存在之后（§2.2 边界），
   这是 `getMods()` 为空、以及 `forgeprobe.jar` 未出现在最终 Mod List 的直接原因。

---

## 十四、结论

**001a-0 唯一问题**：`ForgeModReader` 是否被 ServiceLoader 发现/实例化？

**答案**：**是。PASS。**

**Confidence**: **HIGH**

**Evidence**:

1. `[FORGECOMPAT] INSTANTIATED ForgeModReader` / `INSTANTIATED ForgeModLocator`（stdout）
2. `ServiceLoaderUtil/CORE: Found 3 implementations of IModFileReader` 含
   `100 - com.example.forgecompat.ForgeModReader from forgecompat.jar`
3. `ServiceLoaderUtil/CORE: Found 5 implementations of IModFileCandidateLocator` 含
   `100 - com.example.forgecompat.ForgeModLocator from forgecompat.jar`
4. 机制层证据：`Found additional transformation services from discovery services: [...\mods\forgecompat.jar]`
   + 字节码 `TransformerDiscovererConstants.SERVICES` / `ModDirTransformerDiscoverer.scan`
5. 对照组：同两行在 baseline 与 Spike-000-forgeprobe 运行中分别为空列表 / 计数为 2 与 4

**Hypotheses still unverified**（见第十三节 [H]）:

1. `FMLModType: LIBRARY` 的运行时必要性（本轮未能与其分离）
2. 移除 services 声明后是否必然 FAIL（未做负对照）
3. `forgeprobe.jar` 的类扫描是否真的发生
4. 双 `IModFile` 去重机制的完整语义

---

## 十五、禁止事项确认

- [x] 未修改 forgeprobe.jar
      （SHA-256 仍为 `a93a823c…374e22`，与 Spike-000 一致）
- [x] 未添加 neoforge.mods.toml
      （`forgeprobe.jar` 的 `jar tf` 与 Spike-000 相同；`forgecompat.jar` 也不含该文件）
- [x] 未修改 NeoForge 核心
      （`neoforge-server/` 仅新增 `mods/forgecompat.jar` 与运行期产物）
- [x] 未引入 ASM / Mapping / Mixin / Registry / Event / Isolation
      （`spike001a/compat/src` 仅 4 个类，全部为 Discovery 边界组件；无字节码操作、无事件/注册表代码）
- [x] 未修改 `FMLJavaModLoadingContext` ABI
      （未触碰任何 ABI 相关代码）
- [x] 未提出修复方案
      （第六节为机制定位，第十三节 [D] 为交接建议，均不含实现方案）
- [x] 未跳过子 Spike
      （本报告只交付 001a-0 的判定；001a-1 / -2 / -3 的原始证据见附录 A，**未作判定**）

---

## 附录 A：附带观测（**未按子 Spike 判定**，仅供架构决策方使用）

> 按 Spike_001a.md §七，完成 001a-0 后立即交回，不得跳过子 Spike。
> 但同一次运行天然产生了下列证据。此处**只记录原始观测**，
> 不对 001a-1 / 001a-2 / 001a-3 下 PASS/FAIL 结论——该判定权归架构决策方。

### A.1 与 001a-1 相关的观测（Reader 是否收到 Forge JAR 并返回非 null IModFile）

```
[FORGECOMPAT] CALLED ForgeModReader.read for D:\...\mods\forgeprobe.jar
[FORGECOMPAT] ForgeModReader: found META-INF/mods.toml -> building SecureJar + IModFile
[FORGECOMPAT] CALLED ForgeJarMetadata.<init> for D:\...\mods\forgeprobe.jar
[FORGECOMPAT] ForgeJarMetadata: delegate name=forgeprobe version=null
[FORGECOMPAT] <clinit> ForgeModFileInfoParser
[FORGECOMPAT] INSTANTIATED ForgeModFileInfoParser
[FORGECOMPAT] CALLED ForgeModFileInfoParser.build for D:\...\mods\forgeprobe.jar
[FORGECOMPAT] ForgeModFileInfoParser: parsed keys=[modLoader, loaderVersion, license, mods, dependencies]
[FORGECOMPAT] ForgeModReader: returning IModFile Mod File: D:\...\mods\forgeprobe.jar
```

观测要点：`read()` 被调用；`META-INF/mods.toml` 被识别；`SecureJar` 构造成功；
`IModFile.create(...)` 返回非 null；**无异常**。

### A.2 与 001a-2 相关的观测（Discovery 是否真正接受该 JAR）

```
[main/INFO] [net.neoforged.fml.loading.moddiscovery.ModDiscoverer/SCAN]: Found mod file "forgeprobe.jar" [locator: forgecompat locator (mods/forgeprobe.jar only)]
[main/DEBUG] [net.neoforged.fml.loading.moddiscovery.ModDiscoverer/SCAN]: Locator forgecompat locator (mods/forgeprobe.jar only) found 1 mods, 0 warnings, 0 errors and skipped 0 candidates
```

且 `minecraft_forge` 在三份日志中**零命中**。

另注：Spike_001a.md 给的 PASS 形态是 `Considering mod file candidate ... forgeprobe.jar`。
该字符串属 `ModFileParser` 的日志，**本轮确实出现了同类行**，例如：

```
[main/DEBUG] [net.neoforged.fml.loading.moddiscovery.ModFileParser/LOADING]: Considering mod file candidate D:\...\libraries\net\neoforged\neoforge\21.1.256\neoforge-21.1.256-universal.jar
```

但对 `forgeprobe.jar` 未打印该行——因为它的 `IModFileInfo` 由兼容层 parser 直接产出，
未走 `ModFileParser.readModList`。**该差异是否影响 001a-2 的判定口径，请决策方明示。**

### A.3 与 001a-3 相关的观测（是否进入 Validation）

```
[main/DEBUG] [net.neoforged.fml.loading.moddiscovery.ModFile/LOADING]: Loading mod file D:\DualForge\spike000\neoforge-server\mods\forgeprobe.jar with languages []
[main/DEBUG] [net.neoforged.fml.loading.moddiscovery.ModValidator/SCAN]: Found 4 mod files with 2 mods
```

观测要点：`forgeprobe.jar` 出现在 `ModValidator` 的输入集合（4 个 mod file）中；
`with languages []` 表明其 `IModFileInfo.getMods()` 为空，因此未请求任何语言加载器；
最终 `Mod List` 中仍只有 `Minecraft` / `NeoForge` 两项（`forgeprobe` 未列入）。

### A.4 其他需要决策方注意的观测

1. **同一文件被读两遍**：`forgeprobe.jar` 先后被
   `forgecompat locator` 与内置 `mods folder locator` 各读一次，产生 2 个 `IModFile`，
   由 `UniqueModListBuilder` 去重：
   ```
   Found 2 mods for first modid forgeprobe, selecting most recent based on version data
   Selected file forgeprobe.jar for modid forgeprobe with version 0.0NONE
   ```
   其中 `modid forgeprobe` 与 `version 0.0NONE` 都来自 `getMods()` 为空时的兜底
   （`SecureJar.name()` / `ModFile.getJarVersion()` 默认值），**不是**解析出的 modId/version。
2. **reader 优先级 100 意味着它先于所有内置 reader 处理每个 JAR**，
   包括 NeoForge 自己的 jar。当前实现以"无 `META-INF/mods.toml` 即返回 null"兜底，
   行为安全；但这是一个需要显式设计的策略点。

---

## 附录 B：复现方式

```
# 1. 构建兼容层工件
#    见第三节的 javac 命令；产物 = spike001a\compat\build\forgecompat.jar
#    （SHA-256 68d74a50745d6712888605e4abbbefac2827b91aa7d9b25a967fd0ecae6a8b5a）

# 2. 运行 001a-0（mods/ = forgeprobe.jar + forgecompat.jar）
powershell -NoProfile -ExecutionPolicy Bypass -File spike000\run-server.ps1 `
    -Label spike001a-0 `
    -ModsJar spike000\probe\build\forgeprobe.jar `
    -ExtraModsJars spike001a\compat\build\forgecompat.jar
```

> `run-server.ps1` 新增可选参数 `-ExtraModsJars`（默认空数组），
> 对 Spike-000 的两次运行方式无影响；每次运行前仍清空 `mods/*.jar` 并写入同一份 `server.properties`。
