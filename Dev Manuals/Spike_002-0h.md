# Spike-002-0h：无订阅方法对象的原Forge注册语义边界

v1.0冻结，2026-10-12。root串行执行及复核。依据0g PASS：局部引用变换已进入Neo register，S1为无SubscribeEvent方法异常。本轮运行变量0，仅只读既存字节码；不启动任何JVM应用（仅javap/jar工具）、不编译、不改probe/compat/harness、不加监听器、不吞异常。

唯一问题：固定0e/0g ForgeProbe（只有构造器、继承Object）在原Forge EventBus.register(Object)→registerObject路径中，零订阅方法条件的静态终点及可证明范围是什么？

输入：Forge eventbus6.2.27 `D2A7BCECB80B33F90854A003DC295960263D78435D9D6016C427D711510CB999`；Neo bus8.0.5 `705E24F37752809499A5CD0C7DD31EAABF3D933C5D3B2060168E94E9EF62593A`；原probe D61224F3…35F1C9；0g新jar F0D51E46…B4C739；T dump A9ABBB94…05EF1；harness FBA0003C…9C8C。完整SHA从0g与0f冻结记录读取，前/后复测；不使用不同版本或shared dump作为冻结输入。

步骤：

1. 记录本说明书、输入前/后指纹和工具版本。0f附件为可重用的原始字节码证据（制品哈希相同）；复用须逐项验证现存文件SHA并登记来源，不能伪称本轮重跑工具。
2. 完整读取原probe verbose的类继承、方法数/方法成员及注解；只确定固定probe结构，不外推任意Mod。
3. 原Forge EventBus.register(Object)只追实例分支的registerObject及其用于方法集合/注解过滤的直接步骤。完整方法字节码、分支offset、零候选时return位置落盘。它调用的registerListener等有订阅方法分支仅记录边界，不展开事件分发。
4. Neo register(Object)已在0f全量取证，本轮只核对零计数throw分支与0g实际S1栈。checkSupertypes等前置调用仅记录其直接界限；不从S1推广所有对象行为。
5. 如果原Forge零候选分支有明确正常终止，标[F]条件静态事实；实际原Forge运行是否发生仍未实验验证。若反射/外部接口使固定对象候选集不能仅由字节码完全确定，明确[H]/UNKNOWN，不借JDK行为记成项目字节码事实。
6. 输出0h静态报告与顺序裁定；前/后不变，全部复用证据原件保留。只为下轮原语义保真选择提供依据；不得直接实现适配。

原始全文可以引用0f原文件路径与SHA，新增摘录注明范围，不截断或冒充完整输出。证据新目录 `spike000\logs\spike002-0h-artifacts`，报告 `Dev Manuals\Spike-002-0h 静态取证报告.md`；目录非空不覆盖。

PASS-002-0h-1：固定probe结构、原Forge实例分支与零候选静态终点、Neo零计数throw/0g实测S1、依赖/未知界限均准确记录，输入不变；原Forge实际运行结论可UNKNOWN，不阻塞该静态目标。STOP：输入漂移、证据无法完整取得、必须运行或扩大范围才能填事实。

本轮不授予新实现、不修改v1.1架构。下一语义适配或真实Forge对照运行须另立单因子说明书；不把「无监听器对象空注册」直接升级成所有Forge register调用兼容。
