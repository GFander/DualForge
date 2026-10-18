# Convergence-010E：原始对象、add/Context 与有限多 Payload

生产扩展由完整来源字节码证明触发，不按夹具或 JEI 身份放行。旧 010D 的两个 VarInt/addMain Profile 保留，新 Profile 支持 2–16 个定义、至少每方向一个，顺序必须先 SERVERBOUND 再 CLIENTBOUND，PLAY、optional、非负整数版本。

本轮整体 **PARTIAL**。独立 Forge/NeoForge 的真实 FML 注册、原始全局对象与有限 Context 通过；原 JEI 仍在 targets 预计划整组拒绝，未执行原始 NetworkHandler。真实连接、收发、客户端 GUI、新 JAR 玩家状态均 NOT_RUN。不要将组件 Codec 往返或直接调用原 Handler 记为网络投递。

## 准入与实现

`ForgeNetworkClosurePlan` 完整验证配置引用→连接对象→原始静态 setter→另一连接→Channel 定义；setter 保留真实 PUTSTATIC，桥接仅添加归属/阶段检查。原静态存储留在各来源 Module。状态依次为 CONSTRUCTOR_STARTED、GLOBAL_SETTER_ENTERED、GLOBAL_REFERENCE_ASSIGNED、NAMED…PLANNED、REGISTERING、REGISTERED；后续异常标记 UNCERTAIN，不清空全局字段。构造器 catch 的字节码验证与隔离宿主注册故障模型分开记录，不声称已动态注入整个构造器故障。

只准入 final/Object/直接 CustomPacketPayload 数据类的三个具体 Codec 形态：VAR_INT+BOOL 二元 composite、STRING_UTF8 list+一元 composite、私有构造器 singleton+unit。完整校验字段、描述符、Bootstrap、捕获、构造绑定与所有方法；Type/Codec/Consumer 原对象不重建。业务安全回调只允许创建四引用 Context、handled(true)、enqueue 和计数/记录，不批准 JEI 删除、给予、权限或配方处理。

`ForgeSourceContextBridge` 对 add 使用 NeoForge NETWORK 注册，运行原 Consumer。sender 按原 Forge 算法从实际 Connection 的 ServerGamePacketListenerImpl.player 读取，其他 listener 返回 null；从不使用固定玩家。Scope 严格验证宿主 Context、方向/PLAY、Connection/listener、来源 Module/Loader/Handler、线程和调用顺序。实际玩家身份及客户端执行仍 NOT_RUN。

同线程直接运行并返回 completedFuture，异常直接抛出；跨线程以实际主线程 loop 作为 Executor，复现原 Forge CompletableFuture.supplyAsync，保留异常 Future。NeoForge Context.guard 会记录错误并把该 Future 转为成功，不能复用它掩盖来源异常。模型特意运行一次官方 guard 故障，日志 ERROR/FATAL 是该诊断的原始证据，不是正常报文成功证据。

原始 Channel 对象只承诺定义/归属/官方注册生命周期。连接接口仅准入 owner() 读回网络对象；send、远端存在性、协商、缓存切换等未实现，不提供固定布尔值或假 Packet。

## 输入和实验分层

业务输入 flowalpha/flowbeta 仅用 Forge SDK 编译一次，两侧用同 SHA 原始 JAR；原生观察器分别使用 Forge/NeoForge SDK 编译，明确属于诊断工具。观察器 required/AFTER 依赖保证在业务注册之后运行。

