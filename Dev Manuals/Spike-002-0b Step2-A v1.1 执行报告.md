# Spike-002-0b Step 2-A v1.1 执行报告（PASS：层 d 解除 + 层 e 进入）

**接收方**：架构决策方（原样交回）
**执行依据**：《Spike-002-0b Step2-A v1.1.md》（v1.1 冻结版，2026-10-07）
**执行日期**：2026-10-07
**总体结果**：**PASS-002-0b-2A** —— 层 d 解除（全文无 `unsupported argument`）；层 e 进入（marker 命中，随即 STOP）

**执行链状态**：

| 步骤 | 状态 |
|---|---|
| 1. 保留 v1 现场 | ✅（`logs\spike002-0b-2a-control\` 未动；`Spike_002-0b Step2-A v1.md` 原文保留） |
| 2. 冻结 v1.1 | ✅（4 处替换 + A.2 十项 + 状态行 + 文件 `Spike_002-0b Step2-A v1.1.md`） |
| 3. 四项共同前置 | ✅（6 新增 + 3 修改） |
| 4. 重构建 | ✅（25 条；弹性预检硬要求 1–4 全过；归档 `forgecompat-002-0b-2a-v1.1.jar`） |
| 5. Run C | ✅（**ANCHOR-002-0b-2A-v1.1 命中**） |
| 6. Run T | ✅（**层 d 解除 + 层 e 进入 → STOP**） |
| 7. 2A-d | ✅（dump 1162 字节 + javap 验证五项全过） |
| 8. 2A-f | ✅（清理完成；15/15、逐条目 diff = 0） |
| 9. 报告 | 本文 |

---

## 一、分析对象与环境

- forgeprobe.jar SHA-256（运行前 / 运行后）：`A93A823C988BFD6A8F6EA132D189D9DE732B0E5951F4BF74980D51BB63374E22`（`probe\build` 与 `mods\` 两份一致；**1772 字节；未修改**）
- 探针 jar（forgecompat）：SHA-256 `E27BB9659303DDEDF2EDE02D10B49CAFC586549C0EDA003A0165DF525F97A436` / 20316 字节 / **25 条**（弹性预检全过；全量清单 `2Ac-v1.1-jar-listing.txt`）
- Run C（control）label / meta：`spike002-0b-2a-control-v1.1` / `EXITED_EARLY` / exitCode 0 / 12.1s
- Run T（treatment）label / meta：`spike002-0b-2a-v1.1` / `EXITED_EARLY` / exitCode 0 / 12.1s
- user_jvm_args.txt 内容：
  - Run C：`-Xmx3G -Xms1G -Dforgecompat.2a.rewrite=false`
  - Run T：`-Xmx3G -Xms1G -Dforgecompat.2a.rewrite=true`
- run-server.ps1 diff（核对结论）：
  1. ValidateSet 追加 4 个 Label（`spike002-0b-2a-control`、`spike002-0b-2a`、`spike002-0b-2a-control-v1.1`、`spike002-0b-2a-v1.1`）；
  2. 新增参数 `[string[]]$ExtraJvmArgs = @()`；`user_jvm_args.txt` 以 `@('-Xmx3G','-Xms1G') + $ExtraJvmArgs` 拼接写入（不传时与基线一致）。
- ASM 制品清单（2A-a，v1 取证复用）：win_args.txt `-p` + `--add-modules ALL-MODULE-PATH` 解析 asm / asm-tree / asm-analysis / asm-commons / asm-util **9.10.1**；SHA-256 五件套见 `2Aa-asm-evidence.txt`（asm `ED825D10…10FCB`；asm-tree `3DFB0D5B…0506B`；asm-analysis `DEDE75A2…C2BFC`；asm-commons `6D0ABEFB…97895`；asm-util `1BB99D09…9655C`）。
- 冻结版本核对：Minecraft 1.21.1 / NeoForge 21.1.256 / FancyModLoader 4.0.45 / ModLauncher 11.0.5 / Java 21.0.10（Microsoft OpenJDK 64-Bit Server VM 21.0.10+7-LTS）。

---

## 二、实现探针清单（2A-c）

**新增 6 个文件**（v1.1 执行完毕后已按 §八 清理；内容已归档于 `forgecompat-002-0b-2a-v1.1.jar`）：

1. `src\main\java\com\example\forgecompat\ForgeCompatTransformationService.java`（v1 原有；ITransformationService 最小实现）
2. `src\main\java\com\example\forgecompat\ForgeProbeConstructorTransformer.java`（v1 原有；ITransformer&lt;ClassNode&gt;；唯一 desc 重写 + dump）
3. `src\main\java\net\minecraftforge\eventbus\api\IEventBus.java`（v1 原有；空壳类型）
4. `src\main\resources\META-INF\services\cpw.mods.modlauncher.api.ITransformationService`（v1 原有）
5. `src\main\java\com\example\forgecompat\ForgeCompatLanguageLoader.java`（v1.1 新增；observer：`name()="javafml"` / `version()="9999"` / `loadMod → FMLModContainer`）
6. `src\main\resources\META-INF\services\net.neoforged.neoforgespi.language.IModLanguageLoader`（v1.1 新增）

**修改 3 个文件**（均已恢复基线）：

1. `build.gradle`：追加 `compileOnly` asm / asm-tree 9.10.1
2. `ForgeModFileInfoParser.java`：`build()` 返回 `new ModFileInfo(modFile, ForgeModFileInfo.configOf(parsed), info -> { })`（+ import / cast 行）
3. `ForgeModFileInfo.java`：新增 `static IConfigurable configOf(Map<String, Object> parsed)`（决策方逐字定义）

**编译**：`javac --release 21 -encoding UTF-8`；源文件 = 9；**exit 0，无 warning**（classpath = `build\cp.txt` + asm + asm-tree）。

**打包与弹性预检**（`jar --create --manifest … -C build\jar-root .`）：

- 硬要求 1：6 个新增源 + 3 个修改源的产物全部存在 —— **PASS**（逐项核对：ForgeCompatTransformationService / ForgeProbeConstructorTransformer / ForgeCompatLanguageLoader / ForgeModFileInfo / ForgeModFileInfoParser / IEventBus）；
- 硬要求 2：`META-INF/services/` 下 **4 个 service 文件全部存在**（IModFileReader / IModFileCandidateLocator / IModLanguageLoader / ITransformationService）—— **PASS**；
- 硬要求 3：`IEventBus.class` 为空接口（javap 无成员）—— **PASS**（`public interface net.minecraftforge.eventbus.api.IEventBus { }`）；
- 硬要求 4：`ForgeModFileInfo.class` 含 `configOf` —— **PASS**（`static net.neoforged.neoforgespi.language.IConfigurable configOf(java.util.Map<…>)`）；
- 弹性项：条目总数 = **25**（记录值；全量清单已归档 `2Ac-v1.1-jar-listing.txt`）；
- 归档：`build\forgecompat-002-0b-2a-v1.1.jar`（Run C / Run T 共用同一 jar；未烧入开关值）。

**与 §2.2 "唯一变量" 的逐条对照**：源码层功能性改动仅一处 —— `<init>` 描述符常量替换（`method.desc = NEW_DESC`）；其余全部为 observer 前置 / 观察日志 / 开关逻辑。

---

## 三、服务共存证据（§2.6 / "未干扰 FML"）

**ITransformationService 8 方法逐一处置**（2A-b javap 原文，v1 取证复用；存档 `2Ab-ITransformationService.txt`）：

| # | 方法 | 接口侧 | 探针处置 | 默认行为原文 |
|---|---|---|---|---|
| 1 | `name()` | abstract | 实现 → `"forgecompat-transform"` | — |
| 2 | `initialize(IEnvironment)` | abstract | 实现（1 行日志，无副作用） | — |
| 3 | `onLoad(IEnvironment, Set<String>)` | abstract | 实现（1 行日志，无副作用） | — |
| 4 | `transformers()` | abstract | 实现 → `List.of(new ForgeProbeConstructorTransformer())` | — |
| 5 | `arguments(BiFunction<…>)` | default | **不覆写** | `0: return` |
| 6 | `argumentValues(OptionResult)` | default | **不覆写** | `0: return` |
| 7 | `beginScanning(IEnvironment)` | default | **不覆写** | `List.of()` |
| 8 | `completeScan(IModuleLayerManager)` | default | **不覆写** | `List.of()` |

**4 个 service 声明文件**（jar 内验证）：`IModFileReader` / `IModFileCandidateLocator` / `IModLanguageLoader` / `ITransformationService`。

**ModLauncher services 启动列表原文（v1.1 Run C stdout line 172–174）**：

```
		modlauncher-11.0.5.jar mixin TRANSFORMATIONSERVICE 
		modlauncher-11.0.5.jar forgecompat-transform TRANSFORMATIONSERVICE 
		modlauncher-11.0.5.jar fml TRANSFORMATIONSERVICE 
