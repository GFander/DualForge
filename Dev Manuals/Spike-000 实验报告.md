# Spike-000 实验报告

> 本报告为**纯观测实验报告**，不含任何修复方案，不含任何兼容层代码。
> 所有结论均可回溯到「原始日志 + 已发布 JAR 的字节码」两项证据。

---

## 一、实验环境（精确版本）

```
Minecraft:            1.21.1
NeoForge:             21.1.256
FancyModLoader:       4.0.45
Java:                 21
OS:                   Windows 11 (amd64) version 10.0
启动方式:             CLI（NeoForge 官方 installer --installServer 生成的参数文件 + java @argfile nogui）
客户端/服务端:        Server（Dedicated Server，ModLauncher launch target = forgeserver）

编译依赖 Forge 版本:  1.21.1-52.1.0
```

**逐项取证：**

| 字段 | 值 | 证据 |
|---|---|---|
| Minecraft | `1.21.1` | 日志 `--fml.mcVersion, 1.21.1`；`Starting minecraft server version 1.21.1` |
| NeoForge | `21.1.256` | 日志 `--fml.neoForgeVersion, 21.1.256`；`neoforge-21.1.256-universal.jar` |
| FancyModLoader | `4.0.45` | 见下方专项说明 |
| Java | `21.0.10`（Microsoft Build of OpenJDK，`21.0.10+7-LTS`） | `C:\Users\Albert\.jdks\ms-21.0.10\bin\java.exe -version`；日志 `JVM identified as Microsoft OpenJDK 64-Bit Server VM 21.0.10+7-LTS` |
| OS | Windows 11 (amd64) version 10.0 | 日志 `ModLauncher 11.0.5 starting: ... OS Windows 11 arch amd64 version 10.0` |
| CPU | Intel(R) Core(TM) Ultra 9 275HX，24 逻辑核 | 首次尝试产生的崩溃报告 `-- System Details --` 段 |
| 启动方式 | CLI | 命令见第五节；未使用 Gradle runClient，未使用第三方启动器 |
| 客户端/服务端 | Server | Dedicated Server（`forgeserver` launch target） |

**FancyModLoader 版本专项说明（重要偏差）：**

任务说明书预期从基线日志开头读取形如 `Starting FancyModLoader version 11.0.xx` 的行。
**该格式的行在 FML 4.x 中不存在**。日志中没有该字符串（已全文检索 `baseline.log` / `baseline.debug.log` / `forgeprobe.log` / `forgeprobe.debug.log`）。

FML 4.0.45 的实际取证如下（`[E]` 实验事实 + `[F]` 源码事实）：

1. 两次启动日志第 1 行均含 ModLauncher 参数：
   `args [--launchTarget, forgeserver, --fml.neoForgeVersion, 21.1.256, --fml.fmlVersion, 4.0.45, --fml.mcVersion, 1.21.1, --fml.neoFormVersion, 20240808.144430, nogui]`
2. 该参数由 NeoForge 安装器生成的
   `neoforge-server/libraries/net/neoforged/neoforge/21.1.256/win_args.txt` 固化提供：
   `--fml.fmlVersion 4.0.45`
3. 运行时实际加载的 FML 工件为
   `neoforge-server/libraries/net/neoforged/fancymodloader/loader/4.0.45/loader-4.0.45.jar`
   （同时出现在 `win_args.txt` 的 `-DlegacyClassPath` 中）。
4. 本报告第七、八节引用的全部 FML 类（`ModDiscoverer`、`IncompatibleModReason`、
   `JarModsDotTomlModFileReader`、`ModsFolderLocator`）均反编译自该 `loader-4.0.45.jar`。

> `[D]` 手册中 `FancyModLoader: 11.0.xx` 的书写格式属于 Forge 时代编号，与 NeoForge 21.1.256 实际捆绑的 FML 4.x 编号体系不一致。**后续文档修订时建议把「FancyModLoader 版本」改为「从 `--fml.fmlVersion` 参数或 `fancymodloader/loader/<ver>/` 目录读取」，而不是匹配一句固定文案。**

---

## 二、实验目标

**唯一目标**：确定 NeoForge 原生运行时面对一个原生 Forge 1.21.1 JAR 时，实际第一次失败在哪里。

**禁止目标**：让 `forgeprobe.jar` 成功启动；验证兼容层架构；提出修复方案；修改 NeoForge 核心；写任何兼容层代码。

**第一轮报告只回答**：`实际在哪里失败？`

---

## 三、实验输入

### forgeprobe.jar

- 文件名: `forgeprobe.jar`
- SHA-256: `a93a823c988bfd6a8f6ea132d189d9de732b0e5951f4bf74980d51bb63374e22`
- 文件大小: `1772` 字节
- 构建路径: `D:\DualForge\spike000\probe\build\forgeprobe.jar`
- 存放路径（实验时）: `D:\DualForge\spike000\neoforge-server\mods\forgeprobe.jar`
- class 文件版本: major = 65（Java 21）
- 元数据留存: `D:\DualForge\spike000\logs\forgeprobe.jar.metadata.txt`

**JAR tree（`jar tf forgeprobe.jar` 完整输出）：**

```
META-INF/
META-INF/MANIFEST.MF
META-INF/mods.toml
com/
com/example/
com/example/forgeprobe/
com/example/forgeprobe/ForgeProbe.class
```

### META-INF/mods.toml

以下为 JAR 内实际字节内容（已从构建产物中解包核对，Forge 原生格式，未使用 `neoforge.mods.toml`）：

```toml
modLoader="javafml"
loaderVersion="[52,)"
license="MIT"

[[mods]]
modId="forgeprobe"
version="1.0.0"
displayName="Forge Probe"

[[dependencies.forgeprobe]]
modId="minecraft"
mandatory=true
versionRange="[1.21.1]"
ordering="NONE"
side="BOTH"
```

### META-INF/MANIFEST.MF

以下为 JAR 内实际字节内容（已解包核对）：

```
Manifest-Version: 1.0
Automatic-Module-Name: forgeprobe
Created-By: 21.0.10 (Microsoft)
```

> 说明：`Created-By` 行由 JDK `jar` 工具在打包时自动追加（不是手工写入）。其存在不影响任何实验结论。
> 手工提供的源 manifest 仅含 `Manifest-Version` 与 `Automatic-Module-Name` 两行。
> **JAR 中不存在 `FMLModType` 属性**——这一点在第八节被证明是决定性的。

### ForgeProbe.java

实际参与编译的源码（`D:\DualForge\spike000\probe\src\main\java\com\example\forgeprobe\ForgeProbe.java`），原文如下，未作任何改写：

```java
package com.example.forgeprobe;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;

@Mod("forgeprobe")
public class ForgeProbe {
    public ForgeProbe(IEventBus bus) {
        System.out.println("=================================");
        System.out.println("FORGE PROBE CONSTRUCTOR REACHED");
        System.out.println("BUS = " + bus);
        System.out.println("=================================");
    }
}
```

