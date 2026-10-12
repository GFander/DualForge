# DualForge / ForgeCompat

项目目前是**受控 ForgeProbe 兼容原型**：以 NeoForge 为唯一宿主，在固定服务端环境验证最小 Forge 入口、构造参数转换、指定注册路径及依赖元数据。不是通用 Forge 模组兼容层；不代表任意 Forge 模组或 API 已可用。Fabric 只是未来方向，不是当前交付目标。

有效基线为 **Convergence-002 完成后的当前源码**：[spike001a/compat-002-0l/src](spike001a/compat-002-0l/src)（10 Java＋5资源），保留 Parser 的依赖 owner 分组和 mandatory 适配修复。目录名沿用0l，不意味着回退为历史0l版本；旧 `spike001a/compat/build/forgecompat.jar` 与原服务器安装的历史0l JAR均不是当前新构建基线。

## 已有成果与范围

- [Convergence-001 交付报告](<Dev Manuals/Convergence-001 交付报告.md>)：统一 javac 干净构建、制品指纹追溯、隔离 smoke／回归入口，不依赖残留 class。
- [Convergence-002 交付报告](<Dev Manuals/Convergence-002 交付报告.md>)：复现并修复依赖声明丢失，按owner分组，核验Forge mandatory与FML type差异；六组metadata边界、四个动态依赖正负例和原0l回归已有保存证据。

这些 PASS 是各任务的历史执行结果；本次整理入库没有重跑服务端，也没有执行 Convergence-003a。依赖修复仅保证现有扫描器的简单TOML子集，不是完整Forge TOML规范，不扩展 Registry、Capability 或事件兼容。

## 固定环境与外部前置

| 组件 | 固定版本 |
|---|---|
| Minecraft / NeoForge | 1.21.1 / 21.1.256 |
| FancyModLoader / ModLauncher | 4.0.45 / 11.0.5 |
| Microsoft OpenJDK | 21.0.10+7-LTS |
| securejarhandler / NeoForge bus | 3.0.8 / 8.0.5 |
| ASM / asm-tree | 9.10.1 / 9.10.1 |
| slf4j-api / maven-artifact | 2.0.9 / 3.8.5 |

当前脚本面向 Windows 和 PowerShell 7。本地实验根路径是 `D:\DualForge`；JDK默认路径是 `C:\Users\Albert\.jdks\ms-21.0.10`。构建脚本可用 `-JdkHome` 指定同版本JDK，但依赖测试仍使用固定路径，转换器及历史脚本也有绝对路径。未经修改和验证，不保证其它平台或目录布局可运行。

**克隆仓库不能独立运行。** 必须预先准备以下外置输入；当前入口不自动下载、安装依赖或重建固定Probe：

- `spike000/neoforge-server/libraries/`：上述NeoForge安装解析出的完整服务端依赖（包括 `net/neoforged/neoforge/21.1.256/win_args.txt` 和 Minecraft 服务端）。编译从其中8个既存JAR取classpath，不用历史classes。
- `spike000/probe-002-0l/build/forgeprobe.jar`：原样固定Probe，SHA-256见下表。探针源码／资源在同级 `src/`，但当前入口不会编译它。不能以其它探针版本替代。
- 服务端的 `config/`、`defaultconfigs/`、`spike000world/`：smoke物理复制这些目录；config的三个无凭据文本文件入库，空目录及世界不入库，须由本地环境准备。
- `spike000/run-server.ps1`：本次已纳入的第一方harness。它在测试副本生成 eula、server.properties、user_jvm_args；不要直接用于有重要数据的原服务器。

历史 `build.gradle` 是当时依赖声明／实验记录，不是当前构建入口；本任务未迁移到Gradle或引入新框架。

## 当前构建、回归和依赖测试

在 `D:\DualForge` 的 PowerShell 7中执行：

```powershell
pwsh -NoProfile -File .\tools\forgecompat.ps1 -Mode build
pwsh -NoProfile -File .\tools\forgecompat.ps1 -Mode regress

# 使用上一步 result.json 中的实际新JAR路径，保留文件名：
$compatJar = 'D:\DualForge\out\forgecompat\<实际run目录>\forgecompat-002-0l.jar'
pwsh -NoProfile -File .\tools\forgecompat.ps1 -Mode smoke -Jar $compatJar
pwsh -NoProfile -File .\tools\test-forge-dependencies.ps1 -Stage after -CompatJar $compatJar
# 仅metadata边界，不启动服务端：
pwsh -NoProfile -File .\tools\test-forge-dependencies.ps1 -Stage after -BoundaryOnly -CompatJar $compatJar
```

