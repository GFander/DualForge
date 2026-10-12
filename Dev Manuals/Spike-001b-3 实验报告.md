# Spike-001b-3 实验报告

> **结论（先行）**
>
> **问题 A**：内置 `javafml` provider（`FMLJavaModLanguageProvider`）的实际 **ModLauncher Layer = `BOOT`**；
> 兼容层 provider 的实际 **ModLauncher Layer = `SERVICE`**。判定手段是
> `IModuleLayerManager.getLayer(L).get()` 与 `class.getModule().getLayer()` 的**对象同一性比对**（`==`）。
>
> **问题 B**：**情况 A** —— 兼容 provider **覆盖**内置 provider。
> 顺序稳定（两次运行一致）：`javafml 4.0.45 → lowcodefml 4.0.45 → minecraft 4.0.45 → javafml 9999`，
> `Map.put` 后写入者胜出，`languageProviderMap["javafml"]` 最终为 **9999**。
>
> **附带观测（重要）**：版本校验因此**通过**，流程首次越过 `findLanguage`，
> 随即在 `ModSorter.sort` 因 `ClassCastException`（`ForgeModInfo` 不能转成 NeoForge 内部 `ModInfo`）终止。
>
> **001b-3-c 撤销已完成**：15/15 条目哈希与 001b-1 完全一致。
>
> 按 Spike_001b-3.md §八：**不进入 001b-2 / 001c**，报告原样交回。

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

编译依赖 Forge 版本:  1.21.1-52.1.0
```

运行标签：`spike001b-3`（首次）与 `spike001b-3b`（复跑，用于枚举顺序稳定性）。
`mods/` 内容：`forgecompat.jar`（001b-3 观测版）+ `forgeprobe.jar`（未改动）。

---

## 二、实验目标

**问题 A**：`IModLanguageLoader` 的 provider 实际来自哪个 **ModLauncher Layer**
（`cpw.mods.modlauncher.api.IModuleLayerManager.Layer` 的 `BOOT` / `SERVICE` / `PLUGIN` / `GAME`，
**不是** `java.lang.ModuleLayer`）？

**问题 B**：同名 provider 的实际覆盖行为是什么？

> 概念纪律：**Java ModuleLayer ≠ ModLauncher Layer**。
> 禁止用 `provider.getClass().getModule().getLayer()` 直接声称 ModLauncher Layer。

---

## 三、实验输入

### forgecompat.jar（001b-3 版本）

- 路径：`D:\DualForge\spike001a\compat\build\forgecompat.jar`
- SHA-256: `be3b9cb65aee2e1fcf92cf5798abadc9009e553af3db6f15d5aa31251c2f2d46`
- 文件大小: `17661` 字节
- 基线留档：`build\forgecompat-001b1.jar`（SHA-256 `14efd54f…32e334`）

**JAR tree（`jar tf`，完整输出）**：

```
META-INF/
META-INF/MANIFEST.MF
META-INF/services/
META-INF/services/net.neoforged.neoforgespi.language.IModLanguageLoader
META-INF/services/net.neoforged.neoforgespi.locating.IModFileCandidateLocator
META-INF/services/net.neoforged.neoforgespi.locating.IModFileReader
com/
com/example/
com/example/forgecompat/
com/example/forgecompat/ForgeCompatLanguageLoader.class
com/example/forgecompat/ForgeJarMetadata.class
com/example/forgecompat/ForgeModFileInfo$ForgeModInfo.class
com/example/forgecompat/ForgeModFileInfo$MapConfigurable.class
com/example/forgecompat/ForgeModFileInfo.class
com/example/forgecompat/ForgeModFileInfoParser.class
com/example/forgecompat/ForgeModLocator.class
com/example/forgecompat/ForgeModReader.class
```

**与 001b-1 版本的 diff（逐条目 SHA-256）**：

| JAR 条目 | 001b-1 | 001b-3 | 判定 |
|---|---|---|---|
| `META-INF/MANIFEST.MF` | `7b8130925631a919…` | 同 | **未变** |
| `META-INF/services/…IModFileReader` | `394a0017ea04610a…` | 同 | **未变** |
| `META-INF/services/…IModFileCandidateLocator` | `2e232a7c8c1ed34a…` | 同 | **未变** |
| `META-INF/services/…IModLanguageLoader` | **不存在** | `…`（新增） | **新增**（本 Spike 唯一新增声明） |
| `ForgeCompatLanguageLoader.class` | **不存在** | `…`（新增） | **新增**（本 Spike 唯一新增源文件） |
| 其余 8 个既有条目 | — | — | **全部未变** |

**新增源文件**：`ForgeCompatLanguageLoader.java`（唯一）。
源码要点：`<clinit>` / `INSTANTIATED #n` / `name()` / `version()` 四处 `[FORGECOMPAT][LANG]` 日志；
`loadMod(...)` 抛 `UnsupportedOperationException("Observation only")`；
`observe()` 以 `Launcher.INSTANCE.environment().findModuleLayerManager()` 读取各 Layer，
用 `ServiceLoader.load(moduleLayer, IModLanguageLoader.class).stream()`（**不实例化**）
枚举每层可见的 provider，并与 `matchLayer()` 做**对象同一性**比对。

