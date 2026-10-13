# Convergence-006A：受控 Forge 多 Item 注册

2026-10-13。**006A-S 受控服务端 PASS；006A-C NOT RUN。**
[E] 本轮实际执行， [S] 静态契约， [D] 支持/设计决定。
[ABI与字节码矩阵](Convergence-006A%20ABI%20与字节码报告.md)、
[证据索引](Convergence-006A%20证据索引.json) 和 [重跑入口](../tests/multi_registry/README.md)。
原始运行目录时间戳使用执行器UTC；客户端状态不继承任何旧Mac cohort。

## 基线与源码保全

[E] fetch成功；PR #8 已合并。最新main起点 `6e6fb41a1310105929264aef9fdc7cffd04e49fa`，
独立 worktree `/workspace/DualForge-006A`，分支 `codex/convergence-006a-multi-item`，起点干净。
读取适用AGENTS、README、结构/测试说明及生产源码和已有验收证据。
起点16份生产输入与命名迁移原build输入完全一致，原Gate1–3的同SHA证据和770份附件再次核验无误；
它们仅作为本轮未改代码的前置基线。新命名 cohort Mac仍为NOT RUN，旧客户端PASS不继承。

[E] 本轮生产提交：`c59d6e5eef41e78c863e7d29f3ab09c490b518af`。
干净构建时尚未提交，build-inputs.json保存指纹，提交后16份输入与commit/current/验证worktree核对相同。
Fixture源提交：`63820b7d39d5f0c6dff69a26021fbcdfe266e5aa`。
最终历史回归验证worktree `/workspace/DualForge-006A-verify`，固定源码提交
`54159469c5a909e08c270856e7a7c73f8990d155`，使用同一个新JAR。
最后文档SHA见PR HEAD/交付回复，避免自引用；没有改写历史或合并/发布/进入006B。

[E] 当前 Microsoft java/javac/jar21.0.10+7-LTS、Python3.12.14、zsh5.9、git2.52.0、Linux6.18.44 x86_64。
MC1.21.1/NeoForge21.1.256/Forge52.1.0；FML4.0.45/ModLauncher11.0.5和其余锁定库不升级。
实际工具命令/输出/hash在 `out/convergence-006a/toolchain/20261013-110210-8288d624`。

## Gate 0–1：实际取证与冻结原生Forge输入

[E] 使用原Forge依赖和已核验纯vanilla MC，未用NeoForge或DualForge编译原生输入。
多 Item source为`com.example.multiregistrycheck.MultiRegistryProbe`，@Mod `multiregistryprobe`。
一个 CATALOG DR；PRIMARY/SECONDARY holder；Alpha lambda，Beta createSecondItem静态method reference；
两者只打印各自marker并new普通Item/Properties。原单IEventBus构造器、一次MODbus挂接形态不变，
DF006_REGISTER只控制挂接；原005源码、原JAR和原贴图均保持原字节。

| 本轮冻结制品 | SHA-256 |
|---|---|
| Forge multi-registry-probe.jar | `4099fb08fef19891eb95ca2fcce52300fb428f1faca4b562200dd081f562d904` |
| 原生Neo查询 multi-registry-driver.jar | `6699d17ef920723ce08c62f8a51c68414a9c5641bf4c33de17208e6a70d41f7c` |
| 独立布局/Bootstrap registry-matrix.jar | `7b79a342172c6877f512f5defc81a39f96d4df71c25b8c08ea1ce084e1507efb` |

[E] 类内容、source SHA、实际javac/jar命令、classpath纯度、javap及ASM节点已保存。
matrix有独立modId/包/类/字段/ID/getter/factory名称及实际DR局部ASTORE/ALOAD布局；
捕获和altMetafactory样本从真实源码编译，用于拒绝取证，不当作已支持结构。
详见ABI报告。两个new PNG是原005用户16×16图的原字节副本；model/lang独立打包，静态检查PASS。

## Gate 2：修改前的实际失败

[E] 当前main全量干净构建件SHA
`5b9ea72cf89efe8f6e8ee0e6fdcbd823a45ba525e5068021bb1a26f769c09d3f`。
其18个JAR entry字节均等于已验收命名制品；全JAR hash变化来自重新打包，而非代码变化。
实际隔离运行：`out/convergence-006a/baseline/20261013-100611-05f8f4b8`。
精确三制品安装、一次Forge Locator、Reader和真实 `{multiregistryprobe}` metadata均确认后，
原stderr首次失败：`ControlledForgeItemProfile.validate → DualForgeEntryTransformer.targets`，
`005 unsupported Item profile: class structure`。JVM exit1、尚未入口构造、无Done。
这是旧整类field/method计数的直接失败证据，不只凭启动失败推断原因。

