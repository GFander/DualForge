# Spike-002-0c 裁判裁定修订（反向意见处理）

**文件性质**：对《Spike-002-0c 裁判审查与裁定报告》（2026-10-07，下称"第 1 版"）的修订（第 2 版）。
**角色**：架构决策方（裁判）
**输入**：

- 《对〈Spike-002-0c 裁判审查与裁定报告〉的反向意见》（顾问方，2026-10-07）
- 《Spike-002-0c G-1 四项前置取证（执行方）》（含 5 份证据存档，2026-10-07）

**独立复核**：裁判（见 §一；动作清单见 §六）
**效力**：本文件与第 1 版冲突处，以本文件为准；未涉及条款（含裁定 1 的 PASS 结论、F2/F3、哈希基线）维持第 1 版。
**边界**：本文件 = 只读复核 + 裁定修订；未修改任何探针、日志、实验报告、说明书；未执行手册回写（见 §七）。

---

## 一、四项取证复核（裁判独立动作与判定）

| # | 声称（取证文档） | 裁判独立复核（动作） | 判定 |
|---|---|---|---|
| 1 | `neoforge.mods.toml` 声明 `modLoader="javafml"`、`modId="neoforge"` | 直接从 `neoforge-21.1.256-universal.jar` 重抽 `META-INF/neoforge.mods.toml`，与存档逐字比对 → IDENTICAL | ✅ `[F]` 成立 → F2 前提闭合 |
| 2 | `FMLJavaModLanguageProvider.loadMod` = 纯注解筛选链 + `FMLModContainer` 构造 | 重跑 `javap -p -c -constants`（loader-4.0.45.jar）与存档比对：172 行，内容逐字符一致（仅指令行缩进 2 空格格式差）；原文核对筛选链与调用 | ✅ `[F]` 成立 |
| 3 | `languageProviders`（List）保留全部实例；`languageProviderMap`（Map）仅覆盖；`findLanguage` 取 map 胜者 | 重跑 javap 比对（223 行一致）；原文核对 ctor / `lambda$new$0` / `findLanguage` | ✅ `[F]` 成立 |
| 4 | `FMLLoader.getLanguageLoadingProvider()`（public static）+ `forEach/applyForEach`（public，均遍历 list） | 重跑 javap 比对（510 / 45 行一致）；确认访问器与两个方法均直接遍历 `languageProviders` | ✅ `[F]`（API 层）；运行时可达性 = `[H]`，待 002-0d 实测 |

复核注记：

