# Spike-002-0f：EVENT_BUS 首失败后的静态二进制契约取证

状态：v0.1 送审稿，未冻结，不得据此运行或实施修复。
日期：2026-10-12（Asia/Hong_Kong）。编制：独立裁判 `referee_oct11`。
接收方：实践代理 `executor_oct11`；总指挥独立审查后冻结执行。

## 〇、前置与唯一目标

前置：002-0e 的报告已完成独立裁定，P1 首失败坐标成立。原始观察为 `NoClassDefFoundError: net/minecraftforge/common/MinecraftForge`，构造函数帧 `ForgeProbe.java:14`；它只能定位新增语句/指令组，不代表已有精确 BCI，也不证明后面的字段解析、register 调用发生。

唯一问题：在固定制品中，Forge `MinecraftForge.EVENT_BUS` 与 NeoForge 侧候选入口的类、字段和接口方法二进制契约分别是什么？哪些最小候选仍有资格进入下一轮设计，哪些差异已经能由静态事实排除？

本轮是只读制品取证，运行变量 0，不做 K1、不启动服务端、不构建或修改兼容层/探针。它不消除 P1，也不预选某个候选为语义等价总线。不借此引入第二个 Forge API。

## 一、允许与禁止

允许：读取现存输入、`Get-FileHash`、`jar tf` / `jar --describe-module`、`javap -p -c -s -constants` / `javap -p -v`、只读 ZIP 条目读取；把完整命令、stdout/stderr/退出码、原始反汇编、条目清单、对照表写到新证据目录及报告。为采集证据编写脚本是允许的，脚本仅写本轮新证据目录。

禁止：启动服务端或调用被调查类；初始化 MinecraftForge/NeoForge；下载依赖；修改任一 JAR、源码、harness、配置、世界、历史日志；添加空壳类/字段/桥接实现；编译探针或兼容层；注册监听器；修改描述符或成员引用；通过反射/Unsafe 取实例；研究下一个不相关 API。

依赖追踪只到本说明书列出的直接初始化点和直接注册方法入口。JDK、ASM、日志、Minecraft 或其他外部类型记为边界，不穷尽其应用语义。不得把字节码中一个调用点出现当作本轮该方法已经运行。

## 二、冻结输入

沿用 Java/JDK `C:\Users\Albert\.jdks\ms-21.0.10\bin`，Java 21.0.10；Minecraft 1.21.1、NeoForge 21.1.256、FML 4.0.45、ModLauncher 11.0.5。这里只调用 JDK 静态工具。

| 输入 | 绝对路径 | SHA-256 / 字节数 |
|---|---|---|
| 002-0e probe（P1 制品） | `D:\DualForge\spike000\probe-002-0e\build\forgeprobe.jar` | `D61224F3014F0ADFF85D10EEDEA414763C231367F08EDB99CE99DD2C8935F1C9` / 1848 |
| 002-0e Run T dump（只读逐轮副本） | `D:\DualForge\spike000\logs\spike002-0e-treatment\ForgeProbe.rewritten.class` | `55D81709DC8C41A8FB25189BEF87D41F5F1920C721C02B2027F9EEE764924FE8` / 1381 |
| 002-0d compat 新基线 | `D:\DualForge\spike001a\compat-002-0d\build-treatment\forgecompat-002-0d.jar` | `4CD8195F7AA03B8B595F646CDEAC16454D03D5E3862B5E2F388C820CF2FA2FEF` / 21180 |
| Forge universal | `D:\DualForge\spike000\forge-server\libraries\net\minecraftforge\forge\1.21.1-52.1.0\forge-1.21.1-52.1.0-universal.jar` | `88EF6625122CFDB9D7B3176BD61B8C0692A191BAD0EDD77FFDDC6A765EE4979C` / 2617057 |
| Forge eventbus | `D:\DualForge\spike000\forge-server\libraries\net\minecraftforge\eventbus\6.2.27\eventbus-6.2.27.jar` | `D2A7BCECB80B33F90854A003DC295960263D78435D9D6016C427D711510CB999` / 58819 |
| NeoForge universal | `D:\DualForge\spike000\neoforge-server\libraries\net\neoforged\neoforge\21.1.256\neoforge-21.1.256-universal.jar` | `E0A08BAE5CEA95BE96C09770530F5643BB183AF8AF59C64694EB346CB4C93FA4` / 3552847 |
| NeoForge bus | `D:\DualForge\spike000\neoforge-server\libraries\net\neoforged\bus\8.0.5\bus-8.0.5.jar` | `705E24F37752809499A5CD0C7DD31EAABF3D933C5D3B2060168E94E9EF62593A` / 30340 |
| harness（只读守卫） | `D:\DualForge\spike000\run-server.ps1` | `84C7E2C06BF0A1D2AC0D0E2D5AD20482F1357668EC8807DAD642DA55944F77D0` / 8948 |

