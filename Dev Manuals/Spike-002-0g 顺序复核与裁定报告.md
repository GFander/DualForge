# Spike-002-0g 顺序复核与裁定报告

2026-10-12。root 顺序复核，不是独立智能体审查。已完整复读执行报告（SHA `88C011A8CCF14BFE01340EE9B48E67DDF5A5E0D92C4D1F3CA6A86EF5B9921781` /11701B）、源码diff、构建退出码、C/T完整latest/stdout/元数据及重写后字节码。

裁定 **PASS-002-0g-1**。证据索引82份采集/程序文件与16份运行文件全部重算匹配；源码初/终检一致；15/16内容不变；C dump与0e样本同哈希；C/T 20条指令中只有offset43/47的两条引用行变化（三个耦合操作数），无其它代码语义差异。变量为同一jar的局部规则开关，probe未变。

C控制组复现NCDFE MinecraftForge；T没有该P1，实际异常栈包含 `net.neoforged.bus.EventBus.register(EventBus.java:135)` 与 `ForgeProbe.<init>(ForgeProbe.java:14)`，消息为无SubscribeEvent方法。该运行证明了本限定引用路径进入register，不能改写为注册成功或总线语义等价。ctor BUS地址不证明全局bus对象同一性。

S1符合预登记，不自行修复或新增监听器；本轮终点保持Mod Construction。启动前wrapper语法括号错误已在无Java启动的阶段修正；C/T服务端各一次，不计为复跑。原始异常栈、报告与冻结件保留。

新局部实验jar `F0D51E46533A135A9ECA0FA859805E04E9FBB22E8759B1A29524319E37B4C739` /22432B仅用于包含该唯一语句的探针。它不是可适用于任意Mod/任意probe的兼容基线，也不能替换0d的有效生命周期基线。三引用规则为[D]局部实验，不扩张通用mapping架构。

下一步裁定：002-0h只读取证「原Forge对没有订阅方法的固定ForgeProbe对象，静态注册路径如何终止」，与0g实测S1对照，验证原语义边界后再选修复。暂不通过添加NeoForge原生监听器绕过问题，不吞所有IllegalArgumentException，不直接修改宿主register实现。该轮完整说明书先冻后执行。

Confidence HIGH，限定本次K1与register入口事实。复核独立性受用户选择的串行方式限制，后续语义/通用性仍未知。
