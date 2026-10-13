# Legacy 005-C 固定 cohort

本目录的原 11 个文件与 main 验收版本逐字节一致，包括 README.md 中的历史路径。
该 README 的命令是原现场记录；**当前**运行此 Legacy packager 应使用下面的新路径。
它仅接受原兼容层和原 Forge fixture，不接受本轮新 DualForge JAR。

```zsh
zsh tests/registry/mac-legacy/build-kit.zsh \
  --compat '/原历史 cohort/forgecompat-002-0l.jar' \
  --fixture '/原历史 cohort/registry-probe.jar' \
  --output-dir out/identity-unification/legacy-kit/全新目录
```

原兼容层 SHA：`ed572ca9762e235e7d264355d35b3c9d3ab9ecf1da3f2c3234b1e28056aad752`。
原 Forge SHA：`312a4f697c1dc617ed48822c10179384b6559f4e6399db9210c15541c774132c`。
旧 kit 使用自身 JVM-ARGS.txt、原制品名称和原安装收据；不要混用新包脚本或新参数。
历史 Mac arm64 人工 PASS 只属于这个固定 cohort。

面向新制品的工具和 NOT RUN 客户端状态见 [../mac/README.md](../mac/README.md)。
