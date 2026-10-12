# Spike-001b-1 执行任务说明书

**接收方**：Codex
**前置文档**：《Forge → NeoForge 兼容层开发手册 v1.1（架构冻结版·最终）》
**任务性质**：最小功能验证实验
**唯一目标**：让 `findLanguage()` 首次被调用

```
Spike-001b-0   ✅ 完成
Spike-001b-1   ▶ 本说明书
Spike-001b-2   ⏸ 未开始
Spike-001c     ⏸ 未开始
```

---

## 〇、执行前置

请先完整读取《Forge → NeoForge 兼容层开发手册 v1.1》，特别是：

- **§3.1**：SPI 分类表、`IModLanguageLoader` 解析链
- **§3.3**：Class Scanning 与 Entrypoint 识别阶段切割
- **§3.6**：Service Provider 加载机制
- **§3.7**：兼容层 JAR 当前已验证形态与未验证变量
- **§4.3**：ForgeModFileInfoParser
- **§11**：Spike 开发路线
- **附录 A**：事实标签体系

本说明书不重复手册内容，只定义 **执行流程** 与 **输出格式**。

---

## 一、唯一目标

**让 `ModFile.identifyLanguage()` 调用 `LanguageProviderLoader.findLanguage()`。**

不要求 `findLanguage()` 成功返回。

> **即使最后出现 `fml.modloadingissue.language.missingversion`，本 Spike 仍然可以 PASS。**
> 本 Spike 观察的是四个节点是否被依次打开：

```
getMods()                        → 非空
requiredLanguageLoaders()        → 非空
identifyLanguage()               → 被调用
findLanguage("javafml", [52,))   → 被调用
```

---

## 二、最小实现范围

| 组件 | 修改 | 不修改 |
|---|---|---|
| `ForgeModFileInfoParser` | 解析 `[[mods]]` 的 `modId` 与 `version`，构造 `IModInfo` | `mods.toml` 字段不扩展 |
| `ForgeModFileInfo.getMods()` | 返回包含 1 个 `IModInfo` 的列表 | `IModInfo.getLoader()` 可以不实现 |
| `ForgeModFileInfo.requiredLanguageLoaders()` | 返回包含 1 个 `LanguageSpec` 的列表：`languageName = "javafml"`，`acceptedVersions = VersionRange("[52,)")` | 不修改 `modLoader` / `loaderVersion` 字段 |
| `ForgeModReader` / `ForgeModLocator` | 不修改 | — |
| `forgeprobe.jar` | 不修改 | — |
| 兼容层 JAR 的 `META-INF/services/` | 不修改 | 不新增 `IModLanguageLoader` 声明 |

---

## 三、硬性约束（违反即实验无效）

### 3.1 单因子纪律

> 001b-1 **不得**为了让 `findLanguage()` 成功而注册任何新的 `IModLanguageLoader` provider，
> 也**不得**改变 `modLoader="javafml"` 或 `loaderVersion="[52,)"`。

001b-1 要观察的是：

```
Forge metadata
    ↓
LanguageSpec
    ↓
identifyLanguage()
    ↓
findLanguage("javafml", [52,))
    ↓
现有 provider resolution / version validation
```

**不是"先把兼容 loader 做出来"。**

### 3.2 禁止事项

- [ ] 不注册 `IModLanguageLoader`（Plan A / Plan B 均不在此 Spike 内）
- [ ] 不修改 `mods.toml` 的 `modLoader` / `loaderVersion`
- [ ] 不修改 `forgeprobe.jar`
- [ ] 不添加 `neoforge.mods.toml`
- [ ] 不修改 NeoForge 核心
- [ ] 不引入 ASM / Mapping / Mixin / Registry / Event / Isolation
- [ ] 不修改 `FMLJavaModLoadingContext` ABI
- [ ] 不触碰 `FMLModContainer`
- [ ] 不触碰 ForgeProbe 构造函数
- [ ] 不因为 `missingversion` 出现而自动跳到 001b-4

