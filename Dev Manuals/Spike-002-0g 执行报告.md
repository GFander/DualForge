# Spike-002-0g 执行报告

2026-10-12。root 按用户要求串行完成；没有子代理参与本轮执行/复核。依据 `Spike_002-0g.md` v1.0，SHA `CA85323CA7229C0AAF4A6A2D955C123D9A6A191811482876B85E6F16945C0626`。

结果自评：**PASS-002-0g-1**。C复现P1；T三处成员引用变换后实际进入NeoForge EventBus.register，首个新终点为预登记S1（无SubscribeEvent方法的IllegalArgumentException）。没有监听器登记成功证据，没有修复S1。

## 一、环境及输入

Minecraft1.21.1、NeoForge21.1.256、FML4.0.45、ModLauncher11.0.5、Microsoft OpenJDK21.0.10、Windows amd64；固定CLI服务端。

| 项 | SHA-256 | 字节数 |
|---|---|---|
| 同一0e probe，两轮固定 | D61224F3014F0ADFF85D10EEDEA414763C231367F08EDB99CE99DD2C8935F1C9 | 1848 |
| 0d compat原件（未变） | 4CD8195F7AA03B8B595F646CDEAC16454D03D5E3862B5E2F388C820CF2FA2FEF | 21180 |
| 0g新compat，两轮固定 | F0D51E46533A135A9ECA0FA859805E04E9FBB22E8759B1A29524319E37B4C739 | 22432 |
| 新harness（仅两标签） | FBA0003CEDF843293008868948049F8E9B287A35F52C809D15DA236FF8CA9C8C | 8996 |
| C dump | 55D81709DC8C41A8FB25189BEF87D41F5F1920C721C02B2027F9EEE764924FE8 | 1381 |
| T dump | A9ABBB9440DD49A16BB5749D46744F01D96FAF2796B189920BD8788682705EF1 | 1359 |

共同JVM：`-Xmx3G -Xms1G -Dforgecompat.2a.rewrite=true`。唯一运行变量是`-Dforgecompat.0g.apiRewrite=false`（C）/`true`（T）。两轮probe、compat、源码、harness相同。当前对象BUS打印是构造器注入bus，不把它的地址当作全局EVENT_BUS对象同一性测量。

完整实际命令/输出/退出码在 `spike000\logs\spike002-0g-artifacts`；输入前/后及所有编译依赖SHA在inputs-before/after.json，host Neo universal/bus另外由每轮wrapper按0f指纹校验。

## 二、源码、构建与静态门

先将0d src对照src-fixed归档，14/14源码/资源一致；复制到隔离 `spike001a\compat-002-0g`，原目录不改。新增规则仅在ForgeProbeConstructorTransformer.java，源码前后均完整保留，源diff原文落盘。

javac与jar退出0，实际9源路径和7件classpath全部在command文件，不使用历史模板；原始stdout/stderr分开保存。新jar为25条目/16内容文件，15/16逐字节与0d相同，仅transformer.class不同；包括四项前置及委托loader在内的其余class、4service、manifest保持。证据content-comparison.json与old/new-transformer.stdout.txt。

新增逻辑与冻结定义逐字一致：显式true/false属性检查；已共同重写构造参数后，仅精确匹配ForgeProbe构造器的GETSTATIC EVENT_BUS→ALOAD0→INVOKEINTERFACE register(Object)序列；恰一处才修改三操作数；其余方法、vote、target、dump及捕获策略保持。transformer ERROR任一出现即STOP，本轮未出现。

harness逐字比较证明：仅ValidateSet追加两个0g标签；对0e现态的单步diff及对原19标签快照的累计diff分别保存，未改其它字节。

## 三、Run C（完整首失败）

Label spike002-0g-control。`EXITED_EARLY / exitCode0 / 10.1s / Done=null`。API_REWRITE=false hits0，original-pattern1；constructor REWRITE1；marker/BUS、neoforge委托均命中；dump与0e Run T逐字节相同。运行mods只有固定probe和新compat，其SHA见本轮mods.json。控制门单独存control-gate.md，先于T通过。

首失败完整栈原文，来源本轮latest副本 `spike002-0g-control.log`（日志自身的`...15 more`原样保留，非报告截断）：

