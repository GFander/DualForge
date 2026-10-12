# Convergence-002：Forge 依赖元数据保真修复

2026-10-12；结果：**PASS**。仅修复已复现的依赖读取／适配缺陷，完成后停止。

## 1. 缺陷复现及实际根因

`[E]` 修改前六组 A–F 输入均有依赖声明，但配置适配器与真实 FML `ModInfo` 读取数量均为 0。原 Parser 输出 `dependencies: List<Map>`，FML 查询 `getConfigList("dependencies", modId)` 时无法从 List 继续按 owner 遍历，因此静默返回空列表。两个 owner 同样各读到 0。

证据：[修改前边界日志](D:/DualForge/out/convergence-002/before/20261012-133547-b0d58c37/boundary.stdout.log:3)。原生产源码全量保存在 `out/convergence-002/source-before`，原始 fixture 输入保存在 `out/convergence-002/fixture-before`，原始输入指纹和命令在各轮 `inputs.json`、`*.command.json`。

`[E]` 测试内存中仅修 owner 分组、不修改生产代码后，`mandatory=false` 被真实 FML 读为 REQUIRED：[同日志](D:/DualForge/out/convergence-002/before/20261012-133547-b0d58c37/boundary.stdout.log:13)。这是第二个必须一并处理的适配问题，不能只修结构。

`[E]` 修改前动态 D 反例声明 `convergence_missing` 为必需依赖，却仍执行目标构造器、register，并 `DONE_STOPPED_GRACEFULLY / exit=0 / 22.6s`。这是错误放行，不是修复后的 PASS：[修改前动态结果](D:/DualForge/out/convergence-002/before/20261012-133547-b0d58c37/result.json)。fixture 只替换固定 Probe 的 TOML，所有 class 指纹不变。

取证过程中保留了非结论性失败：独立 metadata JVM 最初缺少 SecureJar 在 classpath 下所需的 opens；首次动态 fixture 的 .NET ZIP 更新产物被 Java JarInputStream 拒绝；两个局部断言先后误以为占位 URL 不被过滤、版本范围字符串不被正规化。已分别修正测试 JVM参数／改用 JDK jar／选择有效 URL／按 MavenVersionAdapter 正规化结果比较，**未为这些测试问题修改生产实现**。原失败日志保留在 `boundary-before/result.log`、`before/20261012-133421-fe46f24f`、`after-boundary/20261012-133750-acf6e56a`、`after-boundary/20261012-133847-d2affe15`。

## 2. FML 契约证据

