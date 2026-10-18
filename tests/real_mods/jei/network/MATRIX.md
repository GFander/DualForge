# 010D 网络测试分层与拒绝矩阵

结果只从最终索引绑定的实际新运行读取；早期失败保留。原生 Forge 与 Neo 复用同 SHA 的两个独立业务 JAR。

| 层级 | 实际工具/验收 | 不覆盖 |
|---|---|---|
| STATIC_ABI | collect_abi 全 923+923 SHA 复核及新 javap；analyze_abi 直接调用与 Bootstrap 图；NetworkContract 两身份9类完整组合/克隆/SHA/负例 | 虚分派完整业务闭包、实际网络 |
| ISOLATED_CODEC | native observer 用实际 source Type/Codec + RegistryFriendlyByteBuf 对 7 个 int 往返，拒绝畸形 VarInt；两方向两 owner | channel 上真实编码/发包/收包 |
| ISOLATED_NATIVE_NETWORK_API | Forge VersionTest 双侧真值表；Neo negotiator 版本/方向/optional 缺失；Native duplicate/late/partial；Bridge 私有 guard 与故障状态 | 连接握手、完整调用链故障注入 |
| REAL_FML_REGISTRATION | 同原始 Forge 输入：Forge 构造实际通道；Neo 官方事件实际4注册、Type/Codec 原对象、MAIN 包装、原回调持有关系、owner 隔离、正常停机和重启 | 真实处理任一方向 payload |
| REAL_CONNECTION | NOT_RUN | 不伪造在线玩家/会话 |
| REAL_PACKET_DELIVERY | NOT_RUN | 不直接调 Handler 替代收包 |
| CLIENT_GUI | NOT_RUN | 不继承旧 Windows PASS |

字节码负例的类别是测试分类；实际异常可能为精确 Cursor 拒绝（`UNKNOWN_CONSTRUCTOR_EFFECT call=...`），完整原件保存实际理由，不要求异常文字等于分类。

| 必需拒绝类别 | 实际检验/范围 |
|---|---|
| UNKNOWN_NETWORK_BUILDER | named 调用改名，严格完整链拒绝 |
| WRONG_CHANNEL_OWNER | 入口 namespace 改为 foreign；桥接 named 的真实错误 caller 组件负例 |
| DUPLICATE_PAYLOAD_ID | Down ID 改为 Up；官方事件内真实 native 重复注册，原记录不覆盖 |
| WRONG_PAYLOAD_DIRECTION | serverbound 改 clientbound；native matchesFlow 排除错误方向；原生 negotiator 方向不匹配失败；真实投递 NOT_RUN |
| WRONG_PROTOCOL_VERSION | 负数 Source 版本拒绝；Source VersionTest 和 Neo native negotiator 实际异版本拒绝，连接断开行为 NOT_RUN |
| UNSUPPORTED_CODEC | writeVarInt 改 writeInt；畸形输入由原 Codec 抛错 |
| UNVERIFIED_HANDLER | 原 callback 尾部未知指令，完整方法拒绝；未批准 Context 访问 |
| WRONG_HANDLER_THREAD | addMain 改 add 拒绝；注册表实际 MAIN 包装；真实回调线程 NOT_RUN |
| UNSUPPORTED_BOOTSTRAP | 原 metafactory handle 改为外部未知 bootstrap |
| WRONG_REGISTRATION_PHASE | live owner 的隔离 PLANNED session + 非官方 setup 栈调用拒绝 |
| REGISTRATION_TOO_EARLY | 隔离 owner complete=false，host 前拒绝 |
| REGISTRATION_TOO_LATE | 实际 sealed native 注册表拒绝，记录保留 |
| DUPLICATE_REGISTRATION | actual REGISTERED session 再调用与 UNCERTAIN session 重试均拒绝 |
| CROSS_MODFILE_PAYLOAD | 丢失所引用的 Up ClassFile；实际两个 owner namespaces 分离 |
| WRONG_MODULE | A actual identity + B actual network Class 组件拒绝 |
| WRONG_CLASSLOADER | 临时改变期望 loader 的隔离身份模型；恢复并确认成功 session 不变 |
| UNVERIFIED_CONNECTION_OPTIONALITY | optional 改 optionalServer，不准入单侧规则；native/source optional 算法对照不冒充完整等价 |
| UNVERIFIED_NETWORK_RESULT | build 改为未批准结果工厂，拒绝 |
| UNKNOWN_NETWORK_SIDE_EFFECT | 网络类额外字段/原 JEI eager Internal.setServerConnection；后者真实预计划精确拒绝 |
| INPUT_SHA_CHANGED | 每个完整闭包类的初始 SHA 改变拒绝 |
| UNCLAIMED_CLASS_MODIFIED | 额外完整 class 与未认领 transform 均拒绝 |
| PARTIAL_NETWORK_PLAN | 未知尾部指令/normalized 字节改变拒绝；两 Dist 下先有效9类再原 JEI 失败，所有 target maps 仍空 |

宿主错误证据分开：官方事件内 native PLAY 成功后 LOGIN 失败，实际部分记录保留；桥接隔离 session 在真实 native duplicate 抛错后 UNCERTAIN、原异常传播、拒绝重试。后者绕过 definition-capture caller path，不声称完整构造链故障注入或回滚。
