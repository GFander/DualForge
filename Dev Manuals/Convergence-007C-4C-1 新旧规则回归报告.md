# 4C-1 新旧规则回归报告

验证环境：Linux x86_64、Microsoft OpenJDK 21.0.10+7-LTS、Minecraft 1.21.1、NeoForge 21.1.256 / FML4.0.45、Forge52.1.0。实际版本输出、官方库指纹、原生 Fixture 的源码/class/JAR 和平台观察器身份均保存在原始命令附件。

最终生产源码 `d850b1c7bf3b16609ed17a58b3fe2eec0c11b751`；最终 `DualForge.jar` SHA-256 `8fd97a0a4e811aed4e8e084f172341c1b808837e202a22986705e9f0c33360c2`。所有本轮最终兼容层验证绑定该 SHA，不沿用初版 JAR 的结果。

## 实际静态 Contract

| 验证 | 实际结果 |
| --- | --- |
| 新 Supplier 类型/链接 | 67个真实发行 Supplier 只读通过；19个负例拒绝，86 assertions |
| 三个新 Fixture 完整计划 | 各2个业务方法规范化相同；各1个后续 Supplier 失败整计划拒绝 |
| 原4B Helper | 110 assertions / 35负例，归属/SHA/原子发布/clone/旧业务负例保留 |
| 007B Normal Item | 原13负例与1个显式JPMS拒绝全部通过 |
| Context | 原80 assertions / 22负例通过，旧Profile不误分类 |
| Event004 / Registry005 / Multi006A / Block006B | 原正负例、scope和atomicity原断言全部通过 |
| Metadata007C-1 / Platform007C-2 | 原只读Contract通过；不启动FnF |
| 生产RegistryBytecodeVerifier | 10 assertions，8个损坏字节码负例通过；synthetic ASM |
| FnF版本化拒绝判定 | Java 2正例/9负例通过；精确supplier-v1收据通过 |
| Python | 8目录84个测试通过，包括错构建/错输入/错观察器/错条件cohort负例 |
| 仓库保全 | 374项冻结保护、命名迁移契约、zsh语法与diff检查通过 |

完整调用、退出码、stdout/stderr、before/after class 与每个原断言在证据索引逐项映射。新 verifier 负例没有发布接口；三组完整计划负例和旧4B Transformer负例分别检查计划及实际发布边界，不能把三个层次混称为同一个测试。

## 实际动态门禁

三个新身份 ordinary/data/subclass 的 Forge Treatment 先于 NeoForge 执行；每个 case 的原始 Forge JAR 字节与其后 NeoForge C/T 相同。新制品的三组 NeoForge C/T、原4B C/T和独立8项桥接拒绝诊断的最终状态见交付索引。所有 JVM 串行，原版启动文件，运行目录为物理副本，原输入/世界不覆盖；正常C/T不注入诊断属性、Agent或额外模块参数。

同轮空宿主基线包含 Settings 和 Yggdrasil 网络 ERROR；实际功能运行严格比较完整 severityRecords，保留 `strictZeroErrors=false`。不能将“与基线相同”写成零ERROR，也不能用生命周期PASS替代 Registry/业务查询。

本轮不重跑不受影响的整套历史服务端002/003/004/005/006游戏矩阵，不把旧动态结果标为新JAR动态PASS。它们的相关静态Contract已按上表重跑；新JAR实际游戏证据只来自本轮明确列出的新三组和4B/桥接运行。客户端、真实吃食物、燃烧、Tab和FnF完整运行均NOT RUN；C4A原PARTIAL不变。

## 失败及非最终尝试

保留所有尝试：首次fixture因Forge patched server classpath优先级错误导致override编译失败；第二次纯Forge编译成功后冻结原字节。中间生产构建遇到未提供的asm-commons依赖后改为现有ASM core SignatureVisitor；不新增依赖。

初版JAR `65b75c27edfd1fde4b7c2feb298be310ddd3c40033aec7ccc551d63d721e3a18` 的三组和4B C/T通过，但随后 protected-receiver 新负例实际FAIL（错误接受）。生产已收紧跨包protected成员访问，最终JAR全部受影响验证重跑。初版PASS不得混入最终cohort。

一次历史命令重放未替换旧compat路径，前置几个静态结果不能计入最终SHA，且新的FnF精确拒绝检查确实使该尝试失败；整个重放作废保留。修正为按 `--jar` 参数显式绑定后，使用新输出目录重新执行全部相关Contract。首次总驱动的FAIL也保留，不将其概括为整体PASS。
