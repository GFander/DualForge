# Convergence-009C Mouse Tweaks 交付报告

**整体 PARTIAL；列明的自动化范围 PASS。** 原始 Forge 2.26 的有限入口、四事件、配置工厂、初始化模型和客户端 ABI 链接通过；同一新 JAR 的 FnF/Clumps 服务端与混装回归通过。真实客户端构造、Mixin 应用、Windows/PCL 启动、鼠标操作及配置 UI 仍 NOT_RUN。

## 一、任务基线

2026-10-17（Asia/Hong_Kong），实际 fetch 确认 `origin/main=2600fe85b0c85b3d79454db6a20a93d1a9219e7c`，包含已合并 PR #32。原仓库 `/home/albert/Codex/DualForge` 有用户未跟踪的008A人工验收报告，未覆盖、删除、暂存或提交它；新建 SMB worktree `/home/albert/Codex/DualForge-009C`，分支 `codex/convergence-009c-mousetweaks`。

实现提交 `8f1e9008b1ca83d024ab6ad1b06a9690ac3647f6`；汇总验证提交 `74740b95cc8fdab8e812c16ca8deac71182fd5cd`。最终构建在实现提交前完成，31个生产源码/资源 SHA 已逐项与提交后 Git Blob 核对一致。最终新增自动化在实现提交后再次执行；18组游戏实验日志实际 HEAD 为该实现提交。后续只新增汇总测试及交付文档，不改生产字节。

本轮所有 SDK、编译产物、运行、世界和原始附件在 SSD `/home/albert/Env/DualForge/runs/convergence-009c-20261017`。固定 Microsoft JDK21.0.10+7-LTS、MC1.21.1、Forge52.1.0、NeoForge21.1.256/FML4.0.45/ModLauncher11.0.5，原安装不迁移。Git 身份沿用 Albert Lee / 已配置邮箱。

用户任务书确认的此前 FnF/Clumps 人工、009A混装、009B联机与重启持久化为 **历史 USER_REPORTED_PASS**。本轮没有重新执行那些人工步骤；旧报告当时的 NOT_RUN 保持原字节，不覆盖最新用户事实，也不被继承给新 JAR。

## 二、原始发行身份及用户首异常

| 文件 | bytes | SHA-256 |
|---|---:|---|
| MouseTweaks-forge-mc1.21-2.26.jar |73287|`750dd1806a9e67ba23be58403648293bc1e936a068ba6d8184aac88fc3e0bc62`|
| 本轮 main 基线重建 dualforge.jar |168348|`4d93a5fb94c48d89baef87fea7e4a3de3c308ab6eed72a0443dd35f03b5b1493`|
| **最终 dualforge.jar** |192754|`50b6ca25eecf2d08505b534df16a4af1fedf23816ca4ceb41cac09a509637db4`|
| 原始 FnF Forge1.3 |128098|`bd415b04529c4ffc31e14757488f71dab6917e4560c61e236c3c4d0de6557958`|
| 原始 Clumps Forge19.0.0.1 |18226|`e1de425ddd6f2b5c195c37a008d9fbc2c00c32fe1e214d76c63ab35446d13c19`|

Mouse Tweaks 来自本机用户 inputs；没有独立验证发行站下载来源，没有使用现有 NeoForge2.26.1 文件替代。实际 Forge TOML 为 javafml `[51,)`、`clientSideOnly=true`、modId `mousetweaks`、version2.26。发行没有单独 Minecraft 依赖范围；本轮以固定1.21.1完整客户端 ABI 核对，不将文件名本身当作支持证明。

用户提供的 `错误报告-2026-10-17_15.59.04.zip` SHA 为 `ca10ca4d63cd2681f53bdc302e4b09791af60055d326c3fa0f62a511c404c663`。未取得/独立检查原 ZIP，保留为用户历史摘要：发现与 metadata 通过，入口 `yalter/mousetweaks/forge/MouseTweaksForge` 的 `UNSUPPORTED_ENTRY` 为首个致命异常。不能由此推断后续事件、Mixin 或 GUI 成败。

## 三、静态 ABI 调用图

生产修改前，以原始 JAR 对全部 **21个 ClassFile** 运行固定 JDK javap，保留 **859条实际调用/字段/动态调用指令引用**、Manifest、TOML及两份Mixin资源。核心构造器唯一 public 无参：Object构造 → 同JAR `Main.initialize()` → 真实全局 `register(this)` → 零捕获 `ConfigScreen::new` Function注册。入口无字段/静态初始化器。