**META-INF/services/ 内容（含新增声明）**：

```
net.neoforged.neoforgespi.language.IModLanguageLoader
    com.example.forgecompat.ForgeCompatLanguageLoader
net.neoforged.neoforgespi.locating.IModFileReader
    com.example.forgecompat.ForgeModReader
net.neoforged.neoforgespi.locating.IModFileCandidateLocator
    com.example.forgecompat.ForgeModLocator
```

### forgeprobe.jar

- SHA-256: `a93a823c988bfd6a8f6ea132d189d9de732b0e5951f4bf74980d51bb63374e22`
- 文件大小: `1772` 字节
- **与 001b-1 / 001a-0 逐字节一致**

---

## 四、问题 A：provider 实际 ModLauncher Layer

**观测方法**：

- [x] 方法 A：源码 + 运行时路径交叉验证
- [ ] 方法 B：ServiceLoader 路径记录
- [ ] 其它

具体做法（只读，不修改任何 NeoForge/FML 类）：

```java
IEnvironment env = Launcher.INSTANCE.environment();            // 公开 API
IModuleLayerManager mgr = env.findModuleLayerManager().orElse(null);
// 决定性判据：对象同一性
for (Layer L : Layer.values()) {
    ModuleLayer cand = mgr.getLayer(L).orElse(null);
    if (cand != null && cand == clazz.getModule().getLayer()) -> 命中 L
}
```

**IModLanguageLoader providers**：

- **FMLJavaModLanguageProvider**
  - CodeSource: `union:/D:/DualForge/spike000/neoforge-server/libraries/net/neoforged/fancymodloader/loader/4.0.45/loader-4.0.45.jar%2359!/`
  - module: `fml_loader`
  - **ModLauncher Layer: `BOOT`**
  - 证据链:
    1. `builtin class=net.neoforged.fml.javafmlmod.FMLJavaModLanguageProvider module=fml_loader codeSource=…loader-4.0.45.jar… modLauncherLayer=[BOOT]`
    2. 同一时刻 `getLayer(BOOT)` 存在，且其 `ModuleLayer` 对象含 `fml_loader`：
       `Layer BOOT present = true / modules=[…, fml_loader, …]`
    3. `viaLayer=BOOT provider=FMLJavaModLanguageProvider … -> match=[BOOT]`

- **LowCodeModLanguageProvider**
  - CodeSource: 同 `loader-4.0.45.jar`
  - module: `fml_loader`
  - **ModLauncher Layer: `BOOT`**（同上判据）

- **MinecraftModLanguageProvider**
  - CodeSource: 同 `loader-4.0.45.jar`
  - module: `fml_loader`
  - **ModLauncher Layer: `BOOT`**（同上判据）

**对照（本次新引入的观测 provider）**：

- `ForgeCompatLanguageLoader`
  - CodeSource: `union:/D:/DualForge/spike000/neoforge-server/mods/forgecompat.jar%23118!/`
  - module: `forgecompat`
  - **ModLauncher Layer: `SERVICE`**

**观测时刻的 Layer 存在性（关键上下文）**：

