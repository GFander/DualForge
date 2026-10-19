# Convergence-010D JEI网络兼容交付报告

日期：2026-10-18。**整体 PARTIAL**。有限 PLAY/addMain 网络定义及 Neo 官方注册子契约通过，原 JEI 完整网络/入口仍 BLOCKED。本轮没有生成客户端 ZIP，也没有请求用户提前进行 Windows 验收。

最终制品：`build-3/dualforge.jar`，254617 bytes，SHA-256 `2b37abbdd1d7a531480c427cc69a893ee3f790a428e8ddb9f4fb668ed24c745f`。以下运行路径相对于 SSD `/home/albert/Env/DualForge/runs/convergence-010d/`。源码在 `/home/albert/Codex/DualForge-010D` SMB worktree；全部 JVM 串行，构建/JAR/日志/世界均位于 SSD。

## Git 基线与保全

重新 fetch 后实际 main 为 `e47423aa2a48e02b128098a03d5486ad8faa37f8`。PR #35 / #36 / #37 仍 OPEN/DRAFT，未合并；010C 实际 HEAD `bdbbb0729f0d9b9c49f027b933d4605387fb626f`。本轮从该提交新建 `codex/convergence-010d-jei-network`，PR base 为 `codex/convergence-010c-jei-constructor`，不把前置研究和 B/C 实现声称为本轮新增。远端实查收据见 `intake/git-baseline.json`。

本轮生产/工具源提交 **`eaba11a27f0ce20ec42f6af2c4750a8c6a9aac39`**。`source-current.json` 绑定全部34 Java与5资源文件；与被测 build-inputs 及提交后的源码逐字节一致。最终文档 HEAD、PR 与真实 CI 收据另存 SSD `delivery.json`，避免文档自引用 SHA。未经授权未合并、未强推、未发布 Release。

010A/B/C 冻结报告及索引保持原字节，374项保护通过；原 Probe 仅使用冻结输入，未重建。主 worktree 用户未跟踪 Windows 人工报告 SHA `ba9a4006253f7bfbdcfee0a7c3632adaad5db7d4215b474f4a17186373b504bf` 未变化。主 worktree 没有切分支或改目录。历史 Ubuntu 构建/启动与旧 Windows PASS 仍属于原 cohort，本报告只列实际新运行。

## 固定输入与 ABI 研究

MC1.21.1 / Forge52.1.0 / Neo21.1.256 / FML4.0.45 / ModLauncher11.0.5 / Microsoft OpenJDK21.0.10+7-LTS。构建版本、原生启动命令、SDK/JDK逐文件 SHA 均保留。

原 Forge JEI：1529402 bytes，SHA `f5e61a001d57dd044d6f2ba255daf955c50d90e4173f8b4933ab0eb8cd386ed5`；同版本 Neo 发行仅作只读对照，SHA `8aaf547432f1b4958239b036356b910692fe40f858c3073d996f56bbf7c99826`。未修改/重新打包原 JEI，也未用 Neo 版替代运行。

`abi-current` 重新核对923+923个ClassFile，逐类匹配原发行 SHA 后保留完整 ASM 指令；新取50个相关类的完整 javap 和固定 SDK 全部网络 API javap。`analysis-current-2` 提供11643个方法的直接调用/Bootstrap 图，NetworkHandler 起始的173个已声明方法边。完整原始指令保存不意味着未知 interface 分派、继承、Dist 或闭包自动准入。

原始数据流、偏移、Packet IDs、Codec 输入、实际发送/查询与 Native 对照详见 [来源图](../tests/real_mods/jei/network/JEI_NETWORK_SOURCE.md)。关键事实：

- 入口局部5的原 IServerConfig、`jei:channel` 和整数3传入 NetworkHandler；结果局部6再与同一 subscriptions 传入 ClientSafeRunner，随后 DistExecutor。
- NetworkHandler 先保存 config、构造 ConnectionToServer，再在 byte25 调 `Internal.setServerConnection`，实际 PUTSTATIC 全局引用；随后才创建 ConnectionToClient 和通道。
- 原注册顺序为五个 SERVERBOUND（delete/give/recipe transfer/hotbar/request permission）与一个 CLIENTBOUND（cheat permission）；使用 **add + 自有 Context/排队包装**，并非本轮有限 addMain 回调形状。
- 原 Codec 包括 ItemStack、Enum、TransferOperation及list、VAR_INT、BOOL、STRING_UTF8和unit；不把本轮单 int 往返推广为原六个复杂 Codec PASS。
- server wrapper 用真实 sender/config/client connection 构造业务 context，再 setPacketHandled/enqueue；client wrapper读取真实 Minecraft.player。发送还依赖原 Channel、PLAY_TO_SERVER/CLIENT.buildPacket、实际 listener/player.connection。远程存在缓存按 listener UUID 更新，不能改为固定值或 NoOp。
- 同版 Neo JEI 自身使用官方 RegisterPayloadHandlersEvent、MAIN/optional，与自有包装。其二进制是参考证据，不是 Forge 发行已被转换通过的证明。

