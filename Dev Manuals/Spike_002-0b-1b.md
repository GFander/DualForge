# Spike-002-0b-1b 执行任务说明书（静态链段 ④⑤⑥ 取证）

**接收方**：Codex
**前置文档**：《Forge → NeoForge 兼容层开发手册 v1.1》§3.6 / §10；
《Spike-002-0b Step 1 取证报告》；《Spike-002-0b-1 取证报告》
**任务性质**：**源码 / 字节码取证（不运行，不引入 ASM，不新增源文件）**
**唯一目标**：补全 002-0b-1 未完成的静态链段（④⑤⑥），确认 `targets()` / `TargetType` 匹配规则，
并判定 `FMLModContainer.<init>` 的 entrypoint 类加载路径实际关联的转换通道。

```
Spike-002-0b Step 1     ✅ 完成（白名单 = 精确匹配）
Spike-002-0b-1          ✅ 完成（VALID / Chain Not Fully Traceable）
Spike-002-0b-1b         ▶ 本说明书
Spike-002-0b Step 2-A   ⏸ 待 002-0b-1b 后决定
```

**本轮不重新验证**：Provider Discovery（002-0b-1 已证 ①②）、白名单判定机制（Step 1 已证）、
`ITransformer` / `ILaunchPluginService` / `TransformStore` 的接口形态（002-0b-1 已证）。

---

## 一、唯一目标（四个问题 + 一个结论项）

| 编号 | 目标 |
|---|---|
| **④** | 类加载触发点：`FMLModContainer.<init>` 的 entrypoint `Class<?>` 获取路径，实际经过哪个 ClassLoader / 类定义触发点？ |
| **⑤** | class bytes 获取点：进入转换链前，目标类字节以何种形态存在？转换链的实际输入类型 `T` 是什么？ |
| **⑥** | `transform` / `processClass` 静态调用点：具体是哪个接口方法被调用？接收的实际类型 `T` 是什么？ |
| **targets** | `ITransformer.targets()` 的匹配规则 + `TargetType` 语义 + ForgeProbe 是否满足静态目标条件 |
| **通道归属** | 实际关联的是通道 A（`ITransformationService.transformers()`）还是通道 B（`ILaunchPluginService`），或两者 |

---

## 二、关键纪律

### 2.1 不预设 ⑤ 的形态

禁止预设："必然是 `byte[] → ClassNode`" / "必然经过 ASM 解析" / "`T` 必然是 `ClassNode`"。
**由实际调用链决定输入形态与实际 `T`。** 通道 A / B 的内部流程都尚未取证。

### 2.2 不因"不在白名单"直接推结论

已知：`ITransformationService` 在 `TransformerDiscovererConstants.SERVICES` 白名单内，
`ILaunchPluginService` 不在。本轮必须用精确字节码把 **`ILaunchPluginService` 的
discovery / layer / classloader 条件**查清楚；
**不得**从"不在白名单"直接推出"第三方 `mods/` JAR 无法注册"，也**不得**反向推出"可以注册"。

### 2.3 判定链的连续性

核心判定链必须**连续、可连接**：

```
FMLModContainer.<init> → entrypoint Class<?> 获取 → 实际 ClassLoader / 类定义触发路径
    → 实际 transformation chain → 实际 transform / processClass 调用点 → 实际 T / TargetType
```

链必须完整从 `FMLModContainer.<init>` 接到 ⑥。**若中间断裂，不得用推断补上。**

### 2.4 连续链的严格定义

必须存在由精确字节码建立的**可连接调用关系**，使 ④ 的 entrypoint `Class<?>` 获取路径、
⑤ 的实际转换输入形成、⑥ 的具体调用**处于同一条可证明的静态调用路径上**。

**不得**将来自不同调用分支、不同初始化路径或仅有类型 / 字段关系的证据**拼接**为连续链。
**本项为本 Spike 最重要的防误判条款。**

### 2.5 不判定 Step 2-A 可执行

本 Spike **只取证**。是否恢复 002-0b Step 2-A 由架构决策方基于报告裁定。

---

## 三、实验边界

**允许**：读 `loader-4.0.45.jar` / `modlauncher-11.0.5.jar` / `bootstraplauncher-2.0.2.jar` /
`securejarhandler-3.0.8.jar` 的字节码（`javap -p -c -constants`）；`unzip` / `jar tf` / `strings`；
读 `META-INF/services/`；读 `module-info.class`；记录进入 JDK / VM class-definition boundary 前的
**最后一个 Java-visible 调用点**。

**禁止**：引入 ASM（哪怕 `compileOnly`）；新增任何源文件；运行服务端；修改任何工件；
改版本；用 IDE 自动重构；用 current/main 分支源码；**为推进 Step 2-A 而补充推断**；
**跨越 JDK / VM 边界强行反推类定义实现细节**。

**不做**：不设计任何实现；不写 Java 代码；不判定 Step 2-A 可执行；不声称任何运行时观测。

---

## 四、精确环境（冻结）

