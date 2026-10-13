# Convergence-006A：实际 ABI 与字节码矩阵

2026-10-13（执行器 UTC）。[E] 本轮实际 javap/ASM/字节断言；[S] 静态契约；[D] 本轮支持边界。
实际取证根目录：`/workspace/DualForge-006A/out/convergence-006a/fixture/20261013-100359-3c202741/`。
`javap-*.command.json/stdout.log/exit.json` 和 `asm-nodes.*` 保存真实输入、描述符、flags、节点与 BootstrapMethods。

## 编译输入与样本

[E] Minecraft1.21.1、Forge52.1.0、Microsoft Java21.0.10+7-LTS；同原 Probe 的三份 Forge依赖：
javafmllanguage `d27e1f3149ea08fc0a80779fe24fa8585692c5042a34bad153e55662486acc15`，
eventbus6.2.27 `d2a7bcecb80b33f90854a003dc295960263d78435d9d6016c427d711510cb999`，
forge universal `88ef6625122cfdb9d7b3176bd61b8c0692a191bad0edd77ffddc6a765ee4979c`。
Forge编译classpath只有这些依赖、原 remapped pre-patch vanilla MC 与其纯运行依赖；
每个 MC class 全量检查无 Forge/NeoForge patch owner；生成 Forge class 无 NeoForge/DualForge owner。
NeoForge查询driver用独立patched Neo server优先classpath编译，不将其计入Forge兼容能力。

| 实际样本 | 字段/方法 | 注册调用 | `<clinit>`实际opcode | Supplier | Mod bus/时序 |
|---|---|---|---|---|---|
| 原005 RegistryProbe（原JAR不重编译） | ITEMS/ICE_ETHER；registeredItem | 1 | 10 | private static synthetic lambda | 静态声明后，构造器参数1条件挂接一次 |
| MultiRegistryProbe | CATALOG/PRIMARY/SECONDARY；alphaValue/betaValue | 2 | 15 | lambda + private static createSecondItem method ref | 同一DR，DF006_REGISTER仅控制同一次挂接 |
| LocalRegistryProbe（独立字节对照） | DEPOT/FIRST_TOKEN/SECOND_TOKEN；firstEntry/secondEntry | 2 | 17 | lambda + makeDelta method ref | 实际ASTORE0/ALOAD0寄存器别名，先赋DR字段再声明，两getter |
| SupplierForms（compile-only/unclaimed） | 无注册字段 | 0 | 无 | plain/staticref/captured/serializable | 不安装为Forge入口，仅取实际Bootstrap供负例 |

[E] 三个入口构造器都是 `(Lnet/minecraftforge/eventbus/api/IEventBus;)V`、26 opcode。
已支持 Supplier 业务子形态为10 opcode：System.out.println(String) marker +
new Item(new Item.Properties()) + ARETURN；getter为4 opcode。
SupplierForms 的普通无marker工厂是7 opcode，属于取证，不能据此称已支持该业务形态。
本轮不同名称、数量及17-opcode布局对照都走实际Reader、扫描和Transformer，不靠虚构ASM例子判定支持。

## 实际成员描述符与一致映射

[S/E] `javap-forge-dr/holder/registry`、`javap-neo-dr/holder` 来源于锁定真实JAR。

| Forge实际调用点 | NeoForge输出 |
|---|---|
| ForgeRegistries.ITEMS:`IForgeRegistry` | BuiltInRegistries.ITEM:`DefaultedRegistry` |
| DeferredRegister.create:`(IForgeRegistry,String)DeferredRegister` | `(Registry,String)DeferredRegister` |
| DeferredRegister.register:`(String,Supplier)RegistryObject` | `(String,Supplier)DeferredHolder` |
| DeferredRegister.register:`(Forge IEventBus)V` | `(NeoForge IEventBus)V` |
| RegistryObject.get:`()Object` | DeferredHolder.get:`()Object` |
| 字段DeferredRegister<Item> | Neo DeferredRegister<Item> |
| 字段RegistryObject<Item> | Neo DeferredHolder<Item,Item> |

完整包名前缀分别为 `net/minecraftforge/registries/`、`net/neoforged/neoforge/registries/`；
Item为`net/minecraft/world/item/Item`；Registry/DefaultedRegistry属于`net/minecraft/core/`；
总线前后为`net/minecraftforge/eventbus/api/IEventBus` / `net/neoforged/bus/api/IEventBus`。
标准 `<clinit>` create/两声明、constructor挂接、两getter及所有字段引用分别验证并映射；没有API owner全局替换。

