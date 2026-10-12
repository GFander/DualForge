# Spike-002-0b Step 1 取证报告（白名单判定机制）

> **结论**：**Step 1 = 精确匹配 ⇒ 正确分支是 A（ASM 构造函数签名重写探针）。**
> **PASS-002-0b-Step1 ✅**
>
> **Step 2 本轮未执行。** 本报告**不包含**任何 Step 2 结果，也不构成 002-0b 的整体结论。

---

## 一、实验环境

同 002-0a：Minecraft 1.21.1 / NeoForge 21.1.256 / FancyModLoader 4.0.45 /
Java 21（OpenJDK 21.0.10，Microsoft Build）/ Windows 11 amd64 / CLI Dedicated Server /
编译依赖 Forge 1.21.1-52.1.0。

**Step 1 为纯取证：未运行服务端，未修改任何文件。**

---

## 二、前置配置确认

Step 1 不需要运行配置。三项前置配置（观测 provider / 真实 `ModFileInfo` / service 声明）
本轮未恢复、未使用。

---

## 三、Step 1：白名单判定机制取证

**取证对象**：`net.neoforged.fml.javafmlmod.FMLModContainer.constructMod()`
（`loader-4.0.45.jar`，`javap -p -c -constants`）。

### 白名单容器类型

`java.util.Map`（由 `java.util.Map.of` 构造）。

### 白名单内容（4 组键值，键为 `Class` 对象）

```
offset  70: ldc           class net/neoforged/fml/ModContainer
offset  72: aload_0
offset  73: ldc           class net/neoforged/fml/javafmlmod/FMLModContainer
offset  75: aload_0
offset  76: ldc           class net/neoforged/api/distmarker/Dist
offset  78: invokestatic  net/neoforged/fml/loading/FMLLoader.getDist()
offset  81: invokestatic  java/util/Map.of(Object,Object,Object,Object,Object,Object,Object,Object)
```

**白名单 4 组键值（offset 59–84，`javap` 原文，逐字，不得改写）**：

```
      59: aload_3
      60: iconst_0
      61: aaload
      62: astore        4
      64: ldc           #203                // class net/neoforged/bus/api/IEventBus
      66: aload_0
      67: getfield      #56                 // Field eventBus:Lnet/neoforged/bus/api/IEventBus;
      70: ldc           #2                  // class net/neoforged/fml/ModContainer
      72: aload_0
      73: ldc           #8                  // class net/neoforged/fml/javafmlmod/FMLModContainer
      75: aload_0
      76: ldc           #205                // class net/neoforged/api/distmarker/Dist
      78: invokestatic  #207                // Method net/neoforged/fml/loading/FMLLoader.getDist:()Lnet/neoforged/api/distmarker/Dist;
      81: invokestatic  #213                // InterfaceMethod java/util/Map.of:(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/util/Map;
```

**4 组键值（全部为直接可见的 `[F]`，不再是推断）**：

| # | 键（`Class`） | 值（注入对象） | 原始指令 |
|---|---|---|---|
| 1 | `net.neoforged.bus.api.IEventBus` | `this.eventBus` | `offset 64 ldc` / `66 aload_0` / `67 getfield #56 Field eventBus` |
| 2 | `net.neoforged.fml.ModContainer` | `this` | `offset 70 ldc` / `72 aload_0` |
| 3 | `net.neoforged.fml.javafmlmod.FMLModContainer` | `this` | `offset 73 ldc` / `75 aload_0` |
| 4 | `net.neoforged.api.distmarker.Dist` | `FMLLoader.getDist()` | `offset 76 ldc` / `78 invokestatic getDist` |

> 附带 `[F]`：`FMLModContainer` 拥有字段
> `private final net.neoforged.bus.api.IEventBus eventBus`
> —— 即白名单第 1 组的注入对象来源。

### 判定代码原文（`javap` 输出，不得摘要）

```
      86: aload         4
      88: invokevirtual #219   // Method java/lang/reflect/Constructor.getParameterTypes:()[Ljava/lang/Class;
      91: astore        6
      93: aload         6
      95: arraylength
      96: anewarray     #160   // class java/lang/Object
      99: astore        7
     127: aaload
     128: invokeinterface #228,  2  // InterfaceMethod java/util/Map.get:(Ljava/lang/Object;)Ljava/lang/Object;
     133: astore        10
     202: aaload
     203: invokeinterface #266,  2  // InterfaceMethod java/util/Set.contains:(Ljava/lang/Object;)Z
     208: ifeq          232
     262: invokevirtual #273  // Method java/lang/reflect/Constructor.newInstance:([Ljava/lang/Object;)Ljava/lang/Object;
```

### 判定机制归类

- [x] **精确匹配**（`Map.get` / `Set.contains` / `equals`）
- [ ] 可赋值匹配（`isAssignableFrom` / `instanceof`）
- [ ] 其它

**归类依据**：

1. 判定全程使用 `java.util.Map.get(Object)`（offset 128）与
   `java.util.Set.contains(Object)`（offset 203）—— 二者均为 `equals()` 语义的**精确匹配**。
2. 方法体**唯一**的 `instanceof` 在
   `offset 293: instanceof class java/lang/reflect/InvocationTargetException`，
   属**异常处理**分支，与白名单判定无关。
3. 方法体**不存在** `Class.isAssignableFrom` / `Class.isInstance`。

**PASS-002-0b-Step1 ✅**

---

## 四、Step 2 分支判定

| Step 1 结果 | 正确分支 |
|---|---|
| **精确匹配**（本轮） | **⇒ A（ASM 构造函数签名重写探针）** |
| 可赋值匹配 | ⇒ B（`extends` 探针） |

**本轮应执行分支 A，且不得执行分支 B。**

---

