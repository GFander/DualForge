# Spike-001b-5 实验报告

> **结论（先行）**
>
> **问题 A（ModSorter 类型契约）**：`ModInfo` **非 final**、**只有一个 public 构造函数**
> `ModInfo(ModFileInfo, IConfigurable)`，**owning file 参数是具体类 `ModFileInfo`（不是 `IModFileInfo`）**，
> **无 static factory**。`ModSorter` 内共 **7 处 `checkcast ModInfo`**。
> 更深一层：`LoadingModList.<init>` 还会做 **`ModFileInfo.class::cast`**，
> 即 `IModFile` 的 `IModFileInfo` **必须**是 `ModFileInfo`。
>
> **问题 B（正常 Java 路径）**：**YES**。`ModFile` / `ModFileInfo` / `ModInfo` 的构造函数**全部 public**，
> 且 `fml_loader` 是自动模块（全包导出），`javac` 实测编译通过（A1 直接构造 + A2 继承均 PASS）。
>
> **运行时验证（001b-5-2）**：成功产出**真正的 `ModInfo`**；001b-3 的
> `IModInfo → ModInfo` CCE **已消失**；流程推进到 `LoadingModList`，
> 在**新的** `IModFileInfo → ModFileInfo` CCE 处终止。
>
> **清理（001b-5-4）**：15/15 JAR 条目哈希与 001b-1 完全一致。
>
> 按 Spike_001b-5.md §九：**不进入 001b-2 / 001c**，报告原样交回。

---

## 一、实验环境

```
Minecraft:            1.21.1
NeoForge:             21.1.256
FancyModLoader:       4.0.45
Java:                 21（OpenJDK 21.0.10，Microsoft Build，21.0.10+7-LTS）
OS:                   Windows 11 (amd64) version 10.0
启动方式:             CLI（官方 installer --installServer 产物 + java @argfile nogui）
客户端/服务端:        Server（Dedicated Server，launch target = forgeserver）

编译依赖 Forge:       1.21.1-52.1.0
```

运行标签：`spike001b-5`（`mods/` = `forgecompat.jar`（001b-5 临时版）+ `forgeprobe.jar`）。

---

## 二、实验目标

**问题 A**：`ModSorter` 的实际类型契约是什么？

**问题 B**：兼容层能否在"不修改 NeoForge 核心"的条件下，通过**正常 Java 路径**
产生一个可被接受的 `net.neoforged.fml.loading.moddiscovery.ModInfo`？

> 路径分级：A = 正常 Java 路径（public / 可继承）；B = 非常规（反射 / Unsafe / 字节码）；
> C = 完全无法做到。**只有 A 才能转化为架构决策。**

---

## 三、源码契约

### ModInfo

```
class:                    net.neoforged.fml.loading.moddiscovery.ModInfo
package:                  net.neoforged.fml.loading.moddiscovery
final:                    NO（public class，非 final）
interfaces:               net.neoforged.neoforgespi.language.IModInfo,
                          net.neoforged.neoforgespi.language.IConfigurable
constructors:             [ public ModInfo(ModFileInfo owningFile, IConfigurable config) ]   ← 仅此一个
constructor visibility:   public
constructor parameters:   (1) net.neoforged.fml.loading.moddiscovery.ModFileInfo
                          (2) net.neoforged.neoforgespi.language.IConfigurable
owning file 参数类型:     **ModFileInfo（具体类）**，不是 IModFileInfo，也不是 ModFile
factory methods:          不存在（无 static factory / create / of / from / parse）
```

`ModInfo` 构造函数从 `IConfigurable` 读取的键（字节码常量）：

| 键 | 必需性 | 证据 |
|---|---|---|
| `modId` | **必需** | `getConfigElement("modId").orElseThrow(...)` → `InvalidModFileException` |
| `namespace` | 可选 | `.orElse(...)` |
| `version` | 可选 | `.map(...).orElse(DEFAULT_VERSION)` |
| `displayName` | 可选 | `.orElse(...)` |
| `description` | 可选 | 默认 `"MISSING DESCRIPTION"` |
| `logoFile` / `logoBlur` | 可选 | `.orElseGet(...)` |
| 其余（features / properties 等） | 可选 | `getConfigList` / `orElse` |

