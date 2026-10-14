# Convergence-007 Drop-in Runtime：007A交付

日期2026-10-14（Asia/Shanghai），分支 `codex/convergence-007-drop-in-bootstrap`，main基线 `ac4c4d7ef50ea35a634925d30dead0b93d798400`。独立007B处理经取证必要的普通Item Profile；本PR不新增构造器ABI、API或加载架构。

| 验收 | 本PR实际结论 |
|---|---|
| ZERO-CONFIG-CORE | **PASS**，目标宿主及受控范围 |
| DROP-IN-RUNTIME | **FAIL**，普通无业务开关Item输入被旧Profile拒绝；独立007B处理 |
| THIRD-PARTY-COMPATIBILITY | **FAIL**，Clumps未加载/功能未验证 |
| LINUX-SERVER | **PASS**，标准Bootstrap、原生共存、受控旧输入及历史严格回归；不含第三方功能 |
| MACOS-CLIENT | **NOT RUN**，新JAR零参数图形验收不能继承006B-C |
| CONVERGENCE-007 | **PARTIAL**，三个完整目标尚未同时PASS |

## 生产修改与制品

源码功能提交 `015be70`；固定测试源码提交 `d6929d0`。二者生产源码/资源逐文件一致，最终文档不改变Java。
四开关缺省true，显式true/false保留，非法值明确拒绝。未指定dump属性时立即返回；移除Windows fallback。显式legacy/profile dump的IOException传播到原有复制后转换拒绝路径，不吞异常；相对输出文件也能正确创建父目录。

唯一生产修改文件为 `DualForgeEntryTransformer.java`。与原159514…JAR逐成员比较，成员集合相同，**只有该Transformer.class变化，其他类/SPI/资源保持原字节**。
当前Locator/Reader/Parser、元数据/依赖、ModuleDescriptor/Layer策略、LanguageLoader、旧Profile及单IEventBus ABI均不改。

最终新兼容层：66,375字节，SHA-256 **`cf9449890c6836ba7512b21e3addb7861c08861491036adf126cacf91b1c50a2`**。
构建路径 `/workspace/DualForge-007/out/convergence-007/final-a-build/20261014-093136-b441c2fe/dualforge.jar`，交付同字节副本 `out/convergence-007/artifacts-a/DualForge.jar`。
Manifest为automatic module dualforge及FMLModType LIBRARY，原四SPI与全部自有生产类在单JAR内。八个编译依赖由官方NeoForge运行时提供，用户无需额外DualForge library、Python或zsh。

## 本轮实际动态运行

官方安装与SPI证据见[Gate0](<Convergence-007 Gate0 SPI与Bootstrap取证报告.md>)。实际工具链Java/javac/jar21.0.10+7，Python与zsh版本另存命令日志；所有JVM串行，输入物理复制，原服务器/世界/Probe和374项冻结文件保护通过。

`tests/bootstrap/BootstrapContractTest.java`实际检查4默认、8显式覆盖、20非法值、2默认无dump、2显式输出和2 I/O失败。旧Scope/Event/Registry suites继续验证复制后拒绝原子性。

| 标准安装矩阵 | 实际结果 |
|---|---|
| 纯NeoForge | Done/stop/exit0；严格无ERROR为FAIL，见基线说明 |
| +DualForge | PASS；四默认开启、真实SPI/委托，无dump |
| +原生NeoForge NativeBootstrap | PASS；构造与ServerStarted、内置委托 |
| +原ForgeProbe | PASS；真实构造与非空profile的register返回 |
| +NativeBootstrap+ForgeProbe | PASS；分别实际生命周期，不互相接管 |
| +Clumps原生Forge第三方 | 未加载，明确宿主cannot be loaded警告；功能FAIL |

上表使用原版 `run.sh`/user JVM文件/官方核心JAR，无DualForge或业务属性、Agent、额外模块参数、cloud relay或测试配置。实际命令从/proc记录；诊断与旧全回归的特殊测试条件单独标注。

云端纯基线已有两种ERROR：首次server.properties未生成、Mojang公钥Connection refused。首次新JAR严格日志检查如实FAIL；复核使用空mods纯基线的stdout/debug/latest具体错误头及计数逐项比较，新增项、不同计数、FATAL、stderr、crash或输入变化仍拒绝。`strictNoErrors:false`原值保留，受控标准矩阵仅称**无新增基线错误**，不称环境日志完全干净。没有放宽任何历史smoke/C/T断言。

