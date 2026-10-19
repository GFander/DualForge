# 原 JEI 网络 ClassFile 来源与尚未准入的闭包

本轮重新核对原 Forge `jei-1.21.1-forge-19.27.0.340.jar` SHA `f5e61a001d57dd044d6f2ba255daf955c50d90e4173f8b4933ab0eb8cd386ed5`，只读对照同版本 Neo 发行 `8aaf547432f1b4958239b036356b910692fe40f858c3073d996f56bbf7c99826`。没有重打包或用 Neo 版替代被测输入。

原件位于 SSD `convergence-010d/abi-current/`：50 个相关发行类的新 `javap -p -c -s -v`；全部 1846 类 ClassFile SHA 与旧 ASM 完整指令逐类复核后保存 `complete-source-graph.jsonl.gz`。`analysis-current-2/network-call-graph.json` 保存全部 11643 个方法及 NetworkHandler 起始可到达的 173 个已声明方法直接调用/Bootstrap 图。interface/继承/Dist 动态可达性未据此自动批准。SDK 的全部 network 二进制、所属 JAR 和逐类 SHA 见 `*-network-binaries.json`，每类的新 javap 同目录保存。下面 ASM index 与 javap byte offset 分开。

## 原入口的数据流

`JustEnoughItems` Class SHA `2a0446c8da2acfcc5271abcd04a56c13bd7d02ae1caab6b727c3d54c72e3a741`：

| byte offset / ASM index | 原指令与对象关系 |
|---|---|
| 4–11 / 6–9 | new MinecraftLocaleSupplier → Translator.setLocaleSupplier |
| 14–34 / 12–26 | 实际 GAME、MOD 局部变量1/2 → PermanentEventSubscriptions，存局部3 |
| 35–45 / 29–35 | ModLoadingContext 局部4 → ServerConfig.register，原 IServerConfig 存局部5 |
| 47–64 / 38–46 | new NetworkHandler，传同一 config 局部5、`jei:channel`、原整数3，结果存局部6 |
| 66–76 / 49–54 | new ClientSafeRunner，传同一 NetworkHandler 局部6和同一 subscriptions 局部3，存局部7 |
| 78–88 / 57–60 | Dist.CLIENT、捕获同一 runner 的 Supplier/安全 Runnable → DistExecutor.safeRunWhenOn |

原入口使用局部对象，不是新增有限夹具的 private final network 字段形状。这一数据流仅取证，没有伪装为已经批准的整入口计划。

## 原 NetworkHandler 构造器

Class SHA `90cf6cbdd5eb3df7a1c57901e684e43d72371543d2f4f722ddacfcddb5acda86`，描述符 `(IServerConfig, ResourceLocation, int)V`。

1. byte4–6：原 config 参数存 `serverConfig`。
2. byte9–18：`new ConnectionToServer(this)`，存原接口字段。
3. **byte21–25 / ASM20–21：取该对象 → Internal.setServerConnection。** `Internal` 的实际方法立即 PUTSTATIC serverConnection。这是业务全局效果，发生在建通道前；本轮未准入，也没有删除、跳过或延迟它。
4. byte28–37：`new ConnectionToClient(this)`，存接口字段。
5. byte42–55：`ChannelBuilder.named(arg2).networkProtocolVersion(arg3).optional().payloadChannel().play()`。
6. byte60 起：serverbound，依序注册下表5个原 Type/STREAM_CODEC/包装 BiConsumer，调用 **add**。
7. byte168 起：clientbound，原 CheatPermission Type/Codec/包装 BiConsumer，调用 **add**。
8. byte196–201：build 返回原 Forge Channel，存 `channel`；getChannel 将它暴露给原连接对象。

| 顺序/方向 | Payload ID（namespace均为 jei） | 原 Codec 输入 |
|---|---|---|
| 1 SERVERBOUND | delete_player_item | ItemStack.STREAM_CODEC → composite |
| 2 SERVERBOUND | give_item_stack | ItemStack.STREAM_CODEC + EnumStreamCodec → composite |
| 3 SERVERBOUND | recipe_transfer | TransferOperation.STREAM_CODEC list、VAR_INT 两个 list、BOOL×2 → composite |
| 4 SERVERBOUND | set_hotbar_item_stack | ItemStack.STREAM_CODEC + VAR_INT → composite |
| 5 SERVERBOUND | request_cheat_permission | singleton INSTANCE → StreamCodec.unit |
| 6 CLIENTBOUND | cheat_permission | BOOL + STRING_UTF8 list → composite |