输入哈希任一不符，STOP，不重建、不用另一个版本替换。NeoForge universal / bus 指纹是编制时只读计算的输入登记，正式执行须独立复测。

证据目录：`D:\DualForge\spike000\logs\spike002-0f-artifacts\`。目录已存在且非空时先交裁判，不覆盖。报告：`D:\DualForge\Dev Manuals\Spike-002-0f 静态取证报告.md`。

## 三、执行步骤

### 2F-a 冻结复测与现有失败边界

1. 完整读本说明书、002-0e 执行报告与裁判报告。确认独立裁定已准入。
2. 保存本说明书指纹、全部输入指纹及大小、`javap -version` / `jar --version` 原文；证据脚本须保留完整实际命令与退出码。
3. 对 probe JAR 和 Run T dump 各作 `javap -p -c -s -constants` 与 `-v`，保存全文；抽取新增语句的三条指令及构造函数描述符原文。
4. 明确：002-0e 已实测什么（marker、P1、构造帧）；什么仍未实测（字段值、接口调用、注册成功）。不重新运行、不把原实验扩大成全类搜索空间的否定证明。

### 2F-b Forge 侧契约

1. `jar tf` 查明 `net/minecraftforge/common/MinecraftForge.class` 的输入归属；全量条目清单保留。
2. `javap -p -c -s -constants` 和 `-v` 完整反汇编 `net.minecraftforge.common.MinecraftForge`。记录类修饰符、`EVENT_BUS` 的修饰符/名称/descriptor、`<clinit>` 完整原文、对 EVENT_BUS 的赋值点。
3. 取证 `net.minecraftforge.eventbus.api.IEventBus` 的完整成员签名：`register` 是否声明、参数/返回 descriptor、public/abstract/static 等标志。若继承而非直接声明，沿该接口的直接父接口追至声明点；只读当前冻结 bus JAR，缺外部父接口即登记边界，不下载。
4. 同样读取 compat JAR 内空壳 Forge IEventBus 的完整签名，区分「原版 Forge 编译契约」与「当前兼容空壳契约」。

### 2F-c NeoForge 候选入口取证（不预设存在或等价）

1. 保存 NeoForge universal 条目清单，精确检查 `net/neoforged/neoforge/common/NeoForge.class` 与 `net/neoforged/neoforge/common/MinecraftForge.class`。它们只是限定的检索候选，不是已认定映射。
2. 对存在的候选作完整 `javap -p -c -s -constants` 和 `-v`。逐个记录是否有 `EVENT_BUS` 字段、准确 owner/name/descriptor/访问标志及赋值指令。候选或字段不存在就是有效静态结果，不猜替代名称，不遍历全项目扩展搜索。
3. 对字段 descriptor 指向的 bus 接口，若位于冻结 bus JAR，则完整取证；并独立检查 `net.neoforged.bus.api.IEventBus` 的 `register` 声明（含必要直接父接口）。记录是否有 `register(Object):void`，不要因同名就称二进制兼容。
4. 保存相关 JAR 的 module 描述（`jar --describe-module`）与 manifest。区分“public 成员/模块声明”与“运行时跨层访问已经成功”；后者在本轮始终未验证。

### 2F-d 有界的初始化与直接方法入口

对 Forge MinecraftForge 和存在的 NeoForge 候选，完整 `<clinit>` 已在 b/c 落盘，本步仅整理其中 EVENT_BUS 值的直接形成链：

- 列出直接 new / invokestatic / invokevirtual / invokeinterface / putstatic 的 owner、descriptor、offset 和顺序；保留其他初始化调用为“同一 `<clinit>` 的其它直接调用”，不删除或假定无副作用。
- 若值由当前冻结 bus JAR 中的 builder 创建，允许反汇编该 builder 的直接 `build()` 入口与其显式实例化的实现类构造函数；到该构造函数的直接调用边界停止，不进一步展开父库或所有对象构造图。
- 对可直接确定的实现类，只读取其 `register(Object)` 或相应已证直接方法入口的完整字节码及一层父声明；若通过 factory/反射/动态分派不能唯一确定实现类，写 UNKNOWN 与精确缺口，不猜类型。
- `register(Object)` 内继续调用的反射扫描、事件处理器注册等只列直接调用名、descriptor 与边界，禁止继续构建整个事件系统调用图或声称完整行为等价。

本步的产出是入口与边界，不是语义等价证明。即便两侧指令相似，也不能证明线程、优先级、取消、分发、生命周期或监听器语义兼容。

### 2F-e 差异矩阵与候选资格

必须分别给出以下矩阵，每行附原文文件与位置：

| 比较项 | Forge 原契约 | 当前 compat 空壳/002-0e 引用 | NeoForge 候选 | 静态结论 / 未验证 |
|---|---|---|---|---|
| 类 owner | | | | |
| EVENT_BUS 字段 owner/name/descriptor/标志 | | | | |
| IEventBus 的二进制名称 | | | | |
| register 的声明 owner/name/descriptor/标志 | | | | |
| 实例直接形成链与边界 | | | | |
| 相关模块声明 | | | | |

仅评估，不实现三种假设的静态资格：

1. **只改变缺失类的 owner**：其它字段/方法引用保持不变时，是否仍有已知 descriptor/owner 差异？只记录已证差异；不预测确定异常顺序。
2. **保留 Forge 公共签名的最小桥接候选**：它至少需要满足哪些现有符号契约（类存在、字段声明类型、方法声明）？没有现成实现就写“待设计”，不得生成类或确定实现方案。
3. **仅限定本语句的成员引用变换候选**：要形成一致的候选引用，哪些 owner/descriptor 操作数必须改变、哪些不变？列出清单，不设计 transformer，不声称这样已通过验证。

每个候选标记为：`静态必要条件满足但运行未验证` / `已知契约差异阻断` / `UNKNOWN（明确缺口）`。不得写运行可达、已兼容或修复成功。裁判根据该报告选择下一轮单因子任务；本轮执行方不得选择或启动下一轮。

### 2F-f 归档与不变性复核

1. 全部输入重新哈希，与 2F-a 对照；本轮没有合法的输入覆盖例外。
2. 所有工具调用保存：完整参数、stdout、stderr（包括空文件）、退出码；原始输出不截断。报告摘录与原文件明确区分。
3. 证据目录清单：相对路径、字节数、SHA-256；输出脚本和报告也登记。目录内追加汇总/清单不冒充原始字节码。
4. 报告交裁判并抄总指挥；不写手册、不动旧报告。

## 四、判据

- **PASS-002-0f-1**：全部冻结输入匹配；b/c/d 的存在或缺席有精确范围内证据；差异矩阵与三种候选资格填写完整；直接调用边界与 UNKNOWN 明示；全文原始输出已保存。候选不存在或差异阻断也可 PASS，因为目标是建立静态事实。
- **STOP-002-0f-1**：输入漂移；所需工具/制品缺失；工具无法产生完整输出；需修改/运行代码或扩大依赖/检索范围才能回答；仅用推测填事实。记录已有成果和缺口，交回裁判。
- 不设运行 VALID，不以“有相似字段”或“有同名方法”升级成修复验证。本轮不实测 P2。

## 五、报告模板

```markdown
# Spike-002-0f 静态取证报告

