# Spike-002-0c 裁判审查与裁定报告

角色：架构决策方（裁判）
审查对象：《Spike-002-0c 实验报告》＋《Spike_002-0c.md》（v1 冻结版）＋ 原始日志与产物全量
审查日期：2026-10-07
审查结论（摘要）：**维持 PASS-002-0c-1**；认定**前置缺陷 G-1**（兼容 provider 接管 `neoforge` 系统容器）；
裁定 **002-0d（G-1 前置修复）→ 002-0e（方向 2 探针 #1 = `MinecraftForge.EVENT_BUS.register(this)`）**。

---

## 一、审查范围与依据

原始产物（全量独立核验，非引用报告转述）：

```
D:\DualForge\spike000\logs\spike002-0c\
  ├ spike002-0c.stdout.log    16837 B / 231 行
  ├ spike002-0c.log            7502 B（latest）
  ├ spike002-0c.debug.log     76151 B
  ├ spike002-0c.stderr.log        0 B
  ├ spike002-0c.mods-list.txt    48 B（forgecompat-002-0b-2a-v1.1.jar + forgeprobe.jar）
  ├ meta.json                  1191 B
  └ crash-reports\crash-2026-10-07_18.05.48-fml.txt  5681 B
```

对照运行（既有产物）：

- `spike002-0b-2a-v1.1`（17:29，treatment，rewrite=true）
- `spike002-0b-2a-control-v1.1`（17:28，control，rewrite=false）
- `baseline` / `forgeprobe` / `spike001a-0`（无兼容 provider 的早期运行）

冻结复用件：

- `D:\DualForge\spike001a\compat\build\forgecompat-002-0b-2a-v1.1.jar`
  SHA-256 `E27BB9659303DDEDF2EDE02D10B49CAFC586549C0EDA003A0165DF525F97A436` / 20316 B / 25 条
- `D:\DualForge\spike000\probe\build\forgeprobe.jar`
  SHA-256 `A93A823C988BFD6A8F6EA132D189D9DE732B0E5951F4BF74980D51BB63374E22` / 1772 B

---

## 二、逐项核验（声称 → 独立核验 → 判定）

| # | 报告声称 | 独立核验（原始产物） | 判定 |
|---|---|---|---|
| 1 | 前置生效：`javafml@null` provider / `offered` / `REWRITE hits=1` / post-check | stdout line 60、64–69 原文逐字一致（含 `post-check ctorCount=1 oldAbsent=true newPresent=true`） | ✅ |
| 2 | marker 命中，`BUS = net.neoforged.bus.EventBus@f2931ef` | stdout line 70–73 原文逐字一致 | ✅ |
| 3 | 7 项生命周期事件零命中；Done = NO | stdout / latest / debug 三份逐名检索均为 0；`Done (` 三份均 0；`doneLine=null` | ✅ |
| 4 | Registry initialization 首个失败 + 11 项 `neoforge:*` 清单 + 完整 stack | crash-report 原文一致：`RegistryManager.postNewRegistryEvent(RegistryManager.java:94)`、`CommonModLoader.lambda$begin$0(CommonModLoader.java:52)` | ✅ |
| 5 | `EXITED_EARLY` / exitCode 0 / 12.1s | meta.json 一致（startedAt 18:05:38.526 → endedAt 18:05:50.638） | ✅ |
| 6 | 复用件哈希 / 字节数 / 条目数 | 实测一致（E27B…/20316/25；A93A…/1772） | ✅ |
| 7 | rewritten dump `5E2F78DB…` / 1162 B | 实测一致（且与 v1.1 归档值完全一致 → 重写输出确定性） | ✅ |
| 8 | `run-server.ps1` 仅追加 `spike002-0c` Label（16 → 17） | 实测 ValidateSet 17 项且该 Label 生效（运行成功） | ✅ |
| 9 | 探针源码树 5 java / mods.toml 未动 / 版本未变 | 实测 5 java；mods.toml SHA-256 `B32CF69B…`（3 份副本一致）；crash-report 版本与冻结环境一致 | ✅ |
| 10 | JVM 参数 `-Dforgecompat.2a.rewrite=true` | `user_jvm_args.txt` 内容含该参数（mtime = 运行开始 18:05:38）；转换器**强制要求**该属性显式设置（未设置即抛异常）[F]，`rewriteEnabled=true` 反证其已生效 | ✅ |
| 11 | §六 "build\forgecompat.jar 保持 001b-1 基线" | 内容级正确（per-entry diff = 0，2A-f 证据）；字节哈希为重建值 `95B7398A…`（≠ 归档 `14EFD54F…`），且本 spike 未使用该文件 | ✅（字节级注记见 §三·F3） |
| 12 | 边界合规（未改 probe/compat/mods.toml/前置/转换/新 API/新 Mod/不修复） | 以哈希、源码树计数、归档记录、日志行为核验，无越界证据 | ✅ |
| 13 | §八标签使用（[F]/[E]/[H]/[D]） | 抽查全部符合；归因项正确留在 [H]（未越权推断） | ✅ |