```text
java.lang.NoClassDefFoundError: net/minecraftforge/common/MinecraftForge
	at TRANSFORMER/forgeprobe/com.example.forgeprobe.ForgeProbe.<init>(ForgeProbe.java:14) ~[forgeprobe.jar%23122!/:?]
	at java.base/jdk.internal.reflect.DirectConstructorHandleAccessor.newInstance(DirectConstructorHandleAccessor.java:62) ~[?:?]
	at java.base/java.lang.reflect.Constructor.newInstanceWithCaller(Constructor.java:502) ~[?:?]
	at java.base/java.lang.reflect.Constructor.newInstance(Constructor.java:486) ~[?:?]
	at MC-BOOTSTRAP/fml_loader@4.0.45/net.neoforged.fml.javafmlmod.FMLModContainer.constructMod(FMLModContainer.java:122) ~[loader-4.0.45.jar%2359!/:4.0]
	at MC-BOOTSTRAP/fml_loader@4.0.45/net.neoforged.fml.ModLoader.lambda$constructMods$3(ModLoader.java:153) ~[loader-4.0.45.jar%2359!/:4.0]
	at MC-BOOTSTRAP/fml_loader@4.0.45/net.neoforged.fml.ModLoader.lambda$dispatchParallelTask$7(ModLoader.java:231) ~[loader-4.0.45.jar%2359!/:4.0]
	at java.base/java.util.concurrent.CompletableFuture.uniHandle(CompletableFuture.java:934) [?:?]
	at java.base/java.util.concurrent.CompletableFuture$UniHandle.tryFire(CompletableFuture.java:911) [?:?]
	at java.base/java.util.concurrent.CompletableFuture$Completion.exec(CompletableFuture.java:483) [?:?]
	at java.base/java.util.concurrent.ForkJoinTask.doExec(ForkJoinTask.java:387) [?:?]
	at java.base/java.util.concurrent.ForkJoinPool$WorkQueue.topLevelExec(ForkJoinPool.java:1312) [?:?]
	at java.base/java.util.concurrent.ForkJoinPool.scan(ForkJoinPool.java:1843) [?:?]
	at java.base/java.util.concurrent.ForkJoinPool.runWorker(ForkJoinPool.java:1808) [?:?]
	at java.base/java.util.concurrent.ForkJoinWorkerThread.run(ForkJoinWorkerThread.java:188) [?:?]
Caused by: java.lang.ClassNotFoundException: net.minecraftforge.common.MinecraftForge
	at java.base/jdk.internal.loader.BuiltinClassLoader.loadClass(BuiltinClassLoader.java:641) ~[?:?]
	at java.base/java.lang.ClassLoader.loadClass(ClassLoader.java:526) ~[?:?]
	at cpw.mods.securejarhandler/cpw.mods.cl.ModuleClassLoader.loadClass(ModuleClassLoader.java:216) ~[securejarhandler-3.0.8.jar:?]
	at java.base/java.lang.ClassLoader.loadClass(ClassLoader.java:526) ~[?:?]
	at cpw.mods.securejarhandler/cpw.mods.cl.ModuleClassLoader.loadClass(ModuleClassLoader.java:216) ~[securejarhandler-3.0.8.jar:?]
	at java.base/java.lang.ClassLoader.loadClass(ClassLoader.java:526) ~[?:?]
	... 15 more
```

上述完整首栈保留原文帧与TAB；报告排版不替代完整附件的字节级原文。原始stdout/latest/debug/crash未删节。

## 四、Run T（完整首失败）

Label spike002-0g-treatment。`EXITED_EARLY / exitCode0 / 10.1s / Done=null`；exit0不代表加载成功。API_REWRITE=true hits1、API post-check准确；marker/BUS及委托命中。stdout P1零命中，首异常为S1。

关键规则行原文：

```text
[FORGECOMPAT-XFORM] API_REWRITE enabled=true hits=1
[FORGECOMPAT-XFORM] API post-check field=net/neoforged/neoforge/common/NeoForge.EVENT_BUS:Lnet/neoforged/bus/api/IEventBus; call=net/neoforged/bus/api/IEventBus.register(Ljava/lang/Object;)V itf=true
```

完整首异常栈原文，来源 `spike002-0g-treatment.log`（原文件的空行保留，字节级原文以附件为准）：

