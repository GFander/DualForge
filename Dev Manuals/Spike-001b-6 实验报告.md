# Spike-001b-6 实验报告

> **结论（先行）**
>
> **两道 checkcast 全部通过。001b-6 整体 PASS。**
>
> - `ModSorter.sort` 的 `ModInfo::cast` —— **PASS**（`stderr` 零字节，无 CCE）
> - `LoadingModList.<init>` 的 `ModFileInfo::cast` —— **PASS**
>
> 且流程**继续推进越过两道门**，直到 `ModLoader.buildModContainerFromTOML`
> **真实调用了 `IModLanguageLoader.loadMod(...)`**；本次终止仅因**我方观测桩**抛
> `UnsupportedOperationException("Observation only")`，**不是** NeoForge 侧的新契约约束。
>
> **清理（001b-6-3）**：15/15 JAR 条目与 001b-1 基线一致。
>
> 按 §九/§十：**不进入 001b-2 / 001c**，报告原样交回。

---

## 一、实验环境

同 001b-5：

```
Minecraft:  1.21.1      NeoForge:  21.1.256      FancyModLoader:  4.0.45
Java:       21（OpenJDK 21.0.10，Microsoft Build）
OS:         Windows 11 amd64       启动方式: CLI / Dedicated Server
编译依赖 Forge: 1.21.1-52.1.0
```

运行标签：`spike001b-6`。

---

## 二、实验目标

验证兼容层能否通过**正常 Java API** 构造出同时满足两道类型契约的
NeoForge 原生 `ModFileInfo` / `ModInfo`，并让两道 `checkcast` 均通过：

```
ModSorter.sort(ModSorter.java:112)              → (ModInfo) imodInfo
LoadingModList.<init>(LoadingModList.java:54-58) → ModFileInfo.class::cast
```

**只通过一道 ≠ 本 Spike PASS。**

---

## 三、源码契约（001b-6-0）

**ModFileInfo**：

```
完整构造函数签名:
  public ModFileInfo(ModFile owningFile, IConfigurable config, Consumer<IModFileInfo> consumer)
  public ModFileInfo(ModFile owningFile, IConfigurable config, Consumer<IModFileInfo> consumer,
                     List<IModFileInfo.LanguageSpec> languageSpecs)
参数逐个说明:
  (1) ModFile         —— 具体类，非 IModFile
  (2) IConfigurable   —— 文件级配置（modLoader / loaderVersion / license / mods 列表）
  (3) Consumer<IModFileInfo> —— 构造期回调，以「正在构造的 ModFileInfo」为参数（本 Spike 传空实现）
  (4) List<LanguageSpec>     —— 仅第二重载；显式指定语言规格
构造函数内部 new ModInfo:  **YES**
  → 位于 private IModInfo lambda$new$2(IConfigurable)
  → 即对 config 中的**每个 mod 条目**执行 new ModInfo(this, entryConfig)
对 IConfigurable 的隐含需求:
  · modLoader / loaderVersion 缺失 → InvalidModFileException("Missing ModLoader version in file")
  · license 缺失或 blank → InvalidModFileException("Missing license")
  · 每个 [[mods]] 条目需含 modId（ModInfo 侧必需）
  · 实测 MapConfigurable 形态的自定义实现**未被拒绝**（见 §五）
```

**ModFileInfo 的第三个参数**：

| 项 | 值 |
|---|---|
| 函数式接口全名 | `java.util.function.Consumer` |
| 方法签名 | `void accept(IModFileInfo)` |
| 可被 lambda 实现 | **YES**（`info -> { }` 已编译并运行通过） |

**LoadingModList.\<init\> 对 ModFileInfo 的其他访问**：

| 访问 | 位置 |
|---|---|
| `ModFileInfo.class::cast` | `invokedynamic #1`（行 55） |
| `ModFileInfo.getFile()` | 紧随 cast 之后（`invokevirtual #144`） |
| 字段 `Map<String, ModFileInfo> fileById` | 构造器后半段（键为 modId） |

> 即：除 cast 外，仅额外要求 `getFile()` 可用 —— 由真实 `ModFileInfo` 天然满足。

**PASS-001b-6-0：✅ 五项均已从精确字节码取得，无 UNKNOWN。**

---

## 四、构造探针（001b-6-1）

探针：`D:\DualForge\spike001b5\probe\ModInfoContractProbe.java`（独立目录，**不参与 jar 打包**），
新增 `b6_fullChain(...)` 覆盖完整链：

```java
SecureJar secureJar = SecureJar.from(jar);
ModFile modFile = new ModFile(secureJar, parser, ModFileDiscoveryAttributes.DEFAULT);
ModFileInfo modFileInfo = (ModFileInfo) modFile.getModFileInfo();     // ← 编译期即要求可强转
if (modFileInfo == null) throw new AssertionError("ModFileInfo is null");
for (IModInfo info : modFileInfo.getMods()) {
    if (!(info instanceof ModInfo)) throw new AssertionError("Not a ModInfo: " + info.getClass());
}
```

