# Convergence-008A Clumps 受控兼容扩展交付

**整体 PARTIAL；限定服务端范围 PASS。** 未修改的 Clumps Forge 19.0.0.1 在 NeoForge 上完成启动、真实经验球合并及经验球重载，与本轮原生 Forge 对照一致。事件取消返回、ServiceLoader 和可变事件对象传播另有原生 API 实测。真实玩家拾取、Mending、客户端启动及人工验收仍 NOT_RUN。

## 一、任务基线与固定 SHA

2026-10-17，仓库 `/home/albert/Codex/DualForge`，源码继续保存在 SMB；全部新运行、编译产物、世界和附件保存在 SSD `/home/albert/Env/DualForge/runs/convergence-008a-20261017`。未恢复旧云端工作区。

两次 fetch 均确认 `origin/main=0de1facf6354b52410ca114ae4e50eacfb305420`，起点工作区干净。该提交已合并 PR #31；[Actions 38918751522](https://github.com/GFander/DualForge/actions/runs/38918751522) 实际回读 COMPLETED / SUCCESS。该报告的 FnF Windows 人工抽检为历史验收，不能覆盖新制品门禁。

分支 `codex/convergence-008a-clumps`。实现提交 `edf424b4dca9ed321ee4f46aec3474707400203f`；后续验证汇总工具修正为 `7ec977d5c3f50b3a14c0f0cb43bb6fbca701b609`，生产零修改。最终构建和 Neo 游戏实验发生在实现提交前，日志中的 Git HEAD 是当时的基线；实际被测工作区含生产修改。最终六组原生 Forge 对照在该实现提交后执行。构建输入逐文件 SHA 和提交后 Git Blob 对照绑定实现，不将基线 SHA 冒充被测代码提交。

| 输入/输出 | bytes | SHA-256 |
|---|---:|---|
| 原始 Clumps Forge 19.0.0.1 |18226|`e1de425ddd6f2b5c195c37a008d9fbc2c00c32fe1e214d76c63ab35446d13c19`|
| 本轮基线 Ubuntu DualForge |152831|`8a465b93edfe34ff562e0b27e46ffb6792d916619f38492830edc472622210b9`|
| **最终 dualforge.jar** |168348|`43bc109e6129c2a6e7c7b6c76f339710a660195e7a7970b25ef6bb3489a16426`|
| 原始 FnF |128098|`bd415b04529c4ffc31e14757488f71dab6917e4560c61e236c3c4d0de6557958`|

Clumps 来源为用户放置的本机输入；未声称独立核验发行站下载来源。固定 Microsoft JDK `/home/albert/Env/DualForge/jdks/msjdk-21.0.10`，实际 21.0.10+7-LTS；Forge52.1.0、NeoForge21.1.256/FML4.0.45/ModLauncher11.0.5，MC1.21.1。安装目录不变。所有实际使用 JAR 的来源、大小、摘要及每组安装清单见机器索引及其原始附件。

## 二、两个用户客户端崩溃现场

以下来自用户提供的摘要，不是本轮独立读取的原始 ZIP：

| Windows 现场 | 安装 | 用户提供的 ZIP SHA-256 |
|---|---|---|
| 23:19:55 | Clumps + FnF + DualForge |`8b5de41a5abb833382de2b47271f73a31d742e9e1591261988898e3825cafb50`|
| 23:28:51 | Clumps + DualForge，无 FnF |`b550d47a83b3a41448caf12217fc5218548b0c66dba46f5bf731a6e605bb921f`|

两份原 ZIP 未在声明的本机输入目录发现，未生成替代文件。用户报告的发现/metadata 通过、入口 Profile 失败、客户端启动失败，均保留为历史现场。第二份已排除 FnF 是该首断点的必要条件。

## 三、原始 Clumps ABI 取证

生产修改前完成全部 **15 个 ClassFile** 的固定 JDK javap，以及双宿主事件 API 取证；最终整理出 225 条实际字节码调用/字段/动态调用引用。`ENTRY_BYTECODE_ANALYZED`、`CALL_GRAPH_RECORDED`、`FORGE_API_DEPENDENCIES_RECORDED`、`MIXIN_REQUIREMENTS_RECORDED`、`FIRST_FAILURE_CONFIRMED` 均有实际证据。

