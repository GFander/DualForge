# Spike-002-0c 执行任务说明书（方向 1）—— 冻结正文

**状态**：v1 冻结版（已交付；2026-10-07 归档）

---

接收方：Codex
前置文档：

· 《Forge → NeoForge 兼容层开发手册 v1.1（架构冻结版·最终）》§3.8.9 / §11
· 《Spike-002-0b Step 2-A v1.1 执行报告》（层 d 解除 + 层 e 进入）
· 《Spike_002-0b.md 母说明书》

任务性质：FML 生命周期基线实验（只观测，不修改）
唯一变量：0
执行上限：FML 完整启动（Done）或第一个新断层，二者之一出现即 STOP

```
Spike-002-0b Step 2-A v1.1   ✅ 完成（层 d 解除 + 层 e 进入）
Spike-002-0c                 ▶ 本说明书
```

---

## 〇、执行前置

请先完整读取：

· v1.1 手册 §3.8.9（五层失败点结构：a PASS / b UNKNOWN / c PASS / d PASS / e 已进入）
· v1.1 手册 §11（Spike 子链状态，截至 002-0b Step 2-A v1.1）
· 002-0b Step 2-A v1.1 报告 §四 / §六（marker 命中与 STOP 位置）

本 Spike 不重新验证：层 a–e 的任一结论、002-0b-2A 的描述符重写、通道 A 可达性、四项前置配置。

---

## 一、唯一目标

只回答一个问题：

在 v1.1 前置 + 描述符重写 + 层 e 进入的基线上，ForgeProbe 构造完成后，FML 自身走到哪里停下？

本 Spike 不回答：

· ForgeProbe 是否"成功"（成功的定义需要方向 2 的 API 主动探测）
· 参数注入 ABI 是否"完全兼容"
· 兼容层是否"可行"

本 Spike 的性质：建立生命周期事实基线——"一个只依赖 @Mod 入口、构造函数本身不调用额外 Forge API 的 Forge Mod，在 NeoForge/FML 生命周期中能走到什么位置"。

---

## 二、关键设计决定

### 2.1 复用 002-0b-2A 归档的探针 jar

不重新构建。 直接复用 build\forgecompat-002-0b-2a-v1.1.jar（SHA-256 E27BB965…97A436，25 条）。

理由：

· 002-0b-2A v1.1 已证明该 jar 使层 d 解除 + 层 e 进入
· 002-0c 的变量 = 0，基础必须完全一致
· 重新构建会引入"编译器版本 / 打包顺序"等无关变量

执行注记：build\forgecompat.jar 已在 002-0b-2A 的 2A-f 清理中恢复为 001b-1 基线，不可用；-ExtraModsJars 必须指向归档 jar。

### 2.2 复用 002-0b-2A 归档的 forgeprobe.jar

原样使用 forgeprobe.jar（SHA-256 A93A823C…374E22，1772 字节）。

禁止任何修改——包括：

· 添加 Forge API 调用
· 修改构造函数体
· 修改 @Mod 注解
· 修改 mods.toml

### 2.3 复用 002-0b-2A Run T 的 JVM 参数

-Dforgecompat.2a.rewrite=true —— 与 002-0b-2A Run T（treatment）完全一致。

### 2.4 关键边界变化：取消"层 e 进入即 STOP"

002-0b-2A 的观察纪律是"层 e 进入即 STOP，marker 之后内容不进入分析"。

002-0c 明确取消此约束 —— 本 Spike 的唯一观测对象就是"marker 之后 FML 自身的行为"。

marker FORGE PROBE CONSTRUCTOR REACHED 出现后，继续观测：

· FML 后续生命周期阶段
· 进程终止位置
· 任何新异常

### 2.5 与 002-0b-2A 的职责分离

| 项目 | 002-0b-2A 回答 | 002-0c 回答 |
|---|---|---|
| 层 d 的 Forge → NeoForge 构造参数 ABI 是否可通过 ModLauncher 第三方 ITransformationService 改写？ | ✅ 已答（PASS） | — |
| 该 ABI 问题跨过后，FML 自身还能继续走到哪里？ | — | ▶ 本 Spike |

本 Spike 不重新证明描述符转换 —— 描述符转换由 002-0b-2A 归档为已验证前提。

---

## 三、实验边界

### 3.1 允许的操作

允许：

· 复用 forgecompat-002-0b-2a-v1.1.jar 与 forgeprobe.jar
· 运行服务端（同 002-0b-2A Run T 的 JVM 参数）
· 完整读取 marker 之后的所有日志（stdout / latest / debug）
· 记录 FML 生命周期阶段的最后一条日志
· 记录进程终止位置（status / exitCode / duration）
· 追加 spike002-0c 到 run-server.ps1 的 ValidateSet（其余逻辑不得修改）

禁止：