```
Layer BOOT present = true      ← 含 fml_loader
Layer SERVICE present = true   ← 含 forgecompat
Layer PLUGIN present = true    ← 含 neoforge.coremods
Layer GAME present = false
```

> 即：`LanguageProviderLoader` 构造时 **GAME 尚未建立，PLUGIN 已存在**。
> 按 001b-0 的 `LaunchContext.loadServices` 算法（逆序取最高已存在层并连带 parent），
> 实际搜索覆盖 `PLUGIN`（+parent `BOOT`）与 `SERVICE`（+parent `BOOT`）。
> 两个来源 provider 分别来自 **BOOT** 与 **SERVICE**，与上表一致。

**Java ModuleLayer 观测（仅作辅助，不作 Layer 判定依据）**：

- `FMLJavaModLanguageProvider.getModule().getLayer()` = `java.lang.ModuleLayer#1690287238`（两次运行一致）
- 说明: 该值**不等于** ModLauncher Layer。**本报告不作"该值 ⇒ SERVICE"之类的推断**；
  上面的 `BOOT` 结论完全来自与 `IModuleLayerManager` 返回对象的**同一性比对**。

**PASS**

---

## 五、问题 B：同名覆盖行为

**ForgeCompatLanguageLoader**：

- `<clinit>`: **触发**
- INSTANTIATED: **YES**（`INSTANTIATED #1`，每次运行恰好 1 次）
- `name()` 调用: **YES**（stdout 中 3 次 `name() -> javafml`）
- `version()` 调用: **YES**（stdout 中 1 次 `version() -> 9999`）

**compat provider 自身的 stdout 原文（按顺序，不截断）**：

```
[FORGECOMPAT][LANG] <clinit>
[FORGECOMPAT][LANG] INSTANTIATED #1
[FORGECOMPAT][LANG][OBS] ---- begin ModLauncher Layer observation ----
…（Layer 观测，见 §四）…
[FORGECOMPAT][LANG][OBS] ---- end ModLauncher Layer observation ----
[FORGECOMPAT][LANG] version() -> 9999
[FORGECOMPAT][LANG] name() -> javafml
[FORGECOMPAT][LANG] name() -> javafml
[FORGECOMPAT][LANG] name() -> javafml
```

> 不对这 3 次 `name()` 的调用点做归因（日志未标注调用点）。

**LanguageProviderLoader 日志原文（按顺序，不截断；两次运行完全一致）**：

```
[main/DEBUG] [net.neoforged.fml.util.ServiceLoaderUtil/CORE]: Found 4 implementations of IModLanguageLoader:
[main/DEBUG] [net.neoforged.fml.loading.LanguageProviderLoader/CORE]: Found language provider javafml, version 4.0.45
[main/DEBUG] [net.neoforged.fml.loading.LanguageProviderLoader/CORE]: Found language provider lowcodefml, version 4.0.45
[main/DEBUG] [net.neoforged.fml.loading.LanguageProviderLoader/CORE]: Found language provider minecraft, version 4.0.45
[main/DEBUG] [net.neoforged.fml.loading.LanguageProviderLoader/CORE]: Found language provider javafml, version 9999
```

**languageProviderMap 中 `name="javafml"` 的最终来源**：

- class: `com.example.forgecompat.ForgeCompatLanguageLoader`
- version: `9999`
- **情况判定: A**（兼容 provider 覆盖内置）

**判定依据**：`LanguageProviderLoader` 构造器按流顺序执行
`languageProviderMap.put(loader.name(), wrapper)`；上表顺序中 `javafml@9999` **最后**写入，
故覆盖先前写入的 `javafml@4.0.45`。

**枚举顺序（可观测性）**：

- 顺序 = `ServiceLoaderUtil.loadServices` 的去重后流顺序；日志**未**打印逐条 provider 类名，
  因此 `FMLJavaModLanguageProvider` 相对兼容 provider 的枚举位次**未直接观测**，
  只能由"最后写入者"反推：`javafml@9999` 在流中位于 `javafml@4.0.45` **之后**。
- **稳定性**：两次独立运行（`spike001b-3` / `spike001b-3b`）该 4 行顺序**完全一致**。

