# Convergence-007C-4C-2 交付报告

**受控服务端 Typed Registry / Creative Tab / Holder Consumer：PASS。** 客户端显示及FnF完整运行NOT RUN；原始证据已获具体授权，公开持久化并独立下载复核：PERSISTED / VERIFIED。

## Git与制品

PR26已于2026-10-15 17:01:08 UTC正式合并。新分支 `codex/convergence-007c4c2-typed-registry` 从最新main `854d378a1caf44ae297dfffe4defc5eb65181b9f` 建立独立worktree；已核对4C-1最终035049e和生产d850b1c均在祖先链，起点工作区干净。

先行设计e924661；初始实现f0d87d8；最终生产补正 **b552f2e42407c7da9a113919f8a20a4070f78b0f**。测试与文档分别提交，最终PR HEAD和Actions以PR正文/交付回复实际回读记录，不以旧HEAD的CI代替。

最终 `DualForge.jar`：**141341 bytes**，SHA-256：

`1afc82338206901c7057951aa2202c8dadd75cdd95d47fd112bb166d8faa6038`

原生Forge输入 `tab-boundary.jar`：SHA `e440da9d73a04c0ec76798c723fee074be83bc176f1b51834b022b640722cd84`。由官方Forge52.1.0纯依赖干净编译；源码/class/JAR、编译classpath、命令与独立Forge/Neo观察器SHA均冻结在tests/typed_registry/fixture-identity.json。两个宿主使用同一原JAR，没有预改成NeoForge输入。实际Java为Microsoft21.0.10+7-LTS；MC1.21.1、Neo21.1.256/FML4.0.45、Linux，版本输出与安装输入SHA均保存。

## 实现与差异

生产只改四文件：

- ForgeRegistryTransformationPlan：独立Item/Creative Tab Kind，ResourceKey精确create、双泛型、Registry key/namespace/path复合ID，完整回调/Consumer闭包与既有clone/ASM/归属/SHA发布门禁。
- ControlledCreativeTab（新增）：按已验证API调用点与栈数据流验证builder、零捕获icon/display、Item Holder/getter来源及输出；不按FnF名、字段名或输出数量白名单。不提前调用延迟回调。
- ControlledItemSupplier：只新增供上述已知宿主调用使用的只读成员/descriptor可访问性检查入口，原Item业务验证不放宽。
- ForgeRegistryBridge：保留原owner/caller/module/fingerprint/receiver/Mod Bus检查，按实际Kind分别核对ITEM或CREATIVE_MODE_TAB；官方异常后保留UNCERTAIN并拒绝重试。提取私有下游guard用于独立白盒负例，没有公开Bus解包。

未改FML发现、Reader/Parser、依赖投影、Module Layer/Descriptor、旧Profile及构造器ABI；未实现Forge全局Event Bus。精确API对照、支持条件与五个业务方法的边界差异见[设计](<Convergence-007C-4C-2 Typed Registry设计与ABI.md>)及[类型/闭包契约](<Convergence-007C-4C-2 类型与回调闭包契约.md>)。

## 本轮实际门禁

| Gate | 结果 |
| --- | --- |
| 基线/历史保护 | PASS，PR26合并及祖先确认；16个固定输入、旧4C-1的25源码和归档内1451份原件SHA核验 |
| 真实ABI取证 | PASS，Forge/Neo实际javap、原FnF ClassFile/Bootstrap、字段/方法/generic/API对应 |
| 原生Forge新Fixture | PASS，真实两个Item与一个Tab，同名shared跨Registry合法，延迟回调和早消费拒绝 |
| 修改前新Fixture Neo | 精确复现4C-1的ITEM_FIELD_DESCRIPTOR，真实metadata后、构造与Done前 |
| 最终JAR新Neo C/T | 2/2 PASS；相同compat/fixture/observer/native及stock输入，仅DF4C2_REGISTER切换 |
| 新Typed Contract | 40 assertions / 25严格负例PASS，完整计划拒绝、原JAR和ClassNode保持 |
| 新Bridge诊断 | 6项PASS：重复、wrong owner/receiver/key、官方partial failure及uncertain retry |
| 原4B/C1动态 | 最终同JAR的4B C/T、C1三组C/T全部PASS；旧8项Bridge负例PASS |
| 原生Forge回归 | 本轮原4B和C1三组共4次PASS；原JAR/观察器不变、无compat安装，生产补正不影响此独立基线 |
| 历史严格静态 | 4B110/35、Context80/22、007B13加JPMS、C1 Supplier86/19及3整计划失败；Event/Registry/Multi/Block、metadata/platform均PASS |
| FnF只读 | typed-v1精确GLOBAL_EVENT_BUS_UNSUPPORTED；67 Supplier独立只读PASS；FNF_RUNTIME=NOT RUN |
| 工具/保全 | Python88、生产ASM源10 assertions/8负例、Java精确判定3正12负、374冻结、zsh/diff PASS |
| JVM/端口 | 无java/javac/javap，实际127.0.0.1:25565临时bind/listen/close成功 |
| 最终HEAD CI | PR建立后回读，静态CI不冒充游戏动态验证 |

