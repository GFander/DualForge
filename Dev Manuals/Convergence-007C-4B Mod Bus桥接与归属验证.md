# Convergence-007C-4B Mod Bus桥接与归属验证

## 实现边界

C3 `BusView` 仍实现来源 Forge IEventBus 标记接口，并不成为 NeoForge Bus。原 `get()` / `getModEventBus()` 两个调用点的语义不变；不新增 register/post/listener 委托。

新增 `ForgeRegistryBridge.register(Object, Forge IEventBus, String)` 仅执行验证后的 Registry 挂接，不返回原生 Bus。`ForgeConstructorContext.registryOwner` 是内部包方法，核对桥接调用者、私有 BusView 类型、真实 active owner 与 delegate、helper Module、所属入口构造栈以及官方 FMLModContainer / DualForge loader。来源为 null、任意代理 Bus、跨 owner、逃逸或非构造阶段均拒绝。

桥接根据真实 owner 重新读取原 ModFile 的不可变计划，核对指纹和 helper 方法；再检查 GAME Layer、精确宿主 DeferredRegister class identity、receiver 等于该 helper 的已验证 static final 字段、真实 namespace 和 Registries.ITEM 对象身份。每个 owner/receiver 只允许一次挂接。最后通过固定官方 public ABI 调用 `DeferredRegister.register(owner.getEventBus())`。

使用精确反射调用是为避免 SERVICE 到 GAME 的静态链接假设；没有通用反射分发、API namespace remapper、伪对象、直接 Registry 写入或全局 Bus 单例。官方异常原因保留；失败后不承诺可回滚官方已执行的副作用。

## 本轮实际运行

正式 Treatment 的 Bridge 日志与独立原生 Observer 从真实 ModContainer 读取的 Bus identity 均为 **1811787596**。随后两 Item 都在 BuiltInRegistries.ITEM，两个 Supplier 各一次；Holder.get、Consumer、Registry key、对象互异和普通 Item class identity 均验证。原始栈包含官方 RegisterEvent、DeferredRegister 及 FML 注册事件投递。Control 没有 Bridge attachment、Supplier 均零、两个 ID 不存在。

上述正例完全使用官方 stock 启动；不启用诊断环境开关。原生 NeoForge 控制模组与宿主 javafml 委托共存。

## 严格负例

独立 `run_bridge_contract.py` 运行同一生产 JAR，启用只属于测试 Observer 的 `DF4B_BRIDGE_NEGATIVES=true`。八个案例：null 来源、任意 Forge IEventBus 代理、重复挂接、foreign helper module、错误 active container、非入口构造期、两个真实 Forge 模组交叉 Bus、Context 逃逸。

跨模组案例使用冻结 C3 contextpeer 的真实 capturedBus；容器均为实际 FML 容器。部分不可从已验证正例产生的错误状态通过白盒反射构造私有视图及临时设置 active container，并在 finally 精确恢复；这些输入只证明拒绝，不是正例来源或用户运行需求。没有生产 API 解包，也未修改原 Forge fixture。

错误 Registry、namespace、receiver 数据流及 descriptor 的可达来源在静态 Contract 拒绝；运行时额外 receiver/class/key 防御检查不等于每个防御分支均被动态注入。此区分保留在验收范围内。

原始诊断与正式 C/T 分目录保存；错误计数与本轮空宿主比较。严格零 ERROR 不成立，不能因正常退出称其为零。