```

**"Found additional transformation services" 行原文**（v1.1 Run T debug.log）：

```
[0710月2026 17:29:28.273] [main/DEBUG] [cpw.mods.modlauncher.TransformationServicesHandler/MODLAUNCHER]: Found additional transformation services from discovery services: [D:\DualForge\spike000\neoforge-server\mods\forgecompat.jar]
```

**service 日志行原文（两 run，行号并列）**：

```
Run C（rewrite=false）                          Run T（rewrite=true）
line  5: [XFORM] onLoad() called, …             line  5: [XFORM] onLoad() called, …
line  7: [XFORM] initialize() called, …         line  7: [XFORM] initialize() called, …
line 57: [LANG] <clinit> ForgeCompatLanguageLoader   （同左）
line 58: [LANG] INSTANTIATED ForgeCompatLanguageLoader（同左）
line 59: [XFORM] transformers() called          line 59: [XFORM] transformers() called
line 60: [XFORM] rewriteEnabled=false           line 60: [XFORM] rewriteEnabled=true
```

**判定**：未干扰 FML 的结构证据 = **YES**（`fml` / `mixin` 行不变；服务被登记且被驱动；无任何异常源自探针代码）。

## 四、转换链运行时证据（§2.4）

**Run C（rewrite=false）**：

- offered 清单（逐条原文，共 1 条）：

```
[FORGECOMPAT-XFORM] offered: com/example/forgeprobe/ForgeProbe
```

- rewrite target count：**0**（全文无 REWRITE 行；无 post-check；无 dump —— 符合预期）

**Run T（rewrite=true）**：

- offered 清单（逐条原文，共 1 条）：

```
[FORGECOMPAT-XFORM] offered: com/example/forgeprobe/ForgeProbe
```

- REWRITE 行原文：

```
[FORGECOMPAT-XFORM] REWRITE hits=1 old=(Lnet/minecraftforge/eventbus/api/IEventBus;)V new=(Lnet/neoforged/bus/api/IEventBus;)V
```

- post-check 行原文：

```
[FORGECOMPAT-XFORM] post-check ctorCount=1 oldAbsent=true newPresent=true
```

- dump 行原文：

```
[FORGECOMPAT-XFORM] dump OK path=D:\DualForge\spike000\logs\spike002-0b-2a-v1.1\ForgeProbe.rewritten.class bytes=1162
```

- rewrite target count：**1**（硬要求满足）

**其它审计字段**：

- non-target transformed class count：**0**（无第二类被修改；also 无第二 offer）；
- 同一 internalName 重复 offered：**NO**（全 run 各 1 条 offered，无 ANOMALY）；
- 三向诊断（Run C / Run T 各一次）：

| 项 | Run C | Run T |
|---|---|---|
| 服务是否被发现 | YES | YES |
| ForgeProbe 是否被 offered | YES | YES |
| 是否发生重写 | **NO** | **YES（hits=1）** |

**因果链与时序说明（[E]）**：日志顺序显示 —— `loadMod(forgeprobe)` 返回 FMLModContainer →（FMLModContainer 构造期间）ForgeProbe 类加载 → ⑥ 转换链触发 → `offered`（Run C）/ `offered + REWRITE`（Run T）。这是 002-0b-1b"④→⑤→⑥ 同一静态调用路径"的**运行时确认**，也是 002-0b-1 证据表中 **C 项（ForgeProbe 实际进入 transform）的首次运行时确认**。

---

## 五、重写后字节码验证（2A-d；仅 Run T）

- 机制：**M1**（运行中 dump + 运行后 javap）。
- dump：`logs\spike002-0b-2a-v1.1\ForgeProbe.rewritten.class` / 1162 字节 / SHA-256 `5E2F78DB2B0909D7368A185F618C3D080C00B732279B105CD5B8010FD66D9295`
- 说明：dump 为"本 Transformer 以 `ClassWriter(0)` 序列化同一 ClassNode"所得（与 ModLauncher 实际写出的字节同源同构、**非逐字节副本**）；**[E] 最强旁证：该重写后的类在 Run T 中被真实 define 并执行**（marker + `BUS = net.neoforged.bus.EventBus@…` 均自该类的构造函数打印）。

**javap 原文（重写后，`javap -p -c -constants`）**：

```
Compiled from "ForgeProbe.java"
public class com.example.forgeprobe.ForgeProbe {
  public com.example.forgeprobe.ForgeProbe(net.neoforged.bus.api.IEventBus);
    Code:
       0: aload_0
       1: invokespecial #18                 // Method java/lang/Object."<init>":()V
       4: getstatic     #24                 // Field java/lang/System.out:Ljava/io/PrintStream;
       7: ldc           #26                 // String =================================
       9: invokevirtual #32                 // Method java/io/PrintStream.println:(Ljava/lang/String;)V
      12: getstatic     #24                 // Field java/lang/System.out:Ljava/io/PrintStream;
      15: ldc           #34                 // String FORGE PROBE CONSTRUCTOR REACHED
      17: invokevirtual #32                 // Method java/io/PrintStream.println:(Ljava/lang/String;)V
      20: getstatic     #24                 // Field java/lang/System.out:Ljava/io/PrintStream;
      23: aload_1
      24: invokestatic  #40                 // Method java/lang/String.valueOf:(Ljava/lang/Object;)Ljava/lang/String;
      27: invokedynamic #52,  0             // InvokeDynamic #0:makeConcatWithConstants:(Ljava/lang/String;)Ljava/lang/String;
      32: invokevirtual #32                 // Method java/io/PrintStream.println:(Ljava/lang/String;)V
      35: getstatic     #24                 // Field java/lang/System.out:Ljava/io/PrintStream;
      38: ldc           #26                 // String =================================
      40: invokevirtual #32                 // Method java/io/PrintStream.println:(Ljava/lang/String;)V
      43: return
}
```

**与原始 javap 逐指令比对结论**：除构造函数签名行（预期变更）外，**指令序列完全一致** —— 全部 opcode、操作数语义与偏移（0–43）逐行相同；唯一差异为常量池索引重编号（原始 `#1/#7/#13/…` → 重写版 `#18/#24/#26/…`），属 ClassWriter(0) 序列化的等价重编号，非指令改写。→ **构造函数指令主体未主动改写 = YES**

