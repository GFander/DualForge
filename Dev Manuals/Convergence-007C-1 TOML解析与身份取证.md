# Convergence-007C-1 TOML解析与身份取证

本轮只修复加载前置机制。标准TOML解析、Forge身份和准入为PASS；FnF功能没有因此获得PASS。完整状态、路径和SHA见[交付报告](<Convergence-007C-1 交付报告.md>)及[索引](<Convergence-007C-1 证据索引.json>)。

## 原始输入与实际旧失败

原始Food N' Fuel `fnf-forge-1.3-1.21.x.jar`为128,098字节，SHA-256 `bd415b04529c4ffc31e14757488f71dab6917e4560c61e236c3c4d0de6557958`；完整成员、Manifest、TOML均与007C冻结索引匹配。没有重新编译、改写或重新打包。

本轮Gate0标准NeoForge重跑安装旧compat `36d84c40e1a10eea7921ed31c967af197b7b43cabf65937d75f27132ec67ac44`与原始FnF。原日志确认Reader到达、宿主Skipping Forge JAR、`give @p fnf:logo 1`返回Unknown item，随后Done/stop/JVM exit0。旧Reader未输出布尔返回值；“guard返回false”由这条实际链与冻结源码中的guard/三引号拒绝位置共同解释，不能冒充独立打印记录。

## NightConfig运行层

测试专用Reader SPI通过实际 `ForgeModReader.class.getClassLoader()`解析并调用NightConfig：`readerModule=dualforge`、`parserModule=com.electronwill.nightconfig.toml`、`canRead=true`。FnF完整原始TOML成功解析，description与原生Forge值一致。使用官方标准启动、原默认argfile，无测试classpath、额外JVM opens或用户新增解析库。该诊断运行比交付输入多一个测试SPI JAR，不作为双JAR功能验收。

生产编译新增宿主已有NightConfig Core/TOML 3.8.3两个编译输入，固定编译集合从8增至10；制品不打包这些依赖。最终双JAR标准运行也实际调用新Parser成功创建真实FML元数据，证明生产路径可用。

## 解析契约与差异

`ForgeModFileInfoParser`：NightConfig语法解析 → 深拷贝Config/list为Map/list → 验证Forge字段/表/类型/版本范围/归属 → 加入确定的FML依赖type。所有原始字段保留；mandatory true/false映射required/optional，不改modId、versionRange、ordering、side。类型冲突、未知依赖约束和未知owner明确拒绝。服务/boolean、modproperties/features等宿主消费字段先检查结构，避免静默丢弃或ClassCastException。

Locator与Parser复用同一解析/验证方法、1MiB上限和严格UTF-8解码；没有路径间缓存。支持标准字符串、真多行字符串、行尾注释、数组表及嵌套表；不做字符串替换预处理。

## 身份与准入契约

`ForgeModLocator`内部提供FORGE_CONFIRMED、NEOFORGE_NATIVE、AMBIGUOUS、INVALID_METADATA、UNSUPPORTED及原因。现有Reader/LanguageLoader仍调用共享guard；不新增加载架构。

当前受控来源证据为mods.toml、javafml、同JAR实际Forge `@Mod.value`覆盖声明modId、没有归属相冲突的NeoForge注解或required NeoForge依赖。ASM只读类注解，不加载/初始化第三方类，也不以普通Forge包引用识别来源。类文件路径须与internal name相同；单class扫描上限8MiB。Manifest显式非MOD留给原Reader。原生neoforge.mods.toml不接管；兼有Forge注解的native payload诊断AMBIGUOUS并拒绝Forge交接，仍保留官方native Reader边界。没有原生排他metadata的真实身份冲突显式停止。

原冻结受控输入没有required forge声明，仍有同JAR实际Forge入口这一强证据；没有强制新依赖字段或身份白名单。来源确认后，loader范围仍须包含当前支持Forge52 ABI；取消包含式52下界及排除Neo4的来源启发式。`[0,)`本身不能证明来源。实际Minecraft/依赖/provider版本由FML处理；不保证任意Forge版本或构造器。

## 实际Provider选择

测试观察器不提供mod、语言provider、依赖override或变换target，仅委托生产Reader并读取FML赋值。实际枚举builtin javafml 4.0.45在前、DualForge javafml 9999在后，原始FnF `[0,)`的assigned loader为DualForgeLanguageLoader/9999。官方LanguageProviderLoader使用name-keyed map、后写覆盖、随后验range；9999不是显式priority规则。本证据仅覆盖本宿主本次顺序，不宣称跨宿主稳定优先级。

## 测试与历史迁移

最终同一新compat通过91条metadata/admission断言：完整FnF/007B/006A/006B/Clumps TOML，真实FML元数据与入口、原依赖/语言range，native隔离，字符串/嵌套表，以及非法语法、重复、类型、缺字段、UTF-8、超大metadata、身份/依赖冲突。非接管class保存ASM canonical before/after字节并逐一断言相等；static initializer设计为执行即失败，扫描没有执行它。

Clumps只静态解析，未启动功能适配。007B原13严格拒绝、显式JPMS负例及调用者ClassNode原子性保持通过。003a/003b生成对照的必要迁移说明见[契约README](../tests/real_mods/food_n_fuel/metadata/README.md)：真实归属C、真实身份歧义、外国native注解对照；不删除Java语义断言，不修改冻结Probe或旧fixture。
