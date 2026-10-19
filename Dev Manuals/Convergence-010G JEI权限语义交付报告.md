# Convergence-010G JEI 权限语义交付报告

整体 **PARTIAL**。原始JEI权限谓词、辅助方法、配置角色和两宿主原生API已取证；新增有限权限只读证明及专服调用来源约束。原权限代码直接运行，不新增权限值映射或通用Forge权限Shim。原删除包的实际新首断点是权限之后的玩家库存读取，完整计划继续`REJECTED_NO_TARGETS`。

原函数组件、实际FML注册、普通Player模型、真实ServerPlayer与实际投递分开验收。本轮没有创建ServerPlayer、建立真实连接或执行原删除包；有效真实玩家身份路径、实际库存和反馈发送、客户端仍NOT_RUN/BLOCKED。`010G_JEI_CLIENT_ZIP=NOT_PRODUCED`。

## Git、环境与固定输入

任务原文SHA `f852e4d6587474a8485bd286c5bf7636b455ac1b2c68f1270872a8b349f24539`。重新fetch main/D/E/F/integration及GitHub实际PR关系：main=`50c24fd4062b10d43aa926aa54c080435db63800`，尚未包含F。#40（BCD→main）、#39（E→D）、#41（F→E）仍OPEN/DRAFT，均未合并；实际前置CI分别核验head与completed/success。#40已有97路径主线收敛Draft，本轮不重复创建，不调整前置PR base、不合并、不强推。

新分支 `codex/convergence-010g-jei-permission`，worktree `/home/albert/Codex/DualForge-010G`，从最新F `a8f288f34ac0e33546fa7ffe008899ee8a0a539f`建立；G PR base为F分支。原根worktree HEAD=`2600fe85b0c85b3d79454db6a20a93d1a9219e7c`不变，用户未跟踪008A Windows报告SHA=`ba9a4006253f7bfbdcfee0a7c3632adaad5db7d4215b474f4a17186373b504bf`不变。A–F冻结报告/索引、旧测试原件和旧运行均保全。

固定MC1.21.1、Forge52.1.0、NeoForge21.1.256、FML4.0.45、ModLauncher11.0.5、Microsoft JDK21.0.10+7-LTS。SDK/JDK留在原SSD路径；源码保留SMB。官方启动文件、无业务JVM参数/Agent，全部本地JVM串行。新世界、mod、构建、日志、命令和完整证据均在 `/home/albert/Env/DualForge/runs/convergence-010g/`。DISPLAY/WAYLAND/Xvfb事实见environment收据，GUI未运行。

原始JEI仍是1,529,402 bytes的Forge发行，SHA `f5e61a001d57dd044d6f2ba255daf955c50d90e4173f8b4933ab0eb8cd386ed5`；路径 `/home/albert/Env/DualForge/runs/convergence-010a-jei/inputs/jei-1.21.1-forge-19.27.0.340.jar`。未修改、重包或预转换。F制品SHA `aa961e7f6cee4b63a84506e6ea288e577d432230bc16627590ef33e163183b51`仅为前置事实，本轮动态结果重新绑定G制品。

## 原始权限ABI与精确谓词

`collect_abi.py`重新校验原发行923个ClassFile SHA，保存6个真实来源类完整javap/图（ServerCommandUtil、IServerConfig、ServerPacketContext、删除包、权限响应包、Forge NetworkHandler）。最终`abi-final`记录双宿主22类/44份native ClassFile与javap、JAR/成员SHA，覆盖Entity继承方法、Player/ServerPlayer、服务器/OP数据、CommandSourceStack、Brigadier、GiveCommand、连接和GameMode。早先缺少继承/连接依赖的采集保留为中间证据，最终使用补齐后集合。

原方法为public static `hasPermissionForCheatMode(Player,IServerConfig):boolean`，描述符 `(Lnet/minecraft/world/entity/player/Player;Lmezz/jei/common/config/IServerConfig;)Z`。完整可达权限子图为主方法、getGiveCommand及两个private static synthetic lambda，另验证Logger-only clinit。Utility其它给物/库存方法未因该子图获准。

