# Spike-002-0a 执行任务说明书（类型存在性探针）

**接收方**：Codex
**前置文档**：《Forge → NeoForge 兼容层开发手册 v1.1》+《Spike-001b-6 实验报告》+
《Spike-001c-0 / 001c-1 / 001c-2 实验报告》
**任务性质**：**类型存在性探针**（不是 Shim 实现）
**唯一目标**：让 `net.minecraftforge.eventbus.api.IEventBus` 在 NeoForge 当前 ClassLoader 策略下
**可解析**，观测层 c 的断点是否消失，并记录下一个真实失败点。

```
Spike-001c-2   ✅ 完成（VALID / Constructor Signature ABI Break，层 c FAIL）
Spike-002-0a   ▶ 本说明书
Spike-002-0b   ⏸ 待 002-0a 后决定
```

---

## 〇、前置配置

与 001c-2 相同的三项（均来自 001b-6 已验证路径）：观测 provider（`javafml` / `9999`）、
`ForgeModFileInfoParser.build()` 返回真实 `ModFileInfo`、service 声明。

---

## 一、唯一目标

只回答：**单独补充 `net.minecraftforge.eventbus.api.IEventBus` 类型存在性后，
层 c 的 `NoClassDefFoundError` 是否消失？如果消失，下一个真实失败点在哪一层？**

**本 Spike 不是**："实现 IEventBus Shim" / "让 ForgeProbe 跑起来" / "修复 ABI 断层"。
**本 Spike 是**：通过最小类型注入**排除一个变量**，看下一断点。

---

## 二、五层定义（沿用 001c-2）

```
层 a: entrypoint 类加载（Class<?> 获取）
层 b: @Mod 注解解释
层 c: 构造函数签名解析
层 d: 参数注入
层 e: 构造函数体
```

> **层 a 的语义是"entrypoint 类被成功加载为 `Class<?>`"**，不是"某处调用了 `Class.forName`"。
> 001c-2 已确认 `constructMod()` 内部**不使用** `Class.forName`，类加载发生在
> `FMLModContainer` 构造函数内。后续所有层 a 的引用必须写作
> "entrypoint 类加载"或"`Class<?>` 获取"。

---

## 三、实验边界

### 3.1 唯一允许的新增

仅允许新增一个**完全空接口**文件：
`src/main/java/net/minecraftforge/eventbus/api/IEventBus.java`

```java
package net.minecraftforge.eventbus.api;

public interface IEventBus {
}
```

不声明任何方法、常量、嵌套类型。理由：任何方法声明都会成为"设计决策"
（例如声明 `register(Object)` 会引入 Shim 语义）；本 Spike 只探测**类型存在性**，
不探测**接口形状**。

### 3.2 明确禁止的额外声明

不得加入 `register(...)` / `post(...)` / `unregister(...)` / `EventBus` /
`IEventBusListener` / `FMLJavaModLoadingContext` / `@SubscribeEvent` 等任何一项，
否则实验变量会从"类型存在性是否足以越过层 c"变成"我们设计的 Shim 能否工作"。

### 3.3 禁止事项

不修改 NeoForge 核心 / `FMLModContainer` / `ModContainer` / `ModSorter` / `LoadingModList` /
`ModInfo` / `ModFile` / `ModFileInfo` / `ModFileScanData`；不修改 `forgeprobe.jar` / `mods.toml`；
不修改 ForgeProbe 源码或字节码；**不在 `IEventBus` 中声明任何成员**；
**不新增任何其它 `net.minecraftforge.*` 类型**；不引入 ASM / Mapping / Mixin / Registry /
Event / Isolation；不改变版本；**不改变 ClassLoader 策略 / 不新增 ClassLoader /
不修改 parent delegation / 不使用 ClassLoader hack / 不修改 ModuleLayer 结构**；
不在 `loadMod` 中手动触发 entrypoint 类加载；不注入 `IEventBus` 实例；不实现 Adapter / Proxy；
不使用 Unsafe / 字节码注入。

### 3.4 类型如何"进入可见范围"

唯一允许方式：**把 `IEventBus.class` 编入兼容层已有的 JAR**（`forgecompat.jar`）。

兼容层 JAR 位于 **SERVICE Layer**（001b-3 已证）；`ForgeProbe` 位于 **GAME Layer**；
GAME 的 parent 包含 PLUGIN 与 SERVICE。