编译产物的 ABI 已用 `javap` 核验为 Forge 命名空间（`net.minecraftforge.eventbus.api.IEventBus`）：

```
public class com.example.forgeprobe.ForgeProbe {
  public com.example.forgeprobe.ForgeProbe(net.minecraftforge.eventbus.api.IEventBus);
}
```

### 编译依赖

`build.gradle`（`D:\DualForge\spike000\probe\build.gradle`）关键行，原始内容：

```
minecraft 'net.minecraftforge:forge:1.21.1-52.1.0'
```

**实际编译命令（必须如实记录的执行偏差）：**

本机未安装 Gradle/Maven，且 ForgeGradle 需要联网执行完整的 Minecraft 反编译，因此本次**未使用 Gradle 构建**，而是用 `javac` 直接对 **Forge 1.21.1-52.1.0 官方安装器解析出的同一份 Forge 工件**编译（即 `build.gradle` 中那一行所声明的同一个 Forge 依赖）：

```
C:\Users\Albert\.jdks\ms-21.0.10\bin\javac.exe --release 21 -encoding UTF-8 ^
  -classpath "D:\DualForge\spike000\forge-server\libraries\net\minecraftforge\javafmllanguage\1.21.1-52.1.0\javafmllanguage-1.21.1-52.1.0.jar;D:\DualForge\spike000\forge-server\libraries\net\minecraftforge\eventbus\6.2.27\eventbus-6.2.27.jar" ^
  -d "D:\DualForge\spike000\probe\build\classes" ^
  "D:\DualForge\spike000\probe\src\main\java\com\example\forgeprobe\ForgeProbe.java"
```

类定位证据（对 Forge 1.21.1-52.1.0 官方工件执行 `jar tf` 的结果）：

- `net.minecraftforge.fml.common.Mod` → `javafmllanguage-1.21.1-52.1.0.jar`
- `net.minecraftforge.eventbus.api.IEventBus` → `eventbus-6.2.27.jar`

**未使用任何 NeoForge 依赖编译。**

---

## 四、基线实验（无 forgeprobe.jar）

结果: **SUCCESS**

日志路径:

- `D:\DualForge\spike000\logs\baseline\baseline.log`（= 服务端 `logs/latest.log` 完整副本）
- `D:\DualForge\spike000\logs\baseline\baseline.debug.log`
- `D:\DualForge\spike000\logs\baseline\baseline.stdout.log`（启动器 stdout，6497 字节）
- `D:\DualForge\spike000\logs\baseline\baseline.stderr.log`（0 字节）
- `D:\DualForge\spike000\logs\baseline\meta.json`

关键行:

```
[main/INFO] [net.neoforged.fml.loading.moddiscovery.ModDiscoverer/SCAN]: Found mod file "server-1.21.1-20240808.144430-srg.jar" [locator: production server provider +net.neoforged:neoforge:21.1.256:server]
[main/INFO] [net.neoforged.fml.loading.moddiscovery.ModDiscoverer/SCAN]: Found mod file "neoforge-21.1.256-universal.jar" [locator: PathBasedLocator[name=neoforge, ...], reader: mod manifest]
[Server thread/INFO] [net.minecraft.server.dedicated.DedicatedServer/]: Done (0.526s)! For help, type "help"
[Server thread/INFO] [net.minecraft.server.MinecraftServer/]: Stopping the server
```

Mod List（日志原文）：

```
     Mod List:
		Name Version (Mod Id)

		Minecraft 1.21.1 (minecraft)
		NeoForge 21.1.256 (neoforge)
```

异常（若有）: **无**。

- 进程退出码 `0`
- 状态 `DONE_STOPPED_GRACEFULLY`（由控制台 `stop` 指令正常关闭）
- **未产生 crash-report**（`logs/baseline/crash-reports` 不存在）
- 本次实验前 `mods/` 目录已被清空，`modsDir` = `[]`（见 `meta.json`）
- 存在 4 条与实验无关的既有警告（`VanillaPackResourcesBuilder ... uses unexpected schema` ×2、`SERVER IS RUNNING IN OFFLINE/INSECURE MODE!` 等），基线/实验两次完全一致

---

## 五、Spike-000 实验（加入 forgeprobe.jar）

结果: **FAIL**

> 精确表述：**服务端进程本身 SUCCESS（退出码 0、达到 `Done`、无崩溃）；实验目标是 `forgeprobe.jar`，该 JAR 在 Discovery 阶段被 NeoForge 判定为不兼容并跳过，从未成为 mod。**

日志路径:

- `D:\DualForge\spike000\logs\forgeprobe\forgeprobe.log`
- `D:\DualForge\spike000\logs\forgeprobe\forgeprobe.debug.log`
- `D:\DualForge\spike000\logs\forgeprobe\forgeprobe.stdout.log`
- `D:\DualForge\spike000\logs\forgeprobe\forgeprobe.stderr.log`（0 字节）
- `D:\DualForge\spike000\logs\forgeprobe\forgeprobe.mods-list.txt`
- `D:\DualForge\spike000\logs\forgeprobe\meta.json`

关键行（`latest.log`，WARN 级，唯一与本次实验相关的输出）:

```
[0710月2026 04:28:02.888] [main/WARN] [net.neoforged.fml.loading.moddiscovery.ModDiscoverer/SCAN]: Skipping jar. File D:\DualForge\spike000\neoforge-server\mods\forgeprobe.jar is for Minecraft Forge or an older version of NeoForge, and cannot be loaded
```

```
[0710月2026 04:28:09.194] [main/WARN] [net.neoforged.neoforge.server.loading.ServerModLoader/LOADING]: Mods loaded with 1 issues
[0710月2026 04:28:09.194] [main/WARN] [net.neoforged.neoforge.server.loading.ServerModLoader/LOADING]: fml.modloadingissue.brokenfile.minecraft_forge [[]]
```

`debug.log` 中的决定性取证（同一时刻、同一 locator 的三连行）:

```
[main/DEBUG] [net.neoforged.fml.loading.moddiscovery.ModDiscoverer/SCAN]: Trying locator {mods folder locator at D:\DualForge\spike000\neoforge-server\mods}
[main/DEBUG] [net.neoforged.fml.loading.moddiscovery.locators.ModsFolderLocator/SCAN]: Scanning mods dir D:\DualForge\spike000\neoforge-server\mods for mods
[main/WARN]  [net.neoforged.fml.loading.moddiscovery.ModDiscoverer/SCAN]: Skipping jar. File D:\DualForge\spike000\neoforge-server\mods\forgeprobe.jar is for Minecraft Forge or an older version of NeoForge, and cannot be loaded
[main/DEBUG] [net.neoforged.fml.loading.moddiscovery.ModDiscoverer/SCAN]: Locator {mods folder locator at D:\DualForge\spike000\neoforge-server\mods} found 0 mods, 1 warnings, 0 errors and skipped 0 candidates
```

