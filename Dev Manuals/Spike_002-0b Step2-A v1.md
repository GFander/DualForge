# Spike-002-0b Step 2-A 执行任务说明书（v1 冻结版）

**接收方**：Codex
**编制**：执行方代拟（批次 2）；经架构决策方两轮审阅；v1 冻结（2026-10-07）
**状态**：**v1 冻结版（已交付）**（已并入全部裁定、3 处补充与 2 处冻结前修正，见 附录 A；执行期不得再变更）
**日期**：2026-10-07

**前置文档**：

- 《Forge → NeoForge 兼容层开发手册 v1.1（架构冻结版·最终）》§3.5 / §3.6.1 / §3.8.9
- 《Spike-002-0b Step1 取证报告》
- 《Spike-002-0b-1 取证报告》
- 《Spike-002-0b-1b 取证报告》
- 《Spike_002-0b.md 母任务说明书（简版索引）》

**任务性质**：单因子运行探针（最小实现 + 运行观测）
**唯一变量**：`com/example/forgeprobe/ForgeProbe` 的**一条构造函数描述符重写**（仅此一处）
**执行上限**：**层 d 被解除 + 层 e 进入证据出现即 STOP**

---

## 〇、执行前置（已证事实与文档状态）

### 0.1 本 Spike 不重新验证的内容（引用已证 `[F]`）

| # | 已证事实 | 来源 |
|---|---|---|
| 1 | 通道 A 接入点 = `ITransformationService#transformers()`（在 §3.6 白名单内）→ `mods/` JAR 被提升进 **SERVICE 层**；通道 B 对第三方 `mods/` JAR 静态不可达 | 002-0b-1 §二；002-0b-1b §2.2 / §八 |
| 2 | ⑥ 静态调用点 = `ClassTransformer.performVote` → `invokeinterface ITransformer.transform(T, ITransformerVotingContext)`；CLASS 级实际 `T` = ASM `ClassNode`（已解析树，非 `byte[]`） | 002-0b-1b §4.3 / §5.1 |
| 3 | `targets()` 匹配 = **精确匹配**（ASM `Type` 归一化 + name/descriptor `equals`）；同一 transformer 的全部 target 必须同属一个 `TargetType` 且等于 `getTargetType()`；空 `targets()` 静默跳过 | 002-0b-1b §7 |
| 4 | ForgeProbe FQCN 满足静态目标条件 = **YES** | 002-0b-1b §7.4 |
| 5 | ⑥ 的注册（`Launcher.run` → `initialiseServiceTransformers`）**先于** ④ 的 entrypoint 类加载 | 002-0b-1b §6.3；手册 §3.6.1 |
| 6 | 层 d 失败对照锚点（002-0a 实测，原样引用）：`java.lang.RuntimeException: Mod constructor has unsupported argument interface net.minecraftforge.eventbus.api.IEventBus. Allowed optional argument classes: IEventBus, Dist, FMLModContainer, ModContainer` | 002-0a 报告 / 运行日志 |

**硬前提声明**：1–5 为本次运行判定的全部依据。运行中若发现其中任何一条不成立，报告的结论必须降级（按 §六 处理），不得用推断补齐。

### 0.2 文档状态

- 本稿为 **v1 冻结版（已交付）**：已并入全部裁定、3 处补充与 2 处冻结前修正（见 附录 A）；
- **执行期不得修改本说明书的任何段落**；唯一例外：2A-b 若发现与 §2.6 预勘不一致 → 按 §六 STOP 条款交回，不自行修改；
- 执行顺序：2A-a → 2A-b → 2A-c → 2A-d → 2A-e（先 Run C 后 Run T）→ 2A-f。

### 0.3 运行前置配置（Run C / Run T 共同）

两次运行都必须在兼容层 jar 中包含以下**空壳类型**（来自 002-0a 的已验证前置）：

```
net.minecraftforge.eventbus.api.IEventBus
public interface IEventBus {}
（无方法 / 无字段 / 无嵌套类型）
```

**理由**：本次实验的唯一变量是"rewrite 开关"。
若不注入该空壳类型，Run C 会在**层 c**（`NoClassDefFoundError`）就失败，
而 Run T 会通过层 c 并暴露层 d —— 两次运行的差异将不只是 rewrite，
从而破坏 K1 单变量设计。

**这一空壳类型的存在**是两次运行的共同前提，**不属于本次实验的变量**。
其技术依据为 002-0a 实验（参见《Spike-002-0a 实验报告》）。

---

## 一、唯一目标与执行上限

一次运行探针，只回答两个问题：

1. **层 d**：把 ForgeProbe 构造函数的唯一参数类型从 Forge 命名空间重写为 NeoForge 命名空间后，`FMLModContainer.constructMod()` 的参数白名单是否接受该构造函数（层 d 是否解除）？
2. **层 e**：`ForgeProbe.<init>` 是否被实际调用进入（证据 1 或证据 2，见 §2.5）？

