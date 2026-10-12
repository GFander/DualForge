# Spike-002-0f 静态取证报告

日期：2026-10-12（Asia/Hong_Kong）。执行：root；流程依据 R-0f-1，按用户指示由同一智能体顺序执行与复核，不称独立智能体审查。

依据：`Spike_002-0f.md` v1.0，SHA-256 `75D287EDD1850DEFEDE17007F7CCAFED7E4CFD265FDD27547CB8FF8393E285E8` /13594 B；002-0e 已正式裁定 VALID-002-0e-1。自评 PASS-002-0f-1，范围仅静态二进制契约与直接入口。

## 一、输入与工具

冻结的八项制品/守卫及说明书共九项初检均匹配，路径与完整 SHA/字节数在 `spike000\logs\spike002-0f-artifacts\inputs-before.json`；终检在 `inputs-after.json`。核心引用：

| 制品 | SHA-256 |
|---|---|
| 0e失败probe，1848B | D61224F3014F0ADFF85D10EEDEA414763C231367F08EDB99CE99DD2C8935F1C9 |
| 0e Run T dump，1381B | 55D81709DC8C41A8FB25189BEF87D41F5F1920C721C02B2027F9EEE764924FE8 |
| compat，21180B | 4CD8195F7AA03B8B595F646CDEAC16454D03D5E3862B5E2F388C820CF2FA2FEF |
| Forge universal，2617057B | 88EF6625122CFDB9D7B3176BD61B8C0692A191BAD0EDD77FFDDC6A765EE4979C |
| Forge bus，58819B | D2A7BCECB80B33F90854A003DC295960263D78435D9D6016C427D711510CB999 |
| NeoForge universal，3552847B | E0A08BAE5CEA95BE96C09770530F5643BB183AF8AF59C64694EB346CB4C93FA4 |
| NeoForge bus，30340B | 705E24F37752809499A5CD0C7DD31EAABF3D933C5D3B2060168E94E9EF62593A |
| harness，8948B | 84C7E2C06BF0A1D2AC0D0E2D5AD20482F1357668EC8807DAD642DA55944F77D0 |

工具 `javap 21.0.10` / `jar 21.0.10`，固定 JDK。没有调用 javac、启动服务端或初始化被调查类。

每个正式采集调用有 `*.command.txt`、`*.stdout.txt`、`*.stderr.txt`、`*.exit.txt`；完整实际参数不使用占位符。native 本地化输出的编码修正及补采见 §八和 `capture-note.md`。全文原始反汇编是附件，本报告明确为事实整理和选录。

## 二、002-0e 失败边界与指令组

`[E]` 已观测：marker/BUS、P1、`ForgeProbe.<init>(ForgeProbe.java:14)`、CNFE cause 与 FML 上报链。未观测：成功取得 EVENT_BUS、register 调用、监听器登记或分发。

0e 原 probe 的新增指令（`probe-code.stdout.txt`）仍为：

```text
      43: getstatic     #33                 // Field net/minecraftforge/common/MinecraftForge.EVENT_BUS:Lnet/minecraftforge/eventbus/api/IEventBus;
      46: aload_0
      47: invokeinterface #39,  2           // InterfaceMethod net/minecraftforge/eventbus/api/IEventBus.register:(Ljava/lang/Object;)V
      52: return
```

Run T dump (`dump-code.stdout.txt`) 的构造参数已经是 `Lnet/neoforged/bus/api/IEventBus;`，但上述成员引用仍是 Forge owner/type。没有精确 BCI 测量，不能据此声称字段解析或接口调用曾完成。

## 三、Forge 侧契约

`forge-universal-entries.stdout.txt` 确认当前冻结 universal 含 `net/minecraftforge/common/MinecraftForge.class`。`forge-MinecraftForge-code.stdout.txt`：类 public，字段 `public static final IEventBus EVENT_BUS`，descriptor `Lnet/minecraftforge/eventbus/api/IEventBus;`。完整类及 initializer 在 code 与有效 verbose-raw 附件。

原 Forge 接口直接声明：

```text
public abstract void register(java.lang.Object);
  descriptor: (Ljava/lang/Object;)V
```

