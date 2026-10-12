# Convergence-004：受控 Forge 事件真实投递

2026-10-12（执行器 UTC）；**受控范围 PASS**。
`[E]` 是本轮实际运行／字节断言，`[F]` 是锁定 JAR 静态契约，`[D]` 是设计决定。
完整原始附件：`/workspace/DualForge-004/out/convergence-004/`。
[证据索引](Convergence-004%20证据索引.json) 保存存在文件、SHA-256、阶段和用途。

## 1. Git 起点、工具链和范围

[E] 已成功 fetch origin；从最新已验收 003b main `73edda36c5829c8bf98634674acd7899a7a0f5de`
建立独立 worktree 和分支 `codex/convergence-004-real-event-dispatch`，起点干净。
原 worktree 和其他任务没有覆盖／清理／混入。完整 Git 规范已由 PR #5 进入此基线。
读取任务指定规范、结构／测试说明、002／003a／003b／0m／0n 报告及当前生产代码。
原生产基线先干净构建 PASS：`baseline-build/20261012-151038-ad43b45f`。

[E] 最终功能源码／runner 提交：`92f2afc03255a203d2316e7674d036bf9c87640f`。最终文档提交 SHA 由 PR 和 Git 交付记录提供，
避免报告自引用。构建时生产文件尚未提交，build-inputs.json 保存原 SHA；提交后逐字节核对。
当前与该构建完全一致；报告提交后不再修改生产代码。未合并、未发布、不进入 005。

[E] 本轮 java/javac/jar 实测为 Microsoft OpenJDK 21.0.10+7-LTS；Python3.12.14、
zsh5.9、git2.52.0、Linux6.18.44 x86_64。实际版本、manifest 和运行库 SHA 在
`toolchain/20261012-153502-ec2e1271`。锁定 Minecraft1.21.1、NeoForge21.1.256、
FML4.0.45、ModLauncher11.0.5、bus8.0.5、ASM9.10.1。没有升级依赖。

## 2. Gate 0：总线、监听器和生命周期

[F] 本轮完整 javap 在 `contract/20261012-151103-e318f4b8`，含命令／exit和依赖hash。
NeoForge.<clinit> 调用 startShutdown，EventBus(builder) 读取该字段；默认全局bus初始关闭。
ServerModLoader.load()V 正常分支176/179通过NeoForge.EVENT_BUS调用IEventBus.start()V；
EventBus.start offset2 将shutdown=false。在 Forge 构造器中不 start 或 post。

[F] 选择 `net.neoforged.neoforge.event.server.ServerStartedEvent`。
ServerLifecycleHooks.handleServerStarted 的 descriptor 为
`(Lnet/minecraft/server/MinecraftServer;)V`，offset0取全局bus，offset11调用post。
patched Main.main offset451先调用ServerModLoader.load；patched MinecraftServer.runServer()V
在initServer成功后offset19调用handleServerStarted。因此选定时点在官方start之后。
原 classpath 顺序的 vanilla javap没有这些patch；改为锁定Neo server JAR优先取证，
两套原始输出均保留，不把vanilla输出用于上述结论。

[F] Neo `post` 为 `(Lnet/neoforged/bus/api/Event;)Lnet/neoforged/bus/api/Event;`。
EventBus.register(Object)V 扫描实例默认SubscribeEvent，建立SubscribeEventListener，
EventListenerFactory生成MethodHandle/hidden class调用；非取消事件unwrap handler。
内部post offset12实际调用EventListener.invoke(Event)V，生成方法invokeExact调用listener。
post正常返回单独不是回调证据。

[F] 旧rewriteSubscriber仅一个RETURN；旧transform先改构造器及成员，再catch并返回input，
对非空方法存在部分修改风险。这里只修复受控转换的提交原子性，没有全面重构。
0n独立bus历史结果只作技术背景，不计本轮FML动态证据。

## 3. Gate 0：可见性、开关及 marker 方案

[D] 使用公开API方案：LoadingModList.get().getModFileById("dispatchprobe") →
IModFile.getSecureJar().name() → FMLLoader.getGameLayer().findLoader(moduleName) →
Class.forName(eventName,true,loader) → 公共无参构造器newInstance。
没有setAccessible、私有字段访问、模块描述符策略变更、Unsafe或自定义ClassLoader。
只有Event构造使用公共反射；listener不反射调用。

