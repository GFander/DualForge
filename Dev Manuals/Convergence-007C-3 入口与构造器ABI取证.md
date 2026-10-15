# Convergence-007C-3 入口与构造器ABI取证

Gate0–2在生产零修改时完成。起点5c11344f3a4f1ab3e6c478716e208d493bc1dad9；PR20未合并，因此分支codex/convergence-007c3-entry-context以007C-2为stacked base。原发行FnF 128098字节，SHA bd415b04529c4ffc31e14757488f71dab6917e4560c61e236c3c4d0de6557958，CRC及全部成员SHA保全。本轮新基线JAR cb16c3907cb78dc184a3a213f4ae4a5300986759538569e78b923892c34664c5；标准双JAR实跑再次平台VERIFIED，随后targets→ControlledForgeItemProfile.plan→class structure拒绝，生命周期FAIL/exit1，不存在FnF loadMod。

实际发行javap -v/-s/-c与全部ASM ClassNode输出见out/convergence-007c3/preflight/abi；没有用公开源码替代。FoodNFuel access33/public非final、Object父类、唯一Forge @Mod(value=fnf)，两字段String常量与Logger。唯一public构造器()V，maxStack2/maxLocals2：Object初始化；FMLJavaModLoadingContext.get()；getModEventBus()；ASTORE1；ALOAD1分别交ItemRegistries.register和FnFCreativeModeTabs.register；最后全局MinecraftForge.EVENT_BUS.register(this)。入口没有BootstrapMethods；两个helper有各自工厂/Tab的invokedynamic，完整原BSM/Handle/javap均保存。原局部1始终为Forge IEventBus。

两个真实helper目标均为同JAR未标@Mod的静态方法，descriptor `(Lnet/minecraftforge/eventbus/api/IEventBus;)V`；体内GETSTATIC本类DeferredRegister，ALOAD0，INVOKEVIRTUAL Forge DeferredRegister.register(同ForgeBus)V，RETURN。Registry初始化、Supplier、Tab行为位于各自未适配的类中，不由入口Context ABI自动支持。

## 实际宿主FML4.0.45

对应官方安装loader JAR与官方source archive，实际javap包含FMLJavaModLanguageProvider、FMLModContainer、ModContainer、ModLoadingContext、ModLoader；提取source逐方法核对，没有借用较新版本。

- provider将metadata entry列表和GAME ModuleLayer交给官方FMLModContainer；容器从该layer按metadata moduleName取得模块，Class.forName(Module,entry)。当前LanguageLoader直接复用此容器，不创建私有Layer或自行实例化模组。
- 每个FMLModContainer构造时独立BusBuilder.build，marker为IModBusEvent，allowPerPhasePost；getEventBus返回该字段。不是全局NeoForge.EVENT_BUS。
- constructMod要求恰好一个public构造器。参数精确允许NeoForge IEventBus、ModContainer、FMLModContainer、Dist，每种最多一次；零参数自然按newInstance(empty Object[])实例化。官方支持多类型不等于DualForge本轮支持所有来源ABI。
- ModLoader.dispatchParallelTask在每个模组任务前setActiveContainer，finally清空，constructMod及FMLConstructModEvent都在此范围。ModLoadingContext为ThreadLocal；无active时getActiveContainer会回退minecraft。不能仅凭此方法有返回值就认定归属。
- 容器最初Class.forName时也临时设置active。无参入口的真实构造在GAME模块、并行任务所属线程中执行。ThreadLocal不代表允许任意异步线程、跨模组重入、静态缓存或事件阶段获取Context；宿主finally清空也不是保存/恢复嵌套栈。

## Forge52.1.0来源ABI

真实javafmllanguage/fmlcore52.1.0字节码：Forge FMLJavaModLoadingContext.get()先取Forge ModLoadingContext.get().extension()并cast；extension由active container的contextExtension提供。getModEventBus直接返回其FMLModContainer的Forge Bus。旧容器优先查单FMLJavaModLoadingContext构造器，否则无参。此事实不改变当前DualForge原单IEventBus支持边界。

| 来源ABI | 宿主实际ABI | 受控桥接候选 |
|---|---|---|
| entry `<init>()V` | 官方支持无参public ctor | 保留无参，由官方容器实例化 |
| Forge context `get()L...FMLJavaModLoadingContext;` | ThreadLocal ModLoadingContext + 已分配FMLModContainer | 新独立命名空间桥接，核对实际caller/metadata/module/active归属 |
| `getModEventBus()L...Forge IEventBus;` | container.getEventBus返回NeoForge IEventBus | 返回来源ABI的只读关联视图，内部delegate必须是该容器真实Bus；不提供新的Forge Bus操作 |
| helper `(Forge IEventBus)V` | 尚无对应跨类Registry适配 | 保留原描述符/调用；不把Context通过说成helper或Registry通过 |

本轮尚未在此文件中预写生产成功；实际fixture/运行及独立Bus身份以交付和证据索引为准。原生FnF基线PARTIAL和客户端NOT RUN继续保持。

生产修改前补充：新ContextProbe/ContextPeer原Forge源码干净编译，原Forge52.1.0空宿主及两输入+仅读观察器已实际Done/stop/exit0；各构造marker一次，捕获Bus与各真实Forge容器Bus逐对象相同、两者独立且不是全局Bus。与同轮空Forge错误计数一致。冻结源/class/JAR SHA见tests/entry_context/fixture-identity.json。首次同包JAR的split-package打包错误记录保留，修复仅限新peer独立包；未修改宿主模块策略。
