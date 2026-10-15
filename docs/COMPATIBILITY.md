# 实际兼容范围

目标宿主为 Minecraft 1.21.1、NeoForge 21.1.256、Java 21。安装方式见 [INSTALL](INSTALL.md)。

| 输入 | 实际范围与结论 |
|---|---|
| 002–006B 冻结受控输入 | 构造、事件、Item、双 Item、Block/BlockItem 等各自限定的契约和回归；不代表一般 Forge API |
| 007B 普通原生 Forge 双 Item | 单 IEventBus 构造器、同入口零捕获纯 Item 工厂、typed 字段/get-only；服务端受控 PASS |
| Food N' Fuel Forge 1.3 | **尚不兼容，007C-1 PARTIAL**。007C原生注册67个Item；本轮标准TOML/Forge准入通过，实际FML因缺失 required `forge [0,)` 拒绝，未进入入口构造或转换 |
| Clumps Forge 19.0.0.1 | 007历史发现失败保留；007C-1仅重新验证完整TOML静态解析，未运行/适配Clumps功能 |

Food N' Fuel 的原生基线为 PARTIAL：普通对象/Holder、食物 component 和代表燃料的真实熔炉行为已经验证；食用后变化、创造 UI、手持及重进未执行。
原生 Forge 的成功子项不等于 DualForge 兼容成功。007C-1采用宿主NightConfig标准TOML，来源依据同JAR Forge `@Mod` 与metadata归属；loader范围须兼容当前Forge52 ABI，宽范围本身不能证明来源。FML仍负责实际provider版本、Minecraft与模组依赖检查。

007C 不修改生产源码、构造器 ABI、模块层或事件/注册能力；未增加 modId、类名、文件名或 SHA 的生产白名单。
原生对照、具体首失败、同 JAR 回归及未执行项见 [交付报告](<../Dev Manuals/Convergence-007C 真实物品兼容交付报告.md>)；补充人工路径见 [原生客户端清单](../tests/real_mods/food_n_fuel/NATIVE-CLIENT-CHECKLIST.md)。

007C-1生产修改仅限Parser/Locator及直接构建依赖；required/optional、原id/range/ordering/side均保留，没有伪造`forge`或替换为`neoforge`。本轮不实现新构造器、跨类Registry、SRG、Food/Fuel/Tab能力。新结果见 [007C-1交付](<../Dev Manuals/Convergence-007C-1 交付报告.md>)；007C冻结报告不改写。