### 前置构造链（同样全部 public）

```
public ModFile(cpw.mods.jarhandling.SecureJar, ModFileInfoParser, ModFileDiscoveryAttributes)
public ModFile(SecureJar, ModFileInfoParser, IModFile$Type, ModFileDiscoveryAttributes)

public ModFileInfo(ModFile, IConfigurable, Consumer<IModFileInfo>)
public ModFileInfo(ModFile, IConfigurable, Consumer<IModFileInfo>, List<IModFileInfo$LanguageSpec>)
```

**模块可达性**：`loader-4.0.45.jar` 的 manifest 为
`Automatic-Module-Name: fml_loader`，**无 `module-info.class`** → 自动模块 → **所有包均导出**
→ `net.neoforged.fml.loading.moddiscovery` 对其它模块（含 `forgecompat`）**可访问**。

### ModSorter

```
cast location:  ModSorter 内共 7 处 checkcast ModInfo
  1) private void addDependency(MutableGraph<ModInfo>, IModInfo$ModVersion)          ×2 处
  2) private void lambda$sort$6(List, MutableGraph, String, List)                    ×2 处
  3) private static ModInfo lambda$sort$0/1/2(ModFile)                               ×3 处
001b-3 命中的那一处 = ModSorter.sort（栈帧 ModSorter.java:112），
                  即 (ModInfo) <某 IModInfo>
后续直接成员访问: ModSorter 仅以 ModInfo 作为图的节点类型
                   (MutableGraph<ModInfo> / Map<ModInfo, List<ModInfo>>)，
                   未观察到对 ModInfo 私有字段的直接读取
source/bytecode evidence: javap -p -c -l -constants loader-4.0.45.jar → ModSorter
```

### 新发现的更深契约：`LoadingModList.<init>`

```
字段声明:
  private final List<IModFileInfo> plugins;
  private final List<ModFileInfo> modFiles;          ← 具体类 ModFileInfo
  private final List<ModInfo> sortedList;            ← 具体类 ModInfo
  private final Map<ModInfo, List<ModInfo>> modDependencies;
  private final Map<String, ModFileInfo> fileById;

构造器字节码（偏移 → 行号）:
  line 54: 47   List.stream()
  line 54: 59   invokedynamic #0  →  ModFile::getModFileInfo        （返回 IModFileInfo）
  line 55: 52   ldc   class net/neoforged/fml/loading/moddiscovery/ModFileInfo
  line 55: 55   Objects.requireNonNull(Class)
  line 55: 59   invokedynamic #1  →  **ModFileInfo.class::cast**    ← **新 CCE 位置**
  line 56: 69   Collectors.toList()
  …
  line 58: 90   ldc   class .../ModInfo
  line 58: 97   invokedynamic #2  →  ModInfo.class::cast
```

> **[F] 结论**：`LoadingModList` 要求 **每个 `ModFile.getModFileInfo()` 的返回值都是 `ModFileInfo`**，
> 且排序后的 mod 列表每个元素都是 `ModInfo`。
> 只满足 `IModInfo → ModInfo` **不足以**通过 —— `IModFileInfo → ModFileInfo` 是**更外层的同一道门**。

### NeoForge 内部 ModInfo 创建点

| 位置 | 是否创建 ModInfo | 可见性 / 第三方可调用性 |
|---|---|---|
| `ModFileInfo` | **是（1 处）**：`new ModInfo(<this>, config)` | `ModFileInfo` 构造器 **public** → 第三方可走同一路径 |
| `ModFileParser` | **否** | — |
| `ModFile` | **否** | — |
| `ModInfo` static factory | **不存在** | — |

---

## 四、正常 Java 构造可行性

### A1 直接构造

