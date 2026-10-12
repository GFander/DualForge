# Convergence-001 交付报告

日期：2026-10-12。结果：**PASS**。仅建立当前 0l 构建／隔离回归入口，未进入下一阶段。

## 1. 本轮目标及有效基线

当前有效源码为 `D:\DualForge\spike001a\compat-002-0l\src`。实际是 **10 Java + 5 资源 = 15 文件**，不是任务背景中的 15 Java。15/15 SHA-256 与 0l 冻结源码清单一致；不存在该源码树内的未归档漂移。当前工作区没有 Git，不能用 Git diff 证明全项目清洁，故使用源码清单和逐文件指纹。

可信对应关系：当前源码指纹 = 历史 `compat-source-after.json`；历史编译命令明确全量 10 源及 8 JAR classpath；当前安装 JAR = 历史构建 JAR = `outputs.json`；本轮从源码重建的全部 17 个内容文件又与该制品一致。见 [基线核验](D:/DualForge/out/convergence-001/baseline-verification.json)。未选择旧 `compat/build/forgecompat.jar`。

| 制品 | 原始安装／冻结 SHA-256 |
|---|---|
| `spike000/neoforge-server/mods/forgecompat-002-0l.jar`，25508 字节 | `D49CAEA48A61C57DA5384E053D7E8DE2834760172E6948C5167BF836BFD0E679` |
| `spike000/neoforge-server/mods/forgeprobe.jar`，2258 字节 | `A774D2F737B17F089046014AB55A53767DBAD008CD44DCCE52C727B87E559AE8` |
| 原 `run-server.ps1`，9092 字节 | `3176A095E7744EA78113DDF7561A83C5F9407DD61CF0ECB7BC0FCB2487464576` |

本轮实际 JVM 日志确认 Minecraft 1.21.1、NeoForge 21.1.256、FML 4.0.45、ModLauncher 11.0.5、Microsoft OpenJDK 21.0.10+7-LTS、Windows 11 amd64；不是仅引用历史版本。JDK 固定路径 `C:\Users\Albert\.jdks\ms-21.0.10`，不使用 PATH Java。运行制品完整哈希见 [验证汇总](D:/DualForge/out/convergence-001/verification-summary.json)。

## 2. 修改及构建入口

新增项目文件仅两个：

- [tools/forgecompat.ps1](D:/DualForge/tools/forgecompat.ps1)：唯一当前入口，支持 build、smoke、regress；固定 0l 源码、既存 8 个依赖 JAR、JDK21.0.10，逐次生成全新输出目录。
- [README.md](D:/DualForge/README.md)：当前基线、命令、验证边界、退出码和隔离要求。

本报告及本轮构建、备份、日志、指纹属于新增 `out/convergence-001` 交付附件。未编辑兼容层源码、历史构建脚本、原 harness、原服务端安装 JAR、手册或冻结 Spike。

```powershell
# 在 D:\DualForge、PowerShell 7 中运行：
pwsh -NoProfile -File .\tools\forgecompat.ps1 -Mode build
pwsh -NoProfile -File .\tools\forgecompat.ps1 -Mode regress
# 或对一个新构建产物单独执行 smoke：
pwsh -NoProfile -File .\tools\forgecompat.ps1 -Mode smoke -Jar D:\DualForge\out\convergence-001\regress\20261012-131209-b80661d9\forgecompat-002-0l.jar
```

构建参数：`javac --release 21 -encoding UTF-8 -classpath <8 个既存 JAR> -d <新目录/classes> <全部10源>`；原资源全量复制，以原 MANIFEST 用 JDK jar 打包。classpath 没有历史 classes 目录；输出目录从不存在到创建，不读旧编译产物。每轮保存实际命令、原始 stdout/stderr、退出码、全部源码／资源／编译依赖／工具指纹和 JAR 内容指纹。普通源码修改不会因不匹配历史 hash 自动失败，历史制品比较仅作诊断。

按 Karpathy 指南保持改动局部：复用 javac 和原 harness，不迁移构建框架，不增加兼容层抽象，不改变运行语义。

## 3. 验证结果与证据

**四次干净构建 PASS**，包括在同一 OutputRoot 下重复运行。所有 JAR 均 25508 字节、26 ZIP 条目（含目录）、17 内容文件。4/4 构建的全部内容文件与历史安装件逐字节一致，无新增、删除或字节码变化。JAR 整体 hash 不同；ZIP 顺序及长度一致，时间戳变化，非运行代码变化。见 [内容汇总](D:/DualForge/out/convergence-001/verification-summary.json) 和 [ZIP 元数据对照](D:/DualForge/out/convergence-001/zip-metadata-comparison.json)。

最终新构建制品：

`D:\DualForge\out\convergence-001\regress\20261012-131209-b80661d9\forgecompat-002-0l.jar`

SHA-256：`6EBF2C1CCF008669EEC14EF70EB3F3769E7538382249F297955F4336DA102335`。