| 顺序 | 原始实际条件、返回与数据流 |
|---|---|
| 1 | 读取isCheatModeEnabledForCreative；true且player.isCreative时立即true |
| 2 | 否则先调用player.createCommandSourceStack；不是等到某个配置为true才创建 |
| 3 | 读取isCheatModeEnabledForOp；true且player.getServer非null时，用该server.getOperatorUserPermissionLevel调用stack.hasPermission，立即返回其true或false，false不回退give |
| 4 | 前面未返回才读取isCheatModeEnabledForGive；true时Optional.ofNullable(player.getServer)→当前Commands/dispatcher/root.getChild("give")→真实CommandNode.canUse(stack)；缺失server/节点为false |
| 5 | 其余false；没有默认授予、客户端权限声明或异常转true |

原函数不自行验证连接、ModContainer、Side或主线程，也不显式检查ConfigSpec是否初始化。null配置在首getter处抛异常；null玩家会在对应原调用处抛异常，本轮实际组件验证。三个源配置getter各自异常身份经原函数/独立函数/原safe process保留。未加载ConfigSpec的实际getter行为未单独运行，不能用null代理测试替代该门禁。

原creative早返回在查询server前，故隔离组件中的“creative+模拟null-server”会保留原true行为；这不是获准网络调用的授权成功。生产权限Profile必须先通过真实专服/玩家/连接来源guard，未知server或普通Player模型不能进入该回调。身份、服务器权威与权限布尔结果是独立条件。

## 双宿主语义、玩家与配置

`semantic-comparison`实际比较11项选定native方法：Entity.getServer/createCommandSourceStack、ServerPlayer.getPermissionLevel/isCreative、MinecraftServer.getProfilePermissions、DedicatedServer.getOperatorUserPermissionLevel、CommandSourceStack.hasPermission、PlayerList.isOp、ServerOpListEntry.getLevel、CommandNode.canUse/getChild。规范化仅去除常量池编号，保留成员、指令及分支目标，11项均相同；不把局部指令相等当作所有动态玩家状态已验证。

ServerPlayer原生权限等级来自当前服务器getProfilePermissions，包含OP列表/条目及单人相关分支；专服OP阈值来自实际DedicatedServerProperties。原命令节点执行当前requirement Predicate。当前测试世界op-permission-level=4，原GiveCommand注册requirement为hasPermission(2)，这些值没有写入生产兼容决策。不是以相似API名称、固定OP等级或相同单个返回值认定等价。

生产准入的G来源数据流仍为实际Forge Context.getSender→原Context构造→原config对象→enqueueWork→原process。对新的有限Profile，Source Context在进入与排队执行前检查真实Host Class/Module、DedicatedServer loop、ServerPlayer、GameListener、双方player/connection引用、连接有效性、同一服务器及实际主线程。没有保存跨请求权限结果；原getter和命令节点每次求值。Integrated Server/未知子类/客户端方向不准入。

配置对象通过既有FML owner/构造/注册链绑定当前mod，网络config与Entry.settings必须为同一对象；静态三个getter和Supplier来源完整核对，G仅允许SERVER配置。两个mod的当前配置可独立变化，未把另一mod的结果当授权依据。排队过程中重查的是身份与线程，原权限函数保持原始顺序和异常。

真实ServerPlayer从已连接会话到OP列表、有效guard正向路径、连接关闭/玩家切换后的实际报文回调均NOT_RUN。普通Player组件覆盖了谓词输入，但其level由模型提供，不能替代真实ServerPlayer.getPermissionLevel运行证据。PLAYER_IDENTITY/SERVER_AUTHORITY/PERMISSION_SEMANTIC保持PARTIAL。

## 原删除process的全部静态分支

原public `process(ServerPacketContext):void`字节码完整保存。首先取得Context.player/config并调用原权限方法，offset15的IFEQ进入拒绝分支；权限允许路径下一项offset19是`ServerPlayer.containerMenu`读取，也是本次真实新预计划门禁。

| 原静态分支 | 原业务及本轮状态 |
|---|---|
| 允许、服务器carried与请求ItemStack的getItem引用相同 | setCarried(ItemStack.EMPTY)；未执行，库存修改BLOCKED |
| 允许、Item不同且服务器carried非空 | 原LOGGER.warn，包含玩家与两Stack显示名；未执行 |
| 允许、Item不同且服务器carried为空 | 无该warn，结束允许路径；未执行 |
| 拒绝 | 仍读取服务器carried并LOGGER.error；构造PacketCheatPermission(false,config)，原IConnectionToClient.sendPacketToClient响应；未执行/发送BLOCKED |