[E] 原source要求2字段/4方法/10-opcode initializer且只认synthetic lambda；
实际B为3字段/6方法/15-opcode，method reference实现为非synthetic private static。
未改变fixture迎合旧Transformer。正式baseline入口重跑在
`baseline/20261013-105726-75b3e87c`同样按原具体失败断言PASS（失败复现，不是功能PASS）。

## Gate 3：最小生产变更

- ControlledForgeItemProfile：移除整类固定数量及lambda$命名假设，以完整字段/方法角色和restricted symbolic initializer追踪真实API数据依赖。
- 每个register独立验证真实ITEMS/create namespace、同一DR receiver、唯一literal ID、标准零捕获Supplier Handle及一对一holder赋值/消费；未知操作/API整类拒绝。
- 参数/泛型/所有字段引用、create/register/attach/get仅对完整Plan记录的实际调用点映射；不做命名空间全局替换。
- 保留既有constructor和普通Item supplier业务子契约、真实FML Mod Event Bus、所有业务指令和Bootstrap；每holder一个独立get-only accessor。
- DualForgeEntryTransformer：仅接收实际Stats、按声明数量报告single-item/controlled-items，打印真实命中数。原scan/@Mod/modId来源、Locator/Reader/Parser/依赖/LanguageLoader/Module策略/Event分支未改变。

[S/E] 原caller ClassNode先复制，transform时重算Plan且全部验证后才编辑副本。
失败不返回部分节点；Supplier方法和BSM不动。只有两个既有production class entry变化，
另增加6个该profile的私有内部helper/Stats class；其余原entry逐字节一致，SPI/Manifest不改。
新的 `dualforge.jar`：47564字节，SHA
`f03d86331830c57c55915c611143d864eaef6d2dbf25cc3cf1628c39015a3a1c`。

## Gate 4：真实C/T

[E] C/T用相同compat、fixture、driver、Java、输入世界/配置/flags；唯一业务差异DF006_REGISTER=false/true。
共同环境和角色指纹由ct-environment/artifacts检查；每个runtime dump再由同生产Transformer的contract校验。

| 验证 | C（103711-dfd45267） | T（103957-856da8ef） |
|---|---|---|
| alpha_item/beta_item真实contains/getOptional | 均false | 均true |
| Alpha/Beta Supplier各执行数 | 0/0 | 1/1 |
| ID、Item类型、非AIR、每Holder.get对象 | 未调用未绑定get | 全部正确 |
| 两Item与两个holder互不相同 | 未注册 | true |
| FML container MODbus与注入身份，区别global | PASS | PASS |
| 每Supplier官方RegisterEvent→DeferredRegister→FML栈 | 不发生 | 两个均实际捕获 |
| Done→正常stop→exit0、无ERROR/FATAL/dump FAIL/stderr/crash | PASS | PASS |

完整source仍由原生Forge API编译，driver只查询公共注册表/holder并观察实际println调用栈。
没有直接注册物品、手工调用Supplier、伪造对象、直接post注册事件或绕开FML生命周期。

## Gate 5：拒绝、范围与原子性

[E] 9项明确结构拒绝：重复ID、未知Bootstrap、真实altMetafactory、真实捕获lambda、错误BLOCKS registry、
跨类impl Handle、额外getEntries ForgeAPI、错误Modbus参数、晚期第二getter descriptor错误。
另1项实际在完整Plan已编辑副本之后阻断dump目录的I/O失败，也抛受控异常且原节点字节不变。
每项before/after class和reason保存；所有原节点SHA/canonical bytes相等。
该dump失败仅为测试负例，真实C/T与历史正例日志不含dump FAIL。

[E] main与local-layout正例成功后原caller不变；所有授权成员反向恢复后整个class字节相同；
lambda与static factory业务/metadata/Bootstrap不变。未被扫描归属的同形类与native metadata同字节类均不改。
旧005负例extra-registration仍严格通过：重复ID/未消费结果拒绝，不依赖原固定10指令数。

## Gate 6：最终新SHA历史回归

[E] 最终同一f03d8633…a3a1c新JAR实际执行：

