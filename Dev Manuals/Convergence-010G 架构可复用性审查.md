# Convergence-010G 架构可复用性审查

审查范围为从010F `a8f288f34ac0e33546fa7ffe008899ee8a0a539f`到本轮生产源码的六个路径。原始JEI权限子契约获准静态证明，完整库存/发送流程继续拒绝。此审查不等价真实玩家或完整网络验收。

## Reusable Contracts

新增`ForgePermissionPlan`只读证明四个可达权限方法及clinit，区分creative、op、give三个配置角色、命令来源和精确控制流；原函数由宿主原生MC/Brigadier API运行。没有按JEI类名、modId或JAR SHA准入，也没有新增通用Forge权限命名空间。原Utility未触及的业务方法没有获得授权。

已有`ForgeBusIdentity`继续负责ModFile、Module、ClassLoader、Container；`ForgeConstructorSegments`处理构造片段；`ForgeRichCodecPlan`处理Codec；`ForgeNetworkClosurePlan`组合全源闭包与只读依赖；`ForgeNetworkBridge`保持注册阶段状态机；`ForgeSourceContextBridge`保持来源Context和原enqueue异常算法。G只在已验证权限Profile上启用专服/连接/玩家/队列线程检查，未建立第二套权限或网络运行时。

独立gatealpha/gatebeta保留全部原业务字节，使用同一谓词结构与不同模组配置/容器、相同原Forge SHA跨宿主运行。安全计数器、原函数组件、真实注册、真实玩家与投递分别有证据层，避免把组件布尔结果当授权身份。

## Mod-Specific Structural Assumptions

这些规则是可复用的有限结构契约，但仍强依赖Java21编译形状：固定局部变量数据流、条件跳转顺序、三个bool getter、两个LambdaMetafactory签名、Logger-only clinit、特定Optional组合、纯计数器Context、只允许serverbound与SERVER配置。一层派生Payload/Codec仍受F的有限规则约束。新编译器产生等价而不同形状可能被拒绝；不能描述成通用权限分析或任意Java控制流证明。

原JEI仅被验证了权限可达子图。原Utility包含其它库存/给物方法，这些原字节保留但不准入；原删除process在权限之后的containerMenu读取拒绝。其它Packet构造、响应、库存及客户端尚需各自契约，不能因一个helper验证通过批准整类所有调用。

独立配置getter沿用enabled/secondary/cancelDeath，角色由AccessRules的实际调用决定；原JEI保留原getter名称。这是数据流匹配，不能以名称相似代替语义。13项宿主ClassFile SHA固定Neo专服ABI，属于平台约束，不是模组发行白名单；客户端/Integrated Server不继承该证明。

## Shared Validation

沿用同一个ASM Cursor、克隆/SHA/规范化检查、Host资源读取、source fingerprint和foreign冲突机制。只读Utility与14项依赖不成为targets，但其字节与全22类闭包绑定，原Entry begin会重读核对。旧E/F输入、转换targets和严格负例未被整体改写。

身份检查在预计划、构造、注册、Handler和排队执行分别发生。这些重复检查针对不同生命周期和可能改变的状态，不能合并成永久“已验证=true”缓存。G重查的是同一玩家/连接/服务器引用与线程，不缓存权限结果；原配置与Native CommandNode在每次业务调用时取值。

旧Cursor的错误分类仍有耦合，例如权限条件跳转可能返回WRONG_EVENT_DOMAIN，字段错位可能返回UNKNOWN_BUS_ORIGIN。MATRIX保存分类到真实诊断的逐项映射，没有只换报错文字制造覆盖。测试Runner沿用现有SSD证据工具与官方服务器入口，E/F的新拒绝预期放在G适配副本，冻结测试保持原字节。

## Remaining Coupling

`ForgeDualBusPlan`仍组合较多职责；`ForgeNetworkClosurePlan`既解析源对象/handlers，也组装Codec/业务闭包。两者与具体构造布局仍有耦合，当前无法宣称任意构造、继承、发送或插件初始化都可组合。

G通过专门组件避免把整个权限控制流塞进DualBusPlan；只在Closure返回权限业务依赖集合、配置类型校验、注册完成阶段与Source Context入口增加必要接点。Runtime仍以source/host字符串及受控反射保持模块边界，重命名或宿主变更会严格拒绝。

有效真实ServerPlayer的正向身份路径、连接建立/关闭后的实际回调、真实玩家OP状态切换和客户端生命周期仍NOT_RUN。当前普通Player模型与真实owner的反例不能填补这些门禁；guard实现和静态API相等不是完整真实授权行为PASS。没有将库内的安全计数器扩大到库存操作。

## Recommended Future Refactoring

未来可将已证明片段表达为不可变业务/类型子计划，明确输入、依赖、方法集合、证据和失败分类，减少Closure解析器与具体构造器局部变量布局耦合。应先为现有旧Profile及源字节保全建立同一回归cohort，再逐项迁移，不能为消除重复验证扩大兼容范围。

可统一Cursor的领域无关错误上下文、公共宿主方法解析和数据流原语，并在真实不同编译器输出上验证后减少过严格模板假设。不能先开放任意分支、父类、Bootstrap或Forge API，再依赖少量正例补证明。

本轮未进行上述广泛重构。保留既有Profile和注册/异常语义，用六个必要生产接点完成权限证明与来源约束；实际回归、最终源码指纹与构建/提交绑定见本轮交付报告和SSD收据。
