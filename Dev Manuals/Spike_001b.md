# Spike-001b 任务说明书与 v1.1 标签修正

**文档性质**：v1.1 标签修正确认 + Spike-001b 执行任务说明书
**前置文档**：

1. `Forge → NeoForge 兼容层开发手册 v1.1（含 Spike-000 / Spike-001a 回写）`
2. `Spike-000 正式实验结论`
3. `Spike-001a-0 实验报告`

---

## 一、标签修正确认

以下修正已确认正确，并已回写进 v1.1 手册：

| 原表述 | 修正后 |
|---|---|
| `IModLanguageLoader` → ServiceLoader → PLUGIN Layer `[F]`（待 Spike-001b 验证） | `[H]` 待 Spike-001b 验证 |

**理由**：`[F]` 标签的含义是"源码已验证的事实"。`IModLanguageLoader` 通过 ServiceLoader 加载到
PLUGIN Layer 这一路径，基于 FancyModLoader 公共文档大概率成立，但尚未在本项目对应的
`loader-4.0.45.jar` 字节码中逐字确认。在完成 001b-0 之前，它只能是 `[H]`。

**同时修正**：`IModLanguageLoader` 实际进入的是 PLUGIN Layer 还是 SERVICE Layer，
本身就是 001b-0 需要回答的问题之一。Spike-001a-0 已经证明 `IModFileReader` 进入的是
**SERVICE Layer** 而非 PLUGIN Layer，`IModLanguageLoader` 的层级归属不能假设与文档一致。

---

## 二、§14 状态措辞

`Spike-001b：进行中` 改为：

```
Spike-001b：已进入 / 待执行
```

**理由**：`进行中` 暗示已开始动作；`已进入 / 待执行` 更准确地描述"阶段已切换，但尚未产生实验数据"。

---

## 三、Spike-001b 执行任务说明书

