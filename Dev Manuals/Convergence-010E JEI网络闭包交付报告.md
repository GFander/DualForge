# Convergence-010E JEI 网络闭包交付报告

本轮整体 **PARTIAL**。实现并验证了独立来源的真实全局连接对象、有限 Forge add/Context 及 2–16 个 Payload 定义组合。原始 JEI 的连接前缀、原 setter 与类初始化已推进到可静态证明，完整网络闭包仍 **BLOCKED**。其派生 Payload、原业务包装、客户端 Context、发送及远端查询没有获得生产准入。

本报告的 PASS 分别标出静态证明、隔离组件、直接 Handler 模型和真实 FML 注册。原 JEI 游戏层构造及网络运行没有执行，所有新 JAR 客户端 GUI、真实玩家身份、连接和报文投递仍 **NOT_RUN**。`010E_JEI_CLIENT_ZIP=NOT_PRODUCED`。

## 基线与保全

任务附件 SHA-256：`6b8fdede5cf25cb6ec493878a830a35d9a9747b2e02f3a94d24ed101b7ec7a4f`。本轮从实际远端 D HEAD `fd278190fe525e8ec3edd39ea064b0d3ec940969` 创建独立 worktree `/home/albert/Codex/DualForge-010E`，分支 `codex/convergence-010e-jei-network-closure`。源码在 SMB；完整运行记录在 SSD `/home/albert/Env/DualForge/runs/convergence-010e/`。

重新 fetch 后 main 为 `50c24fd4062b10d43aa926aa54c080435db63800`。实际 PR #35–#38 均显示已合并，但 #36、#37、#38 分别合入其前置任务分支，D 尚不是 main 祖先。因此 E 的正确 base 仍是 `codex/convergence-010d-jei-network`，不是将 B/C/D 再次算作 E 新增。实际 PR JSON 保存在 `intake/pr-35-actual.json` 至 `pr-38-actual.json`；祖先判断在 `intake/baseline.json`。

原 main worktree HEAD `2600fe85b0c85b3d79454db6a20a93d1a9219e7c` 没有改动。用户未跟踪的 `Convergence-008A-Windows客户端人工验收报告.md` 保留，SHA `ba9a4006253f7bfbdcfee0a7c3632adaad5db7d4215b474f4a17186373b504bf`。A/B/C/D 报告、索引与原发行输入按初始指纹核验，不改写历史结论。D 最终 JAR `2b37abbdd1d7a531480c427cc69a893ee3f790a428e8ddb9f4fb668ed24c745f` 是历史输入，本轮测试使用 E 新 JAR。

固定环境：MC1.21.1，Forge52.1.0，NeoForge21.1.256/FML4.0.45，ModLauncher11.0.5，Microsoft JDK21.0.10+7-LTS。所有 JVM 串行，使用官方启动文件；无业务 JVM 参数、Agent 或发行 JAR 修改。当前没有 DISPLAY、WAYLAND_DISPLAY 或 Xvfb，未建立可靠图形客户端。

## 原始发行图与对象闭包

原 Forge JEI：`jei-1.21.1-forge-19.27.0.340.jar`，1,529,402 bytes，SHA `f5e61a001d57dd044d6f2ba255daf955c50d90e4173f8b4933ab0eb8cd386ed5`。官方 NeoForge JEI SHA `8aaf547432f1b4958239b036356b910692fe40f858c3073d996f56bbf7c99826`，仅作只读 ABI 对照。重新校验 923+923 个来源 class 的冻结 ASM 行，保存完整 1,846 行图；不是从子串搜索推断完整动态可达性。重新 javap 50 个来源节点、全部固定 SDK network 类，以及新增 Internal/Features、TransferOperation、Connection/队列/Codec ABI。详见 `abi-current/`、`context-abi/`。

