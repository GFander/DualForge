# 007C-3：入口Profile与构造期Context

原生Forge52.1.0输入在`fixture/`：ContextProbe与独立包ContextPeer为public非final、唯一无参构造器，直接调用官方FMLJavaModLoadingContext.get().getModEventBus()，只保存Bus观察字段和打印构造marker，不注册Item。先在真实Forge52.1.0启动，再把同一原字节JAR用于NeoForge；冻结源/class/JAR SHA与原生证据路径在`fixture-identity.json`。

`driver/`两种观察器分别用官方Forge和NeoForge编译。Neo观察器是独立原生模组，仅在ServerStarted读取来源视图的private delegate/owner并对照真实FML container.getEventBus，证明两个模组拥有独立、正确且非全局的Bus。视图不等于原生Bus对象；观察不代表Forge Bus register/post/listener API或回调兼容。native构造器尝试借用Forge Context必须明确拒绝。

构建（保存源码副本、编译class、命令、所有依赖与制品SHA）：

```zsh
python3 -B tests/entry_context/build_fixture.py \
  --forge-install <official-Forge-52.1.0-install> \
  --neo-install <official-NeoForge-21.1.256-install> --jdk-home <jdk21>
```

Forge夹具编译classpath只含官方Forge安装运行库，没有NeoForge或DualForge编译输入；Neo观察器单独编译。实际原生门禁使用`run_stock.py --host forge`先空宿主，再新副本安装contextprobe.jar/contextpeer.jar/forge-context-observer.jar。检查各构造marker一次、`[DF007C3-NATIVE] CONTEXT PASS owners=2 distinct=true`、生命周期与同轮空宿主错误计数。`run_stock`只判断生命周期，不自动代表Bus功能通过。精确本轮命令和断言脚本见证据索引的native-context-v2；首次新夹具同包导致的JPMS失败保持记录，修正仅为peer独立包。

```zsh
python3 -B tests/entry_context/run_contract.py \
  --jar <same-final-dualforge.jar> --server-input <official-NeoForge-install> \
  --jdk-home <jdk21> --fixture <native-verified-contextprobe.jar> \
  --peer <native-verified-contextpeer.jar> --fnf <original-FnF.jar> \
  --normal <frozen-007B.jar> --legacy <frozen-005.jar> \
  --block <frozen-006B.jar> --probe <original-Probe.jar> --native <native-bootstrap.jar>

python3 -B tests/entry_context/run_runtime.py \
  --jar <same-final-copy-named-DualForge.jar> --install <official-NeoForge-install> \
  --jdk-home <jdk21> --native <native-bootstrap.jar> \
  --fixture-index tests/entry_context/fixture-identity.json
```

Runtime也可接受本地新构建的fixture result.json；应先独立完成并记录对应原生门禁，不继承旧JAR的原生PASS。当前冻结manifest的绝对路径描述原现场，外部重跑需要选择自己实际生成的index。Native/Neo两侧原JAR身份必须逐SHA对应。所有JVM严格串行。

Contract使用真实Reader/FML扫描，独立选择旧BLOCK/NORMAL_ITEM/LEGACY_ITEM/EVENT和新CONTEXT。未知、歧义、两个Context来源、额外ctor、错误静态调用/返回类型、foreign owner、未知副作用、错误local/stack、MethodHandle/indy、额外入口方法、错误helper归属/描述符/第二helper失败、API关闭和dump后失败均拒绝；每次保存原/调用者后字节。显式JPMS仍拒绝，native和unclaimed路径不改。旧Normal非final仍按原class structure拒绝，不能落入新Profile。两个Context调用精确逆映射后与原class全字节相同，helper不加入targets或修改。

离线Contract仅显式设置FML VersionInfo/dist；exports/opens/classpath不属于用户启动要求。运行归属和Bus身份另由真实GAME层/官方容器完成，不使用合成容器充当动态证明。

最终FnF仍只安装兼容层和原始发行JAR，以官方标准命令启动；无观察器、测试SPI、DualForge属性或模块参数。007C-2矩阵本轮使用`--fnf-next-stage registry-helper`，历史默认`item-profile`保持原严格断言。现在必须同时看见平台VERIFIED、新Context分类/两调用改写/绑定、真实FnF构造及ItemRegistries初始化中的ForgeRegistries缺类；生命周期FAIL不能改写成整体模组PASS。

仅将两个已验证Context方法调用映射到DualForge命名空间shim，保持无参ctor、原Forge IEventBus、局部/帧与业务调用。shim读取当前ThreadLocal active container并核对caller构造方法、metadata、module及loader，getter再核对归属；无全局Bus、额外ThreadLocal、新Layer或新Forge Event方法。只读来源ABI视图关联真实Mod Bus。跨类Registry、SRG、Food/Fuel/Tab、一般异步/重入Context及客户端不在本轮范围。
