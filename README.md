# DualForge

010B 新增受控三类闭包的官方双 Bus、显式 Class/Consumer 和布尔配置适配；两个原始 Forge 身份在双平台验证，仍不支持真实 JEI 完整入口。当前 JEI 在新 profile 的构造器模板预计划处拒绝，整体 PARTIAL，客户端 NOT_RUN、不生成 JEI ZIP。见 [010B 契约](tests/real_mods/jei/dual_bus/README.md)；010A/009C 的既有结论保持各自历史含义。

010A完成原始JEI Forge19.27.0.340完整取证及真实Forge基线，Neo仍在严格预计划以`LISTENER_CONTRACT`拒绝。生产零修改，整体PARTIAL，尚无JEI兼容客户端包；见[010A报告](<Dev Manuals/Convergence-010A JEI 交付报告.md>)。

2026-10-18：009C清单全部项目已获 **USER_REPORTED_MANUAL_PASS**，包括原生/兼容双实例、鼠标/配置持久化及FnF+Clumps混装。新日志与Windows安装SHA未提供独立复核，见[人工验收补充](<Dev Manuals/Convergence-009C Windows 客户端人工验收补充报告.md>)。以下009C交付时的NOT_RUN为历史状态。

009C 新增有限客户端鼠标事件 Profile，已用原始 Mouse Tweaks Forge 2.26 完成严格准入、真实原生/Neo事件 API 隔离、原始配置初始化模型及完整客户端 ABI 链接。新 JAR 的 FnF、Clumps 和混装服务端回归另行通过。Windows 启动/鼠标/配置 UI 仍 NOT_RUN，整体 PARTIAL；见 [009C 报告](<Dev Manuals/Convergence-009C Mouse Tweaks 交付报告.md>)、[Windows 清单](tests/real_mods/mouse_tweaks/CLIENT.md)。用户已报告的 009A/009B 及此前 FnF/Clumps 人工 PASS 属于旧制品，不由下列历史 NOT_RUN 覆盖，也不继承到本轮新 JAR。

008A 的限定事件初始化 Profile 已用未修改的 Clumps Forge 19.0.0.1 完成 NeoForge 服务端启动、真实经验球合并/总经验对照及经验球重载；独立身份夹具和事件取消/可变值观察通过。真实玩家拾取、Mending 和新 JAR 客户端人工验收仍 NOT_RUN，整体 PARTIAL。见 [008A 报告](<Dev Manuals/Convergence-008A Clumps 交付报告.md>)、[测试入口](tests/real_mods/clumps/README.md)。这不是一般 Forge post API 支持。

007C-4C-5已用同一固定制品完成[真实服务端行为对照](<Dev Manuals/Convergence-007C-4C-5 交付报告.md>)：两种代表燃料的真实熔炉tick/槽位结果、Tab icon及66项display/search、全部58个Food属性与原生Forge一致，**4C5_SERVER_BEHAVIOR=PASS**。实际玩家食用、客户端UI和重启/物品持久化仍NOT RUN；没有生产兼容代码修改，不代表FnF全部功能已兼容。

007C-4C-4已合并基线曾用固定4C-3制品完成原始Food N' Fuel首次NeoForge服务端运行：**FNF_SERVER_BOOT=PASS**，原生Forge/Neo只读对照确认67个Item、Holder及Creative Tab。该阶段实际玩家食用、燃料消耗、客户端UI和重启/物品持久化均NOT RUN，不能据此宣称完整适配。本轮不修改生产。见[运行报告](<Dev Manuals/Convergence-007C-4C-4 交付报告.md>)和[重跑入口](tests/real_mods/food_n_fuel/runtime/README.md)。

历史007C-4B阶段新增受控同JAR Registry Helper/Consumer闭包和构造期Mod Bus桥接，使用原生Forge夹具验证实际Item注册；旧Profile保持原严格契约。范围、最终同JAR回归及未运行项见[4B交付](<Dev Manuals/Convergence-007C-4B 交付报告.md>)和[测试入口](tests/helper_registry/README.md)。该阶段FnF新Helper计划只读拒绝，运行NOT RUN；该历史结论与4A PARTIAL不改写。

