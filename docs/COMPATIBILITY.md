# 实际兼容范围

当前007C-4C-4：固定原始FnF及4C-3 DualForge在真实NeoForge服务端 **FNF_SERVER_BOOT=PASS**。同轮官方Forge基线和独立只读查询确认67个Item/Holder、Creative Tab及宿主Bus身份；标准C/T正常启动、保存、停止、退出。玩家食用、燃料消耗、客户端UI、回调UI显示和重启/物品持久化仍NOT RUN，不能推导为FnF全部功能兼容。生产零修改；[交付与证据](<../Dev Manuals/Convergence-007C-4C-4 交付报告.md>)、[运行入口](../tests/real_mods/food_n_fuel/runtime/README.md)。

以下按阶段记录历史范围；其中旧FnF拒绝/NOT RUN不替代上面的本轮实际运行结果。

历史007C-4B在保留旧规则基础上新增[受控Helper/Consumer](../tests/helper_registry/README.md)：同一真实Forge ModFile内的Item Registry、零捕获普通Item Supplier、纯Holder/Item getter闭包；构造期来源BusView经内部桥接挂接该真实容器的NeoForge Mod Bus。独立原生Forge基线和最终同cohort NeoForge C/T验证真实RegisterEvent、两个对象、Holder/Consumer及Bus身份。不是任意helper、Supplier业务或全局Bus支持。FnF只读计划REJECTED，运行NOT RUN；下述C3异常为历史继承。见[4B交付](<../Dev Manuals/Convergence-007C-4B 交付报告.md>)。

历史007C-4A只新增[离线API研究](<../Dev Manuals/Convergence-007C-4A 交付报告.md>)，生产目录逐字节不变，兼容范围没有扩大。候选和离线字节码成功不代表运行时成功；原FnF FAIL继承且未重跑。完整客户端补丁输入缺失，研究整体PARTIAL。固定SHA发行字节码的命名勘误见本轮取证，冻结报告不改。

目标宿主为 Minecraft 1.21.1、NeoForge 21.1.256、Java 21。安装方式见 [INSTALL](INSTALL.md)。

| 输入 | 实际范围与结论 |
|---|---|
| 002–006B 冻结受控输入 | 构造、事件、Item、双 Item、Block/BlockItem 等各自限定的契约和回归；不代表一般 Forge API |
| 007B 普通原生 Forge 双 Item | 单 IEventBus 构造器、同入口零捕获纯 Item 工厂、typed 字段/get-only；服务端受控 PASS |
| Food N' Fuel Forge 1.3 | **007C-4C-4 FNF_SERVER_BOOT=PASS**；指定SHA的原始发行JAR完成67 Item/Holder及Tab查询、标准启动/停止。实际食用、燃料、客户端及重启持久化NOT RUN；007C-3旧构造失败保留为历史，不是当前首失败 |
| Clumps Forge 19.0.0.1 | 007历史发现失败保留；007C-1仅重新验证完整TOML静态解析，未运行/适配Clumps功能 |

冻结007C记录中，Food N' Fuel 的原生基线为 PARTIAL：普通对象/Holder、食物 component 和代表燃料的真实熔炉行为已经验证；食用后变化、创造 UI、手持及重进未执行。
原生 Forge 的成功子项不等于 DualForge 兼容成功。007C-1采用宿主NightConfig标准TOML，来源依据同JAR Forge `@Mod` 与metadata归属；loader范围须兼容当前Forge52 ABI，宽范围本身不能证明来源。FML仍负责实际provider版本、Minecraft与模组依赖检查。

007C 不修改生产源码、构造器 ABI、模块层或事件/注册能力；未增加 modId、类名、文件名或 SHA 的生产白名单。
原生对照、具体首失败、同 JAR 回归及未执行项见 [交付报告](<../Dev Manuals/Convergence-007C 真实物品兼容交付报告.md>)；补充人工路径见 [原生客户端清单](../tests/real_mods/food_n_fuel/NATIVE-CLIENT-CHECKLIST.md)。

007C-1生产修改仅限Parser/Locator及直接构建依赖；required/optional、原id/range/ordering/side均保留，没有伪造`forge`或替换为`neoforge`。本轮不实现新构造器、跨类Registry、SRG、Food/Fuel/Tab能力。新结果见 [007C-1交付](<../Dev Manuals/Convergence-007C-1 交付报告.md>)；007C冻结报告不改写。

007C-2选择受控内部依赖投影：共享识别器确认同JAR Forge来源后，读取真实FML宿主；只支持MC1.21.1、NeoForge21.1.256、FML4.0.45、Java21。参考Forge52.1.0是来源ABI，不是已安装Forge，也不是javafml9999。active required/optional必须满足参考版本、ordering NONE，才从FML实际依赖列表移出；原依赖保存在`dualforgeOriginalDependencies`，逐项决策保存在`dualforgePlatformDependencyAudit`。这确实改变内部FML表示，不能以原JAR未变称作无语义转换。

inactive side保留原项交FML，不能声称版本已通过；active BEFORE/AFTER拒绝。非平台required/optional、Minecraft、side和sorting仍由官方FML验证。原生NeoForge不投影，native required forge仍缺失；没有伪造forge身份、覆盖用户配置或新JVM参数。`SOURCE_PLATFORM_DEPENDENCY=VERIFIED`仅代表平台条件；FnF运行状态仍FAIL，原生Forge基线仍PARTIAL，Mac验收NOT RUN。

精确调用链、A/B/C比较、最终同JAR回归和实际首失败见 [007C-2交付](<../Dev Manuals/Convergence-007C-2 交付报告.md>)、[契约](<../Dev Manuals/Convergence-007C-2 Forge依赖契约与验证.md>)和[测试](../tests/real_mods/food_n_fuel/dependency/README.md)。

007C-3将Profile候选检测与严格验证分开；未知/歧义入口明确拒绝，不再兜底送入旧Item Profile。新增Context只支持public Object子类、唯一public无参构造器、同JAR单一Forge owner、唯一直接Context调用链及已验证副作用。旧Normal/Legacy Item、Block和Event规则不变。

选择隔离shim而不是把NeoForge Bus直接传给仍声明Forge Bus的helper。入口仅映射两个Context调用，保留无参描述符、原Forge IEventBus和所有helper调用；shim按真实构造caller及active container绑定其官方Mod Bus。返回的是来源ABI只读关联视图，**视图对象不是原生Bus对象**；实际delegate/owner身份分别核验。不提供register/post/listener等新Forge总线操作，不创建新Bus或Module Layer。两个Registry helper、Supplier、Tab和全局总线调用保持原字节；不能称FnF完整构造ABI已兼容。

受控Context Fixture先在原Forge52.1.0运行，再用同一JAR在NeoForge验证。原生路径不接管；本轮受控子目标与真实FnF失败分开，客户端/食物/Fuel/Tab/持久化NOT RUN。见 [007C-3交付](<../Dev Manuals/Convergence-007C-3 交付报告.md>)、[ABI/方案](<../Dev Manuals/Convergence-007C-3 方案比较.md>)与[测试](../tests/entry_context/README.md)。
