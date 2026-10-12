# Spike-001c-0 执行任务说明书（修正版）

**接收方**：Codex
**前置文档**：《Forge → NeoForge 兼容层开发手册 v1.1》（含 §3.1 / §3.3 / §3.8）+《Spike-001b-6 实验报告》
**任务性质**：**只读数据采集**（不构造、不实例化、不扩展）
**唯一目标**：确定 NeoForge 21.1.256 在真实加载链中交给 `IModLanguageLoader.loadMod(...)` 的
`ModFileScanData` 是否包含 ForgeProbe 的 `net.minecraftforge.fml.common.Mod` 注解，
以及该 annotation 的 metadata 是否正确。

```
Spike-001b-6   ✅ 完成（两道 cast 均 PASS；loadMod 被真实调用）
Spike-001c-0   ▶ 本说明书
Spike-001c-1   ⏸ 待 001c-0 后决定
Spike-001c-2   ⏸ 待 001c-1 后决定
```

---

## 〇、执行前置

完整读取 v1.1 手册 §3.1 / §3.3 / §3.8 / §11 / 附录 A，以及 Spike-001b-6 实验报告。
本轮**不**重新验证：两道 cast、`[52,)` vs `4.0.45`、同名 provider 覆盖、`ModFileInfo` 构造路径。

---

## 一、唯一目标（五层判据）

| 层级 | 要证明什么 | 记录字段 |
|---|---|---|
| **A** | `loadMod()` 收到了 `ModFileScanData`（非 null） | `scanData != null` |
| **B** | `scanData` 中存在 annotation 数据 | `scanData.getAnnotations().size() > 0` |
| **C** | annotation 名称是否为 Forge `@Mod` | `annotationData.annotationType().getClassName()` |
| **D** | annotation target 是否指向 ForgeProbe | `annotationData.clazz().getClassName()` |
| **E** | annotation 的 value 是否为 `"forgeprobe"` | `annotationData.annotationData().get("value")` |

> **每一层独立记录。A 未通过则 B 无意义；C 未通过则 D、E 无意义。**

**对照信息（仅记录）**：`net.neoforged.fml.common.Mod` 是否也出现在 `scanData` 中。

**D 层目标类名必须以实际 FQCN 为准**：必须从当前实验用的 `forgeprobe.jar` 中读取
（`jar tf forgeprobe.jar`），**不得凭项目名或历史记忆假设**。报告 §三 须显式记录来源。

---

## 二、实验边界

### 2.1 只做只读 dump

不解决：`FMLModContainer` 构造、Forge `@Mod` 实例化、Forge constructor ABI、
Forge `IEventBus` / `FMLJavaModLoadingContext`、Registry / Event / Mapping / Mixin / Isolation、
Module / Container / Constructor 阶段的任何**行为**。

### 2.2 允许变更

- 在 `ForgeCompatLanguageLoader.loadMod(...)` 中**只读遍历** `ModFileScanData`，按 §四 格式打印
- 临时恢复 001b-6 同型观测 provider（`name = "javafml"`，`version = "9999"`）
- 通过兼容层代码读取 `ModFileScanData` / `AnnotationData` 的公开 API
- 用 `javap` / Javadoc 确认这两个类的公开 API 形状
- 用 `jar tf forgeprobe.jar` 确认 ForgeProbe 的实际 FQCN

### 2.3 为什么"不重启独立 Scanner"

本 Spike 要回答的是**架构问题**：

> NeoForge 当前正常加载链产生的 `ModFileScanData` 是否包含 Forge `@Mod`？

**不是**："我们自己写一个扫描器能不能找到 Forge `@Mod`？"
后者会引入新变量（自研扫描器 vs NeoForge 内置 Scanner），污染实验结论。

> **唯一合法数据来源**：`loadMod(IModInfo info, ModFileScanData scanData, ModuleLayer layer)`
> 的**第二个参数**。

### 2.4 禁止事项

