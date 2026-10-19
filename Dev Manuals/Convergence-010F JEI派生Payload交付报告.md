# Convergence-010F JEI 派生 Payload 交付报告

本轮整体 **PARTIAL**。已实现并验证有限的继承式 Payload、复杂 Codec 和保留原 process 的安全业务闭包；两个 Forge-only 独立输入在 Forge/NeoForge 官方服务端注册与重启通过。原始 JEI 第一删除包的父类、构造、Type 和 ItemStack Codec 已越过 010E 的类型拒绝，完整计划继续在未经验证的权限业务调用处整组拒绝。没有替换权限结果、禁用 process 或制造 JEI 启动成功。

本报告区分静态来源、隔离组件、断开连接模型、真实 FML 注册和实际游戏业务。真实连接、Packet Delivery、玩家权限/背包/配方业务和图形客户端仍 NOT_RUN/BLOCKED。`010F_JEI_CLIENT_ZIP=NOT_PRODUCED`。

## 基线、原始输入和主线收敛

任务原文 SHA-256 `3850df861bb321ce7b86b51fecffcab3c0c02876e62aeb033270ff3663998b21`。开始时重新 fetch main 与 A/B/C/D/E 分支，并读取 GitHub PR #35–#39 实际状态、merge commit、merge-base、graph、contains 与 diff。main 实际为 `50c24fd4062b10d43aa926aa54c080435db63800`，包含 A，尚不包含 D 的生产与测试；#36–#38 的 MERGED 是合入前置分支，不能视作 main 已收敛。E #39 仍 OPEN/DRAFT，HEAD `5973f5c9fc6740ba1b571a58fa8b0d8b58e06546`。

F 独立 worktree `/home/albert/Codex/DualForge-010F`，分支 `codex/convergence-010f-jei-derived-payload`，从上述实际 E HEAD 建立。源码继续在 SMB；输出仅在 SSD `/home/albert/Env/DualForge/runs/convergence-010f/`。原根 worktree HEAD `2600fe85b0c85b3d79454db6a20a93d1a9219e7c` 和用户未跟踪的 008A Windows 报告保持不变；该文件 SHA `ba9a4006253f7bfbdcfee0a7c3632adaad5db7d4215b474f4a17186373b504bf`。A–E 冻结报告/索引逐文件保全，旧测试原件与旧运行目录未改。

