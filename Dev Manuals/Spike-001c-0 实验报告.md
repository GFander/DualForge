# Spike-001c-0 实验报告

> **结论（先行）**：**五层判据 A / B / C / D / E 全部 PASS。**
>
> NeoForge 21.1.256 的正常加载链交给 `IModLanguageLoader.loadMod(...)` 的
> `ModFileScanData` **确实包含** ForgeProbe 的 `net.minecraftforge.fml.common.Mod` 注解，
> 且 `targetClass` / `value` **均正确**；同时**不含** `net.neoforged.fml.common.Mod`。
>
> 按 §九：**不进入 001c-1 / 001c-2**，报告原样交回。

---

## 一、实验环境

同 001b-6：Minecraft 1.21.1 / NeoForge 21.1.256 / FancyModLoader 4.0.45 /
Java 21（OpenJDK 21.0.10，Microsoft Build）/ Windows 11 amd64 / CLI Dedicated Server /
编译依赖 Forge 1.21.1-52.1.0。运行标签 `spike001c-0`。

---

## 二、实验目标

确定 NeoForge 21.1.256 在真实加载链中交给 `IModLanguageLoader.loadMod(...)` 的
`ModFileScanData` 是否包含 ForgeProbe 的 `net.minecraftforge.fml.common.Mod` 注解，
以及该 annotation 的 metadata 是否正确（五层判据 A–E）。

---

## 三、ForgeProbe 实际 FQCN

- 来源: `jar tf forgeprobe.jar` 输出
- **FQCN: `com.example.forgeprobe.ForgeProbe`**

```
META-INF/
META-INF/MANIFEST.MF
META-INF/mods.toml
com/
com/example/
com/example/forgeprobe/
com/example/forgeprobe/ForgeProbe.class
```

SHA-256: `a93a823c988bfd6a8f6ea132d189d9de732b0e5951f4bf74980d51bb63374e22`（未改动）。

---

## 四、ModFileScanData / AnnotationData 公开 API（001c-0-a）

> **⚠ 与说明书的偏差（重要）**：Spike_001c-0.md §四 写的是
> `getClassType()` / `getAnnotationData()`，**实际 API 不存在这两个方法**。
> 真实方法名为 **`clazz()`** 与 **`annotationData()`**。本报告的 dump 按**实际 API** 实现。

**ModFileScanData**（`net.neoforged.neoforgespi.language.ModFileScanData`）：

```
public ModFileScanData()
public Set<ModFileScanData.ClassData>        getClasses()
public Set<ModFileScanData.AnnotationData>   getAnnotations()
public Stream<AnnotationData>                getAnnotatedBy(Class<? extends Annotation>, ElementType)
public void                                  addModFileInfo(IModFileInfo)
public List<IModFileInfo>                    getIModInfoData()
```

**AnnotationData**（`...ModFileScanData$AnnotationData`，**record**）：

| 方法 | 返回类型 |
|---|---|
| `annotationType()` | `org.objectweb.asm.Type` |
| `targetType()` | `java.lang.annotation.ElementType` |
| `clazz()` | `org.objectweb.asm.Type` ← **target 类** |
| `memberName()` | `String` |
| `annotationData()` | `Map<String, Object>` ← **注解值** |

字段与构造签名一致（`AnnotationData(Type, ElementType, Type, String, Map)`），全部 public。
**PASS-001c-0-a ✅**（含上述偏差修正）。

---

## 五、loadMod 中 dump（001c-0-b）

### 五层判据

```
A   loadMod received ModFileScanData : PASS   (scanData != null = true)
B   scanData has annotations         : PASS   (size = 1)
C   contains net.minecraftforge.fml.common.Mod : **YES**
D   target == com.example.forgeprobe.ForgeProbe : **YES**
E   value  == "forgeprobe"                       : **YES**
```

### 对照信息

```
contains net.neoforged.fml.common.Mod : **NO**
```

### 完整 dump 日志原文（不截断、不过滤）

