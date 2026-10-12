# Spike_002-0d 执行任务说明书 —— G-1 前置修复（兼容 provider 对非 Forge 容器的接管行为）

**接收方**：Codex（执行方）
**编制**：架构决策方（裁判）
**状态**：**v1.0 冻结版（已交付）**——经顾问方审阅（7 处小修）＋架构决策方裁定"采纳并冻结"；冻结后执行期不得变更
**审阅链**：顾问方反向意见（已出：7 处小修）→ 用户/母代理确认（已确认）→ 决策方发布 v1.0 冻结版（本版）→ 交执行方
**日期**：2026-10-07

**v0.1 → v1.0 变更记录（7 项；依据《Spike-002-0d 审阅意见（顾问方·七处小修）》§七）**：

1. §五 5.2：代码块前补充 `applyForEach` 返回类型标注（`Stream<IModLanguageLoader>`；无需 `.stream()`）；
2. §0.4 + §六：追加"委托后内置 validate 恢复运行"为已知副作用/观测项第 10 条；
3. §五 5.1：明示委托分支以 `return` 结束、不 fall through；
4. §2D-c 表 1：`loadMod called for neoforge entrypoints=[]` 放宽为"至少 1 行"；
5. §七 PASS 第 4 条：操作化为 `[E-1]`（FATAL 完全缺席）/ `[E-2]`（越过信号原文行）；
6. §0.2 表 4：标签降级为"API 签名 `[F]`；跨层可访问性 `[H]`"；
7. §八 分界点主表：以"G-1 是否清零"为唯一标准，覆盖边界情形（含"前置漂移"）。

> 注：本版由总指挥按已裁定内容机械落实（原裁判实例核实修订时卡死、0 输出；内容依据《审阅意见》§七 逐项）。

**依据**：

- 《Spike-002-0c 裁判裁定修订（反向意见处理）》§三 3.2（002-0d 强制要件）
- 《Spike-002-0c G-1 四项前置取证（执行方）》（经裁判独立复核）
- 《Spike-002-0c 裁判审查与裁定报告》F2 / 复核增补 F2-a / F2-b
- 《Forge → NeoForge 兼容层开发手册 v1.1》§1.2（低侵入优先）/ §3.8.10
- 《Spike_002-0b.md 母说明书》

**附带文件（同目录，供执行方读取）**：

