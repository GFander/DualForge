# 008A Windows 客户端安装与人工验收

本轮 Ubuntu 只验证了服务端与限定事件 API。新 JAR 的客户端启动、真实玩家拾取及 Mending 为 **NOT_RUN**。请分别保留原生 Forge 与兼容实例的独立目录、独立新世界，勿覆盖已有存档。

## 输入与安装

| 输入 | 要求 |
|---|---|
| Minecraft | 1.21.1 |
| 原生基线 | Forge 52.1.0，原始 Clumps Forge 19.0.0.1 |
| 兼容对照 | NeoForge 21.1.256，新 dualforge.jar，同一原始 Clumps JAR |
| Java | 21；本轮服务端实测 Microsoft 21.0.10+7-LTS |
| Clumps SHA-256 | `e1de425ddd6f2b5c195c37a008d9fbc2c00c32fe1e214d76c63ab35446d13c19` |
| 新 DualForge SHA-256 | `43bc109e6129c2a6e7c7b6c76f339710a660195e7a7970b25ef6bb3489a16426` |

先测试原生 Forge，再测试兼容实例。兼容实例的 mods 只安装上述两个 JAR，暂不混装 FnF、其他模组或测量观察器。保持相同游戏设置、难度、测试步骤；按正常启动方式运行，无需 DualForge JVM 参数、Agent 或预转换。

交付包只包含 DualForge；Clumps 使用你自己的同指纹原文件。`SHA256SUMS` 同时列出两个文件的期望摘要。

在 PowerShell 中核验实际安装文件：

```powershell
Get-FileHash -Algorithm SHA256 '实际实例路径\mods\dualforge.jar'
Get-FileHash -Algorithm SHA256 '实际实例路径\mods\Clumps-forge-1.21.1-19.0.0.1.jar'
```

## 手动清单

1. 进入主菜单及新单人世界，确认 Clumps 加载且无崩溃。
2. 在相同条件下产生相邻经验球，观察合并。可以使用本轮服务端已验证的三种初始面值 3、7、11；只在生成时指定 Value，不修改已有经验球的 NBT。
3. 以实际玩家拾取，比较前后经验。关闭或移除所有 Mending 装备先验证总经验增加，再单独测试修复；必要时用只读 `/data get entity @s XpTotal` 记录精确值。
4. 装备具有 Mending 的受损工具/装备，比较损坏值、剩余经验和经验球消耗，按相同流程与 Forge 对照。
5. 在尚未拾取经验球时正常保存退出并重新进入，确认经验球仍可拾取、总经验不丢失；另核对玩家 XP 和已修复装备的保存恢复。
6. 原生/兼容分别记录各项 PASS、FAIL 或 NOT_RUN，禁止用“能进主菜单”覆盖拾取、修复和保存门禁。

## 日志反馈

保留每个实例的 `logs/latest.log`、`logs/debug.log`、崩溃报告（如有），以及实际安装两个 JAR 的 SHA、MC/加载器/Java 版本、测试步骤和前后数值。截图或短视频可辅助证明玩家操作，但不能替代文件指纹。

不需要提供账号令牌、启动器账户文件或个人世界。失败时保留首次异常及失败实例，停止覆盖原日志；标明是原生还是兼容环境。收到实际反馈后才更新 `CLUMPS_CLIENT_BOOT`、`CLUMPS_CLIENT_MANUAL`、玩家拾取及 Mending 状态。
