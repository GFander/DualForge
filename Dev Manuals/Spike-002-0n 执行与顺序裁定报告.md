# Spike-002-0n 执行与顺序裁定报告

2026-10-12。结果：**PASS-002-0n-1**。root 单智能体顺序执行→复核→裁定，非独立审查。用户在本轮收尾时要求「当前轮执行完后停止」；归档结束即停，不启动或编制下一执行轮。

## 一、范围与环境

独立 CLI Java 夹具，Microsoft OpenJDK21.0.10+7-LTS；Neo bus8.0.5，ASM9.10.1，log4j-api/core2.22.1，typetools0.6.3。不是Minecraft/FML运行；没有启动服务器、加载新Mod或读写world。宿主0l T现场/日志/mods/harness在实验前后SHA完全不变。

唯一运行变量：`-Dforgecompat.0n.post=false/true`。两轮同一个runtime jar和同一完整classpath，实际Native command仅该值不同。两轮正常 exit0、stderr均0字节；C/T各一次。

## 二、固定输入与产物

| 输入/产物 | SHA-256 | 字节数 |
|---|---|---|
| 冻结说明书 Spike_002-0n.md | DAFE0D5B3AAA708342941E26555DF739888F7583C39C5A1C63874F58BB60276C | 见inputs-before |
| runtime jar：subscriber-002-0n/build/subscriber-002-0n.jar | A452DC6A6333A6B290001ADF2B5E1FAB14AF9D987BAD0B0AA6634E3D77974920 | 3251 |
| 原Forge注解编译的SubscriberFixture.class | 60B035C6F3CBF666B7364086FFD97FADCA2267F788AD587E4FB73C9856C02D84 | 1077 |
| 默认方法注解翻译后的SubscriberFixture.class | 38F72EAC4F3FDA55D3D967F3F2F0F6A3E4E4FCC3000E37E448376E46AF158348 | 1067 |
| 从0l T原样复制的ProbeEvent.class | 15089EC00BC3EA3867E4746DF6FDEB612CBE6C899D52F725BDD8313A7FEAD6AD | 228 |

runtime jar为8条目/4内容（manifest+3class）；包含FixtureDriver、SubscriberFixture、ProbeEvent，没有ProfileRewriter或Forge库。编译listener使用同一0l probe及原Forge bus；离线重写只更换一个默认方法注解descriptor。编译driver使用已翻译class和Neo运行依赖。全部实际javac/jar/java参数、原始stdout/stderr及exit在附件。

原/新源（3个implementation/tool源）、original/translated class、固定事件字节及runtime jar均归档于工作目录archive；完整固定指纹含source、jar、依赖、冻结说明书、既存服务器mods/logs与harness。final-check证明不变。

## 三、实现与字节码门

- listener只有public实例hits字段、Object默认构造器和一个实例onProbe(ProbeEvent)。方法体实际hits++后println，计数不是driver人工赋值。
- 源码注解是Forge SubscribeEvent；runtime方法注解翻译为Neo SubscribeEvent，默认成员不变。全部opcode/成员操作数（去CP编号与显示对齐）相同。
- 加做完整canonical class验证：恢复新class的**唯一一个**注解descriptor至Forge，原/新ClassNode以相同ClassWriter重序列化，全部字节相同。不是只抽检几条opcode，也不是把常量池变化伪称新增语义。完整原始verbose保留，CP编号/排列确实不同。
- driver offset39–42 builder/build；69 register；115唯一Neo IEventBus.post调用；没有对onProbe的直接或反射invoke，不写hits。事件构造器为0l T的原字节，第一次实际实例化在本夹具，未改super指令。
- 独立bus默认正常启动，无startShutdown、无宿主全局bus.start；runtime不包含Forge bus/universal、0l compat/probe jar。

## 四、Run C 完整stdout原文

```text
[SUBSCRIBER-FIXTURE] register returned hits=0
[SUBSCRIBER-FIXTURE] post skipped
[SUBSCRIBER-FIXTURE] FINAL hits=0 expected=0
```

exit0，stderr空。未出现HANDLER REACHED。C硬门通过后才执行T。

## 五、Run T 完整stdout原文

```text
[SUBSCRIBER-FIXTURE] register returned hits=0
[SUBSCRIBER-FIXTURE] HANDLER REACHED hits=1
[SUBSCRIBER-FIXTURE] post returned sameEvent=true
[SUBSCRIBER-FIXTURE] FINAL hits=1 expected=1
```

exit0，stderr空。无新异常，无stack trace或crash-report；不虚构不存在的日志。一次post导致handler marker一次、计数1，返回对象同一性实际用==检查。

## 六、顺序复核与裁定

1. [E] C未post/最终0、T一次post/handler marker一次/最终1，固定同jar同依赖，只有一个属性开关，PASS-002-0n-1成立。
2. [F] driver没有人工或反射调用listener，没有手写hits。唯一状态改变在handler字节码；[E] 因此实际调用证据不由register返回或post返回推断，而由method marker与状态变化给出。
3. [E] 翻译后的Forge默认注解实例listener，在正常启动的**独立Neo bus**中得到一次具体自定义事件。[D] 结论仅限这个夹具和profile。
4. [H]/UNKNOWN：原ForgeProbe的空onProbe实际invoke、FML全局总线送达、Forge post(Event):boolean适配、取消/结果/MONITOR/继承/线程/业务事件兼容。不能把本轮写成「Forge Mod事件系统已兼容」。
5. [D] 用户停止指令生效：完成本报告和事实回写后停止自动推进，不新增下一轮说明书/实验。原0l专用runtime仍冻结，不替换0d正式零API基线。

Confidence HIGH（隔离夹具送达事实）；一般兼容性UNKNOWN。

## 七、工具偏差与修复

两次工具问题均在C/T实验之前，未改冻结实验输入：

- prepare首次因PowerShell路径数组缺括号，在读取host快照前失败，没有进行build或host写入；修正后执行一次真实prepare。
- build文本比较初次因CP编号长度变化带来javap `//`前对齐空格差异，误报指令变化。原/新完整证据保留；仅修正比较器去CP编号/注释前显示padding，以finish-build从既存class继续，不重新编译或覆盖已采记录。随后完整canonical验证排除其它class语义变化。

详情tool-deviations.txt。原始实验Implementation源及jar在运行中/后未改；此为采集器修复，不是新断层修复或额外实验运行。不使用子代理。

## 八、完整附件与边界

[证据索引](D:/DualForge/spike000/logs/spike002-0n-artifacts/evidence-index.json) 列84份文件，包含工具源码/原字节命令输出/exit、C/T完整日志、classpath、输入/输出指纹、完整javap、canonical验证和最终核对，逐项SHA复测通过；索引自身不纳入自引用。独立Java夹具没有latest/debug/meta.json的FML概念，不借宿主日志冒充本轮日志。

- [x] 同一jar/依赖，仅post属性变化；C/T各一次，exit0、stderr空。
- [x] 原事件class字节不变；源/原译class/实际编译与运行命令完整归档。
- [x] 未启动服务器、未改服务端/harness/host制品/日志/mods/world。
- [x] 未绕过bus.register修改内部监听器集合、未反射invoke、未改hits以伪造结果；无异常吞掉。
- [x] 未引入新业务API/Mod、未修改核心、未下载或调整宿主依赖。
- [x] 未将独立送达外推为全局FML送达或Forge post返回兼容。
- [x] Java进程退出；按用户要求本轮后停止。
