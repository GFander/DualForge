# Spike-001c-1 实验报告

> **结论（先行）**：**PASS-001c-1-b + PASS-001c-1-c 均 PASS ⇒ Spike 整体 PASS。**
>
> - entrypoint 提取成功：`[com.example.forgeprobe.ForgeProbe]`（matched = 1）
> - **`new FMLModContainer(info, entrypoints, scanData, layer)` 正常返回**：
>   `net.neoforged.fml.javafmlmod.FMLModContainer@1112d23f`
> - 构造后立即抛出观测桩，流程停在容器构造之后；`constructMod()` 未被调用、
>   ForgeProbe 未被实例化（`FORGE PROBE CONSTRUCTOR REACHED` 零命中）
> - 清理后 15/15 条目与 001b-1 基线一致
>
> 按 §九 属**情况 A**。**不进入 001c-2**，报告原样交回。

---

## 一、实验环境

同 001c-0：Minecraft 1.21.1 / NeoForge 21.1.256 / FancyModLoader 4.0.45 /
Java 21（OpenJDK 21.0.10，Microsoft Build）/ Windows 11 amd64 / CLI Dedicated Server /
编译依赖 Forge 1.21.1-52.1.0。运行标签 `spike001c-1`。

---

## 二、前置配置确认

- [x] `IModLanguageLoader` observer 已就位（`name=javafml`，`version=9999`）
- [x] `ForgeModFileInfoParser.build()` 返回真实 `ModFileInfo`
      （`new ModFileInfo(modFile, ForgeModFileInfo.configOf(parsed), info -> { })`）
- [x] `META-INF/services/...IModLanguageLoader` 已声明该 provider

> 三项均来自 001b-6 已验证路径，**不是本 Spike 的实验变量**。

---

## 三、FMLModContainer 构造签名（001c-1-a）

```
class 全名:        net.neoforged.fml.javafmlmod.FMLModContainer
父类:              net.neoforged.fml.ModContainer
构造函数完整签名:  public FMLModContainer(net.neoforged.neoforgespi.language.IModInfo,
                                          java.util.List<java.lang.String>,
                                          net.neoforged.neoforgespi.language.ModFileScanData,
                                          java.lang.ModuleLayer)
构造函数可见性:    **public**
是否有多个重载:    **否**（仅此一个）
```

**参数类型核对表**（说明书期望 vs 实际）：

| 参数 | 说明书期望 | 实际类型 | 一致 |
|---|---|---|---|
| info | `IModInfo` | `net.neoforged.neoforgespi.language.IModInfo` | ✅ |
| entrypoints | `List<String>` | `java.util.List<java.lang.String>` | ✅ |
| scanData | `ModFileScanData` | `net.neoforged.neoforgespi.language.ModFileScanData` | ✅ |
| layer | `ModuleLayer` | `java.lang.ModuleLayer` | ✅ |

附带 `[F]`：`constructMod()` 为 **`protected`**（本 Spike 未调用）；
另有 `public IEventBus getEventBus()`。

**PASS-001c-1-a ✅**

---

## 四、entrypoint 提取（001c-1-b）

```
annotations.size():             1
matched Forge @Mod annotations: 1
entrypoints list:               [com.example.forgeprobe.ForgeProbe]
```

**PASS-001c-1-b ✅**

---

## 五、FMLModContainer 构造（001c-1-c）

- entered: **YES**
- construction result: **PASS**（构造函数正常返回，未抛异常）
- container = `net.neoforged.fml.javafmlmod.FMLModContainer@1112d23f`

**stdout 原文（不截断）**：

```
[FORGECOMPAT][001c-1] ============================
[FORGECOMPAT][001c-1] scanData  = non-null
[FORGECOMPAT][001c-1] annotations.size() = 1
[FORGECOMPAT][001c-1]   [1] entrypoint = com.example.forgeprobe.ForgeProbe
[FORGECOMPAT][001c-1] matched Forge @Mod annotations = 1
[FORGECOMPAT][001c-1] entrypoints = [com.example.forgeprobe.ForgeProbe]
[FORGECOMPAT][001c-1] ============================
[FORGECOMPAT][001c-1] attempting FMLModContainer construction
[FORGECOMPAT][001c-1] FMLModContainer constructed: net.neoforged.fml.javafmlmod.FMLModContainer@1112d23f
java.lang.UnsupportedOperationException: 001c-1 boundary: FMLModContainer constructed but constructMod() not invoked
```

**PASS-001c-1-c ✅**

---

## 六、清理确认（001c-1-d）

| 项 | 结果 |
|---|---|
| `ForgeCompatLanguageLoader` 已删除 | ✅（源文件 + `.class` + service 声明） |
| `ForgeModFileInfoParser` 已恢复基线 | ✅ |
| `IModLanguageLoader` service 声明已删除 | ✅ |
| `IModFileReader` / `IModFileCandidateLocator` service 与基线一致 | ✅ |
| `MANIFEST` 与基线一致 | ✅ |
| `forgeprobe.jar` 未修改 | ✅（`a93a823c…374e22`） |

**逐条目比对：entries = 15 / diff = 0。 PASS-001c-1-d ✅**

---

## 七、Failure Chain

```
Discovery:                    YES（沿用 001b-6）
Metadata:                     YES（沿用 001b-6）
Validation:                   YES（沿用 001b-6）
Scan:                         YES（沿用 001c-0）
LanguageLoader:               YES（loadMod 被调用）
Entrypoint extraction:        **PASS**
FMLModContainer construction: **PASS**
Forge @Mod constructor:       **NOT ENTERED**
```

