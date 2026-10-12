# Spike-000 执行任务说明书

**接收方**：Codex（具备完整代码执行与编辑能力）
**前置文档**：`Forge → NeoForge 兼容层开发手册 v1.1（架构冻结版·最终）`
**任务性质**：实验数据采集，**非修复任务**
**当前阶段**：架构冻结，事实验证未冻结

---

## 〇、执行前置

请先完整读取 `Forge → NeoForge 兼容层开发手册 v1.1`，特别是：

- **第三章**：FancyModLoader SPI 接入
- **第十一章**：Spike 开发路线
- **第十四章**：Spike-000 执行原则
- **附录 A**：事实标签体系

本说明书不重复该手册内容，只定义 **执行流程** 与 **输出格式**。

---

## 一、任务目标（不可变更）

**唯一目标**：确定 NeoForge 原生运行时面对一个原生 Forge 1.21.1 JAR 时，实际第一次失败在哪里。

**禁止目标**：
- 让 `forgeprobe.jar` 成功启动
- 验证兼容层架构
- 提出修复方案
- 修改 NeoForge 核心
- 写任何兼容层代码

**第一轮报告只回答**：`实际在哪里失败？`

---

## 二、实验环境冻结（精确版本）

在开始前，必须冻结以下精确版本。任何一项填 `UNKNOWN` 都必须在报告中标注为 `[H]`。

```
Minecraft:            1.21.1
NeoForge:             21.1.256
FancyModLoader:       <从启动日志中读取的实际版本，如 11.0.xx>
Java:                 21
OS:                   <系统信息>
启动方式:             <Gradle runClient / 启动器 / CLI>
客户端/服务端:        <Client / Server>

编译依赖 Forge 版本:  1.21.1-52.1.0
```

**不得使用 `21.1.x`、`52.x` 这类范围版本。** 必须到 patch 级。

**Forge 版本与 NeoForge 版本在报告中是两个独立字段，不能混为一谈。** Forge 版本用于编译依赖，NeoForge 版本用于运行时宿主。

> **FancyModLoader 的实际版本必须从基线启动日志中提取**，通常出现在启动日志开头几行，格式类似 `Starting FancyModLoader version 11.0.xx`。不得凭推测填写。

---

## 三、实验输入构建

### 3.1 构建 `forgeprobe.jar`

**必须保持 Forge 原生形态，不得做任何兼容性调整。**

JAR 内部结构：

```
forgeprobe.jar
├── META-INF/mods.toml
├── META-INF/MANIFEST.MF
└── com/example/forgeprobe/ForgeProbe.class
```

`META-INF/mods.toml`（Forge 原生格式，禁止改为 neoforge.mods.toml）：

```toml
modLoader="javafml"
loaderVersion="[52,)"
license="MIT"

[[mods]]
modId="forgeprobe"
version="1.0.0"
displayName="Forge Probe"

[[dependencies.forgeprobe]]
modId="minecraft"
mandatory=true
versionRange="[1.21.1]"
ordering="NONE"
side="BOTH"
```

`META-INF/MANIFEST.MF`：

```
Manifest-Version: 1.0
Automatic-Module-Name: forgeprobe
```

> `Automatic-Module-Name` **仅作为实验辅助条件**，不构成兼容层最终实现要求。用于观察 ModuleLayer 行为。

`ForgeProbe.java`（必须使用 **Forge 命名空间** 的 `IEventBus`）：

```java
package com.example.forgeprobe;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;

@Mod("forgeprobe")
public class ForgeProbe {
    public ForgeProbe(IEventBus bus) {
        System.out.println("=================================");
        System.out.println("FORGE PROBE CONSTRUCTOR REACHED");
        System.out.println("BUS = " + bus);
        System.out.println("=================================");
    }
}
```

**编译依赖必须为 Forge 1.21.1-52.1.0，禁止使用 NeoForge 依赖。**

`build.gradle` 关键行（必须保留原始行）：

```
minecraft 'net.minecraftforge:forge:1.21.1-52.1.0'
```

