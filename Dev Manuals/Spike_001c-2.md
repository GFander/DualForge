# Spike-001c-2 执行任务说明书

**接收方**：Codex
**前置文档**：《Forge → NeoForge 兼容层开发手册 v1.1》+《Spike-001b-6 实验报告》+
《Spike-001c-0 实验报告》+《Spike-001c-1 实验报告》
**任务性质**：最小运行时观测（进入 `constructMod()`，五层失败点分类）
**唯一目标**：让 `FMLModContainer.constructMod()` **被真实调用**，记录 ForgeProbe 实例化的结果，
并按**五层结构**定位第一个真实失败点。

```
Spike-001c-1   ✅ 完成（FMLModContainer 构造 PASS）
Spike-001c-2   ▶ 本说明书
Spike-002      ⏸ 待 001c-2 后决定
```

---

## 〇、前置配置

与 001c-1 相同的三项（均来自 001b-6 已验证路径）：观测 provider（`javafml` / `9999`）、
`ForgeModFileInfoParser.build()` 返回真实 `ModFileInfo`、service 声明。
若未恢复第 2 项，允许出现 `ModSorter → ModInfo::cast` CCE；属前置配置错误，不计为实验结果。

---

## 一、唯一目标

只回答：**`FMLModContainer.constructMod()` 被真实调用后，ForgeProbe 能否被成功实例化？**

**关键设计决定**：

- `constructMod()` 是 `protected`；兼容层**不得反射调用**
- 正确做法：`loadMod()` **正常返回** `FMLModContainer` 实例给 NeoForge，**由 NeoForge 生命周期调用**

```
loadMod() 返回 FMLModContainer
    ↓ NeoForge 生命周期调用 constructMod()
    ├── Class.forName(entrypoint)   ← 层 a
    ├── @Mod 注解解释                ← 层 b（可能不在此处）
    ├── 构造函数签名解析              ← 层 c
    ├── 参数注入                     ← 层 d
    └── 构造函数实际调用              ← 层 e
    ↓ ForgeProbe 构造完成 / 失败
```

### 五层失败点分类（禁止混称"ABI 断层"）

| 层 | 阶段 | 典型异常 |
|---|---|---|
| **a** | `Class.forName(entrypoint)` 类加载 | `ClassNotFoundException` / `NoClassDefFoundError` |
| **b** | `@Mod` 注解语义解释 | `ModLoadingException` / 各种 issue key |
| **c** | 构造函数签名解析 | `NoSuchMethodError` / `ClassNotFoundException`（Forge 参数类型不存在） |
| **d** | 构造函数参数注入 | 参数类型不匹配 / 白名单拒绝 |
| **e** | 构造函数体内 | 用户代码的任何异常 |

---

## 二、实验边界

**不解决**：Forge `IEventBus` / `FMLJavaModLoadingContext` ABI 修复（属 002）、
Registry / Event / Mapping / Mixin / Isolation、Module / Game Layer identity。

**允许**：恢复 §〇 三项；`loadMod()` 正常返回 `new FMLModContainer(...)`；增加不改变控制流的日志；
用 `javap` / Javadoc 确认 `constructMod()` 行为。

**禁止**：修改 NeoForge 核心 / `FMLModContainer` / `ModContainer` / `ModSorter` / `LoadingModList` /
`ModInfo` / `ModFile` / `ModFileInfo` / `ModFileScanData`；修改 `forgeprobe.jar` / `mods.toml`；
引入 ASM / Mapping / Mixin / Registry / Event / Isolation；改变版本；**反射调用 `constructMod()`**；
反射访问 `FMLModContainer` 私有字段；在 `loadMod` 内手动调用 `constructMod()`；
修改 `FMLModContainer` 构造参数或内部行为以让它通过；注入 Forge `IEventBus` /
`FMLJavaModLoadingContext`；修改 ForgeProbe ABI；实现任何 Shim；使用 Unsafe / 字节码注入 / ClassLoader hack。

> **为什么"不反射调用 `constructMod()`"**：反射会绕过访问控制，把
> "**NeoForge 自己会不会调用**"这个变量排除掉；本 Spike 要观察的正是这一点。

---

## 三、精确环境（冻结）

```
Minecraft 1.21.1 / NeoForge 21.1.256 / FancyModLoader 4.0.45
Java 21（OpenJDK 21.0.10，Microsoft Build）/ Windows 11 amd64 / CLI Dedicated Server
编译依赖 Forge 1.21.1-52.1.0
```

---

## 四、子步骤

### 001c-2-a：`constructMod()` 内部行为取证

必须记录：可见性、内部关键调用（按顺序）、是否 `Class.forName`/`loadClass`、是否读取 `scanResults`、
是否反射寻找构造函数、构造参数白名单、是否使用 `eventBus`、entrypoint 构造函数调用入口。

**PASS 判据**：关键行为已从精确版本字节码获取。

### 001c-2-b：`loadMod` 正常返回 `FMLModContainer`

```java
List<String> entrypoints = extractForgeEntrypoints(scanData);
System.out.println("[FORGECOMPAT][001c-2] entrypoints = " + entrypoints);
FMLModContainer container = new FMLModContainer(info, entrypoints, scanData, layer);
System.out.println("[FORGECOMPAT][001c-2] FMLModContainer constructed, returning to NeoForge: " + container);
return container;   // 不抛观测桩
```

**PASS 判据**：`loadMod` 返回非 null `ModContainer`。

### 001c-2-c：观测 `constructMod()` 结果（五层分类）