该process操作当前服务端containerMenu的carried，未携带客户端slot index。原比较仅getItem引用，不显式比较数量/components；本轮不改写其原算法。原字节码没有单独调用Inventory.setItem或显式broadcastChanges，不能据此宣布背包/客户端同步通过。空输入/非法状态的完整库存与同步行为未实际验证。权限拒绝也有库存读取和回包依赖，不能用空Handler替代该分支。原Packet、拒绝日志及响应调用均保留；只批准权限子契约，不放行任一库存路径。

## 生产实现与原子性

仅6个生产路径：新增ForgePermissionPlan，扩展ForgeDerivedPayloadPlan、ForgeDualBusPlan、ForgeNetworkClosurePlan、ForgeNetworkBridge、ForgeSourceContextBridge。生产无JEI/夹具modId、包名、文件名或发行SHA白名单；13项固定Neo专服host ClassFile SHA是宿主ABI约束。

权限组件证明真实JVM描述符、完整控制流/局部变量、三个唯一配置角色、Optional和两种精确Lambda bootstrap、辅助方法与clinit。获准新增业务仅是原process→Context.record→原permission→允许/拒绝计数；未知库存/response/额外副作用整组拒绝。Source utilities不转换；既有桥接、Bus/Owner、注册阶段、Source Context和原enqueue算法复用，没有第二套权限运行时。

独立输入22类中仅8个targets，14个只读依赖包含utility。全22类fingerprint、Source Entry begin重读、foreign名称冲突、每target SHA/规范化/克隆与只读transform拒绝均验证。只读类原Module资源与来源字节逐项核对，不宣称任意其它Transformer或已定义JVM类可回滚。

G允许3个全serverbound定义；旧E/F仍要求并保留原双方向结构，旧路径不启用G guard。源Runnable完整保留原process，guard只在新权限Profile前置重查身份。原异常传播/failed future不改写成许可或成功。模型中的count在异常前已增加则保留，protectedCalls/deniedCalls未执行，不虚构局部副作用回滚。

## 独立输入与实际验证

Forge-only输入每个仅编译一次、同SHA跨宿主使用：gatealpha 21739 bytes，SHA `3bf028c5fecb9ec28e623059ec33999e741bcc856ee2715cdcddb22dff589148`；gatebeta 21677 bytes，SHA `211b7894d728386a76b9efc79c1a568be9c9f1597b6918d3bcada392852bef7a`。各3个官方serverbound定义，总6个；原Bus、配置、Global Connection、Type/Codec/Consumer及Module分别隔离。

四次真实Forge/Neo first/restart均PASS，正常Done/保存/退出。每次以同一个实际服务器及当前已注册own-config，比较原始未改JEI函数、独立函数与原safe process计数：8配置×2creative×5模型native-stack等级×2模拟server引用×2mod=320组合，110允许/210拒绝。Forge每次观察器1694断言，Neo1797；这些包含真实注册、原Module资源、Codec、普通Player组件与owner/guard反例，不是1797项真实玩家授权。

ModelPlayer是观察器内的未连接普通Player，未创建ServerPlayer、未加入真实玩家列表、未成为原生getSender结果。其createCommandSourceStack/native命令判断实际执行，但level输入由模型选择。新guard实际拒绝该模型及null，避免把组件对象当有效玩家。原JEI函数在隔离URLClassLoader加载、原接口通过组件代理委托当前own-config；不等于JEI GAME层构造或原Config实例完整执行。

Permission False仅增加deniedCalls，不增加protectedCalls；true执行安全protected counter，均实际调用原process。missing give节点使原Optional路径false，原树随后恢复；null配置/玩家保留原异常；三个当前配置Supplier分别抛sentinel后原异常根身份保留，既有count不回滚，配置原引用恢复。临时配置与命令树修改只在本轮新测试世界，未执行删除/给予物品。

