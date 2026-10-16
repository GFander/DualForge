# Convergence-007C-4C-3 交付报告

**受控服务端 Global register(this) / Typed Registry 组合：PASS。** 原始证据已获本次具体载荷授权并公开持久化，独立远端下载和逐文件核验通过：PERSISTED / VERIFIED。FnF完整游戏、客户端及未支持事件语义NOT RUN。

## 基线、源码和制品

PR27于2026-10-15 19:39:44 UTC正式合并。fetch后最新main为 `c4860adbea6a69e3a16499fc9eeb3e021519433a`，已确认要求的 `7e18910bf7b1bcfba182cb32fa266c717c4bbf12` 在祖先链；从该main建立独立worktree及 `codex/convergence-007c4c3-global-event-bus`。起点状态干净，其他工作区不动。

先行设计86d9ceb；实现75b3784；最终生产补正 **9fc9cde97888a2d7894252dc59702d8548d5a5e5**；测试提交bb72bb2。最终生产逐文件SHA与Git blob、实际编译输入一致。报告/导航后续提交不改生产。最终交付HEAD及Actions以PR正文/交付回复实际回读，不以旧提交CI替代。

最终 `DualForge.jar`：**152831 bytes**，SHA-256：

`a26893598d1ffdb73078373b007a4d5d54dddd1b774cb2a0fa0dd90fe5602bf7`

新原始Forge JAR：quietbus `f6f9c2bebcee3675dde67e048311ea0c9691883069de0e39a8f86760785058e0`；signalbus及catalogbus完整SHA、源码/class、编译输入与独立平台观察器见tests/global_bus/fixture-identity.json。catalogbus SHA `216bf8f66d30b42bc406344bf15e3027462af3453bdfdc8a1c8ff108a246da6a`。三个原始JAR跨Forge/Neo保持同字节，未预转换、未用Neo/DualForge API编译。

实际MC1.21.1、Forge52.1.0/eventbus6.2.27、Neo21.1.256/bus8.0.5、FML4.0.45、MicrosoftJava21.0.10+7-LTS/Linux，原始版本命令、依赖及安装输入SHA保存。Java/Javac/Javap、所有JVM严格串行；stock启动、物理副本，无业务JVM参数。

## 实际语义和生产差异

官方两宿主行为不是同名等价：Forge无handler返回，Neo无handler精确IAE；两者直接实例handler实际派发一次，重复注册同对象仍一次；static实例和继承handler处理不同。完整表、descriptor/opcode、ABI及原生对照见[语义契约](<Convergence-007C-4C-3 全局Bus ABI与生命周期契约.md>)，先Forge后Neo，未安装compat。

生产仅四文件：

- 新ControlledForgeGlobalBus独立验证唯一Context构造期来源、this、零handler或一个已验证默认public实例ServerStartedEvent marker及宿主ABI；拒绝平台类归属伪造、未知注解/参数/副作用等。
- 新ForgeGlobalEventBridge核对实际owner/module/GAME层/caller/完整计划、目标对象与官方事件Class身份/注解默认参数，再调用真实NeoForge.EVENT_BUS；与Mod Bus独立，无公开原生Bus accessor。
- ForgeRegistryTransformationPlan在Registry/Supplier/Tab/Consumer闭包完成后合并Global验证，指纹包含宿主证据，全部克隆/ASM成功后才返回一个不可变计划。候选粗选不是准入，不先发布Registry部分。
- ForgeConstructorContext只加内部owner校验入口，复用原调用者/容器规范；原Context校验器、RegistryBridge、Supplier/Tab及旧Profile字节不改。

无handler路径实际调用原生register(同一this)，只有精确来源与SHA固定官方0监听器拒绝才翻译为EMPTY_SOURCE_TRANSLATION；真实监听器表为零，不删除调用、伪造listener或吞任意异常。未知异常保留cause/UNCERTAIN，重复/重试拒绝。受支持handler的业务指令完全保持，只改已验证API边界。没有Forge/Neo全局命名空间别名、Mod/Global Bus混用或新post API。

