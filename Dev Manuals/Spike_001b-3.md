# Spike-001b-3 执行任务说明书（Layer 概念修正版）

**接收方**：Codex
**前置文档**：《Forge → NeoForge 兼容层开发手册 v1.1》+《Spike-001b-0 源码分析报告》+《Spike-001b-1 实验报告》
**任务性质**：源码 + 运行时观测
**唯一目标**：确定 `IModLanguageLoader` 的 provider 实际 ModLauncher Layer 与同名覆盖行为

---

## 〇、001b-1 结论与状态冻结

**001b-1：正式 PASS，HIGH confidence。**

四节点全开，`findLanguage` 进入分支②（VersionSupportMatrix），`[52,)` vs `4.0.45` FAIL，
issue key `fml.modloadingissue.language.missingversion`。

**001b-4：标记为「已被 001b-1 覆盖」，不再单独执行。** 理由：001b-1 已直接证明
`javafml provider found → version = 4.0.45 → accepted range = [52,) → VersionSupportMatrix → FAIL → missingversion`，
再单独做一次「`[52,)` vs `4.0.45` 是否 FAIL」不会增加新事实。

**当前 Spike 状态**

```
Spike-000      ✅ 完成
Spike-001a     ✅ 完成
Spike-001b-0   ✅ 完成
Spike-001b-1   ✅ 完成
Spike-001b-3   ▶ 本说明书
Spike-001b-4   ⏭ 已被 001b-1 覆盖
Spike-001b-2   ⏸ 待 001b-3 后决定
Spike-001c     ⏸ 未开始
```

---

## 一、唯一目标（两个问题）

**问题 A**：`IModLanguageLoader` 的 provider 实际来自哪个 **ModLauncher Layer**？

> 这里指的是 ModLauncher Layer，即 `cpw.mods.modlauncher.api.IModuleLayerManager.Layer`
> 的枚举值（`BOOT` / `SERVICE` / `PLUGIN` / `GAME`），**不是** `java.lang.ModuleLayer`。
>
> 001b-0 §3.4 从时序推理得出「不是 mods/ 目录本次运行新建的 PLUGIN 层内容」，
> 但**具体 ModLauncher Layer 名从未由运行时直接观测**。

**问题 B**：同名 provider 的实际覆盖行为是什么？

> 001b-0 §3.5 确认 `LanguageProviderLoader` 使用 `Map.put`，同名静默互相覆盖。
> **实际枚举顺序未观测。**

---

## 二、概念澄清（关键，执行前必读）

**Java ModuleLayer ≠ ModLauncher Layer，两者不能混为一谈：**

| 概念 | 类型 | 用途 |
|---|---|---|
| Java ModuleLayer | `java.lang.ModuleLayer` | JPMS 运行时结构；`Module.getLayer()` 返回它；某些模块可能处于 unnamed module，`getLayer()` 可能返回 null |
| ModLauncher Layer | `cpw.mods.modlauncher.api.IModuleLayerManager.Layer` | ModLauncher 的枚举：`BOOT(0)` / `SERVICE(1)` / `PLUGIN(2)` / `GAME(3)` |

**禁止**：通过 `provider.getClass().getModule().getLayer()` 直接声称"provider 来自 SERVICE Layer"。
即使得到 `module = MC-BOOTSTRAP/fml_loader`、`java.lang.ModuleLayer = ...`，
也**不能**直接写成 `provider Layer = SERVICE`。

---

## 三、实验边界

### 3.1 只做观测，不做修复

- 不解决版本冲突（`[52,)` vs `4.0.45` 的 FAIL 不在本 Spike 范围）
- 不实现 Plan A 或 Plan B
- 不修改 `forgeprobe.jar`
- 不修改 `mods.toml`
- 不修改 NeoForge 核心

### 3.2 唯一允许的变更

**允许**：

- 新增一个 `IModLanguageLoader` 实现（`name()` = `"javafml"`，`version()` = `"9999"`），用于问题 B
- 在 `META-INF/services/` 中新增该 provider 的声明
- 在兼容层中添加日志打印以观测实例化顺序
- 通过反射读取 LaunchContext 的 ModLauncher Layer 状态（**只读，不修改**）