`regress` = 干净构建＋原固定Probe隔离smoke；独立依赖入口另执行六组metadata检查及C/D/E/F加载正负例，不放宽原smoke成功判据。`Stage before`只用于本地保存的修复前制品做失败复现，不是当前验收模式。

可用 `-OutputRoot D:\DualForge\out\my-check`。每次创建时间/GUID子目录，保存输入、工具、JAR指纹、实际命令、完整日志及断言；不删除旧输出。新构建默认产物只在out，**不覆盖原服务器JAR**。构建不要求新源码与历史hash相同，历史内容比较仅诊断。

原smoke要求新JAR实际服务发现、构造marker、真实register返回、内置javafml委托及NeoForge原生加载、Done／正常stop、无ERROR/FATAL/dump FAIL/crash，不能单凭Done。依赖负例必须核验具体FML拒绝原因及目标实例化缺席，不能单凭退出码或没有Done。

统一入口退出码：0=本模式PASS，10=构建失败，20=启动失败，30=断言失败，40=未执行（regress已构建则PARTIAL）。以本轮 `result.json` 为准，失败日志保留。

smoke／动态依赖测试必须串行，在允许所需socket/DNS的安全本机运行。沿用离线模式、空server-ip和25565，会临时监听端口，**不要在暴露公网的机器运行**。其它Java进程、占用端口或无法安全隔离时不要继续。源码中三个shared/current调试缓存路径仍硬编码；脚本在获准的本地实验方式中备份、保留本轮dump、恢复原内容与时间戳，再复核冻结文件，不代表运行完全无临时外部副作用。

## 目录与外置证据

- 当前生产源码／资源：`spike001a/compat-002-0l/src/`。
- 固定Probe源码／资源：`spike000/probe-002-0l/src/`。
- 当前工程入口：`tools/forgecompat.ps1`、`tools/test-forge-dependencies.ps1`。
- 本轮依赖fixture：`tests/dependencies/`。
- 架构、说明书、报告：[Dev Manuals](<Dev Manuals/>)；历史Spike源码、src-before/src-after和第一方脚本保持原路径归档，**不作为当前实现入口**。历史脚本可能原地修改服务端，勿批量执行。

冻结二进制、完整运行附件仍在本地 `D:\DualForge\spike000\logs\`、`D:\DualForge\spike001a\...\build/`／`archive/` 和 `D:\DualForge\out\convergence-001/002\`，原件不移动、不修改，不随Git分发。Convergence交付报告在Dev Manuals新增原字节副本，原out报告保留；报告中的绝对路径是外置证据定位，不是GitHub附件下载地址。

| 外置核验对象 | SHA-256 |
|---|---|
| 当前Parser源码 | `92749EE51A245419D8CCEA80499845B94310251219920FBBAACEF82FF0E8FF1B` |
| Convergence-002回归新JAR，26214字节 | `8CDC180BF7C7401A49A35A895887BD3DB5EF7DB93B997840AC3C476B46E6189D` |
| 固定0l Probe，2258字节 | `A774D2F737B17F089046014AB55A53767DBAD008CD44DCCE52C727B87E559AE8` |
| 历史0l安装JAR（非当前修复件） | `D49CAEA48A61C57DA5384E053D7E8DE2834760172E6948C5167BF836BFD0E679` |
| loader-4.0.45.jar | `8390398E67DF23E72D85ED809685BE2752C8EC545B7CBB6AD4B70FDC81E4DCD4` |
| neoforge-21.1.256-universal.jar | `E0A08BAE5CEA95BE96C09770530F5643BB183AF8AF59C64694EB346CB4C93FA4` |
| modlauncher-11.0.5.jar | `FD9B9FC7CF043D2264EA0113EAC6B258F0A71FECE5B00E831BFBDF9726C78A24` |

Convergence-002新JAR证据：`out/convergence-002/regress/20261012-134158-70642187/`；依赖正负例：`out/convergence-002/after/20261012-134007-c1ed7fd1/`。Git保存第一方源码、fixture、脚本、手册／报告，不保存编译产物、第三方依赖、世界、完整运行日志、名单、凭据或调试缓存。`.gitattributes`禁止Git自动改写文本换行，保留历史文本与源码字节。

目标仓库已有LICENSE及其历史，本次整理不另行选择或添加项目许可证，也不改动既有LICENSE。后续任务仍遵守单目标、最小改动、先复现、回归保护和达到验收即停止。
