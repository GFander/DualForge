# Spike-002-0b Step 2-A 执行报告（STOP：前置配置缺口）

**接收方**：架构决策方（原样交回）
**执行依据**：《Spike-002-0b Step2-A v1.md》（v1 冻结版，2026-10-07 交付）
**执行日期**：2026-10-07
**总体结果**：**STOP —— Run C（control）未达层 c/d，失败于语言提供者版本门**

**停止依据**（v1 条款）：§2.6-5（"若服务端在到达层 d 之前出现失败 ⇒ 在范围内修正后重跑；**无法在范围内修正 ⇒ STOP 交回**"）+ §六-7（Run C 实现性偏离处理）。

**根因定位（一句话）**：v1 §0.3 "运行前置配置" **缺项** —— 未包含原《Spike-002-0b》说明书 §〇 所列"三项前置配置（与 002-0a 相同、来自 001b-6 已验证路径）"中的 **IModLanguageLoader observer + 其 service 声明**。属**前置配置错误**类别（原说明书有明确定义），**非探针实现缺陷**。

**子步骤状态**：

| 子步骤 | 状态 | 说明 |
|---|---|---|
| 2A-a ASM 取证 | ✅ 完成 | runtime = 9.10.1（win_args `-p` + `--add-modules ALL-MODULE-PATH`） |
| 2A-b SPI 方法清单 + default 行为取证 | ✅ 完成 | 与 §2.6 预勘一致；4 个 default 默认行为全部无害 |
| 2A-c 实现探针 + 构建 | ✅ 完成 | 8 源 javac exit 0；jar = 23 条（预检 PASS） |
| 2A-d 重写后字节码验证 | ⏸ N/A | 无重写发生（Run T 未执行 ⇒ 无 dump） |
| 2A-e Run C | ▶ **偏离 → STOP** | 见 §六 |
| 2A-e Run T | ⏸ **未执行** | 语言门失败与 rewrite 开关无关（同一 jar） |
| 2A-f 清理 | ⏸ **未执行** | 保留现场，待裁定（见 §八） |

---

## 一、分析对象与环境

- forgeprobe.jar SHA-256（运行前 / 运行后）：`A93A823C988BFD6A8F6EA132D189D9DE732B0E5951F4BF74980D51BB63374E22`（`probe\build` 与 `neoforge-server\mods` 两份一致；**未修改**）
- 探针 jar（forgecompat）：SHA-256 `B4D119DA5800FAB8D2739F5D3A7D5F03D52DAE36CBBB041E507BFC3415B1C7B5` / 18234 字节 / **23 条**（预检 PASS）
- Run C（control）：label `spike002-0b-2a-control` / status `EXITED_EARLY` / exitCode 0 / duration 12.1s
- Run T（treatment）：**未执行**（N/A）
- user_jvm_args.txt（Run C 实际内容）：`-Xmx3G -Xms1G -Dforgecompat.2a.rewrite=false`
- run-server.ps1 改动（diff 记录）：
  1. ValidateSet 追加 `'spike002-0b-2a-control'`、`'spike002-0b-2a'`（原末项 `'spike002-0a'` 之后）；
  2. 新增参数 `[string[]]$ExtraJvmArgs = @()`；`user_jvm_args.txt` 写入改为 `@('-Xmx3G','-Xms1G') + $ExtraJvmArgs` 的 `-join ' '`（不传时与基线一致）。