**对照证据（同一份 `debug.log` 中，NeoForge 自带 JAR 走的是另一条路）：**

```
[main/DEBUG] [net.neoforged.fml.loading.moddiscovery.readers.JarModsDotTomlModFileReader/SCAN]: Found META-INF/neoforge.mods.toml mod of type null: ...\neoforge-21.1.256-universal.jar
[main/DEBUG] [net.neoforged.fml.loading.moddiscovery.ModFileParser/LOADING]: Considering mod file candidate ...\neoforge-21.1.256-universal.jar
[main/DEBUG] [net.neoforged.fml.loading.moddiscovery.ModFileInfo/LOADING]: Found valid mod file neoforge-21.1.256-universal.jar with {neoforge} mods - versions {21.1.256}
```

**`forgeprobe.jar` 从未出现上述三行中的任何一行。** 即：从未有 `IModFileReader` 接受它，从未构造 `IModFile`，`ModFileParser` 从未被调用。

其他关键行:

```
[Server thread/INFO] [net.minecraft.server.dedicated.DedicatedServer/]: Done (0.386s)! For help, type "help"
```

异常（若有）: **无异常、无堆栈、无 crash-report。** 失败以 `ModLoadingIssue`（严重度 WARNING）形式记录，流水线按「跳过该候选文件」处理并继续。详见第十一节。

Mod List（日志原文，`forgeprobe` 不在其中）：

```
     Mod List:
		Name Version (Mod Id)

		Minecraft 1.21.1 (minecraft)
		NeoForge 21.1.256 (neoforge)
```

---

## 六、日志 Diff 摘要

原始 diff 已单独保存：`D:\DualForge\spike000\logs\spike000.diff`

**生成方式（与说明书 `diff baseline.log forgeprobe.log` 等价，另做时间戳归一化）**：先剥去每行行首的 `[时间戳]`，再比对，以便只显示语义差异（两次运行的毫秒级时间戳必然不同，其它差异会被噪声淹没）。

与 `forgeprobe.jar` 相关的新增行（`=>` = 仅出现在 forgeprobe.log）：

```
  => [main/WARN] [net.neoforged.fml.loading.moddiscovery.ModDiscoverer/SCAN]: Skipping jar. File D:\DualForge\spike000\neoforge-server\mods\forgeprobe.jar is for Minecraft Forge or an older version of NeoForge, and cannot be loaded
  => [main/WARN] [net.neoforged.neoforge.server.loading.ServerModLoader/LOADING]: Mods loaded with 1 issues
  => [main/WARN] [net.neoforged.neoforge.server.loading.ServerModLoader/LOADING]: fml.modloadingissue.brokenfile.minecraft_forge [[]]
```

**diff 中其余行的定性（均为噪声，与实验无关）：**

| diff 行 | 定性 |
|---|---|
| `VanillaPackResourcesBuilder ... server-...-srg.jar%23117` vs `%23118` | 模块/工件编号，两次运行因加载顺序微差而不同，与 mod 无关 |
| `Preparing spawn area: 2%` / `0%`、`Time elapsed: 129 ms` / `236 ms` | 平坦世界生成进度，随机性输出 |
| `Done (0.526s)` vs `Done (0.386s)` | 启动耗时抖动 |

**结论**：除上述三行外，基线日志与 Spike-000 日志**没有任何结构性差异**。这本身就是一条关键实验事实——
`forgeprobe.jar` 的存在既没有让 NeoForge 提前崩溃，也没有进入任何后续阶段，只是被**静默跳过 + 记一条 issue**。

---

## 七、Failure Chain

> 判定原则：`class loaded ≠ method invoked ≠ stage entered ≠ operation succeeded`。
> 下表 `entered` 的判定依据均为「该阶段的落地方法是否被日志/字节码证明调用过」，而不是「日志里是否出现过类名」。

### Discovery
- entered: **YES**
- evidence:
  - `debug.log`：`Trying locator {mods folder locator at D:\DualForge\spike000\neoforge-server\mods}`
  - `debug.log`：`Scanning mods dir D:\DualForge\spike000\neoforge-server\mods for mods`
  - 字节码：`ModsFolderLocator.findCandidates(ILaunchContext, IDiscoveryPipeline)` 对 `mods/` 下每个 `*.jar` 调用 `pipeline.addPath(path, ModFileDiscoveryAttributes.DEFAULT, IncompatibleFileReporting.WARN_ALWAYS)`
  - 即：**候选发现成功，`forgeprobe.jar` 确实被交给发现流水线**
- error:
  ```
  [main/WARN] [net.neoforged.fml.loading.moddiscovery.ModDiscoverer/SCAN]: Skipping jar. File D:\DualForge\spike000\neoforge-server\mods\forgeprobe.jar is for Minecraft Forge or an older version of NeoForge, and cannot be loaded
  ```

### Metadata
- entered: **NO**
- evidence:
  - `ModDiscoverer$DiscoveryPipeline.addJarContent` 的字节码结构：先遍历 `modFileReaders` 调 `IModFileReader.read(...)`；**只有当某个 reader 返回非 null 时才调用 `addModFile(...)`**。
  - `JarModsDotTomlModFileReader` 的字节码：`public static final String MODS_TOML = "META-INF/neoforge.mods.toml"`，其 `read()` 第一分支即 `jar.findFile("META-INF/neoforge.mods.toml")`。`forgeprobe.jar` 不含该路径 → 第一分支不匹配；其第二分支（仅 manifest）要求 manifest 中存在可解析的 `FMLModType` 属性，`forgeprobe.jar` 没有 → 亦不匹配。
  - 因此所有 `IModFileReader` 返回 `null`，**从未构造 `IModFile`**，`ModFileParser` / `IModFileInfo` 从未运行。
  - 对照证据：`debug.log` 中有 `ModFileParser/LOADING: Considering mod file candidate ...neoforge-21.1.256-universal.jar`，**但没有任何一行提到 `forgeprobe.jar`**。
- error: 无（未进入该阶段，故无该阶段错误）

### Validation
- entered: **NO**
- evidence: `ModValidator` 的输入是 `ModFile` 集合；`forgeprobe.jar` 从未成为 `ModFile`（见 Metadata 项）。`debug.log` 全文无 `ModValidator` 处理 `forgeprobe.jar` 的记录。
- error: 无

### Scan（class scan / ModFileScanData）
- entered: **NO**
- evidence: class scan 的输入同样是 `ModFile`。无 `ModFile` → 无 `ModFileScanData` → 无 `@Mod` 注解的扫描结果。
  `forgeprobe.log`/`.debug.log` 中不存在任何针对 `forgeprobe.jar` 的 `Scanner`/`ModFileScanData` 记录。
