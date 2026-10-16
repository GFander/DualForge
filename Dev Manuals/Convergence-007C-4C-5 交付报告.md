# Convergence-007C-4C-5 FnF真实服务端行为交付

**4C5_SERVER_BEHAVIOR=PASS（下述固定发行与受控服务端范围）。** 原始FnF与已验收4C-3 DualForge均未重建或修改；生产 `dualforge/src/main/` 零修改。两种代表燃料、官方Tab回调及58个Food属性与本轮原生Forge对照一致，没有发现真实行为差异，不实施生产修复。

| 独立门禁 | 结论 |
|---|---|
| FNF_SERVER_BOOT | PASS |
| FNF_ITEM_REGISTRY | PASS：67 Item/Holder及Tab |
| FNF_FUEL_FURNACE | PASS：paper_stack、bio_fuel及列明负例/煤校准 |
| FNF_TAB_CALLBACK_SERVER | PASS：LOGO icon与66项display/search，非GUI |
| FNF_FOOD_PROPERTIES | PASS：58项实际FOOD完整记录逐项一致 |
| FNF_FOOD_PLAYER_CONSUMPTION | NOT RUN |
| FNF_CLIENT_UI | NOT RUN |
| FNF_PERSISTENCE | NOT RUN |
| FNF_FULL_COMPATIBILITY | NOT ESTABLISHED |

## 基线、环境和不可变输入

fetch/prune后确认PR #29合并于 `dd3d046820589b9c1e4eb46f1a3249efd816fd93`，即本轮main起点。main [Actions 38850589545](https://github.com/GFander/DualForge/actions/runs/38850589545) SUCCESS。C4证据提交 `36f23ce10e4a9b74dd2522fa096dd095a3128f15` 的ZIP独立下载，原SHA及193文件Manifest验证PASS。未从未收敛分支叠加，不改写历史。

分支 `codex/convergence-007c4c5-fnf-behavior`，独立worktree `/workspace/DualForge-007C4C5`。生产基线 `9fc9cde97888a2d7894252dc59702d8548d5a5e5`，源码/资源指纹 `02e8d50e1dbf81bc79478f0041201905d7a6d7d071c6ad7c27667cee3d2ae25f`，与该提交及main均一致。

| 固定输入 | bytes | SHA-256 |
|---|---:|---|
| 原始fnf-forge-1.3-1.21.x.jar |128098|`bd415b04529c4ffc31e14757488f71dab6917e4560c61e236c3c4d0de6557958`|
| 4C-3 DualForge.jar |152831|`a26893598d1ffdb73078373b007a4d5d54dddd1b774cb2a0fa0dd90fe5602bf7`|
| 原生Neo共存输入 |2265|`270c30aca9ddd42b625efe6e1eeefafa7865b5b4c4d5ae709a0623109cf5251c`|

MC1.21.1，原生官方Forge52.1.0，目标NeoForge21.1.256/FML4.0.45；Linux x86_64、Microsoft OpenJDK21.0.10+7-LTS、javac21.0.10、Python3.12.14。实际版本输出、官方安装全部库SHA、原参数、两宿主14个实际API javap、5个原发行ClassFile/javap、观察器源码/class/JAR及依赖SHA均在索引。两个观察器分别使用各宿主官方API编译，没有FnF/DualForge编译依赖；它们是测量输入。

## 矩阵和真实运行

先复用007C冻结报告设计 [矩阵](../tests/real_mods/food_n_fuel/behavior/MATRIX.md)，再本轮先原生Forge、后Neo功能对照。所有JVM/编译串行，官方安装物理复制，八个新世界，初始seed=442526。C/T共享的实际输入文件大小/SHA、观察器、初始properties和真实JVM命令逐项绑定，唯一安装差异为同一个原FnF JAR。无Agent、测试SPI、业务开关或额外module/JVM参数；标准和附加观察器运行分组。

| 阶段 | 被测工具源码提交 | 生命周期/退出 | 秒 |
|---|---|---|---:|
| forge-empty | 822dc9c | PASS / 0 | 39.03 |
| forge-fuel | b4020d9 | PASS / 0 | 50.4 |
| forge-observed | 7a18cd6 | PASS / 0 | 49.84 |
| neo-empty | 7a18cd6 | PASS / 0 | 30.2 |
| neo-control | 7a18cd6 | PASS / 0 | 44.48 |
| neo-treatment | 7a18cd6 | PASS / 0 | 43.01 |
| neo-control-observed | 7a18cd6 | PASS / 0 | 39.86 |
| neo-treatment-observed | 7a18cd6 | PASS / 0 | 44.28 |