历史007C-4A阶段新增[独立API图谱与离线ASM研究工具](tools/api-atlas/README.md)，生产目录不变；缺少完整客户端补丁输入，整体PARTIAL。原FnF运行FAIL仅继承，未重跑。见[研究交付](<Dev Manuals/Convergence-007C-4A 交付报告.md>)；候选映射不代表生产支持。

DualForge 是以 NeoForge 为宿主的受控 Forge 入口兼容原型。当前实现包括入口Profile/Context、受控Registry Helper、Supplier、Typed Registry及Global Bus组合；007C-2的平台依赖契约继续保留。007C-4C-4只验证既有生产制品的真实运行。唯一生产源码位于 [dualforge/src/main](dualforge/src/main)，构建生成 `dualforge.jar`，交付副本命名为 `DualForge.jar`。Context历史结果与边界见 [007C-3报告](<Dev Manuals/Convergence-007C-3 交付报告.md>)；历史结果见 [007C-2报告](<Dev Manuals/Convergence-007C-2 交付报告.md>)、[007B报告](<Dev Manuals/Convergence-007B Drop-in Runtime 交付报告.md>)、[007A报告](<Dev Manuals/Convergence-007 Drop-in Runtime 交付报告.md>)及[006B报告](<Dev Manuals/Convergence-006B 交付报告.md>)。

仓库只维护 **zsh 脚本入口**。Python 3 标准库负责内部的构建编排、JSON、JAR 指纹、进程及日志捕获；运行不需要 Windows 或 PowerShell。旧Profile保留原单参数构造器ABI；新Context Profile仅支持下述严格无参入口。

## 用户安装

原始Food N' Fuel 1.3的指定JAR已在MC1.21.1/NeoForge21.1.256/Java21上完成服务端启动与注册查询；完整玩家功能和客户端仍未验收。制品SHA及限制见[当前运行报告](<Dev Manuals/Convergence-007C-4C-4 交付报告.md>)和[真实兼容范围](docs/COMPATIBILITY.md)。[007C-3](<Dev Manuals/Convergence-007C-3 交付报告.md>)的构造期失败及更早冻结事实保留为历史结果。

正常安装 NeoForge 后，将 `DualForge.jar` 和当前受支持的原生 Forge JAR 放入 `mods/`，照常启动；不需要 DualForge JVM 参数或开发工具。支持边界与诊断见 [安装指南](docs/INSTALL.md)。默认启动成功不代表任意 Forge 模组兼容；本轮 Mac 图形验收 NOT RUN。

## 仓库结构

生产源码保持在 `dualforge/src/`；`history/` 集中保存旧 compat 迭代、0l 冻结副本和旧实验记录。工具分为 `tools/entry/`（zsh 入口）、`tools/lib/`（Python 实现）和 `tools/harness/`（活跃独立 harness）。`tests/` 的文件位置保持不变。

完整目录树、活跃／历史状态和迁移映射见 [STRUCTURE.md](STRUCTURE.md)；按功能查找实现和报告见 [docs/README.md](docs/README.md)，测试方式见 [tests/README.md](tests/README.md)。早期 Spike 归档于 `history/early-spikes/`，默认服务端输入为 `runtime/neoforge-server/`。Gradle 历史弃用说明见 [DEPRECATED.md](history/early-spikes/discovery-001a/DEPRECATED.md)，原文命令仅供历史追溯。

## 当前支持范围

