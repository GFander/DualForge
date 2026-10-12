# Convergence-004 Gate 0

本轮实际静态取证与最小实验已通过，生产代码在此阶段未修改。
起点 main：73edda36c5829c8bf98634674acd7899a7a0f5de。
原基线干净构建：out/convergence-004/baseline-build/20261012-151038-ad43b45f。

## 精确契约

锁定 JAR 的本轮全量 javap、实际命令及 SHA-256：
`out/convergence-004/contract/20261012-151103-e318f4b8/`。
其中 dependencies.json 含全部实际运行库指纹（FML4.0.45、bus8.0.5、
NeoForge21.1.256、ModLauncher11.0.5、ASM9.10.1）；Java21.0.10 实测见基线构建。

- NeoForge.<clinit> 调用 BusBuilder.startShutdown；EventBus 构造器读取该值。
- ServerModLoader.load()V 正常分支 offset176/179 在全阶段加载后调用
  NeoForge.EVENT_BUS / IEventBus.start()V。EventBus.start offset2 将 shutdown=false。
- 选择 net.neoforged.neoforge.event.server.ServerStartedEvent。
  ServerLifecycleHooks.handleServerStarted(MinecraftServer)V offset0/11 在全局总线上
  post 此事件。patched Main.main offset451 调用 ServerModLoader.load；后续才创建服务端。
  patched MinecraftServer.runServer()V offset19 在 initServer 成功后调用 handleServerStarted。
  因此此时严格晚于正常 start；driver 只在该事件中 post 自定义 Event，绝不自行 start。
- Neo IEventBus.post 描述符为
  (Lnet/neoforged/bus/api/Event;)Lnet/neoforged/bus/api/Event;，不是 Forge 的 boolean。
- EventBus.register(Object)V 扫描实例方法的 SubscribeEvent；registerListener 创建
  SubscribeEventListener，后者使用 EventListenerFactory 的 MethodHandle/hidden class。
  普通非取消事件 ListenerList unwrap handler，EventBus.post 内部 offset12 调用
  EventListener.invoke(Event)V，生成方法通过 MethodHandle.invokeExact 执行目标。
- 当前 rewriteSubscriber 仅接受一个 RETURN；004 非空监听器在原实现中会被拒绝。
  transform 的 catch(Throwable) 返回已被修改的 input，存在部分修改风险。
  本轮将仅以副本承载受控转换、失败抛出受控异常，保证调用者原节点不变。

## 可见性、开关和回调来源

选择公开 API 方案：LoadingModList.get().getModFileById("dispatchprobe")
→ IModFile.getSecureJar().name() → FMLLoader.getGameLayer().findLoader(name)
→ Class.forName(eventName,true,loader) → 公共无参构造器 newInstance。
只反射公共 Event 构造器，不调用 listener、不 setAccessible、不写模块描述符。
原生 driver 无需编译引用 Forge Event；运行时检查 Event 所属 module 和 loader。

最小实验在 `contract/VisibilityCheck.java`：真实 ForgeReader/LoadingModList 取得
自动模块名称，stock JDK 的临时 layer 验证 exports、公开类加载和构造。
这只是独立可见性模型；完整 FML GAME layer 可见性仍待 Gate 2 动态确认。
不增加生产 ClassLoader 或 Layer 策略。

选择系统属性 -DDF004_REGISTER=false/true。fixture 在 FML 构造阶段读取，
仅控制单个 register(this)；属性到达加载后模块的 false/true 实验均通过。
每轮 inputs.json 记录方式、值、读取位置，C/T 不重新构建。

选择 marker 方案 C，以满足 listener 必须保持四条 opcode 的限制。
测试 driver 在 post 周期暂时包装 System.out，透明转发 println 的原字符串；
仅当 StackWalker 的直接调用者是目标 listener 时记录栈（首业务帧为 listener），
并附运行 UUID。恢复原 PrintStream，不改 Event、listener 或总线。
不得在 listener 内拼接 UUID，因为那会违反获准的固定打印 profile。
`contract/StackCheck.java` 的打印点调用者捕获实验通过。此时尚未证明 FML 分发。

最小实验日志：out/convergence-004/contract/20261012-151309-de9ce2c6。
两组开关、公共构造和打印点栈核验 PASS；其源、命令、日志及输入均保存。

## 停止条件

若真实 GAME layer 的类访问失败、ServerStartedEvent 不到达、总线分发异常或
栈无法确认目标 listener/EventBus，则停止受影响路径并报告 PARTIAL/FAIL。
不修改 module-info、不突破封装、不提前 start、不新增 Forge post ABI。
原生 driver 只用于测试，不外推跨模组 Forge 订阅能力。
