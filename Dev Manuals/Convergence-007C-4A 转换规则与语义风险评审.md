# 转换规则与语义风险评审

日期：2026-10-15（Asia/Shanghai）。独立 Draft [PR #22](https://github.com/GFander/DualForge/pull/22)，base 为未合并 PR #21 分支。任务起点 `29893683ea584b26208eb205b819c3f3376147be`，实际被测工具源码 `25571c40671d6d80ca10a4dcec77db84440ff555`。生产目录与起点逐文件相同。

候选格式版本1，source/targets含固定平台版本、owner/name/descriptor、actual artifact；kind、必要flags、Generic Signature、来源形状、指令kind、strategy/status、evidence、scope和启用状态独立记录。Field owner/name/descriptor同时变化的Registry实例已作为seed纳入。目标不是单字符串替换；多目标保留AMBIGUOUS_MAPPING，不按相似度选胜者。来源方法kind改变需要独立栈契约，原型目前拒绝，不能因ASM能改opcode就批准。

类型耦合：IForgeRegistry与Registry并非完整等价API；RegistryObject<T>与DeferredHolder<R,T>泛型个数不同，实例承载/可用时序也不由名称证明。当前离线只验证Item类型，显式Signature规则映射成<Item,Item>，其他Registry泛型拒绝。Forge Mod Bus与NeoForge Mod Bus的类型/生命周期不能由包迁移解决；BusView私有delegate真实归属仅有007C-3受控证据，无生产解包接口。

现有转换规则盘点16条，来自7份当前源码逐文件SHA和未改007C-3编译class的ASM读出；source/target符号均直接核对固定官方/compat ABI。详细exact descriptor、scope和原Contract见checked-in统计摘要引用及归档existing-rules.json。旧构造器单Bus注入、Event superclass/annotation/register、空注册shim、Item/Block typed Registry/create/register/get、两个Context调用各有自己的严格验证。entry-only和same-file event helper均不能授权普通Registry helper。历史动态INHERITED，未新跑。

## 外部项目研究（均未导入生产）

Retromod main `e693278379a6c80cd1a6623136a166f4d8c5cafe`：

- [ForgeRegistryApiShim](https://github.com/Bownlux/Retromod/blob/e693278379a6c80cd1a6623136a166f4d8c5cafe/src/main/java/com/retromod/shim/api/forge/ForgeRegistryApiShim.java)：类重定向、字段实例迁移及create参数的先后关系有参考价值；不能把IForgeRegistry空壳当作完整接口实现。
- [ForgeNeoForgeApiBridge](https://github.com/Bownlux/Retromod/blob/e693278379a6c80cd1a6623136a166f4d8c5cafe/src/main/java/com/retromod/shim/forge/ForgeNeoForgeApiBridge.java)：明确讨论post返回/实例register行为差异，说明同名事件API需要语义桥接。
- [ForgeNeoForgeSynthetics](https://github.com/Bownlux/Retromod/blob/e693278379a6c80cd1a6623136a166f4d8c5cafe/src/main/java/com/retromod/shim/forge/ForgeNeoForgeSynthetics.java)：缺失Context/marker合成是架构参考，不适用于直接在当前宿主命名空间注入同名类。
- Shim声明源1.20.1→目标1.21.0；pom为MC1.21.4，并含26.x条件，不是固定52.1.0/21.1.256的ABI证据。本轮只独立核对Registry局部符号及静态数据流，不执行Retromod或继承其runtime PASS。

Sinytra Connector `1.21.x` / `5a2f668ad492c1586c279a197423f5a1d1a16509`：

- [MappingAwareReferenceMapper](https://github.com/Sinytra/Connector/blob/5a2f668ad492c1586c279a197423f5a1d1a16509/transformer/src/main/java/org/sinytra/connector/transformer/transform/MappingAwareReferenceMapper.java)：owner/name/descriptor和refmap命名层分别处理。
- [JarTransformInstance](https://github.com/Sinytra/Connector/blob/5a2f668ad492c1586c279a197423f5a1d1a16509/transformer/src/main/java/org/sinytra/connector/transformer/jar/JarTransformInstance.java)：class provider、enhanced remapper、逐visitor管线及audit有参考价值，不应把Fabric转换过程等同Forge API桥接。
- libs.versions.toml实际为MC1.21.1 / NeoForge21.1.248，非本任务21.1.256；最新默认26.2.x未用于该分析。IntermediateMapping和固定版本文件一并归档，所有源码SHA见research/manifest.json。未运行Connector，无runtime继承。

## 风险与门禁

1. 委托/事件时序、register是否同一真实容器bus，必须新C/T证明。
2. 跨类Holder字段、方法返回、BSM、泛型不闭合则整组拒绝；不能只改ItemRegistries。
3. FnF普通/食物/燃料工厂业务保持不变。静态Item/food调用存在不代表其游戏行为通过。
4. 完整客户端输入、运行时注入和动态/反射盲区使全平台语义判定仍PARTIAL。
5. 不把未解析判为不存在，不把独立fixture的离线证明变成整个Forge命名空间替换。
6. 生产代码未改，无第二套生产实现；不修改Module Layer或旧严格拒绝，不新增Event/Registry/Networking Shim。