- error: 无

### LanguageLoader
- entered: **NO**
- evidence: 语言加载通过 `IModLanguageLoader.loadMod(IModInfo, ModFileScanData, ModuleLayer)` 进行，其前置条件是存在 `IModInfo`（来自 `IModFileInfo`）。`forgeprobe.jar` 无 `IModFileInfo` → 无 `IModInfo` → 无 language loader 调用。
  日志中未出现 `Unknown language loader`、`javafml` 相关错误——**因为根本没有走到按 `modLoader="javafml"` 查表的环节**。
- error: 无

### Module
- entered: **NO**
- evidence: Game Layer 的输入是 `ModFile.getSecureJar()` 集合（`ModValidator.getModResources()`）。无 `ModFile` → 无 `SecureJar` 进入 Game Layer。
- error: 无

### Container
- entered: **NO**
- evidence: `FMLModContainer` 的创建入口是 `IModLanguageLoader.loadMod(...)`。未进入 LanguageLoader → 不可能进入 Container。
  日志中未出现 `FMLModContainer`。
- error: 无

### Constructor
- entered: **NO**
- evidence: 实验目的之一是观察 `ForgeProbe(IEventBus)` 是否被调用。`ForgeProbe` 的构造函数中的四行 `System.out.println` **一个都没有出现**在 `stdout.log` / `latest.log` 中（已全文检索 `FORGE PROBE CONSTRUCTOR REACHED`）。
  `ForgeProbe.class` 甚至从未被加载到 JVM。
- error: 无

**失败链一句话总结**：

```
Discovery 进入
   ↓ 候选文件被交给发现流水线
所有 IModFileReader 均返回 null（缺少 META-INF/neoforge.mods.toml，且 manifest 无 FMLModType）
   ↓
IncompatibleModReason.detect → MINECRAFT_FORGE（因为存在 META-INF/mods.toml）
   ↓
记录 ModLoadingIssue(fml.modloadingissue.brokenfile.minecraft_forge) → 跳过
   ↓
Metadata 及之后全部阶段：未进入
```

---

## 八、First Confirmed Failure

**阶段**: **Discovery 阶段的最后一环 —— ModFile 候选 → IModFile 的创建边界**
（即 `ModDiscoverer$DiscoveryPipeline.addJarContent` 中「无 reader 接受」之后的不兼容判定分支）

**证据**（三类证据互相独立、结论一致）:

1. **运行日志**（`forgeprobe.log` / `.debug.log`，WARN 级，且限定在 mods-folder locator 的时间窗口内）：
   ```
   Skipping jar. File D:\DualForge\spike000\neoforge-server\mods\forgeprobe.jar is for Minecraft Forge or an older version of NeoForge, and cannot be loaded
   Locator {mods folder locator at ...} found 0 mods, 1 warnings, 0 errors and skipped 0 candidates
   ```
2. **运行日志后的 issue 汇总**（`ServerModLoader`，证明这是一条被正式登记的 ModLoadingIssue）：
   ```
   Mods loaded with 1 issues
   fml.modloadingissue.brokenfile.minecraft_forge [[]]
   ```
3. **已发布 JAR 的字节码**（`loader-4.0.45.jar`，即 NeoForge 21.1.256 实际运行时加载的那一份）：
   - `net.neoforged.fml.loading.moddiscovery.ModDiscoverer$DiscoveryPipeline#addJarContent`：
     遍历 `modFileReaders` → 全部返回 `null` → 判定
     `IncompatibleFileReporting != IGNORE`（实际传入的是 `WARN_ALWAYS`）
     → `IncompatibleModReason.detect(jar)` → 命中 → 构造 issue → `LOGGER.atLevel(...).addMarker(SCAN).log("Skipping jar. {}", FMLTranslations.translateIssueEnglish(issue))` → `addIssue(issue)` → 返回 `Optional.empty()`
   - `net.neoforged.fml.loading.moddiscovery.IncompatibleModReason`（枚举静态初始化块）：
     `MINECRAFT_FORGE = new IncompatibleModReason("MINECRAFT_FORGE", 1, filePresent("META-INF/mods.toml"))`
     → `getReason()` = `StringUtils.toLowerCase(name())` 拼接为 `...brokenfile.minecraft_forge`
     → 命中原因**就是 `META-INF/mods.toml` 这个文件的存在本身**
   - `net.neoforged.fml.loading.moddiscovery.readers.JarModsDotTomlModFileReader`：
     `public static final String MODS_TOML = "META-INF/neoforge.mods.toml"`
     → 解释了为何 `forgeprobe.jar` 在进入不兼容判定之前就已经「无 reader 接受」
   - `net.neoforged.fml.loading.moddiscovery.ModFile`（静态初始化块）：
     `public static final Attributes.Name TYPE = new Attributes.Name("FMLModType")`
     → 这正是 `JarModsDotTomlModFileReader` 第二分支（仅 manifest 的 JAR）所读取的 manifest 属性名，
     而 `forgeprobe.jar` 的 manifest 中不存在该属性，故第二分支同样不成立
   - `net.neoforged.fml.loading.moddiscovery.locators.ModsFolderLocator#findCandidates`：
     → `pipeline.addPath(..., ModFileDiscoveryAttributes.DEFAULT, IncompatibleFileReporting.WARN_ALWAYS)`
     解释了为何该判定被记录为 WARNING 而不是被静默忽略（若传 `IGNORE` 则完全无日志）

**异常**: **无异常对象被抛出。**
失败以 `net.neoforged.fml.ModLoadingIssue` 表示，严重度 = WARNING（由 `IncompatibleFileReporting.WARN_ALWAYS.getIssueSeverity()` 决定），
issue key = `fml.modloadingissue.brokenfile.minecraft_forge`，
`affectedPath` = `D:\DualForge\spike000\neoforge-server\mods\forgeprobe.jar`。

**该 issue 的英文渲染文本来源**（已定位到 NeoForge 21.1.256 自带语言文件
`loader-4.0.45.jar!/lang/en_us.json`）:

```json
"fml.modloadingissue.brokenfile.minecraft_forge": "File {101} is for Minecraft Forge or an older version of NeoForge, and cannot be loaded",
```

**调用链**:

