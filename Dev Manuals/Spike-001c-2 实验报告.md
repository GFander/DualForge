# Spike-001c-2 实验报告

> **结论（先行）**：**VALID EXPERIMENT / Constructor Signature ABI Break（失败层 = c）**
>
> - `constructMod()` **被真实调用**（YES）
> - `FORGE PROBE CONSTRUCTOR REACHED` **未命中**（NO）
> - **第一个真实失败点 = 层 c（构造函数签名解析）**：
>   `java.lang.NoClassDefFoundError: net/minecraftforge/eventbus/api/IEventBus`
>   ← `Caused by: ClassNotFoundException: net.minecraftforge.eventbus.api.IEventBus`
>   位置：`FMLModContainer.constructMod(FMLModContainer.java:89)`
>
> 这是**Forge 命名空间参数类型在类路径上根本不存在**的典型症状 —— ABI 断层的第一次实证，
> 且断层形式比"参数注入被拒"更基础：**参数类型本身无法解析**。
>
> 按 §九 属**情况 C**。**不进入 002**，报告原样交回。

---

## 一、实验环境

同 001c-1：Minecraft 1.21.1 / NeoForge 21.1.256 / FancyModLoader 4.0.45 /
Java 21（OpenJDK 21.0.10，Microsoft Build）/ Windows 11 amd64 / CLI Dedicated Server /
编译依赖 Forge 1.21.1-52.1.0。运行标签 `spike001c-2`。

---

## 二、前置配置确认

- [x] `IModLanguageLoader` observer 已就位（`name=javafml`，`version=9999`）
- [x] `ForgeModFileInfoParser.build()` 返回真实 `ModFileInfo`
- [x] `META-INF/services/...IModLanguageLoader` 已声明该 provider

---

## 三、constructMod() 行为取证（001c-2-a）

```java
protected void constructMod()
```

| 项 | 结果 |
|---|---|
| 可见性 | `protected` |
| 内部关键调用（按顺序） | `for (Class<?> clazz : modClasses)` → `clazz.getConstructors()` → 白名单 `Map.of(...)`（4 组键值） → `Set.contains(...)` → `Constructor.newInstance(args)` → `LOGGER.debug("Loaded mod instance {} of type {}", …)` |
| 是否调用 `Class.forName` / `loadClass` | **NO** |
| 是否读取 `scanResults` | 不使用（`scanResults` 仅用于 mixin 错误处理） |
| 是否反射寻找构造函数 | **YES**（`Class.getConstructors()`） |
| 构造参数白名单 | `Map.of(4 组)` → 与 §3.5 的 `IEventBus` / `ModContainer` / `FMLModContainer` / `Dist` 一致 |
| 是否使用 `eventBus` | 作为候选注入参数 |
| entrypoint 构造函数调用入口 | `Constructor.newInstance(...)` |

**重要推论（`[E]`）**：`constructMod()` 内**没有** `Class.forName` ⇒ **entrypoint 类的加载发生在
`FMLModContainer` 构造函数内**（`List<String> → List<Class<?>> modClasses`）。
因此 **层 a（类加载）在 001c-1 阶段就已通过**。

**PASS-001c-2-a ✅**

---

## 四、loadMod 返回值（001c-2-b）

```
loadMod 返回:
- entered:               YES
- container constructed: YES
- returned to NeoForge:  YES
```

`stdout` 原文：

```
[FORGECOMPAT][001c-2] entrypoints = [com.example.forgeprobe.ForgeProbe]
[FORGECOMPAT][001c-2] FMLModContainer constructed, returning to NeoForge: net.neoforged.fml.javafmlmod.FMLModContainer@4d8a67e5
[FORGECOMPAT][001c-2] entrypoints = []
[FORGECOMPAT][001c-2] FMLModContainer constructed, returning to NeoForge: net.neoforged.fml.javafmlmod.FMLModContainer@4f75de5d
```

`[E]` `loadMod` 被调用**两次**：第一次为 `forgeprobe.jar`（entrypoints 含 ForgeProbe）；
第二次 entrypoints 为空 —— 因本 Spike 的 provider **覆盖了 `javafml`**，
NeoForge 自身的 `neoforge-21.1.256-universal.jar` 也经由此 loader，
其 scanData 中不含 Forge `@Mod`。这是 001b-3「情况 A」覆盖行为的直接后果，非异常。

**PASS-001c-2-b ✅**

---

## 五、constructMod 观测（001c-2-c）

| 项 | 结果 |
|---|---|
| `constructMod called` | **YES** |
| `FORGE PROBE CONSTRUCTOR REACHED` | **NO** |
| **失败层** | **c（构造函数签名解析）** |

**异常链（`latest.log` 原文）**：