- 003a：按受控 Forge metadata 发现 JAR，以规范化路径交由官方 FML pipeline 去重；不依赖固定文件名。52下界是003a当时策略；007C-1改以同JAR Forge注解归属确认来源，独立要求range兼容当前52 ABI。
- 003b：根据所属 modId 的 Forge `@Mod` 扫描确定入口，仅转换该入口与同制品内实际引用的受控 Event。原 ForgeProbe 与不同身份 IdentityProbe 已分别完整加载。
- 003b 证明 register 真实返回；004 证明一个受控自定义 Event 在正常启动后的宿主全局总线上真实 post 一次，注册组回调一次、未注册组零次。测试型 NeoForge driver 不属于 Forge API 兼容能力。
- 支持原单参数 Forge IEventBus 构造器、空 listener，以及严格四条打印指令的 marker-only listener；Event profile 和构造器 ABI 保持不变。
- metadata 使用宿主NightConfig标准TOML，先校验类型和语义再交给真实FML；保留依赖 owner 分组及 mandatory 到 FML REQUIRED/OPTIONAL 的适配。
- 007C-2：只对已确认的同JAR Forge来源、当前精确宿主，按参考来源ABI Forge 52.1.0校验平台范围。active required/optional且ordering NONE的`forge`项通过后从FML实际依赖列表投影移出，原依赖及决策另行保存。其他依赖仍交官方FML；原生NeoForge不投影，没有伪造forge模组或新JVM参数。详见[契约](<Dev Manuals/Convergence-007C-2 Forge依赖契约与验证.md>)及[测试入口](tests/real_mods/food_n_fuel/dependency/README.md)。
- 007C-3：BLOCK/NORMAL_ITEM/LEGACY_ITEM/EVENT/CONTEXT独立选型；无匹配或冲突明确拒绝，旧验证器不放宽。Context仅支持已确认归属的public无参入口、唯一直接get/getModEventBus链和严格副作用集合。独立命名空间shim返回来源ABI关联视图，delegate为当前容器真实Mod Bus；不创建新Bus，不实现新的Forge总线操作，也不改变helper或Registry字节。见[契约](<Dev Manuals/Convergence-007C-3 Profile与Context契约.md>)和[测试入口](tests/entry_context/README.md)。
- 005：一个扫描归属明确、严格结构匹配的 Forge DeferredRegister<Item> 入口；转换创建、单 Supplier、MOD bus 挂接、holder 字段及 get 引用。原构造器 ABI 不变，真实注册 C/T 与旧回归通过。物品 `registryprobe:ice_ether` 已打包用户原始 16×16 RGBA PNG，字节与来源 SHA 严格一致；原 005-S 与资源静态验收 PASS，原 005-C 在用户 macOS arm64 受控人工验收中 PASS；该客户端结论只属于历史 cohort，新制品 Mac 验收为 NOT RUN。详见 [005 报告](<Dev Manuals/Convergence-005 交付报告.md>)。

006A 在保持原构造器 ABI、Mod Event Bus 和 Supplier 业务字节不变的前提下，将整类固定计数改为逐 API 调用点与数据依赖验证。一个原生 Forge 模组通过同一个 DeferredRegister<Item> 声明 lambda Alpha 与静态 method reference Beta，006A-S 原冻结 fixture 动态 PASS；Beta 资源更新版 006A-C 在用户 macOS arm64 人工验收中受控 PASS，见 [客户端报告](<Dev Manuals/Convergence-006A-C macOS 客户端人工验收报告.md>)。Codex仅取得用户控制台摘录/审查报告，未独立取得原始日志或截图。详见 [006A 报告](<Dev Manuals/Convergence-006A 交付报告.md>) 和 [新测试入口](tests/multi_registry/README.md)。

这是受控原型，不代表任意 Forge 模组兼容。一般 Forge post API、取消／优先级／自动／静态订阅、任意多 Forge 模组混合业务、一般 RegistryObject API、一般Block／自定义 Registry、Capability 和 Fabric 尚未验证或实现。受控真实投递和双Context夹具不代表一般Forge Event或多模组业务兼容。历史 PASS 见交付报告；每轮运行以新生成的 `result.json` 和原始日志为准。