```
编译结果:                      **PASS**（javac --release 21 exit 0，无 warning / error）
失败原因:                      无
需要的不可访问类型:            无
ModFileInfo 是否可从 ModFile 直接取得:  YES（编译期即可强转，运行期见 §五）
getMods() 返回的 ModInfo 来源:  **ModFileInfo 内部生成**（lambda$new$2）
```

**PASS-001b-6-1：✅**

---

## 五、运行时验证（001b-6-2）

**实现方式**（临时，已撤销）：`ForgeModFileInfoParser.build()` 由返回自有 `ForgeModFileInfo`
改为返回**真实 `ModFileInfo`**：

```java
IConfigurable cfg = ForgeModFileInfo.configOf(parsed);          // 文件级 config（含 mods 列表）
ModFileInfo nativeInfo = new ModFileInfo(modFile, cfg, info -> { });
return nativeInfo;
```

> 未修改 `ForgeModReader`：`IModFile.create(secureJar, parser)` 的返回值**本身就是 `ModFile`**
> （`instanceof ModFile` 在运行期成立），因此无需替换为 `new ModFile(...)`。

**临时 language provider**：`name = javafml`，`version = 9999`，**实例化 YES**（`INSTANTIATED #1`）。

**观察 1 —— 真实 ModFileInfo / ModInfo 产出**：

```
[FORGECOMPAT] PARSER returned REAL net.neoforged.fml.loading.moddiscovery.ModFileInfo mods=[forgeprobe (Forge Probe) @ 1.0.0]
```

即 `ModFileInfo` 构造器内部（`lambda$new$2`）**确实生成了真实 `ModInfo`** —— §3.8.4 的 `[H]` **成立**。

**ModSorter**：

- entered: **YES**
- `ModInfo::cast`: **PASS**
- 若 FAIL，完整 stack trace: **不适用（未失败）**

**LoadingModList**：

- entered: **YES**
- `ModFileInfo::cast`: **PASS**
- 若 FAIL，完整 stack trace: **不适用（未失败）**

**直接证据**：本次运行 **`stderr` 为 0 字节**，
`latest.log` / `debug.log` 中**不存在任何 `ClassCastException`**
（对照 001b-3 的 `…ForgeModInfo → ModInfo` 与 001b-5 的 `…ForgeModFileInfo → ModFileInfo` 均已消失）。

**第一个新失败点**：

```
stage:      Container 构建（ModLoader.gatherAndInitializeMods → buildMods → buildModContainerFromTOML）
exception:  java.lang.UnsupportedOperationException: Observation only
            ← **由本 Spike 的观测桩抛出**，非 NeoForge 契约问题
call chain:
  at LAYER SERVICE/forgecompat/com.example.forgecompat.ForgeCompatLanguageLoader.loadMod(ForgeCompatLanguageLoader.java:33)
  at MC-BOOTSTRAP/fml_loader@4.0.45/net.neoforged.fml.ModLoader.buildModContainerFromTOML(ModLoader.java:324)
  at MC-BOOTSTRAP/fml_loader@4.0.45/net.neoforged.fml.ModLoader.lambda$buildMods$10(ModLoader.java:311)
  at MC-BOOTSTRAP/fml_loader@4.0.45/net.neoforged.fml.ModLoader.buildMods(ModLoader.java:317)
  at MC-BOOTSTRAP/fml_loader@4.0.45/net.neoforged.fml.ModLoader.gatherAndInitializeMods(ModLoader.java:131)
  at TRANSFORMER/neoforge@21.1.256/net.neoforged.neoforge.internal.CommonModLoader.begin(CommonModLoader.java:49)
  at TRANSFORMER/neoforge@21.1.256/net.neoforged.neoforge.server.loading.ServerModLoader.load(ServerModLoader.java:34)
  at TRANSFORMER/minecraft@1.21.1/net.minecraft.server.Main.main(Main.java:124)
  …（其后为 ModLauncher / BootstrapLauncher 栈）
```

**关键含义**：`IModLanguageLoader.loadMod(IModInfo, ModFileScanData, ModuleLayer)` **被真实调用**，
传入的 `IModInfo` 即本 Spike 产出的真实 `ModInfo`。
**这已越过 Module 阶段的门槛**（Container 构建已开始）。

**附带观测**：栈帧显示 `LAYER SERVICE/forgecompat/...` —— ModLauncher 把兼容层 JAR
渲染为 **SERVICE** 层，与 001b-3 的对象同一性判定一致。

