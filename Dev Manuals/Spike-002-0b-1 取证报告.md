# Spike-002-0b-1 取证报告（第三方类转换 SPI 静态链）

> **结论（先行）**：**VALID EXPERIMENT / Chain Not Fully Traceable（情形 B）**
>
> **已证 `[F]`（可追踪部分）**：
> - 第三方类转换的**两条候选通道**均已定位，且**都存在**：
>   ① `ITransformationService.transformers()`（**在 §3.6 白名单内**）
>   ② `ILaunchPluginService`（ServiceLoader，**不在白名单内**）
> - ② 的 SPI 形态完整取得；③ 的保存容器（`TransformStore` / `TransformList`）已定位。
> - **重要更正**：FML 4.0.45 **不存在 `ClassProcessor` / `ClassProcessorProvider` 类** ——
>   手册 §10 T5 的「依赖 SPI」名称有误。
>
> **未证（本会话未能完成）**：④ 类加载触发点 → ⑤ class bytes 获取点 → ⑥ `transform` 调用点
> **未能取得字节码级证据**；`ITransformer` 的 `targets()` / `TargetType` 目标选择机制
> 与「ForgeProbe 是否满足静态目标条件」**均未判定**。
>
> 按 §六 STOP：「只能证明 Provider 被发现，但无法由精确字节码证明 ②–⑤ 的完整静态路径 →
> **STOP，不得判定 Step 2-A 可执行**」。
> **未进入 002-0b Step 2-A**，未引入 ASM，未新增源文件，未运行服务端。

---

## 一、分析对象

| 工件 | 说明 |
|---|---|
| `loader-4.0.45.jar` | SHA-256 `8390398e67df23e72d85ed809685be2752c8ec545b7cbb6ad4b70fdc81e4dcd4` |
| `modlauncher-11.0.5.jar` | SHA-256 `fd9b9fc7cf043d2264ea0113eac6b258f0a71fece5b00e831bfbdf9726c78a24` |
| `bootstraplauncher-2.0.2.jar` | 本会话未取证（未涉及） |
| `securejarhandler-3.0.8.jar` | 本会话未取证（未涉及） |

手段：`jar tf` + `javap -p -c -constants`。**未使用 current/main 分支源码。**

---

## 二、Provider Discovery 通道盘点（002-0b-1-a）

**通道 1 —— `TransformerDiscovererConstants.SERVICES` 白名单（v1.1 §3.6 已记录）**

```
cpw.mods.modlauncher.api.ITransformationService
net.neoforged.neoforgespi.locating.IModFileCandidateLocator
net.neoforged.neoforgespi.locating.IModFileReader
net.neoforged.neoforgespi.locating.IDependencyLocator
net.neoforged.neoforgespi.earlywindow.GraphicsBootstrapper
net.neoforged.neoforgespi.earlywindow.ImmediateWindowProvider
```

- 是否含 transform / class processor 相关项：**YES（`ITransformationService`）**
- `ILaunchPluginService` **不在**该白名单内。

**通道 2 —— ServiceLoader 直接加载的接口**（来源：`loader-4.0.45.jar` 自带
`META-INF/services/` 清单，即 FML 自身注册的 service 类型）

```
net.neoforged.neoforgespi.locating.IDependencyLocator
net.neoforged.neoforgespi.language.IModLanguageLoader
cpw.mods.modlauncher.serviceapi.ILaunchPluginService      ← 类转换 SPI
net.neoforged.neoforgespi.locating.IModFileCandidateLocator
net.neoforged.neoforgespi.locating.IModFileReader
cpw.mods.modlauncher.serviceapi.ITransformerDiscoveryService
cpw.mods.modlauncher.api.ILaunchHandlerService
cpw.mods.modlauncher.api.ITransformationService           ← 类转换 SPI
```

- 是否有其它与 transform 相关的 service 接口：**YES** —— 见上两处标记。
- `modlauncher-11.0.5.jar` 自身的 services 只有 `ILaunchHandlerService`。

