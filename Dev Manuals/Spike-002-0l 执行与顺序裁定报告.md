# Spike-002-0l 执行与顺序裁定报告

2026-10-12。结果：**VALID-002-0l-1**。root 单智能体串行执行→复核→裁定；未调用子代理，非独立审查。仅确认固定默认订阅 profile 的真实 register 返回及 Done，不证明事件分发。

## 一、环境与固定输入

MC 1.21.1 / NeoForge 21.1.256 / FML 4.0.45 / ModLauncher 11.0.5 / Microsoft Java 21.0.10+7-LTS / Windows 11 amd64，原 CLI dedicated server。编译依赖为现有 Forge javafmllanguage 52.1.0、eventbus 6.2.27、Forge universal 52.1.0；不加入运行 mods。

| 产物 | SHA-256 | 字节数 |
|---|---|---|
| 新 probe，probe-002-0l/build/forgeprobe.jar | A774D2F737B17F089046014AB55A53767DBAD008CD44DCCE52C727B87E559AE8 | 2258 |
| 新 compat，compat-002-0l/build/forgecompat-002-0l.jar | D49CAEA48A61C57DA5384E053D7E8DE2834760172E6948C5167BF836BFD0E679 | 25508 |
| harness（27 label） | 3176A095E7744EA78113DDF7561A83C5F9407DD61CF0ECB7BC0FCB2487464576 | 9092 |
| 原 0e probe（保留） | D61224F3014F0ADFF85D10EEDEA414763C231367F08EDB99CE99DD2C8935F1C9 | 1848 |
| 原 0j compat（保留） | D724FB489A3A1F1180A5FBA09DFF2FECD01FBD6F8AE33EE727851AB211FA44AB | 23861 |

前后全量固定指纹含冻结说明书、依赖及原源码，详见 inputs-before.json；final-check.txt 验证全部不变。runtime-mods.json 证明 C/T 实际装入同一新 probe/compat。JVM 共同参数：2a.rewrite=true、0g.apiRewrite=true、0j.emptyRegistration=true；唯一差异 0l.subscriberRewrite=false/true。实际完整命令与实际 user_jvm_args 分别保存，不以模板冒称命令。

## 二、源码、构建与硬门

两工作副本修改前后 src 全量保存于各自 archive/src-before、src-after，并有源文件 SHA 清单；原工作树不改。

- probe：构造器全部 20 条指令与 D612 旧 probe 相同，新增 onProbe 的唯一指令 return；ProbeEvent 仅默认三指令构造器。编译后 probe 8 条目，4 内容文件（旧3+新事件class），mods.toml/manifest 逐字节不变。
- compat：17 内容文件（26条目）中15完全不变，仅 transformer/helper class 变化；源码 diff 只对应这两文件。不是通用映射或人工注册，helper 仍原 no-op 条件，不 catch delegate 异常。
- build 实际 javac 参数 --release 21 -encoding UTF-8、classpath、全部源路径、jar --manifest 参数及 exit 原样记录；四次编译/打包均 exit 0。其它 javap 原字节输出和 exit 全保留。
- harness diff 机械确认只追加两 label，原逻辑字节不变。现场前轮日志/crash 已归档且 SHA 匹配，清理绝对路径均在既定 server 内、非 reparse point；两 label 均首次使用。
- semantic-before-run.txt 和 semantic-after-run.txt 通过。C/T ForgeProbe 的所有 opcode/成员指令相同，仅 RuntimeVisible 方法注解 descriptor 不同；ProbeEvent 只改 superclass 和 super constructor owner。

## 三、Run C 原文与终点

EXITED_EARLY / exitCode 0 / 10.1s，无 Done。不要因 exitCode=0 判启动成功。

stdout 原文（选择性引用，不替代附件全文）：

```text
[FORGECOMPAT-XFORM] SUBSCRIBER enabled=false annotationHits=0 desc=Lnet/minecraftforge/eventbus/api/SubscribeEvent;
FORGE PROBE CONSTRUCTOR REACHED
BUS = net.neoforged.bus.EventBus@42c43887
[FORGECOMPAT-XFORM] EVENT_PROFILE enabled=false superHits=0 constructorHits=0 super=net/minecraftforge/eventbus/api/Event
```

首次确认失败：P1 `java.lang.NoClassDefFoundError: net/minecraftforge/eventbus/api/Event`，发生在 helper 原 profile `getDeclaredMethods()`（ForgeEmptyRegistration.java:13），解析 ProbeEvent 的父类时。**未进入 delegate/Neo register**，不能称注解筛选已失败，也不能确定注解单独必要性。class dump 形成不代表 class define 成功。

完整、非截断的异常栈原件：[crash-report](D:/DualForge/spike000/logs/spike002-0l-control/crash-reports/crash-2026-10-12_11.42.20-fml.txt)，同时保留 stdout/latest/debug/stderr。stdout 并行日志的部分栈行顺序与 crash 原件不同；没有重排原文件或声称采集乱序的原因已证明。首失败后 FML 自行收集并退出，不修复失败。

## 四、Run T 原文与终点

DONE_STOPPED_GRACEFULLY / exitCode 0 / 20.7s；实际 constructor、profile/delegate、post-call 证据：

