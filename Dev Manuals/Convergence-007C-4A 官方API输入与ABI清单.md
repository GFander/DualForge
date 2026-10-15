# 官方API输入与ABI清单

日期：2026-10-15（Asia/Shanghai）。独立 Draft [PR #22](https://github.com/GFander/DualForge/pull/22)，base 为未合并 PR #21 分支。任务起点 `29893683ea584b26208eb205b819c3f3376147be`，实际被测工具源码 `25571c40671d6d80ca10a4dcec77db84440ff555`。生产目录与起点逐文件相同。

184个实际输入：183个固定官方服务端安装JAR，以及实际Microsoft Java21.0.10+7-LTS的java.base JMOD；共347300558字节（包含未启用中间制品，不是API大小）。所有SHA/大小/JAR中央目录inventory指纹、坐标、来源和实际ClassFile major版本见 [完整输入Manifest](../tests/api_atlas/official-inputs.json)。原冻结清单不改；该发布清单补齐ASM实际版本，并把已启用的MCP srg2off数据正确标为运行时命名数据。

250项安装器/Bootstrap checksum PASS；184项大小、SHA、member inventory再次PASS。Forge在线Maven `.sha1` 请求403原记录保留，不称远端成功；对应现有官方安装器内置SHA-1与官方shim清单SHA-256匹配，包括生成的补丁server。NeoForge四个在线平台checksum也匹配。本轮不重新安装、修改平台核心或启动游戏。

Forge bootstrap-shim.list逐路径确定资源；NeoForge官方unix_args的module/legacy路径和固定game资源确定选择。FML4.0.45真实NeoForgeServerLaunchHandler / ProductionServerProvider源码与新javap共同证明：补丁server覆盖重命名base，其余类由base提供；universal为平台mod。1171个重复MC类显式选补丁提供者；module-info按所属artifact保留，MR按Java21选版本。开发JAR、未使用旧版本和原始/中间Minecraft二进制仍在全量扫描和重复源记录中，不混入可用服务端视图。活动视图无未解provider冲突。

平台名称空间声明、内部实现（public也不保证是受支持API）、第三方运行库、JDK、Minecraft原版/平台补丁、开发工具链分别归属。公共/保护统计基于ClassFile及外部可访问enclosing class；不是JPMS出口或未来生命周期承诺。共享库不算Forge公共API。

**完整补丁后客户端二进制未取得。客户端Minecraft ABI为PARTIAL / INPUT_MISSING，停止该部分推断。** 当前拥有完整可见服务端patch/base集合；不能从服务端推断客户端全量。运行时Mixin/AccessTransformer增加的形状也不属于本次原始ClassFile快照。

核心官方制品（其他支持库、源URL/生成来源及版本详见完整清单）：

| 制品 | 字节 | SHA-256 | ClassFile major |
|---|---|---|---|
| eventbus-6.2.27.jar | 58819 | d2a7bcecb80b33f90854a003dc295960263d78435d9d6016c427d711510cb999 | 60 |
| fmlcore-1.21.1-52.1.0.jar | 122624 | be4d001363625d8dccaefaf9bdedc1509e03c3ba452e3319f9d99dc75fa3c93a | 65 |
| fmlloader-1.21.1-52.1.0.jar | 247290 | 6ca33b9f9e0bb6e58076c2e38002174eddf01d80b54c313824712a309d0183a9 | 65 |
| forge-1.21.1-52.1.0-server.jar | 20536125 | 2dea3fefad7ddf03e0a609cb2084a651732a35ae46dcf1d949d9359405a8621f | 65 |
| forge-1.21.1-52.1.0-shim.jar | 10698 | f7b706c79896728723da0e4e8c5a6ecdb6a617ad4df9d7ea1ddc6367187bc291 | 52 |
| forge-1.21.1-52.1.0-universal.jar | 2617057 | 88ef6625122cfdb9d7b3176bd61b8c0692a191bad0edd77ffddc6a765ee4979c | 65 |
| javafmllanguage-1.21.1-52.1.0.jar | 19235 | d27e1f3149ea08fc0a80779fe24fa8585692c5042a34bad153e55662486acc15 | 65 |
| bus-8.0.5.jar | 30340 | 705e24f37752809499a5cd0c7dd31eaabf3d933c5d3b2060168e94e9ef62593a | 61 |
| earlydisplay-4.0.45.jar | 261719 | 27a791a19a7967b89cbaa820e7748ad6a1e9d8460217d232c4006f852884044f | 65 |
| loader-4.0.45.jar | 505696 | 8390398e67df23e72d85ed809685be2752c8ec545b7cbb6ad4b70fdc81e4dcd4 | 65 |
| neoforge-21.1.256-server.jar | 5675634 | f57ce3018e21459d24b50508e8c1b4fd5c16324ec1dc5f316c192aa0130e7fe0 | 65 |
| neoforge-21.1.256-universal.jar | 3552847 | e0a08bae5cea95be96c09770530f5643bb183af8af59c64694eb346cb4c93fa4 | 65 |
| java.base.jmod | 24418230 | bb821c005afabb2d35963441df0cc472fbad91f749e9d2a311c309c79c7b761b | 65 |

原FnF为128098字节，SHA `bd415b04529c4ffc31e14757488f71dab6917e4560c61e236c3c4d0de6557958`；只读取，不重包。已有DualForge 007C-3 JAR SHA `ccdb80f50c2b511108c1df2e20232d379556d7b1d6d82fcc73f2f9c4eb05865f`，仅供既有规则字节码盘点，不是本轮新构建/新运行制品。

实际Java/javac/Python3.12.14/zsh5.9/Git/OS输出见preflight/environment.json；工具编译依赖为官方已有ASM9.10.1五件和Gson2.10.1，命令/依赖SHA均随每次javac保存。完整输入及原命令在证据索引引用的归档中，不提交运行库。