```
FMLLoader / ModDiscoverer.discoverMods(...)
  └─ ServiceLoader 载入的候选发现器（locator）
       └─ ModsFolderLocator.findCandidates(ILaunchContext, IDiscoveryPipeline)
            ├─ Files.list(mods/) → filter(endsWith(".jar"))（大小写不敏感）
            └─ IDiscoveryPipeline.addPath(forgeprobe.jar,
                                          ModFileDiscoveryAttributes.DEFAULT,
                                          IncompatibleFileReporting.WARN_ALWAYS)
                 └─ ModDiscoverer$DiscoveryPipeline.addJarContent(JarContents, attrs, reporting)
                      ├─ for (IModFileReader r : modFileReaders)               ← ServiceLoader: 0 / -1000 优先级
                      │     r.read(jarContents, attributes)
                      │        ├─ JarModsDotTomlModFileReader.read(...)        ← 需要 META-INF/neoforge.mods.toml
                      │        │     或 manifest 中可解析的 FMLModType         → 均不满足 → null
                      │        └─ NestedLibraryModReader.read(...)              → null
                      ├─ (全部 null，且 reporting != IGNORE)
                      ├─ IncompatibleModReason.detect(jarContents)
                      │     └─ MINECRAFT_FORGE.ident.test(jar) = filePresent("META-INF/mods.toml")
                      │           → Optional.of(MINECRAFT_FORGE)
                      ├─ ModLoadingIssue.error("fml.modloadingissue.brokenfile." + "minecraft_forge")
                      │     .withAffectedPath(forgeprobe.jar)
                      │     .withSeverity(WARNING)
                      ├─ LOGGER.atLevel(WARN).addMarker(SCAN).log("Skipping jar. {}", <英文渲染>)
                      ├─ addIssue(issue)                                       ← 计入 warningCount
                      └─ return Optional.empty()                               ← 候选被丢弃，流水线继续
  └─ （后续）ModValidator / BackgroundScanHandler / LanguageProviderLoader /
             Game Layer / FMLModContainer / ForgeProbe.<init>
             —— 对 forgeprobe.jar 全部未执行
```

**对应源码入口（若可定位）**:

| 类（已发布 JAR 中的真实字节码） | 方法 | 作用 |
|---|---|---|
| `net.neoforged.fml.loading.moddiscovery.ModDiscoverer$DiscoveryPipeline` | `addJarContent(JarContents, ModFileDiscoveryAttributes, IncompatibleFileReporting)` | **首次失败发生处**：reader 全失败后判定不兼容并跳过 |
| `net.neoforged.fml.loading.moddiscovery.IncompatibleModReason` | `detect(JarContents)` / `<clinit>` / `getReason()` | 命中原因 `MINECRAFT_FORGE` ≡ `filePresent("META-INF/mods.toml")` |
| `net.neoforged.fml.loading.moddiscovery.readers.JarModsDotTomlModFileReader` | `read(JarContents, ModFileDiscoveryAttributes)` | 证明第一道门槛是 `META-INF/neoforge.mods.toml` |
| `net.neoforged.fml.loading.moddiscovery.locators.ModsFolderLocator` | `findCandidates(ILaunchContext, IDiscoveryPipeline)` | 以 `WARN_ALWAYS` 提交候选（决定可见性等级） |
| `net.neoforged.fml.i18n.FMLTranslations` | `translateIssueEnglish(ModLoadingIssue)` | 生成 "Skipping jar." 后的英文文本 |
| `net.neoforged.neoforge.server.loading.ServerModLoader` | （LOADING 汇总） | 输出 `Mods loaded with 1 issues` |

> 上述类/方法名与常量均取自 `loader-4.0.45.jar` 的 `javap -p -c -constants` 输出，
> 对应的上游源文件名由 class 文件的 `SourceFile` 属性给出：
> `ModDiscoverer.java`、`IncompatibleModReason.java`、`JarModsDotTomlModFileReader.java`、`ModsFolderLocator.java`。
> **未使用 current/main 分支源码替代，全部来自实验所用的 NeoForge 21.1.256 运行时工件。**

---

## 九、SPI 调用证据

N/A（Spike-000 无兼容层代码，无兼容层 provider）

---

## 十、Module Identity（若实验走到此阶段）

SecureJar.name: **N/A**（`forgeprobe.jar` 从未被构造成 `IModFile` / `SecureJar`）
IModInfo.moduleName: **N/A**
ModuleDescriptor.name: **N/A**
GameLayer.findModule(...): **N/A**
四重一致性: **N/A**

> 补充说明（`[E]`）：实验确实**没有**走到模块阶段，所以此事与 `Automatic-Module-Name: forgeprobe` 是否生效无关。
> 该属性已在 JAR 中声明，但从未被读取。

---

## 十一、完整异常

### 结论：本次实验**没有产生任何 Java 异常（exception）**，因此**不存在 stack trace / `Caused by` 链**。

这是本实验最重要的、与预判不同的观测事实之一：`[E]`

| 预判（手册 §14.2） | 实测 |
|---|---|
| 「NeoForge 默认 metadata reader 无法将 `mods.toml` 作为原生 NeoForge mod metadata 接受」 | 方向一致，但**失败位置比预期更早**：根本没走到 metadata reader，而是在 **Discovery 阶段的 ModFile 创建边界**就被拦下 |
| 预期形态：抛异常 / `Unknown language loader` / `module not found` | 实测形态：**不抛异常，记录一条 WARNING 级 ModLoadingIssue 并跳过**，游戏正常启动 |

**完整相关日志（forgeprobe 运行，不截断，按时间顺序）：**