不修改 NeoForge 核心 / `ModSorter` / `LoadingModList` / `ModInfo` / `ModFile` / `ModFileInfo` /
`ModFileScanData` / `LanguageProviderLoader` / `VersionSupportMatrix` / `FMLModContainer`；
不修改 `forgeprobe.jar` / `mods.toml`；不引入 ASM / Mapping / Mixin / Registry / Event / Isolation；
不改变版本；**不自己重启独立 Scanner**；**不自己解析 `forgeprobe.jar` 字节码查找 `@Mod`**；
不在 `loadMod` 中构造 `FMLModContainer`；不在 `loadMod` 中实例化 ForgeProbe；
不实现任何 annotation shim；不使用 Unsafe / 字节码注入 / ClassLoader hack。

---

## 三、精确环境（冻结）

```
Minecraft 1.21.1 / NeoForge 21.1.256 / FancyModLoader 4.0.45
Java 21（OpenJDK 21.0.10，Microsoft Build）/ Windows 11 amd64
启动方式 CLI / Dedicated Server        编译依赖 Forge 1.21.1-52.1.0
```

---

## 四、子步骤

### 001c-0-a：API 取证（不做运行）

确认 `ModFileScanData`（`getAnnotations()` / `getClasses()` / `getIModInfoData()`）
与 `AnnotationData`（annotation 类型 / target 类 / 注解值 的访问器）的公开 API 形状。

> **已知偏差**：本说明书原文写的 `getClassType()` / `getAnnotationData()` **不存在**；
> 实际访问器为 **`clazz()`** / **`annotationData()`**。执行时以**实际 API** 为准并记录。

**PASS-001c-0-a 判据**：上述项均已从精确版本字节码或 Javadoc 获取，无 UNKNOWN。
若发现 `@ApiStatus.Internal` / `@NonExtendable`，记录其约束但不影响本 Spike（只读，不构造）。

### 001c-0-b：loadMod 中 dump

在 `ForgeCompatLanguageLoader.loadMod(...)` 中只读遍历并打印
（固定前缀 `[FORGECOMPAT][SCAN-DUMP] `）：A–E 五层判据 + 对照 + 全量 annotations 明细
+ classes 明细 + modFileInfos 数量。

**关键纪律**：

- **不要过滤，全量打印**
- **不要猜测，只打印 API 返回的实际数据**
- 不要构造 `FMLModContainer`；不要实例化 ForgeProbe
- 不要因为 `@Mod` 找不到就现场实现 annotation shim
- 不要因为 `@Mod` 找到了就直接进入 `FMLModContainer`
- **打印后立即抛出 `UnsupportedOperationException("Observation only")`**，让流程停在 dump 之后

**PASS-001c-0-b 判据**：dump 已执行；五层判据均已记录，无 UNKNOWN。

> **五层判据中任何一层为 NO 都是合法实验结果，不视为 FAIL。**
> 本 Spike 的 PASS 只要求"dump 已执行 + 五层判据均已记录"。

> **执行提示（实测补充）**：要到达 `loadMod`，除恢复观测 provider 外，
> **还需恢复 001b-6 的 `ForgeModFileInfoParser.build() → 真实 ModFileInfo` 配置**
> （该配置在 001b-6-3 已被回滚到基线）。否则链会在 `ModSorter` 的 `ModInfo::cast` 处终止，
> `loadMod` 不会被调用。该配置属 001b-6 已验证的既有配置，**非新变量**。

### 001c-0-c：清理

全部撤销 `ForgeCompatLanguageLoader`（源文件 + `.class` + service 声明）与任何临时 dump 代码；
清理后与 **001b-1 基线**内容一致（15/15 条目）。
**清理标准不是 SHA-256 恢复，而是内容一致。**

---

## 五、PASS 判据

| 编号 | 判据 |
|---|---|
| PASS-001c-0-a | `ModFileScanData` / `AnnotationData` 公开 API 已确认 |
| PASS-001c-0-b | dump 已执行；五层判据 A / B / C / D / E 均已记录 |
| PASS-001c-0-c | 清理后与 001b-1 基线内容一致 |

> 五层判据中任一层为 NO **不影响** PASS-001c-0-b。

---

## 六、STOP 条件

