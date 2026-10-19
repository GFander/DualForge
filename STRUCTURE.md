# 仓库结构

唯一生产源码为 `dualforge/src/main/`，制品 `dualforge.jar`。功能入口见 [README](README.md)、[tests](tests/README.md)、[tools](tools/UNIX.md)；按功能查文档见 [docs](docs/README.md)。

```text
DualForge/
├── dualforge/src/main/
│   ├── java/                       活跃：唯一生产Java
│   └── resources/                  活跃：Manifest与SPI
├── tests/
│   ├── real_mods/jei/               活跃：010A冻结取证；dual_bus/为010B对照；constructor/为010C有限Locale/配置接口组合，原JEI完整入口仍拒绝
│   ├── real_mods/mouse_tweaks/      活跃：009C客户端有限事件/配置工厂、完整SDK链接、隔离模型及新JAR服务端回归
│   ├── real_mods/clumps/           活跃：008A原始发行取证、有限事件初始化Profile、原生/Neo经验球对照与SSD运行
│   ├── helper_registry/            活跃：007C-4B同ModFile Helper/Consumer计划、Registry桥接及独立原生Forge/Neo C/T
│   ├── supplier_boundary/          活跃：4C-1 Supplier类型/依赖、业务保留及三组原生Forge/Neo C/T
│   ├── global_bus/                 活跃：4C-3官方Global Bus语义、单一组合计划、真实派发及严格负例
│   ├── typed_registry/             活跃：4C-2 Item/Creative Tab类型闭包、延迟回调、跨类Holder与Bridge诊断
│   ├── api_atlas/                   活跃研究：007C-4A离线ABI、映射与ASM/JVM验证，不启动游戏
│   ├── real_mods/food_n_fuel/       活跃：007C冻结发行身份/原生对照与标准运行
│   │   ├── behavior/               活跃：4C-5真实熔炉tick、原生Tab/Food对照及精确SNBT收据
│   │   ├── runtime/                活跃：4C-4原始FnF标准服务端实验及独立原生只读注册查询
│   │   ├── metadata/               活跃：007C-1标准TOML、注解归属/准入负例及诊断SPI
│   │   └── dependency/             活跃：007C-2平台契约、独立Forge夹具、串行零参数依赖矩阵
│   ├── dependencies/               活跃：002 metadata / CDEF
│   ├── discovery/                  活跃：003a 发现与边界
│   ├── identity/                   活跃：003b 入口与范围
│   ├── events/                     活跃：004 事件C/T
│   ├── registry/                   活跃：005 Item / assets
│   │   ├── mac/                    当前命名工具
│   │   └── mac-legacy/             Legacy固定旧cohort，原字节
│   ├── dropin_items/           活跃：007B无业务开关Forge输入、范围/模块拒绝、零参数Mac包
│   ├── bootstrap/              活跃：007A标准启动、默认配置与原生共存
│   ├── entry_context/              活跃：007C-3入口分类、原生Forge Context夹具、归属/Bus身份与原子性
│   ├── block_registry/             活跃：006B Block/BlockItem ABI、Contract、C/T
│   │   ├── client_resources/       活跃：006B-C方块模型/用户贴图，实机验收独立
│   │   └── client_translations/    活跃：冰之以太三组名称覆盖，不修改冻结模组
│   ├── multi_registry/             活跃：006A多Item与ABI/原子性
│   ├── client_objects/             活跃：最新实际ID统一的Forge输入与独立客户端包
│   └── project_identity/           活跃：身份及精确历史迁移保护
├── tools/
│   ├── api-atlas/                  活跃研究工具：固定发行扫描/候选/调用图/离线原型，非生产转换器
│   ├── entry/                      活跃zsh构建/依赖入口
│   ├── lib/                        Python内部实现
│   └── harness/run-server.zsh      活跃独立物理复制harness
├── runtime/neoforge-server/
│   ├── config/                     活跃：三个tracked原配置
│   ├── libraries/                  ignored / 外部依赖
│   ├── defaultconfigs/             ignored / 外部输入，空目录也保留
│   └── spike000world/              ignored / 原世界名不变
├── history/
│   ├── early-spikes/
│   │   ├── forge-probe-000/         归档：早期Probe源码/Gradle
│   │   ├── discovery-001a/          归档：metadata/Locator早期副本
│   │   └── modinfo-001b5/           归档：metadata契约探针
│   ├── compat-002-0d/              原字节历史候选
│   ├── compat-002-0g/              原字节API迭代
│   ├── compat-002-0j/              原字节注册迭代
│   ├── compat-002-0l-archive/       原字节冻结副本
│   ├── compat-build.gradle.deprecated
│   └── spike000/                   原Probe/订阅实验及冻结记录
├── docs/                          活跃：功能导航及runtime迁移说明
├── Dev Manuals/                   冻结事实、报告、原始证据索引
├── .github/workflows/ci.yml        静态/单测CI
├── out/                           ignored：每轮新日志/制品/断言
├── README.md
├── AGENTS.md                       完整六节Git规则
└── LICENSE                         原样保留
```

