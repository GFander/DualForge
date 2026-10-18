# Convergence-010B 多Bus身份与事件语义适配交付报告

日期：2026-10-18。**整体 PARTIAL**。已实现有限、可复用的官方双 Bus / 显式 Consumer / 布尔配置闭包；原始 JEI 完整入口仍未准入。未制作 010B JEI 客户端 ZIP，未请求用户执行 Windows 验收。

最终候选：`build-current/dualforge.jar`，224219 bytes，SHA-256
`7eb452be98d70cda6cabf31e20aa8070dc6ac73664366c61cb20563f15dc2afc`。
生产来源由 `source-current.json` 的逐文件 SHA 绑定；提交后再次核对。最终 Git HEAD / PR / CI 的外部收据另存 `delivery.json`，避免报告自引用提交 SHA。下述路径均相对于 SSD `/home/albert/Env/DualForge/runs/convergence-010b/`。

## Git 与历史保全

实际远端 main 为 `e47423aa2a48e02b128098a03d5486ad8faa37f8`，009C PR #33 已合并。010A 原 #34 只进入 009C 分支，未进入 main。实际核对 main/010A 的生产目录相同，010A 独立增量仅研究、测试与文档；为原研究分支创建 [Draft PR #35](https://github.com/GFander/DualForge/pull/35)，base main、head `5459bec261610769c6c1901b5fdeb5f999f85b45`。它仍 OPEN/DRAFT，未合并；真实 CI [39102354470](https://github.com/GFander/DualForge/actions/runs/39102354470) SUCCESS。

本任务独立 SMB worktree：`/home/albert/Codex/DualForge-010B`；分支 `codex/convergence-010b-dual-bus-config`；起点 010A HEAD `5459bec...`。首实现提交 `66ebed343b4ff011bfe6e077dad5539aa428f4e8`；生产最终源提交 `438cbd3e52495c31cba04b27acfc0dc5eeede91e` 修正真实 JEI 拒绝定位并补强负例；与 source-current 的全部生产指纹一致。010B PR 应堆叠于 `codex/convergence-010a-jei`；未经授权不合并、不强推、不发布。

010A/009C 原交付报告及索引按接手 SHA 保持原字节；374 项保护持续检查。主 worktree 用户未跟踪的 `Convergence-008A-Windows客户端人工验收报告.md` 保留，SHA `ba9a4006253f7bfbdcfee0a7c3632adaad5db7d4215b474f4a17186373b504bf`。原 Probe 仅使用冻结 SHA 制品，未重建。

## Gate 1：原始输入与真实 ABI

| 输入 | 本轮重新核对 |
|---|---|
| Forge JEI 19.27.0.340，1529402 bytes | `f5e61a001d57dd044d6f2ba255daf955c50d90e4173f8b4933ab0eb8cd386ed5` |
| 同版本 Neo JEI，1529391 bytes | `8aaf547432f1b4958239b036356b910692fe40f858c3073d996f56bbf7c99826`，只读参考、未安装替代 |
| MC / Forge / Neo / FML / JDK | 1.21.1 / 52.1.0 / 21.1.256 / 4.0.45 / Microsoft 21.0.10+7-LTS |

`collect_abi.py` 重新核对全部 923+923 ClassFile SHA，原 010A ASM 扫描逐类与实际 JAR 原字节匹配后才复用其指令坐标；另用固定 JDK 做新 javap。59 个相关调用点、注册方法完整指令与 Bootstrap 候选保存在 `jei-abi-final/bus-abi.json`；详细声明、代码偏移和 BootstrapMethods 保存在同目录 javap 原件。候选类型和静态观察不构成生产授权。

[可反查的来源图](../tests/real_mods/jei/dual_bus/JEI_BUS_SOURCE.md)包含 ClassFile SHA、方法描述符、ASM index / 字节偏移、Class 参数及 Consumer capture/target。实际顺序是 LocaleSupplier/Translator、GAME、MOD、PermanentEventSubscriptions、ServerConfig、NetworkHandler、ClientSafeRunner、DistExecutor。不能删除前缀或后续业务来适配模板。

PermanentEventSubscriptions 保存两个真实参数引用，以实际 IModBusEvent marker 判断路由，显式 addListener(NORMAL,false,Class,Consumer)。RuntimeEventSubscriptions 则限定 Game 域并持有 EventSubscription 列表；后者创建 JEI 自己的 WeakConsumer，注销 registeredListener 包装对象。新有限 profile 不声称实现这套动态弱引用生命周期。

