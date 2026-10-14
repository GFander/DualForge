# Convergence-006B-C macOS 客户端人工验收报告

报告日期：2026-10-14（Asia/Shanghai）。

**006B-C = PASS (USER-ATTESTED MANUAL ACCEPTANCE; SCREENSHOTS WAIVED)**。
这是macOS Apple Silicon上的受控Block + BlockItem人工验收，不是Codex自动化图形运行，也不是任意Forge模组兼容性证明。

## 来源、环境与制品

用户在本轮上传的正式验收任务中，亲自确认give、实际放置、六面贴图、手持、中英文名称，以及重进游戏后的方块和物品均正常，并明确本轮不提供截图。
声明来源为上传文本`file_000000004c0881f997bce05e6669550e`，9,261 bytes，SHA-256 `123b5c07242ca50f885768b44762f8aae0899fd399713ba75d98fa58fca24927`。此SHA标识验收声明文本，**不是客户端原始日志SHA**；该上传文件标识不作为持久化下载URL。
当前执行环境只取得该声明中的真实日志摘录，未独立取得006B-C完整Prism/latest.log/debug.log原件；不编造原件路径、哈希或重放记录。

|实际Mac环境（用户提供）|值|
|---|---|
|macOS|27.0|
|CPU|Apple M4 / arm64|
|Prism Launcher|11.1.1|
|Minecraft / NeoForge|1.21.1 / 21.1.256|
|Java|Microsoft OpenJDK 21.0.7|
|ModLauncher|11.0.5|
|实际安装mods|dualforge.jar、block-registry-probe.jar|
|实际对象|blockregistryprobe:ice_ether_block（Block及BlockItem）|

Block fixture SHA-256：`3768e817c101172d829aea1feeb05ff1e7600ba4c31da1ec4bac195ea0e50f0e`。
用户报告已在Mac核对该SHA；Codex本轮重新读取本地交付制品，SHA与用户声明一致。Codex没有取得Mac上的该JAR原文件，不能将本地核对说成独立哈希Mac文件。

兼容层服务端/交付包SHA-256：`159514dd5c88cc445117d6a4778db5e85cf0a496da2651f4faefbdbcacb3905b`。
本轮本地读取该制品匹配原SHA，但没有Mac实际文件或独立Mac测量凭据；**Mac兼容层实际文件SHA未独立核实**。验收结论基于用户实机声明和摘录，不声称Mac两JAR的独立密码学cohort核验全部完成。
上述Mac Java21.0.7与此前Linux编译/验证Java21.0.10+7-LTS分别记录，不用历史Linux工具链替换客户端版本。

## E-LOG：用户提供的真实客户端日志摘录

下列摘录来自用户正式任务，按其中原片段引用，不作为Codex读取完整原件或新运行的证据：

```text
[DUALFORGE-XFORM] TARGET modId=blockregistryprobe
profile=block-item-pair

[DUALFORGE-XFORM] BLOCK_REGISTRY_PROFILE
createHits=2
supplierHits=2
modBusAttachHits=2
holderGetHits=3
supplierImplementationHits=2

[DF006B] CONSTRUCTOR REACHED
[DF006B] ATTACH RETURNED
[DF006B] BLOCK SUPPLIER REACHED
[DF006B] BLOCKITEM SUPPLIER REACHED
```

集成服务器命令摘录：

```text
[16:08:13] [Server thread/INFO] [minecraft/MinecraftServer]:
[GFander: 已将1个[冰之以太块]给予GFander]
```

用户同时提供后续世界保存、集成服务器正常退出、进程exit0的日志事实；其完整原文未在本轮取得，不补写不存在的原始日志行。
这些事实支持发现/扫描/加载、已执行Block profile转换、双register挂接返回、两个Supplier执行、give授予物品及正常保存退出。profile计数是映射调用点统计，不能据此声称Mac实际Supplier调用次数或Holder.get次数的独立计数验证。
日志不能单独证明六面视觉、手持、语言切换或重进后的持久化；这些项目由用户人工声明支持。

## 验收项目与证据分级

