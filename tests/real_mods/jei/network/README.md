# Convergence-010D：有限网络定义与官方注册

固定 MC 1.21.1 / Forge 52.1.0 / NeoForge 21.1.256 / FML 4.0.45 / Microsoft JDK 21.0.10。源码在 SMB；所有输出、制品、SDK、世界在 SSD。所有 JVM 串行。

本目录仅批准经完整字节码证明的 PLAY `addMain` 双向定义子契约，不能推出原 JEI 网络闭包或客户端已通过。整体交付及最终证据以 [010D 报告](../../../../Dev%20Manuals/Convergence-010D%20JEI网络兼容交付报告.md) 和索引为准；各测试层级见 [MATRIX](MATRIX.md)，原始 ABI 及未批准操作见 [来源图](JEI_NETWORK_SOURCE.md)。

## 契约

任意合法 Mod 身份、包名、类/字段/方法名及非负协议整数均从同 ModFile 的原始 ClassFile 推导，不使用 JEI/夹具身份白名单。

- 入口沿用 010C Locale/Translator、两条原生 Bus、实际配置注册和三个显式事件回调，增加 private final 网络对象。
- 网络构造只接受 `named → networkProtocolVersion → optional → payloadChannel → play → serverbound/addMain → clientbound/addMain → build`。恰好两个不同 Type，PLAY 两方向分开；其他注册形状整体拒绝。
- 两个原始自有 payload 各持有一个 int，用原 `RegistryFriendlyByteBuf` VarInt lambda Codec；完整 class、字段、构造器、getter、type、clinit、两个 Codec lambda 都验证。不是任意 Codec 或泛型支持。
- 私有原始回调只接受有限的计数/保存该 payload 值/保存当前线程名形状，不读取 Forge Context。Source Context 描述符经计划映射为私有不透明 Object；没有 Forge/Neo Context 强转，未实现 sender、reply、send 或连接查询。
- 构造时仅捕获原 Type、Codec、BiConsumer 和完整参数，`build` 后以不可变列表封存；返回私有 receipt，不暴露宿主 registrar。
- 实际注册仅发生在官方 `NetworkRegistry.setup → RegisterPayloadHandlersEvent`。要求原 entry 完整构造、实际 owner/ModFile/module/loader、entry/network/channel 对象身份相符，使用该 owner 实际 Mod Bus。
- Native `PayloadRegistrar.executesOn(MAIN).optional().playToServer/playToClient` 保存原 Type/Codec，通过内部适配器持有原业务回调。固定主线程包装 ABI 逐 ClassFile SHA 绑定。未取得报文投递证据前不声称业务回调运行通过。
- native 抛错标 UNCERTAIN，异常原样传播，拒绝重试，不承诺撤销部分注册。

协议整数原值转为十进制版本字符串；两端同值、异值及缺失 optional 以原生算法组件分别测试。Forge 按 channel 协商，Neo 按 payload 协商：完整连接协商等价性 **PARTIAL**。源 channel 的远程存在查询及发送均不在契约内。

## 工具入口

所有命令从此 worktree 执行，`--output` 必须是不存在的 SSD runs 子目录。示例路径中的 `candidate` 必须替换为实际被测 JAR。

```bash
python3 -B tests/real_mods/jei/network/collect_abi.py --output /home/albert/Env/DualForge/runs/convergence-010d/abi-new
python3 -B tests/real_mods/jei/network/analyze_abi.py --abi /home/albert/Env/DualForge/runs/convergence-010d/abi-new --output /home/albert/Env/DualForge/runs/convergence-010d/analysis-new
python3 -B tests/real_mods/jei/network/build_fixture.py --output /home/albert/Env/DualForge/runs/convergence-010d/fixture-new
python3 -B tests/real_mods/jei/network/run_remaining.py --jar /home/albert/Env/DualForge/runs/convergence-010d/candidate/dualforge.jar --output /home/albert/Env/DualForge/runs/convergence-010d/cohort-new
```

`run_remaining.py` 串行构建两平台独立 native observer、运行本目录静态/边界/4轮实服，以及旧 B/C Bus/config/constructor、原 JEI config 组件、Mouse、helper CI、本地检查和真实 JEI 当前首失败。18组 FnF/Clumps/Probe/身份等回归另用 `mouse_tweaks/run_regression.py`。两平台仅 observer 分别编译；业务一律使用相同两个原始 Forge JAR，复制安装前后 SHA 再核对。

`run_live.py` 先正常停机，再物理复制本轮已停止 world/config，第二次真实启动重新注册并正常停止。无玩家、背包或真实包持久化验收。observer 的 Codec 测试直接调用原 Codec；native 协商算法及私有 guard/故障模型有独立标签，不伪装为连接或报文。

`run_jei.py` 要求最终新 JAR + 原 Forge JEI 单因素预计划精确拒绝。异常发生于 targets，未执行 NetworkHandler、全局 setter、payload 注册或原 JEI 构造。

REAL_CONNECTION / REAL_PACKET_DELIVERY / CLIENT_GUI / Windows：NOT_RUN。没有图形会话或 Xvfb，本轮不要求用户提前 Windows 测试，不生成已知失败的客户端 ZIP。