原 NetworkHandler 的参数为 IServerConfig、ResourceLocation、int。原始执行顺序：保存 serverConfig → new ConnectionToServer(this) → connectionToServer 字段 → Internal.setServerConnection → new ConnectionToClient(this) → connectionToClient 字段 → builder/version/optional/play → 五个 serverbound add、一个 clientbound add → 保存 Channel。每条赋值、方法描述符与原顺序均来自原发行字节码。

Internal setter 的真实内容是 ALOAD0、PUTSTATIC 私有 `serverConnection:IConnectionToServer`、RETURN，不允许 NoOp。Internal clinit 只创建 JeiFeatures 保存私有 static final 字段；其默认布尔 true、disable 写 false，IJeiFeatures 只有一个抽象 disable 方法。其他引用字段初始 null。ConnectionToServer 构造只保存 NetworkHandler，静态缓存 UUID=null、value=false；这些只是初始化取证，缓存刷新业务并未批准。

新的独立闭包保留来源 Globals 的原 PUTSTATIC 和可选真实写入计数。连接对象、Network、配置、原 getter 和 Features 留在来源 Module，桥接不建立共享连接单例。Session 绑定实际 ModContainer、ModFile、Module、ClassLoader、完整来源 SHA/规范化节点，构造开始保存私有来源 anchor。

状态：CONSTRUCTOR_STARTED → GLOBAL_SETTER_ENTERED → GLOBAL_REFERENCE_ASSIGNED → NAMED/VERSION/OPTIONAL/PAYLOAD/PLAY → 有序方向定义 → PLANNED → 官方事件 REGISTERING → REGISTERED。异常标记 UNCERTAIN，不清空来源静态引用。来源 setter guard 检查所属 Module、原连接类型、父 Network 身份、来源字段和当前构造栈；setter 返回后再次读取真实字段确认同一对象。

构造 catch 的转换字节码通过验证并在正常真实构造中加载；未动态注入整个来源构造器故障。另在官方注册事件中，用隔离 Session 触发宿主真实重复注册异常，确认 UNCERTAIN、拒绝重试、原静态引用/计数保持。它是 **ISOLATED_HOST_OPERATION MODEL_PASS**，不能冒充整个构造链故障或可回滚证明。各 Module 的来源公开 getter 仍有原访问语义；跨模组写入/错误 owner 拒绝，未声称提供恶意模组安全沙箱。

## add 与 Context

来源 Forge `add` 的 Consumer 在网络线程开始；010D 的 addMain 仍使用 MAIN。新 Profile 明确使用官方 NeoForge NETWORK registrar，并保留原 Type、Codec、Consumer、配置/网络引用。

| 操作 | 固定来源行为 | E 的有限适配与实际证据 |
|---|---|---|
| getSender | Connection.listener 是 ServerGamePacketListenerImpl 时返回其真实 player，否则 null | 使用真实 Connection/listener 和同一字段算法；断开模型返回 null，真实玩家 NOT_RUN |
| setPacketHandled | 来源 Context 的布尔状态，初始 false | Scope 初始 false，原 Handler 在 getSender 后、enqueue 前写 true；非法顺序/false 拒绝 |
| enqueueWork 同线程 | Runnable 同步执行，成功返回 completedFuture；异常同步抛出 | 实际服务器线程检查执行次数、线程及异常对象身份 |
| enqueueWork 跨线程 | 来源 supplyAsync，返回 Future 保留原失败 | 用实际 BlockableEventLoop Executor 重现原算法；实际服务器 FIFO、成功/null、失败/cause 身份通过 |
| Native Context.enqueueWork | 固定 Neo guard 记录错误并将异常 Future 转成功 | 隔离调用官方 API 实测差异；E 不复用会改变来源失败的 guard |
| Handler | 原 Consumer 与原业务 Runnable | 直接调用官方已注册 Handler，在实际主线程执行；没有报文投递 |

Neo loop 的 submitAsync 还增加来源未有的日志包装；因此适配直接使用 CompletableFuture.supplyAsync 与已验证 loop，不使用会吞错误的 Context.guard，也不使用额外日志包装。Scope 验证官方具体 Context class/固定 class SHA、PLAY、方向、Connection/listener 一致性、真实 loop、来源 handler/module/loader、线程和单次顺序，不提供空 Context 或固定 sender。