**通道 3 —— `ITransformationService.transformers()`**

- 是否存在：**YES**
- 签名（`javap` 原文）：

```
public abstract java.util.List<? extends cpw.mods.modlauncher.api.ITransformer<?>> transformers();
```

**通道 4 —— 其它**：`ILaunchPluginService`（ServiceLoader；`processClass(Phase, ClassNode, Type)` 形态，
在 ASM `ClassNode` 层操作）。

**PASS-002-0b-1-a ✅**

---

## 三、SPI 接口形态取证（002-0b-1-b）

### 候选 1：`cpw.mods.modlauncher.api.ITransformationService`

| 项 | 值 |
|---|---|
| 接口全名 | `cpw.mods.modlauncher.api.ITransformationService` |
| 包名 | `cpw.mods.modlauncher.api` |
| 类可见性 | `public interface`（非 final，接口） |
| 公开方法（相关） | `public abstract java.util.List<? extends cpw.mods.modlauncher.api.ITransformer<?>> transformers();` |
| ServiceLoader 声明位置 | JAR 内 `META-INF/services/cpw.mods.modlauncher.api.ITransformationService` |
| 是否在 §3.6 白名单 | **YES** ⇒ `mods/` JAR 声明它会被提升进 SERVICE 层 |

### 候选 2：`cpw.mods.modlauncher.api.ITransformer<T>`

```
public interface cpw.mods.modlauncher.api.ITransformer<T> {
  public static final java.lang.String[] DEFAULT_LABEL;
  public abstract T transform(T, cpw.mods.modlauncher.api.ITransformerVotingContext);
  public abstract cpw.mods.modlauncher.api.TransformerVoteResult castVote(ITransformerVotingContext);
  public abstract java.util.Set<cpw.mods.modlauncher.api.ITransformer$Target<T>> targets();
  public abstract cpw.mods.modlauncher.api.TargetType<T> getTargetType();
  public default java.lang.String[] labels();
}
```

> 泛型 `T` 说明该 SPI 可作用于不同类型的 target（`TargetType<T>`）；
> 具体实例化的 `T`（如 `ClassNode`）**本会话未取证**。

### 候选 3：`cpw.mods.modlauncher.serviceapi.ILaunchPluginService`

```
public abstract java.lang.String name();
public abstract java.util.EnumSet<ILaunchPluginService$Phase> handlesClass(org.objectweb.asm.Type, boolean);
public default  java.util.EnumSet<ILaunchPluginService$Phase> handlesClass(org.objectweb.asm.Type, boolean, java.lang.String);
public default  boolean processClass(ILaunchPluginService$Phase, org.objectweb.asm.tree.ClassNode, org.objectweb.asm.Type);
public default  boolean processClass(ILaunchPluginService$Phase, org.objectweb.asm.tree.ClassNode, org.objectweb.asm.Type, java.lang.String);
public default  int     processClassWithFlags(ILaunchPluginService$Phase, org.objectweb.asm.tree.ClassNode, org.objectweb.asm.Type, java.lang.String);
```

> 输入是 **ASM `ClassNode`（已解析的树）**，不是 `byte[]`。

**PASS-002-0b-1-b ✅**（三个候选形态均已取证）

---

## 四、完整静态调用链取证（002-0b-1-c）

**证据边界声明**：本 Spike 追踪**静态调用链**，不产生运行时观测。
ForgeProbe 是否在本次运行中实际进入 transform 调用点 = **UNKNOWN / N/A**。

