# Convergence-007C-2 FML依赖验证链取证

Gate 0/1在生产零修改时完成。基线main `acf363b84ee1efbce15c98c8e77a3dab693251ca`已合并PR #19，旧HEAD `fdbb206998e2033a94209cb97093434f16d020f9`。本轮基线干净构建76,102字节，SHA `a2bd5ff5a56d0a0690d9d627bb0a4692d496aeaa86410a22404b956c3991d79c`；全部成员与007C-1最终JAR一致（ZIP封装时间不同）。继承并逐SHA核对007C-1全部2,554个原始文件；原FnF CRC/完整成员SHA不变。原生Forge基线仍PARTIAL，不重跑或改写其67 Item/67 Holder/58 Food/8 Fuel及代表熔炉事实。

## 本轮直接证据

`out/convergence-007c2/gate0/N0,N1,N2`串行标准启动：N0与N1正常Done/stop/exit0，错误种类/计数一致；N2真实FML仍报fnf缺失required forge `[0,)`，无FnF loadMod调用。raw生命周期FAIL、宿主exit0和crash原样保留。所有实际/proc JVM保持官方argfile，无附加业务参数。

官方实际安装为FML4.0.45、ModLauncher11.0.5；运行JAR SHA和对应官方source archive SHA见preflight，完整`javap -p -s -c`输出保存于`preflight/bytecode/`，没有拿上游最新main替代。测试专用Reader SPI实际输出：VersionInfo(NeoForge21.1.256,FML4.0.45,MC1.21.1,NeoForm20240808.144430)、DEDICATED_SERVER、Java21。它只观察、不接管、不供给依赖，标记DIAGNOSTIC_ONLY；额外SPI不属于最终双JAR环境。

## 调用链与时序

```text
FMLLoader.setupLaunchHandler: 保存VersionInfo/dist
  → beginModScan → ModDiscoverer → Locator/Reader
  → IModFile.create / ModFile.identifyMods
  → ForgeModFileInfoParser.build → ModFileInfo → ModInfo.ModVersion
  → ModValidator.stage1Validation: identifyMods（可能再次解析）
FMLLoader.completeScan
  → new LanguageProviderLoader
  → ModValidator.stage2Validation → validateLanguages → ModFile.identifyLanguage
  → ModSorter.sort → buildUniqueList → verifyDependencyVersions
  → 无错误才sort依赖图；LoadingModList → background scan
  → 返回GAME resources、模块装配 → transformer.targets → entry/container/constructor
```

`ModFileInfo`读取config的language spec和mods列表，`ModInfo`从同owner的`dependencies.<modId>`创建每项ModVersion。现Parser保留mandatory布尔并添加required/optional type；range/ordering/side原值保留。源TOML、MapConfigurable、真实FML列表是不同表示，不能混称“原样交给FML”。

关键字节码描述符：

| 方法 | descriptor |
|---|---|
| ModFile.identifyLanguage | `()V` |
| ModSorter.sort | `(Ljava/util/List;Ljava/util/List;Ljava/util/List;)Lnet/neoforged/fml/loading/LoadingModList;` |
| ModSorter.verifyDependencyVersions | `()Lnet/neoforged/fml/loading/ModSorter$DependencyResolutionResult;` |
| IModInfo.getDependencies | `()Ljava/util/List;` |
| ModVersion.getVersionRange | `()Lorg/apache/maven/artifact/versioning/VersionRange;` |
| VersionRange.containsVersion | `(Lorg/apache/maven/artifact/versioning/ArtifactVersion;)Z` |
| LanguageProviderLoader.findLanguage | `(Lnet/neoforged/fml/loading/moddiscovery/ModFile;Ljava/lang/String;Lorg/apache/maven/artifact/versioning/VersionRange;)Lnet/neoforged/neoforgespi/language/IModLanguageLoader;` |

## ID、版本和真实约束

ModSorter从传入ModFile列表的IModInfo构造唯一modId及实际getVersion表。版本来自各自metadata的version/文件版本替换（DefaultArtifactVersion），不是provider版本。required缺失/越界报错；optional未安装忽略、已安装且越界报错；按DependencySide过滤版本要求。错误对象带owner/id/range/实际version，生成missingdependency或missingdependency.optional等原生issue。

排序另从所有依赖调用addDependency，并不先过滤side/type；存在实际目标才按BEFORE/AFTER建边，不存在无边。拓扑排序检测cycle，LoadingModList保存真实前驱。依赖失败只保留系统mods及error，不给FnF进入GAME资源。平台与普通模组依赖在官方源码中都走同一id/version表，没有特殊forge依赖通道。

三个版本分开：Forge来源平台参考ABI **52.1.0**，实际NeoForge平台 **21.1.256**，语言provider **4.0.45/9999**。9999只用于语言range选择，不能充当Forge平台版本。52.1.0是拟支持来源ABI契约，不声称安装了Forge运行时或满足所有API。

## 扩展点和不可扩展点

每个ModInfo只有一个id/version。一个ModFile可列多个mod，但那是多个真实mod身份并参与全局解析/容器生命周期，不是可按消费者限定的provides别名。未找到独立虚拟平台/能力提供者SPI。

CandidateLocator/Reader可经官方pipeline提供ModFile和元数据，Parser到ModFileInfo之间可做独立验证后投影；不必改ModSorter。VersionSupportMatrix只含MC/NeoForge特定历史版本替代，无forge项、无公开注册API；反射改表不是正式方案。`0.0NONE`开发版本的通配捷径也不能用于Forge范围验证。FML-System-Mods只标记已存在身份，不创建forge。dependencyOverrides可删除约束/加排序，不是兼容语义，本轮正式环境禁用。

隔离`ProjectionFeasibilityTest`使用原FnF元数据和实际ModSorter：原模型缺forge拒绝；先验证52.1.0/NONE/BOTH，再在测试内存副本投影，实际Sorter接受fnf且不存在forge记录，Minecraft要求保留、原模型仍有两依赖。测试使用显式合成的Minecraft/NeoForge metadata记录，不装配GAME、不证明用户环境已通过，全部标DIAGNOSTIC_ONLY。真实生产投影仍需独立Contract、标准动态和历史回归后才能验收。