- 重跑与存档的差异仅为 javap 版本格式（指令行前导空白差 2 空格）；去掉行首空白后，四个文件逐行逐字符一致。
- 顾问意见中 `loadMod(IModFile, ModFileScanData)` 为笔误；实际签名 = `loadMod(IModInfo, ModFileScanData, ModuleLayer)`（`[F]`，F2 存档 + 本次重跑）。
- 取证 4 的"时序前提"（`loadMod` 调用时 loader 对象已就绪）属 `[H]`-支撑；002-0d 实测复核。
- 证据存档实际路径 = `D:\DualForge\spike000\logs\spike002-0c-artifacts\`（取证文档中为相对路径省略）。

### 复核增补（裁判在复核中新增的两条 [F]，超出四项取证范围）

**F2-a｜兼容 provider 现状（冻结 jar `E27BB965…` 字节码）**

`com.example.forgecompat.ForgeCompatLanguageLoader.loadMod(info, scanData, layer)` 的行为：

1. 遍历 `scanData.getAnnotations()` 全量注解，收集 `annotationType().getClassName().equals("net.minecraftforge.fml.common.Mod")` 的类名；
2. 打印 `[FORGECOMPAT-LANG] loadMod called for {modId} entrypoints={list}`；
3. **无论 list 是否为空**，都 `new FMLModContainer(info, list, scanData, layer)` 并返回。

→ 这解释了 G-1 的机制面：`neoforge` 容器得到的是"entrypoints 为空的 FMLModContainer"（容器被构造、无入口类被实例化），而非"未构造容器"。
→ 同时可见：compat 的 `loadMod` 与内置链相比缺 `modId 过滤 / dist 过滤 / 侧数排序` 三处语义（对 Forge 容器为现状差异，`[F]`）。

**F2-b｜validate 调用侧（loader-4.0.45.jar 字节码）**

`IModLanguageLoader.validate` 为接口 default 方法（no-op；兼容 provider 未覆写）。调用侧 `net.neoforged.fml.ModLoader.buildMods`：容器按 `modInfo.getLoader()` 分组（`Map<loader, Set<container>>`）后，对 **map 的每个条目**调用 `loader.validate(modFile, containerSet, reporting)` `[F]`。

→ 在 Plan A 覆盖下，javafml 容器的 `getLoader()` = 兼容 provider（map 胜者），故内置 `FMLJavaModLanguageProvider.validate`（dangling entrypoint 检查）不运行；兼容 provider 的 validate 为 no-op。
→ **记录为已知观测，不纳入 G-1 与 002-0d 的修复变量**（不属于恢复 NeoForge 核心 mod 构造的必要条件）；如需处理，另立子 Spike 裁定。

---

## 二、裁定修订 1：F1 的语义推到底（采纳顾问方 §一）

维持 **PASS-002-0c-1**（第 1 版裁定 1 的结论不变）。补充/修订语义：

1. **002-0c 的核心产出 = 将 G-1 从"沉默失败"正式承认为判定对象**，而非"观测到生命周期终点"。
2. G-1 起于 002-0b Step 2-A v1.1（同一 Registry 失败已发生，但受"层 e 即 STOP"约束未分析）；002-0c 与 v1.1 构成**两轮确定性复现**（`[E]`）。
3. **"生命周期事实基线"仅对"存在 G-1 的基线"成立**——"层 e 之后 FML 止步于 Registry initialization"不得被引用为"主机生命周期的正常事实"，只能引用为"G-1 条件下的被截断事实"。
4. 002-0d 修复后**必须重建零变量基线**；新基线（jar 哈希 + 日志 + meta）冻结后，旧 002-0c 归档值仅作历史对照（第 1 版裁定 2 已含此条，此处升格为强制）。
5. 不修改已冻结的《Spike_002-0c.md》《Spike-002-0c 实验报告》与第 1 版裁定报告正文；本文件作为其修订件生效。

---

## 三、裁定修订 2：修复方向选定（采纳顾问方 §三，部分修订）

第 1 版"修复方向（[H] 候选）"中对 (a) 的预登记**撤回**（当时未冻结、未执行，可直接修订）。
在四项取证 + 复核增补基础上，裁定如下。

### 3.1 方向选定（[D]）

| 顺序 | 方案 | 状态 |
|---|---|---|
| **首选** | **(c) 委托内置 javafml 实例**——对"无 Forge `@Mod` entrypoints 的 javafml 容器"将 `loadMod` 委托给 list 中的内置实例 | **启用** |
| 备选 | (a') 扩展注解筛选（= 在兼容 provider 内复刻内置 5 步链的 NeoForge 侧） | 仅当 (c) 被 `[F]` 级证据证伪时启用；不得在运行中静默切换 |
| 次选 | (a) 复刻——与 (a') 合并（取证 2 已排除"其它副作用"顾虑） | 同上 |
| 末选 | (b) Plan B 改名 | 不启用（跨组件、语义面大） |

选定 (c) 的理由（全部可回溯到 `[F]`）：

1. **低侵入优先**（v1.1 §1.2）：不复制内置逻辑、不改语言名、不动 Forge 容器路径。
2. **可行性已过前置取证**：list 保留全部实例（取证 3）+ 公开遍历路径存在（取证 4）。
3. **避免语义漂移**：复核增补 F2-a 显示 compat 现行链表与内置存在三处差异；委托可让非 Forge 容器继续使用内置语义，而 (a') 会把复刻链固化为第二实现。
4. **单变量可控**：变更仅发生在"非 Forge 容器"的路由分支；Forge 容器（ForgeProbe）路径字节码级保持原样。

### 3.2 002-0d 说明书强制要件（冻结前必须全部具备）

1. **附件**：四项取证 + 本修订 §一复核结论；方向 = (c)。
2. **单变量定义**（沿用第 1 版）：仅允许变更"兼容 provider 对非 Forge 容器的接管行为"；其余全部冻结（probe jar、mods.toml、四项前置配置、描述符重写、NeoForge/FML/JVM 参数）。
3. **逐字定义 1（委托目标）**：通过 `FMLLoader.getLanguageLoadingProvider()` 遍历 `languageProviders` list，取 `name().equals("javafml")` 且**非自身实例**的加载器；实例判别方式必须显式写出（如 `loader != this` + 类名等值校验），防止自递归与多实例歧义；不得依赖 `languageProviderMap`（其 "javafml" 条目已被自身覆盖）。候选不唯一或不可确定 → STOP。
4. **逐字定义 2（路由条件）**：对"无 Forge `@Mod` entrypoints 的容器"执行委托（判定表达式必须逐字给出并可验证）；Forge 容器走现有路径，行为冻结。
5. **逐字定义 3（委托调用）**：直接调用内置实例的 `loadMod(IModInfo, ModFileScanData, ModuleLayer)`（签名以字节码为准），返回其 `ModContainer`；不得复制其内部逻辑，不得修改内置实例状态。
6. **源码前置**：先从冻结 jar（`E27BB9659303DDEDF2EDE02D10B49CAFC586549C0EDA003A0165DF525F97A436`）恢复/重建 `ForgeCompatLanguageLoader` 等源文件并显式归档（001b-6 `configOf` 教训制度化）。
7. **PASS 判据（002-0d-1 候选）**：修复后重跑零变量基线——
   - `NeoForge mod loading` 回归（neoforge 容器 entrypoints 非空、`NeoForgeMod` 被构造）；
   - FML 越过 Registry initialization；
   - ForgeProbe 路径不受影响（marker 命中、`REWRITE hits=1`）；
   - 记录新终点（完整启动 → VALID 级基线；新断层 → 原样记录，不修复）；
   - 新 compat jar 哈希 + 日志/meta 冻结。
8. **强制对照证据**：修复前后 compat jar 的 `javap -p -c -constants` 对比，证明变更仅落在委托分支（Forge 路径指令语义不变）；恢复源码的归档清单。
9. **STOP 条件**：委托目标查找失败 / 出现递归 / 内置实例不可达（含模块可访问性被 `[F]` 证伪）/ 出现未预登记的新断层 → 原样记录并 STOP，交回裁定；不得自行改走 (a')/(b)。
10. **观察项（记录、不修复）**：F2-b 的 validate 归属现状；委托运行时的类/模块可访问性（`[H]` → 期待 002-0d 升格为 `[E]`）。

### 3.3 不预设事项（维持）

002-0d 的具体代码形态、修复后的新断层、002-0e 之后的方向 3——均不在本修订内裁定。

---

## 四、裁定修订 3：002-0e 探针契约收紧（采纳顾问方 §四）

维持第 1 版裁定 3 的探针本体（API、位置、顺序、观察项），在其上增加两条纪律。

### 4.1 纪律 A：单变量 = "源码仅新增一条语句"，以语义级字节码对比证明

1. 源码唯一变量 = 在 `ForgeProbe` 构造函数**全部 marker 输出之后、`return` 之前**新增一条语句：
   `MinecraftForge.EVENT_BUS.register(this);`
   （现行构造器 `[F]`：`super()` → 4 次 `println`（`====` / `FORGE PROBE CONSTRUCTOR REACHED` / `BUS = …` / `====`）→ `return`；新增语句插在 `return` 前。）
2. 002-0e 报告必须附 `javap -p -c -constants` **新旧 ForgeProbe.class 对照**（强制字段）：
   - 对比规则：**忽略常量池 `#索引`与常量池顺序**（重建必然变化），按"指令助记符 + 解析后的目标成员/常量（字段、方法、字符串语义）+ 分支结构"逐条对比；
   - marker 段（4 次 println 序列）与 `super()` 调用必须逐条语义一致；
   - 方法体内唯一新增 = 新语句对应的指令组（预期 3 条：`getstatic MinecraftForge.EVENT_BUS` / `aload_0` / `invokeinterface IEventBus.register`；若 javac 产生等价形态，须逐条列出并与该语句一一对应）；
   - 除该新增组外，不得有任何其它指令增删改（异常表、行号表的语义变化须列出）。
