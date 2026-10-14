# Ice Ether 实际对象命名统一交付

2026-10-14（Asia/Shanghai）。**实际对象ID、字段/方法、资源一致；两组新Contract/C/T PASS；客户端图形NOT RUN。**

## 本轮问题及修正

上次资源版仅改变贴图与显示名称，仍实际注册alpha_item、beta_item、sample_block。用户要求实际对象统一，因此在tests/client_objects保存新的原生Forge输入，从源码重新编译，而非以语言覆盖或预改写class冒充对象重命名。
原005/006A/006B输入、JAR、世界、结果和报告原样保留；这三个历史名称继续出现在冻结证据及负例断言中，不能全仓库替换。

|实际新ID|字段／getter／工厂|显示名|
|---|---|---|
|multiregistryprobe:ice_ether|ICE_ETHER／iceEtherValue|冰之以太 / Ice Ether|
|multiregistryprobe:raw_ice_ether|RAW_ICE_ETHER／rawIceEtherValue／createRawIceEther|冰之以太粗矿 / Raw Ice Ether|
|blockregistryprobe:ice_ether_block（Block及BlockItem）|ICE_ETHER_BLOCK／ICE_ETHER_BLOCK_ITEM／iceEtherBlockValue／iceEtherBlockItemValue|冰之以太块 / Ice Ether Block|

同名models/textures及blockstates、语言键直接对应实际Registry路径，模组namespace与扫描身份保持原样。两张Item原图和用户16×16方块PNG逐字节保留。
items角色保持一个Item DeferredRegister、普通Lambda及零捕获同类静态工厂；block角色保持两个Registry、一个普通Block及依赖其Holder的普通BlockItem。单IEventBus构造器ABI、业务与挂接时序不扩展。

## Git、编译与验证归属

本轮起点`7e7b1af4d7e99bcd42cc98cc61472ac887fee8ca`，干净工作区，fetch确认main仍为`de4e54002c3b777fb392a4699fdbcbe2261965e0`；继续原PR #13分支codex/convergence-006b-block-registration。
输入/入口提交`90d566ea3a02590d74a0198b49de4c9afd5013aa`。测试在提交前进行，保留source输入指纹；提交后全部实际编译源及资源逐字节等于Git blobs，打包在该提交进行，后续仅文档。

生产Java/资源没有修改，原兼容层JAR仍为66,626 bytes：
`159514dd5c88cc445117d6a4778db5e85cf0a496da2651f4faefbdbcacb3905b`。

|本轮实际编译JAR|SHA-256|
|---|---|
|新双Item Forge输入|`9057a2bdc8d12ec50dfbd0693714d9fa20a08263cc70ec62a1824ab728cb3516`|
|其NeoForge查询Driver|`d9e2d499b3490f299dd6b1e9c04bd8784f54ac9a9ae8ba98e54edb1f3b4c9f6b`|
|新Block/BlockItem Forge输入|`3768e817c101172d829aea1feeb05ff1e7600ba4c31da1ec4bac195ea0e50f0e`|
|其NeoForge查询Driver|`b611766c145ad17639869cc55c8e1b22f6fe2fb0698945f3c3880fefe9d8aec2`|

原Forge52.1.0三份依赖及vanilla MC未patch身份重新核对，JDK实际Microsoft21.0.10+7-LTS；Forge入口class无NeoForge/DualForge编译owner。新Driver独立使用NeoForge依赖，只查询/观察，不创建或直接注册对象。
新javap保存实际注册字符串、字段、getter、工厂及Bootstrap；源码、class/JAR成员、编译命令与退出、raw stdout/stderr/日志/环境/断言/dump/SHA在[独立证据索引](Ice%20Ether%20实际对象命名统一证据索引.json)。

## 本轮实际结果

- 两组Contract PASS：完整原正负/范围/原子性suite、ASM验证与canonical逆向恢复；保存10/19份拒绝reason。原Java Contract测试不修改，只在运行副本更新expected ID和getter选择字符串，identity.diff及原/副本SHA可核对，其他断言逻辑不变。
- items C/T PASS：C新两ID均不存在、supplier0/0；T在BuiltInRegistries.ITEM实际存在新两ID、supplier各一次、Holder.get及真实对象/key正确且不覆盖，官方RegisterEvent/DeferredRegister/FML栈成立。alpha_item/beta_item实际查询不存在。
- block C/T PASS：C新Block及Item均不存在、supplier0/0；T两Registry实际注册ice_ether_block、supplier各一次、Block在Item supplier调用前已bound、BlockItem.getBlock/Holder.get/Registry对象引用一致、官方栈和MODbus成立。sample_block在两Registry实际不存在。
- 四服务器都Done→正常stop→exit0，无新增ERROR/FATAL/dump FAIL、stderr或crash。每个角色C/T仅切换原业务注册开关，同compat/fixture/driver/environment；运行使用物理副本，全部JVM串行，结束无Java进程且25565释放。
- 受影响16项Python cohort单测、当前374项历史保护、diff及新zsh入口语法PASS；最终HEAD CI实际结果见PR/最终回复，不提前声称完成。

原兼容层字节未变，所以原002–006A全回归是先前同SHA实际运行的有效历史证据，**本轮没有重新执行这些生产回归**。新输入不继承旧fixture的C/T；上述新C/T已实际重跑。同SHA历史引用参与严格cohort绑定，不把旧客户端PASS继承给新包。
首轮block编译后索引写入缺父目录，整条构建命令exit1；原编译记录保留并附事后错误注记。修复后新目录干净重建PASS，不将失败命令改写为成功。

## 最新客户端包与剩余项

本地ZIP：`out/client-objects/mac-kit/20261014-canonical-ids/dualforge-ice-ether-client-test-kit.zip`，136,994 bytes；SHA-256：
`55e7d4ca716487b056381d36429d9d693a7a9ac0b3734ed7ba195fde9a0264ea`。
包内分别为items-test和block-test，每组只有原同SHA兼容层和本轮实际测过的Forge JAR；资源已在该JAR中，JAR原字节复制，未另外改class或加名称覆盖。包含对应JVM参数、MAC-SETUP、CHECKLIST、manifest及SHA256SUMS，不含查询Driver、世界、依赖、账号或旧日志。
资源引用/名称/原PNG、ZIP CRC、安全路径/无软链接、fresh解压后清单SHA及实际JAR SHA核验PASS。仅本地文件，未上传远端，不声称持久化；此前服务端原始证据持久化限制继续保留。

Mac必须分别使用新Prism实例/新世界验收；实际命令见[MAC-SETUP](../tests/client_objects/MAC-SETUP.md)。尚未实际give、显示、手持、放置、切语言、保存重进：**CLIENT NOT RUN**。
未验证多Forge模组同时安装，未实现旧世界ID迁移，不更改模块/Loader/Transformer架构/API/ABI，不自动合并/创建Release/进入006C。
