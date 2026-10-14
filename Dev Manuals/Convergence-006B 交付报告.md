# Convergence-006B 交付报告

2026-10-14（Asia/Shanghai）。**006B-S受控PASS；002–006A旧功能回归PASS；006B-C NOT RUN。**
[E]本轮实际执行，[S]静态契约，[H]未验证假设，[D]工程决定。原始run时间戳使用执行器UTC。

## 基线、源码和修改范围

[E] fetch确认最新已验收main `de4e54002c3b777fb392a4699fdbcbe2261965e0`（PR #12已合并），独立worktree `/workspace/DualForge-006B`、分支 `codex/convergence-006b-block-registration`，起点干净。
生产实现提交 `41a431f18ed99e229eeb136ca1c3938b5e63a565`；先干净构建保存source inputs，提交后全部生产输入核对一致。
完整旧回归源码提交 `241d6ce366399b23f9dfa92b3dfb19bb9b625ab9`；仅本轮runner配置注释归一化后重跑006B C/T，生产始终同41a431f。
最终HEAD/PR见交付回复，后续只文档/索引，不改写历史或自动合并。

只新增ControlledForgeBlockProfile，并在DualForgeEntryTransformer的既有FML scan targets/复制转换通道增加受控分支。
原ControlledForgeItemProfile源码与class逐字节不变；只一个既有生产class（EntryTransformer）改变，新profile及8个内部类型加入，其他既有JAR成员原字节一致。
Locator/Reader/Parser/LanguageLoader/模块/SPI/Manifest/Event分支、Registry实际生命周期和原Constructor ABI未改。
测试增加原生fixture/观察driver/改名matrix/ASM/Contract/runner/cohort/README；CI只增加block_registry单测目录，不跳过任何旧检查。

## Gate 0：实际ABI与修改前首次失败

[E/S] [Gate0报告](Convergence-006B%20Gate0%20ABI与可行性报告.md)保存真实Forge编译、javap/ASM节点、descriptor、Bootstrap、来源和允许变换表。
Forge52.1.0三份原依赖SHA匹配；vanilla MC全class无Forge/Neo patch owner，Forgeclasspath无Neo/DualForge；driver独立用真实Neo server编译。
原生 `blockregistryprobe` 注册 `sample_block` 到两Registry；一个RegistryObject<Block>和一个RegistryObject<BlockItem>，同原IEventBus单参数ctor、两次local1 bus挂接；DF006B_REGISTER唯一业务开关。
Block supplier只marker+new Block(Properties.of)，BlockItem supplier只marker+new BlockItem(指定Block holder.get,new Item.Properties)。
改名matrix使用独立modId/package/类/字段/ID/accessor；真实捕获与altMetafactory样本用于拒绝，不当作支持。

[E] main全量干净JAR在一次真实发现/Reader/FML metadata后，首次失败于ControlledForgeItemProfile.onlyField→plan→validate→EntryTransformer.targets：
`005 unsupported Item profile: one DeferredRegister field`。尚无ctor、Supplier或dump，exit-before-Done。
原baseline OBSERVED及独立baseline-analysis保留，不把它写成新功能PASS。

[E/S] 最终fixture来源冻结于012113-e9fd2761；早期baseline使用011319-c33ae78f checkpoint。三份源Manifest EOF空白修正后重新编译，四JAR的每个entry（含class/metadata/Manifest）与旧checkpoint完全相同，仅打包时间/SHA不同，旧快照不覆盖。
Neo21.1.256官方sources和真实target javap交叉核对生命周期；Forge sources Maven403/镜像404如实保留，Forge ABI以锁定真实JAR/编译输出为准，没有声称取得该源码。

## Gate 1：最小受控实现