补充核验（报告未列、审查增列）：

- `spike002-0c.stderr.log` = 0 B（无 JVM 级告警污染）。
- 前置集在 v1.1 与 002-0c 两轮中的行为逐行一致（见 §三·F1）。
- control（rewrite=false，17:28）行为正确：line 60 `rewriteEnabled=false`、marker 零命中、首失败 = 层 d
  `Mod constructor has unsupported argument interface net.minecraftforge.eventbus.api.IEventBus`、
  FATAL = `Failed to wait for future Mod Construction`。

---

## 三、审查发现（关键三条）

### F1 · v1.1 原始日志已含同一 Registry 失败——002-0c 是确定性复现

`spike002-0b-2a-v1.1`（17:29）原始 stdout 末段与 002-0c（18:05）**逐行一致**（仅时间戳与 BUS 地址不同）：

```
line 69: [FORGECOMPAT-LANG] loadMod called for neoforge entrypoints=[]
line 71: FORGE PROBE CONSTRUCTOR REACHED
line 74: [main/FATAL] ... Failed to wait for future Registry initialization, 1 errors found
```

两轮 meta.json 同为 `EXITED_EARLY` / exitCode 0 / 12.1s / doneLine null；两轮 crash-report 的
失败链同为 `RegistryManager.postNewRegistryEvent(RegistryManager.java:94)` + 同一 11 项清单。

**加注**：v1.1 依当时边界纪律（marker 之后不进入分析）未报告该失败；002-0c 取消该约束后将其纳入分析。
报告 §四"后续第一次失败"的措辞在 002-0c 范围内成立；但**该失败并非 002-0c 首次产生，而是两轮确定性复现**。

### F2 · G-1 前置缺陷认定（本次审查新增字节码取证）

**证据链（逐环标注）**：

- `[F]` `neoforge` 容器 entrypoint = `net.neoforged.neoforge.common.NeoForgeMod`
  （`RuntimeVisibleAnnotations: Lnet/neoforged/fml/common/Mod;`；构造器 `(IEventBus, Dist, ModContainer)`，
  来源：`neoforge-21.1.256-universal.jar` 字节码）。
- `[F]` `NeoForgeMod.<init>` 内依次执行各注册表 `DeferredRegister.register(bus)`，并调用
  `NeoForgeRegistriesSetup.setup(bus)`；`setup` 的 `NewRegistryEvent` 监听器 `registerRegistries(...)`
  注册的 **11 项注册表与失败清单逐一对应**（entity_data_serializers / global_loot_modifier_serializers /
  biome_modifier_serializers / structure_modifier_serializers / fluid_type / holder_set_type /
  ingredient_serializer / fluid_ingredient_type / condition_codecs / attachment_types / synced_attachment_types）。