隔离模型的诊断 listener 明确断开，无在线玩家；业务只计数/记录引用，不删除、给予或转移真实玩家物品。模型同时验证同线程 Handler、worker 提交 Handler、线程名称及源 Context 引用。CLIENTBOUND 注册/Type/Codec 已验证，客户端 Handler 实际执行 NOT_RUN。

原 JEI 的 server wrapper 具有 sender 非 null 分支，构造 `ServerPacketContext(player, serverConfig, connectionToClient)`，然后 handled/enqueue；sender null 时仅 debug。client wrapper 获取 Minecraft.player，构造 `ClientPacketContext(player, connectionToServer)`，同样有 null 分支。原 wrapper 的这些分支、Consumer 捕获和派生 packet.process 仍是未批准的业务闭包，不能由安全计数夹具推断兼容。

## 多 Payload 与 Codec

新计划支持 2–16 个定义，至少各方向一个，来源顺序严格保留，每项保存 Type/Codec/Direction/Consumer/原顺序/owner，ID 去重，整组通过才发布。静态正例验证 2、4、6 数量；真实 FML 验证每模组 6 个，两模组共 12 个。只准入以下三个完整数据类形态：

- final/Object 直接 Payload，VAR_INT+BOOL 二元 composite，构造器 `(IZ)V`。
- final/Object 直接 Payload，STRING_UTF8→list→apply→一元 composite，构造器 List。
- final/Object 直接 Payload，私有无参构造器+真实 singleton+StreamCodec.unit。

校验完整 fields/methods、泛型 buffer/packet 签名、Type ID、Codec owner/descriptor、LambdaMetafactory tag/捕获/擦除及实例化类型、构造字段绑定。不自动准入 ItemStack、Enum、Record/派生 packet 或任意 composite。Neo 注册仍使用原 Type、Codec 和原 Consumer 对象；方向、版本3、optional、原顺序均通过官方注册表观察。

原始 JEI 六种定义与本轮组件结果：

| ID / 方向 | 原 Codec / 数据 | 本轮组件用例 | 生产完整准入 |
|---|---|---|---|
| jei:delete_player_item / SERVERBOUND | ItemStack.STREAM_CODEC | 带 CUSTOM_NAME 的 PAPER×7 | BLOCKED |
| jei:give_item_stack / SERVERBOUND | ItemStack + EnumStreamCodec GiveMode | 两个枚举值 | BLOCKED |
| jei:recipe_transfer / SERVERBOUND | TransferOperation list、VAR_INT lists、BOOL flags | 空、边界整数、非空 Record 操作列表，共3组 | BLOCKED |
| jei:set_hotbar_item_stack / SERVERBOUND | ItemStack + VAR_INT | 0/8；-1/MAX 原构造器拒绝 | BLOCKED |
| jei:request_cheat_permission / SERVERBOUND | 原 INSTANCE + unit | singleton | BLOCKED |
| jei:cheat_permission / CLIENTBOUND | BOOL + STRING_UTF8 list | 空、Unicode/空字符串 | BLOCKED |

六个 Codec 以原始未修改 JEI JAR 的隔离子 ClassLoader 加载，使用实际 MinecraftServer.registryAccess 的 RegistryFriendlyByteBuf 编解码，核对实际 Type 对象、实例字段、ItemStack components/count、列表及无剩余字节。没有执行 process、权限检查或发送。原 Internal clinit 也在该隔离组件中实测初始 Feature/default 引用，不等价于原 JEI FML 构造。

## Channel 后续 ABI

实际来源 Channel.isRemotePresent(Connection):boolean；NetworkDirection.PLAY_TO_SERVER/PLAY_TO_CLIENT.buildPacket(Channel,Object):Packet，随后调用来源实际 listener.send(Packet)。ConnectionToServer.isJeiOnServer 先取 Minecraft 当前 ClientPacketListener，无连接返回 false；连接 UUID 改变时读取实际 Channel.isRemotePresent 并更新两项静态缓存。sendPacketToServer 还要求非 null 客户端连接和 isJeiOnServer，随后 build/send；ConnectionToClient 则通过传入真实 ServerPlayer.connection 发送。

