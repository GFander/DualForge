# 007B：普通Forge双Item即插即用输入

`src/` 仅使用Forge52.1.0与未打NeoForge补丁的Minecraft编译，一个DeferredRegister<Item>、普通零捕获Lambda和本类零参数静态工厂，构造器无条件挂接注入的IEventBus。没有测试业务属性、marker打印、预转换或DualForge/NeoForge编译依赖。
物品为dropinprobe:ice_ether / raw_ice_ether，资源使用用户既有两个PNG原字节，原005/006A/006B夹具不改。

`driver/` 是独立NeoForge查询观察输入；只在ServerStarted查询真实Registry/Holder/对象身份，不创建或注册Item。模块名取实际FML metadata，不能假定与文件名或modId固定相等。用户安装/Mac包均不需要Driver。

```zsh
python3 -B tests/dropin_items/build_fixture.py --server-input "$DF_SERVER_INPUT" \
  --forge-dependencies "$DF_FORGE_DEPENDENCIES" --jdk-home "$JAVA_HOME"
# 如仅修订观察器，--existing-forge-jar 保留冻结原Forge JAR并核对其源码/资源指纹。

python3 -B tests/dropin_items/run_contract.py --server-input "$DF_SERVER_INPUT" \
  --jdk-home "$JAVA_HOME" --jar "$DF_COMPAT_JAR" --fixture "$DF_FORGE_FIXTURE" \
  --native-control "$DF_NATIVE_CONTROL" --matching-fixture "$DF_FROZEN_006A"

python3 -B tests/bootstrap/run_bootstrap.py --install "$DF_OFFICIAL_INSTALL" \
  --jdk-home "$JAVA_HOME" --output out/convergence-007b/new-run \
  --mod "$DF_DELIVERY_JAR" --mod "$DF_FORGE_FIXTURE"

python3 -B tests/dropin_items/build_mac_kit.py --jar "$DF_COMPAT_JAR" \
  --fixture "$DF_FORGE_FIXTURE" --tested-commit "$DF_TESTED_COMMIT" \
  --output out/convergence-007b/new-mac-kit
```

先以原Profile复现正常Supplier及构造器布局拒绝；初步扩展后保留实际模块名不匹配失败。随后仅对准入Forge且automatic JAR名与真实FML moduleName不匹配的输入使用官方ModJarMetadata；匹配的旧descriptor原委托不变，不改变候选/Reader识别或Parser依赖语义。
不匹配的显式JPMS descriptor明确拒绝，不丢失原requires/exports/uses语义来取得成功。不新建Module Layer、不改官方库、不改用户Forge JAR。

`NormalItemBytecodeTest.java` 使用实际Reader/FML扫描，检查精确mapping反转后的全字节相等、Supplier与Bootstrap原字节、不同入口身份、未归属/原生路径、显式关闭、13个拒绝及I/O后失败原子性。未知Bootstrap、捕获、错误Registry/Bus、重复/非法ID、额外API或不同构造器ABI均明确拒绝。

范围仍为public final入口、原单IEventBus ABI、public static final typed字段、同类零捕获纯Item工厂及get-only accessors；无参/多参数、任意Supplier逻辑、Properties builder、普通无条件Block/BlockItem及第三方一般Forge API未扩展。
原旧Profile的标记/业务开关规则未放宽，历史回归须绑定同一个最终新JAR。
Mac验收流程见 [mac/README](mac/README.md)，图形验收NOT RUN；静态资源和打包不代表渲染成功。