- `D:\DualForge\Dev Manuals\Spike-002-0c 裁判裁定修订（反向意见处理）.md`
- `D:\DualForge\Dev Manuals\Spike-002-0c G-1 四项前置取证（执行方）.md`
- `D:\DualForge\spike000\logs\spike002-0c-artifacts\`（5 份 javap / metadata 原文）

**任务性质**：单因子前置修复 + K1 对照运行（control + treatment）
**唯一变量**：兼容 provider（`ForgeCompatLanguageLoader`）对**非 Forge 容器**的 `loadMod` 接管行为——在"无 Forge `@Mod` entrypoints"时委托内置 `javafml` 实例
**执行上限**：完整启动（Done）或第一个新断层，二者之一出现即停止观测（新断层不修复）

---

## 〇、背景（母说明书：为什么拆出 002-0d）

### 0.1 G-1 的定义（已裁定）

Plan A 同名 `javafml` 覆盖使兼容 provider 占据 `languageProviderMap["javafml"]`，同时接管 `neoforge` **系统容器**（`META-INF/neoforge.mods.toml` 声明 `modLoader="javafml"`）。兼容 provider 的 `loadMod` 仅收集 `net.minecraftforge.fml.common.Mod` 注解类；对 NeoForge 格式容器返回 `entrypoints=[]` 的 `FMLModContainer` → `NeoForgeMod` 从未被构造 → `NeoForgeRegistriesSetup.setup` 从未执行 → 11 项注册表 created but not registered → `Registry initialization` 失败、进程终止。

**分类**：前置缺陷（Plan A 接管范围问题），**不是** Forge↔NeoForge ABI 断层，**与 ForgeProbe 无关**（变量 = 0 仍发生；层 e marker 在失败前正常命中）。

### 0.2 方向 (c) 的依据（四项取证 + 裁判复核）

| # | 取证结论（`[F]`） | 对方向的意义 |
|---|---|---|
| 1 | `neoforge` 容器声明 `modLoader="javafml"` | G-1 归因链前提闭合 |
| 2 | 内置 `FMLJavaModLanguageProvider.loadMod` = 纯注解筛选链 + `FMLModContainer`，无 mixin / 事件总线 / 其它副作用 | "复刻副作用"顾虑排除；(a') 范围可量化 |
| 3 | `languageProviders`（List）保留全部发现实例；`languageProviderMap.put` 仅覆盖 map | 内置 javafml 实例仍在 list 中 |
| 4 | `FMLLoader.getLanguageLoadingProvider()`（public static）+ `forEach/applyForEach`（public，均遍历 list） | (c) 委托存在公开路径（API 签名 `[F]`；跨层可访问性 `[H]`，待本 Spike 2D-e 运行层验证） |

**复核增补（裁判）**：

- F2-a：compat 现行 `loadMod` 对空 entrypoints **仍构造容器**（G-1 机制面）；且与内置链相比缺 `modId 过滤 / dist 过滤 / 侧数排序` 三处语义 → 委托可避免第二实现漂移；
- F2-b：`validate` 按 `IModInfo.getLoader()` 分组调用；Plan A 下内置 validate 不运行（兼容 provider 用接口默认 no-op）→ **记录为观测项，不在本 Spike 修复范围**。

### 0.3 为什么在 002-0e 之前

002-0e（方向 2 探针 #1 = `MinecraftForge.EVENT_BUS.register(this)`）要求一个**有效基线**：G-1 未修复前，任何"生命周期推进"观测都被主机自身截断（`[E]`：002-0c 与 v1.1 两轮同败）。002-0d 修复后重建零变量基线，002-0e 才有判定意义。

### 0.4 本 Spike 不解决

- 不修复 `validate` 归属问题（F2-b，已明确范围外）；
- 不在本 Spike 评估「委托后内置 validate 恢复运行」的行为变化（属下游观测项）；
- 不修复任何 ABI 断层（含 002-0e 的探针目标）；
- 不采用 Plan B（语言名解耦 / 改名）；
- 不引入 Forge API 调用、不改变探针、不进入方向 2；
- 不设计后续 Spike。

### 0.5 与 002-0c / 002-0e 的职责分离

| 项目 | 002-0c | 002-0d（本 Spike） | 002-0e |
|---|---|---|---|
| 目标 | G-1 条件下的生命周期事实基线 | 清零 G-1，重建有效基线 | 方向 2 首个断层坐标 |
| 变量 | 0 | 1（委托分支） | 1（新增 register 语句） |
| 运行 | 1 | 2（control + treatment） | 1 |
| 判据 | PASS / VALID / STOP / FAIL | 本文件 §七 | 后续裁定 |

---

## 一、唯一目标与执行上限

**唯一目标**：修复 G-1——使 `neoforge` 系统容器在兼容 provider 覆盖下仍获得其真实 entrypoint（`NeoForgeMod` 被构造），FML 越过 `Registry initialization`。

**执行上限**：treatment 运行到达 **Done** 或**第一个新断层**即停止观测；**新断层不修复**，原样记录后交回裁定。

**不判定**：兼容层是否可行 / ABI 是否兼容 / 新断层性质 / 方向 2 API 选择。

---

## 二、单变量定义与 K1 设计

### 2.1 唯一的变量

仅允许变更：**兼容 provider 对非 Forge 容器的 `loadMod` 接管行为**（即 §五 委托分支）。变量落地形态 = `ForgeCompatLanguageLoader.loadMod` 中"entrypoints 为空 → 委托内置 javafml 实例"的代码路径。

### 2.2 冻结项（不得变更）

- `forgeprobe.jar`（`A93A…`）、probe `mods.toml`（`B32C…`）；
- 四项前置语义（observer / service / 真实 ModFileInfo / 空壳 IEventBus）——本 Spike 需先**恢复**其源码，恢复本身必须达到行为等价（§四 2D-a）；
- 描述符重写逻辑（`ForgeProbeConstructorTransformer` 语义）；
- NeoForge / FML / ModLauncher / Minecraft / Java 版本；
- `-Dforgecompat.2a.rewrite=true`；
- `run-server.ps1` 除 ValidateSet 追加两个 Label 外的全部逻辑。

### 2.3 K1 对照（control + treatment）

| 运行 | jar | 与另一运行的唯一差异 |
|---|---|---|
| Run C（control，先） | `forgecompat-002-0d-control.jar`（恢复源码，**未修复**） | — |
| Run T（treatment，后） | `forgecompat-002-0d.jar`（恢复源码 + §五 委托修复） | 仅 §五 委托分支 |

两个 jar 必须由**同一恢复源码树、同一编译/打包流程**产出；不得使用冻结 v1.1 jar 或旧构建作为任一运行输入（冻结 jar 仅作恢复保真参照与历史签名对照）。

---

## 三、精确环境（冻结）

```
Minecraft:            1.21.1
NeoForge:             21.1.256
FancyModLoader:       4.0.45
ModLauncher:          11.0.5
Java:                 21（ms-21.0.10）
OS:                   Windows 11 amd64
启动方式:             CLI / Dedicated Server
```

路径与冻结引用：

- 冻结 v1.1 参照 jar：`D:\DualForge\spike001a\compat\build\forgecompat-002-0b-2a-v1.1.jar`
  SHA-256 = `E27BB9659303DDEDF2EDE02D10B49CAFC586549C0EDA003A0165DF525F97A436` / 20316 B / 25 条
- 探针 jar：`D:\DualForge\spike000\probe\build\forgeprobe.jar`
  SHA-256 = `A93A823C988BFD6A8F6EA132D189D9DE732B0E5951F4BF74980D51BB63374E22` / 1772 B
- probe `mods.toml`：`B32CF69B88FBE053C2580FC739B30FEDC43D96984C72FF78CD6725C7F3A1CE6E`（3 份副本一致）
- FML loader：`...\fancymodloader\loader\4.0.45\loader-4.0.45.jar`
  SHA-256 = `8390398E67DF23E72D85ED809685BE2752C8EC545B7CBB6AD4B70FDC81E4DCD4` / 505696 B
- 服务端：`D:\DualForge\spike000\neoforge-server\`
- 运行脚本：`D:\DualForge\spike000\run-server.ps1`
- JDK：`C:\Users\Albert\.jdks\ms-21.0.10\bin\`
- 工作副本（建议）：`D:\DualForge\spike001a\compat-002-0d\`——**不得覆盖/删除现有 `compat` 目录内容**；若采用其它布局，须在报告写明并满足同样的归档不变量
- 日志目录：`D:\DualForge\spike000\logs\spike002-0d-control\`、`...\spike002-0d-treatment\`
- 证据目录：`D:\DualForge\spike000\logs\spike002-0d-artifacts\`

Label（追加进 ValidateSet，其余逻辑不得动）：`spike002-0d-control`、`spike002-0d-treatment`

运行命令（K1：先 C 后 T；`<control jar>` / `<treatment jar>` 替换为实际产物路径）：

```
D:\DualForge\spike000\run-server.ps1 -Label spike002-0d-control `
  -ExtraJvmArgs '-Dforgecompat.2a.rewrite=true' `
  -ModsJar D:\DualForge\spike000\probe\build\forgeprobe.jar `
  -ExtraModsJars <control jar>

D:\DualForge\spike000\run-server.ps1 -Label spike002-0d-treatment `
  -ExtraJvmArgs '-Dforgecompat.2a.rewrite=true' `
  -ModsJar D:\DualForge\spike000\probe\build\forgeprobe.jar `
  -ExtraModsJars <treatment jar>