### 3.3 核心纪律

> **issue key 相同 ≠ 失败原因相同。**

`fml.modloadingissue.language.missingversion` 至少可能来自两种情况：

- ① `map.get("javafml") == null`（provider 未找到）
- ② `VersionSupportMatrix` 不通过（版本比较失败）

**001b-1 不允许仅凭 `missingversion` 就跳到 001b-4。**

> - 一个成功案例不构成必要条件。必要条件必须由负对照实验确认。
> - **class loaded ≠ method invoked ≠ stage entered ≠ operation succeeded**

---

## 四、实验环境（冻结）

```
Minecraft:            1.21.1
NeoForge:             21.1.256
FancyModLoader:       4.0.45
Java:                 21
OS:                   <系统信息>
启动方式:             CLI（官方 installer --installServer 产物）
客户端/服务端:        Server（Dedicated）

编译依赖 Forge 版本:  1.21.1-52.1.0
```

**不得修改上述任何版本。**

---

## 五、实验输入

### 5.1 forgeprobe.jar（不修改）

- SHA-256: 必须与 Spike-001a-0 一致（`a93a823c988bfd6a8f6ea132d189d9de732b0e5951f4bf74980d51bb63374e22`）
- 文件大小: `1772` 字节
- 内容与 Spike-001a-0 **逐字节相同**

### 5.2 forgecompat.jar（修改）

- 相对 001a-0 版本的 diff **必须仅为**：
  - `ForgeModFileInfoParser` 的 `[[mods]]` 解析逻辑
  - `ForgeModFileInfo.getMods()` 返回值
  - `ForgeModFileInfo.requiredLanguageLoaders()` 返回值
- `META-INF/services/` 内容**不变**
- `MANIFEST.MF` 内容**不变**
- **不新增任何 `IModLanguageLoader` 声明**

### 5.3 源码记录

必须保存实际参与编译的源码：

- `ForgeModFileInfoParser.java`
- `ForgeModFileInfo.java`

**不得只提供描述，必须提供原始源码文件。**

---

## 六、四节点判定

**节点 1：`getMods()` 非空**

- entered: YES / NO / UNKNOWN
- evidence:
- 若 NO，错误:

**节点 2：`requiredLanguageLoaders()` 非空**

- entered: YES / NO / UNKNOWN
- evidence:
- `languageSpecs` 列表内容:

**节点 3：`identifyLanguage()` 被调用**

- entered: YES / NO / UNKNOWN
- evidence:

**节点 4：`findLanguage("javafml", [52,))` 被调用**

- entered: YES / NO / UNKNOWN
- evidence:
- **执行分支判定（关键）**：
  - [ ] 进入 `map.get("javafml") == null` 分支（provider 未找到）
  - [ ] 进入 `VersionSupportMatrix` 分支（provider 找到，版本校验）
  - [ ] 进入其它分支（需描述）
- 返回结果 / 抛出的异常:
- 若抛异常，完整 stack trace:

> **注意**：节点 4 的"执行分支判定"是本 Spike 的**关键证据**。
> 不允许仅凭 `missingversion` 就判定为版本冲突。

---

## 七、Failure Chain（不写死）

| 阶段 | 状态 |
|---|---|
| Discovery | UNKNOWN / YES / NO |
| Metadata | UNKNOWN / YES / NO |
| Validation | UNKNOWN / YES / NO |
| Scan | UNKNOWN / YES / NO |
| LanguageLoader | UNKNOWN / YES / NO |
| Module | UNKNOWN / YES / NO |
| Container | UNKNOWN / YES / NO |
| Constructor | UNKNOWN / YES / NO |

**以实际调用证据填写，不得因为实验目标预期某阶段会发生而填写 YES。**

---

## 八、STOP 条件（按原因分类）

