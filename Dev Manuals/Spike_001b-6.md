# Spike-001b-6 执行任务说明书

**接收方**：Codex
**前置文档**：《Forge → NeoForge 兼容层开发手册 v1.1》（含 §3.8）+《Spike-001b-5 实验报告》
**任务性质**：源码契约取证 + 编译可行性 + 运行时最小观测
**唯一目标**：验证兼容层是否能够通过正常 Java API 构造出同时满足 `ModSorter` 与
`LoadingModList` 类型契约的 NeoForge 原生 `ModInfo` / `ModFileInfo` 对象，
并让**两道 checkcast 均通过**。

```
Spike-001b-5   ✅ 完成（第一层契约暴露：IModInfo → ModInfo）
Spike-001b-6   ▶ 本说明书
Spike-001c     ⏸ 未开始
```

**001b-5 已证明**：`ForgeModFileInfo`（自定义 `IModFileInfo`）在 `LoadingModList.<init>` 抛
`Cannot cast …ForgeModFileInfo to …ModFileInfo`。
本 Spike **不再**重新验证 `javafml` 同名覆盖、`[52,)` vs `4.0.45`、或第一层
`IModInfo → ModInfo` 契约。

---

## 〇、执行前置

完整读取 v1.1 手册 §3.2 / §3.6 / §3.7 / §3.8 / §4.2 / §4.3 / §11 / §13 / 附录 A，
以及 Spike-001b-5 实验报告全文。

---

## 一、唯一目标（明确两道门）

成功必须是**两道 checkcast 都通过**：

```
ModSorter.sort(ModSorter.java:112)               → ModInfo modInfo = (ModInfo) imodInfo;     需要 PASS
LoadingModList.<init>(LoadingModList.java:54-58) → ModFileInfo.class::cast（on getModFileInfo()）需要 PASS
```

> **只通过第一道不算本 Spike PASS。**

要回答：兼容层能否在**不修改 NeoForge 核心**的前提下，通过正常 Java API 构造出
同时满足两道契约的 NeoForge 原生 `ModInfo` / `ModFileInfo`？

---

## 二、实验边界

### 2.1 只验证两道 cast

不解决：Forge constructor ABI、Forge `@Mod` entrypoint 解释、Forge `IEventBus` /
FMLJavaModLoadingContext、Scan 阶段、Registry / Event / Mapping / Mixin / Isolation、
Game Layer identity、Module / Container / Constructor 阶段的任何**行为**。

### 2.2 允许变更

- 修改 `ForgeModReader.java`、`ForgeModFileInfoParser.java`、`ForgeModFileInfo.java`
- 增加最小测试辅助类
- 临时恢复观测用 `IModLanguageLoader`（`name()="javafml"`，`version()="9999"`）
- 临时增加日志
- 使用精确版本 NeoForge / FML API 做源码、`javap`、编译验证
- 通过兼容层代码读取公开 API

### 2.3 禁止事项

- 修改 NeoForge 核心源码
- 修改 `ModSorter` / `LoadingModList` / `ModInfo` / `ModFile` / `ModFileInfo`
- 修改 `LanguageProviderLoader` / `VersionSupportMatrix` / `FMLModContainer`
- 修改 `forgeprobe.jar` / `mods.toml`
- 引入 ASM / Mapping / Mixin / Registry / Event / Isolation
- 改变 NeoForge / FML / Minecraft / Java 版本
- 使用 Unsafe / 字节码注入 / ClassLoader hack 制造假成功
- 为通过实验而修改 NeoForge 内部 class 文件
- **顺手修复任何新失败点**

---

## 三、精确环境（冻结）

```
Minecraft 1.21.1 / NeoForge 21.1.256 / FancyModLoader 4.0.45
Java 21（OpenJDK 21.0.10，Microsoft Build）/ Windows 11 amd64
启动方式 CLI / Dedicated Server
编译依赖 Forge 1.21.1-52.1.0
```

**环境冻结，不得修改。**

---

## 四、子步骤

### 001b-6-0：源码契约取证（不做运行）

从精确版本 `loader-4.0.45.jar` 字节码确认：