```

---

## 四、子步骤

### 2D-a 恢复前置（不运行）：从冻结 jar 恢复 v1.1 源码并归档

**目标**：在**不改动现有 `compat\src` 基线**的前提下，重建与冻结 jar（`E27B…`）行为等价的 v1.1 源码树。

恢复内容（冻结 jar 全量对应）：

- Java 源（9 个）：`ForgeCompatLanguageLoader`、`ForgeCompatTransformationService`、`ForgeJarMetadata`、`ForgeModFileInfo`（含内部类 `ForgeModInfo` / `MapConfigurable`）、`ForgeModFileInfoParser`、`ForgeModLocator`、`ForgeModReader`、`ForgeProbeConstructorTransformer`、`net/minecraftforge/eventbus/api/IEventBus`
- service（4 个）：`cpw.mods.modlauncher.api.ITransformationService`、`net.neoforged.neoforgespi.language.IModLanguageLoader`、`net.neoforged.neoforgespi.locating.IModFileCandidateLocator`、`net.neoforged.neoforgespi.locating.IModFileReader`
- `META-INF/MANIFEST.MF`

恢复起点（建议，不强制顺序）：

1. 现有 `compat\src` 的 001b-1 基线源**先归档**（到 `compat\archive\src-001b1\`，含逐文件 SHA-256）；本 Spike 全程不得覆盖/删除该基线；
2. v1.1 已文档化差异按《Spike-002-0b Step2-A v1.1》§0.3.1 恢复：

```java
static IConfigurable configOf(Map<String, Object> parsed) {
    return new MapConfigurable(parsed);
}
```

```java
// ForgeModFileInfoParser.build(...) 的返回行（v1.1 形态）：
return new ModFileInfo(modFile, ForgeModFileInfo.configOf(parsed), info -> { });
```

3. 其余类 / service / MANIFEST 从冻结 jar 以 `javap -p -c -constants`（或其它可用工具）逐类重建；不得凭记忆改写语义。

**恢复硬门（全部满足方可进入 2D-b）**：

| # | 硬门 |
|---|---|
| 1 | 内容条目集合一致：11 个 class + 4 个 service + `MANIFEST.MF`（目录条目差异允许，记录实际总数） |
| 2 | 4 个 service 文件与冻结 jar 内文本**逐字节一致** |
| 3 | MANIFEST 含 `Manifest-Version` / `Automatic-Module-Name: forgecompat` / `FMLModType: LIBRARY`，且**不含 `Implementation-Version`**；`Created-By` 为打包工具生成行，允许与冻结 jar 不同（记录实际值） |
| 4 | 逐类 `javap -p -c -constants` 对照：方法/字段签名一致；指令差异必须逐条列出并判定为编译等价形态（常量池索引、lambda 引导、字符串拼接实现等允许差异） |
| 5 | **Run C 复现 G-1 签名（见 2D-c）——最终保真门** |

**源码归档（configOf 教训制度化）**：

- 恢复态源码树（修改前）：`<work>\archive\src-recovered\` + 逐文件 SHA-256 清单；
- 修复态源码树（修改后）：`<work>\archive\src-fixed\` + 逐文件 SHA-256 清单；
- 中间产物（javap 原文、构建命令、条目清单、对照结论）归档至 `logs\spike002-0d-artifacts\`。

**STOP**：恢复不可行 / 硬门 1–3 任一不过 / javap 差异无法解释 → STOP，交回（不得以"近似实现"继续）。

### 2D-b 构建 control jar（未修复）

按既有已验证的 javac + 手工打包流程（参考《Spike-002-0b Step2-A v1.1》§2A-c；Gradle 未安装）：

- 编译工具：`ms-21.0.10` 的 `javac`；classpath 复用 `compat\build\cp.txt`（绝对路径）；
- 输出：`<work>\build-control\classes`（先清空重建）→ `jar-root`（classes + 4 services + MANIFEST）→ `forgecompat-002-0d-control.jar`；
- 记录：编译命令原文、javac / jar 工具版本、产物路径 / SHA-256 / 字节数 / 条目清单。

### 2D-c control 运行（K1 control）

- 命令见 §三（Run C）；
- **期望（与 002-0c 冻结签名逐项一致，仅允许时间戳 / BUS 地址不同）**：

| # | 期望观测 | 对照值（002-0c） |
|---|---|---|
| 1 | `[FORGECOMPAT-LANG] loadMod called for neoforge entrypoints=[]` | **至少 1 行**（对照 002-0c 的 line 69；如实际多次，全量记录） |
| 2 | `FORGE PROBE CONSTRUCTOR REACHED` + `BUS = net.neoforged.bus.EventBus@…` | 命中 |
| 3 | 无 `NeoForge mod loading`；FATAL 同链（`Failed to wait for future Registry initialization, 1 errors found`） | 原文一致 |
| 4 | crash-report：`RegistryManager.postNewRegistryEvent(RegistryManager.java:94)` + `CommonModLoader.lambda$begin$0(CommonModLoader.java:52)` + 11 项注册表清单 | 原文一致 |
| 5 | meta.json：`EXITED_EARLY` / exitCode 0 / duration ≈ 12.1s / doneLine null | 对照 |
| 6 | `REWRITE hits=1`；rewritten dump = `5E2F78DB2B0909D7368A185F618C3D080C00B732279B105CD5B8010FD66D9295` / 1162 B | 一致 |

- **STOP**：任一关键项不符（尤其 entrypoints 行缺失、marker 缺失、复现了不同的失败链）→ 环境漂移或恢复失真，STOP 交回。

### 2D-d 修复（唯一变量；逐字定义见 §五）

- 在恢复源码树上，**仅修改** `ForgeCompatLanguageLoader.loadMod`（按 §五）；
- 修改前后源码已归档（2D-a）；
- 重建 → `forgecompat-002-0d.jar`（treatment）；记录路径 / SHA-256 / 字节数 / 条目清单；
- **强制 javap 对照（control vs treatment）**：除 `ForgeCompatLanguageLoader.loadMod` 的委托分支外，不得有任何语义差异——class 集合一致；其它 class 语义一致；同类其它方法语义一致；`loadMod` 既有段（注解收集、现有日志、非空路径构造）语义一致。

### 2D-e treatment 运行

- 命令见 §三（Run T）；
- **期望观测（顺序敏感）**：

| # | 期望 | 说明 |
|---|---|---|
| 1 | `[FORGECOMPAT-LANG] loadMod called for neoforge entrypoints=[]` | 既有行不变（Forge 扫描仍为空） |
| 2 | `[FORGECOMPAT-LANG] delegate -> builtin javafml for neoforge` | 新增行（§五 逐字） |
| 3 | `NeoForge mod loading` | `NeoForgeMod` 被构造（G-1 清零的直接证据） |
| 4 | 无 G-1 同链 FATAL；生命周期越过 `Registry initialization` | 记录越过后的首个阶段 |
| 5 | 新终点：Done 或第一个新断层（完整 stack 原文） | 不修复 |
| 6 | ForgeProbe 不受影响：marker + BUS；`REWRITE hits=1`；dump 哈希 = `5E2F78DB…` / 1162 B | 冻结项未漂移 |
| 7 | meta.json / crash-report / mods-list 全量记录 | — |

- **STOP（委托机制层）**：出现 `IllegalStateException`（`[FORGECOMPAT-LANG] builtin javafml not unique in languageProviders: candidates=…`，含 0 / >1）、跨层访问错误（`IllegalAccessError` / `NoClassDefFoundError` 指向 loader 类）、递归 / 重复委托、或任何在 `NeoForge mod loading` 之前出现且与委托相关的未预登记异常 → 原样记录，STOP 交回。

### 2D-f 清理与冻结

- 新基线冻结：`forgecompat-002-0d.jar`（treatment）+ 两轮日志 / meta + 修复态源码归档；
- 冻结 v1.1 jar（`E27B…`）保持只读参照地位（历史对照），不得删除；
- 确认：`forgeprobe.jar` / `mods.toml` / 四项前置语义 / 描述符重写 / NeoForge / FML / Java 未变；
- 确认：`run-server.ps1` 仅 ValidateSet 追加 2 个 Label（其余未动，diff 记录）；
- 若 treatment 出现新断层：**不得修复**，原样交回裁定（下一轮 Spike 的候选输入）。

---

## 五、逐字定义（决策方权威）

> 语义逐字定义；实现仅允许为适配恢复后源码做 `import` / 空白换行适配，**语义不得变更**。任何语义偏差必须交回裁定。

### 5.1 路由条件（插入点）

在现有 entrypoints 收集与现有日志行之后、`new FMLModContainer(...)` 之前，插入：

```java
if (entrypoints.isEmpty()) {
    // 5.2 + 5.3
}
return new FMLModContainer(info, entrypoints, scanData, layer);
```

- 非空路径 = 原样（Forge 容器路径字节码语义冻结）；
- 空路径 = 委托分支（非 Forge 容器）。
- **委托分支以 `return` 结束，不 fall through**（5.3 的 `return builtin.loadMod(...)` 即为该分支出口；空 entrypoints 且委托分支执行时，不得再落到 `new FMLModContainer(info, entrypoints, scanData, layer)`）。

### 5.2 委托目标选择（逐字）

// 返回类型: `Stream<IModLanguageLoader>`（`[F]`，来源 `logs\spike002-0c-artifacts\G1-F3F4-LanguageProviderLoader.txt`：`public <T> java.util.stream.Stream<T> applyForEach(java.util.function.Function<IModLanguageLoader, T>)`；现有链式 `.filter(...)` 写法成立，**无需 `.stream()`**；亦无需新增 `Stream` import——代码中无显式类型引用）

```java
List<IModLanguageLoader> candidates = FMLLoader.getLanguageLoadingProvider()
        .applyForEach(l -> l)
        .filter(l -> l != this)
        .filter(l -> "javafml".equals(l.name()))
        .filter(l -> "net.neoforged.fml.javafmlmod.FMLJavaModLanguageProvider".equals(l.getClass().getName()))
        .toList();
