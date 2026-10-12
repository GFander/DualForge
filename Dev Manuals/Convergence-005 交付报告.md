# Convergence-005 交付报告

结论：**真实服务端注册检查 PASS；完整 005-S PARTIAL；005-C NOT RUN。**
原生 Forge 输入的 `registryprobe:ice_ether` 在 T 中进入真实 Item Registry，
C 中不存在。原始 PNG 只有内嵌预览、没有可读取文件；未重画替图，完整
资源 Gate 1 未完成。当前环境无 DISPLAY、WAYLAND_DISPLAY、/dev/dri 或
已安装图形客户端，没有客户端加载／渲染证据或真实游戏截图。

本报告记录当前 Linux 动态实验，不证明原 Windows 入口、任意真实模组、
多 Forge 同时启动或一般 Registry API 兼容。完成本轮后停止，不进入 006。

## Git 和实际工具链

已 fetch，PR #6 于 2026-10-12 16:30:55 UTC 合并。
从最新 main `20e69fae7a07c6a5b74f017c29762f414834814c` 建立独立 worktree
`/workspace/DualForge-005`，分支 `codex/convergence-005-registry-item`。
原 004 worktree 未修改。起点工作区干净，快照保存 524 个路径。

本轮提交（文档交付提交 SHA 见 PR 和最终回复）：

- `c2285137ccaab68c37e488f7bb0b9299eb8050a5`：原生 fixture、ABI 与基线拒绝。
- `fc15b6231b62523392bd0d888bf888dbb16ccb4a`：唯一生产功能修改。
- `27c6b0b65b250039ce1a7fde72b9e904c5e46f6e`：字节／原子性、回归和制品绑定 runner。
- `56cb1a78bb18e8019b44f3bfdbf42af2869148bf`：C/T Java、宿主与配置绑定。

实际执行版本：Minecraft 1.21.1、NeoForge 21.1.256、Forge 52.1.0、
Forge bus 6.2.27、FML 4.0.45、ModLauncher 11.0.5、Microsoft OpenJDK
21.0.10+7-LTS (Microsoft-13106412)、zsh 5.9、Python 3.12.14；
securejarhandler 3.0.8、NeoForge bus 8.0.5、ASM/asm-tree 9.10.1、
slf4j-api 2.0.9、maven-artifact 3.8.5。真实版本输出、命令和全部依赖
指纹保存在各阶段，不使用旧报告替代本轮取证。

## Gate 0 / Gate 2：先取证，再修改

详细完整 descriptor 表和生命周期见 [ABI.md](../tests/registry/ABI.md)。
Forge `.create(IForgeRegistry,String)` 对应 Neo `.create(Registry,String)`；
ForgeRegistries.ITEMS 的 IForgeRegistry 字段对应 BuiltInRegistries.ITEM 的
DefaultedRegistry 字段，后者实现 Registry。`register(String,Supplier)` 的
返回类型由 RegistryObject 变为 DeferredHolder，必须同步字段声明、泛型
签名、GET/PUTSTATIC 和 get 方法 owner。get 的擦除 descriptor 均为
`()Object`，原 caller 的 Item CHECKCAST 保留；没有 holder 强转或 Shim。

javap 确认 FMLModContainer 构造独立 MOD bus，并将同一 eventBus 注入唯一
IEventBus 构造参数。GameData 在解冻后通过 ModLoader 的官方容器顺序
发出 RegisterEvent，DeferredRegister 匹配 key、调用 supplier 并绑定 holder，
随后宿主冻结注册表。这些是静态契约；实际 T 栈另行验证了该链。
Supplier 是原 javac 产生的零捕获 LambdaMetafactory，返回普通 Minecraft
Item。bootstrap 中没有需要映射的 Forge 类型；严格核对后保留原字节。
原 Forge registry 实现未作为宿主运行库安装；GAME 层实际解析的是映射后的
NeoForge registry owner，T 的真实运行而非静态猜测证明可见性。

干净 004 compat SHA-256：
`3e4aa3865cd20df968cce7e401d3fe8624d18b86cb829a88d2467b781e716790`。
基线运行 `out/convergence-005/baseline/20261012-165100-fef8857b/` 先经过
一次发现、Reader、真实 `{registryprobe}` metadata，再在 targets() 失败：

```text
IllegalStateException: 0l expected one same-file event profile for com.example.registryprobe.RegistryProbe
at ForgeProbeConstructorTransformer.targets(ForgeProbeConstructorTransformer.java:332)
```