1. `ModFileInfo` 完整构造函数签名（尤其 `owningFile` 参数的具体类型）与参数语义
2. `ModFileInfo` 构造函数内部是否 `new ModInfo(...)`
   （**会** → 一次越过两道 cast 的路径成立；**不会** → `getMods()` 必须显式返回 `ModInfo`）
3. 第三个参数是否为函数式接口、其完整签名、能否被 lambda 实现
4. `ModFileInfo` 是否需要特定 `IConfigurable` 实现（`MapConfigurable` 这类是否会被拒绝）
5. `LoadingModList.<init>` 对 modFiles 元素是否还有除 cast 外的假设

**PASS-001b-6-0 判据**：五项均从精确字节码取得，**无 UNKNOWN**。

### 001b-6-1：构造探针（编译级）

与 001b-5 探针的区别：001b-5 只验证单点 `new ModFileInfo(...)`；
本探针必须验证**完整链**，放在独立目录、**不参与 jar 打包**：

```java
ModFile modFile = new ModFile(secureJar, new ForgeModFileInfoParser(), ModFileDiscoveryAttributes.DEFAULT);
ModFileInfo modFileInfo = (ModFileInfo) modFile.getModFileInfo();
for (IModInfo info : modFileInfo.getMods()) {
    if (!(info instanceof ModInfo)) throw new AssertionError("Not a ModInfo: " + info.getClass());
}
```

必须记录：编译结果、失败原因（完整 `javac` 错误）、需要的不可访问类型、
`ModFileInfo` 是否可从 `ModFile` 直接取得、`getMods()` 返回的 `ModInfo` 来源。

**PASS-001b-6-1 判据**：探针 `javac exit 0` 且四项均已记录。
若编译失败：记录完整错误 → 按 §六 判断 STOP，**不得**用非常规手段绕过。

### 001b-6-2：运行时最小验证

**前置条件**：001b-6-1 `javac exit 0`。

方法：

- 临时恢复 001b-3 同型观测 provider（`javafml` / `9999`）
- `ForgeModReader.read()` 改为返回 `new ModFile(secureJar, parser, attributes)`
  （**若 `IModFile.create(...)` 本身已返回 `ModFile`，则无需改动 —— 需以证据判断**）
- `ForgeModFileInfoParser.build()` 改为返回**真实 `ModFileInfo`**

必须观察：

1. `ModSorter.sort` 的 `ModInfo::cast`：不出现 `Cannot cast … to …ModInfo`
2. `LoadingModList.<init>` 的 `ModFileInfo::cast`：不出现 `Cannot cast … to …ModFileInfo`
3. 记录两道 cast 之后的**第一个新失败点**（阶段 / 异常类型 + 完整 stack trace / 调用链）

允许输出：两道 cast 都通过 → 后续阶段 → 新的异常（**记录但不修复**）。

**禁止**：顺手修复新失败点、扩大实验范围、进入 001c。

### 001b-6-3：清理

全部撤销：`ForgeCompatLanguageLoader`、`META-INF/services/...IModLanguageLoader`、
`ForgeModReader` / `ForgeModFileInfoParser` / `ForgeModFileInfo` 的临时变更。

清理后必须与 **001b-1 基线内容一致**，至少逐条验证：

`ForgeCompatLanguageLoader.class`（不存在）、`META-INF/services/...IModLanguageLoader`（不存在）、
`...IModFileReader`、`...IModFileCandidateLocator`、`MANIFEST.MF`、
`ForgeModReader.class`、`ForgeModFileInfoParser.class`、`ForgeModFileInfo.class`。

**清理标准不是 SHA-256 恢复，而是内容一致。**

---

## 五、PASS 判据

| 编号 | 判据 |
|---|---|
| PASS-001b-6-0 | `ModFileInfo` 契约已从精确字节码取得 |
| PASS-001b-6-1 | 编译探针 `javac exit 0` |
| **PASS-001b-6-A** | `ModSorter.sort` 的 `ModInfo::cast` 通过 |
| **PASS-001b-6-B** | `LoadingModList.<init>` 的 `ModFileInfo::cast` 通过 |
| PASS-001b-6-3 | 清理后与 001b-1 基线内容一致 |

**A 与 B 必须同时成立，本 Spike 才整体 PASS。只通过一道 ≠ PASS。**

---

## 六、STOP 条件

