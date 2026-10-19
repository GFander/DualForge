# 010G 验证矩阵

| Gate | 证据和批准边界 |
|---|---|
| 0 | fetch/祖先/PR base/实际CI；新G worktree；旧A–F和用户未提交文件保全；不合并前置PR |
| 1–2 | 固定原发行完整权限字节码、父方法解析、双宿主ABI和选定指令对照；原权限实现保留 |
| 3 | SOURCE sender/config数据流、真实FML owner、13个host二进制、服务端连接/线程重校验；有效真实玩家路径NOT_RUN |
| 4 | 原删除包两条分支完整静态取证；只批准权限子契约，库存/反馈不执行、不假称通过 |
| 5 | 两个Forge-only原SHA输入，实际FML/官方注册/配置重启；普通Player+原函数隔离组件覆盖320案例/运行 |
| 6 | 专用权限组件；原生API直接运行，无权限shim；全源fingerprint/只读utility/整组失败/原异常保留 |
| 7 | 下表29类独立变异或实际owner错误；不是统一异常或输入SHA白名单 |
| 8 | 最终JAR+原JEI两个mods真实启动，精确targets预计划拒绝，未进入JEI类转换/构造 |
| 9 | 最终JAR重跑旧18和B/C/D/E/F/Mouse/Helper/静态；客户端和真实投递不继承历史PASS |
| 10 | 复用契约、编译器形状假设、共享验证、余留耦合和未来重构审查；无越界大重构 |

## 独立拒绝

静态证据 `PermissionContract.java`，owner证据 `IdentityModels.java.in` / `OwnerModels.java.in`。分类映射到原始诊断，部分仍沿用既有Cursor的opcode/call/field、MISSING_METHOD或WRONG_EVENT_DOMAIN；表中分类不伪称全部为生产同名错误码。

| 分类 | 独立输入/操作 | 层 |
|---|---|---|
| UNKNOWN_PERMISSION_API | native isCreative调用改为未知方法 | static |
| UNVERIFIED_PERMISSION_PREDICATE | 谓词返回前增加未知静态副作用 | static |
| WRONG_PERMISSION_METHOD_DESCRIPTOR | Player参数改为ServerPlayer，调用仍要求原ABI | static |
| WRONG_PLAYER_SOURCE | 原getSender替换为未验证来源；null和普通Player均被实际guard拒绝 | static/live guard |
| WRONG_SERVER_AUTHORITY | getServer改为未知服务；真实owner Scope使用非服务器loop | static/live guard |
| WRONG_CONFIG_OWNER | permission调用描述符改为foreign config | static |
| WRONG_CONFIG_VALUE_SOURCE | creative getter重定向为op，三个角色不再唯一 | static |
| WRONG_MOD_CONTAINER | 真实owner弱引用错位后调用身份检查，再恢复 | live owner |
| WRONG_MODULE | alpha owner检查beta来源Class | live owner |
| WRONG_CLASSLOADER | 真实owner的loader字段故障模型后恢复 | live owner |
| UNVERIFIED_CLIENT_PERMISSION | source调用改为未认领ClientClaims | static |
| UNVERIFIED_SERVER_SIDE | SERVER配置改为CLIENT | static |
| WRONG_PERMISSION_THREAD | enqueue改为未知执行方法；真实服务器外线程调用排队guard | static/live guard |
| UNVERIFIED_PERMISSION_CACHE | 增加静态布尔cache | static |
| PERMISSION_BRANCH_REVERSED | 原creative条件IFEQ改IFNE | static |
| PERMISSION_CHECK_REMOVED | 实际调用改为POP2+constant true | static |
| PERMISSION_CHECK_BYPASSED | Counter决策条件改为无条件跳转 | static |
| UNVERIFIED_INVENTORY_MUTATION | process在已证明record后增加库存mutate调用 | static |
| INVALID_ITEMSTACK_SOURCE | native STREAM_CODEC换为未批准optional codec | static |
| INVALID_SLOT_SOURCE | Record的source getter读取target字段 | static |
| UNVERIFIED_PLAYER_INVENTORY | 原始JEI已证明权限之后实际遇到containerMenu读取 | static/真实启动预计划 |
| UNVERIFIED_PACKET_RESPONSE | 已证明Context尾部增加未验证send | static |
| UNKNOWN_PROCESS_SIDE_EFFECT | Payload增加未知业务方法 | static |
| CROSS_MODFILE_CONFLICT | 只读权限utility发生foreign class名称冲突 | static |
| UNSUPPORTED_BOOTSTRAP | 权限lambda bootstrap改为foreign句柄 | static |
| INPUT_SHA_CHANGED | 每个target错误SHA；只读权限utility字节变化改变全组fingerprint | static |
| UNCLAIMED_CLASS_MODIFIED | 全部14只读依赖transform拒绝 | static |
| PARTIAL_CONSTRUCTOR_PLAN | Entry构造尾部增加未知指令 | static |
| PARTIAL_NETWORK_PLAN | 权限/计数已证明后追加未知副作用 | static |

另有Missing Permission Source→REJECT、实际原谓词变异→UNVERIFIED_PERMISSION_CHECK、固定host ABI变更→REJECT。ClientClaims true不替代Server验证；普通Player模型不能通过生产ServerPlayer guard。

## 业务正反路径

原JEI函数与独立原函数在同一个模型Player、同一实际服务器及当前own-config上对照；8组配置角色、creative两值、native stack等级0–4、模型getServer引用有/无。每次320组合，原函数/独立函数/原safe process计数均比较。生产不把这些模型等级当作真实玩家OP等级，真实ServerPlayer从连接到OP列表的行为仍NOT_RUN。

当前隔离世界server.properties记录op-permission-level=4；固定发行GiveCommand的原生注册requirement检查hasPermission(2)。op+give开启且模型stack等级2时，op分支false立即返回；仅give开启时同一native命令节点可允许。矩阵实际覆盖这些组合，不把2或4写入生产规则。真实阈值由宿主服务器读取，canUse由当前节点执行；原始属性与javap保存在SSD。

Permission False → deniedCalls+1 / protectedCalls不变；Permission True → protectedCalls+1；原process/父字段/配置/线程均保留。缺失give节点false、null原生异常、三个配置getter分别抛出原sentinel后不增加protectedCalls；既有count保留，不宣称回滚。临时配置/命令树恢复。以上是隔离安全计数器业务，不是原删除包的库存操作或发送反馈。

原始权限静态子计划PASS不等于完整网络或真实玩家权限PASS。正式数值、最终SHA、实际首失败和Git/CI见交付报告、证据索引及SSD最终收据。
