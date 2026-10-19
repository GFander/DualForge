# Convergence-010F：派生 Payload、复杂 Codec 与有限 process

本轮整体 PARTIAL。新增对真实抽象父类、继承状态/方法、复合数据 Codec 与安全 process 的结构证明；复用 010B–010E 的 Bus、配置、网络对象、Context、方向和官方注册状态机。原 JEI 第一包的类型/ItemStack Codec 已越过旧拒绝，完整网络计划仍在未验证权限业务处整组拒绝。真实玩家、客户端 Context、连接、发送查询和报文投递 NOT_RUN/BLOCKED。

## 类型与字节保全

`ForgeDerivedPayloadPlan` 支持一层已验证 abstract Base→具体 Packet，Base 直接实现原生 CustomPacketPayload。验证真实 superName/interfaces、全部抽象方法和具体 override、泛型自绑定、父构造器、可选 private final int 状态及 public final 继承 getter、子类完整字段赋值、Type/streamCodec 的真实 GETSTATIC、类与成员访问约束、Bridge/Synthetic 和全部方法集合。更深继承、未知接口/default/父业务、未知初始化、未验证构造控制流继续拒绝，不按类名、modId 或 SHA 放行。

父类、Packet、Record、Enum/Codec helper、业务 Context 和连接数据类只作为同 ModFile 的 SHA 绑定只读依赖，不成为 Transformer Target。独立输入24类中只转换8类，16类原字节由原 Module 加载；只读类不得残留任何 Forge ABI 引用。完整来源 fingerprint 包括全部24类，并在原有 Entry begin 中重新读取验证。Foreign name 冲突也检查只读依赖，不能只核对8个 target。

旧 010D/010E Profile 的 targets 和严格规则保持；新 target 子集仅用于新派生闭包。`transform` 对只读/未认领类拒绝，而不是返回半转换节点。

## Codec 与业务分别证明

`ForgeRichCodecPlan` 支持 1–6 个字段的具体 composite：VAR_INT、BOOL、STRING_UTF8、ItemStack.STREAM_CODEC、已完整验证的 FriendlyByteBuf 枚举包装器及纯 int-pair Record Codec 的 list、singleton unit。校验完整生产者、构造字段绑定、buffer/泛型、LambdaMetafactory tag/捕获/擦除/实例化类型、Getter、Type/Codec、初始化顺序、全部 helper 方法；枚举的两个原始 Bridge/Synthetic 方法不删除。Record 的 ObjectMethods bootstrap 与字段句柄逐项验证。未知任意 Codec/Record 业务不因此准入。

新的 safe process 执行原始 `context.record(this, inheritedValue())`；Context 保留原 sender/config/两连接引用，只计数、记录 payload/父字段、读取实际已注册配置并记录线程。网络 Runnable 保留原 process 调用，然后执行原计数/引用记录。没有 NoOp process、提前返回、替代权限结果或玩家背包操作。

原 JEI server/client Context 是 Record，原 Server Context 的无副作用构造/getter/ObjectMethods 可只读证明；其实际 wrapper 的 sender-null/非null、客户端玩家生命周期及权限业务尚未完整准入。原删除包在 `ServerCommandUtil.hasPermissionForCheatMode(Player,IServerConfig):boolean` 的未验证调用处拒绝。这是 **targets 预计划诊断**，不是该 Java 业务方法已经执行或返回权限不足。

## 实验与证据层