**执行上限（STOP）**：层 e 证据出现即停止观察 —— 不得继续观察 Registry 行为 / EventBus 注册 / FMLJavaModLoadingContext / Minecraft 服务 / Mod 初始化流程 / 其它 Forge 模组生命周期。

**本 Spike 只记录一条 ABI 重写探针的结果**；不得由此推导通用兼容性（见 §十 证据边界与结论上限）。

---

## 二、关键设计决定

### 2.1 接入点（通道 A；已证 `[F]`）

- 兼容层 JAR 新增服务声明：`META-INF/services/cpw.mods.modlauncher.api.ITransformationService`；
- 该接口在 `TransformerDiscovererConstants.SERVICES` 白名单内 ⇒ 兼容层 JAR 经**既有提升路径**进入 SERVICE 层（不新增任何发现 / 提升机制）；
- Transformer 实现 `ITransformer<ClassNode>`，`TargetType.CLASS` 级；
  `targets() = Set.of(Target.targetClass("com.example.forgeprobe.ForgeProbe"))`（FQCN 或 internal 形式均可，归一化后精确匹配）；
- 转换输入是 ModLauncher 已解析的 `ClassNode`（本探针**不解析 `byte[]`、不新增解析层**）。

### 2.2 唯一变量 —— 一条构造函数描述符重写

```
com/example/forgeprobe/ForgeProbe
    <init>(Lnet/minecraftforge/eventbus/api/IEventBus;)V
        ↓ 仅改写 desc 字段
    <init>(Lnet/neoforged/bus/api/IEventBus;)V
```

允许的动作**只有**：命中 `(name = "<init>", desc = 旧描述符)` 的 `MethodNode`，把 `desc` 改为新描述符。

**不允许**：`instructions` / `tryCatchBlocks` / `localVariables` / 注解 / 其它属性任何改动；新建 / 删除 `MethodNode`；改写任何其它方法、任何其它类。

**重写开关（K1 单变量机制；已裁定，见 附录 A-④）**：Transformer 内 `rewriteEnabled` 由系统属性
`forgecompat.2a.rewrite` 决定（**未传即失败**，见下文；两次运行均显式传值）。
Run C 以 `-Dforgecompat.2a.rewrite=false`、Run T 以 `-Dforgecompat.2a.rewrite=true` 运行，
**两次运行使用同一个 jar**（`forgecompat-002-0b-2a.jar`），仅 JVM 参数不同。
`transform()` 入口先记录 `offered`，再按开关分流（关闭时原样返回，不触碰 ClassNode）。

**未传系统属性时的行为**：直接抛出 `IllegalStateException` 并在 ModLauncher 启动期失败。
**不采用**"未传则默认 false"的兜底 —— 防止 Run T 忘传参数时被静默降级为 Run C
而污染结论。缺省值语义为"未传即失败"，而非"未传即关闭"。

**预勘（本稿编制时的只读检查；执行时由 2A-d 正式复核并纳入报告）**——重写合法性依据（Frozen 环境的 forgeprobe.jar）：

- `ForgeProbe` 类 `methods = 1`（仅此构造函数），类文件 major version 65；
- 该构造函数**无 StackMapTable**（直线代码、无分支）→ 仅改描述符不会产生旧帧失配；
- **无 LocalVariableTable** → 无旧类型调试元数据残留；
- 构造函数体中引用参数的唯一方式：`aload_1 → String.valueOf(Object) → makeConcatWithConstants`，**无 checkcast、无对旧类型的调用** → 仅改描述符后字节码保持可验证。

### 2.3 ASM 策略（严格限定）

- ASM 仅 `compileOnly`：**不得**将 ASM 打包进兼容层 JAR（必须与 launcher 共享同一 `org.objectweb.asm` 模块，否则 `ClassNode` 类型身份分裂）；
- 版本 = 与运行时一致。预勘：runtime 模块路径解析到 **9.10.1**（asm / asm-tree / asm-analysis / asm-commons / asm-util）；执行时由 2A-a 正式取证并记录 SHA-256。libraries 中另有 9.3，但不在运行时模块路径上 —— 以 2A-a 结论为准；
- `ClassWriter` 标志 = **0**（禁止 `COMPUTE_FRAMES` / `COMPUTE_MAXS` —— 禁止重算帧 / 栈；重写不改变栈形状）；
- 不得升级 / 变更任何其它依赖。

### 2.4 观察层设计（本探针自身的证据）

统一日志前缀 `[FORGECOMPAT-XFORM]`，输出至 stdout（证据以 stdout.log 为准，latest.log 交叉核对）：

