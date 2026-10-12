# Spike-002-0l：固定真实订阅 profile 的命名空间桥接

v1.0 冻结，2026-10-12。root 单智能体串行编制、执行、复核、裁定。输入依据 0k PASS 与 0j VALID；手册架构 v1.1 不变。

## 目标与变量

只回答：固定 Forge 注解的单个空实例订阅方法及最小 concrete 自定义事件，经限定 profile 规则能否使 Neo 全局 EventBus 的 register 正常返回？不证明事件实际分发、业务事件、取消、优先级、继承扫描或完整 Forge API 兼容。

K1 唯一运行变量：`-Dforgecompat.0l.subscriberRewrite=false/true`。同一新 probe、同一新 compat jar；共同 `2a.rewrite=true / 0g.apiRewrite=true / 0j.emptyRegistration=true`。先 C 后 T，每轮一次。新 probe/共同观测器是两轮共同前提，不能冒称与 0j 只有一个源码改动。

## 精确实现边界

工作副本 `spike000/probe-002-0l` 从 probe-002-0e 的 src 复制；原 D61224F3014F0ADFF85D10EEDEA414763C231367F08EDB99CE99DD2C8935F1C9 不动。只在 ForgeProbe 增 import Forge SubscribeEvent 和方法：

```java
@SubscribeEvent
public void onProbe(ProbeEvent event) { }
```

新增 `public final class ProbeEvent extends net.minecraftforge.eventbus.api.Event { }`，同包。既有构造器指令级不变（常量池编号/调试元数据不同不作语义差异）；新 onProbe 唯一 opcode return；新 ProbeEvent 默认构造器仅 aload0 / invokespecial Forge Event.<init>()V / return。mods.toml、manifest 不动。只用既存 Forge bus/universal 编译，不引入运行依赖或新 Mod。

compat 工作副本 `spike001a/compat-002-0l` 从 0j src 复制；原 D724FB489A3A1F1180A5FBA09DFF2FECD01FBD6F8AE33EE727851AB211FA44AB 不动。只改原 transformer 与 ForgeEmptyRegistration：

- transformer targets 恰为 ForgeProbe / ProbeEvent。ForgeProbe 原 ctor/API/empty route 完整保留；限定 onProbe descriptor `(Lcom/example/forgeprobe/ProbeEvent;)V`、public 非 static、唯一默认 RuntimeVisible Forge SubscribeEvent（values 空）且方法体 return；true 时仅 annotation descriptor→Neo SubscribeEvent。false 不改注解。
- ProbeEvent 必须 final concrete、Forge Event 父类、无字段、一个默认构造器且仅三条指令；true 时只改 superName 与 invokespecial parent owner→Neo Event；false 不改两引用。模式不符、缺失属性、任何 transformer ERROR/dump FAIL 均 STOP。
- 两种目标都最终 dump 到新的 `logs/spike002-0l-current/<简单类名>.class`；逐轮立即归档。旧 shared dump 输出仅 ForgeProbe 保持既有行为，历史逐轮原件不覆盖。
- helper 原固定零方法 no-op 判据保持；非零方法不得 no-op。delegate 日志后调用原 `bus.register(target)`，只在正常返回后打印 `[FORGECOMPAT-SUBSCRIBER] register returned target=... declaredMethods=1`。不 catch、不反射读取私有状态、不人工登记监听器。
- harness 只追加两个 label `spike002-0l-control / spike002-0l-treatment`，其它不变。

## 步骤与硬门

1. 归档前后源码、冻结文件/依赖/原产物指纹，编译9+helper等原全源和两probe源，保存实际命令/exit/原始输出；新旧 jar 内容比较：compat 仅 transformer/helper 两 class 不同，其它15内容逐字节一致；probe资源不变、旧构造器语义一致。
2. 强制 javap code+verbose。运行前固定新 jar/hash；校验现场原日志已归档、无 Java 进程，新 label 不存在，harness 清理绝对路径在指定 server 下且非 reparse point。
3. C：规则 false。归档全日志/stack/meta、两目标 dump（仅实际触达的目标），记录首确认失败；必须看到固定 profile 校验/规则禁用且无 register returned。预登记 [H] P1=缺 Forge Event / 类链接错误；P2=类型可解析但原注解不被 Neo 识别。无关前置漂移或模式门失败 STOP，不运行 T。
4. T：规则 true。要求注解 hits=1、事件 super/constructor hits=1+1；进入 delegate，不走 legacy-empty-noop，记录实际 register 后证据与首后续终点。出现新异常原样留存、不修复。
5. 两dump对照：ForgeProbe 原 ctor与 onProbe opcode C/T相同，仅运行可见注解 descriptor 不同；ProbeEvent opcode/descriptor相同，仅 superclass与super调用owner不同；前后所有固定制品不变。
6. 按结构化报告交环境/源码与构建/硬门/C/T完整原文路径/字节码对照/首次失败/事实标签/偏差/边界清单；同智能体顺序审查不得叫独立审查。完整原始日志附件与 SHA 索引保留。

## 判据与停止

PASS-002-0l-1：C 预登记边界成立、T 限定规则实际触达且 register returned 明确出现，或抵达一个已确认更后新边界并完整记录（此时不得说登记成功）。VALID-002-0l-1：前述真实 register 返回 + Done + 正常 stop。FAIL：T 仍同首失败且规则正确生效。STOP：模式/输入漂移、transformer异常、无关环境前置错误、工具/观测不完整。任何运行新失败后停止该轮，不修复、不把 exception 吞掉继续。

结论上限为固定最小 profile 的注册返回事实。监听器实际 invoke 仍 UNKNOWN；后续主动事件分发另立单变量实验。本轮不引入业务 API，不改核心、不将这个局部 jar 变成正式通用基线。
