# Spike-002-0c 实验报告

**接收方**：架构决策方（原样交回）
**执行依据**：《Spike_002-0c.md》（v1 冻结版，2026-10-07 归档于 `D:\DualForge\Dev Manuals\Spike_002-0c.md`）
**执行日期**：2026-10-07
**总体结果**：**PASS-002-0c-1** —— FML 未达 Done，但到达明确生命周期阶段（**Registry initialization**），且记录到后续第一次失败（`IllegalStateException: The following registries were created but not registered to NewRegistryEvent`）；按 §七 记录完整 stack trace、不修复、停止。

---

## 一、实验环境

- Minecraft / NeoForge / FancyModLoader / ModLauncher / Java：1.21.1 / 21.1.256 / 4.0.45 / 11.0.5 / 21.0.10（Microsoft OpenJDK 64-Bit Server VM 21.0.10+7-LTS）
- forgecompat-002-0b-2a-v1.1.jar SHA-256 / 字节数 / 条目数：
  `E27BB9659303DDEDF2EDE02D10B49CAFC586549C0EDA003A0165DF525F97A436` / 20316 / **25**
- forgeprobe.jar SHA-256 / 字节数：
  `A93A823C988BFD6A8F6EA132D189D9DE732B0E5951F4BF74980D51BB63374E22` / 1772
- JVM 参数：`-Dforgecompat.2a.rewrite=true`
- Label：`spike002-0c`
- meta.json status / exitCode / duration：`EXITED_EARLY` / 0 / **12.1s**（doneLine = null）

---

## 二、前置生效确认（2C-b 阶段 1）

- javafml@null provider：**YES**
- offered：**YES**
- REWRITE hits：**1**
- 原文（stdout，逐行，不得摘要）：

```
line 60: [FORGECOMPAT-XFORM] rewriteEnabled=true
line 64: [FORGECOMPAT-LANG] loadMod called for forgeprobe entrypoints=[com.example.forgeprobe.ForgeProbe]
line 65: [FORGECOMPAT-XFORM] offered: com/example/forgeprobe/ForgeProbe
line 66: [FORGECOMPAT-XFORM] REWRITE hits=1 old=(Lnet/minecraftforge/eventbus/api/IEventBus;)V new=(Lnet/neoforged/bus/api/IEventBus;)V
line 67: [FORGECOMPAT-XFORM] post-check ctorCount=1 oldAbsent=true newPresent=true
line 68: [FORGECOMPAT-XFORM] dump OK path=D:\DualForge\spike000\logs\spike002-0b-2a-v1.1\ForgeProbe.rewritten.class bytes=1162
line 69: [FORGECOMPAT-LANG] loadMod called for neoforge entrypoints=[]

FML Language Providers（stdout）:
	javafml@4.0
	lowcodefml@4.0
	minecraft@4.0
	javafml@null
```

---

## 三、层 e 进入确认（2C-b 阶段 2）

- marker 命中：**YES**
- marker 原文（stdout line 70–73，不得摘要）：

```
=================================
FORGE PROBE CONSTRUCTOR REACHED
BUS = net.neoforged.bus.EventBus@f2931ef
=================================
```

---

## 四、FML 后续生命周期（2C-b 阶段 3）

- FML 生命周期事件清单（在 stdout / latest / debug 三份日志中逐名检索；此表为**观测结果**，非推断）：

| 事件名 | 到达? | 最后一条日志（原文） |
|---|---|---|
| FMLLoadCompleteEvent | **NO（零命中）** | — |
| FMLCommonSetupEvent | **NO（零命中）** | — |
| FMLDedicatedServerSetupEvent | **NO（零命中）** | — |
| FMLServerAboutToStartEvent | **NO（零命中）** | — |
| FMLServerStartingEvent | **NO（零命中）** | — |
| FMLServerStartedEvent | **NO（零命中）** | — |
| Done | **NO**（doneLine = null） | — |
| 其它（注册表同步 / 资源加载 / 网络初始化） | **注册表初始化（Registry initialization）：到达（失败于此阶段）**；资源加载 / 网络初始化：NO | `[18:05:48] [main/FATAL] [ne.ne.fm.ModLoader/LOADING]: Failed to wait for future Registry initialization, 1 errors found` |

