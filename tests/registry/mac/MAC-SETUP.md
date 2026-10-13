# DualForge 005-C：Mac 人工验收

当前 005-S PASS；用户已在 macOS 27.0 arm64 + Java 21.0.7 完成人工 005-C
受控验收 PASS。这个修订包的脚本尚未在 Mac 实机复测；以下清单作为其他
运行的空白模板，不表示预先验收。本包只帮助部署和取证，不含客户端、
账号、世界、JDK、LWJGL、服务端 libraries 或测试 driver。云端不执行 Mac
图形操作；成功安装、主菜单或静态资源检查均不代表完整客户端 PASS。

## 1. 下载与核验

仅使用实际交付的 dualforge-005-mac-client-kit.zip 和发布方报告的 ZIP SHA。
若未提供经验证可下载的附件/链接，先停在传输阻塞，不能猜测 URL。
不要把 Linux 安装、JDK、native libraries 复制到 Mac。正常 Minecraft 登录
在本地 Prism 中完成，不向 Codex 提供账号密码或任何私钥。

```zsh
shasum -a 256 '/实际下载位置/dualforge-005-mac-client-kit.zip'
unzip -t '/实际下载位置/dualforge-005-mac-client-kit.zip'
unzip '/实际下载位置/dualforge-005-mac-client-kit.zip' -d '/你的独立测试目录'
DF_KIT='/你的独立测试目录/dualforge-005-mac-client-kit'
(cd "$DF_KIT" && shasum -a 256 -c SHA256SUMS)
```

ZIP 本身的 SHA 在外部交付报告/.sha256 文件中，不放进自身制造递归；
SHA256SUMS 校验包内所有 payload 文件（不含自身），脚本也会逐项校验。
manifest.json 标明源提交、原生 Forge 编译纯度证据、实际 JAR/资源指纹和
原始 005-S cohort。包内两个 JAR 就是先前被测的原文件，未重编译。

## 2. 手动创建 Prism 隔离实例

1. 从 Prism 官方站点 https://prismlauncher.org/download/ 手动选择 Mac CPU
   对应版本。Apple Silicon 使用 arm64，Intel 使用 x86_64。
2. 新建一个独立实例，选择 Minecraft **1.21.1**，在版本页安装
   NeoForge **21.1.256**。不自动升级或替换为 Forge/Fabric。
3. 在本机手动准备该架构的 Java **21**。Prism 实例设置 → Java，启用
   实例独立 Java 位置覆盖，选择实际 Java executable（可用
   `/usr/libexec/java_home -v 21` 查已安装 JDK）。不改系统默认 Java。
4. 同页启用实例独立 Java 参数覆盖，粘贴 JVM-ARGS.txt 全部七行（可空格
   连接成一行）。Prism 会写 OverrideJavaLocation/JavaPath 和
   OverrideJavaArgs/JvmArgs，preflight 核验实际实例记录。
5. 内存可设最小 1024 MiB、最大 3072 MiB，按设备余量选择。
6. 右键实例 → Folder，明确记录实例目录：它含 mmc-pack.json 和
   instance.cfg，游戏目录是其中 `.minecraft/`（通常）或 `minecraft/`。
   脚本只接受恰好一个已有游戏目录，不猜测 `~/Library` 下实例名字。

脚本使用 plutil 的 JSON parse/extract 模式，而非 plist-only `-lint`；
Prism 给整串 JvmArgs 加的一层外部双引号会先解码，再按实际参数分词。
多空格及内部参数引号保留；缺失、重复或冲突的受控参数仍拒绝，不用 eval。

默认 dump 路径在生产源码里仍有历史 Windows 值，因此本包明确覆盖为
`dualforge-005-evidence/...`，相对于 Prism 启动的该实例游戏目录；Mac 有效，
无 Linux/Windows 绝对路径。四个转换开关和 DF005_REGISTER=true 全部必须
存在。没有新增 --add-opens/--add-exports：当前正常服务端启动不使用它们；
旧 isolated bytecode/metadata harness 的模块参数不等同客户端需求。
若客户端实际报新的模块/ABI 错误，保留首个真实错误，本任务只取证。

## 3. 同一实例顺序执行 A / B / C

先确保 mods 为空，其他客户端完全退出后再安装/回滚。无需 sudo，不修改
系统安全策略，不关闭 Gatekeeper，不触碰别的实例。首次启动 A 后游戏目录
应已存在；如果尚无目录，先通过 Prism 正常启动 A，再执行 preflight。

```zsh
DF_INSTANCE='/从 Prism Folder 取得的确切实例目录'
mkdir -p "$DF_KIT/.state"
cp -n "$DF_KIT/CHECKLIST.md" "$DF_KIT/.state/CHECKLIST-completed.md"
# 编辑 completed 副本记录人工结果；不要编辑受 SHA256SUMS 保护的模板。
```

**Client A：纯 NeoForge。** 不安装两个 JAR，mods 为空。保持上述相同 JVM
配置，启动前 `touch "$DF_KIT/.state/A-start"`。用 Prism 正常启动，进入
主菜单，记录版本/CPU/Java/日志，然后完全退出客户端。

```zsh
zsh "$DF_KIT/scripts/mac-preflight.zsh" --instance-dir "$DF_INSTANCE"
zsh "$DF_KIT/scripts/mac-collect-logs.zsh" --instance-dir "$DF_INSTANCE" \
  --stage A --since-marker "$DF_KIT/.state/A-start" --output-dir "$HOME/Desktop" \
  --checklist "$DF_KIT/.state/CHECKLIST-completed.md"
```