· 修改 forgeprobe.jar（任何字节）
· 修改 forgecompat-002-0b-2a-v1.1.jar（任何字节）
· 修改 mods.toml
· 修改四项前置（observer / service / ModFileInfo / 空壳 IEventBus）
· 修改描述符重写逻辑
· 引入新 Forge API
· 引入新 Mod
· 修改 NeoForge / FML / ModLauncher / Minecraft / Java
· 顺手修复任何新失败点
· 扩大观测范围到"层 e 之后"之外的任何行为（如尝试推测 002-0d）

### 3.2 明确不做

· 不重新构建探针 jar
· 不重新验证层 a–e
· 不尝试方向 2（主动引入 Forge API）
· 不设计后续 Spike
· 不提出修复方案

## 四、精确环境（冻结）

```
Minecraft:            1.21.1
NeoForge:             21.1.256
FancyModLoader:       4.0.45
ModLauncher:          11.0.5
Java:                 21（OpenJDK 21.0.10，Microsoft Build）
OS:                   Windows 11 amd64
启动方式:             CLI / Dedicated Server
```

路径与工具（不变）：

· 探针 jar：D:\DualForge\spike001a\compat\build\forgecompat-002-0b-2a-v1.1.jar
· forgeprobe：D:\DualForge\spike000\probe\build\forgeprobe.jar
· 服务端：D:\DualForge\spike000\neoforge-server\
· 运行脚本：D:\DualForge\spike000\run-server.ps1
· 日志目录：D:\DualForge\spike000\logs\spike002-0c\
· JDK：C:\Users\Albert\.jdks\ms-21.0.10\bin\
· 沙箱外权限（回环 socket / DNS）需开放

Label：spike002-0c（追加进 run-server.ps1 ValidateSet）

---

## 五、子步骤

### 2C-a：环境核对（不运行）

唯一目标：确认四项前置完整 + 探针 jar / forgeprobe.jar 哈希匹配 002-0b-2A 归档值。

必须记录：

```
forgecompat-002-0b-2a-v1.1.jar:
- 路径:
- SHA-256:（应为 E27BB9659303DDEDF2EDE02D10B49CAFC586549C0EDA003A0165DF525F97A436）
- 字节数:（应为 20316）
- 条目数:（应为 25）

forgeprobe.jar:
- 路径:
- SHA-256:（应为 A93A823C988BFD6A8F6EA132D189D9DE732B0E5951F4BF74980D51BB63374E22）
- 字节数:（应为 1772）
```

STOP 条件：任一哈希不匹配 → 交回（不得重新构建）。

### 2C-b：服务端运行（唯一运行）

运行命令（同 002-0b-2A Run T）：

```
D:\DualForge\spike000\run-server.ps1 -Label spike002-0c `
  -ExtraJvmArgs '-Dforgecompat.2a.rewrite=true' `
  -ModsJar D:\DualForge\spike000\probe\build\forgeprobe.jar `
  -ExtraModsJars D:\DualForge\spike001a\compat\build\forgecompat-002-0b-2a-v1.1.jar
```

允许 run-server.ps1 的 ValidateSet 追加 spike002-0c；其余逻辑不得修改。

观测顺序：

1. 确认前置已生效（marker 之前的日志）：
   · javafml@null provider 出现
   · offered: com/example/forgeprobe/ForgeProbe
   · REWRITE hits=1
2. 确认层 e 已进入：
   · FORGE PROBE CONSTRUCTOR REACHED
   · BUS = net.neoforged.bus.EventBus@…
3. 继续观测 marker 之后：
   · FML 后续生命周期阶段
   · 每个阶段的最后一条日志
   · 进程终止位置

必须记录：

```
阶段 1：前置生效确认
- javafml@null provider: YES / NO
- offered: YES / NO
- REWRITE hits: <值>

阶段 2：层 e 进入确认
- marker 命中: YES / NO
- BUS 值:

阶段 3：FML 后续生命周期
- FML 生命周期事件清单（逐条记录）:
  - <事件名>: 到达 / 未到达
- marker 之后最后一条日志（原文）:
- 进程终止位置:
  - status:
  - exitCode:
  - duration:
- 是否到达 Done: YES / NO
- 若有新异常:
  - 异常类型:
  - 异常消息:
  - 完整 stack trace:
```

### 2C-c：判定（不改动任何文件）

判定规则：

```
若 FML 完整启动（Done）:
    → VALID-002-0c-1（完整启动）
若 FML 未达 Done 但到达明确阶段:
    → PASS-002-0c-1（记录该阶段 + 后续第一次失败）
若 FML 出现新异常:
    → 记录 first confirmed failure
若 marker 未命中:
    → STOP（前置配置错误，非本 Spike 范围）