- **compile: PASS**
- evidence：独立编译探针 `D:\DualForge\spike001b5\probe\ModInfoContractProbe.java`
  （**不参与 forgecompat.jar 打包**），`javac --release 21` **exit 0**，无 warning、无 error。
  产出的 class：`ModInfoContractProbe.class`、`ModInfoContractProbe$ModInfoSubclass.class`。

  探针内的调用链：

  ```java
  SecureJar secureJar = SecureJar.from(jar);
  ModFile modFile = new ModFile(secureJar, parser, ModFileDiscoveryAttributes.DEFAULT);
  ModFileInfo modFileInfo = new ModFileInfo(modFile, config, (Consumer<IModFileInfo>) info -> { });
  ModInfo modInfo = new ModInfo(modFileInfo, config);
  ```

  另含 `ModFileInfo` 的 4 参重载（带 `List<IModFileInfo.LanguageSpec>`），同样编译通过。
- error: 无
- 需要的不可访问类型: **无**
- 需要的包访问: **无**（自动模块全包导出）

### A2 继承

- **possible: YES**
- evidence:
  ```java
  public static final class ModInfoSubclass extends ModInfo {
      public ModInfoSubclass(ModFileInfo owningFile, IConfigurable config) { super(owningFile, config); }
  }
  ```
  编译通过（`javac` exit 0）。
- **父类构造函数可调用性: YES**（`ModInfo` 的唯一构造函数为 `public`，且 `ModInfo` 非 final）

### A3 官方工厂

- **exists: NO**
- evidence: `javap -p ModInfo` 未列出任何 `static` 工厂方法；
  `ModInfo` 的全部实例化点只有 `ModFileInfo` 内部的一处 `new`。
- accessible: **N/A**

### 正常路径结论：**YES**

> 三层均为正常 Java 路径（A 类）：无需反射、无需 `Unsafe`、无需字节码注入、
> 无需修改 NeoForge 核心。**因此不触发 001b-5-3 的非常规路径记录。**

---

## 五、运行时最小验证（001b-5-2）

**临时 language provider**：

- name: `javafml`
- version: `9999`
- 实例化: **YES**（`[FORGECOMPAT][LANG] INSTANTIATED #1`）
  → `Found 4 implementations of IModLanguageLoader`；`languageProviderMap["javafml"]` 最终为 9999（复用 001b-3 已证明的机制）

**getMods()**：

- returned type: `List<IModInfo>`
- **returned class: `net.neoforged.fml.loading.moddiscovery.ModInfo`（真实类型，非兼容层自有实现）**
- modId: `forgeprobe`（version `1.0.0`）
- 证据（`stdout`）：
  ```
  [FORGECOMPAT] buildNativeMods: created REAL net.neoforged.fml.loading.moddiscovery.ModInfo modId=forgeprobe version=1.0.0
  ```

**ModSorter**：

- entered: **YES**
- **ClassCastException（001b-3 那一道 `IModInfo → ModInfo`）: 已消失** ——
  `stderr` 中不再出现 `Cannot cast …ForgeModInfo to …ModInfo`。
- result: **在新的 CCE 处终止**（见下）

**第一个新失败点**：

- stage: `FMLLoader.completeScan → ModValidator.stage2Validation → ModSorter.sort → LoadingModList.of → LoadingModList.<init>`
- exception: `java.lang.ClassCastException: Cannot cast com.example.forgecompat.ForgeModFileInfo to net.neoforged.fml.loading.moddiscovery.ModFileInfo`
- call chain（`stderr` 原文，未截断）：

