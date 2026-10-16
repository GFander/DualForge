# Convergence-007C-4C-4 原始 FnF 服务端运行交付

**FNF_SERVER_BOOT=PASS。** 使用原始官方 FnF 与固定 4C-3 DualForge，在真实标准 NeoForge 加载链到达 Done，正常保存、停止并退出。独立只读查询验证67个Item、Holder和Creative Tab。没有生产兼容修改，没有启动4C-5。

玩家实际食用、燃料消耗、客户端UI和重启/物品持久化功能均为 **NOT RUN**；本报告不作FnF全部功能已适配的结论。

## Gate 0–1：基线、环境与身份

PR #28于2026-10-16T03:39:11Z合并；最新main `6a9d867edf8751e8ff188dce44e66c1a3bf7ecf8` 同时包含指定 `7fa33b6d032094478f0bd03c7ee2d6346633a4a0` 和证据回写 `a492d002391c61d99be8c39f45ad66a055b1dfbb`。main Actions [38846585862](https://github.com/GFander/DualForge/actions/runs/38846585862) 实际SUCCESS。C3远端ZIP从固定提交URL重新下载，原SHA和1767项清单核验PASS；本轮没有复用未授权的新上传。

独立worktree `/workspace/DualForge-007C4C4`，分支 `codex/convergence-007c4c4-fnf-runtime`。当前生产源码与main和生产提交 `9fc9cde97888a2d7894252dc59702d8548d5a5e5` 完全一致，源指纹 `02e8d50e1dbf81bc79478f0041201905d7a6d7d071c6ad7c27667cee3d2ae25f`。本轮使用既有验收JAR的物理副本，没有重建/改包。

| 输入 | 大小bytes | SHA-256 |
|---|---:|---|
| 原FnF 1.3/MC1.21.x |128098|`bd415b04529c4ffc31e14757488f71dab6917e4560c61e236c3c4d0de6557958`|
| DualForge 4C-3 |152831|`a26893598d1ffdb73078373b007a4d5d54dddd1b774cb2a0fa0dd90fe5602bf7`|
| 原生Neo共存模组 |2265|`270c30aca9ddd42b625efe6e1eeefafa7865b5b4c4d5ae709a0623109cf5251c`|
| 独立Forge观察器 |4482|`e77cc31f9e724a017673f947b6039e45c3475dd67a94adc6fde5af6f63f53a06`|
| 独立Neo观察器 |5152|`6435eea702dc0f09db2ab3230169367df4d04a2fddc240c1deced8159a44a3d1`|

实际MC1.21.1/Forge52.1.0/NeoForge21.1.256/FML4.0.45；Microsoft OpenJDK21.0.10+7-LTS、javac21.0.10，Linux x86_64、Python3.12.14。实际二进制SHA、Manifest身份、Java版本输出、官方参数文件和完整classpath保存在本轮preflight，不以历史环境报告替代。

## Gate 2–4：原生基线、标准C/T与独立观察

全部JVM/编译串行。每次从官方安装物理复制到新目录，生成全新世界；启动均为 `sh run.sh nogui`。记录真实Java命令和官方argfile内容，无Agent、诊断SPI、业务开关/环境变量改动、额外module参数或用户JVM参数。Control和Treatment的所有已安装文件大小/SHA与Java命令逐项比较，唯一增加的是原始FnF JAR；另一个观察器C/T独立成组，同样只增加FnF。

| 本轮阶段 | 工具源码提交 | 生命周期/退出 | 秒 |
|---|---|---|---:|
| forge-empty-network | 0ff21c9 | PASS / 0 | 35.19 |
| forge-fnf | 0ff21c9 | PASS / 0 | 38.54 |
| forge-fnf-observed | 0ff21c9 | PASS / 0 | 44.44 |
| neo-empty | 0ff21c9 | PASS / 0 | 32.38 |
| neo-control | 0ff21c9 | PASS / 0 | 29.71 |
| neo-treatment | 0ff21c9 | PASS / 0 | 28.22 |
| neo-control-observed | 67e3bb7 | PASS / 0 | 28.79 |
| neo-treatment-observed | 67e3bb7 | PASS / 0 | 32.36 |