[E] 独立Gate0实验用真实Reader／LoadingModList和stock JDK临时自动模块层验证公共加载／构造、
两组开关及打印点栈捕获：`contract/20261012-151309-de9ce2c6`。
加强metadata.moduleName与SecureJar.name一致性的实验在
`contract/20261012-152349-1df83ac7` PASS。这是独立模型；C/T另验证真实FML GAME layer。

[D/E] 开关为JVM系统属性`-DDF004_REGISTER=false/true`，在真实Forge模组构造器读取，
仅决定同一register(this)调用是否执行。inputs.json记录方式、值和读取位置；stdout确认实际到达。

[D/E] 采用方案C。listener维持四条打印opcode，不拼接UUID或添加栈采集指令。
测试driver在post周期暂时包装System.out，透明转发原字符串，仅当实际调用者是目标listener时
记录StackWalker的业务栈顶、后续帧及运行UUID，finally恢复原PrintStream。
这种测试采集适配没有改变marker文本、方法体或总线。driver class没有完整callback文本；
不得把同一JVM的共享stdout说成两个独立模组stdout文件。

## 4. 最小实现与旧能力边界

- 唯一生产修改：ForgeProbeConstructorTransformer.java。保留原空listener；新增严格
  GETSTATIC System.out / LDC String / INVOKEVIRTUAL PrintStream.println(String)V / RETURN。
  非public、异常处理表、声明异常、额外非可见注解、错误owner/name/desc和额外opcode均拒绝。
- 入口／Event targets仍来自003b的FML扫描与所属modId，只选择同制品内关联Event；没有
  DispatchProbe白名单、包名通配或普通类无条件改写。
- 输入复制到ClassNode副本，先验证listener，再改受控结构；失败抛受控异常，原节点不变，
  不打印成功的REWRITE来冒称被拒绝的新profile成功。
- 共享harness仅新增默认空的extra_mods／extra_jvm_args，用于driver与属性；旧参数、flags、
  物理复制、loopback、TLS relay和原严格断言不变。没有第二套服务器启动框架。
- IdentityScopeTest仅改为序列化Transformer的返回节点，以适配副本返回；原断言未放宽。
  原IdentityProbe的源码、资源和冻结JAR不变。
- tests/events保存原生Forge fixture、原生Neo测试driver、负例和runner；README说明边界。

[E] 新旧compat 17件内容中只有Transformer.class变化，另16件逐字节相同：jar-content-proof.json。
Locator、Reader、Parser、LanguageLoader、ModuleDescriptor／Layer、事件bus实现及原构造ABI不改。

## 5. Forge fixture 编译纯度

[E] dispatchprobe入口`com.example.dispatchcheck.DispatchProbe`，Event为同包DispatchEvent。
构造器原descriptor为`(Lnet/minecraftforge/eventbus/api/IEventBus;)V`，一个实例默认SubscribeEvent，
一个原Forge EVENT_BUS.register(this)调用。Event final、无字段／接口、仅平凡无参构造。
编译classpath只有以下原Probe同版本／同指纹的原生Forge依赖，没有Neo或compat：

| 输入 | SHA-256 |
|---|---|
| javafmllanguage 1.21.1-52.1.0 | `d27e1f3149ea08fc0a80779fe24fa8585692c5042a34bad153e55662486acc15` |
| eventbus 6.2.27 | `d2a7bcecb80b33f90854a003dc295960263d78435d9d6016c427d711510cb999` |
| forge universal 1.21.1-52.1.0 | `88ef6625122cfdb9d7b3176bd61b8c0692a191bad0edd77ffddc6a765ee4979c` |

[E] 原JAR的javap和class断言确认Forge Mod、SubscribeEvent、IEventBus、MinecraftForge及Event父类；
没有net/neoforged API引用。原生driver独立用锁定Neo运行库编译，不使用Forge/compat。
所有保存源码、实际javac/jar命令、依赖、原字节、JAR内容和指纹见
`fixture/20261012-152319-db48a76e`。C/T没有重新编译或换JAR。

## 6. 字节码差异与转换范围

| 元素 | 原始形态 | 转换后形态 |
|---|---|---|
| 构造器 | `(Lnet/minecraftforge/eventbus/api/IEventBus;)V` | `(Lnet/neoforged/bus/api/IEventBus;)V` |
| 注册field | MinecraftForge.EVENT_BUS:Forge IEventBus | NeoForge.EVENT_BUS:Neo IEventBus |
| register调用 | Forge IEventBus.register(Object)V | 现有flags经ForgeEmptyRegistration.register(Neo IEventBus,Object)V，随后真实Neo bus.register(Object)V |
| listener注解 | Forge SubscribeEvent默认注解 | Neo SubscribeEvent默认注解 |
| Event superclass／super() | Forge Event | Neo Event |
| listener方法体 | GETSTATIC System.out → LDC固定文本 → INVOKEVIRTUAL println(String)V → RETURN | 保持相同 |

