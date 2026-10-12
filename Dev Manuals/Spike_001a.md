# Spike-000 正式实验结论与 Spike-001a 执行定义

**文档性质**：Spike-000 结论冻结 + Spike-001a 执行定义（四步拆分） + 可交付 Codex 的任务说明书
**前置文档**：

1. `Forge → NeoForge 兼容层开发手册 v1.1（架构冻结版·最终）`
2. `Spike-000 实验报告`（`Dev Manuals/Spike-000 实验报告.md`）

---

## 一、Spike-000 结论（正式冻结）

### 1.1 已被实验直接验证的事实 [E]

| 事实 | 证据 |
|---|---|
| `META-INF/mods.toml` 会触发 `IncompatibleModReason.MINECRAFT_FORGE` | 日志 + 字节码 `filePresent("META-INF/mods.toml")` |
| Forge JAR 在所有 `IModFileReader` 均未接受后被拦截 | `debug.log` 显示 `found 0 mods, 1 warnings` |
| 第一条红线位于 Discovery → `IModFile` 创建边界 | `Skipping jar.` 日志 + 阴性对照 |
| 该失败表现为 WARNING + `ModLoadingIssue` + 跳过，而非异常 | 无堆栈、无 crash-report、退出码 0 |
| `forgeprobe.jar` 未进入 Metadata / Validation / Scan / LanguageLoader / Module / Container / Constructor | `Considering mod file candidate` 行不存在；构造函数输出零命中 |

### 1.2 由 21.1.256 精确运行时源码/字节码验证、但未由动态执行验证的事实 [F]

| 事实 |
|---|
| `IModFileReader` 通过 ServiceLoader 加载 |
| 当前 NeoForge 内置 reader 使用 `SecureJar` / `IModFile` API 构造 `IModFile` |
| `ModFileScanData` / `BackgroundScanHandler` 的后续扫描路径存在 |

> **重要区分**：Spike-000 未进入 Scan 阶段，因此 `BackgroundScanHandler` 的扫描路径未由本次实验动态验证。
> `[F]` 只表示“源码事实”，不表示“本次实验中已运行”。

### 1.3 尚未验证的假设 [H]

| 假设 |
|---|
| `ForgeModReader` 能否成功被 ServiceLoader 加载 |
| `ForgeModReader.read()` 能否成功返回非 null `IModFile` |
| `ForgeModFileInfoParser` 能否完整解析 Forge `mods.toml` |
| Forge `IModFileInfo` 是否能进入 `ModValidator` |
| ForgeProbe 是否能够继续进入 LanguageLoader / Module / Container |
| Forge `@Mod` 是否能够被 `FMLModContainer` 正确消费 |

### 1.4 架构决策 [D]

1. `[D]` 兼容层必须在 `IModFileReader` → `IModFile` 这一 Discovery 边界产生干预，才能阻止 `MINECRAFT_FORGE` 拦截。
2. `[D]` Spike-001a 优先沿 官方 SPI/API 路径实现（`SecureJar` → `IModFile.create()`）。但尚未证明这是第三方 reader 唯一合法的构造方式。
3. `[D]` 当前唯一可写入架构决策记录的结论：NeoForge 21.1.256 对 Forge 原生 JAR 的第一道实际阻断点已确定在 Discovery 的 `IModFileReader` → `IModFile` 边界；下一实验只负责把这一道门打开，不负责继续向后修。

### 1.5 对 v1.1 手册的修正

| 原表述 | 修正后 |
|---|---|
| “ForgeModReader 作为 Discovery 接入点 ✅ 已验证” | `[E]` Reader boundary = 已验证；自定义 ForgeModReader = 尚未验证 |
| “`SecureJar.from()` → `IModFile.create()` 路径 ✅ 已验证为必要” | `[F]` NeoForge 内置 reader 使用该路径；`[D]` Spike-001a 优先沿该路径实现；尚未证明这是唯一合法方式 |
| “`ModFileScanData` 由 `BackgroundScanHandler` 触发 ✅ 已验证” | `[F]` 源码事实；`[E]` Spike-000 未进入 Scan，该路径本次未验证 |
| FML 版本取证方式 | 改为从 `--fml.fmlVersion` 参数或 `fancymodloader/loader/<ver>/` 目录读取。NeoForge 21.1.256 对应 **FML 4.0.45**，不是 11.0.xx |
| §14.2 预判 | 降级为 `[E]` 已被证伪的预判，记录实测失败点比预期更早 |

---

## 二、Spike-001a 执行定义（四步拆分）

### 2.1 子 Spike 表

| 子 Spike | 唯一问题 | PASS | STOP |
|---|---|---|---|
| **001a-0** | Provider 是否进入 ServiceLoader | 能明确证明 `ForgeModReader` 被发现/实例化 | 没加载就停 |
| **001a-1** | Reader 是否真正接收 Forge JAR | `read()` 被调用，并返回非 null `IModFile` | Reader 内失败就停 |
| **001a-2** | Discovery 是否真正接受 JAR | `Considering mod file candidate ... forgeprobe.jar`，且无 `minecraft_forge` | Discovery 再次拒绝就停 |
| **001a-3** | 是否进入 Validation | ForgeProbe 出现在 validator 的输入/相关证据中 | Validation 首次失败就停 |

### 2.2 最小实现范围

| 组件 | 实现 | 不实现 |
|---|---|---|
| `ForgeModLocator` | 只认 `mods/forgeprobe.jar` | 通用扫描 |
| `ForgeModReader` | 识别 `META-INF/mods.toml`，调用 `IModFile.create(SecureJar, parser)` | 自定义 `IModFile` 实现 |
| `ForgeModFileInfoParser` | 解析 `mods.toml`，返回 `IModFileInfo` | features 声明 |
| `ForgeLanguageLoader` | 不实现 | — |