## 生产契约与生命周期

新增 `ForgeNetworkPlan`、`ForgePayloadSemantics`、`ForgeNetworkBridge`；仅在既有 DualBus Plan/Bridge 加入受控组合。身份复用真实 ForgeBusIdentity，与网络定义/语义/副作用分层，不新增宿主总线或 Layer，不公开 PayloadRegistrar。

```text
原 Forge 构造器完整字节码计划
  → 原 Type/Codec/Consumer 的不可变定义
  → 原 entry 构造完成
  → 官方 NetworkRegistry.setup / RegisterPayloadHandlersEvent
  → Native PLAY 两方向注册 / MAIN 包装
```

有限准入要求完整9类组合：C的Locale/Translator、双Bus、布尔配置、显式事件，加 Network 与两个 payload。名字、ID、字段及非负版本从字节码推导，无 JEI/modId/包/类/SHA 白名单。构造链必须 named/version/optional/payload/play/serverbound/addMain/clientbound/addMain/build；恰好两个不同 Type、各单 int VarInt Codec及完整方法证明。

原私有业务 callback 限定计数、保存 payload 值和线程名，不读取 Forge Context。只在完整计划证明后把该 Context 描述符改成内部不透明 Object；不直接 cast Forge Context 为 Neo Context。Native MAIN wrapper 负责入队；未执行真实收包前不声称该 callback 的实际线程或业务效果已 PASS。

构造时不调用 Native 注册表，只捕获真实对象并封存 List.copyOf。实际 owner Mod Bus 的官方事件回调要求：entry COMPLETE、同 ModFile/module/loader、entry/network/channel identity、实际官方 setup 栈。Native Registrar 保存原 Type/Codec，内部适配器持有原 BiConsumer。固定8个宿主网络 ClassFile SHA绑定；未知 builder、bootstrap、Codec、方向、Context/线程、side effect 或返回用途整体拒绝。

协议整数原值转十进制字符串；optional原定义与两个方向属性保留。但 Forge 按 channel、Neo 按 payload 协商，完整协议等价性仍 **PARTIAL**。本轮没有提供任意 channel 发送、存在查询、sender/reply 或断线适配。

宿主抛错保持原异常、标 UNCERTAIN并拒绝重试；不声称撤回部分注册。真实注册时序失败不吞错，不返回空成功。

## 独立 Forge / Neo 验证

两个 Forge-only 业务输入（仅Forge SDK编译，未为Neo重编译）：

| 输入 | 原 JAR SHA-256 |
|---|---|
| netalpha，10773 bytes | `06263528fb3cf3b70571595108cde676527b7505da6f5382fae58fdb735495e5` |
| netbeta，10756 bytes | `8f34c9ab93d977cda0defd7b0d08d161e31247cd70baa97e67cefb4c1cc20bf1` |

`cohort-final/network-live`：两平台分别首次启动、正常停止、物理复制本轮已停止 world/config、重启并再次停止，共4轮 PASS。安装前后原业务输入 SHA相同，两个实际 ModContainer/Module 分离，原 FML setup/start 各一次。

| 层级 | 本轮结果与实际证据 |
|---|---|
| STATIC_ABI | 136项新计划/拒绝断言；9类有效组后遇原JEI失败，两Dist下所有目标映射仍空，34项断言 |
| ISOLATED_CODEC | 两owner×两方向×7个int原Codec往返；畸形VarInt实际拒绝；不是网络收发 |
| ISOLATED_NATIVE_NETWORK_API | Source双侧VersionTest、Neo实际协商算法：同版本、异版本、方向错、optional缺失、required缺失；无真实连接 |
| REAL_FML_REGISTRATION | Forge实际通道与Neo官方事件实际4项Payload；Type/Codec原对象、原Consumer捕获、MAIN包装、版本/optional/方向及隔离；4轮真实启动/停机/重启 |
| REAL_CONNECTION | NOT_RUN |
| REAL_PACKET_DELIVERY | NOT_RUN；callback计数仍0，未直接调Handler冒充投递 |
| CLIENT_GUI / Windows | NOT_RUN |

原生 observer 每轮Forge69、Neo105项检查通过。观察器分别按平台编译，只用于测量；业务始终是同两个原始Forge JAR。所有7层的边界及22类拒绝坐标见 [测试矩阵](../tests/real_mods/jei/network/MATRIX.md)。

native duplicate在真正官方事件内抛错且原记录不覆盖；native lock后实际迟到注册拒绝；官方事件内隔离多协议调用先写PLAY再因LOGIN失败，实际部分记录保留。另用 live owner 的私有隔离session验证Bridge遇真实native duplicate后的UNCERTAIN/异常传播/拒绝重试；该模型绕过definition-capture caller path，**不声称完整构造链故障注入**。错误module/loader、过早/错误阶段及重复等组件模型独立标注。

