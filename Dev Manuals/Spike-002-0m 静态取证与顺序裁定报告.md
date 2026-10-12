# Spike-002-0m 静态取证与顺序裁定报告

2026-10-12。PASS-002-0m-1。root 单智能体串行，不是独立审查。九份全量 javap 原字节、实际命令/exit0 与输入前后指纹在 `D:/DualForge/spike000/logs/spike002-0m-artifacts`；无服务器运行，未改实现制品。补充复用0f Neo BusBuilderImpl dump（SHA 66B69738BA176421707D0DA0DD3EE9D02065CED36D56AE029E663B78BD48C237），对应制品 hash 与本轮相同，不冒称本轮新dump。

## 一、[F] 直接契约

1. `NeoForge.<clinit>` offset3 调用 startShutdown；BusBuilderImpl.startShutdown offset2置true。EventBus(builder) offset6读取该值，委托构造器offset46赋shutdown。[F] 默认初始化状态为shutdown=true；不声称没有任何其它第三方提前start。
2. `ServerModLoader.load` 正常分支17 begin→26 load→35 finish；随后处理loading issues，176 getstatic NeoForge.EVENT_BUS→179 `IEventBus.start()`→184 return。异常分支不走这个start。[F] 此已取证的FML正常调用点位于各加载阶段之后，不是Mod构造器里。[H] 0l已到Done但本轮未新增对该start调用的运行探针，不能叫本轮实际观测到start。
3. Neo `post(Event):Event` offset1读shutdown、4跳转；shutdown=true时7–8原样返回event，不读取监听器。false路径21 getListenerList→24 getListeners→27内部post；内部post offset12实际EventListener.invoke，Throwable分支handleException后再throw。[F] post正常返回单独不证明handler执行。
4. Forge `post(Event):Z` 调用带dispatcher重载；该重载shutdown=true时7–8返回false，正常结束139–143返回event.isCanceled()。[F] 接口的返回descriptor与Neo不同，不能把普通post当register那样只换namespace且保持Z。
5. 非ICancellableEvent类型：ListenerList构造器95–109设置canUnwrapListeners=true；buildCache offset86调用unwrapListeners；unwrap offset45调用IWrapperListener.getWithoutCheck，50替换条目。SubscribeEventListener.getWithoutCheck返回其handler。[F] 固定不取消的自定义事件静态采用无取消检查wrapper；不能由SubscribeEventListener.invoke里的cast孤立推出必然ClassCastException。
6. EventListenerFactory.create实例分支offset22 MethodHandle.invoke(Object)建立listener；createWrapper0 offset9 unreflect，35 asType，70 defineHiddenClassWithClassData，95 findConstructor。makeClass为invoke生成MethodHandle.invokeExact调用。[F] 本项目能证明生成逻辑与调用界限；JDK隐藏类定义/实际目标调用仍是边界，未反推VM实现、不新增未经核验的[F-j]。

## 二、裁定与下一个门槛

- [D] 不在ForgeProbe构造器中直接加post测试送达，也不为测试而提前start宿主全局bus；否则初始化状态或生命周期成为额外变量。
- [D] 下一轮先用隔离的、显式正常启动的Neo事件总线验证同形Forge默认注解+concrete事件的实际handler调用，K1仅post=false/true，源代码及已翻译class两轮相同。可借用0l T的固定ProbeEvent class；独立监听器须有明确计数/marker，不把原空body静默返回写成invoke证据。原Forge annotation compile→限定annotation rewrite的输入/输出完整保留。
- [D] 隔离夹具只证明该夹具的送达事实，不替代全局FML/ModuleLayer运行；宿主全局总线上的实际送达、Forge `post(...):Z`返回兼容另立实验。不得加入业务事件、核心补丁、Unsafe或私有bus状态探测。
- [H] 同形listener在隔离Neo bus中的运行创建/调用待实验；还没有监听器invoke的[E]。线程、取消、结果、MONITOR、继承扫描均UNKNOWN。

结论：注册返回、post返回、handler实际执行是三种证据。静态门槛完成，下一步可编制限定分发夹具说明书；本轮没有执行分发。Confidence HIGH（仅上述静态范围）。