```
Exception in thread "main" java.lang.ClassCastException: Cannot cast com.example.forgecompat.ForgeModFileInfo to net.neoforged.fml.loading.moddiscovery.ModFileInfo
	at java.base/java.lang.Class.cast(Class.java:4069)
	at java.base/java.util.stream.ReferencePipeline$3$1.accept(ReferencePipeline.java:197)
	at java.base/java.util.stream.ReferencePipeline$3$1.accept(ReferencePipeline.java:197)
	at java.base/java.util.AbstractList$RandomAccessSpliterator.forEachRemaining(AbstractList.java:722)
	at java.base/java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:499)
	at java.base/java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:499)
	at java.base/java.util.stream.ReduceOps$ReduceOp.evaluateSequential(ReduceOps.java:921)
	at java.base/java.util.stream.AbstractPipeline.evaluate(AbstractPipeline.java:234)
	at java.base/java.util.stream.ReferencePipeline.collect(ReferencePipeline.java:682)
	at MC-BOOTSTRAP/fml_loader@4.0.45/net.neoforged.fml.loading.LoadingModList.<init>(LoadingModList.java:56)
	at MC-BOOTSTRAP/fml_loader@4.0.45/net.neoforged.fml.loading.LoadingModList.of(LoadingModList.java:70)
	at MC-BOOTSTRAP/fml_loader@4.0.45/net.neoforged.fml.loading.ModSorter.sort(ModSorter.java:82)
	at MC-BOOTSTRAP/fml_loader@4.0.45/net.neoforged.fml.loading.FMLLoader.completeScan(FMLLoader.java:138)
	at MC-BOOTSTRAP/fml_loader@4.0.45/net.neoforged.fml.loading.moddiscovery.ModValidator.stage2Validation(ModValidator.java:96)
	at MC-BOOTSTRAP/fml_loader@4.0.45/net.neoforged.fml.loading.FMLServiceProvider.completeScan(FMLServiceProvider.java:90)
	at MC-BOOTSTRAP/cpw.mods.modlauncher@11.0.5/cpw.mods.modlauncher.TransformationServiceDecorator.onCompleteScan(TransformationServiceDecorator.java:103)
	at MC-BOOTSTRAP/cpw.mods.modlauncher@11.0.5/cpw.mods.modlauncher.TransformationServicesHandler.lambda$triggerScanCompletion$23(TransformationServicesHandler.java:139)
	at java.base/java.util.HashMap$ValueSpliterator.forEachRemaining(HashMap.java:1787)
	at java.base/java.util.stream.AbstractPipeline.copyInto(AbstractPipeline.java:499)
	at java.base/java.util.stream.AbstractPipeline.wrapAndCopyInto(AbstractPipeline.java:499)
	at java.base/java.util.stream.ReduceOps$ReduceOp.evaluateSequential(ReduceOps.java:921)
	at java.base/java.util.stream.AbstractPipeline.evaluate(AbstractPipeline.java:234)
	at java.base/java.util.stream.AbstractPipeline.evaluateToArrayNode(AbstractPipeline.java:260)
	at java.base/java.util.stream.ReferencePipeline.toArray(ReferencePipeline.java:616)
	at java.base/java.util.stream.ReferencePipeline.toArray(ReferencePipeline.java:622)
	at java.base/java.util.stream.ReferencePipeline.toList(ReferencePipeline.java:627)
	at MC-BOOTSTRAP/cpw.mods.modlauncher@11.0.5/cpw.mods.modlauncher.TransformationServicesHandler.triggerScanCompletion(TransformationServicesHandler.java:141)
	at MC-BOOTSTRAP/cpw.mods.modlauncher@11.0.5/cpw.mods.modlauncher.Launcher.run(Launcher.java:91)
	at MC-BOOTSTRAP/cpw.mods.modlauncher@11.0.5/cpw.mods.modlauncher.Launcher.main(Launcher.java:74)
	at cpw.mods.bootstraplauncher@2.0.2/cpw.mods.bootstraplauncher.BootstrapLauncher.run(BootstrapLauncher.java:210)
	at cpw.mods.bootstraplauncher@2.0.2/cpw.mods.bootstraplauncher.BootstrapLauncher.main(BootstrapLauncher.java:69)
```

- 进程退出码 **1**，未达 `Done`，未生成 FML 崩溃报告。

> **本 Spike 未对该新失败点做任何修复**（符合 §001b-5-2 "不得顺手修复"）。

**临时改动（已撤销，记录于此）**：`ForgeModFileInfo.buildNativeMods()` 走
`new ModFileInfo(modFile, rootConfig, info -> {})` → `new ModInfo(nativeInfo, modEntryConfig)`；
其中 `rootConfig` 提供文件级键（`modLoader` / `loaderVersion` / `license`），
`modEntryConfig` 提供 `[[mods]]` 条目级键（`modId` / `version` / `displayName`）。