本机无DISPLAY/WAYLAND会话、无Xvfb。未伪造玩家/在线会话；真实连接、报文、回调线程、客户端绘制和Windows验收均NOT_RUN。重启只是注册/世界文件重新加载，不代表玩家背包或报文持久化。

## 原 JEI 当前实际首失败

最终JAR + 原ForgeJEI 单因素 `cohort-final/jei/runtime` 实际退出于 Transformer.targets 预计划：

```text
Exception in thread "main" java.lang.IllegalStateException: Dual bus rejected: UNVERIFIED_NETWORK_INITIALIZATION UNKNOWN_NETWORK_SIDE_EFFECT source=mezz/jei/forge/network/NetworkHandler.<init>(Lmezz/jei/common/config/IServerConfig;Lnet/minecraft/resources/ResourceLocation;I)V call=mezz/jei/common/Internal.setServerConnection(Lmezz/jei/common/network/IConnectionToServer;)V definitionRegistrationContract=PLAY_ADD_MAIN_ONLY actualChannelUsesAdd=UNVERIFIED combined=REJECTED_NO_TARGETS
```

相比010C笼统的NetworkHandler分配边界，本轮具体定位到建通道前尚未验证的全局setter；**仍在Network层，未推进到Dist执行**。整个原JEI网络闭包仍未准入，不能写 NETWORK_SUBPLAN=PASS。不是 setter 已经运行抛错。

| 阶段 | 当前实际状态 |
|---|---|
| discovery / metadata / dependency projection / entry selection | 实际到达，DUAL_BUS候选选中 |
| Locale / Bus来源 / router / ServerConfig | 原入口预计划中静态验证通过；不是原JEI构造完成 |
| 具体完整事件语义 | BLOCKED：无本轮有限显式订阅形状，原动态/WeakConsumer生命周期未批准 |
| Network | BLOCKED：eager global setter；另有原add/context/复杂codec/send/query闭包未验证 |
| Dist / ClientSafeRunner | NOT_REACHED / NOT_RUN |
| combined plan / targets publication | 实际拒绝，无部分发布 |
| class transform / definition / mod construction / lifecycle | NOT_RUN |

保留完整stderr/stdout/debug、源码栈与安装指纹；没有跳过任何原始业务。`010D_JEI_CLIENT_ZIP=NOT_PRODUCED`。

## 同一最终 JAR 的相关回归

全部是本轮重新运行，复用的是指纹匹配的冻结测量输入，而非旧结果：

- 18组既有Probe、metadata/discovery/entry/module/ModFile、Context/Registry/Global/EventInitializer，Clumps原生/兼容及经验球/XP/重载，FnF原生/兼容注册与代表行为，FnF+Clumps混装。
- 010B 128项计划/负例，Forge/Neo各7项原生Bus API，Alpha/Beta六轮COMMON/SERVER默认/修改/持久化重启；42项额外owner/config/partial-error诊断。
- 010C 212项计划/负例、34项两Dist原子拒绝；六轮Locale/Translator+配置矩阵；原JEI ServerConfig/IServerConfig组件另六轮真实工厂/加载/修改/重启对照。组件通过不等于原JEI mod通过。
- Mouse Tweaks 101项/52负例、ownership、双平台API和静态/无初始化链接；实际GUI/Mixin应用仍NOT_RUN。
- CI helper字节码契约10断言、FnF3正12负；106项Python、25个zsh、374项保护通过。

最终综合收据 `assessment-final.json`：**SCOPED_AUTOMATED=PASS / overall=PARTIAL**，290项证据绑定检查、41个选定正常生命周期游戏运行，另有原JEI当前预计划拒绝。配置原生SERVER路径差异沿用已验证宿主策略：Forge world/serverconfig、Neo config；未宣称完整per-world/同步等价。旧FnF候选分类、Clumps单次事件、Mouse客户端事件负例均保持。

## 证据、失败与交付限制

选定原始附件压缩包 `Convergence-010D-Evidence.zip`，71567248 bytes，4300项，CRC检查通过，SHA **`6ffe4af211591277a6e1926e5f12b272b922f5c28b32bd3aec19139b0f358f98`**。索引含完整manifest及关键原件SHA；SDK/libraries不重复打包，其原始文件继续位于SSD，逐文件/逐Class SHA保留。附件仅本地SSD保存，未对外发布。

早期失败仍保留：夹具RegistryFriendlyByteBuf泛型编译、bridge Event泛型编译、静态接口调用校验、observer编译classpath顺序、测试向抽象接口方法插NOP等；修正后另建目录，未覆盖失败或把它们计入最终PASS。静态图Bootstrap整数解析的首次失败只在工具调用回执中，analysis-current为空目录，没有把它列为完整原始stderr。生产代码在build-3后未改变；最终证据严格绑定该JAR和39个生产输入。

剩余阻塞：原JEI eager全局写入、局部对象组合/Context/复杂Codec/发送与查询/动态事件生命周期/Dist/client完整闭包；完整连接协商与真实包、真实客户端及玩家行为。没有用资源静态、Codec模型、服务端注册或历史Windows人工验收替代这些门槛。