### 3.2 采集 JAR 元数据

执行并保存：

```bash
jar tf forgeprobe.jar
sha256sum forgeprobe.jar
stat -c%s forgeprobe.jar
```

同时保存 `META-INF/mods.toml` 与 `META-INF/MANIFEST.MF` 的**原始内容**。

---

## 四、执行流程

### 4.1 基线实验（无 forgeprobe.jar）

1. 清空 `mods/` 目录中所有非原生模组
2. 启动 NeoForge 21.1.256 一次
3. 从启动日志开头提取 FancyModLoader 实际版本
4. 保存完整日志：
   - `logs/latest.log`
   - `logs/debug.log`
   - `crash-reports/`（若存在）
   - 启动器 stdout / stderr
5. 命名为 `baseline.log`

### 4.2 Spike-000 实验（加入 forgeprobe.jar）

1. 将 `forgeprobe.jar` 放入 `mods/`
2. 再次启动 NeoForge 21.1.256
3. 保存完整日志：
   - `logs/latest.log`
   - `logs/debug.log`
   - `crash-reports/`（若存在）
   - 启动器 stdout / stderr
4. 命名为 `forgeprobe.log`

### 4.3 日志对比

```bash
diff baseline.log forgeprobe.log > spike000.diff
```

保存完整 diff 输出。

---

## 五、纪律约束（违反即实验无效）

### 5.1 禁止修复

一旦出现错误，**不得**：

- 加 `neoforge.mods.toml`
- 加 ASM
- 加 Mixin
- 加 Mapping
- 修改 NeoForge Loader
- 修改 ModuleLayer
- 添加 Registry/Event Shim
- 修改 ForgeProbe ABI
- 把 `@Mod` 改成 NeoForge 命名空间
- 为了绕过错误而添加任何兼容代码

### 5.2 禁止过度推断

- 日志中出现 `FMLModContainer` **不等于** Container 阶段进入
- `class loaded ≠ method invoked ≠ stage entered ≠ operation succeeded`
- 任何无法由日志、源码或调用链证明的结论，必须标记为 `[H]` 或 `UNKNOWN`

### 5.3 禁止提前总结

- **原始日志必须完整保留**，不得只提供摘要
- 不得只截取最后一段异常
- 必须保留完整 `Caused by` / `Suppressed` 链

### 5.4 禁止提出修复方案

第一轮报告**只回答“实际在哪里失败”**。所有修复建议留待 Spike-001a 阶段决策。

---

## 六、结构化输出格式（强制）

报告必须严格按以下结构输出，任何字段不得省略。无法确定的填 `UNKNOWN` 或 `N/A`。

