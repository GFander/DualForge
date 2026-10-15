# Convergence-007C-4B 跨类ABI与Bootstrap闭包

## 精确规则

仅用于已验证的 Item helper/Consumer 闭包；其他类不进行全局字符串或命名空间替换。

| 来源调用/声明 | 宿主结果 |
|---|---|
| ForgeRegistries.ITEMS : IForgeRegistry | BuiltInRegistries.ITEM : DefaultedRegistry |
| DeferredRegister.create(IForgeRegistry,String):DeferredRegister | Neo DeferredRegister.create(Registry,String):DeferredRegister |
| DeferredRegister.register(String,Supplier):RegistryObject | Neo DeferredRegister.register(String,Supplier):DeferredHolder |
| RegistryObject 字段/返回 descriptor | DeferredHolder 字段/返回 descriptor |
| RegistryObject<Item> Signature | DeferredHolder<Item,Item> Signature |
| RegistryObject.get():Object | DeferredHolder.get():Object；保留已验证 Item CHECKCAST |
| DeferredRegister.register(Forge IEventBus):void | ForgeRegistryBridge.register(Object,Forge IEventBus,String):void |

完整 JVM descriptor、owner/name/opcode/interface flag 位于生产常量和保存的 javap。桥接调用从 stack `[receiver, sourceBus]` 增加一个计划指纹，改为 static 三参数 void；更新 maxStack，保留合法 F_SAME 分支 Frame，再以真实 ASM Analyzer 验证。构造器仍 `()V`，不扩展 ABI。

Consumer 除直接 Holder 字段访问，还实际覆盖 helper 静态 Holder 返回和跨类方法返回；泛型和 JVM descriptor 分开验证/改写。未知 cast、Handle、field、method、泛型签名或闭包缺失拒绝，不清除未知调用。

## 实际 Bootstrap

原生 Forge javac 21 产生标准 LambdaMetafactory.metafactory：

- invokedynamic 名称 get，调用 descriptor `()Ljava/util/function/Supplier;`。
- erased SAM `()Ljava/lang/Object;`。
- H_INVOKESTATIC implementation handle，owner 必须为该 helper，descriptor `()Lnet/minecraft/world/item/Item;`。
- instantiated SAM `()Lnet/minecraft/world/item/Item;`。
- 一个 synthetic private static lambda 和一个 private static factory method reference，均零捕获。

Supplier 仅支持保存的普通 Item(new Item.Properties()) 结构，可有 marker println；实现方法业务指令保持原样。不是支持任意 lambda/方法引用，也没有重写 Item 业务。当前固定 Item 类型无需修改这两个 BSM 的方法类型或 implementation owner。

原始 javap -p -v -s -c、源/class SHA、前后 ClassFile 在本轮 out/fixture 和 Contract bytes 中。实际 NeoForge 类定义、官方 RegisterEvent 内两 Supplier 的执行和 Consumer 访问是附加运行时证据；BasicVerifier 本身不提供完整宿主类层级或任意 JVM Frame 的证明。

67 个 FnF Supplier 和3个 Tab invokedynamic 仅逐项盘点；在第一个不支持 Supplier 处整组拒绝，没有验证全部业务，更未将它们批量启用。详见 FnF 边界报告。
