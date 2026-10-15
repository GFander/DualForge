# Convergence-007C-4B FML目标发现与加载时序取证

日期2026-10-15（Asia/Hong_Kong）。先行只读取证，生产实现尚未开始。固定MC1.21.1/NeoForge21.1.256/FML4.0.45/ModLauncher11.0.5/Java21。原始完整javap、argv、exit及官方JAR SHA保存于`out/convergence-007c4b/timing/`；BackgroundScanHandler首次猜测的包名失败保留，真实类在`loading.modscan`，已重新取证。

## Git门槛

重新fetch得到main=`6d7c0bf3643cbb2c0f211a083dba7a373f2e47aa`。C3最终29893683、C4A最终68e5dc3、集成78e9fc5及文档566e25e均为main祖先。21份生产SHA与已审核C4A逐项一致，Context两关键文件、OfflinePrototype及summary齐全且字节一致；PR23实际MERGED到main，Actions38722346778的head_sha精确等于main且success。BASELINE_NOT_CONVERGED已解除。独立正式分支`codex/convergence-007c4b-registry-bridge`从该main创建，起点工作区干净；以前的Gate0报告作为历史事实不重写。

## 实际调用链

1. ModLauncher `Launcher.run`：PLUGIN层建立offset102，`triggerScanCompletion`114，`initialiseServiceTransformers`200，`buildTransformingClassLoader`240，最后才取得GAME层并launch。这里的offset来自实际11.0.5字节码，不是较新版本源码。
2. FMLServiceProvider.completeScan委托FMLLoader.completeScan，已有实际LoadingModList/ModFile/ModInfo及模块描述身份；尚未创建GAME的java.lang.Module对象和正式ModContainer。
3. TransformationServiceDecorator.gatherTransformers的lambda在offset15调用ITransformer.targets()，拿到返回Set之后才遍历并调用TransformStore.addTransformer。Targets无@Mod限制，Class类型标签可加入任意**已归属验证**的辅助类；多个类用多个targetClass注册。
4. ModFile.getScanResult()在future存在时调用CompletableFuture.get，检查scanError后返回ClassData和AnnotationData。应逐文件调用，不把后台提交视为已完成。findResource读取该ModFile的原ClassFile，不使用Class.forName。
5. TransformStore按class名索引，ClassTransformer按每次类加载查询/执行变换；不会承诺类之间执行顺序。它还会执行其他服务的变换和插件处理，不能假设输入节点等于发现时的原节点。
6. FMLModContainer在GAME层按真实moduleName取得Module，对入口使用Class.forName(Module,String)；之后constructMod检查唯一public构造器并反射调用。Helper的GETSTATIC/INVOKESTATIC可在该构造期间触发其clinit，Consumer可在官方事件之后才加载。必须在所有这些路径前发布整组计划，不能等helper加载后补Target。
7. ModLoader.dispatchParallelTask实际使用CompletableFuture/Executor。任务offset33设置线程局部active container，finally正常/异常均清空。FMLModContainer自己的类加载阶段也临时设置并清理container。每容器官方BusBuilder创建独立ModBus；现有Context视图不是该原生Bus。

## 并发、归属和提前加载风险

计划绑定真实ModFile/ModInfo、moduleName、entry、原ClassFile SHA和必要引用闭包。不能依赖全局简单类名缓存：ModLauncher目标索引本身按名字，因此新增目标还要检测其他已扫描ModFile中的同名冲突。未知/native/平台/外来类不能被新规则认领。

生产可行路径是：只读扫描所有本组原字节→建立依赖边→逐规则验证→在副本试变换与字节码检查→整组成功后才返回Target。运行时每类先比对预验证输入并验证归属；发现其他变换器改变受控输入应拒绝，不能重用过期计划。计划发布后只读，避免以实际类加载顺序填充字段或跨线程半初始化缓存。

这只能证明**固定官方启动链**提供前置计划窗口，不能保证任意第三方TransformationService不会自行提前加载来源类。不得在targets/计划中反射定义来源类；正式运行需以本轮变换/构造/注册日志核对。其他服务冲突及提前加载不在一般兼容承诺内，应明确拒绝已观察的不一致。

## 原子性与实施边界

- 计划原子性：完整本组校验后再发布必要Targets；任一helper/consumer/BSM失败不发布部分新计划。
- 单类原子性：只修改克隆ClassNode，重新验证失败或dump失败保持调用者原节点。
- JVM定义原子性：**不提供**。已定义类不能因后续类失败自动回滚；整组预验证减少该风险，不能称为跨类JVM事务。

静态结论：官方链上的预验证窗口和辅助类Target机制**可行**，允许进入受控Fixture和最小生产计划实现。实际新Helper的FML动态证据、跨层运行时计划一致性、Bus与RegisterEvent、对象身份目前均NOT RUN，不能用这份时序取证替代。

C3既有Context调用语义及22负例必须保留。新Registry组只接受本轮独立验证的Item注册、Holder消费者和Bootstrap闭包；一般全局Bus/Tab仍不支持。原FnF只读适用性评估失败时不发布新的部分Registry计划、不尝试强行启动。C4A候选继续不启用，整体PARTIAL保持历史事实。