- `[E]` 兼容 provider 对 `neoforge` 容器返回 `entrypoints=[]`
  （stdout line 69；v1.1 同；control(rewrite=false) 亦同——与描述符重写变量无关）。
- `[E]` `NeoForge mod loading`（`NeoForgeMod` 构造器首条日志）仅出现在**无兼容 provider** 的运行
  （baseline / forgeprobe / spike001a-0 各 1 次），在**所有兼容 provider 激活的运行中零命中**。
- `⇒ [D]` **认定 G-1**：Plan A 同名 `javafml` 覆盖（兼容 provider 占据 `languageProviderMap["javafml"]`）
  使兼容 provider 同时接管 `neoforge` **系统容器**；其 `loadMod` 仅检索 `net.minecraftforge.fml.common.Mod`
  注解（字节码已证），对 NeoForge 格式容器必然返回空 entrypoints → **NeoForge 核心 mod 从未被构造**
  → `NeoForgeRegistriesSetup.setup` 从未执行 → 11 项注册表"created but not registered to NewRegistryEvent"
  → Registry initialization 失败、进程终止。

**分类**：G-1 属**前置缺陷（v1.1 前置集 / Plan A 接管范围问题）**，**不是** Forge↔NeoForge ABI 断层，
**与 ForgeProbe 无关**（变量 = 0 仍然发生；层 e marker 已在失败前正常命中）。
该缺陷解释了"只依赖 @Mod 入口、构造函数不调用额外 Forge API 的探针"为何止步于主机自身生命周期。

**过程注记**：001b-3 已以 [E] 记录"同名覆盖 = 情况 A（兼容层胜出）"，但未评估其对 `neoforge`
系统容器的影响；v1.1 原始日志首次携带该后果（line 69 + Registry 失败），002-0c 将其正式纳入观测。
本次审查通过字节码取证把该 [H] 级别疑点升级为 [F]+[E] 支撑的 [D] 认定。

### F3 · 说明性注记（三项，均不影响任何裁定）

1. 崩溃报告 `JVM Flags: 2 total; -Xmx3G -Xms1G` 不含 `-D` 属性：为报告显示行为；
   属性生效有两条独立证据（`user_jvm_args.txt` 内容与运行时刻一致；转换器强制要求该属性、
   `rewriteEnabled=true` 反证其存在）。**不是缺参。**
2. 日志/报告中的 `javafml@null` 为显示值：`ModLoader` 的崩溃报告 callable 用
   `loader.name() + "@" + Package.getImplementationVersion()` 拼接（字节码已证），兼容 jar 无
   `Implementation-Version` → 显示 null；功能 `version() = "9999"`（冻结 jar 字节码）。
   **"null" 不代表无版本或未覆盖。**
3. `build\forgecompat.jar`（`95B7398A…` / 13215 B / 15 条）为 001b-1 源码的**重建**（2A-f 清理动作，
   per-entry diff = 0）；与归档 `forgecompat-001b1.jar`（`14EFD54F…`）字节不同属 zip 元数据差异。
   002-0c 运行使用的是归档 v1.1 jar（meta.json extraModsJars 可证）。

---

## 四、裁定

### 裁定 1：PASS-002-0c-1 成立（维持）

依据《Spike_002-0c.md》§六判据："FML 未达 Done，但到达某个明确的生命周期阶段；记录该阶段最后一条日志 +
后续第一次失败"。报告对此四项（阶段 = Registry initialization、最后 FATAL 行、首失败 = IllegalStateException、
完整 stack）逐项提供原始证据，且边界合规、标签纪律良好。**维持 PASS-002-0c-1。**

同时明确其**结论上限**（与报告 §九一致，不扩大）：

- [E] 层 e 之后 FML 止步于 Registry initialization，首失败为 `NewRegistryEvent` 一致性检查；
- 不推导 "ForgeProbe 已成功加载 / 兼容层可行 / 方向 2 采用某 API"。