**禁止**：

- 不修改 `FMLJavaModLanguageProvider` 或 NeoForge 内置任何类
- 不修改 `LanguageProviderLoader`
- 不修改 `VersionSupportMatrix`
- 不修改 `LaunchContext`
- 不引入 ASM / Mapping / Mixin / Registry / Event / Isolation
- 不触碰 `FMLModContainer` / Constructor
- 不触碰 ForgeProbe 构造函数

### 3.3 强约束

> **新增的 `IModLanguageLoader` 只用于观测，不用于「让 forgeprobe 启动成功」。**
> **本 Spike 结束时应撤销该新增 provider。**

---

## 四、子步骤

### 001b-3-a：观测 provider 实际 ModLauncher Layer

**问题**：`LanguageProviderLoader` 调用 `ctx.loadServices(IModLanguageLoader.class)` 时，
`LaunchContext` 最终从哪个 ModLauncher Layer 搜索到了 provider？

**方法优先级**：

**方法 A（首选）：源码 + 运行时路径交叉验证**

基于 001b-0 已确认的 `LaunchContext.loadServices` 字节码：

```
LaunchContext.loadServices(Class<T>)
        ↓
reversed IModuleLayerManager.Layer.values()
        ↓
highest existing ModLauncher Layer
        ↓
该 Layer + parent layers
        ↓
ServiceLoader.load(ModuleLayer, Class)
```

需要同时确定：

1. `LaunchContext` 实例中哪些 ModLauncher Layer 实际存在（读取 `IModuleLayerManager` 状态）
2. `IModLanguageLoader` provider 的来源 JAR（`FMLJavaModLanguageProvider` 的 `CodeSource`）
3. 该 JAR 被哪个 ModLauncher Layer 的路径持有（交叉比对）

**证据链形态**：

```
FMLJavaModLanguageProvider
        ↓ 读取 CodeSource
来源 JAR = loader-4.0.45.jar
        ↓ 与 LaunchContext 的 Layer paths 交叉比对
该 JAR 位于 ModLauncher Layer X 的 paths 中
        ↓
X = SERVICE / BOOT / PLUGIN / GAME
```

> 只有形成此证据链，才能写：`[E] FMLJavaModLanguageProvider 来源于 ModLauncher Layer X`

**方法 B（备选）：`ServiceLoader.load(ModuleLayer, Class)` 路径记录**

如果方法 A 因反射限制无法实施，可在兼容层添加一个 `IModLanguageLoader` 包装器，
通过 `ServiceLoader` 枚举时记录每个 provider 的 `CodeSource` + `ModuleLayer.getConfiguration()`，
再与 `IModuleLayerManager.Layer` 的状态做交叉比对。

**必须记录**

```
IModLanguageLoader providers:
- FMLJavaModLanguageProvider:
  - CodeSource:
  - ModLauncher Layer: BOOT / SERVICE / PLUGIN / GAME / UNKNOWN
  - 证据链:
- LowCodeModLanguageProvider:
  - CodeSource:
  - ModLauncher Layer:
- MinecraftModLanguageProvider:
  - CodeSource:
  - ModLauncher Layer:
```

**PASS 条件**：至少对 `FMLJavaModLanguageProvider` 明确记录其 ModLauncher Layer
（或明确记录 `UNKNOWN` 及原因）。

**STOP 条件**：

- 无法读取 `IModuleLayerManager` 的 Layer 状态
- `CodeSource` 与 Layer paths 无法交叉比对
- 记录 `UNKNOWN` 及原因，报告停止

### 001b-3-b：观测同名覆盖行为

**问题**：如果兼容层注册一个 `name()` = `"javafml"` 的 provider，会发生什么？

**方法**：

1. 在兼容层中新增（示意，实际实现须含 `[FORGECOMPAT][LANG]` 日志）：

```java
public class ForgeCompatLanguageLoader implements IModLanguageLoader {
    static { System.out.println("[FORGECOMPAT][LANG] <clinit>"); }
    public ForgeCompatLanguageLoader() { /* INSTANTIATED #n */ }
    @Override public String name() { return "javafml"; }
    @Override public String version() { return "9999"; }
    @Override public ModContainer loadMod(IModInfo info, ModFileScanData data, ModuleLayer layer) {
        throw new UnsupportedOperationException("Observation only");
    }
}
```