| 情况 | 处理 |
|---|---|
| API 无法从精确版本获取 | STOP |
| `loadMod` 未被调用 | STOP（说明 001b-6 结论不稳定） |
| dump 时抛异常（非我方 UOE） | 记录异常，STOP |
| `annotations` 为空 | **不 STOP** —— 记录为 `[E]`，交回 |
| `classes` 中无 ForgeProbe | **不 STOP** —— 记录为 `[E]`，交回 |
| `@Mod` 未找到 | **不 STOP** —— 记录完整 scanData，交回 |
| 必须修改 NeoForge 核心才能 dump | STOP |
| 自研扫描器替换 scanData 才能取得数据 | STOP（违反 §2.3） |

任何 STOP 后：不得进入 001c-1 / 001c-2；不得自行设计修复方案；不得自行实现 annotation shim。

---

## 七、Failure Chain（若触达）

```
Discovery YES / Metadata YES / Validation YES
Scan:            YES / NO / UNKNOWN（本轮首次直接观测）
LanguageLoader:  YES
ModSorter:       PASS
LoadingModList:  PASS
Module:          UNKNOWN
Container:       UNKNOWN（loadMod 已进入，但 FMLModContainer 是否构造未知）
Constructor:     NO
```

> `Scan` 只有在取得**直接证据**时才能标 YES；本 Spike 的 dump 是直接证据来源之一。
> `Container` **不得**因为 `loadMod` 已被进入就标 YES —— **`loadMod` 入口 ≠ `FMLModContainer` 已构造**。

---

## 八、结构化输出格式（强制）

报告须按**十一节**结构输出：实验环境 / 实验目标 / **ForgeProbe 实际 FQCN（含 `jar tf` 原文）** /
API 取证 / loadMod dump（五层判据 + 对照 + 完整 dump 原文）/ 清理确认 / Failure Chain /
完整日志 / 事实标签 / 结论（含"对 001c-1 的输入建议"与**结论上限**）/ 禁止事项确认。

---

## 九、交接规则

执行完成后**报告原样交回**，不得自行决定下一 Spike。按五层判据结果：

| 情况 | 条件 | 处置 |
|---|---|---|
| **A** | A–E 全部 PASS | scanData 含 Forge `@Mod` 且 metadata 正确 → 001c-1 的 entrypoint 从 `scanData.getAnnotations()` 提取；范围由架构决策方裁定 |
| **B** | B = FAIL（annotations 为空） | scanner 未对 Forge JAR 扫描 → 记录 `[E]` first confirmed failure → 交回 |
| **C** | B = PASS 但 C = NO | scanner 未识别 Forge 注解 → 记录完整 dump → 交回 |
| **D** | C = YES 但 D 或 E = NO | 记录 metadata 实际值 → 交回 |
| **E** | dump 时抛异常 | 记录异常 → 按 §六 STOP |

> 无论 A–E 哪一种，都必须**先停下来交报告**，不得自行进入 001c-1。

---

## 十、本 Spike 的真正使命

001c-0 不是为了构造 `FMLModContainer`、不是为了实例化 `@Mod` 类、也不是为了实现 annotation shim。
它只回答：**NeoForge 21.1.256 的 `ModFileScanData` 是否包含 ForgeProbe 的
`net.minecraftforge.fml.common.Mod` 注解，以及该 annotation 的 metadata 是否正确。**

```
[F] ModFileScanData / AnnotationData 公开 API
        ↓
[E] loadMod 中 dump 的实际内容
        ↓
[E] 五层判据 A / B / C / D / E 的实际结果
        ↓
[D] 001c-1 的 entrypoint 来源
```

> **不要**将"scanData 中出现了 ForgeProbe 这个类名"写成"`FMLModContainer` 能识别
> Forge `@Mod` 作为 entrypoint"——前者的语义只是"scanner 扫描到了这个类"；
> 后者是"language loader 把该类的 `@Mod` 注解解释为 entrypoint"。两者不是同一件事
> （v1.1 §3.3 的核心纪律）。
>
> **不要**将"`@Mod` 找到了"写成"可以进入 `FMLModContainer` 构造"。
>
> **001c-0 完成后必须停在这里。**