```
[FORGECOMPAT][SCAN-DUMP] ============================
[FORGECOMPAT][SCAN-DUMP] modId            = forgeprobe
[FORGECOMPAT][SCAN-DUMP] scanData != null = true
[FORGECOMPAT][SCAN-DUMP] A  loadMod received ModFileScanData  : PASS
[FORGECOMPAT][SCAN-DUMP] annotations      = 1
[FORGECOMPAT][SCAN-DUMP]   [0] annotationType = net.minecraftforge.fml.common.Mod
[FORGECOMPAT][SCAN-DUMP]       targetClass    = com.example.forgeprobe.ForgeProbe
[FORGECOMPAT][SCAN-DUMP]       targetType     = TYPE
[FORGECOMPAT][SCAN-DUMP]       memberName     = com.example.forgeprobe.ForgeProbe
[FORGECOMPAT][SCAN-DUMP]       values         = {value=forgeprobe}
[FORGECOMPAT][SCAN-DUMP] B  scanData has annotations           : PASS
[FORGECOMPAT][SCAN-DUMP] C  contains net.minecraftforge.fml.common.Mod : YES
[FORGECOMPAT][SCAN-DUMP] D  target == com.example.forgeprobe.ForgeProbe : YES
[FORGECOMPAT][SCAN-DUMP] E  value  == "forgeprobe"             : YES
[FORGECOMPAT][SCAN-DUMP] ---
[FORGECOMPAT][SCAN-DUMP] 对照: contains net.neoforged.fml.common.Mod : NO
[FORGECOMPAT][SCAN-DUMP] classes          = 1
[FORGECOMPAT][SCAN-DUMP]   [0] ClassData[clazz=Lcom/example/forgeprobe/ForgeProbe;, parent=Ljava/lang/Object;, interfaces=[]]
[FORGECOMPAT][SCAN-DUMP] modFileInfos     = 1
[FORGECOMPAT][SCAN-DUMP] ============================
```

打印后立即抛出 `UnsupportedOperationException("Observation only")`，流程停在 dump 之后。
进程退出码 0，未达 `Done`，无 crash-report。

**PASS-001c-0-b ✅**（五层判据均已记录，无 UNKNOWN）。

---

## 六、清理确认（001c-0-c）

| 项 | 结果 |
|---|---|
| `ForgeCompatLanguageLoader` 已删除 | ✅（源文件 + `.class` + service 声明） |
| `IModLanguageLoader` service 声明已删除 | ✅ |
| `IModFileReader` service 与基线一致 | ✅ |
| `IModFileCandidateLocator` service 与基线一致 | ✅ |
| `MANIFEST` 与基线一致 | ✅ |
| `forgeprobe.jar` 未修改 | ✅（`a93a823c…374e22`） |

**逐条目比对：entries = 15 / diff = 0**（对照 `build\forgecompat-001b1.jar`）。**PASS-001c-0-c ✅**

---

## 七、Failure Chain

```
Discovery:       YES（沿用 001b-6）
Metadata:        YES（沿用 001b-6）
Validation:      YES（沿用 001b-6）
Scan:            **YES**（本轮首次直接观测：ModFileScanData 有 1 条 annotation、1 个 class）
LanguageLoader:  YES
ModSorter:       PASS
LoadingModList:  PASS
Module:          UNKNOWN
Container:       UNKNOWN（`loadMod` 入口已进入，但 **FMLModContainer 是否构造未知**）
Constructor:     NO
```

> `loadMod` 入口 ≠ `FMLModContainer` 已构造；本轮 dump 后即被观测桩终止，未进入容器构造。

---

## 八、完整日志

- latest.log: `D:\DualForge\spike000\logs\spike001c-0\spike001c-0.log`
- debug.log: `D:\DualForge\spike000\logs\spike001c-0\spike001c-0.debug.log`
- stdout: `D:\DualForge\spike000\logs\spike001c-0\spike001c-0.stdout.log`（含完整 dump）
- stderr: `D:\DualForge\spike000\logs\spike001c-0\spike001c-0.stderr.log`
- crash-report: **不存在**

---

## 九、事实标签

### [F] Source Fact

1. `[F]` `ModFileScanData.getAnnotations()` 返回 `Set<AnnotationData>`；
   `getClasses()` 返回 `Set<ClassData>`；`getIModInfoData()` 返回 `List<IModFileInfo>`。
2. `[F]` `AnnotationData` 是 record，访问器为 **`annotationType()` / `targetType()` /
   `clazz()` / `memberName()` / `annotationData()`**（**没有** `getClassType()` / `getAnnotationData()`）。
3. `[F]` `annotationType()` / `clazz()` 返回 `org.objectweb.asm.Type`。

### [E] Experimental Fact

1. `[E]` `loadMod(...)` 收到的 `scanData` **非 null**（A PASS）。
2. `[E]` 该 `scanData` 含 **1 条 annotation**（B PASS）。
3. `[E]` 该 annotation 的 `annotationType = net.minecraftforge.fml.common.Mod` ——
   **NeoForge 内置 scanner 扫描到了 Forge 原生注解类名**（C PASS, **YES**）。
4. `[E]` `clazz = com.example.forgeprobe.ForgeProbe`（D PASS, **YES**），
   `targetType = TYPE`，`memberName = com.example.forgeprobe.ForgeProbe`。
5. `[E]` `annotationData = {value=forgeprobe}`（E PASS, **YES**）。
6. `[E]` **不含** `net.neoforged.fml.common.Mod`（对照 = NO）。
7. `[E]` `classes = 1`：`ClassData[clazz=Lcom/example/forgeprobe/ForgeProbe;, parent=Ljava/lang/Object;, interfaces=[]]`。
8. `[E]` `modFileInfos = 1`。
9. `[E]` 清理后 15/15 条目与 001b-1 基线一致。