所有JVM串行，stock启动、物理副本。新C/T分别核验官方RegisterEvent、真实Registry完整ID、Holder/对象身份、Supplier各一次/零次、正确Mod Bus、native共存、四个且仅四个Target、正常Done/stop/exit0、空stderr/无crash。两次getIconItem只执行一次icon，buildContents实际执行一次display并输出两个正确Item；注册期回调均零。它们是服务端实际API查询，不是客户端创造栏渲染。

桥接诊断独立观察器暂替换本Fixture的receiver和真实容器Bus，调用真实原生register：先置原生flag并向真实Bus加一个监听器，第二次添加抛异常；实测flag=true/listenersAdded=1/UNCERTAIN，恢复原引用后重试拒绝且不再次调用。wrong receiver/key用私有下游guard白盒注入，不能冒充完整caller链正例或安全沙箱，不能宣称回滚了官方副作用。

健康运行与同轮空宿主完整severityRecords比较。Settings/Yggdrasil网络ERROR实际存在，strictZeroErrors=false；没有把相同基线错误声称为零ERROR。原世界不打开/覆盖，运行目录全新；不声称另做了未入索引旧世界的全盘快照。

## 准确FnF边界与未支持项

[只读报告](<Convergence-007C-4C-2 FnF只读边界报告.md>)记录强制完整计划在源类型/回调闭包准入后、输出克隆/发布前拒绝真实入口的全局Bus调用。正常候选排除保留，未删除该调用或返回no-op；未生成或运行FnF输出。旧supplier-v1收据不改，当前typed-v1只接受唯一新原因和版本。

只支持明确的Item与CreativeModeTab两类Registry、限定Builder/零捕获/直线回调与受控Holder/getter；未知Registry、API、控制流、异常处理、捕获、复杂Handle、不完整闭包仍拒绝。客户端、FnF游戏运行、真实食用/燃烧、Tab UI及未受影响历史全量游戏矩阵NOT RUN；4A历史PARTIAL不变。不进入下一阶段。

## 迭代与证据

保留首次观察器classpath编译失败、非法负例label构造失败、首次发现计数合并debug/latest导致的runner FAIL。修正了取证脚本后以新目录重跑，没有放宽唯一发现断言。

初版e6bc...候选完成了运行检查，但补充负例实际证明其错误接受两个NEW代替DUP的未初始化icon对象；b552f2e收紧单次分配。最终1afc...上所有受影响静态与Neo动态完整重跑，旧e6bc结果不计入最终cohort。最后另补三项目标Tab双泛型直接断言，重跑静态Contract至40/25；生产及动态判定未再变。

环境重连使旧PTY id失效，但完整命令/退出/结果文件已完成并保留；检查这些现存记录、SHA与进程状态后继续，没有伪造或覆盖历史输出。

原始附件在out/convergence-007c4c2及机器索引所列同轮路径。最小归档 `out/convergence-007c4c2/evidence-export/DualForge-007C4C2-Evidence.zip`：**4130026 bytes**、2282个payload文件，SHA `f7c909d47d14a73e9f46af07bfce1f4e2684f44c7041f316420afe0e56ab3b06`；Manifest SHA `22ac884a1f13d505a6c2fcac15bb483223696954fc04e7845b4fee4132e36924`。ZIP/CRC/清单/逐文件SHA与原件不变通过；排除世界、完整服务器配置、第三方依赖及原FnF二进制、重复编译文件，包含必要自有制品和before/after，不是所有原始附件无裁剪副本。六份发布副本仅脱敏email-like文本，原SHA和发布SHA分开，原件未改。

## 原始证据持久化与远端复核

**PERSISTED / VERIFIED**。用户明确批准本次指定ZIP及公开分支 `evidence/convergence-007c4c2`；证据提交 `56d92181c2ebefdcf34330347ada5a969f67c1a7`，本地/远端分支SHA一致。

[不可变ZIP下载](https://raw.githubusercontent.com/GFander/DualForge/56d92181c2ebefdcf34330347ada5a969f67c1a7/DualForge-007C4C2-Evidence.zip) · [Manifest](https://github.com/GFander/DualForge/blob/56d92181c2ebefdcf34330347ada5a969f67c1a7/MANIFEST.json)

2026-10-15 18:21:09 UTC从公开URL独立下载到新目录 `out/convergence-007c4c2/remote-independent/`，HTTP读取、ZIP大小4130026/SHA、CRC、成员无重复/路径穿越/软链接、清单/校验列表/解压后2282文件逐项SHA均PASS；总payload25557167 bytes。再次核对2281个原始文件大小/SHA未变，另1文件为清单明确标记的生成README。发布副本与原件的六项脱敏关系保留；安全扫描/排除范围见上文和Manifest。复核结果及命令嵌入机器索引，不改已上传ZIP。

本地复核命令：`python3 -B tests/typed_registry/verify_archive.py <独立下载的ZIP> --sha256 f7c909d47d14a73e9f46af07bfce1f4e2684f44c7041f316420afe0e56ab3b06`。ZIP原始命令/结果保留历史绝对路径，远端链接可独立取得附件，不把本地路径当作下载地址。

旧4C-1归档及远端引用保持原样。重跑命令见tests/typed_registry/README.md；原始指纹/命令/结果关系见本轮机器索引。代码和报告按路径分开提交推送独立PR，不自动合并、不创建Release。