---

## 六、非常规路径（若有）

**未执行**。原因：001b-5-1 的三层（A1 直接构造 / A2 继承 / A3 官方工厂）中，
A1 与 A2 均以**正常 Java 路径**编译通过，A3 属性为"不存在官方工厂"而非"存在但不可访问"。
按 Spike_001b-5.md §001b-5-3 的前置条件（"仅当 A1 / A2 / A3 均失败"），本步骤**不适用**。

| 机制 | 是否使用 |
|---|---|
| reflection | 否 |
| MethodHandles | 否 |
| Unsafe | 否 |
| package injection | 否 |
| bytecode | 否 |

**是否可作为正常架构路径: YES**（本 Spike 走的**就是**正常路径）

---

## 七、Failure Chain

```
Discovery:       YES
Metadata:        YES
Validation:      YES
Scan:            UNKNOWN
LanguageLoader:  YES
ModSorter:       YES（被调用；且类型检查失败于其下游 LoadingModList 构建）
Module:          UNKNOWN
Container:       UNKNOWN
Constructor:     UNKNOWN
```

> 纪律：**ModSorter 被调用 ≠ ModSorter 成功 ≠ Module 阶段进入**。
> 本轮 `ModSorter.sort` 被调用（`ModValidator.stage2Validation` → `ModSorter.sort`），
> 但在其内部调用 `LoadingModList.of` 时抛出 CCE，**未完成排序**，故 Module 记为 UNKNOWN。
> `Scan` 与 001b-1 / 001b-3 一致，仍无直接证据，记 UNKNOWN。

---

## 八、清理确认

**001b-5-4 已执行**：删除 `ForgeCompatLanguageLoader.java` 与其 service 声明，
并将 `ForgeModFileInfo.java` **还原到 001b-1 基线**（移除 `buildNativeMods()` / `castMap()` 及三个新增 import），
重新编译重打包。

逐条目 SHA-256 比对（001b-1 留档 `build\forgecompat-001b1.jar` ↔ 清理后 `build\forgecompat.jar`）：

| 项 | 001b-1 值 | 清理后值 | 一致? |
|---|---|---|---|
| `ForgeCompatLanguageLoader.class` | 不存在 | 不存在 | **YES** |
| `META-INF/services/…IModLanguageLoader` | 不存在 | 不存在 | **YES** |
| `META-INF/services/…IModFileReader` | `394a0017ea04610a…` | `394a0017ea04610a…` | **YES** |
| `META-INF/services/…IModFileCandidateLocator` | `2e232a7c8c1ed34a…` | `2e232a7c8c1ed34a…` | **YES** |
| `MANIFEST.MF` | `7b8130925631a919…` | `7b8130925631a919…` | **YES** |
| `ForgeModFileInfo.class` | `1cf3ec65decfa409…` | `1cf3ec65decfa409…` | **YES** |
| 其余 9 个条目 | — | 逐条目哈希相同 | **YES** |

**same = 15 / diff = 0。**

```
001b-1 整体 SHA-256 : 14efd54fa91e415c5815972b81d16c2895dd9ed52a427154db1261efa532e334
清理后整体 SHA-256 : e7f19a161dea560a6606a7462b479d11600dcefcea793f75d15add15a92edbb
两者文件大小均为    : 13215 字节
```

**整体哈希未恢复**（差异仅来自 ZIP 容器元数据；15 个条目逐条目一致）。
按 001b-5-4 的标准（内容一致而非 SHA-256 恢复），**判定通过，不构成实验污染**。

清单确认：

- [x] `ForgeCompatLanguageLoader` 已删除（源文件 + `.class` + service 声明）
- [x] `IModLanguageLoader` service 声明已删除
- [x] `IModFileReader` service 与基线一致
- [x] `IModFileCandidateLocator` service 与基线一致
- [x] `MANIFEST` 与基线一致
- [x] `forgeprobe.jar` 未修改（SHA-256 `a93a823c…374e22`）
- [x] `ForgeModFileInfo.class` 与基线一致