```

不判定：

· ForgeProbe 是否"成功"
· 是否"兼容"
· 方向 2 是否必要

### 2C-d：清理

本 Spike 为只观测实验，不修改任何文件。

仅确认：

□ forgecompat-002-0b-2a-v1.1.jar 哈希未变
□ forgeprobe.jar 哈希未变
□ 探针源码树未修改（仍为 001b-1 基线 5 java）
□ mods.toml 未修改
□ NeoForge / FML / Minecraft / Java 版本未变
□ run-server.ps1 仅新增 spike002-0c Label，其余未动

---

## 六、判据

| 结果 | 定义 |
|---|---|
| PASS-002-0c-1 | FML 未达 Done，但到达某个明确的生命周期阶段；记录该阶段最后一条日志 + 后续第一次失败（如有）。判定等级：PASS（生命周期事实基线已建立） |
| VALID-002-0c-1 | FML 完整启动（Done）。判定等级：VALID（说明一个只依赖 @Mod 入口的 ForgeProbe 可被 NeoForge 完整加载） |
| STOP-002-0c-1 | marker 未命中 / 前置未生效 / 探针 jar 哈希不匹配。判定等级：STOP（前置配置错误） |
| FAIL-002-0c-1 | 观测本身无法执行（如服务端启动即崩在观察前阶段，非 ForgeProbe 相关） |

---

## 七、STOP 条件

| 情况 | 处理 |
|---|---|
| 探针 jar / forgeprobe.jar 哈希与 002-0b-2A 归档值不匹配 | STOP |
| marker FORGE PROBE CONSTRUCTOR REACHED 未命中 | STOP（前置配置错误） |
| 出现新异常 | 记录完整 stack trace，不修复，STOP |
| 需要修改任何文件才能继续 | STOP |
| 需要引入新 Forge API 才能继续 | STOP（属方向 2） |
| 需要重新构建探针 jar 才能继续 | STOP |
| FML 完整启动 | VALID，STOP |

任何 STOP 后：

· 不得自行设计修复方案
· 不得进入方向 2
· 不得修改探针
· 报告原样交回

## 八、结构化输出格式（强制）

```
# Spike-002-0c 实验报告

## 一、实验环境
- Minecraft / NeoForge / FancyModLoader / ModLauncher / Java:
- forgecompat-002-0b-2a-v1.1.jar SHA-256 / 字节数 / 条目数:
- forgeprobe.jar SHA-256 / 字节数:
- JVM 参数:
- Label:
- meta.json status / exitCode / duration:

## 二、前置生效确认（2C-b 阶段 1）
- javafml@null provider: YES / NO
- offered: YES / NO
- REWRITE hits: <值>
- 原文:
  <不得摘要>

## 三、层 e 进入确认（2C-b 阶段 2）
- marker 命中: YES / NO
- marker 原文:
  <不得摘要>

## 四、FML 后续生命周期（2C-b 阶段 3）
- FML 生命周期事件清单:
  | 事件名 | 到达? | 最后一条日志（原文） |
  |---|---|---|
  | FMLLoadCompleteEvent | YES / NO / 未观测 | |
  | FMLCommonSetupEvent | ... | |
  | FMLDedicatedServerSetupEvent | ... | |
  | FMLServerAboutToStartEvent | ... | |
  | FMLServerStartingEvent | ... | |
  | FMLServerStartedEvent | ... | |
  | Done | ... | |
  | 其它（如注册表同步 / 资源加载 / 网络初始化） | ... | |

- marker 之后最后一条日志（原文）:
  <不得摘要>

- 进程终止位置:
  - status:
  - exitCode:
  - duration:

- 是否到达 Done: YES / NO

- 若有新异常:
  - 异常类型:
  - 异常消息:
  - 完整 stack trace（原文）:

## 五、观测结论
- 本 Spike 结果: PASS-002-0c-1 / VALID-002-0c-1 / STOP / FAIL
- FML 到达的最后阶段:
- 后续第一次失败（如有）:

## 六、清理确认（2C-d）
- [ ] forgecompat-002-0b-2a-v1.1.jar 哈希未变
- [ ] forgeprobe.jar 哈希未变
- [ ] 探针源码树未修改
- [ ] mods.toml 未修改
- [ ] NeoForge / FML / Minecraft / Java 版本未变
- [ ] run-server.ps1 仅新增 Label

## 七、原始日志
- latest.log / debug.log / stdout.log / stderr.log / crash-report:
- 完整日志文件清单:

## 八、事实标签
[F] Source Fact:
[E] Experimental Fact:（本 Spike 的预期产出主体）
[H] Hypothesis:
[D] Design Decision:

## 九、结论
- 本 Spike 结果: PASS-002-0c-1 / VALID-002-0c-1 / STOP / FAIL
- 结论上限（禁止超出）:
  [E] FML 在 v1.1 前置 + 描述符重写 + 层 e 进入的基线上，走到哪个生命周期阶段；
  [E] marker 之后 FML 自身的行为（或无行为）。
