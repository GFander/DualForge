# Convergence-007C-2 平台兼容方案比较

本比较在生产修改前形成，依据FML4.0.45源码/字节码、真实discovery上下文及DIAGNOSTIC_ONLY ModSorter内存实验。不是依据方案名称选择。

| 方案 | 源码可行性与代价 | 当前决定 |
|---|---|---|
| A 独立校验后内部依赖投影 | 现Parser可在ModFileInfo之前构造配置；Sorter已实验证明消费该模型。不增加mod身份、容器或Layer；可限定到已确认Forge来源。必须单独验证host/range/side/ordering，保留原声明、决策，承认FML列表发生转换 | 选作受控实现；最终是否VERIFIED取决于Contract/真实同JAR回归 |
| B 兼容平台对象/提供者 | 官方Sorter没有按消费者限定的provides接口，只识别实际ModInfo。SPI能产出真实ModFile/多mod metadata，但全局forge身份会影响原生模组、引入容器及GAME资源。现DualForge为SERVICE library，不能凭追加metadata假设GAME层/生命周期、无split-package已证明。需额外独立架构验证 | 不实施。未验证生命周期/Layer/split-package；源码已证明其全局ID作用域与本轮隔离目标不符。不能以空forge容器冒充完整平台 |
| C 严格拒绝 | 保留007C-1行为，风险最低，不能推进新阶段 | 作为失败回退/停止结果；若A不能维持语义和严格回归，则交付PARTIAL/DESIGN BLOCKED |

A只校验**来源平台依赖版本条件**，不承诺API兼容。FnF的FMLJavaModLoadingContext、跨类注册、Tab、Fuel和SRG仍未支持；必须允许其在下一实际阶段明确失败。不能把“来源版本接受”命名为“Forge运行时已完整提供”。

拟定边界：确认Forge metadata/同JAR @Mod；宿主精确MC1.21.1、NeoForge21.1.256、FML4.0.45、Java21；active side的forge required/optional按52.1.0参考ABI验证（optional对已声明提供的参考能力同样检查range）；仅NONE排序可投影，BEFORE/AFTER明确拒绝，因为没有实际forge排序节点。inactive side保留给FML原规则；其他依赖原id/range/type/side/ordering不变。版本和ordering不满足时不返回部分结果。

这是一项经前置检查的内部表示转换：成功active forge条目不再位于FML getDependencies列表，保留到独立审计config及原依赖快照。原JAR未改不代表内存语义未变。严禁无条件删除所有forge、forge→neoforge、假Forge版本/容器、FML override、核心字节码或JVM参数实现。
