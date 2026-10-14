# Convergence-006B Gate0 ABI与可行性

2026-10-14（Asia/Shanghai）。[E]实际执行，[S]静态核对，[H]尚未验证，[D]决定。
基线main `de4e54002c3b777fb392a4699fdbcbe2261965e0`，独立worktree DualForge-006B；生产尚未改动。

[E] 原生Forge52.1.0与纯vanilla MC1.21.1干净编译成功；Forgeclasspath无NeoForge/DualForge，vanilla class全量无patch owner。
保留javac/jar/javap/ASM；精确输入清单见fixture/result.json。两次构建：首轮driver纯度断言把查询cast误认成创建Block，原日志保留；修正仅检查实际NEW后重新干净编译并冻结。
最终fixture在 out/convergence-006b/fixture/20261014-011319-c33ae78f，源与各JAR/class/资源SHA见原result/contents/inputs。

|实际位置|Forge实际类型/调用|允许的Neo输出|
|---|---|---|
|BLOCKS/ITEMS来源|ForgeRegistries相应字段:IForgeRegistry|BuiltInRegistries.BLOCK/ITEM:DefaultedRegistry|
|两个create|(IForgeRegistry,String)DeferredRegister|(Registry,String)Neo DeferredRegister|
|两个register|(String,Supplier)RegistryObject|(String,Supplier)DeferredHolder|
|Block holder|RegistryObject<Block>|DeferredHolder<Block,Block>|
|BlockItem holder|RegistryObject<BlockItem>|DeferredHolder<Item,BlockItem>|
|两个bus挂接|(Forge IEventBus)V|(Neo IEventBus)V；参数local1保持|
|get-only两个accessor|RegistryObject.get:()Object，再cast Block/BlockItem|仅get owner到DeferredHolder|
|BlockItem supplier依赖|本类Block holder GETSTATIC→get()Object→castBlock→newBlockItem(Block,Item.Properties)|精确field descriptor/get owner映射；其余业务不改|

[E] Block supplier为marker+new Block(BlockBehaviour.Properties.of())；BlockItem supplier为marker+new BlockItem(指定BlockHolder.get(),new Item.Properties())。
两种lambda均标准metafactory，SAM()Object、同入口静态handle、instantiated()Block/()BlockItem；captured/altMetafactory真实编译样本单独取证，不作为支持形式。
同参数构造器实际先挂接Block DR再挂接Item DR，C/T只切换DF006B_REGISTER。字段/注册ID/方法名独立的matrix也原生编译，避免固定身份白名单。

[E] baseline使用本main干净构建 `out/convergence-006b/baseline-build/20261014-011036-a2b96ec3/dualforge.jar`。
首次实际失败在 `ControlledForgeItemProfile.onlyField → plan → validate → DualForgeEntryTransformer.targets`，stderr为 `005 unsupported Item profile: one DeferredRegister field`。
已确认一次真实Forge发现/Reader/FML metadata；尚无构造或两个Supplier标记，无transform dump，exit-before-Done。具体断言见baseline-analysis.json；原result OBSERVED不改写成新功能PASS。

[S] 从官方NeoForge21.1.256 sources实际读取GameData/DeferredRegister/DeferredHolder/RegisterEvent，与锁定真实字节码javap交叉核对。
GameData按vanilla注册顺序逐Registry post真实FML event，DR仅处理自己的key，event.register调用supplier后holder.bind(false)。真实目标Builtin静态BLOCK先于ITEM；动态两Supplier顺序/Block已注册且holder可用仍需Gate3实际观测，当前标记[H]。
GameData在事件失败时聚合错误并调用revertToVanilla后抛出；这是官方代码的静态事实，不承诺任意失败下的完整跨Registry业务事务。转换原子性必须独立证明：完整验证复制节点，任何非法结构不提交。
Forge52 sources原MavenHTTP403、Neo镜像HTTP404；未假称取得该源码。Forge侧ABI以实际锁定JAR/javap/编译输出为准，Neo侧生命周期源码与实际target bytecode匹配。

[D] Gate0具备可限定的转换范围，继续实现独立Block+BlockItem profile；不放宽006A Item规则，不改Loader/Reader/构造ABI/EventAPI。
仅支持一个普通Block和一个精确关联BlockItem，两typed DR、唯一holder、同modId namespace、标准零捕获bootstrap、已验证supplier体、两个local1挂接和get-only accessor。未知registry/API/捕获/跨类/额外入口严格拒绝。
Gate0不是006B-S动态PASS；BlockItem关联、两Registry顺序、整体ClassNode原子性将在Contract和真实C/T验证。