```
[modloading-worker-0/ERROR] [net.neoforged.fml.javafmlmod.FMLModContainer/LOADING]: Failed to create mod instance. ModID: forgeprobe, c…
java.lang.NoClassDefFoundError: net/minecraftforge/eventbus/api/IEventBus
	at net.neoforged.fml.javafmlmod.FMLModContainer.constructMod(FMLModContainer.java:89)
	at net.neoforged.fml.ModLoader.lambda$constructMods$3(ModLoader.java:153)
Caused by: java.lang.ClassNotFoundException: net.minecraftforge.eventbus.api.IEventBus
…
[main/FATAL] [net.neoforged.fml.ModLoader/LOADING]: Failed to wait for future Mod Construction, 1 errors found
[main/ERROR] [net.minecraft.server.Main/FATAL]: Failed to start the minecraft server
net.neoforged.fml.ModLoadingException: Loading errors encountered:
	- Forge Probe (forgeprobe) has failed to load correctly
	  java.lang.NoClassDefFoundError: net/minecraftforge/eventbus/api/IEventBus
	at net.neoforged.fml.ModLoader.constructMods(ModLoader.java:152)
```

**五层判定**：

| 层 | 结果 |
|---|---|
| a `Class.forName(entrypoint)` | **PASS**（ForgeProbe 类已加载，否则会在 001c-1 的构造函数内失败） |
| b `@Mod` 注解解释 | **UNKNOWN**（未观测到独立的注解解释阶段） |
| c **构造函数签名解析** | **FAIL** ← 首失败点 |
| d 参数注入 | 未到达 |
| e 构造函数体 | 未到达（`FORGE PROBE CONSTRUCTOR REACHED` 零命中） |

`stderr` **为空** —— 异常被 FML 捕获并包装为 `ModLoadingException` + issue，
非未捕获异常。进程退出码 0，未达 `Done`。

**VALID-001c-2-c（失败层 = c）**

---

## 六、清理确认（001c-2-d）

| 项 | 结果 |
|---|---|
| `ForgeCompatLanguageLoader` 已删除 | ✅ |
| `ForgeModFileInfoParser` 已恢复基线 | ✅ |
| `IModLanguageLoader` service 声明已删除 | ✅ |
| `IModFileReader` / `IModFileCandidateLocator` service 与基线一致 | ✅ |
| `MANIFEST` 与基线一致 | ✅ |
| `forgeprobe.jar` 未修改 | ✅ |

**逐条目比对：entries = 15 / diff = 0。 PASS-001c-2-d ✅**

---

## 七、Failure Chain

```
Discovery:                    YES（沿用）
Metadata:                     YES（沿用）
Validation:                   YES（沿用）
Scan:                         YES（沿用 001c-0）
LanguageLoader:               YES（沿用 001c-1）
Entrypoint extraction:        PASS
FMLModContainer construction: PASS
constructMod() invocation:    **YES**
Class.forName(entrypoint):    **PASS**（层 a）
@Mod interpretation:          UNKNOWN（层 b）
Constructor signature:        **FAIL**（层 c）← 首失败点
Parameter injection:          NOT REACHED（层 d）
ForgeProbe constructor body:  NOT REACHED（层 e）
```

---

## 八、完整日志

- latest.log: `D:\DualForge\spike000\logs\spike001c-2\spike001c-2.log`
- debug.log: `D:\DualForge\spike000\logs\spike001c-2\spike001c-2.debug.log`
- stdout: `D:\DualForge\spike000\logs\spike001c-2\spike001c-2.stdout.log`
- stderr: `D:\DualForge\spike000\logs\spike001c-2\spike001c-2.stderr.log`（**0 字节**）
- crash-report: 未生成独立 crash-report（失败经 `ModLoadingException` 包装，栈在 latest.log）

---

## 九、事实标签

### [F] Source Fact

1. `[F]` `FMLModContainer.constructMod()` 为 `protected`，内部**不使用** `Class.forName`/`loadClass`，
   而是遍历已加载的 `List<Class<?>> modClasses`，调用 `Class.getConstructors()`，
   经白名单 `Map.of(...)` + `Set.contains(...)` 判定后 `Constructor.newInstance(...)`。
2. `[F]` 该白名单为 **4 组键值**，与手册 §3.5 的
   `IEventBus` / `ModContainer` / `FMLModContainer` / `Dist` 一致。
3. `[F]` `FMLModContainer` 的字段 `List<Class<?>> modClasses` 说明
   `List<String> → List<Class<?>>` 的转换发生在**构造函数内**。

### [E] Experimental Fact

1. `[E]` `constructMod()` **被 NeoForge 生命周期真实调用**（`ModLoader.lambda$constructMods$3`）。
2. `[E]` 层 a（entrypoint 类加载）**已通过** —— `ForgeProbe` 类成功加载
   （否则 001c-1 的 `FMLModContainer` 构造就会抛 `ClassNotFoundException`）。
