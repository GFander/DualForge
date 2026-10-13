# DualForge / ForgeCompat

DualForge 是以 NeoForge 为宿主的受控 Forge 入口兼容原型。已验收基线为 **Convergence-004**；本分支增加 **005 受控单 Item 注册**，005-S 与资源静态验收 PASS，005-C 在用户 macOS arm64 受控人工验收中 PASS。源码位于 [spike001a/compat-002-0l/src](spike001a/compat-002-0l/src)。目录名沿用历史编号，当前构建始终从这些源码和资源全量编译。

仓库只维护 **zsh 脚本入口**。Python 3 标准库负责内部的构建编排、JSON、JAR 指纹、进程及日志捕获；运行不需要 Windows 或 PowerShell。当前仍使用原单参数构造器 ABI；004 的监听器支持范围如下。

## 仓库结构

生产源码保持在 `spike001a/compat-002-0l/src/`；`history/` 集中保存旧 compat 迭代、0l 冻结副本和旧实验记录。工具分为 `tools/entry/`（zsh 入口）、`tools/lib/`（Python 实现）和 `tools/harness/`（活跃独立 harness）。`tests/` 的文件位置保持不变。

完整目录树、活跃／历史状态和迁移映射见 [STRUCTURE.md](STRUCTURE.md)；测试夹具与运行方式见 [tests/README.md](tests/README.md)。仍保留原位置的 `spike001a/compat/`、`spike000/probe/` 是遗留记录，不能作为并行生产版本。Gradle 弃用说明见 [DEPRECATED.md](spike001a/compat/DEPRECATED.md)。

## 当前支持范围

- 003a：按受控 Forge metadata 发现 JAR，以规范化路径交由官方 FML pipeline 去重；不依赖固定文件名。`loaderVersion` 的 52 下界是当前准入策略。
- 003b：根据所属 modId 的 Forge `@Mod` 扫描确定入口，仅转换该入口与同制品内实际引用的受控 Event。原 ForgeProbe 与不同身份 IdentityProbe 已分别完整加载。
- 003b 证明 register 真实返回；004 证明一个受控自定义 Event 在正常启动后的宿主全局总线上真实 post 一次，注册组回调一次、未注册组零次。测试型 NeoForge driver 不属于 Forge API 兼容能力。
- 支持原单参数 Forge IEventBus 构造器、空 listener，以及严格四条打印指令的 marker-only listener；Event profile 和构造器 ABI 保持不变。
- metadata 仅支持现有 Parser 的简单 TOML 子集；保留依赖 owner 分组及 mandatory 到 FML REQUIRED/OPTIONAL 的适配。
- 005：一个扫描归属明确、严格结构匹配的 Forge DeferredRegister<Item> 入口；转换创建、单 Supplier、MOD bus 挂接、holder 字段及 get 引用。原构造器 ABI 不变，真实注册 C/T 与旧回归通过。物品 `registryprobe:ice_ether` 已打包用户原始 16×16 RGBA PNG，字节与来源 SHA 严格一致；005-S 与资源静态验收 PASS，005-C 在用户 macOS arm64 受控人工验收中 PASS。详见 [005 报告](<Dev Manuals/Convergence-005 交付报告.md>)。

这是受控原型，不代表任意 Forge 模组兼容。一般 Forge post API、取消／优先级／自动／静态订阅、多 Forge 同时完整启动、一般 RegistryObject API、Block／自定义 Registry、Capability 和 Fabric 尚未验证或实现。受控真实投递不代表一般 Forge Event 系统兼容。历史 PASS 见交付报告；每轮运行以新生成的 `result.json` 和原始日志为准。

005-C 的 macOS 原生客户端部署准备见 [Mac kit](tests/registry/mac/README.md)：
只交付两个原 005-S JAR、Prism 精确版本和 Java/JVM 参数检查、安装回滚与
人工清单。用户已回传真实日志、截图及 /give／双语言人工确认，详见
[005-C 客户端验收](<Dev Manuals/Convergence-005-C 客户端验收与脚本修复.md>)。
修订脚本的 Linux 模拟测试及打包成功不代表其已在 Mac 实机复测。

