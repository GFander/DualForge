# Convergence-007C-1 交付报告

**整体PARTIAL：标准TOML、Forge身份/准入和最终同JAR历史回归PASS；原始Food N' Fuel的下一真实首失败为FML required forge依赖缺失。** 没有绕过依赖，没有实现新构造器或注册能力，FnF尚不兼容。

任务分支 `codex/convergence-007c1-forge-metadata`，独立[Draft PR #19](https://github.com/GFander/DualForge/pull/19)，base main。起点/执行末fetch main均为 `9d4935587d5f96c4d183afb4bb594504e3e8d654`，已通过PR #17/#18集成007B/007C；旧PR #16已合并，不堆叠本轮修改。实际被测源码提交 `b382ab63507e58e5aadd374a10a85eaaf77a0f10`；之后只有文档/索引更新，源码指纹复核一致。最终HEAD和Actions实际状态记在PR正文与最终回复，不在提交内容中制造自引用SHA。

## 状态

| 项目 | 本轮状态 |
|---|---|
| GIT_BASELINE | VERIFIED |
| ORIGINAL_FORGE_JAR | FROZEN |
| NIGHTCONFIG_RUNTIME / TOML_PARSER | PASS / PASS |
| FORGE_IDENTITY / FORGE_ADMISSION | PASS / PASS |
| NEOFORGE_NATIVE_ISOLATION | PASS |
| DEPENDENCY_SEMANTICS | PARTIAL：尚无可靠forge等价语义 |
| FOOD_N_FUEL_METADATA | PASS |
| FOOD_N_FUEL_FML_LOADING | PARTIAL：进入依赖检查，未进入entry/transform/constructor |
| NEXT_FIRST_FAILURE | required forge [0,) missing，FML依赖验证 |
| ZERO_JVM_PARAMETERS | PASS |
| HISTORICAL_REGRESSION | PASS，同一最终compat SHA |
| LINUX_SERVER | PARTIAL：受控回归PASS，FnF生命周期FAIL |
| MACOS_CLIENT | NOT RUN |
| OVERALL | PARTIAL |

## 制品、工具链与范围

新 `DualForge.jar` / `dualforge.jar`同字节：76,102字节，SHA-256 `db5ec8fe3704416454a904148878d2d949e4286010dd8fc47a72de28fa71a3b5`。新构建在固定提交独立worktree `/workspace/DualForge-007C1-validation`完成；清空新构建输出、使用本轮实际Microsoft JDK21.0.10+7-LTS，NeoForge21.1.256 / MC1.21.1 / FML4.0.45 / NightConfig Core/TOML3.8.3。Python/zsh/OS实际版本及全部编译JAR SHA见索引toolchain和build输入记录。

原始FnF128,098字节，SHA `bd415b04529c4ffc31e14757488f71dab6917e4560c61e236c3c4d0de6557958`，JAR CRC、完整成员、TOML/Manifest逐项复核不变。当前生产只改Parser/Locator；新Jar与007C旧Jar逐成员对照，已有class只这两项变化，新增五个Locator内部/匿名class，其他成员全部相同，无删除。Reader、ForgeJarMetadata、LanguageLoader、Transformer/profile、resources及构造器/事件/注册策略未改。

逻辑提交：`d62b29a`标准TOML与两个宿主编译依赖，`98872f2`同JAR注解身份与直接测试迁移，`b382ab6`观察实际FML赋值的测试API类型补正；不amend或改写历史。活跃文档更新本轮规则；冻结007C报告、Probe、旧fixtures、原世界、宿主配置/官方Jar与374保护项保持不变。

## 原失败与下一失败

本轮Gate0旧compat+原FnF标准运行：Reader到达，Forge JAR被Skipping，`fnf:logo`为Unknown item，宿主随后Done/stop/exit0。这是发现失败，不是构造器失败。

新compat双JAR标准运行：FORGE_CONFIRMED、Reader/Parser真实交接、FML `fnf 1.3-1.21.x`、一次发现及官方pipeline去重、原language/dependencies保留。随后官方ModSorter明确：

```text
Mod ID: 'forge', Requested by: 'fnf', Expected range: '[0,)', Actual version: '[MISSING]'
Error during pre-loading phase: Mod fnf requires forge 0 or above
```

无Done、真实FML crash report，宿主exit0，stderr为空。原stock runner保留生命周期FAIL；独立派生断言只把“明确准入并正确观察依赖拒绝”记PASS，不覆盖原结果或声称功能通过。

实际/proc命令为 `java @user_jvm_args.txt @libraries/net/neoforged/neoforge/21.1.256/unix_args.txt nogui`。没有额外DualForge/DF005/006业务属性、agent、classpath/module-path或launcher。两个额外诊断运行分别证明NightConfig在真实Reader层可访问和FnF被FML赋值javafml/9999；诊断SPI不在最终双JAR运行中，也不提供依赖/变换。Provider版本不是全局优先级保证。

解析/身份契约详见[TOML与身份](<Convergence-007C-1 TOML解析与身份取证.md>)；FML官方依赖实现、没有等价提供语义及停止理由详见[Forge依赖](<Convergence-007C-1 Forge依赖语义取证.md>)。

## 最终同一JAR验证

| 验证 | 实际结果 |
|---|---|
| 新metadata/admission | 91断言PASS；真实FnF、007B、006A/B及Clumps原TOML，非法语法/类型/字段/UTF8/大小/身份/依赖及未接管字节 |
| Probe / 002 | 原严格smoke、metadata A–F、C/D/E/F动态PASS；负例按明确缺失mod/实际range判断 |
| 原构造器C/T | C仅关闭入口rewrite得到官方“不支持Forge IEventBus参数”拒绝；T严格smoke PASS，同compat与冻结Probe SHA；不是FnF构造器证据 |
| 003a / 003b | Discovery、重复路径/文件名/metadata交接/真实身份歧义，Identity动态与范围/未接管字节PASS |
| 004 / 005 | Event与Registry Contract、C/T、回归、Scope/Atomicity/Assets PASS |
| 006A / 006B | Contract、Control、Treatment全部PASS，同最终SHA |
| 007A / 007B | Bootstrap契约、默认配置，普通Item/原生mod零参数共存查询PASS；原13拒绝与显式JPMS拒绝PASS |
| Python / zsh / 保全 | 44 Python单测、全部zsh语法、diff检查、374保护项PASS |
| FnF原始/新阶段 | 两次真实边界与原制品不变已证实；新阶段止于依赖拒绝 |

全部JVM串行、物理副本运行。最终JVM列表为空、25565端口可重新绑定。历史harness仍使用已验证proxy/public-key relay、dump和loading agent；这些是历史回归环境，不称为用户零参数启动。

本轮标准空宿主基线有原属性文件不存在/Mojang key连接两个错误种类。普通Item+native运行严重记录完全一致且无新增、正常Done/stop/exit0；`strictNoErrors=false`原样保留，不能宣称无ERROR。历史严格smoke/回归仍无ERROR/FATAL/dump FAIL。

003a/003b生成对照有明确必要迁移，但旧冻结输入/Java语义断言未改；依据和重跑入口见[metadata README](../tests/real_mods/food_n_fuel/metadata/README.md)。

## 原始证据、限制与重跑

[证据索引](<Convergence-007C-1 证据索引.json>)逐项保存2,554个选取原文件路径、大小/SHA、阶段、工具链、命令/exit、输入与最终制品。核心路径：

- 旧失败/NightConfig：`/workspace/DualForge-007C1/out/convergence-007c1/gate0-original/`与`nightconfig-runtime/`。
- 新构建与双JAR失败：`/workspace/DualForge-007C1-validation/out/convergence-007c1/final-build/20261014-173138-5302aa1f/`、`final-fnf-stock/`。
- 新Contract：`.../metadata-contract/20261014-173207-8053a0ff/`；Provider：`.../provider-stock/`。
- 全历史：`.../out/identity-unification/verification/20261014-173454-7a5b7ad9/`、`out/convergence-006a/`、`out/convergence-006b/`及索引指向的子结果。
- 标准共存：`.../out/convergence-007c1/stock-normal-native-query/`。

本地ZIP为 `/workspace/DualForge-007C1/out/convergence-007c1/evidence-export-final2/dualforge-007c1-minimal-evidence.zip`，3,872,087字节、2,557个文件（2,554 payload加Manifest/SHA/README）。SHA-256 `9d7f03b2710a942025ac217ee9b6b9240620c049339d30f67f08f9fccc5492ca`。CRC、解压逐Manifest SHA/数量、路径/无软链接、原文件归档前后不变及安全扫描PASS。发布文本副本脱敏workspace/loopback/crash/loader信息和password命名JSON值（现场仅空RCON默认值）；原SHA与副本SHA独立记录。保留小型受控编译/转换class证明字节范围，不包含第三方运行Jar、世界或账号信息。不是全部附件副本。

**ARCHIVE_READY / NOT PERSISTED**：仅本地归档，无远端上传/重下载或URL，不复用旧任务Gist授权。cleanup包装器总exit回执未保存，单阶段exit/result和子进程最终PASS标记完整；缺失项明确保留，不补造历史退出文件。开发/失败尝试单独保留，验收仅引用最终cohort。

重跑构建：`zsh tools/entry/dualforge.zsh --server-input <official-install> --output-root out/<new-run>`。metadata/诊断SPI参见专用README。双JAR标准重跑：

```zsh
python3 -B tests/real_mods/food_n_fuel/run_stock.py --host neoforge \
  --install <official-install> --output out/<fresh-fnf-run> --jdk-home <jdk21> \
  --mod <new-DualForge.jar> --mod <unchanged-fnf-forge-1.3-1.21.x.jar>
```

本轮真实命令、argv、条件、fingerprints和全部阶段runner参数由索引/归档提供，回归入口参见tests/README。停止于缺失可靠forge依赖能力契约；不进入新构造器/跨类Registry/SRG/Food/Fuel/Tab/Clumps、客户端或下一阶段，不自动合并/Release。