2. 在 `META-INF/services/net.neoforged.neoforgespi.language.IModLanguageLoader` 中声明它
3. 启动
4. 观察原始顺序：

```
[FORGECOMPAT][LANG] <clinit>
[FORGECOMPAT][LANG] INSTANTIATED #1
[FORGECOMPAT][LANG] name() -> javafml
[FORGECOMPAT][LANG] version() -> 9999

[main/DEBUG] [ServiceLoaderUtil/CORE]: Found 4 implementations of IModLanguageLoader:
[main/DEBUG] [LanguageProviderLoader/CORE]: Found language provider javafml, version X
[main/DEBUG] [LanguageProviderLoader/CORE]: Found language provider lowcodefml, version X
[main/DEBUG] [LanguageProviderLoader/CORE]: Found language provider minecraft, version X
```

> **不要自行推断内置 provider 的实例化顺序，除非日志/源码明确给出。**

**情况判定**

| 情况 | 日志特征 | 结论 |
|---|---|---|
| **A** | `javafml, version 4.0.45` 在前，`javafml, version 9999` 在后 | 兼容 provider **覆盖内置** → Plan A 有实现基础（但仍不能写「Plan A 已可行」） |
| **B** | `javafml, version 9999` 在前，`javafml, version 4.0.45` 在后 | 兼容 provider **被内置覆盖** → Plan A 的重大不利证据 |
| **C** | `[FORGECOMPAT][LANG] <clinit>` 与 `INSTANTIATED` 零命中，但日志仍有 javafml → 4.0.45 | 需进一步区分：provider 不在搜索 Layer / ServiceLoader 可见性问题。**不能直接判定「Plan A 不可能」** |

**必须记录**

```
ForgeCompatLanguageLoader 实例化: YES / NO / UNKNOWN
LanguageProviderLoader 日志顺序（原文）:
languageProviderMap 中 name="javafml" 的最终来源:
- class:
- version:
- 情况判定: A / B / C / UNKNOWN
枚举顺序（若可观测）:
- FMLJavaModLanguageProvider 在前 / 在后 / UNKNOWN
```

**PASS 条件**：明确判定情况 A / B / C 之一。

**STOP 条件**：

- `ServiceLoader` 枚举顺序不稳定（多跑几次记录）
- 新增 provider 导致启动失败（记录失败形态）
- 无法观测最终 map 中的 `javafml` 来源

### 001b-3-c：撤销观测 provider

**不产生实验数据。** 仅清理：

| 项 | 动作 |
|---|---|
| `ForgeCompatLanguageLoader.class` | 删除 |
| `META-INF/services/...IModLanguageLoader` | 删除该文件（若该文件只为本次实验新增） |
| 源码 `ForgeCompatLanguageLoader.java` | 删除/恢复 |
| `META-INF/services/...IModFileReader` | 与 001b-1 一致 |
| `META-INF/services/...IModFileCandidateLocator` | 与 001b-1 一致 |
| `MANIFEST.MF` | 与 001b-1 一致 |

**清理标准不是 SHA-256 恢复，而是内容一致：**

- 如果 SHA-256 恰好恢复 → 记录
- 如果没恢复 → **不判定为实验污染**（编译环境 / ZIP 时间戳 / entry metadata 变化）

---

## 五、结构化输出格式