**两次真实隔离 smoke PASS**，均 `DONE_STOPPED_GRACEFULLY / exitCode=0 / 22.7s`。最终脚本 13 项断言全通过；不是仅凭 Done。原 libraries/config/defaultconfigs/world 物理复制到本轮目录，无链接至原服务端；本轮 JAR 只安装到副本。四个 JVM true 开关沿用原 0l。

| 实际证据 | 最终运行日志定位 |
|---|---|
| 服务发现指向本轮 server/mods 的新制品 | debug.log:25 |
| transformer 服务实际加载；语言 provider 来自新安装文件 | debug.log:32、161 |
| 构造器 marker | stdout.log:81 |
| SubscribeEvent 与 Event profile 转换命中 | stdout.log:75、85 |
| 真实 register 调用返回，declaredMethods=1 | stdout.log:88 |
| 内置 javafml 委托与 NeoForge 原生加载 | stdout.log:79、89 |
| Done、正常 stop、JVM exit0 | stdout.log:109及后续；meta.json |
| 无 ERROR/FATAL/dump FAIL、stderr 空、无 crash | assertions.json及完整 stdout/latest/debug |
| 新构建 JAR 和固定 probe 安装字节一致 | loaded-mods.json、smoke-inputs.json |

以上文件均位于 [最终 smoke 目录](D:/DualForge/out/convergence-001/regress/20261012-131209-b80661d9/logs/spike002-0l-treatment/meta.json)；[结构化结果](D:/DualForge/out/convergence-001/regress/20261012-131209-b80661d9/result.json)、[断言](D:/DualForge/out/convergence-001/regress/20261012-131209-b80661d9/assertions.json)、[构建命令](D:/DualForge/out/convergence-001/regress/20261012-131209-b80661d9/javac.command.json)。原始日志保留，无摘要替代。

失败路径核验（负例均为预期结果，不是当前实现失败）：

| 类型 | 本轮验证 | 结果 |
|---|---|---|
| 构建失败 | 缺失 JDK 路径，子进程实际退出码 | FAIL / BUILD / 10；failure.txt 保留 |
| 未执行 | 不存在的 JAR，子进程实际退出码 | NOT RUN / NOT_RUN / 40；没有启动服务器 |
| 启动失败 | 新副本 MaxWaitSeconds=1 | FAIL / STARTUP / 20；TIMEOUT_FORCED_KILL、完整可用日志保留，缓存恢复 PASS |
| 断言失败 | 复制本轮成功日志，仅移除 register 返回 marker，执行脚本真实断言块及分类映射 | Done 仍在但被拒绝，ASSERTION / 30；这是合成日志单元检查，**不是额外真实 JVM 断言失败实验** |

证据分别在 `negative-build/20261012-131004-42397cab`、`negative-not-run/20261012-131005-d9d31f4e`、`negative-startup/20261012-131411-1209209b`、[negative-assertion/unit-proof.json](D:/DualForge/out/convergence-001/negative-assertion/unit-proof.json)。regress 构建成功而 smoke 无法执行时，脚本输出 PARTIAL；本轮成功运行不属于 PARTIAL 或 NOT RUN。

## 4. 保全及剩余限制

两次成功 smoke 和超时负例均逐项复核原四个项目根目录中的 **2247 文件**，文件集合及 SHA-256 无变化。原服务器 JAR 未覆盖；原世界、日志、配置、源码和冻结附件内容未变。134 项 0l 取证附件及 17 项运行附件独立复核亦全部匹配：[冻结证据核验](D:/DualForge/out/convergence-001/frozen-evidence-verification.json)。

用户明确批准处理的三个硬编码共享调试缓存：运行前备份；本轮 dump 留存到 out；运行后恢复原字节、CreationTimeUtc、LastWriteTimeUtc，并校验。成功和失败运行均恢复通过。见 [最终保全结果](D:/DualForge/out/convergence-001/regress/20261012-131209-b80661d9/preservation-result.json) 和 [超时保全结果](D:/DualForge/out/convergence-001/negative-startup/20261012-131411-1209209b/preservation-result.json)。最终 Java 进程和 25565 监听均为 0；端口检查需要沙箱外只读权限，已完成，不把沙箱拒绝当作“端口空闲”。

本任务无剩余验收阻塞。保留的限制：需要已有本地工具链／服务端依赖；沿用原离线模式与25565参数，须在安全测试机器串行执行；共享缓存仍是历史源码硬编码，备份恢复是本任务允许的隔离补偿，不是语义修复。smoke 只保证固定 0l ForgeProbe profile 的加载、注册返回和 NeoForge 原生启动，不证明通用 Forge API、真实回调、任意模组或 Fabric 支持。

## 5. 下一步建议及停止

Convergence-001 七项成功标准全部满足，裁定 PASS。后续获授权的功能修改先用本入口回归，再限定一个新的工程目标。本轮不处理 Parser、不新增事件 Spike、不更新历史架构结论。交付后停止，等待下一阶段指令。
