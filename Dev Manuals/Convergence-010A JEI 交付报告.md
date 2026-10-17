# Convergence-010A JEI 交付报告

**整体PARTIAL。原生Forge服务端基线PASS；原始Forge JEI在NeoForge仍FAIL于严格预计划。生产源码零修改，没有010A客户端ZIP。** 本轮交付完整发行取证、真实首失败、官方API对照、独立原生观察与拒绝契约。不能把这些证据写成JEI已兼容。

记录日期2026-10-18（Asia/Hong_Kong）。所有运行、编译、SDK、世界与原始附件在SSD `/home/albert/Env/DualForge/runs/convergence-010a-jei/`，源码在SMB `/home/albert/Codex/DualForge-010A`。

## 009C验收与Git基线

用户在本次任务书明确反馈009C原CLIENT.md的全部A/B、鼠标、容器、配置保存重启及FnF+Clumps混装项目PASS。新增[Windows人工验收补充](<Convergence-009C Windows 客户端人工验收补充报告.md>)，证据类型 **USER_REPORTED_MANUAL_PASS**；`RAW_CLIENT_LOGS=NOT_PROVIDED`，用户Windows安装SHA未独立复核，Codex未重新执行Windows实验。原009C报告、索引、CLIENT.md与MATRIX.md原字节未改。

