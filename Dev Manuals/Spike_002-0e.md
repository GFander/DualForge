# Spike-002-0e 执行任务说明书 —— 方向 2 探针 #1（首个 Forge API 调用断层坐标）

**接收方**：实践代理 `/root/executor_oct11`；独立裁判 `/root/referee_oct11`；总指挥 `/root`。
**编制**：架构决策方（裁判代理）
**状态**：**v1.0 冻结版（已交付）**。授权：《总指挥令 R-0e-1（自动推进与双代理恢复）》；审查记录：《Spike-002-0e 冻结审查记录（2026-10-11）》；旧 v0.1 保留。冻结后执行方不得改写本契约。
**日期**：2026-10-11（Asia/Hong_Kong）
**轮次性质**：独立审查与冻结；本裁判未运行实验，运行证据由实践代理依本版采集。

**依据**：

- 《总指挥令 R-0d-4（002-0e 起草指派与裁定回复）》§三（任务 B 规格）
- 《Spike-002-0c 裁判裁定修订（反向意见处理）》§四（002-0e 探针契约：纪律 A / 纪律 B）
- 《Spike-002-0d 裁判审查与裁定报告》§六（新基线冻结登记）/ §七（002-0e 方向操作化移交）
- 《Spike-002-0d 执行报告》（自评 VALID-002-0d-1，经裁判确认）
- 《Forge → NeoForge 兼容层开发手册 v1.1》§3.5 / §3.6.1 / §3.8.9 / §3.8.10
- harness 快照：`D:\DualForge\spike000\logs\harness-baseline\`（`run-server.ps1` + `_baseline-note.txt` + `run-server.ps1.sha256.txt`）

**任务性质**：单因子探针（源码唯一变量 = 新增一条语句）+ K1 对照（Run C + Run T）
**执行上限**：首个确认失败（P1/P2/其它）锁定分析边界；允许固定 harness 完成同一进程的原始日志、完整异常链、meta 与终止状态采集。失败后的生命周期日志只作附件，不继续解释、不修复、不复跑。若到达 Done 且未观测失败，按 §7.3 交回。

---

## 〇、背景（母说明书：为什么是 002-0e）

### 0.1 位置

002-0d 已实证清零 G-1（兼容 provider 对非 Forge 容器的委托使 `NeoForgeMod` 被构造、FML 越过 `Registry initialization`、完整到达 `Done`）；新基线（treatment jar `4CD8195F…`）已冻结登记（《Spike-002-0d 裁判审查与裁定报告》§六）。

002-0e 是本方向的**第一个探针**：把 Forge 模组构造函数体从"只打印"推进到"第一次调用真实 Forge API"，以定位方向 2 的**首个失败坐标**。

### 0.2 本 Spike 唯一目标

在 `ForgeProbe` 构造函数**全部 marker 输出之后**新增唯一一条语句：

```java
MinecraftForge.EVENT_BUS.register(this);
```

观测该语句在运行时的**首个失败**（预登记 P1 / P2 双形态），给出：失败类型、消息、完整 stack、触发位置（marker 之后）。

### 0.3 与相邻轮次的职责分离

| 项目 | 002-0d（已完成） | **002-0e（本 Spike）** | 002-0f 及以后 |
|---|---|---|---|
| 目标 | 清零 G-1，重建有效基线 | 方向 2 首个断层坐标 | 修复该断层 / 下一探针 |
| 变量 | 1（委托分支） | 1（新增 register 语句） | 后续裁定 |
| 运行 | 2（Run C + Run T） | 2（Run C + Run T） | 后续裁定 |
| 判据 | VALID-002-0d-1（已确认） | 本文件 §七 | 后续裁定 |
| compat 侧输入 | 冻结 v1.1 恢复 + 修复 | **固定 = 002-0d treatment jar（不得改动）** | 后续裁定 |

### 0.4 本 Spike 不解决

- **不修复**任何失败（含 P1 / P2 命中的断层）——修复属下一轮 Spike 的裁定范围；
- 不进入方向 3；不选择/不设计任何修复方案；
- 不新增 Forge API 调用（唯一新增语句 = `MinecraftForge.EVENT_BUS.register(this);`，逐字）；
- 不改动 compat jar / 四项前置 / 描述符重写逻辑 / 冻结探针 jar / probe `mods.toml`；
- 不评估"注册是否成功后的行为"（如监听器语义、事件分发）——本 Spike 只到"首个失败"为止；
- 不设计后续 Spike。

### 0.5 成败的上限（防误读）

PASS 只证明：**在本次条件下**，第一次真实 Forge API 调用在预登记坐标处失败（或按其双形态之一失败）。不得推导：

> "Forge API 全部不可用" / "ABI 断层已定性" / "兼容层方案需要改为 X" / "方向 3 应做 Y"。

---

## 一、唯一目标与执行上限

**唯一目标**：执行"新增 register 语句"的探针变更，在 K1（control + treatment）下观测 **Run T 的首个失败**，并把它作为方向 2 的首个断层坐标原样交回。

**执行上限**：

- Run T 出现首个确认失败即**锁定分析终点**；固定 harness 可继续到自然退出或既有超时，保留完整栈与终止元数据，禁止据后续生命周期扩大结论；
- 首个失败**不修复**；原样记录（类型 / 消息 / 完整 stack / 触发位置）后交回；
- 若 Run T 到达 `Done`（未观测任何失败），按固定 harness 的既有 stop 流程结束并交回（FAIL，见 §7.3）；未到 Done 的无失败退出/超时按 STOP（观测不足）。

**不判定**：断层性质 / 修复方向 / 兼容层可行性 / 方向 3。

---

## 二、单变量定义与 K1 设计

### 2.1 唯一的变量（源码级）

仅允许变更：`ForgeProbe` 构造函数体——在**全部 marker 输出之后**新增**一条语句**（含必要 `import`），逐字定义见 §五 5.1。

- 变更载体 = **探针 jar**（`forgeprobe.jar` 的新构建产物）；**不是** compat jar；
- 除 §五 5.1 的 1 行 import + 1 行语句外，源码**逐字不变**（含空白与换行）；
- compat 侧、harness 运行参数、服务端、Java、NeoForge/FML/ModLauncher 全部冻结（§三）。

### 2.2 冻结项（不得变更）

1. **compat 侧固定输入** = 002-0d 新基线 treatment jar：
   `D:\DualForge\spike001a\compat-002-0d\build-treatment\forgecompat-002-0d.jar`
   SHA-256 = `4CD8195F7AA03B8B595F646CDEAC16454D03D5E3862B5E2F388C820CF2FA2FEF` / 21180 B / 25 条
   （**两轮运行使用同一文件；不得以任何方式修改、重建或替换**）
2. **冻结探针 jar**（Run C 输入 + 字节码对照基线）：
   `D:\DualForge\spike000\probe\build\forgeprobe.jar`
   SHA-256 = `A93A823C988BFD6A8F6EA132D189D9DE732B0E5951F4BF74980D51BB63374E22` / 1772 B / 7 条目
3. **probe `mods.toml`**：`B32CF69B88FBE053C2580FC739B30FEDC43D96984C72FF78CD6725C7F3A1CE6E` / 241 B
   （3 份副本逐字节一致：`probe\src\main\resources\META-INF\mods.toml`、`probe\build\jar-root\META-INF\mods.toml`、`probe\build\verify\META-INF\mods.toml`）
4. **四项前置语义**（observer / service 声明 / 真实 ModFileInfo / 空壳 IEventBus）：随 §2.2-1 的 compat jar 固定，**不得改动**；
5. **描述符重写逻辑**（`ForgeProbeConstructorTransformer` 语义）：随 §2.2-1 固定；本 Spike 不得修改、不得扩展其重写面；
6. **probe 源码基线**（改前）：`D:\DualForge\spike000\probe\src\main\java\com\example\forgeprobe\ForgeProbe.java`
   SHA-256 = `6092CC3D402F6D0132A8F15C6A298BC1AE8D4EFFBD8CD91B01C9D734FC1EBBF5` / 455 B
7. NeoForge / FML / ModLauncher / Minecraft / Java 版本（§三）；
8. JVM 参数 `-Dforgecompat.2a.rewrite=true`（harness 固定 `-Xmx3G -Xms1G`）；
9. `run-server.ps1` 除 §2.5 的 ValidateSet 追加 2 个 Label 外，其余逻辑**逐字不得动**。

### 2.3 K1 对照（control + treatment；本稿裁定 = 重跑 control）

| 运行 | 探针 jar | compat jar | 与另一运行的唯一差异 |
|---|---|---|---|
| **Run C**（control，先） | 冻结探针 jar（`A93A…`） | `4CD8195F…` | — |
| **Run T**（treatment，后） | **新探针 jar**（§五构建产物，叶名 `forgeprobe.jar`） | `4CD8195F…` | 仅探针 jar（源码级唯一新增 = 1 import + 1 语句） |

**顺序纪律**：先完成构建与静态硬门，再 Run C，硬门通过后 Run T；两轮使用同一哈希的变更后 harness 与 compat。两轮之间禁止修改冻结输入、新探针产物和源码；允许只读校验、证据归档，以及固定 harness 正常产生的运行配置/日志/运行时 mods 副本/世界保存/dump。每轮前确认对应 Label 目录不存在；存在即 STOP，不得调用会覆盖同名目录的 harness。

**可比性边界**：世界文件随既有 harness 运行自然变化，未宣称两轮整个服务端目录逐字节相同；不新增世界重置变量。本轮定位构造函数新增语句，若出现世界或其它无关阶段异常，原样交回 STOP，不能归因于 register。

**冻结裁定**：**不复用** 002-0d Run T 作为 002-0e 的 control 基线，而是按上表重跑 Run C。理由：

1. 项目 K1 纪律（《裁判角色交接文档》2.3）："多一次运行换取'唯一变量'的严格性。即使代价是加一次构建 + 一次运行，也应采用"；
2. 本轮 harness 将发生一次受控变更（ValidateSet 追加 2 个 Label）；重跑 control 使**两轮在同一 harness 实例、同一会话**下执行，消除"跨 Spike 复用 + 跨 harness 实例"两项额外论证负担；
3. 002-0d Run T 保持其"新基线首次运行"的历史地位（只读参照），不因角色转换被二次引用。

> 本版固定采用本轮新 Run C；历史 002-0d Run T 仅为参照，不替代本轮 control。

### 2.4 探针文件名硬约束（`[F]`）

`ForgeModLocator`（compat 侧，固定输入）仅识别**固定路径** `mods\forgeprobe.jar`：

```java
Path probe = Paths.get("mods", "forgeprobe.jar");
if (!Files.exists(probe)) { … return; }
```

⇒ **新探针 jar 的叶名必须为 `forgeprobe.jar`**（harness 以 `-ModsJar` 的叶名复制进 `mods\`）。不得通过修改 locator、改名投放或其它方式绕过本约束。

### 2.5 harness 变更（唯一允许：ValidateSet 追加 2 个 Label）

**基线**（只读快照）：`D:\DualForge\spike000\logs\harness-baseline\run-server.ps1`
SHA-256 = `F5319A54086C5BF7EBC8441774F358BC10B74F99C90471A526CAA2D5707FADCA` / 8900 B

**允许的唯一变更**（行 16，ValidateSet 追加 `'spike002-0e-control'`、`'spike002-0e-treatment'`）：

变更前：

```
    [Parameter(Mandatory = $true)][ValidateSet('baseline', 'forgeprobe', 'spike001a-0', 'spike001b-1', 'spike001b-3', 'spike001b-3b', 'spike001b-5', 'spike001b-6', 'spike001c-0', 'spike001c-1', 'spike001c-2', 'spike002-0a', 'spike002-0b-2a-control', 'spike002-0b-2a', 'spike002-0b-2a-control-v1.1', 'spike002-0b-2a-v1.1', 'spike002-0c', 'spike002-0d-control', 'spike002-0d-treatment')][string]$Label,