| 观察点 | 期望证据（形态） |
|---|---|
| 服务发现 | ModLauncher 启动列表新增行（示例形态）：`forgecompat.jar forgecompat-transform TRANSFORMATIONSERVICE`；既有 `fml` / `mixin` 行不变 |
| `initialize()` / `onLoad()` / `transformers()` 被调用 | 各 1 行日志（证明服务被 ModLauncher 实际驱动） |
| 重写开关解析 | `rewriteEnabled=false/true`（记录属性解析值；Run C / Run T 各 1 行） |
| 每次收到转换请求 | `offered: <internalName>`（逐条；Run C 与 Run T 结构一致；期望恰好 1 条且 = `com/example/forgeprobe/ForgeProbe`） |
| 执行重写 | `REWRITE <init> old=… new=…`（期望恰好 1 次） |
| 重写后结构自检 | `post-check ctorCount=1 oldAbsent=true newPresent=true`（在**同一 ClassNode** 上执行） |
| 异常兜底 | 任何内部异常 → 记录后**原样返回**（不得把异常抛回类加载链） |

**审计计数定义（强制；已按 审阅结论-③ 收紧）**：

- `rewrite target count` = 实际执行描述符改写的次数：
  - Run T 硬要求 = **1**；**rewrite > 1（无论 desc 是否相同）→ 直接判 FAIL**；
  - Run C 期望 = **0**（重写关闭）；
- `non-target transformed class count` = 实际被本 Transformer 修改（字节层面任何修改）的非 ForgeProbe 类数（期望 = 0）；
- 发现 / 加载 / 检查 / `offered` 但未修改 → **不计入** transformed 计数；另列原始 `offered` 清单；
- `offered` 计数 > 1 时：逐条记录 `internalName`；**同一 `internalName` 出现 > 1 次 → 记为 ANOMALY**
  （ModLauncher 对同一类的多阶段 offer 可能正常；不自动判 FAIL，但必须如实记录）。

**层期望（K1 两运行；共同前置见 §0.3）**：

- Run C：**层 c PASS**（IEventBus 空壳类型存在）→ **层 d FAIL**（`unsupported argument`，与 002-0a 同款）；
- Run T：**层 c PASS** → **层 d PASS**（构造函数签名已改写为 NeoForge IEventBus）→ 层 e 进入证据 → STOP。

### 2.5 层 e STOP 纪律（用户冻结版，复述）

任一证据满足即视为层 e 已进入，**立即 STOP**：

- **证据 1（marker）**：stdout 出现 `FORGE PROBE CONSTRUCTOR REACHED`；
- **证据 2（stack trace）**：异常 stack trace / `InvocationTargetException` 的 cause 明确包含帧 `at com.example.forgeprobe.ForgeProbe.<init>(ForgeProbe.java:…)`（即使构造函数第一行之前就失败，只要进入 `<init>` 帧即算）。

不得继续观察 Registry / EventBus 注册 / FMLJavaModLoadingContext / Minecraft 服务 / Mod 初始化流程。marker 之后的日志**仅作原始附件保存，不进入分析**。

### 2.6 与 FMLServiceProvider 共存的最小化（本 Spike 最高风险项）

兼容层服务将与 `fml` / `mixin` 同处于 TRANSFORMATIONSERVICE 相位空间。要求：

1. `name()` 唯一：`forgecompat-transform`（禁用 `fml` / `mixin`；报告须用启动列表证明实际注册名无冲突）；
2. 抽象方法最小无害实现，**不得为 null**、不得抛异常：
   `name()`、`initialize(IEnvironment)`、`onLoad(IEnvironment, Set<String>)`、`transformers()`；
   （预勘：上述 4 个为 abstract；执行时 2A-b 用 javap 正式复核抽象 / default 划分 —— 若与预勘不符，停止并交回，不得自行扩大实现面）；
3. 4 个 default 方法（`arguments` / `argumentValues` / `beginScanning` / `completeScan`）**不覆写**
   （不注册 CLI 参数、不贡献扫描资源）—— 见 附录 A-②。**附加取证要求（审阅补充）**：2A-b 须以
   `javap -c` 确认这 4 个 default 的实际默认行为为无害（返回空列表 / 空 / void 等）；若发现非平凡默认行为
   （例如 `arguments()` 注册非空参数、`beginScanning()` 返回非空资源）→"不覆写 = 零贡献"不再成立，
   **停止并交回**（不得自行覆写）；
4. 报告须**逐方法列出** `ITransformationService` 全部 8 个方法的处置（实现 / 不覆写）及实现体内容，以证明"未干扰 FML"；
5. 若服务端在到达层 d **之前**出现与历史运行不同（新增）的失败 ⇒ 视为**实现缺陷**，在范围内修正后重跑；无法在范围内修正 ⇒ STOP 交回。

### 2.7 红线（本 Spike 全程）