| 步骤 | 结果 |
|---|---|
| ① Provider 发现点 | **已证** —— `mods/` JAR 经 `ModDirTransformerDiscoverer`（§3.6 白名单）提升进 SERVICE 层后，`TransformationServicesHandler.discoverServices` 从 **SERVICE 层** `ServiceLoader` 载入 `ITransformationService`；②③ 的入口即 `ITransformationService.transformers()` |
| ② Transformer 产生点 | **已证** —— `ITransformationService.transformers()`，返回 `List<? extends ITransformer<?>>` |
| ③ Transformer 保存点 | **部分已证** —— `cpw.mods.modlauncher.TransformStore`：字段 `Set<String> classNeedsTransforming` 与 `Map<TargetType<?>, TransformList<?>>`；`TransformStore.getTransformersFor(String, FieldNode)` 已见 |
| ④ 类加载触发点 | **未取证** —— 未能定位 `TransformingClassLoader` 的 `loadClass` / `defineClass` 路径 |
| ⑤ class bytes 获取点 | **未取证** |
| ⑥ `transform` 调用点（静态） | **未取证** —— 仅知 `ITransformer.transform(T, ITransformerVotingContext)` 的存在 |

**静态时序结论**：**无法给出**。
既不能证明「`FMLModContainer.<init>` 的 entrypoint `Class<?>` 获取路径经过
`TransformingClassLoader` 并最终到达 ⑥」，也不能否定它。

**运行时执行状态**：

```
- 本 Spike 不运行服务端
- ForgeProbe 是否实际进入 ⑥ 调用点: UNKNOWN / N/A
```

**PASS-002-0b-1-c ❌（未达成）**

---

## 五、目标选择 / 命中机制取证（002-0b-1-d）

| 检查项 | 结果 |
|---|---|
| `targets()` / `getTargets()` / `runsFor()` / `transformType()` | `ITransformer.targets()` 存在（返回 `Set<ITransformer$Target<T>>`）；`getTargetType()` 存在；**其语义与判定规则未取证** |
| entrypoint FQCN 如何表示 | **UNKNOWN** |
| target type / transform type 限制 | **UNKNOWN** |
| className / resource name filter | **UNKNOWN** |
| target voting | `ITransformer.castVote(ITransformerVotingContext)` + `TransformerVoteResult` 存在；**投票语义未取证** |

**静态判定**：

- ForgeProbe 是否满足静态目标选择条件：**UNKNOWN**

**运行时状态**：

- ForgeProbe 在本次运行中是否实际被 transform：**UNKNOWN / N/A（本 Spike 不运行）**

**PASS-002-0b-1-d ❌（未达成）**

---

## 六、注册可行性判定（002-0b-1-e）

**问题 ①（Provider 怎么进入系统）**：在 `mods/` JAR 的 `META-INF/services/` 中声明
`cpw.mods.modlauncher.api.ITransformationService`（**在白名单内**）→ 被提升进 **SERVICE 层** →
ModLauncher 从 SERVICE 层 `ServiceLoader` 载入。

**问题 ②（Transformer 从哪里产生）**：由该 `ITransformationService` 实现提供
`transformers()` 返回 `ITransformer<?>` 列表。

**问题 ③（Transformer 怎么进入转换链）**：**部分回答** —— 保存到
`cpw.mods.modlauncher.TransformStore`（`Map<TargetType<?>, TransformList<?>>`）。
**静态触发时机未取证**。

**第三方实现可行性判定**：

- [x] `public` 接口，可第三方实现（`ITransformationService` / `ITransformer` / `ILaunchPluginService` 均为 public）
- [x] 需要特定 ClassLoader / Layer 上下文：**需要在 SERVICE 层可见**（通过 §3.6 白名单提升）
- [ ] 需要特定版本依赖（**未判定**）
- [x] 需要特定 ServiceLoader 声明位置：`META-INF/services/cpw.mods.modlauncher.api.ITransformationService`

**ASM 依赖来源取证**：

- `loader-4.0.45.jar` / `modlauncher-11.0.5.jar` 声明的依赖：**未取证**（未读 POM）
- ASM 是否内置：**未取证**（未做 JAR tree 全量比对）
- Frozen Runtime 实际解析到的 ASM artifact / version：**UNKNOWN**
  —— 可从 `server/libraries/org/ow2/asm/` 目录静态读出（本会话**未执行**该查证），
  **不硬填版本**。

