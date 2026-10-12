# Spike-002-0a 实验报告

> **结论（先行）**：**OBS-002-0a-CLEAR —— 新失败点在层 d（参数注入）。**
>
> - 层 c（构造函数签名解析）：**FAIL → PASS**。注入空接口后，
>   `NoClassDefFoundError: net/minecraftforge/eventbus/api/IEventBus` **消失**
>   ⇒ GAME Layer 通过 parent delegation **成功解析了 SERVICE Layer 中的该类型**。
> - 新失败点 = **层 d**：
>   ```
>   java.lang.RuntimeException: Mod constructor has unsupported argument
>     interface net.minecraftforge.eventbus.api.IEventBus.
>     Allowed optional argument classes: IEventBus, Di…
>       at FMLModContainer.constructMod(FMLModContainer.java:110)
>   ```
>
> **这是极有价值的 CLEAR**：证明「**类型存在性**」与「**类型被接受**」是**两道独立的门** ——
> 前者由 ClassLoader / ModuleLayer 策略决定，后者由 `FMLModContainer` 的**参数白名单**决定。
> 两者都不是"ABI 断层"一词可以概括的。
>
> 按 §十 属 **CLEAR（新失败点在层 d）**。**不进入 002-0b**，报告原样交回。

---

## 一、实验环境

同 001c-2：Minecraft 1.21.1 / NeoForge 21.1.256 / FancyModLoader 4.0.45 /
Java 21（OpenJDK 21.0.10，Microsoft Build）/ Windows 11 amd64 / CLI Dedicated Server /
编译依赖 Forge 1.21.1-52.1.0。运行标签 `spike002-0a`。

---

## 二、前置配置确认

- [x] `IModLanguageLoader` observer 已就位（`name=javafml`，`version=9999`）
- [x] `ForgeModFileInfoParser.build()` 返回真实 `ModFileInfo`
- [x] `META-INF/services/...IModLanguageLoader` 已声明该 provider

---

## 三、类型存在性前置检查（002-0a-a）

前置事实（来自 001c-2，**未重跑**）：

```
异常类型: NoClassDefFoundError
缺失类型: net.minecraftforge.eventbus.api.IEventBus
抛出位置: FMLModContainer.constructMod(FMLModContainer.java:89)
```

**PASS-002-0a-a ✅**

---

## 四、类型注入 + 重跑（002-0a-b）

**IEventBus.java 文件路径**：`src/main/java/net/minecraftforge/eventbus/api/IEventBus.java`

**内容（完整原文，确认为空接口 —— 无方法 / 无字段 / 无嵌套类型）**：

```java
package net.minecraftforge.eventbus.api;

public interface IEventBus {
}
```

**`jar tf forgecompat.jar | findstr IEventBus` 原文**：

```
net/minecraftforge/eventbus/api/IEventBus.class
```

**`javap -classpath forgecompat.jar net.minecraftforge.eventbus.api.IEventBus` 原文**：

```
Compiled from "IEventBus.java"
public interface net.minecraftforge.eventbus.api.IEventBus {
}
```

**逐项确认**：class 存在 ✅ / `interface` 关键字 ✅ / 无方法 ✅ / 无字段 ✅ / 无嵌套类型 ✅

**JAR SHA-256**: `fa14adb184399c921cef7da34e57ba917b46d7568163c8e5060f36894ee78aca`

**服务端启动结果**：

- `NoClassDefFoundError (IEventBus)`: **消失**
- 新失败点：

```
位置:      FMLModContainer.constructMod(FMLModContainer.java:110)
异常类型:  java.lang.RuntimeException
异常消息:  Mod constructor has unsupported argument interface
           net.minecraftforge.eventbus.api.IEventBus.
           Allowed optional argument classes: IEventBus, Di…（后接允许列表）
```

`latest.log` 上下文原文：

```
[modloading-worker-0/ERROR] [FMLModContainer/LOADING]: Failed to create mod instance. ModID: forgeprobe, class com.e…
java.lang.RuntimeException: Mod constructor has unsupported argument interface net.minecraftforge.eventbus.api.IEventBus. Allowed optional argument classes: IEventBus, Di…
	at net.neoforged.fml.javafmlmod.FMLModContainer.constructMod(FMLModContainer.java:110)
	at net.neoforged.fml.ModLoader.lambda$constructMods$3(ModLoader.java:153)
…
[main/FATAL] [net.neoforged.fml.ModLoader/LOADING]: Failed to wait for future Mod Construction, 1 errors found
net.neoforged.fml.ModLoadingException: Loading errors encountered:
	- Forge Probe (forgeprobe) has failed to load correctly
	  java.lang.RuntimeException: Mod constructor has unsupported argument interface net.minecraftforge.eventbus.api.IEventBus. …
	at net.neoforged.fml.ModLoader.constructMods(ModLoader.java:152)
```

