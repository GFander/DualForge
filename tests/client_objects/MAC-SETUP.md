# Ice Ether：实际注册对象命名统一后的客户端验收

此包的实际注册路径、Java对象字段、模型/贴图文件与语言键已统一。不是旧alpha/beta/sample对象的名称覆盖包。

|实际注册ID|Java字段|中文／英文|
|---|---|---|
|multiregistryprobe:ice_ether|ICE_ETHER|冰之以太 / Ice Ether|
|multiregistryprobe:raw_ice_ether|RAW_ICE_ETHER|冰之以太粗矿 / Raw Ice Ether|
|blockregistryprobe:ice_ether_block（Block及BlockItem）|ICE_ETHER_BLOCK / ICE_ETHER_BLOCK_ITEM|冰之以太块 / Ice Ether Block|

使用新建Prism实例MC1.21.1 + NeoForge21.1.256 + 本机架构Java21，先在包根执行`shasum -a 256 -c SHA256SUMS`。
本包包含两组独立测试：items-test是两个物品，block-test是方块及关联BlockItem。分别新建实例和启用作弊的新世界；每次只安装该组mods下的两个JAR，使用该组JVM-ARGS.txt作为实例Java参数。
命名变化没有实现旧世界ID迁移；原世界、实例和旧测试包均保留，不用于这次新ID验收。

## items-test

复制items-test/mods中的dualforge.jar和multi-registry-probe.jar。使用items-test/JVM-ARGS.txt，然后执行：

```text
/give @p multiregistryprobe:ice_ether 1
/give @p multiregistryprobe:raw_ice_ether 1
```

同时检查两个物品的背包/第一及第三人称手持、原用户两张不同贴图、英文及中文名称。
原alpha_item与beta_item未注册，命令应不存在；实际新ID由服务器查询driver已经独立验证，不依赖语言包覆盖。

## block-test

复制block-test/mods中的dualforge.jar和block-registry-probe.jar。使用block-test/JVM-ARGS.txt，然后执行：

```text
/give @p blockregistryprobe:ice_ether_block 16
```

检查背包、手持、实际右键放置及六面原始用户蓝色贴图；分别核验Ice Ether Block与冰之以太块。保存退出重进检查方块仍存在。
原sample_block未注册。PNG含半透明像素，本轮保持普通Block渲染路径，实际显示效果仍需人工记录。

两个组分别保存本次latest.log/debug.log、实际mod SHA、Java/Prism/macOS版本及给物品、手持、放置与中英文名称截图。原件本地保留，分享副本先检查隐私；不要上传世界、依赖或账号。
资源静态、打包与新ID服务端C/T均不代表客户端渲染PASS；实际Mac执行并复核前维持CLIENT NOT RUN。
不混装两组Forge模组，不自动合并、不创建Release、不进入006C。