来源 `forge-IEventBus-code.stdout.txt`。接口无直接父接口需要展开。compat 内同名 Forge IEventBus 无成员，来源 `compat-IEventBus-code.stdout.txt`。编译契约与兼容空壳不是一个可互换接口实现。

## 四、NeoForge 候选

限定 NeoForge universal 条目集合内：`net/neoforged/neoforge/common/NeoForge.class` 存在；`net/neoforged/neoforge/common/MinecraftForge.class` 不存在。范围及精确检查在 `neo-candidates.json`，不外推整个有效类搜索空间。

存在候选 NeoForge 的字段为：

```text
public static final net.neoforged.bus.api.IEventBus EVENT_BUS;
  descriptor: Lnet/neoforged/bus/api/IEventBus;
```

来源 `neo-NeoForge-code.stdout.txt`。NeoForge IEventBus 直接声明 `public abstract void register(java.lang.Object)`，descriptor `(Ljava/lang/Object;)V`（`neo-IEventBus-code.stdout.txt`）。方法 descriptor 相同，但接口二进制名称不同；字段 descriptor 也不同。

模块工具显示两个 universal 均为自动模块；NeoForge bus 为显式 open 模块 `net.neoforged.bus`，exports `net.neoforged.bus` / `net.neoforged.bus.api`；Forge bus 为模块 `net.minecraftforge.eventbus`（详见有效 module-raw 附件及 manifest）。工具派生名字和声明不是 ModLauncher 运行层对象同一性证明。public/exports 不证明本轮跨层访问成功或两总线语义等价。

## 五、初始化与直接实现入口

Forge MinecraftForge 的完整 `<clinit>`（`forge-MinecraftForge-code.stdout.txt`，以下原文）：

```text
       0: invokestatic  #92                 // InterfaceMethod net/minecraftforge/eventbus/api/BusBuilder.builder:()Lnet/minecraftforge/eventbus/api/BusBuilder;
       3: invokeinterface #98,  1           // InterfaceMethod net/minecraftforge/eventbus/api/BusBuilder.startShutdown:()Lnet/minecraftforge/eventbus/api/BusBuilder;
       8: invokeinterface #101,  1          // InterfaceMethod net/minecraftforge/eventbus/api/BusBuilder.useModLauncher:()Lnet/minecraftforge/eventbus/api/BusBuilder;
      13: invokeinterface #104,  1          // InterfaceMethod net/minecraftforge/eventbus/api/BusBuilder.build:()Lnet/minecraftforge/eventbus/api/IEventBus;
      18: putstatic     #108                // Field EVENT_BUS:Lnet/minecraftforge/eventbus/api/IEventBus;
      21: new           #112                // class net/minecraftforge/common/ForgeInternalHandler
      24: dup
      25: invokespecial #114                // Method net/minecraftforge/common/ForgeInternalHandler."<init>":()V
      28: putstatic     #115                // Field INTERNAL_HANDLER:Lnet/minecraftforge/common/ForgeInternalHandler;
      31: invokestatic  #119                // Method org/apache/logging/log4j/LogManager.getLogger:()Lorg/apache/logging/log4j/Logger;
      34: putstatic     #7                  // Field LOGGER:Lorg/apache/logging/log4j/Logger;
      37: ldc           #125                // String FORGE
      39: invokestatic  #126                // Method org/apache/logging/log4j/MarkerManager.getMarker:(Ljava/lang/String;)Lorg/apache/logging/log4j/Marker;
      42: putstatic     #13                 // Field FORGE:Lorg/apache/logging/log4j/Marker;
      45: return
```

NeoForge 候选的完整 `<clinit>`（`neo-NeoForge-code.stdout.txt`）：