## 最终同JAR实际验证

| Gate | 结果 |
| --- | --- |
| 官方Forge/Neo原生语义探针 | PASS，两宿主各一次，独立SDK输入，不冒充跨宿主Forge夹具 |
| 新Forge原字节输入 | 4次PASS：quiet、signal、组合Control/Treatment，无compat安装 |
| 新Neo标准运行 | 4次PASS，同最终JAR：quiet、signal、组合C/T |
| Global静态Contract | 189断言 / 31负例PASS（29整计划 + 归属scope + Profile分类） |
| Global独立故障诊断 | 6负例PASS；保留真实监听器副作用、UNCERTAIN拒绝重试、真实后续派发一次 |
| 4B / C1 Neo动态 | 8次C/T PASS，同最终JAR；旧Bridge 8负例PASS |
| 4C-2 Neo动态 | C/T PASS；原typed Bridge 6负例及官方partial failure PASS |
| 4C-2静态 | 40/25 PASS；旧blanket Global拒绝按新授权换为不支持post负例 |
| Supplier | 67只读Supplier、86断言/19负例、3整计划晚失败PASS |
| Context / 007B | 80/22、原13负例及显式JPMS PASS；旧Context validator原字节保持 |
| Helper / 历史其他静态 | 110/35、Event/Registry/Multi/Block、metadata/platform及ASM/精确拒绝收据PASS |
| Python / 冻结 / 语法 | 100测试、374冻结、zsh/diff/Python AST PASS |
| 同cohort校验 | 六组真实Contract/C/T绑定compat/fixture/observer/native输入及精确开关，PASS |
| 进程/端口 | 无java/javac/javap，实际127.0.0.1:25565 bind/listen/close PASS |
| FnF完整只读 | global-v1三目标ACCEPTED；FNF_RUNTIME=NOT RUN |
| 最终HEAD CI | PR建立后实际回读；静态CI不替代上述游戏动态 |

真实RegisterEvent先完成Item/Tab注册；真实ServerLifecycleHooks经官方Global Bus派发正确handler一次。Holder/get结果、两个物品及Tab完整ID/对象身份、延迟icon/display时机次数、Mod/Global独立身份、原生模组共存、Done/stop/exit0均通过。真实Bus监听器表读取是独立查询证据，不是生产实现；空实例无原生来源监听器对象，不能伪造保留记录。Tab服务端查询不等于创造栏客户端显示。

Global故障观察器不混入标准C/T：真实入口二次构造验证duplicate拒绝；私有invoke故障代理先完成一次真实原生注册、再注入异常，验证UNCERTAIN、一次保留listener和后续一次派发。没有将代理边界故障写成Neo内部扫描自行失败，也没有宣称覆盖完整公共caller链、安全沙箱或回滚。原4C-2独立诊断仍真实检查官方部分添加后的异常状态。

健康日志完整severityRecords与同轮空宿主相同。Settings/Yggdrasil网络ERROR实际非零，strictZeroErrors=false；没有声称零ERROR。早期一次随机世界MonsterRoomFeature ERROR导致旧候选组合Control实际FAIL，保留原日志，未归一化/忽略；最终候选在新目录全部通过。世界只用新副本，不打开或覆盖原世界。

## FnF边界及限制

[FnF只读报告](<Convergence-007C-4C-3 FnF完整只读计划报告.md>)：固定原发行SHA不变，正常Context + 组合候选相符，完整计划返回入口/Item/Tab三个经原SHA、clone、类型、引用闭包及ASM检查的输出。越过旧GLOBAL_EVENT_BUS_UNSUPPORTED，本轮无下一静态拒绝。未把FnF类定义到JVM、未实际发布到其游戏加载流程，更未启动完整FnF；独立夹具验证共享生产发布路径。FNF_RUNTIME=NOT RUN。