这些涉及客户端类、连接切换、协商、远端缺失、断线和权限。本轮只保留 ABI/来源图与明确拒绝，不以固定 true/false、假 Packet 或替代玩家实现。`SEND_QUERY_CONTRACT=NOT_IMPLEMENTED`。原 Channel 的完整后续业务身份尚未完成，独立夹具只承诺定义与归属。

## 本轮最终验证

以下均为本轮最终 JAR 实际运行，不继承 D 的动态结果：

| 验证组 | 实际结果 |
|---|---|
| E 完整计划与独立负例 | 246 断言；2/4/6 数量；来源克隆与整组保护 PASS |
| E 实际 Reader/预计划原子性 | CLIENT / DEDICATED_SERVER 模型 34 断言 PASS；不是客户端启动 |
| E 独立 Forge first/restart | 2 组 PASS；每组观察器221、Context结束累计244断言 |
| E 独立 Neo first/restart | 2 组 PASS；每组观察器256、Context结束累计280断言；12个原定义 |
| 原始六 Codec 组件 | 每组6个 Type、共12个正例；热栏2个拒绝；真实registry buffer；process NOT_RUN |
| 旧 D 网络 | 136计划+34双Dist原子性；4个真实 first/restart PASS |
| B 双 Bus/配置 | 128计划；双方 default/modified/persisted 6组；额外Neo owner组 PASS |
| C 构造/Locale | 212计划+34双Dist；双方配置6组 PASS |
| C 原 JEI ServerConfig 组件 | 双方6组 PASS；与完整 JEI mod 构造分开 |
| Mouse Tweaks | 101断言/52负例；归属15/7；ForgeAPI53、NeoAPI65/7拒绝；728链接、6Mixin、21 no-init、8隔离初始化 PASS；GUI NOT_RUN |
| 冻结/旧模组/混装 | 18组生命周期 PASS；Probe、metadata/discovery/identity、Registry/Context/Global/Event initializer、Clumps/FnF、混装和相应严格负例 PASS |
| Clumps 静态契约 | 99断言/28负例 PASS；实际物理球/重载对照另有运行组 |
| FnF 观察器 | 67 Item/Holder、66 Tab、58 FOOD及燃料正负例实际 PASS；食用/GUI/玩家状态 NOT_RUN |
| Helper CI 本地 | 10字节码断言；FnF expectation 3正/12负 PASS |
| 本地静态 | 106 Python、25 zsh、374冻结保护、E新增Python内存语法 PASS |
| 原始 JEI 单因素启动 | 精确预计划拒绝测量 PASS_EXPECTED_REFUSAL；游戏生命周期 FAIL |

总计最终 cohort 46 次实际服务器调用，其中45个生命周期PASS、1个原始JEI预期FAIL。对照空服等生命周期PASS不等价于原mod功能PASS。`final-cohort-audit.json` 重新核对所有安装mods字节及最终JAR，所有检查的原输入保持不变。

C 的专服 Locale/Translator 诊断会保留客户端类的 Dist 拒绝日志；E Neo 原生 guard 模型故意记录异步异常。原始 severityLines、stdout/stderr 均保留；生命周期PASS不是“日志无ERROR”或“客户端可用”的声明。

严格负例 32 类的独立变异、来源及真实错误见 `tests/real_mods/jei/network_closure/MATRIX.md`。Cursor 部分错误沿用 UNKNOWN_CONSTRUCTOR_EFFECT/UNKNOWN_BUS_ORIGIN，不能将审计分类伪称为新的生产错误码。来源 SHA、规范化 clone、未声明 class、跨 ModFile、owner/module/loader/phase 和故障重试均有独立断言；旧 D 拒绝矩阵保留。