**五项强制字段**：

- 构造函数数量 = 1：**YES**
- 原签名 `(Lnet/minecraftforge/eventbus/api/IEventBus;)V`：**不存在**
- 新签名 `(Lnet/neoforged/bus/api/IEventBus;)V`：**存在**
- 未生成其它 `<init>`：**YES**
- 构造函数指令主体未主动改写：**YES**

---

## 六、服务端运行与层判定（2A-e）

### 6.1 v1 Run C vs v1.1 Run C（纪律 1 并列对照）

| 项 | v1 Run C（`spike002-0b-2a-control`，永久保留） | v1.1 Run C（`spike002-0b-2a-control-v1.1`） |
|---|---|---|
| 前置配置 | 仅空壳 IEventBus（observer / 真实 ModFileInfo 缺失） | 四项齐全（§0.3） |
| FML Language Providers | `javafml@4.0` / `minecraft@4.0` / `lowcodefml@4.0`（3 条） | `javafml@4.0` / `minecraft@4.0` / **`javafml@null`** / `lowcodefml@4.0`（4 条） |
| offered | **无**（ForgeProbe 类从未加载） | `offered: com/example/forgeprobe/ForgeProbe` |
| 到达点 | 语言提供者版本门失败（**未达层 c/d**） | **层 c PASS → 层 d FAIL** |
| 最终异常 | `needs language provider javafml:52 or above … found 4.0.45` | `RuntimeException: Mod constructor has unsupported argument …`（与 002-0a 同款） |
| 结论 | 偏差证据（前置配置缺口；v1 STOP 报告 §六） | **ANCHOR-002-0b-2A-v1.1 命中** |

