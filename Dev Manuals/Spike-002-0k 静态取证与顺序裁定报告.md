# Spike-002-0k 静态取证与顺序裁定报告

2026-10-12。结果：PASS-002-0k-1。root 单智能体先取证、后顺序复核；不是独立审查。

## 直接证据

原始全文在 `spike000/logs/spike002-0k-artifacts`，九次 javap 命令、stdout/stderr 原字节及 exit 全部保留；均 exit 0。输入前/后指纹相同（完整 SHA 见 inputs-before/after.json）。终端默认 UTF-8 展示的本地日期可能乱码，原字节文件未重新编码；关键 JVM 名称/指令为 ASCII。

| 契约 | [F] 固定制品直接证据 | 结论边界 |
|---|---|---|
| SubscribeEvent | 双方都是 METHOD/RUNTIME；priority 默认各自 EventPriority.NORMAL，receiveCanceled 默认 false | 名称空间不同，不能相互认作同一注解 Class；本次仅允许默认成员，不涉及显式枚举值迁移 |
| Event 根类 | Forge public concrete，public `<init>()V`；Neo public abstract，protected `<init>()V`，仅声明 package boolean isCanceled | 不是全 API 同构；Forge 的取消/结果/phase/listenerList 方法不能由根类换名获得 |
| EventPriority | Forge 有 MONITOR 且实现 IEventListener；Neo 五个优先级、无 MONITOR、无该接口 | 默认 NORMAL 同名不证明其它优先级/监控语义兼容 |
| Neo register(Object) | 实例路径 getDeclaredMethods；155–160 精确检查 Neo SubscribeEvent；182 registerListener；249 增计数；零计数 263–284 throw | 空返回规则不能套到真实订阅方法 |
| registerListener | 参数数=1；35–42 Neo Event.isAssignableFrom；67–78 classChecker；107–112 register | 加载类型、注解存在、注册返回是三个不同门 |
| addToListeners | 0–26 abstract 检查并 throw；27–35 ListenerList.register；38–61 写 target listeners 集合 | 直接把参数根类型改为 Neo Event 会撞 abstract 门；不能当有效订阅类型 |
| NeoForge 全局总线 | classChecker 仅该 lambda 判 IModBusEvent.isAssignableFrom 并拒绝；全局 bus startShutdown | 无接口的自定义 concrete Event 候选静态不落入 IModBusEvent 分支；运行仍待证 |
| SubscribeEventListener | 构造器 EventListenerFactory.create；getAnnotation(Neo SubscribeEvent)；getPriority 读取注解 | factory 是记录的外部调用边界，本轮未证明运行创建成功；invoke/分发未测试 |

## 顺序复核与裁定

1. [D] 下一轮不用 Event 根类作直接订阅参数，使用只继承 Forge Event、仅默认构造器、无业务 API 的 concrete `com.example.forgeprobe.ProbeEvent`。映射只在该固定类的 superName 和 super `<init>()V` 调用 owner 上发生，不作通用重映射。
2. [D] ForgeProbe 增加一个 public 非 static `@SubscribeEvent public void onProbe(ProbeEvent event) { }`；不调用 post、不实例化事件、不改变现有构造器语句。默认注解 descriptor 的限定替换与事件 superclass 两引用组成一个 profile 规则，用一个运行属性开关进行 K1；不是分别证明三个修改都为必要条件。
3. [D] 两轮使用同一新 probe 和同一 compat jar。旧 0j 的全部前置与空 profile 分支共同保留；新增方法令空 profile 不成立。共同 helper 在实际 `bus.register(target)` 正常返回之后输出证据，不吞异常、不伪装空返回为注册。
4. [H] 关闭规则时可能在反射解析 ProbeEvent 过程中缺 Forge Event；开启后若能正常注册，须有真实调用后证据和最终 dump，Done 单独不算注册证据。新失败完整记录，不能当场修复。

本轮无服务器运行、无源码/jar/harness改动，未证明事件分发、取消/结果行为或一般 Forge Mod 兼容。Confidence：HIGH（仅上述静态契约）。新实现必须遵守下一冻结说明书。