## 原始 JEI 首失败

实际首次异常（`remaining-final/jei/runtime/stderr.log`）：

```text
Exception in thread "main" java.lang.IllegalStateException: Dual bus rejected: UNVERIFIED_NETWORK_INITIALIZATION UNKNOWN_PAYLOAD_TYPE source=mezz/jei/common/network/packets/PacketDeletePlayerItem super=mezz/jei/common/network/packets/PlayToServerPacket access=33 required=VERIFIED_FINAL_DATA_PAYLOAD globalConnection=STATIC_VERIFIED originalSetter=RETAINED sourceClinit=STATIC_VERIFIED context=FINITE_ADD_PROFILE_ONLY combined=REJECTED_NO_TARGETS
```

实际生命周期 FAIL / EXITED_BEFORE_DONE / exit1。预期拒绝测量的 PASS_EXPECTED_REFUSAL 不代表 JEI 启动通过。

Discovery、metadata、DUAL_BUS 候选为真实启动通过；locale/config/连接前缀与 source clinit 是该真实 preplan 的静态证明。combined plan 拒绝，所有转换 target map 均未发布。原 Minecraft 依赖范围 `[1.21,1.21.1)` 也按原声明保留，两个 Dist 的 Reader 模型有明确断言；本轮 targets 先拒绝，并未证明后续 FML 版本依赖校验通过。原 JEI 的 GAME 层 transform、class definition、mod constructor、NetworkHandler runtime、Dist 后续和游戏 lifecycle 均 NOT_RUN。不要把隔离 Codec 子 ClassLoader 的类定义记为这些阶段通过。

## 制品、失败记录与归档

最终 JAR：`/home/albert/Env/DualForge/runs/convergence-010e/build-7/dualforge.jar`，281,925 bytes，SHA `210dea8a93ba60ade5a38f01cdc1a8c9532b2f7e29675176ff61ebb370af0558`。38 Java + 5 resources 的 43 项精确生产输入与 build-inputs.json 一致，并在提交后逐路径核对。源码提交及归档指纹见证据索引；本地运行验证绑定代码提交；GitHub CI 绑定最终 docs HEAD，外部 delivery-final.json 保存最终 HEAD/远端/CI 核验，避免报告自指。

保留 fixture-build-1/2 的泛型失败、build-2 的常量歧义、较早合同拒绝预期错误、forge-model-1 对热栏 -1 的原构造拒绝等试跑原件。它们是本轮失败/中间候选，不计入最终 PASS；修正测试或实现后采用新目录，不覆盖旧输出。

运行副本中重复 libraries 在完成并 SHA 验证后回收，完整 canonical SDK、原 JAR、world/config/mods、原始 stdout/stderr/启动命令、输入前后清单和重建 receipt 继续保存于 SSD。只处理 010E 新生成重复副本，不删除旧阶段实验。归档包含剩余完整本轮证据、来源快照及清单；完整 SDK 由 canonical 外部路径和逐文件指纹绑定，不声称重复副本仍在每个 run 中。

完整保留证据归档：`/home/albert/Env/DualForge/runs/convergence-010e/Convergence-010E-Evidence.zip`，95,620,776 bytes，6719成员，SHA `b9a2422851e5e43d36e32ca5d2c8933d926cfb97fa9463897a04eb7864918c08`。ZIP CRC及每成员SHA全部核验。清单 `/home/albert/Env/DualForge/runs/convergence-010e/evidence-manifest.json`，SHA `11f682664e719f5972a8d41e74937ce7342acaf61e1e8e6aa71b12608c89c333`。后续 PR/CI 最终核验收据与归档分开保存于同一 SSD 目录，不伪称已包含在此前归档。

## Git 交付与剩余阻塞