- marker 之后最后一条日志（原文）：

```
最后一条 FATAL 行（stdout line 74）:
[18:05:48] [main/FATAL] [ne.ne.fm.ModLoader/LOADING]: Failed to wait for future Registry initialization, 1 errors found

stdout 字面最后一行（stdout 末行）:
	at cpw.mods.bootstraplauncher@2.0.2/cpw.mods.bootstraplauncher.BootstrapLauncher.main(BootstrapLauncher.java:69) [bootstraplauncher-2.0.2.jar:?] {}
```

- 进程终止位置：
  - status：`EXITED_EARLY`
  - exitCode：`0`
  - duration：`12.1s`
- 是否到达 Done：**NO**
- 若有新异常：**YES（first confirmed failure）**
  - 异常类型：`java.lang.IllegalStateException`（由 `net.neoforged.fml.ModLoadingException` 包装上报；
    伴随 `CrashReportExtender$ModLoadingCrashException: Mod loading has failed`）
  - 异常消息：

```
An uncaught parallel processing error has occurred.
java.lang.IllegalStateException: The following registries were created but not registered to NewRegistryEvent:
 - neoforge:fluid_type
 - neoforge:ingredient_serializer
 - neoforge:biome_modifier_serializers
 - neoforge:entity_data_serializers
 - neoforge:holder_set_type
 - neoforge:global_loot_modifier_serializers
 - neoforge:structure_modifier_serializers
 - neoforge:fluid_ingredient_type
 - neoforge:attachment_types
 - neoforge:condition_codecs
 - neoforge:synced_attachment_types
```

  - 完整 stack trace（原文）：

