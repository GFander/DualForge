# DualForge 命名统一迁移报告

2026-10-13（执行器 UTC）。**Gate 1–3 受控范围 PASS；Gate 4 新 Mac cohort NOT RUN，整体客户端验收未完成。**
[E] 表示本轮实际执行；[S] 表示静态契约/源码检查；[D] 表示设计与交付边界。
原始附件根目录：`/workspace/DualForge-identity/out/`。
[命名审计](DualForge%20命名审计.json) 保存修改前 1810 处命中与 A–F 分类；
[迁移索引](DualForge%20命名统一迁移索引.json) 保存逐文件保护 SHA、逐行豁免、制品与本轮证据路径/指纹。

## Git 基线与被测来源

[E] fetch 成功，PR #7 已于 2026-10-13T07:35:59Z 合并。独立 worktree：
`/workspace/DualForge-identity`；分支 `codex/dualforge-identity-unification`。
起点 `87edda824dc9fb22eaa69a3fcb1b3653f9132b4e`，工作区干净。
读取 AGENTS、README、STRUCTURE、tests README 与 004/005/005-C 最终报告。
不覆盖其他 worktree，不改写历史，不混入 006、CI 或主分支保护。

生产身份提交：`66d0cf37399e4dcbc48dbd17a47ede80c2603ccd`（完整 SHA 见索引 productionCommit）。
动态 runner 被测提交：`854a2bc1ccb6ef10458d6d43ef158e9aaddc9c57`。
新 kit 源提交：`1ffabf0582ae94692b5e646c002e4f6e1e25cc39`。
构建时源码未全部提交，build-inputs.json 保存 16 个生产输入的 SHA；提交后逐项证明
与 productionCommit 和当前生产字节一致。最后文档提交 SHA 以 PR HEAD/交付回复为准，避免自引用。

## 原身份 → 新身份

| 对象 | 原身份 | 新身份 |
|---|---|---|
| 展示名称 | ForgeCompat / DualForge 混用 | DualForge |
| 生产目录 | spike001a/compat-002-0l/src/main | dualforge/src/main |
| Java package | com.example.forgecompat | io.github.gfander.dualforge |
| TransformationService 类 | ForgeCompatTransformationService | DualForgeTransformationService |
| LanguageLoader 类 | ForgeCompatLanguageLoader | DualForgeLanguageLoader |
| Transformer 类 | ForgeProbeConstructorTransformer | DualForgeEntryTransformer |
| 服务 name | forgecompat-transform | dualforge-transform |
| JPMS 自动模块 | forgecompat | dualforge |
| 开发 JAR | forgecompat-002-0l.jar | dualforge.jar |
| 主入口 | tools/entry/forgecompat.zsh | tools/entry/dualforge.zsh |
| 四个 feature 属性 | forgecompat.{2a.rewrite,0g.apiRewrite,0j.emptyRegistration,0l.subscriberRewrite} | dualforge. 同后缀 |
| dump 属性 | forgecompat.dump.path / forgecompat.dump.profileDir | dualforge.dump.path / dualforge.dump.profileDir |
| 主/转换/语言日志 | FORGECOMPAT / FORGECOMPAT-XFORM / FORGECOMPAT-LANG | DUALFORGE / DUALFORGE-XFORM / DUALFORGE-LANG |
| empty/subscriber helper 日志 | FORGECOMPAT-EMPTY / FORGECOMPAT-SUBSCRIBER | DUALFORGE |
| 原 Mac 流程 | tests/registry/mac | tests/registry/mac-legacy（原字节） |
| 新 Mac 流程 | 无 | tests/registry/mac，独立新 cohort 与收据 |

[S] 所有生产迁移使用 git mv，再按逐文件审计更新引用。三类 Java 名称和内部 helper owner
同步改变。四份 META-INF/services **外部接口文件名不变**，Provider 为：

- cpw.mods.modlauncher.api.ITransformationService → io.github.gfander.dualforge.DualForgeTransformationService
- net.neoforged.neoforgespi.language.IModLanguageLoader → io.github.gfander.dualforge.DualForgeLanguageLoader
- net.neoforged.neoforgespi.locating.IModFileCandidateLocator → io.github.gfander.dualforge.ForgeModLocator
- net.neoforged.neoforgespi.locating.IModFileReader → io.github.gfander.dualforge.ForgeModReader

Manifest 保持 Manifest-Version:1.0 / FMLModType:LIBRARY，仅 Automatic-Module-Name 改为 dualforge。
name()=javafml / version()=9999、@Mod 归属、构造器注入、Reader/Parser/依赖、Event/Registry 算法保持原样。
外部 net.minecraftforge / net.neoforged API 名称与 DF004_REGISTER / DF005_REGISTER 不改。
`net/minecraftforge/eventbus/api/IEventBus.java` 是既有外部 ABI shim，保持 package 和原字节；
其新旧 class 字节也完全相同。旧 dump fallback 默认路径及四开关默认/解析行为未改变。

