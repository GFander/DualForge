# Convergence-007C-2 Forge依赖契约与验证

本契约先于生产实现记录；执行结果由交付报告与本轮证据索引补充，不把预期写作PASS。

1. 生产入口仅对共享识别器FORGE_CONFIRMED的同JAR metadata投影。native/普通输入不投影；歧义拒绝，无modId/文件/SHA白名单。没有平台依赖的旧输入保持原流程。
2. 实际宿主VersionInfo/dist在discovery前初始化，来源必须为当前FML API。只支持MC1.21.1、NeoForge21.1.256、FML4.0.45和Java21；不可用/不符明确拒绝，不用测试属性作为生产替代。
3. 参考来源ABI固定52.1.0，与NeoForge21.1.256、javafml9999分开。active forge `[0,)`、`[52,)`、`[52.1.0,)`、`[52,53)`通过版本条件；`[53,)`、`[52.2,)`及非法range拒绝；缺失range沿用FML UNBOUNDED默认语义。该条件不验证业务API。
4. Forge mandatory到required/optional转换仍由Parser完成。仅这两个一致类型支持；原冲突type、错误boolean、未知语义约束继续拒绝。active optional平台能力的range也验证参考版本，不当作缺失普通optional略过。其他optional依赖仍由官方FML决定缺失忽略/已安装越界拒绝。
5. active forge仅ordering NONE（含原默认）支持；BEFORE/AFTER拒绝，不虚造排序节点。inactive side原项保持，由FML处理，不能声称其range已通过。普通依赖的side/ordering完全保留，验证官方side与拓扑图行为。
6. 成功返回新的配置副本。原依赖快照和逐项决策（owner、原字段、host/dist、参考ABI、结果、API未验状态）可从真实FML config读取，并有日志。active成功条目从FML实际依赖列表投影移出；非平台项逐字段不变。保留审计key冲突时拒绝。失败不污染调用者map，不吞异常或缓存跨文件结果。
7. 两mod输入A `[0,)`/B `[53,)`必须分别验证，不能共享全局成功状态。额外缺失/低版本真实mod、Minecraft错误范围仍由FML拒绝；native required forge不会被满足。
8. 最终原FnF双JAR标准启动须越过缺失forge并保存下一实际异常。不得改FnF、宿主JAR/config、原fixtures、ABI/转换器或加入生产参数。测试SPI、内存系统metadata仅DIAGNOSTIC_ONLY，不进入用户环境。
9. 全部最终动态、Contract和历史回归绑定同一新JAR，JVM串行，物理副本。受影响的007C-1测试保留原解析模型的严格断言，增加对投影模型的独立断言，不能用更宽松判断删掉历史负例。

独立状态：`SOURCE_PLATFORM_DEPENDENCY=VERIFIED/REJECTED`描述版本/侧/排序条件；`FORGE_RUNTIME_API_COMPATIBILITY`只由后续API实际证据决定。平台验收不能自动将FnF运行记PASS，原生Forge基线也仍PARTIAL。
