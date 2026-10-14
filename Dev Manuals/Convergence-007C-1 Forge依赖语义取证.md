# Convergence-007C-1 Forge依赖语义取证

`DEPENDENCY_SEMANTICS = PARTIAL`。新Parser/Admission推进到真实FML拒绝：`Mod ID: 'forge', Requested by: 'fnf', Expected range: '[0,)', Actual version: '[MISSING]'`。本轮不添加任何依赖豁免、假forge模组或宿主能力架构。

## 本轮动态证据

只安装本轮新DualForge与冻结原FnF、使用标准 `sh run.sh nogui`。实际/proc JVM为 `java @user_jvm_args.txt @libraries/net/neoforged/neoforge/21.1.256/unix_args.txt nogui`。

真实ModFileInfo有fnf 1.3-1.21.x、原description和两项依赖；发现一次，官方mods locator随后跳过已发现路径。ModSorter明确报required forge缺失，ModLoader预加载报 `Mod fnf requires forge 0 or above`。FnF没有进入入口加载、转换、构造或Item注册。宿主返回exit0却生成FML crash report且无Done；stock runner如实记录生命周期FAIL，不能以退出码判断兼容成功。stderr为空和输入SHA未变并不消除这项拒绝。

诊断SPI额外运行记录FML已为FnF选择javafml/9999，仍发生相同依赖拒绝；它没有提供forge或修改依赖。最终双JAR实际运行没有诊断SPI。原默认FML配置 `dependencyOverrides = {}`。

## 官方FML 4.0.45静态契约

取证使用当前官方loader源码及本轮实际运行artifact SHA，源片段和原始路径见索引。

- `ModFileInfo`从IConfigurable构造语言spec、mod列表和license；`ModInfo.ModVersion`持有id/type/range/ordering/side/owner，不把loader版本当作mod版本。
- `ModFile.identifyLanguage`调用当前LanguageProviderLoader.findLanguage并保存loader。宽range仅允许版本匹配，不满足mod依赖。
- `ModSorter.buildUniqueList`以实际IModInfo.modId建表；`verifyDependencyVersions`从实际mods收集版本，按当前side筛选REQUIRED以及已安装OPTIONAL。required缺失/越界拒绝；optional不存在不拒绝，已安装optional越界拒绝。
- `VersionSupportMatrix`在MC1.21.1仅提供minecraft/NeoForge历史兼容版本，不提供forge别名或Forge API能力保证。
- `FML-System-Mods`仅标记实际存在的系统mod，不创建缺失mod；不是虚拟提供机制。FMLConfig dependencyOverrides可以强制移除约束或排序，不能证明等价能力，禁止借此绕过本轮原required声明。
- 依赖失败后的LoadingModList只保留系统mods并带error。当前Transformer已有hasErrors早返回，未改这条流程。

## 判定与停止

声明 `forge [0,)`不等于调用所有Forge API，但原required语义必须保留。把forge改名为neoforge会混淆两个版本体系；删除或虚构forge会把未知API支持变为无条件满足。当前没有已验证的宿主提供机制能准确声明DualForge能力并保证第三方需求，所以不实现替代语义。

未来若要设计最小宿主能力契约，必须单独界定提供能力、生命周期、版本/冲突语义、第三方需求校验和明确失败边界；本轮只提交取证，不开始该架构。

冻结007C只读ABI报告所示无参构造器、跨类注册和SRG等潜在后续边界仍是静态事实；**本轮实际首失败只有依赖阶段，不能称为已复现构造器失败**。原FnF原生Forge基线仍PARTIAL，客户端没有新增验收。