## 五、Step 2 未执行 —— 原因与交接

**Step 2（分支 A）本轮未执行。**

分支 A 需要新建一条**运行时字节码转换链**，其最小前置至少包括：

1. 取证 FML 4.0.45 中第三方可注册的 ClassProcessor / Transformer SPI 的确切形态
   （包名、层级、ServiceLoader 声明方式）—— 这本身是一次**独立取证**；
2. 实现只重写**单条构造函数描述符**的 transformer，并内置
   `rewrite target count = 1` / `non-target transformed class count = 0` 审计；
3. 构建、注册、运行，并对重写后字节码做四项验证
   （构造函数数量 = 1、原签名不存在、新签名存在、无其它 `<init>`）。

这是 002-0b 中**风险最高的部分**（ASM 运行时接入点尚未取证）。在接入点未确认时贸然实现，
最可能留下一条**半成品转换链**，污染 001b-1 基线，并使"层 d 是否解除"的观测失去可信度。

因此本轮**只交付 Step 1 这一 PASS 门槛**，并把仓库保持在**未修改状态**。

供架构决策方选择的三个选项：

```
选项 1：为 002-0b Step 2-A 单独拆一个前置取证 Spike（只做 ClassProcessor SPI 取证）
选项 2：直接授予 002-0b Step 2-A 的完整执行预算
选项 3：改走 002-0c（其它方向）
```

**本轮未执行任何 Step 2 内容**：未引入 ASM、未新增源文件、未运行服务端、
未修改 forgecompat 工程、未触碰 ClassLoader / ModuleLayer / `FMLModContainer` / ForgeProbe。

---

## 六、Failure Chain（Step 1 不产生新阶段证据）

```
Discovery … 构造函数签名解析:  全部沿用 002-0a（无新观测）
@Mod 注解解释:                UNKNOWN（沿用）
参数注入:                     FAIL（沿用 002-0a；Step 2 未执行，无更新）
构造函数体:                   NOT ENTERED
```

---

## 七、完整日志

Step 1 未运行服务端，故无新日志。
取证来源：`...\libraries\net\neoforged\fancymodloader\loader\4.0.45\loader-4.0.45.jar`
（SHA-256 `8390398e67df23e72d85ed809685be2752c8ec545b7cbb6ad4b70fdc81e4dcd4`）。

---

## 八、事实标签

### [F] Source Fact

1. `[F]` 白名单为 `Map.of(...)` 构造的 **4 组 `Class → 注入对象`** 映射，键与手册 §3.5 一致。
2. `[F]` 判定使用 **`Map.get(Object)`（offset 128）** 与 **`Set.contains(Object)`（offset 203）**，
   即 **`equals()` 语义的精确匹配**。
3. `[F]` 方法体内**不存在** `Class.isAssignableFrom` / `Class.isInstance`；
   唯一 `instanceof`（offset 293）针对 `InvocationTargetException`，属异常处理。

### [E] Experimental Fact

**无。** Step 1 为纯取证，未产生运行时观测。

### [H] Hypothesis

1. `[H]` 由于白名单为精确匹配，让 `net.minecraftforge.eventbus.api.IEventBus`
   继承 NeoForge `IEventBus`（分支 B）在机制上不会生效 —— 这是**源码推断**，
   本轮**未做分支 B 对照**，不作 `[E]`。
2. `[H]` 分支 A 的可行性取决于 FML 4.0.45 是否向第三方开放 ClassProcessor 接入点；**未取证**。

### [D] Design Decision

1. `[D]` 002-0b 的正确分支是 **A**；B 分支不应执行。
2. `[D]` 分支 A 的 ASM 运行时接入点是一项**独立前置取证**，建议先行。

---

## 九、结论

- Step 1 判定机制: **精确匹配**
- Step 2 分支: **A**（应执行但**本轮未执行**）
- Step 2 结果 / 层 d 是否解除 / 层 e 是否进入: **N/A**
- 本 Spike 整体结果: **Step 1 PASS；整体未完成（待 Step 2）**

**结论上限（禁止超出）**：

> `[F]` `FMLModContainer.constructMod()` 的白名单判定机制 = **精确匹配**（`Map.get` / `Set.contains`）。
>
> **未产生任何 `[E]`**：层 d 能否由签名重写解除、层 e 是否进入，**均未观测**。

**禁止从本报告推导**：层 d 可被解除 / ASM 重写可行 / 最终应采用 ASM /
其它 Forge 命名空间类型已兼容 / 全局重写策略已确定。

**Confidence**: **HIGH**（仅针对 Step 1 的机制归类）

**Evidence**: §三 的 `javap` 原文与三处归类依据。

**Hypotheses still unverified**: §八 `[H]` 1–2。

---

## 十、边界确认

- [x] **ASM 未被引入**（全程未触碰 ASM）
- [x] 未改变 ClassLoader / ModuleLayer 策略
- [x] 未修改 ForgeProbe 源码 / 字节码
- [x] 未修改 `FMLModContainer`
- [x] 未注入 `IEventBus` 实例
- [x] 未实现 Adapter / Proxy
- [x] 未执行 A 分支，也未执行 B 分支
- [x] 未进入 002-0c

---

## 十一、禁止事项确认

- [x] 未修改 NeoForge 核心
- [x] 未修改 `FMLModContainer` / `ModContainer` / `ModSorter` / `LoadingModList`
- [x] 未修改 `ModInfo` / `ModFile` / `ModFileInfo` / `ModFileScanData`
- [x] 未修改 `forgeprobe.jar` / `mods.toml`
- [x] 未引入 ASM / Mixin / Registry / Event / Isolation
- [x] 未使用 Unsafe / 字节码注入
- [x] 未进入 002-0c
- [x] 未提出修复方案