006A 双 Item 的独立人工输入包说明见 [006A Mac门槛](tests/multi_registry/MAC-SETUP.md)，Beta 资源更新版客户端受控人工验收 PASS；原服务端 fixture 与新客户端 fixture 分别记录。
原005 fixture 的命名迁移 cohort 部署工具见 [Mac kit](tests/registry/mac/README.md)：
使用实际重新编译并完成服务端回归的 `dualforge.jar` 和原字节 RegistryProbe，
提供 Prism/Java/JVM 检查、安装回滚及人工清单。新 cohort 的 Mac 实机验收 **NOT RUN**。
旧已验收 kit 保留在 [Legacy 流程](tests/registry/mac-legacy/LEGACY.md)，适用于原始固定 SHA；
其历史 [005-C 验收](<Dev Manuals/Convergence-005-C 客户端验收与脚本修复.md>) 不迁移为新制品结论。

## 环境与外部输入

| 组件 | 版本 |
|---|---|
| Minecraft / NeoForge | 1.21.1 / 21.1.256 |
| FancyModLoader / ModLauncher | 4.0.45 / 11.0.5 |
| Microsoft OpenJDK | 21.0.10+7-LTS |
| securejarhandler / NeoForge bus | 3.0.8 / 8.0.5 |
| ASM / asm-tree / asm-analysis | 9.10.1 |
| slf4j-api / maven-artifact | 2.0.9 / 3.8.5 |
| mergetool API（官方Dist类型） | 2.0.0 |
| 工具 | zsh、Python 3.11+；服务端运行另需 pgrep |

通过 `JAVA_HOME` 或 `--jdk-home` 选择 JDK。克隆仓库后仍需准备以下外部输入：

- 完整 NeoForge 服务端安装的 `libraries/`，包含官方 `net/neoforged/neoforge/21.1.256/unix_args.txt` 和 Minecraft 服务端。
- 原固定 Probe：`forgeprobe.jar`，SHA-256 为 `a774d2f737b17f089046014ab55a53767dbad008cd44dcce52c727b87e559ae8`。原回归入口拒绝重建或替代输入。
- 同一服务端输入下的 `config/`、`defaultconfigs/` 和 `spike000world/`。空的 defaultconfigs 也须保留目录；世界与第三方运行库不入 Git。

`tools/entry/prepare-unix.zsh` 只取得并校验十二个固定编译 JAR（含受控栈验证所需asm-analysis），不安装完整服务端、不重建 Probe、不启动 JVM 服务端。旧兼容层 Gradle 记录已归档为 `history/compat-build.gradle.deprecated`，当前入口不使用 Gradle。

## 构建与回归

在仓库根目录执行。以下变量须指向你实际准备的输入：

```zsh
export JAVA_HOME='/absolute/path/to/msopenjdk-21'
DF_SERVER_INPUT='/absolute/path/to/server-base'
DF_PROBE='/absolute/path/to/forgeprobe.jar'

# 仅准备编译依赖；已有完整安装时也会核验现有文件。
zsh tools/entry/prepare-unix.zsh --server-input "$DF_SERVER_INPUT"
zsh tools/entry/dualforge.zsh --mode build --server-input "$DF_SERVER_INPUT"

# 取上一步 result.json 中实际新制品路径，保留文件名。
DF_COMPAT_JAR='/absolute/path/to/new-run/dualforge.jar'

# 仅执行 metadata 边界，不启动服务端。
zsh tools/entry/test-forge-dependencies.zsh --stage after --boundary-only \
  --server-input "$DF_SERVER_INPUT" --probe "$DF_PROBE" \
  --compat-jar "$DF_COMPAT_JAR"

# 原 Probe 隔离 smoke；所有 JVM 命令必须串行。
zsh tools/entry/dualforge.zsh --mode smoke \
  --server-input "$DF_SERVER_INPUT" --probe "$DF_PROBE" \
  --jar "$DF_COMPAT_JAR" --allow-server-start

# metadata + C/D/E/F 动态依赖正负例。
zsh tools/entry/test-forge-dependencies.zsh --stage after \
  --server-input "$DF_SERVER_INPUT" --probe "$DF_PROBE" \
  --compat-jar "$DF_COMPAT_JAR" --allow-server-start

# 003a 发现与边界测试。
zsh tests/discovery/run-discovery.zsh \
  "$DF_COMPAT_JAR" "$DF_SERVER_INPUT" "$DF_PROBE"
```

