# Spike-001c-1 执行任务说明书

**接收方**：Codex
**前置文档**：《Forge → NeoForge 兼容层开发手册 v1.1》（含 §3.1 / §3.3 / §3.8）+
《Spike-001b-6 实验报告》+《Spike-001c-0 实验报告》
**任务性质**：**最小运行时构造**（不调用 `constructMod`，不实例化 `@Mod`）
**唯一目标**：在真实 `IModLanguageLoader.loadMod(...)` 调用中，用 001c-0 已获得的
`ModFileScanData` 与提取出的 Forge entrypoint 数据，**尝试构造 `FMLModContainer`**。

```
Spike-001b-6   ✅ 完成（两道 cast 均 PASS；loadMod 被真实调用）
Spike-001c-0   ✅ 完成（五层判据 A–E 全部 PASS；Forge @Mod 数据可得）
Spike-001c-1   ▶ 本说明书
Spike-001c-2   ⏸ 待 001c-1 后决定
```

---

## 〇、前置配置（固定复现条件）

运行前必须恢复以下三项（**均来自 001b-6 已验证路径，不是本 Spike 的实验变量**）：

1. `IModLanguageLoader` observer：`name()="javafml"`，`version()="9999"`，已注册于 service
2. `ForgeModFileInfoParser.build()` → 返回真实 `ModFileInfo`
   （`new ModFileInfo(modFile, config, info -> { })`）
3. `META-INF/services/net.neoforged.neoforgespi.language.IModLanguageLoader` 已声明该 provider

> 若未恢复第 2 项，会出现 `ModSorter → ModInfo::cast` CCE；
> 该情况属**前置配置错误**，不计为 001c-1 的实验结果。

---

## 一、唯一目标

只回答：**用真实 `scanData` 提取的 Forge entrypoint 数据构造 `FMLModContainer` 是否成功？**

```
loadMod() → 解析 scanData → 构造 entrypoints → new FMLModContainer(info, entrypoints, scanData, layer)
   → return ModContainer → 【观测桩停止】← 本 Spike 的终点
```

**不进入**：`constructMod()` / ForgeProbe 实例化 / Forge constructor 参数注入 /
Forge `IEventBus` / `FMLJavaModLoadingContext` ABI 断层。

---

## 二、实验边界

### 2.1 只做 FMLModContainer 构造

不解决：ForgeProbe 实例化（属 001c-2）、Forge constructor ABI（001c-2 / 002）、
Forge `IEventBus` / `FMLJavaModLoadingContext`（002）、Registry / Event / Mapping / Mixin /
Isolation、Module / Game Layer identity。

### 2.2 允许变更

- 在 `ForgeCompatLanguageLoader.loadMod(...)` 中：从 `scanData.getAnnotations()` 提取 Forge entrypoints、
  构造 `FMLModContainer`、打印构造前后日志、**构造成功后立即抛观测桩**
- 临时恢复 §〇 前置配置
- 读取 `ModFileScanData` / `AnnotationData` 公开 API；用 `javap` / Javadoc 确认构造函数签名

### 2.3 为什么"构造成功后要立即抛观测桩"

- 若 `loadMod` 正常返回 `ModContainer`，NeoForge 会继续调用 `constructMod()` → 进入 001c-2 范围
- 若不抛观测桩，ForgeProbe 会在 001c-1 中被实例化 —— **违反边界**

> 观测桩抛出位置：`new FMLModContainer(...)` **之后**、`return` **之前**。

### 2.4 禁止事项

不修改 NeoForge 核心 / `ModSorter` / `LoadingModList` / `ModInfo` / `ModFile` / `ModFileInfo` /
`ModFileScanData` / `FMLModContainer` / `LanguageProviderLoader` / `VersionSupportMatrix`；
不修改 `forgeprobe.jar` / `mods.toml`；不引入 ASM / Mapping / Mixin / Registry / Event / Isolation；
不改变版本；**不调用 `constructMod()`**；**不调用 ForgeProbe 构造函数**；
不主动注入 Forge `IEventBus` 或 `FMLJavaModLoadingContext`；
**不修改 `FMLModContainer` 的构造参数类型或顺序以"让它通过"**；不实现 annotation shim；
不使用 Unsafe / 字节码注入 / ClassLoader hack；**不使用反射绕过构造函数的公开访问约束**。