- ASM 制品清单（2A-a）：win_args.txt `-p` 模块路径含 asm / asm-tree / asm-analysis / asm-commons / asm-util **9.10.1**（`--add-modules ALL-MODULE-PATH`）；libraries 中的 9.3 未出现在 win_args.txt（不在运行时模块路径）。
  - asm-9.10.1.jar `ED825D10AB1399C8C0CB669E688CF0C8C82629B4C8399B58352B68E92CA10FCB`
  - asm-tree-9.10.1.jar `3DFB0D5B6A106CD40B5B250E39935FBF2F927F4477546A5369A3AC609CF0506B`
  - asm-analysis-9.10.1.jar `DEDE75A21306B65974ECD8F87114FF6970F09FB794157A4CA09AB25C888C2BFC`
  - asm-commons-9.10.1.jar `6D0ABEFB7CBF972EA16EDB37EC14835372505063A45F976AB7EA889ED9497895`
  - asm-util-9.10.1.jar `1BB99D091FBA2597DC6D51193E9BBCF0D8447E7ED96BD8F0198B18152F09655C`
- 冻结版本核对：Minecraft 1.21.1 / NeoForge 21.1.256 / FancyModLoader 4.0.45 / ModLauncher 11.0.5 / Java 21.0.10（Microsoft OpenJDK 64-Bit Server VM 21.0.10+7-LTS）

---

## 二、实现探针清单（2A-c）

**新增 4 个文件**（全部仍在原位，处置见 §八）：

1. `src\main\java\com\example\forgecompat\ForgeCompatTransformationService.java`
   —— ITransformationService 最小实现（4 个 abstract 实现；4 个 default 不覆写；name = `forgecompat-transform`）
2. `src\main\java\com\example\forgecompat\ForgeProbeConstructorTransformer.java`
   —— ITransformer&lt;ClassNode&gt;；唯一功能改点 = `<init>` 描述符 `(Lnet/minecraftforge/eventbus/api/IEventBus;)V` → `(Lnet/neoforged/bus/api/IEventBus;)V`；ClassWriter(0)；运行中 dump；属性开关（未传即抛 IllegalStateException）
3. `src\main\java\net\minecraftforge\eventbus\api\IEventBus.java`
   —— 空壳类型（javap 验证：`public interface net.minecraftforge.eventbus.api.IEventBus { }`，**无成员**）
4. `src\main\resources\META-INF\services\cpw.mods.modlauncher.api.ITransformationService`
   —— 单行 FQCN

**修改 1 个文件**：`build.gradle` dependencies 追加 `compileOnly 'org.ow2.asm:asm:9.10.1'` 与 `compileOnly 'org.ow2.asm:asm-tree:9.10.1'`（其它未动）。

**编译与打包**：

- `javac --release 21 -encoding UTF-8`；源文件 = 8；**exit 0，无 warning**
- classpath = `build\cp.txt` + asm-9.10.1.jar + asm-tree-9.10.1.jar
- 打包 = `jar --create --manifest src\main\resources\META-INF\MANIFEST.MF -C build\jar-root .`
  （打包方法已先用基线留档做 packtest：逐条目 diff = 0，方法验证通过）
- `jar tf` = **23 条**；完整清单见 `logs\spike002-0b-2a-artifacts\2Ac-jar-listing.txt`
  > **实证说明**：v1 冻结前的条目数修正（审阅替换原文 19 → 修正 23，含 4 个新增目录条目 `net/`、`net/minecraftforge/`、`net/minecraftforge/eventbus/`、`net/minecraftforge/eventbus/api/`）由本次构建**实证成立**。
- 归档：`build\forgecompat-002-0b-2a.jar`

**与 §2.2 "唯一变量" 的逐条对照**：源码层面唯一的功能性改动 = 描述符常量替换（命中条件：name = `<init>` 且 desc = 旧描述符；`method.desc = NEW_DESC`）；其余全部为观察 / 日志 / 开关逻辑。

---

## 三、服务共存证据（§2.6 / "未干扰 FML"）

**ITransformationService 8 方法逐一处置**（javap 原文存档：`2Ab-ITransformationService.txt`）：