真实 ServerConfig 的 Builder 定义三项 boolean，defaults true/true/false，build 后经 ModLoadingContext 注册 SERVER，字段是 Supplier<Boolean>。其工厂返回 IServerConfig，区别于本轮有限夹具的具体返回形状。Forge IConfigSpec 与 Neo IConfigSpec 不同，不能直接强转。新旧接口与宿主真实 ModContainer.registerConfig 的描述符均留有新 javap。

## Gate 2/5：实现与架构边界

```text
原始 Forge 两条 Bus 获取及传递链
    ↓ 同 ModFile 的不可变完整字节码计划
ForgeBusIdentity：实际 GAME / 逐容器 MOD 对象及可信归属
    ↓
ForgeEventSemantics：固定类型、事件域、NORMAL、取消选项
    ↓
ForgeDualBusBridge：真实 Native Bus / ModContainer 配置调用
```

新增四个生产类；EntryProfile / Transformer 只增加独立 DUAL_BUS 分支和整组发布。旧 Registry、Context、Global、EventInitializer、ClientEvent 验证器未放宽。初期候选分类误认 FnF 被旧实际契约捕获，已收紧为还必须存在双 Bus 参数构造链，并加入专门保全断言；失败证据未删除。

- **Identity** 保存实际 game/mod 引用、weak ModContainer、所属 ModFile、Module、ClassLoader、既有 GAME layer。验证真实 loader、扫描入口、active container 和阶段。GAME 读取官方 NeoForge.EVENT_BUS；MOD 取该容器 getEventBus，不新建 Layer、Game Bus 或统一 Mod Bus。
- **Semantics** 仅 FMLCommonSetupEvent / ServerStartedEvent / LivingDeathEvent。类型须是固定宿主实际 Class；Mod marker 与取消能力须匹配。NORMAL，receiveCanceled 仅在已验证可取消事件使用。取消方法转为真正 ICancellableEvent 接口调用；不造空事件或固定返回值，不适配 Source post/Event.Result。
- **Immutable plan** 只接受三类闭包的已验证构造/路由/布尔 Spec 数据流。任意 modId、包/类名、配置名称及 boolean 默认值均按输入证明，不使用夹具/JEI 身份白名单。Class/Consumer 的 erased/instantiated MethodType、captured entry、具体回调描述符、字段绑定与顺序都校验。整组克隆、渲染、验证后才发布，逐类再核对原始 SHA / normalized bytes；冲突、未认领或部分输入拒绝。
- **Host** 直接传递原实际 Consumer，保留 native Event 对象、业务方法和线程。禁止迟到/重复初始化、重复/乱序注册。注销校验实际 Consumer、Class 与原 Bus。静态缓存 key/value 均弱，活 entry/router 的私有 anchor 保留会话，不通过静态 value→owner 强引用环保活。
- **不可回滚** native 调用失败标 UNCERTAIN，原异常传播；不承诺撤回已执行副作用。事件注册记录与配置 once-only 标记分开，只有不可变归属共享；可变状态不能另选配置 owner。

只读参考及明确采用/不采用项见 [REFERENCES](../tests/real_mods/jei/dual_bus/REFERENCES.md)：Kilt `8f008c5...`、FFA `204211b...`、Architectury `321ad30...`。借鉴逐模组来源与语义分层；不移植 Kilt 的 fallback Bus、Fabric invoker 或仅凭 modId 的授权。未复制实现代码。

`CUSTOM_BUS_SUPPORT=NOT_IMPLEMENTED`。任意事件/配置 Spec、优先级、bootstrap、未知构造器效果、JEI WeakConsumer 动态订阅、网络、Dist 和新增客户端生命周期均未泛化支持。

## Gate 3：真实配置契约

原 Forge Builder `push/comment/define(String,boolean)/pop/build` 经过生产者数据流验证后构造实际 Neo ModConfigSpec，保留原 Supplier<Boolean> 字段和读取方法；实际 native spec 交给所属 ModContainer.registerConfig。不是把外来 IConfigSpec cast 成 native 类型，也不是 NoOp。

COMMON/SERVER 的默认值、已修改值、正常保存/停止、重启读取、再次不修改重启恢复分别验证。Alpha COMMON 与 Beta SERVER 使用独立容器、Spec/value 和文件。Type 常量保持原枚举语义；CLIENT 枚举映射存在，但真实加载/保存/同步为 NOT_RUN。

固定 Forge 默认 SERVER 文件在 `world/serverconfig/`，固定 Neo dedicated 默认在 `config/`。独立 native observer 的 SERVER 控制 Spec 在各宿主采用相同目录策略。没有把 SERVER 降为 COMMON，也没有宣称恢复 Forge 全部 per-world 路径策略。真实客户端/联机同步、热重载、跨世界 SERVER 策略仍 NOT_RUN。

## Gate 4：独立原生输入与真实运行