| 情况 | 处理 |
|---|---|
| 无法取得精确 `ModFileInfo` 构造信息 | STOP |
| 编译探针失败（`javac` 报错） | 记录错误，STOP |
| `ModInfo::cast` 通过但 `ModFileInfo::cast` 失败 | 记录第二道门失败形态，STOP |
| `ModFileInfo::cast` 通过但 `ModInfo::cast` 失败 | 记录第一道门失败形态，STOP |
| 只能通过修改 NeoForge 核心才能通过 | STOP |
| 必须修改 `ModSorter` / `LoadingModList` 才能通过 | STOP |
| 只能依赖字节码注入 / Unsafe / ClassLoader hack | STOP |
| 两道 cast 均通过但出现新异常 | 记录 first confirmed failure，STOP |
| 出现新的 Container / Constructor 行为 | **不进入后续 Spike**，只记录实际到达位置 |

任何 STOP 后：不得进入 001b-2 / 001c；不得自行设计修复方案；不得自行扩大实验范围。

---

## 七、Failure Chain

```
Discovery:       YES
Metadata:        YES
Validation:      YES
Scan:            UNKNOWN
LanguageLoader:  YES
ModSorter:       UNKNOWN / YES / NO
LoadingModList:  UNKNOWN / YES / NO
Module:          UNKNOWN
Container:       UNKNOWN
Constructor:     UNKNOWN
```

> `ModSorter 被调用 ≠ ModSorter 的 cast 通过`；
> `LoadingModList 被调用 ≠ LoadingModList 的 cast 通过`。
> 不得因为实验目标预期某阶段会发生而填写 YES。

---

## 八、结构化输出格式（强制）

报告须按**十二节**结构输出，字段不得省略：

```
## 一、实验环境
## 二、实验目标              两道 cast
## 三、源码契约（001b-6-0）  ModFileInfo / 第三参数 / LoadingModList 的其它访问
## 四、构造探针（001b-6-1）  编译结果 / 失败原因 / ModFileInfo 可否直接取得 / ModInfo 来源
## 五、运行时验证（001b-6-2）临时 provider / ModSorter / LoadingModList / 第一个新失败点
## 六、清理确认（001b-6-3）  逐项 + 逐条目哈希
## 七、Failure Chain
## 八、完整日志              latest / debug / stdout / stderr / crash-report
## 九、事实标签              [F] / [E] / [H] / [D]
## 十、First Confirmed Failure
## 十一、结论                两道 cast 结果 + 是否均通过 + 第一个新失败点
## 十二、禁止事项确认
```

---

## 九、交接规则

执行完成后**报告原样交回架构决策方**，**不得自行决定下一 Spike**。

| 情况 | 条件 | 处置 |
|---|---|---|
| **A** | 两道 cast 均通过 | 001b-6 PASS → **进入 001c 的前置准备** |
| **B** | `ModInfo::cast` 通过，`ModFileInfo::cast` 失败 | 记录第二道门失败形态 → 架构决策方判断 Plan A 是否失去技术基础 |
| **C** | `ModFileInfo::cast` 通过，`ModInfo::cast` 失败 | 记录第一道门失败形态 → 架构决策方判断路径设计 |
| **D** | 两道 cast 均通过但暴露新的硬约束 | 记录新的 First Confirmed Failure → 停止 → 交回 |

---

## 十、本 Spike 的真正使命

001b-6 **不是为了修复 001b-5 的 CCE**。它只回答：

> 兼容层能否通过正常 Java API 构造出同时满足 `ModSorter` 与 `LoadingModList`
> 类型契约的 NeoForge 原生 `ModInfo` / `ModFileInfo` 对象，并让两道 checkcast 均通过？

证据链目标：

```
[F] ModFileInfo 构造签名与内部 new ModInfo 位置
        ↓
[F] 编译探针 javac exit 0
        ↓
[E] ModSorter 的 ModInfo::cast 通过
        ↓
[E] LoadingModList 的 ModFileInfo::cast 通过
        ↓
[D] 是否进入 001c
```

> **不要**将"兼容层构造出了 `ModInfo`"写成"兼容层满足了 NeoForge 内部类型契约的全部要求"。
>
> **001b-6 完成后必须停在这里。**
