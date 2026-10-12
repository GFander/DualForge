# Convergence-003b：受控 Forge 入口身份通用化

2026-10-12；结论：**受控范围 PASS**。本轮只消除入口身份限制，未扩展构造器 ABI、API owner/描述符或事件能力。使用了 Andrej Karpathy Skills 的最小改动、先复现与结果验证原则。

证据标签：`[E]` 为本轮动态／字节断言；`[F]` 为当前安装版本的静态契约；`[D]` 为实现选择与支持边界。完整索引为同目录 `Convergence-003b 证据索引.json`；原始证据根目录为 `/workspace/DualForge/out/convergence-003b/`。日志与运行目录时间使用执行器 UTC；本报告日期按用户 Asia/Hong_Kong 时区。

## 1. 前置检查、原身份限制和首次失败

`[E]` 已读取 README、Unix 入口说明、003a/002 报告、全部当前生产源码及相关原始日志。工作区及祖先没有适用 AGENTS.md。起点 HEAD 为 `9862621`，工作区已有 Locator/Reader 的003a改动、Transformer的dump I/O适配及Unix脚本；完整状态、diff、源码原字节、输入指纹在 `out/convergence-003b/preflight/`。这些既有变化没有算作003b修改。

按用户裁定，先将既有Linux工作基线、003a改动、报告和证据索引提交，得到 **`b763efa6ebba3c5ba495acd288ac373a8bd4a763`**。003a原报告按原字节复制入Dev Manuals，原证据不改。本轮起点源码全量干净构建件在 `before-build/20261012-104956-00e5e69f/`，其17个内容文件与已验收003a JAR全部相同：`production-content-proof.json`。

身份审计原始列表：`preflight/identity-occurrences.txt`。

| 位置 | 原限制或用途 | 本轮处理 |
|---|---|---|
| Transformer `targets()`、入口判断、API目标判断 | 固定 `com.example.forgeprobe.ForgeProbe` | 移除；由实际FML扫描确定目标 |
| Subscriber匹配、Event目标 | 固定 `onProbe`、`ProbeEvent`及其描述符 | 根据已选入口的实际订阅方法参数关联同制品Event，保留原profile |
| LanguageLoader | 原来扫描所有Forge `@Mod`，未按value关联所属modId；没有固定modId分支 | 增加TYPE和值匹配，复用003a准入检查，原生委托保留 |
| `ForgeEmptyRegistration` | 旧零方法ForgeProbe专属legacy-empty-noop | 原字节保留；本轮非空profile不进入该分支，真实bus.register仍被调用 |
| Transformer类名、旧Windows dump叶名 | 历史实现符号／调试输出路径 | 保留；不用于输入身份匹配 |
| 原smoke／依赖／发现fixture、harness默认JAR名 | 固定A输入的严格测试断言和安装默认值 | 保留；仅给harness增加可选B安装叶名 |

`[E]` 原A实际构造器描述符为 `(Lnet/minecraftforge/eventbus/api/IEventBus;)V`，public；另有一个默认Forge SubscribeEvent的public非static空实例方法，参数为final、无字段／接口、仅平凡构造器的Forge Event子类。必要转换仍为单参数构造器描述符、原EVENT_BUS/register成员序列、SubscribeEvent注解和Event superclass／super调用。

`[E]` B从保存的原生Forge源码干净编译：modId `identityprobe`，入口 `com.example.identitycheck.IdentityProbe`，辅助 `IdentityEvent`，JAR `identity-check.jar`。构造、register后的独有marker分别为 `IDENTITY PROBE CONSTRUCTOR REACHED`、`IDENTITY PROBE REGISTER RETURNED`。注册调用、空订阅行为及构造器形态与A相同。仅为观察register返回增加一行输出，没有新增注册或事件行为。

编译只使用原Probe同版本、同SHA的三个Forge依赖：javafmllanguage 1.21.1-52.1.0、eventbus 6.2.27、forge 1.21.1-52.1.0-universal；来源、下载结果及历史hash对照在 `forge-dependencies/downloads.json`。B编译classpath没有compat shim或NeoForge。`fixture/20261012-105146-57a9e7aa/` 保存源码副本、编译／打包／javap命令、stdout/stderr/exit、输入hash和JAR entry hash；断言原JAR含Forge Mod、IEventBus、SubscribeEvent、MinecraftForge和Event引用，且两个class没有NeoForge引用。构建后源码指纹不变。

