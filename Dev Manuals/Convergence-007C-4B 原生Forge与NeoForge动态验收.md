# Convergence-007C-4B 原生Forge与NeoForge动态验收

## 同一来源输入

原生 Forge fixture：`helper-probe.jar`，6119 bytes，SHA-256 `0fd2e66dd33623ff58d8eca931ec9ae24b76aabbb9fa1eee5af191e1c1c0d610`。它由保存的四份原生 Forge 源码干净编译，仅使用官方 Forge 52.1.0 安装依赖；没有 NeoForge / DualForge 编译输入。逐源码/class SHA、javac argv、全部依赖 SHA、JAR 成员、javap -p -v -s -c 已保存。

第一次构建的 Forge JAR 冻结保留。后续 Observer 修改只改变独立平台观察器；重新编译得到的 fixture 成员逐项相同，但 ZIP metadata 时间戳不同，因此没有将新 ZIP 替代原 JAR。Native / Control / Treatment 全部使用上述第一份原字节 JAR。

## 原生 Forge 实际基线

使用官方 Forge 52.1.0 Dedicated Server、stock run.sh / 官方参数文件；实际 Forge 原生 ModLauncher 版本为10.2.4，不能写作 NeoForge 宿主的11.0.5。

首次观察器严格错误地要求栈类为 DeferredRegister，实际 Forge 是 DeferredRegister$EventDispatcher，导致原生实验 FAIL。失败原件保留；只修正独立观察器后，原 Forge JAR 原字节运行通过：构造一次、两个 Item 各有真实 ID、两个 Supplier 各一次、Holder.get / Consumer / Registry key / 对象互异、官方 RegisterEvent 栈、Done / stop / exit0 / stderr空 / 无crash。

Native 结果：`out/convergence-007c4b/native-fixture-v2/result.json`；功能断言：`preflight/native-functional-result.json`。空宿主：`native-empty/result.json`。这是本轮实际运行，不继承 C3 或旧 Item 的 PASS。

## 最终 NeoForge cohort

生产源码：`24a66d3437898860183d84a3e63c069604c2f0a8`。最终测试源码：`2ff283086cee743f6ecd40cce87146237aad854c`。最终干净输出构建 JAR 为119083 bytes，SHA-256：

`13e1b35bdb21667e2e1093e2a1e8ae28044faf2b51dbd8fce094eefcfe1d3a3f`

交付副本 `delivery/DualForge.jar` 与 build/dualforge.jar 字节一致。正式两组安装同一 compat、同一原 Forge fixture、同一独立 NeoForge Observer，以及同一 native-bootstrap.jar。Observer SHA `f1c026b08138c61a1bb11234be0f2002d151b7f88d5144bf4f235372f551636a`。只切换 fixture 环境 `DF4B_REGISTER=false/true`。

| 核验 | Control | Treatment |
|---|---|---|
| 真实 FML/来源及唯一入口 | PASS | PASS |
| Targets先完整计划，再逐类定义/构造 | PASS | PASS |
| Entry / Helper / Consumer各转换一次 | PASS | PASS |
| 未引用Unused不被接管 | PASS | PASS |
| 构造次数 | 1 | 1 |
| 实际Bridge挂接 | 0 | 1 |
| alpha_item / beta_item存在 | 否 / 否 | 是 / 是 |
| 两个Supplier次数 | 0 / 0 | 1 / 1 |
| 实际官方RegisterEvent/DeferredRegister栈 | 无注册符合条件 | PASS |
| Registry/Holder/Consumer/key/对象身份 | 存在性负对照PASS | PASS |
| 独立原生容器Bus查询与Bridge | 容器Bus存在，无挂接 | identity=1811787596一致 |
| 内置javafml及原生模组共存 | PASS | PASS |
| Done、正常stop、JVM exit0、空stderr、无crash | PASS | PASS |

正式原始记录：`formal-control/`、`formal-treatment/`；独立断言与精确环境：对应 `*-invocation/`；同轮空NeoForge宿主：`neo-empty/`。严格 cohort 检查将 Contract / C / T compat 和 fixture SHA、C/T Observer及全部安装JAR SHA、唯一环境差异绑定，混用构建的自动负例必须拒绝。

## 日志与隔离限制

本轮 stock 空宿主已有 Settings 首次缺少 server.properties 和 Mojang public-key 网络错误。两组按实际 stdout/debug 日志的规范化类别及次数与同轮空宿主完全对照；无新增错误，**严格零 ERROR = false**。原始完整错误没有过滤删除。旧历史 harness 的网络适配及已有世界输入单独记录，不能将其叫作默认用户运行参数。

原生及NeoForge均使用新物理运行副本，串行运行，原 Forge输入/官方安装/原世界不回写。首次候选版本和测试接线失败保留，不能计入最终 PASS。修正后的最终 runner 增加实际安装SHA断言后，正式 C/T 已另起新目录重跑。

未运行：客户端图形、Food/Fuel/Tab、任意第三方兼容。独立白盒桥接负例与正式 stock C/T 分开，不因功能PASS声称安全沙箱或全JVM事务。

最终验证目录为 `/workspace/DualForge-007C4B-validation/out/`，干净detached源码提交2ff2830；原生Forge和空宿主基线仍保留在 `/workspace/DualForge-007C4B/out/`。空宿主没有compat，宿主与官方安装未变，基线复用明确标注。最终生产build实际位于主任务worktree的final2-build，原argv/源码SHA保留；验证worktree使用核对SHA的物理JAR副本，不伪造一次新构建。前版4d19414f…的完整运行日志原样保留，已从最终验收排除。
