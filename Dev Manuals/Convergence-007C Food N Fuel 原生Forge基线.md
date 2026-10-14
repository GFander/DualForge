# Convergence-007C：Food N' Fuel 原生 Forge 基线

2026-10-14（UTC / Asia/Shanghai 同日）。结论 **NATIVE_FORGE_BASELINE = PARTIAL**。
输入确为正式发行 JAR，原生加载、注册及代表燃料成功；未将 FOOD component 查询替代实际食用，也未将服务端结果替代客户端显示。

## 发行输入和来源

官方候选页面为 [CurseForge file 5842396](https://www.curseforge.com/minecraft/mc-mods/food-n-fuel/files/5842396)。本次网页 HTTP 403，但[官方 CDN](https://mediafilez.forgecdn.net/files/5842/396/fnf-forge-1.3-1.21.x.jar)实际 HTTP 200 下载成功，JAR CRC 通过。
`fnf-forge-1.3-1.21.x.jar`，128,098 字节，SHA-256 **`bd415b04529c4ffc31e14757488f71dab6917e4560c61e236c3c4d0de6557958`**。
原 JAR、每个安装副本及全部成员指纹保存；无重建、预转换或原字节修改，`ORIGINAL_FORGE_JAR = FROZEN`。

真实 metadata：modId `fnf`，version `1.3-1.21.x`，javafml `[0,)`，MIT；必需 `forge [0,)`、`minecraft [1.21,1.22)`，BOTH / NONE。没有嵌入 JAR 或声明的额外依赖。
Manifest 没有 Automatic-Module-Name / FMLModType；没有 module-info。JDK 推导 automatic 名 `fnf.forge`，原生 Forge 实际模块名 `fnf`。
原 TOML 含行尾注释及三引号 description（内容在一行），不是当前简单扫描器已支持的输入。

GitHub `1.21.x` 实际 HEAD `2d5c6435dceceb8dea3269e9054edb7338ba05c8`（2024-11-01，late commit），tags 为空。
CDN Last-Modified 为 2024-10-24，class ZIP timestamp 为 2024-10-25；时间戳不是官方发布日或精确源码证明。
公开源码标为 **SOURCE_REFERENCE**；五个真实 class 的 javap/ASM 标为 **RELEASE_BYTECODE**。没有证据将两者声明为精确可重建对应。

## 官方宿主与隔离

本次下载并实际执行 Forge 1.21.1-52.1.0 installer，SHA `f1b620f2879ad6a5bbe15daba4d8f81eab9f5e08004967604960b98996bdebc4`，安装 exit0。
Java 为 Microsoft OpenJDK 21.0.10+7-LTS；原生 Forge ModLauncher 10.2.4。NeoForge 比较宿主为 21.1.256 / FML4.0.45 / ModLauncher11.0.5。
只预置官方依赖缓存，未把 NeoForge / DualForge 放入 Forge 环境。安装时的云端网络文件单独记录，服务端运行未使用它。

使用物理副本、默认 run.sh/user_jvm_args/unix_args 和官方 Forge shim，真实 JVM 命令为：

```text
java @user_jvm_args.txt @libraries/net/minecraftforge/forge/1.21.1-52.1.0/unix_args.txt nogui
```

官方 run.sh 的 Java 版本检查也使用官方 shim；没有自制启动器或业务参数。全部 JVM 严格串行，新世界隔离，原输入及安装文件 SHA 保全。

## 实际动态矩阵

| 阶段 | 安装 | 实际证据 |
|---|---|---|
| F0-final | 空 mods | Done / stop / exit0；`fnf:logo` 为 Unknown item |
| F1 | 仅原始 Food N' Fuel | 官方 Forge metadata、FMLModContainer/class 加载；`forge mods` 列出 fnf；67 个 Item 参数均通过真实命令解析，不存在 ID 被拒绝；Done / save / stop / exit0 |
| F1-observed-final | 原 JAR + 独立 Forge 观察器 | 真实官方 RegisterEvent / GameData.postRegisterEvents 栈；67 个 Item 和 67 个 Holder，全部同对象；模块 fnf；58 个 FOOD component；8 个原自定义 FuelItem；标签 Holder `fnf:fnf_item_tab` 已绑定 |
| F1-furnace | 仅原始 Food N' Fuel | 原版命令设置隔离熔炉/配方输入并逐 tick 观察；真实燃烧、燃料消耗及石头产出；Done / save / stop / exit0 |

67 项全部来自实际 Registry，按类别为 1 个普通 Item `fnf:logo`、58 个食物 Item、8 个 FuelItem。完整运行对象和数值在 `F1-observed-final/server/fnf-observation.json`；不是用语言文件猜测注册表。
carrot_bread 的实际 FOOD component：nutrition=6、saturation≈7.2、night vision 1200 ticks、probability=1。该查询没有触发食用。
8 个燃料 API 返回：paper_stack 100，bio_fuel 3200，blazing_coal 6400，blazerod_bundle 10400，fire_core 14400，energized_fire_core 28800，soul_fire_core 32000，energized_soul_fire_core 64000。

熔炉实际 NBT 序列：paper_stack 初始 BurnTime 100 → 20 ticks 后 80 → 用尽 0，燃料槽已消耗；它不足以完成 200-tick 配方。
bio_fuel 初始 3200 → 200 ticks 后 3000，输入圆石 2 → 1，输出 stone 1，RecipesUsed 记录 minecraft:stone。没有 Driver 直接执行熔炼或创建测试物品。
此子项验证两种代表燃料，不声称全部八种均完成完整燃烧时长测试。

## 限制、错误和未执行项

玩家实际 `/give`、背包/手持模型、食用及饥饿/饱和度变化、效果触发、创造 UI、箱子物品保存重进均 **NOT RUN**。
检查原生 Forge 的 88 个实际运行 JAR，没有 FakePlayer / MockPlayer helper；计划的官方 FakePlayer 路径未实施，不新造玩家模拟来冒充现成可信环境。
人工补齐路径见 [原生客户端清单](../tests/real_mods/food_n_fuel/NATIVE-CLIENT-CHECKLIST.md)。没有为当前不兼容目标制作可运行的 NeoForge Mac 包。

原生空宿主含首次 server.properties 不存在和云端 Mojang 公钥请求拒绝两类 ERROR。
F1 / 正确观察器 / 熔炉组的 stdout、debug、latest 严重错误头及计数与 F0 一致；无新增阻断错误、stderr 或 crash。严格“零 ERROR”不是 PASS。

工具失败保留：初次 F0 缺少隔离复制的官方 shim；初次观察器使用不存在的旧 getFoodProperties API 导致诊断 crash，随后依据真实 DataComponentHolder.get(FOOD) 修正；初次熔炉设置 CookTimeTotal=0，未声称产出成功，正确组改为普通配方的 200 ticks 并等待 step 完成。
这些是工具/设置失败，与仅原 JAR 的 F1 分开，不归咎于第三方制品。初次观察器 wrapper 在 BrokenPipe 后未写原 result/launcher exit，明确保留该缺项，不回造历史结果。

所有原始命令、日志、退出/缺项、输入和 class/资源 SHA 位于 `/workspace/DualForge-007C/out/convergence-007c/`，按 [证据索引](<Convergence-007C 证据索引.json>)复核。它们不是永久远端下载链接。