---

## 三、精确环境（冻结）

```
Minecraft 1.21.1 / NeoForge 21.1.256 / FancyModLoader 4.0.45
Java 21（OpenJDK 21.0.10，Microsoft Build）/ Windows 11 amd64
启动方式 CLI / Dedicated Server        编译依赖 Forge 1.21.1-52.1.0
```

---

## 四、子步骤

### 001c-1-a：`FMLModContainer` 构造函数签名取证（不做运行）

从精确版本 `loader-4.0.45.jar` 字节码确认：class 全名、公开构造函数完整列表、
参数逐个说明（含精确类型）、构造函数可见性、是否有多个重载。

| 参数 | 期望类型 |
|---|---|
| info | `IModInfo` |
| entrypoints | `List<String>` |
| scanData | `ModFileScanData` |
| layer | `ModuleLayer` |

**PASS-001c-1-a 判据**：签名已从精确版本字节码获取，无 UNKNOWN。

### 001c-1-b：entrypoint 提取

**提取规则（不得扩展）**：

1. 遍历 `scanData.getAnnotations()`
2. 筛选 `annotationType().getClassName().equals("net.minecraftforge.fml.common.Mod")`
3. 对每条匹配项取 `clazz().getClassName()` 作为 entrypoint 的 FQCN
4. 组成 `List<String>` 作为 entrypoints

必须打印（前缀 `[FORGECOMPAT][001c-1] `）：`scanData` 是否非 null、`annotations.size()`、
`matched Forge @Mod annotations`、逐条 `entrypoint = <fqcn>`、`entrypoints = [...]`。

**PASS 判据**：entrypoints 非空且至少含 `com.example.forgeprobe.ForgeProbe`。
**FAIL 判据**：entrypoints 为空，或未含该 FQCN。

### 001c-1-c：`FMLModContainer` 构造

骨架：

```java
List<String> entrypoints = extractForgeEntrypoints(scanData);
System.out.println("[FORGECOMPAT][001c-1] entrypoints = " + entrypoints);
try {
    FMLModContainer container = new FMLModContainer(info, entrypoints, scanData, layer);
    System.out.println("[FORGECOMPAT][001c-1] FMLModContainer constructed: " + container);
    // 不调用 constructMod()；不返回 container 给 NeoForge
    throw new UnsupportedOperationException("001c-1 boundary: FMLModContainer constructed but constructMod() not invoked");
} catch (Throwable t) {
    System.out.println("[FORGECOMPAT][001c-1] FMLModContainer construction FAILED:");
    t.printStackTrace();
    throw new UnsupportedOperationException("001c-1 boundary: FMLModContainer construction failed", t);
}
```

必须记录：`entered`、`construction result`、若 PASS 则 `container.toString()`，
若 FAIL 则异常类型 / 消息 / 完整 stack trace / 失败阶段。

**PASS-001c-1-c 判据**：构造函数正常返回（未抛异常）。
**FAIL-001c-1-c 判据**：构造函数抛出异常。

### 001c-1-d：清理

全部撤销观测 provider（源文件 + `.class` + service 声明）、
`ForgeModFileInfoParser.build()` 的临时变更（恢复基线）、任何临时观测代码。
清理后与 **001b-1 基线**一致（15/15 条目）。
**清理标准不是 SHA-256 恢复，而是内容一致。**

---

## 五、PASS / FAIL 判据

| 编号 | 判据 | 类型 |
|---|---|---|
| PASS-001c-1-a | 构造签名已确认 | PASS 门槛 |
| PASS-001c-1-b | entrypoints 提取成功 | PASS |
| FAIL-001c-1-b | entrypoints 为空 / 未含 ForgeProbe | FAIL |
| PASS-001c-1-c | `FMLModContainer` 构造成功（正常返回） | PASS |
| FAIL-001c-1-c | 构造失败（构造函数抛异常） | FAIL |
| PASS-001c-1-d | 清理后与 001b-1 基线内容一致 | PASS 门槛 |

