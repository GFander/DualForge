# Convergence-007C-4B 交付报告

日期2026-10-15 UTC。**OVERALL：PASS（本轮受控Helper Registry目标）**。FnF Helper计划REJECTED、FnF运行NOT RUN；不代表FnF整体兼容。C4A整体PARTIAL保持原结论，4C未实施。

## Git与源码身份

- Branch：`codex/convergence-007c4b-registry-bridge`；Base：已收敛main `6d7c0bf3643cbb2c0f211a083dba7a373f2e47aa`。
- C3最终29893683、C4A最终68e5dc3及集成78e9fc5/566e25e均为main祖先，21项旧生产指纹和关键文件独立核验；PR23 main合并及精确SHA的Actions38722346778通过。BASELINE_NOT_CONVERGED已解除。
- 先行时序取证提交e77f9c8；初始实现34801fa；测试add608a；最终生产补正 `24a66d3437898860183d84a3e63c069604c2f0a8`，最终运行/测试提交 `2ff283086cee743f6ecd40cce87146237aad854c`。
- PR：[24](https://github.com/GFander/DualForge/pull/24)，base main。代码已推送；最终文档提交及最新CI以PR当前HEAD和最终任务回复核对。未合并、未创建Release。

最终源码与24a66d3生产逐文件一致；正式串行动态在干净detached验证worktree执行。构建原始argv/源码指纹及物理制品复制关系保存；文档和编译准备计数说明不改变生产Java字节。不改写旧证据或把初始候选结果当最终PASS。

## 结果与范围

原生Forge-only输入 `helper-probe.jar`：6119 bytes，SHA `0fd2e66dd33623ff58d8eca931ec9ae24b76aabbb9fa1eee5af191e1c1c0d610`；原生Forge52.1.0先实际验证真实两个Item/Holder/Consumer/Supplier/官方事件栈及完整生命周期。原JAR冻结，NeoForge C/T始终使用同一原字节。

最终生产JAR：**119083 bytes**，SHA-256：

`13e1b35bdb21667e2e1093e2a1e8ae28044faf2b51dbd8fce094eefcfe1d3a3f`

最终NeoForge Control/Treatment、8桥接拒绝、110/35新Helper契约、所有允许执行的受影响历史回归通过。两组仅改变DF4B_REGISTER；未注册组Supplier均0且ID不存在，注册组Supplier各1、两个真实Item互不覆盖，Holder.get/Consumer/Registry key/Mod Bus及官方RegisterEvent栈核验。原生模组与内置javafml委托正常，Done/stop/exit0、空stderr、无crash。strictZeroErrors=false，实际空宿主已有Settings/网络ERROR，完整日志保留且无新增。

## 修改文件与关键差异

| 文件/范围 | 变化 |
|---|---|
| ForgeRegistryTransformationPlan.java（新增） | 真实FML归属/同JAR引用闭包、逐调用数据流与不可变整组计划、精确Registry/Holder/Consumer ABI、版本v2、enum内部值、防冲突和输入SHA |
| ForgeRegistryBridge.java（新增） | 仅构造期、真实owner/module/active容器、原生receiver/Registry身份及一次挂接；精确官方public方法调用，不公开原生Bus |
| RegistryBytecodeVerifier.java（新增） | 实际ASM Analyzer/BasicVerifier，不定义游戏类 |
| DualForgeEntryTransformer.java | 新组预验证后发布Targets、输入比对/逐类副本转换；旧分支保留 |
| ControlledForgeContextProfile.java | 新增仅接受已验证helper集合的独立调用路径；原Context入口仍用空集合/原验证 |
| ForgeConstructorContext.java | 最小内部归属检查；两个既有Context调用点和来源BusView语义不变 |
| tools/lib/unix_workflow.py | 添加asm-analysis固定编译输入；prepare入口/说明同步12个输入 |
| tests/helper_registry/及CI | 原生源码/独立Observer、真实FML与ASM/Bridge负例、stock C/T、严格cohort；CI增加JDK21及不依赖游戏安装的生产栈验证器Contract |
| 其他测试/文档 | 平台矩阵仅增加显式Gate8跳过N2选项，默认及18项断言不变；更新README/STRUCTURE/AGENTS/导航，新增本轮报告/索引 |

Reader/Locator准入、Parser和依赖投影、Module Layer/Descriptor、旧Item/Block/Event/Profile及其负例、冻结输入和历史报告均未改变。

## 最终Gate状态

| 项目 | 状态 |
|---|---|
| MAIN_BASELINE_CONVERGED | PASS |
| FML_TARGET_DISCOVERY | PASS |
| TRANSFORMATION_PREPLAN | PASS |
| HELPER_OWNERSHIP | PASS |
| EXISTING_PROFILES_PRESERVED | PASS |
| REGISTRY_FIELD_MAPPING | PASS |
| DEFERRED_REGISTER_CREATE | PASS |
| SUPPLIER_REGISTER_ABI | PASS |
| HOLDER_CROSS_CLASS_CLOSURE | PASS |
| METHOD_HANDLE_AND_INDY | PASS |
| CONTEXT_BUS_OWNERSHIP | PASS |
| REGISTRY_MOD_BUS_BRIDGE | PASS |
| OFFICIAL_REGISTER_EVENT | PASS |
| NATIVE_FORGE_FIXTURE | PASS |
| NEOFORGE_FIXTURE_RUNTIME | PASS |
| NEGATIVE_CONTRACTS | PASS |
| HISTORICAL_REGRESSION | PASS |
| NATIVE_NEOFORGE_ISOLATION | PASS |
| ORIGINAL_JARS_UNCHANGED | PASS |
| OVERALL | PASS |
| FNF_PLAN_ADMISSION | REJECTED |
| FNF_RUNTIME | NOT RUN |

## 支持边界与交接

当前只支持经过验证的普通Item helper、零捕获Lambda/本类静态factory reference、纯Holder/Item getter及单ModFile归属。两个helper及全新身份通过实际FML静态Contract；动态验收为一个独立helper的两个Item。未知Registry/API/业务/Bootstrap/Consumer闭包明确拒绝。计划/单类克隆原子性不等于JVM已定义类可回滚，也不是恶意JVM沙箱。

FnF首次只读拒绝为SUPPLIER_BUSINESS_UNSUPPORTED；67 Item /3 Tab invokedynamic仅盘点，未完整验证。C3最后真实ForgeRegistries缺类异常继承未重跑。4C建议限于逐Supplier/SRG/Food/Fuel、完整Tab消费者、独立全局Bus语义及之后真实运行；本轮没有实施。客户端、Tab显示、食用/燃料行为和持久化NOT RUN。

## 证据与重跑

完整分项见[时序](Convergence-007C-4B%20FML目标发现与加载时序取证.md)、[计划契约](Convergence-007C-4B%20Registry%20Helper与转换计划契约.md)、[Bus](Convergence-007C-4B%20Mod%20Bus桥接与归属验证.md)、[ABI](Convergence-007C-4B%20跨类ABI与Bootstrap闭包.md)、[动态](Convergence-007C-4B%20原生Forge与NeoForge动态验收.md)、[历史与负例](Convergence-007C-4B%20历史回归与负例.md)、[FnF/4C边界](Convergence-007C-4B%20FnF实际边界报告.md)和[机器索引](Convergence-007C-4B%20证据索引.json)。

测试参数见[helper_registry](../tests/helper_registry/README.md)。每条实际argv、cwd、环境身份、版本、源码/输入/制品SHA及原始stdout/stderr/debug/assertions在索引列出的out路径中；所有JVM串行，物理副本运行。374项冻结保护、原世界及原始JAR保全和端口/JVM释放PASS。

选定原始证据归档（不是全部workspace副本）：`/workspace/DualForge-007C4B-validation/out/convergence-007c4b/evidence-export/dualforge-007c4b-evidence.zip`。
大小 **5269418 bytes**，payload **3074 files**；SHA-256 `ace523de09942c5e56600cf3d21e48f33a5ba28e8025ba6f820bd1218c14ab90`。
ZIP、独立解压、Manifest逐文件SHA及原文件归档前后SHA均PASS。包含必要命令/日志/结果/指纹/字节码和自有制品；保留native首次观察器失败及旧版token碰撞复现，最终结果只按13e1b35b cohort验收。排除世界、第三方依赖/发行JAR、生成缓存及重复server副本；没有改写或删除原始记录。选定内容凭据模式扫描无命中，内部workspace/loopback路径保留。

**ARCHIVE_READY / NOT PERSISTED**。本轮没有上传或远端下载验证，没有持久化URL；不恢复已取消的WebDAV上传。此限制不影响本轮受控服务端功能验收，但本地out无法保证环境消失后仍可取得。

代码HEAD2ff2830的GitHub CI实际PASS：[Actions38731852426](https://github.com/GFander/DualForge/actions/runs/38731852426)。最终文档HEAD另行核对最新CI，最终任务回复提供实际SHA/状态；不会将代码HEAD的CI冒充另一HEAD。
