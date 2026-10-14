# Convergence-006B：受控 Block + BlockItem

原生 Forge52.1.0 fixture：一个扫描归属明确的@Mod、两个typed DeferredRegister，分别注册普通Block和引用其Holder的普通BlockItem。
两Registry允许同一个ID `blockregistryprobe:sample_block`；不是同Registry重复注册。
生产profile按actual registry源、field泛型、独立声明、标准零捕获Bootstrap、Supplier体和跨方法Block Holder引用验证，不依赖fixture类/字段/modId名字。
原单IEventBus构造器ABI保持，已验证业务为两个local1 MODbus挂接及既有marker/布尔开关布局。原006A Item profile不变。

只支持marker+`new Block(BlockBehaviour.Properties.of())`和marker+`new BlockItem(指定BlockHolder.get(),new Item.Properties())`。
捕获、未知/alternate Bootstrap、跨类工厂、其他Registry/额外API/业务、额外Forge入口、不同构造器ABI或布局均不支持。
不能承诺任意模组错误下的通用跨Registry事务；完整ClassNode验证和官方事件失败处理分别记录。

## 编译、冻结和运行

所有JVM严格串行；使用原固定输入，不能重建旧Probe替代品。仓库根执行：

```zsh
DF006B_SERVER="$PWD/runtime/neoforge-server"
DF006B_JDK='/实际/Microsoft-JDK21.0.10+7-LTS'
DF006B_FORGE='/锁定Forge52.1.0三份依赖目录'
python3 -B tests/block_registry/build_fixture.py \
  --server-input "$DF006B_SERVER" --jdk-home "$DF006B_JDK" --forge-dependencies "$DF006B_FORGE"
# 冻结fixture.json记录的各实际路径/SHA；后续C/T不再次构建。
DF006B_FIXTURE='/本轮实际fixture目录'
zsh tools/entry/dualforge.zsh --mode build --server-input "$DF006B_SERVER"
DF006B_JAR='/实际新build/dualforge.jar'
zsh tests/block_registry/run-block-registry.zsh --stage contract \
  --jar "$DF006B_JAR" --fixture "$DF006B_FIXTURE/block-registry-probe.jar" \
  --driver "$DF006B_FIXTURE/block-registry-driver.jar" \
  --native-control "$DF006B_FIXTURE/native-control.jar" --matrix "$DF006B_FIXTURE/block-matrix.jar" \
  --server-input "$DF006B_SERVER" --jdk-home "$DF006B_JDK"
```

C/T选择control/treatment并加`--allow-server-start`；只有DF006B_REGISTER改变。
当前云端追加实际`--java-network-args /workspace/dualforge-cloud/java-network.args --public-key-relay`，其他平台按实际代理/CA配置。
`--stage baseline`必须用原main干净JAR，输出OBSERVED；看完原始日志后独立记录baseline-analysis，不把真实错误预设成某种失败。
旧wrapper所需server-input使用绝对路径，避免在子运行目录中解析相对classpath失败；现有旧断言不放宽。

## 判定与范围

- Contract：真实FML扫描、原/新字节、精确逆向恢复全class、两个typed Registry/Holder、三个get用途、Bootstrap和业务保持、ASM verify、改名matrix、native/unclaimed范围。
- 19项拒绝/失败场景：错误Registry/owner、两个重复ID、未知BlockHolder/错误类型/返回值、真实捕获/alternate及未知bootstrap、额外API、错误/漏bus、晚期getter、错误入口owner、额外扫描入口及已编辑副本之后dump I/O失败。保存before/after和reason，原节点不污染。
- C：两个Registry缺席，两个Supplier0次；T：两个真实对象、各Supplier1次、Block在Item phase已注册且Holder可用、BlockItem/Registry/Holder.get同一Block、正确key/registry、官方栈/Modbus、正常Done-stop-exit0及严格错误断言。
- Driver仅观察stdout的实际Supplier栈、查询注册表/holder；不创建或注册Block/BlockItem。
- 汇总复用006A严格cohort函数并递归检查全部历史结果SHA；全部required阶段PASS且同新SHA才能006B-S PASS。

```zsh
python3 -B -m unittest discover -s tests/block_registry -p 'test_*.py' -v
python3 -B tests/block_registry/summarize.py \
  --historical out/identity-unification/validation.json \
  --multi-stages out/convergence-006a/stage-results.json
```

完整历史重跑参见 [身份验证入口](../project_identity/README.md)，原冻结006A Contract/C/T参见 [multi_registry](../multi_registry/README.md)，全部用本轮最终同一新JAR。
raw命令/退出/logs/环境/dump指纹在新out/convergence-006b；旧回归在各本轮新out子目录，不覆盖历史。
报告见 [Gate0](<../../Dev Manuals/Convergence-006B Gate0 ABI与可行性报告.md>) 和 [交付](<../../Dev Manuals/Convergence-006B 交付报告.md>)。

## 客户端和归档

006B-C NOT RUN。客户端资源已准备于client_resources/，使用用户原始ice_ether_block.png；BlockState、Block/Item Model及中英文名称已配套。client_translations/是MC1.21.1语言包源，分别覆盖冰之以太/Ice Ether、冰之以太粗矿/Raw Ice Ether、冰之以太块/Ice Ether Block，既有注册ID与原005/006A制品不变。世界放置、背包/手持及语言显示仍需实机验收，不能继承006A-C。
只按服务端受控Registry事实交付。长期证据仅发布脱敏副本，各原SHA/副本SHA分别记录；本地ZIP不是远端持久化。没有上传接口/授权会话时明确NOT PERSISTED。

客户端包以冻结服务端fixture为输入，保留所有原JAR成员字节，只添加六份资源，不重编译Java；compat使用原服务端同SHA。资源版fixture使用独立新SHA，原服务端PASS不冒充新包客户端PASS。

```zsh
zsh tests/block_registry/build-mac-kit.zsh \
  --compat "$DF006B_JAR" --fixture "$DF006B_FIXTURE/block-registry-probe.jar" \
  --validation out/convergence-006b/summary.json \
  --output-dir out/convergence-006b/mac-client-kit/本轮新目录
```

安装与实机检查见 [MAC-SETUP](MAC-SETUP.md) 和 [CHECKLIST](CHECKLIST.md)。ID仍为blockregistryprobe:sample_block，不改变Java注册声明。
本次正式包指纹与部署记录见 [006B-C部署说明](<../../Dev Manuals/Convergence-006B-C 客户端测试包部署说明.md>)；此前006B交付报告保留原时点事实。
