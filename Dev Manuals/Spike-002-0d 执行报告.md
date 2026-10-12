# Spike-002-0d 执行报告

**任务**：G-1 前置修复（兼容 provider 对非 Forge 容器的委托行为）+ K1 对照（Run C / Run T）
**依据**：《Spike_002-0d.md v1.0 冻结版》（顾问方 7 处小修已落实）+《Spike-002-0c 裁判裁定修订（反向意见处理）》
**日期**：2026-10-08
**执行方**：Codex（总指挥应急代行——治理偏差见 §八-1）
**自评结果**：VALID-002-0d-1（待裁判裁定）

---

## 一、实验环境

- Minecraft 1.21.1 / NeoForge 21.1.256 / FancyModLoader 4.0.45 / ModLauncher 11.0.5 / Java 21（Microsoft OpenJDK 21.0.10+7-LTS，ms-21.0.10）
- 冻结引用哈希复测（全部匹配）：

  | 引用 | SHA-256 | 字节数 |
  |---|---|---|
  | 冻结 v1.1 参照 jar | `E27BB9659303DDEDF2EDE02D10B49CAFC586549C0EDA003A0165DF525F97A436` | 20316 B / 25 条 |
  | forgeprobe.jar | `A93A823C988BFD6A8F6EA132D189D9DE732B0E5951F4BF74980D51BB63374E22` | 1772 B |
  | probe mods.toml（3 份副本一致） | `B32CF69B88FBE053C2580FC739B30FEDC43D96984C72FF78CD6725C7F3A1CE6E` | — |
  | loader-4.0.45.jar | `8390398E67DF23E72D85ED809685BE2752C8EC545B7CBB6AD4B70FDC81E4DCD4` | 505696 B |

- JVM 参数：`-Dforgecompat.2a.rewrite=true`（harness 固定 `-Xmx3G -Xms1G`）
- Label：`spike002-0d-control`（Run C）/ `spike002-0d-treatment`（Run T）
- meta.json（两轮）：
  - Run C：`EXITED_EARLY` / exitCode 0 / 12.1s / doneLine `null`
  - Run T：`DONE_STOPPED_GRACEFULLY` / exitCode 0 / 24.7s / doneLine 原文：
    `[0810月2026 00:03:59.601] [Server thread/INFO] [net.minecraft.server.dedicated.DedicatedServer/]: Done (0.340s)! For help, type "help"`（含行尾 `\r`）

## 二、源码恢复与归档（2D-a）

- 恢复源清单（14 文件；= `archive\src-recovered\_sha256.txt` 原文）：

```
main/java/com/example/forgecompat/ForgeCompatLanguageLoader.java  7CC41E6A71FA377859397E08A66226F9F8D7F24E6D923342D6F97C7B92A5B873
main/java/com/example/forgecompat/ForgeCompatTransformationService.java  C3F859A395AEAE1AABF01558ACAA0B1E763F8BE2011AE910B359F70C83A47E33
main/java/com/example/forgecompat/ForgeJarMetadata.java  6D1E8D4DDC1316972237D9E98974FB659A9CBB8B7779D6518C196B4110511286
main/java/com/example/forgecompat/ForgeModFileInfo.java  830D22D0651F853E4AFC1F5E3475983ADA12873621E5B39F9FBBB7210631B537
main/java/com/example/forgecompat/ForgeModFileInfoParser.java  FE6BBCD18C7504CE794157CCCB2DC51313DFEF8476BFEEA8FFA1DC4041908825
main/java/com/example/forgecompat/ForgeModLocator.java  3B010724C34F4537D5F759694A0454302882316E0223F33C9801375AE8A19472
main/java/com/example/forgecompat/ForgeModReader.java  EEAD3529EEF70C865B31CFE0DF4993EA5E65EE98AD5BEE986AB80E1A5A1E8925
main/java/com/example/forgecompat/ForgeProbeConstructorTransformer.java  CAB00F3AC9CD2CE9EBFAF6BFDA289D616C0B85BF733B58B76A9EFA0058511B42
main/java/net/minecraftforge/eventbus/api/IEventBus.java  5AC37276F9B441C3B2A18C5F8D394C39571A6A500E03F78C60D34661E39E5982
main/resources/META-INF/MANIFEST.MF  D78F95581EC70C1B9265D81CDC2251CB8E2F45E9D05160927FD35BBA20F26E93
main/resources/META-INF/services/cpw.mods.modlauncher.api.ITransformationService  7BE1179561BE12F8C738A9FEB87D2BF4CD85227EF495B42BC37D68268E03DECF
main/resources/META-INF/services/net.neoforged.neoforgespi.language.IModLanguageLoader  65968D8B635730F9CE29B77401EC97310D712574A3C266EBE7F7F23D50473941
main/resources/META-INF/services/net.neoforged.neoforgespi.locating.IModFileCandidateLocator  2E232A7C8C1ED34AC4989866DEC84F6488C1AACE5DCD26CCC9A9A7A890B065A3
main/resources/META-INF/services/net.neoforged.neoforgespi.locating.IModFileReader  394A0017EA04610A7A8D6F7E6410797ED13DF52C3F4885CA1DF0136BCEC81DD7
```