四个默认 public 实例订阅为 MouseButtonPressed.Pre、MouseButtonReleased.Pre、MouseDragged.Pre、MouseScrolled.Post。前三个由业务返回值决定是否 `setCanceled(true)`；滚轮 Post 的 boolean 业务结果原本 POP，不能额外取消。点击/拖动会将按钮转为同JAR枚举，无效按钮保持原拒绝；日志参数和分支均保留。

`Main.initialize` 有重复初始化保护：取得 Minecraft实例、由 gameDirectory 构造配置路径、创建原 Config、执行read、设置ready。Config(String) 实际为 **package-private**，同包调用合法；不能根据参考源码猜为public。配置 Screen 的原构造器保存传入父 Screen。

[官方参考分支](https://github.com/YaLTeR/MouseTweaks/tree/minecraft-1.21.1) 实际克隆提交 `e1a5523b93f120c8d3f427d725f19d537c38af60`，只作参考，不冒充2.26发行源码绑定。该分支 Neo示例仍使用 Minecraft 参数，而固定宿主实际使用 ModContainer；实现以真实二进制为准。

## 四、Forge / NeoForge API 差异

| 契约 | 实际对照/处理 |
|---|---|
| 四类 ScreenEvent |前三个可取消；滚轮Post不可取消，继承链和构造参数逐项核对|
| getScreen/mouseX/mouseY/button/dragX/dragY |有限调用点按真实宿主类型保留数据流|
| 滚轮getDeltaY()D |宿主getScrollDeltaY()D，两处读取均适配|
| SubscribeEvent |只接受默认NORMAL、receiveCanceled=false的四个实例方法|
| Forge registerConfigScreen(Function) |原内部适配为忽略Minecraft参数的BiFunction|
| Neo IConfigScreenFactory |实际SAM为createScreen(ModContainer,Screen):Screen；新增私有适配器仍只把第二参数传给原Function|
| 初始化时序 |实际Neo客户端Minecraft构造器先赋值instance，再调用client/loading/ClientModLoader.begin；原初始化调用位置不删除/延后|

原 Mixin 仅 AbstractContainerScreenAccessor，六个Invoker/Accessor的目标、名称、描述符均在完整宿主客户端文件中存在：findSlot、slotClicked、isQuickCrafting的读写、quickCraftingButton、skipNextRelease。原始类和选择器已为Mojmap，无额外remap规则。没有修改原Mixin、资源或业务类。**目标存在不是实际Mixin应用PASS**。

## 五、原生 Forge 验证

原服务端安装只有部分客户端补丁类，独立夹具首次编译缺少Renderable等基类。随后使用固定官方安装器，在本轮SSD目录分别完成Forge/Neo官方客户端依赖准备；没有启动游戏、读取账户或改动已有实例。

不同 modId/包/入口的 `gestureinput` 夹具以完整 **Forge-only** SDK编译。原生API隔离测试使用真实6.2.27总线，并在内存中执行官方EventSubclassTransformer，保持原SDK JAR不变。实际四事件每次派发一次，参数/按钮/分数滚轮值、拖动参数、取消返回、已取消过滤和unregister均验证。原生配置Factory实际调用原Function一次且保持延迟。**53项断言PASS**。

API测试的入口与Screen对象明确跳过构造器，不运行Minecraft/窗口/Screen.init/渲染/鼠标输入。不能称为原生Forge游戏启动或GUI通过。

## 六、NeoForge 首失败

使用本轮 main 基线新构建JAR及原始入口ClassFile，实际受控选型复现：

```text
UNSUPPORTED_ENTRY yalter/mousetweaks/forge/MouseTweaksForge: no controlled Forge entry profile
ForgeEntryProfile.select(ForgeEntryProfile.java:23)
CONTROLLED_PROFILE_FAILURE_REPRODUCED=PASS; REAL_CLIENT_BOOT=NOT_RUN
```

这证明选择器断点与用户摘要一致，不是本轮Linux客户端/FML游戏生命周期独立动态复现。没有混入FnF或Clumps来解释该断点。

## 七、兼容方案与实际生产修改

四个生产文件：新增 `ForgeClientEventPlan`、`ForgeClientEventBridge`，在选型与Transformer集成独立CLIENT_EVENTS分支。原Item/Block/Context/Global/Clumps验证器、metadata、模块归一及语言加载器契约不改。

候选只是路由；严格验证单一@Mod与metadata owner、ModFile、automatic module、唯一构造顺序、初始化字段/调用、四个完整处理器控制流、默认注解、同JAR业务链接和纯父页面配置构造器。整闭包与其他ModFile冲突、显式模块、未知事件/Bootstrap/额外入口副作用、错误参数及歧义继续拒绝。没有 Mouse Tweaks 身份、包名、文件名或发行SHA白名单。

只发布入口一个转换目标；计划全验证、生成完整输出后发布，实际输入SHA/规范化字节变化拒绝。转换克隆、dump失败和禁用分支不返回半改节点。只翻译有限事件类型/方法/注解，保留业务、日志、分支与配置Function，不提供任意Forge客户端API或命名空间替换。

构造期桥接再次绑定实际active Forge容器、entry module、既有GAME layer、完整计划指纹和对象身份。状态顺序为初始化 → 真实宿主全局注册 → 真实容器扩展注册；重复、错误owner和顺序拒绝，发生官方API异常保留UNCERTAIN，不承诺回滚已经发生的副作用。公共入口不返回原生Bus，不建立新Module Layer。

## 八、新增正负例及配置模型

最终同JAR：**101项字节码/分支断言、52次拒绝检查**；实际Reader/扫描/预计划的client15/server7项断言。覆盖构造器、注解/事件/参数、重复、owner、配置Bootstrap/父页面、初始化附加副作用/字段、跨ModFile、显式JPMS、未知入口、输入改变、未认领节点及dump/禁用路径。诊断注入不冒充完整FML构造。

同一原Forge夹具的Neo原生总线/容器扩展模型 **65项断言PASS**。实际测试生成的SAM调用点，而非手写假工厂；Screen对象身份、取消与修改数据、延迟调用、重复状态拒绝和公共构造期API的逃逸拒绝均记录。为提取调用点加入的内存诊断accessor不是游戏输入。

另有 **8项原始初始化模型断言PASS**：在独立JVM注入无构造的Minecraft对象，gameDirectory限定新SSD目录；实际原始Main.initialize生成配置、重复调用保留同一Config与文件，实际原始ConfigScreen构造器由两个原/新调用点触发，每次新建Screen并保留父对象。没有执行entry/Minecraft构造器、Screen.init、渲染或输入，因此配置GUI仍NOT_RUN。

**728个成员链接、6个Mixin目标、21类不初始化JVM验证PASS**，均使用完整固定Neo客户端文件。源码/静态链接、独立模型、实际游戏结果分开归档。

## 九、FnF / Clumps 回归

使用同一最终SHA、固定JDK、原版启动脚本，18组全新SSD世界，全部正常保存/stop/exit0，无新ERROR/FATAL/崩溃，输入与实际JVM参数核对通过：

- Forge：空宿主、原Clumps、观察组、独立发布者、真实世界重载、FnF行为，共6组。
- Neo：Clumps单因子control/treatment、观察组、独立发布者、重载、原生共存、FnF标准/观察、原Probe、Registry/Global，共10组。
- Forge与Neo各一组FnF+Clumps混装：先FnF真实熔炉，再普通经验球，共2组。

Clumps对照为初始3/7/11面值、XP21、精确400实际world tick：control仍3球，原生与Neo均1球、XP21、clumpedMap三种面值各1。合并后Count=2/Value=21，总XP按clumpedMap求和，不误算为42。两端新世界保存/复制/重载的UUID/Value/Count/map保持一致，源世界字节不变。真实玩家拾取/Mending/玩家数据没有重新执行。

FnF原生、Neo观察及两混装组的67 IDs/Holder、58 Food记录、66 Tab display/search/icon对照一致；五组原生/Neo标准/观察/混装的 **8设备×9时点=72完整熔炉快照**一致。总线identityHashCode只在各自JVM内解释，不要求不同进程整数相同；每组实际身份观测原样保留。

旧相关严格契约重跑：平台91、Helper110/35负例、Global189/31负例、Context80/22负例、Normal13负例+JPMS、metadata6组、FnF完整计划、Clumps99/28负例。原字节Probe实际构造/注册与原生Neo共存通过，未重建Probe。固定回归输入及测量JAR来自已校验SHA的旧附件，只复用输入，**不复用旧结果或旧世界**；活跃Normal/Legacy/Block/Context源码编译为新cohort。

106项Python测试、25份zsh语法、374项冻结保护、独立字节码10断言与FnF期望契约通过。最终汇总 **2008项检查PASS**，包括逐文件保全/副本核对，不是2008种独立游戏行为。完整历史游戏/客户端矩阵之外的项目均NOT_RUN。

## 十、最终构建及客户端 ZIP

26个生产Java，31个生产源码/资源，构建输入/编译命令/JDK与JAR内容逐项索引。实际维护构建入口：

```text
python3 -B tests/real_mods/clumps/build_compat.py --server-input /home/albert/Env/DualForge/neoforge-21.1.256 --jdk-home /home/albert/Env/DualForge/jdks/msjdk-21.0.10 --output /home/albert/Env/DualForge/runs/convergence-009c-20261017/build-final
```

完整javac/jar命令保存在该目录的command.json附件，不以简写冒充全部classpath。

客户端包：`/home/albert/Env/DualForge/runs/convergence-009c-20261017/DualForge-009C-client-kit.zip`，183851 bytes，SHA-256 **`d79f203d76a5d5695fb71a7fc6ae1187141eaaaf4741d9b9525be72ca94ac8ca`**。仅含dualforge.jar、SHA256SUMS、CLIENT.md；第三方原文件未重打包/分发。只需正常安装，无业务JVM参数、Agent、预转换或宿主文件替换。

## 十一、未完成客户端门禁

| 状态 | 结论 |
|---|---|
| MOUSE_RELEASE_IDENTITY / METADATA_ADMISSION / ENTRY_PROFILE |PASS，限定实际读入/预计划范围|
| MOUSE_EVENT_API_ISOLATION / CONFIG_FACTORY_API / INITIALIZATION_MODEL |PASS，明确隔离/注入条件|
| MOUSE_MEMBER_LINKS / MIXIN_TARGETS_STATIC |PASS，静态/不初始化验证|
| DUALFORGE_RELEVANT_REGRESSION / FNF_SERVER_REGRESSION / CLUMPS_SERVER_REGRESSION / FNF_CLUMPS_MIXED_SERVER |PASS，列明服务端范围|
| MOUSE_FML_CLIENT_CONSTRUCTION / MOUSE_MIXIN_APPLICATION |NOT_RUN|
| MOUSE_CLIENT_BOOT |NOT_RUN|
| MOUSE_GUI_BEHAVIOR |NOT_RUN|
| MOUSE_CONFIG_GUI |NOT_RUN|
| MOUSE_CLIENT_MANUAL |NOT_RUN|
| NEW_JAR_FNF_CLUMPS_CLIENT_REGRESSION / NEW_JAR_REAL_PLAYER_XP_MENDING_PERSISTENCE |NOT_RUN|

没有活动图形显示会话，本轮未启动真实客户端。不以服务端、模型或旧人工截图替代这些门禁。

## 十二、失败及证据保全

首次离线基线因无序classpath选中旧ASM而拒绝major65；有序classpath在新目录复现正确首失败。夹具缺完整GUI基类后准备官方完整SDK。首版初始化模板误要求Config构造器public，按真实同包访问修正并增加严格字段检查。原生API首次缺官方Event变换、随后null Screen违反requireNonNull，均保留，最终使用官方变换及真实SDK无构造Screen对象。取证器曾猜错ClientModLoader包路径，按实际`client/loading`修正。初始化模型首次未打开final字段反射访问，修正后独立新目录通过。

初次汇总错误要求不同JVM的总线identityHashCode相等；仅修正比较方法，业务数据保持全量对照，没有重跑或改写游戏来掩盖差异。早期API日志库曾在新worktree生成一份日志，已按SHA完整迁至SSD；修正新工具cwd，并为旧诊断JVM加独立、逐次记录实际cwd的SSD适配器。旧测试文件未改，后续源码区无运行日志。

原始输出、失败、SDK、制品及世界外置；只提交源码、测试、报告和指纹索引。未公开上传完整证据，不继承过去的上传授权。

## 十三、限制及后续

列明自动化范围没有未解决失败。首个尚未实测的阶段是 **真实FML客户端构造及Mixin应用**，随后是GUI/输入；客户端实测前整体保持PARTIAL。此Profile不是任意无参入口、任意ScreenEvent/订阅选项、公开Bus解包、通用配置API或Java安全沙箱。

用户唯一工作是按 [Windows/PCL清单](../tests/real_mods/mouse_tweaks/CLIENT.md) 先Forge A、再Neo B，完成拖动/滚轮/组合键/创造栏/容器/配置保存重载，最后FnF+Clumps混装。反馈PASS/FAIL与日志即可，无需Ubuntu编译/脚本/服务端/PR操作。

详见 [机器证据索引](<Convergence-009C Mouse Tweaks 证据索引.json>)、[测试入口](../tests/real_mods/mouse_tweaks/README.md)及[矩阵](../tests/real_mods/mouse_tweaks/MATRIX.md)。任务分支按要求推送并建Draft PR；最终HEAD/PR/Actions以GitHub实际回读及交付回执为准，禁止自动合并或Release。