当前安装 `loader-4.0.45.jar` SHA-256：`8390398E67DF23E72D85ED809685BE2752C8EC545B7CBB6AD4B70FDC81E4DCD4`。使用固定 Microsoft JDK21.0.10 的 `javap -p -c -constants -classpath <该JAR> <类名>` 重新反汇编，结果在 `out/convergence-002/contract/*.javap.txt`。从 [NeoForge 官方 Maven](https://maven.neoforged.net/releases/net/neoforged/fancymodloader/loader/4.0.45/loader-4.0.45-sources.jar) 取得同版本源码附件用于交叉核对，SHA-256 `232DB58B2B6AC1BF3834E659FB156E8943B42CC2BC18FF8201FFB404D559C952`；只归档源码，不安装或更换依赖。

| 已核对契约 | 直接证据 |
|---|---|
| `[F]` `ModInfo` 从文件级配置按 dependencies、当前 modId 两段路径读取 | [ModInfo.java:97](D:/DualForge/out/convergence-002/contract/ModInfo.java:97)，[实际字节码](D:/DualForge/out/convergence-002/contract/ModInfo.javap.txt:523) |
| `[F]` 配置必须是 owner 嵌套对象，末端为集合；不存在的路径返回空集合 | [NightConfigWrapper.java:52](D:/DualForge/out/convergence-002/contract/NightConfigWrapper.java:52)；当前 MapConfigurable 同样逐层 Map 遍历、末端 List 包装 |
| `[F]` FML 读取 type 字符串，转大写枚举，缺省 REQUIRED；不读取 mandatory | [ModInfo.java:223](D:/DualForge/out/convergence-002/contract/ModInfo.java:223)，[ModVersion 字节码](D:/DualForge/out/convergence-002/contract/ModInfo$ModVersion.javap.txt:45) |
| `[F]` REQUIRED：不存在或版本不符即错误；OPTIONAL：不存在不报错，存在但版本不符仍报错 | [ModSorter.java:279](D:/DualForge/out/convergence-002/contract/ModSorter.java:279) |
| `[F]` 先在 stage2Validation 中排序／校验，再由 ModLoader 在 pre-loading 发现 errors 后取消；未到 buildMods/constructMods | [ModValidator.java:96](D:/DualForge/out/convergence-002/contract/ModValidator.java:96)，[ModLoader.java:104](D:/DualForge/out/convergence-002/contract/ModLoader.java:104) |

字段契约：modId 为必填 String；type 为上述枚举字符串；versionRange 为 Maven 范围字符串，缺省 UNBOUNDED；ordering 为 NONE/BEFORE/AFTER 字符串，缺省 NONE；side 为 BOTH/CLIENT/SERVER 字符串，缺省 BOTH；reason、referralUrl 为可选 String。本轮实际 metadata 测试确认默认范围可接受99.0、ordering=NONE、side=BOTH。

Forge 与 NeoForge 不直接等同字段名：Forge 1.21.1 官方 [ModInfo 源码](https://raw.githubusercontent.com/MinecraftForge/MinecraftForge/1.21.1/fmlloader/src/main/java/net/minecraftforge/fml/loading/moddiscovery/ModInfo.java) 读取 Boolean mandatory；[ModSorter 源码](https://raw.githubusercontent.com/MinecraftForge/MinecraftForge/1.21.1/fmlloader/src/main/java/net/minecraftforge/fml/loading/ModSorter.java) 对“必需或已安装”的依赖检查版本。`[D]` 因而在本次支持的合法 Forge 子集中将 true→REQUIRED、false→OPTIONAL，同时保留 mandatory 本身及其他字段；这是明确适配，不是未经核验的字段重命名，也不宣称两平台所有依赖语义完全等价。

## 3. 修改文件与关键差异

唯一生产修改：[ForgeModFileInfoParser.java](D:/DualForge/spike001a/compat-002-0l/src/main/java/com/example/forgecompat/ForgeModFileInfoParser.java:69)。

- dependencies 改为 `Map<owner, List<Map>>`，`computeIfAbsent` 追加交错／重复 owner 的条目；移除原扫描器自行添加的虚假 owner 字段，owner 由表路径承载。
- mandatory 保留为真正 Boolean；生成 FML 所需 type。合法输入的 modId、versionRange、ordering、side、referralUrl、自定义字段均保留。
- 缺失／非布尔／带引号的 mandatory 明确拒绝，不静默变成必需或可选；混入 NeoForge type 字段也明确拒绝，避免覆盖相互矛盾的声明。

新增测试文件：[DependencyContractTest.java](D:/DualForge/tests/dependencies/DependencyContractTest.java)、`tests/dependencies/A-single.toml`、`B-owners.toml`、`C-satisfied.toml`、`D-missing-required.toml`、`E-missing-optional.toml`、`F-wrong-version.toml`；新增 [独立测试入口](D:/DualForge/tools/test-forge-dependencies.ps1)。共1个生产文件修改、8个测试文件新增；报告和生成证据另存 out。按 Karpathy 指南保持局部改动，不引入第三方解析框架或新架构。

配置适配器、Locator、Reader、LanguageLoader、ASM、模块层、原 harness、统一构建／smoke 脚本均未修改。新 JAR 的17个内容文件中，**仅 Parser.class 变化，其他16个逐字节不变**：[构建对照](D:/DualForge/out/convergence-002/after-build/20261012-133718-492348e8/historical-content-comparison.json)。生产源码15文件指纹对照同样仅 Parser 变化：[最终保全记录](D:/DualForge/out/convergence-002/final-preservation.json)。

## 4. 依赖正负例结果

所有结构测试均使用真实 FML `ModFileInfo → ModInfo`，不是自写依赖验证器。动态 fixture 沿用固定 Probe 两个 class、原 JVM开关、原命名模块启动文件；仅 TOML 与测试 JAR 的容器时间戳改变。所有安装哈希、class 指纹、输入、命令及完整日志已保存。

| 输入 | 结构／FML metadata | 实际加载结果 |
|---|---|---|
| A 单 owner 必需依赖 | 1/1；REQUIRED；全部字段及 URL/自定义字段保留 | 结构验证 PASS；不单独重复动态 C |
| B 两个 owner、交错表 | forgeprobe 2/2，otherprobe 1/1；目标分别 minecraft+missing / neoforge；依赖 owner 对象同一性正确 | metadata 验证 PASS；不宣称已动态测试双入口模组 |
| C minecraft `[1.21.1]` 必需 | 1/1；REQUIRED；满足范围 | PASS；构造、register、NeoForge加载、Done、正常停止，22.7s |
| D 缺失必需 | 1/1；REQUIRED | PASS（预期拒绝）；明确 missingdependency 原因，目标 loadMod/构造/register 均未出现，10.0s |
| E 缺失可选 | 1/1；OPTIONAL | PASS；debug 明确1条optional、0条missing；构造、register、Done、正常停止，22.6s |
| F minecraft `[9.0,)` 必需 | 1/1；REQUIRED；不满足范围 | PASS（预期拒绝）；明确版本拒绝，目标 loadMod/构造/register 均未出现，10.1s |

[边界原文与断言结果](D:/DualForge/out/convergence-002/after/20261012-134007-c1ed7fd1/boundary.stdout.log:3)；[四个动态结果及断言](D:/DualForge/out/convergence-002/after/20261012-134007-c1ed7fd1/result.json)。该目录各 case 下还有原始 stdout/latest/debug/stderr、meta.json、crash-report（负例）、fixture.json、assertions.json、preservation.json。4个非法 mandatory／混合type输入明确拒绝，FML默认值测试亦 PASS。

负例不能用“没有 Done”或退出码判断。两轮负例 JVM exitCode 都为0，但 FML 明确拒绝：

- D：[stdout:60](D:/DualForge/out/convergence-002/after/20261012-134007-c1ed7fd1/D-missing-required/logs/spike002-0l-treatment/spike002-0l-treatment.stdout.log:60) 为 `Mod ID: 'convergence_missing', Requested by: 'forgeprobe', Expected range: '[1,)', Actual version: '[MISSING]'`；69行 pre-loading FATAL 指明 requires convergence_missing。
- F：[stdout:60](D:/DualForge/out/convergence-002/after/20261012-134007-c1ed7fd1/F-wrong-version/logs/spike002-0l-treatment/spike002-0l-treatment.stdout.log:60) 为 `Mod ID: 'minecraft', Requested by: 'forgeprobe', Expected range: '[9.0,)', Actual version: '1.21.1'`；69行 pre-loading FATAL 指明 requires minecraft 9.0。

可重跑命令（先用统一入口新构建，再将实际新路径传给独立测试）：

```powershell
pwsh -NoProfile -File .\tools\forgecompat.ps1 -Mode build -OutputRoot D:\DualForge\out\dependency-build
pwsh -NoProfile -File .\tools\test-forge-dependencies.ps1 -Stage after -CompatJar D:\DualForge\out\convergence-002\regress\20261012-134158-70642187\forgecompat-002-0l.jar
# 只执行六组metadata边界测试：追加 -BoundaryOnly；不需要启动服务器。
# 复现原缺陷：Stage before + 本轮 before-build 保存的旧源码构建件，不使用旧 compat/build。
```

## 5. 原 0l 回归结果

原统一入口未改、成功判据未放宽：`pwsh -NoProfile -File .\tools\forgecompat.ps1 -Mode regress -OutputRoot D:\DualForge\out\convergence-002\regress`。

`[E]` 全新目录干净构建＋固定原 Probe 回归 **PASS**；10 Java＋5资源，8个既存编译依赖，无历史 class 参与。13项原断言全部通过；构造器 stdout:81、register:88、NeoForge原生加载:89、Done:109；`DONE_STOPPED_GRACEFULLY / JVM exit0 / 22.7s`，stdout/latest/debug 无新增 ERROR/FATAL/dump FAIL、stderr空、无crash。[完整回归结果](D:/DualForge/out/convergence-002/regress/20261012-134158-70642187/result.json)及[断言](D:/DualForge/out/convergence-002/regress/20261012-134158-70642187/assertions.json)。日志1–3行重新确认全部固定环境版本。

最终新构建 JAR SHA-256 `8CDC180BF7C7401A49A35A895887BD3DB5EF7DB93B997840AC3C476B46E6189D`，26214字节。四个依赖动态case使用另一干净构建件 `D69185D3B14A8F9FEB585A086085A200D0AE216DD730CA924339469C75738CAC`；两者内容语义相同，打包时间戳不同。

所有动态运行只操作独立服务端副本；每次核验原目录2247文件集合及哈希不变，三个共享缓存原内容／CreationTimeUtc／LastWriteTimeUtc恢复。原服务器 JAR仍为 `D49CAEA48A61C57DA5384E053D7E8DE2834760172E6948C5167BF836BFD0E679`，Probe仍为 `A774D2F737B17F089046014AB55A53767DBAD008CD44DCCE52C727B87E559AE8`；原harness及统一入口未变。历史134项取证和17项运行附件全部匹配：[冻结核验](D:/DualForge/out/convergence-002/frozen-check.json)、[保全汇总](D:/DualForge/out/convergence-002/final-preservation.json)。当前源码的有意 Parser 修复保留，未回退为历史版本；历史归档未改。

## 6. 尚未覆盖的依赖语义

本轮 PASS 仅限现有扫描器子集：未加引号的 `[[dependencies.<owner>]]`、一行一个标量、合法布尔 mandatory、Maven版本范围、现有 ordering/side 字符串。未实现完整 TOML：带引号／转义的表键、行尾注释、复杂字符串／数组／内联表不在保证范围。

未做动态验证：可选依赖已安装但版本不符、CLIENT side排除、加载排序／循环、依赖 overrides、VersionSupportMatrix 特例、双入口模组实例化、多模组跨JAR依赖链。已从当前 FML源码确认可选已安装且版本不符会拒绝，但不把该源码事实写成本轮动态 PASS。

未映射 `forge` 依赖ID为 `neoforge`，未实现 NeoForge INCOMPATIBLE/DISCOURAGED 扩展声明，未探测新 Forge API、Registry、Capability、事件或Fabric。保留原有有效成果，不外推通用模组兼容。全部验收已满足；停止，等待用户确认下一阶段。