```
-- 首次失败（内层，异步执行链；crash-report "Head"/"Mod loading issue" 原文）--

-- Head --
Thread: main
Stacktrace:
	at TRANSFORMER/neoforge@21.1.256/net.neoforged.neoforge.registries.RegistryManager.postNewRegistryEvent(RegistryManager.java:94) ~[neoforge-21.1.256-universal.jar%23121!/:?] {re:classloading}

-- Mod loading issue --
Details:
	Mod file: <No mod information provided>
	Failure message: An uncaught parallel processing error has occurred.
		java.lang.IllegalStateException: The following registries were created but not registered to NewRegistryEvent:
			 - neoforge:fluid_type
			 - neoforge:ingredient_serializer
			 - neoforge:biome_modifier_serializers
			 - neoforge:entity_data_serializers
			 - neoforge:holder_set_type
			 - neoforge:global_loot_modifier_serializers
			 - neoforge:structure_modifier_serializers
			 - neoforge:fluid_ingredient_type
			 - neoforge:attachment_types
			 - neoforge:condition_codecs
			 - neoforge:synced_attachment_types
	Mod version: <No mod information provided>
	Mod issues URL: <No issues URL found>
	Exception message: java.lang.IllegalStateException: The following registries were created but not registered to NewRegistryEvent:
	 - neoforge:fluid_type
	 - neoforge:ingredient_serializer
	 - neoforge:biome_modifier_serializers
	 - neoforge:entity_data_serializers
	 - neoforge:holder_set_type
	 - neoforge:global_loot_modifier_serializers
	 - neoforge:structure_modifier_serializers
	 - neoforge:fluid_ingredient_type
	 - neoforge:attachment_types
	 - neoforge:condition_codecs
	 - neoforge:synced_attachment_types
Stacktrace:
	at TRANSFORMER/neoforge@21.1.256/net.neoforged.neoforge.registries.RegistryManager.postNewRegistryEvent(RegistryManager.java:94) ~[neoforge-21.1.256-universal.jar%23121!/:?] {re:classloading}
	at TRANSFORMER/neoforge@21.1.256/net.neoforged.neoforge.internal.CommonModLoader.lambda$begin$0(CommonModLoader.java:52) ~[neoforge-21.1.256-universal.jar%23121!/:?] {re:classloading}
	at java.base/java.util.concurrent.CompletableFuture$AsyncRun.run(CompletableFuture.java:1804) ~[?:?] {}
	at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1144) ~[?:?] {}
	at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:642) ~[?:?] {}
	at java.base/java.lang.Thread.run(Thread.java:1583) ~[?:?] {}

-- 外层包装（main 线程；latest.log 原文）--

net.neoforged.fml.ModLoadingException: Loading errors encountered:
	- An uncaught parallel processing error has occurred.
	  java.lang.IllegalStateException: The following registries were created but not registered to NewRegistryEvent:
	  	 - neoforge:fluid_type
	  	 - neoforge:ingredient_serializer
	  	 - neoforge:biome_modifier_serializers
	  	 - neoforge:entity_data_serializers
	  	 - neoforge:holder_set_type
	  	 - neoforge:global_loot_modifier_serializers
	  	 - neoforge:structure_modifier_serializers
	  	 - neoforge:fluid_ingredient_type
	  	 - neoforge:attachment_types
	  	 - neoforge:condition_codecs
	  	 - neoforge:synced_attachment_types

	at MC-BOOTSTRAP/fml_loader@4.0.45/net.neoforged.fml.ModLoader.waitForFuture(ModLoader.java:281) ~[loader-4.0.45.jar%2359!/:4.0]
	at MC-BOOTSTRAP/fml_loader@4.0.45/net.neoforged.fml.ModLoader.waitForTask(ModLoader.java:183) ~[loader-4.0.45.jar%2359!/:4.0]
	at MC-BOOTSTRAP/fml_loader@4.0.45/net.neoforged.fml.ModLoader.runInitTask(ModLoader.java:163) ~[loader-4.0.45.jar%2359!/:4.0]
	at TRANSFORMER/neoforge@21.1.256/net.neoforged.neoforge.internal.CommonModLoader.begin(CommonModLoader.java:51) ~[neoforge-21.1.256-universal.jar%23121!/:?]
	at TRANSFORMER/neoforge@21.1.256/net.neoforged.neoforge.server.loading.ServerModLoader.load(ServerModLoader.java:34) ~[neoforge-21.1.256-universal.jar%23121!/:?]
	at TRANSFORMER/minecraft@1.21.1/net.minecraft.server.Main.main(Main.java:124) ~[server-1.21.1-20240808.144430-srg.jar%23120!/:?]
	at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:103) ~[?:?]
	at java.base/java.lang.reflect.Method.invoke(Method.java:580) ~[?:?]
	at MC-BOOTSTRAP/fml_loader@4.0.45/net.neoforged.fml.loading.targets.CommonLaunchHandler.runTarget(CommonLaunchHandler.java:136) ~[loader-4.0.45.jar%2359!/:4.0]
	at MC-BOOTSTRAP/fml_loader@4.0.45/net.neoforged.fml.loading.targets.CommonServerLaunchHandler.runService(CommonServerLaunchHandler.java:32) ~[loader-4.0.45.jar%2359!/:4.0]
	at MC-BOOTSTRAP/fml_loader@4.0.45/net.neoforged.fml.loading.targets.CommonLaunchHandler.serverService(CommonLaunchHandler.java:128) ~[loader-4.0.45.jar%2359!/:4.0]
	at MC-BOOTSTRAP/fml_loader@4.0.45/net.neoforged.fml.loading.targets.CommonLaunchHandler.lambda$launchService$4(CommonLaunchHandler.java:118) ~[loader-4.0.45.jar%2359!/:4.0]
	at MC-BOOTSTRAP/cpw.mods.modlauncher@11.0.5/cpw.mods.modlauncher.LaunchServiceHandler.launch(LaunchServiceHandler.java:53) [modlauncher-11.0.5.jar%2364!/:?]
	at MC-BOOTSTRAP/cpw.mods.modlauncher@11.0.5/cpw.mods.modlauncher.LaunchServiceHandler.launch(LaunchServiceHandler.java:71) [modlauncher-11.0.5.jar%2364!/:?]
	at MC-BOOTSTRAP/cpw.mods.modlauncher@11.0.5/cpw.mods.modlauncher.LaunchServiceHandlerDecorator.launch(LaunchServiceHandlerDecorator.java:30) [modlauncher-11.0.5.jar%2364!/:?]
	at MC-BOOTSTRAP/cpw.mods.modlauncher@11.0.5/cpw.mods.modlauncher.Launcher.run(Launcher.java:103) [modlauncher-11.0.5.jar%2364!/:?]
	at MC-BOOTSTRAP/cpw.mods.modlauncher@11.0.5/cpw.mods.modlauncher.BootstrapLaunchConsumer.accept(BootstrapLaunchConsumer.java:26) [modlauncher-11.0.5.jar%2364!/:?]
	at MC-BOOTSTRAP/cpw.mods.modlauncher@11.0.5/cpw.mods.modlauncher.Launcher.main(Launcher.java:74) [modlauncher-11.0.5.jar%2364!/:?]
	at MC-BOOTSTRAP/cpw.mods.modlauncher@11.0.5/cpw.mods.modlauncher.BootstrapLaunchConsumer.accept(BootstrapLaunchConsumer.java:23) [modlauncher-11.0.5.jar%2364!/:?]
	at cpw.mods.bootstraplauncher@2.0.2/cpw.mods.bootstraplauncher.BootstrapLauncher.run(BootstrapLauncher.java:210) [bootstraplauncher-2.0.2.jar:?]
	at cpw.mods.bootstraplauncher@2.0.2/cpw.mods.bootstraplauncher.BootstrapLauncher.main(BootstrapLauncher.java:69) [bootstraplauncher-2.0.2.jar:?]
```