首次实际服务端运行日志在 `before/20261012-105211-57acc274/`。B已由003a Locator发现一次、ForgeReader返回IModFile、未改Parser创建真实FML metadata；LanguageLoader输出 `identityprobe entrypoints=[com.example.identitycheck.IdentityProbe]`，FML创建对应FMLModContainer。但Transformer未收到IdentityProbe，FML构造阶段直接报告：

```text
Failed to create mod instance. ModID: identityprobe, class com.example.identitycheck.IdentityProbe
Mod constructor has unsupported argument interface net.minecraftforge.eventbus.api.IEventBus
```

构造和register marker都缺席。`[F]` FMLModContainer只注入NeoForge IEventBus；`[E]` 起点Transformer仅登记固定ForgeProbe目标，故B构造器仍是Forge类型。这是直接失败链，未由“缺少Done”推断根因。

首次真实运行的两个发现日志断言写错了FML实际格式，因此测试result为FAIL，日志中的产品失败有效且保留。修正测试格式后，原起点构建件重新串行运行，在 `before/20261012-105309-c90f2762/` 完成全部复现断言。该轮JVM exit0也未被当作B加载成功。另保留了fixture初次相对classpath错误及一次不存在fixture路径的准备失败；均未用于产品结论，见summary.json的nonConclusionFailures。

## 2. 新入口身份来源、生命周期和转换范围

`[D]` LanguageLoader与Transformer共用 `forgeEntrypoints(info, scanData)`：先调用**未修改的**003a候选准入函数，随后仅选择TYPE级Forge `@Mod`，且 `annotationData.value == info.getModId()`。入口名称来自annotation.clazz，未增加IdentityProbe或其他身份白名单。原生metadata即使同时有旧Forge注解，也返回空Forge入口集合，继续原builtin javafml委托。

`[F]` 当前ModLauncher11.0.5的Launcher在FML completeScan之后调用initialiseServiceTransformers，再创建GAME层；TransformationServiceDecorator调用现有transformers()/targets()注册目标。FML4.0.45已有LoadingModList和后台扫描；IModFile.getScanResult等待该扫描。静态源码及来源指纹保存于 `contract/`，动态日志证实该通道实际登记并执行。本轮没有新扫描架构、类加载器、ModuleDescriptor或Layer策略。

`[D]` Transformer从当前LoadingModList逐制品读取同一FML扫描。仅为选中入口检查METHOD级Forge SubscribeEvent，其void单对象参数必须是**同一制品扫描中的直接Forge Event子类**，且只允许一个关联Event。登记目标只有入口与该Event。匹配同包名、相同构造器或相同Event基类本身均不建立归属。实际transform还检查已登记集合；对未选中普通类直接返回原节点。已有pre-loading错误时不登记目标，保留FML具体依赖拒绝链。

`[D]` 保留原约束：构造器旧／新描述符及单EVENT_BUS/register(this)序列；入口总方法数2；实例listener public非static、唯一默认注解、仅RETURN；Event final非abstract、无字段／接口、仅`<init>()V`，精确ALOAD0/super()/RETURN。只放开身份名称，未新增ABI/API/事件语义。缺失／歧义关联Event等形态不在本轮支持范围。

## 3. 修改文件和关键差异

- `spike001a/compat-002-0l/src/main/java/com/example/forgecompat/ForgeCompatLanguageLoader.java`：按实际modId筛选Forge入口，保持真实FMLModContainer和builtin委托。
- 同目录 `ForgeProbeConstructorTransformer.java`：移除固定入口、listener名和Event类名目标；从已有扫描登记精确入口／关联Event，增加未登记类返回检查。
- `tools/unix_workflow.py`：harness签名新增 `installed_mod_name="forgeprobe.jar"`，复制时使用该参数。原A默认、MARKERS、正负断言、隔离流程全部保留；B实装名为identity-check.jar。
- `tests/identity/`：保存原生Forge B源码／资源、构建和动态runner、4个最小范围对照、实际FML扫描／ASM字节对照测试及重跑说明。
- README及本报告／索引：更新受控支持边界和证据路径。