```text
       0: invokestatic  #22                 // InterfaceMethod net/neoforged/bus/api/BusBuilder.builder:()Lnet/neoforged/bus/api/BusBuilder;
       3: invokeinterface #28,  1           // InterfaceMethod net/neoforged/bus/api/BusBuilder.startShutdown:()Lnet/neoforged/bus/api/BusBuilder;
       8: invokedynamic #31,  0             // InvokeDynamic #0:check:()Lnet/neoforged/bus/api/IEventClassChecker;
      13: invokeinterface #35,  2           // InterfaceMethod net/neoforged/bus/api/BusBuilder.classChecker:(Lnet/neoforged/bus/api/IEventClassChecker;)Lnet/neoforged/bus/api/BusBuilder;
      18: invokeinterface #39,  1           // InterfaceMethod net/neoforged/bus/api/BusBuilder.build:()Lnet/neoforged/bus/api/IEventBus;
      23: putstatic     #43                 // Field EVENT_BUS:Lnet/neoforged/bus/api/IEventBus;
      26: return
```

`BusBuilder.builder()` 两侧均显式 new 各自 `BusBuilderImpl`；两个 Impl.build() 均直接 new 对应 EventBus（offset0），以本侧 BuilderImpl 参数 invokespecial 构造器（offset5），areturn（offset8）。原文在 `forge/neo-BusBuilder-code` 与 `forge/neo-BusBuilderImpl-code`。

因此静态形成链可到各自 EventBus 构造器入口；构造器的日志、LockHelper、ListenerList、反射/ASM/Minecraft 等直接依赖仅登记边界，不追到整个事件系统，也不据此判运行可达。

Forge `EventBus.register(Object)` 完整入口（`forge-EventBus-code.stdout.txt` L380 起）：先查 listeners.containsKey；按 Class/实例分派到本类 `registerClass` / `registerObject`，最后 return。两个内部扫描方法为直接调用边界，没有把它们的行为等价作为结论。

NeoForge `EventBus.register(Object)` 完整入口见 `neo-EventBus-code.stdout.txt` L102 起。直接事实包括 listeners.containsKey；Method/Class/实例区分；`checkSupertypes`、`Class.getDeclaredMethods`、`Method.isAnnotationPresent(SubscribeEvent)` 与 `registerListener` 调用点。没有继续展开这些直接依赖。

**与本探针相关的新静态差异**：该方法对本次声明方法循环的计数若为 0，会在 offset263–284 构造并抛 `IllegalArgumentException`。原常量与抛点选录：

```text
     258: iload         5
     260: ifne          285
     263: new           #83                 // class java/lang/IllegalArgumentException
     266: dup
     267: ldc           #131                // String %s has no @SubscribeEvent methods, but register was called anyway.\nThe event bus only recognizes listener methods that have the @SubscribeEvent annotation.\n
     278: invokevirtual #121                // Method java/lang/String.formatted:([Ljava/lang/Object;)Ljava/lang/String;
     281: invokespecial #95                 // Method java/lang/IllegalArgumentException."<init>":(Ljava/lang/String;)V
     284: athrow
     285: return
```

这是已证条件分支，不是本轮 register 实际调用证据。`[H]` 当前仅构造函数/打印的 ForgeProbe 直接变换到该入口后，可能出现「无 @SubscribeEvent 方法」异常；仍须后续运行，且不得由此断言 Forge 与 NeoForge 的整个注册语义相等或不等。

## 六、差异矩阵与候选资格

| 比较项 | Forge 原契约 / 0e引用 | compat 当前契约 | NeoForge 候选 | 静态结论 |
|---|---|---|---|---|
| 类 owner | net/minecraftforge/common/MinecraftForge | 无该类（0e已测P1）；本轮只读现有jar | net/neoforged/neoforge/common/NeoForge | 有不同owner候选，跨层访问未测 |
| 字段 | EVENT_BUS: Lnet/minecraftforge/eventbus/api/IEventBus;，public static final | 空接口仅补类型，未补该字段 | EVENT_BUS: Lnet/neoforged/bus/api/IEventBus;，public static final | name相同、descriptor不同 |
| 接口二进制名 | net/minecraftforge/eventbus/api/IEventBus | 同名空接口 | net/neoforged/bus/api/IEventBus | Class身份不同 |
| register | 原接口public abstract register(Object):void；0e invokeinterface指向Forge | 未声明register | Neo接口public abstract register(Object):void | 方法descriptor同，owner不同；空壳不满足方法声明 |
| 值形成 | builder/startShutdown/useModLauncher/build/EventBus | 未建立全局bus值 | builder/startShutdown/classChecker/build/EventBus | 调用链和附加初始化不同，不证语义等价 |
| 模块 | 原Forge模块声明见附件 | 既有SERVICE输入 | Neo universal automatic + net.neoforged.bus open module | 声明不证运行可访问 |

