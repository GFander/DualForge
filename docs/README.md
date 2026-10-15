# 功能导航

本页链接当前实现、测试及冻结报告，不复制报告，也不将旧实验能力升级为当前支持。
唯一生产源码在 `dualforge/src/main`。当前运行方式见 [根README](../README.md)、[UNIX说明](../tools/UNIX.md) 和 [测试入口](../tests/README.md)。

第三方输入的实际范围见 [COMPATIBILITY](COMPATIBILITY.md)。007C Food N' Fuel 的
[原生基线](<../Dev Manuals/Convergence-007C Food N Fuel 原生Forge基线.md>)、
[ABI/首失败](<../Dev Manuals/Convergence-007C 第三方Item ABI取证.md>)、
[PARTIAL交付](<../Dev Manuals/Convergence-007C 真实物品兼容交付报告.md>)及
[重跑入口](../tests/real_mods/food_n_fuel/README.md)分开记录，以上为冻结007C事实。007C-1新增 [TOML/身份](<../Dev Manuals/Convergence-007C-1 TOML解析与身份取证.md>)、[依赖语义](<../Dev Manuals/Convergence-007C-1 Forge依赖语义取证.md>)、[交付](<../Dev Manuals/Convergence-007C-1 交付报告.md>)和 [Contract](../tests/real_mods/food_n_fuel/metadata/README.md)：Parser/准入PASS、required forge实际拒绝、整体PARTIAL。

| 主题 | 当前实现与验证 | 历史/决策/证据 |
|---|---|---|
| Architecture / Loader | [LanguageLoader](../dualforge/src/main/java/io/github/gfander/dualforge/DualForgeLanguageLoader.java)、[TransformationService](../dualforge/src/main/java/io/github/gfander/dualforge/DualForgeTransformationService.java) | [开发手册](<../Dev Manuals/Forge → NeoForge 兼容层开发手册.md>)、[身份迁移报告](<../Dev Manuals/DualForge 命名统一迁移报告.md>) |
| Discovery / Metadata | [Locator](../dualforge/src/main/java/io/github/gfander/dualforge/ForgeModLocator.java)、[Parser](../dualforge/src/main/java/io/github/gfander/dualforge/ForgeModFileInfoParser.java)、[dependencies](../tests/dependencies/)、[discovery](../tests/discovery/) | [002](<../Dev Manuals/Convergence-002 交付报告.md>)、[003a](<../Dev Manuals/Convergence-003a 交付报告.md>) |
| Forge platform dependencies | [Resolver](../dualforge/src/main/java/io/github/gfander/dualforge/ForgePlatformDependencyResolver.java)、[007C-2测试](../tests/real_mods/food_n_fuel/dependency/README.md) | [FML取证](<../Dev Manuals/Convergence-007C-2 FML依赖验证链取证.md>)、[方案比较](<../Dev Manuals/Convergence-007C-2 平台兼容方案比较.md>)、[契约](<../Dev Manuals/Convergence-007C-2 Forge依赖契约与验证.md>)、[交付](<../Dev Manuals/Convergence-007C-2 交付报告.md>) |
| Entry Profile / constructor Context | [分类](../dualforge/src/main/java/io/github/gfander/dualforge/ForgeEntryProfile.java)、[Context profile](../dualforge/src/main/java/io/github/gfander/dualforge/ControlledForgeContextProfile.java)、[测试](../tests/entry_context/README.md) | [实际ABI](<../Dev Manuals/Convergence-007C-3 入口与构造器ABI取证.md>)、[A/B/C方案](<../Dev Manuals/Convergence-007C-3 方案比较.md>)、[契约](<../Dev Manuals/Convergence-007C-3 Profile与Context契约.md>)、[交付](<../Dev Manuals/Convergence-007C-3 交付报告.md>) |
| Transformer / Constructor ABI | [Transformer](../dualforge/src/main/java/io/github/gfander/dualforge/DualForgeEntryTransformer.java)、[identity](../tests/identity/) | [003b](<../Dev Manuals/Convergence-003b 交付报告.md>)、[006A ABI矩阵](<../Dev Manuals/Convergence-006A ABI 与字节码报告.md>) |
| EventBus Compatibility | [events](../tests/events/README.md)、Transformer的受控事件分支 | [004](<../Dev Manuals/Convergence-004 交付报告.md>)、[历史Spike-002-0n](<../Dev Manuals/Spike_002-0n.md>)、[历史订阅源码](../history/spike000/subscriber-002-0n/) |
| Registry Compatibility | [Item profile](../dualforge/src/main/java/io/github/gfander/dualforge/ControlledForgeItemProfile.java)、[registry](../tests/registry/README.md)、[multi_registry](../tests/multi_registry/README.md) | [005](<../Dev Manuals/Convergence-005 交付报告.md>)、[006A](<../Dev Manuals/Convergence-006A 交付报告.md>) |
| Client Acceptance | [最新对象输入](../tests/client_objects/README.md)、[006A人工输入](../tests/multi_registry/MAC-SETUP.md)、[005工具](../tests/registry/mac/README.md) | [006B-C人工PASS／本轮截图WAIVED](<../Dev Manuals/Convergence-006B-C macOS 客户端人工验收报告.md>)、[006A-C用户报告](<../Dev Manuals/Convergence-006A-C macOS 客户端人工验收报告.md>)、[Beta资源变体](<../Dev Manuals/Convergence-006A Beta 贴图更新.md>)、[原005-C](<../Dev Manuals/Convergence-005-C 客户端验收与脚本修复.md>) |
| Historical Spikes | [结构与路径映射](../STRUCTURE.md)、[early-spikes](../history/early-spikes/) | [Spike-000](<../Dev Manuals/Spike-000 实验报告.md>)、[001a](<../Dev Manuals/Spike_001a.md>)、[001b5](<../Dev Manuals/Spike_001b-5.md>) |
| Decisions / Evidence | [当前Git规则](../AGENTS.md)、[路径保护映射](../tests/project_identity/structure_relocation.json) | [冻结命名索引](<../Dev Manuals/DualForge 命名统一迁移索引.json>)、[006A证据](<../Dev Manuals/Convergence-006A 证据索引.json>)、[006A-C索引](<../Dev Manuals/Convergence-006A-C 客户端证据索引.json>) |