[E-LOG]用户真实日志直接支持的事实（本轮获取形式为声明内摘录）；[E-MANUAL]用户本人实机检查；[WAIVED]本轮明确豁免；[NOT RUN]未执行或未确认的其他测试；[H]尚未独立验证的推测。

|验收项|状态|证据|
|---|---|---|
|Forge模组发现、扫描、加载|PASS|E-LOG|
|Block Profile ASM转换|PASS|E-LOG|
|双DeferredRegister MOD bus挂接|PASS|E-LOG|
|Block/BlockItem Supplier执行|PASS|E-LOG|
|give获取冰之以太块|PASS|E-LOG + E-MANUAL|
|实际放置到世界|PASS|E-MANUAL|
|六面原始方块纹理|PASS|E-MANUAL|
|BlockItem手持显示|PASS|E-MANUAL|
|中文“冰之以太块”|PASS|E-MANUAL|
|英文“Ice Ether Block”|PASS|E-MANUAL|
|重新进入后之前放置的方块仍存在|PASS|E-MANUAL|
|重新进入后物品仍存在|PASS|E-MANUAL|
|世界保存、集成服务器正常退出、exit0|PASS|E-LOG（用户提供的后续日志事实）|
|截图|WAIVED|用户明确豁免本轮，不是遗漏或待补|
|多个Forge模组同时安装|NOT RUN|本轮范围外|
|任意第三方Forge Block兼容|NOT RUN|本轮范围外|
|新版items-test两个物品的Mac图形验收|NOT RUN|本轮声明仅确认block-test，不继承此PASS|

用户明确以人工实机检测代替截图；该决定只适用于本轮006B-C。没有截图可供Codex独立视觉复核，不将E-MANUAL升级为自动化或独立图像验证；不会要求补交本轮已豁免截图，也不泛化为后续任务的默认豁免。

## 开关、旧ID及支持边界

最新Block源码以`Boolean.getBoolean("DF006B_REGISTER")`控制两次挂接，正确业务开关为`-DDF006B_REGISTER=true`；双Item组使用`DF006_REGISTER`，005组使用`DF005_REGISTER`。后两者不控制Block组；错误/缺失Block开关会走ATTACH SKIPPED，不能以转换profile统计推断已经注册。
本轮日志给出ATTACH RETURNED及两个Supplier执行。先前具体错误参数命令/原始配置没有取得，以上为现有源码/部署参数对照，**不编造某次错误运行的实际命令或原因链**。
原006B服务端回归输入实际ID为`blockregistryprobe:sample_block`；最新客户端输入为`blockregistryprobe:ice_ether_block`，不是同一个ID。历史sample记录不改写，本轮不实施旧世界ID迁移。

验收仅覆盖固定版本、当前受控普通Block与关联BlockItem、原单IEventBus构造器及已经验证的两Registry调用/Holder依赖。未扩大Forge API、Constructor ABI、事件、模块或加载架构，不证明所有模组或平台。
新版双Item组的实机结论仍单独NOT RUN；原006A-C旧cohort人工PASS也不作为其新版证明。

## 文档回写与停止

本轮起点/PR #13 HEAD为`7ca3478d6a36f45969599293764b9019658c142f`，fetch确认main `de4e54002c3b777fb392a4699fdbcbe2261965e0`，原任务分支`codex/convergence-006b-block-registration`。最终文档提交、远端HEAD及实际CI结果见PR/交付回复。
本轮只新增验收报告、当前状态索引及活跃导航；不修改生产Java/资源、fixture、PNG、世界、已发布测试包或历史报告/输出。历史CLIENT NOT RUN保留当时事实，由当前报告说明状态演进。
Linux Contract/C/T及旧功能回归均引用此前同JAR已执行的实际记录，**本轮不重建JAR、不启动JVM、不重新运行Linux C/T或图形客户端**。

006B服务端原始证据ZIP仍为**ARCHIVE_READY / NOT PERSISTED**，远端upload/readback NOT RUN；没有新远端URL。客户端人工PASS不完成原始证据归档。
PR #13保持Draft；不自动Ready/合并，不创建Release，不启动006C。最终结论：**PASS — macOS Apple Silicon，用户实机人工验收；截图经用户明确豁免**。