- `collect_types.py`：重新核对固定原 JAR，完整 ClassFile/构造/继承/成员与固定宿主 ABI。源图923类、总发行图1846类来自本轮重新 SHA 校验。
- `collect_process.py`：六个真实 process 的静态来源方法、调用/Bootstrap/active clinit 边；接口、虚派发和 Host 业务实现明确 NOT_PROVED，不将图可达性视作权限证明。
- `DerivedContract.java`：8个实际转换类、16个只读依赖、克隆/规范化/SHA/全来源 fingerprint、独立结构/Codec/process 负例和原 JEI 第一包权限拒绝。模型，不是游戏。
- `DerivedBoundaryContract.java`：两个 Dist 的真实 Reader/scan/组合 planner 原子拒绝；前面已有合法8-target计划，后面JEI拒绝后所有缓存仍空。
- `ClosureRegressionBoundaryContract.java`：旧E的20-target正例与本轮新JEI拒绝边界结合，保留旧E冻结测试原件和历史首失败；不继续断言过时 UNKNOWN_PAYLOAD_TYPE。
- `Observer.java.in` / `DerivedModels.java.in`：真实 FML、两个 Module、每Mod6定义、父构造状态/继承解析、Type/Codec/Consumer 身份、实际服务器 registryAccess 的 buffer、枚举/非空Record list/UTF8/ItemStack components、全部字节消费与错误输入。
- `ReadonlyModels.java.in`：实际官方注册后的8 targets/16 dependencies、真实Module/Loader与每个只读class资源原字节。
- `ContextModels.java.in`：断开连接、官方已注册Handler直调、实际主线程队列、原 process/config/线程/次数。临时替换原配置 Supplier 为抛出 sentinel 的故障模型，验证经原 process 的同步异常身份及后续网络计数未执行，随后恢复。没有权限或背包业务，不声称局部副作用回滚。
- `OwnerModels.java.in`：真实owner下的隔离Module/Loader/Container/phase与宿主故障模型；UNCERTAIN保留、不假称完整构造故障注入。
- `OriginalCodecModels.java.in`：原JEI六Codec和Internal clinit的隔离组件；不调用原JEI process，不等价于 GAME 层mod构造。

业务 JAR 只用 Forge SDK 编译一次，两平台原 SHA 相同。原生观察器分别编译，required/AFTER 保证观察顺序。首次配置 alpha=true/beta=true；重启物理复制已停机 world/config 后，仅在新副本将 alpha 改为 false，process 实测 alpha=false/beta=true，原始运行不变。未测玩家存档或跨网络传输。

## 执行

所有输出在 SSD `/home/albert/Env/DualForge/runs/` 的新目录，所有本地 JVM 串行，使用固定 JDK21 与官方启动文件，不改发行 JAR。

```bash
python3 -B tests/real_mods/jei/network/collect_abi.py --output SSD_FRESH
python3 -B tests/real_mods/jei/derived_payload/collect_types.py --graph FRESH_GRAPH --output SSD_FRESH
python3 -B tests/real_mods/jei/derived_payload/collect_process.py --graph FRESH_GRAPH --output SSD_FRESH
python3 -B tests/real_mods/jei/derived_payload/build_fixture.py --output SSD_FRESH
python3 -B tests/real_mods/jei/derived_payload/run_contract.py --jar FINAL --fixtures FIXTURES --output SSD_FRESH
python3 -B tests/real_mods/jei/derived_payload/run_boundary.py --jar FINAL --fixture TREEALPHA --output SSD_FRESH
python3 -B tests/real_mods/jei/derived_payload/build_observer.py --output SSD_FRESH
python3 -B tests/real_mods/jei/derived_payload/run_live.py --jar FINAL --fixtures FIXTURES --observers OBSERVERS --output SSD_FRESH
python3 -B tests/real_mods/jei/derived_payload/run_remaining.py --jar FINAL --output SSD_FRESH
python3 -B tests/real_mods/mouse_tweaks/run_regression.py --jar FINAL --inputs-008a /home/albert/Env/DualForge/runs/convergence-008a-20261017 --output SSD_FRESH
```

`retain_evidence.py` 仅回收本任务新建、已完成且每文件SHA与完整canonical SDK一致的重复server/libraries；无Java进程才允许，保留完整SDK、mods/world/config、日志和重建receipt。不删除旧阶段。

准确制品、最终选定运行、失败试跑、独立主线收敛PR及CI见 `Dev Manuals/Convergence-010F JEI派生Payload交付报告.md` 与证据索引。`010F_JEI_CLIENT_ZIP=NOT_PRODUCED`。