没有进入 GAME 构造、注册或 Done。基线 assertion PASS 表示复现拒绝，
不表示注册成功。首轮 `20261012-165006-1875ef40` 的 runner 只搜 stdout，
未读真正含异常的 stderr，断言 FAIL；修正后重跑，两个原始输出均保留。
原始输出冻结后才修改生产代码。后续重复干净构建改变 JAR 时间戳，
所有原始 fixture 条目字节与最终输入一致，见 fixture-content-preservation.json。

## 生产范围与原生输入

只修改生产 Transformer，新增 ControlledForgeItemProfile；未修改 Locator、
Reader、Parser、依赖适配、Module Layer、SPI、总线行为或宿主核心。
targets 仍来自所属 modId 的真实 Forge @Mod 扫描，同文件读取入口字节，
识别无 Event 的严格单 Item profile，只注册该入口为目标。没有包通配、
身份白名单或对所有 Forge owner 的全局 remapping。旧 Event 分支未放宽。

新 profile 精确验证两字段、四方法、原单参数构造器、开关分支目标和 bus
参数位置、一次 DeferredRegister 创建、一次注册、同入口零捕获 Lambda、
marker 后普通 Item 构造和 get-only accessor。类、字段、modId、入口名从
扫描和实际结构推导。先验证 ClassNode 副本，后进行有限 owner/descriptor
修改；不调用真实注册表 API、不提前触发事件、不吞异常、不伪造结果。

Forge 输入由保存的 Forge 源码干净编译；classpath 只有三份固定 Forge
JAR、未打 Forge/NeoForge 补丁的 remapped vanilla Minecraft 及 vanilla
依赖。扫描整个 Minecraft 编译 JAR 的 class，未发现 Forge/NeoForge owner；
原 fixture class 不含 NeoForge 或 compat 引用。driver 单独用 NeoForge
编译，只有公开 API 查询和 supplier println 点栈观察，不执行 Item 注册。

最终 fixture / driver：`out/convergence-005/fixture/20261012-170706-e09009d1/`。

| 输入／资源 | SHA-256 |
| --- | --- |
| 最终 compat | `ed572ca9762e235e7d264355d35b3c9d3ab9ecf1da3f2c3234b1e28056aad752` |
| 原生 Forge registry-probe.jar | `5ead02f95ca4c19f74d7d944a7035cb467a38816495f1eb42b0024f9fc9b47af` |
| 原生 NeoForge registry-driver.jar | `c418424b5dfd1e0a227bd1ccf6097206092eb3a550633de8dfa449a7135f9730` |
| ice_ether.png | UNKNOWN：原文件不可读取，未打包替代图 |
| models/item/ice_ether.json | `c1a59acc99caf2bb7aafbe072d7fe39f928ee88dbd3661cbdbfa14dd97422563` |
| lang/en_us.json | `c6846d4dd0afa6c97363bdcd65bfa8e8bc51656f79e692545037f5778f587f65` |
| lang/zh_cn.json | `8905a599dee672be2691b8a47a7047b3d0027333f8a335ac5fdd65f7aae3052a` |

构建的 16 个生产 Java／资源指纹逐一与生产提交 fc15b62 比对一致；其后
只有测试及文档变化，生产 Java 字节未再修改。旧回归运行于 27c6b0b，
56cb1a7 仅增加 registry C/T 环境判定，已重跑受影响 C/T 和自动负例。
最终旧能力使用的 compat 与新能力完全一致，不虚构未重跑的生产构建。

## 动态结果与转换对照

最终 C：`out/convergence-005/control/20261012-171305-4d99c1f2/`。
最终 T：`out/convergence-005/treatment/20261012-171332-1143201d/`。

| 检查 | C / false | T / true |
| --- | --- | --- |
| containsKey / getOptional | false / empty | true / present |
| 原 supplier 实际调用 | 0 | 1 |
| 原 Forge attach 调用 | 跳过 | 正常返回 |
| Item 类型／key／AIR | 不存在，未 get holder | Item / registryprobe:ice_ether / false |
| DeferredHolder.get 对象身份 | 未查询未绑定 holder | 与真实 registry 值同一对象 |
| 正常 Done、stop、exit0 | PASS | PASS |
| ERROR/FATAL/dump FAIL/stderr/crash | 无 | 无 |

