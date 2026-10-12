# Spike-002-0m：注册返回之后的分发前置契约

v1.0 冻结，2026-10-12。root 单智能体串行，依据 0l VALID（只证明真实 register 返回）。变量0，只读既存制品，不运行服务器、不改源码/jar/harness。

唯一问题：Neo 全局 EventBus 在 Mod 构造器登记之后，何时解除 shutdown 状态；post 与监听器 wrapper 的直接静态路径是什么？这是选择后续分发探针位置的门槛，不能提前把构造器中的 post 视为有效送达实验。

输入为相同固定 Neo bus8.0.5（705E24F3…62593A）、Neo universal21.1.256（E0A08BAE…3FA4）、Forge bus6.2.27（D2A7BCEC…10CB999）、0l同一新jar/probe（D49CAE…E679 / A774D2…9AE8）；完整SHA来自0k/0l指纹。原字节 javap code/s/descriptors 完整保存，实际命令/exit以及前后SHA记录。

步骤：dump Neo EventBus/ListenerList/EventListenerFactory/SubscribeEventListener、NeoForge、CommonModLoader、ServerModLoader，及 Forge EventBus/IEventBus。解读限post/shutdown/start、ListenerList选wrapper/调用、factory建立目标处理器，以及全局bus.start的FML直接调用点。JDK/动态生成边界只能注明未知，不能将孤立路径拼成运行已发生。

不实例化事件、不post、不start宿主总线、不加业务API、不推断线程/取消/优先级全兼容。新证据目录spike000/logs/spike002-0m-artifacts；输出静态取证与顺序裁定报告。

PASS-002-0m-1：直达契约、可证明时序与未知项落盘，输入不变；UNKNOWN可作为明确门槛结论。STOP：必须修改/运行/跨越边界反推才能补证。下一真实分发必须另冻结单变量实验，不复用0l结果冒称listener invoked。