[E] 保存完整原始／转换Class和verbose javap；方法单独规范化（移除唯一注解差异）后
before/after class字节相同，包含打印Code、常量和调试代码结构。
完整Class的常量池索引可以重排，不将其与实际指令语义变化混淆。
反向恢复获准结构后整个Class canonical字节相同；Event同样只改变superclass与super调用。
现有0j helper在非空声明方法profile真实delegate到Neo register，并非legacy-empty-noop。

[E] 对照包含多一个flush调用的非空RejectedProbe，原／输出SHA及规范化字节码相同，
且有明确受控异常；校验发生在改动前，副本失败不泄露到原节点。UnclaimedMarker不登记、不改字节。
003b Ordinary、UnownedEntry、UnrelatedEvent、NativeControl及native dual-metadata原对照全部PASS。
各最终C/T还把实际运行dump与离线production Transformer输出逐字节规范化比较。

## 7. Run C：未注册对照

[E] `out/convergence-004/control/20261012-153251-9b014d34`：
开关false实际到达；构造一次、REGISTER SKIPPED一次；正确GAME模块`dispatchprobe`中的Event
由公共构造器实例化；POST BEGIN与POST RETURNED各一次；callback与callback stack均0。
Done、正常stop、exit0；23.8s。无新增ERROR/FATAL/dump FAIL、stderr或crash。
**真实post一次，未注册回调零次。**

## 8. Run T：真实注册与投递

[E] `out/convergence-004/treatment/20261012-153413-f38a4b25`：
开关true实际到达；单次发现和metadata／入口关联正确，入口与Event各一次必要转换；
Neo register真实执行／返回，post一次、callback恰好一次，正常post返回。
相对顺序为CONSTRUCTOR REACHED → REGISTER RETURNED → POST BEGIN → CALLBACK REACHED → POST RETURNED。
Done、正常stop、exit0；23.1s。所有正例错误／stderr／crash断言通过。

[E] 打印点业务栈顶为DispatchProbe.onDispatch，后续真实帧：
MethodHandle/LambdaForm → EventListenerFactory$onDispatch.invoke → EventBus.post →
DispatchDriver.onStarted → EventListenerFactory$onStarted.invoke → EventBus.post →
ServerLifecycleHooks.handleServerStarted → MinecraftServer.runServer。
栈UUID与driver本轮UUID一致。该链证明回调实际来自总线，不能由register/post返回或Done替代。

## 9. 同制品和实验可信度

| 两组同一文件 | SHA-256 |
|---|---|
| compat | `60eaf67e84886ba260f5b81cc7330019d63b6ec9262bbd2255093a6d918d9559` |
| 原生Forge fixture | `8c6b4f9d8b02910c3527d97a29b03ad12d209cb7773a6ba3c2e10678133efaf8` |
| 原生Neo driver | `d91a16293482d8c287f68036f0d8941eeba2664d66f9694eb22c75c6f6691e9e` |

[E] artifacts.json的路径与SHA C/T完全相同。只改变注册系统属性；相同服务端输入、Java、flags、
发布逻辑和事件类。每轮独立物理副本／JVM，不复用进程；无直接listener调用、内部状态写入或伪造计数。
隔离目录、dump路径、relay端口和运行UUID自然不同，不构成注册之外的行为变量。

## 10. Gate 4 原回归

| 验证 | 结果 | 本轮实际输出 |
|---|---|---|
| original-smoke | PASS | `out/convergence-004/regression/original-smoke/20261012-152744-48cc2e67` |
| identity | PASS | `out/convergence-003b/after/20261012-152807-12af69d1` |
| scope | PASS | `out/convergence-003b/scope/20261012-152831-1eeb0fee` |
| discovery | PASS | `out/convergence-003a/discovery/20261012-152835-229b8682` |
| dependencies | PASS | `out/convergence-004/regression/dependencies/20261012-152838-ed0f9a2c` |