> 纪律：`FMLModContainer construction`（`new FMLModContainer(...)`）与
> `Forge @Mod constructor`（`new ForgeProbe(...)`）**不可混称 "constructor"**。

---

## 八、完整日志

- latest.log: `D:\DualForge\spike000\logs\spike001c-1\spike001c-1.log`
- debug.log: `D:\DualForge\spike000\logs\spike001c-1\spike001c-1.debug.log`
- stdout: `D:\DualForge\spike000\logs\spike001c-1\spike001c-1.stdout.log`（含 §五 原文）
- stderr: `D:\DualForge\spike000\logs\spike001c-1\spike001c-1.stderr.log`
- crash-report: **不存在**
- meta.json: `EXITED_EARLY`，`exitCode = 0`，`durationSeconds = 12.1`

---

## 九、事实标签

### [F] Source Fact

1. `[F]` `FMLModContainer` 有**唯一**构造函数，且为 **public**：
   `FMLModContainer(IModInfo, List<String>, ModFileScanData, ModuleLayer)`。
2. `[F]` `FMLModContainer.constructMod()` 为 `protected`；`getEventBus()` 为 public。
3. `[F]` `FMLModContainer` 字段含 `ModFileScanData scanResults`、`IEventBus eventBus`、
   `List<Class<?>> modClasses`、`Module layer`。

### [E] Experimental Fact

1. `[E]` 从 `scanData.getAnnotations()` 按 `annotationType == net.minecraftforge.fml.common.Mod`
   筛选，得到 **1** 条，`clazz()` = `com.example.forgeprobe.ForgeProbe`。
2. `[E]` **`new FMLModContainer(info, entrypoints, scanData, layer)` 正常返回**，
   `toString` = `net.neoforged.fml.javafmlmod.FMLModContainer@1112d23f`。
3. `[E]` 构造过程中**未抛出任何异常**（`catch (Throwable)` 分支未被触发）。
4. `[E]` 构造后立即抛观测桩；`constructMod()` **未被调用**；
   `FORGE PROBE CONSTRUCTOR REACHED` **零命中** ⇒ ForgeProbe 未被实例化，边界未越界。
5. `[E]` 清理后 15/15 条目与 001b-1 基线一致。

### [H] Hypothesis

1. `[H]` `FMLModContainer` 构造函数**未解析 entrypoint 类的字节码**
   （否则 `ForgeProbe` 的 Forge 命名空间参数类型应在此处暴露为错误）。
   本轮只观测到"构造成功"，未观测其内部行为。
2. `[H]` entrypoint 的类解析与 `@Mod` 解释很可能发生在 `constructMod()` 内，
   因此 ABI 断层（若存在）应出现在 **001c-2** 而不是本 Spike。
3. `[H]` `getEventBus()` 返回的 `net.neoforged.bus.api.IEventBus` 与 Forge 的
   `net.minecraftforge.eventbus.api.IEventBus` 不是同一类型 —— 属 002 范围，本轮未触碰。

### [D] Design Decision

1. `[D]` **容器层不构成障碍**：Forge 注解数据 + 公开构造函数即可产出 `FMLModContainer`
   实例，无需修改 NeoForge 核心、无需 annotation shim、无需反射。
2. `[D]` 边界确认：`FMLModContainer` 的构造与 `constructMod()` **必须分开评估**；
   本轮只完成了前者。
3. `[D]` 001c-2 的范围应由架构决策方依据 §十 结论上限裁定。

---

## 十、结论

- entrypoints 提取: **PASS**
- FMLModContainer 构造: **PASS**
- 本 Spike 整体结果: **PASS**

**结论上限（禁止超出）**：

> `[E]` 使用 001c-0 已获得的 `ModFileScanData` 与提取出的 Forge entrypoint，
> 在真实 `loadMod()` 中尝试构造 `FMLModContainer`，**结果 = PASS**（构造函数正常返回）。
>
> `[D]` 仅根据上述结果决定 001c-2 的范围。

**禁止从本 Spike 推导**（本报告均未主张）：ForgeProbe 已实例化 /
Forge `@Mod` 已被 NeoForge 解释 / Forge constructor ABI 已可用 /
Forge Mod 已经能够被 NeoForge 加载 / Forge `IEventBus` / `FMLJavaModLoadingContext` 已兼容。

**Confidence**: **HIGH**

**Evidence**: §五 的 stdout 原文；§六 的 15/15 逐条目一致；边界检查零命中。

**Hypotheses still unverified**: §九 `[H]` 1–3。

---

## 十一、边界确认

- [x] 未调用 `constructMod()`
- [x] 未调用 ForgeProbe 构造函数（`FORGE PROBE CONSTRUCTOR REACHED` 零命中）
- [x] 未注入 Forge `IEventBus` 或 `FMLJavaModLoadingContext`
- [x] 构造成功后立即抛出观测桩
- [x] 未进入 001c-2

---

## 十二、禁止事项确认

- [x] 未修改 NeoForge 核心
- [x] 未修改 `ModSorter` / `LoadingModList` / `ModInfo` / `ModFile` / `ModFileInfo` / `ModFileScanData`
- [x] 未修改 `FMLModContainer` / `LanguageProviderLoader` / `VersionSupportMatrix`
- [x] 未修改 `forgeprobe.jar` / `mods.toml`
- [x] 未引入 ASM / Mapping / Mixin / Registry / Event / Isolation
- [x] 未自研独立 Scanner
- [x] 未实现 annotation shim
- [x] 未使用 Unsafe / 字节码注入 / ClassLoader hack / 反射绕过
- [x] 未修改 `FMLModContainer` 构造参数以让它通过（按实际 public 签名调用）
- [x] 未进入 001c-2
- [x] 未提出修复方案
