# Spike-001b-5 执行任务说明书

**接收方**：Codex
**前置文档**：《Forge → NeoForge 兼容层开发手册 v1.1》+《Spike-001b-0 源码分析报告》+
《Spike-001b-1 实验报告》+《Spike-001b-3 实验报告》
**任务性质**：源码契约取证 + 编译可行性 + 最小运行时观测
**唯一目标**：验证 `ModSorter` 对 `IModInfo` 的隐式类型契约，并确定兼容层能否在
**不修改 NeoForge 核心**的前提下提供一个可被 `ModSorter` 接受的
`net.neoforged.fml.loading.moddiscovery.ModInfo` 实例。

```
Spike-000      ✅ 完成
Spike-001a     ✅ 完成
Spike-001b-0   ✅ 完成
Spike-001b-1   ✅ 完成
Spike-001b-3   ✅ 完成
Spike-001b-4   ⏭ 已被 001b-1 覆盖
Spike-001b-5   ▶ 本说明书
Spike-001b-2   ⏸ 待 001b-5 后决定
Spike-001c     ⏸ 未开始
```

**001b-3 已证明**：

```
LanguageProvider → findLanguage("javafml", ...) → 成功
        → ModSorter.sort(...) → (ModInfo) imodInfo → ClassCastException
```

> 本 Spike **不再**重新验证 `javafml` 同名覆盖，也**不再**重新验证 `[52,)` vs `4.0.45`。

---

## 〇、执行前置

先完整读取 v1.1 手册：§3.1、§3.6、§3.8、§4.3、§11、§13、附录 A；
并完整读取 001b-0 / 001b-1 / 001b-3 三份报告。

---

## 一、唯一目标（两个问题）

**问题 A：`ModSorter` 的实际类型契约是什么？**

001b-3 已从精确版本字节码确认 `ModSorter.sort` 内部 `ModInfo modInfo = (ModInfo) imodInfo;`。
需进一步确定：

1. `ModInfo` 是否 `final`
2. `ModInfo` 的全部构造函数
3. 每个构造函数的可见性
4. 构造函数参数类型（**特别是 owning file 参数的最具体类型**）
5. 是否要求 `ModFileInfo` / `ModFile` / scan data / config 等特定内部对象
6. `ModInfo` 是否存在继承扩展路径
7. `ModSorter` 在该强转之后对 `ModInfo` 的直接成员访问（方法 / 字段）
8. NeoForge 自身在何处创建 `ModInfo`（`ModFileParser` / `ModValidator` / `ModFile` 内部）

**问题 B：兼容层能否在"不修改 NeoForge 核心"的条件下产生一个可接受的 `ModInfo`？**

| 类别 | 说明 |
|---|---|
| **A. 正常 Java 路径** | public / protected / 可合法继承或调用的 API |
| **B. 非常规路径** | 仅通过反射、MethodHandles、Unsafe、package 假装、字节码修改等 |
| **C. 完全无法做到** | 无任何路径 |

> **只有 A 才能作为正常兼容层架构的可行路径。**
> B 类方法只能作为实验事实记录，**不能转化为架构决策**。

---

## 二、实验边界

### 2.1 只验证 ModSorter 契约

本 Spike **不解决**：Forge constructor ABI、Forge `IEventBus`、
Registry / Event / Mapping / Mixin / Isolation、Game Layer identity、Forge `@Mod` entrypoint 解释。

### 2.2 允许变更

- 修改 `ForgeModFileInfo.java`
- 为本 Spike 增加最小测试辅助类
- 临时恢复一个 001b-3 同型的观测用 `IModLanguageLoader`（`name()="javafml"`，`version()="9999"`）
- 临时增加日志
- 使用精确版本 NeoForge / FML API 做源码、`javap`、编译验证
- 在运行时通过兼容层代码读取公开 API
- 使用反射 / MethodHandles 进行可行性探测，但**必须单独标记为"非常规路径"**

### 2.3 禁止事项

- 修改 NeoForge 核心源码
- 修改 `ModSorter` / `ModInfo` / `ModFile` / `LanguageProviderLoader` / `VersionSupportMatrix` / `FMLModContainer`
- 修改 `forgeprobe.jar`、修改 `mods.toml`
- 引入 ASM / Mapping / Mixin / Registry / Event / Isolation
- 改变 NeoForge / FML / Minecraft / Java 版本
- 为了通过实验而修改 NeoForge 内部 class 文件
- 使用 Unsafe、字节码注入、ClassLoader hack 等手段**制造假成功并视为正常兼容路径**