首启alpha/beta的creative=true/op=true/give=false；复制停机world/config后只在新副本将alpha creative改false，重启读取alpha=false/beta=true，原首启文件不变。整个矩阵使用当前配置，不静默缓存旧结果。不声明玩家背包存档持久化。

29类要求的独立拒绝全部覆盖，逐项输入变异与原诊断见MATRIX和negative-audit。静态模型与实际FML owner下的Module/Loader/Container、服务器loop、普通/缺失玩家与非主线程guard分开。部分报错沿用原Cursor领域名称，未统一用一个错误替代。Missing Permission Source、ClientClaims替代、固定host变更、已证明片段后未知副作用都拒绝；原输入字节不变。

本轮最终G JAR实际重跑，不继承前置阶段动态结果：

| 验证组 | 本轮实测 |
|---|---|
| G权限完整计划/负例 | 176断言；两个来源、8 targets/14只读依赖、只读utility完整SHA、29类必须拒绝及额外fail-open反例PASS |
| G native API对照 | 11项选定方法指令相同；静态比较PASS，动态真实玩家授权NOT_RUN |
| G Forge/Neo注册与重启 | 4组PASS；每组320权限组件案例；Forge1694/Neo1797观察器断言；有效真实连接路径NOT_RUN |
| B双Bus/配置 | 128计划；6组default/modified/persisted+1 Neo owner组PASS |
| C构造/Locale | 212计划+34双Dist原子性，6组实际配置对照PASS |
| C原JEI ServerConfig | 6组原配置组件PASS，完整JEI mod构造另行BLOCKED |
| D网络 | 136计划+34双Dist，4组实际first/restart PASS |
| E网络对象/Context | 246计划+34双Dist，4组first/restart PASS，原enqueue异常/future模型PASS |
| F派生Payload/Codec/process | 220计划+34双Dist，4组first/restart PASS；原process保留、parent/type/codec/only-read资源、原JEI六Codec组件PASS；真实连接NOT_RUN |
| Mouse Tweaks | 101断言/52负例、owner15/7、原生API Forge53/Neo65、728链接/6Mixin、21 no-init/8隔离初始化PASS；GUI/Mixin实际应用NOT_RUN |
| 旧契约/模组/混装 | 18组实际生命周期PASS，覆盖Probe、metadata/discovery/module/ModFile、Registry/Context/Global和对应严格负例、FnF/Clumps/混装 |
| Clumps | 99静态断言/28负例PASS；原生/Neo经验球、独立来源、重载、混装对照实际PASS |
| FnF | 67 Item/67 Holder、66 Tab、58 FOOD、真实燃料/正负熔炉观察PASS；玩家食用/GUI/背包持久化NOT_RUN |
| Helper/本地 | 10字节码断言、FnF expectation 3正/12负，106 Python、25 zsh、374冻结保护、12个G Python内存语法PASS |
| 原JEI单因素 | 测量PASS_EXPECTED_REFUSAL，游戏生命周期FAIL；首失败如下 |

总计54次实际服务端调用，53个生命周期PASS、1个原JEI预期FAIL。最终安装164份mods和30份DualForge副本逐文件SHA核验，全部inputsUnchanged=true；两个新Forge-only输入各在4组运行SHA相同。控制空服的生命周期PASS不等于业务PASS。

旧E/F的原JEI首失败预期在G适配副本中从已证明的权限拒绝迁移到库存门禁；冻结E/F源文件不改，其余旧负例保留。真正未知权限方法仍有独立原发行字节变异，继续UNVERIFIED_PERMISSION_CHECK。C专服Locale/客户端Dist拒绝日志、E/F Neo原生guard故意记录的异步错误均保留，不把有ERROR的隔离模型说成完整GUI/无错误游戏证据。

## 原始JEI真实单因素首失败

仅安装最终DualForge与原始JEI两个必要mods，官方NeoForge专服启动：exit1、EXITED_BEFORE_DONE、inputsUnchanged=true。精确首行：

