# Spike-002-0e 裁判审查与裁定报告

日期：2026-10-12（Asia/Hong_Kong）。独立裁判：`referee_oct11`。实践方：`executor_oct11`。任务：R-0e-1。

## 一、裁定

**确认 VALID-002-0e-1：P1 首失败坐标成立。** K1 control 到达 Done；treatment 唯一新增语句在 `ForgeProbe.java:14` 以 `NoClassDefFoundError: net/minecraftforge/common/MinecraftForge` 失败，cause 为 `ClassNotFoundException: net.minecraftforge.common.MinecraftForge`。根因、构造函数帧、新增语句/指令组与 FML 上报链闭合。

这不是 register 已被调用的证据，也不是精确 BCI 测量。P2 本轮未观测；未证明 EVENT_BUS 字段成功解析或有值；不推一般 Forge API 兼容性。

采纳实践报告的偏差披露。没有需重跑或回改冻结说明书的阻塞。首失败未修复；下一轮仅准入 002-0f 静态契约取证，具体执行须依总指挥审过的独立冻结说明书。

## 二、输入与独立核验范围

- 冻结说明书 `Spike_002-0e.md`：`072ADCCB6A063E1C037836769058958E08D376E807F774EC7C69F052AB7C6F2A` / 43468 B，重算一致。
- 实践报告 `Spike-002-0e 执行报告.md`：`FF19DF21F6211F73A45B891F757F69C473FD6AA20EF8B260C359E9AB71C5464D` / 62467 B，已完整阅读，重算一致。
- 本裁判未执行服务端、未构建或修改产物，仅只读校验。本报告与下一轮说明书为新输出，不改历史文件。

独立复核动作及结果：

1. 重算终检清单全部 15 项冻结引用的 SHA/字节数：15/15 匹配；其中 harness 使用许可改动后的指纹，历史 dump 使用首轮前另存副本。
2. 逐项解析实践报告 §十二清单，重算全部 **98/98 文件**的 SHA 与字节数，零缺失、零不符。含 83 份 artifacts、7 份 control、8 份 treatment；这验证完整性，不把“存在文件”代替内容判断。
3. 直接只读打开新旧 JAR，每个内容条目重算：3 个内容文件中 manifest 与 mods.toml 完全相同，唯一差异为 ForgeProbe.class；两个 jar 的 7 条目结构一致。新 class 1391 B，旧 class 1172 B。
4. 本裁判重跑新旧 `javap -p -c -constants`，与实践存档按换行归一后的文本逐字一致。复读完整 verbose：super、四次 println、BUS StringConcatFactory 配方、@Mod("forgeprobe") 保持；无新增字段/方法/分支/异常处理。新增指令为 offsets 43/46/47 的 getstatic/aload_0/invokeinterface；return 43→52。既有语句行号 +1，新增 line14 与 return line15 单列。
5. 本裁判直接 javap treatment 逐轮 rewritten dump，constructor descriptor 是 NeoForge IEventBus，新增字段/方法引用仍为 Forge 命名空间；控制流与原新增语句相同。该 dump 的实际哈希在 §五登记。
6. 完整读取 Run C stdout/meta、Run T stdout/latest/meta/crash-report；control 全部关键签名在，Done 正常 stop；treatment 首失败及 cause、FML constructMod 帧、Mod Construction 上报和 ModLoadingException 全部存在。
7. 复读 runtime mods 清单及现存副本哈希：两件为指定 probe/compat，未包含三件 Forge compile-only 库；新 probe jar 内也没有这些库的类或嵌套制品。
8. harness 对快照机械 diff 为一行替换（仅追加两个 label），SHA 与冻结预期 `84C7E2C0…F77D0` 完全一致。复读实践 wrapper：仅哈希/路径/目录门与命令记录后调用固定 harness；没有隐藏参数变更。
9. 实际 javac/jar 参数和退出码已落盘；不复用旧 0d 的不完整命令记录。源码前后归档与两行源差异已复核。

## 三、冻结判据逐条裁定

| 判据 | 独立证据 | 判定 |
|---|---|---|
| 冻结输入及唯一语义变量 | 15项终检、源码两行新增、内容文件差异仅一个class、javap | 通过 |
| Run C 复现 0d有效基线 | offered / REWRITE1 / post-check / delegate / 四次打印 / NeoForge mod loading / Done | 通过 |
| Run T 前置与marker | 同一compat、probe入口、delegate、四次打印、rewrite1 | 通过 |
| P1/P2合格形态 | P1 NCDFE MinecraftForge；cause CNFE | P1命中 |
| 首失败位置在打印之后 | 实测marker/BUS；源码line14；直线字节码映射到43–51指令组 | 通过，不声称BCI |
| dump与终止记录 | 两轮逐轮副本、完整meta、crash-report | 通过 |
| VALID 根因→上报链 | CNFE→NCDFE/ForgeProbe.<init>→FMLModContainer.constructMod→Mod Construction/FML ModLoadingException | 闭合 |

Run C：`DONE_STOPPED_GRACEFULLY / exitCode 0 / 24.9s`，Done 原文（stdout）：

```text
[00:31:31] [Server thread/INFO] [minecraft/DedicatedServer]: Done (0.355s)! For help, type "help"
```

Run T：`EXITED_EARLY / exitCode 0 / 12.1s / doneLine null`。exitCode 0 不代表实验无失败。