---

## 三、精确环境（冻结）

```
Minecraft:            1.21.1
NeoForge:             21.1.256
FancyModLoader:       4.0.45
Java:                 21（OpenJDK 21.0.10，Microsoft Build）
OS:                   Windows 11 amd64
启动方式:             CLI / Dedicated Server（官方 installer --installServer 产物）
编译依赖 Forge:       1.21.1-52.1.0
```

**环境冻结，不得修改。**

---

## 四、子步骤

### 001b-5-0：源码契约取证（不做运行）

**唯一目标**：从精确 NeoForge 21.1.256 / FML 4.0.45 工件（`loader-4.0.45.jar` 及
`neoforge-21.1.256-universal.jar`）的字节码确定 `ModInfo` 的真实结构。

必须记录：

```
class:               net.neoforged.fml.loading.moddiscovery.ModInfo
package:             net.neoforged.fml.loading.moddiscovery
final / non-final:   yes / no
implemented interfaces:  <完整列表>
constructors:        <完整列表>
constructor visibility:  public / protected / package-private / private（逐个记录）
constructor parameters:  <完整签名，含每个参数的精确类型>
factory methods:     <若存在，完整记录>
```

必须显式检查并记录：

1. **`ModInfo` 构造函数的 owning file 参数类型**：是 `IModFileInfo`？是 `ModFileInfo`（具体类）？
   是 `ModFile`（具体类）？还是其他？——这是最关键的一条。
2. **`ModSorter.sort` 后续对 `ModInfo` 的直接成员访问**：从强转位置开始逐条记录
   方法名 + 参数类型 + 返回类型。这决定即使构造成功，后续是否还能继续通过。
3. **NeoForge 自身在何处创建 `ModInfo`**：`ModFileParser.readModList()` 内部？
   `ModValidator` 内部？`ModFile` 内部？`ModInfo` 自身的 static factory？
   完整记录创建点的可见性与第三方可调用性。

输出：一份足以回答"第三方代码是否能够正常构造 `ModInfo`"的精确源码证据。

**PASS-001b-5-0 判据**：上述三项均已从精确版本字节码获取，**无 UNKNOWN**。

**STOP 条件**：无法取得精确 `ModInfo` 构造信息；或只能用 current/main 替代。

---

### 001b-5-1：正常 Java 构造可行性（编译级）

**唯一目标**：验证 forgecompat 编译单元是否能够正常 Java 调用或继承 `ModInfo`。

**A1：直接构造** —— 尝试最小 `new ModInfo(...)`。必须记录：

```
compile:              PASS / FAIL / UNKNOWN
失败原因:             <完整 javac 错误>
需要的不可访问类型:   <若有>
需要的包访问:         <若有>
```

**A2：继承** —— 若 `ModInfo` 非 final，检查 `class X extends ModInfo { ... }` 是否能够正常编译。

> **关键约束**：继承是否可行**不取决于 `ModInfo` 是否 final**，而取决于**父类构造函数能否被第三方调用**。
> 即使 `ModInfo` 非 final，若其唯一构造函数是 package-private 且接收 `ModFile`，则继承同样不可行。

必须记录：

```
继承:                 允许 / 不允许 / UNKNOWN
构造器约束:           <完整记录>
父类构造函数可调用性: YES / NO / UNKNOWN
```

**A3：官方工厂 / 构造路径** —— 若源码存在 `factory(...)` / `create(...)` / `parse(...)` / `from(...)`
等官方构造入口，必须记录：完整签名、可见性、是否接受第三方可构造的参数类型、
是否能被兼容层的非 NeoForge 包路径调用。

> **禁止自己发明"等价工厂"。**

**PASS-001b-5-1 判据**：A1 / A2 / A3 三层的编译结果均已记录，**无 UNKNOWN**。

**正常路径结论**：`YES` / `NO` / `UNKNOWN`。

> 若 A1 / A2 / A3 中任一层给出"部分可行"（例如需要反射才能访问某个参数），
> 必须按 §六 记录为**非常规路径**，**不得**直接运行 001b-5-2。

---

### 001b-5-2：运行时最小验证

**前置条件**：001b-5-1 找到正常 Java 可行的 `ModInfo` 创建路径（**A 类**）。
**若只找到 B 类路径，跳过本步骤，直接进入 §六。**