```
# Spike-001b 执行任务说明书

接收方：Codex
前置文档：
  1. Forge → NeoForge 兼容层开发手册 v1.1（含 Spike-000 / Spike-001a 回写）
  2. Spike-000 正式实验结论
  3. Spike-001a-0 实验报告

任务性质：源码事实采集 + 最小运行验证
当前阶段：Spike-001a 已冻结，Spike-001b 正式启动

## 〇、执行前置

请先完整读取 v1.1 手册 §3.1（SPI 分类）、§3.6（Service Provider 加载机制）、
§3.7（兼容层 JAR 身份约束）、§11（Spike 路线）、附录 A（事实标签体系）。

本说明书不重复手册内容。

## 一、任务目标（唯一）

回答一个问题：

> NeoForge/FML 4.0.45 是否能够把 Forge 的 `mods.toml` 中
> `modLoader="javafml"` 解析成一个可用的 IModLanguageLoader？
> 以及这个 loader 的**名称、实现类、版本**是如何解析出来的？

禁止目标：
- 让 ForgeProbe 构造函数被调用
- 进入 FMLModContainer / Constructor
- 修改 ForgeProbe ABI
- 修改 FMLJavaModLoadingContext
- 引入 ASM / Mapping / Mixin / Registry / Event / ClassLoader Isolation
- 预设 Plan A 或 Plan B

## 二、子 Spike 拆分

### 001b-0：源码路径定位（不做运行实验）

唯一问题：在 exact `loader-4.0.45.jar` 中，从 `mods.toml` 的 `modLoader` 字段
到 `IModLanguageLoader` 实例的完整调用链是什么？

必须定位（字节码级）：

- `ModFileInfoParser` / `ModFileInfo.getMods()`
- `IModInfo.getLoader()` 的返回类型
- `IModLanguageLoader` 的定义与加载方式
- `IModLanguageLoader` 的实际层级（SERVICE / PLUGIN / 其他）
- `LanguageProviderLoader` 或等价的 language provider registry
- `loaderVersion` 的版本范围校验位置

必须回答：

    mods.toml
        ↓
    modLoader = "javafml"
        ↓
    loader name = ?
        ↓
    loaderVersion = "[52,)"
        ↓
    VersionRange / validation 发生在哪一步？
        ↓
    IModLanguageLoader 实例从哪来？

输出：**源码分析报告**（非实验报告），不修改任何兼容层代码。

### 001b-1：让 getMods() 返回一个非空 IModInfo

唯一问题：`ForgeModFileInfoParser` 能否构造一个最小可用的 `IModInfo`？

最小实现范围：

- 解析 `[[mods]]` 的 `modId="forgeprobe"` 和 `version="1.0.0"`
- 解析 `modLoader="javafml"` 和 `loaderVersion="[52,)"`
- `getMods()` 返回包含 1 个 `IModInfo` 的列表
- 其余字段暂时不扩展

禁止：
- 构造 `IModLanguageLoader`（如果 001b-0 未确认）
- 修改 `ForgeProbe` 的 ABI
- 触碰 FMLModContainer

PASS：`getMods()` 返回非空，`IModInfo` 成功创建。

### 001b-2：验证 IModInfo.getLoader()

唯一问题：`IModInfo.getLoader()` 返回什么？

必须打印：

    IModInfo.modId
    IModInfo.getLoader()
      · loader name
      · loader implementation class
      · loader version

**关键纪律**：

> 看到名为 `javafml` 的对象，**不能**直接断言它来自某个具体实现类。
> 必须区分：loader name / loader implementation class / loader version 三个独立字段。

PASS：`getLoader()` 返回非 null，且三个字段的值均已记录。

### 001b-3：验证 javafml 的解析来源

唯一问题：`javafml` 的 provider / registry / resolution 机制是哪种？

**不预选 Plan A 或 Plan B。** 只记录实际机制：

| 可能机制 | 观测特征 |
|---|---|
| NeoForge 已存在 `javafml`，直接匹配 | `LanguageProviderLoader` 中存在名为 `javafml` 的 provider |
| 兼容层注册了 `javafml`，映射到 `forge_compat` | 兼容层的 provider 被优先选中 |
| 第三种：通过其他 registry / provider 机制 | 需要重新定义 |

PASS：能够确定实际机制。

### 001b-4：验证 loaderVersion 的解析

唯一问题：`loaderVersion="[52,)"` 在 NeoForge/FML 4.0.45 中的解析与校验结果是什么？

必须记录：

    loader name         = ?
    loader version      = ?
    accepted range      = [52,)
    comparison result   = PASS / FAIL / SKIPPED
    validation location = findLanguage / loadMod / 其他

**重点不是要求它通过**，而是记录实际行为。

> 这里很可能暴露 Forge 与 NeoForge 在 `javafml` 版本语义上的第一处真正兼容冲突。

PASS：明确记录四个字段的值和校验位置。

### 001b-5：停止边界

001b **禁止进入**：

- FMLModContainer
- Forge `@Mod` 识别
- constructor
- IEventBus
- FMLJavaModLoadingContext
- registry
- event
- mapping
- ASM
- Mixin
- classloader isolation

**即使最后出现**：

    IModInfo successfully created
    IModInfo.getLoader() successfully resolved

**也必须在此停止。**

边界说明：

    001a = Discovery → IModFile
    001b = IModFile → IModInfo → IModLanguageLoader
    001c = IModLanguageLoader → ModContainer / Entrypoint

## 三、PASS 判据

| 编号 | 判据 |
|---|---|
| PASS-001b-1 | `mods.toml` 被成功解析为非空 `getMods()` |
| PASS-001b-2 | `forgeprobe` 对应的 `IModInfo` 成功创建 |
| PASS-001b-3 | `IModInfo.getLoader()` 返回非 null |
| PASS-001b-4 | 能够确定 loader 名称是否为 `"javafml"` |
| PASS-001b-5 | 能够确定 `loaderVersion="[52,)"` 在当前 NeoForge/FML 4.0.45 中的解析/校验结果 |
| PASS-001b-6 | 能够确定 `javafml` 的 provider / registry / resolution 机制 |

**PASS-001b-1 ~ 001b-3** 是功能进入下一层；
**PASS-001b-4 ~ 001b-6** 是决定 Plan A / Plan B 的关键证据。

## 四、禁止事项

- [ ] 不修改 `forgeprobe.jar`
- [ ] 不添加 `neoforge.mods.toml`
- [ ] 不修改 NeoForge 核心
- [ ] 不引入 ASM / Mapping / Mixin / Registry / Event / Isolation
- [ ] 不修改 `FMLJavaModLoadingContext` ABI
- [ ] 不提出修复方案
- [ ] 不预设 Plan A 或 Plan B

## 五、输出格式

按 v1.1 手册附录 A 的 `[F]/[E]/[H]/[D]` 标签体系输出。

**每个子 Spike（001b-0 到 001b-4）单独出报告。**

报告结构：
- 001b-0：**源码分析报告**（非实验报告），不填 Failure Chain，只记录源码路径
- 001b-1 至 001b-4：**实验报告**，Failure Chain 只填写到当前子 Spike 已进入的阶段

## 六、交接

完成 001b-0 后立即交回，由架构决策方判断是否进入 001b-1。

**不得跳过子 Spike 直接执行下一步。**

## 七、当前状态

    Spike-000   ✅ 完成
    Spike-001a  ✅ 完成
    Spike-001b  ▶ 已进入 / 待执行
    Spike-001c  ⏸ 未开始

## 八、纪律声明

> 001b 不把 `javafml` 的"兼容方案"写进手册，
> 只把"它实际上是怎么被解析的"写进去。
> 等证据出来后，再决定 Plan A、Plan B，或者发现需要第三条路线。
```

---

## 四、v1.1 标签修正汇总

| 位置 | 原标签 | 修正后 |
|---|---|---|
| §3.1 `IModLanguageLoader` 层级 | `[F]` | `[H]` 待 Spike-001b 验证 |
| §3.1 `IModLanguageLoader` 具体层级（SERVICE vs PLUGIN） | 未明确 | `[H]` 由 001b-0 回答 |
| §14 Spike-001b 状态 | 进行中 | 已进入 / 待执行 |

---

## 五、下一步

v1.1 + Spike-001a 回写已冻结。Spike-001b 任务说明书已生成。

下一步行动：将 Spike-001b 任务说明书交付 Codex，从 **001b-0（源码路径定位）** 开始。

001b-0 不产生实验数据，只产生源码分析报告。报告完成后，由架构决策方判断是否进入 001b-1。