**仅基于取证的最小步骤**（不实现）：

1. 在 `mods/` JAR 的 `META-INF/services/` 声明 `ITransformationService` 实现类
2. 该实现返回 `transformers()` → 一个 `ITransformer<ClassNode>`（或对应 TargetType）
3. `targets()` 声明目标类
4. —— 但**第 3 步之后的行为（何时被调用、targets 如何匹配）本轮未能取证**，
   因此**该步骤清单不完整，不足以支撑实现**

**PASS-002-0b-1-e ⚠️ 部分达成**（①②有答案；③与 ASM 依赖来源未完成）

---

## 七、清理确认（002-0b-1-f）

- [x] 兼容层工程未修改
- [x] 无新增源文件 / 依赖
- [x] `forgecompat.jar` 未重新构建
- [x] `mods/` 未修改
- [x] 无新日志（**未运行服务端**）

---

## 八、事实标签

### [F] Source Fact

1. `[F]` **FML 4.0.45 中不存在 `ClassProcessor` / `ClassProcessorProvider` 类**
   （`loader-4.0.45.jar` 中 `ClassProcessor|Transformer|transformation` 的类名检索仅命中
   `ModDirTransformerDiscoverer` / `ClasspathTransformerDiscoverer` / `TransformerDiscovererConstants`）。
   → 手册 §10 T5 的「依赖 SPI：`ClassProcessorProvider`」名称**在 4.0.45 中不成立**。
2. `[F]` `ITransformationService.transformers()` 返回
   `java.util.List<? extends ITransformer<?>>`。
3. `[F]` `ITransformer<T>` 的公开方法为 `transform(T, ITransformerVotingContext)` /
   `castVote(...)` / `targets()` / `getTargetType()` / `labels()`。
4. `[F]` `ILaunchPluginService` 的类转换入口为
   `handlesClass(org.objectweb.asm.Type, boolean)` 与
   `processClass(Phase, org.objectweb.asm.tree.ClassNode, Type[, String])`
   —— 输入是 **ASM `ClassNode`**，不是 `byte[]`。
5. `[F]` `cpw.mods.modlauncher.TransformStore` 持有
   `Set<String> classNeedsTransforming` 与 `Map<TargetType<?>, TransformList<?>>`，
   并提供 `getTransformersFor(String, FieldNode)`。
6. `[F]` §3.6 白名单含 `ITransformationService`，**不含** `ILaunchPluginService`。

### [E] Experimental Fact

**无。** 本 Spike 为纯取证，未运行服务端，不产生 `[E]`。

### [H] Hypothesis

1. `[H]` 第三方类转换的**实际接入通道**很可能是「`mods/` JAR 声明 `ITransformationService` →
   SERVICE 层 → `transformers()`」，因为该类型在白名单内；但**转换链的 ④⑤⑥ 段未取证**，
   该假设不能作为实现依据。
2. `[H]` `ILaunchPluginService` 虽是 ServiceLoader SPI，但**不在白名单内**，
   因此 `mods/` JAR 声明它**可能无法被提升进 SERVICE 层** —— 未做对照。
3. `[H]` `TransformStore.getTransformersFor(...)` 的按 target 分发可能即 ⑥ 的前置，
   但**未取证到调用点**。

### [D] Design Decision

1. `[D]` 手册 §10 T5 的 `ClassProcessorProvider` 应更正为
   `cpw.mods.modlauncher.api.ITransformationService#transformers()`（或
   `cpw.mods.modlauncher.serviceapi.ILaunchPluginService`），**待决策方确认后回写**。
2. `[D]` 按 §六 STOP：**不得判定 002-0b Step 2-A 可执行**。
   需要补做一段**只针对 ④⑤⑥ 与 target 选择机制**的取证（可命名为 002-0b-1b）。