首失败摘录（下列明确为 latest 的原行选录；完整栈无删节见实践报告 §七.1 和原文件）：

```text
[1210月2026 00:32:34.992] [modloading-worker-0/ERROR] [net.neoforged.fml.javafmlmod.FMLModContainer/LOADING]: Failed to create mod instance. ModID: forgeprobe, class com.example.forgeprobe.ForgeProbe
java.lang.NoClassDefFoundError: net/minecraftforge/common/MinecraftForge
	at TRANSFORMER/forgeprobe/com.example.forgeprobe.ForgeProbe.<init>(ForgeProbe.java:14) ~[forgeprobe.jar%23122!/:?]
```

同一完整栈内 cause 原文：

```text
Caused by: java.lang.ClassNotFoundException: net.minecraftforge.common.MinecraftForge
```

上述观测建立类符号缺失坐标；字段名/descriptor 与接口方法调用是新增语句中的后续静态契约，不能因本轮 P1 命中就把它们判为已执行或已接受。

## 四、偏差与限制处置

1. **跨日恢复**：静态构建 10月11日、运行 10月12日。运行前重测，C/T在同一次恢复阶段先后完成，两个输入 probe 与同一 compat/harness 未变化；采纳，不重跑。
2. **stdout乱序**：C 的 marker/分隔线、T 的栈与 crash 打印有捕获顺序差异。原文保留，latest 与磁盘 crash 各自为独立原文；不拼成不存在的单一日志、不猜成因。冻结契约已允许这类捕获限制，且有字节码/行号与结构化异常链，VALID不受影响。
3. **首失败之后采集**：固定 harness 完成异常链与终止信息；“Negative index in crash report handler”等后续内容只作附件，没有转为新探针目标；符合边界。
4. **世界状态**：按固定 harness 自然保存，未声称整个服务端文件树逐字节一致；本轮失败在构造函数新增语句，未把无关世界变化当变量验证。
5. **记录措辞微校准**：实践报告 §四“输出文件为空”实际对应 javac/jar 输出文件各 2 B（CRLF），应理解为“无实质诊断文本”，不是 0 字节。清单已经准确列出 2 B，不影响编译/打包退出0或任何实验判据；本裁定登记，不覆盖原报告。
6. 没有通过本轮验证 register 方法是否执行、监听器是否登记、事件分发或跨命名空间整体兼容。`getstatic`具体解析失败 BCI 未被测量，不升级为[F]/[E]。

## 五、产物与证据冻结登记

| 对象 | SHA-256 | 字节数 |
|---|---|---|
| 原零API probe（保留） | `A93A823C988BFD6A8F6EA132D189D9DE732B0E5951F4BF74980D51BB63374E22` | 1772 |
| 002-0d compat基线（保持） | `4CD8195F7AA03B8B595F646CDEAC16454D03D5E3862B5E2F388C820CF2FA2FEF` | 21180 |
| 002-0e失败探针（build及archive同值） | `D61224F3014F0ADFF85D10EEDEA414763C231367F08EDB99CE99DD2C8935F1C9` | 1848 |
| 历史dump归档及Run C dump | `5E2F78DB2B0909D7368A185F618C3D080C00B732279B105CD5B8010FD66D9295` | 1162 |
| Run T dump | `55D81709DC8C41A8FB25189BEF87D41F5F1920C721C02B2027F9EEE764924FE8` | 1381 |
| harness许可变更后 | `84C7E2C06BF0A1D2AC0D0E2D5AD20482F1357668EC8807DAD642DA55944F77D0` | 8948 |

路径：`spike000\probe-002-0e\build\forgeprobe.jar`、`archive\forgeprobe-002-0e.jar`；`spike000\logs\spike002-0e-control` / `spike002-0e-treatment` / `spike002-0e-artifacts`。三处日志/证据目录的完整 98 文件指纹见实践报告 §十二，已经独立复算。源码前后副本与采集脚本一起保留。

冻结的是本轮失败样本及证据，不把失败探针叫作“已可用兼容基线”。002-0d compat 基线继续保持。

## 六、下一轮与回写裁定

**002-0f = 静态二进制契约取证**：用固定 Forge/NeoForge制品核对全局 EVENT_BUS 候选类/字段/接口方法及有界初始化入口，输出差异矩阵、候选资格与 UNKNOWN。不存在的可选候选是有效结果；不先假设同名全局总线语义等价。

该轮不运行、不构建、不加入空壳或桥接、不修改成员引用；依独立说明书先审后冻。它为后续最小修复提供必要事实，不能跳成一次填补两个未经验证的门。

准许总指挥事实回写：002-0e 完成（VALID-002-0e-1，P1）、K1终点、未观测P2、未证register执行、新失败样本哈希；架构与手册v1.1不变。不可回写“Forge API不可兼容”或某候选已获运行验证。

## 七、事实标签

- [F] 指令、descriptor、源码行映射、签名与内容条目事实。
- [E] 98项文件完整性复测、15项冻结复测、C/T日志与进程元数据、P1及cause。
- [H] 捕获乱序成因、精确BCI、P2条件与任何修复机制可行性。
- [D] VALID确认、失败样本冻结、002-0f只读取证准入。

Confidence：HIGH，限于本轮固定输入的首失败坐标与上报链；其余事项保持未验证。
