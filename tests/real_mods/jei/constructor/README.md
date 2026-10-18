# 010C：有限构造器片段组合

本组把原始 Locale/Translator 初始化、010B 双 Bus/显式 Consumer 和接口返回布尔配置组成同一个预计划。生产范围是固定字节码形状，不以 JEI/夹具 modId、包名、类名或发行 SHA 准入。完整闭包通过前不发布任何 target，不执行业务。独立的本地变量布局检查只用于精确报告拒绝边界，始终不产生可执行计划。

原始 JEI 仍 **PARTIAL**。它的 Locale 初始化和 ServerConfig 子闭包可取证；NetworkHandler、Dist/ClientSafeRunner、完整客户端事件与动态弱引用订阅尚未准入。不能将通过的拒绝测试称为 JEI 启动 PASS。

## 输入与执行

源代码保持 SMB；JDK、SDK、JAR、日志、世界、配置、完整证据均在 `/home/albert/Env/DualForge`。所有 JVM 串行，游戏由原官方 `sh run.sh nogui` 启动，不增加 JVM 参数、Agent 或业务开关。`DF010B_CONFIG_ONLY` 沿用独立观察器的诊断选择，不被生产读取。

从仓库根目录运行，每个输出目录必须新建；以下路径只是示例，每次选择空的 SSD 目录：

```bash
python3 -B tests/real_mods/jei/constructor/collect_abi.py --output /home/albert/Env/DualForge/runs/convergence-010c/abi-new
python3 -B tests/real_mods/jei/constructor/verify_locale_api.py --output /home/albert/Env/DualForge/runs/convergence-010c/locale-api-new
python3 -B tests/real_mods/jei/constructor/build_fixture.py --output /home/albert/Env/DualForge/runs/convergence-010c/fixture-new
python3 -B tests/real_mods/jei/constructor/build_observer.py --output /home/albert/Env/DualForge/runs/convergence-010c/observer-new
python3 -B tests/real_mods/jei/constructor/run_contract.py --jar /absolute/SSD/dualforge.jar --fixtures /absolute/SSD/fixture-new --output /absolute/SSD/runs/contract-new
python3 -B tests/real_mods/jei/constructor/run_boundary.py --jar /absolute/SSD/dualforge.jar --fixture /absolute/SSD/fixture-new/localpha/localpha.jar --output /absolute/SSD/runs/boundary-new
python3 -B tests/real_mods/jei/constructor/run_live.py --jar /absolute/SSD/dualforge.jar --fixtures /absolute/SSD/fixture-new --observers /absolute/SSD/observer-new --output /absolute/SSD/runs/live-new
```

`build_locale_observer.py` 单独编译原生 Forge JEI 观察器；与未修改 JEI 一起交给 `tests/real_mods/clumps/run_server.py`。它对原 Supplier 的 Dedicated Server 调用故意触发原生 DistCleaner 拒绝，记录该错误并保留异常；该诊断 ERROR 不属于普通无错误启动断言。不可拿它作客户端行为测试。

`collect_abi.py` 逐类重新核对两个 JEI 发行各 923 个 class，保存原始调用图、Bootstrap/字段/方法、订阅器及完整 SDK javap。`verify_locale_api.py` 直接读取 ClassFile 方法声明，结果只算 STATIC_VERIFIED。

## 独立正例与观察范围

`localpha` 与 `locbeta` 是仅用 Forge SDK 编译的两个原始 Forge JAR，两平台始终使用相同 SHA。它们具有：

- 保留的 Locale.ROOT Supplier 与 Translator 初始化；同模块的可观察静态计数器。
- 分别属于真实 ModContainer 的 Mod Bus，以及同一个真实官方 Game Bus。
- 三个原有有限事件的 Class/Consumer 注册、路由、receiveCanceled 和注销。
- `ISettings` 三个 `()Z` 抽象方法；原 Builder/Supplier 延迟读值；SERVER 默认 true/true/false。

可选计数器仅支持同类 `private static int` 的精确 GETSTATIC/ICONST_1/IADD/PUTSTATIC，无外部方法调用或隐式初始化。它使运行时能断言构造/注册各一次，不把任意计数器业务放入准入范围。

`LocaleChecks` 记录真实构造数、注册数、Supplier 值、Translator 结果及模块隔离。观察器构造与业务构造可能并发，因此 BEFORE 是真实时间采样（0 或 1），不能假定观察器总先执行。最终值必须为 1。初始 ROOT→setter 的顺序另由完整 ClassFile 计划及原始类隔离模型验证。

`OriginalModels` 把原 JEI 的四个选定 class 以 `.bin` 诊断资源放入**外置观察器**；绝不改写原发行 JAR或提交第三方 class。自有 child loader 验证原 Translator 全局字段、重复 setter、异常对象及失败状态，和原 ServerConfig/IServerConfig 的真实方法。Neo 的配置组件先通过生产子校验器，再仅使用既有有限映射生成模型字节。原配置对象的 Supplier 与官方 spec 取值/改值/afterReload 均实际调用。

这些组件模型不是第二个 JEI ModContainer，没有执行原 JEI config 工厂注册。Neo 使用真实 sealed 接口允许的 `LoadedConfig`，只有已经纠正并确认正确的内存配置，path/modConfig 为 null；不执行 save/注册/事件。完整 owner/文件/重启结果来自两份独立 Forge 正例，不能由这个模型替代。

Forge SERVER 路径在 `world/serverconfig/`，Neo 当前宿主策略在 `config/`。正例逐个对照 native observer SERVER 文件位置。持久化表示各宿主真实策略下的加载和恢复，**不代表逐世界路径或同步策略等价**。

`build_original_config.py` 另建 `originalcfg` 集成夹具：四个自有 Forge 源文件仅编译一次，原发行 `ServerConfig` / `IServerConfig` 两个 class 原字节复制（逐类 SHA 核对）。原始 JEI JAR 不修改、不安装于该集成实验。该模组通过原配置工厂在自己的真实 ModContainer 注册 SERVER spec，`build_config_observer.py` / `run_config_live.py` / `run_config_restart.py` 验证原接口、真实 native BooleanValue、默认值、修改后业务读取和两次重启。两平台使用同一个原 Forge 业务 JAR。它是原配置组件在新容器中的集成证明，不能称为完整原 JEI 入口运行。

## 拒绝与历史保护

[矩阵](MATRIX.md) 对每个负例列出独立变异/证据位置。`ConstructorContract` 是字节码模型；`ConstructorBoundaryContract` 使用真实 Reader/scan/planner，但没有 GAME layer 或 GUI；`OriginalModels` 是真实宿主上的隔离组件。真实 JEI 首失败必须另用最终新 JAR 和原 JEI 标准 Neo 安装副本记录。

旧 010A/010B 脚本、报告、输入和 Windows PASS 原样保留。新 JAR 重跑旧双 Bus/配置、FnF/Clumps/Mouse 等矩阵；旧客户端人工 PASS 不继承。完整交付事实和选定原件见本轮报告与证据索引。
