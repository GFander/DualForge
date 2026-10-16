# Convergence-007C-4C-6 客户端人工验收摘要

**验收日期：** 2026-10-16  
**目标：** 在 NeoForge 21.1.256 上通过 DualForge 运行原始 Food N' Fuel 1.3  
**实机环境：** Windows 11 x86_64、Microsoft Java 21.0.7、RTX 5080 Laptop GPU

## 一、验收结果

| 验收项目 | 状态 |
|---|---|
| 客户端启动及 FnF 加载 | PASS |
| 创造模式物品栏与物品贴图 | PASS（人工抽检） |
| FnF 物品获取、背包与手持 | PASS（人工抽检） |
| 真实食用、饥饿恢复与效果 | PASS（人工抽检） |
| FnF 燃料实际使用 | PASS（人工抽检） |
| 退出游戏与物品持久化 | PASS（人工抽检） |
| 帧率异常 | RESOLVED（渲染距离差异） |

**六项人工功能测试全部通过。**

## 二、日志分析

本次提供的 `latest.log` 和 `debug.log` 记录了真实 NeoForge 客户端运行过程。

经核查：

- DualForge TransformationService 正常加载。
- 原始 FnF 由 DualForge Locator 发现。
- FML 创建 FnF 模组容器。
- FnF 客户端资源进入资源管理器。
- 玩家成功进入单人世界。
- 世界正常保存，客户端正常退出。
- 日志中未发现 ERROR 或 FATAL 级别记录。
- 共有七条非致命 WARN，其中包含 FnF 动画帧和贴图 mipmap 警告。

没有发现需要立即修改 DualForge 的致命客户端异常。

## 三、性能异常调查

最初观察到 NeoForge + DualForge + FnF 运行时约 50–90 FPS，低于原生 Forge 的约 120 FPS。

随后用户发现两套环境的区块渲染距离不同。调整设置后，帧率表现恢复正常。

因此本次没有确证 DualForge 性能回退，不需要进行针对性生产代码修复。

正式的受控性能基准仍为 NOT RUN。

## 四、测试覆盖边界

本轮已经证明代表性客户端功能可以实际使用，但尚未完整执行：

- 全部 66 个创造栏展示项的逐项视觉对照。
- 全部 58 种食品的实际食用行为。
- bowl、stick 等食用转换返还。
- 精确饥饿值、饱和度、效果 tick 对照。
- 独立 Dedicated Server 联机与重启持久化验收。

同时，当前 Windows 实际安装的两个 JAR 尚缺独立 SHA-256 记录。

## 五、最终判定

```text
FNF_CLIENT_MANUAL_SMOKE=PASS
FNF_NEO_CLIENT_BOOT=PASS
FNF_CLIENT_PERFORMANCE_OBSERVATION=RESOLVED_CONFIG_DIFFERENCE

4C6_CLIENT_PLAYER_BEHAVIOR=PARTIAL
FNF_FULL_COMPATIBILITY=NOT_ESTABLISHED

PRODUCTION_FIX_REQUIRED=NOT_ESTABLISHED
```

**结论：** DualForge 已在真实 Windows 客户端上完成 Food N' Fuel 的六项代表性功能验证。当前没有发现需要立即修复的兼容性错误，完整 4C-6 验收仍需进一步补证。