docs-only提交 `a613cd24326f83a2b1c3b96e81bf3eb3ea91dd0f` 已追加并推送[PR #33](https://github.com/GFander/DualForge/pull/33)，[该提交CI SUCCESS](https://github.com/GFander/DualForge/actions/runs/39052152768)。PR33仍OPEN/Draft，无正式review记录，继续等待审查；未合并。旧版gh pr edit因Classic Projects弃用失败后，改用官方REST更新同一PR正文，未重复创建PR。

实际fetch确认 `origin/main=2600fe85b0c85b3d79454db6a20a93d1a9219e7c`。010A按授权从PR33最新HEAD `a613cd2` 新建独立worktree与分支 `codex/convergence-010a-jei`，**PR base为 `codex/convergence-009c-mousetweaks`**。009C代码不是010A新增。研究工具提交 `c6d31d9d2367594442d08c0740957e742caefc5e`；后续只追加汇总器与交付文档，不改生产字节。原main工作区的用户未跟踪008A报告保持原字节、不暂存、不提交。

## 官方原始输入

| 角色 | 原文件 / 官方来源 | bytes | SHA-256 |
|---|---|---:|---|
| 唯一目标输入 | [Forge文件7420585](https://www.curseforge.com/minecraft/mc-mods/jei/files/7420585)，jei-1.21.1-forge-19.27.0.340.jar |1529402|`f5e61a001d57dd044d6f2ba255daf955c50d90e4173f8b4933ab0eb8cd386ed5`|
| 只读二进制参考 | [NeoForge文件7420587](https://www.curseforge.com/minecraft/mc-mods/jei/files/7420587)，jei-1.21.1-neoforge-19.27.0.340.jar |1529391|`8aaf547432f1b4958239b036356b910692fe40f858c3073d996f56bbf7c99826`|
| 本轮重建研究基线dualforge.jar | build-baseline/dualforge.jar；不是JEI可验收制品 |192754|`9ca6526b996132b4fbfbded6b0192f2e1f7d7399f6585118efd300fd5bb9a05a`|

本机inputs未发现指定JEI版本，使用官方ForgeCDN HTTPS下载，两个HTTP200、实际URL/响应头/大小/摘要完整保存。ForgeURL为 `https://mediafilez.forgecdn.net/files/7420/585/jei-1.21.1-forge-19.27.0.340.jar`；Neo参考为同域7420/587。没有从Neo包复制平台实现，没有将其安装为兼容输入；两份原JAR均未修改、重新打包或预转换。

## Metadata、模块与静态调用图

两包各1159项、923个ClassFile、无嵌套JAR、无显式module-info、多发行版本类或Mixin配置/类。Forge Manifest记录19.27.0.340；Forge TOML为javafml `[51,)`、单modId `jei`，硬依赖forge `[51.0.31,)`、minecraft `[1.21, 1.21.1)`，均BOTH/NONE，无另行声明的第三方硬/可选依赖。没有删除或修改依赖。原自动模块名 `jei.forge` 经已有官方ModJarMetadata规则规范为 `jei`；这不是010A新能力。

发行页的1.21.1标记与Minecraft范围表面不一致，但**实际原生基线通过**。固定Forge VersionSupportMatrix在MC1.21.1加入mod.minecraft=1.21；固定Neo版本矩阵也有该支持项。已保存两侧真实javap，不根据Maven区间字面错误宣布依赖FAIL，也不通过改TOML来通过。

原服务描述符 `META-INF/services/mezz.jei.common.platform.IPlatformHelper` 指向Forge `PlatformHelper`；Neo参考指向其自身实现。发现5个JeiPlugin注解。两者均有Access Transformer：Forge18条规则含SRG字段/方法名；Neo参考使用Mojmap，并有额外连接/酿造访问规则。**ClassFile业务成员的SRG计数为0，不表示AT已映射或应用。**

完整ASM usage包括两侧各923类SHA逐一对原件核验，Forge114941条引用记录、Neo114876条引用记录；包括声明、类型、局部、bootstrap和字符串，不能称为同数量实际调用。Forge ABI为674处引用、211个唯一符号、84个owner，1151个invokedynamic站点。

`analysis-committed/complete-callgraph.jsonl.gz`保留两侧完整引用图；`forge-api.json`为全部211符号逐项保存原类/方法/指令位置、来源描述符、真实Forge声明、Neo候选类型/描述符/声明、A–G分类、未知语义及拒绝条件；`official-platform-comparison.json`保留原/参考平台类的方法差异。候选只有研究意义，**没有一条因此批准为生产转换规则**。固定SDK44个含平台类JAR另行扫描；30项重复工具库类保持歧义，不选赢家，均属srgutils且不在这84个JEI Forge owner中；module-info按各artifact独立归属，不算全局冲突。

## 真实构造与API差异

原Forge入口为唯一@Mod的 `mezz.jei.forge.JustEnoughItems`，public无参。实际顺序：创建locale supplier →读取全局Bus与当前Mod Bus →创建双Bus PermanentEventSubscriptions →注册服务器配置 →创建网络处理器/实际channel →创建ClientSafeRunner →捕获safe runner的Supplier与DistExecutor安全分发。入口还有合成lambda/反序列化方法，不是009C的固定四监听器入口。

| 契约 | 真实发行/宿主差异与当前边界 |
|---|---|
| 构造器 | Forge`()V`；官方Neo参考`(IEventBus,Dist)V`，不能照搬Neo平台构造器替换原业务 |
| FML Context | 现有DualForge shim仅服务已准入Profile；不授予JEI任意Bus、registerConfig或完整容器接口 |
| 双Bus订阅 | 原helper保存两Bus，按IModBusEvent把显式Class/Consumer注册到对应总线，NORMAL/receiveCanceled=false；同名宿主addListener签名存在不证明整闭包已准入 |
| addListener | Forge`(EventPriority,Z,Class,Consumer)V`；Neo有对应描述符但类型身份不同，Consumer与事件类/生命周期仍需成组验证 |
| 服务器配置 | Forge registerConfig(ModConfig.Type,IConfigSpec)V；固定Neo的ModLoadingContext没有同描述符，官方参考从当前真实容器注册ModConfigSpec；配置生命周期不能NoOp |
| 网络 | Forge构造期ChannelBuilder/payloadChannel/play/serverbound/clientbound/add/build；官方Neo参考通过RegisterPayloadHandlersEvent/PayloadRegistrar注册，协议版本、可选连接、方向、handler/线程及权限不能靠包名替换证明 |
| DistExecutor | 原SafeRunnable含序列化lambda和安全分发；同名Neo目标不存在，官方参考使用注入Dist的客户端分支；不能删除或提前执行ClientSafeRunner |
| 客户端 | 原始多个Screen/Container/Key/Tooltip/Reload事件、FluidStack、模型/渲染及插件/运行时接口超出009C有限四事件契约 |
| Service与插件 | 原服务提供者与平台实现必须保持业务；注解扫描/发现不等于实际客户端插件注册或JEI runtime就绪 |
| Access Transformer | 原cfg的SRG规则/目标可见性及宿主读取应用尚未实测；不能用无初始化JVM链接替代AT应用 |

粗名称候选中40个符号的目标类型不存在或未解析，涉及18个不同owner；这是**候选扫描结果，不是40个已实际触发的异常**。两侧源码参考不是本轮依据；结论来自准确发行二进制和固定官方SDK，不将最新Git分支假定为发行源码。

## 原生Forge实际基线

固定MC1.21.1 / Forge52.1.0 / Microsoft JDK21.0.10+7-LTS，独立全新SSD目录，原版`sh run.sh nogui`，无业务JVM参数/Agent/预转换。标准单因子仅安装原JEI：真实发现、javafml加载、入口构造、配置注册、Done、mods列表、保存/stop/exit0通过，stderr空，无ERROR/FATAL/崩溃。debug日志实际记录FMLModContainer创建、modclass加载、jei-server.toml跟踪与世界配置加载。

独立Forge-only观察器附加组也正常启动/退出，实际读取JEI容器中的对象类与module=jei、真实FML扫描的5个插件注解、ServiceLoader发现的Forge PlatformHelper类型，以及宿主1290条配方。没有创建假的插件/配方数据，没有直接调用JEI GUI业务绕开生命周期。观察器只发现provider类型，没有独立实例化provider或触发JEI客户端插件注册。

完整Forge SDK中对923类执行`Class.forName(...,false,...)`及声明成员类型解析，`-Xverify:all`：923链接、0未解析。Neo SDK中同一原Forge JAR为904链接、19未解析，各异常逐类保存。这是classpath SDK隔离实验，非GAME Layer/FML构造、AT应用或客户端启动。

## NeoForge真实首失败

单因子Neo21.1.256 + 本轮未改生产基线JAR + 同一原始Forge JEI，实际原版启动，2.5秒退出1：

```text
ENTRY_PROFILE modId=jei entry=mezz.jei.forge.JustEnoughItems selected=CONTEXT
java.lang.IllegalStateException: Global bus rejected: LISTENER_CONTRACT
  ControlledForgeGlobalBus.validate(ControlledForgeGlobalBus.java:51)
  ForgeRegistryTransformationPlan.prepare(ForgeRegistryTransformationPlan.java:124)
  DualForgeEntryTransformer.targets(DualForgeEntryTransformer.java:456)
```

真实发现、metadata、受控平台投影、语言provider选择和模块规范化已到达预计划。CONTEXT只是候选路由；全局Bus读取又触发组合Registry/Global验证。已有严格Global规则只允许空注册对象或一个默认ServerStarted marker，JEI的额外合成方法先被LISTENER_CONTRACT拒绝。

**没有走到JEI类转换、类定义、构造或后续生命周期。** 完整异常链在neo-baseline/stderr.log，未将退出码本身当根因，也未把未观察的网络/GUI异常编成后续动态失败链。客户端首失败没有实机运行；客户端dist模型只证明同一预计划拒绝。

## 生产决策、契约与回归

生产Java/资源零修改：没有跳过合成方法、放宽Global/Context验证器、开放整个Forge命名空间、增加JEI身份/SHA白名单或复制Neo平台实现。只改掉首个guard将把尚未证明的双Bus、配置、网络与Dist生命周期整体放行；原入口构造不能靠跳过其余业务来证明兼容。本轮收敛到**完整取证与严格失败边界**，保留后续独立契约任务。

新增28项断言通过：实际Reader/FML扫描，CLIENT与DEDICATED_SERVER版本/dist模型，唯一来源与依赖保留，CONTEXT候选、009C不接管、精确LISTENER_CONTRACT以及四个计划/范围表均空、原始输入不变。该模型没有GAME Layer，不声称Neo JEI构造PASS。未知ABI/owner/module/bootstrap/SHA/冲突/未认领字节/半转换拒绝由本轮相关既有严格契约重跑核对；没有新增转换器，因此没有虚构新的JEI Neo正例。

本轮同一9ca基线JAR的18组全新服务端实验全部PASS（Forge6、Neo10、混装2），均原版启动/保存/exit0、输入保持，无ERROR/FATAL；原Probe使用固定原字节，未重建。Clumps原生/Neo/混装的3、7、11面值普通经验球400实际world tick后control3球、treatment1球、总XP21；新世界复制/重载保留UUID/Value/Count/clumpedMap。FnF对照67个ID/Holder、66项Tab与58个Food，两端及混装一致；5个cohort各72个完整熔炉快照一致。真实玩家/Windows没有重跑。

本轮既有严格契约通过：metadata6组、平台91、Helper110/35负例、Global189/31负例、Context80/22负例、Normal13负例+JPMS、FnF完整计划、Clumps99/28负例。不同身份原生Forge夹具与Neo正例属于这些各自已支持契约，不冒称新的JEI Neo正例。

Mouse Tweaks本轮ABI回归PASS：101项断言/52次拒绝检查、Reader/owner client15/server7、真实Forge/Neo总线API隔离53/65、728成员/6Mixin目标/21类不初始化验证、8项显式初始化模型。原始业务GUI与新JAR的Windows仍NOT_RUN。

106项Python测试、25份zsh语法、374项冻结保护及独立字节码10断言/FnF期望契约通过。通用回归汇总2008项检查包括逐文件身份/副本，不是2008种游戏行为；已提交的独立JEI汇总器（789df6a4be560af45b038f5aa360e24ee110b282）另通过37项证据核对，验证实际首失败并保持整体PARTIAL。所有18组游戏实际HEAD为c6d31d9；首次JEI原生/Neo基线为a613cd2，工具运行前后的源码指纹均索引，不继承旧结果。

## 制品与证据保全

研究基线由当前维护构建器`tests/real_mods/clumps/build_compat.py`在SSD新目录生成，26个生产Java、31个生产文件逐项匹配a613cd2的Git blob；JAR每个内部文件也与既有009C制品逐字节一致，只是ZIP容器SHA不同。`production-binding.json`记录源与内容绑定，不以内部一致冒充用户实际安装SHA复核。

原始JAR、完整SDK、世界和原日志仅SSD保存；Git仅源码、测试、报告、索引。首次静态汇总把不同artifact的module-info列入冲突，已按实际模块归属修正，在新目录从提交c6d31d9重跑，旧输出保留。一次显式py_compile尽管-B仍在新测试目录产生两份缓存，按实际Python3.14标签迁至SSD并核对SHA；第一次使用旧标签的迁移未移动文件，均如实记录。生产与用户文件未受影响，后续语法检查改为内存compile。

完整附件LOCAL_ONLY，未公开上传、未创建Release。索引记录实际命令、sourceCommit、SHA、stdout/stderr/退出与SDK来源；最终PR/远端HEAD/CI以推送后单独保存的实际回读为准。

## 剩余门禁与明确的后续突破段

| 项目 | 结论 |
|---|---|
| JEI Neo严格预计划 | FAIL；已知技术阻塞 |
| 新的JEI生产兼容能力 | NOT_IMPLEMENTED；原严格边界保持 |
| JEI Neo转换/定义/构造/生命周期 | NOT_RUN，前置失败 |
| 实际JEI客户端插件注册/runtime | NOT_RUN |
| 实际AT应用、客户端启动、物品列表、R/U、搜索/书签/配置重启 | NOT_RUN |
| JEI + Mouse Tweaks + FnF + Clumps客户端混装 | NOT_RUN |
| 本轮重建JAR的Windows回归 | NOT_RUN，不能继承009C用户PASS |
| DualForge-010A-client-kit.zip | BLOCKED，未生成 |

需要后续自动化兼容开发，不要求用户执行Ubuntu操作或验收已知失败的包：

1. 双Bus显式事件类/Consumer与配置注册：独立Forge输入、真实owner/module及IModBusEvent路由、异常/重复/错误事件/反注册、配置保存重载与原子计划；不能复用009C四事件模板代替。
2. 网络payload与Dist安全初始化：精确来源channel闭包和官方注册阶段、双向协议/optional/handler线程/权限、真实连接正负对照；保留所有原业务并严格拒绝未知流。
3. 客户端事件/Fluid/渲染/AT及插件运行时：逐项成员/资源/模块归属、官方生命周期和独立夹具验证后才准入；全链通过后才生成Windows包。

这些是静态取证确定的待验证契约段，不是已经实现或实际触发的后续错误。010A不改变固定宿主版本，不擅自拆出新PR或合并任何PR。

详见[证据索引](<Convergence-010A JEI 证据索引.json>)、[测试入口](../tests/real_mods/jei/README.md)、[矩阵](../tests/real_mods/jei/MATRIX.md)与[暂缓Windows清单](../tests/real_mods/jei/CLIENT.md)。