- 修复态源码（= `archive\src-fixed\_sha256.txt`）：14 文件清单同上，**唯一不同**：
  - `ForgeCompatLanguageLoader.java`：`7CC41E6A…` → `D7BA951C31FCAD4CFA86E2681B0EA21880250E5A4A2C636129C122F434F7C0A1`；其余 13 项逐项相同 → **源码级单变量封闭**
- services 逐字节对照结论：4 个 service 文件与冻结 jar 内文本逐字节一致（含于 16/16 一致，硬门 2 通过）
- MANIFEST 对照结论：与冻结 jar 的 MANIFEST 逐字节一致；含 `Manifest-Version` / `Automatic-Module-Name: forgecompat` / `FMLModType: LIBRARY` / `Created-By: 21.0.10 (Microsoft)`；**无 `Implementation-Version`**（硬门 3 通过）
- 逐类 javap 对照结论：11 个 class 与冻结 jar **逐字节一致**（强于 javap 要求）→ 语义差异清单 = **空**；抽检 `ForgeCompatLanguageLoader` javap 对照（`2Da-javap-frozen-loader.txt` vs `2Da-javap-control-loader.txt`，各 4655 B）完全一致
- 硬门 1–4：全部通过（最强形态：16/16 内容文件逐字节一致；整 jar SHA 差异仅 zip 元数据）；硬门 5 = Run C 保真（见 §四）
- 归档路径：
  - 恢复态：`D:\DualForge\spike001a\compat-002-0d\archive\src-recovered\`（14 文件 + `_sha256.txt`）
  - 修复态：`D:\DualForge\spike001a\compat-002-0d\archive\src-fixed\`（14 文件 + `_sha256.txt`）
  - 原 `compat\` 目录全程未动（其基线备份 `archive\src-001b1\` 保持只读）

## 三、构建记录（2D-b / 2D-d）

- 工具：`ms-21.0.10` 的 `javac` / `jar`；classpath = `compat\build\cp.txt` + `asm-9.10.1.jar` + `asm-tree-9.10.1.jar`
- control jar：`D:\DualForge\spike001a\compat-002-0d\build-control\forgecompat-002-0d-control.jar`
  - SHA-256 `62A15F5734F09508D7BE033E828604671DBB6AD0E21747321CF85BC0E8289613` / 20316 B / 25 条目（清单：`2Da-gate1-control-entries.txt`）
- treatment jar：`D:\DualForge\spike001a\compat-002-0d\build-treatment\forgecompat-002-0d.jar`
  - SHA-256 `4CD8195F7AA03B8B595F646CDEAC16454D03D5E3862B5E2F388C820CF2FA2FEF` / 21180 B / 25 条目
  - 条目名与顺序与 control 完全一致（`jar --list` 全量比对）
  - 编译命令（逐字模板，含 classpath / 退出码 / 工具版本）：`2Dd-treatment-build-record.txt`
    - `javac --release 21 -encoding UTF-8 -classpath <cp.txt;asm-9.10.1;asm-tree-9.10.1> -d build-treatment\classes <9 java sources, sorted>`（exit 0）
    - `jar --create --file … --manifest src\main\resources\META-INF\MANIFEST.MF -C build-treatment\jar-root .`（exit 0）
  - 注：control 编译/打包流程与本命令同构（同一恢复源码树、同一工具、同一 classpath）；control 的逐字命令行未单独落盘——记录完整性缺口，见 §八-2
- javap 对照（冻结 vs control）：16/16 内容文件（11 class + 4 service + MANIFEST）逐字节一致 → 语义差异 = 空；整 jar SHA 差异 = zip 元数据（时间戳）
- javap 对照（control vs treatment）：16 内容文件 **15/16 逐字节一致**；唯一差异 = `ForgeCompatLanguageLoader.class`
  - control `0F5FF48F5492035F868C40567F1C9FBAB0F01AC0786EE8A6C52EA6CBBA60E320`
  - treatment `5DE730322499B581705B219C9792585EA7BFE5165B778775BE1ADE17FE826A10`
  - 逐条差异（`javap -p -c -constants` + 逐字节）：
    - 成员/签名：无增删；`NAME` / `VERSION` / 类签名不变
    - `<init>` / `name()` / `version()`：逐字节等价
    - `<clinit>`：等价（仅常量池 `ldc #98` → `ldc #173` 重编号）
    - `loadMod` offsets 0–96：逐字节等价（注解收集循环 + 既有 `loadMod called …` 日志 + invokedynamic #0）
    - 新增 99–229：`List.isEmpty()` 判定 → `FMLLoader.getLanguageLoadingProvider().applyForEach(λ0).filter(λ1).filter(λ2).filter(λ3).toList()` → `size!=1` 抛 `IllegalStateException` → `get(0)` + checkcast → `delegate ->` 日志 → `builtin.loadMod(info, scanData, layer)` → `areturn`
    - 原尾部 230–242 = control 99–111 的等价字节码（偏移 +131；常量池 `#93/#95`→`#154/#156`）
    - 新增 4 个 synthetic：`lambda$loadMod$0`（恒等）/ `$1`（`l != this`）/ `$2`（`"javafml".equals(l.name())`）/ `$3`（类名 equals）——与 §五 5.2 逐字对应
    - 无其它差异；全部常量池位移由新增常量解释
  - 详表：`2Dd-control-vs-treatment-compare.txt`