**PASS**

**由此产生的附带观测（记录失败形态）**：

版本校验 `[52,)`.containsVersion(`9999`) = **true** → `findLanguage` **成功返回**兼容 loader
→ 流程首次越过 LanguageLoader 阶段 → 在 `ModSorter.sort` 抛：

```
Exception in thread "main" java.lang.ClassCastException: Cannot cast com.example.forgecompat.ForgeModFileInfo$ForgeModInfo to net.neoforged.fml.loading.moddiscovery.ModInfo
	at …/net.neoforged.fml.loading.ModSorter.sort(ModSorter.java:112)
	at …/net.neoforged.fml.loading.ModSorter.sort(ModSorter.java:77)
	at …/ModValidator.stage2Validation(ModValidator.java:96)
	at …/FMLLoader.completeScan(FMLLoader.java:138)
	at …/FMLServiceProvider.completeScan(FMLServiceProvider.java:90)
	at cpw.mods.modlauncher.TransformationServiceDecorator.onCompleteScan(TransformationServiceDecorator.java:103)
	at cpw.mods.modlauncher.TransformationServicesHandler.triggerScanCompletion(TransformationServicesHandler.java:141)
	at cpw.mods.modlauncher.Launcher.run(Launcher.java:91)
	at cpw.mods.modlauncher.Launcher.main(Launcher.java:74)
	at cpw.mods.bootstraplauncher.BootstrapLauncher.run(BootstrapLauncher.java:210)
	at cpw.mods.bootstraplauncher.BootstrapLauncher.main(BootstrapLauncher.java:69)
```

进程退出码 **1**（`EXITED_EARLY`，未达 `Done`，未生成 FML 崩溃报告——异常直接终止 main 线程）。

> 按 Spike_001b-3.md §七，「新增 provider 导致启动失败 → 记录失败形态，报告停止」。
> 此处**两个问题均已在失败前完整观测**，故不构成观测阻塞；失败形态已完整记录。

---

## 六、撤销确认

**001b-3-c 已执行**：删除 `ForgeCompatLanguageLoader.java` 与
`META-INF/services/net.neoforged.neoforgespi.language.IModLanguageLoader`，重新编译并重打包。

| 项 | 001b-1 值 | 001b-3 清理后值 | 一致? |
|---|---|---|---|
| `ForgeCompatLanguageLoader.class` | 不存在 | 不存在 | **YES** |
| `META-INF/services/…IModLanguageLoader` | 不存在 | 不存在 | **YES** |
| `META-INF/services/…IModFileReader` | `394a0017ea04610a…` | `394a0017ea04610a…` | **YES** |
| `META-INF/services/…IModFileCandidateLocator` | `2e232a7c8c1ed34a…` | `2e232a7c8c1ed34a…` | **YES** |
| `MANIFEST.MF` | `7b8130925631a919…` | `7b8130925631a919…` | **YES** |
| 其余 10 个条目 | — | 逐条目哈希相同 | **YES** |

**逐条目比对结果：same = 15 / diff = 0。**

```
SHA-256 是否恢复 001b-1: NO（整体 JAR 哈希不同）
001b-1 整体 SHA-256 : 14efd54fa91e415c5815972b81d16c2895dd9ed52a427154db1261efa532e334
清理后整体 SHA-256 : 671c263551bfe46868e1cbac5ee2db1746679a7a65d7db96b604d32689606003
两者文件大小均为    : 13215 字节
```

**若未恢复，差异原因**：整体哈希差异来自 ZIP 容器层（entry 顺序元数据 / 时间戳），
而**不是**内容差异——15 个条目的逐条目 SHA-256 全部一致。
按 Spike_001b-3.md §3（001b-3-c）"清理标准不是 SHA-256 恢复，而是内容一致"，
**本次清理判定为通过，不构成实验污染**。

观测用源码与 services 声明已从 `spike001a/compat/src` 中移除；当前源码树为：