### 裁定 2：G-1 前置缺陷认定与处置——002-0d

1. **认定**：G-1 为前置缺陷（Plan A 接管 `neoforge` 系统容器导致 NeoForge 核心 mod 缺构造）。
   在 G-1 未修复前，**任何"生命周期推进"类观测都在被主机自身截断的基线上进行**，不具备链条效力。
2. **处置**：先执行 **002-0d（G-1 前置修复）**，再执行方向 2 探针（见裁定 3）。
3. **002-0d 单变量定义**：仅允许变更"兼容 provider 对非 Forge 容器的接管行为"；其余全部冻结
   （probe jar、mods.toml、四项前置配置语义、描述符重写逻辑、NeoForge/FML/JVM 参数）。
4. **目标可观察（PASS 判据）**：修复后重跑**零变量基线**（复用 forgeprobe 原样 + rewrite=true）：
   - `NeoForge mod loading` 回归（neoforge 容器被构造）；
   - FML 越过 Registry initialization；
   - 记录新终点（若完整启动 → 升级为 VALID 级基线；若出现新断层 → 原样记录，不修复）。
5. **修复方向**（[H] 候选，具体实现须由决策方在 002-0d 说明书中给出**逐字定义**后冻结）：
   - (a) 保留 Plan A：兼容 provider 对"无 Forge @Mod entrypoints"的容器复刻内置 javafml 行为
     （纳入 `net.neoforged.fml.common.Mod` 注解类），使 `neoforge` 容器得到其真实 entrypoint；
   - (b) 改 Plan B（语言名解耦）：改动面更大、现有证据不足，非首选。
6. **源码约束（维护事项并入本裁定）**：v1.1 的 `ForgeCompatLanguageLoader` /
   `ForgeCompatTransformationService` / `ForgeProbeConstructorTransformer` 源文件已在 2A-f 清理中删除
   （有删除记录）。002-0d 触碰前必须先从冻结 jar（`E27B…`）恢复/重建对应源码，并**显式归档**
   （001b-6 `configOf` 丢失教训的制度化执行）。
7. **修复后冻结**：新的兼容 jar 哈希、重跑基线日志与 meta 一并冻结；此后基线以新值为准
   （002-0c 归档值保留为历史对照）。

### 裁定 3：方向 2 探针 #1 冻结——002-0e（`MinecraftForge.EVENT_BUS.register(this)`）

依据候选表"按断层深度递增"与 002-0c 结果（构造阶段可达、其后被 G-1 截断）：

- **API**：`MinecraftForge.EVENT_BUS.register(this)`（首个探针，浅断层：类存在性 + 全局事件总线映射）。
- **位置**：`ForgeProbe` 构造函数体内、marker 三行打印**之后**（保证 marker 仍命中，且该调用成为
  marker 之后首条新语句）。
- **单变量**：仅 `forgeprobe.jar`（源码 + 重建）变化；其余全部冻结（含 002-0d 修复后的兼容 jar）。
- **预登记预期（[H]，须由 002-0e 验证/证伪）**：首个失败 = `NoClassDefFoundError:
  net.minecraftforge.common.MinecraftForge`；依据 [F]：转换器仅重写 `<init>` 描述符
  （`REWRITE hits=1`），不重写字段/方法引用，新增调用的 `MinecraftForge` 引用保持 Forge 命名空间。
- **观察项**：marker 命中 + BUS 值；首失败类型 / 消息 / 完整 stack；`REWRITE hits`（预期仍 = 1）；
  dump 新哈希（含新增指令）；进程终点（预期死于 Mod Construction，早于 Registry initialization）。
- **判据**：首失败 = 预期 NCDFE → PASS-002-0e-1（方向 2 首个断层坐标：`net.minecraftforge.common`
  命名空间缺失）；其它首失败 → 原样记录，交裁判再裁定；marker 未命中 → STOP（前置错误）。