## 四、Run C（control）

关键签名对照表（§四 2D-c 表 1–6；逐项原文）：

| # | 期望 | 实测原文 | 对照 002-0c |
|---|---|---|---|
| 1 | `loadMod called for neoforge entrypoints=[]` | stdout L69: `[FORGECOMPAT-LANG] loadMod called for neoforge entrypoints=[]`（恰好 1 行；`loadMod called for forgeprobe` = 1 行 L64） | 一致（"至少 1 行"满足） |
| 2 | marker + BUS | L70–L73 原文块：<br>`=================================`<br>`FORGE PROBE CONSTRUCTOR REACHED`<br>`BUS = net.neoforged.bus.EventBus@e78b4ee`<br>`=================================` | 命中；BUS 地址不同（允许） |
| 3 | 无 `NeoForge mod loading` + FATAL 同链 | `NeoForge mod loading` = 0 行；L74: `[23:59:56] [main/FATAL] [ne.ne.fm.ModLoader/LOADING]: Failed to wait for future Registry initialization, 1 errors found` | 一致 |
| 4 | crash-report 同链 + 11 项注册表 | `at TRANSFORMER/neoforge@21.1.256/net.neoforged.neoforge.registries.RegistryManager.postNewRegistryEvent(RegistryManager.java:94)` + `at TRANSFORMER/neoforge@21.1.256/net.neoforged.neoforge.internal.CommonModLoader.lambda$begin$0(CommonModLoader.java:52)` + 11 项清单（fluid_type / ingredient_serializer / biome_modifier_serializers / entity_data_serializers / holder_set_type / global_loot_modifier_serializers / structure_modifier_serializers / fluid_ingredient_type / attachment_types / condition_codecs / synced_attachment_types） | 一致 |
| 5 | meta | `EXITED_EARLY` / exitCode 0 / 12.1s / doneLine null | 一致 |
| 6 | REWRITE + dump | L65 `offered: com/example/forgeprobe/ForgeProbe`；L66 `REWRITE hits=1 old=(Lnet/minecraftforge/eventbus/api/IEventBus;)V new=(Lnet/neoforged/bus/api/IEventBus;)V`；L67 `post-check ctorCount=1 oldAbsent=true newPresent=true`；L68 `dump OK … bytes=1162`；dump 文件 SHA-256 = `5E2F78DB2B0909D7368A185F618C3D080C00B732279B105CD5B8010FD66D9295` / 1162 B | 一致 |