- `ClosureContract.java`：来源克隆、2/4/6 数量、Codec、所有原子拒绝。只读/ASM 模型。
- `ClosureBoundaryContract.java`：两个 Dist 下实际 Reader、scan、combined planner；前面已有合法计划，后面的原 JEI 拒绝后所有 targets map 仍为空。不是客户端启动。
- `Observer.java.in`：真实 FML、两 Module、全局 PUTSTATIC 一次、父对象/配置身份、12 个官方注册属性、原 Type/Codec/Consumer、实际服务器注册表 buffer 往返。
- `ContextModels.java.in`：明确断开的 Connection。Forge 原 Context 与 Neo 官方 Context+诊断 listener 的隔离比较；直接调用真实已注册 Handler，实际服务器队列执行，不模拟在线玩家。
- `OwnerModels.java.in`：活跃 owner 的独立归属负例；官方注册事件内调用隔离 Session 触发真实宿主重复注册异常，保留静态引用且 UNCERTAIN 拒绝重试。故障模型不冒充完整构造调用链。
- `OriginalCodecModels.java.in`：未改字节 JEI 在隔离子 ClassLoader 中的六个 Codec/Type 与 Internal clinit。使用实际服务器 registryAccess，含带 CUSTOM_NAME 的 ItemStack、所有 GiveMode、非空 TransferOperation 列表、空/边界列表、BOOL/UTF8/unit；不调用 process、权限、发送或原 NetworkHandler。
- `collect_context.py` / 010D `collect_abi.py`：重新绑定原发行图、Java 字节码和固定宿主 ABI；官方 Neo JEI 只读取证。

原 JEI 的精确新边界是首个 PacketDeletePlayerItem 的非 final 派生 PlayToServerPacket 闭包尚未证明；预计划已静态验证连接前缀、原 setter PUTSTATIC、Internal/JeiFeatures 及连接缓存 clinit。组件 Codec 往返不授予整组生产准入。

## 运行

源码留 SMB；所有输出必须位于 `/home/albert/Env/DualForge/runs/` 的新目录。固定 JDK21、MC1.21.1、Forge52.1.0、Neo21.1.256/FML4.0.45。所有编译与游戏 JVM 串行，不注入业务 JVM 开关。

```bash
python3 -B tests/real_mods/jei/network_closure/collect_context.py --output SSD_FRESH
python3 -B tests/real_mods/jei/network_closure/build_fixture.py --output SSD_FRESH
python3 -B tests/real_mods/jei/network_closure/run_contract.py --jar FINAL --fixtures FIXTURES --output SSD_FRESH
python3 -B tests/real_mods/jei/network_closure/run_boundary.py --jar FINAL --fixture FLOWALPHA --output SSD_FRESH
python3 -B tests/real_mods/jei/network_closure/build_observer.py --output SSD_FRESH
python3 -B tests/real_mods/jei/network_closure/run_live.py --jar FINAL --fixtures FIXTURES --observers OBSERVERS --output SSD_FRESH
python3 -B tests/real_mods/jei/network_closure/run_remaining.py --jar FINAL --output SSD_FRESH
python3 -B tests/real_mods/mouse_tweaks/run_regression.py --jar FINAL --inputs-008a /home/albert/Env/DualForge/runs/convergence-008a-20261017 --output SSD_FRESH
```

`run_live.py` 执行双方 first/restart，物理复制已停机的 world/config 并验证原件不变。每组必须同时有 CLOSURE_OBSERVER_PASS、SOURCE_CONTEXT_MODEL_PASS、六条 ORIGINAL_CODEC_COMPONENT_PASS 与正常保存退出；Neo 还检查隔离故障模型。重启没有玩家背包持久化结论。

`retain_evidence.py` 仅在没有 Java 进程、已完成 E 运行的 inputsUnchanged 为 true、每个 library 文件与完整 canonical SDK SHA 精确一致时，回收本任务生成的重复 `server/libraries`。保留输入清单、原始日志、world/config/mods、重建指纹；完整 SDK 原位置保留。每次操作生成新 receipt，不修改历史运行。

准确制品、命令、失败试跑、完整结果与 Git 绑定见 `Dev Manuals/Convergence-010E JEI网络闭包交付报告.md` 及同阶段证据索引。旧阶段报告保留历史结论，不覆盖本轮实际证据。