| # | 方法 | 接口侧 | 探针处置 | 默认行为原文（javap -c） |
|---|---|---|---|---|
| 1 | `name()` | abstract | 实现 → `"forgecompat-transform"` | — |
| 2 | `initialize(IEnvironment)` | abstract | 实现（1 行日志，无副作用） | — |
| 3 | `onLoad(IEnvironment, Set<String>)` | abstract（throws IncompatibleEnvironmentException） | 实现（1 行日志，无副作用） | — |
| 4 | `transformers()` | abstract | 实现 → `List.of(new ForgeProbeConstructorTransformer())` | — |
| 5 | `arguments(BiFunction<…OptionSpecBuilder>)` | default | **不覆写** | `0: return`（空实现） |
| 6 | `argumentValues(OptionResult)` | default | **不覆写** | `0: return`（空实现） |
| 7 | `beginScanning(IEnvironment)` | default | **不覆写** | `invokestatic java/util/List.of:()Ljava/util/List; areturn`（空列表） |
| 8 | `completeScan(IModuleLayerManager)` | default | **不覆写** | `invokestatic java/util/List.of:()Ljava/util/List; areturn`（空列表） |

> **2A-b 结论（含审阅补充项）**：4 个 default 的默认行为**全部无害**（空实现 / 空列表），"不覆写 = 零贡献"成立；未发现任何额外 abstract 方法 —— 与 §2.6 预勘一致，PASS。

**service 声明文件内容（逐字节）**：`com.example.forgecompat.ForgeCompatTransformationService` + 换行（jar 内验证通过）。

**ModLauncher services 启动列表原文（Run C stdout line 171–179）**：

```
	ModLauncher services: 
		sponge-mixin-0.16.4+mixin.0.8.7.jar mixin PLUGINSERVICE 
		loader-4.0.45.jar slf4jfixer PLUGINSERVICE 
		loader-4.0.45.jar runtime_enum_extender PLUGINSERVICE 
		at-modlauncher-10.0.1.jar accesstransformer PLUGINSERVICE 
		loader-4.0.45.jar runtimedistcleaner PLUGINSERVICE 
		modlauncher-11.0.5.jar mixin TRANSFORMATIONSERVICE 
		modlauncher-11.0.5.jar forgecompat-transform TRANSFORMATIONSERVICE 
		modlauncher-11.0.5.jar fml TRANSFORMATIONSERVICE 
```

**"Found additional transformation services" 行原文**（该行为 DEBUG 级，位于 **debug.log** line 25，不在 stdout）：

```
[0710月2026 16:56:23.362] [main/DEBUG] [cpw.mods.modlauncher.TransformationServicesHandler/MODLAUNCHER]: Found additional transformation services from discovery services: [D:\DualForge\spike000\neoforge-server\mods\forgecompat.jar]
```

**service 日志行原文（stdout，逐条）**：

```
line   5: [FORGECOMPAT-XFORM] onLoad() called, otherServices=[mixin, forgecompat-transform, fml]
line   7: [FORGECOMPAT-XFORM] initialize() called, name=forgecompat-transform
line  92: [FORGECOMPAT-XFORM] transformers() called
line  93: [FORGECOMPAT-XFORM] rewriteEnabled=false
```

**判定**：未干扰 FML 的结构证据 = **YES** —— `fml` / `mixin` 服务行不变；我们的服务被实际登记与被驱动；`rewriteEnabled=false` 解析正确；无任何异常源自探针代码。

---

## 四、转换链运行时证据（§2.4）

- Run C（rewrite=false）：
  - offered 清单（逐条原文）：**（空 —— 全文无 `offered:` 行）**
  - rewrite target count：**未发生**（非"观测为 0"，而是转换链未达类加载 —— 见 §六）
- Run T（rewrite=true）：**未执行**（N/A）
- non-target transformed class count：未发生（N/A）
- 同一 internalName 重复 offered：N/A
- **三向诊断（Run C）**：
  - 服务是否被发现：**YES**
  - ForgeProbe 是否被 offered：**NO**
  - 是否发生重写：**NO**