```text
Exception in thread "main" java.lang.IllegalStateException: Dual bus rejected: UNVERIFIED_NETWORK_INITIALIZATION UNVERIFIED_PLAYER_INVENTORY source=mezz/jei/common/network/packets/PacketDeletePlayerItem.process next=net/minecraft/server/level/ServerPlayer.containerMenuLnet/minecraft/world/inventory/AbstractContainerMenu; permission=STATIC_VERIFIED originalPermission=RETAINED playerIdentity=STATIC_DATAFLOW_ONLY originalProcess=RETAINED process=BLOCKED combined=REJECTED_NO_TARGETS globalConnection=STATIC_VERIFIED originalSetter=RETAINED sourceClinit=STATIC_VERIFIED context=FINITE_ADD_PROFILE_ONLY combined=REJECTED_NO_TARGETS
```

| 阶段 | 实际证据层/状态 |
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
| PermissionPlan | STATIC_VERIFIED_ORIGINAL_NATIVE_PREDICATE_RETAINED |
| PlayerIdentity | STATIC_DATAFLOW_ONLY_REAL_CONNECTED_PLAYER_NOT_RUN |
| HandlerPlan | BLOCKED_UNVERIFIED_PLAYER_INVENTORY |
| PayloadDefinitionPlan | BLOCKED_INCOMPLETE_BUSINESS_CLOSURE |
| ChannelOperationPlan | NOT_IMPLEMENTED |
| NetworkPlan | BLOCKED_FULL_ORIGINAL_CLOSURE |
| DistBoundary | NOT_REACHED |
| CombinedConstructorPlan | REAL_RUNTIME_FAIL |
| Transformation | NOT_RUN |
| ClassDefinition | NOT_RUN |
| ModConstruction | NOT_RUN |
| Lifecycle | NOT_RUN |

`permission=STATIC_VERIFIED`描述真实启动期间的只读预计划证明，权限方法此时没有被GAME层执行。第一次静态未批准访问是containerMenu，尚不是setCarried的实际异常；原权限拒绝/允许业务均未执行。隔离组件调用过原permission函数是另一证据层，不能覆盖这项NOT_RUN。

原始MC依赖范围`[1.21,1.21.1)`保持；targets拒绝发生在后续FML依赖验证之前，不声明依赖门禁已通过。后续库存、响应、其它Payload、Channel、Dist/ClientSafeRunner/插件与GUI尚未进入，未将静态预测写成下一实际异常。

## 架构审查、制品与Git

已完成独立《Convergence-010G 架构可复用性审查》：复用契约、编译器形状/模组结构假设、共享验证、余留耦合和未来重构分别记录。当前大Plan职责与Cursor错误分类仍有耦合，G通过专用组件控制增长；没有在本轮做扩大兼容范围的泛化或广泛重构。

最终JAR `/home/albert/Env/DualForge/runs/convergence-010g/build-3/dualforge.jar`，315254 bytes，SHA `f98f7658f433ff2761830931bce89a8368c55cfefff44413378d154558437e23`。41 Java+5资源共46项，全指纹 `17a8118510bced1bb1083d7bf8e83df13206d32a52a0430c6d88de19635558ae`。本地先记录未提交源码指纹，随后逐文件绑定代码commit，build-3之后生产未变。

最终选定contract-verified、live-1、remaining-verified、regressions-verified。live-1虽名称为1，四次均已独立核对同最终JAR与46项生产输入，属于本轮实际验证。build-1编译失败、contract-1错误诊断预期失败、build-2与contract-2/3等中间证据全部保留，不把其PASS额外累计到最终cohort。

完整证据 `/home/albert/Env/DualForge/runs/convergence-010g/Convergence-010G-Evidence.zip`，90,796,100 bytes，5425成员，SHA `79c1945fe117db583d0127488588bedc58dd646b9afce6c6da8c3869fe81d1b6`；manifest SHA `b36b3f7f28066828609294346580f521015bc436295bfc831c19fd511c0a70aa`。ZIP CRC和每成员SHA全部核验，包含全部保留试跑、失败日志、原始world/mods/config/命令及92项代码快照。只回收本G新建、停机且逐文件SHA与完整canonical SDK相同的重复server/libraries；外部完整SDK、所有旧阶段和完整恢复清单保留。归档在最终文档/CI前冻结，后续GitHub交付收据单独保存，不伪称已包含在ZIP。