```
[0710月2026 04:28:02.469] [main/INFO] [cpw.mods.modlauncher.Launcher/MODLAUNCHER]: ModLauncher running: args [--launchTarget, forgeserver, --fml.neoForgeVersion, 21.1.256, --fml.fmlVersion, 4.0.45, --fml.mcVersion, 1.21.1, --fml.neoFormVersion, 20240808.144430, nogui]
[0710月2026 04:28:02.472] [main/INFO] [cpw.mods.modlauncher.Launcher/MODLAUNCHER]: JVM identified as Microsoft OpenJDK 64-Bit Server VM 21.0.10+7-LTS
[0710月2026 04:28:02.474] [main/INFO] [cpw.mods.modlauncher.Launcher/MODLAUNCHER]: ModLauncher 11.0.5+main.901c6ea8 starting: java version 21.0.10 by Microsoft; OS Windows 11 arch amd64 version 10.0
[0710月2026 04:28:02.561] [main/INFO] [net.neoforged.fml.loading.ImmediateWindowHandler/]: ImmediateWindowProvider not loading because launch target is forgeserver
[0710月2026 04:28:02.604] [main/INFO] [mixin/]: SpongePowered MIXIN Subsystem Version=0.8.7 Source=union:/D:/DualForge/spike000/neoforge-server/libraries/net/fabricmc/sponge-mixin/0.16.4+mixin.0.8.7/sponge-mixin-0.16.4+mixin.0.8.7.jar%2373!/ Service=ModLauncher Env=SERVER
[0710月2026 04:28:02.827] [main/INFO] [net.neoforged.fml.loading.moddiscovery.ModDiscoverer/SCAN]: Found mod file "server-1.21.1-20240808.144430-srg.jar" [locator: production server provider +net.neoforged:neoforge:21.1.256:server]
[0710月2026 04:28:02.849] [main/INFO] [net.neoforged.fml.loading.moddiscovery.ModDiscoverer/SCAN]: Found mod file "neoforge-21.1.256-universal.jar" [locator: PathBasedLocator[name=neoforge, paths=[libraries\net\neoforged\neoforge\21.1.256\neoforge-21.1.256-universal.jar]], reader: mod manifest]
[0710月2026 04:28:02.888] [main/WARN] [net.neoforged.fml.loading.moddiscovery.ModDiscoverer/SCAN]: Skipping jar. File D:\DualForge\spike000\neoforge-server\mods\forgeprobe.jar is for Minecraft Forge or an older version of NeoForge, and cannot be loaded
[0710月2026 04:28:02.959] [main/INFO] [net.neoforged.fml.loading.moddiscovery.locators.JarInJarDependencyLocator/]: Found 2 dependencies adding them to mods collection
[0710月2026 04:28:02.959] [main/INFO] [net.neoforged.fml.loading.moddiscovery.ModDiscoverer/SCAN]: Found gamelibrary file "mixinextras-neoforge-0.5.5.jar" [parent: neoforge-21.1.256-universal.jar, locator: jarinjar, reader: mod manifest]
[0710月2026 04:28:02.959] [main/INFO] [net.neoforged.fml.loading.moddiscovery.ModDiscoverer/SCAN]: Found library file "net.neoforged.neoforge-coremods-21.1.256.jar" [parent: neoforge-21.1.256-universal.jar, locator: jarinjar, reader: mod manifest]
[0710月2026 04:28:02.960] [main/INFO] [net.neoforged.fml.loading.moddiscovery.ModDiscoverer/]: 
     Mod List:
		Name Version (Mod Id)

		Minecraft 1.21.1 (minecraft)
		NeoForge 21.1.256 (neoforge)
[0710月2026 04:28:03.314] [main/INFO] [cpw.mods.modlauncher.LaunchServiceHandler/MODLAUNCHER]: Launching target 'forgeserver' with arguments [nogui]
[0710月2026 04:28:05.236] [main/INFO] [MixinExtras|Service/]: Initializing MixinExtras via com.llamalad7.mixinextras.service.MixinExtrasServiceImpl(version=0.5.5).
[0710月2026 04:28:08.950] [modloading-worker-0/INFO] [net.neoforged.neoforge.common.NeoForgeMod/NEOFORGE-MOD]: NeoForge mod loading, version 21.1.256, for MC 1.21.1
[0710月2026 04:28:09.194] [main/WARN] [net.neoforged.neoforge.server.loading.ServerModLoader/LOADING]: Mods loaded with 1 issues
[0710月2026 04:28:09.194] [main/WARN] [net.neoforged.neoforge.server.loading.ServerModLoader/LOADING]: fml.modloadingissue.brokenfile.minecraft_forge [[]]
[0710月2026 04:28:10.661] [Server thread/INFO] [net.minecraft.server.dedicated.DedicatedServer/]: Starting minecraft server version 1.21.1
[0710月2026 04:28:11.253] [Server thread/INFO] [net.minecraft.server.dedicated.DedicatedServer/]: Done (0.386s)! For help, type "help"
[0710月2026 04:28:11.284] [Server thread/INFO] [net.neoforged.neoforge.server.permission.PermissionAPI/]: Successfully initialized permission handler neoforge:default_handler
[0710月2026 04:28:23.041] [Server thread/INFO] [net.minecraft.server.MinecraftServer/]: Stopping the server
[0710月2026 04:28:23.111] [Server thread/INFO] [net.minecraft.server.MinecraftServer/]: Saving chunks for level 'ServerLevel[spike000world]'/minecraft:overworld
[0710月2026 04:28:23.159] [Server thread/INFO] [net.minecraft.server.MinecraftServer/]: ThreadedAnvilChunkStorage: All dimensions are saved
```

**完整问题对象（`ModLoadingIssue`）的等价文本表示：**

```
key:      fml.modloadingissue.brokenfile.minecraft_forge
severity: WARNING                       (来自 IncompatibleFileReporting.WARN_ALWAYS.getIssueSeverity())
path:     D:\DualForge\spike000\neoforge-server\mods\forgeprobe.jar
cause:    <无>                          (没有任何 Throwable 被附加到该 issue)
en_us:    File D:\DualForge\spike000\neoforge-server\mods\forgeprobe.jar is for Minecraft Forge or an older version of NeoForge, and cannot be loaded
汇总输出: Mods loaded with 1 issues / fml.modloadingissue.brokenfile.minecraft_forge [[]]
```

> 注意 `[[]]` 不是乱码：它是该 issue 参数列表（此处为空列表）在控制台渲染时的表示。

**stdout 中确实没有出现的字符串（用于反证 Constructor 阶段未进入）：**

```
FORGE PROBE CONSTRUCTOR REACHED
BUS = 
=================================
```

---

## 十二、原始日志

- baseline.log 路径: `D:\DualForge\spike000\logs\baseline\baseline.log`（7881 字节，完整 `latest.log` 副本）
- baseline debug.log 路径: `D:\DualForge\spike000\logs\baseline\baseline.debug.log`（75091 字节）
- baseline stdout 路径: `D:\DualForge\spike000\logs\baseline\baseline.stdout.log`（6497 字节）
- baseline stderr 路径: `D:\DualForge\spike000\logs\baseline\baseline.stderr.log`（0 字节）
- forgeprobe.log 路径: `D:\DualForge\spike000\logs\forgeprobe\forgeprobe.log`（8424 字节）
- forgeprobe debug.log 路径: `D:\DualForge\spike000\logs\forgeprobe\forgeprobe.debug.log`（75626 字节）
- forgeprobe stdout 路径: `D:\DualForge\spike000\logs\forgeprobe\forgeprobe.stdout.log`（6933 字节）
- forgeprobe stderr 路径: `D:\DualForge\spike000\logs\forgeprobe\forgeprobe.stderr.log`（0 字节）
- **crash-report 路径（若存在）: 不存在。** 两次有效运行均未产生 crash-report
  （`D:\DualForge\spike000\logs\baseline\crash-reports` 与 `...\forgeprobe\crash-reports` 均不存在）
- diff 路径: `D:\DualForge\spike000\logs\spike000.diff`
- 运行元数据: `...\baseline\meta.json`、`...\forgeprobe\meta.json`
- jar 元数据: `D:\DualForge\spike000\logs\forgeprobe.jar.metadata.txt`
- 运行脚本（可复现）: `D:\DualForge\spike000\run-server.ps1`
- 服务端工作目录: `D:\DualForge\spike000\neoforge-server`（NeoForge 官方 installer `--installServer` 产物，未做任何修改）