## 本轮精确路径迁移

基线main `40577b6866b49952769caf2951dfbb231900b4e9`。所有26个tracked文件使用git mv；23个早期文件及3个运行配置原字节不变。完整逐文件SHA见 [structure_relocation.json](tests/project_identity/structure_relocation.json)。

| 原位置 | 当前位置 |
|---|---|
| spike000/probe/ | history/early-spikes/forge-probe-000/ |
| spike001a/compat/ | history/early-spikes/discovery-001a/ |
| spike001b5/probe/ | history/early-spikes/modinfo-001b5/ |
| spike000/neoforge-server/config/ | runtime/neoforge-server/config/ |

原命名迁移索引仍记录原现场路径；保护器将授权项定位到新位置，严格核对原SHA和一对一目标。374项保护不减少，禁止目录级豁免。未迁移项继续要求原位置存在。

早期DEPRECATED中的旧命令保留原文，仅为历史记录；[归档说明](history/early-spikes/discovery-001a/DEPRECATED.md)。历史Java包、Gradle、TOML、SPI、指纹不改，不重新构建冻结JAR。

## 当前构建、测试与运行输入

`zsh tools/entry/dualforge.zsh --mode build` 使用新默认 `runtime/neoforge-server`。`--server-input`仍支持外部路径。依赖入口 `tools/entry/test-forge-dependencies.zsh`，独立harness `tools/harness/run-server.zsh`，具体测试见 [tests/README](tests/README.md)。
完整运行库和原世界必须外部准备；本地复制步骤见 [docs/runtime](docs/runtime.md)。原A回归只使用SHA匹配的原JAR，默认位置仍为 `history/spike000/probe-002-0l/build/forgeprobe.jar`，可用 `--probe`指定外部制品。绝不重建替代。

所有JVM串行、运行使用物理副本；旧回归显式dump到每轮out；007A默认关闭dump，不再使用历史Windows fallback。运行数据、第三方库和原日志不入Git；不删除旧安装。

## 历史路径追溯

先前归档的 `spike001a/compat-002-0d/0g/0j`、`compat-002-0l/archive` 分别位于 `history/compat-*`；旧 `spike000/logs`、`probe-002-0e/0l`、`subscriber-002-0n` 位于 `history/spike000`。旧run-server入口在tools/harness；旧tools顶层入口和内部实现分别在tools/entry和tools/lib。命名迁移后的唯一生产目录为dualforge。

历史报告内绝对路径、实验命令及SHA不重写。需要复核原现场时使用报告的Git提交；需要当前位置时按精确映射导航。历史实验不提供并行生产版本，也不代表当前兼容能力。

006A-S原冻结服务端fixture与006A-C Beta资源更新版分别保存；后者用户macOS arm64受控人工PASS，不继承其他cohort或平台。详见 [客户端报告](<Dev Manuals/Convergence-006A-C macOS 客户端人工验收报告.md>)。