```
src/main/java/com/example/forgecompat/{ForgeJarMetadata, ForgeModFileInfo, ForgeModFileInfoParser, ForgeModLocator, ForgeModReader}.java
src/main/resources/META-INF/MANIFEST.MF
src/main/resources/META-INF/services/net.neoforged.neoforgespi.locating.IModFileReader
src/main/resources/META-INF/services/net.neoforged.neoforgespi.locating.IModFileCandidateLocator
```

此时磁盘上的 `build/forgecompat.jar` = **清理后的 001b-1 等价物**
（内容一致；`build/forgecompat-001b1.jar` 为原始留档）。

---

## 七、Failure Chain（更新）

```
Discovery:       YES
Metadata:        YES
Validation:      YES
Scan:            UNKNOWN
LanguageLoader:  YES（provider lookup 与 VersionSupportMatrix 分支均已进入；
                      本轮因 9999 覆盖而**通过了**版本校验）
Module:          UNKNOWN（ModSorter.sort 在进入 Module 层之前即抛 ClassCastException）
Container:       UNKNOWN
Constructor:     UNKNOWN
```

> `Module` 标 UNKNOWN 而非 NO：`stage2Validation` 已在执行 `ModSorter.sort`（mod 排序属于
> Module 层构建的前置步骤），但**未**观测到 `getModResources()` / Game Layer 相关行为，
> 故不足以判 YES 也不足以判 NO。

---

## 八、完整日志

首次运行（`spike001b-3`）：

- latest.log: `D:\DualForge\spike000\logs\spike001b-3\spike001b-3.log`（2893 字节）
- debug.log: `D:\DualForge\spike000\logs\spike001b-3\spike001b-3.debug.log`（32395 字节）
- stdout: `D:\DualForge\spike000\logs\spike001b-3\spike001b-3.stdout.log`（14559 字节）
- stderr: `D:\DualForge\spike000\logs\spike001b-3\spike001b-3.stderr.log`（3638 字节，含完整 ClassCastException）
- crash-report: **不存在**（异常直接终止 main 线程，未走 FML 崩溃报告路径）
- meta.json: `status = EXITED_EARLY`，`exitCode = 1`，`durationSeconds = 6.1`

稳定性复跑（`spike001b-3b`）：

- latest.log: `D:\DualForge\spike000\logs\spike001b-3b\spike001b-3b.log`
- debug.log: `D:\DualForge\spike000\logs\spike001b-3b\spike001b-3b.debug.log`
- stdout: `D:\DualForge\spike000\logs\spike001b-3b\spike001b-3b.stdout.log`
- stderr: `D:\DualForge\spike000\logs\spike001b-3b\spike001b-3b.stderr.log`
- meta.json: `status = EXITED_EARLY`，`exitCode = 1`，`durationSeconds = 6.1`

运行脚本: `D:\DualForge\spike000\run-server.ps1`（`-Label spike001b-3` / `spike001b-3b`）

### 8.1 Layer 观测原文（`spike001b-3.stdout.log`，节选连续块，未改写）