**启动命令（两次完全一致）：**

```
C:\Users\Albert\.jdks\ms-21.0.10\bin\java.exe @user_jvm_args.txt @libraries/net/neoforged/neoforge/21.1.256/win_args.txt nogui
```

working directory = `D:\DualForge\spike000\neoforge-server`，
`user_jvm_args.txt` = `-Xmx3G -Xms1G`。

**两次运行唯一的差异**：`mods/` 目录内容
（baseline = 空；forgeprobe = 仅含 `forgeprobe.jar`）。
`server.properties`、`eula.txt`、JVM 参数、启动参数、Java 版本、NeoForge 版本、目录全部相同。

---

## 十三、事实标签

### [F] Source Fact

1. `[F]` NeoForge 21.1.256 的 mod 元数据文件名是 `META-INF/neoforge.mods.toml`
   —— 常量 `JarModsDotTomlModFileReader.MODS_TOML = "META-INF/neoforge.mods.toml"`（`loader-4.0.45.jar`）。
2. `[F]` `IncompatibleModReason.MINECRAFT_FORGE` 的判定谓词是 `filePresent("META-INF/mods.toml")`
   —— 即「JAR 中存在 `META-INF/mods.toml`」本身就足以被判为 Minecraft Forge 包。
3. `[F]` `IncompatibleModReason` 枚举共 7 项：`OLDFORGE(mcmod.info)`、`MINECRAFT_FORGE(META-INF/mods.toml)`、
   `FABRIC(fabric.mod.json)`、`QUILT(quilt.mod.json)`、`LITELOADER(litemod.json)`、
   `OPTIFINE(optifine/Installer.class)`、`BUKKIT(plugin.yml)`；`getReason()` = `"fml.modloadingissue.brokenfile." + lowercase(name())`。
4. `[F]` `ModsFolderLocator.findCandidates` 以 `IncompatibleFileReporting.WARN_ALWAYS` 提交候选，
   因此这类文件会产生 WARNING 级 issue 与 "Skipping jar." 日志（若传 `IGNORE` 则无任何输出）。
5. `[F]` `DiscoveryPipeline.addJarContent` 只有在某个 `IModFileReader.read(...)` 返回非 null 时才调用 `addModFile(...)`；
   全部返回 null 时才进入不兼容判定分支。
6. `[F]` `IModFileReader` 通过 ServiceLoader 加载，内置实现为
   `JarModsDotTomlModFileReader`（优先级 0）与 `NestedLibraryModReader`（优先级 -1000）。
7. `[F]` FML 版本由 `--fml.fmlVersion` 参数给出，且与工件目录 `fancymodloader/loader/4.0.45/` 一致；
   FML 4.x 日志中不存在 `Starting FancyModLoader version ...` 形式的行。
8. `[F]` 本实验所有 FML 源码结论均由 `loader-4.0.45.jar` 的字节码（`javap -p -c -constants`）得出，
   `SourceFile` 属性给出上游文件名，未使用其它分支源码。
9. `[F]` `ModFile.TYPE = new java.util.jar.Attributes.Name("FMLModType")`；
   `JarModsDotTomlModFileReader.getModType()` 读取的正是该属性，取值非法时抛
   `fml.modloadingissue.brokenfile.unknownfmlmodtype`，缺失时返回 `null`。

### [E] Experimental Fact

1. `[E]` 在 NeoForge 21.1.256（Dedicated Server）中放入一个 Forge 原生 JAR（`META-INF/mods.toml`，
   无 `neoforge.mods.toml`，manifest 无 `FMLModType`），**不会导致启动失败**：
   服务端退出码 0、达到 `Done (0.386s)`、无 crash-report。
2. `[E]` 该 JAR 被判定为 `fml.modloadingissue.brokenfile.minecraft_forge`，以 WARNING 级 issue 记录，
   文件被**跳过**（`found 0 mods, 1 warnings, 0 errors and skipped 0 candidates`）。
3. `[E]` 该 JAR **从未**产生 `Found META-INF/neoforge.mods.toml mod of type ...`、
   `Considering mod file candidate ...`、`Found valid mod file ...` 中任何一行
   （对照：NeoForge 自己的 `neoforge-21.1.256-universal.jar` 三行俱全）。
4. `[E]` 该 JAR **从未**出现在最终 `Mod List` 中。
5. `[E]` `ForgeProbe` 的构造函数输出（`FORGE PROBE CONSTRUCTOR REACHED` / `BUS = ...`）
   **从未**出现在 stdout 或任何日志中；`ForgeProbe.class` 从未被加载。
6. `[E]` 基线日志与实验日志除 3 行新增 WARN 外**无结构性差异**（见第六节 diff 表）。
7. `[E]` 沙箱内（受网络策略限制）运行同一服务端时，`java.base/sun.nio.ch.WEPollSelectorImpl`
   构造失败（`java.net.SocketException: Permission denied: connect`，loopback），
   导致 `ServerConnectionListener.startTcpServerListener` 抛 `IllegalStateException` 并崩溃。
   **这是宿主沙箱策略造成的环境干扰，与 NeoForge/Forge 无关**，该次运行已作废（见附录）。
8. `[E]` 未观察到 `Unknown language loader`、`javafml` 相关错误、`module not found`、
   `NoSuchMethodError`、`ClassNotFoundException` 中任何一项——因为流水线未走到那些阶段。

### [H] Hypothesis

1. `[H]` 若 `forgeprobe.jar` 改用 `META-INF/neoforge.mods.toml`（其余不动），则本轮「Discovery 阶段拦截」将消失，
   失败点会向后推移到语言加载 / 模块 / 容器阶段。**本 Spike 未验证，不得当作结论。**
2. `[H]` Spike-000 结论对 Client 端大概率同样成立（发现与元数据流水线在两端共用），
   但**本 Spike 仅在 Dedicated Server 上验证**，Client 端属未验证。
3. `[H]` `NestedLibraryModReader` 亦未接受该 JAR，因为其面向内嵌库而非 mod 本体；
   本 Spike 只观察到「全部 reader 返回 null」的净结果，未逐一向其内部取证。
4. `[H]` `skipped 0 candidates` 中的 `skipped` 计数器与本次的 `warnings` 计数器不是同一个量，
   故本次显示为 1 warnings / 0 skipped。

### [D] Design Decision

1. `[D]` 手册中「FancyModLoader 版本必须从日志读取 `Starting FancyModLoader version 11.0.xx`」的约定，
   与 NeoForge 21.1.256 实际捆绑的 FML 4.x 编号体系不符，**需要在文档修订时改正取证方式**。
2. `[D]` 「第一条红线」被实测定位在 **Discovery 阶段的 ModFile 创建边界**，
   按交接规则（说明书第八节）该项对应 **Spike-001a（Locator/Reader 接入）优先**，
   而不是 Metadata 从 Parser 接入。此判断需由架构决策方确认。