### 6.2 v1.1 Run C（control）判定

- 层 d 结果：**FAIL（ANCHOR 锚点，符合预期）**
- 层 c 结果：**PASS**（空壳 IEventBus 使签名解析通过；失败点落在参数白名单）
- 异常原文（stdout line 77 / 103 / 187 同一条）：

```
java.lang.RuntimeException: Mod constructor has unsupported argument interface net.minecraftforge.eventbus.api.IEventBus. Allowed optional argument classes: ModContainer, IEventBus, FMLModContainer, Dist
```

- 判定等级：**ANCHOR-002-0b-2A-v1.1**（仅对照，不判成败）→ 满足 K1 放行条件，进入 Run T。

### 6.3 v1.1 Run T（treatment）判定

- **层 d 结果：解除** —— 全文无 `unsupported argument` 异常；构造流程越过白名单检查。
- **层 e 进入证据：marker 命中 = YES**（stdout line 70–73 原文）：

```
=================================
FORGE PROBE CONSTRUCTOR REACHED
BUS = net.neoforged.bus.EventBus@a9b007c
=================================
```

- stack trace 命中：**NO**（无异常帧；构造函数正常返回）。
- **STOP（观察范围）**：marker 出现即停止观察 —— marker 之后的运行内容（含进程终态前的其它日志）**仅作原始附件保存，不进入分析**。
- 进程终态（元数据记录，非分析项）：`EXITED_EARLY` / exitCode 0 / 12.1s。

