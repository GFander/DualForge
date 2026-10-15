# Convergence-007C-3 交付报告

日期：2026-10-15（Asia/Shanghai）。本轮只处理入口Profile分类和严格构造期Context ABI。**受控子目标PASS；原始Food N' Fuel整体仍FAIL，完整构造器未返回。** 未进入跨类Registry/SRG/Food/Fuel/Tab生产实现。

## Git、输入与最终源码

开始fetch并核实PR20未合并，起点及stacked base为`5c11344f3a4f1ab3e6c478716e208d493bc1dad9`，base分支`codex/convergence-007c2-forge-dependency`。新独立分支`codex/convergence-007c3-entry-context`，PR [#21](https://github.com/GFander/DualForge/pull/21)。未自行合并前置任务、强推、改写历史、删除分支或创建Release。007C-2平台投影是前置，不计本轮新增。

先行取证/方案/契约及原生输入冻结提交`419ad7d`，入口实现`5065451`，候选检测补正和本轮最终被测生产源码 **`76346e81dc03fb5ef3faf57c83c3450dd58fc9e2`**。最终源码从新的干净detached worktree构建；最终文档提交的精确HEAD及CI由PR正文和git-finalization.json/交付回复记录，不把生产源码提交冒充文档HEAD。

原发行FnF：128098字节，SHA-256 **`bd415b04529c4ffc31e14757488f71dab6917e4560c61e236c3c4d0de6557958`**，CRC、全部成员和运行前后SHA保全；未重编/重包/预转换。最终兼容层98308字节，SHA-256 **`ccdb80f50c2b511108c1df2e20232d379556d7b1d6d82fcc73f2f9c4eb05865f`**。构建dualforge.jar与交付副本DualForge.jar相同。所有最终Context、平台、metadata、历史测试只采用此JAR。

实际环境MC1.21.1、NeoForge21.1.256、FML4.0.45、ModLauncher11.0.5、Microsoft JDK21.0.10+7-LTS；原生Forge52.1.0为独立安装。Python/zsh/OS版本、实际编译classpath和每轮Java输出见preflight/environment与build记录，未用历史工具链报告代替。

## Gate0：真实失败与发行ABI

生产零修改时新基线JAR SHA `cb16c3907cb78dc184a3a213f4ae4a5300986759538569e78b923892c34664c5`。标准双JAR实际再次平台VERIFIED，然后targets→旧ControlledForgeItemProfile.plan→class structure拒绝；原始生命周期FAIL/exit1保留。

[ABI取证](Convergence-007C-3%20入口与构造器ABI取证.md)以真实发行javap -v/-s/-c和ASM节点为准。FoodNFuel为public非final/Object、单@Mod、唯一`<init>()V`；Object构造后唯一context get/getModEventBus，ASTORE1，再交两个helper，最后原全局EVENT_BUS.register(this)。maxStack2/maxLocals2，入口无BSM；所有helper BSM、SRG调用与字段在原字节转储中保留，没有把公开源码当发行字节码。

宿主实际FML4.0.45源码和javap确认：恰好一个public ctor，允许零参数或唯一类型的NeoForge IEventBus/ModContainer/FMLModContainer/Dist；由官方GAME模块内FMLModContainer反射实例化。每容器独立BusBuilder。ModLoader的并行任务设置ThreadLocal active container并在finally清空；不存在active时回退minecraft，不能据此直接授予Context。Forge52.1.0旧context通过active extension取得其容器Bus。静态契约与本轮动态无参/原单Bus验证分开，不宣称一般多参数、异步或重入支持。

## 方案和实现

[A/B/C比较](Convergence-007C-3%20方案比较.md)先于生产修改：A把NeoForge Bus直接注入仍接收Forge Bus的helper会留下接口/descriptor边界，不能只改签名；B保留官方无参构造和来源Bus ABI，采用独立命名空间shim；C是无法验证时的严格拒绝。选择B，不注入宿主/Forge包同名Context，不自建Layer/ModContainer，不增加参数、覆盖或新的事件能力。

生产范围：

- 新ForgeEntryProfile进行BLOCK/NORMAL_ITEM/LEGACY_ITEM/EVENT/CONTEXT独立候选检测；未知UNSUPPORTED、冲突AMBIGUOUS，绝不catch旧验证失败后尝试新Profile。Item候选必须有实际Registry字段证据；旧Profile精确验证不放宽。
- 新ControlledForgeContextProfile验证唯一public无参ctor、真实@Mod owner、唯一直接Context来源、受控栈/local/元数据与副作用。字段名/包/类/modId不白名单。两个helper只读核对同JAR扫描及实际单ForgeBus→void方法/直接DeferredRegister委托；Registry静态初始化、Supplier、Tab不在其兼容证明内，也不加入targets。
- 仅把两个已验证Context MethodInsn的owner及get返回类型映射到ForgeConstructorContext；原无参描述符、Forge IEventBus、局部/帧、helper及全局Bus调用原样保留。ClassNode先克隆，完整plan验证后才改；dump失败也不污染调用者。精确逆映射后class全字节相等。
- shim核对真实caller为该owner的入口构造方法、metadata/module、DualForge loader及active FMLModContainer；getter再次核对owner/caller/active一致。无全局Bus或额外ThreadLocal缓存，不借用minecraft fallback/native容器。
- 返回来源ABI只读关联视图，private delegate是该官方容器的真实Mod Bus，owner是同一容器。**视图对象本身不是原生Bus对象**，不是完整Forge EventBus实现；旧空IEventBus保持原字节，不新增register/post/listener等方法。Bus身份PASS指逐对象核对delegate/owner，不冒充`view == nativeBus`或新事件回调能力。

旧Item/Block验证器、LanguageLoader、EmptyRegistration、Forge Bus空壳、Parser/Reader/Locator/平台投影、Module策略和资源未改。最终JAR逐成员核对仅既有Transformer变化、8个新相关class，其余成员原字节相等。事件分支保留原逻辑，其结果由原始004严格契约和C/T复核。

## 实际结果与未通过候选

| 阶段 | 本轮证据 |
|---|---|
| 原生Forge Context | 原源码干净编译，先原Forge空宿主和双输入+独立仅读观察器正常启动/停止；两构造marker各一次，各Bus等于真实容器Bus、彼此独立、非全局。Native/Neo使用同一冻结JAR |
| 新Context Contract | 80断言/22拒绝负例PASS；两来源、额外ctor、静态形态/返回类型、owner、未知副作用、local/stack、MethodHandle/indy、外来/错descriptor/第二helper、JPMS、API关闭、dump后失败与原子性 |
| 旧边界保护 | 旧Normal非final仍class structure拒绝，无Context兜底；原007B13负例+显式JPMS严格PASS；旧004拒绝listener仍在原预期位置拒绝 |
| Context标准启动 | 空宿主、纯compat+原contextprobe、双Context+原生观察器/native-bootstrap三组PASS；原无参/非final保留，真实GAME层，delegate/owner逐对象核对；native构造借用Context明确拒绝 |
| 平台/metadata | 最终同JAR平台91、metadata93断言PASS；19组N0/N1/N2/N3、原生共存和required/optional/side/ordering矩阵PASS；平台和普通依赖严格断言保留 |
| Probe/002、003a/003b、004/005、006A/006B、007A | 最终同一JAR全部PASS；002 metadata/CDEF、003a发现/边界、003b身份/范围、004原Contract/C/T/拒绝、005单Item/原子性/assets、006A/006B Contract/C/T、原Probe及007A Bootstrap；24份阶段结果独立绑定最终SHA。 |
| 静态检查 | 44 Python单测、25 zsh语法、全部Python AST、374项冻结保护、完整diff均PASS；最终报告/索引加入后再核对命名与diff。 |

有两项失败/NOT RUN不能改写：首次两个新fixture JAR同包，原Forge拒绝split-package，冻结前仅调整新peer的独立包并重新干净编译；首次最终候选`5065451`/SHA `6f78402f…f65f837`在原004拒绝夹具上误选Item导致全历史回归FAIL。已通过新增Registry字段candidate前置修复，旧测试和validator不改；该候选及原失败全保留，最终结果全部来自`76346e8`的新worktree、新JAR、新运行，绝不混合。另一次手填不存在制品路径的Contract预检NOT RUN保留，之后从build-result读取真实路径执行。

旧007C-2矩阵默认仍为item-profile断点；本轮显式`--fnf-next-stage registry-helper`，要求平台VERIFIED、Context分类/两调用改写/绑定、真实FnF ctor栈和精确ForgeRegistries缺类。仅预期推进改变，其他依赖、cohort及原生隔离断言不动。历史回归保留原C/T开关和云端网络/relay配置；不能把开发harness参数说成用户零配置方式。

所有JVM严格串行，FML自身按官方生命周期执行任务。标准组实际/proc命令为官方argfile，无DualForge业务属性/Agent/额外模块参数。健康组逐项对照同轮N0的严重日志类型/计数；首次server.properties和Mojang公钥网络ERROR为本轮空宿主也出现的基线，**不宣称严格零ERROR**。负例和FnF的ERROR/FATAL/crash/原始退出状态保留。

## FnF下一真实失败

最终标准N2仅DualForge.jar与原FnF：平台VERIFIED、selected=CONTEXT、必要两调用映射、loadMod/官方容器构造、Context绑定均出现。get/getModEventBus链已经执行并返回来源ABI视图，随后第一次helper调用触发类初始化：

```text
java.lang.NoClassDefFoundError: net/minecraftforge/registries/ForgeRegistries
  net.natevg.fnf.item.ItemRegistries.<clinit>(ItemRegistries.java:12)
  net.natevg.fnf.FoodNFuel.<init>(FoodNFuel.java:25)
  Constructor.newInstance
  net.neoforged.fml.javafmlmod.FMLModContainer.constructMod(:122)
  net.neoforged.fml.ModLoader.lambda$constructMods$3(:153)
  net.neoforged.fml.ModLoader.lambda$dispatchParallelTask$7(:231)
Caused by: ClassNotFoundException: net.minecraftforge.registries.ForgeRegistries
```

两个helper仍是`register(Lnet/minecraftforge/eventbus/api/IEventBus;)V`；来源视图实现原有来源接口，因此没有把NeoForge Bus直接塞入旧参数，也未伪装成已转换helper。第二个Tab helper和原全局register没有运行成功证据。完整ctor仍FAIL，未注册67 Item，未转换跨类Registry、SRG或提供Food/Fuel/Tab。宿主可能exit0但原始生命周期FAIL，不能解释为模组成功。

## 证据、保全与重跑

[逐文件证据索引](Convergence-007C-3%20证据索引.json)区分原生门禁、开发运行、失败候选、最终同JAR与静态检查。任务根`/workspace/DualForge-007C3/out/convergence-007c3/`，失败候选`/workspace/DualForge-007C3-validation/out/`，最终验证根`/workspace/DualForge-007C3-validation-r2/out/`。构建/每阶段command、invocation、环境、JVM/proc、stdout/stderr/debug、exit、assertions、输入和源码/JAR指纹均保存。

重跑见 [entry_context](../tests/entry_context/README.md) 与 [dependency](../tests/real_mods/food_n_fuel/dependency/README.md)；完整本轮调度源及精确命令在final-driver/history-driver。只用最终同一个JAR，不重建替代历史Probe/fixtures。源码快照和ClassNode前后class保留，用于两个调用点精确逆映射和失败原子性；FnF二进制来源为原发行JAR，SHA逐项列出，不冒充新编Forge输入。

保全PASS：起点648 tracked文件的变化仅授权范围；007C-2全部3202项原始证据重新逐大小/SHA核对，本轮1063项外部输入记录核对；Context冻结源码/class/JAR与最终生产源码均匹配。两处历史可变source引用沿用已查明的旧提交说明，未重写原SHA。旧世界/输入不变，所有运行使用物理副本；仅清理本轮已退出且逐JAR匹配原来源的运行库副本，保留清理收据、日志、结果、世界、命令及dump。最终JVM为零、25565可绑定。

选择性原证据ZIP `/workspace/DualForge-007C3/out/convergence-007c3/evidence-export/convergence-007c3-selected-evidence.zip`，8692892字节，SHA-256 `d28ec11673f9debd36183cb0fa4fd1cc3713ad22209a3b6df1d22178320d162f`，4771个载荷加MANIFEST/README/SHA256SUMS三文件。ZIP CRC、逐载荷SHA、新临时目录完整解压复核、计数及无symlink/路径穿越均PASS，归档前后原文件不变。包含失败候选并明确分类，不是只有PASS的裁剪记录；排除世界、第三方依赖/JAR、配置/缓存及旧工作区全集，不能称完整原工作区副本。安全模式扫描无凭据/私网地址命中，绝对工作区路径保留，公开前仍需内容审查。**ARCHIVE_READY / NOT PERSISTED**，没有远端上传/回读或持久远端链接声明。解压复核及最终Git/CI记录在归档后独立保存，不伪称它们已在ZIP内。

## 最终状态

```text
ENTRY_PROFILE_CLASSIFICATION: PASS
NEOFORGE_CONSTRUCTOR_ABI: VERIFIED
FORGE_CONTEXT_BRIDGE: PASS (strict constructor-only source-ABI reference view)
MOD_EVENT_BUS_IDENTITY: PASS (actual delegate/owner, not view==nativeBus)
CONTROL_FIXTURE_NATIVE_FORGE: PASS
CONTROL_FIXTURE_NEOFORGE: PASS
FOOD_N_FUEL_TARGETS: PASS
FOOD_N_FUEL_CONSTRUCTOR: FAIL (entered; Context returned; helper initialization aborted)
FOOD_N_FUEL_NEXT_FAILURE: ItemRegistries.<clinit> -> missing ForgeRegistries
HISTORICAL_REGRESSION: PASS
CONVERGENCE_007C_3: PASS (controlled entry/Context subgoal)

FOOD_N_FUEL_RUNTIME: FAIL
NATIVE_FORGE_BASELINE_FNF: PARTIAL (inherited, not rerun)
CLIENT / FOOD / FUEL / TAB / PERSISTENCE: NOT RUN
```

完成后停止；未自动合并PR、创建Release、改写历史或开始下一阶段跨类Registry实现。