禁止：修改 NeoForge / FML 核心（`FMLModContainer` / `ModContainer` / `ModSorter` / `LoadingModList` / `ModInfo` / `ModFile` / `ModFileInfo` / `ModFileScanData` / `TransformStore` / `ClassTransformer`）；修改 forgeprobe.jar / mods.toml / ForgeProbe 源码；全局 Forge→NeoForge 映射；方法调用重写；字段描述符重写；改写方法体；Mixin / Registry / Event 兼容；ClassLoader 策略变更 / 新增 ClassLoader；ModuleLayer 结构修改；注入 IEventBus 实例；Adapter / Proxy；Unsafe / 其它字节码注入（除 §2.2 这一条描述符重写外）；同时或补试 B 分支；进入 002-0c。

---

## 三、精确环境（冻结）

```
Minecraft:            1.21.1
NeoForge:             21.1.256
FancyModLoader:       4.0.45（loader-4.0.45.jar）
ModLauncher:          11.0.5
Java:                 21（OpenJDK 21.0.10，Microsoft Build）
OS:                   Windows 11 amd64
启动方式:             CLI / Dedicated Server
```

路径与工具（不变）：

- 兼容层工程：`D:\DualForge\spike001a\compat\`（基线 = 001b-1：5 java + MANIFEST + 2 services；基线留档 `build\forgecompat-001b1.jar`）
- forgeprobe 制品：`D:\DualForge\spike000\probe\build\forgeprobe.jar`（SHA-256 `a93a823c988bfd6a8f6ea132d189d9de732b0e5951f4bf74980d51bb63374e22`，不得修改）
- 服务端与运行脚本：`D:\DualForge\spike000\neoforge-server\`、`D:\DualForge\spike000\run-server.ps1`
  - ValidateSet 追加两个 Label：`spike002-0b-2a-control`（Run C）、`spike002-0b-2a`（Run T）；
  - 新增可选参数 `-ExtraJvmArgs`（默认空；非空时追加进 `user_jvm_args.txt`），用于注入
    `-Dforgecompat.2a.rewrite=false|true`；不传时行为与基线一致（其余逻辑不动；diff 记入报告）。
- 日志目录：`D:\DualForge\spike000\logs\<label>\`
- JDK：`C:\Users\Albert\.jdks\ms-21.0.10\bin\`
- 服务端运行需沙箱外权限（回环 socket / DNS）

---

## 四、子步骤

### 2A-a：ASM 版本 / 制品取证（不运行）

- 从 `libraries\net\neoforged\neoforge\21.1.256\win_args.txt` 的模块路径 / legacyClassPath 静态读取运行时 ASM 制品清单；
- 记录：每个相关 ASM jar 的路径 + SHA-256（至少 `asm` 与 `asm-tree`）；
- 若正式结论 ≠ 9.10.1：以取证为准，记录与预勘的差异，并按"与运行时一致"原则选取编译用制品（不得即兴换版本）。

### 2A-b：SPI 方法清单正式取证（不运行）

- `javap -p -c -constants`：`ITransformationService`（含全部方法与抽象 / default 划分）、`ITransformer`、`ITransformer$Target`、`TargetType`、`TransformerVoteResult`；
- **default 行为取证（审阅补充）**：对 4 个 default 方法（`arguments` / `argumentValues` / `beginScanning` /
  `completeScan`）以 `javap -c` 逐字记录默认实现，确认默认行为无害（空列表 / 空 / void 等）；
  若发现非平凡默认行为 → 停止并交回（"不覆写 = 零贡献"不再成立）；
- 报告逐字记录 javap 原文（不得摘要）；与 §2.6 预勘对照；
- **停止条件**：若发现草案未列出的必须实现项（abstract 方法）→ 停止并交回（不得自行扩大实现面）。

### 2A-c：实现探针（兼容层工程最小改动）

**新增 4 个文件**：

1. `src\main\java\com\example\forgecompat\ForgeCompatTransformationService.java`
   —— `ITransformationService` 最小实现（§2.6）
2. `src\main\java\com\example\forgecompat\ForgeProbeConstructorTransformer.java`
   —— `ITransformer<ClassNode>`（§2.2 / §2.4；ClassWriter 标志 = 0）
3. `src\main\java\net\minecraftforge\eventbus\api\IEventBus.java`
   —— 空壳类型（§0.3；**Run C / Run T 共同前置**，非本实验变量）
4. `src\main\resources\META-INF\services\cpw.mods.modlauncher.api.ITransformationService`
   —— 单行 FQCN

**修改 1 个文件**：

- `build.gradle`：`compileOnly` 追加 ASM 制品（2A-a 结论版本；不升级其它依赖）

**构建**（按 001a-0 已验证的 javac 流程；Gradle 未安装）：

- 编译 classpath = `build\cp.txt` 既有条目 + 2A-a 的 `asm` / `asm-tree` 制品；输出 `build\classes`（**先清空重建**）；
- 重新装配 `build\jar-root\`（classes + MANIFEST + 3 个 services），打包 `build\forgecompat.jar`；
- 预检：`jar tf` = **23 条**（基线 15 + 2 class + 1 IEventBus class + 1 service + 4 个新增目录条目
  `net/`、`net/minecraftforge/`、`net/minecraftforge/eventbus/`、`net/minecraftforge/eventbus/api/`）
  （修正说明：审阅替换原文记 19 条；经核基线 15 条本身含 5 个目录条目，空壳类型引入 4 个新目录条目，故实际为 23 条）；
- 记录新 jar SHA-256 与字节数；
- 归档探针 jar：`build\forgecompat-002-0b-2a.jar` —— **Run C / Run T 共用同一个 jar**；
  构建产物中不得烧入开关值（开关只来自运行时系统属性，见 §2.2）。

### 2A-d：重写后字节码验证（机制：M1；已裁定，见 附录 A-⑤；仅 Run T 产生）

**M1**：

1. **运行中**：Transformer 在完成重写后，以 `ClassWriter(0)` 序列化**同一 ClassNode**，dump 至
   `D:\DualForge\spike000\logs\spike002-0b-2a\ForgeProbe.rewritten.class`（try/catch，非致命，成败均记日志）；
2. **运行后**：对 dump 执行 `javap -p -c -constants`（如需再 `-v`），与原始 `forgeprobe.jar` 的
   `javap -p -c` **逐指令比对**（除方法签名行外必须完全一致）；
3. **强制字段**：构造函数数量 = 1；旧签名 = 不存在；新签名 = 存在；未生成其它 `<init>` = YES；
   构造函数指令主体未主动改写 = YES；
4. 报告中注明：dump 为"本 Transformer 以 ClassWriter(0) 序列化同一 ClassNode"所得
   （与 ModLauncher 实际写出的字节同源同构、**非逐字节副本**）。

> 说明：Run C（rewrite 关闭）不产生重写，因此**无 dump、无 post-check**（offered 日志仍记录）；
> M2 方案未采纳。dump 路径按 Run T 的 label（`spike002-0b-2a`）。

### 2A-e：服务端运行与层判定

运行命令（K1：先 Run C 后 Run T；同一 jar，仅 JVM 参数不同）：

```
# Run C（control；先执行）
D:\DualForge\spike000\run-server.ps1 -Label spike002-0b-2a-control `
  -ExtraJvmArgs '-Dforgecompat.2a.rewrite=false' `
  -ModsJar D:\DualForge\spike000\probe\build\forgeprobe.jar `
  -ExtraModsJars D:\DualForge\spike001a\compat\build\forgecompat.jar

# Run T（treatment；后执行）
D:\DualForge\spike000\run-server.ps1 -Label spike002-0b-2a `
  -ExtraJvmArgs '-Dforgecompat.2a.rewrite=true' `
  -ModsJar D:\DualForge\spike000\probe\build\forgeprobe.jar `
  -ExtraModsJars D:\DualForge\spike001a\compat\build\forgecompat.jar