## 实际 invokedynamic 与 BootstrapMethods

[E] Alpha和Beta的indy均为 `get:()Ljava/util/function/Supplier;`。
BSM均为`H_INVOKESTATIC java/lang/invoke/LambdaMetafactory.metafactory`，完整descriptor：
`(MethodHandles$Lookup,String,MethodType,MethodType,MethodHandle,MethodType)CallSite`（java/lang/invoke包）。
三参数严格为：

1. SAM `()Ljava/lang/Object;`
2. 同入口类 `H_INVOKESTATIC` 实现 `()Lnet/minecraft/world/item/Item;`
3. instantiated type `()Lnet/minecraft/world/item/Item;`

Alpha handle指向真实`lambda$static$0`（PRIVATE|STATIC|SYNTHETIC）；
Beta handle指向真实`createSecondItem`（PRIVATE|STATIC）。matrix同样有不同命名的`makeDelta`。
生产验证器从Handle发现方法，不要求lambda$前缀，不加入factory或fixture名称白名单。

[E] 实际捕获样本indy为 `(Ljava/lang/String;)Ljava/util/function/Supplier;`，
实现method为`(String)Item`；序列化intersection样本产生`altMetafactory`、额外BSM args和
`$deserializeLambda$`。这些都不能直接复用零捕获映射。负例使用真实编译的BSM/implementation，
仅把owner重定位到受控测试节点，分别明确拒绝；没有改写BSM或业务方法试图支持它们。

## 验证算法及提交原子性

[S/D] 原整类2字段/4方法和10-opcode initializer模板已移除。
新的私有Plan记录经完整验证的具体字段和指令：

- 一个typed DeferredRegister<Item>字段；每个其余字段是typed RegistryObject<Item>，public/static/final；不允许未解释字段。
- restricted `<clinit>` symbolic stack证明真实ITEMS来源、metadata所属namespace、唯一create、DR赋值、literal ID、Supplier来源及holder消费。
- 每个声明独立检查ID唯一、同一个已赋值DR receiver、标准零捕获Bootstrap、结果只消费到一个独立holder；全部holder恰好赋值。
- 仅允许实际取证的DR局部ASTORE/ALOAD；禁止未使用别名、任意locals、跳转、POP丢弃或未知API。
- constructor保留原26-opcode/branch子契约，必须使用参数local1挂接该DR一次；不扩展构造器ABI或bus来源。
- supplier仍是已证明的10-opcode普通Item子契约；getter各自验证真实holder、get/Object、Item cast和return；每holder一个getter。
- 每个方法按ctor/init/由Handle证明的supplier/由holder证明的getter完全归类，未验证额外方法或调用整类拒绝。

[S/E] targets阶段只从官方Forge metadata和@Mod扫描归属发现；transform阶段重新在original.accept生成的
ClassNode副本构造Plan，全部验证通过后只修改记录的成员点。不会缓存discovery节点的指令引用。
Supplier方法、Bootstrap数组及业务字节完全不动。某一晚期getter失败时不会暴露前面已验证的部分编辑；
另有实际在rewrite后dump I/O失败的负例，确认抛受控异常且原节点字节不变。

## 实际字节与范围证据

[E] primary/local-layout保存完整 `.before.class/.after.class`；按唯一允许的constructor、字段signature/desc、
registry field和成员点反向恢复后，整个canonical class字节等于原始字节。
两种Supplier的方法、flags/metadata与BootstrapMethods逐项/逐字节不变；真实服务器dump再与离线Transformer结果比较。
unclaimed同形类和native metadata路径保持原字节，普通SupplierForms类不被target。
10项拒绝/失败场景均保存before/after/reason，其中9项结构拒绝，1项测试型dump I/O注入。
原005 extra-registration负例也通过：重复ID/未消费结果被拒绝，而不是靠固定opcode/字段数。

[D] 本轮动态证明原单Item和新双Item；更多声明的角色算法没有固定N上限，但未宣称任意数量/真实模组已动态验收。
不支持捕获/altMetafactory、跨类factory、无marker/任意supplier业务、额外ForgeAPI、不同Registry、
额外构造形态、不同constructor业务布局、多个getter/未验证方法或Registry+Event混合入口。
没有新增RegistryObject API、事件系统、Capability、Fabric或006B功能。
