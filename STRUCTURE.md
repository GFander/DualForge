# 仓库结构

唯一生产版本是 `spike001a/compat-002-0l/src/`。目录中的历史编号不表示
当前构建可选多个 compat 版本；工具只编译这里的 Java 与资源。

```text
DualForge/
├── spike001a/
│   ├── compat-002-0l/src/main/
│   │   ├── java/                         活跃：唯一生产 Java
│   │   └── resources/                    活跃：manifest 与 SPI
│   └── compat/
│       ├── src/、archive/                历史：保留原位置的早期副本
│       └── DEPRECATED.md                 弃用说明
├── spike000/
│   ├── neoforge-server/config/            活跃：服务端环境配置
│   └── probe/                            历史：保留原位置的早期 Probe
├── spike001b5/probe/                      历史：保留原位置的 metadata 契约探针
├── tools/
│   ├── entry/
│   │   ├── forgecompat.zsh                活跃：build / smoke / regress
│   │   ├── prepare-unix.zsh               活跃：八个编译依赖的准备
│   │   └── test-forge-dependencies.zsh    活跃：依赖 metadata / 动态回归
│   ├── lib/
│   │   ├── unix_workflow.py               活跃：构建、运行、指纹与断言
│   │   └── prepare_unix.py                活跃：依赖下载与校验
│   ├── harness/run-server.zsh             活跃：独立隔离服务端 harness
│   └── UNIX.md                           当前运行环境说明
├── tests/
│   ├── README.md                         五类测试的统一说明
│   ├── dependencies/                     活跃：TOML 夹具和 FML metadata 契约
│   ├── discovery/                        活跃：实际发现 pipeline 与边界
│   ├── identity/                         活跃：原生 Forge 身份夹具与范围对照
│   ├── events/                           活跃：004 受控事件投递、C/T 和阶段一致性
│   └── registry/                         活跃：005 原生 Forge 单 Item、注册 C/T、资源检查
├── history/
│   ├── compat-002-0d/                    归档：旧生产候选及冻结副本
│   ├── compat-002-0g/                    归档：旧 API 转换迭代
│   ├── compat-002-0j/                    归档：旧注册路径迭代
│   ├── compat-002-0l-archive/             归档：0l 的 src-before / src-after
│   ├── compat-build.gradle.deprecated    归档：原字节 Gradle 实验记录
│   └── spike000/
│       ├── logs/                         归档：第一方源码和冻结说明
│       ├── probe-002-0e/                 归档：早期 Forge Probe
│       ├── probe-002-0l/                 归档：原 A 的源码及冻结副本
│       └── subscriber-002-0n/            归档：独立订阅语义实验
├── Dev Manuals/                          历史说明书、报告和证据索引
├── out/                                  外置／Git 忽略：每轮新输出
├── README.md                             当前使用说明
└── AGENTS.md                             Git 交付与结构约定
```

## 活跃入口和输入

从仓库根目录执行 `zsh tools/entry/forgecompat.zsh --mode build`。
其他构建／依赖参数见 [README.md](README.md) 和
[tools/UNIX.md](tools/UNIX.md)，测试入口见 [tests/README.md](tests/README.md)。

`tools/harness/run-server.zsh` 仍是活跃独立入口；它调用
`tools/lib/unix_workflow.py` 的 harness 模式，没有归档为历史脚本。
三个 tools/entry 入口同样只调用 tools/lib 中的实现。Python 实现按其
新位置计算仓库根目录，生产 SOURCE 路径保持不变。

`spike000/neoforge-server/` 继续作为默认服务端输入位置，完整运行库、
原配置、defaultconfigs 和世界需外部准备，不因归档历史源码而迁移。
原 A 的源码在 history/spike000/probe-002-0l，但回归使用的是原制品，
不会重建它。默认原制品路径相应为
`history/spike000/probe-002-0l/build/forgeprobe.jar`；也可通过 `--probe`
指定外部原 JAR，其固定 SHA-256 校验不变。build/ 和 JAR 仍不入 Git。