最早空Forge在822dc9c，之后run_behavior仅补充未被空宿主调用的chunk等待/lit查询；实际燃料及观察运行使用同一最终driver。生产输入没有改变。源/编译指纹与Git核对，文档后续提交不冒充新游戏回归。

## 真实燃料与负例

官方命令放置furnace/smoker，只用 `item replace` 填入真实输入/燃料。等待forced chunk实际加载，`tick freeze`、`tick rate 1000`、`tick step`，以世界game-time校验精确偏移0/1/21/100/101/200/201/3200/3201，再读取完整官方NBT及实际LIT状态。单位是实际世界tick，不是默认20TPS的墙钟时长。**没有写BurnTime/CookTime/CookTimeTotal或产物槽**，配方总时长由官方输入处理取得。

| 样本 | 原生Forge实际结果 | NeoForge实际结果 |
|---|---|---|
| paper_stack +2圆石 | t1燃料消耗，Burn=100/Cook=1/LIT；t21 Burn=80；t100 Burn=1/Cook=100；t101 Burn=0/Cook=98/熄火；t201进度归零；全程无产物 | 全部状态完全相同 |
| bio_fuel +20圆石 | t1消耗燃料/Burn=3200；t200产石头1；t201 Burn=3000/余19圆石/石头1；t3200 Burn=1/余4圆石/石头16；t3201 Burn=0/熄火，保持16产物 | 全部状态完全相同 |
| fnf:logo非燃料 | 保留燃料与圆石，不点火/不产物 | 同 |
| diamond错误输入 / 空输入 / 空燃料 | 不点火、不消耗相应燃料、不产物 | 同 |
| smoker中圆石 | 没有对应smoking配方，不消耗bio_fuel | 同；不推导为FnF不能作合法smoker燃料 |
| campfire的fuel槽命令 | 精确拒绝“Target position 9, 100, 0 is not a container” | 安装FnF时精确相同 |
| 原版coal校准 | 初始1600 tick，最终8石头、余12圆石 | 同 |

每个有矩阵的运行是8设备×9时点=72条完整状态，包含槽位、燃烧/进度、实际配方计数和点火。原生有/无观察器、Neo标准/观察Treatment逐条相同；两次各自真实实验排除观察器对代表燃料结果的影响。Control的FnF物品不存在，官方解析先精确拒绝 `Unknown item 'fnf:bio_fuel'`，它不是campfire行为不一致；Control煤校准与原生相同。

这是两种代表燃料的完整燃烧与负例验证，不是八种燃料/所有有效RecipeType的全覆盖。

## 官方延迟Tab回调

真实Tab `fnf:fnf_item_tab`，Holder.get与Registry对象相同。ServerStarted阶段读取Minecraft私有缓存（只读）：icon缓存null、display空；随后调用官方 `getIconItem()`，返回真实 `fnf:logo`，第二次返回同一个缓存Stack。使用真实server.registryAccess、默认FeatureFlags及官方ItemDisplayParameters执行 `buildContents`，display/search均66个唯一真实Item ID，对象身份、顺序及集合与原生一致，无重复/缺失/错误对象或JVM链接异常。

未直接调用/替换FnF私有lambda、未写缓存或Registry、未人工构造预期展示列表。初始缓存/结果与本轮官方方法字节码共同限定懒执行证据，未发现提前填充；没有给未插桩的整个JVM声称精确私有lambda调用次数。Holder在注册完成后的允许阶段解析。服务端官方查询可执行，不代表GUI显示已验收。

## Food属性与真实食用边界

实际67个Item中58个FOOD component，逐ID完整记录与原生完全一致：运行时类型、stacksTo、nutrition、saturation、canAlwaysEat、eatSeconds、usingConvertsTo、每个effect的ID/duration/amplifier/probability/ambient/visible。nutrition实际有2/3/4/5/6/8/12多种；56项含effect，两项为空，不能假设58个食品相同。

42项最大堆叠64、16项堆叠16。36项转换属性分别为20项minecraft:stick、16项minecraft:bowl；这是Food组件转换声明的实际查询，**没有模拟或声称食用后真实返还**。

