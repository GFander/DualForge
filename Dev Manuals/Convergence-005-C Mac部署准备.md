# Convergence-005-C Mac 部署准备

结论：云端原制品复核、Mac kit 制作及脚本模拟验证完成。
**005-S 保持 PASS；005-C NOT RUN，等待用户执行 Mac 人工验收。**
没有访问/部署用户 Mac，没有实际 Mac 脚本执行、图形客户端启动、/give、
背包/手持/双语言渲染或截图。没有扩大生产 API/ABI、合并或进入 006。

## 基线与实际制品

起点和 PR #7 head 均为 `51e1603310ed225264f9e04cd33dbad232195b6f`，工作区
干净，已 fetch。沿用原任务分支 `codex/convergence-005-registry-item`；
工具源码提交 `69d2e3284537067a55f2018fc826c04519c79901`。PR 中的新提交
仅测试部署工具与当前说明，不修改生产 Java/资源、原 005 证据或冻结历史。

原两份 005-S 文件仍存在，实际核对 SHA、JAR CRC、模块/Forge metadata、
Forge-only class 引用和 PNG/model/en/zh 条目后直接复制，未构建新 cohort。
原生 Forge 编译纯度证据来自匹配该原文件的先前实际输入/源码/命令记录，
没有假称本次重编译或重跑生产服务端回归。

| 项目 | 实际 SHA-256 |
| --- | --- |
| compat (38,752 bytes) | `ed572ca9762e235e7d264355d35b3c9d3ab9ecf1da3f2c3234b1e28056aad752` |
| Forge RegistryProbe (4,518 bytes) | `312a4f697c1dc617ed48822c10179384b6559f4e6399db9210c15541c774132c` |
| 原始 16×16 RGBA PNG | `2c810e0c1cca3c451a536a4cf1d72448a5809f999933acc45ca01240315f1681` |
| Mac kit ZIP (54,342 bytes) | `7b2cc61a8a8101f2aded2560dcd6db8d1dde42150636f6aa8b4ec8e7067e81eb` |

ZIP 路径：
`/workspace/DualForge-005/out/convergence-005/mac-client-kit/delivery/dualforge-005-mac-client-kit.zip`。
同目录有外部 .sha256、result.json 和解包树。包内 manifest 记录 source
commit、production commit fc15b62、原 cohort、全部资源 SHA 和用途。
SHA256SUMS 绑定所有包内 payload（不含自身）；ZIP SHA 在外部避免递归。
新 source commit 是部署脚本版本，不冒充原生产源码提交。

## 包内内容和操作边界

11 个文件：两个 JAR、manifest.json、MAC-SETUP.md、JVM-ARGS.txt、
CHECKLIST.md、三个请求的 Mac zsh 入口、共享 mac-common.zsh 和 SHA256SUMS。
不含 Minecraft/Mojang 资源、账号、Linux JDK/native libraries、服务端
libraries、世界、测试 driver、凭据或测试机全局配置。

preflight 检查 Darwin、arm64/x86_64、macOS 版本、原 cohort SHA、Prism、
用户明确给出的实例、精确 MC 1.21.1/NeoForge 21.1.256、实际游戏目录、
实例 Java 21 架构及全部七项 JVM 参数。拒绝 Rosetta、混合/重复 loader、
不同 SHA 同名文件、其他 mods、symlink、冲突/重复受控属性。
Prism 常用 .minecraft，亦接受显式实例内唯一 minecraft；不猜测目录。
实例/Java 不满足时输出手动创建或设置建议，不自动安装/升级软件。

install 先要求 exact-instance/phase/cohort/mods 指纹 dry-run receipt。
dry-run 仅在 kit .state 写 receipt，实例不变。复制仅在该游戏 mods 目录，
不覆盖已有文件；同 SHA 预先存在的文件不算脚本所有。阶段 B 仅 compat，
C 加原 Forge JAR。回滚先全量核对所有 owned JAR SHA，发现改动则全部
拒绝，正常时只 unlink 本脚本新增的两个文件/ownership ledger；用户
世界、截图、日志及其他 mods 不动。无 sudo、rm -rf、全局 Java/安装或
账号/profile/Gatekeeper 修改。

源码/tools 的四项转换开关和 DF005_REGISTER=true 加两项 dump override
写入 JVM-ARGS。dump 用游戏目录内 Mac 有效的相对路径，没有照搬 Windows
默认值或 Linux 路径。不添加隔离 bytecode harness 的 add-opens/exports；
真实客户端如有新的模块/ABI 错误只保存首个错误，不擅自扩展兼容代码。

collector 由用户明确执行，只复制阶段 marker 之后的 latest/debug/crash、
Java/架构/版本、JAR 名/SHA、用户完成的清单副本及显式选择的 PNG 截图。
账号 token/password/session/cookie/key/登录用户行隐藏，URL/IP/连接地址
及用户提供的私有字符串脱敏，原件保留。REDACTION-NOTES 说明隐藏范围与
上下文损失，截图不改字节且需用户检查。只生成本地诊断 ZIP，不上传。

## 本轮实际验证与限制

- 五份 zsh 文件语法 PASS。
- 九项 Linux Zsh 行为测试 PASS，**Mac/Prism/Java 元数据为模拟值**：平台
  拒绝、带空格路径、B/C dry-run/安装、原用户 JAR 保留、世界保护、修改
  owned 文件后原子拒绝回滚、其他模组/错版本/错 Java/缺参数/冲突属性/
  symlink/kit 篡改拒绝以及诊断脱敏。没有把模拟执行称为 Mac 实机 PASS。
- 最终 ZIP 实际 CRC、11 文件白名单内容、包内 SHA256SUMS、原 JAR SHA
  和资源 SHA 核对 PASS；初始 tracked 快照除三个授权当前 README/结构
  文档外不变，原生产与冻结历史 diff 为空。
- 原 005-S 服务端已测制品未变；本次不重跑生产回归，保留此前实际 PASS。
- 真正 Mac Java/Prism/脚本与图形客户端全部 NOT RUN；没有真实截图。

原始 mock 命令/stdout/stderr 在
`out/convergence-005/mac-client-kit/tests/d7be48f6/commands.json`，声明 MacExecuted=false。
新证据索引 [Mac部署证据索引.json](<Convergence-005-C Mac部署证据索引.json>)
绑定本轮实际路径、SHA、源码提交和验证/交付状态，不改写旧冻结索引。

## 文件传输和下一步

**ZIP 仅保存在云端工作区；未上传，没有已验证的外部下载 URL。** 当前
工具没有通用文件上传/附件接口，也没有用户指定的受控存储目的地；
不假设 Gist 权限，不发公开 Release/网盘，不打开公网 SSH 或访问用户 Mac。
若 Codex 应用能导出该工作区文件，用户取出 ZIP 后先核对上表 SHA；若
不能导出，传输仍是 STOP/阻塞，需用户指定可授权的受控方式。
这一传输阻塞不是客户端兼容 FAIL，也不改变 005-S PASS 或 005-C NOT RUN。

Mac 用户按 [MAC-SETUP.md](../tests/registry/mac/MAC-SETUP.md) 手动创建原生
Prism/Java 精确版本实例，配置 JVM 参数，依次执行 A 纯 NeoForge、B compat、
C 两 JAR，保留阶段日志与真实截图，再检查本地诊断包后决定回传。
只有真实加载、/give、背包/手持/双语言/无缺失资源均满足且证据充分，
才能裁定 005-C PASS。完成云端准备后停止，等待这些人工证据。
