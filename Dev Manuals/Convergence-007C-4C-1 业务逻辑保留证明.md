# 4C-1 业务逻辑保留证明

生产源码提交 `d850b1c7bf3b16609ed17a58b3fe2eec0c11b751`；最终兼容层 SHA-256 `8fd97a0a4e811aed4e8e084f172341c1b808837e202a22986705e9f0c33360c2`。设计在生产修改前的 `5ffa2f3` 提交。

## 转换边界

唯一生产差异为 `ControlledItemSupplier.java` 和 `ForgeRegistryTransformationPlan.java`。已支持的精确旧普通 Item 快路径保留；其他业务由只读 ASM 类型/链接验证器检查，不按 Food/Fuel、类名、modId 或 FnF SHA 分派。BSM/SAM/零捕获/同 owner private static 工厂绑定及原 Registry/Holder/Consumer 规则不放宽。

验证器不运行 Supplier、构造器或 clinit，不定义 Minecraft 类。宿主 ABI 取实际 LoadingModList 的 minecraft/neoforge ModFile 资源及 JDK；离线 Contract 显式提供固定实际 ClassFile oracle，其路径不会注入游戏。所使用 host ClassFile SHA 纳入 v3 计划身份，原同 ModFile 类全体原字节仍绑定计划身份。

Supplier 和前向业务依赖的未知 Forge 链接、缺失/不匹配/不可访问成员、未知类型、抽象分配、循环调用、分支/Frame/异常表、反射/动态类加载、复杂 Handle/业务 indy、数组构造等拒绝。跨包 protected 接收者需要额外证明，本轮保守拒绝该访问；此补正由实际失败负例驱动。该范围是兼容契约，不是通用 Java 安全沙箱、任意方法终止或副作用证明。

## 静态实证

`tests/supplier_boundary/PlanContract.java` 对三个原生 Forge 输入执行真实 Reader/FML scan 和完整计划，然后检查：

- 每组仅 entry/helper/Holder consumer 三个 Target。
- 每组两个原 Supplier 方法的 ASM Textifier 规范化结果完整相同；包含常量、调用、Handle/BSM（如有）、标签、局部/Frame 和异常结构。原 ClassNode 没有被修改。
- `Unused`、独立 `Pantry` 数据类及 `EnergyShard` Item 子类不进入 Target；这些依赖沿原 ModFile 原字节加载。
- 保存 before/after ClassNode、规范化方法文本、原 JAR SHA。不以整个重序列化 ClassFile SHA 相同代替方法语义对照。
- 后续 Supplier 注入未知 Forge 调用时，完整计划拒绝、不返回 Target；原输入 JAR 不变。另由原 4B Transformer 35 个负例验证整组发布、SHA、归属和克隆原子性。

三个计划的原始证据在 `out/convergence-007c4c1/supplier-contract-final/{ordinary,data,subclass}/`；最终证据索引及独立归档包含这些文件的逐项 SHA。

## 动态观察的含义

三个新 modId/包名使用同一批原 Forge 输入，不重新编译成 NeoForge 输入。观察器分别编译且不属于被转换输入：

| Case | 被保留业务 | 运行查询 |
| --- | --- | --- |
| ordinary / basicboundary | 普通 Item 与原标记行为 | 两个 Item、Registry/Holder 对象身份及各 Supplier 一次 |
| data / databoundary | 独立静态 FoodProperties、算术工厂和 Item.Properties 链 | nutrition=7、maxStack=7、数据初始化一次 |
| subclass / constructboundary | 独立 Item 子类、额外 int 构造参数、字段和真实 override | EnergyShard 实际类型、getBurnTime 返回2468、maxStack=11、类初始化一次 |

最终动态状态以交付报告和机器索引为准。C/T 仅改变 DF4C1_REGISTER；Control 中各 Supplier/依赖初始化零次，Treatment 各一次。官方 RegisterEvent 栈、实际所属 Mod Bus、native 模组共存和正常退出均由查询观察器与 runner 核验。

对 FoodProperties/getBurnTime 的查询不代表真实吃食物或燃料燃烧、Tab、客户端或 FnF 游戏运行验证。不能据此推导任意第三方 Item 子类均受支持；未解析的外部类、注解值、控制流及动态行为仍拒绝。
