# 010C 验证矩阵

最终结果绑定交付报告的生产源码、JAR SHA 和证据索引；早期失败目录不参与最终 PASS。

| 项目 | 证据层级 | 断言 |
|---|---|---|
| 原/参考 JEI 各923类、Bootstrap、成员 | STATIC_VERIFIED | 原发行字节与指纹未改；精确 Locale/Translator/接口/订阅器 |
| Minecraft Locale/Language/I18n成员 | STATIC_VERIFIED | 两完整 SDK 的 public 方法声明；不表示客户端运行 |
| 原生 Forge 原 JEI Locale | REAL_RUNTIME_PASS / expected supplier failure | 真实实例注册；Dedicated Server getter 被原生 DistCleaner 拒绝；重复/失败状态与全局 Language 保全 |
| 新受控完整计划 | ISOLATED_MODEL_PASS | 212项断言，原 Supplier/Translator/ServerConfig 静态验证，未知部分不产生计划 |
| Reader/预计划双dist | ISOLATED_MODEL_PASS | CLIENT与SERVER明确 Network 拒绝，全部计划map为空，原输入不改 |
| 独立原 Forge 双输入 | REAL_RUNTIME_PASS | 每平台default/modified/persisted三轮、同SHA、一次初始化、Bus/Owner隔离、接口派发、真实配置读值/保存/重启 |
| 原始JEI组件 | ISOLATED_COMPONENT_WITH_LIVE_HOST | 两平台原方法与native spec；不执行工厂注册，不替代ModContainer或保存 |
| 原配置工厂的新容器集成 | REAL_RUNTIME_PASS_COMPONENT | 原ServerConfig/IServerConfig原字节、真实owner注册与业务接口读取、两宿主共六轮默认/修改/恢复；不等于完整JEI |
| 原010B双Bus/配置 | REAL_RUNTIME_PASS + MODEL | 原输入/观察器、COMMON/SERVER三阶段、旧128项计划断言及各7 native API |
| FnF/Clumps/Mouse/混装 | REAL_RUNTIME / MODEL分开 | 新JAR相关全组，原Probe不重建；Mouse GUI/Mixin应用无继承 |
| 原JEI完整Neo | REAL_RUNTIME_FAIL | 完整预计划拒绝Network，无转换/定义/构造/生命周期 |
| 客户端 / GUI / weak生命周期 / network / Dist | BLOCKED或NOT_RUN | 不跳过、不制作已知失败的客户端包 |

## 逐类订阅器

| 原类 | 状态 | 已知事实及限制 |
|---|---|---|
| PermanentEventSubscriptions | PARTIAL | 原双引用构造、marker分支与NORMAL/false Class/Consumer、mod getter静态形状通过；实际客户端捕获/调用闭包不准入 |
| RuntimeEventSubscriptions | UNVERIFIED | ArrayList保持Subscription强引用；拒绝Mod事件；动态register/clear/重复调用未纳入计划 |
| EventSubscription | REJECTED | 持有原Consumer，实际注册的是新WeakConsumer，注销必须使用registeredListener；不能复用强Consumer桥接 |
| WeakConsumer | UNVERIFIED | WeakReference#get后非null才accept；GC、动态生命周期和整链身份未验证 |

## 严格负例定位

全部在 `ConstructorContract.java` 的具名变异或明确断言，并在最终 `contracts-current/plan.stdout.log` / 源码中定位；同一输入前后 SHA 保全。边界模型另验证 Transformer 所有相关 map 没有发布。

| 必须类别 | 实际独立测试 |
|---|---|
| UNVERIFIED_LOCALE_SUPPLIER | 改实现接口为Runnable |
| WRONG_TRANSLATOR_OWNER | 原入口setter改指Supplier owner |
| WRONG_LOCALE_INITIALIZATION_ORDER | setter提前到构造前；真实栈验证拒绝 |
| DUPLICATE_TRANSLATOR_INITIALIZATION | 克隆完整NEW/DUP/ctor/setter四指令 |
| UNKNOWN_INITIALIZER_SIDE_EFFECT | Supplier构造加入System.setProperty真实调用指令 |
| WRONG_CONSTRUCTOR_DATAFLOW | router输入mod局部变量改为game |
| WRONG_SEGMENT_ORDER | Game Bus读取移到locale序列之前 |
| PARTIAL_CONSTRUCTOR_PLAN | 删除RETURN；真实帧/控制流拒绝；另有normalized输入改变拒绝 |
| CROSS_MODFILE_INITIALIZER | LocaleSource名字与外来ModFile交集，rejectForeignClasses拒绝 |
| UNSUPPORTED_BOOTSTRAP | Translator clinit metafactory与入口Consumer分别改成未知bootstrap |
| UNSUPPORTED_CONFIG_INTERFACE | 增加Object返回的未知接口方法 |
| CONFIG_SUPPLIER_DATAFLOW_MISMATCH | 三getter均改读同一个Supplier，不能形成一一对应 |
| CONFIG_OWNER_MISMATCH | 工厂access改为private；旧真实跨Owner反例另保留 |
| WEAK_CONSUMER_IDENTITY_UNVERIFIED | router对传入listener先创建WeakReference包装，原注册形状拒绝 |
| UNKNOWN_DYNAMIC_SUBSCRIPTION | router注册尾部增加未知动态调用 |
| UNVERIFIED_NETWORK_INITIALIZATION | 入口尾部增加Forge network调用；原JEI真实完整预计划另拒绝NetworkHandler |
| UNVERIFIED_DIST_INITIALIZATION | 入口尾部增加Dist.CLIENT读取 |
| INPUT_SHA_CHANGED | 对每个认领class传错误initial SHA |
| UNCLAIMED_CLASS_MODIFIED | 未认领class transform拒绝；closure多余类拒绝；原件不变 |

旧010B的Bus源、事件域/类、Consumer owner/bootstrap、priority/cancellation、重复/顺序、配置工厂/type/重复、ModFile冲突、所有者和宿主故障模型仍按原工具执行。字节码或组件拒绝不称为完整 FML 负向运行。早期失败及观察器修正原件均外置保留。
