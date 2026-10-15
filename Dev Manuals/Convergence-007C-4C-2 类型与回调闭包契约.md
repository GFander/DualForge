# 4C-2 类型、回调闭包与桥接契约

生产提交 `b552f2e42407c7da9a113919f8a20a4070f78b0f`；只涉及Plan、Bridge、Supplier的host成员检查入口和新增ControlledCreativeTab。起点与先行ABI裁定见本轮设计文档。原FML/发现/依赖/Module Layer、旧Profile和构造器ABI不改。

## 精确类型

Item保持原IForgeRegistry→Registry路径，Tab只接受真实 `Registries.CREATIVE_MODE_TAB:ResourceKey`。源/目标create均为INVOKESTATIC，参数 `(Lnet/minecraft/resources/ResourceKey;Ljava/lang/String;)`，返回owner分别为Forge与Neo的DeferredRegister。不是将任意ResourceKey放行；Item的ResourceKey新变体也未顺带支持。

每个helper一个明确typed DeferredRegister，字段/Holder泛型必须一致。源 `RegistryObject<CreativeModeTab>` 的字段、返回、调用和Signature统一成为 `DeferredHolder<CreativeModeTab,CreativeModeTab>`；Item则为Item/Item。注册身份为 `(registry key,namespace,path)`，不同Registry中的shared合法，同Registry重复拒绝。未知类型、descriptor或generic不降级为Object。

## 延迟闭包

Tab工厂按API调用及栈数据流验证builder、title(translatable)、icon、displayItems、build，非固定整类指令数量模板。标准精确LambdaMetafactory、零捕获、static实现、SAM与实现descriptor一致；回调实现必须属于同ModFile并可访问。未知Handle/Bootstrap、捕获、额外API、分支/异常表拒绝。

icon只在延迟调用时消费已验证Item Holder或纯getter，构造真实ItemStack；display逐项从同闭包Item Holder/getter取得Item，使用实际Output.accept(ItemLike)。不按两个或66个输出固定计数识别生产。字段、方法返回、Consumer generic、cast、Handle和实现归属必须闭合；未知/缺失Consumer不发布任何Target。不在clinit或工厂中主动执行回调/Holder.get。

原Supplier一般业务验证保持；未涉及的Food数据及Item子类仍不变换。新Fixture精确四Target：入口、Item helper、Tab helper和跨类Consumer。克隆后ASM验证、初始SHA、冲突和完整计划发布保持；不承诺回滚JVM已定义类。

静态规范化比较涵盖五个private static业务方法，允许的差异仅为经过验证的Holder字段/方法owner或descriptor边界；Minecraft Builder、常量、控制流、BSM/Handle和原业务顺序保持。before/after ClassFile及原始JAR指纹保存，不用整个重序列化class字节相等冒充语义证明。

## Bridge状态与动态诊断

依次核对source BusView、active owner、实际caller helper方法/module、entry构造栈、重建计划fingerprint、GAME层、宿主DeferredRegister精确class、真实字段receiver和namespace。最后根据计划类型分别比较官方Registries.ITEM或CREATIVE_MODE_TAB的对象身份。没有公开原生Bus解包。

每owner/receiver状态：首次调用前ATTACHING；官方成功后ATTACHED；调用抛异常后UNCERTAIN。成功重复、重入和不确定状态重试分别拒绝。官方异常作为cause保留，不清除状态或假设副作用已回滚。

独立诊断实测六例：Tab重复挂接、wrong owner、wrong receiver、wrong Registry key、官方partial failure、uncertain retry。wrong receiver/key调用实际私有下游guard，是明确白盒范围，不能冒充通过了完整合法caller链。异常诊断临时替换本Fixture的final receiver与真实FML容器Bus，使用实际原生receiver；官方register先置flag，向真实Bus添加一个监听器，第二次添加时注入异常。实测flag=true/listenersAdded=1/UNCERTAIN；恢复原Bus/receiver后重试仍拒绝，且未再次调用官方注册。标准C/T使用原观察器，不包含这些替换。

服务端通过真实getIconItem和buildContents检查回调时机、次数及结果，不是创造栏客户端渲染验收。客户端/全局ForgeBus/任意Registry和任意Java业务均不在范围。