| 001c-1-b | 001c-1-c | 整体结果 |
|---|---|---|
| PASS | PASS | **PASS** |
| PASS | FAIL | VALID EXPERIMENT / CONSTRUCTION FAILED |
| FAIL | 未执行 | VALID EXPERIMENT / ENTRYPOINT EXTRACTION FAILED |

> **FAIL-001c-1-c ≠ Spike 失败**，只是"实验有效但构造失败"。

---

## 六、STOP 条件

| 情况 | 处理 |
|---|---|
| 构造签名无法从精确版本获取 | STOP |
| `loadMod` 未被调用 | STOP（前置配置未就位） |
| entrypoints 提取失败 | **不 STOP** —— 记录 FAIL-001c-1-b，交回 |
| `FMLModContainer` 构造失败 | **不 STOP** —— 记录 FAIL-001c-1-c，交回 |
| 构造成功后未抛观测桩 | **视为实现错误，修正后重跑** |
| 构造成功后 `constructMod()` 被调用 | **视为边界违规，实验作废，重跑** |
| ForgeProbe 被实例化 | **视为边界违规，实验作废，重跑** |
| 必须修改 NeoForge 核心才能构造 | STOP |

任何 STOP 后：不得进入 001c-2；不得自行设计修复方案；不得修改构造参数"让它通过"。

---

## 七、Failure Chain

```
Discovery / Metadata / Validation:  YES（沿用 001b-6）
Scan:                               YES（沿用 001c-0）
LanguageLoader:                     YES
Entrypoint extraction:              PASS / FAIL
FMLModContainer construction:       PASS / FAIL
Forge @Mod constructor:             NOT ENTERED（本 Spike 明确不进入）
```

> `FMLModContainer construction` 指 `new FMLModContainer(...)`；
> `Forge @Mod constructor` 指 `new ForgeProbe(...)`。**两者不可混称 "constructor"**。

---

## 八、结构化输出格式（强制）

报告须按**十二节**结构输出：实验环境 / 前置配置确认 / 构造签名 /
entrypoint 提取 / 构造结果（含完整 stdout 原文）/ 清理确认 / Failure Chain / 完整日志 /
事实标签 / 结论（含"结论上限"与禁止推导清单）/ 边界确认 / 禁止事项确认。

---

## 九、交接规则

报告原样交回架构决策方，**不得自行决定下一 Spike**。

| 情况 | 条件 | 处置 |
|---|---|---|
| **A** | PASS-001c-1-b + PASS-001c-1-c | `FMLModContainer` 构造成功 → 进入 **001c-2 的前置准备**（ForgeProbe 实例化）；范围由架构决策方裁定 |
| **B** | PASS-001c-1-b + FAIL-001c-1-c | 记录失败形态，作为架构决策方输入；**不自行设计修复方案** |
| **C** | FAIL-001c-1-b | 与 001c-0 的 A–E 全 PASS 矛盾 → 记录完整 `scanData` 供决策 |
| **D** | 构造成功但流程越界 | **实验作废**，修正观测桩边界后重跑 |

---

## 十、本 Spike 的真正使命

001c-1 **不是为了让 Forge Mod 被加载**。它只回答：

> 用真实 `scanData` 提取的 Forge entrypoint 数据构造 `FMLModContainer` 是否成功？

```
[F] FMLModContainer 构造签名
        ↓
[E] entrypoint 提取成功（来自 001c-0 的 scanData）
        ↓
[E] FMLModContainer 构造成功 / 失败（含完整失败形态）
        ↓
[D] 001c-2 的范围
```

> **不要**将"`FMLModContainer` 构造成功"写成"Forge Mod 已经能够被 NeoForge 加载"——
> 前者只是"容器对象被创建"，后者是"整个生命周期闭环通过"。
>
> **不要**将"`constructMod()` 未被调用"写成"`constructMod()` 已经通过"。
>
> **001c-1 完成后必须停在这里。**