---

## 九、完整日志

- latest.log: `D:\DualForge\spike000\logs\spike001b-5\spike001b-5.log`
- debug.log: `D:\DualForge\spike000\logs\spike001b-5\spike001b-5.debug.log`
- stdout: `D:\DualForge\spike000\logs\spike001b-5\spike001b-5.stdout.log`
  （含 `buildNativeMods: created REAL net.neoforged.fml.loading.moddiscovery.ModInfo modId=forgeprobe version=1.0.0`）
- stderr: `D:\DualForge\spike000\logs\spike001b-5\spike001b-5.stderr.log`
  （含完整 `ClassCastException` 栈，见 §五）
- crash-report: **不存在**
- meta.json: `status = EXITED_EARLY`，`exitCode = 1`，`durationSeconds = 6.1`

编译探针（不参与打包）：`D:\DualForge\spike001b5\probe\ModInfoContractProbe.java`
→ 编译产物 `D:\DualForge\spike001b5\build\classes\`

---

## 十、事实标签

### [F] Source Fact

1. `[F]` `ModInfo` 为 **non-final public class**，`implements IModInfo, IConfigurable`，
   **只有一个构造函数**：`public ModInfo(ModFileInfo, IConfigurable)`。
2. `[F]` `ModInfo` 的 **owning file 参数是具体类 `ModFileInfo`**，不是 `IModFileInfo`。
3. `[F]` `ModInfo` **无 static factory**；其唯一实例化点是 `ModFileInfo` 内部的
   `new ModInfo(this, config)`（1 处）。
4. `[F]` `ModFile`（2 个）与 `ModFileInfo`（2 个）的构造函数**全部 public**。
5. `[F]` `loader-4.0.45.jar` 的 manifest 为 `Automatic-Module-Name: fml_loader`，
   **无 `module-info.class`** → 自动模块 → 所有包导出，`net.neoforged.fml.loading.moddiscovery` 对外可访问。
6. `[F]` `ModSorter` 内共 **7 处 `checkcast ModInfo`**，分布在
   `addDependency`（2）、`lambda$sort$6`（2）、`lambda$sort$0/1/2(ModFile)`（3）。
7. `[F]` `LoadingModList` 的字段类型为 `List<ModFileInfo> modFiles` 与 `List<ModInfo> sortedList`；
   其构造器对 `List<ModFile>` 依次做 `ModFile::getModFileInfo` 与
   **`ModFileInfo.class::cast`**，即要求每个 `ModFile` 的 `IModFileInfo` **必须是 `ModFileInfo`**。
8. `[F]` `ModInfo` 构造函数从 config 读取的键中，**只有 `modId` 是必需**
   （`orElseThrow` → `InvalidModFileException`），其余均有默认值或可空。

### [E] Experimental Fact

1. `[E]` **正常 Java 路径存在**：`new ModFile(...)` → `new ModFileInfo(...)` → `new ModInfo(...)`
   全部编译通过（`javac --release 21` exit 0）。
2. `[E]` **继承可行**：`class X extends ModInfo` 编译通过（父类构造函数 public）。
3. `[E]` 运行时成功产出**真实** `net.neoforged.fml.loading.moddiscovery.ModInfo`
   （`modId=forgeprobe version=1.0.0`），由 `ForgeModFileInfo.getMods()` 返回。
4. `[E]` **001b-3 的 `IModInfo → ModInfo` CCE 消失** —— `stderr` 中不再出现该转换失败。
5. `[E]` 流程推进到 `LoadingModList`，在 **`IModFileInfo → ModFileInfo`** 的
   `ClassCastException` 处终止（`LoadingModList.<init>` 报告行 56，实际 cast 表达式在行 55）。
6. `[E]` 退出码 1，未达 `Done`，未生成 FML 崩溃报告。
7. `[E]` 清理后 15/15 条目哈希与 001b-1 一致。

### [H] Hypothesis

1. `[H]` 若 `ForgeModFileInfoParser.build()` 返回**真实 `ModFileInfo`**（而非自有 `IModFileInfo` 实现），
   `LoadingModList` 的这一道 cast 应可通过；**本轮未做该对照**（超出 001b-5 授权范围）。
2. `[H]` 由于 `ModFileInfo` 构造器内部即 `new ModInfo(this, config)`，
   走真实 `ModFileInfo` 路径可能**同时**满足 `ModInfo` 契约，从而一次性越过两道 cast。未验证。
3. `[H]` `LoadingModList` 之后是否还有第三道同型 cast（例如对 `IModLanguageLoader` 或 scan data），
   本轮未观测。
4. `[H]` `ModSorter` 对 `IModInfo → ModInfo` 的 7 处 cast 中，
   本轮只实际经过其中一部分；其余分支是否可被触发未验证。

### [D] Design Decision

1. `[D]` **ModSorter 类型契约不止一层**：`IModInfo → ModInfo` 之后还有
   `IModFileInfo → ModFileInfo`。任何"兼容层返回自有 `IModFileInfo` 实现"的架构
   都会在 `LoadingModList` 处被同一类问题拦下。
2. `[D]` 但**正常 Java 路径是存在的**（[E]1–2）——因此这不是"必须修改 NeoForge 核心"的情形，
   也不属于 001b-5.md §六 的 C 类。
3. `[D]` 手册 §3.1 的 Layer 标签、001b-3 §九 的回写项、以及本次新增的
   `IModFileInfo` 隐式契约，**均待架构决策方统一决定后回写**；本 Spike 未改动手册。

---

## 十一、First Confirmed Failure

**阶段**: `LanguageLoader 之后、Module 之前的 ModSorter 下游` ——
具体为 `LoadingModList.<init>` 构建 `modFiles` 列表时的类型转换。

**证据**:
1. `stderr` 首行：`Cannot cast com.example.forgecompat.ForgeModFileInfo to net.neoforged.fml.loading.moddiscovery.ModFileInfo`
2. 调用链：`ModValidator.stage2Validation:96` → `ModSorter.sort:82` → `LoadingModList.of:70` → `LoadingModList.<init>:56`
3. 字节码：`LoadingModList.<init>` 行 55 处的 `ModFileInfo.class::cast`（`invokedynamic #1`）