`--mode regress` 合并干净构建和原 Probe smoke；独立 `--mode smoke` 核验指定新 JAR。IdentityProbe 原生 Forge 编译、修改前复现、完整加载和范围对照的 zsh 命令见 [tests/identity/README.md](tests/identity/README.md)。

以下历史隔离回归入口需要 `--allow-server-start`；普通用户标准NeoForge启动不使用这些开发入口。入口要求没有其他 Java 进程且 25565 可用；运行物理复制完整输入，绑定 127.0.0.1，停止后等待 JVM 退出。固定 server.properties 由内部实现直接生成，运行不读取旧脚本。dump 写入每轮新目录，原安装、输入和世界均复核指纹。

云端代理／CA 和 Authlib public-key relay 的实际参数见 [tools/UNIX.md](tools/UNIX.md)。历史严格回归在当前云环境使用 `--java-network-args /workspace/dualforge-cloud/java-network.args --public-key-relay`；普通本地环境不应照搬云端文件路径。

## zsh 入口与结果

| 入口 | 用途 |
|---|---|
| `tools/entry/prepare-unix.zsh` | 准备十二个固定编译输入 |
| `tools/entry/dualforge.zsh` | build／smoke／regress |
| `tools/entry/test-forge-dependencies.zsh` | metadata 与动态依赖回归 |
| `tools/harness/run-server.zsh` | 独立隔离服务端 harness |
| `tests/discovery/run-discovery.zsh` | 发现与候选边界 |
| `tests/identity/run-identity.zsh` | 原生 Forge 身份 fixture、before／after |
| `tests/identity/run-scope.zsh` | 实际 FML 扫描和转换字节范围对照 |
| `tests/events/run-events.zsh` | 004 原生夹具、字节契约、真实投递 C/T 和旧回归 |
| `tests/registry/run-registry.zsh` | 005 原生单 Item、ABI／范围、真实注册 C/T、资源与客户端状态 |
| `tests/multi_registry/run-multi-registry.zsh` | 006A 独立原生双 Item、调用点/数据依赖、范围/拒绝/原子性及 C/T |

每轮输出在新建的 `out/` 子目录；`--output-root` 可选择仓库 out 下的其他位置。保存实际命令、工具版本、输入与 JAR entry SHA-256、完整日志、退出记录和断言，不覆盖原服务器制品。

统一构建／回归退出码：0 PASS、10 构建失败、20 启动失败、30 断言失败、40 NOT RUN；已完成构建但未能回归时为 PARTIAL。独立依赖入口的失败码保持 1，前置条件不满足为 40。

smoke 不只检查 Done，还检查精确安装制品、服务发现、构造 marker、非空 profile 的真实 register 返回、builtin javafml 委托及 NeoForge 原生加载、正常 stop／exit0、无 ERROR/FATAL/dump FAIL、空 stderr 和无 crash。依赖负例按具体 FML 拒绝原因判断，不能仅凭退出码或没有 Done。

## 源码、历史记录与证据

- 当前生产源码：[dualforge/src](dualforge/src)；原 Probe 源码：[history/spike000/probe-002-0l/src](history/spike000/probe-002-0l/src)。
- 当前 fixture：[tests/dependencies](tests/dependencies)、[tests/discovery](tests/discovery)、[tests/identity](tests/identity)、[tests/events](tests/events/README.md)、[tests/registry](tests/registry/README.md)。
- 历史交付：[Convergence-001](<Dev Manuals/Convergence-001 交付报告.md>)、[002](<Dev Manuals/Convergence-002 交付报告.md>)、[003a](<Dev Manuals/Convergence-003a 交付报告.md>)、[003b](<Dev Manuals/Convergence-003b 交付报告.md>)、[004](<Dev Manuals/Convergence-004 交付报告.md>)。
- 架构及冻结报告：[Dev Manuals](<Dev Manuals/>)。历史文本仍保留原平台命令与证据路径，作为当时的执行记录，不作为当前入口。

