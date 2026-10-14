# 客户端实际对象命名统一

本目录是最新客户端Forge输入，原005、006A、006B fixture继续作为冻结回归输入保留。唯一生产实现仍在dualforge/src/main；本轮没有修改兼容层Java/API/构造器ABI。

|源输入|实际Registry路径|Java字段／方法|资源名称|
|---|---|---|---|
|items|multiregistryprobe:ice_ether|ICE_ETHER／iceEtherValue|ice_ether|
|items|multiregistryprobe:raw_ice_ether|RAW_ICE_ETHER／rawIceEtherValue／createRawIceEther|raw_ice_ether|
|block|blockregistryprobe:ice_ether_block（Block与Item两Registry）|ICE_ETHER_BLOCK／ICE_ETHER_BLOCK_ITEM／iceEtherBlockValue／iceEtherBlockItemValue|ice_ether_block|

新源码使用原Forge52.1.0依赖和未打补丁MC1.21.1编译；普通Lambda、零捕获静态工厂、BlockItem Holder依赖、构造器与业务保持。三个用户PNG原字节使用；英文/中文语言键直接对应实际新ID，不依赖旧名称覆盖。
各组观察Driver实际查询新注册表、Holder与对象身份、原supplier栈，并明确拒绝仍存在alpha_item/beta_item/sample_block。Driver不创建或直接注册测试对象。

## 重跑

所有JVM串行，仓库根执行；输出为独立out/client-objects，不覆盖原结果。

```zsh
DF_SERVER_INPUT="$PWD/runtime/neoforge-server"
DF_JDK='/实际/Java21'
DF_FORGE='/锁定Forge52.1.0三份依赖'
python3 -B tests/client_objects/build_fixtures.py --role items \
  --server-input "$DF_SERVER_INPUT" --forge-dependencies "$DF_FORGE" --jdk-home "$DF_JDK"
python3 -B tests/client_objects/build_fixtures.py --role block \
  --server-input "$DF_SERVER_INPUT" --forge-dependencies "$DF_FORGE" --jdk-home "$DF_JDK"
```

分别用role=items/block执行Contract和C/T：

```zsh
zsh tests/client_objects/run-client-objects.zsh --role items --stage contract \
  --fixture-result out/client-objects/items/fixture.json \
  --jar "$DF_COMPAT_JAR" --matrix '/原冻结006A/registry-matrix.jar' \
  --server-input "$DF_SERVER_INPUT" --jdk-home "$DF_JDK"
```

block使用原冻结block-matrix.jar。Control/Treatment增加`--allow-server-start`，当前云端还加实际java-network-args和public-key-relay；两组只各自切换原DF006/DF006B_REGISTER。
复用原严格Contract，原Java测试不修改；每次保存测试副本，只更新expected ID和getter选择字符串，生成identity.diff和原/副本指纹。断言逻辑、负例、ASM验证、canonical逆向对照和runtime dump对照不变。
原runner默认ID断言保持；额外角色参数只供本目录新输入，实际stdout不改写。

打包前要求两组Contract/C/T同compat/fixture/driver/environment，复用原严格cohort函数校验；旧历史结果按同compat SHA引用，明确属于先前实际运行。

```zsh
python3 -B tests/client_objects/build_mac_kit.py --compat "$DF_COMPAT_JAR" \
  --historical out/identity-unification/validation.json \
  --output-dir out/client-objects/mac-kit/本轮新目录
```

包内分items-test与block-test，各含同compat和已测Forge JAR、对应JVM参数；分别独立Prism实例验收，不混装两组。详见[MAC-SETUP](MAC-SETUP.md)与[CHECKLIST](CHECKLIST.md)。
状态保持CLIENT NOT RUN，实际give/背包/手持/放置/中英文显示及保存重进后再填写。原世界、冻结输入及旧命名证据不改写；不实施旧世界ID迁移。

本轮实际结果及正式包SHA见[交付](<../../Dev Manuals/Ice Ether 实际对象命名统一交付.md>)与[证据索引](<../../Dev Manuals/Ice Ether 实际对象命名统一证据索引.json>)。
