# Convergence-007 Gate 0：SPI / Bootstrap 取证

日期：2026-10-14（Asia/Shanghai）。此阶段生产源码只读；起点573个tracked文件逐项SHA不变。基线为实际已合并PR #13的main `ac4c4d7ef50ea35a634925d30dead0b93d798400`，不是006B任务分支。

## 前置事实

PR #13实际HEAD `e580adbed8f8d747e01daba3eef924f861129076`，已于2026-10-14合并；最终CI run `38535338593` 为success。006B-S及用户006B-C受控人工PASS分别按原报告保留，不继承为007客户端结论。

用户提供的[006B附件](https://github.com/user-attachments/files/33419811/dualforge-006b-runtime-evidence.zip)本轮实际下载并新目录解压。ZIP 4,961,881字节，SHA-256 `8829de2cbbad04701aec42aeb5483fc6b5bd42e19a110e3229174a4e02432a11`；CRC、安全路径、2965文件的Manifest/清单/解压SHA均通过。2958个索引原文件与Git原006B索引大小/SHA对应；Manifest SHA `6f0d07ea871ccd3d6fcc18aa94c7706d542e2d73812ba044e9e3393702f32324`。

当前 **PERSISTED / VERIFIED**，新增[独立复核记录](<Convergence-006B 原始证据远端复核.json>)。ZIP覆盖原006B服务端及选择的旧回归证据，不包括后续实际对象统一的客户端fixture和人工报告。ZIP内原生成时状态及冻结报告未改字节。

## 官方安装与实际动态证据

官方NeoForge 21.1.256 installer本轮重新下载，SHA `4aad931e8fd85fbf6ba35bab647811069d35ff010900492e39db1cb8b017348c`，实际 `--installServer` exit0。仅预置官方library下载缓存；无旧mods、配置或世界。
70个运行输入按官方installer/version JSON核对SHA1，再保存实际SHA256。Java为Microsoft OpenJDK21.0.10+7-LTS，FML4.0.45，ModLauncher11.0.5，Linux amd64。

| 默认文件 | 与installer模板 | SHA-256 |
|---|---|---|
| run.sh | 原字节一致 | `39a1a0ea3ab8adf0c3b6a1867903fa3cefa51747fa6e0e10a2fc57377f244d37` |
| user_jvm_args.txt | 默认注释，无参数 | `60908ccc470bc913341006fb076baf75b1ce2d9c132b837aec133e8d98474278` |
| unix_args.txt | 原字节一致 | `ae7d31a5e320c76f3e954ce7c8edc419c12c91b36a6f70351adf88d2f5c29063` |

真实入口 `sh run.sh nogui`，进程参数从/proc记录为 `java @user_jvm_args.txt @libraries/net/neoforged/neoforge/21.1.256/unix_args.txt nogui`。仅PATH选择Java21，无环境JVM options。官方自带module/add-opens属于正常安装，没有增加DualForge参数或Agent。

纯NeoForge Done/正常stop/exit0。首次默认生成server.properties的读取ERROR及云端Mojang公钥Connection refused也发生于纯基线；原始日志不改，严格无ERROR状态不标PASS。

放入原159514…兼容层JAR后，官方ModDirTransformerDiscoverer从 `mods/DualForge.jar` 自动发现TransformationService。Locator、Reader及LanguageLoader实际实例化，Reader不接管NeoForge原生metadata。**首次真正退出点是 `DualForgeEntryTransformer.<init>` 要求未设置的 `dualforge.2a.rewrite`**；SERVICE层堆栈直接证明自动发现已经完成。
无需新增Bootstrap启动器、Agent、核心补丁或固定文件名策略。

原四开关显式设置的只读审计独立保存，绝不称零配置。审计首个脚本把首参数追加到无末尾换行的默认注释；另一次未申请网络权限导致socket拒绝；修正后的独立 `configured-network` 实际Done/正常stop/exit0。全部失败日志保留。

## LanguageLoader：动态事实与静态契约分别记录

实际debug枚举内置javafml4.0.45，然后DualForge javafml9999；原生NeoForge委托到内置Provider。后续同源码007A原生共存输入记录每个Provider真实模块，独立诊断观测SERVICE/BOOT/PLUGIN/GAME层；该Agent仅用于历史加载审计，标准启动证据没有Agent。

官方 `LanguageProviderLoader` 对同名使用Map.put，最后枚举者覆盖。`9999`是版本值，不是最高版本优先策略。`IModLanguageLoader`不继承IOrderedProvider，ServiceLoaderUtil不会按priority排序。LaunchContext按实际Layer加载；JDK模块Provider的枚举规则不构成任意宿主/第三方同名Provider的通用保证。

当前固定NeoForge21.1.256/FML4.0.45多次标准启动顺序一致，原生及Forge分流均有动态证据。**没有把它提升为所有正常安装/启动器都确定可靠**。本轮未观察到目标宿主的顺序故障，因此不凭风险推断重构LanguageLoader；同名Provider机制及任意组合的稳定性仍是明确限制。

## 属性审计与决定

| 类型 | 属性 | 修改前事实 | 后续范围 |
|---|---|---|---|
| 生产 | dualforge.2a.rewrite | 缺省抛错，非法值原来静默false | 007A默认true，严格true/false |
| 生产 | dualforge.0g.apiRewrite / 0j.emptyRegistration / 0l.subscriberRewrite | 缺省抛错 | 007A默认true，保留覆盖 |
| 诊断 | dualforge.dump.path / dump.profileDir | 历史Windows fallback，profile主动写入 | 007A缺省不写，显式写入失败拒绝 |
| 冻结业务 | DF005_REGISTER / DF006_REGISTER / DF006B_REGISTER | 仅fixture决定C/T挂接 | 完全保留，不注入普通运行 |

Gate 0结论：仅放入mods足以自动进入必需SPI流程；现有硬性属性阻断后续启动。007A处理默认策略与诊断；普通Item Supplier/直线构造器因旧测试专用Profile拒绝，其实际ABI及失败点另作为独立007B依据，不混入纯Bootstrap改动。

原始文件位于 `/workspace/DualForge-007/out/convergence-007/preflight/`、`gate0/`；精确大小/SHA见[007证据索引](<Convergence-007 证据索引.json>)。默认文件与已结束运行副本的library指纹均保存；经用户明确授权，只清理本轮已结束副本的重复运行库，日志/结果/世界/原输入不删。