`stdout` 中 `[FORGECOMPAT][002-0a]` 标记：`entrypoints = [com.example.forgeprobe.ForgeProbe]`
→ `FMLModContainer constructed, returning to NeoForge: …@2ac4b262`（另有第二次空 entrypoints，同 001c-2）。

`stderr` **0 字节**（异常被 FML 包装为 issue）。

---

## 五、层判定（002-0a-c）

| 层 | 001c-2 | 002-0a |
|---|---|---|
| a entrypoint 类加载 | PASS | **PASS**（不变） |
| b `@Mod` 注解解释 | UNKNOWN | **UNKNOWN**（预期内保留态） |
| **c 构造函数签名解析** | FAIL | **PASS** ← 核心观测 |
| **d 参数注入** | NOT REACHED | **FAIL** ← 新断点 |
| e 构造函数体 | NOT REACHED | NOT REACHED |

**PASS-002-0a-c ✅**（a / c / d / e 均有明确结果；层 b 的 UNKNOWN 为预期保留态）

---

## 六、清理确认（002-0a-d）

| 项 | 结果 |
|---|---|
| `ForgeCompatLanguageLoader` 已删除 | ✅ |
| `ForgeModFileInfoParser` 已恢复基线 | ✅ |
| `IModLanguageLoader` service 声明已删除 | ✅ |
| **`IEventBus.java` 已从源码中删除** | ✅ |
| **`IEventBus.class` 已从 JAR 中删除**（`jar tf` 命中数 = **0**） | ✅ |
| `IModFileReader` / `IModFileCandidateLocator` service 与基线一致 | ✅ |
| `MANIFEST` 与基线一致 | ✅ |
| `forgeprobe.jar` 未修改 | ✅ |

**逐条目比对：entries = 15 / diff = 0。 PASS-002-0a-d ✅**

---

## 七、Failure Chain

```
Discovery / Metadata / Validation / Scan / LanguageLoader:  YES（沿用）
Entrypoint extraction:        PASS（沿用）
FMLModContainer construction: PASS（沿用）
constructMod() invocation:    YES（沿用）
entrypoint 类加载:            PASS（沿用）
@Mod 注解解释:                UNKNOWN（沿用）
构造函数签名解析:             **PASS**（001c-2 为 FAIL）
参数注入:                     **FAIL** ← 核心观测
构造函数体:                   NOT REACHED
```

---

## 八、完整日志

- latest.log: `D:\DualForge\spike000\logs\spike002-0a\spike002-0a.log`
- debug.log: `D:\DualForge\spike000\logs\spike002-0a\spike002-0a.debug.log`
- stdout: `D:\DualForge\spike000\logs\spike002-0a\spike002-0a.stdout.log`
- stderr: `D:\DualForge\spike000\logs\spike002-0a\spike002-0a.stderr.log`（0 字节）
- crash-report: 未生成独立 crash-report

---

## 九、事实标签

### [F] Source Fact

1. `[F]` `FMLModContainer.constructMod` 在白名单判定失败时抛出的文案为
   `"Mod constructor has unsupported argument interface {}"`，并列出
   `"Allowed optional argument classes: …"`（`FMLModContainer.java:110`）。
2. `[F]` 该白名单**只接受 NeoForge 命名空间的参数类型**（`net.neoforged.bus.api.IEventBus` 等），
   **不接受** `net.minecraftforge.eventbus.api.IEventBus`。

### [E] Experimental Fact

1. `[E]` 注入空接口后，`IEventBus.class` 已确认进入 JAR（`jar tf` 可查）且为空接口（`javap` 可查）。
2. `[E]` **层 c 消失**：运行时不再抛
   `NoClassDefFoundError: net/minecraftforge/eventbus/api/IEventBus`。
   ⇒ GAME Layer（`ForgeProbe`）经 **parent delegation** 成功解析了 SERVICE Layer 中的该类型。
