# DualForge 007：Mac 零参数人工验收

本包未完成Mac图形实测；状态 **NOT RUN**。不继承旧006B-C或旧实验配置的PASS。
包含同一受测DualForge JAR与未预转换的原生Forge双Item JAR，无Driver、依赖库、世界或测试配置。

1. 在Prism新建Minecraft1.21.1实例，安装官方NeoForge21.1.256，选择Java21；不要复制旧实验实例或旧JVM-ARGS.txt。
2. 将本包 `mods/` 的两个JAR复制到该实例的 `mods/`。保留启动器默认参数，不填写任何DualForge或DF00*业务参数。
3. 照常启动、进入主菜单、建立允许命令的单人世界，执行以下实际获取测试。

```text
/give @p dropinprobe:ice_ether 1
/give @p dropinprobe:raw_ice_ether 1
```

检查两物品图标不同、显示/手持正确。英文分别Ice Ether / Raw Ice Ether，中文分别冰之以太 / 冰之以太粗矿。
保存退出再进入世界，核对物品仍在、无注册/资源错误，并正常退出游戏。
本fixture只声明普通Item，不声明Block，不据此验证方块放置或一般Forge API。

验收时保存Prism实际Java版本与启动参数、本轮 `BUILD-IDENTITY.json` 的两个JAR SHA、latest/debug及退出状态，并填写CHECKLIST。
Prism模组/内容列表与实际Mod加载日志共同核对dropinprobe。DualForge是FML LIBRARY，不要求作为普通内容Mod出现在UI列表；SPI日志应显示dualforge-transform及受控入口转换。

可在实例mods目录执行 `shasum -a 256 DualForge.jar dropin-items.jar` 核对实际安装文件。该命令仅用于验收，不是游戏启动要求。
Linux同字节服务端已通过，不代替Mac图形验收。若失败，保留原JAR与原日志；不要增加参数、替换官方JAR或悄悄删掉模组后记PASS。
原始客户端日志可能含账号参数；发布副本前检查token/个人信息，保留原件并分别记录原件及脱敏副本SHA。