原始运行日志/制品/世界不入Git。报告及索引中的现场路径、旧命令和原SHA保持原文，需按报告提交SHA或当前精确迁移映射定位。历史 DEPRECATED/Gradle 不是当前入口。
006A-S原冻结fixture和006A-C Beta资源fixture身份分别保存；用户人工PASS不代表所有平台，也不代表Codex获得原始视觉附件。

运行环境本地复制见 [runtime迁移步骤](runtime.md)。

006B受控Block+BlockItem见 [profile](../dualforge/src/main/java/io/github/gfander/dualforge/ControlledForgeBlockProfile.java)、[block_registry](../tests/block_registry/README.md)、[Gate0](<../Dev Manuals/Convergence-006B Gate0 ABI与可行性报告.md>)及 [交付](<../Dev Manuals/Convergence-006B 交付报告.md>)。此前[客户端部署快照](<../Dev Manuals/Convergence-006B-C 客户端测试包部署说明.md>)保留原NOT RUN；当前006B-C为[用户Mac人工PASS](<../Dev Manuals/Convergence-006B-C macOS 客户端人工验收报告.md>)，本轮截图WAIVED，完整Mac日志/兼容层文件SHA未在Codex独立取得。

最新客户端已进一步统一实际Registry路径、Java声明与资源：见[client_objects](../tests/client_objects/README.md)和[实际对象交付](<../Dev Manuals/Ice Ether 实际对象命名统一交付.md>)。旧资源版及alpha/beta/sample身份保留为历史cohort。

用户正常安装见 [INSTALL](INSTALL.md)，007标准入口与配置验证见 [bootstrap](../tests/bootstrap/README.md)。默认安装方式与API兼容范围分别验收。

007B普通Forge注册、模块身份取证和Mac零参数包见 [dropin_items](../tests/dropin_items/README.md) 与 [本轮报告](<../Dev Manuals/Convergence-007B Drop-in Runtime 交付报告.md>)。第三方兼容FAIL与整体PARTIAL明确分开。
