# Spike-002-0b 母任务说明书

**文档性质**：母任务说明书 + 子阶段索引
**用途**：保留"为什么拆分"的上下文，防止未来只看到碎片报告时丢失设计意图
**版本**：v1.1（随 002-0b Step 2-A v1.1 完成一并归档）

---

## 1. 母任务背景

001c-2 的结论确立了 `FMLModContainer` 的**五层失败点结构**：

```
层 a  entrypoint 类加载
层 b  @Mod 注解解释
层 c  构造函数签名解析
层 d  参数注入
层 e  构造函数体
```

- **001c-2** 实测停在 **层 c**（`NoClassDefFoundError: net.minecraftforge.eventbus.api.IEventBus`）
- **002-0a** 补类型存在性后，层 c 解除，暴露 **层 d**
  （`RuntimeException: Mod constructor has unsupported argument`）

**002-0b 的目标：解除层 d。**

层 d 的失败根因：`FMLModContainer.constructMod()` 的参数白名单**精确匹配 `Class` 对象**；
Forge 命名空间的 `IEventBus` 与 NeoForge 命名空间的 `IEventBus` 是两个不同的 `Class`，
白名单不含 Forge 类型。

---

## 2. 为什么拆成 Step 1 → 002-0b-1 → 002-0b-1b → Step 2-A

**不拆的后果**：把"白名单判定机制取证"、"第三方类转换 SPI 取证"、"描述符改写探针实现"
塞进一个 Spike，变量太多——一次失败无法定位是哪一层的问题。

**拆分原则**：每个子阶段只解决一个独立问题，且只引入一个变量。

---

## 3. 子阶段关系图

```
002-0b  目标：解除层 d
│
├── Step 1 ──── 白名单判定机制取证（不运行）
│   │          产出：[F] 精确匹配
│   │          门槛：确认 exact match 后进入 Step 2-A；确认 assignable 后进入 Step 2-B
│   │
│   ├── 决策点：exact vs assignable
│   │
│   └── Step 2-A ─── 最小 ASM 构造函数签名重写探针（运行）
│       │          前置：002-0b-1 / 002-0b-1b 完成
│       │          产出：[E] 层 d PASS / FAIL + 层 e 进入与否
│       │          边界：层 e 进入即 STOP
│       │
│       ├── 002-0b-1 ─── 第三方类转换 SPI 静态链（不运行）
│       │   │            产出：[F] 候选通道 + SPI 形态 + 保存容器
│       │   │            结果：VALID / Chain Not Fully Traceable
│       │   │            （静态链到 ③ 为止，④⑤⑥ 未取证）
│       │   │
│       │   └── 002-0b-1b ─── ④⑤⑥ + targets() + 通道归属（不运行）
│       │       │             产出：[F] 完整静态链 + 实际 T + 通道归属
│       │       │             结果：PASS
│       │       │             （通道 A 可达；通道 B 静态不可达）
│       │       │
│       │       └── [允许恢复 Step 2-A]
│       │
│       └──（备注：Step 2-B 只在 Step 1 = assignable 时启用；本案不适用）
```

---

## 4. 各子阶段索引

| 子阶段 | 报告文件 | 状态 | 关键结论 |
|---|---|---|---|
| Step 1 | `Spike-002-0b Step1 取证报告.md` | ✅ | `[F]` 白名单判定 = **精确匹配**（含 raw bytecode 四条） |
| 002-0b-1 | `Spike-002-0b-1 取证报告.md` | ✅ | `[F]` 候选通道 2 条；`ClassProcessor` 不存在；静态链止于 ③ |
| 002-0b-1b | `Spike-002-0b-1b 取证报告.md` | ✅ | `[F]` ④→⑤→⑥ 同一静态路径；实际 `T` = `ClassNode`；通道 A 可达 |
| Step 2-A v1 | `Spike-002-0b Step2-A 执行报告.md` | ⛔ | STOP / 前置配置缺口（语言门失败；未达层 c/d） |
| Step 2-A v1.1 | `Spike-002-0b Step2-A v1.1 执行报告.md` | ✅ | **PASS：层 d 解除 + 层 e 进入**（marker 命中后 STOP） |

---

## 5. 关键约束（贯穿全 Spike）

| 约束 | 说明 |
|---|---|
| **单因子** | 每个子阶段只解决一个问题，只引入一个变量 |
| **不预设** | Step 1 前不预设白名单机制；1b 前不预设 ⑤ 形态；不预设 `ILaunchPluginService` 不可注册 |
| **不拼接** | 连续链必须由精确字节码建立的**同一静态调用路径**证明；不得拼接孤立证据 |
| **不越界** | 静态链允许记录 JDK / VM 边界（`[F-j]`），但不得跨越边界反推实现细节 |
| **不提前** | Step 2-A 的实现细节在 002-0b-1b PASS 前不得预先设计 |

---

## 6. 进入门槛

```
Step 1      → 002-0b-1      门槛：Step 1 = exact match
002-0b-1    → 002-0b-1b     门槛：002-0b-1 = Chain Not Fully Traceable（明确缺口在 ④⑤⑥）
002-0b-1b   → Step 2-A      门槛：002-0b-1b = PASS（④→⑤→⑥ 连续链 + 通道 A 可达）
Step 2-A v1 → v1.1          门槛：v1 STOP 已定位根因（前置配置缺口）+ 决策方补齐 configOf 定义
```

> Step 2-A v1.1 已完成（层 d 解除 + 层 e 进入）；**002-0c 方向未裁定前，不得设计 002-0c 的实现细节。**

---

## 7. 证据边界（贯穿全 Spike）

**静态取证链能证明**：静态调用链、SPI 形态、通道归属、实际 `T`、`targets()` 规则、
ForgeProbe 静态资格。

**该边界已由 Step 2-A v1.1 补上**（`[E]`，仅本探针路径）：ForgeProbe 本次运行真的进入
transform（`offered`）/ 改写真的解除层 d（`REWRITE hits=1`）/ 层 e 真的进入（marker）。

> 静态可达 ≠ 运行时发生。这是本母任务与其所有子报告的共同边界；
> v1.1 的运行时确认只覆盖**本探针路径**，不外推。

---

## 8. 教训归档

- 每次"临时修改后撤销"都会丢失实现细节：001b-6 的 `configOf` 未随报告保存，导致 v1.1 需决策方补齐定义；
- 前置配置错误 ≠ 探针实现缺陷：v1 STOP 属前置配置错误，v1.1 修复后 PASS；
- K1 对照（control + treatment）是单变量设计的必要代价：多一次运行换取"唯一变量"的严格性。