3. `[D]` ASM 版本应从 `server/libraries/org/ow2/asm/` **静态读出**再填表，
   本轮未做，保持 `UNKNOWN`。

---

## 九、结论

| 判据 | 结果 |
|---|---|
| PASS-a 找到 transformation provider discovery mechanism | ✅ |
| PASS-b 找到第三方可实现的公开 SPI（完整签名 + 注册方式） | ✅ |
| **PASS-c 完整静态调用链（①→⑥）** | **❌ 未达成**（仅 ①②③，缺 ④⑤⑥） |
| **PASS-d 类加载时机 + 相对 entrypoint 类获取的静态先后** | **❌ 未达成** |
| PASS-e 目标选择 / 命中机制 + ForgeProbe 静态资格 | **❌ 未达成**（UNKNOWN） |
| PASS-f 注册所需 Layer / ClassLoader / dependency 条件 | ⚠️ 部分（③ 与 ASM 来源未完成） |
| PASS-cleanup 无副作用 | ✅ |

**Runtime Observation: N/A** —— 本 Spike 明确禁止运行服务端，因此不产生 `[E]`。

- 本 Spike 整体结果: **VALID EXPERIMENT / Chain Not Fully Traceable**

**结论上限（禁止超出）**：

> `[F]` FML/ModLauncher 4.0.45/11.0.5 中第三方类转换的**候选 SPI 通道**已定位并取证；
> `ITransformationService.transformers()` 在 §3.6 白名单内，`ILaunchPluginService` 不在；
> `ClassProcessor` / `ClassProcessorProvider` **不存在**。
>
> **未证明**：完整静态调用链（④⑤⑥）、`transform` 的实际调用时机、
> `targets()` 的匹配规则、ForgeProbe 是否满足静态目标条件。
>
> `[D]` **不得据此判定 002-0b Step 2-A 可执行。**

**证据边界（必读）**：

| 本 Spike 能证明 | 本 Spike **不能**证明 |
|---|---|
| SPI 是什么、怎么声明 | ForgeProbe 本次运行真的进入 transform |
| `transformers()` 的签名 | 改写真的解除层 d |
| `TransformStore` 的存在与字段 | 层 e 真的进入 |
| 白名单是否含某类型 | target 选择规则 |

**禁止从本报告推导**：ASM 重写可行 / ClassProcessor 实现方案已确定 /
层 d 可被解除 / 全局 Forge → NeoForge 重写策略已确定 / ForgeProbe 的字节码一定会被 transform。

**Confidence**: **MEDIUM**（已证部分 HIGH；未证部分不设置信度）

**Evidence**: §二–§三 的 `jar tf` 与 `javap` 原文。

**Hypotheses still unverified**: §八 `[H]` 1–3。

---

## 十、边界确认

- [x] 未预设 `ClassProcessor` / `ClassProcessorProvider` / `ITransformer` 名称
      （**由取证反证了 `ClassProcessor` 不存在**）
- [x] 未引入 ASM
- [x] 未新增任何源文件到兼容层工程
- [x] 未运行服务端
- [x] 未修改任何工件
- [x] 未声称任何运行时执行状态
- [x] 未进入 002-0b Step 2-A

---

## 十一、禁止事项确认

- [x] 未修改 NeoForge / FML 核心
- [x] 未使用 current/main 分支源码
- [x] 未使用 IDE 自动重构功能
- [x] 未提出实现方案
- [x] 未进入 002-0b Step 2-A

---

## 附录：交接建议

按 §九 属**情况 B（Provider 可注册，但完整静态链无法证明）**：

> `[D]` 002-0b Step 2-A 在当前取证深度下**不可执行**。
> 建议补做 **002-0b-1b**：仅针对 ④ 类加载触发点 / ⑤ class bytes 获取点 /
> ⑥ `transform` 调用点 / `ITransformer.targets()` 匹配规则 四项目标取证 ——
> 范围比本轮更窄，但正好是当前缺口。
