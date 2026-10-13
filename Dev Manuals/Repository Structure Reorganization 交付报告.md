# 仓库结构重组交付报告

2026-10-14（Asia/Shanghai）。本轮仅目录与活跃路径重构，生产功能不变。
[E]本轮实际执行；[S]静态核验；[D]边界/决策。原始运行目录时间戳使用执行器UTC。

## 起点与目录迁移

[E] fetch成功；独立worktree `/workspace/DualForge-reorganization`，任务分支 `codex/repository-structure-reorganization`。
最新main起点 `40577b6866b49952769caf2951dfbb231900b4e9`，包含已合并PR #9。起点干净，没有连带其他worktree的改动。
实际动态验证源码提交 `2eac4f869d93e363b9b42a240780fb29927913f0`；最终报告提交只增加文档/索引，生产及被测工具字节核对相同。最终HEAD和PR见交付回复/PR分支。

| 原目录 | 当前目录 | tracked数量 |
|---|---|---|
| spike000/probe/ | history/early-spikes/forge-probe-000/ | 4 |
| spike001a/compat/ | history/early-spikes/discovery-001a/ | 18 |
| spike001b5/probe/ | history/early-spikes/modinfo-001b5/ | 1 |
| spike000/neoforge-server/config/ | runtime/neoforge-server/config/ | 3 |

[E/S] 实际26个文件（早期源码23+配置3），全部精确git mv，100%原字节rename，不整目录移动ignored数据。
逐文件旧/新路径和原SHA在 [structure_relocation.json](../tests/project_identity/structure_relocation.json)，Git blob及全部484个起点tracked文件SHA在out/reorganization/before.json。
根目录早期spike不再作为活跃入口；history/compat-*、history/spike000及全部冻结Dev Manuals不变。

## 活跃引用和导航

- tools/lib/unix_workflow.py：默认输入改为runtime/neoforge-server；preservation扫描dualforge/runtime/history/Dev Manuals，包含三个运行配置和early-spikes全部文件。
- tools/lib/prepare_unix.py：沿用REPO，编译库准备的默认输入改为runtime；只下载原八个固定库，功能不变。
- tests/identity/run_identity.py、run_scope.py、tests/events/run_events.py、tests/registry/run_registry.py：默认输入同步改为runtime，其他代码/判据不变。
- 必须显式输入的multi_registry/project_identity wrapper保留现有参数契约，显式 --server-input 仍可接受外部安装。
- tools/entry、tools/harness和CI调用位置不变；所有活跃代码旧路径引用仅剩精确保护规则及负例样本，不用于构建/运行。
- README、STRUCTURE、AGENTS、tools/UNIX、tests README、project_identity README同步；新增docs/README按8类功能导航和docs/runtime用户本地复制说明。
- .gitignore只允许runtime三个原tracked配置；其他安装输入忽略。LICENSE、.gitattributes、完整六节Git规范不变。

## Gate 1：保护与数据完整性 PASS

[E/S] 旧命名索引的374个原保护事实不改写。新映射必须完整覆盖26个授权项，一对一、预期路径、原SHA一致；未移动项仍要求原tracked路径存在。
验证保护项缺失、字节变更、重复目标、未授权位置、旧新同时tracked、漏映射、整张映射缺失、错误SHA、未跟踪目标、路径穿越及额外元数据；13项新增用例（2正例、11负例）全部通过。
旧命名索引的9条行豁免不变；不增加目录级豁免。映射JSON仅精确继承已经通过原索引+路径+SHA核验的oldPath/newPath值，其他文本照常命名扫描，额外键拒绝。

[E] 新worktree旧运行目录只有三个tracked配置，没有其他untracked/ignored文件。完整运行数据位于外部 `/workspace/dualforge-cloud/server-base`。
先建立131文件及完整目录清单/指纹，再逐文件保留式复制到runtime。三个目标配置与外部源完全相同，无覆盖冲突；其余文件为物理副本。
复制后及全部服务器运行后，新旧131个文件集合和SHA、空defaultconfigs、原spike000world世界/目录均匹配；源安装未删除或改写。
未把运行世界、JAR、第三方库、日志加入Git。

[D] 用户机器实际ignored数据不可访问，因此用户机器迁移NOT RUN；[本地迁移步骤](../docs/runtime.md)要求核验并保留源数据，不继承云端复制PASS。world名及level-name保持spike000world。

## Gate 2：静态与自动检查 PASS