### 6.4 原始日志文件清单

```
Run C（control）: logs\spike002-0b-2a-control-v1.1\
  ├ spike002-0b-2a-control-v1.1.stdout.log / .log / .debug.log / .stderr.log（0 字节）
  ├ spike002-0b-2a-control-v1.1.mods-list.txt / meta.json / crash-reports\
Run T（treatment）: logs\spike002-0b-2a-v1.1\
  ├ spike002-0b-2a-v1.1.stdout.log / .log / .debug.log / .stderr.log（0 字节）
  ├ spike002-0b-2a-v1.1.mods-list.txt / meta.json / crash-reports\
  └ ForgeProbe.rewritten.class（2A-d dump）
v1 历史现场（永久保留）: logs\spike002-0b-2a-control\（v1 Run C 全套）
取证存档: logs\spike002-0b-2a-artifacts\（2Aa / 2Ab / 2Ac / 2Ad / 2Ae / 2Af 全部证据）
```

## 七、与说明书的偏差

**偏差 1（冻结期文字更正，已披露）**：

- v1.1 替换 2 原文两处写作"**3 个 service 文件**"，但并列项为 4 个（IModFileReader / IModFileCandidateLocator / IModLanguageLoader / ITransformationService）；已按实际条目更正为 **4 个**（依据：基线 2 个 + v1 新增 1 个 + v1.1 新增 1 个），并在执行中实证（jar 内 4 个 service 文件齐备）。
- §八 清理清单追加的 5 项中，"`build.gradle` 的 ASM compileOnly 已移除"与既有条目（"回退为 4 条基线依赖"）重合，**未重复列入**；其余 4 项均已追加。