独立主线收敛 Draft PR [#40](https://github.com/GFander/DualForge/pull/40)，base `main`，head `codex/convergence-010b-010d-main-integration`=`fd278190fe525e8ec3edd39ea064b0d3ec940969`。完整差异97路径，仅 main 缺少的 B/C/D；A 已是 main 祖先，不重复计算；E/F 不混入。`merge-tree --write-tree` 无冲突，结果树 `bfed5f04e051f065c29bf3fd431dee74a547b50b` 与 D 树相同。未重写任何历史提交。[实际 CI 39187526921](https://github.com/GFander/DualForge/actions/runs/39187526921) completed/success，核对 head=D，已关联当前任务，未合并。MAIN_INTEGRATION_STATUS=PARTIAL 指 Draft 已准备且 CI 通过、main 尚未实际合并；不将本轮 F 回归写成对 integration 分支新启动的游戏证据。

固定环境：Minecraft1.21.1、Forge52.1.0、NeoForge21.1.256、FML4.0.45、ModLauncher11.0.5、Microsoft JDK21.0.10+7-LTS。固定 JDK `/home/albert/Env/DualForge/jdks/msjdk-21.0.10`，SDK 位于原 SSD 目录；使用官方启动文件，不增加业务 JVM 参数或 Agent。全部本地 JVM 串行。

原始 JEI `/home/albert/Env/DualForge/runs/convergence-010a-jei/inputs/jei-1.21.1-forge-19.27.0.340.jar`，1,529,402 bytes，SHA `f5e61a001d57dd044d6f2ba255daf955c50d90e4173f8b4933ab0eb8cd386ed5`。未修改、重包、预转换。Neo 参考发行只读取证，不能替换 Forge 业务输入。其他固定模组指纹见安装清单和最终 cohort 审计，FnF 原始 SHA `bd415b04529c4ffc31e14757488f71dab6917e4560c61e236c3c4d0de6557958`。

## 原始六包的静态完整取证

`abi-current` 重新 SHA 核对两发行1846类/每发行923类，并记录50份 source javap 与199份固定 SDK 网络 ABI javap。`types-current` 对16个选定 source ClassFile 完整取证（包括六包、两父类、两 Context、Codec/enum/record/业务服务），12份双宿主 javap 绑定实际 JAR/成员 SHA。固定补丁 server JAR 在宿主 API 查找顺序最前，避免 slim/obfuscated ABI 误命中。

原始两父类 `PlayToServerPacket` / `PlayToClientPacket` 直接实现原生 `CustomPacketPayload`，子类通过父类实现；没有把每个包当作 final extends Object。父类声明 abstract type/streamCodec/process；子类具体 override 使用真实描述符。完整取证包括字段与静态字段、构造与父构造调用、访问标志、Record组件、Enum、Type/Codec声明、所有方法、Bridge/Synthetic、Bootstrap、clinit顺序与来源class SHA。JAR 来源静态归属和独立输入真实 Module/Loader 身份分开记录；原JEI GAME层运行身份未建立。

`process-complete/packet-process-closure.json` 从六个 process 出发收集145次来源方法访问、742条 call/Bootstrap handle/active clinit 边。Declared/Overridden 与父声明分别记录，Bridge/Synthetic 保留原标志。原始虚派发、接口/default方法的实际实现、宿主业务、权限和玩家身份明确 NOT_PROVED；静态图可达不等于业务准入。

| 原包 | 方向/父类 | 原始 process 静态调用与当前业务边界 |
|---|---|---|
| PacketDeletePlayerItem | SERVERBOUND / PlayToServerPacket | 原Context.player/config → hasPermissionForCheatMode → carried ItemStack检查/修改、日志及拒绝分支回包；第一未知权限调用即拒绝，后续未执行 |
| PacketGiveItemStack | SERVERBOUND / PlayToServerPacket | ServerCommandUtil.executeGive；给物、权限、背包与发送闭包未准入 |
| PacketRecipeTransfer | SERVERBOUND / PlayToServerPacket | 原玩家/容器、两个List stream映射 → BasicRecipeTransferHandlerServer.setItems；动态/配方与背包业务未准入 |
| PacketSetHotbarItemStack | SERVERBOUND / PlayToServerPacket | ServerCommandUtil.setHotbarSlot；额外Hotbar构造校验及原业务闭包未完整准入 |
| PacketRequestCheatPermission | SERVERBOUND / PlayToServerPacket | 原权限调用 → PacketCheatPermission构造 → sendPacketToClient；未准入 |
| PacketCheatPermission | CLIENTBOUND / PlayToClientPacket | ClientCheatPermissionHandler.handleHasCheatPermission；客户端玩家/生命周期/显示业务 NOT_RUN |

此表来自静态来源，不是上述权限、物品修改、配方或发送实际触发记录。完整原六类生产准入=PARTIAL。

## 有限生产规则与原字节保全

仅改5个生产 Java 路径：新增 `ForgeDerivedPayloadPlan`、`ForgeRichCodecPlan`，扩展既有 `ForgeDualBusPlan`、`ForgeNetworkClosurePlan`、`ForgeNetworkBoundaryProof`。复用原 Bus/配置/网络桥/Source Context/官方注册状态机，没有并行 Forge 网络运行时。生产代码无 jei、treealpha/treebeta 类名、modId、文件名或 SHA 白名单。

类型规则支持一层同 ModFile 的 public abstract Base→public具体Packet（可final或非final），Base extends Object并直接实现原生Payload；验证泛型自绑定、三个抽象方法、全部具体成员与访问约束、真实父构造、可选 private final int 父状态与 public final getter。子构造必须直接调用已证明的父无参构造并完整赋值每个私有final实例字段。Type/Codec getter须真实读取自己的 public static final 字段并有准确泛型绑定；不能凭名称假定解析结果。未知更深继承、父业务、接口/default方法、控制流或初始化继续拒绝。

独立业务输入24类仅8个转换 target；16个父类/包/Codec helper/Record/enum/Context/连接数据依赖保持原字节，由原业务 Module/Loader 加载。只读依赖拒绝残留 Forge ABI。整组来源 fingerprint 仍覆盖全部24类；原 Entry begin 重新读取来源验证；Foreign名称冲突也包括只读类。实际观察器验证每个只读class资源原字节和真实身份。transform 拒绝未认领类/只读类、错误SHA及已改变规范化节点；克隆转换不修改输入节点。旧 D/E Profile 的 targets 和严格规则保持。

完整组合为 Bus Identity → Constructor Segments → 原 Network/Global对象 → 所有 Payload 类型 → Codec → process/Context → 原 Consumer/Runnable → 官方注册。任何未知成员或一个失败定义导致整组无 targets；不是局部发布。已执行的构造/宿主副作用不可回滚时保留 UNCERTAIN 并拒绝重试，不声称 JVM 已定义类可回滚。

## Codec 与 process 分别验证

有限生产 Codec 允许1–6字段composite：VAR_INT、BOOL、STRING_UTF8、原生 ItemStack.STREAM_CODEC、已完整证明的 FriendlyByteBuf enum wrapper、纯 int-pair Record Codec 的 list，以及 singleton unit。逐项核对真实 owner/描述符、字段与构造绑定、getter顺序、buffer/泛型、LambdaMetafactory和MethodHandle的tag/捕获/擦除/实例化类型、Type/Codec初始化、全部 helper 方法。枚举两个 compiler Bridge/Synthetic 原样保留；Record ObjectMethods bootstrap及字段句柄完整匹配。未知Record额外业务/合成helper不因此自动准入。

真实服务端 registryAccess 创建 RegistryFriendlyByteBuf。独立输入覆盖负/边界整数、bool、UTF8、enum、空/非空Record list、非空带组件ItemStack、unit、读完全部字节及解码真实字段；截断、非法enum ordinal、null等错误输入保留实际异常。原JEI六个Codec隔离组件在两宿主每次各跑11个正例（1+2+3+2+1+2），包括Paper×7的CUSTOM_NAME、GiveMode、多Record配方数据；原热栏-1/MAX构造拒绝、Internal clinit组件也实际测试。组件使用隔离ClassLoader，不属于JEI在GAME层构造，更未调用原JEI process。

新增safe process保留原 `context.record(this, inheritedValue())`。原Context保留sender/config/两连接引用，记录payload、父值、次数、当前线程并调用真实已注册配置的原getter；Source Runnable先调用process再执行原网络计数/引用记录。通过原已注册Handler直调和实际服务器队列验证，不是替换process、提前return、伪造权限或背包行为。

首次 alpha/beta配置均true，停机后物理复制 world/config，核对每文件SHA，仅在新副本将alpha改false，重启原process实际读取alpha=false/beta=true，原首次证据不变。没有玩家，因此不声明玩家背包持久化。

Source Context 使用断开连接的官方Native Context/诊断listener，无在线玩家、无伪造玩家身份。模型实测入队、真实server线程执行、handled/异常契约和原引用；server sender仍null。通过临时将原Settings.enabled Supplier换成抛出同一sentinel，验证原process/config异常身份保留、后续Source网络计数不执行，再恢复原Supplier。Context先前局部记录不回滚，不写成回滚PASS。Neo原生guard按其既有行为故意记录ERROR并处理failedFuture，原日志保留；Source adapter异常语义与宿主原生guard区别有实测，不吞掉原业务异常。

原ServerPacketContext的纯Record结构仅静态可证明；原wrapper的真实玩家、客户端Context生命周期仍需证明。权限/背包/配方业务未实现即拒绝，PacketProcess/ServerContext为PARTIAL，ClientContext和PermissionBusiness为BLOCKED。发送、Channel查询、远端存在性、DistExecutor/ClientSafeRunner都未绕过。

## 独立输入、负例和实际运行

业务只用Forge SDK编译一次；同原JAR在两宿主使用：

| 输入 | 字节 / SHA-256 |
|---|---|
| treealpha | 25090 / `8ef25a4a75a9118ca722b691ae9d7e005701979bf296780edf1277a1a3688bce` |
| treebeta | 25014 / `98019702a2725ec33221071ce1110cfd50317ed475f0c5136adcfad5b8812bf3` |

两者分别有原Bus、配置、Global Connection和每Mod6定义（PLAY、optional、version3、3个双向数据形态对应server/client方向），总12个官方Payload定义，两个独立ModContainer/Module，无碰撞。观察器分别使用原生Forge/Neo API编译并required/AFTER排序，不将业务输入用Neo重编。

Gate6要求的全部34分类有独立证据，逐项见 `tests/real_mods/jei/derived_payload/MATRIX.md`。30类静态分类（包含原始真实权限调用拒绝）与4类实际owner模型（Module/Loader/Container/phase）分别取证；另外有3类泛型/只读Forge ABI拒绝。静态生产诊断部分沿用Cursor的opcode/call/MISSING_METHOD，分类与原始错误映射明确，不伪称34个同名生产错误码。模型检查拒绝输入字节不变；未认领/只读类transform拒绝、所有targets错SHA、全source fingerprint、前面合法计划后面JEI拒绝仍不发布targets分别验证。

以下均为本轮最终JAR实际运行；旧输入只按SHA复用，历史运行结果不继承：

| 验证组 | 本轮实际结果及证据层 |
|---|---|
| F完整继承/Codec/process计划 | 220断言；8 targets/16只读依赖；克隆、全source fingerprint及严格负例 PASS；静态模型 |
| F真实Reader/双Dist原子性 | CLIENT/DEDICATED_SERVER模型34断言 PASS；不是客户端启动 |
| F原生Forge first/restart | 2组PASS，每组观察器601、Context结束累计634断言；真实注册+断开连接模型 |
| F Neo first/restart | 2组PASS，每组观察器734、Context结束累计768断言；实际12定义、只读类/owner模型 |
| 原始JEI六Codec组件 | F各次运行均6个Codec/11正例（1+2+3+2+1+2）、真实registry buffer；热栏2拒绝；原process未调用 |
| D网络 | 136计划+34双Dist边界；4组真实first/restart PASS |
| B双Bus/配置 | 128计划；双方default/modified/persisted共6组+额外Neo owner组 PASS |
| C构造/Locale | 212计划+34双Dist；双方配置共6组 PASS |
| C原JEI ServerConfig组件 | 双方共6组PASS；不等价完整JEI模组构造 |
| E网络闭包 | 246计划+34双Dist；4组真实first/restart PASS；Forge观察器221/Context244、Neo256/280 |
| Mouse Tweaks | 101断言/52负例、owner15/7、Forge API53、Neo API65/7拒绝、728链接/6Mixin、21 no-init/8隔离初始化 PASS；GUI NOT_RUN |
| 旧契约/Probe/模组/混装 | 18组新服务端生命周期PASS；metadata/discovery/identity/module/ModFile、Registry/Context/Global/client-event相关模型和严格负例PASS |
| Clumps | 99静态断言/28负例PASS；原生/兼容/观察/物理XP球/重载/混装实际对照PASS |
| FnF | 67 Item/67 Holder、66 Tab、58 FOOD、真实燃料和正负熔炉观察PASS；玩家食用/GUI/背包持久化NOT_RUN |
| Helper CI本地 | 10字节码断言；FnF expectation 3正/12负 PASS |
| 本地静态 | 106 Python单测、25 zsh语法、374冻结保护、新增Python内存语法 PASS |
| 原始JEI单因素 | 测量PASS_EXPECTED_REFUSAL，服务端生命周期FAIL；新权限首断点如下 |

选定最终cohort共50次实际服务端调用：49个生命周期PASS，1个原JEI预期FAIL。`final-cohort-audit.json`核对全部安装mods与最终JAR字节，同一Forge-only输入各在4次F运行原SHA一致；每次inputsUnchanged=true。对照空服/控制组PASS不代表目标模组功能PASS。

C专服Locale诊断保留客户端类Dist拒绝日志；E/F Neo原生guard模型故意记录异步异常，所有stdout/stderr/severityLines保留。生命周期PASS不是“日志没有ERROR”或客户端可用的声明。

## 最终 JAR 下的原始 JEI 真实首断点

仅安装最终DualForge与固定原始Forge JEI两个mod，官方NeoForge专服启动；返回exit1、EXITED_BEFORE_DONE、inputsUnchanged=true。精确stderr首行：

```text
Exception in thread "main" java.lang.IllegalStateException: Dual bus rejected: UNVERIFIED_NETWORK_INITIALIZATION UNVERIFIED_PERMISSION_CHECK source=mezz/jei/common/network/packets/PacketDeletePlayerItem.process(Lmezz/jei/common/network/ServerPacketContext;)V call=mezz/jei/common/util/ServerCommandUtil.hasPermissionForCheatMode(Lnet/minecraft/world/entity/player/Player;Lmezz/jei/common/config/IServerConfig;)Z derivedType=STATIC_VERIFIED codec=STATIC_VERIFIED originalProcess=RETAINED globalConnection=STATIC_VERIFIED originalSetter=RETAINED sourceClinit=STATIC_VERIFIED context=FINITE_ADD_PROFILE_ONLY combined=REJECTED_NO_TARGETS
```

实际阶段收据（STATIC_VERIFIED_IN_LIVE_PREPLAN表示真实启动期间的静态证明，不是该业务方法运行PASS）：

| 阶段 | 实测层/状态 |
|---|---|
| Discovery | REAL_RUNTIME_PASS |
| Metadata | REAL_RUNTIME_PASS |
| EntrySelection | REAL_RUNTIME_PASS_DUAL_BUS |
| LocaleInitializerPlan | STATIC_VERIFIED_IN_LIVE_PREPLAN |
| BusIdentityPlan | STATIC_VERIFIED_IN_LIVE_PREPLAN |
| EventSemanticPlan | BLOCKED_NO_EXPLICIT_SUBSCRIPTIONS |
| ConfigPlan | STATIC_VERIFIED_IN_LIVE_PREPLAN |
| GlobalConnectionPrefix | STATIC_VERIFIED_IN_LIVE_PREPLAN_ORIGINAL_SETTER_RETAINED |
| SourceClinit | STATIC_VERIFIED_IN_LIVE_PREPLAN |
| SourceContextPlan | FINITE_ADD_PROFILE_ONLY_ORIGINAL_DERIVED_HANDLER_NOT_APPROVED |
| DerivedPayloadTypePlan | STATIC_VERIFIED_FIRST_PACKET_ONLY |
| CodecPlan | STATIC_VERIFIED_FIRST_ITEMSTACK_PACKET_ONLY |
| HandlerPlan | BLOCKED_UNVERIFIED_PERMISSION_CHECK |
| PayloadDefinitionPlan | BLOCKED_INCOMPLETE_BUSINESS_CLOSURE |
| ChannelOperationPlan | NOT_IMPLEMENTED |
| NetworkPlan | BLOCKED_FULL_ORIGINAL_CLOSURE |
| DistBoundary | NOT_REACHED |
| CombinedConstructorPlan | REAL_RUNTIME_FAIL |
| Transformation | NOT_RUN |
| ClassDefinition | NOT_RUN |
| ModConstruction | NOT_RUN |
| Lifecycle | NOT_RUN |

这仍是生产targets预计划拒绝，不是PacketDeletePlayerItem类运行时异常或权限方法返回false。第一包继承/构造/ItemStack Codec静态证明与整个JEI网络闭包批准是不同结论。其它包Hotbar额外构造、CheatPermission重载、原TransferOperation额外业务/合成helper等尚需逐段证明，不能因组件Codec往返通过就整体放行。

原JEI Minecraft范围 `[1.21,1.21.1)` 保留原字节；本次targets拒绝在后续FML依赖验证之前发生，不能据此说该范围或后续依赖已验证通过。Channel发送/远端存在性、Dist、插件与GUI后续没有执行，静态图不能预测为“下一实际异常”。

## 制品、归档与 Git 交付

最终制品 `/home/albert/Env/DualForge/runs/convergence-010f/build-6/dualforge.jar`，305239 bytes，SHA-256 `aa961e7f6cee4b63a84506e6ea288e577d432230bc16627590ef33e163183b51`。40 Java +5资源共45项生产输入，全指纹 `e793b26a3612012d8e3dc6f0ab3f8d151b873890dfdd77b4c77be83710c92c47`。本地运行先绑定未提交源码指纹，随后逐文件与代码commit blob核对。build-6之后生产没有再变；不能将旧候选PASS直接计入最终制品。

最终选定目录仅 `contract-verified`、`boundary-verified`、`live-verified`、`remaining-verified`、`regressions-verified`。含final名称但JAR SHA不同的 `contract-final` / `live-final`、先前build失败和其它试跑均为中间证据，全部保留，不混入最终PASS。预期拒绝测试测量PASS与原JEI生命周期FAIL分别记录。

`retain_evidence.py` 仅在无Java时回收F新建且每文件SHA与canonical完整SDK相同的server/libraries重复副本；保留mods/world/config、全部日志/命令/退出码/安装清单/恢复receipt、完整固定SDK和所有旧阶段。归档内恢复receipt绑定外部canonical SDK原字节，没有删旧证据或原输入。

完整证据 `/home/albert/Env/DualForge/runs/convergence-010f/Convergence-010F-Evidence.zip`，100,984,934 bytes，6715成员，SHA `7b15a269a233add6071a41f77d8b24b366efb8769ca33cda73d1ce279076527e`。清单 `/home/albert/Env/DualForge/runs/convergence-010f/evidence-manifest.json`，SHA `54ae2dd5d900368f273bdc69298f71a8d26787b16ab2f8906c90c61608d2c031`。ZIP CRC与每成员SHA全部核验，包含全部保留的中间/失败试跑、原始日志/世界/mods及91项代码快照；外部canonical SDK由恢复清单绑定。归档在最终文档/CI之前冻结；后续GitHub最终核验独立保存于同SSD目录，不伪称已包含于此前ZIP。

代码提交 `0f1f429efd7e00ace1fb89a8c65d8d4e7ba6aa2a` 已推送且45项生产输入匹配提交blob。Draft PR [#41](https://github.com/GFander/DualForge/pull/41)，base实际E `codex/convergence-010e-jei-network-closure`=`5973f5c9fc6740ba1b571a58fa8b0d8b58e06546`，差异仅F，已由应用工具成功关联。报告/索引独立文档提交。最终文档HEAD、远端SHA、PR状态/base/diff与实际CI在 `/home/albert/Env/DualForge/runs/convergence-010f/delivery-final.json`；该外部收据绑定报告/索引SHA，避免将文件自身提交SHA写进自身。CI对最终文档HEAD实际dispatch并核验完成，不以排队状态冒充通过。本轮两个PR均不合并、不强推、不发布Release。

## 结果字段

字段PASS限于上述有限形态和实际证据层。COMPOSITE_CODEC_SUPPORT/TRANSFER_OPERATION_CODEC标PARTIAL：独立受控形态与原组件通过，原JEI六包完整生产闭包未批准。客户端与报文收发未继承旧Windows或历史PASS。

```text
010F_BRANCH=codex/convergence-010f-jei-derived-payload
010F_HEAD_SHA=见SSD delivery-final.json（最终文档HEAD；代码commit=0f1f429efd7e00ace1fb89a8c65d8d4e7ba6aa2a）
010F_PR_URL=https://github.com/GFander/DualForge/pull/41
010F_PR_BASE=codex/convergence-010e-jei-network-closure
010F_PR_STATUS=PARTIAL (OPEN / DRAFT; not merged)
MAIN_INTEGRATION_STATUS=PARTIAL (Draft #40 prepared, CI success; main not merged)
DERIVED_PAYLOAD_HIERARCHY=PASS
SUPERCLASS_CONSTRUCTOR_CONTRACT=PASS
INHERITED_METHOD_RESOLUTION=PASS
CUSTOM_PACKET_PAYLOAD_IDENTITY=PASS
ORIGINAL_SIX_PACKET_TYPES=PARTIAL
COMPOSITE_CODEC_SUPPORT=PARTIAL
ITEMSTACK_CODEC=PASS
ENUM_CODEC=PASS
TRANSFER_OPERATION_CODEC=PARTIAL
RECORD_CODEC=PASS
LIST_CODEC=PASS
PACKET_PROCESS_CONTRACT=PARTIAL
SERVER_PACKET_CONTEXT=PARTIAL
CLIENT_PACKET_CONTEXT=BLOCKED
PERMISSION_BUSINESS_CONTRACT=BLOCKED
INDEPENDENT_FORGE_RUNTIME=PASS
INDEPENDENT_NEO_RUNTIME=PASS
REAL_CONNECTION=NOT_RUN
REAL_PACKET_DELIVERY=NOT_RUN
JEI_NETWORK_CLOSURE=BLOCKED
JEI_NEO_CURRENT_FIRST_FAILURE=BLOCKED (UNVERIFIED_NETWORK_INITIALIZATION / UNVERIFIED_PERMISSION_CHECK; PacketDeletePlayerItem.process → ServerCommandUtil.hasPermissionForCheatMode; REJECTED_NO_TARGETS)
JEI_COMPATIBILITY=PARTIAL
FNF_REGRESSION=PASS
CLUMPS_REGRESSION=PASS
MOUSE_TWEAKS_REGRESSION=PASS
DUAL_BUS_REGRESSION=PASS
CONSTRUCTOR_REGRESSION=PASS
NETWORK_010D_REGRESSION=PASS
NETWORK_010E_REGRESSION=PASS
FINAL_DUALFORGE_SHA256=aa961e7f6cee4b63a84506e6ea288e577d432230bc16627590ef33e163183b51
CLIENT_ZIP_STATUS=NOT_RUN (010F_JEI_CLIENT_ZIP=NOT_PRODUCED)
REMAINING_BLOCKERS=BLOCKED (original six full type/constructor/helper/process/permission/player/context closure; Channel send/query/remote presence; Dist/plugins/client; real connection/delivery)
PUSH_STATUS=PASS (final docs remote/CI verification recorded in delivery-final.json)
MERGE_STATUS=NOT_RUN (neither #40 nor #41 merged)
```

后续所需契约是：逐包证明原权限服务及真实玩家来源、容器/背包/配方数据与线程契约；原Context/wrapper与复杂构造/helper；Channel存在性和真实发送/投递；Dist/客户端插件与图形界面。它们保持拒绝或NOT_RUN，不要求用户提前运行已知无法完整启动的Windows包。