> 本 Spike 就是验证：当前 ClassLoader / ModuleLayer 策略下，GAME Layer 通过
> **parent delegation** 能否解析 SERVICE Layer 中的该类型。
>
> 若解析成功 → 层 c 消失，观测下一断点。
> 若解析失败 → 记录为 `OBS-002-0a-BLOCKED`，交回架构决策方，
> **不得尝试其他注入方式**。

---

## 四、精确环境（冻结）

```
Minecraft 1.21.1 / NeoForge 21.1.256 / FancyModLoader 4.0.45
Java 21（OpenJDK 21.0.10，Microsoft Build）/ Windows 11 amd64 / CLI Dedicated Server
编译依赖 Forge 1.21.1-52.1.0
```

---

## 五、子步骤

### 002-0a-a：类型存在性前置检查

直接复用 001c-2 的结果作为前置事实（**无需重跑**）：

```
异常类型: NoClassDefFoundError
缺失类型: net.minecraftforge.eventbus.api.IEventBus
抛出位置: FMLModContainer.constructMod(FMLModContainer.java:89)
```

### 002-0a-b：新增空接口 + 重跑

1. 新增空接口源文件（内容见 §3.1）
2. 重新编译 + 重打包
3. **强制验证类型已进入 JAR**（防止"JAR 没打进去"被误判为 ClassLoader 问题）：

```bash
jar tf forgecompat.jar | findstr IEventBus     # 期望：net/minecraftforge/eventbus/api/IEventBus.class
javap -classpath forgecompat.jar net.minecraftforge.eventbus.api.IEventBus
                                               # 期望：public interface … { }（无方法/字段/嵌套类型）
```

4. 恢复 §〇 三项前置配置
5. 启动服务端
6. 观测：层 c 的 `NoClassDefFoundError` 是否消失；若消失，下一个失败点是什么

### 002-0a-c：层判定

必须给出 a / c / d / e 的明确结果；**b 可保持 UNKNOWN**（本 Spike 不专门验证 `@Mod` 注解解释阶段）。
**不得为了"消除 UNKNOWN"而主动增加实验** —— 层 b 的 UNKNOWN 是预期保留态，不是实验缺陷。

### 002-0a-d：清理

撤销观测 provider、`ForgeModFileInfoParser` 临时变更、**新增的 `IEventBus.java`
（从源码与 JAR 中移除）**。清理后与 001b-1 基线一致（15/15 条目）。
**标准是内容一致，非 SHA-256 恢复。**

---

## 六、观测判据（OBS 三态）

| 编号 | 定义 |
|---|---|
| **OBS-002-0a-CLEAR** | 层 c 消失 + 出现新失败点（`NoClassDefFoundError` 不再出现，第一个新失败点已记录） |
| **OBS-002-0a-BLOCKED** | 在未改变 ClassLoader / ModuleLayer 策略的前提下，`IEventBus.class` 已确认进入 JAR（`jar tf` + `javap` 双验证），但运行时**仍**无法解析该类型 |
| **OBS-002-0a-NOOP** | 异常形态改变但失败原因仍与 `IEventBus` 类型解析相关；或结果无法归入以上两态 |

**BLOCKED 的严格定义**（必须同时满足）：

```
- IEventBus.class 已确认进入 JAR（jar tf 可查）
- IEventBus 是空接口（javap 可查）
- ClassLoader / ModuleLayer 策略未改变
- 运行时仍抛 NoClassDefFoundError: net/minecraftforge/eventbus/api/IEventBus
```

**CLEAR 的子类型与 Spike 结论**：

| 新失败点 | Spike 结论 |
|---|---|
| 层 d（参数注入 / 白名单） | 层 c 已被类型存在性解除；下一断点为层 d |
| 层 e（构造函数体） | 层 c 与层 d 均被解除；下一断点为层 e |
| 另一个 Forge 类型缺失 | 层 c 有多重成因；下一断点仍是"类型缺失"类 |
| **`AbstractMethodError`** | 类型存在性 OK；**接口形状 / 方法 ABI 缺失** ← 极有价值的 CLEAR |

> `AbstractMethodError` 证明"**类型存在性 ✅ / 接口形状 / 方法 ABI ❌**"是**两个独立的门**。
> 这是 002-0a 想测的东西，**不是失败**。

---

## 七、STOP 条件