`fixture-build-2/` 由完整固定 Forge SDK 编译，不依赖 DualForge/Neo 类型。两个原始 Forge JAR 在 Forge 和 Neo 安装副本之间逐字节/SHA 相同；任何第三方 JAR 未改写。

| 组 | 证据与界限 |
|---|---|
| `current-single-forge` / `current-single-neo` | 同原始 Alpha，无 observer 的真实 setup/start、配置默认值、Done/正常 stop |
| `live-current/*-default` | 实际两容器、真实独立 Mod Bus、共享实际 Game Bus、原生 setup 各一次且事件对象分离；ServerStarted 同一事件各一次 |
| Mod Bus A-only/B-only | 官方 native bus 的诊断性额外 post；另一 owner 计数不增，并用原 Consumer 实际注销。不是新增 FML lifecycle stage |
| `live-current/*-modified` | 复制已停止世界/config，在新实验改官方 TOML，实际读取 Alpha=false/true、Beta=true/false |
| `live-current/*-persisted` | 再复制并不再修改，重启恢复同值；源世界/config 未变化 |
| 原生 API / 故障模型 | 以下独立记录，不代替真实 FML 或客户端 |

`PlanContract` 的最终字节码断言数量由 final assessment 记录，全部是 MODEL_TEST；`NativeBusContract` 双平台各 7 项 ISOLATED_NATIVE_API，验证真实优先级/receiveCanceled、线程、参数/事件 identity、取消、注销、异常及 Source/Neo post 返回 ABI 差异。

独立 native Observer 的原 FML 回调与诊断性额外 post 分开。Supplier 临时替换后直接调用原 Consumer 的取消正例、null-entity death posts 均为诊断，**实际 mob death / 玩家行为 NOT_RUN**。

Owner 负例明确标为 ISOLATED_MODEL_WITH_LIVE_OWNER：反射更改并恢复诊断身份字段；跨 A/B config 调用由 B 的真实已转换工厂取得 A token，须在 host 注册前拒绝。宿主故障测试绕过 caller path，向 native bus 实际注册前/后注入异常：UNCERTAIN、原异常、拒绝重试、真实 partial effect 保留分别断言。没有声称完成全调用链故障注入或安全沙箱。

## Gate 6：严格拒绝的定位

异常文字不强制等于类别；每项有实际测试/原件坐标，不用一条 ClassCastException 覆盖。

| 类别 | 测试与证据位置 |
|---|---|
| UNKNOWN_BUS_ORIGIN | PlanContract 同名；OwnerModels route 外来实际 bus |
| FOREIGN_MOD_BUS | OwnerModels actual A→B route / owner.getEventBus identity guard |
| WRONG_MOD_CONTAINER | OwnerModels 弱 owner 失效 |
| WRONG_MODULE | OwnerModels B module caller |
| WRONG_MODFILE | OwnerModels file identity |
| WRONG_CLASSLOADER | OwnerModels expected loader identity |
| WRONG_EVENT_DOMAIN | Source router branch 反转；native identity.route |
| WRONG_EVENT_CLASS | Source Class literal / native String.class |
| WRONG_CONSUMER_TYPE | 改变实际 metafactory factory descriptor |
| WRONG_LISTENER_OWNER | Consumer implementation handle 改指 helper |
| UNVERIFIED_PRIORITY | Source HIGH 及 native options(HIGH) |
| UNVERIFIED_RECEIVE_CANCELED | 非 boolean、非 cancellable 事件，以及 native options |
| UNSUPPORTED_EVENT_SEMANTICS | 未授权 result/cancel member |
| DUPLICATE_LISTENER | 重复绑定及实际重复 API 调用指令克隆 |
| WRONG_REGISTRATION_ORDER | Source Class/Consumer field/order 不一致 |
| UNSUPPORTED_BOOTSTRAP | 未验证 bootstrap handle |
| UNKNOWN_CONSTRUCTOR_EFFECT | 尾部/前缀未知指令；真实 JEI LocaleSupplier |
| CONFIG_SPEC_UNSUPPORTED | 未验证 define member / unknown type |
| CONFIG_OWNER_MISMATCH | Source factory contract；真实 B factory + A token 隔离负例 |
| DUPLICATE_CONFIG_REGISTRATION | 克隆完整 registerConfig 参数与调用指令；整组拒绝 |
| CUSTOM_BUS_UNSUPPORTED | Source custom field 与 native 未认证 bus |
| CROSS_MODFILE_CONFLICT | 外来同名 helper intersects closure |
| INPUT_SHA_CHANGED | 逐类 ModLauncher initial SHA 不匹配 |
| UNCLAIMED_CLASS_MODIFIED | 未认领 ClassNode 不得输出 |
| PARTIAL_TRANSFORM_REJECTED | normalized input 改变；Source 原件逐项不变 |