代码提交 `7670f4ca3f887b7ffbf85182c0831b6cfc3a1931` 已推送，43项生产输入与该提交blob逐字节匹配。Draft PR [#39](https://github.com/GFander/DualForge/pull/39)，base `codex/convergence-010d-jei-network`，仅新增E。报告/索引独立文档提交；最终文档HEAD、远端SHA、实际CI结论、PR状态和完整diff审计在 `/home/albert/Env/DualForge/runs/convergence-010e/delivery-final.json`。该外部收据是最终HEAD/CI的权威记录，避免把自身提交SHA写入自身。PR已通过应用工具成功关联。本轮不合并、不强推、不改写旧提交、不发布Release。

原 JEI 派生 Payload/Record、原 process 与权限业务、null/player 分支及 CLIENT Context、Channel presence/send/协商缓存、Dist/插件与 GUI 闭包仍需后续逐项验证。真实客户端条件未建立，不请求 Windows/PCL 人工验收，不制作已知无法启动的 ZIP。新制品不会继承旧 FnF/Clumps/Mouse 客户端人工 PASS。

## 结果字段及范围

PASS 的范围以正文矩阵为准。INTERNAL_SET_SERVER_CONNECTION 为 PARTIAL：原 JEI setter 静态证明、独立 setter 真实执行通过，但原 JEI NetworkHandler未执行；SOURCE_CONTEXT_ADAPTER/SENDER 为 PARTIAL：断开服务端模型通过，真实玩家及客户端运行仍未测。

```text
010E_BRANCH=codex/convergence-010e-jei-network-closure
010E_HEAD_SHA=See delivery-final.json (final docs HEAD; code=7670f4ca3f887b7ffbf85182c0831b6cfc3a1931)
010E_PR_URL=https://github.com/GFander/DualForge/pull/39
010E_PR_BASE=codex/convergence-010d-jei-network
010E_PR_STATUS=OPEN / DRAFT
GLOBAL_CONNECTION_IDENTITY=PASS
INTERNAL_SET_SERVER_CONNECTION=PARTIAL
NETWORK_CONSTRUCTOR_CLOSURE=PARTIAL
FORGE_ADD_SEMANTICS=PASS
SOURCE_CONTEXT_ADAPTER=PARTIAL
SENDER_IDENTITY=PARTIAL
ENQUEUE_WORK_SEMANTICS=PASS
PACKET_HANDLED_SEMANTICS=PASS
SIX_PAYLOAD_ABI=PASS
COMPOSITE_CODEC_SUPPORT=PARTIAL
MULTI_PAYLOAD_REGISTRATION=PASS
CHANNEL_OPERATION_CONTRACT=PARTIAL
REMOTE_PRESENCE_CONTRACT=BLOCKED
SEND_PACKET_CONTRACT=BLOCKED
INDEPENDENT_FORGE_RUNTIME=PASS
INDEPENDENT_NEO_RUNTIME=PASS
REAL_CONNECTION=NOT_RUN
REAL_PACKET_DELIVERY=NOT_RUN
JEI_NETWORK_CLOSURE=BLOCKED
JEI_NEO_CURRENT_FIRST_FAILURE=BLOCKED (UNKNOWN_PAYLOAD_TYPE / PacketDeletePlayerItem inherits PlayToServerPacket; REJECTED_NO_TARGETS)
JEI_COMPATIBILITY=PARTIAL
FNF_REGRESSION=PASS
CLUMPS_REGRESSION=PASS
MOUSE_TWEAKS_REGRESSION=PASS
DUAL_BUS_REGRESSION=PASS
CONSTRUCTOR_REGRESSION=PASS
NETWORK_010D_REGRESSION=PASS
FINAL_DUALFORGE_SHA256=210dea8a93ba60ade5a38f01cdc1a8c9532b2f7e29675176ff61ebb370af0558
CLIENT_ZIP_STATUS=NOT_RUN (010E_JEI_CLIENT_ZIP=NOT_PRODUCED)
REMAINING_BLOCKERS=BLOCKED (original derived payload/process/permission/context; Channel send/query/cache; Dist/plugins/client; real connection)
PUSH_STATUS=PASS (code pushed; final docs head verified in delivery-final.json)
MERGE_STATUS=NOT_RUN (not merged)
```