原生Forge标准运行仅安装原FnF；FML发现、依赖校验、加载及生命周期到达Done，`forge mods`列出FnF。独立观察使用原样的旧NativeRegistryObserver源码重新编译，查询真实Registry与Holder：67个Item、67个Holder全部同一对象，`fnf:fnf_item_tab`的Holder返回真实CreativeModeTab。观察器只读查询Food/Fuel API不代表玩家食用或炉内燃烧；此前007C PARTIAL是冻结历史，本轮仅补齐服务端注册/启动证据。

Neo空宿主、Control及Treatment正常启动/停止。原生共存模组构造及ServerStarted各一次；内置javafml委托正常。所有健康阶段严重日志header及次数与本轮对应空宿主精确一致：有新目录server.properties缺失和Yggdrasil公钥请求网络错误。**不是严格零ERROR**；未删除或按关键词屏蔽这些错误。无新增严重记录、stderr、崩溃或dump FAIL。

独立Neo观察器使用原生API编译，不编译或改写FnF/DualForge，不拦截业务调用。Control实际0个FnF Item/Tab；Treatment的67个Item完整ID集合和运行时业务类名与原生Forge一致，67个Holder.get均为对应Registry对象，Tab Holder与原生Registry对象相同。实际容器Mod Bus身份与两个桥接收据相同；Global Bus身份与全局桥接收据相同且两Bus不同；原生Global监听器表中FnF对象0个，符合空监听器注册语义。记录官方GameData.postRegisterEvents实际调用栈。没有主动执行icon/display回调。

## 五个证据层次与首次失败

| 层次 | 本轮实际证据 | 结论 |
|---|---|---|
| 转换计划/转换字节码返回 | 标准Treatment HELPER_PREPLAN与PUBLISHED(3)，三条HELPER_TRANSFORM绑定同一plan | PASS；不是只读Atlas模拟 |
| JVM实际定义/链接 | 三个目标随后作为Context caller和Registry bridge helper Class参与真实宿主检查，FML实例化/初始化及注册查询成功 | PASS；未用静态计划替代类定义 |
| 原构造流程 | Context BOUND一次，Item/Tab两个ATTACHED，最后Global REGISTERED一次，FML正常完成加载 | PASS；原Jar无marker插桩 |
| Registry完成 | 独立原生Forge/Neo实际67 Item ID、Holder及Tab身份对照 | PASS；观察组与标准组分别记录 |
| 服务端正常运行 | 标准Treatment Done、stop、退出0及独立新世界保存 | FNF_SERVER_BOOT=PASS |

标准Treatment计划 `0144ea3aafdc4ff4431fa74d40e310f7ab3db52b665a473582bead517b6da0a5`。三个目标为FoodNFuel、ItemRegistries、FnFCreativeModeTabs；两个Helper的实际Mod Bus `736169734`，Global Bus `612318847`，原入口对象identity `932127950`，`listeners=0 outcome=EMPTY_SOURCE_TRANSLATION`。这些数值仅属于本次标准运行，观察运行的独立身份不混用。

**本轮未发生FnF首次加载失败**，不存在可交付的FnF异常链或4C-5修复。已保存全部原日志中的环境异常，不能把Settings/网络错误误报为FnF首失败。五个原始ClassFile及本轮实际javap逐项SHA保存；它们是来源ABI证据。标准JVM未启用class dump，**实际最终定义字节未导出/无独立SHA**；不把原ClassFile或旧离线输出冒充本轮JVM定义字节。

## Gate 6–7：验证、保全、交付与限制

本轮68项跨阶段/注册/身份/生命周期断言PASS；错误制品SHA的两个自动负例均在创建安装/JVM前拒绝；工具Python语法与374项冻结保护PASS。原安装运行文件与依赖SHA、440项既有生产/历史源码/报告、C3的1766个原附件及ZIP均未变；生产diff为空。所有JVM结束，端口25565 bind/listen/close核验PASS。

