# Convergence-003a：Forge JAR 发现路径通用化

## 1. 原固定文件名限制及根因

本轮最终 PASS。原 Locator 直接使用 `Paths.get("mods", "forgeprobe.jar")`，未读取配置的mods目录、未检查JAR内容，故不同叶名不进入该Locator。另一个已证实缺陷是相对Path与FML默认ModsFolderLocator的绝对Path不相等：安装版LaunchContext.addLocated只解开UnionFS，并不将普通相对路径规范化。旧机制有重复发现风险。

安装版FML4.0.45源码从官方Maven取得，SHA-256 `232DB58B2B6AC1BF3834E659FB156E8943B42CC2BC18FF8201FFB404D559C952`，来源与原始源码在 `/workspace/dualforge-cloud/convergence-003a/loader-sources.jar` 和 `contract/`。

契约：IModFileCandidateLocator.findCandidates仅发现候选；IDiscoveryPipeline.addPath负责context.addLocated、创建JarContents、按优先级调用Reader、以实际ModFile进入loadedFiles。provider按priority降序排序。本Locator/Reader为100，原生默认provider为0。官方JarModsDotTomlModFileReader优先读取 `META-INF/neoforge.mods.toml`，没有该元数据时可按FMLModType读取显式库。

调用时机与Layer保持原样：ModDirTransformerDiscoverer先将含SPI声明的compat JAR放入SERVICE Layer；ModDiscoverer再加载Locator/Reader。提交候选仍调用addPath，不直接造自定义IModFile、不改ModuleDescriptor、不改Layer。ForgeJarMetadata仍使用原SecureJar通用metadata委托。当前回归mods副本只有新compat JAR与原Probe，原服务器输入目录未安装mods。其他官方来源为MavenDirectoryLocator/上下文modLists、mods、mavenRoots，以及后续JarInJar依赖发现；本任务不扩展这些来源。Native核心来自原官方server runtime，动态日志证实仍原生加载。

## 2. 新候选识别规则

扫描 `FMLPaths.MODSDIR.get()` 下的普通磁盘JAR；后缀仅作为容器筛选，按文件名稳定排序，并提交绝对规范化Path。已由context标记的路径跳过；不自行addLocated，交由官方pipeline维护去重。

内容判据：可读ZIP；没有 `META-INF/neoforge.mods.toml`；具有Forge `META-INF/mods.toml`；显式FMLModType若存在必须为MOD，其他类型保留原生库路径；Forge元数据最大1MiB、有效UTF-8。仅支持现有Parser的简单逐行标量子集，排除多行字符串。要求顶层双引号 `modLoader="javafml"`、`loaderVersion` 及至少一个[[mods]]；沿用原Parser检查依赖声明，不改变其行为。loaderVersion使用原MavenVersionAdapter：一个范围、含52、下界为52且包含下界，排除当前Neo javafml4.0.45。若范围同时接受Forge52与Neo4.0.45，则明确抛出Ambiguous诊断，停止交接，绝不猜测身份。

原生元数据存在时留给原生Reader，即使同时有mods.toml也不接管。普通库不进ForgeReader；非法ZIP由本Locator只读诊断并跳过，由FML默认pipeline产生明确invalidzip ERROR（只在独立负例测试中出现）。实际metadata字段验证及IModFile创建继续由未改Parser和Reader既有流程承担。

必须给Reader复用同一前置判据：默认ModsFolder/Maven/其他Locator也会调用高优先级ForgeReader，单独过滤ForgeLocator无法阻止这些路径误接管任意mods.toml。Reader仅新增此两行guard；正式的读取/构造流程不变。

## 3. 修改文件与差异

- `spike001a/compat-002-0l/src/main/java/com/example/forgecompat/ForgeModLocator.java`：去除固定路径，使用配置mods目录、稳定扫描、内容判据、绝对路径交接及明确歧义诊断。
- 同目录 `ForgeModReader.java`：最小共享身份前置检查，覆盖其他Locator进入Reader的路径。
- `tests/discovery/DiscoveryContractTest.java`：实际安装版FML DiscoveryPipeline/Reader/ModFileInfo契约断言。
- `tests/discovery/run-discovery.zsh`：可复核的独立A–F及边界fixture准备、干净测试编译、原始命令/stdout/stderr/exit/input指纹记录。测试专属cwd隔离日志。

禁止修改的Parser、LanguageLoader、ASM Transformer、构造ABI、模块层、事件总线、Registry/Fabric均与本轮任务起点一致。工作区原有上轮dump I/O适配不属于本轮变动。新旧基线JAR仅Locator.class/Reader.class变化，另15件内容字节一致。证据：`/workspace/dualforge-cloud/convergence-003a/scope-proof.json` 与 `workspace-before.json`。

## 4. A–F 测试结果

