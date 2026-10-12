# Spike-002-0d 裁判审查与裁定报告

**文件性质**：对《Spike-002-0d 执行报告》（2026-10-08 00:08，执行方：总指挥应急代行）的架构裁定——第 1 版
**角色**：架构决策方（裁判）
**输入**：

- 《Spike-002-0d 执行报告》全文；
- 原始日志：`D:\DualForge\spike000\logs\spike002-0d-control\`、`D:\DualForge\spike000\logs\spike002-0d-treatment\`；
- 证据档：`D:\DualForge\spike000\logs\spike002-0d-artifacts\`（18 份）；
- 《Spike_002-0d.md》v1.0 冻结版（含七处小修）、《Spike-002-0d 审阅意见（顾问方·七处小修）》、《Spike-002-0c 裁判裁定修订（反向意见处理）》、《Forge → NeoForge 兼容层开发手册》（只读参照）。

**独立复核**：本裁判对全部关键证据独立重跑/重算（§二；动作、结果逐条留痕）。
**效力**：本文件为第 1 版。§一 裁定 1–3（VALID 确认 / 观测项 10 修正采纳 / 新基线冻结）即行生效；其余条款标注「待反向意见」或「待总指挥/用户确认」。
**边界声明**：本轮未修改任何探针、日志、实验报告、说明书、冻结件；未执行手册回写（提案见 §九）；唯一新建文件 = 本报告。临时核验目录 `D:\DualForge\_referee4-verify\`（jar 解包 / javap 重跑用）已在复核完成后移除；§二 全部步骤可用公开工具重跑复现。

---

## 一、裁定摘要

| # | 事项 | 裁定 |
|---|---|---|
| 1 | VALID-002-0d-1 | **成立（确认）**。§7.1 八条判据逐条复核通过；§7.2 条件（到达 Done）复核通过 → **VALID 级新基线**成立。结论上限照录（§三）。 |
| 2 | 观测项 10 实证修正 | **采纳**：neoforge 容器的 `IModInfo.getLoader()` 实际 = `com.example.forgecompat.ForgeCompatLanguageLoader`（compat，version 9999）；送审预期（builtin）被 `[F]`+`[E]` 链否证。下游含义登记（§四）。 |
| 3 | 新基线冻结 | **执行**：treatment jar `4CD8195F…2FEF` / 21180 B / 25 条 + 两轮日志/meta + 18 份证据档 + 恢复/修复态源码归档（§六）。冻结 v1.1 jar 与 002-0c 归档保持只读历史对照。 |
| 4 | §八 偏差清单 | 逐条处置（§五）：1 / 2 / 4 / 5 采纳；3 采纳（限制确认 + 治理动作）；另附两处措辞校准。 |
| 5 | 002-0e 方向 | 维持《002-0c 修订》§四 已冻结契约；按 §七 操作化移交（含「语义级字节码对比 + P1/P2 双预登记」两条纪律）。 |
| 6 | 冻结手续补记（对应总指挥令 R-0d-3 §三） | v1.0 指纹 = `C36FE57E…8221` / 30116 B；七项变更定位表 + 一致性复读结论（§八）；独立变更记录文件缺口处置待总指挥裁定（§八-4）。 |
| 7 | 手册回写 | **本轮不写**（先审后落盘）；提案清单见 §九，待总指挥/用户审后统一执行。 |

---

## 二、独立复核记录（动作 → 结果）

1. **冻结引用哈希复测**（v1.1 参照 jar / probe jar / probe `mods.toml`×3 / loader jar）→ 全部与说明书 §三 一致：
   - v1.1 jar `E27BB965…7A436` / 20316 B；probe `A93A823C…74E22` / 1772 B；`mods.toml`（src / build jar-root / build verify 三份）`B32CF69B…CE6E` / 241 B；loader `8390398E…4DCD4` / 505696 B。
2. **Jar 内容级独立比对**（tar 解包 + 逐文件 SHA-256）→ frozen vs control = **16/16 内容文件逐字节一致（0 差异）**；control vs treatment = **15/16 一致**，唯一差异 = `com/example/forgecompat/ForgeCompatLanguageLoader.class`（control `0F5FF48F…E320` / treatment `5DE73032…A10`）。
3. **条目集合与顺序** → 25（frozen）/ 25（control）/ 25（treatment）条；名称与顺序完全一致（含 9 个目录条目 + 16 个内容文件）。
4. **源码归档复算** → `archive\src-recovered\` 14/14 与 `_sha256.txt` 一致；`archive\src-fixed\` 14/14 一致；两树唯一差异 = `ForgeCompatLanguageLoader.java`（`7CC41E6A…B873` → `D7BA951C…C0A1`）。
5. **treatment 构建记录** → `2Dd-treatment-build-record.txt` 的命令原文 / javac 工具版本 / classpath 核对通过；jar 哈希、字节数、25 条条目清单复算一致。
6. **javap 重跑** → 本裁判用 `ms-21.0.10` 的 javap 对 frozen / control / treatment 三个 jar 的 loader 类重跑 `-p -c -constants`，输出与存档**逐字节一致**（`07AEF578…` / `07AEF578…` / `D594C01A…`）；control↔treatment diff = 仅「新增委托块（offsets 99–229）+ 4 个 synthetic lambda + 常量池重编号 + 尾部偏移平移（+131）」，非空路径字节码语义等价保留（原 99–111 ⇒ 230–242）。
7. **委托语义逐字核对（§五 5.2/5.3）** → 过滤顺序（`l != this` → `name()` → 类名）、候选数 ≠ 1 抛 `IllegalStateException`（消息含 `[FORGECOMPAT-LANG] builtin javafml not unique in languageProviders: candidates=`）、先打印后调用、`return builtin.loadMod(info, scanData, layer)`、**不 fall through**——与逐字定义完全一致（源码级 + 字节码级双重核对）。
8. **loader jar 调用点扫描（复核增补，超出 2De 存档范围）** → `loader-4.0.45.jar` 内引用 `IModLanguageLoader` 的类共 9 个；其中**仅** `net.neoforged.fml.ModLoader.buildModContainerFromTOML` 含 `loadMod` 调用（`IModInfo.getLoader()` → `IModLanguageLoader.loadMod(IModInfo, ModFileScanData, ModuleLayer)`）`[F]`。该点闭合「compat.loadMod 被实际调用 ⇒ getLoader(neoforge) = compat」的推理。
9. **观测项 10 数据链复核 `[F]`** → 逐条重跑 javap 一致：`ModInfo.getLoader()` = `owningFile.getFile().getLoaders().getFirst()`；`ModFile.identifyLanguage()` = `modFileInfo.requiredLanguageLoaders().stream().map(λ).toList()` → `putfield loaders`；λ = `FMLLoader.getLanguageLoadingProvider().findLanguage(this, languageName, acceptedVersions)`；`findLanguage` = `languageProviderMap.get(...)` + `VersionSupportMatrix` 通过 → `ModLanguageWrapper.modLanguageProvider()` 返回；`LanguageProviderLoader.<init>` / `lambda$new$0` = `languageProviders`（List，保留全部）+ `languageProviderMap.put(name, wrapper)`（后注册覆盖）。
10. **Run C 日志（control）** → stdout 行号与报告一致（L64–68 probe、L69 `entrypoints=[]`、L70–73 marker/BUS、L74 FATAL）；`[E-1]` 三文件各 1 行；`NeoForge mod loading` = 0 行；crash-report 含 `RegistryManager.postNewRegistryEvent(RegistryManager.java:94)` + `CommonModLoader.lambda$begin$0(CommonModLoader.java:52)` + 11 项注册表（两处清单全量核对）。
11. **Run T 日志（treatment）** → L69 / L70 / L75 逐字命中；`[E-1]` 三文件 = 0 行；`delegate` / `NeoForge mod loading` / `Done (` / `Starting minecraft server version` / `Stopping the server` 各 1 行；stdout 与 latest 零 FATAL/ERROR；无 crash-report 目录；stderr 0 B。
12. **meta 两轮复读** → Run C：`EXITED_EARLY` / exitCode 0 / 12.1s / doneLine null；Run T：`DONE_STOPPED_GRACEFULLY` / exitCode 0 / 24.7s / doneLine 原文（含行尾 `\r`）。
13. **`[E-2]` 越过信号** → treatment latest.log L28 `Starting minecraft server version 1.21.1`、L42 `Done (0.340s)! For help, type "help"`、L44 `Stopping the server`；stdout L81 / L95 / L97 同构。
14. **002-0c 对照签名** → 002-0c stdout（18:05 轮）L69/L74 与本轮 Run C 同构（仅时间 / BUS 地址不同）→「G-1 确定性复现」在本裁判直接复核范围内 = 002-0c 与 002-0d-control 两轮一致（17:29 轮沿用前裁判复核）。
15. **harness 核对** → ValidateSet = 19 labels（与 `2Df-harness-change-record.txt` 原文一致）；`$jvmArgs = @('-Xmx3G','-Xms1G') + $ExtraJvmArgs`；`PostDoneSeconds = 10` + `stop` 注入 + `DONE_STOPPED_GRACEFULLY` 判定逻辑核对一致；文件 mtime = 2026-10-07 23:59:28（早于首轮 23:59:47），其后未再写入。
16. **运行时副本核对** → `D:\DualForge\spike000\neoforge-server\mods\` 现态 = `forgecompat-002-0d.jar`（`4CD8195F…2FEF`，即 treatment）+ `forgeprobe.jar`（`A93A823C…74E22`）。
17. **治理面核对** → `spike001a\compat\src` 无 23:50 后写入；`compat\archive\src-001b1\_sha256.txt` 于 23:58:25 写入（2D-a 要求的归档动作；该基线文件本身未动，8/8 完整性复算通过）；v0.1 存档 SHA = `09CB21D4…691E`，与 R-0d-3 所记一致。
18. **v0.1 → v1.0 差异复核** → 见 §八（29 insertions / 8 deletions；仅七处小修 + 版头手续行）。

---

## 三、判据逐条核对（§七）

### 7.1 PASS-002-0d-1 八条

| # | 判据（§7.1） | 复核证据 | 判定 |
|---|---|---|---|
| 1 | Run C 复现 G-1 签名（2D-c 表 1–6；仅时间/BUS 允许不同） | §二-10、§二-14：L69 至少 1 行（实际 1）；marker+BUS 命中；`NeoForge mod loading`=0；FATAL 同链；crash-report 两行栈 + 11 项；meta `EXITED_EARLY`/0/12.1s/null；REWRITE hits=1 + dump `5E2F78DB…` | ✓ |
| 2 | Run T 出现 `[FORGECOMPAT-LANG] delegate -> builtin javafml for neoforge` | §二-11：stdout L70，恰好 1 行，逐字一致 | ✓ |
| 3 | Run T 出现 `NeoForge mod loading`（`NeoForgeMod` 被构造） | §二-11：stdout L75 1 行；latest.log L22 = 同一事件（渲染差异） | ✓ |
| 4 | `[E-1]` G-1 同链 FATAL 完全缺席 | §二-11：stdout / latest / debug 三处均 0 行 | ✓ |
| 5 | `[E-2]` 越过信号原文行出现 | §二-13：`Done (0.340s)!` + `Starting minecraft server version 1.21.1` + `Stopping the server` | ✓ |
| 6 | ForgeProbe 冻结证据不变（marker / `REWRITE hits=1` / dump `5E2F78DB…`） | §二-2/6/16：dump 哈希与字节数一致（1162 B）；REWRITE hits=1；marker+BUS 命中 | ✓ |
| 7 | control vs treatment javap 对照通过（仅委托分支差异） | §二-6：15/16 内容文件逐字节一致；唯一差异类 = loader；差异结构 = 新增委托块 + 4 synthetic + 常量池重编号 | ✓ |
| 8 | 新终点如实记录（若新断层：完整 stack 原文） | 新终点 = **Done**；无新断层、无新 stack；`DONE_STOPPED_GRACEFULLY` / exit 0 / 24.7s | ✓ |

### 7.2 VALID-002-0d-1

7.1 全部条件满足 **且** 服务端到达 Done（`Done (` 命中，两处复核）→ **VALID 成立**。

### 结论上限（照录，禁止超出）

仅证明「本次条件下，兼容 provider 对非 Forge 容器的委托使 G-1 清零、生命周期越过 `Registry initialization` 并完整到达 Done」；**不得**推导「兼容层可行 / ABI 兼容 / Forge Mod 可被加载 / 方向 2 API 选择」。

---

## 四、观测项 10 裁定（实证修正采纳）

**事实**：委托生效后，neoforge 容器的 `IModInfo.getLoader()` **实际 = compat 实例**（`com.example.forgecompat.ForgeCompatLanguageLoader`，version 9999），与送审文本「预期 = builtin `FMLJavaModLanguageProvider`」不符 `[F]`+`[E]`。

**裁定依据（本裁判独立复核链）**：

1. 数据链 `[F]`（§二-9）：`ModInfo.getLoader()` 取 `ModFile.loaders` 首项；`ModFile.loaders` 来自 `identifyLanguage` → `findLanguage` → **`languageProviderMap` 胜者**；debug.log L162（builtin 4.0.45 先注册）→ L165（compat 9999 后注册）→ `HashMap.put` 覆盖 ⇒ 胜者 = compat。
2. 调用侧闭合 `[F]`（§二-8）：`ModLoader.buildModContainerFromTOML` 为 loader jar 内**唯一** `loadMod` 调用点，`getLoader()` → `loadMod(...)`；Run T 中 compat.loadMod 被实际调用（stdout L69）⇒ `getLoader(neoforge) = compat`。
3. 委托只改变「由谁执行 `loadMod` 逻辑」，**不改变容器 loader 归属**（归属由 map 胜者决定）。送审预期混淆了这两个层面。

**登记（不修复）**：

- 若 `validate` 分组按 `IModInfo.getLoader()`（F2-b 前提不变），neoforge 仍归 **compat 组** → 内置 `FMLJavaModLanguageProvider.validate` 仍**不运行**；
- 《审阅意见》补 2 所述「委托后内置 validate 恢复运行」**不成立**（前提被 `[F]` 链否证）——该预期**不得**作为后续 Spike 的既成前提；如需改变 validate 归属，另立子 Spike 裁定。
- 本项为「必须记录」观测项，**非 PASS/FAIL 判据**，不影响 VALID 判定。

**对已冻结文本的处理**：不改动《Spike_002-0d.md v1.0》正文；以本裁定 §四 作为该观测项的正式修正记录（送审「预期」以实证为准更新）。

---

## 五、§八 偏差处置（逐条）

| # | 偏差 | 裁定 | 依据 / 后续要求 |
|---|---|---|---|
| 1 | 治理偏差：2D-a…2D-f 由总指挥应急代行（协作通道故障；执行子代理两次 0 输出） | **采纳（披露有效）**。不影响本结论：全部产物经本裁判独立复核（§二），证据强度未降级。 | 后续 Spike 优先恢复「执行方 = 独立子代理」；如再次应急，沿用「总指挥代行 + 独立裁判复核 + 逐次披露」模式，并在报告单列受影响环节。 |
| 2 | control 逐字编译命令未单独落盘 | **采纳（记录缺口）；不阻塞。** | 风险已被覆盖：control 内容 16/16 与冻结 jar 逐字节一致（等效于既有已记录构建的产出）；treatment 命令已落盘。要求：此后**两个 jar 的命令均落盘**。 |
| 3 | `run-server.ps1` 无机械 diff 基线（非 git、无基线副本） | **采纳（限制成立）；不阻塞本裁定。** K1 有效性依赖「两次运行使用同一脚本实例」而非「与旧基线一致」：脚本 mtime 23:59:28 早于首轮、其后未再写入；同参数、同产出结构可证。 | 治理动作（建议 002-0e 前完成）：将该脚本纳入版本控制，或在每轮 Spike 前建立带哈希的基线副本。治理形式由总指挥裁定。 |
| 4 | marker/BUS 捕获顺序差异（Run T：BUS 先于 marker，1 行级乱序） | **采纳（记录、不推断）。** 判据仅要求两行命中（均为恰好 1 行）；`[E-1]` 缺席另有 log4j 独立文件（latest/debug）佐证，不依赖 stdout 捕获路径。 | 成因未定（probe 源码 `[F]` 打印顺序为 `==== / marker / BUS / ====`）；如需归因，另立观测任务，不阻塞。 |
| 5 | 观测项 10 与送审预期不符 | **采纳为实证修正**（见 §四）。 | 修正记录已登记；后续沿用「预期」被否证的教训：容器 loader 归属与委托目标为两个层面。 |

**措辞校准（不影响结论）**：

- 报告 §五-2「stdout L75（= latest.log L22 原文）」：两行为同一事件的两种渲染（短类名 / 无毫秒 vs 全类名 / 含毫秒 + `\r`），非逐字相同 → 建议表述为「同一事件行（渲染差异）」。
- 报告 §九「原 `compat\` 目录全程未动」：实测 `compat\src` 无写入；唯 `compat\archive\src-001b1\_sha256.txt` 于 23:58:25 写入（2D-a 要求的基线归档动作）→ 建议表述为「`compat\src` 未动；`archive\src-001b1` 仅新增哈希清单」。

---

## 六、新基线冻结登记（自本裁定生效）

**新基线组合**：

| 项 | 路径 | 指纹 |
|---|---|---|
| compat（treatment，基线本体） | `D:\DualForge\spike001a\compat-002-0d\build-treatment\forgecompat-002-0d.jar` | `4CD8195F7AA03B8B595F646CDEAC16454D03D5E3862B5E2F388C820CF2FA2FEF` / 21180 B / 25 条 |
| K1 control（对照件） | `D:\DualForge\spike001a\compat-002-0d\build-control\forgecompat-002-0d-control.jar` | `62A15F5734F09508D7BE033E828604671DBB6AD0E21747321CF85BC0E8289613` / 20316 B / 25 条 |
| 运行产物（Run C） | `D:\DualForge\spike000\logs\spike002-0d-control\`（7 文件，含 crash-report） | meta：`EXITED_EARLY` / 0 / 12.1s |
| 运行产物（Run T） | `D:\DualForge\spike000\logs\spike002-0d-treatment\`（6 文件，无 crash-report） | meta：`DONE_STOPPED_GRACEFULLY` / 0 / 24.7s |
| 证据档 | `D:\DualForge\spike000\logs\spike002-0d-artifacts\`（18 份） | 逐份字节数与报告附录 A 一致（§二-2/6 复算） |
| 源码归档 | `compat-002-0d\archive\src-recovered\`、`src-fixed\`（各 14 文件 + `_sha256.txt`） | 逐文件哈希复算通过（§二-4） |

**只读历史对照（不得删除/修改）**：

- v1.1 参照 jar `spike001a\compat\build\forgecompat-002-0b-2a-v1.1.jar`（`E27BB965…`）；
- 002-0c 全部归档值（日志 / meta / 证据）；
- `compat\archive\src-001b1\` 基线备份、`compat\src` 原目录；
- probe 冻结件：`forgeprobe.jar`（`A93A…`）+ `mods.toml`（`B32C…`）。

**不变式**：上述文件冻结后不得修改/删除/覆盖；一切后续引用以本节指纹为准；`neoforge-server\mods\` 内为运行时副本，不构成基线本体。

---

## 七、002-0e 方向裁定（维持《002-0c 修订》§四，操作化移交）

1. **探针与插入点**：在 `ForgeProbe` 构造函数**全部 marker 输出之后、`return` 之前**新增唯一语句 `MinecraftForge.EVENT_BUS.register(this);`（源码唯一变量）。
2. **纪律 A（语义级字节码对比，强制）**：新旧 `ForgeProbe.class` 附 `javap -p -c -constants` 对照——忽略常量池 `#索引` 与顺序；marker 段（4 次 println 序列）与 `super()` 调用逐条语义一致；唯一新增 = 该语句对应指令组；异常表 / 行号表差异须列出；附新旧 jar 哈希与字节数、新旧 rewritten dump 哈希、`REWRITE hits`（预期 = 1）。
3. **纪律 B（P1/P2 双预登记）**：P1 = `NoClassDefFoundError: net.minecraftforge.common.MinecraftForge`；P2 = `NoSuchMethodError: net.minecraftforge.eventbus.api.IEventBus.register`。任一出现 = **PASS-002-0e-1**（首个断层坐标；须给失败类型 / 消息 / 完整 stack / 触发位置在 marker 之后）；其它首失败（如 `NoSuchFieldError`、其它 `LinkageError`）→ 原样记录 + STOP 交回，不得自行扩展解释。
4. **前置与登记**：compat 侧输入固定为本裁定 §六 的 treatment jar（`4CD8195F…`）；探针源码与旧 jar（`A93A…`）修改前归档（`configOf` 教训制度化）；新 jar 构建命令 / 工具 / 哈希落盘；Label 追加 `spike002-0e`（harness 基线治理先行，见 §五-3）。
5. **边界**：不改 compat jar、不改四项前置、不改描述符重写、不修复首失败、不进入方向 3；marker/BUS 未命中 → STOP（前置错误）。
6. **说明书**：002-0e 说明书全文（v0.1）按惯例另行起草、送审后冻结；本裁定只固定方向与契约，不代替说明书。

---

## 八、冻结手续补记（对应总指挥令 R-0d-3 §三）

### 8.1 七项变更定位表（v0.1 → v1.0；本裁判经机械 diff 复核）

总差异：**29 insertions / 8 deletions**；除下表七处 + 版头手续行（状态 / 审阅链 / 变更记录块 / 总指挥注记）外，无任何其它差异 ⇒ 「除这七处外无其他实质变更」**成立**。

| # | 审阅意见编号 | v1.0 位置（行号） | 修改后原文（摘录） | 修改前（v0.1） |
|---|---|---|---|---|
| 1 | 补 1 / §七表 #1 | L296（§五 5.2 代码块前） | `// 返回类型: Stream<IModLanguageLoader>（…applyForEach…；无需 .stream()…）` | 无此行 |
| 2 | 补 2 / §七表 #2 | L70（§0.4）+ L359（§六） | 「不在本 Spike 评估『委托后内置 validate 恢复运行』的行为变化（属下游观测项）」；观测项 10 全文 | 无此二条（§六 止于第 9 条） |
| 3 | 补 3 / §七表 #3 | L292（§五 5.1） | 「**委托分支以 `return` 结束，不 fall through**…」 | 无此句（5.1 列表以「空路径 = 委托分支」结束） |
| 4 | §二 / §七表 #4 | L232（2D-c 表 1） | 「**至少 1 行**（对照 002-0c 的 line 69；如实际多次，全量记录）」 | L219：「原文一致」 |
| 5 | §三 / §七表 #5 | L372–L373（§7.1） | `[E-1]`（FATAL 完全缺席）+ `[E-2]`（越过信号原文行）两条 | L355：单条模糊表述（「且日志明确越过…」） |
| 6 | §四 / §七表 #6 | L56（§0.2 表 4） | 「API 签名 `[F]`；跨层可访问性 `[H]`，待本 Spike 2D-e 运行层验证」 | L44：「（API 层 `[F]`；运行层待本 Spike 验证）」 |
| 7 | §五 / §七表 #7 | L407 / L415 / L416（§八） | 「分界唯一标准 = G-1 是否清零」；委托机制层异常行加限定；新增「与委托机制无关 → 前置漂移，STOP」行 | L395 / L398：旧两行（无边界情形覆盖） |

### 8.2 一致性复读结论（§0.2 / §0.4 / §五 / §六 / §七 / §八 联动）

- §0.2 表 4（标签降级）↔ §五 5.2（返回类型 `[F]` 来源）↔ §六-10（观测项）：无矛盾；
- §0.4（不评估 validate 变化）↔ §六-10 ↔ §0.2 F2-b（记录不修复）：一致；
- §五 5.1（return 语义）↔ §五 5.3（`return builtin.loadMod(...)`）↔ 实际字节码：一致（§二-6/7）；
- §2D-c 表 1「至少 1 行」↔ §七判据 ↔ §八 STOP 表（entrypoints 行缺失 → STOP）：一致（本轮实际 = 1 行）；
- §七 `[E-1]`/`[E-2]` ↔ §八「G-1 是否清零」唯一标准：一致；
- §八 两类边界情形 ↔ §7.4：一致；本轮未触发（G-1 已清零，路径 = PASS/VALID）；
- §六-10「预期 = builtin」与实际（compat）不符：属观测项性质，**不构成说明书内部矛盾**；以本裁定 §四 登记修正；
- §十二 证据链目标（delegate → NeoForge mod loading → 越过 → 新终点）↔ 实际运行：完全吻合。

**结论：一致性复读通过（含上述 1 处预期-实证差异的登记处置）。**

### 8.3 v1.0 指纹（补记）

- `D:\DualForge\Dev Manuals\Spike_002-0d.md`：`C36FE57E7F1D5930DF702EEB9E6ADFCA958806E7C2733B40FCD0EA5EB5428221` / 30116 B / 551 行。
- 建议：将该指纹登记为 v1.0 冻结指纹（原冻结流程未及记录；本裁判复算补记）。

### 8.4 缺口与处置建议（待总指挥裁定）

- **缺口**：独立文件《Spike-002-0d v1.0 变更记录（冻结）.md》未生成（R-0d-3 §三-2 要求的独立落盘形态）；R-0d-3 §三-3/4 的独立回报链未走完（协作通道故障；见执行报告 §八-1 与说明书版头注记）。
- **实质评估**：七项修改内容与定位已由本裁判机械复核（§8.1），SHA 与一致性复读已补记（§8.3 / §8.2）——**实质已完成，仅文书形态缺失**。
- **处置选项**：(a) 采纳本报告 §8.1–8.3 作为变更记录（不另立文件）；(b) 由 referee2（原指定接收方）依本报告数据补发独立文件。请总指挥裁定其一。

---

## 九、手册回写提案（先审后落盘；本轮未执行）

依 R-0d-3 §三-5 治理裁定，以下为**提案**（暂不写入《开发手册》）：

1. **G-1 / 002-0d 结果**：方向 (c) 委托已实证清零 G-1（VALID-002-0d-1）；新基线 = treatment jar `4CD8195F…`（登记哈希）；
2. **条件性基线语义**：002-0c 的「生命周期事实基线」正式标注为「G-1 条件下的历史基线」；
3. **观测项 10 结论**：neoforge 容器 `getLoader()` 实际 = compat（9999）⇒ validate 归属未回内置（F2-b 现状维持）；「内置 validate 恢复运行」预期被否证；
4. **治理条目**：双 jar 构建命令落盘、harness 基线（版本控制 / 哈希副本）、执行方角色恢复要求。

以上待总指挥/用户审阅后再统一落盘。

---

## 十、待反向意见 / 待裁定 / 待确认

**顾问方（如流程需要）**：请对 §四（观测项 10 归属与下游含义）、§五-3（harness 基线处置）、§七（002-0e 方向操作化）出具反向意见。
**总指挥**：§8.4 缺口处置形式；§五-3 harness 基线治理形式；执行方角色恢复；是否将本报告升为终版。
**用户**：按惯例放行 / 确认（如需要）。

---

## 附 A：本次复核重算的全部关键指纹

| 对象 | SHA-256 | 字节数 |
|---|---|---|
| v1.1 参照 jar（只读） | `E27BB9659303DDEDF2EDE02D10B49CAFC586549C0EDA003A0165DF525F97A436` | 20316 |
| probe jar（冻结） | `A93A823C988BFD6A8F6EA132D189D9DE732B0E5951F4BF74980D51BB63374E22` | 1772 |
| probe mods.toml（×3 副本） | `B32CF69B88FBE053C2580FC739B30FEDC43D96984C72FF78CD6725C7F3A1CE6E` | 241 |
| loader-4.0.45.jar | `8390398E67DF23E72D85ED809685BE2752C8EC545B7CBB6AD4B70FDC81E4DCD4` | 505696 |
| control jar | `62A15F5734F09508D7BE033E828604671DBB6AD0E21747321CF85BC0E8289613` | 20316 |
| **treatment jar（新基线）** | `4CD8195F7AA03B8B595F646CDEAC16454D03D5E3862B5E2F388C820CF2FA2FEF` | 21180 |
| `ForgeCompatLanguageLoader.class`（control / treatment） | `0F5FF48F…E320` / `5DE73032…A10` | — |
| loader javap 文本（frozen=control=本裁判重跑 / treatment=本裁判重跑） | `07AEF578…0841` / `D594C01A…0B4FD` | 4655 / 9534 |
| rewritten dump | `5E2F78DB2B0909D7368A185F618C3D080C00B732279B105CD5B8010FD66D9295` | 1162 |
| 修复前 / 修复后 loader 源码 | `7CC41E6A…B873` / `D7BA951C…C0A1` | 1743 / 2656 |
| `run-server.ps1` | `F5319A54086C5BF7EBC8441774F358BC10B74F99C90471A526CAA2D5707FADCA` | 8900 |
| 《Spike_002-0d.md》v1.0 冻结版 | `C36FE57E7F1D5930DF702EEB9E6ADFCA958806E7C2733B40FCD0EA5EB5428221` | 30116 |
| 《Spike_002-0d v0.1（送审稿·存档）》 | `09CB21D4A87F541DDAE4EE438A244F0A9DD71FF28637A14ED2692354CA37691E` | 27540 |
| 《Spike-002-0d 执行报告》 | `77ABC0F3CB3B379B323D6F480C4E008927476F4F942B9DFB68133777D38490D8` | 23951 |

## 附 B：关键原文摘录（本裁判复核）

**Run C（control）**：

```
L69: [FORGECOMPAT-LANG] loadMod called for neoforge entrypoints=[]
L74: [23:59:56] [main/FATAL] [ne.ne.fm.ModLoader/LOADING]: Failed to wait for future Registry initialization, 1 errors found
meta: EXITED_EARLY / exitCode 0 / 12.1s / doneLine null
```

**Run T（treatment）**：

```
L69: [FORGECOMPAT-LANG] loadMod called for neoforge entrypoints=[]
L70: [FORGECOMPAT-LANG] delegate -> builtin javafml for neoforge
L75: [00:03:57] [modloading-worker-0/INFO] [ne.ne.ne.co.NeoForgeMod/NEOFORGE-MOD]: NeoForge mod loading, version 21.1.256, for MC 1.21.1
L95: [00:03:59] [Server thread/INFO] [minecraft/DedicatedServer]: Done (0.340s)! For help, type "help"
[E-1] stdout / latest / debug = 0 / 0 / 0
meta: DONE_STOPPED_GRACEFULLY / exitCode 0 / 24.7s
```

**观测项 10（treatment debug.log）**：

```
L162: Found language provider javafml, version 4.0.45   （builtin，先注册）
L165: Found language provider javafml, version 9999      （compat，后注册 → map 胜者）
```

---

*本报告由架构决策方（裁判）出具；第 1 版；2026-10-08。*