3. 附：旧/新 jar 哈希与字节数、旧/新 rewritten dump 哈希、`REWRITE hits`（预期仍 = 1）。

### 4.2 纪律 B：首个失败点双预登记（[H]），任一为合格形态

| 编号 | 预登记失败 | 触发条件 |
|---|---|---|
| P1 | `NoClassDefFoundError: net.minecraftforge.common.MinecraftForge` | 类不存在（首选；依据：转换器仅重写 `<init>` 描述符 `[F]`） |
| P2 | `NoSuchMethodError: net.minecraftforge.eventbus.api.IEventBus.register` | 若 `MinecraftForge` 类可达且 `EVENT_BUS` 字段可得，但空壳 IEventBus（`[F]`：无任何方法的空接口）缺 `register` |

- 任一出现 = **PASS-002-0e-1**（方向 2 的首个断层坐标；P1 = 命名空间级缺失；P2 = 方法级缺失）；报告须给出失败类型、消息、完整 stack、触发位置（marker 之后）。
- 出现其它首失败（如 `NoSuchFieldError`、其它 `LinkageError`）→ 原样记录 + STOP 交回，不得自行扩展解释。
- marker 未命中 / BUS 行未打印 → STOP（前置错误）。

---

## 五、对反向意见的逐条处置

| # | 反向意见要点 | 处置 |
|---|---|---|
| 1 | F1 语义推到底（贡献 = 承认 G-1；基线条件性；修复后重建） | **采纳**（§二；002-0d 判据升格） |
| 2 | F2 补取证：universal.jar metadata | **采纳并闭合**（取证 1 复核 ✅；F2 归因链前提关闭） |
| 3 | 补取证：内置 `loadMod` 全行为 | **采纳并闭合**（取证 2 复核 ✅；"复刻副作用"顾虑排除） |
| 4 | 修复方向漏 (a')/(c)；先取证再选 | **采纳**（取证 3/4 复核 ✅；本修订选定 (c)，(a') 备选） |
| 5 | 单变量不严格：需字节码对比 | **采纳并加严**（§四 4.1：语义级对比规则 + 忽略常量池索引） |
| 6 | 预期失败应双预登记 | **采纳**（§四 4.2：P1/P2） |
| 7 | F3 三注正确 | **维持**（第 1 版 §三·F3 不变） |
| 8 | 不立即冻结 002-0d、先做四项取证 | **已完成**（取证 + 独立复核；002-0d 可进入起草；冻结前须满足 §三 3.2 全清单） |
| 9 | 002-0c 报告不回改 | **维持**（本修订只产生新文件；冻结文件保持原样） |

