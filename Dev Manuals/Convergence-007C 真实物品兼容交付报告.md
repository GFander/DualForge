# Convergence-007C：真实 Food N' Fuel 取证交付（PARTIAL）

本轮完成官方输入冻结、纯 Forge 对照、真实 Registry/Holder 与代表燃料取证、NeoForge 首次失败复现及同 JAR 回归。
**没有实现 Food N' Fuel 兼容；生产源码零修改。** 原生功能基线尚未达到强制 PASS 门槛，保留可独立复核的阶段进展，停止扩大生产范围。

## 实际基线和版本控制

执行时 latest main `32089726b2021b84b0af223583e2fe3926e975c0`，PR #14 HEAD `2ca2d20d8182fc41c08f9545eaed995be74d8121`，PR #15 HEAD `2ae4742597d2c45ec8b1fdf925812af059672fcd`，两 PR 均已 merged。
但 #15 合并到 #14 的任务分支，merge commit `ae1460b62ca1698f1a761befffa8cf58f2cbfb61` 未进入 main。实际 main 与 007B 的生产 tree 不同，不能宣称已从包含全部前置代码的 main 起步。

本轮从已核实 007B HEAD 建立独立 `/workspace/DualForge-007C`，分支 `codex/convergence-007c-real-forge-items`，PR base 为 `codex/convergence-007b-normal-forge-items`。
该依赖和 main 集成缺口明确保留，不自行合并前置分支、不将 007A/B 已有功能算作本轮新增。原 worktree 已有内容不覆盖；Git 身份沿用用户配置。

生产 clean build 源码提交 `2ab2c78`（完整 SHA 见索引），后续测试/文档提交与它的生产文件逐 SHA 相同。
本轮新 JAR **67,568 字节，SHA-256 `36d84c40e1a10eea7921ed31c967af197b7b43cabf65937d75f27132ec67ac44`**。
与 007B 最终 dbb70… JAR 成员集合及逐成员字节全部相同，差异仅在 ZIP 封装时间；不虚构生产 Java 修改。
历史回归仍实际使用本轮 36d84… JAR，未继承旧 JAR 的 PASS。

## 验收状态

| 字段 | 当前实际结果 |
|---|---|
| TARGET_MOD | Food N' Fuel Forge 1.3 / MC 1.21.1 |
| ORIGINAL_FORGE_JAR | 官方 CDN，FROZEN；128,098 字节；SHA `bd415b04529c4ffc31e14757488f71dab6917e4560c61e236c3c4d0de6557958` |
| NATIVE_FORGE_BASELINE | **PARTIAL**：原生加载、67 Item/Holder、58 FOOD component、8 燃料 API、两种代表燃料真实熔炉子项 PASS；食用/图形/重进 NOT RUN |
| DUALFORGE_DISCOVERY | **FAIL**：共享准入 guard 返回 false，宿主实际跳过原 JAR，L0 |
| CONSTRUCTOR_ABI | **NOT RUN**（NeoForge 没有到达 fnf 入口；只读静态 Contract 明确拒绝原 profile） |
| CROSS_CLASS_ITEM_REGISTRY | **NOT RUN**（NeoForge）；原生 Forge 实际成功子项单独保存 |
| ITEM_IDENTITY | **NOT RUN**（NeoForge）；原生 67 Holder 身份一致 |
| FOOD_FUNCTION | **NOT RUN**：未验证实际食用后的饥饿、饱和度、效果与数量 |
| FUEL_FUNCTION | **NOT RUN**（NeoForge）；原生代表燃料实际燃烧/消耗/熔炼子项 PASS |
| CREATIVE_TAB | **NOT RUN**（NeoForge/UI）；原生 Tab Holder 已绑定 |
| ZERO_CONFIGURATION | **FAIL**（目标两 JAR 的可用性）；真实官方零附加参数命令检查 PASS |
| LINUX_SERVER | **PARTIAL**：宿主生命周期 PASS，真实目标未加载 |
| MACOS_CLIENT | **NOT RUN**；无新 NeoForge 可运行包，不继承任何旧客户端 PASS |
| HISTORICAL_REGRESSION | **PASS**：本轮同一个 36d84… JAR；范围、原子性和原生共存分别复核 |
| CONVERGENCE_007C | **PARTIAL** |

## 原生事实、首次失败与静态差异

[原生报告](<Convergence-007C Food N Fuel 原生Forge基线.md>)记录正式发行制品、官方 Forge52.1.0 安装、空宿主和原 JAR 运行、完整物品及数值、熔炉 NBT 序列和未执行项。
[ABI报告](<Convergence-007C 第三方Item ABI取证.md>)区分 RELEASE_BYTECODE、SOURCE_REFERENCE、静态 profile 拒绝和 N2 实际首失败。