| 未打开节点 | STOP 原因 | 检查方向 |
|---|---|---|
| `getMods()` | `ForgeModFileInfo` 对 `[[mods]]` 的解析失败 | TOML 解析逻辑 |
| `requiredLanguageLoaders()` | `LanguageSpec` 构造与返回失败 | 构造与返回 |
| `identifyLanguage()` | `ModValidator` / `ModFile` 处理链未触发 | 调用链 |
| `findLanguage()` | `identifyLanguage()` 内部调用链未触发 | 内部调用 |

**`findLanguage()` 已打开但抛出异常时：不自动视为实验失败。** 必须先判断异常来源：

```
① provider 未找到
    → map.get("javafml") == null
    → 记录：provider lookup 失败

② VersionSupportMatrix 校验失败
    → 日志含 "Missing language javafml version ..."
    → 记录：版本比较失败

③ ServiceLoader / provider 实例化失败
    → 异常栈含 ServiceLoader / ClassNotFoundException
    → 记录：provider 加载失败

④ 其它异常
    → 完整保留异常栈，交回架构决策方
```

分类完成后停止，交回架构决策方。

---

## 九、结构化输出格式（强制）

报告必须严格按以下结构输出，任何字段不得省略。

```
# Spike-001b-1 实验报告

## 一、实验环境（精确版本）
Minecraft:            1.21.1
NeoForge:             21.1.256
FancyModLoader:       4.0.45
Java:                 21
OS:
启动方式:
客户端/服务端:

编译依赖 Forge 版本:  1.21.1-52.1.0

## 二、实验目标
<复述本说明书第一节>

## 三、实验输入
forgecompat.jar:
- SHA-256:
- 文件大小:
- JAR tree:
<完整 jar tf 输出>
- 与 001a-0 版本的 diff:

forgeprobe.jar:
- SHA-256:（必须与 001a-0 一致）
- 文件大小:

ForgeModFileInfoParser.java:
<实际参与编译的源码>

ForgeModFileInfo.java:
<实际参与编译的源码>

## 四、四节点打开证据

节点 1：getMods() 非空
- entered: YES / NO / UNKNOWN
- evidence:
- 若 NO，错误:

节点 2：requiredLanguageLoaders() 非空
- entered: YES / NO / UNKNOWN
- evidence:
- languageSpecs 列表内容:

节点 3：identifyLanguage() 被调用
- entered: YES / NO / UNKNOWN
- evidence:

节点 4：findLanguage("javafml", [52,)) 被调用
- entered: YES / NO / UNKNOWN
- evidence:
- 执行分支判定（关键）:
  - [ ] 进入 map.get("javafml") == null 分支（provider 未找到）
  - [ ] 进入 VersionSupportMatrix 分支（provider 找到，版本校验）
  - [ ] 进入其它分支（需描述）
- 返回结果 / 抛出的异常:
- 若抛异常，完整 stack trace:

## 五、版本校验结果
loader name         =
loader version      =
accepted range      =
comparison result   = PASS / FAIL / SKIPPED / UNKNOWN
validation location =
issue key（若有）   =

## 六、Failure Chain（更新）

Discovery:       UNKNOWN / YES / NO
Metadata:        UNKNOWN / YES / NO
Validation:      UNKNOWN / YES / NO
Scan:            UNKNOWN / YES / NO
LanguageLoader:  UNKNOWN / YES / NO
Module:          UNKNOWN / YES / NO
Container:       UNKNOWN / YES / NO
Constructor:     UNKNOWN / YES / NO

> 以实际调用证据填写，不得因为实验目标预期某阶段会发生而填写 YES。

## 七、First Confirmed Failure（若有）
阶段:
证据:
异常:
调用链:
原因分类（若为 findLanguage 异常）：
  - [ ] provider 未找到
  - [ ] VersionSupportMatrix 校验失败
  - [ ] ServiceLoader / provider 实例化失败
  - [ ] 其它

## 八、SPI 调用证据
IModFileReader:
- 沿用 001a-0 状态
IModLanguageLoader:
- 本次实验是否触发了 findLanguage？
- 触发了哪个 provider？（若可观测）
- provider Layer 是否可观测？

## 九、完整异常
<完整 stack trace，含所有 Caused by / Suppressed，若存在>

## 十、原始日志
- latest.log 路径:
- debug.log 路径:
- stdout 路径:
- stderr 路径:
- crash-report 路径（若存在）:
<不得摘要>

## 十一、事实标签
[F] Source Fact:
[E] Experimental Fact:
[H] Hypothesis:
[D] Design Decision:

## 十二、结论
四节点打开状态:
- getMods()                     PASS / FAIL / UNKNOWN
- requiredLanguageLoaders()     PASS / FAIL / UNKNOWN
- identifyLanguage()            PASS / FAIL / UNKNOWN
- findLanguage()                PASS / FAIL / UNKNOWN

findLanguage 执行分支判定:
- provider lookup 结果:
- version validation 结果:
- issue key:

Confidence: HIGH / MEDIUM / LOW
Evidence:
Hypotheses still unverified:

## 十三、禁止事项确认
- [ ] 未注册新的 IModLanguageLoader
- [ ] 未修改 modLoader / loaderVersion
- [ ] 未修改 forgeprobe.jar
- [ ] 未修改 NeoForge 核心
- [ ] 未引入 ASM / Mapping / Mixin / Registry / Event / Isolation
- [ ] 未触碰 FMLModContainer
- [ ] 未触碰 ForgeProbe 构造函数
- [ ] 未在报告中提出修复方案
```