**因果链（[E]）**：ForgeProbe 类**从未被加载**（语言提供者版本门在其之前的 pre-loading 阶段终止了流程）⇒ 类加载转换链未触发 ⇒ ⑥ 调用点未到达 ⇒ 无 offered。

---

## 五、重写后字节码验证（2A-d）—— 未执行（N/A）

- 无重写发生 ⇒ 无 dump（`logs\spike002-0b-2a\ForgeProbe.rewritten.class` 不存在；Run T 未运行）。
- 五项强制字段（构造函数数量 / 原签名 / 新签名 / 其它 `<init>` / 指令主体）：**全部 N/A（未到达）**。

## 六、服务端运行与层判定（2A-e）

### Run C（control，label `spike002-0b-2a-control`）

- 层 d 结果：**未达**（层 c 与层 d 均未到达）
- 实际失败阶段：**语言提供者版本门**（`LanguageProviderLoader`，pre-loading phase）
- 关键原文：
  - `[16:56:23.887] [main/ERROR] [loading.LanguageProviderLoader/LOADING]: Missing language javafml version [52,) wanted by forgeprobe.jar, found 4.0.45`（stdout line 91）
  - `[16:56:30] [main/FATAL] [ne.ne.fm.ModLoader/CORE]: Error during pre-loading phase: Mod File D:\DualForge\spike000\neoforge-server\mods\forgeprobe.jar needs language provider javafml:52 or above to load` / `We have found 4.0.45`（stdout line 99–100）
- **完整 stack trace（原文）**：

```
net.neoforged.fml.ModLoadingException: Loading errors encountered:
	- Mod File D:\DualForge\spike000\neoforge-server\mods\forgeprobe.jar needs language provider javafml:52 or above to load
	  We have found 4.0.45
	at MC-BOOTSTRAP/fml_loader@4.0.45/net.neoforged.fml.ModLoader.gatherAndInitializeMods(ModLoader.java:110) ~[loader-4.0.45.jar%2359!/:4.0] {}
	at TRANSFORMER/neoforge@21.1.256/net.neoforged.neoforge.internal.CommonModLoader.begin(CommonModLoader.java:49) ~[neoforge-21.1.256-universal.jar%23121!/:?] {re:classloading}
	at TRANSFORMER/neoforge@21.1.256/net.neoforged.neoforge.server.loading.ServerModLoader.load(ServerModLoader.java:34) ~[neoforge-21.1.256-universal.jar%23121!/:?] {re:classloading}
	at TRANSFORMER/minecraft@1.21.1/net.minecraft.server.Main.main(Main.java:124) ~[server-1.21.1-20240808.144430-srg.jar%23120!/:?] {re:classloading}
[16:56:31] [main/ERROR] [minecraft/Main]: Failed to start the minecraft server

	at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:103) ~[?:?] {}
	at MC-BOOTSTRAP/fml_loader@4.0.45/net.neoforged.fml.loading.targets.CommonLaunchHandler.runTarget(CommonLaunchHandler.java:136) ~[loader-4.0.45.jar%2359!/:4.0] {}
	at MC-BOOTSTRAP/fml_loader@4.0.45/net.neoforged.fml.loading.targets.CommonLaunchHandler.serverService(CommonLaunchHandler.java:128) ~[loader-4.0.45.jar%2359!/:4.0] {}
	at MC-BOOTSTRAP/fml_loader@4.0.45/net.neoforged.fml.loading.targets.CommonServerLaunchHandler.runService(CommonServerLaunchHandler.java:32) ~[loader-4.0.45.jar%2359!/:4.0] {}
	at MC-BOOTSTRAP/fml_loader@4.0.45/net.neoforged.fml.loading.targets.CommonLaunchHandler.lambda$launchService$4(CommonLaunchHandler.java:118) ~[loader-4.0.45.jar%2359!/:4.0] {}
	at java.base/java.lang.reflect.Method.invoke(Method.java:580) ~[?:?] {}
	at MC-BOOTSTRAP/cpw.mods.modlauncher@11.0.5/cpw.mods.modlauncher.LaunchServiceHandlerDecorator.launch(LaunchServiceHandlerDecorator.java:30) [modlauncher-11.0.5.jar%2364!/:?] {}
	at MC-BOOTSTRAP/cpw.mods.modlauncher@11.0.5/cpw.mods.modlauncher.LaunchServiceHandler.launch(LaunchServiceHandler.java:53) [modlauncher-11.0.5.jar%2364!/:?] {}
	at MC-BOOTSTRAP/cpw.mods.modlauncher@11.0.5/cpw.mods.modlauncher.LaunchServiceHandler.launch(LaunchServiceHandler.java:71) [modlauncher-11.0.5.jar%2364!/:?] {}
	at MC-BOOTSTRAP/cpw.mods.modlauncher@11.0.5/cpw.mods.modlauncher.Launcher.run(Launcher.java:103) [modlauncher-11.0.5.jar%2364!/:?] {}
	at MC-BOOTSTRAP/cpw.mods.modlauncher@11.0.5/cpw.mods.modlauncher.Launcher.main(Launcher.java:74) [modlauncher-11.0.5.jar%2364!/:?] {}
	at MC-BOOTSTRAP/cpw.mods.modlauncher@11.0.5/cpw.mods.modlauncher.BootstrapLaunchConsumer.accept(BootstrapLaunchConsumer.java:26) [modlauncher-11.0.5.jar%2364!/:?] {}
	at MC-BOOTSTRAP/cpw.mods.modlauncher@11.0.5/cpw.mods.modlauncher.BootstrapLaunchConsumer.accept(BootstrapLaunchConsumer.java:23) [modlauncher-11.0.5.jar%2364!/:?] {}
	at cpw.mods.bootstraplauncher@2.0.2/cpw.mods.bootstraplauncher.BootstrapLauncher.run(BootstrapLauncher.java:210) [bootstraplauncher-2.0.2.jar:?] {}
	at cpw.mods.bootstraplauncher@2.0.2/cpw.mods.bootstraplauncher.BootstrapLauncher.main(BootstrapLauncher.java:69) [bootstraplauncher-2.0.2.jar:?] {}
```