[E] 65项当前Python单测全部PASS：常规37项（events7、registry6、project_identity15、multi_registry9），其中新增迁移保护13项；另当前Mac脚本15项、Legacy13项使用原已验收冻结输入在Linux mock执行。后28项不等于真实Mac验收或新JAR服务端回归，未伪造验证报告。Legacy要求的相对out输入用原制品物理复制且SHA匹配，历史源码原字节不动。
22个活跃zsh脚本逐一 `zsh -n` PASS；命名保护checker PASS、全部374保护文件匹配。
108个活跃文档链接核验存在，历史文档内部旧链接/命令保持原文；git diff --check PASS。
生产Java/SPI/Manifest/资源及全部Forge fixture源码/资源、历史Java/TOML/Gradle/指纹、原配置、原报告/索引、LICENSE均按起点SHA核对不变。

## Gate 3：本轮实际构建与动态回归 PASS

[E] 实际工具链 Microsoft JDK21.0.10+7-LTS、Python3.12.14、zsh5.9、git2.52.0，Linux x86_64；MC1.21.1、NeoForge21.1.256及原冻结Forge52.1.0输入不升级。
先执行server_preflight；首次因shell沙箱禁止socket创建而未运行，随后使用授权网络执行权限完成正常preflight，无绕过原隔离条件。
`zsh tools/entry/dualforge.zsh --mode build --output-root out/reorganization/build` 未传server-input，invocation直接记录新默认runtime路径。
新全量build PASS，11份Java；实际新JAR SHA：

`1b2729c6638c7f747d5121a8b624a80d1ac4aa4df83357f13ea5e6db5c176c9c`

[S] 47564 bytes，与已验收f03d8633…a3a1c比较：全部34个JAR成员内容逐字节一致，含所有class/Manifest/SPI/资源；整体SHA变化来自打包元数据。
仍实际使用本轮新JAR跑回归，未用历史PASS代替。

| 本轮实际运行 | 结果 |
|---|---|
| 新默认路径的preflight及全量build | PASS |
| 显式 --server-input runtime/neoforge-server 原Probe严格smoke | PASS |
| 006A原冻结fixture Contract/范围/拒绝/原子性 | PASS |
| 006A C：两个Item不存在、Supplier 0/0 | PASS |
| 006A T：真实两个Item、Supplier 1/1、holder/ID/对象、官方栈/FML Modbus | PASS |

[E] 本轮新compat SHA在Contract/C/T及嵌套runtime bytecode均相同；C/T compat/fixture/driver/环境相同，仅DF006_REGISTER业务开关变化。
原Probe SHA a774d2f…559ae8；006A原冻结fixture SHA4099fb08…62d904，driver SHA6699d17e…d41f7c。未重建、改名或替换原输入。
三个真实服务器均Done→正常stop→exit0，所有严格日志/制品/发现/加载断言通过，无ERROR/FATAL/dump FAIL/stderr/crash。
Contract故意注入的dump失败仍仅为既有负例，不混成真实服务器失败。
100次实际JVM命令（含版本/编译/javap/服务器）按command/exit/meta写入区间核对无重叠；最后无Java进程且25565可绑定并释放。
原始命令、stdout/stderr/debug、meta/exit、输入及dump指纹由 [本轮证据索引](Repository%20Structure%20Reorganization%20证据索引.json)定位。

[D] 本轮只重跑任务要求的原smoke和006A Contract/C/T；002–005完整历史重跑及新JAR Mac客户端NOT RUN，不虚构新增验证，也不把旧006A-C人工PASS绑定新整体JAR SHA。

## Gate 4：GitHub Actions

现有ci.yml原字节不改，自动执行37项Python测试、命名/历史保护、22份zsh语法及PR diff检查。
[E] PR #12 的实际Actions [run 38482107378](https://github.com/GFander/DualForge/actions/runs/38482107378) 在提交 `47011dd248a1a74e712082b11ea4abcb47f324fc` 全部PASS：命名/历史保护、37项常规Python单测、zsh语法、PR diff检查。完成时间2026-10-14 07:06:18（Asia/Shanghai）。本CI回写提交仅修改报告/索引，之后最终HEAD亦须等待同一workflow完成；最终run记录在PR正文/交付回复，不将前一提交CI冒称最终提交检查。

## 失败记录及完成边界

首次迁移后命名扫描拒绝新映射中的原包名；不是保护字节变化。新增精确已验证路径值处理后重新扫描PASS，未改旧索引或扩大行/目录豁免。
执行期两次路径批处理的前置断言拒绝了“REPO必须在本文件声明”及“显式必填wrapper应有默认值”的错误假设；在复制/启动前停止，修正为保留导入和必填参数契约。最终所有受影响文件已核验、独立提交，没有改写历史。

本轮原始附件在新out，完整运行数据保持external/ignored；用户本地迁移需按自身环境执行。
新布局、精确SHA映射、功能导航及旧报告均可追溯；不自动合并、不发布、不进入006B。
