# Convergence-007C-4A 交付报告

日期：2026-10-15（Asia/Shanghai）。**整体PARTIAL：可用固定服务端输入的图谱/候选/FnF静态分析与受控离线验证已完成；完整客户端补丁输入缺失，完整Registry链仍需运行时桥接。** 不扩大生产支持，不声称FnF可用。

## Git与范围

分支`codex/convergence-007c4a-api-atlas`，独立[Draft PR #22](https://github.com/GFander/DualForge/pull/22)。起点`29893683ea584b26208eb205b819c3f3376147be`，PR20/21开始及推送前均未合并，因此base为`codex/convergence-007c3-entry-context`，不是main。先fetch默认refspec，随后显式fetch全部远端heads核对；最新已取得main为`acf363b84ee1efbce15c98c8e77a3dab693251ca`。前置C2/C3变化不计本轮新增。

工具/测试提交及实际被测源码`25571c40671d6d80ca10a4dcec77db84440ff555`，之后只有文档/输入清单/统计与索引。最终文档HEAD、推送远端相等性及可用Actions结果将在PR正文和git-finalization.json实际回读后记录，不把被测源码当作最终HEAD。

只新增`tools/api-atlas`独立Java/ASM+Python工具、`tests/api_atlas`测试、Schema/候选seed和直接导航；现有CI只加该Python单测目录，未改变保护/权限/触发策略。生产21份文件及所有374项冻结保护不变；没有修改DualForgeEntryTransformer、Profiles、Shim、Module Layer、原始FnF、宿主配置或原测试断言。没有启动游戏服务器、客户端、Release或4B实现。

## 研究与实际验证

- 184项实际输入（183安装JAR+实际Java21 base JMOD）逐大小/SHA/inventory复核；250项官方安装器/Bootstrap checksum匹配。Forge在线checksum403如实记录，未称联网核验成功。
- 105708条ClassFile记录两轮完整重扫，raw SHA一致；15份规范化输出逐字节相同。全量内部和公共声明、flags/Signature/注解/Record/Nest/Module/Permit均保留，第三方/JDK与平台API分开。
- Forge平台1603类/4388字段/11444方法；NeoForge平台1925类/4874字段/13155方法。17435个来源符号；390个歧义符号保留所有候选，8480未解析符号不等于证明不存在目标；单一待审候选8565。所有候选未启用，通用runtime verified=0。
- 服务端MC Forge6482类、NeoForge6528类，629类声明ABI不同；与原版服务端基线的807/963类差异另存。1171项NeoForge补丁覆盖有明确提供者；活动类来源无未解冲突。完整客户端patched二进制缺失，停止该部分分析，MC全量和整体PARTIAL。
- 原FnF五类完整静态扫描：4030外部使用点、92唯一符号，含1个override契约；576个Forge点/16个唯一Forge符号、70个indy。所有引用明确分类，反射/动态盲区独立。2点仅属C3已验证Context调用对。
- 盘点既有16规则，精确source/target ABI与原Contract、entry/helper范围对应。只读取7份源码与旧最终JAR，不继承为通用helper映射。
- 受控原生Forge编译fixture跨类/字段/方法/泛型闭包离线PASS；38断言/15拒绝和9项scanner元数据断言PASS，原ClassNode/JAR不污染，普通/native不误改。独立java -Xverify:all真正加载fixture，未运行Registry/事件/游戏。完整FnF离线拒绝，无输出。
- 63个Python测试、58份Python AST、25个zsh语法、374冻结保护、21份生产SHA和原FnF/官方输入SHA保全PASS。全部JVM串行。历史游戏回归只记INHERITED，不写RERUN PASS。

## 文档与数据

1. [官方输入与ABI清单](<Convergence-007C-4A 官方API输入与ABI清单.md>)。
2. [平台与Minecraft差异](<Convergence-007C-4A Forge-NeoForge API差异报告.md>)。
3. [FnF全量使用图谱及命名勘误](<Convergence-007C-4A Food N Fuel API使用图谱.md>)。
4. [规则、外部项目与语义风险](<Convergence-007C-4A 转换规则与语义风险评审.md>)。
5. [离线原型与正负例](<Convergence-007C-4A 离线ASM原型验证.md>)。
6. [逐文件证据索引](<Convergence-007C-4A 证据索引.json>)，以及[便携输入Manifest](../tests/api_atlas/official-inputs.json)、[统计摘要](../tests/api_atlas/summary.json)、[工具命令](../tools/api-atlas/README.md)、[测试命令](../tests/api_atlas/README.md)。

完整生成数据在`/workspace/DualForge-007C4A/out/convergence-007c4a/final/`。各命令的argv、stdout/stderr/exit、编译输入和断言都保留；final-driver.py精确串行调度，当前源码/原输入与所有最终输出绑定。正则只用于命名文件/安全模式，不用于ClassFile解析。

## 历史事实与限制

本轮原始固定SHA FnF的新javap/ASM为Mojang成员名，m_/f_引用数为0；旧现场javap也如此。旧报告的SRG描述在本轮勘误，冻结报告/指纹保持原样，不把错误历史描述变成转换需求。

当前生产的FnF真实失败继续继承007C-3：`ItemRegistries.<clinit> → NoClassDefFoundError: net/minecraftforge/registries/ForgeRegistries`，本轮未运行该异常。离线原型从未适配的原JAR开始，先因缺少其独立Context/Bus转换许可报RUNTIME_SHIM_REQUIRED，这不是新的游戏首异常。生产C3受控Context已存在；它返回的来源视图不是原生NeoForge Bus，仍无一般解包API。Registry/CreativeTab/Supplier全闭包没有实现，事件/Food/Fuel/客户端行为仍非本轮PASS。

## 证据归档

ZIP：`/workspace/DualForge-007C4A/out/convergence-007c4a/evidence-export-final/convergence-007c4a-api-atlas-evidence.zip`，63082456字节，328项payload加3项控制文件。SHA-256：`3f985750fa0aa6cad5e72284330ee5c69a12255040dac3395f0441f22dd1faf1`。

该ZIP包含完整最终可用范围数据库和所选原始命令、结果、断言、原class/改写class、javap、固定项目研究来源及保全记录；独立raw ClassFile元数据不含第三方运行JAR。两个重复final扫描/atlas只留一份，另一份通过实际重复生成SHA逐项绑定；开发中间数据库、绝大多数非最终尝试和无关旧工作区排除。小型新第一方fixture JAR只是离线证据，不是原FnF或依赖。

CRC、Manifest/逐SHA、全新临时目录实际解压、331文件计数、无symlink/路径穿越和原文件归档前后SHA均PASS。安全模式无token/private-key/授权Header命中，但没有声称对所有压缩API常量完成公开内容审查；工作区路径/临时机器身份仍保留，不作脱敏原件声明。**ARCHIVE_READY / NOT PERSISTED**：未上传、未远端下载，无持久URL。原始本地路径是重放定位，不是远端下载链接。最终Git/CI/文档回执在ZIP之后单独记录。早期归档草稿仅README夹具数量措辞有误，以本final包为准，草稿保留不覆盖。

## 007C-4B Implementation Candidates（建议，未实施）

1. **Required production modules**：在现有FML归属和Profile架构内增加受控helper Registry计划与原子提交；保留C3 Context，不另建加载器或复制Atlas为生产实现。
2. **Verified mapping rules**：当前只有Item field→BuiltInRegistries.ITEM、create(Registry,String)、Holder.get及Item-typed返回闭包的限定离线证据。register(String,Supplier)只核对了target存在，仍需新的官方事件动态验证；全部候选不能批量上线。
3. **Required runtime bridges**：研究按真实mod container/scan归属验证的source BusView→native ModBus桥接；不能添加任意反射解包或把两个Bus接口视为相同。现有C3两个Context调用和空事件规则只能在各自契约内复用。
4. **Required helper-class scope**：先受控fixture的一组owned helper，再研究真实ItemRegistries、全部引用其Holder的消费者（包括FnFCreativeModeTabs）及BSM。类名仅为取证示例，不作白名单；未证明闭包不得输出局部生产改写。
5. **Bytecode dependency closure**：字段/声明/调用/返回/CHECKCAST/Signature/Handle/indy逐点一致；67个原Supplier业务不删改，3个Tab indy不得静默忽略。若Tab消费者无法纳入一致闭包，则停在受控fixture，不宣称FnF推进。
6. **Target discovery strategy**：实际Reader确认的同JAR Forge metadata和所属scan，建立可审计依赖边；无modId、JAR名、类名或SHA生产匹配。native/外来/未识别API明确拒绝。
7. **Registry lifecycle assumptions**：真实FML ModBus、官方RegisterEvent及原始静态初始化/挂接顺序。API符号存在不能代替事件顺序/对象身份/Supplier次数证明；Tab/Food/Fuel行为独立验收。
8. **Negative contracts**：延续全部旧拒绝；增加Bus归属/逃逸、缺consumer、Handle/indy闭包、错误泛型/Registry、重复ID、部分失败/原子性、原生与JPMS隔离。不得吞掉旧Profile失败或删除调用。
9. **Expected first runtime test**：独立原生Forge helper+caller fixture先原Forge，再同JAR的NeoForge C/T，查真实注册/次数/Holder/Bus/官方栈。通过后再标准双JAR原FnF，只报告实际下一异常；不能预写FnF成功或删除Tab/全局bus路径。
10. **Known unsupported cases**：一般EventBus、Networking、Capability、动态反射/类生成、未知Bootstrap、客户端未供给的patched ABI，以及未经验证的Tab/Food/Fuel完整语义。SRG是一般未来命名研究，本固定FnF未观察到该需求。

本轮只提供该交接，不自动开始4B，不合并前置或当前PR，不创建Release。

## 最终状态

```text
OFFICIAL_INPUTS: PARTIAL
FORGE_ABI_INVENTORY: PASS (available official platform artifacts)
NEOFORGE_ABI_INVENTORY: PASS (available official platform artifacts)
MINECRAFT_PATCH_ABI: PARTIAL
API_DIFF_DATABASE: PARTIAL
MAPPING_CANDIDATES: PASS (discovery only)
AMBIGUITY_REPORT: PASS
FNF_STATIC_REFERENCE_INVENTORY: PASS
FNF_COVERAGE_REPORT: PASS
EXISTING_DUALFORGE_RULES: PASS
OFFLINE_ASM_PROTOTYPE: PARTIAL (controlled fixture output PASS; full Registry chain incomplete)
BYTECODE_VERIFICATION: PASS (ASM + standalone fixture JVM)
PRODUCTION_CODE_UNCHANGED: PASS
ORIGINAL_FNF_UNCHANGED: PASS
HISTORICAL_RUNTIME_REGRESSION: INHERITED
FNF_RUNTIME: FAIL (inherited, not rerun)
OVERALL: PARTIAL
```

可用范围的静态图谱与fixture字节码验证PASS，不等于全平台兼容、FnF启动或候选获生产批准。客户端输入、完整Registry/Bus闭包是剩余阻塞。