---

## 十、PASS 判据

| 编号 | 判据 |
|---|---|
| PASS-001b-1-a | `getMods()` 返回非空 |
| PASS-001b-1-b | `requiredLanguageLoaders()` 返回非空 |
| PASS-001b-1-c | `identifyLanguage()` 被调用 |
| PASS-001b-1-d | `findLanguage("javafml", [52,))` 被调用 |

**四个节点全部 PASS 即 001b-1 PASS。**

`findLanguage()` 的返回值（成功或抛异常）**不影响 PASS 判定**；返回值只作为 **001b-4** 的输入数据。

---

## 十一、交接规则

```
完成报告后，不得进入 001b-2。将报告原样交回。

架构决策方根据四节点打开状态 + findLanguage() 的实际执行分支判断：

- 若四节点全开 + findLanguage 成功返回 provider
    → 进入 001b-2

- 若四节点全开 + findLanguage 明确进入 VersionSupportMatrix
  且因 version() 不满足 accepted range 而抛 missingversion
    → 进入 001b-4

- 若 findLanguage 抛 missingversion，
  但无法证明执行到了版本比较分支
    → 不得直接判定为版本冲突；停止并交回架构决策方

- 若抛出其它异常
    → 停止并由架构决策方判断

- 若四节点任一未开
    → 停止，不进入下一 Spike
```

---

## 十二、优先级（若输出受限）

若因上下文或执行限制无法完整输出，按以下优先级保留：

1. **四节点打开状态 + 节点 4 的执行分支判定（第四节）**
2. **完整原始日志 + 完整异常链（第十、九节）**
3. **Failure Chain 表（第六节）**
4. **实际 forgecompat.jar 内容/源码 + 精确版本（第三节）**

**任何情况下不得省略第四节和第十节。**

---

## 十三、本 Spike 的真正使命

拿到一条可证明的实际链：

```
ForgeModFileInfo.getMods()
        ↓
requiredLanguageLoaders()
        ↓
ModFile.identifyLanguage()
        ↓
LanguageProviderLoader.findLanguage("javafml", [52,))
        ↓
[provider lookup]
        ↓
[若找到，再进入 VersionSupportMatrix]
```

只要这条链**第一次被真实日志/调用证据打开**，001b-1 就完成了它真正的使命。

---

> Spike-001b-1 是"打开 `findLanguage` 调用"的最小功能实验。
> 它的价值不在于让 `findLanguage` 成功，而在于**让四个节点的状态显形**。
>
> **完成报告后不得进入下一 Spike。将报告原样交回，由架构决策方判断下一步。**
