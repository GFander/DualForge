# DualForge 新制品 Mac kit

新 cohort 的真实 macOS 客户端验收 **NOT RUN**，不继承旧 005-C PASS。
[MAC-SETUP.md](MAC-SETUP.md) 提供原生 Prism A/B/C、精确 MC 1.21.1 / NeoForge 21.1.256、
Java 21、安装/回滚和本地脱敏取证；参数在 [JVM-ARGS.txt](JVM-ARGS.txt)。
Mac 脚本依赖系统 plutil、shasum、Perl、zip/unzip，无需 Python。

打包前须完整执行 [身份迁移验证](../../project_identity/README.md)。packager 比对
新兼容层 SHA 与真实 Contract/C/T/Regression，并验证原字节 RegistryProbe 和资源。

```zsh
DF_COMPAT='/实际本轮构建/dualforge.jar'
DF_FIXTURE='/固定原 005 cohort/registry-probe.jar'
DF_VALIDATION='out/identity-unification/validation.json'
zsh tests/registry/mac/build-kit.zsh \
  --compat "$DF_COMPAT" --fixture "$DF_FIXTURE" --validation "$DF_VALIDATION" \
  --output-dir out/identity-unification/mac-kit/全新目录
# 测试只模拟 Mac/Prism/Java，不启动 Minecraft 客户端。
DF_TEST_COMPAT="$DF_COMPAT" DF_TEST_FIXTURE="$DF_FIXTURE" \
DF_TEST_VALIDATION="$DF_VALIDATION" DF_TEST_LEGACY_COMPAT='/原历史兼容层制品' \
  python3 -B tests/registry/mac/test_mac_kit.py
```

ZIP 为 `dualforge-identity-mac-client-kit.zip`，包含新 `dualforge.jar`、原 Forge
RegistryProbe、manifest、COHORT-SHA256.txt、SHA256SUMS、脚本和空白人工清单。
不含世界、依赖、账号、JDK、客户端或 driver；不自动上传或发布 Release。
旧 JAR、旧参数或其他模组混入实例时明确拒绝；不覆盖用户文件。

[mac-legacy](../mac-legacy/README.md) 保留原已验收 005-C kit 的源码、固定 SHA 和参数，
整体移动后原字节未变。它只服务历史 cohort；新旧 kit 使用独立收据和输出路径。
新 kit 的实机人工记录齐全后才能更新本轮客户端结论。
