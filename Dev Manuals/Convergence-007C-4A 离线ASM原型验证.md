# 离线ASM原型验证

日期：2026-10-15（Asia/Shanghai）。独立 Draft [PR #22](https://github.com/GFander/DualForge/pull/22)，base 为未合并 PR #21 分支。任务起点 `29893683ea584b26208eb205b819c3f3376147be`，实际被测工具源码 `25571c40671d6d80ca10a4dcec77db84440ff555`。生产目录与起点逐文件相同。

OFFLINE_TRANSFORMATION为受控fixture PASS；FnF完整Registry链PARTIAL，RUNTIME_COMPATIBILITY未验证。新fixture原生Forge编译依赖纯度有完整命令与SHA：同官方Forge52.1.0安装，无NeoForge/DualForge编译输入。AtlasProbe、Peer、Untouched、VerifyFixture源码class/JAR均保存。原JAR5551字节，SHA `6722f1f681231beaeaa3834d0b1a153fffbb291db7cfaaf7b5a601a02d6f1adb`。

流程：读取完整JAR/Forge注解和metadata存在性→显式离线experiment许可→完整source/target owner/name/descriptor/opcode存在性→全组原始数据流→Plan→ClassNode克隆→规则重写→目标层次/指令/跨类声明调用检查→全部验证完才原子发布独立out目录。不是FML生产准入系统；不加载原FnF，不给非Forge输入自动转换。无命中class沿用原字节。

正例实际验证：@Mod/RegistryObject/DeferredRegister等显式类型迁移；ForgeRegistries.ITEMS的owner/name/descriptor一起变化；create的owner和参数/返回一起变化；Holder get迁移；字段和两类间返回/调用、Signature<Item,Item>闭包；多类共同转换；Untouched含自己的indy仍原字节。最终3个实际执行规则为registry.items.field、registry.create.from.item.registry、registry.holder.get；register(Supplier)虽检查目标存在，未由本fixture实际执行改写，不虚称覆盖。

38条Contract断言/15个负例PASS：来源descriptor/opcode、缺失target、返回不可赋值、未知Registry字段/泛型、映射冲突、MethodHandle、indy、显式JPMS、克隆后部分失败、未知依赖、缺Bus bridge、未知Holder泛型、跨类调用与声明不一致。每个负例直接比对原ClassNode canonical bytes，原始JAR前后SHA也相同。native注解输入明确UNCHANGED，失败不发布目标。

ScannerContract另9断言覆盖Condy/Bootstrap元数据、模块requires/exports、bridge/synthetic和IEEE非有限值；真实Java21 Shapes夹具检查record、sealed/nest、泛型、注解。Fixture scanner输出重复SHA相同。

ASM CheckClassAdapter与基于精确target provider的SimpleVerifier分开运行；未知父类不猜Object。ClassWriter(0)保留控制流，Frame仅随显式类型映射，不重算frame。随后独立java -Xverify:all实际加载fixture/反射检查声明，并核对空Holder receipt；stdout明确registry lifecycle NOT RUN。未构造真实注册表、未挂ModBus、未调用Supplier或游戏事件，因此不将该JVM验证称作NeoForge运行成功。

原始FnF用相同离线许可检查，精确报告RUNTIME_SHIM_REQUIRED:FMLJavaModLoadingContext，无输出class；不跳过Context、不加入生产解包方法、不隐藏helper/Tab/全局bus缺口。

失败开发记录保留：初次全扫描遇Infinity的JSON序列化异常，改为IEEE bits后重跑全部输入；初次fixture的测试driver字符串拼接生成indy，离线管线明确拒绝，随后仅修改独立测试driver使用String.concat（未改FnF）；普通unclaimed class自身indy保持字节。中间失败记录不混入最终PASS，最终源和全部正式结果绑定固定提交。

63个Python测试PASS（19个新研究测试+44个既有单测）、58份Python AST、25个zsh语法、374项冻结保护、21份生产文件大小/SHA均PASS。没有游戏服务器启动。详情及每条命令、stdout/stderr/exit在索引与归档。