| 候选 | 实际结果 |
|---|---|
| A 原固定Probe | PASS；真实pipeline→ForgeReader→ModFileInfo，modId=forgeprobe |
| B A的不同文件名原字节副本 | PASS；arbitrary-renamed-input.jar不受固定文件名限制，modId不变；与A独立运行，非同时安装 |
| C 第二受控Forge元数据JAR | PASS；modId=controlled_candidate，经过真实Parser/FML metadata；不含入口class，不宣称已构造 |
| D 官方NeoForge原生JAR | PASS；Locator/ForgeReader均不接管，官方native Reader识别neoforge |
| E 普通库JAR | PASS；不接管、不当作Forge mod；官方路径警告属于非模组预期结果 |
| F 无效JAR | PASS（预期安全拒绝）；Locator不提交、FML产生明确invalidzip ERROR，无loadedFile |

额外边界也通过：native-dual保留原生Reader所有权；缺失/错误loader不接管；两个不同modId候选按a-candidate.jar、z-probe.jar稳定交接；默认Locator后扫及重复调用自Locator均不重复；同时接受Forge/Neo的[1,)范围明确拒绝并停止交接。

测试使用class path下通过反射实例化安装版FML私有DiscoveryPipeline，不替代其实现；stub只提供ILaunchContext服务/located状态。没有构建游戏Module Layer或实例化C，亦不将该测试称为多候选完整启动。已另以真实命名模块服务器回归证明A及Native宿主。

最终结果、聚合断言、fixtures/input哈希及完整原始输出：`/workspace/DualForge/out/convergence-003a/discovery/20261012-102332-8b0007b3/result.json`、`/workspace/DualForge/out/convergence-003a/discovery/20261012-102332-8b0007b3/fixtures.json`、`/workspace/DualForge/out/convergence-003a/discovery/20261012-102332-8b0007b3/inputs.json`、`/workspace/DualForge/out/convergence-003a/discovery/20261012-102332-8b0007b3/discovery.command.json`、`/workspace/DualForge/out/convergence-003a/discovery/20261012-102332-8b0007b3/discovery.stdout.log`、`/workspace/DualForge/out/convergence-003a/discovery/20261012-102332-8b0007b3/discovery.stderr.log`、`/workspace/DualForge/out/convergence-003a/discovery/20261012-102332-8b0007b3/discovery.exit.json`。重跑入口：`zsh tests/discovery/run-discovery.zsh <新compat.jar> /workspace/dualforge-cloud/server-base /workspace/dualforge-cloud/inputs/forgeprobe.jar`。

## 5. 原加载及依赖回归结果

统一Unix入口执行mode regress，10源全量干净构建＋原固定Probe隔离smoke PASS。所有原严格断言通过：服务发现指向新制品、构造marker、真实register返回、javafml委托、NeoForge21.1.256原生加载、Done/正常stop/exit0、无ERROR/FATAL/dump FAIL、stderr空、无crash。每轮实际日志中的Probe发现次数均为1。

Convergence-002现有六组metadata、非法mandatory/type输入与默认值 PASS；严格串行C/D/E/F动态回归4/4 PASS：C/E正常加载并停止；D/F核验具体缺依赖/版本不符原因、pre-loading拒绝、目标实例化缺席及无Done。D/F JVM exit0不是PASS依据。没有放宽断言。

原始证据：`/workspace/DualForge/out/convergence-003a/regress/20261012-101746-994c9b64/result.json`、`/workspace/DualForge/out/convergence-003a/regress/20261012-101746-994c9b64/assertions.json`、`/workspace/DualForge/out/convergence-003a/regress/20261012-101746-994c9b64/build-inputs.json`、`/workspace/DualForge/out/convergence-003a/regress/20261012-101746-994c9b64/jar-contents.json`、`/workspace/DualForge/out/convergence-003a/regress/20261012-101746-994c9b64/logs/spike002-0l-treatment/`；`/workspace/DualForge/out/convergence-003a/serial-dependencies/20261012-102151-f48062de/result.json`、各case的fixture/assertions/meta/raw logs、boundary.stdout.log。所有案例只操作物理副本；用户上传原世界22文件的集合与SHA-256不变，固定Probe和官方输入指纹不变，正式历史源码/证据未覆盖。Java全部退出，25565可绑定。

执行方法披露：首轮依赖回归PASS期间独立metadata JVM与C启动短暂重叠，不将该轮作为严格隔离最终证据；保留在 `/workspace/DualForge/out/convergence-003a/dependencies/20261012-101924-1eb1d306`。随后完整单独串行重跑PASS，最终采用/workspace/DualForge/out/convergence-003a/serial-dependencies/20261012-102151-f48062de。初期metadata测试写入了仓库根目录临时logs/latest.log；该文件任务起点为空，诊断已归档、原空字节已恢复，最终runner使用专属cwd。全部证据索引：`/workspace/dualforge-cloud/convergence-003a/summary.json`。

## 6. 尚未支持的候选类型

不支持其他loader、非52下界范围、无法区分Forge/Neo的范围、多范围声明、完整TOML语法（多行/复杂字段等）、超过受控metadata大小、目录形式及虚拟/嵌套Forge JAR；这些不作为本轮通用兼容承诺。JarInJar和额外扫描来源未扩展，原生路径保留。真实多Forge候选的完整Module Layer、C构造器ABI、任意Forge模组入口/API/事件/Registry/Fabric均未验证或实现。

003a成功标准已满足，停止，等待用户裁决；未进入003b。
