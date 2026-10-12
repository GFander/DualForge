# Spike-002-0j 执行与顺序裁定报告

2026-10-12。root按用户要求串行执行、复核与裁定，不称独立代理审查。冻结说明书SHA `F83ACB24E9D2F71CC5DC67F6D845C5EED24C9F02BC3E436A0C04397E97D79A4E`。

**VALID-002-0j-1：限定零订阅探针形态的API路径生命周期通过。** C复现S1；T在真实profile条件成立后执行兼容空返回，FML/服务端到达Done并正常停止。没有新增监听器、没有吞异常、没有Neo register在T成功登记的证据。

## 一、冻结制品与变量

| 项 | SHA-256 / 字节数 |
|---|---|
| 两轮同一0e probe | D61224F3014F0ADFF85D10EEDEA414763C231367F08EDB99CE99DD2C8935F1C9 /1848 |
| 0g参照jar（保留） | F0D51E46533A135A9ECA0FA859805E04E9FBB22E8759B1A29524319E37B4C739 /22432 |
| 两轮同一0j新jar | D724FB489A3A1F1180A5FBA09DFF2FECD01FBD6F8AE33EE727851AB211FA44AB /23861 |
| 两轮同一harness | 33049C802F8AF4E79665A541DAB9A9DFAAE0CFCFEE8CDA8702A8309DD5DBF64A /9044 |
| C dump | A9ABBB9440DD49A16BB5749D46744F01D96FAF2796B189920BD8788682705EF1 /1359 |
| T dump | E0EB498C24EB3BA95E6E6D3BB13511AFA84C00AC588C844DFD0BFA4156893BC7 /1405 |

环境仍MC1.21.1/NeoForge21.1.256/FML4.0.45/ModLauncher11.0.5/msJava21.0.10。共同 `-Dforgecompat.2a.rewrite=true -Dforgecompat.0g.apiRewrite=true`，唯一变量0j.emptyRegistration=false/true。Xmx3G/Xms1G、其他参数同一harness。

## 二、构建与不变性

隔离compat-002-0j源树由0g src对src-after14项哈希一致后复制；只新增ForgeEmptyRegistration.java、修改transformer按冻结定义读取属性和改变精确调用节点。前后源码归档，原0g/0d源未改。新jar26条目/17内容文件；原15文件逐字节不变，transformer改变、helper新增。四项前置及委托loader、所有4services/manifest未改。

编译classpath仅额外加入已冻结Neo bus8.0.5用于helper签名；实际10源命令、stdout/stderr/exit0与打包exit0完整记录。classpath初/终指纹在artifacts。新helper源码逐字符合冻结定义，不catch异常，不对非固定形态返回空操作。

harness最终机械单步diff只有两Label追加，累计diff对原基线亦保存。编辑时短暂把两个旧Label顺序误换，已在任何构建/运行前纠正；最终硬门严格按旧文本+两项新增比较通过，运行期间脚本哈希固定。

## 三、C控制门及完整异常来源

spike002-0j-control：EXITED_EARLY/0/10.1s/Done空，EMPTY_ROUTEfalse0；ctor REWRITE1/API_REWRITEtrue1、marker/BUS与委托命中。逐轮dump与0g T同值，runtime mods恰D612 probe+D724 jar。

首异常S1：IllegalArgumentException（has no @SubscribeEvent methods），Neo EventBus.register:135与ForgeProbe:14帧。没有helper日志；没有transformer ERROR。完整首栈、FML上报链及crash原文在 `spike000\logs\spike002-0j-control\spike002-0j-control.log`、stdout/debug与crash-reports；不从乱序stdout拼造栈。控制门单独存control-gate.md，先于T通过。

## 四、T实际信号与生命周期

spike002-0j-treatment：DONE_STOPPED_GRACEFULLY/0/22.7s，无crash-report，stderr0B；stdout/latest/debug均无ERROR/FATAL，S1零命中。实际stdout原文：

```text
[FORGECOMPAT-XFORM] emptyRegistrationEnabled=true
[FORGECOMPAT-XFORM] EMPTY_ROUTE enabled=true hits=1
[FORGECOMPAT-EMPTY] legacy-empty-noop target=com.example.forgeprobe.ForgeProbe declaredMethods=0 customPublic=0
[06:40:24] [Server thread/INFO] [minecraft/DedicatedServer]: Done (0.257s)! For help, type "help"
```

helper日志证明真实目标条件成立且进入return分支：固定class名、直接继承Object、声明方法0、公共非Object方法0。没有helper delegate日志；T没有调用Neo register进行该目标登记。compat helper的跨层调用仅本路径实际进入，不推广所有类/成员的可访问性。

源码的空分支不是把所有异常吞掉；未匹配形态仍直接委托Neo register，不使用反射写field/Unsafe操作。空注册语义依据0h静态条件与0i-1原Forge裸对象返回实验；夹具与ForgeProbe不为同一class，不假装已测试原ForgeProbe完整宿主加载。

Done后既有harness等待并stop，正常结束；不是用exit0代替Done。未分析后续额外API、未加入监听方法或事件分发操作。

## 五、重写后字节码复核

C/T各20条指令。四次println、字段GETSTATIC NeoForge.EVENT_BUS、ctor Neo IEventBus参数均不变。唯一语义指令变化是offset47：

```text
C: invokeinterface net/neoforged/bus/api/IEventBus.register:(Ljava/lang/Object;)V
T: invokestatic com/example/forgecompat/ForgeEmptyRegistration.register:(Lnet/neoforged/bus/api/IEventBus;Ljava/lang/Object;)V
```

原receiver+target变为static两个参数，操作码/owner/desc/itf按定义耦合变化。原return52→50仅是接口调用5bytes改静态调用3bytes的布局差异；未新增指令、未改打印/控制流。CP索引差异忽略，原始javap/verbose全文保存，source-after与归档终检匹配。没有将API链接通过推成总线整体行为兼容。

## 六、复核裁定与范围

verify.ps1核验C S1、T真实profile/空路径/Done、无delegate/异常、指令数及两行变化（调用和return布局）；build-evidence final核验原输入、两固定输出和源码不变。执行与复核同一智能体，独立性限制披露。

确认VALID-002-0j-1。局部实验样本D724 jar可复现本固定API零方法对象路径，不作为任意Forge Mod可用基线。原A93零API probe与0d基线继续保留。未验证有SubscribeEvent方法对象、事件登记、事件发送、线程/优先级/取消等语义；全局bus对象身份未打印测量。

[F]冻结代码、指令/descriptor、限定profile条件及静态分支。[E]K1日志、原件/源码内容哈希、原Forge夹具返回、T空路径和Done。[H]一般注册兼容、原ForgeProbe完整原宿主加载、监听器事件语义仍未知。[D]局部空对象规则，不扩张v1.1架构。

## 七、归档与下一步

C/T各唯一服务端运行一次；全量stdout/stderr/latest/debug/meta/mods-list/crash（C）/dump在两个运行目录，原始完整异常不截断。artifacts内完整命令/退出码、构建对照、源码索引、harness差异、验证JSON与证据/运行清单；源码/jar前后副本在compat-002-0j/archive。Java已退出，不覆盖旧Label。

下一阶段应另立**带真实Forge订阅方法的最小探针契约**，先取证事件类与注解/方法契约再单变量实验；本轮不预选新业务事件或直接试探DeferredRegister。若要泛化空注册策略，先独立证明更广形态，不把本固定类名规则当作通用适配。