```
Minecraft 1.21.1 / NeoForge 21.1.256 / FancyModLoader 4.0.45
Java 21（OpenJDK 21.0.10，Microsoft Build）/ Windows 11 amd64
```

---

## 五、子步骤

### 002-0b-1b-a：通道 A / B 的 discovery / layer / classloader 条件

**通道 A（`ITransformationService`）**：`META-INF/services` 声明位置、白名单（YES）、
提升后 Layer、第三方 `mods/` JAR 实际可见性。

**通道 B（`ILaunchPluginService`）**：`META-INF/services` 声明位置、白名单（NO）、
**不被白名单提升时实际通过哪个发现机制**、**实际所在 Layer（需精确字节码确认）**、
第三方 `mods/` JAR 实际可见性、**是否存在其它提升通道**。

### 002-0b-1b-b：④ 类加载触发点取证

追踪：`FMLModContainer.<init>` → `List<String> entrypoints` 的处理 → `Class<?>` acquisition
（精确 API / 调用点）→ 实际 ClassLoader / ModuleLayer loader 实例来源 →
实际 `loadClass` / `findClass` / `defineClass` 或等价类定义触发点 →
（若存在）`TransformingClassLoader` / ModLauncher transformation 介入点。

必须记录：entrypoint `Class<?>` 获取 API；实际 ClassLoader 类型 / 实例来源；类定义触发路径；
若最终进入 JDK / VM boundary，**最后一个 Java-visible 调用点 + 精确字节码依据**；
是否经过 `TransformingClassLoader`；是否经过 ModuleLayer。

> **不要求**最终必须出现某个特定 `defineClass` 调用。**不得**因进入 JDK / VM 边界而 STOP，
> 也不得因进入该边界而人为补推断。

### 002-0b-1b-c：⑤ class bytes 获取点与实际 T 取证

必须记录：字节读取位置（ClassLoader / SecureJar / 其它）；进入转换链时的**实际类型**
（`byte[]` / `ClassNode` / 其它）；转换链入口类型；若发生 ASM 解析，解析在哪个方法；
实际 transformer 实现类（若已知）；**实际 `T`**；实际 `TargetType<T>` 实例（若已知）；
调用点接收参数的**字节码类型**。

### 002-0b-1b-d：⑥ transform / processClass 静态调用点取证

必须记录：调用点类名 / 方法名 / 调用指令（`invokeinterface` / `invokevirtual` / …）；
被调用接口 / 方法全名（是 `ITransformer.transform` 还是 `ILaunchPluginService.processClass`，还是其它）；
输入类型（实际泛型 `T`）；输出类型；调用点所属 Layer；该调用点的**静态前置条件**
（如 `classNeedsTransforming.contains(...)`）。

> 调用点静态存在 ≠ 运行时一定被触发。

### 002-0b-1b-e：④→⑤→⑥ 连续链与 entrypoint 静态时序

必须给出：连接点 1（④→⑤）与连接点 2（⑤→⑥）的位置 / 调用关系 / **是否同一静态调用路径**；
与 `FMLModContainer.<init>` 的静态时序（先于 / 后于 / 未知 + 依据）。

> **STOP**：若任一连接点断裂，或无法证明三点处于同一静态调用路径 →
> 记录 **VALID EXPERIMENT / Chain Not Fully Traceable (deeper)**，STOP，交回。

### 002-0b-1b-f：`targets()` / `TargetType` 匹配规则取证

必须记录：`Set<Target<T>>` 的元素类型；`Target<T>` 的字段 / 方法；已注册的 `TargetType` 实例
（如 CLASS / METHOD / FIELD）及各自语义；匹配算法位置（类 / 方法）；
匹配方式（按 class name String / 按 `Type` / 其它）；是否区分方法 / 字段；匹配的目标表示法
（内部名 / FQCN / 其它）。

并给出 ForgeProbe 静态资格判定：FQCN `com.example.forgeprobe.ForgeProbe` 是否满足
`targets()` 的匹配前提（YES / NO / UNKNOWN）+ 依据。

### 002-0b-1b-g：通道归属判定

判定 `FMLModContainer.<init>` 的 entrypoint 类加载路径实际关联的是通道 A / B / 两者 / 其它 / UNKNOWN，
并给出依据：④→⑤→⑥ 链中实际调用的是哪个接口方法、该方法的实际参数类型 `T`、
该接口的 provider 来源 Layer。

### 002-0b-1b-h：清理

本 Spike 为纯取证，未修改任何文件。仅确认：兼容层工程未修改 / 无新增源文件 / 无新增依赖 /
`forgecompat.jar` 未重新构建 / `mods/` 未修改 / 无新日志。

---

## 六、PASS 判据

