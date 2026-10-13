# Convergence-005-C 客户端验收与 Mac kit 脚本修复

结论：两个部署脚本 bug 已修复，13 项 Linux 模拟回归和 ZIP 校验 PASS。
原 005-S 保持 PASS。用户回传的真实 Mac 日志、截图及人工确认支持
**005-C 在 macOS 27.0 arm64、Microsoft Java 21.0.7+6、原两 JAR cohort 上
受控人工验收 PASS**。不是云端自动图形测试，也不覆盖 Intel 客户端。
本轮修订脚本尚未在用户 Mac 实机复测，不把客户端 PASS 等同新脚本实机 PASS。

## 起点与保护范围

起点/PR #7 head：`bbc14d16ae09fe32e5c74c4c20faeb48c07e1a92`；同一分支
`codex/convergence-005-registry-item`，初始干净，已 fetch/核对远端状态。
修复提交 `d48ca2a714d5bf858bca3a165e6c03350821d5cf`。文档提交见 PR。
仅修改 Mac 测试部署脚本、模拟用例、包 metadata 和当前文档；原生产
Java/resources、旧冻结报告、原包/证据和 JAR 均不变。保护快照中授权
文件之外 414 个 tracked 输入 SHA 未变，production/history diff 为空。

## 两个 bug 的复现及修复

1. `plutil -lint` 在用户系统按 plist 校验，拒绝合法 Prism JSON。旧 mock
   错误地让该调用成功，未反映 Mac 现场。现在用 `plutil -p` 解析 JSON，
   后续仍按 -extract 读取 UID/version；不依赖 Mac Python，不降低版本约束。
   mock 明确拒绝 JSON 的 -lint、接受 -p/-extract，畸形 JSON 仍被拒绝。
2. Qt INI 对整串 JvmArgs 加外部双引号时，旧 `(z)` 将七项视为一个 token。
   现在先判断是否是完整单 token 外部 wrapper，解码**一层**后再分词；
   多空格、内部 escaped quotes 和单独参数引号分组保留。无 eval，参数
   文本不执行；缺失、同值重复或 true/false 冲突仍严格拒绝。

修改前新增两个用例分别真实复现 `Invalid mmc-pack.json` 和
`Conflicting JVM property: -Dforgecompat.2a.rewrite`，原始失败输出保存于
`out/convergence-005/mac-client-kit/fix-prism-format/reproduce.*.log`。
修复后完整 13 项模拟用例 PASS。扩展测试第一次有一段 Intel 测试尾部
误放入新测试，出现 NameError；已纠正，失败记录未覆盖，最终 13 项重跑
通过。语法检查 5 个 zsh 文件通过；模拟结果不冒称 Mac 实机执行。

## 附带诊断与回滚建议

- 清单副本移入 `checklist/CHECKLIST.md`，REDACTION-NOTES 明确其模板/
  人工文字不是运行日志证据。应只在 log-*.log 按 logger/message 判断，
  不对整个 ZIP grep 后把关键词命中算作加载。
- collector 对实例完整路径作 literal 替换为 `<INSTANCE>`；模板、目录
  名 DualForge 和路径不再当作运行 marker。原件不变，不自动上传。
- 新 `.dualforge-005-owned` 每行记录文件名与原 cohort SHA。旧 filename-only
  格式继续兼容，仍按当前固定 kit SHA 核验；错 SHA 收据在任何删除前
  全量拒绝。原同名/其他模组保护和 changed-JAR 原子回滚断言保留。

## 用户真实 A/B/C 证据评审

上传 `dualforge_client_test.zip` 包含三阶段脱敏 latest/debug、环境、已安装
JAR SHA、Java 版本、空白 checklist、C 的两张真实截图。没有 scripts/.orig
或 instance.cfg，临时脚本修复内容以用户消息为依据，未执行附件中代码。
原 ZIP 和解包文件保存于 ignored out，不将日志/截图/个人路径上传 Git。

| 阶段 | 实际证据与边界 |
| --- | --- |
| A | 无测试 JAR；真实 NeoForge 21.1.256 / MC 1.21.1 客户端日志，无 ERROR/FATAL |
| B | 仅 compat，SHA 与原 005-S 相同；debug 有 ModLauncher `Loaded service forgecompat-transform`，无 ERROR/FATAL |
| C | 原两 JAR SHA；FML `Found valid mod file registry-probe.jar with {registryprobe}`、受控 Forge Locator 和对 `RegistryProbe` 的实际 transformer target；集成服务端进入/正常保存/停止，无 ERROR/FATAL |
| 截图 | 真实背包/快捷栏蓝色图标及手持模型，未见紫黑缺失纹理；第一张 hover 的是云杉活板门，不是测试物品名称 |
| 人工确认 | 用户明确回复“三项都已实际检查通过”：精确 `/give @p registryprobe:ice_ether 1`、英文 DualForge Test Chip、中文 DualForge 测试芯片 |

截图支持真实物品显示；/give ID 与两个名称是**用户人工确认**，不声称
截图显示了这些名称或日志含有对应命令。清单均未勾选，未把模板当成
完成记录。当前 latest/debug 没有 System.out 的 FORGECOMPAT/DF005 marker；
生产 log() 使用 System.out，Prism 控制台未回传。缺少 println marker
不覆盖真实 FML/ModLauncher 服务、metadata、target 和实际渲染证据。

受控 PASS 的依托是相同 JAR 身份、真实客户端加载/世界/渲染、无新增严重
错误和用户明确的剩余人工项确认，不能概括为全部 Forge 客户端兼容。
没有 Intel、其他模组/构造器、Registry/API/事件扩大验证，不启动 006。

## 新版 kit 与验证

两份平台无关 JAR 原文件直接复用，未重编译、未重跑生产回归：

- compat：`ed572ca9762e235e7d264355d35b3c9d3ab9ecf1da3f2c3234b1e28056aad752`。
- Forge fixture：`312a4f697c1dc617ed48822c10179384b6559f4e6399db9210c15541c774132c`。

新 kit 55,808 字节、11 个文件，路径：
`/workspace/DualForge-005/out/convergence-005/mac-client-kit/fix-prism-format/delivery/dualforge-005-mac-client-kit.zip`。
SHA-256 `f2a01c9693363af041c8a870390f9651352e0ea863be99a1cebe1aae05d95dae`。
实际 ZIP CRC、包内 SHA256SUMS、两 JAR 及四个资源 SHA 校验通过。
manifest 绑定修复源码 commit、已受控人工验收的 Mac/JAR 范围和
repairedScriptsMacExecuted=false，不把原人工 PASS 冒称新脚本实机复测。

新 kit、源脚本、诊断元数据及原上传材料指纹见
[客户端验收与脚本修复索引](<Convergence-005-C 客户端验收与脚本修复索引.json>)。
此前部署准备报告及包保留原状态，作为当时现场，不改写历史。

新 ZIP 仍仅云端工作区保存，未上传，无已验证外部下载 URL。没有创建
公开 Release/Gist/网盘，没有连接或写入用户 Mac。新版解压到新目录，
保留旧版临时改动/.orig/日志；先 preflight、dry-run 和 collection 验证，
已有原 JAR 不需为了脚本修复而重装或重跑游戏。新的脚本实机若失败，
保存实际错误继续本范围排查。PR #7 已更新，仍不自动合并。