所有 tests 文件保持原位置。IdentityProbe 在 tests/identity/src 内从原生
Forge 源码编译；dependencies 的动态夹具只替换原 A 的 TOML；discovery
通过实际 FML pipeline 验证候选交接。events 的独立原生 Forge fixture 与
测试型 NeoForge driver 验证一次真实受控投递；阶段一致性自动负例不启动 JVM。
入口为 `zsh tests/events/run-events.zsh`，参数与证据布局见
[tests/events/README.md](tests/events/README.md)。归档源码不参与生产编译。

registry 的 Forge 输入位于 `tests/registry/src/`，原生 NeoForge 查询／栈观察
driver 位于 `driver/`；拒绝及原子性对照在 `RegistryBytecodeTest.java`。
入口 `zsh tests/registry/run-registry.zsh` 复用现有 Python harness。
单 Item ID 为 `registryprobe:ice_ether`；用户原始 16×16 PNG 已原字节打包，
服务端与资源静态验收 PASS，真实客户端仍 NOT RUN。阶段、输入和重跑方式见 [tests/registry/README.md](tests/registry/README.md)。

## 新旧路径映射

本轮基线为 `4078888fb13958c61829c4041f832b88164a7c01`，所有已有文件的
位置变更使用 `git mv`。以下映射适用于当前仓库文件及入口：

| 旧路径 | 当前路径 |
|---|---|
| `spike001a/compat-002-0d/` | `history/compat-002-0d/` |
| `spike001a/compat-002-0g/` | `history/compat-002-0g/` |
| `spike001a/compat-002-0j/` | `history/compat-002-0j/` |
| `spike001a/compat-002-0l/archive/` | `history/compat-002-0l-archive/` |
| `spike000/logs/` | `history/spike000/logs/` |
| `spike000/probe-002-0e/` | `history/spike000/probe-002-0e/` |
| `spike000/probe-002-0l/` | `history/spike000/probe-002-0l/` |
| `spike000/subscriber-002-0n/` | `history/spike000/subscriber-002-0n/` |
| `spike000/run-server.zsh` | `tools/harness/run-server.zsh` |
| `spike001a/compat/build.gradle` | `history/compat-build.gradle.deprecated` |
| `tools/forgecompat.zsh` | `tools/entry/forgecompat.zsh` |
| `tools/prepare-unix.zsh` | `tools/entry/prepare-unix.zsh` |
| `tools/test-forge-dependencies.zsh` | `tools/entry/test-forge-dependencies.zsh` |
| `tools/unix_workflow.py` | `tools/lib/unix_workflow.py` |
| `tools/prepare_unix.py` | `tools/lib/prepare_unix.py` |

历史报告、索引中的现场绝对路径、命令和指纹仍描述原执行时的输入，
不全局重写。需要定位当前仓库文件时使用上述映射；需要复核原提交
时使用基线或报告记录的 Git SHA。Java 内的旧 dump 默认路径也保留
原字节，当前 harness 已通过 JVM 属性将输出定位到每轮隔离目录。

`spike001a/compat/` 的 src/archive 和不带版本后缀的 `spike000/probe/`
按本轮确认范围保留原位置，均为遗留记录；前者说明见
[DEPRECATED.md](spike001a/compat/DEPRECATED.md)。这两个目录及其中的
旧 Gradle 记录不提供当前构建入口。
`spike001b5/probe/` 的早期 metadata 契约探针同样保留原位置，不属于
当前 dependencies/discovery/identity/events/registry 测试入口。

## 保全与输出

历史 Java、资源、manifest、指纹文件和冻结说明只移动位置，保持原
字节。不删除文件，不迁移世界，不将历史目录误作活跃版本。
忽略规则继续排除完整运行附件、JAR、class 和依赖；归档 logs 中
的第一方 Java 和说明文件有明确例外。

新构建、日志、断言和 dump 保存于当前仓库 out 下的全新目录，
不覆盖旧证据。所有 JVM 验证串行，运行使用物理副本。
