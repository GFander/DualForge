# 测试入口

tests 下现有文件位置保持不变。生产源码和工具布局见
[STRUCTURE.md](../STRUCTURE.md)，环境及隔离运行参数见
[tools/UNIX.md](../tools/UNIX.md)。从仓库根目录执行以下命令，所有
JVM 编译、metadata 测试和服务端运行均严格串行。

## real_mods/food_n_fuel：007C真实第三方取证

[输入、原生运行和人工补充](real_mods/food_n_fuel/README.md)使用未改字节的官方发行JAR。
独立Forge/NeoForge标准运行只声明生命周期；原生Registry/燃料子项不等于DualForge兼容。
原生基线PARTIAL、NeoForge发现FAIL；本轮生产未改，实际食用和Mac仍NOT RUN。

先通过新入口干净构建兼容层，再使用 result.json 中实际的新 JAR：

```zsh
export JAVA_HOME='/absolute/path/to/msopenjdk-21'
DF_SERVER_INPUT='/absolute/path/to/server-base'
DF_PROBE='/absolute/path/to/original/forgeprobe.jar'
zsh tools/entry/dualforge.zsh --mode build --server-input "$DF_SERVER_INPUT"
DF_COMPAT_JAR='/absolute/path/to/new-run/dualforge.jar'
```

完整运行库和原 Probe 是外部输入。归档后的原 Probe 源码位于
`history/spike000/probe-002-0l/src`，但测试不会从它重建替代 JAR。
原制品指纹校验、现有正负断言和当前构造器 ABI 保持不变。

## dependencies：依赖元数据和加载门禁

夹具在 `tests/dependencies/*.toml`；契约测试为同目录的
`DependencyContractTest.java`。验证 owner 分组、字段保留、Forge
mandatory 到真实 FML REQUIRED/OPTIONAL 的适配、范围及非法输入。

仅 metadata：

```zsh
zsh tools/entry/test-forge-dependencies.zsh --stage after --boundary-only \
  --server-input "$DF_SERVER_INPUT" --probe "$DF_PROBE" \
  --compat-jar "$DF_COMPAT_JAR"
```

六组 metadata 加 C/D/E/F 动态正负例：

```zsh
zsh tools/entry/test-forge-dependencies.zsh --stage after \
  --server-input "$DF_SERVER_INPUT" --probe "$DF_PROBE" \
  --compat-jar "$DF_COMPAT_JAR" --allow-server-start
```

动态夹具保留原 A 的 class，只替换 TOML；负例按具体 FML 拒绝原因
判断，不能用 exitCode 或缺少 Done 代替。默认输出在 out/unix/dependencies。

## discovery：真实发现 pipeline 和候选边界

夹具由 `tests/discovery/run-discovery.zsh` 在每轮新输出目录生成，
测试为 `DiscoveryContractTest.java`，使用当前 FML 的实际 pipeline、
Reader 和 metadata。覆盖 A–F、文件名无关、原生路径、重复发现和
歧义拒绝等边界；不初始化候选入口，不代表多 Forge 完整启动。

```zsh
zsh tests/discovery/run-discovery.zsh \
  "$DF_COMPAT_JAR" "$DF_SERVER_INPUT" "$DF_PROBE"
```

输出保持在 out/convergence-003a/discovery 下，包含生成夹具、命令、
指纹和原始断言日志；目录名是测试系列名称，不表示使用旧兼容层。

## identity：受控入口身份与转换范围

原生 Forge B 源码／资源在 `tests/identity/src/`；对照源码在
`tests/identity/controls/`；FML 扫描与字节测试为 `IdentityScopeTest.java`。
zsh 入口仍留在 tests/identity，不移入工具目录。

```zsh
DF_FORGE_DEPS='/absolute/path/to/pinned-forge-compile-jars'
zsh tests/identity/run-identity.zsh --stage fixture \
  --forge-dependencies "$DF_FORGE_DEPS"
DF_IDENTITY_JAR='/absolute/path/to/fixture/identity-check.jar'

zsh tests/identity/run-identity.zsh --stage after \
  --fixture "$DF_IDENTITY_JAR" --jar "$DF_COMPAT_JAR" \
  --server-input "$DF_SERVER_INPUT" --allow-server-start

zsh tests/identity/run-scope.zsh \
  --fixture "$DF_IDENTITY_JAR" --jar "$DF_COMPAT_JAR" \
  --server-input "$DF_SERVER_INPUT" --forge-dependencies "$DF_FORGE_DEPS"
```

fixture 使用原 Probe 相同版本／指纹的 Forge 依赖，拒绝预改写输入。
after 核验身份关联、必要转换、构造／真实 register 返回、宿主委托、
Done／正常退出和日志；scope 保存未识别类及原生对照的转换前后字节。
输出在 out/convergence-003b 下。before 需要历史起点构建件，其目的
是复现旧失败，不能用当前修复后制品替代。细节见
[identity/README.md](identity/README.md)。

## 原 A 回归、独立 harness 与云端参数

```zsh
zsh tools/entry/dualforge.zsh --mode regress \
  --server-input "$DF_SERVER_INPUT" --probe "$DF_PROBE" --allow-server-start

zsh tools/harness/run-server.zsh \
  --server-input "$DF_SERVER_INPUT" --probe "$DF_PROBE" \
  --jar "$DF_COMPAT_JAR" --allow-server-start
```

regress 包含干净构建和原 A 严格 smoke。独立 harness 负责隔离运行和
正常停止，完整兼容 smoke 判据应使用 dualforge 的 smoke/regress。
服务端运行需要完整配置、世界、无其他 Java 进程及空闲 25565。

