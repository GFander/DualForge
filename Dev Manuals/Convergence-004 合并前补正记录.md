# Convergence-004 / PR #6 合并前最小补正

## 基线、范围与结论

本轮起点 `6f3283e42783380011af4a953822e8697cf4c0aa`，沿用
`codex/convergence-004-real-event-dispatch` 更新 [PR #6](https://github.com/GFander/DualForge/pull/6)。
工作区干净，fetch成功。未改写历史，未新建PR，未合并，未进入005。

只修改 events runner、它的自动负例、STRUCTURE.md、events README 和本记录。
生产Java、资源、fixture Java、共享harness及历史冻结文件不修改。

**原完整004受控功能PASS仍成立。** 新增一致性判据已用原始四阶段及九个回归子项
输入指纹独立复核；全部compat SHA-256为
`60eaf67e84886ba260f5b81cc7330019d63b6ec9262bbd2255093a6d918d9559`。
这次是原证据的元数据复核，不是新的动态PASS；没有生产构建或JVM回归运行。
原报告与原证据索引保持原字节。

## 修复与实际验证

旧publish_result只比较C/T的artifacts.json，Contract或Regression换轮次仍可能整体PASS。
新增自动测试先在旧代码上复现该漏洞：7项测试中出现8个失败断言（含四个阶段的子用例）。
修复后7项测试全部通过：同构建正例、四阶段分别不一致、缺失旧schema指纹、
C/T共同使用另一构建、C/T制品与结果指纹不一致、缺失阶段，以及回归子项不一致。

Contract和C/T结果中的compat字段来自执行前的真实inputs记录，原unchanged检查保留。
Regression执行前记录compat，并逐项核对每个子runner的真实input记录和原文件指纹。
汇总输出stageCompatSha256、sameCompatAcrossStages和ctCompatEvidenceConsistent；
只有四阶段指纹完整、一致且与C/T实际制品一致时才允许整体PASS。

命令：

```zsh
python3 -B -m unittest discover -s tests/events -p test_stage_consistency.py -v
```

本轮附件根目录：`/workspace/DualForge-004/out/convergence-004-premerge/`。
`before-tests.log`保存修复前失败；`after-tests.log`保存修复后结果。
`replay/`把原result复制到新目录并依据原inputs补充compat，用修复后的汇总器校验。
每份复制结果标明historical evidence metadata replay及原result路径／SHA，
没有更改原证据或将复制结果称作新服务端运行。
`replay/child-input-audit.json`核对五个回归runner和C/D/E/F四个动态子项。

## 生产字节与历史保全

`production-preservation.json`实测10个生产Java文件原字节一致；原被测JAR SHA不变，
12个已编译class与原jar-contents逐字节指纹一致。历史目录及原004报告、索引不变，
索引中的1278份原附件SHA仍匹配。本轮没有重新编译生产源码。
STRUCTURE.md只补充events活跃测试，原迁移映射表不改变。

## 最小C/T原始证据归档与可访问限制

已生成本地归档：
`out/convergence-004-premerge/convergence-004-ct-minimal.tar.gz`。

- SHA-256：`501a276a10ad5feb01c9812d37733f00b30519f83513b3796b2f63b2b3002f7d`
- 大小：34555字节；22个文件。
- 每组10份原始文件：stdout、stderr、latest、debug、command、meta、inputs、artifacts、assertions、result。
- 另含README和MANIFEST；原始文件逐字节复制，MANIFEST记录原路径、SHA和大小。
- 明确允许清单打包，没有世界文件、依赖文件、JAR/class、环境变量转储或凭据。
  私钥、GitHub token、Authorization及含用户名密码的URL扫描通过；内部工作区路径保留。
- 原C为control/20261012-153251-9b014d34，原T为treatment/20261012-153413-f38a4b25。

**尚未上传，不能从GitHub PR下载。** 最初自动审批审核因缺少对具体目的地和载荷的授权，
拒绝向GitHub Gist导出原始证据；没有绕过拒绝。

2026-10-13（Asia/Hong_Kong），用户明确批准将上述34555字节、22文件的同一归档
上传至其GitHub账户的unlisted Gist并在PR提供链接。重新核对归档SHA及base64载荷后，
上传请求通过自动审批，但GitHub API实际返回：

```text
POST /gists
HTTP 403
Resource not accessible by integration
```

当前GitHub集成凭据不具备创建Gist的权限。没有创建Gist，没有远端下载URL；
阻塞已从上传授权缺失变为GitHub凭据权限不足，不再重复请求用户批准。
请求结果及归档指纹保存在本轮附件的gist-upload-attempt.json与gist-upload-response.json。
没有改用其他外部目的地、发布Release或提交原始日志到Git。

本地归档仍可取得；完成外部分享需要具备Gist写权限的用户凭据或手动上传。
unlisted Gist不是私有访问控制，知道链接的人可以读取。
本次后续仅更新本记录和PR状态，没有修改代码或重新运行生产测试，004受控PASS结论不变。
