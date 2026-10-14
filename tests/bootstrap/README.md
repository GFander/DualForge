# 007A 默认启动验证

`BootstrapContractTest.java` 检查四属性默认 true、独立 true/false 覆盖、20 个非法值、默认无 dump、显式 dump 和 I/O 失败传播。
历史 Scope/Registry/Event suites 保留转换拒绝与原子性的严格断言。

`native/` 是独立 NeoForge 共存输入，记录真实构造、ServerStarted 和实际语言 Provider 的模块/版本；不注册 Forge 对象。
`build_native.py` 使用宿主官方运行库干净编译。`run_bootstrap.py` 物理复制官方安装，保持 `run.sh`、`user_jvm_args.txt` 和全部官方 JAR 原字节，执行 `sh run.sh nogui`。
它拒绝继承的 JVM option 环境变量，不生成任何兼容开关、业务开关、Agent 或额外模块参数。EULA 是正常 Minecraft 前置，运行库选自官方安装。

```zsh
python3 -B tests/bootstrap/build_native.py --server-input "$DF_OFFICIAL_INSTALL" --jdk-home "$JAVA_HOME"
python3 -B tests/bootstrap/run_bootstrap.py \
  --install "$DF_OFFICIAL_INSTALL" --jdk-home "$JAVA_HOME" \
  --output out/convergence-007/stock-check --mod "$DF_DELIVERY_JAR" \
  --require 'delegate -> builtin javafml for neoforge'
```

每次使用新的 output。实际 Java 命令、输入 SHA、stdout/stderr、日志、退出码和断言保存在该目录。
默认保留世界/配置/日志，结束后只移除该 runner 当次自行创建的重复运行库副本；`--keep-runtime` 可保留全部。
全量历史 runner 另有自己固定的输出保全规则，不能复用旧首次预检后修改过源码的 worktree。

云端原版首次运行可产生 `server.properties` 初次读取错误及 Mojang 公钥连接错误。本 runner 的原始严格 `noErrorFatalDumpFail` 仍记 FAIL；不把它改写成 PASS。
可用 `--baseline <纯NeoForge实际result.json>` 逐项比较 stdout、debug/latest 中的 ERROR/FATAL 头和计数，要求同一原版空 mods 基线已经 Done/正常 stop/exit0，且无 FATAL。
新增错误、不同计数、stderr、崩溃、dump 或任何输入变化仍 FAIL。该比较只适用于此新标准启动测试，不放宽旧 smoke/C/T 的断言。

历史 harness 的四开关、云端网络参数、public-key relay、诊断路径及额外模块参数均按旧条件显式保存；这些结果属于历史受控回归，不作为零参数运行证据。