[E] 全部安装同一个最终compat JAR。A是原SHA冻结制品，B是原003b冻结fixture，没有重建或改输入。
原smoke、空listener/Event profile、register返回、builtin javafml宿主、Done/stop/exit0均保留。
003a A–F、文件名、规范化去重和native/歧义边界PASS；002 metadata六组及C/D/E/F动态PASS。
D/F仍按具体missingdependency／minecraft版本范围拒绝理由、pre-loading链和目标未实例化判断；
负例的ERROR/FATAL和exit0不能用作正例成功，不以缺少Done代替拒绝原因。

## 11. 失败与修复记录

1. 首个真实FML失败：control/20261012-152057-88d8306d，FMLModContainer第58行
   ModuleLayer.findModule(...).orElseThrow抛NoSuchElementException，入口尚未构造或转换。
   fixture manifest从B复制时误带Automatic-Module-Name:identityprobe。修正新测试输入的自身身份
   dispatchprobe／dispatchdriver，增加manifest与metadata身份核验，重新原生编译并重跑。
   没有改生产模块策略、原B或核心代码；旧错误制品和日志全部保留。
2. control/20261012-152258-bb14a50a因输入路径拼写错误NOT RUN，未启动JVM；以实际fixture路径重跑。
3. 汇总指纹最初按排序列表位置关联，误交换Forge/driver标签；原artifacts.json正确。
   改为按实际文件名映射并用独立audit核验。没有改日志、制品或回调断言。
4. 原manifest的合法尾部空行曾触发diff空行提示；新fixture身份修正同时使用单结尾换行。
   初始提交与源字节仍在历史；最终完整diff --check通过。
5. runner增加可重复preflight和冻结B指纹校验后，最终C/T重跑；此前PASS也作为独立历史运行保留。

没有吞异常、删除失败日志或放宽判断。非目标结构、任意事件语义和其他ABI仍UNKNOWN。

## 12. 历史与输入保全结果

[E] `regression/preservation.json`：起点497文件中，除允许的5个源码／工具／文档路径外
492文件原字节不变。所有198 history跟踪文件、原A／冻结B、依赖TOML和原identity/src均不变。
生产Java只有Transformer修改，其余源码／资源集合与字节不变；STRUCTURE迁移映射原样，
history内历史dump绝对路径随原字节保全。完整原服务端127文件集合不变，世界22文件及集合不变。

[E] 所有174次JVM工具／测试／服务器命令严格串行，command/exit或meta时间区间无重叠：
serial-jvm-timeline.json。全部原始输入运行物理副本、127.0.0.1绑定、上游TLS校验未降低。
最终无java进程，25565可绑定并关闭检查socket。audit.py及summary.json记录最终核验。

## 13. 重跑和证据导航

完整串行CLI见 [tests/events/README.md](../tests/events/README.md)，包括先preflight、干净构建、
原生fixture、contract、C/T和冻结旧回归。Gate0源码和原命令在对应contract目录。
实际输出目录（相对于/workspace/DualForge-004）：

| 阶段 | 路径 |
|---|---|
| treatment | `out/convergence-004/treatment/20261012-153413-f38a4b25` |
| regression | `out/convergence-004/regression/20261012-152744-c639e954` |
| fixture | `out/convergence-004/fixture/20261012-152319-db48a76e` |
| control | `out/convergence-004/control/20261012-153251-9b014d34` |
| contract | `out/convergence-004/contract/20261012-153031-a2b9a9a4` |

动态日志沿用共享harness布局`logs/spike002-0l-treatment/`；完整command/meta、
stdout/stderr/latest/debug副本及server/logs原文件均保存。每次新目录，没有覆盖旧证据。
summary.json从实际result、日志断言和artifact指纹聚合，缺少阶段不能给PASS。
完整运行附件不入Git；证据索引含存在文件及用途，旧失败日志也在索引。

## 14. 最终判定与停止

**Convergence-004 PASS — 在锁定的 NeoForge 1.21.1 运行时中，原生 Forge 编译的受控自定义 Event，
通过限定转换与真实全局 EventBus 注册和投递，使目标监听器执行恰好一次；未注册对照执行零次。**

[D] 仅一个普通非取消、非泛型自定义Event和一个实例marker-only listener。
不支持一般Forge post(Event):boolean、取消/优先级/MONITOR/receiveCanceled、自动/静态/跨模组
Forge订阅、多Forge完整启动、Registry/Capability/Fabric或任意模组兼容。原Windows入口未重验。
测试driver是原生Neo模组，不能被计入Forge兼容能力。完成004后停止，不进入005。