入口注入 bus 的 identityHashCode 与其实际 FMLModContainer.getEventBus()
一致；driver 直接核对该 bus != NeoForge.EVENT_BUS。结合宿主注入字节码
及严格 ALOAD1 attach 规则，证明使用 MOD bus。查询发生在安全的
ServerStartedEvent；使用 containsKey 和 getOptional，避免 AIR 默认回退。

T 在未改写 supplier println 点捕获的真实栈依次包含原 lambda、
DeferredRegister、RegisterEvent.register、DeferredRegister.addEntries、
EventBus.post、ModContainer.acceptEvent、ModLoader 官方容器顺序 dispatch、
GameData.postRegisterEvents 和 CommonModLoader。driver 不调用 supplier
或直接注册 Item。日志 marker 和 attach 返回不是单独的成功依据。

同三份 JAR、实际 Java 及版本输出、完整原宿主输入指纹、所有真实
server.properties 属性、公共 JVM 参数和网络适配一致。只改变
DF005_REGISTER；物理目录、dump 路径、生成时间戳注释自然不同。
自动汇总绑定 Contract/C/T/Regression compat SHA，还核对 fixture、driver、
runtime checker 及资源的输入归属。6 个自动测试覆盖每个阶段错 SHA、
缺 SHA、排序指纹角色误标、旧 fixture/driver/runtime/资源和宿主配置错配。

最小字节对照通过：只发生授权构造 descriptor、字段与调用 owner/descriptor/
generic signature 修改；supplier 指令及 metadata 字节一致；实际 runtime
dump 等于生产 transformer 输出。非法额外注册、错误字段、未知 bootstrap、
外部 lambda owner 和错误 bus 参数明确拒绝，原 ClassNode 字节完全不变。
未认领同形类和原生 metadata 下相同 Forge 入口字节不被转换。

## 原回归、资源和保全

新 compat 上实际运行全部 PASS：原冻结 ForgeProbe 严格 smoke、002 metadata
边界及 C/D/E/F 动态依赖、003a discovery/边界、冻结 003b IdentityProbe
加载与范围、004 字节／原子性与真实事件 C/T。负例 D/F 按明确 FML
required missing/version mismatch 拒绝原因验收，均 exit0 也没有误当成功加载。
004 的 register/post 返回与回调次数仍使用原断言，没有放宽旧能力。

模型与两个 lang JSON 实际在同一 Forge JAR 中且静态校验通过；PNG 缺失，
其格式、32×32、RGBA、透明/可见像素与 SHA 都 NOT RUN。未执行真实客户端
启动、/give、背包／手持、语言切换或渲染检查，没有截图。完整重跑和
人工客户端步骤见 [tests/registry/README.md](../tests/registry/README.md)。
收到原 PNG 后须重新打包原生 fixture，按新 SHA 重跑受影响 Contract/C/T/assets；
不能与当前无 PNG JAR 的结果混为完整 PASS。

全部 JVM 严格串行；每轮物理复制原安装和世界，使用既有 Linux 网络/dump
适配。初始 524 路径中授权修改的 5 个现有文件之外 519 路径指纹不变；
所有历史源码、冻结报告、原输入、原世界及库均未覆盖。新生产文件和
tests/registry 是本轮新增。最终无 java/javac，25565 可绑定。

## 证据位置与限制

证据索引：[Convergence-005 证据索引.json](<Convergence-005 证据索引.json>)，
记录每项真实路径和 SHA。所有原始命令、版本、源码快照、JAR 内容、
日志、退出和断言仍在 `/workspace/DualForge-005/out/convergence-005/`；
旧回归本轮输出另在当前 worktree 的 out/convergence-004、003a、003b。

最小原始 C/T 归档：
`/workspace/DualForge-005/out/convergence-005/convergence-005-ct-minimal.tar.gz`，
105206 字节、27 文件，SHA-256
`172005c32f609e3818706e64862b12075ee0d6130a3f7ceb297bc44e45d53798`。
内容为日志、命令、metadata、断言和指纹，不含世界、依赖/制品二进制或
凭据；保留现场工作区路径。**仅本地保存，未上传，无外部下载 URL。**
此前 Gist 授权仅针对 004 的具体载荷，未冒用作本轮上传许可。

剩余阻塞：原 PNG 文件；真实图形客户端环境。没有发现服务端技术 STOP，
但不能给完整 005-S 或 005-C PASS。创建 Draft PR，推送后核对远端 SHA；
不自动合并、不发布、不进入下一阶段。通用 RegistryObject、更多物品/
Block/custom Registry、复杂 supplier、一般事件 API、其他构造器 ABI 均未支持。