每个原 Packet 的 clinit、Codec Lambda Bootstrap、process 及业务调用保存于完整图/新 javap，尚未用本轮单 int Codec 子契约替代批准。

## Handler、发送、存在查询与清理

`wrapServerHandler` 捕获 this 与传入的 Packet::process BiConsumer；Source Context.getSender 非空时，用原 player、同一 serverConfig、connectionToClient 构造 ServerPacketContext，setPacketHandled(true)，enqueueWork 捕获原 payload/context 的 Runnable，实际执行原 process。null sender 分支仅日志。`wrapClientHandler` 直接读取 Minecraft.getInstance().player，非空时构造 ClientPacketContext、同样 handled/enqueue，null 分支日志。两个 enqueue 返回 CompletableFuture 均 POP；不得把原包装直接 cast 成 Neo IPayloadHandler，亦未批准任意 Context 映射。

`ConnectionToServer.isJeiOnServer` 读取客户端 listener UUID；UUID 改变时更新 cache，经 NetworkHandler.getChannel → Channel.isRemotePresent(actual connection) 查询。无连接返回 false。sendPacketToServer 要求实际连接及该存在查询为真，再以 PLAY_TO_SERVER.buildPacket(channel,payload) → 实际 client listener.send。没有独立断线回调清空 cache 的指令；下次新 UUID 触发更新，不推断更多清理保证。

`ConnectionToClient.sendPacketToClient` 使用 PLAY_TO_CLIENT.buildPacket(channel,payload) → 实际 ServerPlayer.connection.send。ServerPacketContext/ClientPacketContext 保留原业务引用；process 后还进入物品、权限、背包和配方转移逻辑。这些原始发送/连接/sender/客户端业务仍未准入，没有虚拟在线身份或 NoOp 返回。

原网络类还有 Log4j LOGGER clinit；Codec 使用原发行已有 DataFixer 函数及 MC 物品/转移类。固定发行直接字节码引用的外部库保留完整图与 SDK 来源；未知接口分派或可选路径不据此宣称闭包已穷尽。

## 固定 Forge / Neo 生命周期对照

Forge ChannelBuilder 在构造调用中建立 NetworkInstance；默认 exact(int)，optional() 在 client/server 两侧组合 ACCEPT_MISSING、ACCEPT_VANILLA、exact。play BUF 为 RegistryFriendlyByteBuf。Source addMain 包装原 BiConsumer 到 Context.enqueueWork，然后标 handled；本轮有限 callback 不读取这个 Context。

Neo NetworkRegistry.setup 先通过 ModLoader.postEvent 发 RegisterPayloadHandlersEvent，再锁 setup=true。Registrar 默认 MAIN/required，optional 与 executesOn 返回保留属性的新 registrar；playToServer/Client 存真实 PayloadRegistration。注册表锁后、空协议、blank version、minecraft namespace、duplicate 均抛错。多协议注册循环可先写 PLAY 再遇不支持 LOGIN 抛错，不能宣称原子回滚。

同版本 Neo JEI 自己也有 eager Internal setter，但把 payload 注册拆为 registerPacketHandlers(PermanentEventSubscriptions) 添加官方事件监听；其事件 callback 调 registrar(protocolVersion).executesOn(MAIN).optional，依原顺序注册五 serverbound、一 clientbound。原包装仍 enqueueWork。它是精确对照证据，**不等于 Forge 发行已经能被本轮子契约转换**；尤其原 Source add/context/global/send/cache/Dist 与两个固定方向的无 Context addMain 契约不同。

完整握手、连接缺失、断线、实际拒绝断开和游戏线程回调：NOT_RUN。版本/optional/方向只有独立 native 算法与注册属性证据，完整协议语义保持 PARTIAL。
