# 010F 矩阵

| Gate | 可验证通过条件 / 当前边界 |
|---|---|
| 0 | fetch、PR关系、merge-base/graph/diff/contains；独立BCD→main Draft，F基于E；旧worktree/报告保全 |
| 1 | 六真实Packet/两抽象父类/两Context与Codec服务完整ClassFile；字段、构造、方法解析与Bootstrap逐项记录 |
| 2 | 有限一层继承/原父构造/abstract与override/继承getter；实际class/module/loader/Type身份 |
| 3 | 数据布局+Codec producer/getter/constructor/泛型/Buffer/helper全部证明；实际Registry buffer往返和异常 |
| 4 | 有限原process+config记录及异常真实执行；原JEI权限/背包/配方业务未证明则拒绝 |
| 5 | 两个Forge-only同SHA输入，双方真实FML first/restart，12定义，无碰撞，正常保存退出 |
| 6 | 下列34类独立拒绝；包括原始权限实际字节码，不以通用异常替代未执行测试 |
| 7 | 旧框架组合、全源fingerprint、只转换8类、16依赖原字节、整组发布、UNCERTAIN不回滚 |
| 8 | 最终JAR+原JEI真实单因素启动，精确preplan首失败；无Source JEI targets/transform |
| 9 | 新JAR重跑旧18、B/C/D/E及Mouse/Helper/静态；最终Git/CI/两个Draft PR |

## 独立拒绝清单

静态：`DerivedContract.java` / `contract-verified/plan.stdout.log`；owner：`OwnerModels.java.in` / `live-verified/neo-{first,restart}/stdout.log`。每个变异检查输入不被修改。部分生产诊断沿用 Cursor 的 call/opcode/field/MISSING_METHOD，分类与原始错误逐项映射，表中分类不伪称全部都是生产错误码。

| 分类 | 独立输入/操作 | 层 |
|---|---|---|
| UNKNOWN_PAYLOAD_SUPERCLASS | child superclass foreign | static source plan |
| UNVERIFIED_INHERITED_METHOD | extra parent method | static source plan |
| WRONG_SUPER_CONSTRUCTOR | child calls Object constructor | static source plan |
| WRONG_INTERFACE_METHOD | base loses native Payload interface | static source plan |
| WRONG_TYPE_METHOD_RESOLUTION | type getter reads other packet Type | static source plan |
| WRONG_FIELD_OWNER | constructor stores foreign field | static source plan |
| UNKNOWN_STATIC_INITIALIZER | reachable extra System.gc in clinit | static source plan |
| UNKNOWN_BUSINESS_SIDE_EFFECT | extra concrete business member | static source plan |
| UNSUPPORTED_ITEMSTACK_CODEC | optional ItemStack codec substituted | static source plan |
| UNSUPPORTED_ENUM_CODEC | helper loses StreamCodec interface | static source plan |
| UNSUPPORTED_LIST_CODEC | unverified list operation | static source plan |
| UNSUPPORTED_RECORD_CODEC | record gains business method | static source plan |
| UNSUPPORTED_COMPOSITE_CODEC | unverified composite call | static source plan |
| WRONG_CODEC_BOOTSTRAP | foreign bootstrap | static source plan |
| WRONG_CODEC_BUFFER | wrong RegistryFriendlyByteBuf signature | static source plan |
| WRONG_CODEC_OWNER | foreign producer owner | static source plan |
| UNKNOWN_PROCESS_METHOD | process declaration renamed | static source plan |
| WRONG_PROCESS_CONTEXT | process descriptor mismatches abstract parent | static source plan |
| WRONG_HANDLER_CAPTURE | foreign captured method owner | static source plan |
| WRONG_HANDLER_THREAD | enqueue replaced by unverified execution | static source plan |
| UNVERIFIED_PLAYER_IDENTITY | source getSender replaced | static source plan |
| UNVERIFIED_PERMISSION_CHECK | actual original delete process invokes unproved permission service | static source plan |
| UNVERIFIED_INVENTORY_MUTATION | reachable unknown Inventory mutation call in process | static source plan |
| UNKNOWN_RECIPE_TRANSFER | reachable unknown RecipeTransfer call in process | static source plan |
| CROSS_MODFILE_CONFLICT | foreign name overlaps read-only base | static source plan |
| WRONG_MODULE | real alpha owner + beta class | live-owner component |
| WRONG_CLASSLOADER | isolated live identity loader mismatch | live-owner component |
| WRONG_MOD_CONTAINER | isolated live owner mismatch | live-owner component |
| DUPLICATE_PAYLOAD_ID | down id duplicates up id | static source plan |
| WRONG_PAYLOAD_DIRECTION | clientbound changed to serverbound | static source plan |
| WRONG_REGISTRATION_PHASE | isolated session outside official setup stack | live-owner component |
| INPUT_SHA_CHANGED | all8 targets wrong SHA; all24 source fingerprint | static source plan |
| PARTIAL_NETWORK_PLAN | unproved ctor tail / changed normalized target | static source plan |
| UNCLAIMED_CLASS_MODIFIED | unknown class and every read-only dependency rejected by transform | static source plan |

另有 WRONG_METHOD_GENERIC_BINDING、WRONG_ENUM_GENERIC_BINDING、FORGE_ABI_IN_READONLY_DEPENDENCY；真实buffer的截断/非法enum ordinal/null；原热栏 -1/MAX 构造拒绝；真实宿主重复注册→UNCERTAIN→拒绝重试；Source process 原配置抛出异常后保留原异常身份并恢复配置。

## 分层结论

| 项目 | 可声明范围 |
|---|---|
| 独立继承/父构造/Type/Codec/process | 真实FML + 原process直调 + 实际主线程；不是报文收发 |
| 纯Record/Enum/ItemStack/List/composite | 有限生产形态与组件通过；不是所有原JEI六包完整生产准入 |
| 原六类来源图 | 静态完整；第一删除包类型/Codec通过；Hotbar额外构造验证、其它额外业务/Record helper等仍需闭包 |
| 原Server Context | 原Record静态证明；真实玩家与原wrapper完整执行 NOT_RUN |
| 原Client Context | 静态来源，客户端生命周期 NOT_RUN；不得完整PASS |
| 原权限/背包/配方业务 | BLOCKED；未知业务没有NoOp/提前返回/伪造结果 |
| 原JEI网络闭包 | BLOCKED，生产预计划拒绝；GAME类定义/构造/生命周期NOT_RUN |
| 连接、协议、实际投递、发送查询、GUI | NOT_RUN / NOT_IMPLEMENTED；不继承旧Windows PASS |
| 配置复制/保存 | 已停机世界副本与新副本修改对照；不声明玩家背包持久化 |
| 故障模型 | original process/config fault、isolated owner/host operation；不声明完整构造故障注入或回滚 |

最终实际数值、最终SHA与报告/归档指纹，以本阶段交付报告和索引为准。更早候选和命名含final但SHA不符的目录均为中间试跑，不能计入最终PASS。