`[E]` 新旧兼容层17件内容中仅LanguageLoader.class、Transformer.class变化，其余15件逐字节相同。Parser、Locator、Reader、依赖语义、ForgeEmptyRegistration、资源／SPI、模块策略及历史源码未修改。源码对照见 `source-and-script.diff`、`preflight/source-before/`、`source-after/`；制品对照见 `production-content-proof.json`。两个变化class均没有ForgeProbe或IdentityProbe的输入身份常量：`additional-direct-assertions.json`。历史dump路径中的ForgeProbe叶名仍是输出名称。

## 4. A/B和转换范围对照结果

| 验证 | 本轮实际结果 |
|---|---|
| B 修改前 | 完成发现／Reader／metadata／正确入口交接，随后被FML以未转换Forge IEventBus参数明确拒绝；复现PASS |
| B 修改后 | **PASS**；单次发现、精确身份关联、入口与Event各一次转换、构造／register返回marker各一次；Done、正常stop、exit0；24.0s |
| A 原字节回归 | **PASS**；原严格smoke全通过；Done、正常stop、exit0；23.0s |
| 原A必要转换保真 | 入口rewrite dump、入口profile和Event profile三件，与已验收003a对应字节全部相同 |
| Ordinary | 具有原Forge IEventBus构造器但无Forge Mod；不登记、字节不变 |
| UnownedEntry | Forge Mod的value不属于metadata modId；不登记、字节不变 |
| UnrelatedEvent | 同包的直接Forge Event子类但未被目标listener引用；不登记、字节不变 |
| NativeControl | NeoForge Mod类，具有同Forge构造器形态；不登记、字节不变 |
| 原生双metadata对照 | 实际native Reader／metadata／scan，包含与B相同的Forge入口字节；Forge入口集合和转换目标为空，入口／NativeControl字节不变，NeoForge注解仍在FML scan |

范围对照使用真实Reader、ModFileInfo、Scanner和LoadingModList，未初始化候选class，不冒称完整多模组启动。只对字节比较两侧作相同ASM规范化；保存before/after class与SHA。动态NeoForge宿主另由B、A、C/E日志证明builtin委托及原生加载正常。

B、A与C/D/E/F全部安装同一新兼容层SHA。B与A独立物理副本运行，从未同时安装。B/A正例stdout/latest/debug均无ERROR/FATAL/dump FAIL，运行stderr为空、无crash；真实非空profile经过NeoForge bus.register并返回，没有legacy-empty-noop。**未验证真实事件回调或投递语义。**

| 制品 | SHA-256 | 字节数 |
|---|---|---:|
| 新compat | `6a11f674544761262e1e73aa5e88506c67583adcd14bebc1c54c6ffd6a012f1f` | 31590 |
| B identity-check.jar | `95ad77b0c2f5005bd7e98af139a911559635d4dd55e25463052a463748161b22` | 2349 |
| A原forgeprobe.jar | `a774d2f737b17f089046014ab55a53767dbad008cd44dcce52c727b87e559ae8` | 2258 |

## 5. 原发现、metadata和动态依赖回归

`[E]` 原smoke未放宽任何既有断言。003a原发现runner重跑A–F和native-dual、缺失／错误loader、稳定多候选去重、歧义停止边界全部PASS；其输出仍在旧runner的默认根目录，但inputs.json明确是本轮003b新兼容层。A/B发现测试只证明文件名无关，C只证明metadata交接；不据此宣称多个Forge入口完整启动。

`[E]` Convergence-002六组metadata、owner隔离、字段保留、FML type/version/defaults和4个非法mandatory/type边界全部PASS。动态C/D/E/F原断言4/4 PASS：

| case | 判据 | 运行结果 |
|---|---|---|
| C 满足必需minecraft | 原构造、真实register、宿主、Done/stop全部通过 | 正常exit0，23.0s |
| D 缺失必需 | FML具体 `convergence_missing` / Requested by forgeprobe / Actual `[MISSING]`；pre-loading拒绝；无目标loadMod、构造、register | 预期拒绝，10.0s |
| E 缺失可选 | OPTIONAL保留，原构造、真实register、Done/stop通过 | 正常exit0，22.5s |
| F minecraft版本不符 | FML具体minecraft `[9.0,)` / Actual `1.21.1`；pre-loading拒绝；无目标实例化 | 预期拒绝，10.0s |

