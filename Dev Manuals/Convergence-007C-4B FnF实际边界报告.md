# Convergence-007C-4B FnF实际边界报告

## 原始输入与本轮只读结论

原始官方 `fnf-forge-1.3-1.21.x.jar`：128098 bytes，SHA-256 `bd415b04529c4ffc31e14757488f71dab6917e4560c61e236c3c4d0de6557958`。读取前后字节完全一致。

使用最终新 DualForge JAR、真实 Reader / metadata / ScanResult，但不定义/执行 FnF 类。FoodNFuel 仍属于原 C3 Context Profile；入口含 MinecraftForge 全局 Bus，故不作为新 Helper 运行时候选。独立只读强制尝试新计划时明确 **REJECTED：SUPPLIER_BUSINESS_UNSUPPORTED**。没有发布 FnF 的部分 Item/helper Targets。

实际发行字节码盘点：ItemRegistries 有67个 invokedynamic，FnFCreativeModeTabs 有3个；逐项 BSM、参数、Handle 和字段 descriptor 已保存。计数不等于这70个调用点均已适配。完整闭包仍不成立，依据 Gate 8 **FNF_RUNTIME = NOT RUN**。

## 沿用的真实运行边界

C3 已观察到 `FoodNFuel.<init> → ItemRegistries.<clinit> → NoClassDefFoundError: net/minecraftforge/registries/ForgeRegistries`。这是历史继承的首异常，不是4B重跑。本轮没有声称越过该位置、完成 FnF Mod Bus attachment、67 Item 注册、Tab、全局事件总线、Food/Fuel 或客户端。

C3 对 FnF 的旧 Context-only Target / 两调用点契约继续作为严格历史 Contract 回归；新 Registry 闭包拒绝与保留旧行为分开报告。007C-2矩阵只在显式 `--skip-fnf-runtime` 时跳过 N2（NOT RUN），其余18案例原断言不变；默认历史命令保持原行为。跳过不被计为第19项 PASS。

C4A整体 PARTIAL（缺少完整 patched client 等）保持原结论，未改写任何冻结报告。

## 4C 交接建议（本轮不实施）

1. 逐个分析67 Supplier 的真实业务与 SRG 调用，尤其 Food 属性、FuelItem 自定义构造/继承；先形成可验证数据流和失败原子性规则，不能将普通 Item Supplier 扩成任意业务白名单。
2. 完成 Tab Holder Consumer、3个 Bootstrap 及注册事件的整体闭包；独立证明错误调用会整组拒绝，再决定扩展现有 helper Profile 或新增独立 Profile。
3. 对 MinecraftForge 全局 Bus 单独研究事件语义桥接；本轮 Mod Bus 注册桥接不提供全局 register/post/listener 兼容。
4. 在原始 FnF 全组准入后再进行标准运行，记录下一个真实异常及是否越过官方 Registry attachment；不能沿用静态推测。
5. Food/Fuel 行为、Tab显示、贴图、语言、客户端和世界持久化均另设实际验收；本轮全部 NOT RUN。