```
# Spike-001b-3 实验报告

## 一、实验环境
<同 001b-1>

## 二、实验目标
<复述问题 A 和 B>

## 三、实验输入
forgecompat.jar（001b-3 版本）:
- SHA-256:
- JAR tree:
- 与 001b-1 版本的 diff:
- 新增源文件:

META-INF/services/ 内容（含新增声明）:

forgeprobe.jar:
- SHA-256:（应与 001b-1 一致）

## 四、问题 A：provider 实际 ModLauncher Layer

观测方法:
- [ ] 方法 A：源码 + 运行时路径交叉验证
- [ ] 方法 B：ServiceLoader 路径记录
- [ ] 其它（描述）

IModLanguageLoader providers:
- FMLJavaModLanguageProvider:
  - CodeSource:
  - ModLauncher Layer:
  - 证据链:
- LowCodeModLanguageProvider:
  - CodeSource:
  - ModLauncher Layer:
- MinecraftModLanguageProvider:
  - CodeSource:
  - ModLauncher Layer:

Java ModuleLayer 观测（若有，仅作辅助，不作 Layer 判定依据）:
- FMLJavaModLanguageProvider.getModule().getLayer():
- 说明: 该值不等于 ModLauncher Layer

PASS / FAIL / UNKNOWN

## 五、问题 B：同名覆盖行为

ForgeCompatLanguageLoader:
- <clinit>: 触发 / 未触发
- INSTANTIATED: YES / NO
- name() 调用: YES / NO
- version() 调用: YES / NO

LanguageProviderLoader 日志原文:
<按顺序，不截断>

languageProviderMap 中 name="javafml" 的最终来源:
- class:
- version:
- 情况判定: A / B / C / UNKNOWN

枚举顺序（若可观测）:

PASS / FAIL / UNKNOWN

## 六、撤销确认
| 项 | 001b-1 值 | 001b-3 清理后值 | 一致? |
|---|---|---|---|
| ForgeCompatLanguageLoader.class | 不存在 | | |
| META-INF/services/...IModLanguageLoader | 不存在 | | |
| META-INF/services/...IModFileReader | <hash> | | |
| META-INF/services/...IModFileCandidateLocator | <hash> | | |
| MANIFEST.MF | <hash> | | |

SHA-256 是否恢复 001b-1: YES / NO / 部分
若未恢复，差异原因:

## 七、Failure Chain（更新）
Discovery:       YES
Metadata:        YES
Validation:      YES
Scan:            UNKNOWN
LanguageLoader:  YES（provider lookup 分支已进入）
Module:          UNKNOWN
Container:       UNKNOWN
Constructor:     UNKNOWN

## 八、完整日志
<路径，不得摘要>

## 九、事实标签
[F] Source Fact:
[E] Experimental Fact:
[H] Hypothesis:
[D] Design Decision:

## 十、结论
问题 A（provider 实际 ModLauncher Layer）:
问题 B（同名覆盖行为）:

Confidence: HIGH / MEDIUM / LOW
Evidence:
Hypotheses still unverified:

## 十一、禁止事项确认
- [ ] 未修改 FMLJavaModLanguageProvider
- [ ] 未修改 LanguageProviderLoader
- [ ] 未修改 VersionSupportMatrix
- [ ] 未修改 LaunchContext
- [ ] 未解决版本冲突
- [ ] 未实现 Plan A 或 Plan B
- [ ] 未修改 forgeprobe.jar
- [ ] 未修改 mods.toml
- [ ] 未通过 Java ModuleLayer 直接声称 ModLauncher Layer
- [ ] 观测 provider 已撤销
- [ ] 未在报告中提出修复方案
```

---

## 六、PASS 判据

| 编号 | 判据 |
|---|---|
| PASS-001b-3-a | `FMLJavaModLanguageProvider` 的实际 ModLauncher Layer 已明确记录（或明确 UNKNOWN 及原因） |
| PASS-001b-3-b | 同名 `javafml` provider 的实际覆盖行为已明确判定（A / B / C 之一） |
| PASS-001b-3-c | 观测 provider 已撤销；清理后内容与 001b-1 一致 |

三项全部 PASS 即 001b-3 PASS。

---

## 七、STOP 条件

| 情形 | 处理 |
|---|---|
| 无法读取 `LaunchContext` / `IModuleLayerManager` 的 Layer 状态 | 记录 UNKNOWN，转方法 B；若方法 B 也不可行，报告停止 |
| `ServiceLoader` 枚举顺序不稳定 | 多次运行记录，报告停止 |
| 新增 provider 导致启动失败 | 记录失败形态，报告停止 |
| 无法在不修改 NeoForge 核心的情况下观测 | 报告停止，由架构决策方判断是否引入外部注入 |

**任何 STOP 后不得自动进入下一 Spike，报告原样交回。**

---

## 八、交接规则