**目标**：证明

```
ForgeModFileInfo.getMods()
        ↓ 返回真正的 net.neoforged.fml.loading.moddiscovery.ModInfo
LanguageLoader 通过
        ↓
ModSorter.sort()
        ↓ 不再因为 IModInfo → ModInfo 强转而失败
```

**实验方法**：

- 临时恢复 001b-3 的观测 provider：`name() = "javafml"`，`version() = "9999"`
  - **唯一作用**：让 `[52,)` 校验通过
  - **不要加入任何 Forge lifecycle 代码**
- `ForgeModFileInfo.getMods()` 返回 1 个**真正的** NeoForge `ModInfo` 实例
  - 内容只提供：`forgeprobe` / `1.0.0` / `Forge Probe`
  - 其余字段保持最小化

**必须观察**：

1. `ModSorter.sort` 是否进入
2. **不再出现** `ClassCastException: Cannot cast ...ForgeModInfo to ...ModInfo`
3. 记录 `ModSorter` 后续的**第一个新失败点**（若有），包括：阶段、异常类型 + 完整 stack trace、调用链

本 Spike 的合法输出包括：

```
ModSorter → PASS → 下一阶段 → 新的异常（记录但不修复）
```

> **不得在 001b-5-2 中"顺手修复"新失败点。**

---

### 001b-5-3：非常规路径单独记录

**仅当 A1 / A2 / A3 均失败**，但以下方式理论上可以得到 `ModInfo` 时，可做最小可行性探测：
reflection / MethodHandles / Unsafe / package injection / 字节码注入。

必须严格标记：

```
NORMAL JAVA PATH       = FAIL
NONSTANDARD PATH       = POSSIBLE
```

并记录：使用的机制、需要突破的访问控制、是否修改字节码、是否修改 NeoForge 核心、
是否依赖内部 package、是否依赖 Unsafe。

> **重要：即使非常规路径成功，也不能判定"兼容层可行"。**
> 只能判定：`[E]` 在非常规手段下可以制造符合 `ModSorter` 类型检查的对象。

---

### 001b-5-4：清理

若使用了临时 `ForgeCompatLanguageLoader` 或
`META-INF/services/...IModLanguageLoader`，**必须全部撤销**。
清理后的兼容层内容必须与 **001b-3-c 的 001b-1 等价基线**一致。

至少逐条验证：

- `ForgeCompatLanguageLoader.class`
- `META-INF/services/...IModLanguageLoader`
- `META-INF/services/...IModFileReader`
- `META-INF/services/...IModFileCandidateLocator`
- `MANIFEST.MF`
- `ForgeModFileInfo.class`

**清理标准不是 SHA-256 恢复，而是内容一致。**

- 若 SHA-256 恰好恢复 → 记录
- 若未恢复 → **不判定为实验污染**（编译环境 / ZIP 元数据变化）

---

## 五、成功与失败判定

**PASS-001b-5-A**：精确确定 `ModSorter` 对 `IModInfo` 的实际类型约束
（至少证明必须是 `ModInfo` / `ModInfo` 子类，以及所有已观测的后续直接契约）。

**PASS-001b-5-B**：明确回答"第三方兼容层能否通过正常 Java 路径产生 `ModInfo`"，
结果只能是 `YES` / `NO` / `UNKNOWN`。

**PASS-001b-5-C**：若存在正常构造路径，且
`真实 ModInfo → getMods() → ModSorter.sort() → 通过类型检查`，则 PASS。

> **注意**：不要把"`ModSorter` 没再抛 CCE"直接写成"`ModInfo` 契约全部满足"。
> 本 Spike 只验证**当前已暴露**的类型契约。后续若出现 `NullPointerException` /
> `IllegalStateException` / 字段访问错误 / 其它内部假设，
> 应作为**新的隐式约束**记录，而不是提前归类。

---

## 六、STOP 条件

| 情况 | 处理 |
|---|---|
| 无法取得精确 `ModInfo` 构造信息 | STOP |
| `ModInfo` 为 final 且无可访问构造路径 | 记录并 STOP |
| 只能通过修改 NeoForge 核心才能实现 | STOP |
| 必须修改 `ModSorter` 才能通过 | STOP |
| 只能依赖字节码注入 / Unsafe / ClassLoader hack | 记录为非常规路径，STOP |
| 正常路径成功但出现新的异常 | 记录 first confirmed failure，STOP |
| `ModSorter` 未被实际调用 | STOP |
| 出现新的 Container / Constructor 行为 | **不进入后续 Spike**，只记录实际到达位置 |