此前 zsh 工具整理移除了全部 29 个受 Git 管理的 Windows／PowerShell 脚本；该轮原字节副本和指纹存于外部归档 `out/zsh-only-tooling/`，也可从此前 Git 提交恢复。此前仓库结构整理仅使用 git mv 归档现有文件，未删除文件。历史 Java 源码、资源、报告及已有证据索引保持原字节，不批量改写历史引用，也不重写 Git 历史。

Git 保存第一方源码、fixture、zsh 入口、内部实现、报告和指纹索引；完整运行附件、第三方依赖、构建产物、世界、缓存及凭据继续外部保存。`.gitattributes` 保留原文件换行与字节。既有 LICENSE 未修改。

## 当前运行身份

生产 package 为 `io.github.gfander.dualforge`，自动模块为 `dualforge`，
ModLauncher 服务为 `dualforge-transform`。LanguageLoader 保留 `javafml` / `9999` 契约。
四个生产开关未设置时默认true；开发者显式覆盖可用 `-Ddualforge.2a.rewrite=false` 等。属性为 `dualforge.2a.rewrite`、`dualforge.0g.apiRewrite`、
`dualforge.0j.emptyRegistration`、`dualforge.0l.subscriberRewrite`；非法布尔值明确拒绝。
默认不创建dump；仅显式设置 `dualforge.dump.path` / `dualforge.dump.profileDir` 才输出，I/O失败拒绝转换。
新工具不接受历史制品或旧参数混用；当前身份检查入口见
[project_identity](tests/project_identity/README.md)。所有历史 API/ABI 支持范围保持原样。

006B新增独立Block/BlockItem受控profile，保留006A Item规则和原构造器ABI；支持范围与实际证据见 [006B报告](<Dev Manuals/Convergence-006B 交付报告.md>)、[测试入口](tests/block_registry/README.md)。006B-C当前为 **PASS（用户macOS Apple Silicon实机人工验收，本轮截图WAIVED）**，见[人工验收报告](<Dev Manuals/Convergence-006B-C macOS 客户端人工验收报告.md>)；不继承旧客户端cohort。

最新客户端输入已将实际对象统一为ice_ether、raw_ice_ether、ice_ether_block，Java声明、Registry路径、资源同名，原冻结合约回归保留；见[client_objects](tests/client_objects/README.md)及[交付](<Dev Manuals/Ice Ether 实际对象命名统一交付.md>)。两组服务端Contract/C/T为此前实际PASS；Block组Mac人工验收现已PASS，新版双Item组Mac验收仍NOT RUN。原始服务端证据ZIP已在007前置检查中独立远端下载并逐项核验，当前PERSISTED / VERIFIED；后续客户端输入/人工报告不属于该ZIP覆盖范围。

007零参数SPI/启动、原生共存与诊断验证见 [bootstrap](tests/bootstrap/README.md)。Clumps的007历史TOML准入失败保持记录，007C-1只静态重验其完整TOML；整体007不因启动成功而宣称PASS。

007B在已识别的Forge入口内支持无条件单Mod Bus挂接与纯Item工厂；真实双Item Registry/Holder/对象身份通过，匹配的历史ModuleDescriptor不变，automatic名称不匹配使用官方FML metadata，显式JPMS不匹配拒绝。新客户端包见 [dropin_items](tests/dropin_items/README.md)，Mac零参数图形验收NOT RUN。Clumps功能未重新运行，不因007C-1的静态解析PASS宣布兼容。
