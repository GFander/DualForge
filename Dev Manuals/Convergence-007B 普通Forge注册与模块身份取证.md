# Convergence-007B：普通Forge注册与模块身份取证

日期2026-10-14（Asia/Shanghai）。独立子任务依赖007A PR #14；基线main `ac4c4d7ef50ea35a634925d30dead0b93d798400`，007A HEAD `2ca2d20d8182fc41c08f9545eaed995be74d8121`。不把007A默认策略算作本PR新增API。

## 原生输入与两个实际失败点

新输入 `com.example.dropin.DropInItems`、modId dropinprobe，仅以Forge52.1.0/eventbus6.2.27/javafml及未打NeoForge补丁的Minecraft1.21.1编译。`forge-compile-inputs.json`逐依赖SHA；vanilla JAR全部class检查没有Forge/NeoForge注入，原输入class没有NeoForge/DualForge owner或业务属性。
冻结来源提交 `3a32d06`；最终Forge JAR 4,867字节，SHA **`01acf5a450d9c23faa8cb42befbe164476f7a2ca320230289bf1bd40917db30f`**。之后修订的只是独立NeoForge查询观察器，构建选项核对Forge src/resources指纹并复用原JAR；没有重建它迎合Transformer或修改Manifest。

一个DeferredRegister<Item>声明 ice_ether（普通Lambda）、raw_ice_ether（本类private static零参数method reference），构造器无条件register注入的Mod Bus。工厂只new Item(new Item.Properties())，无marker/DF00*属性。

| 结构 | 原005/006A profile | 新原生Forge实际输出 |
|---|---|---|
| constructor descriptor | `(Lnet/minecraftforge/eventbus/api/IEventBus;)V` | 相同，ABI未扩展 |
| constructor opcode | 26条，打印/布尔属性/分支/单挂接 | 6条，Object初始化→owned GETSTATIC→ALOAD1→register→RETURN |
| Supplier implementation | 10条：marker打印+普通Item创建 | 7条：普通Item创建与RETURN |
| invokedynamic | LambdaMetafactory.metafactory、零捕获、同类静态Handle | 相同实际BSM/参数形式，lambda与命名工厂各一 |
| initializer | 已验证Item registry/create/register/字段赋值的数据依赖 | 同一调用族与typed字段，独立两声明 |
| fields/getters | Forge RegistryObject<Item>与get()Object | 相同已验证描述符与get-only方法 |
| JAR module identity | 冻结输入声明名与modId一致 | 无Automatic-Module-Name；默认dropin.items，metadata要求dropinprobe |

逐指令javap、BootstrapMethods、ASM节点、source/class/JAR SHA及命令在 `out/convergence-007b/fixture/20261014-093945-80d4a401/`。

**失败1（007A cf944…）：** discovery→Reader→真实FML metadata已识别dropinprobe；`ControlledForgeItemProfile.validateSupplier`首先拒绝没有marker的 `lambda$static$0`。原始失败stdout/stderr/退出及参数在 `/workspace/DualForge-007/out/convergence-007/normal-before-extension/`。不依据启动失败猜根因。

**失败2（初步普通Profile ca79…）：** 正确选择normal-items，但入口转换尚未发生。`FMLModContainer.<init>:58` 调用gameLayer.findModule(info.getOwningFile().moduleName())为空，NoSuchElementException；实际metadata名dropinprobe与原通用JarMetadata名dropin.items不一致。原字节Forge JAR不变，失败保存在 `out/convergence-007b/stock/normal-only/`。

官方FML4.0.45源直接给出 `ModFileInfo.moduleName()` = 首个真实modId；官方 `ModJarMetadata`使用该值、实际version/packages/services生成automatic descriptor。这是独立正常加载子任务新增的必要取证，不是Gate0之前悄悄改变模块政策。

## 最小实现及映射边界

普通Profile与原marker/分支Profile分开选择；只有真实Forge @Mod扫描归属的入口可能成为目标。六条constructor仅是候选筛选，完整验证仍逐字段、API、Supplier Handle、注册ID和数据依赖完成；不是只把固定字段数增加或为dropinprobe加白名单。
旧Profile的constructor分支、marker工厂、get-only及全部拒绝条件保留。Normal模式只接受取证的无条件单总线挂接与纯Item工厂；不修改工厂业务字节、BSM或Mod Bus。

| 已验证Forge调用/字段 | 运行时目标 |
|---|---|
| ForgeRegistries.ITEMS : IForgeRegistry | BuiltInRegistries.ITEM : DefaultedRegistry |
| DeferredRegister.create(IForgeRegistry,String) | NeoForge create(Registry,String) |
| register(String,Supplier):RegistryObject | NeoForge register(String,Supplier):DeferredHolder |
| RegistryObject<Item>字段/引用 | DeferredHolder<Item,Item>字段/引用 |
| register(Forge IEventBus)与原constructor | 对应NeoForge IEventBus，参数来源仍ALOAD1 |
| RegistryObject.get():Object | DeferredHolder.get():Object，原cast/对象逻辑保留 |

所有修改在已复制ClassNode完整验证后执行；只使用Plan记录的实际API与字段引用，不全局替换Forge namespace。Supplier实现及BSM全字节相同；反转精确映射后全类原字节相等。

必要模块修复位于 `ForgeJarMetadata.bind` 与Reader创建后bind的3行plumbing：候选识别guard、优先级和Parser/依赖语义不变。匹配名仍返回原delegate descriptor；**只有准入Forge automatic名不匹配**时调用官方ModJarMetadata并绑定实际IModFile。显式JPMS名不匹配明确拒绝，不能抛弃原requires/exports/uses语义来取得成功。
不新建Layer、不修改NeoForge/JAR输入、不修改javafml/9999或内置委托、不创建Parallel Loader。

## 本轮直接验证

NormalItemBytecodeTest实际Reader/FML扫描：两个Supplier/两个Holder/单挂接的精确sites；不同modId/包/入口ASM对照；普通无注解类、非扫描归属及原生metadata不改；显式rewrite=false不改。
13个目标拒绝：重复/非法ID、错误Registry、未知BSM、捕获、错误Bus、额外普通/Forge API、重复挂接、不同constructor ABI、错误Holder泛型、错误metadata归属及映射后dump I/O失败。每项before/caller-after class字节相等。
另一个实际显式module-info JAR负例被Reader明确拒绝，JAR前后字节不变；matching历史006A descriptor相等。模块名before/after、字节、断言及13+1结果在 `out/convergence-007b/contract/20261014-104153-a1df52ae/`。

最终生产source commit `4b99638439cdee3bd4394d91fa2478212076b717`，固定测试提交 `3c50a5b8e14fd2b581efffee191ba193f753f0c1`。最终JAR SHA **`dbb70bfa48ccd299c9a0bc74fc8fe07f645fce370a2131264cfdc44e5d235a36`**，67,568字节。
标准无参数normal-only、查询观察器、混合原生NeoForge实际Registry/Holder/对象身份与Done/stop/exit0已通过，字段不互相覆盖；没有把查询观察器当用户运行依赖。

此结果只覆盖取证的public final入口、原单IEventBus ABI、同类纯Item工厂/typed字段/get-only访问。无参/多参数、捕获/任意业务Supplier、一般Properties builder、普通无条件Block profile、显式JPMS重命名或一般第三方API不支持。Simple TOML和52准入策略不升级为所有Forge分类；Provider顺序不作任意宿主保证。