```text
java.lang.IllegalArgumentException: class com.example.forgeprobe.ForgeProbe has no @SubscribeEvent methods, but register was called anyway.
The event bus only recognizes listener methods that have the @SubscribeEvent annotation.

	at MC-BOOTSTRAP/net.neoforged.bus/net.neoforged.bus.EventBus.register(EventBus.java:135) ~[bus-8.0.5.jar%2362!/:?]
	at TRANSFORMER/forgeprobe/com.example.forgeprobe.ForgeProbe.<init>(ForgeProbe.java:14) ~[forgeprobe.jar%23122!/:?]
	at java.base/jdk.internal.reflect.DirectConstructorHandleAccessor.newInstance(DirectConstructorHandleAccessor.java:62) ~[?:?]
	at java.base/java.lang.reflect.Constructor.newInstanceWithCaller(Constructor.java:502) ~[?:?]
	at java.base/java.lang.reflect.Constructor.newInstance(Constructor.java:486) ~[?:?]
	at MC-BOOTSTRAP/fml_loader@4.0.45/net.neoforged.fml.javafmlmod.FMLModContainer.constructMod(FMLModContainer.java:122) ~[loader-4.0.45.jar%2359!/:4.0]
	at MC-BOOTSTRAP/fml_loader@4.0.45/net.neoforged.fml.ModLoader.lambda$constructMods$3(ModLoader.java:153) ~[loader-4.0.45.jar%2359!/:4.0]
	at MC-BOOTSTRAP/fml_loader@4.0.45/net.neoforged.fml.ModLoader.lambda$dispatchParallelTask$7(ModLoader.java:231) ~[loader-4.0.45.jar%2359!/:4.0]
	at java.base/java.util.concurrent.CompletableFuture.uniHandle(CompletableFuture.java:934) [?:?]
	at java.base/java.util.concurrent.CompletableFuture$UniHandle.tryFire(CompletableFuture.java:911) [?:?]
	at java.base/java.util.concurrent.CompletableFuture$Completion.exec(CompletableFuture.java:483) [?:?]
	at java.base/java.util.concurrent.ForkJoinTask.doExec(ForkJoinTask.java:387) [?:?]
	at java.base/java.util.concurrent.ForkJoinPool$WorkQueue.topLevelExec(ForkJoinPool.java:1312) [?:?]
	at java.base/java.util.concurrent.ForkJoinPool.scan(ForkJoinPool.java:1843) [?:?]
	at java.base/java.util.concurrent.ForkJoinPool.runWorker(ForkJoinPool.java:1808) [?:?]
	at java.base/java.util.concurrent.ForkJoinWorkerThread.run(ForkJoinWorkerThread.java:188) [?:?]
```

这证明方法实际进入，不是仅class loaded。对应字段值的取得和该invokeinterface路径已越过符号缺失；没有对象同一性或监听器登记测量。FML后续上报 `Failed to wait for future Mod Construction, 1 errors found` / ModLoadingException，完整原链在两份latest与crash附件，后续其它内容只附件、不新增分析目标。

## 五、重写后逐指令验证

C/T dump构造器参数均Neo IEventBus；忽略CP索引后全文code除两条指令行外相同，指令数量、所有offset、四次println和return52不变：

| offset | C | T |
|---|---|---|
| 43 getstatic | Forge MinecraftForge.EVENT_BUS:Forge IEventBus | NeoForge NeoForge.EVENT_BUS:Neo IEventBus |
| 47 invokeinterface | Forge IEventBus.register(Object):void | Neo IEventBus.register(Object):void |

两条行差异正是三处操作数（字段owner+descriptor、方法owner），opcode/name/方法descriptor/itf保持。原始dump-code/verbose全文、差异JSON在artifacts；规则只改节点操作数，没有改LineNumberTable、异常表或注解。verify.ps1逐项验证信号、P1缺席、S1/方法帧、dump全文语义比较。

## 六、自评与标签

PASS-002-0g-1：冻结输入和源码门通过，C严格复现P1，T匹配三处变换并进入Neo register、出现预登记S1。没有触发另一个首失败、transformer ERROR或输入漂移。

[F]源/字节码三引用契约与其余内容不变。[E]两轮实际log、hash、meta、S1栈、register进入。[H]整个事件总线语义、无监听器对象在Forge原运行中的行为、未来适配策略仍未实验验证。[D]按冻结局部规则做K1，不是通用mapping架构变更。

## 七、偏差、清理与限制

- 用户要求不再使用子代理，root顺序处理，不声称独立智能体复核。
- 一次启动前wrapper路径数组括号错误，未调用harness/未启动Java；在编排脚本中修正，不计额外实验运行。过程在prelaunch-correction.md。
- native工具原始字节采集避免0f的解码问题；本轮编译输出不把空行等同0字节。
- 原probe、0d兼容层、0e/0f证据及原源码保持；新源码前后全量归档。harness只许可标签差异；两轮使用同一哈希。运行副本/旧server日志按固定harness清理，已有原始归档可恢复，不删除日志本体或冻结制品。
- 实际服务端各一次，无复跑；S1出现后不加监听器、不修复异常；server最后T副本保留，Java已退出。

## 八、完整文件清单

`spike000\logs\spike002-0g-artifacts\evidence-index.json`登记采集/脚本/输入/源码/输出及工具原文；`run-files-index.json`登记C/T全部stdout/stderr/latest/debug/meta/mods-list/crash/dump，包含完整原始异常与上报链。索引自身不作自引用哈希。source archive 位于compat-002-0g\archive\src-before/src-after，jar同哈希归档副本亦保留。

原始完整文件不能由报告转录替代；后续顺序裁定另存并登记本报告指纹。