3. `[D]` 兼容层若要让 Forge JAR 越过本红线，需要提供能接受 `META-INF/mods.toml`
   且带 `attributes.locator() instanceof ForgeModLocator` 语义的 `IModFileReader`；
   但本 Spike 不提出任何实现方案。

---

## 十四、结论

**First confirmed failure**:

> NeoForge 21.1.256 原生运行时面对 Forge 原生 JAR `forgeprobe.jar` 时，
> **第一次失败发生在 Discovery 阶段的 ModFile 创建边界**：
> 所有 `IModFileReader`（关键实现 `JarModsDotTomlModFileReader` 要求 `META-INF/neoforge.mods.toml`）
> 均无法接受该 JAR，随后 `IncompatibleModReason` 依据 `META-INF/mods.toml` 的存在命中
> `MINECRAFT_FORGE`，NeoForge 记录 `fml.modloadingissue.brokenfile.minecraft_forge`
> 并**跳过**该文件。**Metadata 及其后所有阶段（Validation / Scan / LanguageLoader /
> Module / Container / Constructor）均未进入。**
>
> **该失败不表现为异常或崩溃**，而是「WARNING + 跳过 + 继续启动」。

**Confidence**: **HIGH**

**Evidence**:

1. 运行日志三行：`Skipping jar. File ...\forgeprobe.jar is for Minecraft Forge or an older version of NeoForge, and cannot be loaded`
   / `Mods loaded with 1 issues` / `fml.modloadingissue.brokenfile.minecraft_forge [[]]`。
2. `debug.log` 的 locator 级对账行：`Locator {mods folder locator at ...} found 0 mods, 1 warnings, 0 errors`。
3. 阴性对照：同一份 `debug.log` 中 NeoForge 自带 JAR 完整走完
   `JarModsDotTomlModFileReader` → `ModFileParser` → `ModFileInfo`，而 `forgeprobe.jar` 三行全无。
4. 字节码级证据：`DiscoveryPipeline#addJarContent` 的控制流 + `IncompatibleModReason.<clinit>` 的
   `filePresent("META-INF/mods.toml")` + `JarModsDotTomlModFileReader.MODS_TOML` 常量，
   三者与日志现象一一对应，且全部取自实验所用的 `loader-4.0.45.jar`。
5. 构造函数标记字符串 `FORGE PROBE CONSTRUCTOR REACHED` 在全部输出中零命中。

**Hypotheses still unverified**（见第十三节 [H]）:

1. 换成 `neoforge.mods.toml` 后失败点是否会后移，以及后移到哪一阶段。
2. Client 端（`forgeclient`）是否给出完全相同的失败点。
3. 逐 reader 的拒绝原因（本 Spike 只取得「全部返回 null」的净结论）。
4. `WARN_ALWAYS` 之外的 `IncompatibleFileReporting` 取值对本流程的影响面。

---

## 十五、禁止事项确认

- [x] 未修改 ForgeProbe ABI
      （构造函数仍为 `ForgeProbe(net.minecraftforge.eventbus.api.IEventBus)`，`javap` 已核验）
- [x] 未添加 neoforge.mods.toml
      （JAR tree 中只有 `META-INF/mods.toml`，已用 `jar tf` 核验）
- [x] 未添加兼容层代码
      （`D:\DualForge\spike000` 下无 Locator/Reader/Parser/LanguageLoader/ASM/Mixin/Mapping 代码）
- [x] 未修改 NeoForge 核心
      （`neoforge-server/` 为官方 installer 原样产物；仅新增 `eula.txt`、`server.properties`、
      `user_jvm_args.txt`、`mods/forgeprobe.jar` 与运行期自动生成物）
- [x] 未在报告中提出修复方案
      （第八节给出的是源码入口与调用链，第十四节的 [D] 仅为交接归属建议，不含实现方案）

---

## 附录（非模板字段，仅供追溯）

### A. 执行偏差清单（诚实记录）

| 项 | 说明书要求 | 实际执行 | 影响评估 |
|---|---|---|---|
| 运行端 | `客户端/服务端: <Client / Server>` | **Server（Dedicated）** | 发现/元数据流水线两端共用；但 Client 端未验证，已列为 [H] |
| 启动方式 | Gradle runClient / 启动器 / CLI 均可 | **CLI**（官方 installer 产物 + `java @argfile nogui`） | 符合说明书允许的第三种方式 |
| 构建方式 | `build.gradle`（ForgeGradle） | **javac + 官方 installer 解析出的 Forge 1.21.1-52.1.0 工件** | 编译依赖仍为 Forge 1.21.1-52.1.0，未引入 NeoForge；`build.gradle` 关键行已按要求保留 |
| 停止方式 | 未规定 | 达到 `Done` 后经控制台 `stop` **优雅关闭** | 无影响，两次一致 |
| FML 版本取证 | 读 `Starting FancyModLoader version 11.0.xx` | 该行不存在，改由 `--fml.fmlVersion` 与工件目录取证 | 已在第一节详述 |

### B. 首个被作废的运行（环境干扰，非实验数据）

第一次基线运行在**沙箱受限环境**中执行，服务端在
`ServerConnectionListener.startTcpServerListener` 处崩溃，根因为宿主网络策略禁止 loopback socket：

```
Caused by: java.net.SocketException: Permission denied: connect
	at java.base/sun.nio.ch.Net.connect0(Native Method)
	...
	at java.base/sun.nio.ch.WEPollSelectorImpl.<init>(WEPollSelectorImpl.java:79)
```

该次运行与 NeoForge/Forge 无关，且其产物已被后续有效运行覆盖（不再存在于磁盘）。
第四节所报的基线结果是**在沙箱外重新执行**的结果。两次有效运行（baseline / forgeprobe）
均在同一环境、同一配置下完成，具备可比性。

### C. 复现方式

```
# 1. 构建探针（需 Forge 1.21.1-52.1.0 工件）
#    见第三节的 javac 命令；产物 = spike000\probe\build\forgeprobe.jar

# 2. 安装 NeoForge 21.1.256 服务端（官方 installer）
java -jar neoforge-21.1.256-installer.jar --installServer spike000\neoforge-server

# 3. 基线（mods/ 为空）
powershell -NoProfile -ExecutionPolicy Bypass -File spike000\run-server.ps1 -Label baseline

# 4. 实验（mods/ 仅含 forgeprobe.jar）
powershell -NoProfile -ExecutionPolicy Bypass -File spike000\run-server.ps1 -Label forgeprobe `
    -ModsJar spike000\probe\build\forgeprobe.jar
```

> 注：脚本会在每次运行前清空 `mods/*.jar` 并写入同一份 `server.properties`，
> 因此两次运行唯一变量就是 `-ModsJar`。
