# 007C 原生 Forge 客户端补充验收

此清单补齐纯 Forge 基线；不是 NeoForge + DualForge 兼容 PASS。
本轮 Linux 已验证注册表、Holder、FOOD component 和代表燃料熔炼，实际食用、图形及保存重进尚未验收。

1. 新建 Minecraft 1.21.1 + Forge 52.1.0 + Java 21 实例，使用启动器的标准配置。
2. 只安装官方 `fnf-forge-1.3-1.21.x.jar`。不安装 NeoForge、DualForge 或测试观察器。
3. 从[官方页面](https://www.curseforge.com/minecraft/mc-mods/food-n-fuel/files/5842396)取得文件。
   官方 CDN：[下载](https://mediafilez.forgecdn.net/files/5842/396/fnf-forge-1.3-1.21.x.jar)。
4. 使用 `shasum -a 256` 核对原 JAR：
   `bd415b04529c4ffc31e14757488f71dab6917e4560c61e236c3c4d0de6557958`，128,098 字节。
   不修改 Manifest、TOML、class 或资源。
5. 新建允许命令的测试世界，记录实例版本、JAR SHA、日期和原始日志。

| 项目 | 实际操作与需要记录的事实 | 当前状态 |
|---|---|---|
| 普通 Item | `/give @p fnf:logo 1`；背包、第一/第三人称手持、英文名称 FnF Logo | NOT RUN |
| 食物 | `/give @p fnf:carrot_bread 2`；生存模式且未吃饱，实际右键食用一次，记录数量、饥饿值、饱和度和夜视 | NOT RUN |
| 食物数值 | 食用前后可用 `/data get entity @p foodLevel` 和 `/data get entity @p foodSaturationLevel` 记录；原生 FOOD component 为 nutrition=6、saturation≈7.2、night vision 1200 ticks、probability=1 | 属性已实际查询；食用变化 NOT RUN |
| 燃料 | `/give @p fnf:paper_stack 1`、`/give @p fnf:bio_fuel 1`；用熔炉和圆石观察燃烧、消耗及产物 | Linux 熔炉子项 PASS；客户端操作 NOT RUN |
| 燃料范围 | paper_stack 初始 BurnTime=100，单个不足以完成 200-tick 普通配方；bio_fuel=3200，Linux 已实际产出石头 | 动态服务端证据；不要将其当作客户端显示 |
| 创造标签页 | Food n' Fuel 标签页、图标和实际物品列表；注册 ID 为 `fnf:fnf_item_tab` | Holder 已绑定；UI NOT RUN |
| 保存重进 | 将普通物品、食物及燃料存入箱子，退出并重进，检查 ID、数量和继续使用 | NOT RUN |

每项填写实际结果和证据来源。原发行包仅有英文语言资源；不添加翻译或替换贴图来验收它。
原生基线补齐之后才能推进生产兼容修改。当前 NeoForge + DualForge 会跳过该模组，不能把该组合的 Done 当作验收。