```

判定顺序（强制）：

1. **服务发现证据**：ModLauncher 启动列表新增 `forgecompat-transform TRANSFORMATIONSERVICE` 行；
   以及 `Found additional transformation services from discovery services: [ …\mods\forgecompat.jar ]` 行；
2. **转换链证据**：`offered` / `REWRITE` / `post-check` 日志 —— 必须先于层判定，对 Run C / Run T
   各完成一次三向诊断：服务未发现 / 未 offered / offered 未重写（三种情况各自对应不同结论）；
3. **层 d**：不存在 002-0a 同款 `unsupported argument` 异常（对 ForgeProbe）→ 层 d 解除候选；
   但仍需第 4 步证据确认；
4. **层 e**：证据 1 或证据 2 出现 → **STOP（观察范围）**；
5. 若 FAIL：记录异常类型 / 异常消息 / 完整 stack trace（原文）+ 三向诊断结论；
   不得修改探针以外任何东西；实现缺陷修正后重跑仅限范围内一次为限，仍失败 → FAIL 交回。

**K1（已裁定；顺序：先 control 后 treatment）**：

- **Run C（control，label `spike002-0b-2a-control`）**：
  `-Dforgecompat.2a.rewrite=false`；
  期望：offered ≥ 1 且含 ForgeProbe / `rewrite target count = 0` /
  **层 c PASS**（IEventBus 空壳类型存在，见 §0.3）/
  **层 d FAIL（与 002-0a 同款异常）**；
  判定等级：**ANCHOR-002-0b-2A**（仅提供对照，不判成败）；
- **Run T（treatment，label `spike002-0b-2a`）**：`-Dforgecompat.2a.rewrite=true`；
  期望：offered ≥ 1 / `rewrite target count = 1` / 层 d 解除候选 → 层 e 证据 → STOP；
- 两次运行使用**同一个 jar**；仅 JVM 参数不同（K1 单变量收敛）；
- Run C 的偏离形态一律如实记录；若偏离表明探针实现未按设计生效（服务未发现 / offered 缺失 / rewrite ≠ 0）
  → 视为实现缺陷，按 §2.6-5 修正后重跑；仍偏离 → FAIL 交回。

### 2A-f：清理与归档

按 §八 清单执行；核心标准：兼容层恢复基线（逐条目比对 diff = 0）、forgeprobe.jar 未修改、
探针 jar 与全部日志 / dump 归档保留。

---

## 五、判据

| 结果 | 定义 |
|---|---|
| **ANCHOR-002-0b-2A**（Run C） | 对照结果：offered ≥ 1 且含 ForgeProbe + `rewrite target count = 0` + 层 d FAIL（与 002-0a 同款异常）；**仅提供对照，不判成败**。 |
| **PASS-002-0b-2A**（Run T） | 运行中：服务被发现；`offered` ≥ 1 且含 ForgeProbe；`rewrite target count = 1`；`non-target transformed class count = 0`；重写后字节码验证 5 项全过（M1 口径）；层 d 解除（无同款异常）；层 e 证据出现（marker 或栈帧）。随即 **STOP**。 |
| **VALID-002-0b-2A**（Run T） | 探针正确执行，但层 d 未解除 / 出现新失败形态 / 转换链未达 transform（offered 缺失等）/ 层 d 解除但层 e 未进入 —— 如实记录失败形态，交回。 |
| **FAIL-002-0b-2A**（Run T） | 实现缺陷：审计不过（含 Run T `rewrite > 1`）、字节码验证不过、构建 / 预检失败且范围内无法修正；到达层 d 之前服务端失败且无法在范围内修正；或需要突破 §2.7 红线才能继续。 |

> **PASS / VALID / FAIL 判据只适用于 Run T**；Run C 仅为 ANCHOR（对照锚点）。

---

## 六、STOP 条件

1. 层 e 证据（1 或 2）出现 → 立即 STOP（观察范围）；
2. 2A-b 取证与 §2.6 预勘不一致且影响实现 → STOP，交回；
3. 服务名冲突 / 无法证明未干扰 FML → STOP，交回；
4. ASM 链接失败（`NoClassDefFoundError: org/objectweb/asm/…`）→ 记录 + STOP 交回
   （不得打包 ASM / 改 ClassLoader / 改 ModuleLayer）；
5. 需要突破 §2.7 红线 → STOP，交回；
6. 运行前预检失败（jar 条目数 / 内容不符）→ 修正构建一次；仍失败 → FAIL 交回。
7. Run C（对照）出现实现性偏离（服务未发现 / offered 缺失 / rewrite ≠ 0）且修正后仍偏离 → FAIL 交回。

---

## 七、结构化输出格式（强制）

（报告中字段不得省略；未知填 `UNKNOWN` 并说明；异常 / 日志一律原文引用，不得摘要）

```
# Spike-002-0b Step 2-A 实验报告

