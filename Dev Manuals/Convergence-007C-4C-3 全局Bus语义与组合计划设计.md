# 4C-3 全局 Event Bus 语义与组合计划设计

基线 main c4860adbea6a69e3a16499fc9eeb3e021519433a，PR27已正式合并，7e18910为祖先。起点生产、原4C-2归档和既有输入指纹记录在out/convergence-007c4c3/preflight/start.json；独立worktree，不改旧工作区。

## 实际官方差异（先取证，后实现）

固定Forge52.1.0/eventbus6.2.27和Neo21.1.256/FML4.0.45/bus8.0.5，MC1.21.1/Java21。实际javap及原生平台独立模组验证：

| 情形 | Forge | NeoForge |
| --- | --- | --- |
| EVENT_BUS字段 | MinecraftForge.EVENT_BUS: Forge IEventBus | NeoForge.EVENT_BUS: Neo IEventBus |
| register(Object) | INVOKEINTERFACE (Object)V | 同名同descriptor；行为不等价 |
| 无监听器实例 | 正常返回，不添加监听器 | 精确IllegalArgumentException，未添加监听器 |
| public实例默认SubscribeEvent | 实际ServerStartedEvent派发一次 | 对应宿主类型/注解，实际派发一次 |
| 同一监听器再次注册 | 返回，仍一次 | 返回，仍一次 |
| static方法却注册实例 | 忽略该方法 | 明确拒绝 |
| inherited handler | 扫描继承的方法 | 明确拒绝 |
| 错误事件参数 | 明确拒绝 | 明确拒绝 |

原生Forge先完成，随后Neo独立对照；均未安装compat，实际Mod Bus与Global Bus对象不同。完整命令/类/javap/异常/派发marker/生命周期在本轮abi与runtime。非法参数诊断可能产生预期官方ERROR，独立于健康输入基线；不能声称这些诊断零ERROR。

## 有界语义桥接裁定

只接受已识别同ModFile/metadata/module的Context入口、唯一构造期精确GETSTATIC MinecraftForge.EVENT_BUS + ALOAD0 + INVOKEINTERFACE IEventBus.register(Object)。注册对象只能为该入口this，单次调用；没有公开原生Bus accessor。

监听器仅允许零个，或一个public非static、非继承、默认注解参数的ServerStartedEvent方法；本轮业务体为既有受控打印marker，不更改其业务指令。新类型/priority/cancellation/post/未知注解及Bootstrap均拒绝，不扩大旧Profile。

无监听器路径不能复用旧ForgeProbe身份no-op：必须实际调用真实NeoForge.EVENT_BUS.register(同一入口对象)。只有精确验证目标为官方8.0.5 EventBus、ClassFile SHA固定、源/目标实例无注解监听器、异常实际来自该register且消息/类型完整匹配时，才将官方“没有监听器”的已知拒绝翻译为Forge的空监听集合注册成功。该官方分支在发现0方法后抛出，无addToListeners调用。记录一次真实宿主调用、owner/object/bus/计划SHA和明确的EMPTY_SOURCE_TRANSLATION状态；不是删除调用、伪造listener或任意异常吞掉。其他官方异常保留cause与UNCERTAIN，禁止重试。Bridge不实现post、不新增假事件。

有监听器路径只改已验证注解和参数类型为实际NeoServerStartedEvent，使用同一个入口实例调用真实宿主register。真实官方派发验证次数及栈。Mod Bus继续原Context/RegistryBridge，不与Global混用。重复注册在受控合同中明确拒绝（比两个原生宿主的静默幂等更窄）。

## 不可分割计划

复用ForgeRegistryTransformationPlan：先验证Registry/Supplier/Tab/Consumer闭包，独立验证Global入口及监听器，核对同一所有权与Context生命周期；对源ClassNode只读。旧Context校验器保持原样；在新Global专用副本中剥离已验证handler后复用它，而不是放宽旧extra-method规则。

源/宿主指纹进入一个不可变计划。全部入口/Helper/Consumer克隆、边界映射及ASM验证完成后，才发布完整Target集合。独立Global入口也走相同计划而非另建加载架构。候选检测只是粗选，不能取代完整计划；错误来源、冲突、无闭包、ASM失败均不得发布部分Registry Target。

本轮FnF仅强制只读完整计划，不定义/初始化/执行发行类；新的global-v1收据与历史typed-v1分开。FnF运行、客户端和业务post仍NOT RUN。公开原始证据另需具体载荷授权；当前不发布本轮原始附件。