- **与 002-0a 的直接对照（根因证据）**：

| 项 | 002-0a（其配置含前置 observer） | Run C（v1 冻结配置） |
|---|---|---|
| FML Language Providers 列表 | `javafml@4.0` / `lowcodefml@4.0` / `minecraft@4.0` / **`javafml@null`**（4 条） | `javafml@4.0` / `minecraft@4.0` / `lowcodefml@4.0`（3 条，**无** `javafml@null`） |
| observer 生效证据 | `[FORGECOMPAT][002-0a] FMLModContainer constructed, returning to NeoForge: net.neoforged.fml.javafmlmod.FMLModContainer@…`（stdout lines 59 / 61） | 无 |
| 结果 | 到达层 d：`unsupported argument interface net.minecraftforge.eventbus.api.IEventBus` | 语言门失败（未达层 c/d） |

### Run T（treatment）

- **未执行**。理由：语言门失败发生于任何 mod 类加载之前，与 `forgecompat.2a.rewrite` 开关无关（同一 jar；属性仅影响 ForgeProbe 被 offer 时的行为，而 ForgeProbe 从未加载）。运行 Run T 将复现同一失败、无新增信息 —— 按 v1 §2A-e "Run C 偏离处理" 与 K1 顺序纪律停止。
- 层 e 进入证据：无（N/A）
- STOP 类型：**非层 e 触发**；为**前置配置缺口**的 STOP 交回。

---

## 七、与说明书的偏差

**偏差 1（关键，触发 STOP）：v1 §0.3 前置配置缺项 —— Run C 未达层 c/d**

