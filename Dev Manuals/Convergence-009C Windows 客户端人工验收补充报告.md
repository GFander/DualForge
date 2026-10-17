# Convergence-009C Windows 客户端人工验收补充报告

记录日期：2026-10-18（Asia/Hong_Kong）。实际测试日期未提供。证据类型：**USER_REPORTED_MANUAL_PASS**。

本报告补充009C交付后的用户反馈，不改写原自动化报告、证据索引、MATRIX.md或CLIENT.md的历史NOT_RUN，也不声称Codex在Ubuntu重新运行了Windows客户端。

## 用户声明与范围

用户在《009C收敛 + 010A JEI任务书》中明确声明：“009C 客户端测试全部 PASS，包括此前 CLIENT.md 列出的全部项目。”

| 验收项 | 后续状态 | 证据 |
|---|---|---|
| A：MC1.21.1 / Forge52.1.0 / 原始Mouse Tweaks Forge2.26启动 | PASS | USER_REPORTED_MANUAL_PASS |
| B：MC1.21.1 / NeoForge21.1.256 / DualForge / 同一原始Mouse Tweaks启动 | PASS | USER_REPORTED_MANUAL_PASS |
| 左键、右键拖动，滚轮双方向及Shift组合操作 | PASS | USER_REPORTED_MANUAL_PASS |
| 箱子、熔炉、创造模式GUI及容器行为 | PASS | USER_REPORTED_MANUAL_PASS |
| 模组配置界面、修改、保存退出与重启持久化 | PASS | USER_REPORTED_MANUAL_PASS |
| FnF + Clumps + Mouse Tweaks混装及CLIENT.md列明的抽检 | PASS | USER_REPORTED_MANUAL_PASS |

对应四门禁：`MOUSE_CLIENT_BOOT`、`MOUSE_GUI_BEHAVIOR`、`MOUSE_CONFIG_GUI`、`MOUSE_CLIENT_MANUAL` 均更新为 **USER_REPORTED_MANUAL_PASS**。混装反馈覆盖原清单中的FnF食物/燃料、经验球拾取和Mending抽检；不扩展为完整玩家数据、所有配方或其他模组的未列明验收。

真实启动成功由用户报告；FML构造和Mixin路径没有本轮Codex独立客户端trace，不将静态链接或用户启动结果表述为独立Mixin应用取证。

## 对应交付的预期身份

| 对象 | 预期身份 / SHA-256 |
|---|---|
| 009C实现提交 | `8f1e9008b1ca83d024ab6ad1b06a9690ac3647f6` |
| 009C交付HEAD（补充前） | `5bf5b0bb6aadd54e6bac437aeee1e96f2f02d948` |
| dualforge.jar | `50b6ca25eecf2d08505b534df16a4af1fedf23816ca4ceb41cac09a509637db4` |
| DualForge-009C-client-kit.zip | `d79f203d76a5d5695fb71a7fc6ae1187141eaaaf4741d9b9525be72ca94ac8ca` |
| MouseTweaks-forge-mc1.21-2.26.jar | `750dd1806a9e67ba23be58403648293bc1e936a068ba6d8184aac88fc3e0bc62` |

以上是009C既有交付记录的预期身份，**不是用户Windows安装文件的本次独立哈希复核**。用户没有随此反馈提供全部新日志、截图、配置差异或实测槽位数值：`RAW_CLIENT_LOGS=NOT_PROVIDED`、`CLIENT_INSTALLED_SHA_INDEPENDENT_CHECK=NOT_RUN`、`CODEX_WINDOWS_REPRODUCTION=NOT_RUN`。这些证据缺口不否定用户已明确报告的人工PASS。

本次仅文档收敛，没有重建或改变生产JAR。后续010A若产生新JAR，不能继承本报告的Windows PASS。

## Git与历史保全

回读GitHub：PR [#33](https://github.com/GFander/DualForge/pull/33) 在本次补充前为OPEN/Draft，HEAD为上述5bf5b0b，CI SUCCESS；无正式review记录。本报告以docs-only commit追加原任务分支。PR保持Draft，等待正式审查；未获合并授权，不合并、不发布。

原[009C自动化交付报告](<Convergence-009C Mouse Tweaks 交付报告.md>)、机器索引及原[清单](../tests/real_mods/mouse_tweaks/CLIENT.md)/[矩阵](../tests/real_mods/mouse_tweaks/MATRIX.md)原字节保留。当前状态应结合本补充阅读，不能用历史NOT_RUN覆盖后续用户PASS，也不能把后续PASS冒充原交付当时已经运行。