[S/D] 一个Block DR和一个Item DR、各自一个typed holder；所有字段按泛型角色和实际调用数据依赖判定，无具体modId/类/字段白名单。
restricted initializer追踪真实ForgeRegistries.BLOCKS/ITEMS、metadata所属namespace、create、已赋DR receiver、literal ID、标准零捕获Supplier和唯一holder消费。
每Registry独立拒绝重复ID，同一ID分别属于Block/Item合法；未知field/type/API/opcode/BSM/跨类实现均拒绝。
BlockItem supplier强制GETSTATIC同入口已声明Block holder→RegistryObject.get()Object→cast Block，并将该对象传入原BlockItem构造器。
只映射经Plan验证的两Registry字段、两个create/declaration/bus attach、四字段descriptor/signature和三个Holder.get（含supplier内部一个），不改变new Block/new BlockItem业务、条件、marker或BSM。
Block holder→DeferredHolder<Block,Block>；BlockItem holder→DeferredHolder<Item,BlockItem>。保留既有一参数ctorABI，新增的两个挂接属于本任务业务形态。

[S/E] targets从实际scan/modId取入口，同文件仅一个Forge@Mod/metadata owner；transform重新在original.accept副本上完整建Plan，全部成功后修改；dump错误也不返回部分节点。
官方GameData event失败时聚合错误并revertToVanilla是源码静态事实，未扩展为任意跨Registry事务支持。静态输入拒绝和ClassNode原子性与运行时注册回滚是不同边界。

## Gate 2：Contract PASS

[E] 两正例（原生入口、独立改名matrix）通过真实Reader/FML扫描和ASM verify；BSM/handle不动，各instruction layout不增删；只逆向恢复允许的字段/API/ctor点，整个canonical class字节等于原输入。
Block supplier业务原字节不变；BlockItem仅holder field descriptor/get owner改变，指定Block对象构造语义不变。
未归属同形类/native metadata路径不接管，保存before/after。

19项拒绝/失败：错误Block/Item Registry、其他Registry、外部namespace、重复Block/Item ID、未知BlockHolder、错误Holder类型/Supplier返回、真实捕获、alt/未知BSM、额外Forge API、错误/漏MODbus、晚期第二getter错误、错误入口owner、实际额外扫描入口、已验证编辑后的dump I/O。
每项保存具体reason，before/after原ClassNode不污染；没有吞异常。正常服务器日志不包含contract故意注入的dump失败。

## Gate 3：真实C/T PASS

[E] 最终新JAR（66626 bytes）：
`159514dd5c88cc445117d6a4778db5e85cf0a496da2651f4faefbdbcacb3905b`

|项|最终C（013651-1a7524a2）|最终T（013730-aa551b82）|
|---|---|---|
|真实Block/Item Registry contains/getOptional|均false|均true|
|Block/BlockItem supplier次数|0/0|1/1|
|原Forge入口构造/两个bus挂接|构造一次、跳过两挂接|构造一次、两挂接返回|
|Item phase时Block已注册/holder bound|不调用|真实query+accessor同对象，sequence2|
|BlockItem.getBlock/两个holder accessor/真实Registry对象|不绑定get|引用相同，正确keys及Registry，holder不同|
|两个Supplier官方RegisterEvent/DeferredRegister/FML栈|不发生|两者均实际捕获|
|注入bus/实际FML container bus/区别global|PASS|PASS|
|Done→正常stop→exit0、严格无ERROR/FATAL/dumpFAIL/stderr/crash|PASS|PASS|

两组compat/fixture/driver/JDK/世界配置/兼容开关完全一致，只DF006B_REGISTER业务变化；隔离目录、relay地址、VM对象地址自然不同。
Driver只查询、观察原Supplier栈和对象关联，不创建或直接注册Block/BlockItem，不手工post或补偿注册失败。
两个runtime dump逐canonical byte与同新JAR离线Contract转换对照PASS。

## Gate 4：最终同SHA旧回归 PASS

[E] 原ForgeProbe严格smoke、003a发现/边界、003b身份/范围、004 Event Contract/C/T、005单Item Contract/C/T/范围/原子性/assets、metadata六边界与C/D/E/F、006A多Item Contract/C/T/实际runtime bytecode均重新执行PASS。
D/F仍用实际缺失mandatory或minecraft版本范围等FML具体拒绝原因；未放宽断言、不靠exitCode/缺Done判负例。
原004/005整个历史任务summary可能PARTIAL（未重建其fixture/pre-change阶段）；本任务所需实际回归全部PASS，递归检查每个子结果compat SHA。
Block汇总复用006A严格cohort规则，旧回归和006A三阶段亦绑定同159514…3905b，未借用旧JAR PASS。