## 环境与外部输入

| 组件 | 版本 |
|---|---|
| Minecraft / NeoForge | 1.21.1 / 21.1.256 |
| FancyModLoader / ModLauncher | 4.0.45 / 11.0.5 |
| Microsoft OpenJDK | 21.0.10+7-LTS |
| securejarhandler / NeoForge bus | 3.0.8 / 8.0.5 |
| ASM / asm-tree | 9.10.1 / 9.10.1 |
| slf4j-api / maven-artifact | 2.0.9 / 3.8.5 |
| 工具 | zsh、Python 3.11+；服务端运行另需 pgrep |

通过 `JAVA_HOME` 或 `--jdk-home` 选择 JDK。克隆仓库后仍需准备以下外部输入：

- 完整 NeoForge 服务端安装的 `libraries/`，包含官方 `net/neoforged/neoforge/21.1.256/unix_args.txt` 和 Minecraft 服务端。
- 原固定 Probe：`forgeprobe.jar`，SHA-256 为 `a774d2f737b17f089046014ab55a53767dbad008cd44dcce52c727b87e559ae8`。原回归入口拒绝重建或替代输入。
- 同一服务端输入下的 `config/`、`defaultconfigs/` 和 `spike000world/`。空的 defaultconfigs 也须保留目录；世界与第三方运行库不入 Git。

`tools/entry/prepare-unix.zsh` 只取得并校验八个固定编译 JAR，不安装完整服务端、不重建 Probe、不启动 JVM 服务端。旧兼容层 Gradle 记录已归档为 `history/compat-build.gradle.deprecated`，当前入口不使用 Gradle。

## 构建与回归

在仓库根目录执行。以下变量须指向你实际准备的输入：

```zsh
export JAVA_HOME='/absolute/path/to/msopenjdk-21'
DF_SERVER_INPUT='/absolute/path/to/server-base'
DF_PROBE='/absolute/path/to/forgeprobe.jar'

# 仅准备编译依赖；已有完整安装时也会核验现有文件。
zsh tools/entry/prepare-unix.zsh --server-input "$DF_SERVER_INPUT"
zsh tools/entry/forgecompat.zsh --mode build --server-input "$DF_SERVER_INPUT"

# 取上一步 result.json 中实际新制品路径，保留文件名。
DF_COMPAT_JAR='/absolute/path/to/new-run/forgecompat-002-0l.jar'

# 仅执行 metadata 边界，不启动服务端。
zsh tools/entry/test-forge-dependencies.zsh --stage after --boundary-only \
  --server-input "$DF_SERVER_INPUT" --probe "$DF_PROBE" \
  --compat-jar "$DF_COMPAT_JAR"

# 原 Probe 隔离 smoke；所有 JVM 命令必须串行。
zsh tools/entry/forgecompat.zsh --mode smoke \
  --server-input "$DF_SERVER_INPUT" --probe "$DF_PROBE" \
  --jar "$DF_COMPAT_JAR" --allow-server-start

# metadata + C/D/E/F 动态依赖正负例。
zsh tools/entry/test-forge-dependencies.zsh --stage after \
  --server-input "$DF_SERVER_INPUT" --probe "$DF_PROBE" \
  --compat-jar "$DF_COMPAT_JAR" --allow-server-start

# 003a 发现与边界测试。
zsh tests/discovery/run-discovery.zsh \
  "$DF_COMPAT_JAR" "$DF_SERVER_INPUT" "$DF_PROBE"
```

`--mode regress` 合并干净构建和原 Probe smoke；独立 `--mode smoke` 核验指定新 JAR。IdentityProbe 原生 Forge 编译、修改前复现、完整加载和范围对照的 zsh 命令见 [tests/identity/README.md](tests/identity/README.md)。