## 一、分析对象与环境
- forgeprobe.jar SHA-256（运行前 / 运行后）:
- 探针 jar（forgecompat）SHA-256 / 字节数 / 条目数（期望 18）:
- Run C（control）label / meta.json status / exitCode / duration:
- Run T（treatment）label / meta.json status / exitCode / duration:
- user_jvm_args.txt 内容（Run C / Run T，逐次原文）:
- run-server.ps1 diff 原文（ValidateSet ×2 + -ExtraJvmArgs）:
- ASM 制品清单（2A-a，含路径 + SHA-256）:
- 冻结版本核对（Minecraft / NeoForge / FML / ModLauncher / Java）:

## 二、实现探针清单（2A-c）
- 新增文件（3）逐条列出（路径 + 用途）:
- build.gradle diff（原文）:
- 编译命令原文 / 退出码 / 警告:
- 与 §2.2"唯一变量"的逐条对照（证明只做了一条 desc 重写）:

## 三、服务共存证据（§2.6 /"未干扰 FML"）
- ITransformationService 8 方法逐一处置表（javap 原文 + 实现 / 不覆写 + 实现体内容）:
- 4 个 default 方法的默认行为原文（javap -c）与无害性结论:
- service 声明文件内容（逐字节）:
- ModLauncher services 启动列表原文（含新增行 + fml / mixin 行）:
- "Found additional transformation services" 行原文:
- service 日志行原文（initialize / onLoad / transformers）:

## 四、转换链运行时证据（§2.4）
- Run C（rewrite=false）:
  - offered 清单（逐条原文）:
  - rewrite target count:（期望 0）
- Run T（rewrite=true）:
  - offered 清单（逐条原文）:
  - REWRITE 行原文:
  - post-check 行原文:
  - rewrite target count:（硬要求 1）
- non-target transformed class count:（期望 0）
- 同一 internalName 重复 offered: YES / NO（如 YES：ANOMALY 明细）
- 三向诊断（Run C / Run T 各一次）:
  - 服务是否被发现:      YES / NO
  - ForgeProbe 是否被 offered: YES / NO
  - 是否发生重写:        YES / NO（Run C 期望 NO；Run T 期望 YES）

