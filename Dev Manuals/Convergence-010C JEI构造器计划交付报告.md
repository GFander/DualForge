# Convergence-010C JEI构造器计划交付报告

日期：2026-10-18。**整体 PARTIAL；本轮有限自动化验证 PASS。** 原始 JEI 的实际 Neo 首失败已从 Locale 前缀推进到完整预计划中的 `UNVERIFIED_NETWORK_INITIALIZATION` / `NetworkHandler`。没有跳过该业务，没有执行 JEI 转换、定义、构造或生命周期，不生成客户端安装包。

## Git、输入与证据身份

- 实际 fetch 后远端 main：`e47423aa2a48e02b128098a03d5486ad8faa37f8`，作为远端状态参考，不声称已包含本轮前置代码。
- #35 与 #36 均 OPEN/DRAFT、未合并。010C 按指定从实际 010B HEAD `9748d75418d5874f9356f0742f60bbff58ae2f2e` 建立。
- 分支：`codex/convergence-010c-jei-constructor`；实现提交：`1ad456e44d7c8bfec9aeec4586b981b3a7a23dc0`。
- Draft PR：[GFander/DualForge #37](https://github.com/GFander/DualForge/pull/37)，base `codex/convergence-010b-dual-bus-config`。没有合并、强推、重写历史或发布 Release。
- 源码：`/home/albert/Codex/DualForge-010C`；全部运行附件：`/home/albert/Env/DualForge/runs/convergence-010c`。
- 固定 MC 1.21.1 / Forge 52.1.0 / NeoForge 21.1.256 / FML 4.0.45 / Microsoft JDK 21.0.10。
- 原 Forge JEI：`jei-1.21.1-forge-19.27.0.340.jar`，SHA-256 `f5e61a001d57dd044d6f2ba255daf955c50d90e4173f8b4933ab0eb8cd386ed5`，完整原件未修改。
- 最终生产 JAR：`build-current/dualforge.jar`，232006 bytes，SHA-256 **`066aa4aa4ddcfc6841b6b2ddbcfa887e00f716a3afdfe1c4bd4e11dff5b94ba1`**。

测试期间生产源码未提交的状态、36 个生产文件指纹及新 JAR 记录于 `source-current.json`；提交后 `committed-source-binding.json` 逐项核对上述实现提交的 Git blob 与被测字节一致。最终文档提交、远端 HEAD、PR 和实际 CI 收据在交付后写入 SSD `delivery.json`，不把实现 SHA 冒充文档提交自身 SHA。

根工作区用户未跟踪的 008A Windows 报告保持原样，未混入 PR。七份 009C/010A/010B 历史报告/索引及全部 374 项保护通过。旧报告中的 FAIL/PASS、历史 Windows USER_REPORTED_MANUAL_PASS 与本轮结果严格分开。

## Gate 1：原始 Locale/Translator

本轮重新核对原/参考发行各 923 个 ClassFile 的 SHA，保存 fresh javap、Bootstrap、字段/方法及订阅调用证据。SDK 中相关 Minecraft public 方法逐个从 ClassFile 读取，不把方法存在作为实际客户端运行证明。

原 `JustEnoughItems.<init>` byte offsets：

```text
0/1   aload_0 / Object.<init>
4/7/8 NEW MinecraftLocaleSupplier / DUP / invokespecial ()V
11    Translator.setLocaleSupplier:(Supplier)V
14    MinecraftForge.EVENT_BUS
18/21 FMLJavaModLoadingContext.get / getModEventBus
25–34 PermanentEventSubscriptions(game, mod) → local 3
35–45 ModLoadingContext → local 4; ServerConfig.register → IServerConfig local 5
47    NEW NetworkHandler                  ← 本轮实际预计划拒绝
66    NEW JustEnoughItemsClientSafeRunner ← 尚未准入
78/88 Dist.CLIENT / safeRunWhenOn         ← 尚未准入
```

`MinecraftLocaleSupplier implements Supplier<Locale>`：public 无参构造器只有 Object 初始化和 RETURN；typed `get():Locale` 及 bridge `get():Object`。它没有 `<clinit>`，两个 private cache 字段初始为 null。get 才调用客户端 `Minecraft.getInstance`、LanguageManager/getSelected；若 Minecraft 实例为 null 则取 JVM default，否则按语言代码缓存 `Locale.of`。缓存仅修改本实例，原方法没有线程校验或同步。

`Translator` 唯一业务状态是 private static Supplier。其 `<clinit>` 用标准 metafactory 建立返回 Locale.ROOT 的 Supplier；setter 只有 ALOAD/PUTSTATIC/RETURN，不求值、不改 Minecraft Language 或 JVM Locale。lowercase 才取 Supplier 并调用 String.toLowerCase；另两个 translate 方法调用客户端 I18n。所有这些原调用保留，不新增 Locale Bridge。

**原生 Forge 实际结果**（`jei-native-locale-2`）：原 JEI 正常完成服务端启动/停止，实际 Translator 字段指向原 MinecraftLocaleSupplier。诊断性调用 getter 被原生 RuntimeDistCleaner 以 DEDICATED_SERVER 拒绝；`getInstance == null` 回退不能绕过客户端类加载。重复 setter 替换字段引用；故意传入抛异常 Supplier 后，lowercase 原样传播同一个异常对象，字段仍保持失败 Supplier，观察器 finally 恢复原引用。Minecraft Language 对象 identity 与 JVM default Locale 保持不变。

注册在 modloading-worker、诊断求值在 Server thread 的实际线程名均入日志；原 getter 在 CLIENT 中的真实线程、语言切换、缓存命中、I18n 和图形行为仍 NOT_RUN。没有声称服务端能成功取得客户端 Locale。

Forge 模组构造并发，metadata ordering 不保证观察器构造先于 JEI。因此 BEFORE 是实际采样，可能已经注册。独立原类 child-loader 模型补充 ROOT 初始状态与 setter 顺序证明，不能冒充真实 JEI 的构造前观测。第一次观察器错误假定构造顺序，实际失败原件 `jei-native-locale-1` 保留；修正后才获得上述结果。

两平台 `OriginalModels` 还实际定义和调用原发行四个选定 class。Locale get 在 Neo Dedicated Server 也触发原生拒绝。该实验标记 ISOLATED_COMPONENT_WITH_LIVE_HOST，不是第二个 JEI ModContainer。

## Gate 2：有限组合与生产修改

仅新增 `ForgeConstructorSegments.java`，扩展 `ForgeDualBusPlan.java`；Reader、元数据、平台投影、语言服务、旧 Profile 和 Bridge 的所有者/运行期校验不改。

| 片段 | 输入、输出与数据流 | 效果及拒绝条件 |
|---|---|---|
| Object base | 未初始化 this → 已初始化 this；空栈 | 精确 ALOAD0/INVOKESPECIAL |
| Locale allocation | 同 ModFile 的 NEW/DUP/无参 ctor → 一个 Supplier | 完整核对 ROOT 或原 MC cache getter、bridge 与构造；无未知 clinit/副作用 |
| Translator registration | 栈上该对象 → setter；空栈 | setter owner 是同文件的独立类；完整核对全局字段、ROOT bootstrap、setter/翻译方法；不得重复或换序 |
| Game/Mod acquisition | 官方 Game 字段 → local1；当前 context 的 Mod Bus → local2 | 复用010B来源/host ABI及真实Owner Bridge |
| Subscription construction | local1/2 → 同 owner router；存入口 final 字段 | 精确双参数引用归属、marker分支、NORMAL/receiveCanceled、Class/Consumer；mod getter只返回已验证mod字段 |
| Config factory | 当前 owner token、同 builder → spec/local/配置对象；返回 concrete 或同文件布尔接口 | 真实Spec、精确注册顺序、Supplier延迟读取与getter双射、接口封闭 |
| Explicit subscriptions/return | this捕获、已存Consumer、显式Class → 受控host调用；RETURN | 逐调用、身份、顺序、重复、异常与UNCERTAIN继续沿用010B |

新完整正例仍采用有限字段布局；原 JEI 的 local3/4/5 另按精确数据流取证，但该布局的诊断分支**始终拒绝**未验证后续，不生成可执行部分计划。不实现任意 Java 解释器。

辅助类由 NEW/调用/字段/接口关系取得，不依赖 JEI 或夹具 modId、包名、类名或发行 SHA 白名单。生产固定的是已验证 API/形状及原010B宿主ABI；输入 SHA 用于绑定完整已验证计划，不代替准入。

计数正例允许精确的同类 private static int GETSTATIC/ICONST_1/IADD/PUTSTATIC，除此没有调用、外来状态或隐式初始化；原 JEI 本身没有这些计数器。完整方法、局部变量、栈、控制流、初始化顺序和所有构造副作用先验证，再渲染 clone，整组通过后才发布 targets。原初始化调用执行一次、不吞异常。原010B入口/容器/模块/ModFile/ClassLoader/session/fingerprint约束保持。

新模型212项断言通过。双 dist Reader/scan/planner 34项断言还先建立一个完整有效的6类计划，再遇原JEI拒绝：全部相关已发布map仍为空，原输入不改。这是预计划原子性模型，不承诺 JVM 已定义类可回滚。

## Gate 3：原 ServerConfig / IServerConfig

原 ClassFile 验证：Builder local1；同 builder 构造 ServerConfig local2；build spec local3；context0/Type.SERVER/spec3 注册；返回同 local2，声明结果为 IServerConfig。三个 private final Supplier<Boolean> 默认 true/true/false；接口三个 `()Z` 方法；原 getter 各读取对应 Supplier/Boolean.booleanValue，读取没有提前常量折叠。

扩展允许同 ModFile、无字段/父接口/默认方法的闭合布尔接口；接口方法必须全部由该配置对象 public getter 实现，getter 与实际字段必须形成双射。其他接口或Supplier数据流拒绝。原 Spec 映射到固定真实 Neo ModConfigSpec，注册继续调用010B Bridge及官方 ModContainer API。

证据分三层：

1. 原始ServerConfig/接口静态逐指令验证。
2. 两平台原类隔离组件：原 private ctor、原接口和实际 native spec，默认 true/true/false → 改同一个配置源并 afterReload → false/true/false，Supplier identity 不变。Neo 使用官方 sealed 接口允许的 LoadedConfig 内存记录；不执行工厂注册或保存，不能当生命周期证明。
3. **原字节配置工厂真实容器集成**：`originalcfg.jar` 中两个原class原字节复制，其他入口/Locale/router是自有独立 Forge 夹具，仅 Forge 编译一次；原发行 JAR 不改、不安装在此组件实验。原工厂在新真实 ModContainer 调用、注册 SERVER native Spec，原接口由未改回调读取。两平台同SHA，各 default/modified/persisted 三轮，业务实际读出 true/true/false → false/true/false → false/true/false。

组件集成原JAR SHA：`613880868e7cbe25dd5a2019e319b6c4ac523717af5522831bd5f245c0c8247c`。原class SHA：ServerConfig `a0dc65a72c39821502f68d3d37af4276529cb67993d5fba3ac0624ddfef4365d`；IServerConfig `9bea507a3368386263803b9d966f93e5de4719aff58a1445d1cad23bfb348725`。

因此原配置组件的工厂/接口/真实容器/保存重启闭包通过；**完整 JEI owner 的运行仍因总入口拒绝而 NOT_RUN**。多个 owner 的隔离由独立 localpha/locbeta 及原010B Alpha/Beta分别实测，不声称可同时安装两个带相同原JEI包的配置组件。

SERVER策略差异明确保留：Forge `world/serverconfig/*.toml`，本 Neo宿主 `config/*.toml`；各source注册位置均与同轮native observer SERVER对照一致。通过的是各宿主策略下的真实保存/复制/加载，不是逐世界路径或同步策略等价。

## Gate 4：原订阅器

| 原类 | 状态 | 已核对边界 |
|---|---|---|
| PermanentEventSubscriptions | PARTIAL | 双Bus构造来源、marker路由、NORMAL/false显式Class/Consumer、mod getter形状通过；原client调用者/捕获及全部事件未准入 |
| RuntimeEventSubscriptions | UNVERIFIED | ArrayList持有EventSubscription；动态register拒绝Mod marker；clear先forEach unregister再清列表；重复注册/动态生命周期未验证 |
| EventSubscription | REJECTED | 保存原Consumer；实际注册新WeakConsumer，保存registeredListener；unregister使用包装对象而非原Consumer |
| WeakConsumer | UNVERIFIED | WeakReference#get、非null才accept；GC、注销、动态整链身份未验证 |

Runtime列表强持有Subscription，Subscription又强持有原listener；Game Bus上的注册对象为WeakConsumer。不能把010B直接Consumer桥接当作此生命周期的替代。未知动态/弱引用闭包继续 BLOCKED；没有跳过这些类来启动 JEI。

## Gate 5/6：独立正例与拒绝

`localpha.jar` SHA `2822a818854015e1ec7ec523c9f0782063ba6164e9615290d3ccf0df3d157865`，`locbeta.jar` SHA `a7ce6391b0afff6026659b400669f5045e50c245907872cfa51c6bb595ddf0c8`。两份不同身份、仅Forge编译的原输入，同SHA用于Neo。每平台 default/modified/persisted 三轮，共6轮。

实际计数每 owner Supplier构造=1、Translator注册=1；before采样0或1，after必须1；ROOT真实值、原tolower结果、不同模块全局字段/对象隔离通过。官方Game Bus同identity、各容器Mod Bus不同；原FML回调次数/事件对象、原接口、native配置值及停止重启恢复通过。配置只在停止后复制新的实验世界/config并修改官方TOML，源副本SHA保持。

额外post、null-entity death、临时Supplier替换、私有Owner/故障注入均标为诊断/模型，不作真实玩家死亡或客户端证明。旧42项Owner诊断（包括B工厂+A token）和host partial-effect/UNCERTAIN模型用最终JAR重跑。

19个指定拒绝类别均有独立具名变异/断言，完整定位表见 `tests/real_mods/jei/constructor/MATRIX.md`：未知Supplier、setter owner/顺序/重复、未知构造副作用、局部变量/片段顺序、部分计划、跨文件、Bootstrap、接口、Supplier双射、config owner、weak包装身份、动态注册、network、Dist、initial SHA、未认领class。

各负例保留具体变异与实际错误；部分故意无效栈/控制流由真实ASM帧验证拒绝，不冒充运行时业务测试。所有原件保全，逐类转换仅返回新clone；输入改变、未认领class、跨文件冲突和整组未发布分别验证，不以一个通用异常代替全部拒绝。

## Gate 7：最终 JAR + 原始 JEI 的真实新边界

`jei-current/runtime` 是独立官方Neo安装物理副本，mods只有最终DualForge和同一个原Forge JEI。实际 main stderr 首行：

```text
Dual bus rejected: UNVERIFIED_NETWORK_INITIALIZATION
locale=STATIC_VERIFIED busIdentity=STATIC_VERIFIED router=STATIC_VERIFIED config=SERVER
eventSemantics=BLOCKED_NO_EXPLICIT_SUBSCRIPTIONS combined=REJECTED_NO_TARGETS
nextOpcode=187 nextType=mezz/jei/forge/network/NetworkHandler
```

真实异常来自 ForgeDualBusPlan.rejectLocalBoundary / DualForgeEntryTransformer.targets，exit1、未Done。它是**预计划拒绝未知网络初始化**，不是 NetworkHandler 已执行后的网络错误。

| 阶段 | 本轮原JEI状态 |
|---|---|
| Discovery / Metadata / Entry candidate DUAL_BUS | REAL_RUNTIME_PASS（只到候选选择） |
| Locale / Bus来源 / router / Config子计划 | STATIC_VERIFIED，实际预计划运行时完成这些字节码检查 |
| Event Semantic完整闭包 | BLOCKED_NO_EXPLICIT_SUBSCRIPTIONS；固定宿主有限API与独立夹具通过不批准JEI客户端 |
| Combined Constructor Plan | REAL_RUNTIME_FAIL；无targets发布 |
| Transformation / Definition / Construction / Lifecycle | NOT_RUN |
| network / Dist / ClientSafeRunner / weak动态链 | BLOCKED |
| GUI / plugins / AT应用 / client配置生命周期 / Windows | NOT_RUN |

旧010A LISTENER_CONTRACT、010B Locale前缀失败保持历史事实，本轮没有改写这些报告，也不借它们声称已重跑旧时实验。

## Gate 8：最终制品回归

最终同JAR实际重跑：

- 原010B：128项计划断言，Forge/Neo各7 native API，COMMON/SERVER双owner的6轮默认/修改/恢复；独立42项Owner/故障诊断。
- 18个新原生/兼容游戏组：Probe、metadata/platform、discovery/entry/module/ModFile、Context/Registry/Global/EventInitializer、Clumps原生/兼容/独立输入经验球21 XP及重载、FnF原生/兼容注册/66 Tab与58 FOOD查询/代表熔炉、FnF+Clumps混装。原Probe只使用指纹匹配原件，没有重建。
- FnF候选仍CONTEXT，不被新DUAL_BUS劫持；Clumps99断言/28负例通过。
- Mouse：101断言/52负例；CLIENT/SERVER ownership15/7；原生API Forge53/Neo65；728成员、6 Mixin目标、21类no-init链接及8隔离初始化模型。实际Mixin应用/图形不算PASS。
- 106 Python单测、25 zsh语法、374保护、CI字节码10断言及FnF expectation 3正/12负通过。

`assessment-final.json` 独立核对286项证据条件，SCOPED_AUTOMATED=PASS、overall=PARTIAL。每个安装副本/stock实际JVM命令、原输入、所有36个生产文件与最终JAR SHA绑定。测试重用旧不可变输入和启动说明，没有继承旧日志、结果或世界PASS。

## 归档与交付限制

选定原件包：`Convergence-010C-Evidence.zip`，60775469 bytes、3088成员、ZIP CRC通过；SHA `3a2f337c5082622ae4ab2f0c7b80de216ebd2dada7139ac9f114838ee6ca4efe`。完整逐文件索引和角色见本轮 JSON；SDK/libraries不重复打包，输入指纹保留，原世界及所有完整原件继续在SSD。

早期 contracts/observer/live 失败目录保留，不作为最终PASS。包括构造并发错误假设、诊断资源误作class导致模块包名拒绝、sealed接口代理模型失败及修正；最终选定目录为 `live-verified`、`original-config-live`、`dual-live-current` 等索引所列，不覆盖旧目录。

只推送代码、工具、报告与指纹索引；完整运行附件未上传。所有JVM串行，源码SMB，编译/制品/世界/日志SSD；游戏命令官方默认，无Agent/额外模块或业务JVM参数。原生DistCleaner诊断拒绝的ERROR有明确独立用途，不混成普通无错启动。

`010C_JEI_CLIENT_ZIP=NOT_PRODUCED`。剩余网络/Dist/ClientSafeRunner、动态weak注销生命周期、更多事件/捕获、客户端Locale语言切换、插件/GUI/AT及完整JEI配置/客户端运行仍需后续专门开发与实测；本轮不要求用户做已知被总入口阻塞的Windows测试。