**异常**: `java.lang.ClassCastException`（未捕获，直接终止 main 线程；进程退出码 1）

**调用链**: 见 §五 的完整 stack trace（未截断）

**原因**: 兼容层 `ForgeModFileInfoParser.build()` 返回的是**自有 `IModFileInfo` 实现**
（`com.example.forgecompat.ForgeModFileInfo`），而 `LoadingModList` 要求它是
`net.neoforged.fml.loading.moddiscovery.ModFileInfo`。

> 按 001b-5.md §六："正常路径成功但出现新的异常 → 记录 first confirmed failure，STOP"。
> **本 Spike 在此停止**，不对该失败点提出修复方案。

---

## 十二、结论

**ModSorter 类型契约**：

> `ModSorter` 要求 `IModInfo` 必须是 `ModInfo`（7 处 cast）；
> 而 `LoadingModList` 进一步要求 `IModFileInfo` 必须是 `ModFileInfo`。
> 两者都是**具体实现类**，不是 SPI 接口。

**正常 Java 构造 ModInfo**：**YES**

> `ModFile` / `ModFileInfo` / `ModInfo` 构造链**全部 public**，`fml_loader` 是自动模块，
> 编译与继承均实测通过；运行时已成功产出真实 `ModInfo` 实例。

**运行时通过类型检查**：**部分** —— `IModInfo → ModInfo` **通过**；
`IModFileInfo → ModFileInfo` **未通过**（新的 CCE）。

**第一个新失败点**：`LoadingModList.<init>`（`LoadingModList.java:56` 报告行）
的 `(ModFileInfo) modFile.getModFileInfo()`。

**Confidence**: **HIGH**

**Evidence**:

