# Git 工作规范恢复调查

## 本轮基线和范围

2026-10-12 从 origin 成功获取远端状态，以最新 main
`d51e4d317e8c9b13089ced3ccf78a0235814ce97` 创建独立 worktree 和
`codex/restore-git-workflow` 分支。起点工作区干净，没有混入其他任务改动。
仅恢复根目录 AGENTS.md 和新增本调查记录；不修改生产功能、CI 或主分支保护，
不改写提交历史，不自动合并。

## 调查结论

没有发现从 main 删除完整六节规范的提交。完整规范未进入当前 main 的祖先链，
原因是 PR #2 的合并基线为旧任务分支，而不是 main。

| 记录 | 直接证据 |
|---|---|
| 短版规范 | `7f5c56b8da2ac9bff9d380769f7aa0c34a95a339` 引入八行协作约定 |
| 完整六节 | `97b82492ae0da81a35c16c11f5710174ae6ea514` 替换短版；父提交是上述 7f5c56b |
| PR #1 | 合并提交 `60895595dd4af226a4da9bfc3792c5516ed6879c`，第二父提交为 7f5c56b，仅带入短版 |
| PR #2 | 合并提交 `00db1c2c5bccbc70661a1c504c5680fd54180e24`，父提交为 7f5c56b、97b8249；GitHub baseRefName 为 `codex/convergence-003b-entry-identity` |
| 后续 main | PR #3 的 `4078888fb13958c61829c4041f832b88164a7c01` 继承短版；PR #4 的 `8cfa72cf4729792182cad90284f02963297f653b` 只追加结构与归档约定 |

PR #1 于 2026-10-12 11:33:06 UTC 合并；PR #2 于 11:33:40 UTC 合并。
PR #2 虽然标记 MERGED，但合并的是旧任务分支；不能将其表述为已合入 main。
当前 main 不包含 97b8249 或 00db1c2。比较这些不同分支的文件虽显示
完整规则与短版差异，但这不是某次 main 提交的删除补丁。

可复核链接：[PR #1](https://github.com/GFander/DualForge/pull/1)、
[PR #2](https://github.com/GFander/DualForge/pull/2)。

## 复核命令

```sh
git log --all --graph --oneline
git log --first-parent --format='%H %P %s' origin/main
git log --all --format='%H %P %s' -- AGENTS.md
git show 97b82492ae0da81a35c16c11f5710174ae6ea514:AGENTS.md
git show --no-patch --format='%H %P %s' 60895595dd4af226a4da9bfc3792c5516ed6879c
git fetch origin 00db1c2c5bccbc70661a1c504c5680fd54180e24
git show --no-patch --format='%H %P %s' 00db1c2c5bccbc70661a1c504c5680fd54180e24
git merge-base --is-ancestor 97b82492ae0da81a35c16c11f5710174ae6ea514 d51e4d317e8c9b13089ced3ccf78a0235814ce97
gh pr view 1 --repo GFander/DualForge --json baseRefName,headRefOid,mergeCommit,mergedAt,commits
gh pr view 2 --repo GFander/DualForge --json baseRefName,headRefOid,mergeCommit,mergedAt,commits
```

最后的祖先检查返回 1，表示完整规范提交不是本轮 main 基线的祖先。
GitHub 元数据与 Git 父提交均支持上述结论；不推断未记录的操作动机。

## 恢复与条款核对

AGENTS.md 的 Git 规范段逐字节恢复自 97b8249，六节全部保留：
开始任务、控制修改和提交范围、提交要求、验证和证据、推送和 PR、交付。
当前 main 的“仓库结构与历史归档”段也逐字节保留，不用短版重复覆盖完整规则。

本轮实际静态核对：原规范前缀一致、六节顺序一致、所有原条款一致、
结构段一致、完整 diff 和暂存区只包含这两份 Markdown 文档，
`git diff --check` 通过。记录提交前文件 SHA-256，提交后核对 Git blob 一致。
环境为 Linux、Git 与 Python 3；未运行生产构建或 JVM 回归，因本轮仅修复文档。
没有本轮生产制品，不将历史运行声称为当前验证。