- 期望（v1 §2A-e）：`offered ≥ 1 且含 ForgeProbe` / `rewrite target count = 0` / **层 c PASS** / **层 d FAIL（与 002-0a 同款异常）**
- 实际：无 offered；语言提供者版本门失败；层 c/d 未达
- **根因**：v1 的"运行前置配置"（§0.3）只含空壳 IEventBus（审阅-2 修正），**未包含**原《Spike-002-0b》说明书 §〇 明确列出的"三项前置配置（与 002-0a 相同，均来自 001b-6 已验证路径）"中的第 1、3 项：
  1. **IModLanguageLoader observer（兼容层）**（name = `javafml`；版本取值使 `[52,)` 检查通过）；
  2. （第 2 项 = `ForgeModFileInfoParser.build()` 返回真实 ModFileInfo —— **基线已满足**：Run C 日志 `[FORGECOMPAT] ForgeModFileInfo built: mods=1 languageSpecs=[…]`、`CALLED ForgeModFileInfo.getMods -> 1 entry(ies)`）；
  3. **`META-INF/services/net.neoforged.neoforgespi.language.IModLanguageLoader`** 声明。
- **定性**：**前置配置错误**（原《Spike-002-0b》§〇 对该类别有明确定义）—— **不是探针实现缺陷**：探针组件全部按设计工作（服务被发现、被驱动、属性解析正确；"无 offered"是因为 mod 类从未加载）。
- 证据文件：`logs\spike002-0b-2a-artifacts\2Ae-runC-evidence.txt`（本报告 §六 对照表为其摘要）。

**偏差 2（已获准的形式修正，实证成立）：预检条目数 19 → 23**

- 审阅替换原文记 19 条；v1 冻结前已按打包拓扑修正为 23 条（含 4 个新增目录条目）。本次构建 `jar tf` 实测 = **23**，修正成立。

**偏差 3：无**。除上述两项外，2A-a / 2A-b / 2A-c 与 v1 描述一致（含 2A-b "default 行为取证" 审阅补充项）。

---

## 八、STOP 判定与当前状态

**STOP 判定**：按 v1 §2.6-5（"若服务端在到达层 d 之前出现失败 ⇒ 在范围内修正后重跑；**无法在范围内修正 ⇒ STOP 交回**"）与 §六-7 —— 修复需要恢复 v1 未列入的**前置配置**（observer + service 声明，属说明书级变更），**无法在冻结版范围内修正** ⇒ **STOP 交回**。

**未执行**：Run T、2A-d 字节码验证、2A-f 清理。

**当前工作区状态（保留现场，待裁定；未清理）**：