入口唯一 public 无参构造器，无字段/静态初始化器：Object 构造后，由零捕获 LambdaMetafactory 生成 `BiPredicate<Player,ExperienceOrb>`，写入同 JAR 的 `ClumpsCommon.pickupXPEvent`。实现方法精确发布新建的 Forge `PickupXp`，返回 `IEventBus.post(Event):boolean`。公共类的静态初始化另包含 logger 和默认 false predicate，不能跳过初始化以制造成功。

`Services` 实际使用 ServiceLoader 加载 `IEventHelper` 和 `IPlatformHelper`。ForgeEventHandler 为两种自定义事件各创建一个对象、真实 post、丢弃 boolean 后返回同一 `Either.left(event)`。ValueEvent/RepairEvent 是非取消的 Forge Event 子类，保存 Player 和可变 int。Mixin 会消费这些修改后的值，因此无条件返回常量/不发布事件均不成立。

原发行的 Mixin 目标、Shadow/Accessor、注入 selector 和业务成员引用已是 Mojmap；本轮不改 Mixin、目标方法或资源。配置声明的 `clumps.refmap.json` 实际缺失，两端原始运行保留相应 WARN。JAVA_18 配置与 class65 的诊断也保留，不能将其误判成实际 Mixin 应用失败。

[官方参考源码](https://github.com/jaredlll08/Clumps/tree/9dc57a1d3bd3cab65a34394718bc5813c46c9915) 来自 1.21.1 分支，只作参考；该分支提交不被冒充发行 JAR 的源码提交。发行 ClassFile 始终是本轮准绳，没有用参考源码重建 Clumps。

## 四、原生 Forge 行为

先运行 Forge 空宿主，再运行原始 Clumps。官方命令生成三个普通经验球，初始面值3/7/11，总经验21；以真实世界 game-time 验证精确推进400 tick。没有设置 Count、clumpedMap、age、输出经验或现有经验球 NBT。

| 本轮真实实验 | 初始 | 400 tick 后 | 结果 |
|---|---|---|---|
| Forge 空宿主 |3个 / XP21|3个 / XP21|PASS 对照|
| Forge + 原始 Clumps |3个 / XP21|1个 / XP21，map为3/7/11各1|PASS|

原生标准与观察组结果一致，正常保存/停止/退出0。合并后实际 Count=2、Value=21；**总经验按真实 clumpedMap 求和为21，不将 Value×Count 误算成42**。

原生附加 API 观察组使用显式合成 null player/orb 参数：实际入口回调在无取消时 false、监听器取消时 true，监听器调用一次并可移除；实际 ServiceLoader 提供者返回的 Value/Repair 对象被监听器由7改为19，消费者取得同一对象和修改值。15项断言 PASS。这不是玩家拾取或 Mending 测试。

## 五、NeoForge 首失败

`neo-before` 仅安装基线 DualForge 和原始 Clumps，实际首失败与用户 Windows 摘要相同：

```text
java.lang.IllegalStateException: UNSUPPORTED_ENTRY com/blamejared/clumps/Clumps: no controlled Forge entry profile
ForgeEntryProfile.select(ForgeEntryProfile.java:22)
DualForgeEntryTransformer.targets(DualForgeEntryTransformer.java:381)
```

退出1，未到 Done。stdout/stderr/debug、实际 JVM 命令及安装 SHA 均保留。不是 TOML 首失败，也没有混装 FnF。

## 六、兼容方案与修改范围

仅修改三个生产文件：新增 `ForgeEventInitializerPlan`，在 `ForgeEntryProfile` 加入独立候选，在 `DualForgeEntryTransformer` 集成完整计划发布与克隆转换。旧 Item、Block、Context、Global、事件验证器及模块/metadata/依赖投影不改。

候选识别不等于准入。完整验证唯一 Forge 入口/metadata owner、ModFile、automatic Module、构造器、Lambda bootstrap、跨类字段、限定公共类初始化、ServiceLoader 提供者及两个可变整型事件。拒绝显式/多版本模块、未知 Forge 引用、额外副作用、歧义 Profile、未验证自定义事件子类/继承 API/句柄/动态成员访问。生产规则没有 Clumps modId、包名、JAR 名或第三方 SHA 白名单；另一身份的原生 Forge 夹具实际跨宿主通过。

四个转换目标整体验证并生成完整输出后才发布：入口、事件提供者、两种事件。其他原始类保持原样。ModLauncher 初始 ClassFile SHA 与规范化输入分别核对，失败不向调用方返回半改 ClassNode；不声称回滚已经定义的 JVM 类。

Forge6.2.27 的 `post(Event):boolean` 与 Neo8.0.5 的 `post(Event):Event` 不同。限定 PickupXp 映射为真实宿主事件，调用真实宿主全局总线，随后读取真实 `ICancellableEvent.isCanceled()`；不固定返回 false。新建事件默认未取消，也不导出 shutdown/start 等 Forge 总线操作。两个自定义事件只转换 Event 父类和父构造调用，发布者保留对象及 `Either.left` 数据流，宿主 post 返回对象按原控制流丢弃。宿主总线及事件继承链绑定已验证的官方 ABI 指纹。没有公开通用 Forge post API、新总线或新 Module Layer。

## 七、实际动态验证

早期候选与最终制品分开归档。最终目录 `final/` 的 **十组 Neo 实验全部重新执行**并安装同一最终 SHA；**六组原生 Forge 对照也以已提交的最终运行器重新执行**，均不加载 DualForge。交付核对发现早期运行器文件 SHA 与最终版本不同，因此保留旧组并以新目录补齐，不将早期工具版本冒充最终已提交版本。

| 最终 Neo 组 | 结论 |
|---|---|
| neo-clumps-control / neo-treatment-1 |PASS；实际启动文件/初始properties/JVM命令一致，仅增加原始Clumps；经验球3→3与3→1，总经验均21|
| neo-observed |PASS；15项事件/服务/修改可见性断言，真实经验球结果同标准组|
| neo-independent |PASS；原生Forge验证过的同一独立身份JAR，15项观察断言|
| neo-reload |PASS；本轮世界保存后物理复制到新实例，UUID/Value/Count/clumpedMap/XP恢复一致，源世界不变|
| neo-control |PASS；新兼容制品与原生Neo模组共存|
| fnf-neo-standard / fnf-neo-observed |PASS；实际FnF加载及下述行为对照|
| original-probe |PASS；原字节Probe构造、真实register返回及原生Neo共存|
| registry-global-observed |PASS；原组合夹具实际Registry生命周期、早期拒绝、Tab延迟查询和handler一次|

Clumps 两个原 Mixin 的实际应用日志、合并后的 clumpedMap 及保存/重载共同支持应用门禁，不以“无异常”代替经验球行为。最终所有运行正常 stop/exit0，stderr空、无新崩溃或ERROR/FATAL；WARN与DEBUG原样保留。

最终汇总 **3759项检查 PASS**，包括运行库源/副本逐项一致、源安装JAR保全、实际最终兼容SHA、C/T、重载、行为和29个生产源码/资源与最终构建一致。该数量包含逐文件检查，不是3759种独立功能。索引记录214个来源JAR及1007份原始附件的摘要；原件继续外置。

## 八、严格负例与历史回归

最终同JAR：新入口99项断言、28个列明拒绝负例；附加真实 transform 分支的dump失败/禁用/未认领类检查为明确的诊断注入。旧 Normal Item 13负例及显式JPMS拒绝、Context 80断言/22负例、Helper 110断言/35负例、Global 189断言/31负例、平台91断言、metadata6组及原FnF完整准入均本轮重跑。

原 Probe SHA 为 `a774d2f737b17f089046014ab55a53767dbad008cd44dcce52c727b87e559ae8`，没有重建替代。原字节Probe/Helper/Global等输入来自独立下载并核对固定SHA的已公开4C-3证据ZIP；只取回归输入，不恢复旧工作区或继承旧结果。Normal/Legacy/Block/Context活跃夹具源码新编译cohort独立记录，旧输入和源码均未覆盖。

新 JAR 的 FnF 另行对照本轮原生Forge：67个ID/Holder、66项Tab icon/display/search和全部58项Food记录一致；标准/观察Neo与原生的8设备×9时点、72个完整熔炉快照逐项一致。覆盖paper_stack、bio_fuel、煤校准及列明负例。没有继承4C-5制品PASS，没有声称全部燃料/配方或玩家食用已重新验收。

106项Python测试、固定ASM字节码验证器契约、zsh语法及374项冻结保护PASS。完整历史002至007C游戏矩阵、新JAR旧客户端验收均 NOT_RUN；上述回归PASS限定于实际列明的相关范围。GitHub CI仍是静态/单测，不代替游戏证据。

保留失败：最初新契约对invokeinterface的匹配及post参数类型集合有遗漏，前两份失败目录原样保留；随后修正并通过。首次旧组合夹具少装其required globalobserver，被官方FML拒绝（进程退出0仍被runner判FAIL）；补齐原测量依赖后新目录通过。早期人工汇总曾把lit布尔值当字符串而TypeError，改用原始布尔值后比较通过，未重跑/改写游戏来掩盖汇总问题。最终硬化后已重新构建并重跑受影响链路，早期候选不承接为最终PASS。

交付脚本首次核对原生运行器指纹时按预期拒绝不一致；补跑入口曾假定早期目录存在 invocation.json，在启动游戏前退出，随后改用原始 source-inputs 清单。最终原生组使用同一已提交运行器。汇总器曾假定 final 仅有Neo组而要求原生组安装DualForge，现已分别严格检查原生无兼容层/Neo安装最终SHA。上述工具失败不冒充游戏失败，旧记录均保留。

## 九、最终状态矩阵

| 门禁 | 状态 | 范围 |
|---|---|---|
| CLUMPS_NATIVE_FORGE_BOOT |PASS|本轮原始发行原生Forge|
| CLUMPS_NEOFORGE_DISCOVERY |PASS|最终制品实际发现|
| CLUMPS_ENTRY_PROFILE |PASS|有限初始化闭包|
| CLUMPS_MOD_CONSTRUCTION |PASS|真实FML实例化与初始化|
| CLUMPS_EVENT_BRIDGE |PASS|限定取消/可变事件API；合成输入观察|
| CLUMPS_SERVICELOADER |PASS|实际原提供者初始化及调用|
| CLUMPS_MIXIN_APPLICATION |PASS|原Mixin应用及真实合并/保存读取|
| CLUMPS_NEOFORGE_SERVER_BOOT |PASS|标准无额外业务参数|
| CLUMPS_XP_ORB_MERGE |PASS|面值3/7/11，400tick，总XP21|
| CLUMPS_PLAYER_XP_PICKUP |NOT_RUN|无真实连接玩家|
| CLUMPS_MENDING |NOT_RUN|不以合成RepairEvent测试替代|
| CLUMPS_PERSISTENCE |PASS|仅本轮真实经验球；玩家数据NOT_RUN|
| CLUMPS_CLIENT_BOOT |NOT_RUN|等待新JAR实机反馈|
| CLUMPS_CLIENT_MANUAL |NOT_RUN|等待用户人工验收|
| DUALFORGE_REGRESSION |PASS|实际列明的相关契约/动态范围|
| FNF_REGRESSION |PASS|实际列明的注册/服务端行为；新客户端NOT_RUN|

## 十、未解决问题

未发现限定服务端范围内尚未解决的技术失败。真实玩家XP拾取、Mending、玩家存档与Windows新JAR客户端仍需外部实机证据；本轮不模拟玩家、不虚构GUI结果。两个原Windows崩溃ZIP未取得，已由Ubuntu同断点实际复现支持本轮定位，不能声称原ZIP已独立校验。

原始附件保存在SSD，未公开上传；不继承任何历史证据上传授权。Git只提交源码、测试、报告与指纹索引，排除第三方JAR、世界、依赖、完整日志、账户资料。分支推送后创建Draft PR，禁止自动合并或Release；最终HEAD/PR/CI在实际GitHub结果中核验，不写入原游戏日志冒充运行时事实。

## 十一、下一阶段建议

使用 [客户端说明及清单](../tests/real_mods/clumps/CLIENT.md)，先建立同一原始Clumps的Forge客户端基线，再用NeoForge+新DualForge+原始Clumps单因子测试；提交实际安装SHA、日志及XP/损坏值前后记录。确认Clumps独立功能后，另行验证与FnF混装，不以此次各自通过承诺任意多模组业务。

本机交付包 `/home/albert/Env/DualForge/runs/convergence-008a-20261017/DualForge-008A-client-kit.zip`，SHA-256 `144ac4027aea65145356a27b8eac2429f108ba7a548ee381e607e520912a783b`；仅含新 dualforge.jar、SHA256SUMS、安装及人工清单。Clumps仍使用用户原文件，未重复分发。

完整路径、实际命令、输入/制品SHA、最终来源绑定及状态见 [机器证据索引](<Convergence-008A Clumps 证据索引.json>)；重跑入口及边界见 [测试README](../tests/real_mods/clumps/README.md) 和 [矩阵](../tests/real_mods/clumps/MATRIX.md)。