历史typed-v1、supplier-v1及4A PARTIAL保持原样。旧Context测试对未变校验器直接回归，实际生产FnF由新global-v1只读门禁验证；旧拒绝期望不与新JAR混用。其余严格断言不放宽。

仅当前受控Context链构造器、默认ServerStartedEvent marker、现有Typed Registry；不扩展纯Global但无Context链的入口形态、复杂handler、其他事件/priority/cancellation/post、构造ABI或架构。客户端、真实FnF构造/67注册/食用/燃烧/创造栏UI/持久化及未受影响完整历史游戏矩阵NOT RUN。旧4B/C1原生Forge在本轮未重跑（无compat安装，不受本轮生产改变影响），不冒充本轮新证据。

## 保全、归档和停止

再次核对16项原输入、C1的25/C2的26生产文件原Git字节、C1的1451/C2的2281份原件及旧ZIP SHA不变；374项保护通过。最终生产源/关键测试编译输入指纹与提交后对应文件核对，原FnF/冻结Probe/JAR均不重建。

保留首次原生探针Neo Context导入失败、观察器误用getMod失败、负例module关键字/克隆metadata构造错误和Reader拒绝、before checker漏stderr导致的FAIL；修正后新目录重跑，旧记录不覆盖。75b3784/9f1e6e...旧候选的健康结果不混入最终9fc9cde/a268935...。开发过程旧输入指纹保留，但不声称每个早期未提交harness源码版本都有独立快照。

最小归档 `out/convergence-007c4c3/evidence-export/DualForge-007C4C3-Evidence.zip`：**4086831 bytes**，1767个payload文件、23123298 bytes；SHA-256：

`6076da52a82b93640c5ab9c657e96b06fc77f1ad5699aba0e8a460589fa4c154`

Manifest SHA `8df30cbe6109d4dddaa7376eb9b1644c34abdf9080d04fe7cd2423c3c72e5179`。ZIP/CRC/无重复/软链接/路径穿越、清单/解压后逐文件SHA及原件不变PASS。包含命令/退出/原日志/结果/断言/指纹、必要项目制品及FnF三个目标的前后ClassFile（只读转换证据，不是可运行发行包）；排除世界、完整服务器配置、第三方运行库、完整FnF JAR、重复编译输出。不声称全部附件无裁剪副本。9份发布副本脱敏，原SHA与发布SHA分别记录，原始日志未改。

## 原始证据持久化与远端复核

**PERSISTED / VERIFIED**。用户明确授权本次具体ZIP与GitHub evidence/convergence-007c4c3目的地后，已上传并核对远端分支提交 `fbfbe5aaeaf4c65433fbc81e6c20f9bb900d0037`。

[原始证据ZIP（固定提交）](https://raw.githubusercontent.com/GFander/DualForge/fbfbe5aaeaf4c65433fbc81e6c20f9bb900d0037/DualForge-007C4C3-Evidence.zip)；归档名称、大小、ZIP/Manifest SHA及覆盖范围见上文，保持原归档字节。核验日期：2026-10-16T03:33:46.024435+00:00。

从公开URL独立下载至新的remote-independent目录，HTTP读取、ZIP SHA/CRC、无重复/软链接/路径穿越、Manifest/SHA256SUMS及解压后1767个文件逐项大小与SHA全部PASS；再次核对1766个原文件的大小与原SHA不变。9份脱敏发布副本的原始与发布SHA分别保留，原始日志未改；既有安全扫描PASS，不包含世界、第三方运行库、完整FnF JAR或实例配置。完整结构化远端复核收据嵌入本轮证据索引archive.remoteVerification；本地原路径和历史状态不删除。

本次仅持久化证据并更新文档，不修改生产源码或字节，不重新执行游戏回归；此前同最终JAR的受控功能结论保持，FnF游戏与客户端仍NOT RUN。重跑命令见tests/global_bus/README.md。不自动合并、不创建Release、不进入下一阶段。
