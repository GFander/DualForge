# 009C Windows / PCL 唯一人工验收清单

本轮自动化覆盖静态 ABI、严格准入、真实事件总线的隔离 API 和服务端回归。
**客户端启动、鼠标操作、配置 UI 和人工验收均为 NOT_RUN。**
无需协助 Ubuntu 编译、写脚本、服务端取证或创建 PR。

## 两个独立实例

| 条件 | A 原生基线 | B 兼容对照 |
|---|---|---|
| Minecraft | 1.21.1 | 1.21.1 |
| 加载器 | Forge 52.1.0 | NeoForge 21.1.256 |
| Java | 21 | 21 |
| mods | 原始 Mouse Tweaks Forge 2.26 | 同一原始 Mouse Tweaks + 本包 dualforge.jar |

使用独立新世界和相同配置/测试步骤，先 A 后 B。B 初次不要混装 FnF、Clumps
或观察器；确认单模组后再做混装回归。安装只需两个原文件，无 JVM 参数、Agent、
预转换或 NeoForge 文件替换；实例中只保留一个 DualForge JAR。

本包不包含第三方模组。Mouse Tweaks 仍用你自己的原文件：
`MouseTweaks-forge-mc1.21-2.26.jar`，SHA-256
`750dd1806a9e67ba23be58403648293bc1e936a068ba6d8184aac88fc3e0bc62`。
DualForge 的准确摘要见本包 `SHA256SUMS` 与交付报告。

```powershell
Get-FileHash -Algorithm SHA256 '实例路径\mods\dualforge.jar'
Get-FileHash -Algorithm SHA256 '实例路径\mods\MouseTweaks-forge-mc1.21-2.26.jar'
```

## 按相同步骤分别记录 A / B

1. 正常启动、进入新世界，无崩溃。
2. 左键拖动整理：记录操作前后各槽物品及总数。
3. 右键拖动分配：核对分配方式、数量守恒和鼠标持有物品。
4. 滚轮移动：分别检查两方向及 Shift 等组合键，与 A 一致。
5. 检查普通 Shift 点击等原生操作，没有重复移动或意外取消。
6. 创造物品栏与各页签：拖动、滚动及原生滚动行为一致。
7. 箱子和其他容器：两边背包、空槽/非空槽及不同物品均检查。
8. 从模组列表打开 Mouse Tweaks 配置界面，修改 RMB/LMB/Wheel 等设置。
9. 正常保存退出并重启：设置保留且实际行为随配置变化。
10. 最后在 B 加入原始 FnF、Clumps：再次检查上述鼠标/配置操作，并抽检物品、
    食物/燃料、经验球拾取与 Mending；旧 JAR 的通过不替代新 JAR 的抽检。

每项填写 PASS / FAIL / NOT_RUN；启动成功不覆盖后续功能门禁。不要覆盖旧世界。

## 反馈

提供 A/B 各自的 MC、加载器、Java、实际 mods SHA、测试项和前后数量，保留
`logs/latest.log`、`logs/debug.log`、首次崩溃报告（如有）及实际配置文件变化。
失败时保留首个异常并标明实例/步骤；截图或短视频可补充鼠标操作证据。
无需账户、令牌、启动器登录资料或私人世界。收到实际反馈后才更新四个客户端门禁。