```
[FORGECOMPAT][LANG][OBS] self class=com.example.forgecompat.ForgeCompatLanguageLoader module=forgecompat codeSource=union:/D:/DualForge/spike000/neoforge-server/mods/forgecompat.jar%23118!/ javaLayer=java.lang.ModuleLayer#1713568869 modLauncherLayer=[SERVICE]
[FORGECOMPAT][LANG][OBS] builtin class=net.neoforged.fml.javafmlmod.FMLJavaModLanguageProvider module=fml_loader codeSource=union:/…/loader-4.0.45.jar%2359!/ javaLayer=java.lang.ModuleLayer#1690287238 modLauncherLayer=[BOOT]
[FORGECOMPAT][LANG][OBS] builtin class=net.neoforged.fml.lowcodemod.LowCodeModLanguageProvider module=fml_loader codeSource=union:/…/loader-4.0.45.jar%2359!/ javaLayer=java.lang.ModuleLayer#1690287238 modLauncherLayer=[BOOT]
[FORGECOMPAT][LANG][OBS] builtin class=net.neoforged.fml.mclanguageprovider.MinecraftModLanguageProvider module=fml_loader codeSource=union:/…/loader-4.0.45.jar%2359!/ javaLayer=java.lang.ModuleLayer#1690287238 modLauncherLayer=[BOOT]
[FORGECOMPAT][LANG][OBS] Layer BOOT present = true
[FORGECOMPAT][LANG][OBS]   javaModuleLayer#1690287238 modules=[…, fml_loader, cpw.mods.modlauncher, …]
[FORGECOMPAT][LANG][OBS]   viaLayer=BOOT provider=net.neoforged.fml.javafmlmod.FMLJavaModLanguageProvider … -> match=[BOOT]
[FORGECOMPAT][LANG][OBS]   viaLayer=BOOT provider=net.neoforged.fml.lowcodemod.LowCodeModLanguageProvider … -> match=[BOOT]
[FORGECOMPAT][LANG][OBS]   viaLayer=BOOT provider=net.neoforged.fml.mclanguageprovider.MinecraftModLanguageProvider … -> match=[BOOT]
[FORGECOMPAT][LANG][OBS] Layer SERVICE present = true
[FORGECOMPAT][LANG][OBS]   javaModuleLayer#1713568869 modules=[forgecompat]
[FORGECOMPAT][LANG][OBS]   viaLayer=SERVICE provider=com.example.forgecompat.ForgeCompatLanguageLoader module=forgecompat … -> match=[SERVICE]
[FORGECOMPAT][LANG][OBS]   viaLayer=SERVICE provider=net.neoforged.fml.javafmlmod.FMLJavaModLanguageProvider … -> match=[BOOT]
[FORGECOMPAT][LANG][OBS] Layer PLUGIN present = true
[FORGECOMPAT][LANG][OBS]   javaModuleLayer#896134613 modules=[neoforge.coremods]
[FORGECOMPAT][LANG][OBS] Layer GAME present = false
[FORGECOMPAT][LANG][OBS] ---- end ModLauncher Layer observation ----
```

> 注：`…` 处为路径中重复的 `D:/DualForge/spike000/neoforge-server/libraries/net/neoforged/fancymodloader/`
> 前缀与 `modules=[…]` 的完整模块清单。**完整未删节原文在上述日志文件中**。
> 唯一被压缩的是这类极长路径前缀与模块清单，`match=` / `modLauncherLayer=` 等判据字段**未被改动**。

### 8.2 两次运行的顺序对照

| 行 | spike001b-3 | spike001b-3b |
|---|---|---|
| 1 | `Found 4 implementations of IModLanguageLoader:` | 同 |
| 2 | `Found language provider javafml, version 4.0.45` | 同 |
| 3 | `Found language provider lowcodefml, version 4.0.45` | 同 |
| 4 | `Found language provider minecraft, version 4.0.45` | 同 |
| 5 | `Found language provider javafml, version 9999` | 同 |

**顺序稳定。**

---

## 九、事实标签

### [F] Source Fact

1. `[F]` `cpw.mods.modlauncher.api.IModuleLayerManager.Layer` 的 parent 关系为：
   `BOOT` = 无 parent；`SERVICE` = `[BOOT]`；`PLUGIN` = `[BOOT]`；`GAME` = `[PLUGIN, SERVICE]`。
2. `[F]` `LaunchContext.loadServices` 逆序遍历 Layer，取最高已存在层并调用
   `ServiceLoader.load(moduleLayer, service)`；该方法会连同**父层**一起搜索
   （这正是 `visitLayer` 需要把 parent 一并标记为 handled 的原因）。
3. `[F]` `LanguageProviderLoader` 构造器按 `ServiceLoader` 流顺序执行
   `languageProviderMap.put(loader.name(), new ModLanguageWrapper(loader, version))`，
   **无重名检测**，因此在同名情况下**后写入者胜出**。
4. `[F]` `ModSorter.sort` 内部对 `IModInfo` 做**强制类型转换**为
   `net.neoforged.fml.loading.moddiscovery.ModInfo`
   （`ModSorter.java:112`，`ReferencePipeline` 中的 `Class.cast`），
   即 `IModFileInfo.getMods()` 返回的 `IModInfo` **必须**是 NeoForge 自身的 `ModInfo`（或其子类）。

### [E] Experimental Fact

