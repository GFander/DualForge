# Convergence-006B-C 客户端测试包部署说明

2026-10-14（Asia/Shanghai）。**资源与打包PASS；CLIENT_READY / 006B-C NOT RUN。**

本次在PR #13已有006B实现上继续准备客户端，不重新实现兼容层、不改历史报告。起点`7273e60334d9912d8b7ac6cbba1d45b0fe0caaad`，资源/入口提交`cf900030a51781795d1edb50e1e4402e9f2ba18d`；最终文档HEAD及CI见PR。

## 用户贴图及资源

[E] 上传ice_ether_block.png.zip中的原始PNG为16×16、RGBA、863 bytes，SHA-256：
`cae07e312af545d8a98b5347032d51353583b22edc9fa39cea5353c689e56596`。
PNG全部chunk CRC通过，无文本个人元数据；保留原字节和alpha（139–255），忽略ZIP中的__MACOSX伴随文件。

[D/S] 普通Block ID仍为`blockregistryprobe:sample_block`。新增六份资源：BlockState→cube_all方块模型→用户贴图，BlockItem继承方块模型，以及en_us/zh_cn的Block名称。
六面使用同一贴图；英文名Ice Ether Block，中文名冰以太方块。本轮仍为原普通Block，半透明像素的实际显示效果需实机记录，不增加客户端API或渲染层设置。

## 制品与实际验证

正式包：`out/convergence-006b/mac-client-kit/20261014-ice-ether-final/dualforge-006b-mac-test-kit.zip`。

|制品|SHA-256|
|---|---|
|兼容层（原服务端同JAR）|`159514dd5c88cc445117d6a4778db5e85cf0a496da2651f4faefbdbcacb3905b`|
|原冻结服务端Forge fixture|`c65cf9736db71b61f4547e386c01dbe688195df106a63a240a6b30f24de63e71`|
|客户端资源版Forge fixture|`9b19986bf3ee0a210b3449d0e7191d8887820294fc17a492ab11d2f813e19320`|
|正式客户端ZIP（71,549 bytes）|`8449815ee324cd24c73e07b77a82da79067ee7f6fe3549d4ef658ab22590ac52`|

[E] 生产Java/资源和原fixture Java/metadata未改，未执行Java编译。资源版复制原JAR全部成员字节，只新增六资源；两class的SHA分别保存，仍包含Forge API且没有预改成NeoForge。
实际检查：资源JSON引用及名称、原图SHA、原JAR成员及class逐字节对照、同服务端cohort的来源锁定、ZIP CRC/安全路径/无软链接、独立新目录解压后SHA256SUMS均PASS。
全部2,958项旧rawEvidence大小/SHA不变；当前命名及374项历史保护PASS，新zsh入口语法PASS。Python3.12.14/zsh5.9；实际命令、构建输入、结果、清单及SHA见[独立部署索引](Convergence-006B-C%20资源与部署索引.json)。

[E] 预览包在未提交源码时构建，原结果保留；正式包在上述cf90003提交上生成，manifest记录精确提交及11份输入的指纹。后续只增补文档，不修改包内容。
[S/E] 生产代码和compat字节未变，因此原服务端结论仍指向原已测cohort。本轮没有重新执行生产回归或图形客户端；不把原服务端fixture的PASS归于新资源版客户端JAR。

包含两个mods JAR、MAC-SETUP、JVM-ARGS、CHECKLIST、manifest和SHA256SUMS，共7文件；没有观察driver、依赖、世界、账号或日志。ZIP仅在本地out，未上传远端，不能宣称持久化下载。

## 安装与验收

见包内[MAC-SETUP](../tests/block_registry/MAC-SETUP.md)。新建Prism实例DualForge-006B-Mac，MC1.21.1 + NeoForge21.1.256 + 本机架构Java21，核验SHA后只安装本包两JAR，并使用本包七项JVM参数。
在启用作弊的新世界执行`/give @p blockregistryprobe:sample_block 16`，检查背包、第一/第三人称手持、实际右键放置及六面纹理；保存退出重进，检查方块仍存在。分别核验中英文名称，正常退出并保存本次日志及必要截图。

[H] 实际give/放置/显示/名称/退出尚未执行：**006B-C NOT RUN**。用户填写[CHECKLIST](../tests/block_registry/CHECKLIST.md)，提供隐私检查后的本次原始日志与截图，复核实际cohort后再更新客户端状态。
不继承006A-C、不自动合并、不创建Release、不进入006C。现有服务端原始证据持久化限制继续保留。
