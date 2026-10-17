# 008A 验证矩阵

固定 MC1.21.1 / Forge52.1.0 / NeoForge21.1.256 / Microsoft Java21.0.10+7-LTS。
所有 JVM 串行。原始 JAR 不改字节，新实例/世界位于 SSD，实际命令和指纹随每组保存。

| 门禁 | PASS 判据 | 证据与边界 |
|---|---|---|
| 原始发行静态取证 | 15个ClassFile及元数据、Bootstrap、逐指令引用、Forge API、Mixin和服务声明完整记录 | `audit/static-analysis.json`及原始javap；官方源码只作参考 |
| 原生生命周期 | 原始Clumps加载、Done、正常stop/exit0、输入不变 | Forge空宿主与Clumps分别运行；不是玩家行为 |
| 首失败复现 | 单独DualForge+Clumps在基线准确失败于UNSUPPORTED_ENTRY | 完整stdout/stderr/debug和真实退出，不能仅看exit code |
| 新Profile | 原始/独立身份正例通过；异常Bootstrap、归属、字段、API、继承成员与动态访问拒绝 | `contract-final`；原ClassNode/JAR不变、禁用/dump失败单独诊断 |
| 标准Neo C/T | 同宿主、同最终兼容JAR、同启动文件/初始属性；只增加原始Clumps | `neo-clumps-control` / `neo-treatment-1`；不安装观察器或FnF |
| 真实经验球 | 初始3个，面值3/7/11，总经验21；400实际世界tick后，空宿主仍3个，Clumps为1个且总经验21 | 官方summon/tick/data命令；只生成时指定Value，不写合并结果NBT |
| 事件API | 原生/Neo均验证取消返回、监听器实际调用一次及移除；Value/Repair修改7→19由同对象返回 | `forge-observed` / `neo-observed`，15项断言；合成null输入，不是实际玩家 |
| 独立身份 | 另一个modId/入口/字段/提供者，同原Forge夹具分别在两端通过事件观察 | `forge-independent` / `neo-independent`；不使用生产身份白名单 |
| Mixin | 两个原始Mixin实际应用到ExperienceOrb，真实合并和保存/读取回调生效 | 原Mixin/目标/资源不重写；Mending业务另验 |
| 经验球持久化 | 正常保存后物理复制本轮世界到新实例；UUID、Value、Count、clumpedMap和总经验一致 | `forge-reload` / `neo-reload`；源世界指纹不变，不代表玩家持久化 |
| 旧Profile/模块/metadata | 原Normal Item、Context、平台、Registry/Global契约和严格拒绝实际重跑 | 新Normal/Legacy/Block/Context编译cohort与原字节Probe/Helper/Global输入分开 |
| 旧动态回归 | 原Probe构造/register返回；原生Neo共存；Registry/Global夹具实际注册与handler一次 | 测量依赖、夹具环境参数明确保存，不冒充普通用户安装 |
| FnF回归 | 新JAR实际启动；67 ID/Holder、66 Tab、58 Food与本轮Forge一致；72个熔炉快照逐项相等 | 标准和观察组分开；两种代表燃料及列明负例，不是全部配方/燃料 |
| 玩家拾取/Mending | 真实连接玩家的XP、修复、取消行为与Forge对照 | 没有真实玩家时NOT_RUN；不能用合成API观察替代 |
| 客户端 | 同原始Clumps的Forge/Neo独立客户端实际反馈及安装SHA | 用户反馈前NOT_RUN，旧FnF截图/日志不迁移 |

原生基线先于Neo功能实验。最终生产制品改变后，受影响的Neo契约/运行必须重跑；同轮不加载DualForge的原生基线可在输入及测量源码指纹一致时复用，必须注明实际原运行路径。

最终逐门禁状态和失败保全见 [交付报告](../../../Dev%20Manuals/Convergence-008A%20Clumps%20交付报告.md) 及机器索引。完整历史002至007C动态矩阵、新JAR客户端FnF、玩家存档均无自动继承。
