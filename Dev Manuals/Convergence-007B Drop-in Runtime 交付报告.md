# Convergence-007：007B最终受控Drop-in交付

2026-10-14（Asia/Shanghai）。main起点 `ac4c4d7ef50ea35a634925d30dead0b93d798400`；独立前置[007A PR #14](https://github.com/GFander/DualForge/pull/14) HEAD `2ca2d20d8182fc41c08f9545eaed995be74d8121`。本分支 `codex/convergence-007b-normal-forge-items`，PR基线为007A任务分支，依赖其默认启动。007A源码/测试/报告不计作本PR新增。

| 验收 | 最终实际结论 |
|---|---|
| ZERO-CONFIG-CORE | **PASS**，目标NeoForge/FML组合及已验证范围 |
| DROP-IN-RUNTIME | **PASS**，无业务开关原生Forge双Item输入，原JAR不变 |
| THIRD-PARTY-COMPATIBILITY | **FAIL**，Clumps未加载，XP聚合功能NOT RUN |
| LINUX-SERVER | **PASS**，标准入口、受控运行、原生共存与同JAR历史回归；不含第三方功能 |
| MACOS-CLIENT | **NOT RUN**，提供新零参数包，旧006B-C不继承 |
| CONVERGENCE-007 | **PARTIAL**，不能降低第三方门槛取得整体PASS |

## 原因、修改与精确生产范围

[独立取证与ABI报告](<Convergence-007B 普通Forge注册与模块身份取证.md>)保留两个实际失败：原Profile强制Supplier marker；初步普通Profile通过后，默认JAR模块名dropin.items与真实FML名dropinprobe不匹配，容器在入口转换前失败。
没有修改旧fixture/预转换新Forge输入/改Manifest来绕过问题。

相对007A仅四生产文件变化：

- ControlledForgeItemProfile：分开验证普通直线挂接/纯Item工厂与原marker分支，逐调用/字段/数据依赖仍严格，旧Profile不放宽。
- DualForgeEntryTransformer：以实际扫描归属选择normal-items并记录该目标模式；所有转换仍先复制、验证、再提交。
- ForgeJarMetadata：匹配名保持原delegate；准入Forge automatic名不匹配采用官方ModJarMetadata及实际IModFile。显式JPMS不匹配拒绝。
- ForgeModReader：创建后绑定metadata的最小plumbing；原guard、优先级和候选识别不变。

Parser、依赖、LanguageLoader/javafml9999、Mod Event Bus、Registry事件行为、构造器ABI和模块层构建不改。不全局改Forge namespace、不注入物品或伪造Holder。
模块descriptor的**automatic身份来源在实际不匹配子集改变**，不是声称模块政策完全未动；必要性、官方实现和matching保全/explicit拒绝均直接验证。

生产功能提交 `4b99638439cdee3bd4394d91fa2478212076b717`，最终固定验证源码 `3c50a5b8e14fd2b581efffee191ba193f753f0c1`；生产文件指纹一致。与007A JAR逐成员核对，成员集合相同，仅上述四class变化，其他类/SPI/资源原字节。
当前完整单JAR **67,568字节，SHA-256 `dbb70bfa48ccd299c9a0bc74fc8fe07f645fce370a2131264cfdc44e5d235a36`**。
实际构建 `out/convergence-007b/bounded-module-build/20261014-103620-7464080a/dualforge.jar`，用户同字节交付 `out/convergence-007b/final-artifacts/DualForge.jar`。
原四SPI、FMLModType LIBRARY、automatic module dualforge与全部自有类在JAR中；用户只依赖目标NeoForge提供的运行库。

## 冻结输入与实际标准启动

新原生Forge JAR SHA `01acf5a450d9c23faa8cb42befbe164476f7a2ca320230289bf1bd40917db30f`，4,867字节，来源3a32d06。完整源码/命令/Forge编译依赖/源与class/JAR内容指纹、实际javap/ASM保存。无NeoForge/DualForge编译依赖，Supplier与register没有DF00*业务属性或marker。
观察器独立NeoForge JAR SHA `4bc619748f6de7ad397b5437644158bc87e1f1d410c93d6ca0df0ac56477e01f`，3,257字节，只查询，不注册/创建Item。

最终标准矩阵目录 `out/convergence-007b/stock/bounded-*`：

| 输入 | 证据/结论 |
|---|---|
| 纯NeoForge | 007A独立官方同输入基线，Done/stop/exit0；严格无ERROR为FAIL保留 |
| +最终DualForge | PASS，四默认、SPI、内置NeoForge委托、无dump |
| +原生NeoForge NativeBootstrap | PASS，实际构造/ServerStarted/内置委托 |
| +原生Forge dropin-items，仅两个JAR | PASS，真实normal目标/转换、Done/stop/exit0，无Driver |
| +dropin-items+查询观察器 | PASS，两个真实Registry Item、非AIR、各Holder.get/getter对象身份一致且互不覆盖 |
| +dropin-items+原生NeoForge+观察器 | PASS，两条实际功能/生命周期共存 |
| +Clumps原字节第三方 | FAIL，actual load marker缺失且宿主明确cannot be loaded；Done并不等于功能成功 |

全部使用原版run.sh、默认user_jvm_args.txt、官方unix_args和核心库。/proc实际命令保存；无DualForge/DF00*参数、Agent、额外cp/module参数、relay、自制launcher或测试配置。正常EULA和新生成世界属于普通安装前置/运行数据。
两JAR用户路径与附加查询观察路径分别实际运行；观察器不成为用户依赖。
服务端/give只到无玩家解析，**不声称真实玩家获取或客户端显示通过**；Registry/Holder真实对象由观察器验证。资源静态存在也不等于渲染。

原版云端基线含首次server.properties读取及Mojang公钥Connection refused两个ERROR。逐stdout/debug/latest错误头/计数与空mods基线相同，最终无新增FATAL/ERROR、无stderr/crash/dump；原strictNoErrors=false不改写。历史严格回归仍用原断言和原云端隔离/网络/relay条件，不混作零参数证据。

## 最终同一新JAR回归与拒绝

独立只读 `/workspace/DualForge-007B-final-validation`，HEAD3c50a5b，实际总结果 `out/identity-unification/verification/20261014-104730-7f768e3e/result.json` PASS。
原Probe严格smoke、002 metadata/CDEF（按FML具体拒绝）、003a发现/边界、003b Identity/Scope、004 Event C/T、005 Registry C/T/Bytecode/Scope/Atomicity/Assets全部PASS。
006A Contract/双Item C/T及006B Contract/Block+BlockItem C/T全部PASS，使用最终dbb70…兼容层；DF005/DF006/DF006B保留原C/T，不隐藏注入true。
阶段原结果/命令/日志/退出/环境/断言及输入SHA逐项绑定。[证据索引](<Convergence-007B 证据索引.json>)不混用A的cf944…、初步ca79…或中间c0db…结果。

本轮Normal Contract实际PASS：13个目标拒绝及before/caller-after字节保全，额外显式Module拒绝1个、JAR字节不变；正例精确反转映射全类相同，Supplier/BSM不改，不同入口身份、普通类/原生路径/显式关闭对照通过。匹配历史descriptor与通用原delegate相等。
功能最终边界修改后，标准矩阵及全历史用新dbb70…重跑，不继承中间c0db…PASS。

原005/006A/006B及历史源码/资源、Probe、世界和tracked运行配置逐SHA保全，374项保护不减少。所有JVM严格串行，正常停止；最后另存JVM/端口释放检查。
用户授权清理只处理已结束的新007副本运行库，逐JAR与官方参考SHA一致后删除；原日志、结果、世界、原输入、旧轮次工作区不删。记录与排除范围在out保存。

## 第三方、Mac与剩余范围

第三方[Clumps Forge1.21.1 19.0.0.1](https://modrinth.com/mod/clumps/version/aeoQuGBI)原18,226字节，SHA `e1de425ddd6f2b5c195c37a008d9fbc2c00c32fe1e214d76c63ab35446d13c19`，发行SHA1/SHA512另核验。
实际首阻塞是既有TOML三引号排除规则，早于API/ABI；没有改原JAR、跳过后记PASS或换成专用fixture。XP聚合NOT RUN，兼容FAIL。进一步TOML/第三方API扩展没有自动开始。

新Mac包 `out/convergence-007b/mac-kit-final/dualforge-007-zero-config-mac-kit.zip`，67,714字节，SHA **`397b9bb2a50765604427c2ea352e4030a63d657ad9093f55100debed2d2aae38`**。
ZIP CRC/清单逐文件SHA通过，只含最终DualForge与原Forge两JAR、身份、步骤/清单，无Driver、业务参数、世界、依赖或旧配置。按新Prism实例/NeoForge21.1.256/Java21验收/give、两图标/手持、英中名称、保存重进和日志。本环境没有Mac图形实机，**NOT RUN**；不继承006B-C或其截图豁免。

当前普通模式仍限定public final入口、单IEventBus ABI、public static final typed字段、同类零捕获纯Item工厂及get-only访问。无参/多参数、一般Supplier/Properties builder、普通Block/BlockItem、显式JPMS重命名、一般事件/注册API、任意第三方或多Forge完整启动未扩展。
同名javafml最后覆盖及9999非优先级事实保留，目标宿主顺序动态通过；不承诺任意启动器/Provider组合稳定性。

## 证据与版本控制

源码、文档、指纹入Git；原命令、日志、结果与ClassNode拒绝字节保存在本轮out，[索引](<Convergence-007B 证据索引.json>)记录原SHA/大小和路径。完整附件不提交生产树，世界/官方依赖/缓存不归档。最小归档与远端状态如有独立记录，以实际核验为准，不把本地路径当永久链接。
006B既有附件已独立远端VERIFIED，后续客户端不在旧ZIP内，详见007A Gate0。新007证据不冒充旧归档内容。
用户安装见[指南](../docs/INSTALL.md)，重跑与Mac步骤见[dropin_items](../tests/dropin_items/README.md)。PR正文记录最终HEAD/CI及007A依赖；不合并、不创建Release、不继续其他阶段。

最小原始归档当前 **ARCHIVE_READY / NOT PERSISTED**：`out/convergence-007b/evidence-export/dualforge-007-minimal-evidence.zip`，160,046字节，87个ZIP文件（83个原始记录payload＋1个明确标记的派生历史指针汇总），SHA `dd0c6e7f862887e056b865ad6b7bb2c2dc41e84b3e129d1bf4afcb769f06dfcd`。CRC、逐Manifest/SHA、原文件未变和文本凭据模式扫描通过。覆盖最终标准normal/query/mixed/Clumps、纯基线、Contract before/after及构建；完整历史raw、世界、官方依赖与JAR二进制不收录。当前工具没有GitHub文件附件上传接口，本ZIP尚未远端上传/复核，不能称永久下载；可由用户在本PR评论拖入ZIP后再独立核验。