### [H] Hypothesis

1. `[H]` scanner **不关心注解的包名**，只记录类文件里实际存在的 descriptor ——
   本轮只观测到 `net.minecraftforge.*` 一条，未做多注解对照。
2. `[H]` `Scan` 阶段确实对 Forge 原生 JAR 执行了 class scan；但**扫描器的具体实现类与
   扫描时机**本轮未观测。
3. `[H]` 注解 `value` 的实际运行时类型（`String` 还是 `String[]`）未单独确认
   （dump 打印为 `{value=forgeprobe}`；判定逻辑对两种形态均作了兼容）。

### [D] Design Decision

1. `[D]` **001c-1 的 entrypoint 可以从 `scanData.getAnnotations()` 提取** ——
   Forge annotation 及其 metadata 在正常加载链中**已经可用**，无需自研 scanner、
   无需 annotation shim。
2. `[D]` 但这**仅**证明"数据可得"，**不等于** `FMLModContainer` 能消费它
   （v1.1 §3.3 的核心纪律：**发现 ≠ 解释**）。
3. `[D]` 本轮 dump 的存在使得 `Scan` 阶段首次有了直接证据，可从 UNKNOWN 升级为 YES。

---

## 十、结论

**五层判据结果**：

| 层级 | 结果 |
|---|---|
| A `loadMod` 收到 `ModFileScanData` | **PASS** |
| B `scanData` 有 annotation | **PASS**（size = 1） |
| C 含 `net.minecraftforge.fml.common.Mod` | **YES** |
| D target == `com.example.forgeprobe.ForgeProbe` | **YES** |
| E value == `"forgeprobe"` | **YES** |

**对 001c-1 的输入建议**：

- 情形对应制造书 §九 的 **情况 A**（A–E 全部 PASS）
  → entrypoint 从 `scanData.getAnnotations()` 提取；范围由架构决策方裁定。

**结论上限（禁止超出）**：

> `[E]` NeoForge 21.1.256 的正常加载链向 `IModLanguageLoader.loadMod()` 传入的
> `ModFileScanData` 中，**实际包含**该 Forge annotation，其
> `annotationType` / `clazz` / `annotationData` 的真实内容已记录。
>
> `[D]` 仅根据上述观察结果决定 001c-1 的 entrypoint 来源。

**禁止从本 Spike 推导**（本报告均未主张）：`FMLModContainer` 已构造 /
Forge `@Mod` 已被 NeoForge 解释 / ForgeProbe 已实例化 / Forge constructor ABI 已可用 /
Forge Mod 已经能够被 NeoForge 加载。

**Confidence**: **HIGH**

**Evidence**: §五 的完整 dump 原文（`stdout` 可复核）；§六 的 15/15 逐条目一致。

**Hypotheses still unverified**: §九 `[H]` 1–3。

---

## 十一、禁止事项确认

- [x] 未修改 NeoForge 核心
- [x] 未修改 `ModSorter` / `LoadingModList` / `ModInfo` / `ModFile` / `ModFileInfo` / `ModFileScanData`
- [x] 未修改 `FMLModContainer`
- [x] 未修改 `forgeprobe.jar`（SHA-256 一致）/ `mods.toml`
- [x] 未引入 ASM / Mapping / Mixin / Registry / Event / Isolation
      （仅**读取** `org.objectweb.asm.Type` 的返回值，未做任何字节码操作）
- [x] **未自研独立 Scanner**（数据来源仅为 `loadMod` 的第二参数）
- [x] 未在 `loadMod` 中构造 `FMLModContainer`
- [x] 未在 `loadMod` 中实例化 ForgeProbe
- [x] 未实现 annotation shim
- [x] 未使用 Unsafe / 字节码注入 / ClassLoader hack
- [x] 未进入 001c-1 / 001c-2
- [x] 未提出修复方案

---

## 附录：与 Spike_001c-0.md 的偏差（如实记录）

| 项 | 说明书 | 实际 | 说明 |
|---|---|---|---|
| `AnnotationData` 访问器名 | `getClassType()` / `getAnnotationData()` | **`clazz()` / `annotationData()`** | 说明书笔误；已按实际 API 实现并在 §四 标注 |
| 到达 `loadMod` 的前置条件 | §2.2 仅允许"临时恢复 001b-6 同型观测 provider" | **额外需要恢复 001b-6 的 `ForgeModFileInfoParser.build() → 真实 ModFileInfo` 配置** | 首次运行因 parser 已回滚到基线而停在 `ModSorter` 的 `ModInfo::cast`（`loadMod` 未调用）。补上该配置后成功 dump。**该配置是 001b-6 已验证的既有配置，非新变量** |
| `getClasses()` 打印 | 要求打印 `<fqcn>` | 打印 `ClassData` 的 `toString()`（含 `clazz` / `parent` / `interfaces`） | 未逐个取 `ClassData.clazz()`；信息量更大且无过滤 |