所有服务端运行需要 `--allow-server-start`。入口要求没有其他 Java 进程且 25565 可用；运行物理复制完整输入，绑定 127.0.0.1，停止后等待 JVM 退出。固定 server.properties 由内部实现直接生成，运行不读取旧脚本。dump 写入每轮新目录，原安装、输入和世界均复核指纹。

云端代理／CA 和 Authlib public-key relay 的实际参数见 [tools/UNIX.md](tools/UNIX.md)。当前云环境启动时还需传入 `--java-network-args /workspace/dualforge-cloud/java-network.args --public-key-relay`；普通本地环境不应照搬云端文件路径。

## zsh 入口与结果

| 入口 | 用途 |
|---|---|
| `tools/entry/prepare-unix.zsh` | 准备八个固定编译输入 |
| `tools/entry/forgecompat.zsh` | build／smoke／regress |
| `tools/entry/test-forge-dependencies.zsh` | metadata 与动态依赖回归 |
| `tools/harness/run-server.zsh` | 独立隔离服务端 harness |
| `tests/discovery/run-discovery.zsh` | 发现与候选边界 |
| `tests/identity/run-identity.zsh` | 原生 Forge 身份 fixture、before／after |
| `tests/identity/run-scope.zsh` | 实际 FML 扫描和转换字节范围对照 |
| `tests/events/run-events.zsh` | 004 原生夹具、字节契约、真实投递 C/T 和旧回归 |
| `tests/registry/run-registry.zsh` | 005 原生单 Item、ABI／范围、真实注册 C/T、资源与客户端状态 |

每轮输出在新建的 `out/` 子目录；`--output-root` 可选择仓库 out 下的其他位置。保存实际命令、工具版本、输入与 JAR entry SHA-256、完整日志、退出记录和断言，不覆盖原服务器制品。

统一构建／回归退出码：0 PASS、10 构建失败、20 启动失败、30 断言失败、40 NOT RUN；已完成构建但未能回归时为 PARTIAL。独立依赖入口的失败码保持 1，前置条件不满足为 40。

smoke 不只检查 Done，还检查精确安装制品、服务发现、构造 marker、非空 profile 的真实 register 返回、builtin javafml 委托及 NeoForge 原生加载、正常 stop／exit0、无 ERROR/FATAL/dump FAIL、空 stderr 和无 crash。依赖负例按具体 FML 拒绝原因判断，不能仅凭退出码或没有 Done。

## 源码、历史记录与证据

- 当前生产源码：[spike001a/compat-002-0l/src](spike001a/compat-002-0l/src)；原 Probe 源码：[history/spike000/probe-002-0l/src](history/spike000/probe-002-0l/src)。
- 当前 fixture：[tests/dependencies](tests/dependencies)、[tests/discovery](tests/discovery)、[tests/identity](tests/identity)、[tests/events](tests/events/README.md)、[tests/registry](tests/registry/README.md)。
- 历史交付：[Convergence-001](<Dev Manuals/Convergence-001 交付报告.md>)、[002](<Dev Manuals/Convergence-002 交付报告.md>)、[003a](<Dev Manuals/Convergence-003a 交付报告.md>)、[003b](<Dev Manuals/Convergence-003b 交付报告.md>)、[004](<Dev Manuals/Convergence-004 交付报告.md>)。
- 架构及冻结报告：[Dev Manuals](<Dev Manuals/>)。历史文本仍保留原平台命令与证据路径，作为当时的执行记录，不作为当前入口。

此前 zsh 工具整理移除了全部 29 个受 Git 管理的 Windows／PowerShell 脚本；该轮原字节副本和指纹存于外部归档 `out/zsh-only-tooling/`，也可从此前 Git 提交恢复。此前仓库结构整理仅使用 git mv 归档现有文件，未删除文件。历史 Java 源码、资源、报告及已有证据索引保持原字节，不批量改写历史引用，也不重写 Git 历史。

Git 保存第一方源码、fixture、zsh 入口、内部实现、报告和指纹索引；完整运行附件、第三方依赖、构建产物、世界、缓存及凭据继续外部保存。`.gitattributes` 保留原文件换行与字节。既有 LICENSE 未修改。
