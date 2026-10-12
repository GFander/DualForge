# Spike-002-0k：真实订阅方法的最小静态契约

v1.0 冻结，2026-10-12。root 单智能体串行执行、复核和裁定；非独立审查。自动推进依据用户授权，架构仍为 v1.1。

唯一问题：固定版本 Forge/Neo 订阅注解、事件基类及 Neo 注册直达路径，对一个 public、非 static、单事件参数、默认注解成员的空订阅方法有哪些直接契约？运行变量 0，仅 javap 工具，不运行服务器，不改既存源码/jar/harness。

输入：已安装 Forge eventbus 6.2.27（D2A7BCECB80B33F90854A003DC295960263D78435D9D6016C427D711510CB999）、Neo bus 8.0.5（705E24F37752809499A5CD0C7DD31EAABF3D933C5D3B2060168E94E9EF62593A）、Neo universal 21.1.256（E0A08BAE5CEA95BE96C09770530F5643BB183AF8AF59C64694EB346CB4C93FA4）。JDK 为现有 Microsoft 21.0.10，不下载依赖。

步骤：记录输入前后 SHA/字节数及每次实际工具命令/exit；原字节完整保存 stdout/stderr。双方 SubscribeEvent（verbose、默认值、Retention/Target）、Event（全部声明及字节码）、EventPriority；Neo EventBus、SubscribeEventListener 和 NeoForge 全量 dump，解读只限 register→registerListener→register→addToListeners 与默认全局总线 classChecker。记录后续分发边界，不调用 post。

必须区分：注解同形不代表同 Class；Forge/Neo Event 根类型是否 abstract、能否直接作为订阅类型；若不能，候选必须是显式的 concrete 自定义事件而非选一个业务 API。默认注解不证明显式 priority/receiveCanceled 兼容。静态可达不证明实际注册返回。

证据目录：spike000/logs/spike002-0k-artifacts（不得覆盖）；报告：Dev Manuals/Spike-002-0k 静态取证与顺序裁定报告.md。

PASS-002-0k-1：上述直达契约、原始证据及未知界限已建立，输入不变。STOP：输入漂移、工具取证不完整或需要扩大范围。不改变 0j 结论，不以本轮证明订阅注册或分发成功。后续实现必须另立冻结单因子说明书。