## 新旧 JAR 与 Gate 1

[E] 新 JAR：`out/identity-unification/build/20261013-075425-5c200426/dualforge.jar`。
SHA-256：`4c0c11448cef6b22381ef94393ff23c6fd582613b7e8d441b2473917fd3c6af6`；38950 字节。
旧已验收兼容层 SHA：`ed572ca9762e235e7d264355d35b3c9d3ab9ecf1da3f2c3234b1e28056aad752`；38752 字节。
新文件是从迁移源码全量 javac/jar 生成，未将旧文件改名冒充。

[S/E] 11 个 Java 编译成功。package/path、Provider class、Manifest、自动模块名、四 SPI 和六个属性均 PASS。
新 JAR 无旧 package/class/property/module 字符串，无旧类混入。
jar tf、jar --describe-module、三份 javap、SPI.json、assertions.json 在本轮 verification 下。
源码/资源逐字节规范化对照：16 份生产文件只允许表中身份映射；没有算法、方法调用结构或 ABI 的额外变化。
新旧每个 JAR entry 的指纹在 jar-difference.json，不把重编译后多数 class 指纹改变称作二进制相同。

## Gate 2：官方实际加载与 Module Layer

[E] loading 阶段在新物理服务端副本，使用原 Probe、新 JAR、官方 unix_args.txt 启动。
严格 smoke、实际新服务发现/initialize/transformers 调用、语言 Provider 枚举和 loadMod 均 PASS。
原生 javafml/NeoForge 委托正常，无重复 Module/SPI 错误，Done → stop → exit0。
所有正例保持无 ERROR/FATAL/dump FAIL、空 stderr、无 crash 的原断言。

[E] read-only 测试 agent 只在正常 shutdown 时观察 Instrumentation 中已加载 class、
公开 Launcher.findLayerManager 与 IModuleLayerManager；不注册 transformer、私有反射或自定义 ClassLoader。
BOOT / SERVICE / PLUGIN / GAME 全部存在；SERVICE 仅含 dualforge，PLUGIN 含 neoforge.coremods，
GAME 含原 forgeprobe、minecraft、neoforge 等。五个实际生产类的 getModule 都为命名模块 dualforge。
完整官方各层模块清单保存为 loading/module-layers.json，不依靠编译通过推断服务发现。

## Gate 3：同新 SHA 的历史功能回归

| 本轮实际验证 | 结果 |
|---|---|
| 原 A 严格 smoke | PASS |
| 002 metadata 六组 + C/D/E/F | PASS |
| 003a A–F/发现/规范化去重/native/边界 | PASS |
| 003b 冻结 IdentityProbe / 转换范围 | PASS |
| 004 Contract / Event C/T / runtime dump | PASS |
| 005 Registry Contract / C/T | PASS |
| 005 Bytecode / Scope / Atomicity / runtime dump | PASS |
| 005 原始 PNG/model/双语言 assets | PASS |

[E] 全部实际安装同一个新 SHA 4c0c1144…d3c6af6。Registry C/T 仍使用原 fixture SHA
`312a4f697c1dc617ed48822c10179384b6559f4e6399db9210c15541c774132c`，同一 driver、环境和 flags；
仅 DF005_REGISTER 决定原构造器是否挂接 MOD bus。C：真实查询不存在、supplier 0；
T：真实 Item/key、非 AIR、holder get 对象同一、supplier 1 和官方注册栈都通过。
Event C/T 同原 Forge fixture/原 Neo driver，post 各 1，callback 为 0 / 1，真实总线栈与返回通过。

[E] D 因 mandatory convergence_missing 缺失被 FML 拒绝；F 因 minecraft 实际 1.21.1 不符合范围被拒绝。
两者包含具体 rejection、pre-loading fatal 和未实例化证据，不以 exit0 或缺少 Done 推断成功。
C/E 均正常完成启动和停止。原 Forge/Neo fixture、Driver、物品 ID 与原 PNG 均未改/未重编译。
新旧 owner 的 Forge 输入纯度拒绝均保留；最终新增保留旧 owner 检查后对原 class 常量池重新实际核验，
结果在 retained-fixture-purity.json。该最后变动只涉及未执行的 fixture 构建检查，无生产/JVM行为修改。

[D] 本轮不重复历史 before/baseline 失败复现，也不重编译已冻结 fixture。
因此原 004/005 总任务 summary 仍为 PARTIAL（缺少本轮 fixture/baseline stage），未放宽原 required 集合。
身份迁移的明确 Gate 集合由新的 validation.json 汇总，不把旧 PASS 重命名为新结果。

