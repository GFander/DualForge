# 005-C Mac kit：云端准备，Mac 原生人工运行

macOS 用户说明：[MAC-SETUP.md](MAC-SETUP.md)，人工清单：[CHECKLIST.md](CHECKLIST.md)，
必需的五个受控开关及两个相对 dump 路径：[JVM-ARGS.txt](JVM-ARGS.txt)。
`scripts/` 包含 preflight、安装/回滚、只生成本地脱敏诊断的 collector，
以及共享的版本、SHA、实例验证。无 Python 的 Mac 也可使用这些 zsh 脚本；
依赖 Mac 系统 plutil、shasum、Perl、zip/unzip，不自动安装软件。

云端直接使用两份 **原始已测 005-S** JAR，不重编译、不复制 Linux 安装：

```zsh
DF_COMPAT='/实际原 cohort/forgecompat-002-0l.jar'
DF_FIXTURE='/实际原 cohort/registry-probe.jar'
zsh tests/registry/mac/build-kit.zsh \
  --compat "$DF_COMPAT" --fixture "$DF_FIXTURE" \
  --output-dir out/convergence-005/mac-client-kit/一个全新目录
python3 -B tests/registry/mac/test_mac_kit.py
```

packager 拒绝不同 SHA 的输入；两份 JAR 的固定指纹在原 005 报告中。
输出仅包含所需的 `dualforge-005-mac-client-kit/` 树（另有共享验证脚本），
生成 manifest、SHA256SUMS、ZIP、ZIP 外部 .sha256 与实际 result.json。
manifest 记录源码 commit、原始 cohort、资源 SHA、Mac 架构与 Java 21。
不包含 Minecraft、Linux JDK/native libraries、世界、账号、driver 或依赖。

行为测试在 **Linux 上用模拟 Mac/Prism/Java 元数据** 执行实际 zsh 脚本；
覆盖平台拒绝、B/C 干净安装、dry-run 约束、用户文件/世界保护、回滚原子性、
错误版本/Java/开关、symlink、kit 篡改和日志脱敏。不是 Mac 脚本实机结果，
更不是图形客户端 PASS。真实 Mac A/B/C 与渲染只能由人工证据裁定。

下载 URL 只有真实上传并验证后才能记录。当前云端未上传包，不使用未经
授权的公共 Release、Gist 或网盘，也不自行开启 SSH。若应用无法取出云端
工作区 ZIP，则传输仍阻塞；包保留，不能声称已在用户 Mac 部署。
完成云端准备后等待用户执行 MAC-SETUP，不自动合并 PR #7 或开始 006。
