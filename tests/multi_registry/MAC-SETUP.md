# 006A Mac 客户端人工门槛

本轮客户端 **NOT RUN**。旧 005-C 和旧命名 cohort 的结果不适用于本轮 SHA。
包只含新 dualforge.jar、原生 Forge 双 Item fixture、参数、清单及 SHA，不含 driver、世界、
账号、Java/MC/NeoForge/LWJGL 或依赖。客户端图形运行需要用户在真实 Mac 执行。

在原生 Prism 手动新建隔离实例 Minecraft 1.21.1 + NeoForge 21.1.256，配置同架构 Java21。
实例 mods 只放本包两个 JAR；不得与旧兼容层或其他 fixture 同装。不使用原 005 kit 安装脚本，
其固定 fixture/cohort 校验有不同用途。Prism 实例 Java JVM 参数使用本包 JVM-ARGS.txt，
无需修改全局 Java 或 Launcher 配置。启动前核验包 SHA256SUMS 和报告中的 ZIP SHA。

实际打开单人世界后逐项执行并人工记录：

- `/give @p multiregistryprobe:alpha_item 1`
- `/give @p multiregistryprobe:beta_item 1`
- 两个不同物品均可同时存在，背包与手持图标无缺贴图。
- 英文与中文名称分别对照本包 CHECKLIST.md；两物品不互相覆盖。
- 正常保存退出，无新的严重错误或 crash。保存真实日志和必要截图，人工检查隐私后再分享。

成功安装、主菜单、静态资源存在不等于完成客户端验收。复核真实 cohort 与完整人工证据后
才更新006A-C；未执行继续NOT RUN，不自动合并PR或进入006B。
回滚只手动移除这次新建隔离实例中核验 SHA 相同的两个 JAR；不要删除用户世界或其他文件。
