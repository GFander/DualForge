# 4C-3 全局 Bus ABI 与生命周期契约

本轮实际环境：Minecraft1.21.1、Forge52.1.0/eventbus6.2.27、NeoForge21.1.256/bus8.0.5、FML4.0.45、Java21。官方javap、依赖/JDK指纹与原生对照见out/convergence-007c4c3/abi和runtime/*-semantics，先Forge后Neo，均未安装DualForge。

| 来源 | 实际目标/桥接 | 受控条件 |
| --- | --- | --- |
| GETSTATIC net/minecraftforge/common/MinecraftForge.EVENT_BUS:Lnet/minecraftforge/eventbus/api/IEventBus; | 从GAME层neoforge模块取得真实NeoForge.EVENT_BUS:Lnet/neoforged/bus/api/IEventBus; | 不别名所有Bus，不返回原生Bus，不接收外部Bus参数 |
| ALOAD0; INVOKEINTERFACE Forge IEventBus.register(Object)V | ALOAD0; LDC计划指纹; INVOKESTATIC ForgeGlobalEventBridge.register(Object,String)V | 唯一已验证Context无参入口构造期；this、真实modId/ModFile/module/active owner及完整计划一致 |
| Forge @SubscribeEvent 无显式参数 | Neo @SubscribeEvent 默认NORMAL/receiveCanceled=false | 零个或一个public非static非继承handler；其他配置明确拒绝 |
| (Forge ServerStartedEvent)V | (Neo ServerStartedEvent)V | 业务指令不动，只改验证过的注解/参数/调试局部类型；运行时比较官方模块实际Class身份 |
| Item/Tab helper的Forge Mod Bus参数 | 既有ForgeRegistryBridge/构造器来源视图 | 同一计划内验证，仍绑定实际owner的独立Mod Bus，不混入Global |

独立原生测试观察：无handler Forge返回、Neo精确IAE；实例默认handler两宿主均真实派发一次；重复注册同对象仍一次；static实例注册Forge忽略、Neo拒绝；继承handler Forge发现并派发、Neo拒绝；非Event参数两者均拒绝。相关非法输入可能有预期官方ERROR，单独归为语义诊断，不能当健康空宿主基线。

## 空监听集合不是删除调用

新桥接不使用旧ForgeProbe身份no-op。它对原入口this真实调用NeoForge.EVENT_BUS.register。只有以下条件全成立，才翻译已知拒绝为Forge的空注册结果：

- 来源完整计划证明Object父类/无接口/无handler；注册在唯一入口构造期，目标为该this。
- 实际bus来自官方GAME层NeoForge.EVENT_BUS，与owner Mod Bus不是同对象；实现模块/类正确。
- 官方EventBus ClassFile SHA必须为 `cda8876c904d1374f77bbff02e8cdb01fba48ae0f528c69713eaefa11b5128a4`。
- IAE实际从该register抛出，类型和完整消息（包括换行）精确匹配“has no @SubscribeEvent methods”。实际javap表明该0方法分支无添加监听器调用，独立动态读取真实监听器表确认零个来源对象。

Source registration状态为EMPTY_SOURCE_TRANSLATION，记录真实宿主调用、对象/Bus身份和计划指纹；不伪造空listener。错误异常或no-op正常返回不能被当成功。其他失败保留cause/UNCERTAIN，重试拒绝。重复构造/注册在本受控合同中拒绝，比原生宿主更窄。

## 组合闭包与原子性

ForgeRegistryTransformationPlan先独立验证Registry/Supplier/Tab/Consumer，再校验Global/handler/Context。源类只读；所有必要类逐个克隆、映射、ASM检查完成后才返回一个不可变输出计划，ModLauncher只在整组成功后发布Targets。候选检测同时识别Global来源/注册使用，但不代替完整验证；不是删除排除判断后直接放行。

新标准夹具独立quietbus、signalbus、catalogbus，原生Forge编译，不含Neo或DualForge编译引用。组合组四Target为入口、Item helper、Tab helper和Holder Consumer。原4C-2后续类型/业务限制不放宽，未实现任意Registry或任意Java回调。Context校验器、旧Item/Block/自定义Event Profile和构造ABI原字节保持。

Global Contract为189断言/31负例：29个真实Reader/ModFile完整计划拒绝变体，另有平台归属scope guard和Profile冲突分类；不能将后二者冒充整个生产加载链动态测试。原ClassNode/源JAR不污染；旧4B VotingContext SHA、晚期失败与整组发布负例仍复跑。

## 实际动态与诊断边界

新原生Forge四次（quiet、signal、组合C/T）及最终同JAR Neo四次通过。真实ServerLifecycleHooks → EventBus.post → 来源handler，次数准确一次；独立读取官方监听器表验证handler对象及Bridge参数身份。Mod/Global两个实际对象不同。组合C/T只改变Registry挂接开关DF4C3_REGISTER，Global listener都存在；真实RegisterEvent先注册，icon/display延迟到服务端查询才执行。不是客户端UI证明。

Global故障观察器与标准运行分开。一个用真实入口反射重构的重复注册被拒绝；其他私有状态机故障明确是白盒：代理先调用真实原生注册（已返回，监听器已加入），随后注入异常。实测UNCERTAIN、拒绝再次调用、保留的监听器后续真实派发一次。这不是声称Neo内部扫描自行部分失败，也不是完整公共owner/caller路径的替代。错误空异常/no-op返回拒绝；不声称回滚或安全沙箱。

宿主错误与同轮空宿主完整severityRecords比较。保留Settings/Yggdrasil非零网络ERROR；strictZeroErrors=false，不能写成零ERROR。早期一次随机世界MonsterRoomFeature ERROR实际判FAIL，未放宽断言；最终新JAR健康组在新目录重跑通过。

纯Global但不使用现有Context链的其他构造形态、复杂handler业务、其他事件/优先级/取消/post、客户端UI均未扩展或验证。FnF另做完整只读计划，不是游戏启动。
