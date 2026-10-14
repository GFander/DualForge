# 安装 DualForge

目标环境：Minecraft 1.21.1、NeoForge 21.1.256、Java 21。

1. 安装正常的 NeoForge。
2. 将 `DualForge.jar` 放入实例的 `mods/`。
3. 将当前受支持的原生 Forge 模组 JAR 放入同一 `mods/`。
4. 使用原来的游戏启动方式。

无需填写 DualForge JVM 参数、添加 Agent、替换官方运行库或预转换模组。
用户运行 JAR 不需要 Python、zsh 或开发工具。构建工具只用于开发者从源码制作 JAR。

当前 007A 交付默认启动与可选诊断，保留原受控兼容范围。普通无条件 Item 注册需要独立 007B Profile 扩展；实验 fixture 的 C/T 业务开关不能当作普通用户安装步骤。
简单 TOML、单 `IEventBus` 构造器与经过验证的调用点限制仍适用。第三方 Clumps 19.0.0.1 的 Forge 1.21.1 版尚不兼容：其多行 TOML 描述被当前候选准入排除，宿主日志显示 `cannot be loaded`，不能把随后游戏启动成功解释为模组成功。

诊断从实例 `logs/latest.log`、`logs/debug.log` 查找。`Controlled Forge transformation rejected` 或 `unsupported Item profile` 表示结构超出当前范围；保留原 JAR、版本和日志以定位原因。`Skipping jar ... cannot be loaded` 表示该模组未加载，不能忽略后宣布兼容。

开发者可显式设置四个 `dualforge.*` 布尔属性为 `true` 或 `false`；非法值会拒绝启动。默认不输出 class dump，仅在显式指定 `dualforge.dump.path` 或 `dualforge.dump.profileDir` 时写入诊断文件；写入失败明确拒绝转换。

007 的 Mac 零参数图形验收为 **NOT RUN**。历史 006B-C 人工 PASS 不属于新 JAR 与新安装方式。