## 工具链、检查与串行保全

[E] 本轮实测 Microsoft OpenJDK/java/javac/jar 21.0.10+7-LTS，zsh5.9，Python3.12.14，git2.52.0，
Linux6.18.44 x86_64。Minecraft1.21.1 / NeoForge21.1.256 / FML4.0.45 / ModLauncher11.0.5 等输入未升级。
实际版本 stdout/stderr、二进制 SHA 和命令在 toolchain/20261013-081259-cf209226。

[E] Python 自动测试共 30 项通过：events7、registry6、共享混装拒绝2、Mac模拟15。
20 份 zsh 脚本逐一 zsh -n PASS；git diff --check PASS。
scanner 自动负例在临时活跃 .md 中加入旧身份，实际拒绝后移除本轮临时文件；正常扫描再次 PASS。

[E] 374 个逐路径保护文件、154 个外部输入 SHA 不变；原服务端输入文件集合/世界/配置不变。
198 个 history 跟踪文件、旧 Dev Manuals 原交付报告/索引、原 Forge源码/资源和旧 kit 原11文件保持原字节。
AGENTS 的完整六节 Git 工作规范原字节保留，仅后续结构说明更新。
serial-jvm-timeline.json 根据实际 command/exit/meta 原文件时间保存88次 JVM命令，无重叠；
所有运行使用物理副本。最终无 java 进程，25565 可绑定并释放。
终检首次在默认沙箱创建 socket 被拒；仅端口检查在本任务可用网络权限下重试成功，未放宽服务端判据。

## Gate 4：独立新 Mac kit

[E/S] 已生成 `out/identity-unification/mac-kit/delivery/dualforge-identity-mac-client-kit.zip`，
56346字节/12文件，SHA：`625cc940b22264e931a7188c60280060a1ff19ea9974ec816835e4058c92fdc1`。
包含新 dualforge.jar、原 RegistryProbe、COHORT-SHA256、manifest/SHA256SUMS、准确新 JVM 参数与安装/回滚/清单。
不含世界、账号、JDK、依赖、客户端或测试 driver。新收据和输出路径独立，拒绝旧 JAR、旧属性和错 SHA 混装。
packager 要求 Gate1/2 和 Registry Contract/C/T/Regression 对应同一实际被测 SHA。

[E] 15项 Mac行为测试是在 Linux 运行实际 zsh，并模拟 Darwin/Prism/Java元数据；不算实机。
[D] 无可访问的 macOS 图形客户端，所以**新 cohort 的客户端 NOT RUN**，未继承原 005-C PASS。
ZIP 保存在本地云端工作区，未上传、无外部下载URL；提供实际文件路径供应用取出。
Legacy 原11文件保持字节，新 LEGACY.md 给出移动后的准确构建路径及原 cohort指纹。

## 旧名残留、修改范围和重跑

[S] 剩余旧名只在374个逐文件冻结/Legacy输入、审计/迁移文件及9条精确豁免行：
历史目录映射1、历史 before/baseline示例2、Legacy模块/安装/参数拒绝3、Mac负例1、
JAR旧名拒绝检查1、Forge原生输入旧owner拒绝1。每条路径、完整文本和理由在索引，
不是全目录放行。Forge/NeoForge API 属F类，保持原契约。

修改涉及 dualforge/src 的16个生产输入及原位置删除/移动、tools主入口与workflow、
当前测试的package/链接/日志断言、Mac新/Legacy工具、project_identity新测试、
README/STRUCTURE/AGENTS/当前测试文档和三份新迁移文档。完整Git文件清单在索引 changedPaths。
无修改 history、旧交付文档、原 fixture源码、世界、配置、LICENSE、CI；不强推、不合并、不发布、不进入006。

重跑构建/静态/服务端命令见 [当前身份验证 README](../tests/project_identity/README.md)，
本轮实际完整参数在 verification/20261013-080337-82eb4426/invocation.json。
各 native command.json、exit.json、stdout/stderr、输入指纹、assertions、result 与 dump 路径由索引导航。
服务端原始日志仍位于每个 stage 的 logs/spike002-0l-treatment/，这是证据布局系列名称，不是旧生产制品。
报告和索引入Git，完整原始附件、JAR、ZIP、第三方依赖和世界不入Git。

**保留边界：**原单参数构造器 ABI、受控 marker-only Event、受控单 Itemprofile 的支持集合未扩展。
一般事件API/取消/优先级/跨模组、多参数/无参构造器、一般 Registry/RegistryObject/Capability/Fabric仍不支持。
本任务完成后停止；新 Mac实机验收是剩余验证项，合并须由用户授权。