3. `[E]` **层 c 失败**：`NoClassDefFoundError: net/minecraftforge/eventbus/api/IEventBus`
   ← `ClassNotFoundException: net.minecraftforge.eventbus.api.IEventBus`，
   抛自 `FMLModContainer.constructMod(FMLModContainer.java:89)`。
4. `[E]` `FORGE PROBE CONSTRUCTOR REACHED` **零命中** ⇒ 层 d/e 未到达，ForgeProbe 未被实例化。
5. `[E]` FML 将失败包装为 `fml` issue：`Forge Probe (forgeprobe) has failed to load correctly`；
   `stderr` 为空（非未捕获异常）。
6. `[E]` 因同名覆盖，`loadMod` 被调用两次（第二次 entrypoints 为空，属 NeoForge 自身 JAR）。
7. `[E]` 清理后 15/15 条目与 001b-1 基线一致。

### [H] Hypothesis

1. `[H]` 层 b（`@Mod` 注解解释）在本路径中**不存在独立阶段** ——
   `javafml` 的 entrypoint 语义只依赖 `IModFileInfo.requiredLanguageLoaders()` 与
   `scanData` 中的注解，本 Spike 未观测到该层的独立失败点。
2. `[H]` 若 `net.minecraftforge.eventbus.api.IEventBus` 出现在类路径上（002 的 Shim 范围），
   本失败点会前移或消失；**本轮未做该对照**。
3. `[H]` 层 c 失败**先于**层 d —— 说明"参数类型不可解析"比"白名单拒绝"更基础，
   `002` 需要解决的是**类型的可得性**而非注入策略。

### [D] Design Decision

1. `[D]` **ABI 断层已实证，且形式明确**：Forge 命名空间的构造参数类型
   （`net.minecraftforge.eventbus.api.IEventBus`）在 NeoForge 运行时类路径上**不存在**。
2. `[D]` 该断层的修复点在 **002**（Forge `IEventBus` / `FMLJavaModLoadingContext` 的 Shim 接管），
   但本 Spike **不提出任何修复方案**。
3. `[D]` 001c-1 报告 §九 `[H]`1 需**订正**：`FMLModContainer` 构造函数**已经加载了 entrypoint 类**
   （层 a PASS），只是**未解析其构造函数签名**（层 c 在 `constructMod` 内发生）。

---

## 十、结论

- constructMod invoked: **YES**
- ForgeProbe instantiated: **NO**
- 若 NO，失败层: **c（构造函数签名解析）**
- 本 Spike 整体结果: **VALID EXPERIMENT / Constructor Signature ABI Break**

**结论上限（禁止超出）**：

> `[E]` 在真实 `loadMod` 调用中返回 `FMLModContainer` 给 NeoForge 后，`constructMod()` **被调用**；
> ForgeProbe **未被实例化**；失败点为**层 c**，症状为
> `NoClassDefFoundError / ClassNotFoundException: net.minecraftforge.eventbus.api.IEventBus`。
>
> `[D]` 仅根据上述结果决定 002 的范围。

**禁止从本 Spike 推导**（本报告均未主张）：Forge `IEventBus` /
`FMLJavaModLoadingContext` 已兼容 / Registry / Event / Config 已兼容 /
Forge Mod 已经能够被 NeoForge 完整加载 / Mapping / Mixin / Isolation 已就绪。

**Confidence**: **HIGH**

**Evidence**: §五 的异常链原文与五层判定；§三 的 `constructMod` 字节码；§六 的 15/15 一致。

**Hypotheses still unverified**: §九 `[H]` 1–3。

---

## 十一、边界确认

- [x] 未反射调用 `constructMod()`（由 NeoForge 生命周期调用）
- [x] 未反射访问 `FMLModContainer` 私有字段
- [x] 未在 `loadMod` 内手动调用 `constructMod()`
- [x] 未注入 Forge `IEventBus` / `FMLJavaModLoadingContext`
- [x] 未修改 ForgeProbe ABI
- [x] 未实现任何 Shim
- [x] 未进入 002

---

## 十二、禁止事项确认

- [x] 未修改 NeoForge 核心
- [x] 未修改 `FMLModContainer` / `ModContainer` / `ModSorter` / `LoadingModList`
- [x] 未修改 `ModInfo` / `ModFile` / `ModFileInfo` / `ModFileScanData`
- [x] 未修改 `forgeprobe.jar` / `mods.toml`
- [x] 未引入 ASM / Mapping / Mixin / Registry / Event / Isolation
- [x] 未使用 Unsafe / 字节码注入 / ClassLoader hack / 反射绕过
- [x] 未进入 002
- [x] 未提出修复方案