进程退出码 **0**，未达 `Done`，未生成崩溃报告（`logs/spike001b-6/` 下无 crash-report）。

> 按 §六：「两道 cast 均通过但出现新异常 → 记录 first confirmed failure，STOP」。
> 此处新异常**来自观测桩**，非 NeoForge 侧硬约束，已在 §十 如实归类。

---

## 六、清理确认（001b-6-3）

| 项 | 结果 |
|---|---|
| `ForgeCompatLanguageLoader` 已删除 | ✅（源文件 + `.class` + service 声明） |
| `IModLanguageLoader` service 声明已删除 | ✅ |
| `IModFileReader` service 与基线一致 | ✅ |
| `IModFileCandidateLocator` service 与基线一致 | ✅ |
| `MANIFEST` 与基线一致 | ✅ |
| `ForgeModReader.class` 与基线一致 | ✅ |
| `ForgeModFileInfoParser.class` 与基线一致 | ✅ |
| `ForgeModFileInfo.class` 与基线一致 | ✅ |
| `forgeprobe.jar` 未修改 | ✅（SHA-256 `a93a823c…374e22`） |

**逐条目比对：entries = 15 / diff = 0**（对照 `build\forgecompat-001b1.jar`）。

```
001b-1 基线整体 SHA-256 : 14efd54fa91e415c5815972b81d16c2895dd9ed52a427154db1261efa532e334
清理后整体 SHA-256      : 01580869010f707f6f708f6f7f24acd304921856741c1903195e5ba9cbf55353
```

整体哈希未恢复（仅 ZIP 容器元数据差异）；按「内容一致」标准**判定通过**。

---

## 七、Failure Chain

```
Discovery:       YES
Metadata:        YES
Validation:      YES
Scan:            UNKNOWN
LanguageLoader:  YES（findLanguage 成功返回；9999 通过 [52,) 校验）
ModSorter:       YES（**cast 通过**）
LoadingModList:  YES（**cast 通过**）
Module:          YES（已越过；未观测到 Game Layer 相关行为，仅证明未在 Module 处被拦）
Container:       YES（`ModLoader.buildModContainerFromTOML` 已执行）
Constructor:     NO（`loadMod` 被调用后即被观测桩终止，未进入 `@Mod` 构造）
```

> 纪律：`ModSorter 被调用 ≠ cast 通过`，`LoadingModList 被调用 ≠ cast 通过` ——
> 本轮由 `stderr` 零字节 + 流程推进到 `loadMod` 双重佐证**两道 cast 均通过**。

---

## 八、完整日志

- latest.log: `D:\DualForge\spike000\logs\spike001b-6\spike001b-6.log`
- debug.log: `D:\DualForge\spike000\logs\spike001b-6\spike001b-6.debug.log`
- stdout: `D:\DualForge\spike000\logs\spike001b-6\spike001b-6.stdout.log`
- stderr: `D:\DualForge\spike000\logs\spike001b-6\spike001b-6.stderr.log`（**0 字节**）
- crash-report: **不存在**
- meta.json: `status = EXITED_EARLY`，`exitCode = 0`，`durationSeconds = 12.1`

---

## 九、事实标签

### [F] Source Fact

1. `[F]` `ModFileInfo` 有 2 个 public 构造函数，`owningFile` 参数为**具体类 `ModFile`**。
2. `[F]` `ModFileInfo` 构造器**内部**通过 `lambda$new$2(IConfigurable)` 逐个 mod 条目
   执行 `new ModInfo(this, config)` —— **这是 NeoForge 唯一的 `ModInfo` 实例化点**。
3. `[F]` `ModFileInfo` 的第三个参数是 `java.util.function.Consumer<IModFileInfo>`，
   `void accept(IModFileInfo)`，可被 lambda 实现。
4. `[F]` `LoadingModList.<init>` 除 `ModFileInfo.class::cast` 外，仅额外调用 `ModFileInfo.getFile()`；
   另有字段 `Map<String, ModFileInfo> fileById`。
5. `[F]` `ModFileInfo` 构造器对 config 的硬性要求：`modLoader` / `loaderVersion` / `license`
   （缺失抛 `InvalidModFileException`）；每个 `[[mods]]` 条目需有 `modId`。

### [E] Experimental Fact

1. `[E]` **完整链编译通过**：`ModFile` → `(ModFileInfo) modFile.getModFileInfo()` → `getMods()` →
   `instanceof ModInfo` 断言，`javac exit 0`。
2. `[E]` **两道 checkcast 均通过**：本次运行 `stderr` 为 **0 字节**，
   两份日志中**零 `ClassCastException`**。
3. `[E]` 兼容层自定义的 `MapConfigurable` 形态 `IConfigurable` **被 `ModFileInfo` 接受**，
   未触发任何 `InvalidModFileException`。
