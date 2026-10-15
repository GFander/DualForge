# 实际兼容范围

目标宿主为 Minecraft 1.21.1、NeoForge 21.1.256、Java 21。安装方式见 [INSTALL](INSTALL.md)。

| 输入 | 实际范围与结论 |
|---|---|
| 002–006B 冻结受控输入 | 构造、事件、Item、双 Item、Block/BlockItem 等各自限定的契约和回归；不代表一般 Forge API |
| 007B 普通原生 Forge 双 Item | 单 IEventBus 构造器、同入口零捕获纯 Item 工厂、typed 字段/get-only；服务端受控 PASS |
| Food N' Fuel Forge 1.3 | **尚不兼容**。007C-2越过平台依赖检查后，真实首失败为`DualForgeEntryTransformer.targets()`中的`005 unsupported Item profile: class structure`，尚未进入构造器；未修复该后续失败 |
| Clumps Forge 19.0.0.1 | 007历史发现失败保留；007C-1仅重新验证完整TOML静态解析，未运行/适配Clumps功能 |

Food N' Fuel 的原生基线为 PARTIAL：普通对象/Holder、食物 component 和代表燃料的真实熔炉行为已经验证；食用后变化、创造 UI、手持及重进未执行。
原生 Forge 的成功子项不等于 DualForge 兼容成功。007C-1采用宿主NightConfig标准TOML，来源依据同JAR Forge `@Mod` 与metadata归属；loader范围须兼容当前Forge52 ABI，宽范围本身不能证明来源。FML仍负责实际provider版本、Minecraft与模组依赖检查。

007C 不修改生产源码、构造器 ABI、模块层或事件/注册能力；未增加 modId、类名、文件名或 SHA 的生产白名单。
原生对照、具体首失败、同 JAR 回归及未执行项见 [交付报告](<../Dev Manuals/Convergence-007C 真实物品兼容交付报告.md>)；补充人工路径见 [原生客户端清单](../tests/real_mods/food_n_fuel/NATIVE-CLIENT-CHECKLIST.md)。

007C-1生产修改仅限Parser/Locator及直接构建依赖；required/optional、原id/range/ordering/side均保留，没有伪造`forge`或替换为`neoforge`。本轮不实现新构造器、跨类Registry、SRG、Food/Fuel/Tab能力。新结果见 [007C-1交付](<../Dev Manuals/Convergence-007C-1 交付报告.md>)；007C冻结报告不改写。

007C-2选择受控内部依赖投影：共享识别器确认同JAR Forge来源后，读取真实FML宿主；只支持MC1.21.1、NeoForge21.1.256、FML4.0.45、Java21。参考Forge52.1.0是来源ABI，不是已安装Forge，也不是javafml9999。active required/optional必须满足参考版本、ordering NONE，才从FML实际依赖列表移出；原依赖保存在`dualforgeOriginalDependencies`，逐项决策保存在`dualforgePlatformDependencyAudit`。这确实改变内部FML表示，不能以原JAR未变称作无语义转换。

inactive side保留原项交FML，不能声称版本已通过；active BEFORE/AFTER拒绝。非平台required/optional、Minecraft、side和sorting仍由官方FML验证。原生NeoForge不投影，native required forge仍缺失；没有伪造forge身份、覆盖用户配置或新JVM参数。`SOURCE_PLATFORM_DEPENDENCY=VERIFIED`仅代表平台条件；FnF运行状态仍FAIL，原生Forge基线仍PARTIAL，Mac验收NOT RUN。

精确调用链、A/B/C比较、最终同JAR回归和实际首失败见 [007C-2交付](<../Dev Manuals/Convergence-007C-2 交付报告.md>)、[契约](<../Dev Manuals/Convergence-007C-2 Forge依赖契约与验证.md>)和[测试](../tests/real_mods/food_n_fuel/dependency/README.md)。