当前云环境的动态命令还需加
`--java-network-args /workspace/dualforge-cloud/java-network.args --public-key-relay`。
本地环境应按实际网络和 CA 配置选择参数，不照搬云端文件路径。
实际结果以每轮 result.json、原始日志、断言和输入指纹为准，
不把历史验证当作当前运行。

## events：004 受控真实事件投递

独立 Forge fixture、原生 NeoForge 测试 driver、拒绝对照及 Linux runner 位于
`tests/events/`，不改原 A 或 IdentityProbe。运行入口为
`zsh tests/events/run-events.zsh --stage fixture/contract/control/treatment/regression`
（每次选择一个 stage）。完整参数、固定输入和串行重跑说明见
[events/README.md](events/README.md)。

同一组 JAR 只改变构造阶段的注册属性，未注册组真实 post 一次／回调零次，
注册组真实 post 一次／回调一次；打印点调用栈确认实际 EventBus 链。
仅支持当前 marker-only listener 与普通自定义 Event，不实现 Forge post ABI，
不将 driver 的原生 NeoForge API 用法计入 Forge 兼容范围。

## registry：005 受控单 Item 注册

`tests/registry/src/` 从原生 Forge 52.1.0 和未打补丁的 Minecraft 字节编译；
`driver/` 是只查询真实注册表和观察 supplier 调用栈的原生 NeoForge 输入。
两组用同一制品，只有 `DF005_REGISTER` 决定是否挂接注入的 MOD bus。
Run C 查询 `registryprobe:ice_ether` 不存在；Run T 验证实际 Item、key、
非 AIR、holder get 对象身份和官方注册调用栈。

入口 `zsh tests/registry/run-registry.zsh`，阶段、依赖纯度、夹具、资源检查和
客户端人工步骤见 [registry/README.md](registry/README.md)。负例在
`RegistryBytecodeTest.java`；`test_stage_results.py` 拒绝跨阶段制品错配。
assets 已核对四份真实资源及用户原始 16×16 PNG 的 SHA；服务端和资源
静态检查 PASS。旧 005-C 的受控客户端 PASS 仅属于旧 cohort；新 DualForge JAR 的客户端 NOT RUN。

## project_identity：当前生产身份和完整回归

[project_identity/README.md](project_identity/README.md) 说明命名审计、
Manifest/SPI/模块/属性检查、官方 Module Layer 实际观测及串行兼容回归。
所有 C/T 和回归使用同一个重新编译的 `dualforge.jar`；原 Forge 输入和资源不变。
新 Mac kit 见 [registry/mac/README.md](registry/mac/README.md)，
固定历史 kit 见 [registry/mac-legacy/LEGACY.md](registry/mac-legacy/LEGACY.md)。

## multi_registry：006A 调用点与数据依赖验证

[006A入口](multi_registry/README.md) 独立保存原生 Forge 的两个Item声明：普通lambda与同入口静态method reference。
用同一compat/fixture/driver仅切换DF006_REGISTER，验证真实两个注册、各supplier一次、holder身份、实际MODbus及官方栈；
独立本轮矩阵覆盖不同字段/ID/方法名和实际局部寄存器别名。
重复ID、捕获、未知Bootstrap/额外ForgeAPI/错误Registry/Bus和部分失败均明确拒绝且原ClassNode不污染。
原005和所有历史回归保持原严格断言；Beta资源更新版006A-C由用户macOS arm64人工验收受控PASS，见 [客户端报告](../Dev%20Manuals/Convergence-006A-C%20macOS%20客户端人工验收报告.md)。Codex取得的是用户控制台摘录/审查报告，独立新版服务端C/T和Intel Mac仍NOT RUN。

## 当前运行输入与历史定位

默认服务端输入为 `runtime/neoforge-server/`，显式 `--server-input` 仍可指定外部物理安装。新运行目录不修改源世界。早期 Spike 已按原字节归档，见 [结构](../STRUCTURE.md) 和 [功能导航](../docs/README.md)。历史报告中的旧路径不改写，精确文件映射及SHA见 [迁移映射](project_identity/structure_relocation.json)。

## block_registry：006B受控双Registry

[入口与范围](block_registry/README.md)包含原生Forge Block+BlockItem、观察型NeoForge Driver、Gate0/改名布局、Contract拒绝/原子性和真实C/T。跨Registry同ID不算重复；BlockItem必须引用已验证BlockHolder。现有历史回归不放宽，006B-C NOT RUN。

## client_objects：最新实际注册对象身份

[统一输入与运行](client_objects/README.md)将Registry路径、Java字段/工厂/getter和资源统一为ice_ether/raw_ice_ether/ice_ether_block。从原生Forge重新编译，受影响Contract和独立C/T使用同兼容层，原回归输入保留；Mac图形验收另行NOT RUN。

## bootstrap：007A标准入口与默认配置

[入口与契约](bootstrap/README.md)记录无参数标准NeoForge运行、可选dump、显式布尔覆盖、原生共存和严格错误基线比较。旧回归与新零参数证据分别保存，不把历史harness称为用户启动方式。

## dropin_items：007B普通Forge输入

[原生构建、范围/原子性与零参数包](dropin_items/README.md)支持无业务属性Item注册与独立查询。原始Forge JAR保持不变，automatic模块身份按真实FML metadata对齐；显式模块不匹配拒绝。客户端图形NOT RUN，旧阶段回归使用最终同一新JAR。