| 编号 | 判据 |
|---|---|
| PASS-a | 通道 A / B 的 discovery / layer / classloader 条件均取证 |
| PASS-b | ④ 类加载触发点由精确字节码确认（允许进入 JDK/VM boundary，须记录最后 Java-visible 调用点） |
| PASS-c | ⑤ class bytes 获取点 + 实际 `T` 由精确字节码确认 |
| PASS-d | ⑥ 静态调用点 + 实际接口方法 + 实际 `T` 由精确字节码确认 |
| **PASS-e** | **④→⑤→⑥ 同一静态调用路径的连续链成立 + entrypoint 静态时序明确** |
| PASS-f | `targets()` / `TargetType` 匹配规则明确 + ForgeProbe 静态资格可判定 |
| PASS-g | 通道归属有字节码级依据 + 实际被调用的接口方法明确 |
| PASS-cleanup | 无副作用 |

**全部 PASS ⇒ 002-0b-1b 整体 PASS。**
**若 PASS-e 无法达成 ⇒ VALID EXPERIMENT / Chain Not Fully Traceable (deeper)，交回。**

**Runtime Observation: N/A** —— 本 Spike 明确禁止运行服务端，因此不产生 `[E]`。

---

## 七、STOP 条件

| 情况 | 处理 |
|---|---|
| 无法从精确版本字节码确认 ④ / ⑤ / ⑥ 任一 | STOP |
| 无法确定 entrypoint `Class<?>` 获取所用的实际 ClassLoader / 类定义触发路径（**不因"进入 JDK/VM 边界"而 STOP**） | STOP |
| ④→⑤→⑥ 任一连接点断裂 | 记录 VALID / Chain Not Fully Traceable (deeper)，STOP |
| 无法证明 ④⑤⑥ 处于同一静态调用路径（拼接孤立证据） | STOP |
| 无法确认实际 `T` / 实际接口方法 | STOP |
| 需要读 current/main 分支源码才能回答 | STOP（不得使用） |
| 无法判定 ForgeProbe 静态资格 | 记录 UNKNOWN，STOP |
| 需要运行服务端才能取证 | STOP |
| 为推进 Step 2-A 而补充推断 | STOP（违反 §2.3 / §2.4 / §2.5） |

任何 STOP 后：不得进入 002-0b Step 2-A；不得自行设计实现方案；不得扩大实验范围。

---

## 八、结构化输出格式（强制）

报告须包含以下十三节，字段不得省略：分析对象（含四个工件的 SHA-256）/
通道 A·B 的 discovery·layer·classloader 条件 / ④ 类加载触发点 / ⑤ class bytes + 实际 T /
⑥ 静态调用点 / **④→⑤→⑥ 连续链与静态时序（含"同一静态调用路径 YES/NO"判定）** /
`targets()`·`TargetType` 匹配规则 / 通道归属 / 清理确认 / 事实标签 / 结论 /
边界确认 / 禁止事项确认。

**结论节必须写明"证据边界"**：

| 能证明 | **不能**证明 |
|---|---|
| ④⑤⑥ 静态调用点与连续链 | ForgeProbe 本次运行真的进入 transform |
| 实际 `T` / `targets()` 规则 | 改写真的解除层 d |
| ForgeProbe 静态资格 / 通道归属 | 层 e 真的进入 |

**禁止从本 Spike 推导**：ASM 重写可行 / 层 d 可被解除 /
全局 Forge → NeoForge 重写策略已确定 / ForgeProbe 的字节码一定会被 transform。

---

## 九、交接规则

报告原样交回架构决策方。

| 情况 | 条件 | 处置 |
|---|---|---|
| **A** | 完整静态链 ④→⑤→⑥（同一路径）+ `targets()` 规则明确 + 通道归属明确 + 实际 `T` 明确 | 002-0b-1b PASS → 架构决策方裁定 Step 2-A 最小实现范围 |
| **B** | 链仍断裂 / 无法证明同一路径 | VALID / Chain Not Fully Traceable (deeper) → 裁定后续方向（可能需进一步细分或改走 002-0c） |
| **C** | 通道 B 的注册条件证明第三方不可注册 | 记录限制条件 → 裁定 |
| **D** | ForgeProbe 不满足静态目标条件 | 记录限制条件 → 裁定 |

---

## 十、本 Spike 的真正使命

**不是为了设计 ClassProcessor，也不是为了判定 Step 2-A 可执行。** 它只回答：

> ④ / ⑤ / ⑥ 是什么？三者是否形成同一静态调用路径上的连续链？
> `targets()` 的匹配规则是什么？ForgeProbe 是否满足静态目标条件？
> 实际关联的是哪条通道，实际被调用的接口方法是什么？

> **不要**将"静态链存在"写成"ForgeProbe 实际被 transform"。
> **不要**将"`ILaunchPluginService` 不在白名单"写成"第三方无法注册"。
> **不要**将"ForgeProbe 满足静态目标条件"写成"ForgeProbe 一定会经过该转换链"。
> **不要**将"链已完整"写成"Step 2-A 可执行"。
> **不要**将"分别找到了 `TransformStore` / ClassLoader / `ITransformer` 的调用点"写成"连续链成立"。
>
> **002-0b-1b 完成后必须停在这里。**