```

变更后：

```
    [Parameter(Mandatory = $true)][ValidateSet('baseline', 'forgeprobe', 'spike001a-0', 'spike001b-1', 'spike001b-3', 'spike001b-3b', 'spike001b-5', 'spike001b-6', 'spike001c-0', 'spike001c-1', 'spike001c-2', 'spike002-0a', 'spike002-0b-2a-control', 'spike002-0b-2a', 'spike002-0b-2a-control-v1.1', 'spike002-0b-2a-v1.1', 'spike002-0c', 'spike002-0d-control', 'spike002-0d-treatment', 'spike002-0e-control', 'spike002-0e-treatment')][string]$Label,
```

**机械 diff 原文**（起草侧生成方式：`git diff --no-index`，快照副本 vs 变更后副本；已去除 autocrlf 警告行。执行侧须对**快照 vs 实际** `run-server.ps1` 重做同构机械 diff 并记入执行报告）：

```diff
diff --git "a/.\\harness-baseline\\run-server.ps1" "b/.\\live\\run-server.ps1"
index df8ca70..9bfbf97 100644
--- "a/.\\harness-baseline\\run-server.ps1"
+++ "b/.\\live\\run-server.ps1"
@@ -13,7 +13,7 @@
     baseline run and the forgeprobe run is the -ModsJar argument.
 #>
 param(
-    [Parameter(Mandatory = $true)][ValidateSet('baseline', 'forgeprobe', 'spike001a-0', 'spike001b-1', 'spike001b-3', 'spike001b-3b', 'spike001b-5', 'spike001b-6', 'spike001c-0', 'spike001c-1', 'spike001c-2', 'spike002-0a', 'spike002-0b-2a-control', 'spike002-0b-2a', 'spike002-0b-2a-control-v1.1', 'spike002-0b-2a-v1.1', 'spike002-0c', 'spike002-0d-control', 'spike002-0d-treatment')][string]$Label,
+    [Parameter(Mandatory = $true)][ValidateSet('baseline', 'forgeprobe', 'spike001a-0', 'spike001b-1', 'spike001b-3', 'spike001b-3b', 'spike001b-5', 'spike001b-6', 'spike001c-0', 'spike001c-1', 'spike001c-2', 'spike002-0a', 'spike002-0b-2a-control', 'spike002-0b-2a', 'spike002-0b-2a-control-v1.1', 'spike002-0b-2a-v1.1', 'spike002-0c', 'spike002-0d-control', 'spike002-0d-treatment', 'spike002-0e-control', 'spike002-0e-treatment')][string]$Label,
     [string]$ModsJar = '',
     [string[]]$ExtraModsJars = @(),
     [string[]]$ExtraJvmArgs = @(),
```

**预期变更后指纹**（起草侧机械复算）：SHA-256 = `84C7E2C06BF0A1D2AC0D0E2D5AD20482F1357668EC8807DAD642DA55944F77D0` / 8948 B（+48 B）。
执行侧应得到同一值；若不同，须记录实际值并说明（不得静默接受其它形态的变更）。

**硬要求**（依 R-0d-4 §二-2）：自本 Spike 起，`run-server.ps1` 的任何变更必须以该快照为机械 diff 基线；**diff 原文记入执行报告**。

---

## 三、精确环境（冻结）

```
Minecraft:            1.21.1
NeoForge:             21.1.256
FancyModLoader:       4.0.45
ModLauncher:          11.0.5
Java:                 21（ms-21.0.10；javac / javap / jar = 21.0.10）
OS:                   Windows 11 amd64
启动方式:             CLI / Dedicated Server
```

路径与冻结引用：

| 项 | 路径 | 指纹 |
|---|---|---|
| **compat 固定输入**（002-0d 新基线 treatment jar） | `D:\DualForge\spike001a\compat-002-0d\build-treatment\forgecompat-002-0d.jar` | `4CD8195F7AA03B8B595F646CDEAC16454D03D5E3862B5E2F388C820CF2FA2FEF` / 21180 B / 25 条 |
| 冻结探针 jar（Run C 输入） | `D:\DualForge\spike000\probe\build\forgeprobe.jar` | `A93A823C988BFD6A8F6EA132D189D9DE732B0E5951F4BF74980D51BB63374E22` / 1772 B / 7 条目 |
| probe `mods.toml` | `D:\DualForge\spike000\probe\src\main\resources\META-INF\mods.toml`（+ 2 副本） | `B32CF69B88FBE053C2580FC739B30FEDC43D96984C72FF78CD6725C7F3A1CE6E` / 241 B |
| FML loader | `D:\DualForge\spike000\neoforge-server\libraries\net\neoforged\fancymodloader\loader\4.0.45\loader-4.0.45.jar` | `8390398E67DF23E72D85ED809685BE2752C8EC545B7CBB6AD4B70FDC81E4DCD4` / 505696 B |
| 编译 classpath ① javafmllanguage | `D:\DualForge\spike000\forge-server\libraries\net\minecraftforge\javafmllanguage\1.21.1-52.1.0\javafmllanguage-1.21.1-52.1.0.jar` | `D27E1F3149EA08FC0A80779FE24FA8585692C5042A34BAD153E55662486ACC15` / 19235 B |
| 编译 classpath ② eventbus | `D:\DualForge\spike000\forge-server\libraries\net\minecraftforge\eventbus\6.2.27\eventbus-6.2.27.jar` | `D2A7BCECB80B33F90854A003DC295960263D78435D9D6016C427D711510CB999` / 58819 B |
| 编译 classpath ③ **universal（本 Spike 新增冻结点）** | `D:\DualForge\spike000\forge-server\libraries\net\minecraftforge\forge\1.21.1-52.1.0\forge-1.21.1-52.1.0-universal.jar` | `88EF6625122CFDB9D7B3176BD61B8C0692A191BAD0EDD77FFDDC6A765EE4979C` / 2617057 B |
| harness 快照 | `D:\DualForge\spike000\logs\harness-baseline\run-server.ps1` | `F5319A54086C5BF7EBC8441774F358BC10B74F99C90471A526CAA2D5707FADCA` / 8900 B |
| 历史 rewritten dump（对照基线） | `D:\DualForge\spike000\logs\spike002-0b-2a-v1.1\ForgeProbe.rewritten.class` | `5E2F78DB2B0909D7368A185F618C3D080C00B732279B105CD5B8010FD66D9295` / 1162 B |
| 冻结 v1.1 参照 jar（只读历史对照，**不作本轮输入**） | `D:\DualForge\spike001a\compat\build\forgecompat-002-0b-2a-v1.1.jar` | `E27BB9659303DDEDF2EDE02D10B49CAFC586549C0EDA003A0165DF525F97A436` / 20316 B / 25 条 |

**新增冻结点的取证**（字节码/签名为 `[F]`；jar 成员清单为制品事实；执行侧须独立复测）：

- `net/minecraftforge/common/MinecraftForge.class` ∈ `forge-1.21.1-52.1.0-universal.jar`（`jar tf` 取证）；**不在** javafmllanguage / eventbus 两件中；
- `javap`：`public static final net.minecraftforge.eventbus.api.IEventBus EVENT_BUS;`；
- `javap`（eventbus-6.2.27）：`public abstract void register(java.lang.Object);`；
- compat jar 内空壳 `net.minecraftforge.eventbus.api.IEventBus` = **无成员接口**（`javap` 取证）。

**服务端 / 日志 / 证据目录**：

- 服务端：`D:\DualForge\spike000\neoforge-server\`
- 工作副本（建议）：`D:\DualForge\spike000\probe-002-0e\`——**不得覆盖/删除现有 `probe\` 目录内容**；若采用其它布局，须在报告写明并满足同样的归档不变量
- 日志目录：`D:\DualForge\spike000\logs\spike002-0e-control\`、`...\spike002-0e-treatment\`
- 证据目录：`D:\DualForge\spike000\logs\spike002-0e-artifacts\`

**compile-only 隔离硬门**：上述 Forge javafmllanguage / eventbus / universal 三件只准进入 javac classpath；不得打包进 probe，不得放进 NeoForge mods、JVM classpath/module-path 或服务发现目录。每轮运行时 mods 清单必须恰为指定 probe + compat 两件，实际投放副本哈希复测与本轮输入一致。新增依赖是编译输入，不是运行变量。

Label（§2.5 追加，其余逻辑不得动）：`spike002-0e-control`、`spike002-0e-treatment`

**运行命令**（K1：先 C 后 T；`<new probe jar>` = §五构建产物路径，叶名必须为 `forgeprobe.jar`）：

```
D:\DualForge\spike000\run-server.ps1 -Label spike002-0e-control `
  -ExtraJvmArgs '-Dforgecompat.2a.rewrite=true' `
  -ModsJar D:\DualForge\spike000\probe\build\forgeprobe.jar `
  -ExtraModsJars D:\DualForge\spike001a\compat-002-0d\build-treatment\forgecompat-002-0d.jar

D:\DualForge\spike000\run-server.ps1 -Label spike002-0e-treatment `
  -ExtraJvmArgs '-Dforgecompat.2a.rewrite=true' `
  -ModsJar <new probe jar> `
  -ExtraModsJars D:\DualForge\spike001a\compat-002-0d\build-treatment\forgecompat-002-0d.jar
```

---

## 四、子步骤

### 2E-a 取证与冻结项复测（不运行）

1. 复测 §三 全部冻结引用哈希（逐项比对——**任一不匹配 → STOP**）；
2. 复测 harness 快照与现行 `D:\DualForge\spike000\run-server.ps1` 哈希（执行前应 = 快照值；若已变更 → STOP）；
3. 独立重做 §三"新增冻结点的取证"四项（`jar tf` / 三次 `javap`）；
4. 记录：`javac -version` / `javap -version` / `jar --version` 原文；JDK 路径。
5. 首轮前将硬编码共享 dump 的现态另存为 `spike002-0e-artifacts\historical-ForgeProbe.rewritten.class`，哈希必须仍为 `5E2F78DB2B0909D7368A185F618C3D080C00B732279B105CD5B8010FD66D9295`。该副本成为历史证据的只读载体；共享路径为本轮运行输出，允许固定 transformer 覆盖，不再对它要求运行后仍是历史哈希。现有历史其余日志不动。
6. 确认 control/treatment Label 目录不存在，当前无待保留的服务端进程；核实固定 harness 将清理的 server mods/logs/crash-reports 均为已归档实验运行副本。未经归档的新现场不得覆盖。

产出：`spike002-0e-artifacts\2Ea-frozen-refs-recheck.txt`（哈希逐项 + 工具版本 + 取证结论）。

**STOP**：任一哈希不匹配、取证结论与 §三 不符、工具版本异常。

### 2E-b 探针源码变更与归档

1. 归档（**先于任何修改**）：
   - 冻结探针 jar 只读副本 → `<work>\archive\forgeprobe-frozen-A93A.jar`（复测哈希）；
   - 改前源码树 → `<work>\archive\src-before\`（`src\main\java` + `src\main\resources` 全量复制）+ 逐文件 SHA-256 清单；
2. 按 §五 5.1 **逐字**变更（新增 1 行 import + 1 行语句；其余逐字不变）；
3. 归档改后源码树 → `<work>\archive\src-after\` + 逐文件 SHA-256 清单；
4. 行级 diff（改前 vs 改后）原文落盘（预期：仅 2 行新增）。

**硬门**：改前源码树与 `probe\src` 逐文件一致；改后源码树与改前唯一差异 = `ForgeProbe.java`（且仅 2 行新增）。

**STOP**：出现第 3 处差异 / 需要改动其它文件。

### 2E-c 构建 treatment 探针 jar（`forgeprobe.jar`）

按既有已验证的 javac + 手工打包流程（参照 Spike-000 §三 与 002-0b Step2-A v1.1 §2A-c 的形态）：

- 编译：`ms-21.0.10` 的 `javac`（§五 5.2 逐字命令；classpath = §三 三件）；
- 装配 `<work>\build\jar-root\`：`META-INF\MANIFEST.MF`（自 `probe\src\main\resources` 逐字节复制）+ `META-INF\mods.toml`（自 §2.2-3 逐字节复制）+ 编译产物 `com\example\forgeprobe\ForgeProbe.class`；
- 打包：§五 5.2 逐字命令 → 产物 `<work>\build\forgeprobe.jar`（**叶名硬约束**，§2.4）；
- 归档副本：`<work>\archive\forgeprobe-002-0e.jar`（须与构建产物同哈希）；
- 记录：编译/打包命令原文（含 classpath）、工具版本、产物路径 / SHA-256 / 字节数 / `jar tf` 条目清单（对照冻结 jar 的 7 条目结构）。

**STOP**：编译 / 打包非 0 退出、条目结构不符、叶名无法满足 §2.4。

### 2E-d 语义级字节码对照（纪律 1）

- old = 冻结 jar 内 `com/example/forgeprobe/ForgeProbe.class`；new = 新 jar 内同名类；
- 双份 `javap -p -c -constants` 落盘（另加 `javap -p -l` 供行号表差异核验）；
- **对照规则**：忽略常量池 `#索引` 与常量池顺序；按"指令助记符 + 解析后的目标成员/常量（字段、方法、字符串语义）+ 分支结构"逐条对比；
- 必须证明（全部满足）：
  1. `<init>` 描述符不变（`(Lnet/minecraftforge/eventbus/api/IEventBus;)V`）——descriptor 重写匹配前提保持；
  2. `super()` 调用 + 4 次 `println` 序列（`====` / `FORGE PROBE CONSTRUCTOR REACHED` / `BUS = …` / `====`）逐条语义一致；
  3. 方法体内**唯一新增** = 新语句对应的 3 条指令组：
     `getstatic net/minecraftforge/common/MinecraftForge.EVENT_BUS` / `aload_0` / `invokeinterface net/minecraftforge/eventbus/api/IEventBus.register:(Ljava/lang/Object;)V`；若 javac 产出等价形态，须逐条列出并与该语句一一对应；
  4. 除该指令组外**无任何增删改**；异常表差异（预期无）与行号表差异须列出（**既有执行语句因 import 新增行号 +1；新增语句与末尾 return 的行号另行按源码逐项核验，不得声称全表简单 +1**）；
- 条目级对照：`jar tf` 条目集合与顺序（对照冻结 jar）；`MANIFEST.MF` / `mods.toml` 逐字节一致；使用冻结相同 JDK/jar 工具，`Created-By` 亦应一致。除 `ForgeProbe.class` 外内容文件**零差异**（ZIP 时间戳/压缩封装不作为内容文件差异，仍记录整个 jar 新哈希）。

**STOP**：任一差异超出上述允许范围（"近似实现"不得继续）。

**历史编制预检（起草侧，2026-10-08；编译结果属历史 `[E]`，反汇编结构属 `[F]`；本裁判未复跑，不作为本轮执行证据）**：§五 5.1 的改后源码已用 `ms-21.0.10` javac 对 §三 三件 classpath 编译通过（exit 0）；`javap -p -c -constants` 复核：`<init>` 描述符不变；新增指令组 = offsets 43/46/47 的三条（`getstatic` / `aload_0` / `invokeinterface`），`return` 移至 offset 52；对照实验：同一源码对**不含 universal** 的旧两件 classpath 编译失败（2 errors，`net.minecraftforge.common` 缺失）⇒ universal 为本轮必需冻结点。执行方仍须在正式流程中重建并记录。

### 2E-e K1 运行（Run C → Run T）

**Run C（control）**：命令见 §三；**期望 = 复现 002-0d Run T 冻结签名**（下表；行号仅参照，按内容与因果证据判定，不按 stdout 行号强制总顺序。时间戳、BUS 地址、时长、线程调度、并行消息交错及运行环境生成的非判据数据可变化；关键语义信号不准缺失）：

| # | 期望观测 | 对照（002-0d Run T） |
|---|---|---|
| 1 | `[FORGECOMPAT-LANG] loadMod called for forgeprobe entrypoints=[com.example.forgeprobe.ForgeProbe]` | 1 行（L64） |
| 2 | `offered: com/example/forgeprobe/ForgeProbe` | 1 行（L65） |
| 3 | `REWRITE hits=1 old=(Lnet/minecraftforge/eventbus/api/IEventBus;)V new=(Lnet/neoforged/bus/api/IEventBus;)V` | 1 行（L66） |
| 4 | `post-check ctorCount=1 oldAbsent=true newPresent=true` | 1 行（L67） |
| 5 | `dump OK path=…\ForgeProbe.rewritten.class bytes=1162`；dump 归档副本哈希 = `5E2F78DB…` | 1 行（L68） |
| 6 | `[FORGECOMPAT-LANG] loadMod called for neoforge entrypoints=[]` | ≥1 行（L69） |
| 7 | `[FORGECOMPAT-LANG] delegate -> builtin javafml for neoforge` | 1 行（L70） |
| 8 | marker 四条逐字命中（`====` / `FORGE PROBE CONSTRUCTOR REACHED` / `BUS = net.neoforged.bus.EventBus@…` / `====`；**实际捕获顺序记录**） | L71–74 |
| 9 | `NeoForge mod loading` | 1 行（L75） |
| 10 | `Done (` 行命中（时间值不冻结） | L95 |
| 11 | meta：`DONE_STOPPED_GRACEFULLY` / exitCode 0 / duration ≈ 24.7s（时长不冻结）；crash-report 无；stderr 0 B | 对照 |

**STOP**：任一关键项不符（尤其 marker / BUS / delegate / `Done (` 缺失，或复现出不同的失败链）→ 环境漂移或基线失真，STOP 交回。

**Run T（treatment）**：命令见 §三；**期望观测（原始捕获顺序全量记录，但不作为程序执行总顺序）**：

| # | 期望 | 说明 |
|---|---|---|
| 1 | 同 Run C 的 #1–#5（probe 路径） | 新 jar：`REWRITE hits=1` 预期不变；`dump OK bytes=…` = **新值**（记录，不预登记） |
| 2 | 同 Run C 的 #6–#7（neoforge 路径） | 委托调用信号应存在；不以该信号独自推断本轮 neoforge 构造已完成 |
| 3 | marker 四条逐字命中（顺序记录） | 层 e 再次进入；marker 之后不再 STOP（002-0c 起开放） |
| 4 | **首个失败 = P1 或 P2**（纪律 2，§七） | 完整 stack 原文 + 触发位置（marker 之后、新增指令组） |
| 5 | FML 上报形态与进程终止：crash-report（如有）/ FATAL 行 / meta（status / exitCode / duration） | 全量记录，不解释 |
| 6 | `NeoForge mod loading`：命中或缺席**均如实记录**（构造阶段被截断为预期可能形态之一，不构成 STOP） | 并行构造顺序不作因果推断 |
| 7 | mods-list / stderr（预期 0 B）/ 两轮产物清单 | 全量记录 |

**分析终点**：出现首个确认失败（P1/P2/其它）即锁定；按 §一 允许完成同轮日志、异常链及 meta 采集。固定 harness 不增加 stop-on-exception 逻辑；不得为了截断进程而丢弃完整异常链。若超时，仅记录既有 harness 处置，不复跑、不修复。

### 2E-f 清理与冻结登记

- 归档：两轮日志（latest / debug / stdout / stderr）/ meta / mods-list；crash-report（如有）；每轮的 rewritten dump 副本（**Run C 后先归档 dump，再执行 Run T**——dump 路径被 transformer 硬编码共享，Run T 会覆盖同一文件）；
- 登记：新探针 jar（§2E-c 产物 + 归档副本）哈希 / 字节数 / 条目清单；Run T dump 哈希 / 字节数（新值）；
- 再复测全部冻结输入哈希（确认未被修改）；harness 应为许可变更后的固定哈希；历史 dump 核验只读归档副本，共享路径核验 Run T 新值，不把合法输出覆盖误判成冻结输入漂移；
- harness：机械 diff 原文 + 变更前后 SHA-256 落盘；
- 若 Run T 出现新断层：**不得修复**，原样交回裁定（下一轮 Spike 的候选输入）。

---

## 五、逐字定义（决策方权威）

> 语义逐字定义；变更面之外不得有任何改写。任何语义偏差必须交回裁定。

### 5.1 源码变更（逐字；改后全文）

```java
package com.example.forgeprobe;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;

@Mod("forgeprobe")
public class ForgeProbe {
    public ForgeProbe(IEventBus bus) {
        System.out.println("=================================");
        System.out.println("FORGE PROBE CONSTRUCTOR REACHED");
        System.out.println("BUS = " + bus);
        System.out.println("=================================");
        MinecraftForge.EVENT_BUS.register(this);
    }
}
```

与冻结基线（改前）的差异**仅**：新增 `import net.minecraftforge.common.MinecraftForge;`（第 3 行）+ 新增 `MinecraftForge.EVENT_BUS.register(this);`（第 14 行）；其余逐字不变。

### 5.2 构建命令（逐字模板）

```
& 'C:\Users\Albert\.jdks\ms-21.0.10\bin\javac.exe' --release 21 -encoding UTF-8 `
  -classpath "D:\DualForge\spike000\forge-server\libraries\net\minecraftforge\javafmllanguage\1.21.1-52.1.0\javafmllanguage-1.21.1-52.1.0.jar;D:\DualForge\spike000\forge-server\libraries\net\minecraftforge\eventbus\6.2.27\eventbus-6.2.27.jar;D:\DualForge\spike000\forge-server\libraries\net\minecraftforge\forge\1.21.1-52.1.0\forge-1.21.1-52.1.0-universal.jar" `
  -d "<work>\build\classes" `
  "<work>\src\main\java\com\example\forgeprobe\ForgeProbe.java"
```

```
& 'C:\Users\Albert\.jdks\ms-21.0.10\bin\jar.exe' --create --file "<work>\build\forgeprobe.jar" `
  --manifest "<work>\src\main\resources\META-INF\MANIFEST.MF" `
  -C "<work>\build\jar-root" .
```

（PowerShell 模板：`<work>` 替换为已核验工作副本绝对路径；classpath **必须含三件**且仅用于编译；完整实际命令、输出、退出码先后落盘。`jar-root` 内容见 §2E-c。不把模板补填冒充此前已执行命令。）

### 5.3 运行命令（逐字模板）

见 §三（Run C / Run T 两条命令；`<new probe jar>` 叶名 = `forgeprobe.jar`）。

### 5.4 harness 变更（逐字）

仅 §2.5 的单行变更（ValidateSet 追加 `'spike002-0e-control'`, `'spike002-0e-treatment'`）；无其它任何改动。

### 5.5 禁止事项

- 不得修改 compat jar（`4CD8195F…`）、冻结探针 jar（`A93A…`）、probe `mods.toml`（`B32C…`）、四项前置语义、描述符重写逻辑；
- 不得修改新探针 jar 的 `MANIFEST.MF` / `mods.toml` / 类数（唯一差异 = `ForgeProbe.class` 内的新增指令组）；
- 不得修改服务端 / NeoForge / FML / ModLauncher / Java / JVM 参数；
- 不得使用反射 / Unsafe / 额外转换器；
- 不得修复首个失败、不得进入方向 3、不得追加任何新 API 探针；
- 不得改动 `run-server.ps1` 的 ValidateSet 以外任何字节。

---

## 六、必须记录（观测项）

1. 环境冻结项：§三 全部哈希复测值 + 工具版本原文；
2. 探针源码变更：改前/改后归档路径 + 逐文件 SHA-256 + 行级 diff 原文；
3. 构建记录：编译/打包命令原文、工具版本、产物路径 / SHA-256 / 字节数 / 条目清单；
4. 语义级字节码对照（纪律 1）：`javap -p -c -constants` 与 `-l` 双份；逐条差异清单（既有语句 +1、新语句与 return 按实际源码逐项映射）；条目级对照结论；
5. Run C 全量签名对照表（§2E-e 表 1–11 逐项原文）+ 产物清单 + dump 归档哈希；
6. Run T 全量：日志序列（§2E-e 表 1–7）、**首个失败的类型 / 消息 / 完整 stack / 触发位置**、FML 上报形态、meta、crash-report（如有）、mods-list、stderr；
7. ForgeProbe 证据：两轮 `offered` / `REWRITE hits` / `post-check` / dump 哈希与字节数；
8. harness 机械 diff 原文 + 变更前后 SHA-256（R-0d-4 §二-2 强制）；
9. 边界自检清单（§九末）；
10. 与说明书的**逐条偏差**（如有，必须单列一节）。

---

## 七、判据

### 7.1 PASS-002-0e-1（合格形态：首个断层坐标成立）

全部满足：

- `[E]` §二 冻结输入哈希复测匹配；`[F]` 2E-d 字节码对照通过（唯一新增 = §5.1 语句对应指令组；marker 段 + `super()` 逐条语义一致；其余条目零差异）；
- `[E]` Run C 复现 002-0d Run T 冻结签名（§2E-e 表 1–11，按已操作化的关键语义信号判定）；
- `[E]` Run T 出现 §2E-e 表 1–3（probe 路径 + 委托 + marker）；
- `[E]` **Run T 首个失败 = P1 或 P2**（纪律 2 双预登记）：
  - **P1** = `NoClassDefFoundError: net.minecraftforge.common.MinecraftForge`
  - **P2** = `NoSuchMethodError: net.minecraftforge.eventbus.api.IEventBus.register`
  - 命中判定：异常类型 + 类名 / 方法名语义匹配（JVM 消息渲染差异——斜杠 vs 点号、引号、描述符后缀——不作为否决条件；**原文全量记录**）；任一命中 = 合格形态；
- `[E]` 首个失败**触发位置在 marker 之后**：失败 stack 含 `com.example.forgeprobe.ForgeProbe.<init>` 帧，且 marker/BUS 实际命中，源文件、行号表与字节码控制流证明新增语句位于四次 println 之后；原文上下文及捕获乱序如实保留。stdout 行序不等于执行时序，不得排序日志后冒充原文；完整 stack 落盘；
- `[E]` 两轮 dump：Run C = `5E2F78DB…` / 1162 B（冻结值）；Run T = 新值（记录）；
- `[E]` 新终点与进程终止形态如实记录（crash-report / FATAL / meta）。

**结论上限**：仅证明"本次条件下，首次真实 Forge API 调用在 P1/P2 坐标处失败"；不得推导断层性质 / 修复方案 / 兼容层可行性。

### 7.2 VALID-002-0e-1（更强形态）

满足 7.1 全部条件，**且**失败坐标以"根因 → 上报链"完整闭合：

- 根因（P1/P2）可由构造函数 stack 帧、源码行号及字节码映射定位到 §2E-d 的新增语句/指令组；单一源码行含多条指令，没有 BCI 证据时不得宣称精确到 `getstatic` 或 `invokeinterface`。潜在解析机制只列 `[H]`，不作为 VALID 的伪精确证明；
- FML / 日志侧的上报链完整（崩溃报告或等效 FATAL/异常链原文，含 wrap 层与根因层）；
- 无超出本版允许范围的未解释语义差异；已登记的日志捕获顺序差异不单独阻塞，成因继续标为未知。

（若任一环节不完整 → 记 PASS 而非 VALID；不得为凑 VALID 追加复跑。）

### 7.3 FAIL-002-0e-1

Run C 正常、静态硬门与 Run T 前置/marker 均通过，Run T 到达 `Done`，完整捕获中**未观测到任何首失败**：预登记的本轮失败结果未出现，记 FAIL（实验预测层级，非项目失败）。仅正常退出/exitCode 0 但无 Done，或捕获不完整，记 STOP（观测不足），不得据缺少错误行推断 register 成功。

→ FAIL，交回裁定；不得自行追加探针或改变语句形态。

### 7.4 STOP-002-0e-1

见 §八 汇总表。STOP 后：不得修复、不得修改探针、不得扩展分析；可完成 §一 规定的同轮原始证据与终止状态采集，报告原样交回。

### 7.5 新产物登记（待裁定后冻结）

- 新探针 jar（`forgeprobe.jar` 新构建）：哈希 / 字节数 / 条目清单；
- Run T dump（新哈希 / 字节数）；
- 两轮日志 / meta / 证据档；改前 / 改后源码归档。

### 7.6 P2 的边界说明（`[H]`，冻结登记）

P1 是预期的先行形态；但未完成整个有效类搜索空间的穷尽证明，不把“无提供模块”写成已证事实。P2 仅在 `MinecraftForge` 可得且此前未失败等条件满足时才可能成为首失败；不得只凭空接口就宣称某个具体错误必然发生。双预登记形态**仍按契约保留**（P1 / P2 任一命中均为合格形态）；本说明不改变判据。

---

## 八、STOP 条件（汇总）

| 情况 | 处理 |
|---|---|
| 冻结引用哈希不匹配（compat jar / 冻结探针 / mods.toml / loader / 三件 classpath / harness 快照） | STOP |
| 2E-a 改动前 `run-server.ps1` ≠ 快照哈希；或许可改动后的 diff 超出 ValidateSet 单行；或两轮期间 harness 哈希变化 | STOP |
| 改前源码树与 `probe\src` 不一致；或改后差异超出唯一 import + 唯一语句这两行新增 | STOP |
| 编译 / 打包失败；条目结构不符；叶名无法满足 `forgeprobe.jar` | STOP |
| 2E-d 出现超出预期范围的字节码 / 条目差异 | STOP |
| Run C 未复现 002-0d Run T 冻结签名（环境漂移 / 基线失真） | STOP |
| marker 未命中（marker 行或 BUS 行缺席） | STOP（前置错误） |
| Run T 首个失败为 P1 / P2 之外的形态（如 `NoSuchFieldError`、其它 `LinkageError`、`VerifyError`、marker 之前失败） | 原样记录（完整 stack）+ STOP，交回裁定 |
| Run T 首失败涉及委托机制，或其它未预登记形态 | 原样记录 + STOP；不按并行 `NeoForge mod loading` 日志行的先后猜测归因 |
| 需要修改本说明书之外的文件、或引入新工具链 / 新依赖才能继续 | STOP |
| Run T 到达 `Done` 且完整捕获未观测失败，其他前置均通过 | 记录 + FAIL 路径（§7.3），交回裁定 |
| Run T 未达 Done 且无可确认首失败（含正常退出、超时、日志不足） | STOP（观测不足） |
| Run T 出现首个失败（P1 / P2） | 锁定分析终点；完成同轮异常链/meta归档（PASS/VALID路径；不修复） |

任何 STOP / 终止后：不得自行设计修复方案；不得进入方向 3；报告原样交回。

---

## 九、结构化输出格式（强制；参照 002-0d §九 结构）

```
# Spike-002-0e 执行报告

## 一、实验环境
- Minecraft / NeoForge / FancyModLoader / ModLauncher / Java:
- 冻结引用哈希复测（compat jar / 冻结探针 / mods.toml / loader / 三件 classpath）:
- 工具版本（javac / javap / jar）:
- JVM 参数:
- Label（control / treatment）:
- meta.json（两轮）status / exitCode / duration / doneLine:

## 二、取证与冻结项复测（2E-a）
- 逐项哈希比对结论:
- universal 新增冻结点取证（jar tf / javap ×3）:

## 三、探针源码变更与归档（2E-b）
- 改前 / 改后归档路径 + 逐文件 SHA-256:
- 行级 diff 原文（预期 2 行新增）:

## 四、构建记录（2E-c）
- 编译命令原文 / 退出码 / 工具版本:
- 打包命令原文 / 退出码:
- 产物路径 / SHA-256 / 字节数 / 条目清单:

## 五、语义级字节码对照（2E-d，纪律 1）
- <init> 描述符不变确认:
- super() + 4 次 println 序列逐条对照:
- 唯一新增指令组（逐条）:
- 异常表 / 行号表差异（既有语句 +1；新语句与 return 单独对应）:
- 条目级对照结论（MANIFEST / mods.toml / 条目集合与顺序）:

## 六、Run C（control）
- 签名对照表（§2E-e 表 1–11，逐项原文）:
- dump 归档哈希:
- 完整日志 / meta / mods-list 清单:

## 七、Run T（treatment）
- 日志序列（§2E-e 表 1–7，逐条原文，不得摘要）:
- 首个失败：类型 / 消息 / 完整 stack 原文 / 触发位置（marker 之后）:
- FML 上报形态 / 进程终止 / crash-report（如有）:
- marker 实际捕获顺序（含乱序记录）:
- NeoForge mod loading：命中 / 缺席（如实）:
- dump 归档哈希（新值）:
- meta / mods-list / stderr:

## 八、判据自评（含纪律 2 命中形态）
- 结果: PASS-002-0e-1 / VALID-002-0e-1 / FAIL-002-0e-1 / STOP-002-0e-1
- 依据（逐条对应 §七）:
- P1 / P2 命中形态与原文:
- 结论上限（禁止超出）:

## 九、事实标签
[F] Source Fact:
[E] Experimental Fact:
[H] Hypothesis:
[D] Design Decision:

## 十、与说明书的偏差
- 逐条记录（如有）:

## 十一、边界自检
- [ ] 未修改 compat jar / 四项前置 / 描述符重写逻辑
- [ ] 未修改冻结探针 jar / probe mods.toml
- [ ] 仅新增 1 行 import + 1 行语句（改后源码与改前唯一差异 = ForgeProbe.java 2 行新增）
- [ ] 新探针 jar 叶名 = forgeprobe.jar；条目结构 = 7 条目
- [ ] 未修改 NeoForge / FML / ModLauncher / Minecraft / Java / JVM 参数
- [ ] run-server.ps1 仅 ValidateSet 追加 2 个 Label（机械 diff 原文已附）
- [ ] 未修复首个失败 / 未进入方向 3 / 未追加新 API 探针
- [ ] control 先于 treatment 执行（K1 顺序）
- [ ] 每轮 dump 已归档（Run C 先；Run T 覆盖后已另存）
- [ ] 改前 / 改后源码均已归档

## 十二、原始产物清单
- control / treatment 的 latest / debug / stdout / stderr / meta / mods-list / crash-report:
- artifacts 目录清单与逐文件字节数:
- 新探针 jar（构建产物 + 归档副本）与 dump 副本:
```

---

## 十、交接规则

执行完成后，报告原样交回架构决策方；不得自行决定下一 Spike。根据结果：

- **PASS / VALID** → 决策方登记首个断层坐标 → 起草下一轮（修复或下一探针）说明书；
- **FAIL** → 交回；由决策方裁定预登记前提的处置（不得执行方自改探针）；
- **STOP** → 交回；由决策方裁定前置 / 机制问题。

---

## 十一、审查裁定与交接

1. 采纳本轮新 Run C + Run T；冻结 compat，仅变更 probe 的必要 import 与唯一 API 语句。
2. P1/P2 均为 `[H]` 预登记；异常语义匹配，原文保留；四次 println 不变。
3. 正式采用首轮前历史 dump 归档 + 每轮 dump 立即另存；Run T 前 Run C 证据必须已封存。
4. 三件 Forge 编译依赖冻结且只用于 javac，不投放运行环境。
5. harness 仅两 label 追加，机械 diff 对照快照；两轮期间冻结其哈希。
6. 首失败之后只采集完整异常链和终止附件；不用 stdout 捕获顺序代替控制流，不用单行 stack 代替精确 BCI。
7. PASS/VALID/FAIL/STOP 按 §七操作化；Done无失败表示预测未出现，非 API 成功证明。
8. 实践代理收到本文件及完整 SHA 后执行；报告交独立裁判、抄送总指挥。手册由总指挥在裁定后作事实回写。

---

## 十二、交付清单

```
1. 本说明书（v1.0 冻结版）
2. 归档至：D:\DualForge\Dev Manuals\Spike_002-0e.md（旧送审稿另存保留）
3. Label：spike002-0e-control / spike002-0e-treatment（ValidateSet 追加 2 个）
4. 冻结引用（用于两轮）：
   - forgecompat-002-0d.jar（002-0d 新基线）  4CD8195F…2FEF / 21180 B / 25 条
   - forgeprobe.jar（冻结探针）               A93A823C…374E22 / 1772 B
   - probe mods.toml                          B32CF69B…CE6E / 241 B
   - loader-4.0.45.jar                        8390398E…DCD4 / 505696 B
   - javafmllanguage / eventbus / universal    D27E1F31… / D2A7BCEC… / 88EF6625…
   - run-server.ps1 快照                      F5319A54…ADCA / 8900 B
5. 新产物（执行后）：
   - 新探针 jar（forgeprobe.jar，含 1 行 import + 1 行 register 语句）
   - Run C / Run T 日志 / meta / dump 归档
   - 改前 / 改后源码归档 + 逐文件哈希
```

---

## 十三、本 Spike 的真正使命

002-0e 不是为了证明 Forge API "不能用"，也不是为了修复任何断层。

它只做一件事：**把"层 e 之后的第一个真实 Forge API 调用"从不可观测变为可判定的失败坐标**——以 K1 + 语义级字节码对照的双重封闭，证明唯一变量就是那条 register 语句，并把首个失败的完整形态原样交回。

证据链目标：

```
[E] 冻结输入哈希复测（compat 新基线 + 冻结探针 + 三件 compile-only classpath）
        ↓
[F] 语义级字节码对照（唯一新增 = 3 指令组；marker 段逐条不变）
        ↓
[E] Run C：冻结探针复现 002-0d Run T 签名（基线保真）
        ↓
[E] Run T：新探针 → marker → 首个失败（P1 / P2）
        ↓
[D] 断言首个断层坐标 → 新产物登记 → 交回裁定
```

不要将：

"首次 Forge API 调用在 P1/P2 处失败"

写成：

"Forge API 不可兼容" / "断层已定性" / "需要 XX 修复"。

前者只说明本次条件下第一次接触真实 Forge API 的失败坐标；后者需要后续多轮探针与修复裁定。

---

*本版由独立裁判 referee_oct11 审查冻结；2026-10-11。执行代理不得自行改动冻结契约；首失败按本版交回。*