产物清单：`D:\DualForge\spike000\logs\spike002-0d-control\`

- `meta.json`（1262 B）/ `spike002-0d-control.log`（7502 B，latest）/ `.debug.log`（76150 B）/ `.stdout.log`（16850 B）/ `.stderr.log`（0 B）/ `.mods-list.txt`（48 B）
- `crash-reports\crash-2026-10-07_23.59.56-fml.txt`（5689 B，原文摘录见上表 #4）

**硬门 5（Run C 复现 G-1 签名）= 通过**（表 1–6 逐项一致；仅时间戳 / BUS 地址不同）。

## 五、Run T（treatment）

> 说明：以下 1–7 中 System.out 行存在于 `spike002-0d-treatment.stdout.log`（**不**写入 latest.log）；log4j 行同时存在于 stdout 与 latest.log。

### 日志序列（1–7，逐条原文，不得摘要）

1. stdout L69：`[FORGECOMPAT-LANG] loadMod called for neoforge entrypoints=[]`（恰好 1 行；`forgeprobe` 1 行 L64；两轮日志均已全量记录）
2. stdout L70：`[FORGECOMPAT-LANG] delegate -> builtin javafml for neoforge`（§五 5.3 逐字；恰好 1 行，无递归/重复委托）
3. stdout L75（= latest.log L22 原文）：`[0810月2026 00:03:57.185] [modloading-worker-0/INFO] [net.neoforged.neoforge.common.NeoForgeMod/NEOFORGE-MOD]: NeoForge mod loading, version 21.1.256, for MC 1.21.1`（`NeoForgeMod` 被构造 = G-1 清零直接证据）
4. **`[E-1]`**：`Failed to wait for future Registry initialization` **完全缺席**——stdout = 0 行 / latest.log = 0 行 / debug.log = 0 行；**`[E-2]`** 越过信号（原文行）：
   - stdout L81 / latest.log：`[0810月2026 00:03:59.046] [Server thread/INFO] [net.minecraft.server.dedicated.DedicatedServer/]: Starting minecraft server version 1.21.1`
   - stdout L95 / latest.log L42：`[0810月2026 00:03:59.601] [Server thread/INFO] [net.minecraft.server.dedicated.DedicatedServer/]: Done (0.340s)! For help, type "help"`
   - stdout L97：`[0810月2026 00:04:10.940] [Server thread/INFO] [net.minecraft.server.MinecraftServer/]: Stopping the server`
5. 新终点：**Done**（无新断层；无新 stack trace；harness 于 Done 后 10s 注入 `stop`，`DONE_STOPPED_GRACEFULLY` / exit 0 / 24.7s）
6. ForgeProbe 证据（原文）：
   - L64 `[FORGECOMPAT-LANG] loadMod called for forgeprobe entrypoints=[com.example.forgeprobe.ForgeProbe]`
   - L65 `[FORGECOMPAT-XFORM] offered: com/example/forgeprobe/ForgeProbe`
   - L66 `[FORGECOMPAT-XFORM] REWRITE hits=1 old=(Lnet/minecraftforge/eventbus/api/IEventBus;)V new=(Lnet/neoforged/bus/api/IEventBus;)V`
   - L67 `[FORGECOMPAT-XFORM] post-check ctorCount=1 oldAbsent=true newPresent=true`
   - L68 `[FORGECOMPAT-XFORM] dump OK path=D:\DualForge\spike000\logs\spike002-0b-2a-v1.1\ForgeProbe.rewritten.class bytes=1162`
   - L71–L74（原文块，逐字）：

     ```
     =================================
     BUS = net.neoforged.bus.EventBus@7e14653b
     =================================
     FORGE PROBE CONSTRUCTOR REACHED
     ```

     - 两行均命中；BUS 地址与 Run C 不同（允许）。顺序注记：probe 源码打印顺序为 `===== / marker / BUS / =====`（Run C 捕获即此顺序）；Run T 捕获中 marker 行位于 BUS 块之后——1 行级捕获乱序，如实记录（§八-4，不影响判据：判据仅要求 marker + BUS 命中）。
   - dump 文件校验：`5E2F78DB2B0909D7368A185F618C3D080C00B732279B105CD5B8010FD66D9295` / 1162 B（与冻结值一致）
7. meta / crash-report / mods-list：
   - `meta.json`：`DONE_STOPPED_GRACEFULLY` / exitCode 0 / 24.7s / doneLine = 原文（见 §一）
   - crash-report：**无**（run 目录无 `crash-reports\`；服务端 `crash-reports` = 0 文件）
   - `mods-list.txt`：`forgecompat-002-0d.jar` + `forgeprobe.jar`

### 观测项 10（§六-10）：neoforge 容器的 `IModInfo.getLoader()` 实际指向

**实际结论：= `com.example.forgecompat.ForgeCompatLanguageLoader`（compat 实例，version 9999）**，与《审阅意见（顾问方）》补 2 的预期（builtin `FMLJavaModLanguageProvider`）**不符**。[F] + [E] 链如下（完整记录：`2De-observation-10-getLoader-mechanism.txt`）：

1. `ModInfo.getLoader()` = `owningFile.getFile().getLoaders().getFirst()`（`net.neoforged.fml.loading.moddiscovery.ModInfo` 字节码）[F]
2. `ModFile.identifyLanguage()`：`modFileInfo.requiredLanguageLoaders().stream().map(lambda$identifyLanguage$10).toList()` → `putfield loaders` [F]
3. `ModFile.lambda$identifyLanguage$10(LanguageSpec)`：`FMLLoader.getLanguageLoadingProvider().findLanguage(this, languageName, acceptedVersions)` [F]
4. `LanguageProviderLoader.findLanguage`：`languageProviderMap.get(languageName)` → 版本矩阵通过 → `wrapper.modLanguageProvider()` 返回 [F]
5. `LanguageProviderLoader.<init>`：`languageProviders = ServiceLoaderUtil.loadServices(...)`；`forEach(lambda$new$0)`；`lambda$new$0` = 打印 `Found language provider {}, version {}` + `languageProviderMap.put(name, new ModLanguageWrapper(...))`（`HashMap`，后者覆盖前者）[F]
6. 运行序 [E]：debug.log L162 `Found language provider javafml, version 4.0.45`（builtin 先注册）→ L165 `Found language provider javafml, version 9999`（compat 后注册）→ `map["javafml"]` 终值 = compat；`9999 ∈ [3,)` 与 `[52,)` 均通过 → 两个 ModFile 的 loaders 均为 [compat]
7. 运行时闭合 [E]：`net.neoforged.fml.ModLoader` 调用点 = `IModInfo.getLoader()` → `IModLanguageLoader.loadMod(...)` [F]；Run T 中 compat 的 `loadMod` 被实际调用（stdout L69）→ 若 getLoader() 指向 builtin 则 compat.loadMod 不会被调用；实测被调用 ⇒ `getLoader(neoforge) = compat` 闭合。

下游含义（供裁定方）：若 `validate` 分组按 `IModInfo.getLoader()`（F2-b 前提），则 neoforge 仍归入 compat 组 → builtin validate 仍不会运行；《审阅意见》补 2 所述"委托后内置 validate 恢复运行"未发生（前提被上述链否定）。本项为"必须记录"观测项，非 PASS/FAIL 判据。

## 六、判据自评

- **结果：VALID-002-0d-1**（修复生效且完整启动）
- 依据（逐条对应 §七）：
  - §7.1-1：Run C 复现 G-1 签名（2D-c 表 1–6 逐项一致；仅时间戳 / BUS 地址不同）✓
  - §7.1-2：Run T 出现 `[FORGECOMPAT-LANG] delegate -> builtin javafml for neoforge` ✓
  - §7.1-3：Run T 出现 `NeoForge mod loading`（`NeoForgeMod` 被构造）✓
  - §7.1-4 `[E-1]`：G-1 同链 FATAL 完全缺席（stdout / latest / debug 三处均为 0）✓
  - §7.1-5 `[E-2]`：出现 `Done (0.340s)!`（并伴随 `Starting minecraft server version 1.21.1` → `Done` → `Stopping the server` 完整后阶段序列）✓
  - §7.1-6：ForgeProbe 冻结证据不变（marker + BUS 命中；`REWRITE hits=1`；dump `5E2F78DB…` / 1162 B）✓
  - §7.1-7：control vs treatment javap 对照通过（仅委托分支 + 4 lambda + 常量池重编号；其余 15/16 内容文件逐字节一致）✓
  - §7.1-8：新终点如实记录 = Done（非新断层；无栈）✓
  - §7.2：满足 7.1 全部条件且到达 Done → **VALID 升级成立** ✓
- 结论上限（禁止超出）：仅证明"本次条件下，兼容 provider 对非 Forge 容器的委托使 G-1 清零、生命周期越过 `Registry initialization` 并完整到达 Done"；**不得**推导"兼容层可行 / ABI 兼容 / Forge Mod 可被加载 / 方向 2 API 选择"。

## 七、事实标签

[F] Source Fact：

- 冻结 jar vs control：16/16 内容文件（11 class + 4 service + MANIFEST）逐字节一致（恢复保真，强于硬门 1–4 要求）
- control vs treatment：仅 `ForgeCompatLanguageLoader.class` 差异；差异 = 委托分支 + 4 个 synthetic lambda + 常量池重编号（`2Dd-control-vs-treatment-compare.txt`）
- `getLoader()` 数据链（ModInfo → ModFile.loaders → identifyLanguage → LanguageProviderLoader.findLanguage → languageProviderMap）与 `ModLoader` 调用点（getLoader → loadMod）均由字节码直接证明（`2De-javap-*.txt`）

[E] Experimental Fact：

- Run C 6 项签名与 002-0c 一致（唯一差异：时间戳 / BUS 地址）
- Run T：L69 委托行 → L70 delegate → L75 `NeoForge mod loading` → Done；E-1 缺席；ForgeProbe 证据不变；meta `DONE_STOPPED_GRACEFULLY`
- debug.log L162/L165：javafml@4.0.45（builtin）与 javafml@9999（compat）同时存在于 `languageProviders`
- Run T 中 compat 的 loadMod 被调用 ⇒ `getLoader(neoforge)` 运行时指向 compat（观测项 10 的实际值）

[H] Hypothesis：

- marker 行在 treatment 捕获中的 1 行级乱序源于 harness stdout 事件捕获路径（未做额外复跑验证；不影响判据）
- 若 `validate` 分组按 `getLoader()`，builtin validate 仍不运行（前提 F2-b 待复核；属下游观测项）

[D] Design Decision：

- 无新增；本 Spike 未作任何架构决定（裁定权归决策方）

## 八、与说明书的偏差

1. **治理偏差（过程）**：受协作通道故障影响（子代理 `followup_task` 载荷被吞、执行子代理两次 0 输出卡死），本 Spike 的 2D-a…2D-f 由总指挥（root）应急直接执行；裁判环节改由新裁判实例在报告交回后执行。此偏差不影响产物与判据证据强度，但违反"执行方 = 独立子代理"的协作约定，如实披露。
2. **control 编译命令未单独落盘**：control 的逐字命令行未单独归档（treatment 有 `2Dd-treatment-build-record.txt`）；两 jar 由同一恢复源码树、同一工具、同一 classpath、同构流程产出——记录完整性缺口，如实披露。
3. **run-server.ps1 无机械 diff 基线**：`D:\DualForge` 非 git 仓库、全盘无基线副本 → "仅 ValidateSet 追加 2 个 Label、其余未动"无法以机械 diff 证明。已记录：ValidateSet 行原文（19 个 label；新增 2 个位于列表末尾）+ 文件 SHA-256 + 变更认定 + 限制说明（`2Df-harness-change-record.txt`）；间接证据 = 两轮 K1 均以冻结参数正常运行、artifacts 齐全。
4. **marker/BUS 捕获顺序差异**：Run C 为源码打印顺序；Run T 捕获中 marker 行位于 BUS 块之后（1 行级乱序）。两行均逐字命中；判据不涉顺序。已按原文归档，不作进一步推断。
5. **观测项 10 与送审预期不符**：实际 = compat（非 builtin），系对送审文本的实证修正（§五 观测项 10 / `2De-observation-10-getLoader-mechanism.txt`），非执行偏差。

除上述外，无其它偏差：K1 顺序（control → treatment）保持；未修改任何冻结件；未修复任何新断层；未进入方向 2。

## 九、边界自检

- [x] 未修改 forgeprobe.jar（`A93A…` / 1772 B）/ mods.toml（`B32C…`）/ ForgeProbe 源码
- [x] 未修改四项前置语义与描述符重写逻辑（恢复=逐字节一致；treatment 差异仅限 loader 委托分支）
- [x] 未修改 NeoForge / FML / ModLauncher / Minecraft / Java（loader `8390…`/505696；运行环境 mc 1.21.1 / neoforge 21.1.256 / FML 4.0.45 / ModLauncher 11.0.5 / Java 21.0.10）
- [x] 仅修改 `ForgeCompatLanguageLoader.loadMod`（§五 委托分支）——源码级（仅 1 java 变化）与字节码级（仅该类差异）双重确认
- [x] 未修改 validate / 未新增 validate 委托
- [◐] run-server.ps1 仅 ValidateSet 追加 2 个 Label——ValidateSet 行原文已记录；"其余未动"无机械证明（§八-3）
- [x] 未修复任何新断层 / 未进入方向 2（Run T 到达 Done，无需修复）
- [x] control 先于 treatment 执行（K1 顺序：23:59:47 → 00:03:48）
- [x] 恢复态 / 修复态源码均已归档（含逐文件 SHA-256）

## 十、原始日志

control（`D:\DualForge\spike000\logs\spike002-0d-control\`）：

| 文件 | 字节数 |
|---|---|
| `meta.json` | 1262 |
| `spike002-0d-control.log`（latest） | 7502 |
| `spike002-0d-control.debug.log` | 76150 |
| `spike002-0d-control.stdout.log` | 16850 |
| `spike002-0d-control.stderr.log` | 0 |
| `spike002-0d-control.mods-list.txt` | 48 |
| `crash-reports\crash-2026-10-07_23.59.56-fml.txt` | 5689 |

treatment（`D:\DualForge\spike000\logs\spike002-0d-treatment\`）：

| 文件 | 字节数 |
|---|---|
| `meta.json` | 1410 |
| `spike002-0d-treatment.log`（latest） | 8316 |
| `spike002-0d-treatment.debug.log` | 81448 |
| `spike002-0d-treatment.stdout.log` | 10800 |
| `spike002-0d-treatment.stderr.log` | 0 |
| `spike002-0d-treatment.mods-list.txt` | 40 |
| `crash-reports\` | 无（0 文件） |

elsewhere：

- rewritten dump：`D:\DualForge\spike000\logs\spike002-0b-2a-v1.1\ForgeProbe.rewritten.class`（`5E2F78DB…` / 1162 B）
- harness 记录：`D:\DualForge\spike000\logs\spike002-0d-artifacts\2Df-harness-change-record.txt`

## 附录 A：证据文件清单（`logs\spike002-0d-artifacts\`，共 18 份）

```
2Da-2Db-gates-summary.txt            783
2Da-gate1-control-entries.txt        1080
2Da-gate1-frozen-entries.txt         1080
2Da-javap-control-loader.txt         4655
2Da-javap-frozen-loader.txt          4655
2Da-recovered-src-sha256.txt         1867
2Dd-control-vs-treatment-compare.txt 1950
2Dd-javap-control-loader.txt         4655
2Dd-javap-treatment-loader.txt       9534
2Dd-treatment-build-record.txt       2300
2De-javap-LanguageProviderLoader.txt 13425
2De-javap-ModFile-members.txt        5053
2De-javap-ModFile.txt                37170
2De-javap-ModInfo.txt                36154
2De-javap-ModLoader.txt              68899
2De-observation-10-getLoader-mechanism.txt  2669
2De-run-evidence-extract.txt         3103
2Df-harness-change-record.txt        1365
```

## 附录 B：交接说明

- 本报告交架构决策方（裁判）裁定：确认/否证 VALID-002-0d-1；处置观测项 10 的实证修正与 §八 偏差清单。
- 新基线登记（待裁定后冻结）：treatment jar `4CD8195F…2FEF` / 21180 B / 25 条 + 两轮日志 / meta + 修复态源码归档。
- 冻结 v1.1 jar（`E27B…`）保持只读历史对照，不得删除；旧 002-0c 归档值保留。
- 执行方不自行决定下一 Spike；002-0e 方向（含"语义级字节码对比 + P1/P2 双预登记"两条纪律）由决策方起草。
