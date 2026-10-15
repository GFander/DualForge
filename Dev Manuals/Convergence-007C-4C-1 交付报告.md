# Convergence-007C-4C-1 交付报告

**Convergence-007C-4C-1 受控范围 PASS；原始证据 PERSISTED / VERIFIED。**

起点为已合并PR25的最新main `47693225ce71c7241a0bc8579c62b062df15f30c`，祖先包含4C-0最终 `fc0ad24165d3bc046c5f2a9594e1542ab1d3fa12`。独立worktree与分支 `codex/convergence-007c4c1-supplier-boundary`，没有混入其他工作区改动。

生产源码冻结提交 `d850b1c7bf3b16609ed17a58b3fe2eec0c11b751`；最终 `DualForge.jar`：**131028 bytes**，SHA-256：

`8fd97a0a4e811aed4e8e084f172341c1b808837e202a22986705e9f0c33360c2`

源码逐文件指纹、Git中不可变基线、工具链、编译输入及所用67个实际host provider SHA见[机器索引](<Convergence-007C-4C-1 证据索引.json>)。基线源码清单从原Git blob延后记录，明确不是任务前文件系统快照。当前生产目录逐字节匹配生产提交；后续测试与文档提交未改生产。

## 实现与范围

只改 `ForgeRegistryTransformationPlan.java` 并新增只读 `ControlledItemSupplier.java`。在旧精确路径之外，验证API/descriptor/类型/继承/返回值/可访问性及同ModFile前向业务依赖，不按FnF/类名/Food/Fuel类别放行。现有Registry API边界改写、owner/modId/module/SHA/完整计划及clone原子性保持。业务方法不重写，必要数据/子类不成为Target，host ABI SHA加入计划身份。跨包protected访问暂拒绝。

详见[先行设计](<Convergence-007C-4C-1 Supplier准入设计.md>)、[业务保留证明](<Convergence-007C-4C-1 业务逻辑保留证明.md>)和[新旧回归](<Convergence-007C-4C-1 新旧规则回归报告.md>)。没有改发现/FML/平台依赖/模块层/旧Profile/Tab/全局Bus或冻结输入。

## 本轮实际结果

| Gate | 结果 |
| --- | --- |
| 新ordinary/data/subclass 原生Forge Treatment | 3/3 PASS，纯官方Forge编译原字节输入 |
| 同最终JAR NeoForge Control/Treatment | 6/6 PASS；同fixture/observer/native SHA，仅DF4C1_REGISTER改变 |
| 原4B Helper NeoForge C/T | 2/2 PASS，同最终JAR |
| 真实Bus桥接独立诊断 | 8项负例PASS，诊断不混入标准C/T |
| 新Supplier/完整计划 | 67只读正例、19负例及3整计划负例PASS；6个业务方法规范化相同 |
| 原4B/007B/Context | 35 / 13加JPMS / 22负例PASS |
| Event/Registry/Multi/Block、metadata/platform | 受影响原静态契约PASS，未放宽旧断言 |
| 生产ASM / FnF精确拒绝判定 | 10 assertions / Java2正9负PASS |
| Python / 374冻结 / zsh / diff | 84测试PASS；保全与语法PASS |
| JVM与端口释放 | 无java/javac/javap；实际127.0.0.1:25565临时bind/listen/close成功 |
| GitHub Actions | 以最终PR HEAD的实际回读为准；此报告提交时尚待PR创建 |

每个动态运行核验真实注册ID、两个Supplier各一次/零次、Holder及对象身份、无重复、正确Mod Bus、官方RegisterEvent路径、native共存、Done/stop/exit0、stderr空及无crash。Food数据nutrition=7/stack7，独立子类override返回2468/stack11与初始化次数均实测。空宿主有Settings/Yggdrasil网络ERROR，各轮完整severityRecords相同；保留strictZeroErrors=false，不宣称零ERROR。

## FnF与未完成范围

67个真实FnF Supplier独立只读通过；完整强制诊断下一处精确拒绝为 `Registry helper rejected: ITEM_FIELD_DESCRIPTOR`，对应CreativeModeTab类型字段。[只读进展报告](<Convergence-007C-4C-1 FnF只读准入进展.md>)区分新supplier-v1与旧4B历史原因。正常Helper候选仍因全局Bus被排除，不发布FnF输出。**FNF_RUNTIME=NOT RUN**。

未知控制流、反射/动态加载、复杂Handle/额外业务indy和未解析外部依赖保持拒绝。本轮不是通用Java安全沙箱。客户端、真实吃食物、炉内燃烧、Tab、FnF完整兼容均NOT RUN；4C-A原PARTIAL不变；不进入4C-2。未重跑的历史完整游戏矩阵不冒充新JAR动态验证。

## 原始证据归档

本地归档：`out/convergence-007c4c1/evidence-export-final/DualForge-007C4C1-Evidence.zip`。

- 大小：2,562,401 bytes。
- SHA-256：`7b99ef408f518a787c3ad4f36454817e35d2e467155013751ca33af3dd0d6b9e`。
- Manifest：1,452个payload文件，18,703,560 bytes；逐文件原路径/原SHA/发布SHA/大小/脱敏关系与排除项。
- Manifest SHA：`94d9fca0bd112694c66697ec3a3b015f3afe886fbaf70101cd49254b189daf08`。
- 已核验外部ZIP SHA、CRC、成员集合、Manifest及SHA256SUMS；原件归档前后SHA不变。
- 最小选择归档包含命令/退出/日志/断言/环境/源码与输入指纹/本项目制品和必要before-after类；排除世界、完整服务器副本/配置、第三方库、重复编译类和中间JAR，并非所有附件的无裁剪副本。四份发布日志脱敏email-like文本，原SHA与发布SHA分开；原始工作区记录未改。
- 当前状态 **PERSISTED / VERIFIED**。用户明确批准此具体ZIP及目的地后，已推送独立 evidence/convergence-007c4c1 分支 `d9acfcc1d4e96602d91c43ecfd1cc4c2bc91102c`。随后使用无认证公开URL独立下载至新目录，ZIP SHA/CRC、解压后全部Manifest及SHA256SUMS通过；原始文件SHA再次不变。核验时间：2026-10-15 16:50:36 UTC。未使用WebDAV或Release。
- [公开证据ZIP](https://raw.githubusercontent.com/GFander/DualForge/d9acfcc1d4e96602d91c43ecfd1cc4c2bc91102c/DualForge-007C4C1-Evidence.zip)；[远端Manifest](https://github.com/GFander/DualForge/blob/d9acfcc1d4e96602d91c43ecfd1cc4c2bc91102c/MANIFEST.json)。本轮最初上传被自动审批因缺少具体授权阻止；用户批准后原封不动重试成功，无绕过。

主要原始运行目录为 `out/convergence-007c4c1/`；最终权威cohort在 `cohort-final.json` 与 `runtime/verified-*-invocation/result.json`。归档还保留失败和作废尝试，详见回归报告，不将初版PASS混入最终结果。

重跑入口及参数见 `tests/supplier_boundary/README.md` 与 `tests/helper_registry/README.md`。独立PR不自动合并、不创建Release。当前无服务端受控目标的剩余阻塞；最终PR HEAD与GitHub Actions实际结果将在PR正文及交付回复回读记录。