1. 字节码取证：`ModInfo` 构造签名 / `fml_loader` manifest / `LoadingModList` 字段与 cast 字节码。
2. 编译取证：独立探针 `javac` exit 0（A1 + A2）。
3. 运行时取证：`buildNativeMods: created REAL …ModInfo`；CCE 从 `ModSorter:112` 迁移到 `LoadingModList:56`。
4. 清理取证：15/15 条目哈希一致。

**Hypotheses still unverified**:

1. 返回真实 `ModFileInfo` 是否可一次性越过两道 cast（[H]1–2）。
2. `LoadingModList` 之后是否还有第三道同型 cast（[H]3）。
3. `ModSorter` 其余 cast 分支的触发条件（[H]4）。

---

## 十三、禁止事项确认

- [x] 未修改 `ModSorter`
- [x] 未修改 `ModInfo`
- [x] 未修改 `ModFile` / `ModFileInfo`
- [x] 未修改 `LanguageProviderLoader`
- [x] 未修改 `VersionSupportMatrix`
- [x] 未修改 NeoForge 核心（仅**调用**其 public 构造函数）
- [x] 未修改 `forgeprobe.jar`（SHA-256 `a93a823c…374e22`）
- [x] 未修改 `mods.toml`
- [x] 未引入 ASM / Mapping / Mixin / Registry / Event / Isolation
- [x] 未使用 Unsafe / 字节码注入 / ClassLoader hack
- [x] 未把非常规反射路径冒充正常兼容路径（本 Spike 未使用非常规路径）
- [x] 未进入 001b-2
- [x] 未进入 001c
- [x] 未提出修复方案
- [x] 临时观测 provider 与临时 `ModInfo` 构造代码已全部撤销

---

## 附录 A：与 Spike_001b-5.md 的偏差与说明

| 项 | 说明书要求 | 实际执行 | 说明 |
|---|---|---|---|
| 前置文档 §3.8（IModFileInfo 契约的隐式约束） | 要求先读 | **手册中不存在该节** | 手册目前只到 §3.7；§3.8 属待回写内容。本 Spike 的报告 §三 已提供该内容的事实基础 |
| 001b-5-3 非常规路径 | 仅在 A 类失败时执行 | **未执行** | A1/A2 编译通过，前置条件不成立 |
| 临时 provider | "001b-3 同型" | 复用了 001b-3 的 `name/version` 机制，**未**再复制 Layer 观测代码 | Layer 问题已由 001b-3 回答，避免冗余输出 |
| 编译探针位置 | 未规定 | 放在 `spike001b5\probe\`，**不参与 forgecompat.jar 打包** | 保证交付物不被实验代码污染 |

## 附录 B：v1.1 手册待回写项（本 Spike 未执行）

> 沿用上一轮的口径：回写由架构决策方统一决定。本轮新增以下待回写内容。

**§3.1 / §3.8 追加 [F]**

```
[F] LoadingModList 的字段为 List<ModFileInfo> modFiles / List<ModInfo> sortedList；
    其构造器对每个 ModFile 执行 ModFile::getModFileInfo 后再做 ModFileInfo.class::cast。
    即：IModFile 的 IModFileInfo 必须是 net.neoforged.fml.loading.moddiscovery.ModFileInfo。

[F] ModInfo 为 non-final public class，唯一构造函数为
    public ModInfo(ModFileInfo owningFile, IConfigurable config)；
    无 static factory；owning file 参数为具体类 ModFileInfo。

[F] loader-4.0.45.jar 为自动模块（Automatic-Module-Name: fml_loader，无 module-info.class），
    net.neoforged.fml.loading.moddiscovery 对外可访问。
```

**§9 追加 [D]**

```
[D] NeoForge 21.1.256 的 mod 加载路径并非只依赖 IModFileInfo / IModInfo 两个 SPI 接口：
    ModSorter 与 LoadingModList 均对具体实现类 (ModInfo / ModFileInfo) 做强制转换。
    兼容层若返回自有 IModFileInfo 实现，会在 LoadingModList 处被同一类问题拦下。
    → 这一点必须在 Plan A / Plan B 决策前纳入考虑。
```