原件为最终 `contracts-verified/plan.stdout.log`、测试源码及 `owner-negative-current/stdout.log`；它们的 SHA 见索引。除真实正例外，不把上述分离组件模型写作完整 FML 拒绝路径。

## Gate 7：真实 JEI 当前边界

本轮再次实际运行原生 Forge JEI + 独立 observer（`jei-forge-final`）：原 JEI instance、5 plugin annotations、ServiceLoader provider 类型、host recipes 的有限观察通过；不等于客户端 plugin runtime。

最终新 JAR + 同一个原 Forge JEI，在纯 Neo 单因素 `jei-current/` 中：

```text
ENTRY_PROFILE ... selected=DUAL_BUS
Dual bus rejected: UNKNOWN_CONSTRUCTOR_EFFECT
 expectedField=net/minecraftforge/common/MinecraftForge.EVENT_BUS
 actualOpcode=187
 actualType=mezz/jei/common/util/MinecraftLocaleSupplier
```

该异常在 Transformer.targets 预计划，真实 JEI 的转换/类定义/构造/生命周期尚未执行。Source 入口 byte offset 4 的 LocaleSupplier 是实际尚未授权的构造器闭包；新模板期待其后才能出现的 Bus 获取。已知静态 Bus 来源与当前闭包不准入是两件事。没有跳过这段业务，**也没有声称网络/Dist 已成为本轮实际新首失败**。

| 阶段 | 010B 真实 JEI 状态 |
|---|---|
| discovery / metadata / dependency projection / language provider discovery | 实际到达预计划前置 |
| DUAL_BUS candidate | 实际选中；不是准入 PASS |
| 完整 Bus / Semantic / Config / combined plan | BLOCKED 于原完整构造器前缀，未发布目标 |
| class transformation / definition / mod construction / lifecycle | NOT_RUN |
| GUI、插件 runtime、AT应用、渲染、网络、客户端配置持久化 | NOT_RUN |

独立夹具的新子契约通过不自动批准 JEI 的具体 helper/interface/WeakConsumer/动态注册闭包。后续仍需逐项证明原始构造器、具体事件/弱引用注销及原配置返回接口；网络/Dist/client 也继续阻塞。该边界只允许 PARTIAL 交付。

## 最终回归、证据与限制

最终 `assessment-final.json`：SCOPED_AUTOMATED=PASS，overall=PARTIAL；128 项计划/拒绝断言、双平台各7原生API断言、6轮双 Owner 配置运行与42项附加诊断通过。全部安装副本、原 Forge 两输入 SHA 及最终生产源码再次绑定。

最终新 JAR 实际重跑18组：Probe、metadata/platform、discovery/identity/module、Context/Registry/Global/EventInitializer、原生/兼容 Clumps 经验球聚合与XP21/重载、FnF 原生/兼容注册/Tab/Food/代表熔炉，以及 FnF+Clumps 混装。旧 Clumps 契约99断言/28负例通过。原始Probe未重建；现有严格拒绝均保留。

Mouse 新 JAR 自动组：101断言、52负例、ownership CLIENT15/SERVER7；原生API Forge53/Neo65；728成员、6个Mixin目标、21类无初始化链接、8项隔离初始化模型。实际 GUI / Mixin应用未运行。

本轮106项 Python 单测、25个 zsh 语法检查、374项保护与 CI 字节码契约通过。字节码模型/源文件检查均不充当图形客户端 PASS。

本地证据包 `Convergence-010B-Evidence.zip` 通过 ZIP CRC 检查；SHA、全部文件清单和选定原件见 `Convergence-010B 证据索引.json`。完整 SDK/libraries 及参考 clone 不重复打包，逐输入指纹保留；世界仍位于SSD，并随选定实验原件归档。未上传这些运行附件，也未生成客户端安装 ZIP。

本轮所有本机 JVM 串行，source 保持 SMB，编译/SDK/JAR/世界/日志在 SSD。标准游戏 JVM 命令是官方默认文件，无新增 Agent、模块参数、业务开关或伪造 forge 容器。配置重启的 `DF010B_CONFIG_ONLY` 仅控制独立 observer 是否执行额外诊断；生产不读取它。

旧 FAIL/PASS 保留在旧报告/本轮早期失败目录，不作为最终候选结果；最终选定目录、JAR 与源指纹见证据索引。所有旧 Windows USER_REPORTED_MANUAL_PASS 只属于其历史 cohort。新 JAR 客户端/图形/联机/真实玩家均 NOT_RUN；`010B_JEI_CLIENT_ZIP=NOT_PRODUCED`。
