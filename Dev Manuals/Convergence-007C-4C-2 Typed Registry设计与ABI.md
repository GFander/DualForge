# 4C-2 Typed Registry 与延迟 Consumer 设计

起点main `854d378a1caf44ae297dfffe4defc5eb65181b9f`，PR26已正式MERGED；4C-1最终035049e及生产d850b1c均为祖先。原生产、4B/4C-1输入、FnF与证据只读冻结，起点清单在本轮out/preflight/start.json；不改旧报告。

实际Forge52.1.0和NeoForge21.1.256二进制javap已保存于out/convergence-007c4c2/abi，非源码推测：

| 来源 | 宿主 | 类型约束 |
| --- | --- | --- |
| DeferredRegister.create(IForgeRegistry,String):Forge DeferredRegister | Neo create(Registry,String):Neo DeferredRegister | 已支持Item路径，原严格规则保留 |
| Forge DeferredRegister.create(ResourceKey,String):Forge DeferredRegister | Neo create(ResourceKey,String):Neo DeferredRegister | 只接收真实Registries.CREATIVE_MODE_TAB，精确INVOKESTATIC/descriptor |
| RegistryObject<CreativeModeTab> | DeferredHolder<CreativeModeTab,CreativeModeTab> | 字段/方法/调用/泛型/Handle/局部完整闭包 |
| RegistryObject<Item>.get:Object | DeferredHolder.get:Object | 只认已验证Item helper，保留Item/ItemLike cast与对象身份 |
| CreativeModeTab.builder/icon/title/displayItems/build | 相同Minecraft owner及descriptor | 验证固定宿主实际成员；不全局替换命名空间 |
| icon Supplier / DisplayItemsGenerator | 相同Minecraft/JDK SAM | 标准精确BSM、零捕获、真实同ModFile实现，创建与调用分开 |

只扩展Item/Creative Tab两种明确Registry。注册身份为(registry key,namespace,path)，跨Registry同名合法，同Registry重复拒绝。各helper仍一个明确typed DeferredRegister，逐调用数据依赖验证，不按样本名或字段数量硬编码。

Tab工厂只建Builder并创建延迟回调；必须验证每个回调实现和所有Holder来源，禁止在clinit/工厂中提前消费Holder或主动执行回调。不按66项等样本计数接受display列表。已识别Consumer和回调才成为Target，业务数据类仍只读；全部闭包通过后克隆验证并整组发布。

Bridge重新构建同来源不可变计划，保留owner/caller/module/fingerprint/receiver/namespace/实际Mod Bus核验，并根据计划类型精确比较ITEM或CREATIVE_MODE_TAB对象身份。官方Neo register(bus)先置registeredEventBus，再分别addListener；因此调用中异常可能已有副作用。记录ATTACHING/ATTACHED/UNCERTAIN，失败不删除记录、不重试，不声称回滚官方状态。独立白盒诊断必须与正常C/T分开，不能提供公开原生Bus解包。

新tabboundary纯Forge测试输入使用两个Item和一个Tab（跨Registry合法同名ID）、icon及display跨类消费；先原生Forge再原字节Neo C/T，保持4B/4C-1回归。实际Registry/Holder/对象/官方RegisterEvent/回调时序次数/早消费拒绝与正常停止分别检查，服务端不等于客户端UI。

FnF只读强制计划记录下一真实结果：可能仍有其他拒绝，也可能计划成功，不能预设下一错误。正常入口global Bus排除不变，FNF_RUNTIME=NOT RUN。旧supplier-v1收据及冻结证据保留，新版本精确断言，不接受任意异常。客户端与全局Bus不在本轮。