carrot_bread本轮重新测得nutrition=6、saturation=7.2000003、maxStack=64、night_vision=1200 tick、amplifier=0、probability=1、ambient=false/visible=true。没有真实连接玩家，不修改FoodData、不扣物品或手动加效果，FNF_FOOD_PLAYER_CONSUMPTION=NOT RUN。

## 回归、异常与保全

本轮110项最终核对、六个SNBT自动负例/正例通过。C4实际服务端门禁重新执行：三目标ModLauncher转换、原构造Context、两个同容器Mod Bus helper、67 Item/Holder、Tab注册、受控Global空监听器路径及观察器实际源对象0个、原生共存、Done/stop/exit0。标准与观察C/T均绑定同一固定JAR，未混旧cohort。

所有运行严重日志header/次数精确等于同轮对应空宿主：保留Mojang公钥获取网络ERROR，**不是零ERROR**。无新增严重记录、stderr、crash/dump FAIL。除此之外的Probe/002至4C3完整历史游戏矩阵本轮NOT RUN；生产未变，不把历史PASS当新动态结果。最终CI静态/单测独立核验，六个新parser测试另行手动实际运行，没有改CI范围。

未发现真实FnF行为差异，无需实施修复或为假设差异新增Shim。保留工具问题：最初native比较混用JSON字符串slot key/解析器整数key，规范化后完全相同；首版汇总误要求无FnF Control也到达campfire容器检查，原FAIL脚本/JSON/stdout/stderr保留。最终版根据安装身份精确要求未知物品或非容器拒绝，不接受任意异常；原服务器命令/结果未改写，无生产修复。

442项既有生产/历史源码/报告、两套官方安装及固定Jar均未变；C4的192份原文件及归档SHA不变；374项冻结保护PASS。所有世界是新目录，原世界未打开/复制。runner仅清理本轮核对SHA后的独立运行库副本，保留世界/输入/命令/日志。JVM全部结束，25565 bind/listen/close释放验证PASS。

## Git、证据与停止

新增原生观察器、官方命令driver、严格SNBT读取/测试、矩阵和文档；共享stock runner仅加两个可选测试hook（console_driver/初始properties），普通CLI默认路径保留。docs/INSTALL按已合并C4事实更新，历史冻结报告不改。生产源码/资源、原FnF和DualForge字节始终零修改。[PR #30](https://github.com/GFander/DualForge/pull/30)，main基线；最终HEAD/Actions由PR与最终回复实际回读，不用旧CI代替新HEAD。

原始证据 [本地ZIP](../out/convergence-007c4c5/evidence-export/DualForge-007C4C5-Evidence.zip)，**835787 bytes、236个payload文件、5620563 bytes**。ZIP SHA `1995361e09b8e6e6bb1836ee552ae29c9cd55c18009a5fbd2a7e540fb87f2a95`；Manifest SHA `61da2d60fc9f6402997a40b2c6a3f66a1def188b0097a0d47cf475d7a6391bbb`。CRC/路径安全/无软链接/无重复/Manifest/SHA256SUMS/解压逐文件SHA与235个原文件不变全部PASS。

收录实际命令、退出、stdout/stderr/debug、原生/Neo行为、熔炉tick/槽位/LIT、Tab/Food结果、输入/观察器指纹、失败汇总和必要原ClassFile/项目制品。排除世界、第三方运行库、完整原FnF JAR及配置（仅保留明确测试seed初始化）。这是选定最小附件，不是完整工作区备份。**ARCHIVE_READY / NOT PERSISTED**：本次ZIP没有公开上传授权、没有远端发布或独立远端下载，不继承C4授权；亦未声称公开安全审查或脱敏已完成。

重跑见 [behavior](../tests/real_mods/food_n_fuel/behavior/README.md)，完整逐文件路径/SHA及精确状态见 [机器索引](<Convergence-007C-4C-5 证据索引.json>)。本地原始路径保留，不冒充远端下载URL。

后续4C-6若另行授权，需要同一原FnF/4C3 DualForge的上述SHA、官方MC1.21.1/NeoForge21.1.256/Java21客户端、干净实例和可登录玩家，并核对客户端自身制品。待验收：真实饥饿/饱和度与食用消耗/效果/转换返还，创造栏icon及66展示物的GUI/模型/名称，背包/容器物品保存重进。服务端API和旧受控Mac PASS不能代替这些项。本轮不制作/启动客户端任务，不自动合并、Release或适配其他模组。