if (candidates.size() != 1) {
    throw new IllegalStateException("[FORGECOMPAT-LANG] builtin javafml not unique in languageProviders: candidates=" + candidates.size());
}
IModLanguageLoader builtin = candidates.get(0);
```

> 编制级静态预检（2026-10-07，决策方）：本片段已用 `ms-21.0.10` javac 针对 `compat\build\cp.txt` 的真实 classpath 编译通过（`[F]`；**不作为执行证据**——执行方仍须在恢复源码树上重建并记录）。

语义要点（冻结）：

1. 目标来源 = `languageProviders` list（经 `applyForEach` 遍历）；**不得**使用 `languageProviderMap`（其 `javafml` 条目已被自身覆盖）；
2. 排除自身（`l != this`）；
3. 语言名必须恰为 `javafml`；类名必须恰为 `net.neoforged.fml.javafmlmod.FMLJavaModLanguageProvider`；
4. **每次调用解析一次**（不得缓存为静态 / 实例字段）；
5. 候选数 ≠ 1（找不到 = 0，或出现多个 = >1）→ 抛上述 `IllegalStateException`（fail-loud），不得静默回退为空 entrypoints 容器、不得静默取首项。

### 5.3 委托调用与日志（逐字，顺序冻结）

```java
System.out.println("[FORGECOMPAT-LANG] delegate -> builtin javafml for " + info.getModId());
return builtin.loadMod(info, scanData, layer);
```

- 先打印、后调用；
- 现有 `[FORGECOMPAT-LANG] loadMod called for ...` 行保持原样，位置不变；
- 委托返回值**直接**作为本方法返回值；不得包装 / 转换 / 二次加工。

### 5.4 禁止事项（修复面之外）

- 不得修改 `name()` / `version()` / 构造函数 / `<clinit>`，不得修改其它任何类；
- 不得新增 / 删除 class、service、资源条目；不得改 MANIFEST 属性；
- 不得对非空（Forge 容器）路径做任何变更（含重排日志、改变字符串）；
- 不得使用反射 / `Unsafe`；不得预缓存 builtin 实例；
- 不得修改 `validate` 或新增 validate 委托（F2-b 已列为范围外观测项）；
- 不得修改描述符重写逻辑（`ForgeProbeConstructorTransformer`）。

### 5.5 语法适配边界

- 允许：新增 `import net.neoforged.fml.loading.FMLLoader;`、`import net.neoforged.neoforgespi.language.IModLanguageLoader;`、`import java.util.List;`（若原文件未导入）；空白 / 换行 / 括号布局；
- 不允许：任何"语义等价但结构不同"的改写（例如：`forEach` + 可变容器、先缓存再过滤、改变过滤顺序、改用 map 查找、改用反射）。

---

## 六、必须记录（观测项）

1. 环境冻结项：全部哈希复测值（§三 清单）；
2. 恢复记录：源文件清单 + 逐文件 SHA-256 + 逐类 javap 对照结论 + services 逐字节对照 + MANIFEST 对照；
3. 构建记录：control / treatment 两个 jar 的编译命令、工具版本、路径 / SHA-256 / 字节数 / 条目清单；
4. 字节码对照：冻结 jar vs control（恢复保真）；control vs treatment（单变量封闭，逐条差异清单）；
5. Run C 全量产物与签名对照表（2D-c 表）；
6. Run T 全量产物：日志序列（1–7 项）、新终点、完整 stack（如新断层）、meta.json、crash-report、mods-list；
7. ForgeProbe 冻结证据：marker / BUS / `REWRITE hits` / rewritten dump 哈希与字节数；
8. 边界自检清单（§九）；
9. 与说明书的**逐条偏差**（如有，必须单列一节）；
10. 委托生效后，neoforge 容器的 `IModInfo.getLoader()` 实际指向（预期 = builtin `FMLJavaModLanguageProvider`）。

---

## 七、判据

### 7.1 PASS-002-0d-1（修复生效；未达 Done）

全部满足：

- `[E]` Run C 复现 G-1 签名（2D-c 表 1–6 逐项一致；仅时间戳 / BUS 地址允许不同）；
- `[E]` Run T 出现 `[FORGECOMPAT-LANG] delegate -> builtin javafml for neoforge`；
- `[E]` Run T 出现 `NeoForge mod loading`（`NeoForgeMod` 被构造）；
- `[E-1]` Run T 完全缺席 G-1 同链 FATAL（`Failed to wait for future Registry initialization`）；
- `[E-2]` Run T 出现以下任一"越过"的可验证信号（原文行）：`FMLCommonSetupEvent` / `FMLLoadCompleteEvent` / `Done (` / 其它明确晚于 Registry initialization 的阶段行；
- `[E]` ForgeProbe 冻结证据不变（marker / `REWRITE hits=1` / dump `5E2F78DB…`）；
- `[F]` control vs treatment javap 对照通过（仅委托分支差异）；
- 新终点如实记录（若为新断层：first failure 完整 stack 原文）。

**结论上限**：仅证明"本次条件下，兼容 provider 对非 Forge 容器的委托使 G-1 清零、生命周期越过 Registry initialization"；不得推导兼容层可行 / ABI 兼容 / 方向 2 结论。

### 7.2 VALID-002-0d-1（修复生效且完整启动）

满足 7.1 全部条件，**且**服务端到达 Done（`Done (` 命中）。判定等级升为 VALID（更强基线）。

### 7.3 FAIL-002-0d-1

Run C 正常复现 G-1，但 Run T 完整执行后 G-1 未清除：

- 无 `delegate` 行（修复未生效）；或
- 有 `delegate` 行但 `NeoForge mod loading` 仍缺席，且 Registry initialization 同链失败依旧。

→ FAIL，交回裁定；不得自行更换修复方案（不得改走 (a') / (b)）。

### 7.4 STOP-002-0d-1

见 §八 汇总表。STOP 后：不得修复、不得进入 002-0e、不得修改探针；报告原样交回。

### 7.5 新基线登记

- treatment jar（`forgecompat-002-0d.jar`）哈希 + 两轮日志 / meta → **新基线**；
- 旧 002-0c 归档值保留历史对照，不得删除；
- 若出现新断层 → 记录为"新断层级基线"，交回裁定（下一 Spike 的分界点）。

---

## 八、STOP 条件（汇总）

**分界唯一标准 = G-1 是否清零**（`NeoForge mod loading` 是否命中）：

| 情况 | 处理 |
|---|---|
| 冻结引用哈希不匹配（v1.1 jar / probe / mods.toml / loader） | STOP |
| 恢复不可行，或硬门 1–3 任一不过，或 javap 差异无法解释 | STOP |
| Run C 未复现 G-1 签名（环境漂移 / 恢复失真） | STOP |
| marker 未命中 / `REWRITE hits ≠ 1` / dump 哈希 ≠ `5E2F78DB…` | STOP |
| 委托机制层异常（内置实例未找到或不唯一（0 / >1） / 跨层访问错误 / 递归 / 委托相关未预登记异常）——**G-1 未清零且与委托机制相关** | STOP |
| **G-1 未清零且与委托机制无关**的异常（例如 FML 内部新 bug） | 记录为「前置漂移」，STOP（由裁定方判断是否需重建基线） |
| 需要修改本说明书之外的文件才能继续 | STOP |
| 需要引入新 Forge API / 新 Mod / 新构建工具链才能继续 | STOP（属其它 Spike 范畴） |
| Run T 到达 Done 或第一个新断层（**G-1 已清零之后的任何异常**） | 记录后停止观测（PASS / VALID 路径；不修复） |

任何 STOP 后：不得自行设计修复方案；不得进入方向 2；不得修改探针；报告原样交回。

> 分界说明（提请审阅确认）：本文件对"新断层"作操作化区分——**G-1 清零之后**出现的生命周期间新断层 = 预期的新终点（记录、PASS 路径、不修复）；**在 `NeoForge mod loading` 之前**出现且与委托机制相关的未预登记异常 = STOP。分界点 = "G-1 是否清零"（对应修订 §三 3.2 第 7 / 9 条的操作化衔接）。

---

## 九、结构化输出格式（强制）

```
# Spike-002-0d 执行报告

## 一、实验环境
- Minecraft / NeoForge / FancyModLoader / ModLauncher / Java:
- 冻结引用哈希复测（v1.1 jar / probe / mods.toml / loader）:
- JVM 参数:
- Label（control / treatment）:
- meta.json（两轮）status / exitCode / duration / doneLine:

## 二、源码恢复与归档（2D-a）
- 恢复源清单 + 逐文件 SHA-256:
- services 逐字节对照结论:
- MANIFEST 对照结论（含 Implementation-Version 缺席确认）:
- 逐类 javap 对照结论 + 差异清单:
- 恢复态 / 修复态归档路径:

## 三、构建记录（2D-b / 2D-d）
- control jar: 路径 / SHA-256 / 字节数 / 条目清单 / 编译命令
- treatment jar: 同上
- javap 对照：冻结 vs control；control vs treatment（逐条差异）

## 四、Run C（control）
- 关键签名对照表（§四 2D-c 表 1–6，逐项原文）:
- 完整日志 / meta / crash-report 清单:

## 五、Run T（treatment）
- 日志序列（1–7，逐条原文，不得摘要）:
- 新终点（Done / 新断层 + 完整 stack）:
- ForgeProbe 证据（marker / BUS / REWRITE hits / dump）:
- meta / crash-report / mods-list:

## 六、判据自评
- 结果: PASS-002-0d-1 / VALID-002-0d-1 / FAIL-002-0d-1 / STOP-002-0d-1
- 依据（逐条对应 §七）:
- 结论上限（禁止超出）:

## 七、事实标签
[F] Source Fact:
[E] Experimental Fact:
[H] Hypothesis:
[D] Design Decision:

## 八、与说明书的偏差
- 逐条记录（如有）:

## 九、边界自检
- [ ] 未修改 forgeprobe.jar / mods.toml / ForgeProbe 源码
- [ ] 未修改四项前置语义与描述符重写逻辑
- [ ] 未修改 NeoForge / FML / ModLauncher / Minecraft / Java
- [ ] 仅修改 ForgeCompatLanguageLoader.loadMod（§五 委托分支）
- [ ] 未修改 validate / 未新增 validate 委托
- [ ] run-server.ps1 仅 ValidateSet 追加 2 个 Label
- [ ] 未修复任何新断层 / 未进入方向 2
- [ ] control 先于 treatment 执行（K1 顺序）
- [ ] 恢复态 / 修复态源码均已归档

## 十、原始日志
- control / treatment 的 latest / debug / stdout / stderr / crash-report 清单:
```

---

## 十、交接规则

执行完成后，报告原样交回架构决策方；不得自行决定下一 Spike。

根据结果：

- **PASS / VALID** → 架构决策方冻结新基线 → 起草 002-0e 说明书（含"语义级字节码对比 + P1/P2 双预登记"两条纪律）；
- **FAIL** → 交回；由决策方裁定修复方案的下一步（不得执行方自改）；
- **STOP** → 交回；由决策方裁定前置 / 机制问题。

---

## 十一、交付清单

```
1. 本说明书（送审稿 v0.1 → 冻结后 v1.0）
2. 归档至：D:\DualForge\Dev Manuals\Spike_002-0d.md
3. Label：spike002-0d-control / spike002-0d-treatment（ValidateSet 追加）
4. 冻结引用：
   - forgecompat-002-0b-2a-v1.1.jar  E27BB965…97A436 / 20316 B / 25 条
   - forgeprobe.jar                  A93A823C…374E22 / 1772 B
   - mods.toml                       B32CF69B…CE6E
   - loader-4.0.45.jar               8390398E…DCD4 / 505696 B
5. 新产物（执行后）：
   - forgecompat-002-0d-control.jar（恢复、未修复）
   - forgecompat-002-0d.jar（恢复 + §五 修复）
   - 恢复态 / 修复态源码归档 + 逐文件哈希
```

---

## 十二、本 Spike 的真正使命

002-0d 不是为了证明兼容层可行，也不是为了探测下一个 ABI 断层。

它只做一件事：**把主机自身的截断（G-1）从基线中清除**——使"层 e 之后 FML 能走到哪里"第一次成为可判定的问题。

证据链目标：

```
[F] 四项取证（list 保留内置实例 + 公开遍历路径）
        ↓
[E] Run C：恢复 jar 复现 G-1（保真 + K1 control）
        ↓
[F] control vs treatment：仅委托分支差异
        ↓
[E] Run T：delegate 行 → NeoForge mod loading → 越过 Registry initialization → 新终点
        ↓
[D] 新基线冻结 → 002-0e（方向 2 探针 #1）
```

不要将：

"G-1 清零"

写成：

"兼容层已可行" / "ABI 已兼容" / "Forge Mod 已可加载"。

前者只说明主机生命周期不再被 Plan A 接管范围截断；后者需要方向 2 及以上的多个探针。