## 一、输入与工具
- 冻结说明书 SHA、全部输入前/后 SHA/字节数、工具版本
- 实际命令/退出码/证据路径

## 二、002-0e 失败边界与指令组
- 已观测 [E] 与未观测项
- 新增指令组 / constructor descriptor 原文

## 三、Forge 侧契约
- class / field / interface / register 原文与来源
- 原版接口与 compat 空壳区分

## 四、NeoForge 候选
- 候选条目存在/缺席及检索范围
- class / field / interface / register 原文
- 模块声明与运行访问未知项

## 五、初始化与直接实现入口
- 两侧完整 clinit 引用
- EVENT_BUS 直接形成链、builder/build/直接构造与 register 入口
- 未展开边界、UNKNOWN；不宣称语义等价

## 六、差异矩阵与候选资格
- 契约差异矩阵
- 三种候选：必要契约/已知阻断/未验证项（不实现）

## 七、自评
- PASS-002-0f-1 / STOP-002-0f-1
- 逐条判据，结论上限
- [F] / [E]（工具观测）/ [H] / [D]

## 八、偏差与不变性
- 无运行/无构建/无生产文件修改
- 实际偏差逐项披露，输入后哈希

## 九、完整证据清单
- 原始文本、命令、stderr、退出码、指纹
```

## 六、交接纪律

先由总指挥审查本稿，裁判据意见另存冻结版并给 SHA，再由实践执行；实验事实不因自动推进而免审。执行后独立裁定，再给下一轮具体说明书。任何实现、运行、加入补类或重映射，都不在本轮授权内。