新世界正常保存全部维度，level.dat gzip可读；所有非空region chunk的sector边界和压缩内容可读。个别entity-region为零字节占位文件，健康Control中也存在，单独记录不声称校验空payload。初版附加读取器错误地要求每个MCA非空而FAIL，原脚本/失败输出保留；v2按明确的有限读取范围记录，不重跑或改写服务器结果。该检查不是完整世界一致性、重启或物品持久化验收。

最早一次Forge空宿主调用被云sandbox拒绝socket，发生在安装/JVM之前；原失败保留，之后在新目录使用网络权限运行。所有运行目录中的旧结果不覆盖。现有runner只删除本轮自己的、已核对SHA的物理运行库副本，世界、mods、原始命令、日志、结果均保留。

原始附件本地ZIP `out/convergence-007c4c4/evidence-export/DualForge-007C4C4-Evidence.zip`：**627450 bytes、193个payload文件、3034503 bytes**，SHA `0e022f4ebbb5cc932b88290dd1314bd258be6923733d02485a1c421eeaf77859`。Manifest SHA `7f1f3a0b4fc6335956b42ea9a9f4c09ce5214b44309ddae61d9aac6aa6c936f5`。ZIP/CRC/路径/重复/软链接、Manifest、解压逐文件SHA及192个原文件不变核验PASS。包含命令、退出、日志、查询、指纹、本项目制品和必要原始FnF五ClassFile；不含世界、第三方运行库、完整FnF JAR或实例配置。它是选定附件，不是完整工作区备份；原路径保留，生成时未作公开发布审查；后续审查结果见下节。

## 原始证据持久化与远端复核

**PERSISTED / VERIFIED**。用户于本轮明确授权上传4C-4证据后，已将上述原ZIP原字节上传到 `evidence/convergence-007c4c4`，远端提交 `36f23ce10e4a9b74dd2522fa096dd095a3128f15`。没有复用旧C3授权，没有生成替代历史日志。

[固定提交证据ZIP](https://raw.githubusercontent.com/GFander/DualForge/36f23ce10e4a9b74dd2522fa096dd095a3128f15/DualForge-007C4C4-Evidence.zip)；文件大小、ZIP/Manifest SHA及覆盖范围保持上文原值。核验时间：2026-10-16T04:17:03.039995+00:00。从公开URL独立下载至新目录，HTTP、ZIP SHA/CRC、Manifest/SHA256SUMS、无路径穿越/软链接/重复成员及解压后193个文件逐项大小/SHA全部PASS；192个原文件再次核对不变。结构化复核收据嵌入机器索引archive.remoteVerification。

公开前模式扫描与内容范围复核PASS：47个邮箱模式命中实际为45个Java模块版本串及2个公开Git SSH地址，未发现个人邮箱/凭据，未作脱敏改写；保留/workspace路径。仅含已声明的本项目制品及五个必要FnF原ClassFile，排除世界、运行库、完整第三方JAR和实例配置。安全复核记录随证据分支保存。ZIP内README/Manifest保留生成时“尚未授权/未发布”的历史快照，不重写原归档；本节与证据分支外层README记录之后的授权、上传和独立复核。

本次只持久化证据并更新报告/索引及PR，生产Java、测试工具和已测JAR未改，不重新执行游戏回归。此前FNF_SERVER_BOOT=PASS及所有NOT RUN边界保持。后续Git/CI收据由PR实时链接，非原ZIP内历史运行附件。

[运行和重跑命令](../tests/real_mods/food_n_fuel/runtime/README.md)，[机器索引](<Convergence-007C-4C-4 证据索引.json>)。当前[PR #29](https://github.com/GFander/DualForge/pull/29)，基线main，最终交付HEAD/CI由PR及最终回复实际核验；不将旧main CI代替新HEAD CI。

停止于本次首次服务端运行验证。真实食用、燃料、客户端创造栏显示、icon/display回调及重启/物品持久化NOT RUN；不宣称整个FnF已完整适配。不修改生产代码、不合并、不Release、不进入4C-5。
