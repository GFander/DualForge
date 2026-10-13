# Convergence-006A：受控 Forge 多 Item

生产支持一个扫描归属明确的 `DeferredRegister<Item>`、多个独立声明，
Supplier 为零捕获 lambda 或同入口私有静态零参数工厂 method reference。
仍只接受原单参数 Forge IEventBus 构造器和既有受控构造/普通 Item supplier 子形态；
不支持任意 supplier 逻辑、捕获、Serializable/altMetafactory、Block、自定义 Registry 或新增事件 API。

`src/` 是原生 Forge `multiregistryprobe`：`alpha_item` lambda、`beta_item` method reference；
`driver/` 为只观察真实官方调用栈及查询注册表/holder 的 NeoForge driver。
`matrix/` 有不同 modId、包/字段/ID/getter/factory 名称和实际 ASTORE/ALOAD 局部寄存器别名，
另含捕获和 altMetafactory 编译取证。matrix 不用于同时安装多个 Forge 模组。
两个新贴图使用原 005 用户 PNG 的原字节副本；原 PNG、物品 ID 和原 JAR 不改。

所有 JVM 命令严格串行，在仓库根执行：

```zsh
DF_SERVER='/实际输入/server-base'
DF_FORGE='/固定 Forge 52.1.0 原编译依赖'
DF_ORIGINAL005='/冻结原 005/registry-probe.jar'
export JAVA_HOME='/实际 Microsoft JDK21.0.10+7-LTS'
python3 -B tests/multi_registry/build_fixture.py \
  --server-input "$DF_SERVER" --forge-dependencies "$DF_FORGE" \
  --jdk-home "$JAVA_HOME" --original-fixture "$DF_ORIGINAL005"
# Fixture 冻结后从 result.json 取实际路径，不再次构建替换 C/T 输入。
DF_FIXTURE_DIR='/本轮冻结fixture目录'
zsh tools/entry/dualforge.zsh --mode build --server-input "$DF_SERVER"
DF_COMPAT='/本轮实际构建/dualforge.jar'
# stage 每次分别选择 contract、control、treatment。
zsh tests/multi_registry/run-multi-registry.zsh --stage contract \
  --jar "$DF_COMPAT" --fixture "$DF_FIXTURE_DIR/multi-registry-probe.jar" \
  --driver "$DF_FIXTURE_DIR/multi-registry-driver.jar" \
  --native-control "$DF_FIXTURE_DIR/native-control.jar" \
  --matrix "$DF_FIXTURE_DIR/registry-matrix.jar" \
  --server-input "$DF_SERVER" --jdk-home "$JAVA_HOME"
```

control/treatment 使用同一命令，改 stage 并加 `--allow-server-start`；当前云端另加
`--java-network-args /workspace/dualforge-cloud/java-network.args --public-key-relay`。
本地环境按真实代理/CA 条件配置，不复制云端绝对路径。C/T 只有 `DF006_REGISTER=false/true`
这一业务差异；环境、三制品、源世界和 flags 相同，隔离输出目录/relay 端口自然不同。

`--stage baseline` 使用同样参数和**原 main 干净构建件**重现拒绝，必须在开发修改前保存。
不能拿当前新 JAR 冒称它仍有旧限制。原实际完整命令和 stderr 见本轮报告索引。
contract 检查逐调用映射、Supplier/BootstrapMethods 不变、不同字节布局、未知/捕获 Bootstrap、
重复 ID、错误 Registry/Bus/额外 API、晚期拒绝和发生已验证编辑后的 dump I/O 失败原子性。
注入 dump I/O 失败只属于测试负例；实际服务器正常阶段不允许 dump FAIL。

```zsh
python3 -B -m unittest discover -s tests/multi_registry -p test_cohort.py -v
python3 -B tests/project_identity/check_identity.py
```

006A 汇总必须有 Contract、C、T 和完整旧回归的实际 PASS/同 compat SHA，
还验证 C/T 三制品、运行环境及嵌套 runtime checker 指纹。
历史回归复用 [project_identity runner](../project_identity/README.md)，使用原冻结 005/004/003b/A
输入与最终同一个新 JAR；没有重建原 Forge fixture 或放宽已有断言。
运行期间冻结测试源码；回归保全失败时保留原输出，并在新验证 worktree 重跑。

完整附件保存在新 `out/convergence-006a/`；历史回归可能在独立验证 worktree 的 out。
源码/制品/环境/命令/退出/字节 dump/原始日志路径与 SHA 见本轮证据索引。
006A-S 与 006A-C 独立：资源静态和服务端成功不代表客户端渲染。