- 禁止从本 Spike 推导:
  "ForgeProbe 已成功加载" / "参数注入 ABI 已完全兼容" /
  "Forge Mod 可被 NeoForge 加载" / "兼容层已可行" /
  "方向 2 应采用 XXX API"（方向 2 的 API 选择由后续 Spike 裁定）。
- Confidence: HIGH / MEDIUM / LOW
- Evidence:
- Hypotheses still unverified:

## 十、边界确认
- [ ] 未修改 forgeprobe.jar（任何字节）
- [ ] 未修改 forgecompat-002-0b-2a-v1.1.jar（任何字节）
- [ ] 未修改 mods.toml
- [ ] 未修改四项前置
- [ ] 未修改描述符重写逻辑
- [ ] 未引入新 Forge API
- [ ] 未引入新 Mod
- [ ] 未顺手修复任何新失败点
- [ ] 未进入方向 2
- [ ] marker 之后内容已完整观测（未因 002-0b-2A 的"层 e 进入即 STOP"约束而截断）
```

---

## 九、交接规则

执行完成后，报告原样交回架构决策方。不得自行决定下一 Spike。

根据结果：

**情况 A：FML 完整启动（VALID-002-0c-1）**

· → 说明"只依赖 @Mod 入口、构造函数不调用额外 Forge API"的 ForgeProbe 可被 NeoForge 完整加载
· → 建立"零 API 调用"的生命周期事实基线
· → 架构决策方裁定方向 2 的首个 Forge API 探针（MinecraftForge.EVENT_BUS / DeferredRegister / 其它）

**情况 B：FML 未达 Done 但到达明确阶段（PASS-002-0c-1）**

· → 记录生命周期阶段边界
· → 若该阶段之后有第一次失败 → 属新的 ABI 断层
· → 架构决策方裁定后续

**情况 C：新异常（first confirmed failure）**

· → 记录完整异常 + 阶段
· → 架构决策方裁定后续

**情况 D：marker 未命中**

· → STOP（前置配置错误，非本 Spike 范围）
· → 交回，由架构决策方裁定前置是否需修复

---

## 十、本 Spike 的真正使命

002-0c 不是为了证明兼容层成功，也不是为了探测下一个 ABI 断层。

它只回答：

在 v1.1 前置 + 描述符重写 + 层 e 进入的基线上，ForgeProbe 构造完成后，FML 自身走到哪里停下？

证据链目标：

```
[E] 前置生效（javafml@null + offered + REWRITE hits=1）
        ↓
[E] 层 e 进入（marker + BUS = net.neoforged.bus.EventBus@…）
        ↓
[E] FML 后续生命周期：
    - 到达的最后一个阶段
    - marker 之后最后一条日志
    - 进程终止位置
        ↓
[D] 方向 2 的首个 API 探针
```

不要将：

"FML 完整启动"

写成：

"Forge Mod 已能被 NeoForge 加载"

前者仅说明"零 API 调用的 ForgeProbe"可被完整加载；后者需要方向 2 及以上的多个探针。

不要将：

"FML 停在某阶段"

写成：

"兼容层失败"

前者只是生命周期事实基线的终点；后者需要方向 2 的验证。

002-0c 完成后必须停在这里。

---

## 十一、附：与 002-0b-2A 的对照表

| 维度 | 002-0b-2A v1.1 | 002-0c |
|---|---|---|
| 目标 | 层 d 解除 + 层 e 进入 | FML 生命周期事实基线 |
| 变量 | 1（描述符重写） | 0 |
| 前置 | 四项 + 探针 jar 构建 | 四项 + 002-0b-2A 归档 jar |
| JVM 参数 | -Dforgecompat.2a.rewrite=true | 同 |
| 观察范围 | marker 命中即 STOP | marker 之后继续观测 |
| 判据 | PASS / VALID / FAIL | PASS-002-0c-1 / VALID-002-0c-1 / STOP / FAIL |
| 输出 | 层 e 进入证据 | FML 生命周期终点 |

---

## 十二、交付清单

```
1. 本正文（冻结）
2. 归档至：D:\DualForge\Dev Manuals\Spike_002-0c.md
3. 关键哈希基线：
   - forgecompat-002-0b-2a-v1.1.jar:
     E27BB9659303DDEDF2EDE02D10B49CAFC586549C0EDA003A0165DF525F97A436 / 20316 字节 / 25 条
   - forgeprobe.jar:
     A93A823C988BFD6A8F6EA132D189D9DE732B0E5951F4BF74980D51BB63374E22 / 1772 字节
4. JVM 参数：
   -Dforgecompat.2a.rewrite=true
5. Label：
   spike002-0c
6. harness 改动：
   run-server.ps1 ValidateSet 追加 spike002-0c（其余未动）
```
