# DualForge 当前生产身份验证

此系列只验证命名迁移，复用 002–005 的原严格判据，不增加 API、ABI 或架构。
运行前阅读根 AGENTS.md、STRUCTURE.md 与 tools/UNIX.md。所有 JVM 工具和服务器严格串行；
需要完整 NeoForge 安装的物理复制输入和原冻结 Forge JAR。

```zsh
python3 -B tests/project_identity/check_identity.py
python3 -B -m unittest discover -s tests/project_identity -p 'test_*.py' -v
export JAVA_HOME='/实际路径/msopenjdk-21'
DF_SERVER='/实际输入/server-base'
DF_PROBE='/原输入/forgeprobe.jar'
DF_FORGE_DEPS='/锁定 Forge 52.1.0 编译依赖目录'
zsh tools/entry/dualforge.zsh --mode build --server-input "$DF_SERVER"
DF_COMPAT='/以上新 result.json 中的实际路径/dualforge.jar'
# 以下固定输入来自已验收 003b / 004 / 005 原制品，不重新编译或改名。
DF_IDENTITY='/原 003b fixture/identity-check.jar'
DF_EVENT_DIR='/原 004 fixture'
DF_REGISTRY_DIR='/原 005 带原始 PNG 的 fixture'
zsh tests/project_identity/run-identity-unification.zsh \
  --jar "$DF_COMPAT" --server-input "$DF_SERVER" --probe "$DF_PROBE" \
  --jdk-home "$JAVA_HOME" --forge-dependencies "$DF_FORGE_DEPS" \
  --identity-fixture "$DF_IDENTITY" \
  --event-fixture "$DF_EVENT_DIR/dispatch-probe.jar" \
  --event-driver "$DF_EVENT_DIR/dispatch-driver.jar" \
  --event-rejected "$DF_EVENT_DIR/rejected-probe.jar" \
  --fixture "$DF_REGISTRY_DIR/registry-probe.jar" \
  --driver "$DF_REGISTRY_DIR/registry-driver.jar" \
  --native-control "$DF_REGISTRY_DIR/native-control.jar" \
  --allow-server-start
```

在当前云环境才追加 `--java-network-args /workspace/dualforge-cloud/java-network.args --public-key-relay`。
常规本地环境按实际代理/CA 配置执行，不复制云端绝对路径。

Gate 1 保存 Manifest、四份 SPI、jar tf / describe-module / javap，并检查每个 package 路径、
Provider class、新属性和旧二进制身份缺失。check_identity 对照起点源码，只允许明确的身份
字符串映射；所有 16 份生产 Java/资源及逐文件历史保护 SHA 都要匹配。
豁免按路径、理由和完整行记录在迁移索引中，没有目录级字符串扫描豁免。

Gate 2 使用独立 read-only Java agent，在正常 shutdown 时通过 Instrumentation 已加载 class、
公开 Launcher.findLayerManager / IModuleLayerManager API 观察 BOOT/SERVICE/PLUGIN/GAME。
agent 不注册 transformer、不访问私有字段、不替换 ClassLoader、不修改 Module Layer。
实际服务发现、javafml 委托和原 Probe 严格 smoke 同时通过才算 PASS。

Gate 3 复用 registry runner 的 Contract/C/T/assets/regression；其 regression 复用 events runner
验证 004 Contract/C/T、003b、003a、002 和原 A。所有子结果绑定同一个新 SHA，Registry 的
原环境、fixture/driver、MOD bus、supplier 官方调用栈、holder get、范围/原子性判据保持不变。
当前任务未重跑历史 before/baseline 复现，也未重建冻结 Forge fixture；原 004/005 的完整任务
汇总可能因此是 PARTIAL。此次身份迁移验证使用本任务的明确 gate 集合，不能改写历史汇总。

新结果在 `out/identity-unification/verification/<新 run>/`，最终有效 cohort 在
`out/identity-unification/validation.json`；失败 run 保留，不能被后续 PASS 覆盖为成功记录。
服务原始日志、命令与输入指纹继续沿用共享 harness 布局。

Gate 4 见 [新 Mac kit](../registry/mac/README.md)：真实 Mac 客户端不可访问时为 NOT RUN。
不继承历史 005-C 人工 PASS，也不发布 Release。