1. `[E]` `FMLJavaModLanguageProvider` / `LowCodeModLanguageProvider` / `MinecraftModLanguageProvider`
   的 module 均为 `fml_loader`，CodeSource 均为 `loader-4.0.45.jar`，
   **ModLauncher Layer = `BOOT`**（由 `IModuleLayerManager.getLayer(BOOT).get() == class.getModule().getLayer()`
   对象同一性判定）。
2. `[E]` 兼容层 `ForgeCompatLanguageLoader` 的 module 为 `forgecompat`，
   CodeSource 为 `mods/forgecompat.jar`，**ModLauncher Layer = `SERVICE`**。
3. `[E]` `LanguageProviderLoader` 构造时刻的 Layer 存在性：
   `BOOT` 有、`SERVICE` 有、`PLUGIN` 有（含 `neoforge.coremods`）、`GAME` **无**。
4. `[E]` 兼容 provider 被 ServiceLoader 枚举到：`Found 4 implementations of IModLanguageLoader`
   （001b-1 为 3），且 `ForgeCompatLanguageLoader` 被实例化（`INSTANTIATED #1`）。
   → 001b-0 的 [H]「因其它白名单服务被提升进 SERVICE 层的 JAR，其同时声明的 IModLanguageLoader
   也会被同一 ServiceLoader 看见」**得到证实**。
5. `[E]` **同名覆盖 = 情况 A**：日志顺序 `javafml@4.0.45 … javafml@9999`，
   `languageProviderMap["javafml"]` 最终为 9999；两次运行顺序一致。
6. `[E]` 因 9999 通过 `[52,)` 校验，`findLanguage` **首次成功返回**兼容 loader；
   流程越过 LanguageLoader，随后在 `ModSorter.sort` 抛 `ClassCastException`
   （`ForgeModInfo` → `ModInfo`），进程退出码 1，未生成 FML 崩溃报告。
7. `[E]` `Found 4 mod files with 3 mods` / 发现阶段 Mod List 含 `Forge Probe 1.0.0 (forgeprobe)`
   （与 001b-1 一致，本轮未变）。
8. `[E]` 撤销后 15 个 JAR 条目逐条目 SHA-256 与 001b-1 完全一致（diff = 0）；
   整体 JAR 哈希不同（ZIP 元数据）。

### [H] Hypothesis

1. `[H]` `ModSorter` 的 `ModInfo` 强转是否**只**发生在该行、是否可通过让
   `getMods()` 返回 NeoForge 自身 `ModInfo` 实例来满足，本轮未验证
   （001b-1 的最小 `IModInfo` 实现**不足以**通过 `ModSorter`）。
2. `[H]` 兼容 provider 的枚举位次为何固定在**所有内置 provider 之后**（是否由 Layer 顺序
   `PLUGIN → SERVICE` 决定），本轮只能由"最后写入者"反推，**未直接观测**枚举次序。
3. `[H]` 若兼容 provider 的 `version()` 取一个**不满足** `[52,)` 的值，流程将回到 001b-1 的
   `missingversion` 形态——本轮未做该对照。
4. `[H]` `PLUGIN` 层在 `completeScan` 之前已存在且含 `neoforge.coremods`，
   说明 PLUGIN 的创建早于 `buildTransformingClassLoader`；本轮只观测到"存在"，
   **未**观测其创建时机。

### [D] Design Decision

1. `[D]` 手册 §3.1 的 `IModLanguageLoader` Layer 标签：本轮已把可判定部分从 [H] 推进到 [E]
   （内置 provider 来自 **BOOT**，非 SERVICE/PLUGIN）。**是否回写为 [E] 由架构决策方裁定**
   （Spike_001b-3.md §九 要求由决策方更新）。
2. `[D]` Plan A 的关键前置问题「同名 provider 能否占据 `javafml`」已由情况 A 回答为**能**；
   但**仍不能**写作"Plan A 已可行"——`ModSorter` 的 `ModInfo` 强转是本轮新暴露的
   **下一道硬约束**，属 001b-2 / 后续 Spike 范围。
3. `[D]` 若未来兼容层要在 `IModFileInfo.getMods()` 中返回自有 `IModInfo` 实现，
   必须同时满足 `ModSorter` 对 `ModInfo` 的类型要求；这是 `IModFileInfo` 契约的一条
   **未被文档记录的隐式约束**。