| 情况 | 处理 |
|---|---|
| 类型注入后服务端未能启动（与本 Spike 无关的启动失败） | 记录，STOP |
| 层 c 仍存在（BLOCKED） | 记录，STOP，交回 |
| 层 c 消失但失败原因无法归入五层 | 记录，STOP，交回 |
| 需要修改 ClassLoader 才能让类型可解析 | STOP（违反 §3.3） |
| 需要新增其他 `net.minecraftforge.*` 类型 | STOP（违反 §3.3） |
| 需要修改 `FMLModContainer` / ForgeProbe 才能继续 | STOP |

任何 STOP 后：不得进入 002-0b；不得自行设计修复方案；不得扩大实验范围。

---

## 八、Failure Chain

```
Discovery / Metadata / Validation / Scan / LanguageLoader:  YES（沿用）
Entrypoint extraction:        PASS（沿用）
FMLModContainer construction: PASS（沿用）
constructMod() invocation:    YES（沿用）
entrypoint 类加载:            PASS（沿用）
@Mod 注解解释:                UNKNOWN（沿用）
构造函数签名解析:             PASS / FAIL ← 核心观测
参数注入:                     PASS / FAIL / NOT REACHED
构造函数体:                   PASS / FAIL / NOT REACHED
```

---

## 九、结构化输出格式（强制）

报告须按**十二节**结构输出：实验环境 / 前置配置确认 / 类型存在性前置检查 /
类型注入 + 重跑（含 `IEventBus.java` 原文、`jar tf` 原文、`javap` 原文、JAR SHA-256、
新失败点完整 stack trace）/ 层判定表 / 清理确认 / Failure Chain / 完整日志 /
事实标签 / 结论（含"结论上限"与禁止推导清单）/ 边界确认（13 项）/ 禁止事项确认。

---

## 十、交接规则

报告原样交回架构决策方。

| OBS 结果 | 处置 |
|---|---|
| **CLEAR**（新失败点在**层 d**） | 层 c 由类型存在性解除；下一断点为层 d → 002-0b 方向：**参数注入 ABI 探针** |
| **CLEAR**（新失败点在**层 e**） | 层 c 与 d 均被解除 → 002-0b 方向：**构造函数体 ABI 探针** |
| **CLEAR**（新失败点是 **`AbstractMethodError`**） | 类型存在性 ✅；接口形状 / 方法 ABI ❌ → 002-0b 方向：**接口形状探针** |
| **CLEAR**（新失败点仍是另一个 Forge 类型缺失） | 层 c 有多重成因 → 002-0b 方向：**类型补全策略** |
| **BLOCKED** | 类型注入在当前策略下无效；**不自行尝试其他注入方式**；交回 |
| **NOOP** | 结果异常；交回 |

> 不预设 002-0b 的具体策略。

---

## 十一、本 Spike 的真正使命

002-0a **不是为了修复 ABI 断层**，也不是为了确定最终 Shim 方案。它只回答：

> 单独补充 `net.minecraftforge.eventbus.api.IEventBus` 类型存在性，是否足以越过层 c？
> 如果足以，下一断点在哪一层？

```
[F] ForgeProbe 构造函数引用 Forge IEventBus
        ↓
[E] NeoForge 运行时无法解析该类型（001c-2）
        ↓
[E] 层 c ABI 断层
        ↓ 002-0a
[E] 单补类型存在性是否足以越过层 c
        ↓
[E] 下一个真实断点
        ↓
[D] 真正 Shim / Adapter / Mapping 的范围
```

> **不要**将"层 c 消失"写成"ABI 断层已解除"—— 前者只说明一个具体类型的缺失被补上；
> 后者需要所有 Forge 命名空间类型都可得。
>
> **不要**将"补一个空接口有效"写成"最终应使用空壳 Shim 方案"—— 后者是 `[D]`，
> 需要 002-0b 之后的完整证据。
>
> **不要**将"层 c 消失 + 出现 `AbstractMethodError`"写成"`IEventBus` Shim 成功"；
> 正确写法是：`[E]` 提供空 `IEventBus` 类型后，原层 c 类型解析失败消失，
> 运行继续至新的 ABI 失败点。
>
> **CLEAR ≠ 修复成功 —— 这一点必须保持。**
>
> 现在还不应该决定"最终用空壳、Adapter 还是字节码转换"。002-0a 的价值正是
> 把这个选择从猜测变成实验结果。
>
> **002-0a 完成后必须停在这里。**