- **顺序**：在 002-0d 之后执行（对修复后的有效基线），保证证据链与有效主机一致；
  该探针契约与顺序无关，可原样复用。

### 裁定 4：边界与纪律复核

- 002-0c 执行方在 STOP 后未自行修复、未进入方向 2、未扩散范围——**执行纪律正确**，予以确认。
- G-1 的修复方式属决策方裁定范围（延续 Step 2-A v1 的先例：逐字定义后再交执行方）。
- 本报告不修改任何冻结文件：《Spike_002-0c.md》《Spike-002-0c 实验报告》保持原样；
  回写仅进入手册（见 §五）。

---

## 五、回写清单（本次已执行）

对《Forge → NeoForge 兼容层开发手册 v1.1》执行以下回写（只动事实/状态，不动架构）：

| 位置 | 动作 |
|---|---|
| 顶部状态块（line 26） | `Spike-002-0c ⏸ 未开始` → `✅ 完成（PASS-002-0c-1）`；新增 002-0d / 002-0e 两行 |
| 版本标签（line 30） | 追加 "+ Spike-002-0c 回写（含 G-1 认定）" |
| §3.8.9 之后 | 新增 **§3.8.10 层 e 之后：FML 生命周期事实基线（Spike-002-0c 回写）** |
| §十一 子链状态头（line 1090） | "截至 002-0b Step 2-A v1.1" → "截至 Spike-002-0c" |
| §十一 子链清单（line 1110） | 002-0c → ✅ 完成；新增 002-0d（待冻结）/ 002-0e（未开始） |
| 状态对照表（line 1328） | "层 e 之后的生命周期 ⏸ 待 002-0c" → ✅ 002-0c 观测结论 |
| 结尾状态块（line 1391/1394–1395） | 002-0c → ✅ 完成；下一步改为 002-0d → 002-0e |

---

## 六、哈希基线（本次审查新增/沿用）

```
forgecompat-002-0b-2a-v1.1.jar   E27BB9659303DDEDF2EDE02D10B49CAFC586549C0EDA003A0165DF525F97A436 / 20316 B / 25 条
forgeprobe.jar                  A93A823C988BFD6A8F6EA132D189D9DE732B0E5951F4BF74980D51BB63374E22 / 1772 B
forgecompat-001b1.jar（归档）   14EFD54FA91E415C5815972B81D16C2895DD9ED52A427154DB1261EFA532E334 / 13215 B / 15 条
forgecompat.jar（build，重建）   95B7398AED7DA33665E3623C190749A8B2B95EF14CA5624D33E9310089FBA5CE / 13215 B / 15 条
ForgeProbe.rewritten.class      5E2F78DB2B0909D7368A185F618C3D080C00B732279B105CD5B8010FD66D9295 / 1162 B
mods.toml（探针）               B32CF69B88FBE053C2580FC739B30FEDC43D96984C72FF78CD6725C7F3A1CE6E
```

---

## 七、交回信息（给执行方与顾问方）

- **给执行方（Codex）**：暂无执行动作；等待 **002-0d 说明书**（决策方起草，含逐字修复定义）冻结后执行。
- **给顾问方（GPT）**：请针对以下三点给出反向意见——
  1. G-1 归因链（[F]/[E] 逐环）是否存在反例；
  2. 修复方向 (a)（复刻内置 javafml 对 `neoforge` 容器）是否优于 (b)（Plan B 改名）；
  3. 002-0e 探针契约（位置 / 预登记 / 判据）是否有漏洞。
- **不预设**：002-0d 的具体实现细节、002-0e 之后的方向 3，均不在本报告内裁定。

> 纪律提醒：`class loaded ≠ stage entered`；`CLEAR ≠ 修复成功`；`从正例不反推必要条件`。
> 本次同时适用一条新注记：**"显示值 ≠ 功能值"**（`javafml@null` 与 `JVM Flags` 两例均为显示层现象，
> 须以字节码/独立证据为准）。