**Client B：只安装 DualForge。** 必须先 dry-run。该步骤仅在 kit 的 .state
保存 cohort/实例/phase/mods 指纹 receipt，实例仍只读；只有匹配的 receipt
才允许 install。其他模组或同名不同 SHA 一律拒绝，不覆盖或自动清理。

```zsh
zsh "$DF_KIT/scripts/mac-install.zsh" --dry-run --phase B --instance-dir "$DF_INSTANCE"
zsh "$DF_KIT/scripts/mac-install.zsh" --install --phase B --instance-dir "$DF_INSTANCE"
touch "$DF_KIT/.state/B-start"
# 在 Prism 启动，检查主菜单、SPI/LanguageLoader/Transformer 日志；退出后：
zsh "$DF_KIT/scripts/mac-collect-logs.zsh" --instance-dir "$DF_INSTANCE" \
  --stage B --since-marker "$DF_KIT/.state/B-start" --output-dir "$HOME/Desktop" \
  --checklist "$DF_KIT/.state/CHECKLIST-completed.md"
```

**Client C：DualForge + 原生 Forge RegistryProbe。** 同实例、同七项 JVM
参数，只有新增原生 Forge JAR；兼容层已有相同文件不会被覆盖。

```zsh
zsh "$DF_KIT/scripts/mac-install.zsh" --dry-run --phase C --instance-dir "$DF_INSTANCE"
zsh "$DF_KIT/scripts/mac-install.zsh" --install --phase C --instance-dir "$DF_INSTANCE"
touch "$DF_KIT/.state/C-start"
```

用 Prism 启动 C，进入新建的隔离单人世界，允许作弊。
输入 `/give @p registryprobe:ice_ether 1`，实际确认背包图标、手持模型和无
紫黑缺失纹理。切换 English 与简体中文，分别检查 **DualForge Test Chip**
和 **DualForge 测试芯片**。保存真实背包/手持/语言截图；原图是 16×16
RGBA 蓝色菱形，禁止合成游戏截图。无需创造分类，不增加事件功能。

若 B/C 失败，不继续猜测修复。记录首个错误、对应 cohort、阶段和分类：
Discovery、FML、Class Loading、Bytecode、Registry、Client Setup、Resource
Loading 或 Rendering。收集本阶段日志再回传，不擅自扩展生产代码。

## 4. 本地诊断、回传与回滚

客户端退出后显式运行 collector。仅收集 marker 之后的 latest/debug/current
crash reports、版本和 JAR SHA、清单及你选中的 PNG 截图，不读世界或账号
文件。token/password/session/登录用户行整行隐藏；URL/IP/连接地址和
--redact-text 指定的私有域名替换。原件保持原位，REDACTION-NOTES 标明
丢失上下文。截图原字节保留，**必须人工检查截图中的名称、聊天、地址**。

```zsh
zsh "$DF_KIT/scripts/mac-collect-logs.zsh" --instance-dir "$DF_INSTANCE" \
  --stage C --since-marker "$DF_KIT/.state/C-start" --output-dir "$HOME/Desktop" \
  --checklist "$DF_KIT/.state/CHECKLIST-completed.md" \
  --screenshot '/你选择的真实背包截图.png' \
  --screenshot '/你选择的真实手持截图.png' \
  --redact-text '需要隐藏的私有域名或其他字符串'
```

collector **不上传**。先检查诊断 ZIP 每个文件，确认可分享后决定是否发给
Codex，并指出脱敏是否隐藏了必要上下文。所有人工项通过且日志/截图充分
才可将 005-C 改为 PASS。只安装成功或只见主菜单仍是 PARTIAL/NOT RUN。

```zsh
# 完全退出客户端后；无需 Java 或 Prism 工作状态即可回滚。
zsh "$DF_KIT/scripts/mac-install.zsh" --rollback --instance-dir "$DF_INSTANCE"
```

回滚只删除 mods/.dualforge-005-owned 记录的本脚本新增 JAR，且删除前核对
原 cohort SHA；用户预先已有的相同 JAR不会被删除，发生修改则全部拒绝。
新版 ledger 每行记录文件名和 cohort SHA；旧版 filename-only ledger 仍可
安全回滚（核对当前原 JAR SHA），不删除或重建用户原来的收据来绕过校验。
不删除世界、日志、截图、其他模组或游戏目录。脚本不使用 rm -rf，不安装
软件，不修改 launcher profile/账号或全局配置。恢复纯 A 后再次检查 mods
为空；未由脚本新增的文件由你自行决定如何处理。

诊断包的清单副本位于 checklist/CHECKLIST.md；它包含模板/人工陈述，不能
作为日志痕迹。应仅在 log-*.log 中按实际 logger/message 判断加载。
collector 将实例路径替换为 <INSTANCE>，避免目录名 DualForge 造成误命中；
Prism 控制台 System.out 可能不在 latest/debug 中，必要时另行手动保存
并检查脱敏，不能把缺少 println marker 当成未加载。

替换旧 kit 时将新版解压到新目录，保留旧脚本/.orig、清单、日志及世界。
不要把旧脚本复制进新包或手工改 SHA256SUMS 绕过完整性检查。两 JAR 原字节
未变，已安装的旧收据仍兼容；新的脚本验证先 preflight/dry-run，再决定操作。