---

## 六、复核可追溯性（裁判本轮动作清单）

- 重抽 metadata：`tar -xOf neoforge-21.1.256-universal.jar META-INF/neoforge.mods.toml` → 与 `G1-F1-neoforge.mods.toml.txt` IDENTICAL。
- 重跑 javap（jdk-24，`-p -c -constants`，cp = `loader-4.0.45.jar`）四类：`FMLJavaModLanguageProvider`(172 行) / `LanguageProviderLoader`(223) / `BuiltInLanguageLoader`(45) / `FMLLoader`(510)；行数与内容逐字符一致（仅指令缩进格式差）。
- 冻结件哈希复测：`forgecompat-002-0b-2a-v1.1.jar` = `E27BB965…F97A436` / 20316 B；`forgeprobe.jar` = `A93A823C…63374E22` / 1772 B。
- 新增抽读 `[F]`：`ForgeCompatLanguageLoader`（F2-a）、`net.minecraftforge.eventbus.api.IEventBus`（空接口）、`IModLanguageLoader`（validate 默认方法）、`ModLoader`（validate 调用侧，F2-b）、`ModFile.identifyLanguage`（`findLanguage` 入口）。
- 临时展开的 jar 目录位于系统临时目录（只读复制 + 展开），未触碰工作区冻结文件。

---

## 七、治理与回写（待用户裁定项，本文件不执行）

1. **本轮未执行任何手册回写**：第 1 版裁判已落盘的 7 处手册回写保持现状（其治理问题仍待用户裁定）。
2. 本修订产生的待回写内容（002-0d 方向 (c)、002-0e 两条纪律、F1 条件性基线语义及其对已回写 §3.8.10 的修正范围）**预登记**，待 002-0d/002-0e 完成并最终裁定后统一处理。
3. 裁判建议：对"手册回写"采用 **先审后落盘**（回写清单先交用户/母代理确认，再写手册）；裁定文档与取证文档按任务产出正常落盘。若用户选择"直接落盘"，请明确授权范围。**在裁定前，裁判不再写手册。**

---

## 八、状态与下一步

- 当前状态：PASS-002-0c-1（第 2 版语义）维持；G-1 方向选定 (c)；002-0e 契约收紧完毕；002-0d / 002-0e 尚未冻结执行。
- 下一步（顺序不可交换）：
  1. 起草 **002-0d 说明书**（按 §三 3.2 清单，含逐字定义）；冻结前交审。
  2. 执行 002-0d → 交裁判裁定（PASS / FAIL / STOP）→ 新基线冻结。
  3. 起草 **002-0e 说明书**（按 §四）；执行 → 裁定方向 2 首个断层坐标。
- 不可变更项：002-0e 不得先于 002-0d；不预设 002-0e 之后的任何方向；不拼接证据；边界纪律（层 e 之后已开放，但不得越过 002-0e 裁定范围）。

> 纪律提醒：`[F] / [F-j] / [E] / [H] / [D]` 标签纪律；从正例不反推必要条件；单路径结论限定单路径；
> `class loaded ≠ method invoked ≠ stage entered ≠ operation succeeded`；`CLEAR ≠ 修复成功`。
> 本修订新增引用一条：**"基线是有条件的"**——G-1 修复前的"生命周期事实基线"仅对 G-1 条件成立，不得外推。