3. `[E]` 新失败点 = **层 d**：`RuntimeException: Mod constructor has unsupported argument
   interface net.minecraftforge.eventbus.api.IEventBus`，抛自 `constructMod(FMLModContainer.java:110)`。
4. `[E]` `FORGE PROBE CONSTRUCTOR REACHED` 仍未命中 ⇒ 层 e 未到达。
5. `[E]` `stderr` 0 字节（异常被 FML 包装为 issue）。
6. `[E]` 清理后 `IEventBus` 在 JAR 中命中数 = 0；15/15 条目与 001b-1 基线一致。

### [H] Hypothesis

1. `[H]` 层 d 的判定**发生在签名解析之后**：`FMLModContainer` 先成功取得参数类型
   （层 c PASS），再比对白名单（层 d FAIL）。两阶段的分离由两次运行的异常类型差异佐证。
2. `[H]` 若把参数类型"变成"白名单内的类型（例如让 `ForgeProbe` 的构造函数接受
   `net.neoforged.bus.api.IEventBus`），层 d 应可通过 —— 但这需要修改 ForgeProbe ABI，
   **属被禁止手段**，本轮未做。
3. `[H]` 「类型存在性」与「类型被接受」是两道独立的门，**任何单一手段都不能同时越过**。

### [D] Design Decision

1. `[D]` **ClassLoader / ModuleLayer 策略不是瓶颈**：SERVICE → GAME 的 parent delegation
   已被证明可以把 Forge 命名空间的类型送达 entrypoint。
2. `[D]` **真正的瓶颈在 `FMLModContainer` 的参数白名单**：它是硬编码的 NeoForge 类型集合，
   不接受 Forge 命名空间参数。这是 002-0b 需要面对的对象。
3. `[D]` `CLEAR ≠ 修复成功`：本次只证明"一个具体类型的缺失被补上"，
   不代表 ABI 断层已解除，也不代表最终应采用空壳方案。

---

## 十、结论

- **OBS-002-0a 结果: CLEAR**
- 层 c 是否消失: **是**
- 第一个新失败点:
  - 位置: `FMLModContainer.constructMod(FMLModContainer.java:110)`
  - 层级: **d（参数注入）**
  - 异常类型: `java.lang.RuntimeException`

**结论上限（禁止超出）**：

> `[E]` 在当前 ClassLoader / ModuleLayer 策略下，向兼容层 JAR 注入
> `net.minecraftforge.eventbus.api.IEventBus` **空接口**后，层 c 的 `NoClassDefFoundError`
> **消失**，下一个失败点出现在**层 d**（参数白名单拒绝）。
>
> `[D]` 仅根据上述结果决定 002-0b 的范围。

**禁止从本 Spike 推导**（本报告均未主张）：Forge `IEventBus` 已被兼容 /
ForgeProbe 已被实例化 / 参数注入策略已确定 / 完整 Shim 方案已可行 /
后续是否使用 ASM / Adapter / Mapping 已决定。

**Confidence**: **HIGH**

**Evidence**: §四 的 `jar tf` + `javap` 双验证与异常原文；§五 的层对照；§六 的 15/15 一致。

**Hypotheses still unverified**: §九 `[H]` 1–3。

---

## 十一、边界确认

- [x] `IEventBus` 接口声明为空（无方法 / 字段 / 嵌套类型）
- [x] 未新增任何其它 `net.minecraftforge.*` 类型
- [x] 未改变 ClassLoader 策略 / 未新增 ClassLoader / 未修改 parent delegation
- [x] 未使用 ClassLoader hack
- [x] 未修改 ModuleLayer 结构
- [x] 未修改 ForgeProbe 源码 / 字节码
- [x] 未修改 `FMLModContainer`
- [x] 未在 `loadMod` 中手动触发 entrypoint 类加载
- [x] 未注入 `IEventBus` 实例
- [x] 未实现 Adapter / Proxy
- [x] 未进入 002-0b

---

## 十二、禁止事项确认

- [x] 未修改 NeoForge 核心
- [x] 未修改 `FMLModContainer` / `ModContainer` / `ModSorter` / `LoadingModList`
- [x] 未修改 `ModInfo` / `ModFile` / `ModFileInfo` / `ModFileScanData`
- [x] 未修改 `forgeprobe.jar` / `mods.toml`
- [x] 未引入 ASM / Mapping / Mixin / Registry / Event / Isolation
- [x] 未使用 Unsafe / 字节码注入
- [x] 未进入 002-0b
- [x] 未提出修复方案