D/F的exit0、缺少Done不是PASS依据；负例ERROR/FATAL/crash为已断言的FML拒绝证据，不混入B/A正例“无新增错误”结论。

`[E]` 本轮实际工具链：Microsoft OpenJDK/javac/jar 21.0.10+7-LTS，Python3.12.14，zsh5.9，git2.52.0，Linux6.18.44 x86_64。服务日志、当前安装manifest及hash确认Minecraft1.21.1、NeoForge21.1.256、FML4.0.45、ModLauncher11.0.5、securejarhandler3.0.8、bus8.0.5和ASM/asm-tree9.10.1；原始版本输出在 `toolchain/`，没有用历史报告替代本轮版本读取。

46次本轮JVM命令（含工具版本、编译、打包、javap、metadata和服务端）均串行等待退出；记录的command与exit/meta时间无重叠：`serial-jvm-timeline.json`。继续使用Linux既有物理复制、127.0.0.1绑定、官方Unix命名模块argfile、原flags、隔离dump、验证上游TLS的loopback public-key relay。Windows入口未重新验证。

保全：任务起点3707项原输入／历史源码／已有证据全部hash不变；原世界22文件及文件集合不变；生产源文件集合不变，仅允许的两个Java文件变化。新发现输出是新增独立目录，没有覆盖旧证据。最终无java进程，25565可绑定并已关闭检查socket：`preservation.json`。核验脚本副本为 `audit.py`。

### 可复核路径和重跑命令

以下均为本轮实际目录，位于 `/workspace/DualForge/`：

| 证据 | 路径 |
|---|---|
| B原生构建及Forge引用 | `out/convergence-003b/fixture/20261012-105146-57a9e7aa/` |
| 修改前最终复现 | `out/convergence-003b/before/20261012-105309-c90f2762/` |
| 新compat干净构建 | `out/convergence-003b/after-build/20261012-105456-37beba5e/` |
| B动态及转换dump/javap | `out/convergence-003b/after/20261012-105533-335d4bcf/` |
| 范围对照及前后字节 | `out/convergence-003b/scope/20261012-105721-5d20727e/` |
| A原严格smoke | `out/convergence-003b/original-smoke/20261012-105822-807f9bef/` |
| 003a发现／边界重跑 | `out/convergence-003a/discovery/20261012-105901-e85949f0/` |
| 002 metadata和动态依赖 | `out/convergence-003b/dependencies/20261012-105923-d2b18a9e/` |

各动态目录保存 `result.json`、`assertions.json`、`inputs.json`／`harness-inputs.json`、`logs/spike002-0l-treatment/command.json`、`meta.json`、完整stdout/latest/debug/stderr，负例另有crash。构建目录保存原始命令／exit、build-inputs、jar-contents；范围目录保存真实FML断言stdout、fixture内容及before/after byte hash。总汇聚在 `out/convergence-003b/summary.json`。

完整可复制的顺序命令与外部依赖说明见 `tests/identity/README.md`。实际路径可直接取上述各 `result.json`；入口是 `zsh tools/forgecompat.zsh --mode build`、`python3 -B tests/identity/run_identity.py --stage fixture/before/after`、`python3 -B tests/identity/run_scope.py`、原smoke、原discovery和原dependency脚本。不得并行执行JVM命令。复现before必须使用已保存的起点构建件，不能以修复后的源码构建件冒充。

## 6. 未支持项与停止

`[D]` 52下界仍只是当前受控候选准入策略；未证明识别所有Forge模组。没有支持新的无参／多参数构造器、任意listener／Event继承／注解参数、多个关联Event、自动subscriber、跨JAR Event、多入口／多Forge同时完整启动、任意真实模组、Registry、Capability或Fabric。原Windows入口未重测。保留的FML静态能力不等于这些动态场景通过。

003b八项成功标准均满足；完成后停止，不进入下一阶段。003a接受基线提交如上；003b最终提交SHA由本轮Git交付记录和最终回复给出，避免报告内自引用提交SHA。
