# Convergence-007C-2 交付报告

日期：2026-10-15（Asia/Shanghai）。任务范围仅Forge平台依赖，未进入新构造器、跨类Registry、SRG或Food/Fuel/Tab实现。

结论：**受控平台依赖目标PASS；原始Food N' Fuel运行FAIL。** 最终同JAR依赖矩阵19/19及历史回归见本轮证据索引；原生Forge基线仍PARTIAL，Mac/玩家食用/客户端显示/持久化未重新验收。平台版本接受不是Forge运行时/API兼容。

## 基线、输入与最终构建

- 分支：`codex/convergence-007c2-forge-dependency`；独立PR [#20](https://github.com/GFander/DualForge/pull/20)，base `main`，不自动合并。
- 起点main：`acf363b84ee1efbce15c98c8e77a3dab693251ca`，已包含PR #19；旧PR HEAD `fdbb206998e2033a94209cb97093434f16d020f9`。起点工作区干净，未混入旧任务改动。
- 设计先行提交：`bf2a9b5`；实际最终被测生产/测试源码提交：`a883c777b607e165a99d5247e32179398dee12e9`。从该提交新建干净detached验证worktree；其后只更新文档和证据索引，生产源与已测提交逐文件SHA相同。
- 官方原FnF：128098字节，SHA-256 `bd415b04529c4ffc31e14757488f71dab6917e4560c61e236c3c4d0de6557958`；未改、未重编、未重包，CRC及全JAR成员SHA核对，运行前后核对。
- 最终JAR：82213字节，SHA-256 **`ddecc827733fac48076eef71aec2eeb51bd72d8c1720861ed07634803b4c28bc`**。构建名dualforge.jar，交付物理副本DualForge.jar；两者SHA相同，全部最终Contract、标准启动和历史回归绑定此SHA。
- 实际工具链：Microsoft OpenJDK21.0.10+7-LTS；MC1.21.1、NeoForge21.1.256、FML4.0.45、ModLauncher11.0.5。其余实际工具版本、编译输入和生产源逐路径SHA见final-cohort/build记录。为直接读取官方Dist类型添加官方已有mergetool2.0.0-api编译输入，固定编译依赖10→11，SHA `0260881294d8b218afe3d673f084d3745a55c28b35205d07d5e62d7f22202a00`。
- Git最终文档HEAD及Actions以PR当前head与本轮`git-finalization.json`/交付回复的精确SHA为准；不把被测源码SHA冒充最终文档HEAD。没有合并、发布或改写历史。

## 原失败、FML取证与设计决定

本轮先用未改生产基线干净构建（SHA `a2bd5ff5a56d0a0690d9d627bb0a4692d496aeaa86410a22404b956c3991d79c`，所有JAR成员等于007C-1最终制品），串行实跑N0/N1/N2。前两组正常；原FnF实际进入Reader/metadata，再被官方FML以required `forge [0,)`、actual `[MISSING]`拒绝，未进构造器。原始生命周期FAIL、host exit0、crash记录保留，没有将宿主退出码当兼容成功。

[FML4.0.45调用链及描述符](Convergence-007C-2%20FML依赖验证链取证.md)证明：ModSorter从真实IModInfo构建唯一modId/version表；没有按消费者限定的虚拟provides。language验证先于dependency sort；依赖接受后才会继续扫描和transformer targets。VersionSupportMatrix、FML-System-Mods不能产生受控forge能力；dependencyOverrides不是正式方案。版本52.1.0、21.1.256、provider4.0.45/9999分别代表不同事物。

[A/B/C比较](Convergence-007C-2%20平台兼容方案比较.md)在生产修改前提交：选择A的独立校验后内部投影，不创建forge身份或容器，不碰官方核心/Layer；B的官方metadata节点是全局身份，不能隔离原生消费者，生命周期/Layer/split-package也未验证，因此不实施；C保留为语义/回归不能成立时的严格拒绝方案。内存模型与额外Reader观察SPI仅DIAGNOSTIC_ONLY，未进入最终双JAR运行。

[先行契约](Convergence-007C-2%20Forge依赖契约与验证.md)：来源必须是同JAR真实Forge @Mod/metadata归属；实际宿主精确匹配。active required/optional的Forge范围须包含参考ABI52.1.0且ordering NONE。BEFORE/AFTER因无真实平台排序节点拒绝。inactive side原项保留给FML，不能称其版本通过；其他依赖仍使用官方required/optional、range、side和拓扑排序。

**内部FML依赖表示确实改变：** active成功的forge项从实际getDependencies移出，保留完整原声明/适配前置模型到`dualforgeOriginalDependencies`，逐项owner/range/side/ordering/host/版本决策到`dualforgePlatformDependencyAudit`，并输出日志。不是“原始声明原样交FML”，也不是无条件删除。失败不污染原map，不共享跨文件成功状态。

## 生产与测试修改

生产只修改Parser的最终ModFileInfo建模交接并新增独立ForgePlatformDependencyResolver；无平台依赖保持旧流程，原生来源不投影。共享Locator/Reader guard、LanguageLoader、Transformer、Item/Block profiles、构造器ABI、Module Layer、宿主配置、原发行和冻结输入均未修改。

测试在`tests/real_mods/food_n_fuel/dependency/`新增真实FML sorter Contract、Forge-only A/B夹具和标准矩阵；生产无fnf或新fixture身份白名单。A/B从保存源码用固定Forge52.1.0/eventbus编译，未使用NeoForge/DualForge依赖；新JAR、源、class和实际命令有SHA记录。沿用原单bus/空Event profile，无新事件能力。

007C-1 Contract迁移保留原两依赖及字段的所有断言，放在真实未投影ModFileInfo上；另加实际Reader投影模型和审计检查。原解析/准入负例继续执行。007C的旧Reader-decline Contract代表已被007C-1替代的历史边界，未冒充当前期望；原Java文件不改。本轮重新执行原发行只读ASM全节点对照与原字节保全，原生游戏功能未重跑。

## 本轮最终JAR实际结果

| 验证 | 实际结果 |
|---|---|
| 平台Contract | 91断言PASS；真实FML sorter、四范围正例、53/52.2/非法范围、side、required/optional、ordering、host、类型/冲突、失败原子性、普通依赖拒绝和原生隔离 |
| 007C-1 metadata/身份 | 93断言PASS；原声明与实际投影模型分开，Clumps仅静态TOML，未运行其功能 |
| N0/N1/N3 | 标准启动、Done/stop/exit0，N1/N3错误类型/计数与本轮N0严格一致 |
| N2 原FnF | 平台依赖PASS；下一ASM targets/profile拒绝，原始生命周期FAIL/JVM exit1，stderr完整栈，未调用FnF loadMod/构造器 |
| Native/combined | 原生NeoForge单独及与普通Forge Item/查询观察器共存PASS；真实查询两Item、对象/Holder、官方注册栈，原生构造/started marker正常 |
| A/B平台范围 | A版本接受；同次安装B [53,)明确拒绝，B未构造，整体宿主FAIL保留；非全局平台成功缓存 |
| 其他依赖 | 缺失真实required、低required、错MC、低installed optional均由具体官方FML原因拒绝；optional absent接受；CLIENT inactive保留；普通AFTER排序由构造顺序验证 |
| 平台side/ordering | CLIENT inactive保留原要求，服务端正常；active AFTER明确拒绝；客户端语义仅离线模型，无图形客户端PASS |
| Native Forge声明 | 原生required forge仍由FML报缺失，未投影；原生optional forge缺失按FML忽略并正常启动 |
| 002/Probe、003a/003b、004、005 | PASS；保持既有严格断言，全部绑定最终同一SHA |
| 006A/006B | PASS；保持既有严格断言，全部绑定最终同一SHA |
| 007A/007B | Bootstrap Contract、stock零业务参数矩阵；007B严格13负例+1显式JPMS拒绝/范围/原子性PASS |
| 静态CI等价检查 | 44个Python单测、374项冻结保护、全zsh语法及diff检查；最终文档提交前记录在static-checks |

19组矩阵每组保存实际命令、/proc JVM、安装JAR指纹、stdout/stderr/debug、exit和独立gate断言。所有JVM严格串行、物理复制。最终标准组无DualForge属性、Agent、测试SPI、额外模块参数或依赖覆盖。历史严格回归继续显式C/T及既有云代理/public-key relay；不能把这些开发命令说成零配置用户方式。

标准云N0存在首次缺server.properties与Mojang公钥网络连接的两类既有ERROR，stdout/debug/latest重复呈现；健康标准组逐key/计数完全一致。因此`no new baseline errors=PASS`，**严格zero-ERROR不是PASS**。历史harness生成配置并沿用已验证网络适配，严格错误门禁不放宽。预期负例中的FATAL/crash不隐藏，也不算健康启动PASS。

## FnF下一真实失败与停止点

最终N2只安装本轮DualForge.jar和原发行FnF。真实平台审计VERIFIED，FML模组列表包含fnf，继续到Transformer.targets。首异常：

```text
java.lang.IllegalStateException: 005 unsupported Item profile: class structure
  ControlledForgeItemProfile.require(:356)
  ControlledForgeItemProfile.plan(:105)
  ControlledForgeItemProfile.validate(:82)
  DualForgeEntryTransformer.targets(:375)
  TransformationServiceDecorator.gatherTransformers
  TransformationServicesHandler.initialiseServiceTransformers
```

`targets()Ljava/util/Set;`，`validate(ClassNode,String)Stats`与`plan(ClassNode,String,boolean)Plan`完整descriptor/字节码见release-forensics/javap-production。只读ASM及原入口javap确认原FoodNFuel access33（public、非final），当前profile先要求public/final/super及严格class结构；这是实际第一拒绝条件。原构造器`<init>()V`仍未执行，不能把旧静态ABI差异说成本次运行构造器异常。

最后确认的推进是metadata/语言选择/依赖排序与扫描后到真实targets校验；没有FnF目标转换完成、GAME层容器就绪或API成功证据。不修复该失败。FMLJavaModLoadingContext、跨类注册、CreativeModeTab、FuelItem和SRG映射继续不支持或未验。

## 证据、重跑与保全

完整逐文件路径/大小/SHA与各阶段入口见 [证据索引](Convergence-007C-2%20证据索引.json)。任务取证根`/workspace/DualForge-007C2/out/convergence-007c2/`，最终运行根`/workspace/DualForge-007C2-validation/out/`；这些是本地路径，不是持久远端下载链接。

最小重跑：从被测源码提交建立干净worktree，按README准备官方安装/JDK和原冻结输入，运行zsh构建入口；使用实际build-result的同一个新JAR，再按dependency README运行build_fixture.py/run_contract.py/run_matrix.py。每项最终原始invocation.json和command.json给出本轮精确命令；历史完整调度与已核对运行库清理脚本保存于history-driver。不得混入旧cohort。

选择性原始证据ZIP：`/workspace/DualForge-007C2/out/convergence-007c2/evidence-export-final/convergence-007c2-selected-evidence.zip`，5656216字节，SHA-256 `054f1e5120791cd3bb0cdf121cda6bb06c077150740dd0f111eb355619f67663`，3202项原字节载荷，另含MANIFEST/README/SHA256SUMS三文件。ZIP CRC、逐项SHA、全新临时目录解压复核、文件数和无symlink/路径穿越均PASS；原始文件归档前后不变。排除世界、第三方运行库/JAR、配置/缓存及旧工作区全集，不能称完整原工作区副本。安全模式扫描未发现凭据或私网地址命中；绝对工作区路径保留，公开前仍需内容审查。**ARCHIVE_READY / NOT PERSISTED**：本轮未上传远端、未做远端下载复核，无持久远端链接声明。归档后新增的JAR成员对照、解压复核和最终Git/CI元数据独立记录，不声称它们已在该ZIP内。

本轮前后核对631个起点tracked文件，变化限于授权文件；374项冻结保护不减少。007C-1全部2554个原始文件再次逐大小/SHA核对。保全检查另外发现两处旧清单指向已由历史提交更新的可变源码：006A Beta贴图（6ea5b965…）与006B runner（90d566ea…），实际字节均匹配该旧worktree已提交且干净的版本，未被本轮修改；这些旧引用不伪装成当前输入SHA，详见preservation-historical-source-note。原冻结fixture JAR及本轮实际runner另行独立指纹核对。所有原始记录保持原字节；只清理本轮已结束且逐JAR匹配来源的运行库物理副本，保留清理收据、世界、日志、命令、结果和dumps。结束时JVM为零、25565可绑定。

## 最终状态

```text
CONVERGENCE_007C_2:
FML_SOURCE_CHAIN: PASS
DEPENDENCY_DESIGN: VERIFIED
PRODUCTION_IMPLEMENTATION: PASS
FORGE_PLATFORM_VERSION_CHECK: PASS
FORGE_DEPENDENCY_RESOLUTION: PASS
OTHER_REQUIRED_DEPENDENCIES: PASS
NEOFORGE_NATIVE_ISOLATION: PASS
ZERO_JVM_PARAMETERS: PASS
FNf_NEXT_RUNTIME_STAGE: REACHED
HISTORICAL_REGRESSION: PASS
OVERALL: PASS

DEPENDENCY_STAGE: PASS
FOOD_N_FUEL_RUNTIME: FAIL
NATIVE_FORGE_BASELINE: PARTIAL (inherited, not rerun)
MAC_GRAPHICAL: NOT RUN
```

OVERALL只表示本轮受控平台依赖机制达到目标。没有执行下一阶段功能开发，没有自动合并或创建Release。