## 五、重写后字节码验证（2A-d；仅 Run T）
- 机制: M1（dump + javap）；Run C 无此项
- dump 路径 / 字节数 / 说明（同源同构、非逐字节副本）:
- javap -p -c -constants 原文（重写后）:
- 与原始 javap 逐指令比对结论（除签名行外一致）: YES / NO
- 构造函数数量 = 1:                      YES / NO
- 原签名 <init>(Lnet/minecraftforge/eventbus/api/IEventBus;)V: 不存在 / 存在
- 新签名 <init>(Lnet/neoforged/bus/api/IEventBus;)V:           存在 / 不存在
- 未生成其它 <init>:                     YES / NO
- 构造函数指令主体未主动改写:            YES / NO

## 六、服务端运行与层判定（2A-e）
### Run C（control；ANCHOR 对照）
- 层 d 结果（期望 FAIL，同款异常）:
- 异常消息原文（如 FAIL）:
- 结论等级: ANCHOR-002-0b-2A
### Run T（treatment）
- 层 d 结果:  PASS / FAIL
- 层 e 进入证据:
  - marker 命中:      YES / NO
  - stack trace 命中: YES / NO
  - 命中原文（行 / 栈帧）:
  - 若任一为 YES: **STOP（停在此处）**
- 若层 d FAIL 或出现新失败形态:
  - 异常类型:
  - 异常消息:
  - 完整 stack trace（原文）:
- 原始日志文件清单（两 run 的 stdout / latest / debug / crash-report）:

## 七、与说明书的偏差（如有）

## 八、清理确认（对照 §八 逐项 + 逐条目比对结果）

## 九、事实标签
[F] Source Fact:
[E] Experimental Fact:（本 Spike 的预期产出主体）
[H] Hypothesis:
[D] Design Decision:

## 十、结论
- Run C 对照结论（ANCHOR-002-0b-2A）:
- Run T 结果:  PASS / VALID / FAIL
- 层 d 是否解除:  YES / NO
- 层 e 是否进入:  YES / NO
- 结论上限（禁止超出）:
  [E] 本探针（一条描述符重写）下，层 d / 层 e 的实际结果；
  [E] transformer 在本次运行中被实际驱动（offered / rewrite 日志）；
  [E] 未干扰 FML 的结构证据。
- 禁止从本 Spike 推导:
  "参数注入 ABI 已兼容" / "最终 IEventBus Shim 应采用 XXX" /
  "Forge Mod 已能被 NeoForge 加载" / "全局 Forge→NeoForge 重写策略可行" /
  "其它 Forge 命名空间类型已兼容"。
- Confidence: HIGH / MEDIUM / LOW
- Evidence:
- Hypotheses still unverified:

## 十一、边界确认
（对照 §九 逐项打勾）
```

---

## 八、清理确认（执行清单）

- [ ] `ForgeCompatTransformationService.java` 已删除
- [ ] `ForgeProbeConstructorTransformer.java` 已删除
- [ ] `META-INF/services/cpw.mods.modlauncher.api.ITransformationService` 已删除
- [ ] `src\main\java\net\minecraftforge\eventbus\api\IEventBus.java` 已删除
- [ ] `net\minecraftforge\eventbus\api\IEventBus.class` 已从 jar 中移除（jar tf 验证）
- [ ] `build.gradle` 的 ASM `compileOnly` 追加已移除（回退为 4 条基线依赖）
- [ ] `build\cp.txt` 未被修改（仍为基线内容）
- [ ] `build\classes` / `build\jar-root` 已清空重建（仅含基线 5 源产物）
- [ ] 重建后 `forgecompat.jar` 逐条目比对 vs `forgecompat-001b1.jar`：**entries = 15，diff = 0**
      （整包 SHA-256 允许因重打包而不同，记录实际值即可）
- [ ] `forgeprobe.jar` SHA-256 前后一致 = `a93a823c…74e22`（未修改）
- [ ] `IModFileReader` / `IModFileCandidateLocator` service 与 MANIFEST 与基线一致
- [ ] `mods\` 目录状态已记录（与 002-0a 结束时状态一致或按既定惯例）
- [ ] 探针 jar 归档保留：`build\forgecompat-002-0b-2a.jar`（**Run C / Run T 共用这一份**）
- [ ] 日志与 dump 归档保留：`logs\spike002-0b-2a-control\` 与 `logs\spike002-0b-2a\`（两目录）
- [ ] `run-server.ps1` 改动 = ValidateSet 追加 2 个 Label + 新增 `-ExtraJvmArgs`（默认空、不传时与基线一致）；
      其余逻辑未动；改动 diff 已记录

---

## 九、边界确认 / 禁止事项确认

- [ ] 未修改 NeoForge / FML 核心（含 `FMLModContainer` / `TransformStore` / `ClassTransformer`）
- [ ] 未修改 `forgeprobe.jar` / `mods.toml` / ForgeProbe 源码
- [ ] 未改动 ClassLoader 策略 / 未新增 ClassLoader；未修改 ModuleLayer 结构
- [ ] 未注入 IEventBus 实例；未实现 Adapter / Proxy；未使用 Unsafe
- [ ] 未执行 B 分支（extends 探针）
- [ ] K1 执行顺序 = 先 Run C（`spike002-0b-2a-control`）后 Run T（`spike002-0b-2a`）
- [ ] 两次运行（Run C / Run T）都包含空壳 IEventBus 类型（§0.3 前置，非变量）
- [ ] `-Dforgecompat.2a.rewrite` 未传时抛异常，而非静默降级为 false
- [ ] ASM 使用严格限定：仅一条 desc 重写；标志 = 0；`compileOnly` 且不打包进 JAR
- [ ] 未全局映射；未重写方法调用 / 字段描述符 / 方法体
- [ ] 层 e 证据出现后已立即 STOP（未继续观察 / 未分析后续行为）
- [ ] 未进入 002-0c；未提出修复方案；未设计最终 Shim
- [ ] 报告含"与说明书的偏差"一节（如有偏差必须如实记录）

---

## 十、证据边界与结论上限

**能证明**：

- `[E]` 本次运行中 transformer 被实际驱动（offered / rewrite 日志）；
- `[E]` 层 d 是否解除；层 e 是否进入（**仅对本探针、仅对本 ForgeProbe 构造函数**）；
- `[E]` 未干扰 FML 的结构证据（启动列表 / 方法处置 / FML 里程碑）。

**不能证明 / 禁止推导**：

- "参数注入 ABI 已兼容"（那需要全部 Forge 模组的构造函数参数类型均兼容）；
- "最终 IEventBus Shim 应采用 XXX"（`[D]`，需后续证据）；
- "Forge Mod 已能被 NeoForge 加载"（需要完整生命周期闭环）；
- "全局 Forge→NeoForge 重写策略可行" / "ASM 重写方案已确定"；
- "其它 Forge 命名空间类型（Registry / Event / Config）已兼容"。

---

## 十一、交接规则

- **ANCHOR-002-0b-2A**（Run C）→ 随 Run T 报告一并交回（对照证据）；
- **PASS-002-0b-2A**（Run T）→ 交回；由架构决策方裁定 002-0c 的范围；
- **VALID-002-0b-2A**（Run T）→ 记录失败形态，交回；
- **FAIL-002-0b-2A**（Run T）→ 实现问题，交回；
- 层 e 证据出现时，报告停在那里（marker 之后不分析、不外推）。

---

## 附录 A：裁定结果（5 点）与冻结前确认清单（6 项）

### A.1 五个裁定点（审阅结论：全部采纳，含 2 处补充）

- **① guard 策略 → 裁定：不加 guard**（唯一选择逻辑 = `targets()` 注册 + 描述符精确匹配；`offered` 无条件记录）。
- **② default 覆写 → 裁定：不覆写任何 default**；**附加取证（审阅补充）**：2A-b 以 `javap -c` 确认 4 个
  default 的默认行为无害；若发现非平凡默认行为 → 停止交回。（已并入 §2.6-3 / 2A-b）
- **③ 计数口径 → 裁定：采纳并收紧**：`offered > 1` 逐条记录 `internalName`；同一 `internalName` 重复 → ANOMALY；
  `rewrite = 1` 为硬要求；`rewrite > 1`（无论 desc 是否相同）→ 直接 FAIL。（已并入 §2.4 / §五）
- **④ 对照运行 → 裁定：K1**（先 control 后 treatment；单一 jar + 命令行系统属性开关）。（已并入 §2.2 / §2A-e / §三）
- **⑤ dump 机制 → 裁定：M1**（运行中 dump + 运行后 javap；仅 Run T 产生）。（已并入 §2A-d）

### A.2 冻结前确认清单（8 项；对应《审阅结论》§三）

| # | 项目 | 落位位置 |
|---|---|---|
| 1 | 5 个待裁定点全部采纳（② 含 default 行为取证） | 附录 A.1；§2.6-3；2A-b |
| 2 | Run C 实现方式：命令行系统属性 + 同一 jar | §2.2；2A-e；§三 |
| 3 | 两个 label（control / treatment）明确 | §三；2A-e；§八 |
| 4 | §五 判据表新增 ANCHOR 行 | §五 |
| 5 | K1 顺序：先 control 后 treatment | 2A-e（已明确） |
| 6 | 层 e STOP 后"marker 之后日志仅作附件"纪律保持 | §2.5（保持） |
| 7 | 【新增】两次运行共同恢复空壳 IEventBus（修正漏洞） | §0.3；2A-c；2A-e；§八；§九 |
| 8 | 【新增】rewriteEnabled 未传系统属性 → 抛异常而非缺省 false | §2.2；§九 |

> 以上 8 项已在 **v1 冻结版** 中逐项落位；执行期间不得再变更。