## 五、观测结论

- 本 Spike 结果：**PASS-002-0c-1**
- FML 到达的最后阶段：**Registry initialization**（`ModLoader.runInitTask` → `CommonModLoader.begin` → `RegistryManager.postNewRegistryEvent` / `NewRegistryEvent`）
- 后续第一次失败（如有）：见 §四 —— `java.lang.IllegalStateException: The following registries were created but not registered to NewRegistryEvent`（完整 stack trace 已记录，未修复，按 §七 停止）

---

## 六、清理确认（2C-d）

- [x] forgecompat-002-0b-2a-v1.1.jar 哈希未变
      （`E27BB9659303DDEDF2EDE02D10B49CAFC586549C0EDA003A0165DF525F97A436` / 20316 字节 / 25 条，执行前后一致）
- [x] forgeprobe.jar 哈希未变
      （`A93A823C988BFD6A8F6EA132D189D9DE732B0E5951F4BF74980D51BB63374E22` / 1772 字节）
- [x] 探针源码树未修改（`spike001a\compat\src\main\java` = 001b-1 基线 **5 java**）
- [x] mods.toml 未修改（本轮无任何写操作；SHA-256 `B32CF69B88FBE053C2580FC739B30FEDC43D96984C72FF78CD6725C7F3A1CE6E`）
- [x] NeoForge / FML / Minecraft / Java 版本未变（21.1.256 / 4.0.45 / 1.21.1 / 21.0.10）
- [x] run-server.ps1 仅新增 `spike002-0c` Label —— **diff 记录**：
  - before：`ValidateSet(... 'spike002-0b-2a-control-v1.1', 'spike002-0b-2a-v1.1')`（16 个 label）
  - after：`ValidateSet(... 'spike002-0b-2a-v1.1', 'spike002-0c')`（17 个 label）
  - 其余逻辑未动；语法检查通过；本 Spike 的 `-Label spike002-0c` 由该追加生效

**附加注记（非清单项，供审计）**：归档 jar 的 transformer 在本次运行中将 dump 写入
`logs\spike002-0b-2a-v1.1\ForgeProbe.rewritten.class`（其硬编码路径），内容与 002-0b-2A 记录一致
（SHA-256 `5E2F78DB2B0909D7368A185F618C3D080C00B732279B105CD5B8010FD66D9295` / 1162 字节）——
同内容重写，未产生任何差异。

---

## 七、原始日志

- latest.log / debug.log / stdout.log / stderr.log / crash-report：

```
logs\spike002-0c\
  ├ spike002-0c.stdout.log   [16837 B]
  ├ spike002-0c.log          [7502 B]（latest）
  ├ spike002-0c.debug.log    [76151 B]
  ├ spike002-0c.stderr.log   [0 B]
  ├ spike002-0c.mods-list.txt[48 B]
  ├ meta.json                [1191 B]
  └ crash-reports\crash-2026-10-07_18.05.48-fml.txt [5681 B]
```