任何 STOP 后：

- **不得**进入 001b-2
- **不得**进入 001c
- **不得**自行设计修复方案
- **不得**自行扩大实验范围

---

## 七、Failure Chain

必须按实际调用证据填写：

```
Discovery:       YES
Metadata:        YES
Validation:      YES
Scan:            UNKNOWN
LanguageLoader:  YES
ModSorter:       UNKNOWN / YES / NO
Module:          UNKNOWN
Container:       UNKNOWN
Constructor:     UNKNOWN
```

> **特别注意**：`ModSorter 被调用 ≠ ModSorter 成功 ≠ Module 阶段进入`。
> 不得因为实验目标预期某阶段会发生而填写 YES。

---

## 八、结构化输出格式（强制）

报告必须严格按以下**十三节**结构输出，任何字段不得省略：

```
# Spike-001b-5 实验报告

## 一、实验环境
## 二、实验目标                     问题 A / 问题 B
## 三、源码契约                     ModInfo / ModSorter / NeoForge 内部创建点
## 四、正常 Java 构造可行性         A1 直接构造 / A2 继承 / A3 官方工厂 / 正常路径结论
## 五、运行时最小验证（若有）       临时 provider / getMods() / ModSorter / 第一个新失败点
## 六、非常规路径（若有）           reflection / MethodHandles / Unsafe / package injection / bytecode
## 七、Failure Chain
## 八、清理确认                     逐项 + 逐条目哈希对照
## 九、完整日志                     latest / debug / stdout / stderr / crash-report
## 十、事实标签                     [F] / [E] / [H] / [D]
## 十一、First Confirmed Failure    阶段 / 证据 / 异常 / 调用链 / 原因
## 十二、结论                       ModSorter 类型契约 / 正常 Java 构造 / 运行时通过 / 第一个新失败点
## 十三、禁止事项确认
```

---

## 九、交接规则

执行完成后，**报告原样交回架构决策方**。**不得自行决定下一 Spike。**

根据结果：

| 情况 | 条件 | 处置 |
|---|---|---|
| **A** | 正常 Java 路径可构造 `ModInfo` + 运行时通过 `ModSorter` 类型检查 | 001b-5 PASS → 重新评估 **001b-2** 是否值得执行 |
| **B** | `ModInfo` 可识别，但第三方正常路径**无法**构造 | 001b-5 PASS → Plan A 面临内部类型构造壁垒 → 架构决策方判断是否继续 Plan A |
| **C** | 必须修改 NeoForge 核心 / `ModSorter` | Plan A 正常路径被当前约束排除，但**不得自行宣布进入 Plan B** |
| **D** | `ModSorter` 通过 → 暴露新的硬约束 | 记录新的 First Confirmed Failure → 停止 → 交回架构决策方 |

> 情况 D 中的"新硬约束"可能是 001c 的输入，也可能需要一个新的 Spike（**001b-6**）。
> 由架构决策方判断，**不得由 Codex 自行决定**。

---

## 十、本 Spike 的真正使命

001b-5 **不是为了修复 `ClassCastException`**。

它只回答：

> **NeoForge 21.1.256 的 `ModSorter` 到底要求什么类型？
> 第三方兼容层有没有正常 Java 路径能够满足这个内部类型契约？**

证据链目标：

```
[F] ModSorter 强制 cast IModInfo → ModInfo
        ↓
[F] ModInfo 构造 / 继承 / 工厂可见性
        ↓
[E] 第三方是否能产生真实 ModInfo
        ↓
[E] ModSorter 是否接受
        ↓
[D] Plan A 是否继续具备技术基础
```

> **不要**将"存在 `ModInfo` 类"写成"`ModInfo` 可以由兼容层直接实例化"。
> **不要**将"非常规反射可以构造"写成"兼容层架构可行"。
>
> **001b-5 完成后必须停在这里。**
>
> Spike-001b-5 是 Plan A / Plan B 决策前的**最后一道硬门**。它的价值不在于消除
> `ClassCastException`，而在于让"兼容层能不能满足 NeoForge 内部类型契约"这个问题显形。
>
> **完成报告后不得进入下一 Spike。将报告原样交回，由架构决策方判断。**
