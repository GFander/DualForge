# Convergence-005-C 人工记录

状态：NOT RUN（用户实际执行后再填写；脚本成功不是客户端 PASS）。
Mac 架构／macOS／Java 版本：________
Prism 实例及 MC/NeoForge 精确版本：________
compat／Forge JAR SHA：________
每阶段使用同一实例、版本与 JVM 参数；先关闭客户端再改 mods。

## Client A：纯 NeoForge，mods 为空

- [ ] Minecraft 1.21.1 进入主菜单（人工）
- [ ] NeoForge 21.1.256 加载（日志／版本页）
- [ ] Java 21 与 Mac CPU 匹配（preflight／日志）
- [ ] 已保存阶段 A 的真实日志与版本记录

## Client B：仅 DualForge

- [ ] 进入主菜单（人工）
- [ ] DualForge SPI／LanguageLoader／Transformer 有真实日志
- [ ] 没有新增客户端加载异常
- [ ] 已保存阶段 B 日志与首个错误（若失败）

## Client C：DualForge + 原生 Forge RegistryProbe

- [ ] RegistryProbe 真实加载（日志）
- [ ] 单人隔离测试世界正常进入，允许作弊（人工）
- [ ] `/give @p registryprobe:ice_ether 1` 成功（人工／截图）
- [ ] 物品出现在背包（人工／截图）
- [ ] 原始 16×16 蓝色菱形贴图正常（人工／截图）
- [ ] 手持模型正确（人工／截图）
- [ ] 英文名称 DualForge Test Chip 正确（人工／截图）
- [ ] 简体中文名称 DualForge 测试芯片 正确（人工／截图）
- [ ] 无紫黑缺失纹理、missing model/texture 或严重资源错误
- [ ] 无新增客户端崩溃
- [ ] 已保存真实客户端日志
- [ ] 已保存真实背包／手持／双语言截图
- [ ] 已检查脱敏副本与截图，确认可回传且知道隐藏了哪些字段

填写本次人工结果 A/B/C：________
证据路径／文件 SHA：________
首个真实失败与分类（Discovery/FML/Class Loading/Bytecode/Registry/Client Setup/Resource Loading/Rendering）：________
人工观察、日志证据、静态检查请分别记录；不凭主菜单或脚本退出 0 填 PASS。
