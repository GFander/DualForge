# Convergence-007C-4B 审计收尾

日期：2026-10-15 UTC。PR #24；起点 `31a4393d41e5ab44db86935dd921b9b94b7d73db`，main `6d7c0bf3643cbb2c0f211a083dba7a373f2e47aa` 已 fetch 并确认为祖先。开始工作区干净；起点 CI [38734563675](https://github.com/GFander/DualForge/actions/runs/38734563675) SUCCESS。

## 精确拒绝门禁

测试提交 `93d1b07a93a1cf2943a7c8eeee9dd048221a04ac`。只修改 tests/helper_registry 中的测试和 runner，不修改生产实现。

FnFRegistryAdmission 仅接受完整消息 `Registry helper rejected: SUPPLIER_BUSINESS_UNSUPPORTED`，其他 IllegalStateException、空消息、附加文本、包装异常均失败，意外接受也失败。Java 独立 Contract 验证 1 正例、6 负例；CI 无需游戏安装即可执行。Python runner 再检查唯一完整拒绝行及唯一 NOT RUN 收据；9 种损坏/冲突收据拒绝，不能凭退出码或宽泛 PASS 字样验收。

## 本轮实际验证

所有 JVM 严格串行；未启动任何 Minecraft 服务端或真实 FnF。Microsoft OpenJDK 21.0.10、Python 3.12.14、Linux x86_64。新 detached 验证 worktree 保留每个 argv、cwd、stdout/stderr、exit 和输入 SHA。完整命令及逐文件指纹见[审计索引](Convergence-007C-4B%20审计收尾索引.json)。

- Helper Contract：110 断言 / 35 负例 PASS；原始 FnF 只读准入精确拒绝 PASS。
- 新拒绝断言：Java 1 正例 / 6 负例 PASS；Python 收据正例及 9 种负例 PASS。
- 历史 Context：80 断言 / 22 负例 PASS；007B：13 负例 / 1 显式 JPMS 拒绝 PASS。
- 平台依赖、metadata Contract PASS；ASM verifier 10 断言 PASS。
- 七组 Python unittest 共 76 测试 PASS；374 项冻结保护 PASS。

重跑主命令：`python3 -B tests/helper_registry/run_contract.py --jar <frozen-dualforge.jar> --fixture <helper-probe.jar> --server-input <official-NeoForge-install> --jdk-home <Java21> --fnf <original-fnf.jar>`。独立 Java 门禁：`python3 -B tests/helper_registry/run_ci_contract.py --java-home <Java21> --libraries <official-install/libraries> --output <new-output>`。Python 门禁：`python3 -B -m unittest discover -s tests/helper_registry -p 'test_*.py' -v`。实际绝对路径参数和其余历史负例命令全部见索引 actualCommands。

同一冻结 compat SHA-256：`13e1b35bdb21667e2e1093e2a1e8ae28044faf2b51dbd8fce094eefcfe1d3a3f`。`git diff 24a66d3 HEAD -- dualforge/src/main` 为空；逐路径 Git blob 列表 SHA-256 为 `585954cb11dc1c66cc2a928e7aecba78e4b105a9effdc1d9b592ae43ddeb050d`，与冻结生产提交相同。未重新编译生产 JAR，不虚构新的生产动态回归。

## 原始证据持久化与远端复核

`ARCHIVE_PERSISTED=PASS`。原 ZIP 原字节保存于同仓库独立 `evidence/convergence-007c4b` 分支，归档提交 `cd283a7e7c4f934d1f7b3a2453af8b3216cb0979`；不进入 main/任务分支代码树、不创建 Release 或第二个 PR。该分支须保留，以维持下载可达性。

[原 ZIP 下载](https://raw.githubusercontent.com/GFander/DualForge/cd283a7e7c4f934d1f7b3a2453af8b3216cb0979/dualforge-007c4b-evidence.zip)；[远端 Manifest](https://raw.githubusercontent.com/GFander/DualForge/cd283a7e7c4f934d1f7b3a2453af8b3216cb0979/MANIFEST.json)。

- 大小：5269418 bytes。
- ZIP SHA-256：`ace523de09942c5e56600cf3d21e48f33a5ba28e8025ba6f820bd1218c14ab90`。
- Manifest SHA-256：`6779e8b1b0930f40d6781faa401c1c17f48b3e52219385bda99b4c186f442c79`。
- HTTP 200 独立下载，ZIP SHA/CRC、解压安全、3074 载荷文件逐项大小/SHA、无重复/额外文件均 PASS。核验时间 2026-10-15T12:52:20Z。
- 原件与 Manifest 全部吻合，归档操作未改变原文件。原 ZIP 未重建。安全扫描的三处邮箱模式命中均为 Netty 模块版本，不是个人邮箱；无凭据、私网 IP 或个人 home 路径命中。通用 /workspace 路径保留。
- 范围沿用原 Manifest：包括所选原始命令、日志、退出/结果、指纹及必要自有转换 class/测试 JAR。不是全部工作区副本；不含世界、第三方运行库、无关缓存。历史失败复现仍为失败，不改写为 PASS。
- 原交付报告/索引的 NOT PERSISTED 是当时状态，原字节保留；本补充记录其后发生的实际持久化。本轮新增 Contract 原始附件位于独立 audit-validation/out，未冒充原 ZIP 的内容。

## 最终边界

4B 受控 Helper Registry 的历史动态 PASS 保留，并以本轮实际只读/负例检查补强；没有重跑全部服务端历史回归。`FNF_RUNTIME=NOT RUN`；真实 FnF 不兼容，不扩大 Supplier 白名单，不实施 4C。C4A PARTIAL、客户端和未验证玩法 NOT RUN 等历史结论保持不变。

仅更新现有 PR #24，不自动合并。最终文档提交的 GitHub Actions 与远端 HEAD 在 PR 审计章节和交付回复中核验，避免把旧 HEAD 的 CI 结果用于新 HEAD。