代码commit `521c649f212937867af8708ae058bb2f88ebdd29` 已推送，46项生产输入逐文件匹配其blob。Draft PR [#42](https://github.com/GFander/DualForge/pull/42)，base实际F `codex/convergence-010f-jei-derived-payload`=`a8f288f34ac0e33546fa7ffe008899ee8a0a539f`；差异仅G。报告、索引和架构审查另作文档提交。最终文档HEAD、远端SHA、实际PR base/diff、完成CI和各文档SHA以 `/home/albert/Env/DualForge/runs/convergence-010g/delivery-final.json` 为权威记录，避免文档自身提交SHA自指。CI绑定最终文档HEAD实际dispatch并核验，不以排队或仅创建PR代替success。未经授权不合并、不强推、不发布Release。

## 最终字段与限制

JEI_PERMISSION_SUBPLAN=PASS仅限原始静态谓词、固定宿主API与原函数组件，不等于真实玩家身份/权限、原删除业务或完整网络PASS。有效connected guard路径、玩家OP/连接变化、未加载ConfigSpec、库存和反馈实际业务、客户端保持NOT_RUN/BLOCKED。未来需要合法真实测试连接及相应独立业务契约，不要求用户提前运行已知无法完整启动的Windows包。

```text
010G_BRANCH=codex/convergence-010g-jei-permission
010G_HEAD_SHA=见SSD delivery-final.json（最终文档HEAD；代码commit=521c649f212937867af8708ae058bb2f88ebdd29）
010G_PR_URL=https://github.com/GFander/DualForge/pull/42
010G_PR_BASE=codex/convergence-010f-jei-derived-payload
010G_PR_STATUS=PARTIAL (OPEN / DRAFT; not merged)
MAIN_INTEGRATION_STATUS=PARTIAL (#40 Draft prepared and CI success; main not merged)
ORIGINAL_PERMISSION_ABI=PASS
PERMISSION_SEMANTIC_CONTRACT=PARTIAL
PLAYER_IDENTITY_CONTRACT=PARTIAL
SERVER_AUTHORITY_CONTRACT=PARTIAL
CONFIG_PERMISSION_BINDING=PASS
PERMISSION_NEGATIVE_PATH=PASS
PERMISSION_POSITIVE_PATH=PARTIAL
DELETE_PACKET_PROCESS=BLOCKED
INVENTORY_MUTATION_CONTRACT=BLOCKED
PACKET_RESPONSE_CONTRACT=BLOCKED
INDEPENDENT_FORGE_RUNTIME=PASS
INDEPENDENT_NEO_RUNTIME=PASS
REAL_SERVER_PLAYER=NOT_RUN
REAL_CONNECTED_PLAYER=NOT_RUN
REAL_PACKET_DELIVERY=NOT_RUN
JEI_PERMISSION_SUBPLAN=PASS
JEI_NEO_CURRENT_FIRST_FAILURE=BLOCKED (UNVERIFIED_NETWORK_INITIALIZATION / UNVERIFIED_PLAYER_INVENTORY; PacketDeletePlayerItem.process → ServerPlayer.containerMenu read; permission=STATIC_VERIFIED; REJECTED_NO_TARGETS)
JEI_COMPATIBILITY=PARTIAL
FNF_REGRESSION=PASS
CLUMPS_REGRESSION=PASS
MOUSE_TWEAKS_REGRESSION=PASS
DUAL_BUS_REGRESSION=PASS
CONSTRUCTOR_REGRESSION=PASS
NETWORK_010D_REGRESSION=PASS
NETWORK_010E_REGRESSION=PASS
DERIVED_PAYLOAD_010F_REGRESSION=PASS
ARCHITECTURE_REVIEW=PASS
FINAL_DUALFORGE_SHA256=f98f7658f433ff2761830931bce89a8368c55cfefff44413378d154558437e23
CLIENT_ZIP_STATUS=NOT_RUN (010G_JEI_CLIENT_ZIP=NOT_PRODUCED)
REMAINING_BLOCKERS=BLOCKED (valid real connected-player guard/OP state/config initialization; original inventory/response/full network; Channel/Dist/plugins/client; real packet delivery)
PUSH_STATUS=PASS (final docs remote/actual CI checked in delivery-final.json)
MERGE_STATUS=NOT_RUN (#39/#40/#41/#42 not merged)
```
