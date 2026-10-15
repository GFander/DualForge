# 安装 DualForge

目标环境：Minecraft 1.21.1、NeoForge 21.1.256、Java 21。

具体第三方结果见 [COMPATIBILITY](COMPATIBILITY.md)：Food N' Fuel Forge1.3在007C-1可被发现并解析，但FML拒绝缺失的必需forge依赖，尚不能按本安装方式正常使用；原生Forge对照不是DualForge兼容PASS。

1. 安装正常的 NeoForge。
2. 将 `DualForge.jar` 放入实例的 `mods/`。
3. 将当前受支持的原生 Forge 模组 JAR 放入同一 `mods/`。
4. 使用原来的游戏启动方式。

无需填写 DualForge JVM 参数、添加 Agent、替换官方运行库或预转换模组。
用户运行 JAR 不需要 Python、zsh 或开发工具。构建工具只用于开发者从源码制作 JAR。

当前007B在007A基础上验证普通无条件Item注册：单IEventBus构造器、同类零捕获纯Item工厂、typed字段及get-only访问。原005/006A/006B实验fixture的C/T业务开关保留，不能当作普通用户安装步骤。
采用宿主NightConfig标准TOML；单IEventBus构造器与经过验证的调用点限制仍适用；automatic模块默认名由真实FML metadata对齐，不匹配的显式JPMS descriptor拒绝。第三方 Clumps 19.0.0.1 的 Forge 1.21.1 版尚不兼容：007历史多行TOML准入失败保留为历史事实，007C-1只做完整TOML静态解析回归，未重新启动其功能，不能宣称已兼容。

诊断从实例 `logs/latest.log`、`logs/debug.log` 查找。`Controlled Forge transformation rejected` 或 `unsupported Item profile` 表示结构超出当前范围；保留原 JAR、版本和日志以定位原因。`Skipping jar ... cannot be loaded` 表示该模组未加载，不能忽略后宣布兼容。

开发者可显式设置四个 `dualforge.*` 布尔属性为 `true` 或 `false`；非法值会拒绝启动。默认不输出 class dump，仅在显式指定 `dualforge.dump.path` 或 `dualforge.dump.profileDir` 时写入诊断文件；写入失败明确拒绝转换。

007 的 Mac 零参数图形验收为 **NOT RUN**。历史 006B-C 人工 PASS 不属于新 JAR 与新安装方式。