### 2.3 禁止引入

- ASM、Mapping、Mixin、Registry、Event、Isolation
- 任何修改 `forgeprobe.jar` 的行为
- 任何修改 NeoForge 核心的行为
- 任何试图修改 `FMLJavaModLoadingContext` ABI 的行为

### 2.4 当前状态定义

```
Spike-000
    ✅ Discovery 红线已确认
    ✅ MINECRAFT_FORGE 触发机制已确认
    ✅ "不是异常，而是跳过"已确认
    ✅ 后续阶段未进入已确认

Spike-001a
    ⏭ ServiceLoader
    ⏭ IModFileReader
    ⏭ IModFile
    ⏭ Discovery acceptance
    ⏭ Validation entry

暂不动：
    ⏸ LanguageLoader
    ⏸ Module
    ⏸ FMLModContainer
    ⏸ ABI
```

> 现在还没有证据表明 LanguageLoader / Module / FMLModContainer / ABI 有问题。
> Spike-000 甚至还没让 ForgeProbe 走到那里。**Spike-001a 的唯一职责是把 Discovery 这道门打开，不负责继续向后修。**

---

## 三、Spike-001a 执行任务说明书（可直接交付 Codex）

```
# Spike-001a 执行任务说明书

接收方：Codex
前置文档：
  1. Forge → NeoForge 兼容层开发手册 v1.1（架构冻结版·最终）
  2. Spike-000 正式实验结论（本文档第一节）
任务性质：实验驱动验证，非修复任务

## 〇、执行前置
请先完整读取 v1.1 手册第三章（SPI 接入）、第四章（组件设计）、
第十一章（Spike 路线）、附录 A（事实标签体系）。
本说明书不重复该手册内容。

## 一、任务目标（唯一）
证明 ForgeModReader 能够接管 META-INF/mods.toml 格式的 JAR，
使其通过 Discovery 阶段的 reader 检查。

禁止目标：
- 让 ForgeProbe 构造函数被调用
- 进入 LanguageLoader / Module / Container
- 修改 FMLJavaModLoadingContext ABI
- 写任何 ASM / Mapping / Mixin / Registry / Event 代码

## 二、实验环境（冻结）
Minecraft:            1.21.1
NeoForge:             21.1.256
FancyModLoader:       4.0.45（从 --fml.fmlVersion 读取）
Java:                 21
OS:                   <系统信息>
启动方式:             CLI（官方 installer 产物）
客户端/服务端:        Server（Dedicated）

编译依赖 Forge 版本:  1.21.1-52.1.0

## 三、四步拆分

### 001a-0：Provider 是否进入 ServiceLoader
唯一问题：ForgeModReader 是否被 ServiceLoader 发现/实例化？
PASS：日志或断点明确证明 ForgeModReader 被实例化
STOP：未加载则停止，检查 META-INF/services 注册

### 001a-1：Reader 是否真正接收 Forge JAR
唯一问题：ForgeModReader.read() 是否被调用并返回非 null IModFile？
PASS：read() 被调用 + 返回非 null IModFile
STOP：Reader 内失败则停止，定位失败点

### 001a-2：Discovery 是否真正接受 JAR
唯一问题：forgeprobe.jar 是否通过 Discovery 边界？
PASS：出现 "Considering mod file candidate ... forgeprobe.jar"，且无 minecraft_forge
STOP：Discovery 再次拒绝则停止

### 001a-3：是否进入 Validation
唯一问题：forgeprobe.jar 是否进入 ModValidator？
PASS：ForgeProbe 出现在 validator 的输入/相关证据中
STOP：Validation 首次失败则停止

## 四、最小实现范围
ForgeModLocator：只认 mods/forgeprobe.jar
ForgeModReader：识别 META-INF/mods.toml，调用 IModFile.create(SecureJar, parser)
ForgeModFileInfoParser：解析 mods.toml，返回 IModFileInfo
ForgeLanguageLoader：不实现

## 五、禁止事项
- 不修改 forgeprobe.jar
- 不添加 neoforge.mods.toml
- 不修改 NeoForge 核心
- 不引入 ASM / Mapping / Mixin / Registry / Event / Isolation
- 不修改 FMLJavaModLoadingContext ABI
- 不提出修复方案

## 六、输出格式
按 v1.1 手册附录 A 的 [F]/[E]/[H]/[D] 标签体系输出。
每一步（001a-0 到 001a-3）单独出报告。
报告结构同 Spike-000，但 Failure Chain 只填写到当前子 Spike 已进入的阶段。

## 七、交接
完成 001a-0 后立即交回，由架构决策方判断是否进入 001a-1。
不得跳过子 Spike 直接执行下一步。

## 八、当前唯一可写入架构决策记录的结论
NeoForge 21.1.256 对 Forge 原生 JAR 的第一道实际阻断点
已确定在 Discovery 的 IModFileReader → IModFile 边界；
下一实验只负责把这一道门打开，不负责继续向后修。
```

---

## 四、Spike-000 正式冻结

```
Spike-000
    ✅ Discovery 红线已确认
    ✅ MINECRAFT_FORGE 触发机制已确认
    ✅ "不是异常，而是跳过"已确认
    ✅ 后续阶段未进入已确认

Spike-001a
    ⏭ 001a-0: ServiceLoader
    ⏭ 001a-1: IModFileReader
    ⏭ 001a-2: IModFile / Discovery acceptance
    ⏭ 001a-3: Validation entry

暂不动：
    ⏸ LanguageLoader
    ⏸ Module
    ⏸ FMLModContainer
    ⏸ ABI
```

Spike-000 证据链已冻结。进入 Spike-001a-0。