观察点：`constructMod()` 是否被调用（间接证据 = `FORGE PROBE CONSTRUCTOR REACHED` 是否命中）；
失败点按 a–e 分类，记录异常类型 / 消息 / 完整 stack trace / 调用链。

**PASS 判据**：`FORGE PROBE CONSTRUCTOR REACHED` 命中 ⇒ **整体 PASS**。
**VALID 判据**：未命中但失败层已归类到 a–e。
**FAIL 判据**：未命中且失败层无法归类。

### 001c-2-d：清理

撤销观测 provider（源文件 + `.class` + service 声明）、`ForgeModFileInfoParser` 临时变更、
任何临时日志。清理后与 **001b-1 基线**一致（15/15 条目）。**标准是内容一致，非 SHA-256 恢复。**

---

## 五、PASS / FAIL 判据

| 编号 | 判据 |
|---|---|
| PASS-001c-2-a | `constructMod()` 行为已取证 |
| PASS-001c-2-b | `loadMod` 正常返回 `ModContainer` |
| PASS-001c-2-c | `FORGE PROBE CONSTRUCTOR REACHED` 命中 ⇒ **PASS** |
| VALID-001c-2-c | 未命中但失败层已归类到 a–e ⇒ **VALID EXPERIMENT** |
| FAIL-001c-2-c | 未命中且失败层无法归类 ⇒ **SPIKE FAILED** |
| PASS-001c-2-d | 清理后与 001b-1 基线内容一致 |

整体结果映射：`PASS` → 成功实例化；`VALID(层 a)` → Class Load Failed；
`VALID(层 b)` → Annotation Interpretation Failed；`VALID(层 c)` → **Constructor Signature ABI Break**；
`VALID(层 d)` → Parameter Injection Failed；`VALID(层 e)` → User Code Exception；`FAIL` → Spike Failed。

---

## 六、STOP 条件

| 情况 | 处理 |
|---|---|
| `constructMod()` 行为无法取证 | STOP |
| `loadMod` 未返回非 null `ModContainer` | STOP |
| 服务端在 `constructMod` 之前就崩溃（与本 Spike 无关） | 记录，STOP |
| `constructMod` 未被调用 | **不 STOP** —— 记录为 VALID，交回 |
| ForgeProbe 构造函数抛异常 | **不 STOP** —— 按五层归类，记录 VALID |
| 需要反射调用 `constructMod()` 才能继续 | STOP（违反 §二） |
| 需要修改 `FMLModContainer` 才能继续 | STOP |
| 需要注入 Forge `IEventBus` / `FMLJavaModLoadingContext` | STOP（属 002） |

任何 STOP 后：不得进入 002；不得自行设计修复方案；不得扩大实验范围。

---

## 七、Failure Chain

```
Discovery / Metadata / Validation:  YES（沿用）
Scan:                               YES（沿用 001c-0）
LanguageLoader:                     YES（沿用 001c-1）
Entrypoint extraction:              PASS
FMLModContainer construction:       PASS
constructMod() invocation:          YES / NO / UNKNOWN
Class.forName(entrypoint):          PASS / FAIL / UNKNOWN   ← 层 a
@Mod interpretation:                PASS / FAIL / UNKNOWN   ← 层 b
Constructor signature:              PASS / FAIL / UNKNOWN   ← 层 c
Parameter injection:                PASS / FAIL / UNKNOWN   ← 层 d
ForgeProbe constructor body:        PASS / FAIL / UNKNOWN   ← 层 e
```

---

## 八、结构化输出格式（强制）

报告须按**十二节**结构输出：实验环境 / 前置配置确认 / `constructMod()` 行为取证 /
`loadMod` 返回值 / `constructMod` 观测（含完整 stack trace 与调用链）/ 清理确认 /
Failure Chain / 完整日志 / 事实标签 / 结论（含"结论上限"与禁止推导清单）/ 边界确认 / 禁止事项确认。

---

## 九、交接规则

报告原样交回架构决策方，**不得自行决定下一 Spike**。

| 情况 | 条件 | 处置 |
|---|---|---|
| **A** | `FORGE PROBE CONSTRUCTOR REACHED` 命中 | ForgeProbe 成功实例化 → ABI 断层不存在于当前路径 → 重新评估 002 的必要性 |
| **B** | 失败层 = **a**（类加载） | 架构决策方判断：类路径 / ModuleLayer / 隔离问题 |
| **C** | 失败层 = **c**（签名解析） | **Forge 命名空间参数类型的 ABI 断层确认** → 架构决策方判断 002 的 Shim 范围 |
| **D** | 失败层 = **d** 或 **e** | 架构决策方判断 |
| **E** | `constructMod` 未被调用 | 记录失败形态，交回 |

---

## 十、本 Spike 的真正使命

001c-2 **不是为了修复 ABI 断层**。它只回答：

> `FMLModContainer.constructMod()` 被真实调用后，ForgeProbe 能否被成功实例化？

```
[F] constructMod() 行为取证
        ↓
[E] loadMod 返回 FMLModContainer
        ↓
[E] NeoForge 生命周期调用 constructMod()
        ↓
[E] ForgeProbe 实例化结果（PASS / 分层失败）
        ↓
[D] 002 的范围
```

> **不要**将"ForgeProbe 构造成功"写成"Forge Mod 已经能够被 NeoForge 加载"——
> 前者只证明 entrypoint 实例化通过；后者需要 Registry / Event / Config / 完整生命周期。
>
> **不要**将"ForgeProbe 构造失败（层 c）"写成"Forge constructor ABI 断层"——
> 前者是实验记录；后者是机制结论，需要 002 的进一步验证。
>
> **001c-2 完成后必须停在这里。**
