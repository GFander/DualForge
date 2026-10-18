# 010E 验证矩阵与拒绝证据

所有 PASS 绑定本轮最终 JAR；历史失败与中间候选运行另列。最小正例完整通过才发布 targets；后续宿主异常不假称回滚。具体文件 SHA/命令见证据索引。

| Gate | 可验证 PASS / 拒绝条件 | 证据层 |
|---|---|---|
| 0 | 实际远端/祖先关系/原用户文件保全；E 从实际 D 创建 | Git + intake |
| 1 | 原始图重新校验、setter/类初始化/缓存/对象父引用逐条证明 | ABI + live preplan |
| 2 | NETWORK 开始、原 Consumer、真实队列、同线程和异步异常均保留 | 独立 FML + 隔离断开 Context |
| 3 | 2/4/6 计划；双方各 6 定义；三生产 Codec 形式完整验证；原 JEI 六 Codec 分组件测量 | ASM + 官方注册 + registry buffer |
| 4 | 未证实 Channel/query/send 一律拒绝，不伪造返回 | 静态清单 + 独立负例；实现 BLOCKED |
| 5 | 两个同 SHA Forge 输入、双方 first/restart、保存退出、Module 隔离 | 4 个真实服务器 |
| 6 | 来源克隆、整组验证、未知指令拒绝、失败 Session UNCERTAIN | ASM + 隔离宿主故障 |
| 7 | 以下 32 类逐个独立变异或运行组件拒绝；旧 D/B/C 负例继续通过 | plan / owner |
| 8 | 单因素最终 JAR+原 JEI 精确预计划拒绝，无 JEI targets/transform | 新 Neo 原始启动 |
| 9 | 旧 18 组、Mouse、B/C 配置/构造、D 网络重新运行；Draft PR、实测 CI | 最终 JAR 回归 |

## 独立负例

plan：`ClosureContract.java`，原始日志 `contract-final/plan.stdout.log`；owner：`OwnerModels.java.in`，`live-components-final/neo-{first,restart}/stdout.log`。每个 plan 拒绝同时验证源字节不变。Cursor 的 opcode/call/field 错误沿用旧严格错误类型；下表逐项列出独立输入变异，不以同一个异常替代未执行的负例。实际生产错误和位置以原日志为准（例如 UNKNOWN_CONSTRUCTOR_EFFECT、UNKNOWN_BUS_ORIGIN、PARTIAL_NETWORK_PLAN），不是把表内审计分类都宣称为生产错误码。

| 审计分类 | 独立输入/操作 | 收据 |
|---|---|---|
| GLOBAL_CONNECTION_OWNER_MISMATCH | Globals.set access/owner | plan |
| GLOBAL_CONNECTION_WRONG_ORDER | Network.setter receiver source field | plan |
| DUPLICATE_GLOBAL_CONNECTION_WRITE | duplicate PUTSTATIC in setter | plan |
| UNKNOWN_STATIC_SIDE_EFFECT | System.setProperty in source clinit | plan |
| WRONG_NETWORK_HANDLER_IDENTITY | ToServer parent changed to null | plan |
| UNKNOWN_CONTEXT_METHOD | unknownContext method | plan |
| CONTEXT_SENDER_UNVERIFIED | getPlayerUnchecked method | plan |
| CONTEXT_THREAD_UNVERIFIED | runImmediately instead of enqueueWork | plan |
| CONTEXT_ENQUEUE_UNVERIFIED | wrong enqueueWork return descriptor | plan |
| CONTEXT_HANDLED_STATE_UNVERIFIED | handled(false) | plan |
| UNKNOWN_PAYLOAD_TYPE | missing direct Payload interface | plan |
| DUPLICATE_PAYLOAD_ID | down ID changed to up ID | plan |
| WRONG_PAYLOAD_DIRECTION | clientbound changed to serverbound | plan |
| WRONG_CODEC_OWNER | foreign VAR_INT owner | plan |
| WRONG_CODEC_DESCRIPTOR | FriendlyByteBuf signature mismatch | plan |
| UNSUPPORTED_COMPOSITE_CODEC | BOOL changed to VAR_INT | plan |
| UNSUPPORTED_UNIT_CODEC | unit call changed | plan |
| UNSUPPORTED_BOOTSTRAP | foreign LambdaMetafactory | plan |
| UNVERIFIED_HANDLER_CAPTURE | foreign captured method owner | plan |
| WRONG_CHANNEL_OWNER | foreign namespace | plan |
| UNKNOWN_CHANNEL_OPERATION | extra unproved Channel call | plan |
| UNVERIFIED_REMOTE_PRESENCE | extra Channel.isRemotePresent | plan |
| UNVERIFIED_SEND_OPERATION | extra Channel.send | plan |
| UNVERIFIED_PERMISSION_RESULT | unknown boolean permission call | plan |
| CROSS_MODFILE_CONFLICT | rejectForeignClasses target collision | plan |
| WRONG_MODULE | live alpha identity + beta class | owner |
| WRONG_CLASSLOADER | isolated identity loader mismatch, restore | owner |
| WRONG_MOD_CONTAINER | wrong static plan id / isolated owner, restore | plan+owner |
| WRONG_REGISTRATION_PHASE | PLANNED session outside official setup stack | owner |
| PARTIAL_NETWORK_PLAN | trailing constructor effect + altered normalized class | plan |
| INPUT_SHA_CHANGED | all 20 targets per owner use wrong source SHA | plan |
| UNCLAIMED_CLASS_MODIFIED | rename transformed class outside exact targets | plan |

另外测试跨模组全局 late write、真实宿主重复注册→UNCERTAIN→拒绝 retry、原热栏构造器拒绝 -1/MAX。归属模型使用真实 owner 的隔离反射变异并恢复，不声明安全沙箱或完整恶意模组隔离。

## 不得继承的结论

| 项目 | 本轮状态及限制 |
|---|---|
| 原 JEI 全局连接前缀 | STATIC_VERIFIED；原发行 setter 未在 JEI NetworkHandler 中运行 |
| 独立全局对象 / 多定义 | REAL_RUNTIME_PASS；保留原 PUTSTATIC、对象及模块存储 |
| 原始六 Payload ABI/Codec | STATIC_ABI + ISOLATED_COMPONENT_PASS；不是生产完整类型/业务准入 |
| 实际玩家 sender | NOT_RUN；断开模型只有来源算法应返回的 null |
| CLIENTBOUND handler 业务执行 | NOT_RUN；方向/注册/Codec 已测，未启动客户端 |
| 网络构造后续故障 | catch STATIC_VERIFIED；实际宿主调用故障为 ISOLATED_MODEL_PASS |
| 真实连接、报文、远端存在性、协商/发送 | NOT_RUN / NOT_IMPLEMENTED；不得从注册或 Consumer 直调推断 |
| 原始 JEI Combined Plan | BLOCKED；派生 Payload 继承/业务闭包尚未验证 |
| 原始 JEI GAME 层类定义、构造、网络运行 | NOT_RUN；隔离子 ClassLoader Codec 定义单独记录 |
| 新 JAR GUI、玩家/客户端存档 | NOT_RUN；没有图形会话或 Xvfb |
| 客户端 ZIP | 010E_JEI_CLIENT_ZIP=NOT_PRODUCED |