```text
[FORGECOMPAT-XFORM] SUBSCRIBER enabled=true annotationHits=1 desc=Lnet/neoforged/bus/api/SubscribeEvent;
FORGE PROBE CONSTRUCTOR REACHED
BUS = net.neoforged.bus.EventBus@348a1941
[FORGECOMPAT-XFORM] EVENT_PROFILE enabled=true superHits=1 constructorHits=1 super=net/neoforged/bus/api/Event
[FORGECOMPAT-EMPTY] delegate to NeoForge register
[FORGECOMPAT-SUBSCRIBER] register returned target=com.example.forgeprobe.ForgeProbe declaredMethods=1
[11:42:55] [modloading-worker-0/INFO] [ne.ne.ne.co.NeoForgeMod/NEOFORGE-MOD]: NeoForge mod loading, version 21.1.256, for MC 1.21.1
[11:42:57] [Server thread/INFO] [minecraft/DedicatedServer]: Done (0.245s)! For help, type "help"
```

marker 后完整日志持续记录到正常 stop，最后 stdout 原文：

```text
[11:43:07] [Server thread/INFO] [minecraft/MinecraftServer]: ThreadedAnvilChunkStorage: All dimensions are saved
```

无 legacy-empty-noop，stdout/latest/debug 无 ERROR/FATAL；stderr 0 字节；无 crash-report。Done 是启动终点，register returned 才是实际方法正常返回的独立证据。declaredMethods=1 是对象声明方法数，**不是私有 listeners 集合计数**；未读取该私有状态，也未观测 onProbe.invoke。

## 五、字节码归档

| 轮次 / 类 | SHA-256 | 字节数 |
|---|---|---|
| C ForgeProbe | 9302074F8E832C13EAF15EFA58ACBBFB8EB95D687EFC2A75F97E28FA090E50C0 | 1558 |
| T ForgeProbe | 9102958D372BE792927CA1788394282B81DCD0F144CBDE8C92CD7023AC4DC909 | 1548 |
| C ProbeEvent | 5B48527094BB902D3F55461E5098CC703905269A71CB74A2C627957C6FB106A3 | 238 |
| T ProbeEvent | 15089EC00BC3EA3867E4746DF6FDEB612CBE6C899D52F725BDD8313A7FEAD6AD | 228 |

两轮 ForgeProbe ctor offset 43 是 NeoForge.EVENT_BUS、47 invokestatic ForgeEmptyRegistration.register(bus,target)、50 return；onProbe offset 0 return。T 方法注解为 Neo SubscribeEvent、class Forge @Mod 原样保留。ProbeEvent 默认 ctor 三条 opcode 相同，仅 offset1 invokespecial owner 与 class父类改为Neo Event。每轮完整 code/verbose 与 dump 都归档到独立 Label，shared/current 输出不是历史原件。

## 六、顺序复核与裁定

1. [E] K1 两轮同新 jar/新 probe，仅一个属性开关；C 命中 P1，T 命中3个限定引用变更及真实 delegate 返回、Done。VALID-002-0l-1 成立。
2. [F] 0k 的 Event abstract 门与注解默认类型分歧成立；[D] 采用具体自定义事件而非直接替换根事件作为订阅参数，符合最小 profile 边界。
3. [E] 0j 空返回与 0l 真实 register 返回是不同结果，不能合并。两轮无 no-op；T不从 Done反推注册，而依据call之后的日志。
4. [H] 未独立观测私有监听器数量、wrapper invoke、event dispatch、取消或优先级语义。未给每条映射分别做负对照，因此必要性只限组合规则的这条实验路径，不能说每条是全系统必要条件。
5. [D] 下一门槛为只读取证 post/启动状态/监听器包装路径及 FML 全局bus.start 时序，再冻结真实分发单因子实验；不在构造器中盲目 post，也不擅自提前 start 宿主总线。

Confidence HIGH（只限真实 register 返回及生命周期终点）；完整事件兼容 UNKNOWN。

## 七、偏差与工具修复

无实验输入/运行次数偏差。prepare 包装脚本初次因 dot-source 共享作用域覆写 Phase 成空而未进入任何阶段（无构建、无服务器运行）；保存并恢复请求 Phase 后执行一次真实 prepare。此为工具脚本修复，不修改冻结说明书或实验源码规则。原0j工具脚本不动，调用Native原字节捕获函数；借用函数时旧固定输入也核对，但不冒称独立新工具实现。

不使用子代理，符合用户当前指令；复核由同一智能体完成，这降低独立审查性，已明确披露。

## 八、原始附件与边界确认

完整原始附件目录：

- `D:/DualForge/spike000/logs/spike002-0l-artifacts/`：所有源码/内容对比、actual commands/exit、原字节javap、指纹、实际JVM参数及验证脚本；evidence-index.json 列全量。
- `D:/DualForge/spike000/logs/spike002-0l-control/`：stdout、stderr、latest、debug、meta、mods-list、2 dump、完整 crash-report。
- `D:/DualForge/spike000/logs/spike002-0l-treatment/`：同上述文件、2 dump；无 crash；runtime-index.json 列两轮全部文件与 SHA。

- [x] 未修改既存0e probe、0j compat、冻结0b/0d制品、宿主/依赖/Java版本。
- [x] mods.toml、manifest、原构造器语义未变；前后源码已完整归档。
- [x] 未引入新Mod/业务API/post/人工监听器登记/宿主补丁；未吞新异常。
- [x] 未覆盖独立历史Label；清理只针对既定服务端现场，旧现场日志已归档可恢复。
- [x] C/T各一次；T正常 stop，之后 Java进程退出；服务器临时日志/mods保留T现场。
- [x] 未把声明方法数伪称监听器数，未把注册返回推为分发成功，未扩写架构。