## 同一最终JAR全历史回归

固定源码只读worktree `/workspace/DualForge-007A-validation`（detached d6929d0）实际验证：

- 原ForgeProbe严格smoke和Module Layer/SPI加载审计PASS。
- 002 metadata及C满足/D缺失required/E缺失optional/F版本错误动态回归PASS；负例检查具体FML原因。
- 003a发现A–F及边界、003b实际Identity/Scope PASS。
- 004 Contract、真实Event C/T及旧回归PASS。
- 005 Contract/Scope/Atomicity、Registry C/T、Assets PASS。
- 006A Contract及双Item C/T PASS。
- 006B Contract及Block/BlockItem C/T PASS。

所有以上结果实际compat SHA均为cf944…，原DF005/DF006/DF006B C/T属性分别保留；使用旧Linux隔离、网络/CA和public-key relay作为明确的历史云端条件，不能称零配置证据。原输入不重编译，不混入旧159514…cohort。
总结果 `out/identity-unification/verification/20261014-095328-b8263707/result.json` 及 `out/convergence-007/history-a/`；006A/B阶段索引分别在只读worktree out/convergence-006a、006b。

先前两次007工作区汇总FAIL保留：第一次运行中runner变化触发保全；第二次继续引用不可重置的首次预检。未覆盖快照、修改失败状态或放宽断言。最终采用新的固定提交worktree，保全及汇总真实PASS。

## 普通输入与第三方阻塞

新原生Forge dropinprobe从Forge52.1.0及未打NeoForge补丁的Minecraft干净编译，无NeoForge/DualForge依赖、业务属性或预转换。冻结JAR SHA `01acf5a450d9c23faa8cb42befbe164476f7a2ca320230289bf1bd40917db30f`。在本007A上进入003a发现、Reader、真实FML metadata后，首次实际拒绝为 `ControlledForgeItemProfile.validateSupplier` 要求marker-only工厂；实际普通Lambda没有打印。单IEventBus构造器直线挂接也与旧分支模板不同。
命令、javap/ASM和失败原字节保存，不修改旧005/006A/006B输入；该Profile扩展必须独立007B PR。

真实第三方为[Clumps Forge1.21.1 19.0.0.1](https://modrinth.com/mod/clumps/version/aeoQuGBI)，[原作者源码](https://github.com/jaredlll08/Clumps)，无额外Modrinth依赖。原下载18,226字节，SHA-256 `e1de425ddd6f2b5c195c37a008d9fbc2c00c32fe1e214d76c63ab35446d13c19`，同时核对发行SHA1/SHA512。未改任何原字节。
第一阻塞早于API/ABI：TOML含三引号多行description，003a候选规则明确返回false，宿主跳过并警告cannot be loaded；不能根据Done、exit0或空stderr判断兼容。实际XP聚合功能**NOT RUN**，第三方兼容**FAIL**。没有推断未到达的构造器/API支持，也不扩展Parser或替换成DualForge专用fixture来取得PASS。

## 边界、安装及可复核性

同名javafml/9999只在本目标宿主实际顺序下验证；9999非优先级，任意Provider组合稳定性不作保证。简单TOML、当前单总线ABI、严格事件/注册调用点范围保留；不支持一般Forge生态、捕获Supplier、一般Registry/Event API或多个Forge模组完整共存。
用户入口见[INSTALL](../docs/INSTALL.md)，标准测试见[bootstrap](../tests/bootstrap/README.md)。新Mac测试包由独立007B正常输入制作，尚未图形运行；旧人工PASS不继承。

原始命令、stdout/stderr、debug、exit、断言、工具链、输入/JAR/成员指纹按[证据索引](<Convergence-007 证据索引.json>)定位。完整附件保存在out外部工作区；新007附件未声称已远端上传。006B远端归档已独立VERIFIED，覆盖边界与007新证据分开。
所有生产输入与冻结文件未变；用户批准后仅删除已结束新副本中逐JAR校验的重复运行库，保存清单，不删除旧工作区、日志、结果、世界或官方参考安装。

本轮不合并、不创建Release、不开始其他阶段。最新main、最终分支HEAD/PR及CI实际状态在提交后的PR正文记录；报告中的测试源码SHA与制品SHA不以文档提交代替。