```
# Spike-000 实验报告

## 一、实验环境（精确版本）
Minecraft:            1.21.1
NeoForge:             21.1.256
FancyModLoader:       <从日志读取的实际版本>
Java:                 21
OS:
启动方式:
客户端/服务端:

编译依赖 Forge 版本:  1.21.1-52.1.0

## 二、实验目标
<复述本任务说明书第一节>

## 三、实验输入
forgeprobe.jar:
- 文件名:
- SHA-256:
- 文件大小:
- JAR tree:
<完整 jar tf 输出>

META-INF/mods.toml:
<原文>

META-INF/MANIFEST.MF:
<原文>

ForgeProbe.java:
<实际参与编译的源码，不得描述>

编译依赖:
<build.gradle 关键行，保留原始内容>

## 四、基线实验（无 forgeprobe.jar）
结果: SUCCESS / FAIL
日志路径:
关键行:
异常（若有）:

## 五、Spike-000 实验（加入 forgeprobe.jar）
结果: SUCCESS / FAIL
日志路径:
关键行:
异常（若有）:

## 六、日志 Diff 摘要
<仅摘要 diff 中与 forgeprobe.jar 相关的新增行，原始 diff 单独保存>

## 七、Failure Chain

Discovery:
- entered: YES / NO / UNKNOWN
- evidence:
- error:

Metadata:
- entered: YES / NO / UNKNOWN
- evidence:
- error:

Validation:
- entered: YES / NO / UNKNOWN
- evidence:
- error:

Scan:
- entered: YES / NO / UNKNOWN
- evidence:
- error:

LanguageLoader:
- entered: YES / NO / UNKNOWN
- evidence:
- error:

Module:
- entered: YES / NO / UNKNOWN
- evidence:
- error:

Container:
- entered: YES / NO / UNKNOWN
- evidence:
- error:

Constructor:
- entered: YES / NO / UNKNOWN
- evidence:
- error:

## 八、First Confirmed Failure
阶段:
证据:
异常:
调用链:
对应源码入口（若可定位）:

## 九、SPI 调用证据
N/A（Spike-000 无兼容层代码，无兼容层 provider）

## 十、Module Identity（若实验走到此阶段）
SecureJar.name:
IModInfo.moduleName:
ModuleDescriptor.name:
GameLayer.findModule(...):
四重一致性: PASS / FAIL / UNKNOWN / N/A

## 十一、完整异常
<完整 stack trace，含所有 Caused by / Suppressed>
<不得截断>

## 十二、原始日志
- baseline.log 路径:
- forgeprobe.log 路径:
- crash-report 路径（若存在）:
- stdout / stderr 路径:
<不得摘要>

## 十三、事实标签

[F] Source Fact:
<可被源码或日志直接证明的>

[E] Experimental Fact:
<本次实验中实际观察到的>

[H] Hypothesis:
<合理但未被证明的>

[D] Design Decision:
<需留待后续架构决策的>

任何无法由日志、源码或调用链证明的结论，必须归入 [H] 或 UNKNOWN。

## 十四、结论
First confirmed failure:
Confidence: HIGH / MEDIUM / LOW
Evidence:
Hypotheses still unverified:

## 十五、禁止事项确认
- [ ] 未修改 ForgeProbe ABI
- [ ] 未添加 neoforge.mods.toml
- [ ] 未添加兼容层代码
- [ ] 未修改 NeoForge 核心
- [ ] 未在报告中提出修复方案
```

---

## 七、优先级（若输出受限）

若因上下文或执行限制无法完整输出，按以下优先级保留：

1. **完整原始日志 + 完整异常链**（第十二、十一节）
2. **Failure Chain 表**（第七节）
3. **实际 forgeprobe.jar 内容/源码 + 精确 NeoForge/FML 版本**（第三节）

**任何情况下不得省略第七节和第十一节。**

---

## 八、交接说明

完成报告后，**不得进入 Spike-001a**。将报告原样交回，由架构决策方根据第一条红线判断：

- 若失败在 Discovery / Metadata → Spike-001a 从 Locator/Reader 接入
- 若失败在 Validation / Scan → Spike-001a 从 ModValidator 接入分析
- 若失败在 LanguageLoader → Spike-001b 优先
- 若失败在 Module → Spike-001a 优先（Module Identity）
- 若失败在 Container / Constructor → Spike-001c 优先

**在架构决策方确认前，不得启动任何后续 Spike。**

---

## 九、版本选择说明

**NeoForge 21.1.256**：属于 1.21.1 的稳定维护分支，满足精确 patch 级冻结要求。FancyModLoader 版本随 NeoForge 自动绑定，必须从基线日志读取实际版本。

**Forge 1.21.1-52.1.0**：`52.1` 分支的 RB 1（Release Build 1），是该分支的起点版本。后续 52.1.1 到 52.1.16 均为在此基础上叠加的增量修补。选择分支起点版本可避免后期 patch 引入的无关行为干扰，同时与 `mods.toml` 中 `loaderVersion="[52,)"` 的语义一致。

**不得因为官网有更新的 52.1.16 就改用最新版。** Spike-000 的目标是定位失败阶段，不是测试“最新 Forge 是否能工作”。52.1.0 作为分支起点，是比任何后期 patch 都更合适的实验基准。

---

**Spike-000 是纯观测实验。它的价值不在于让 ForgeProbe 启动，而在于让第一条红线显形。**