---

## 十、结论

**问题 A（provider 实际 ModLauncher Layer）**：

> 内置 `javafml`（及另两个内置 language provider）来自 **BOOT** 层；
> 兼容层 provider 来自 **SERVICE** 层。
> 判定不是靠 Java ModuleLayer，而是 `IModuleLayerManager.getLayer(L).get()`
> 与 `class.getModule().getLayer()` 的**对象同一性**。

**问题 B（同名覆盖行为）**：

> **情况 A** —— 兼容 provider **覆盖**内置 provider。
> `javafml@9999` 在 `javafml@4.0.45` 之后写入 `languageProviderMap`，
> 成为 `name="javafml"` 的最终来源；两次运行顺序一致。

**Confidence**: **HIGH**

**Evidence**:

1. Layer 判定：`match=[BOOT]` / `match=[SERVICE]`，附各 Layer 的 Java ModuleLayer 对象与模块清单。
2. 覆盖判定：`Found language provider` 4 行原文顺序 + 两次运行一致。
3. 覆盖生效的**行为证据**：`findLanguage` 的版本校验由 FAIL 转为通过，
   流程首次越过 LanguageLoader（随后暴露 `ModSorter` 的 `ClassCastException`）。
4. 撤销：15/15 条目哈希与 001b-1 一致。

**Hypotheses still unverified**:

1. `ModSorter` 的 `ModInfo` 强转的完整契约与最小满足方式。
2. 兼容 provider 枚举位次的直接观测（本轮为反推）。
3. 若兼容 provider 版本不满足 `[52,)` 的行为（未做对照）。
4. `PLUGIN` 层的实际创建时机。

---

## 十一、禁止事项确认

- [x] 未修改 `FMLJavaModLanguageProvider`（仅 `Class.forName` 只读观测）
- [x] 未修改 `LanguageProviderLoader`（仅使用其日志）
- [x] 未修改 `VersionSupportMatrix`
- [x] 未修改 `LaunchContext`（仅通过公开 API `Launcher.INSTANCE` / `IModuleLayerManager` 只读读取）
- [x] 未解决版本冲突（`[52,)` vs `4.0.45` 的 FAIL 未做任何处理）
- [x] 未实现 Plan A 或 Plan B（本报告未给出任何兼容实现）
- [x] 未修改 `forgeprobe.jar`（SHA-256 与 001b-1 一致）
- [x] 未修改 `mods.toml`（`forgeprobe.jar` 未改动）
- [x] 未通过 Java ModuleLayer 直接声称 ModLauncher Layer
      （判据为对象同一性；报告已明确标注该纪律）
- [x] 观测 provider 已撤销（源文件 + services 声明已删除，JAR 条目 15/15 与 001b-1 一致）
- [x] 未在报告中提出修复方案

---

## 附录：与 Spike_001b-3.md 的偏差与说明

| 项 | 说明书要求 | 实际执行 | 说明 |
|---|---|---|---|
| 观测代码位置 | "在兼容层中添加日志打印以观测实例化顺序" + "通过反射读取 LaunchContext 的 ModLauncher Layer 状态" | 全部放入新增的 `ForgeCompatLanguageLoader`（单文件） | 未使用反射——`Launcher.INSTANCE` / `IEnvironment.findModuleLayerManager()` / `IModuleLayerManager.getLayer()` 均为**公开 API**，无需 `setAccessible` |
| 方法 A/B | A 为首选，B 为备选 | 采用 **A**，且用 `ServiceLoader.load(layer, …).stream()` 逐层枚举作为交叉比对 | `stream()` 不实例化 provider，避免观测自身递归 |
| STOP 条件 | "新增 provider 导致启动失败 → 记录失败形态，报告停止" | 失败形态已记录（`ModSorter` CCE），两个问题均在失败前完整观测 | 不构成观测阻塞 |
| 稳定性检查 | "ServiceLoader 枚举顺序不稳定 → 多跑几次记录" | 两次独立运行，顺序一致 | 未触发 STOP |