```
完成报告后，不得进入 001b-2 / 001c。将报告原样交回。

架构决策方根据问题 A + 问题 B 的结果判断：

- 若 provider 实际 ModLauncher Layer 明确 = SERVICE 或 BOOT
  → 更新 v1.1 §3.1 的 [H] 为 [E]
- 若 provider Layer 无法确定
  → 保持 [H]
- 若同名覆盖行为为情况 A（兼容 provider 覆盖内置）
  → Plan A 有实现基础；仍不能写「Plan A 已可行」，进入 Plan A 设计
- 若情况 B（兼容 provider 被内置覆盖）
  → Plan A 重大不利证据；进入 Plan B 设计
- 若情况 C（provider 未被 ServiceLoader 加载）
  → 进一步区分 provider 可见性问题；不直接判定 Plan A 不可能
```

---

## 九、v1.1 手册回写项（待 001b-3 完成后执行）

> **本次不立即回写。** 回写项列于此，待 001b-3 完成后统一执行。

**§3.1 追加 [F]**

```
[F] findLanguage 的两条错误分支文案长度不同：
    分支①（provider 未找到）      : "Missing language {} version {} wanted by {}"
    分支②（VersionSupportMatrix）  : "Missing language {} version {} wanted by {}, found {}"
    可用作运行时区分两分支的日志判据。

[F] findLanguage 的调用发生在 FMLLoader.completeScan → stage2Validation
    → validateLanguages → identifyLanguage，而非 beginScanning 阶段。
    stage1 里 "Loading mod file ... with languages [...]" 只读取
    requiredLanguageLoaders() 用于日志，不触发 findLanguage。

[F] 分支② 抛出的 ModLoadingException 不带 cause；validateLanguages 只取
    issue，丢弃异常对象。findLanguage 的原始栈不可恢复。
```

**§9 追加 [D]**

```
[D] 诊断系统设计约束：
    - missingversion 是 ERROR 级 issue，会导致启动中止（非「跳过」）
    - findLanguage 类失败点的原始异常栈被 NeoForge 丢弃；
      结构化诊断只能依赖日志文案分支判定，不能依赖 stack trace
```

**§13 追加 [E]**

```
[E] 加入非空 getMods() 后，ForgeProbe 首次出现在发现阶段的 Mod List：
    "Forge Probe 1.0.0 (forgeprobe)"
    ModValidator/SCAN: Found 4 mod files with 3 mods（对比 001a-0 为 2 mods）
```

**§14.4 失败链更新**

| 阶段 | 001a-0 | 001b-1 |
|---|---|---|
| Discovery | 进入 | 进入 |
| Metadata | 未进入 | 进入 |
| Validation | 未进入 | 进入 |
| Scan | 未进入 | UNKNOWN |
| LanguageLoader | 未进入 | 进入（fail） |
| Module | 未进入 | 未进入 |
| Container | 未进入 | 未进入 |
| Constructor | 未进入 | 未进入 |

**§3.1 Layer 标签**

保持 `[H]` 不变，**即使 001b-3-a 通过 Java ModuleLayer 观测到了 provider 的 Layer 对象**。
不能因 Java ModuleLayer 观测而提前改成 `[E]`。

---

## 十、优先级

若输出受限，按以下优先级保留：

1. **问题 B 的观测结果（同名覆盖行为）** — Plan A/B 决策的关键输入
2. **问题 A 的观测结果（provider 实际 ModLauncher Layer）**
3. **撤销确认**

---

## 十一、本 Spike 的真正使命

回答一个问题：

> **在当前 NeoForge 21.1.256 / FML 4.0.45 环境下，同名 provider 能否占据 `javafml` 名称？**

**本 Spike 不能得出「Plan A 已可行」的结论。** 它最多回答：

- `javafml` 名称在当前环境下能否被兼容层占据
- 若能被占据，占据的具体机制是什么

真正的 Forge constructor ABI 仍然属于 001c / 002。

---

> Spike-001b-3 是 Plan A / Plan B 决策前的**最后一块拼图**。
> 它的价值不在于解决版本冲突，而在于让「同名 provider 能不能注册」这个关键问题显形。
>
> **完成报告后不得进入下一 Spike。将报告原样交回，由架构决策方判断。**