[E] 当前Python72项PASS：常规44（含新增Block cohort7）+当前/Legacy Mac Linux mock28。后28使用各自原已验收输入，不冒充新JAR Mac或服务端回归。
23份活跃zsh语法、命名374保护、diff --check及新增文档链接核验PASS；CI在最终HEAD实际执行，见PR checks/交付回复，不预填PASS。
实际工具：Microsoft21.0.10+7-LTS、Python3.12.14、zsh5.9、git2.52.0，Linux x86_64；MC1.21.1/Neo21.1.256/FML4.0.45/ModLauncher11.0.5不升级。

## 保全和失败如实记录

[E] 486个不可变起点tracked文件、433个原外部输入、10份实际fixture编译源/资源SHA不变；原世界/配置/历史结果/旧Item profile/SPI/Manifest/六节Git规范未改。
503次实际JVM命令含失败/版本/编译/javap/服务器，全部串行；结束无Java进程、25565可绑定释放。

- fixture初次纯度检查错误地把driver查询cast判为new：保留原日志，改为实际NEW检查后干净重编译。
- 首轮完整旧wrapper FAIL：relative runtime classpath在子cwd失效；保留FAIL，改传绝对同安装路径，完整新目录重跑PASS，未改旧runner或断言。
- 初次Block汇总FAIL：runner错误保留server.properties生成时间注释；保留原两组和aggregate-rejection，按旧006A规则只比较实际配置，实际重跑两服务器后同cohort PASS，生产不变。

## 客户端、证据与停止

[D] 006B-C NOT RUN。BlockState、Block/Item Model、Texture、语言资源未准备/实测；世界放置、背包/手持显示未实机验证，不继承006A-C。
仅普通Block+精确关联BlockItem、已验证supplier体/typed字段/标准零捕获Bootstrap/原单bus参数构造及受控双挂接；不支持任意业务、捕获/其他Bootstrap/Registry/一般ForgeAPI、新Event/ABI/Capability/Fabric/006C。

原始源码/依赖/资源/class/JAR/commands/stdout/stderr/debug/meta/exit/断言/前后字节/canonical/失败与最终重跑在 [证据索引](Convergence-006B%20证据索引.json)逐路径/SHA定位。
### 原始附件归档与持久化状态

[E] 2,958项索引原文件（21,075,360 bytes）全部存在、大小/SHA一致，无重复；归档后再核原SHA不变。ZIP CRC、安全路径/无软链接、fresh解压后Manifest逐文件大小/SHA及SHA256SUMS均PASS。
公开副本脱敏检查PASS：仅副本处理工作区路径/IP/凭据参数等，Manifest同时记录原SHA、发布SHA和转换关系；1,206份文本脱敏，452份自有controlled class为转换前后/runtime dump/拒绝原子性证据，按必要性及来源逐项记录，原字节保持一致。
不收录世界、运行库、第三方依赖/源码、compat/fixture/driver JAR和重复server副本；包括这些输入的原指纹与实际断言。归档是全部rawEvidence条目的选择集，不是整个out/的副本。预导出索引快照明确标记；之后仅添加导出元数据，不改原测试记录。

- 本地归档：`out/convergence-006b/evidence-export/20261014-032304/dualforge-006b-runtime-evidence.zip`
- ZIP：4,961,881 bytes，2,965文件；SHA-256 `8829de2cbbad04701aec42aeb5483fc6b5bd42e19a110e3229174a4e02432a11`。
- Manifest SHA-256：`6f0d07ea871ccd3d6fcc18aa94c7706d542e2d73812ba044e9e3393702f32324`；独立状态及清单路径见证据索引archive。
- 状态：**ARCHIVE_READY / NOT PERSISTED**，remote upload/readback均**NOT RUN**，无远端URL。当前无可调用ZIP附件上传接口或已授权GitHub网页会话，未创建Release/另用第三方存储。原始路径不是持久下载链接。
- 下一步：登录GitHub，在本轮PR会话评论中拖入ZIP，等待附件URL后提交；取得真实URL后需重新下载并按ZIP SHA和内置verify_archive.py核验，才能标记PERSISTED / VERIFIED。此处未声称已上传。

本轮未创建Release、未合并、不启动006C。原始运行附件和世界不入Git。