每行原文来源依次为两侧 universal-code、两侧 IEventBus-code、compat-IEventBus-code、BusBuilder/Impl/EventBus-code、module-raw与manifest附件；原文件全文可定位对应成员标题/offset，不使用推断代替缺席。

三候选资格：

1. **仅改缺失类 owner：已知契约差异阻断。** 仍保留 Forge 字段 descriptor 和 Forge invokeinterface owner，与 NeoForge 声明不一致。这里只认定引用不匹配，不预言 JVM 的确定异常次序。
2. **保留 Forge 签名做桥接：UNKNOWN（待设计）。** 至少要有 Forge MinecraftForge 类、准确 Forge 类型 EVENT_BUS 字段、声明 register(Object) 的 Forge IEventBus 及能满足该类型的实例/委托。现有空壳并不满足；本轮未生成桥接类，不选实现。
3. **限定本语句的一致成员引用变换：静态必要条件满足但运行未验证。** getstatic owner 改为 NeoForge、字段 descriptor 改为 Neo IEventBus、invokeinterface owner 改为 Neo IEventBus；`EVENT_BUS`、`register`、`(Ljava/lang/Object;)V`、getstatic/invokeinterface操作码与接口调用标志不变。这是三处二进制引用协同契约，不是仅一个类名替换；尚有初始化/跨层/无订阅方法等未验证边界。不设计 transformer，不称修复成功。

## 七、自评与结论上限

自评 PASS-002-0f-1：输入匹配；限定候选存在/缺席证据完整；两侧静态形成链及直接 register 入口完整；矩阵及三候选资格已填写；条件与 UNKNOWN 明示；原始输出全文归档。有损本地化捕获已保留并补采，不使用有损文本作为最终原文。

`[F]` owner/descriptor/访问标志、initializer和直接方法字节码。`[E]` 工具版本、退出码、哈希复测、精确输入内的条目存在/缺席。`[H]` 运行访问、三引用变换可行性、无订阅方法异常预测和总线语义。`[D]` 本轮执行冻结静态范围，不自行启动下一实验。

本轮没有实测 P2，未修复 P1，未证明字段值/注册/事件分发成功。结论仅服务于下一轮最小候选设计。

## 八、偏差与不变性

用户改为不使用子代理，故 root 按执行→顺序复核完成；这是治理安排改变，非运行变量；正式门槛见 R-0f-1。原独立0e裁定保持。

采集第一阶段的 UTF-8 解码导致模块中文提示/verbose日期有损。修正为 native BaseStream 原字节保存后，以相同工具/输入参数补采四份module及七份verbose，旧捕获保留，补采使用 `*-raw` 名称。code/entries均ASCII、无该损失；初始化/注册直接入口 extra 从一开始用原字节保存。细节见 capture-note.md；这是采集问题修复，没有改被调查制品、扩大检索或运行代码。

全输入终检不变。没有服务端运行、javac构建、修改JAR/源码/harness/配置/世界或历史日志；只新增本轮证据、脚本及报告。所有必需工具退出0。可选 Neo MinecraftForge 不存在作为有效静态结果，不触发 STOP。

## 九、完整证据清单

根目录：`D:\DualForge\spike000\logs\spike002-0f-artifacts`。`evidence-index.json` 列出全部采集文件、脚本、说明、输入前后记录的路径/字节数/SHA；清单自身不参与自引用哈希。完整实际命令及原始stdout/stderr/exit分别保存。

有效原文选择：core的 `*-code.stdout.txt`；七份补采 `*-verbose-raw.stdout.txt`；四份 `*-module-raw.stdout.txt`；extra的 builder/implementation code/verbose 原字节；全量 jar条目与manifest。旧无raw verbose/module为已披露的有损捕获，保留而不使用。

本报告外置于 Dev Manuals，报告指纹登记在下一顺序复核文书。禁止以本报告替代原始全量输出。