- 完整日志文件清单（含 2C-b 参数）：
  - `user_jvm_args.txt` = `-Xmx3G -Xms1G -Dforgecompat.2a.rewrite=true`
  - `mods/` = `forgecompat-002-0b-2a-v1.1.jar` + `forgeprobe.jar`（meta.json 记录一致）

---

## 八、事实标签

**[F] Source Fact**：

- 复用件哈希 = 冻结基线（jar `E27BB965…A436` / 20316 / 25；forgeprobe `A93A…74E22` / 1772）；
- run-server.ps1 唯一改动 = ValidateSet 追加 `spike002-0c`（16 → 17）；
- 探针源码树 = 001b-1 基线 5 java；mods.toml 未修改。

**[E] Experimental Fact**（本 Spike 产出主体）：

- 阶段 1：`javafml@null` provider 出现 / `offered: com/example/forgeprobe/ForgeProbe` / `REWRITE hits=1` / `post-check ctorCount=1 oldAbsent=true newPresent=true`；
- 阶段 2：`FORGE PROBE CONSTRUCTOR REACHED` 命中；`BUS = net.neoforged.bus.EventBus@f2931ef`；
- 阶段 3：marker 之后进入 **Registry initialization**，首个事件失败于 `RegistryManager.postNewRegistryEvent`（`IllegalStateException`，11 个 `neoforge:*` 注册表"created but not registered to NewRegistryEvent"）；
- `FMLLoadCompleteEvent / FMLCommonSetupEvent / FMLDedicatedServerSetupEvent / FMLServerAboutToStartEvent / FMLServerStartingEvent / FMLServerStartedEvent / Done` 在 stdout / latest / debug 三份日志中**逐名检索零命中**；
- 进程终止位置：`EXITED_EARLY` / exitCode 0 / 12.1s；`doneLine = null`。

**[H] Hypothesis**：

- 该失败点（Registry initialization / NewRegistryEvent）与"零 API 调用的 ForgeProbe"之间的因果关系**未判定**（本 Spike 只观测、不归因）；是否涉及前置配置（observer 对 neoforge mod 容器的影响等）同样**未判定**。

**[D] Design Decision（交回）**：

- 报告与全部日志交回；方向 2 的首个 API 探针由架构决策方裁定。

---

## 九、结论

- 本 Spike 结果：**PASS-002-0c-1**
- 结论上限（禁止超出）：
  - `[E]` FML 在 v1.1 前置 + 描述符重写 + 层 e 进入的基线上，走到**注册表初始化（Registry initialization）**阶段；
  - `[E]` marker 之后 FML 自身的行为：继续执行至 Registry initialization，于 `NewRegistryEvent` 的 `RegistryManager.postNewRegistryEvent` 处抛出首个失败并终止（`EXITED_EARLY` / 0 / 12.1s）。
- 禁止从本 Spike 推导：
  - "ForgeProbe 已成功加载" / "参数注入 ABI 已完全兼容"；
  - "Forge Mod 可被 NeoForge 加载" / "兼容层已可行"；
  - "方向 2 应采用 XXX API"（方向 2 的 API 选择由后续 Spike 裁定）。
- Confidence：**HIGH**
- Evidence：§一–§七（含完整 stack trace 与日志清单）；`logs\spike002-0c\` 全量存档
- Hypotheses still unverified：§八 `[H]` 1 项（失败点归因未判定）

---

## 十、边界确认

- [x] 未修改 forgeprobe.jar（任何字节）
- [x] 未修改 forgecompat-002-0b-2a-v1.1.jar（任何字节）
- [x] 未修改 mods.toml
- [x] 未修改四项前置（源码树 5 java / 2 services / `build\forgecompat.jar` 保持 001b-1 基线）
- [x] 未修改描述符重写逻辑（复用归档 jar，未重新构建）
- [x] 未引入新 Forge API
- [x] 未引入新 Mod
- [x] 未顺手修复任何新失败点
- [x] 未进入方向 2
- [x] marker 之后内容已完整观测（未因 002-0b-2A 的"层 e 进入即 STOP"约束而截断）