N2 使用仅 DualForge + 原 Food N' Fuel，官方 run.sh/默认 JVM 文件；实际无 DualForge、DF00*、Agent、特殊 cp/module-path 或网络补丁参数。
Reader 被调用，因原 TOML 三引号 guard 返回 null；宿主日志明确 `Skipping jar … cannot be loaded`，实际 `fnf:logo` 查询 Unknown item。
未到达 Forge metadata、Dependency、LanguageLoader fnf 入口、构造或转换。不能根据 Done/exit0 或静态 ABI 表推断后续运行成功/失败。

现有简单 TOML、52 下界准入政策、原单 bus ABI、单入口受控数据依赖与 API 集合均保留；没有生产身份白名单。
真实发行 class 还涉及无参 FMLJavaModLoadingContext、两 helper 的注册、Tab、Properties/效果、SRG 成员及 FuelItem；在缺少完整原生功能基线时不同时新建多个兼容子系统。

## 本轮实际回归与保全

固定只读 worktree `/workspace/DualForge-007C-validation`，源码 `27a0c0e9e3af9d1b49f4347b2837472c3e958d9b`。
原 Probe smoke、002 metadata/CDEF、003a discovery/边界、003b identity/scope、004 Event C/T、005 Registry C/T/bytecode/scope/atomicity/assets、006A 与 006B Contract/C/T 全部实际 PASS。
总结果 `out/identity-unification/verification/20261014-134559-2607bad7/result.json`；006A/B stage-results 指向本轮结果，compat SHA 均为 36d84…。
原 C/T 开关、具体 FML 拒绝原因和既有严格断言不放宽；历史 harness 的云端网络/relay/显式参数与用户零配置证据分开。

007A Bootstrap Contract、007B Normal Contract（13 个拒绝/字节保全 + 显式 JPMS 拒绝）、新真实发行边界 Contract 实际 PASS。
标准 NeoForge 原生模组与普通 Forge 双 Item 的独立查询验证同 JAR，真实两个 Registry/Holder/对象身份、原生构造/ServerStarted 和内置委托。
Clumps 对应生产代码没改，不重新适配其历史边界或 XP 功能。

Python 原有 44 个单测、25 个 zsh 语法检查、新 Python AST、生产成员比较和 374 项冻结保护通过；最终提交另做 diff/导航核对。
runner 的真实 required 依赖缺失负例明确按 `df007c_absent` 的 FML 拒绝判断；原生 JVM 可以 exit0，runner 仍凭真实失败结果返回1。Done/exitCode 不被当作功能判据。
共享 baseline schema 首次共存汇总缺字段导致工具失败，原日志保留；修复新 runner 的命令身份字段后，以新的空宿主基线与新共存运行复核，不回改先前结果。

所有 JVM 串行、物理副本、原第三方 JAR/历史输入/世界/冻结报告不修改。新 runner 只清理自己已逐 SHA 保全的重复依赖副本，保留原日志、结果、mods、世界；历史运行库和原安装不删除。

## 修改范围、证据与停止边界

新增 `tests/real_mods/food_n_fuel/`：原发行身份/成员指纹、只读 ASM 与边界 Contract、可选原生查询观察器、标准宿主 runner、构建辅助与重跑/人工清单。
更新 README、STRUCTURE、测试/文档导航、AGENTS 附加规则和 COMPATIBILITY；保留完整六节 Git 规范。
生产 `dualforge/src/main/`、历史冻结源码/报告/输入、服务端原配置、CI workflow **零修改**。

[证据索引](<Convergence-007C 证据索引.json>)保存实际命令、工具链、制品/源/字节 SHA、各阶段和原始路径；最小附件的数量、SHA、裁剪/脱敏及本地/远端状态独立记录。
本地 out 路径不等于持久化 URL。新附件尚未上传、下载及远端复核，状态 **ARCHIVE_READY / NOT PERSISTED**；不创建 Release 或借用旧任务的上传授权。

剩余阻塞是原生实际食用、客户端普通物品/Tab显示及重进验证；随后仍需独立 metadata/依赖与后续 ABI 契约。
提供 [原生客户端清单](../tests/real_mods/food_n_fuel/NATIVE-CLIENT-CHECKLIST.md)，未生成声称已可用的 NeoForge Food N' Fuel 包。
PR 为 Draft，未合并。最终 HEAD、远端 SHA 和实际 GitHub Actions 记录在 PR 正文；不改写历史、不合并、不创建 Release、不进入 Clumps 或其他兼容阶段。

最小归档为 `out/convergence-007c/evidence-export/dualforge-007c-minimal-evidence.zip`，488,764字节，186个ZIP文件：183个payload（含1个明确标记的派生最终汇总）加Manifest/清单/说明。SHA-256 `f78da8695dae21b315c50fa2c752f4610fd785adf0f87cc049b33a04062c6063`。CRC、逐Manifest字节/SHA、安全路径、原文件未变及凭据模式扫描PASS；发布副本将workspace/localhost替换为占位符，原SHA与副本SHA分别保存。覆盖代表原生/目标运行、ABI/构建和历史结果指针；不包含全部历史raw、世界、依赖或第三方JAR。远端上传/重下载均NOT RUN。
