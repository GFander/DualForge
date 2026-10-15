# Convergence-007C-4B Git基线收敛报告

日期：2026-10-15（Asia/Hong_Kong）。本次仅完成用户任务Gate 0规定的独立集成PR，**4B状态为BASELINE_NOT_CONVERGED / BLOCKED**。不将已经审核的C3/C4A实现称为本轮新增Registry功能。

## 实际基线与合并方向

开始工作区`/workspace/DualForge-007C4A`干净，HEAD为`68e5dc3cf634206da08d70010198112fa43badfa`。先执行`git fetch origin --prune`，再按实际单main的refspec显式fetch三个任务分支。实际最新main为`bdf0020cc076c86dd0fac875260bbae680dd0bb1`，当时main Actions [38712899512](https://github.com/GFander/DualForge/actions/runs/38712899512)为success。

- [PR #20](https://github.com/GFander/DualForge/pull/20)：MERGED到main，merge=`bdf0020cc076c86dd0fac875260bbae680dd0bb1`。
- [PR #21](https://github.com/GFander/DualForge/pull/21)：MERGED到`codex/convergence-007c2-forge-dependency`，merge=`bbe6812bcaa8fd71c81e41bd6a95d5ae25e894c6`。
- [PR #22](https://github.com/GFander/DualForge/pull/22)：MERGED到`codex/convergence-007c3-entry-context`，merge=`9c4e03d649c13e2f5c8c41fea01aed91512c7201`。

C3最终`29893683ea584b26208eb205b819c3f3376147be`和C4A最终`68e5dc3cf634206da08d70010198112fa43badfa`对main的`merge-base --is-ancestor`均返回1。main实际缺少`ForgeConstructorContext.java`、`ControlledForgeContextProfile.java`、`tools/api-atlas/OfflinePrototype.java`、`tests/api_atlas/summary.json`四项；不能以PR的Merged标记声称main收敛。

独立worktree：`/workspace/DualForge-integration`。分支：`codex/integrate-007c3-007c4a-main`，base=`main`。集成[PR #23](https://github.com/GFander/DualForge/pull/23)。本轮未创建正式4B功能分支或重复的4B功能PR。

## 精确集成与保全

通过`git merge --no-ff --no-commit 68e5dc3cf634206da08d70010198112fa43badfa`正常合入，无冲突。合并提交`78e9fc5f1d78b68f573d6976e719ec4778423633`具有两个原始父提交；其**整个tree等于已审核C4A最终tree** `4528c2d55affc9713defea7a86eb83cb12dff3f7`，没有生产补丁、手工冲突修改或历史改写。

相对main实际补入62个路径，均为此前C3/C4A的已审核差异；之后仅新增本报告、证据索引和导航。21份生产Java/resources逐SHA一致，C2平台依赖resolver相对main原字节不变，C2没有复制实现。C4A工具、Schema、固定输入索引、测试完整保留；AGENTS完整六节Git规范与结构约定保留。所有原有冻结报告和结论保持原字节。

集成PR本身通过不代表main已收敛。只有用户将该PR合入main后，再fetch并核验C3/C4A祖先、四项实际文件、源码指纹和main CI，才能建立`codex/convergence-007c4b-registry-bridge`并进入Gate 1。本轮不自动合并。

## 本轮实际验证

被测源码提交统一为`78e9fc5f1d78b68f573d6976e719ec4778423633`。Python3.12.14、zsh5.9、Microsoft JDK21.0.10+7-LTS、Linux6.18.44 x86_64/glibc2.41均为本轮实际读取。

| 检查 | 实际结果 |
|---|---|
| 身份和冻结保护 | PASS，699个tracked扫描、374项冻结保护 |
| Python单测 | PASS，6组共63个，含C4A19个；未修改历史断言 |
| 静态语法 | PASS，58份Python AST、25个zsh脚本 |
| 干净构建 | PASS，原生产源码编译；没有新增或修改生产逻辑 |
| JAR成员对照 | PASS，新JAR全部成员字节与C3原交付件逐项相同 |
| C3 Context Contract | PASS，80断言、22严格负例，包含真实Reader/FML扫描；这是离线Contract，不是新GAME层Bus运行证明 |
| 007B旧严格Contract | PASS，原13负例、1显式JPMS拒绝，Supplier业务不改 |
| C4A离线Contract | PASS，38原型断言/15拒绝、9个scanner断言、独立`java -Xverify:all`、确定性及原FnF离线拒绝；候选仍不启用 |
| Git diff检查 | PASS；提交后最终HEAD另行核对 |
| 游戏服务端/客户端 | NOT RUN；本轮启动游戏服务器数为0 |

新构建件：`/workspace/DualForge-integration/out/convergence-007c4b-gate0/build-output/20261015-094837-1868316b/dualforge.jar`，98308字节，SHA-256 **`c22f4276331e1418a2e44f4b3639f0d636bbc768f07c0ee8c19da6b364ce7085`**。这是本次集成编译验证件，**不是已完成4B Runtime验收的JAR**。它的ZIP容器SHA与历史C3不同，成员字节全部相同，不能将旧运行结果冒充新SHA的动态PASS。

精确重跑命令和argv/cwd/stdout/stderr/exit保存于`out/convergence-007c4b-gate0/checks.py`及各命令目录。入口为：

```bash
python3 -B out/convergence-007c4b-gate0/checks.py
```

该调度器是本轮外部原始附件，不是仓库跟踪脚本；现有tests/README与各Contract README提供可移植命令。全新重跑须使用新输出目录，不能覆盖本轮记录。Java编译和所有JVM检查严格串行。

## 尚未执行与停止点

- MAIN_BASELINE_CONVERGED：**BLOCKED**；独立集成已准备并验证，尚未合入main。
- EXISTING_PROFILES_PRESERVED：本轮源码/成员字节及离线负例PASS；历史游戏回归只记INHERITED。
- FML_TARGET_DISCOVERY、PREPLAN、Helper/Consumer闭包、Registry映射、Mod Bus桥接：本轮**NOT RUN / NOT IMPLEMENTED**，未进入Gate 1–7。
- 原生Forge Helper Fixture、NeoForge Registry C/T、官方RegisterEvent、运行时Bus归属与原生共存：**NOT RUN**。
- FnF新计划准入及新运行：**NOT RUN**。历史C3首失败仍为`FoodNFuel.<init> → ItemRegistries.<clinit> → NoClassDefFoundError: net/minecraftforge/registries/ForgeRegistries`，本轮未重新观察该异常。
- C4A OVERALL继续**PARTIAL**；不改写为PASS。
- Food/Fuel/Tab、客户端与持久化：**NOT RUN**。没有实施4C。

按任务Gate 0.4原文“若集成 PR 尚未进入 main，本轮生产开发必须暂停”，到此停止生产推进。Gate 1的实际ModLauncher时序与整组预验证取证是合入main后的首项工作，不提前宣称可行。

## 本轮证据归档

`/workspace/DualForge-integration/out/convergence-007c4b-gate0/evidence-export/convergence-007c4b-gate0-evidence.zip`，136477字节，258项载荷加1项MANIFEST，SHA-256 **`879531b7767d98ca665b21c5e74f8089a56a34c3e0c1420e1445be5f6a23f3af`**。

范围为本次原始命令、环境、输出/错误、退出、结果、断言、构建指纹和串行检查脚本。排除生成class/JAR二进制、第三方依赖、世界、缓存及历史附件全集；不是云端目录完整副本。ZIP CRC、全新临时目录解压、逐文件大小/SHA、无路径穿越、文件计数及归档前后原件未变均PASS。

**ARCHIVE_READY / NOT PERSISTED**：未上传本轮证据，没有远端下载URL。此前WebDAV上传取消继续有效。最终PR/HEAD/CI元数据在归档生成之后单独记录，不声称已经收入该ZIP。详细清单见[证据索引](<Convergence-007C-4B Git基线证据索引.json>)。

## Git和后续交接

提交保留标准非快进合并；报告单独docs提交。分支将推送并核对本地/远端/PR HEAD，最终CI链接与状态以PR实时检查和`out/convergence-007c4b-gate0/git-finalization.json`为准。没有amend/rebase/force push、Release或自动合并。

当前唯一前置阻塞是本集成PR尚未进入main。4B完成标准尚未验收，不能据此对4C给出生产实施成功交接；合并后按用户原任务从目标发现和预验证开始，保持FnF Tab/全局Bus/67 Supplier及3个Tab Bootstrap的未验证状态。