**偏差 2（冻结期协调性同步，已披露）**：

- 按 v1.1 执行纪律文本，将下列位置的标签与名称统一为 v1.1 变体：§三（ValidateSet ×4）、§2A-e（两 label + `ANCHOR-002-0b-2A-v1.1`）、§2A-d（dump 路径 label）、§五 / §七 / §十一（ANCHOR 名称）、§八（jar / 日志目录 / Label 计数）、§九（K1 顺序标签）。
- `Spike_002-0b Step2-A v1.md` **保留未删除**（v1 现场为其执行依据，属偏差证据链的一部分）。

**偏差 3（执行期，0 处）**：执行与 v1.1 文本逐字一致；dump 路径常量按 §2A-d 既定说明设为 v1.1 Run T dir。无其它偏差。

---

## 八、清理确认

- [x] `ForgeCompatTransformationService.java` / `ForgeProbeConstructorTransformer.java` / `ForgeCompatLanguageLoader.java` / `IEventBus.java` 已删除
- [x] `META-INF/services/cpw.mods.modlauncher.api.ITransformationService` / `META-INF/services/net.neoforged.neoforgespi.language.IModLanguageLoader` 已删除
- [x] `src\main\java\net` 空目录树已移除
- [x] `ForgeModFileInfo.java` 的 `configOf` 方法已删除（恢复基线）
- [x] `ForgeModFileInfoParser.java` 已恢复 001b-1 基线（返回自定义 `ForgeModFileInfo`）
- [x] `build.gradle` 的 ASM `compileOnly` 追加已移除（回退为 4 条基线依赖）
- [x] `build\cp.txt` 未被修改（仍为基线内容）
- [x] `build\classes` / `build\jar-root` 已清空重建（仅含基线 5 源产物）
- [x] 重建后 `forgecompat.jar` 逐条目比对 vs `forgecompat-001b1.jar`：**entries = 15，diff = 0**（10 个文件逐条目哈希一致；整包 SHA-256 `95B7398A…FBA5CE` / 13215 字节 —— 整包 SHA 因重打包而异，属既定口径）
- [x] `forgeprobe.jar` SHA-256 前后一致 = `a93a823c…74e22`（1772 字节，未修改）
- [x] `IModFileReader` / `IModFileCandidateLocator` service 与 MANIFEST 与基线一致
- [x] `mods\` 目录状态已记录（`forgeprobe.jar` + `forgecompat.jar`）
- [x] 探针 jar 归档保留：`forgecompat-001a0.jar` / `forgecompat-001b1.jar` / `forgecompat-002-0b-2a.jar` / `forgecompat-002-0b-2a-v1.1.jar`
- [x] 日志与 dump 归档保留：`logs\spike002-0b-2a-control\`（v1，永久）+ `logs\spike002-0b-2a-control-v1.1\`、`logs\spike002-0b-2a-v1.1\`（v1.1）+ `logs\spike002-0b-2a-artifacts\`
- [x] `run-server.ps1` 改动保留（ValidateSet ×4 + `-ExtraJvmArgs`；改动 diff 已记录）

---

## 九、事实标签

**[F] Source Fact**：

- 2A-b（复用）：ITransformationService = 4 abstract + 4 default；4 个 default 默认行为 = `return` / `return` / `List.of()` / `List.of()`；
- v1.1 探针 jar = 25 条；弹性预检四项硬要求全过（含 `configOf` 存在、IEventBus 无成员、4 service 齐备）；
- 重写后 javap（dump）：唯一构造函数、新签名 `(Lnet/neoforged/bus/api/IEventBus;)V`、指令 opcode/偏移与原始一致（仅常量池索引重编号）；
- 清理后 jar = 15 条 / 逐条目 diff = 0；`forgeprobe.jar` 未修改。

**[E] Experimental Fact**：

- 四项共同前置使 v1.1 Run C 到达层 d 对照点（`javafml@null` provider + `offered` + 层 d 同款异常）；
- v1.1 Run T：`rewriteEnabled=true` → `offered` → `REWRITE hits=1` → `post-check` 全过 → dump OK → **层 d 解除** → **marker 命中（层 e 进入）** → STOP；
- 两 run：服务登记（`forgecompat-transform TRANSFORMATIONSERVICE`）、被驱动、`user_jvm_args.txt` 生效、stderr 0 字节、无探针侧异常；`fml` / `mixin` 行不变。

**[H] Hypothesis**：

- 层 e 之后的生命周期（注册表 / 事件 / 后续阶段）仍需另设探针验证 —— 本 Spike 按 STOP 纪律未观察。

**[D] Design Decision（交回）**：

- 本报告与全部证据交回；002-0c 的范围由架构决策方裁定。

---

## 十、结论

- Run C 对照结论（**ANCHOR-002-0b-2A-v1.1**）：**命中**（Service PASS / ForgeProbe offered PASS / rewrite target count = 0 / 层 c PASS / 层 d FAIL 同款异常）
- Run T 结果：**PASS-002-0b-2A**
- 层 d 是否解除：**YES**
- 层 e 是否进入：**YES**（marker 命中；随即 STOP）
- 结论上限（禁止超出）：
  - `[E]` 本探针（**一条构造函数描述符重写**）下，层 d / 层 e 的实际结果；
  - `[E]` transformer 在本次运行中被实际驱动（offered / REWRITE 日志）；
  - `[E]` 未干扰 FML 的结构证据；
  - `[E]` 四项共同前置可将运行推进至层 d 锚点（v1.1 Run C）。
- 禁止从本 Spike 推导：
  - "参数注入 ABI 已兼容"（需全部 Forge 模组的构造函数参数类型均兼容）；
  - "最终 IEventBus Shim 应采用 XXX"（`[D]`，需后续证据）；
  - "Forge Mod 已能被 NeoForge 加载"（需要完整生命周期闭环；层 e 之后未观察）；
  - "全局 Forge→NeoForge 重写策略可行" / "ASM 重写方案已确定"；
  - "其它 Forge 命名空间类型（Registry / Event / Config）已兼容"。
- Confidence：**HIGH**
- Evidence：§一–§六 的原文、行号与文件；`logs\spike002-0b-2a-artifacts\` 全部存档；v1 STOP 报告（`Spike-002-0b Step2-A 执行报告.md`）
- Hypotheses still unverified：§九 `[H]` 1 项

---

## 十一、边界确认

- [x] 未修改 NeoForge / FML 核心（含 FMLModContainer / TransformStore / ClassTransformer）
- [x] 未修改 forgeprobe.jar / mods.toml / ForgeProbe 源码（哈希运行前后一致）
- [x] K1 顺序 = 先 Run C 后 Run T；未颠倒、未并发
- [x] Run C 到达预期的层 d 对照点后，才执行 Run T
- [x] 层 e 证据出现后立即 STOP（未继续观察 / 未分析 marker 之后内容）
- [x] 未执行 B 分支；未进入 002-0c
- [x] 未改动 ClassLoader / ModuleLayer 策略；未注入 IEventBus 实例；未使用 Unsafe
- [x] ASM 使用严格限定（仅一条 desc 重写；ClassWriter(0)；`compileOnly` 不打包进 JAR）
- [x] 未自行修改 v1.1 说明书任何段落
- [x] v1 现场永久保留（`logs\spike002-0b-2a-control\` 未动）；v1 说明书原文（v1.md）保留
- [x] 清理完成：entries = 15 / 逐条目 diff = 0
- [x] 报告含"与说明书的偏差"一节（§七）