4. `[E]` **§3.8.4 的 `[H]` 成立**：走真实 `ModFileInfo` 路径**一次越过两道 cast**。
5. `[E]` 流程推进到 `ModLoader.buildModContainerFromTOML`，
   **`IModLanguageLoader.loadMod(...)` 被真实调用**（`[FORGECOMPAT][LANG] loadMod called for forgeprobe`）。
6. `[E]` 栈帧渲染 `LAYER SERVICE/forgecompat/...`，与 001b-3 的 SERVICE 判定一致。
7. `[E]` 清理后 15/15 条目与 001b-1 一致。

### [H] Hypothesis

1. `[H]` 若观测桩不在 `loadMod` 抛出，下一步将进入 `FMLModContainer` 构造与
   Forge `@Mod` entrypoint 解析 —— 属 **001c** 范围，本轮未验证。
2. `[H]` `ModFileInfo` 构造器的第三个参数（`Consumer<IModFileInfo>`）在 NeoForge 自身路径中
   承担何种职责（本 Spike 传空实现即可通过），未深究。
3. `[H]` `LoadingModList` 之后是否还有第三道同型（具体类）cast，仍未验证。

### [D] Design Decision

1. `[D]` **Plan A 的类型契约路径已证明可行**：兼容层可通过正常 Java API
   构造 NeoForge 原生 `ModFile` / `ModFileInfo` / `ModInfo`，并让 Discovery → ModSorter →
   LoadingModList → Container 构建全链接受。
2. `[D]` 这也意味着 `ForgeModFileInfo` / `ForgeModInfo` 作为**兼容层自有实现**的定位需要修正
   （§3.8.8 已记录"删除或降级为工具类"）。
3. `[D]` 手册 §3.8.4 的 `[H]` 具备升级为 `[E]` 的证据；是否回写由架构决策方裁定。

---

## 十、First Confirmed Failure

**阶段**: Container 构建（`ModLoader.buildModContainerFromTOML` → `loadMod`）

**证据**: `latest.log` 中 `java.lang.UnsupportedOperationException: Observation only`，
栈帧指向 `com.example.forgecompat.ForgeCompatLanguageLoader.loadMod(…:33)`

**异常**: `java.lang.UnsupportedOperationException`（**本 Spike 观测桩主动抛出**）

**调用链**: 见 §五 完整 stack trace

**原因**: **非 NeoForge 契约约束**。观测桩的 `loadMod` 按 Spike_001b-5.md §001b-5-2 的要求
以 `throw new UnsupportedOperationException("Observation only")` 实现，用于标记观测边界。
→ 本轮**未暴露任何 NeoForge 侧的新硬约束**。

---

## 十一、结论

| 判据 | 结果 |
|---|---|
| PASS-001b-6-0（契约取证） | ✅ |
| PASS-001b-6-1（编译探针） | ✅ |
| **PASS-001b-6-A（`ModInfo::cast`）** | **✅ PASS** |
| **PASS-001b-6-B（`ModFileInfo::cast`）** | **✅ PASS** |
| PASS-001b-6-3（清理一致） | ✅ |

**两道 cast 均通过: YES ⇒ 001b-6 整体 PASS。**

**第一个新失败点**: Container 构建阶段，由观测桩主动抛出的 `UnsupportedOperationException`
（非 NeoForge 硬约束）。

**Confidence**: **HIGH**

**Evidence**:

1. `stderr` 0 字节；两份日志零 `ClassCastException`（对照 001b-3 / 001b-5 的两次 CCE 均已消失）。
2. `PARSER returned REAL net.neoforged.fml.loading.moddiscovery.ModFileInfo mods=[forgeprobe (Forge Probe) @ 1.0.0]`。
3. 流程推进至 `ModLoader.buildModContainerFromTOML` 并**真实调用 `loadMod`**。
4. 编译探针 `javac exit 0`；清理后 15/15 条目与 001b-1 一致。

**Hypotheses still unverified**: §九 `[H]` 1–3。

---

## 十二、禁止事项确认

- [x] 未修改 `ModSorter`
- [x] 未修改 `LoadingModList`
- [x] 未修改 `ModInfo` / `ModFile` / `ModFileInfo`
- [x] 未修改 NeoForge 核心（仅**调用**其 public 构造/API）
- [x] 未修改 `forgeprobe.jar`（SHA-256 `a93a823c…374e22`）
- [x] 未修改 `mods.toml`
- [x] 未引入 ASM / Mapping / Mixin / Registry / Event / Isolation
- [x] 未使用 Unsafe / 字节码注入 / ClassLoader hack
- [x] 未把非常规路径冒充正常路径（本轮全程正常 Java 路径）
- [x] 未进入 001b-2
- [x] 未进入 001c
- [x] 未提出修复方案（未"顺手修复"新失败点）
