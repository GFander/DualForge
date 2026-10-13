# Convergence-006A Beta 贴图更新

2026-10-14，用户授权只替换第二个物品 `multiregistryprobe:beta_item` 的贴图。
起点分支 `codex/convergence-006a-multi-item`，提交 `4fc5ed9de2056d0b913bdeb0f4cf5b70f17fb8df`；工作区干净，fetch 成功，更新现有 PR #9。

从用户上传的 `raw_ice_ether.png.zip` 仅读取 `raw_ice_ether.png`，不导入 macOS 元数据。
原字节 229 bytes，16×16 PNG，SHA-256 `4fce855f8822a589c8a0a1b83d61c422711710fbf69a52947358727b183dcf69`。
只替换 `tests/multi_registry/src/main/resources/assets/multiregistryprobe/textures/item/beta_item.png`；Alpha、原005贴图、所有Java源码和兼容层不变。

## 实际检查与制品

- PNG签名、所有chunk CRC、尺寸及原图字节一致性通过。
- 从已测原生Forge fixture生成单独资源更新版；全部JAR成员逐字节对照，唯一差异是Beta PNG，全部class字节、metadata、Alpha及其他资源保持不变。未重新编译或改写Java。
- 新Mac ZIP CRC及所有SHA256SUMS通过；内置兼容层与原已测包逐字节一致。
- `git diff --check`及修改路径核对通过。未声称重跑JVM、生产回归或Mac图形验收。

| 制品 | SHA-256 |
|---|---|
| 原已测Forge fixture，保持冻结 | `4099fb08fef19891eb95ca2fcce52300fb428f1faca4b562200dd081f562d904` |
| 新资源更新版Forge fixture | `9094b4f1d83f5a6355bfb4bf7ae775964a8067d8663c16a292c9685da2ededd9` |
| 新Mac ZIP，50580 bytes | `887f46c17df009722767ed7e00bb4d786e50f7d1dc13b7a3c0fb4ba303b2e7be` |

实际命令脚本、结果与新制品位于 `/workspace/DualForge-006A/out/convergence-006a/beta-texture-20261014/`：`command.py`、`result.json`、`multi-registry-probe.jar`、`dualforge-006a-mac-test-kit-beta-texture.zip`。
复核/重跑脚本需给新的输出目录；原验证目录及原完整证据保持不变，不能覆盖原输出。
新包manifest明确标记资源变体动态NOT RUN、客户端NOT RUN，并单列原class对应服务端PASS。

原006A-S受控PASS仍适用于原同SHA服务端cohort。新fixture的资源已静态核对，但其新JAR SHA未执行动态验证；新客户端仍NOT RUN。
原报告、原证据索引及冻结fixture不改写；新包仅本地保存，未上传、不自动合并、不进入006B。