| 原验证 | 本轮最终结果 |
|---|---|
| 原ForgeProbe严格smoke | PASS |
| 002六组metadata与C/D/E/F | PASS |
| 003a发现/去重/边界 | PASS |
| 003b身份/范围 | PASS |
| 004 Event Contract/C/T/runtime bytecode | PASS，post各1、callback0/1 |
| 005 Registry Contract/C/T/Scope/Atomicity/runtime bytecode | PASS |
| 005原始PNG/model/lang Assets | PASS |
| 006A新两结构C/T及实际布局对照 | PASS |

D/F仍按FML实际缺失mandatory convergence_missing和minecraft范围不符拒绝原因、pre-loading和无实例化判断，
不以exit0/无Done判定PASS。原原生Forge A/B/004/005输入未重建，不混用旧compat cohort。
原004/005整任务summary可能为PARTIAL（未重跑历史fixture/baseline阶段），不放宽其required集合；
006A按本轮明确需要的实际Gate集合由summary.json聚合并核对同新SHA。

## 失败与必要重跑记录

[E] 首轮historical wrapper的动态子项均通过，但最终preservation失败：
`/workspace/DualForge-006A/tests/multi_registry/run_multi_registry.py`在运行期间新增baseline入口，
其SHA相对pref snapshot变化。这是本轮执行错误；没有放宽断言或把该次整轮FAIL写作PASS。
原失败目录`out/identity-unification/verification/20261013-104321-8772caaa`及004/005 failure完整保留。

[E] 先提交并冻结工具，再建立detached验证worktree固定5415946，完整重新串行执行所有Gate1/2与历史动态：
`/workspace/DualForge-006A-verify/out/identity-unification/verification/20261013-105048-30f6ba57`，
实际完整PASS，所有保全断言通过。复核主/验证worktree生产源与被测JAR输入相同；没有覆盖旧pref记录。
以后Source只做报告/包准备，不再改生产代码；baseline新入口亦已独立实际重跑。

## 自动检查、保护与客户端

[E] 24项Python自动测试PASS（004一致性7、005一致性6、Legacy混装2、006A一致性/嵌套历史SHA绑定9）；
22个zsh入口逐一语法PASS，命名扫描仍无未豁免旧身份，diff --check PASS。
历史命名checker的“身份映射对照”现在明确读取冻结生产提交；原命名扫描和逐路径保护仍严格，
不拿授权006A功能变化与旧“只许重命名”的契约错误比较。冻结旧迁移报告/索引不改。

[E] 377个起点不可变跟踪文件、156个外部输入、18个本轮冻结fixture输入SHA不变；
源世界/原配置/完整服务端文件集合未变。AGENTS完整六节Git规范原字节保留。
292次实际JVM命令（包含初次失败与重跑）按command/exit/meta时间无重叠；最后无Java进程，25565可绑定并释放。

[E/S] 本轮Mac手动输入包已生成：`out/convergence-006a/mac-kit/delivery/dualforge-006a-mac-test-kit.zip`，
50018字节/7文件，SHA `ce51c14510430fe4e1cdaa5a4d8141ee76c8c5fe4002efe4b70a859eb77b2798`。
仅新compat+原生双Item fixture（内含两model/PNG/lang）、参数/人工说明/清单/manifest/SHA；CRC及全部payload SHA校验PASS。
未运行Mac图形客户端，未执行新Mac安装脚本（本包仅手动安装），没有上传或外部下载URL。
006A-C明确NOT RUN；此前005-C或命名cohort结果不自动适用。

## 原始证据与停止边界

本轮源/制品/SHA/实际命令、ASM节点、descriptor、bootstrap、stdout/stderr/debug/exit、断言与字节前后，
连同失败和最终重跑在证据索引中逐文件导航。完整运行附件不入Git。
最小C/T原始归档：`out/convergence-006a/convergence-006a-ct-minimal.tar.gz`，85871字节，26份原记录+manifest/说明；
SHA `6adfcde1cfd97aee4b1808ab2b52060cf5b9be4c23e81420f5b3681587fad75a`。
逐字节复制，不含世界/依赖payload/凭据，metadata保留工作区路径、loopback及源输入指纹；未上传。

[D] 完成标准在受控范围满足：同一原生Forge模组的lambda和私有静态method reference两种声明，
通过同一DR与真实FML注册生命周期运行成功，不再依赖完整RegistryProbe类模板。
动态证明原1个与新2个声明；更多数量未完整启动验证。构造器/普通Item supplier业务子形态仍受限，
捕获、altMetafactory、跨类/任意factory、一般Registry/RegistryObject/Block/Capability/事件新API/Fabric不支持。
不自动合并、不发布、不启动006B。新客户端人工验收是独立剩余门槛。