- compat 工程：4 个新文件仍在；`build.gradle` 含 ASM compileOnly ×2；`build\forgecompat.jar` = 探针构建（23 条）；`build\forgecompat-002-0b-2a.jar` 已归档
- `run-server.ps1`：保留 2 处改动（ValidateSet ×2 + `-ExtraJvmArgs`）
- 日志：`logs\spike002-0b-2a-control\`（Run C 全套：stdout / latest / debug / meta / crash 目录）与 `logs\spike002-0b-2a-artifacts\`（2A-a / 2A-b / 2A-c / Run C 证据）保留；`logs\spike002-0b-2a\` 不存在（Run T 未运行）
- `mods\`：`forgeprobe.jar` + `forgecompat.jar`（Run C 结束时状态）
- NeoForge / FML 工件、forgeprobe.jar：**未修改**（forgeprobe.jar 哈希运行前后一致）

**证据存档清单**（`logs\spike002-0b-2a-artifacts\`）：

```
win_args.txt                       （win_args.txt 副本）
2Aa-asm-evidence.txt               （2A-a：ASM 版本/路径/SHA-256）
2Ab-ITransformationService.txt     （2A-b javap 原文）
2Ab-ITransformer-family.txt        （2A-b javap 原文：ITransformer/Target/TargetType/TransformerVoteResult）
2Ac-build-evidence.txt             （2A-c：构建/打包/条目/哈希）
2Ac-jar-listing.txt                （probe jar 全条目清单）
2Ac-IEventBus-javap.txt            （空壳类型无成员验证）
2Ae-runC-evidence.txt              （Run C 核心证据：服务块/日志行/语言门/002-0a 对照）
```

**待裁定项（架构决策方）**：

1. 是否按原《Spike-002-0b》§〇 / 001b-6 已验证路径，将 **IModLanguageLoader observer + IModLanguageLoader service 声明**恢复为 Run C / Run T 的**共同前置配置**（即修订 v1 → 修订版 / v1.1），随后按原顺序重跑 Run C → Run T？
2. observer 的精确形态（name / version / loadMod 实现）是否需要先从 001b-6 / 001c-1 / 002-0a 材料复核并冻结，再执行？
3. 本现场（未清理状态）是否维持至裁定？

---

## 九、事实标签

**[F] Source Fact**：

- 2A-a：runtime ASM = 9.10.1（win_args.txt `-p` + `--add-modules ALL-MODULE-PATH`；5 个制品 SHA-256 见 §一）
- 2A-b：ITransformationService = 4 abstract + 4 default；4 个 default 默认行为 = `return` / `return` / `List.of()` / `List.of()`；未发现额外 abstract 方法
- 探针 jar = 23 条；IEventBus 类无成员；service 文件单行 FQCN

**[E] Experimental Fact**：

- Run C（`-Dforgecompat.2a.rewrite=false`）：服务被发现（services 列表 + debug.log promotion 行）、被驱动（onLoad / initialize / transformers / rewriteEnabled=false）；`user_jvm_args.txt` 生效；**无 offered**；失败于语言提供者版本门（`found 4.0.45` vs `[52,)`）；EXITED_EARLY / exitCode 0 / 12.1s
- 002-0a 对照：`javafml@null` provider + `FMLModContainer constructed` 行 + 到达层 d 的 `unsupported argument`

**[H] Hypothesis**：

- 恢复 observer 前置配置后，Run C 将复现 002-0a 的层 d 失败（`unsupported argument`）—— 待修正后重跑验证

**[D] Design Decision（交回）**：

- STOP 判定；待裁定项见 §八

---

## 十、结论

- 本 Spike 结果：**STOP（VALID EXPERIMENT / 前置配置缺口）** —— **不产出层 d / 层 e 判定**
- 层 d 是否解除：**未判定**（未到达）
- 层 e 是否进入：**未判定**（未到达）
- 结论上限：除"探针组件正常工作 + Run C 偏离形态 + 根因定位"外，**不做任何层 d / 层 e 推断**；不进入 002-0c。
- Confidence：**HIGH**（根因由 002-0a 与 Run C 的直接日志对照支持）
- Evidence：§一–§六 的文件与行号；`logs\spike002-0b-2a-artifacts\` 全部存档
- Hypotheses still unverified：§九 `[H]` 1 项

---

## 十一、边界确认

- [x] 未修改 NeoForge / FML 核心（含 FMLModContainer / TransformStore / ClassTransformer）
- [x] 未修改 forgeprobe.jar / mods.toml（forgeprobe.jar 哈希运行前后一致）
- [x] 未执行 Run T（K1 顺序 = 先 control；偏离即停）
- [x] 未自行修改 v1 说明书任何段落
- [x] 未自行恢复 observer 前置配置（超出现行冻结版实现面 → 交回）
- [x] 未执行 B 分支；未进入 002-0c
- [x] 未改动 ClassLoader / ModuleLayer 策略；未注入 IEventBus 实例；未使用 Unsafe
- [x] ASM 使用严格限定（仅一条 desc 重写路径；本运行未发生重写）
- [ ] 2A-f 清理：**未执行**（保留现场待裁定）
