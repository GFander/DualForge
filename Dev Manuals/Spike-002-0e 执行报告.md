# Spike-002-0e 执行报告

日期：2026-10-12（Asia/Hong_Kong）。实践代理：executor_oct11。任务标识：R-0e-1。
执行依据：D:\DualForge\Dev Manuals\Spike_002-0e.md，v1.0 冻结版，SHA-256 072ADCCB6A063E1C037836769058958E08D376E807F774EC7C69F052AB7C6F2A /43468 B。

自评：**VALID-002-0e-1（P1 命中；交独立裁判复核）**。Run C 到达 Done 并由既有 harness 正常 stop；Run T 在新语句 ForgeProbe.java:14 发生 NoClassDefFoundError: net/minecraftforge/common/MinecraftForge。没有修复或追加运行。

## 一、实验环境

Minecraft 1.21.1 / NeoForge 21.1.256 / FancyModLoader 4.0.45 / ModLauncher 11.0.5+main.901c6ea8 / Microsoft OpenJDK 21.0.10+7-LTS / Windows 11 amd64。
JDK：C:\Users\Albert\.jdks\ms-21.0.10\bin。javac、javap、jar 工具版本分别为 javac 21.0.10、21.0.10、jar 21.0.10。
两轮 JVM 参数均为 -Xmx3G -Xms1G -Dforgecompat.2a.rewrite=true；CLI dedicated server。
compat 两轮固定：4CD8195F7AA03B8B595F646CDEAC16454D03D5E3862B5E2F388C820CF2FA2FEF /21180 B/25条。
Run C probe：A93A823C988BFD6A8F6EA132D189D9DE732B0E5951F4BF74980D51BB63374E22 /1772 B/7条。
Run T probe：D61224F3014F0ADFF85D10EEDEA414763C231367F08EDB99CE99DD2C8935F1C9 /1848 B/7条。

| 轮次 | status | exitCode | durationSeconds | Done |
|---|---|---|---|---|
| spike002-0e-control | DONE_STOPPED_GRACEFULLY | 0 | 24.9 | Done (0.355s)! |
| spike002-0e-treatment | EXITED_EARLY | 0 | 12.1 | null |

exitCode=0 不替代实验判据：Run T 有明确首失败。两轮 meta 原文见附录 A。

## 二、取证与冻结项复测（2E-a）

以下初检全部匹配冻结说明书；最终复测输入不变。表中 live harness 初检为旧值，许可改动后两轮固定为84C7E2C06BF0A1D2AC0D0E2D5AD20482F1357668EC8807DAD642DA55944F77D0 /8948 B；历史共享dump已先归档，运行后核对的是归档副本。

| 路径 | 初检 SHA-256 | 字节数 |
|---|---|---|
| `D:\DualForge\spike001a\compat-002-0d\build-treatment\forgecompat-002-0d.jar` | `4CD8195F7AA03B8B595F646CDEAC16454D03D5E3862B5E2F388C820CF2FA2FEF` | 21180 |
| `D:\DualForge\spike000\probe\build\forgeprobe.jar` | `A93A823C988BFD6A8F6EA132D189D9DE732B0E5951F4BF74980D51BB63374E22` | 1772 |
| `D:\DualForge\spike000\probe\src\main\java\com\example\forgeprobe\ForgeProbe.java` | `6092CC3D402F6D0132A8F15C6A298BC1AE8D4EFFBD8CD91B01C9D734FC1EBBF5` | 455 |
| `D:\DualForge\spike000\probe\src\main\resources\META-INF\mods.toml` | `B32CF69B88FBE053C2580FC739B30FEDC43D96984C72FF78CD6725C7F3A1CE6E` | 241 |
| `D:\DualForge\spike000\probe\build\jar-root\META-INF\mods.toml` | `B32CF69B88FBE053C2580FC739B30FEDC43D96984C72FF78CD6725C7F3A1CE6E` | 241 |
| `D:\DualForge\spike000\probe\build\verify\META-INF\mods.toml` | `B32CF69B88FBE053C2580FC739B30FEDC43D96984C72FF78CD6725C7F3A1CE6E` | 241 |
| `D:\DualForge\spike000\neoforge-server\libraries\net\neoforged\fancymodloader\loader\4.0.45\loader-4.0.45.jar` | `8390398E67DF23E72D85ED809685BE2752C8EC545B7CBB6AD4B70FDC81E4DCD4` | 505696 |
| `D:\DualForge\spike000\forge-server\libraries\net\minecraftforge\javafmllanguage\1.21.1-52.1.0\javafmllanguage-1.21.1-52.1.0.jar` | `D27E1F3149EA08FC0A80779FE24FA8585692C5042A34BAD153E55662486ACC15` | 19235 |
| `D:\DualForge\spike000\forge-server\libraries\net\minecraftforge\eventbus\6.2.27\eventbus-6.2.27.jar` | `D2A7BCECB80B33F90854A003DC295960263D78435D9D6016C427D711510CB999` | 58819 |
| `D:\DualForge\spike000\forge-server\libraries\net\minecraftforge\forge\1.21.1-52.1.0\forge-1.21.1-52.1.0-universal.jar` | `88EF6625122CFDB9D7B3176BD61B8C0692A191BAD0EDD77FFDDC6A765EE4979C` | 2617057 |
| `D:\DualForge\spike000\logs\harness-baseline\run-server.ps1` | `F5319A54086C5BF7EBC8441774F358BC10B74F99C90471A526CAA2D5707FADCA` | 8900 |
| `D:\DualForge\spike001a\compat\build\forgecompat-002-0b-2a-v1.1.jar` | `E27BB9659303DDEDF2EDE02D10B49CAFC586549C0EDA003A0165DF525F97A436` | 20316 |
| `D:\DualForge\Dev Manuals\Spike_002-0e.md` | `072ADCCB6A063E1C037836769058958E08D376E807F774EC7C69F052AB7C6F2A` | 43468 |
| `D:\DualForge\spike000\run-server.ps1` | `F5319A54086C5BF7EBC8441774F358BC10B74F99C90471A526CAA2D5707FADCA` | 8900 |
| `D:\DualForge\spike000\logs\spike002-0b-2a-v1.1\ForgeProbe.rewritten.class` | `5E2F78DB2B0909D7368A185F618C3D080C00B732279B105CD5B8010FD66D9295` | 1162 |

新增冻结点独立取证：universal 的 jar tf 含 net/minecraftforge/common/MinecraftForge.class；javafmllanguage/eventbus 两件均不含。
javap 原文：

```text
public static final net.minecraftforge.eventbus.api.IEventBus EVENT_BUS;
public abstract void register(java.lang.Object);

Compiled from "IEventBus.java"
public interface net.minecraftforge.eventbus.api.IEventBus {
}
```

完整三份签名与三件 jar 全条目分别见 artifacts/2Ea-MinecraftForge.txt、2Ea-IEventBus-original.txt、2Ea-IEventBus-shim.txt、2Ea-entries-{7,8,9}.txt。每条实际命令与退出码单独保存。

首轮前 server/latest 与debug SHA分别 F59F43C9615F2BCC91F54801FF634CF25893D875976D0732F8481EB1B0EA41FC、2CF844BF907F71A9EE2021B84C68E9F6E961DA63B53AA4C124F617AC7B3B69CF，与002-0d treatment归档一致；无crash-report、无java进程。两轮Label目录均不存在。固定harness仅清理这些已归档运行副本。历史其它日志未动。

## 三、探针源码变更与归档（2E-b）

工作目录：D:\DualForge\spike000\probe-002-0e。
修改前先复制原probe源码三文件到 archive/src-before，并保留 archive/forgeprobe-frozen-A93A.jar；修改后复制到 archive/src-after。
原probe/src及原jar从未编辑。前后仅ForgeProbe.java不同（455→553 B），其它两个资源哈希相同。src-before/sourceAfter逐项哈希：


```json
[
  {
    "Path": "main\\java\\com\\example\\forgeprobe\\ForgeProbe.java",
    "SHA256": "6092CC3D402F6D0132A8F15C6A298BC1AE8D4EFFBD8CD91B01C9D734FC1EBBF5",
    "Bytes": 455
  },
  {
    "Path": "main\\resources\\META-INF\\MANIFEST.MF",
    "SHA256": "AC0B123F98FC1B1D0FB78C04FB2493A58A299A6ED4D0A444100023D10DCA3382",
    "Bytes": 57
  },
  {
    "Path": "main\\resources\\META-INF\\mods.toml",
    "SHA256": "B32CF69B88FBE053C2580FC739B30FEDC43D96984C72FF78CD6725C7F3A1CE6E",
    "Bytes": 241
  }
]
```

```json
[
  {
    "Path": "main\\java\\com\\example\\forgeprobe\\ForgeProbe.java",
    "SHA256": "F735EF5C9119FC680D1A0475B8A18662EA5BC8D81694C6E4863B01DE290BB959",
    "Bytes": 553
  },
  {
    "Path": "main\\resources\\META-INF\\MANIFEST.MF",
    "SHA256": "AC0B123F98FC1B1D0FB78C04FB2493A58A299A6ED4D0A444100023D10DCA3382",
    "Bytes": 57
  },
  {
    "Path": "main\\resources\\META-INF\\mods.toml",
    "SHA256": "B32CF69B88FBE053C2580FC739B30FEDC43D96984C72FF78CD6725C7F3A1CE6E",
    "Bytes": 241
  }
]
```

行级 diff 原文：

```diff
diff --git "a/D:\\DualForge\\spike000\\probe-002-0e\\archive\\src-before/main/java/com/example/forgeprobe/ForgeProbe.java" "b/D:\\DualForge\\spike000\\probe-002-0e\\archive\\src-after/main/java/com/example/forgeprobe/ForgeProbe.java"
index 2ed0666..defd269 100644
--- "a/D:\\DualForge\\spike000\\probe-002-0e\\archive\\src-before/main/java/com/example/forgeprobe/ForgeProbe.java"
+++ "b/D:\\DualForge\\spike000\\probe-002-0e\\archive\\src-after/main/java/com/example/forgeprobe/ForgeProbe.java"
@@ -1,5 +1,6 @@
 package com.example.forgeprobe;
 
+import net.minecraftforge.common.MinecraftForge;
 import net.minecraftforge.eventbus.api.IEventBus;
 import net.minecraftforge.fml.common.Mod;
 
@@ -10,5 +11,6 @@ public class ForgeProbe {
         System.out.println("FORGE PROBE CONSTRUCTOR REACHED");
         System.out.println("BUS = " + bus);
         System.out.println("=================================");
+        MinecraftForge.EVENT_BUS.register(this);
     }
 }
```

## 四、构建记录（2E-c）

唯一一次新probe构建；未重建Run C冻结probe或compat。实际命令如下，均exit=0，输出文件为空（无编译/打包错误）；命令在调用前写入，退出码随调用完成写入。


```powershell
& 'C:\Users\Albert\.jdks\ms-21.0.10\bin\javac.exe' '--release' '21' '-encoding' 'UTF-8' '-classpath' 'D:\DualForge\spike000\forge-server\libraries\net\minecraftforge\javafmllanguage\1.21.1-52.1.0\javafmllanguage-1.21.1-52.1.0.jar;D:\DualForge\spike000\forge-server\libraries\net\minecraftforge\eventbus\6.2.27\eventbus-6.2.27.jar;D:\DualForge\spike000\forge-server\libraries\net\minecraftforge\forge\1.21.1-52.1.0\forge-1.21.1-52.1.0-universal.jar' '-d' 'D:\DualForge\spike000\probe-002-0e\build\classes' 'D:\DualForge\spike000\probe-002-0e\src\main\java\com\example\forgeprobe\ForgeProbe.java'
```

```powershell
& 'C:\Users\Albert\.jdks\ms-21.0.10\bin\jar.exe' '--create' '--file' 'D:\DualForge\spike000\probe-002-0e\build\forgeprobe.jar' '--manifest' 'D:\DualForge\spike000\probe-002-0e\src\main\resources\META-INF\MANIFEST.MF' '-C' 'D:\DualForge\spike000\probe-002-0e\build\jar-root' '.'
```

产物 D:\DualForge\spike000\probe-002-0e\build\forgeprobe.jar 与 archive/forgeprobe-002-0e.jar 同hash D61224F3014F0ADFF85D10EEDEA414763C231367F08EDB99CE99DD2C8935F1C9，1848 B。叶名保持forgeprobe.jar。

新旧jar的7条条目集合与顺序完全一致：

```text
META-INF/
META-INF/MANIFEST.MF
META-INF/mods.toml
com/
com/example/
com/example/forgeprobe/
com/example/forgeprobe/ForgeProbe.class
```

三件Forge编译依赖只在javac classpath出现，未打包或投放运行。每轮mods副本恰为本轮probe与同一compat，实际hash分别写入 artifacts/spike002-0e-{control,treatment}-runtime-mods.json。

## 五、语义级字节码对照（2E-d，纪律1）

constructor descriptor在转换前均为 (Lnet/minecraftforge/eventbus/api/IEventBus;)V。类均interfaces=0、fields=0、methods=1，@Mod("forgeprobe")与StringConcatFactory的BUS拼接配方不变。
offset 0至40所有指令（super与四次println）相同；无额外分支、异常表。唯一新增如下：

```text
43: getstatic     #33 // Field net/minecraftforge/common/MinecraftForge.EVENT_BUS:Lnet/minecraftforge/eventbus/api/IEventBus;
46: aload_0
47: invokeinterface #39, 2 // InterfaceMethod net/minecraftforge/eventbus/api/IEventBus.register:(Ljava/lang/Object;)V
52: return
```

上块是指令对照摘录（空格排版简化），双份逐字javap见附录B及artifacts。原return在43，新return在52。LineNumberTable原8:0、9:4、10:12、11:20、12:35、13:43；新9:0、10:4、11:12、12:20、13:35、14:43、15:52。前五项+1，新增语句line14、末尾return line15分别核验。

| 内容条目 | old/new | 核对 |
|---|---|---|
| MANIFEST.MF | 93 B / 93 B，05CE503A9AABBDEFB32C96960C3EF58482EC941F9E0E8F282703FF2F55FFA0D8 | 逐字节一致，含Created-By |
| mods.toml | 241 B /241 B，B32CF69B88FBE053C2580FC739B30FEDC43D96984C72FF78CD6725C7F3A1CE6E | 逐字节一致 |
| ForgeProbe.class | 1172 B→1391 B；FAE8EFCA6EB81893F8D02D4867CDA298650A5DE45359A8A05DB01CC7B6C46DDE→89051EED55F4025E7BF6B2727A4A152AD7B11BC747AE0F2CD28AEB270DDEEFD1 | 唯一内容差异 |

Run T实际rewritten dump再次javap核对：constructor参数变为NeoForge IEventBus，新增API指令仍为Forge命名空间，line14仍映射43–51。未据行号猜测精确失败BCI。

## 六、Run C（control）

实际运行命令（wrapper只作hash/目录门与命令输出留存，调用未改逻辑的harness）：

```powershell
& 'D:\DualForge\spike000\run-server.ps1' -Label 'spike002-0e-control' -ExtraJvmArgs '-Dforgecompat.2a.rewrite=true' -ModsJar 'D:\DualForge\spike000\probe\build\forgeprobe.jar' -ExtraModsJars 'D:\DualForge\spike001a\compat-002-0d\build-treatment\forgecompat-002-0d.jar'
```

原文签名片段（stdout L64–75，未重排）：

```text
[FORGECOMPAT-LANG] loadMod called for forgeprobe entrypoints=[com.example.forgeprobe.ForgeProbe]
[FORGECOMPAT-XFORM] offered: com/example/forgeprobe/ForgeProbe
[FORGECOMPAT-XFORM] REWRITE hits=1 old=(Lnet/minecraftforge/eventbus/api/IEventBus;)V new=(Lnet/neoforged/bus/api/IEventBus;)V
[FORGECOMPAT-XFORM] post-check ctorCount=1 oldAbsent=true newPresent=true
[FORGECOMPAT-XFORM] dump OK path=D:\DualForge\spike000\logs\spike002-0b-2a-v1.1\ForgeProbe.rewritten.class bytes=1162
[FORGECOMPAT-LANG] loadMod called for neoforge entrypoints=[]
[FORGECOMPAT-LANG] delegate -> builtin javafml for neoforge
FORGE PROBE CONSTRUCTOR REACHED
=================================
BUS = net.neoforged.bus.EventBus@564484f3
=================================
[00:31:29] [modloading-worker-0/INFO] [ne.ne.ne.co.NeoForgeMod/NEOFORGE-MOD]: NeoForge mod loading, version 21.1.256, for MC 1.21.1
```

| # | 冻结判据 | 本轮记录 |
|---|---|---|
| 1 | forgeprobe loadMod/entrypoints | L64，1次，原文见上 |
| 2 | offered | L65，1次 |
| 3 | REWRITE hits=1 | L66，1次 |
| 4 | ctorCount=1/oldAbsent=true/newPresent=true | L67 |
| 5 | dump bytes=1162，冻结hash | L68；5E2F78DB2B0909D7368A185F618C3D080C00B732279B105CD5B8010FD66D9295，已封存 |
| 6 | neoforge entrypoints=[] ≥1 | L69，1次 |
| 7 | delegate builtin | L70，1次 |
| 8 | marker/BUS/两分隔符 | L71–74全部；BUS @564484f3；捕获顺序如上 |
| 9 | NeoForge mod loading | L75，1次 |
| 10 | Done | L95，见下方原文 |
| 11 | 正常stop / 无crash /stderr0 | DONE_STOPPED_GRACEFULLY/0/24.9s；无crash；stderr0 B |

Done原文：

```text
[00:31:31] [Server thread/INFO] [minecraft/DedicatedServer]: Done (0.355s)! For help, type "help"
```

控制门通过后才执行T；C dump先于T归档。完整日志、meta和mods清单路径见§十二。全量latest/debug无ERROR或FATAL。control捕获顺序与源码不同，保留为已知观测，不归因。

## 七、Run T（treatment）

实际命令：

```powershell
& 'D:\DualForge\spike000\run-server.ps1' -Label 'spike002-0e-treatment' -ExtraJvmArgs '-Dforgecompat.2a.rewrite=true' -ModsJar 'D:\DualForge\spike000\probe-002-0e\build\forgeprobe.jar' -ExtraModsJars 'D:\DualForge\spike001a\compat-002-0d\build-treatment\forgecompat-002-0d.jar'
```

stdout L64–76原文（未重排）：

```text
[FORGECOMPAT-LANG] loadMod called for forgeprobe entrypoints=[com.example.forgeprobe.ForgeProbe]
[FORGECOMPAT-XFORM] offered: com/example/forgeprobe/ForgeProbe
[FORGECOMPAT-XFORM] REWRITE hits=1 old=(Lnet/minecraftforge/eventbus/api/IEventBus;)V new=(Lnet/neoforged/bus/api/IEventBus;)V
[FORGECOMPAT-XFORM] post-check ctorCount=1 oldAbsent=true newPresent=true
[FORGECOMPAT-XFORM] dump OK path=D:\DualForge\spike000\logs\spike002-0b-2a-v1.1\ForgeProbe.rewritten.class bytes=1381
[FORGECOMPAT-LANG] loadMod called for neoforge entrypoints=[]
[FORGECOMPAT-LANG] delegate -> builtin javafml for neoforge
=================================
FORGE PROBE CONSTRUCTOR REACHED
BUS = net.neoforged.bus.EventBus@7460f348
=================================
[00:32:34] [modloading-worker-0/ERROR] [ne.ne.fm.ja.FMLModContainer/LOADING]: Failed to create mod instance. ModID: forgeprobe, class com.example.forgeprobe.ForgeProbe
java.lang.NoClassDefFoundError: net/minecraftforge/common/MinecraftForge
```

首失败：[E] java.lang.NoClassDefFoundError: net/minecraftforge/common/MinecraftForge；cause为java.lang.ClassNotFoundException: net.minecraftforge.common.MinecraftForge。
构造帧：ForgeProbe.<init>(ForgeProbe.java:14)。源码line14即唯一新增语句，字节码控制流在四次println之后；本轮marker四行按源码次序被捕获。定位到语句/三指令组，不声称精确到某条BCI。
P1命中；P2本轮未观测。

FML上报同一失败：Failed to create mod instance → Failed to wait for future Mod Construction, 1 errors found → ModLoadingException: Loading errors encountered；完整原文latest.log和crash-report附于§七.1/七.2（未省略栈帧，原本的“… 15 more”保留）。
T stdout捕获的若干栈/报告行有乱序，原文件完整保留；同事件latest/crash-report为各自独立原文，不能互称逐字相同。crash文件UUID90875f65…；stdout还打印另一UUID5c3974f2…的报告，不合并为一份。

| 观测项 | 结果 |
|---|---|
| offered/REWRITE/post-check | 全部命中；hits=1 |
| 委托 | neoforge entrypoints=[] + delegate 两行均命中 |
| marker/BUS | 全命中，BUS = net.neoforged.bus.EventBus@7460f348 |
| dump | 55D81709DC8C41A8FB25189BEF87D41F5F1920C721C02B2027F9EEE764924FE8 /1381 B |
| NeoForge mod loading | 命中；只按要求记录，不据后续行扩展生命周期分析 |
| 终止 | EXITED_EARLY /0 /12.1s /doneLine=null |
| stderr | 0 B |
| mods | forgecompat-002-0d.jar + forgeprobe.jar，实际副本hash对应指定输入 |
| crash-report | crash-2026-10-12_00.32.35-fml.txt /8234 B |

分析边界锁定于本次P1；其后“Negative index in crash report handler”等信息只属完整原始附件，不修复、不把它作为另一个探针终点。未复跑。

### 七.1 Treatment latest.log 完整原文（含首失败完整栈及上报链）


```text
[1210月2026 00:32:28.232] [main/INFO] [cpw.mods.modlauncher.Launcher/MODLAUNCHER]: ModLauncher running: args [--launchTarget, forgeserver, --fml.neoForgeVersion, 21.1.256, --fml.fmlVersion, 4.0.45, --fml.mcVersion, 1.21.1, --fml.neoFormVersion, 20240808.144430, nogui]
[1210月2026 00:32:28.236] [main/INFO] [cpw.mods.modlauncher.Launcher/MODLAUNCHER]: JVM identified as Microsoft OpenJDK 64-Bit Server VM 21.0.10+7-LTS
[1210月2026 00:32:28.237] [main/INFO] [cpw.mods.modlauncher.Launcher/MODLAUNCHER]: ModLauncher 11.0.5+main.901c6ea8 starting: java version 21.0.10 by Microsoft; OS Windows 11 arch amd64 version 10.0
[1210月2026 00:32:28.376] [main/INFO] [net.neoforged.fml.loading.ImmediateWindowHandler/]: ImmediateWindowProvider not loading because launch target is forgeserver
[1210月2026 00:32:28.456] [main/INFO] [mixin/]: SpongePowered MIXIN Subsystem Version=0.8.7 Source=union:/D:/DualForge/spike000/neoforge-server/libraries/net/fabricmc/sponge-mixin/0.16.4+mixin.0.8.7/sponge-mixin-0.16.4+mixin.0.8.7.jar%2373!/ Service=ModLauncher Env=SERVER
[1210月2026 00:32:28.698] [main/INFO] [net.neoforged.fml.loading.moddiscovery.ModDiscoverer/SCAN]: Found mod file "server-1.21.1-20240808.144430-srg.jar" [locator: production server provider +net.neoforged:neoforge:21.1.256:server]
[1210月2026 00:32:28.723] [main/INFO] [net.neoforged.fml.loading.moddiscovery.ModDiscoverer/SCAN]: Found mod file "neoforge-21.1.256-universal.jar" [locator: PathBasedLocator[name=neoforge, paths=[libraries\net\neoforged\neoforge\21.1.256\neoforge-21.1.256-universal.jar]], reader: mod manifest]
[1210月2026 00:32:28.730] [main/INFO] [net.neoforged.fml.loading.moddiscovery.ModDiscoverer/SCAN]: Found mod file "forgeprobe.jar" [locator: forgecompat locator (mods/forgeprobe.jar only)]
[1210月2026 00:32:28.736] [main/INFO] [net.neoforged.fml.loading.moddiscovery.ModDiscoverer/SCAN]: Found mod file "forgeprobe.jar" [locator: {mods folder locator at D:\DualForge\spike000\neoforge-server\mods}]
[1210月2026 00:32:28.828] [main/INFO] [net.neoforged.fml.loading.moddiscovery.locators.JarInJarDependencyLocator/]: Found 2 dependencies adding them to mods collection
[1210月2026 00:32:28.828] [main/INFO] [net.neoforged.fml.loading.moddiscovery.ModDiscoverer/SCAN]: Found gamelibrary file "mixinextras-neoforge-0.5.5.jar" [parent: neoforge-21.1.256-universal.jar, locator: jarinjar, reader: mod manifest]
[1210月2026 00:32:28.828] [main/INFO] [net.neoforged.fml.loading.moddiscovery.ModDiscoverer/SCAN]: Found library file "net.neoforged.neoforge-coremods-21.1.256.jar" [parent: neoforge-21.1.256-universal.jar, locator: jarinjar, reader: mod manifest]
[1210月2026 00:32:28.829] [main/INFO] [net.neoforged.fml.loading.moddiscovery.ModDiscoverer/]: 
     Mod List:
		Name Version (Mod Id)

		Forge Probe 1.0.0 (forgeprobe)
		Minecraft 1.21.1 (minecraft)
		NeoForge 21.1.256 (neoforge)
[1210月2026 00:32:29.173] [main/INFO] [cpw.mods.modlauncher.LaunchServiceHandler/MODLAUNCHER]: Launching target 'forgeserver' with arguments [nogui]
[1210月2026 00:32:31.216] [main/INFO] [MixinExtras|Service/]: Initializing MixinExtras via com.llamalad7.mixinextras.service.MixinExtrasServiceImpl(version=0.5.5).
[1210月2026 00:32:34.992] [modloading-worker-0/ERROR] [net.neoforged.fml.javafmlmod.FMLModContainer/LOADING]: Failed to create mod instance. ModID: forgeprobe, class com.example.forgeprobe.ForgeProbe
java.lang.NoClassDefFoundError: net/minecraftforge/common/MinecraftForge
	at TRANSFORMER/forgeprobe/com.example.forgeprobe.ForgeProbe.<init>(ForgeProbe.java:14) ~[forgeprobe.jar%23122!/:?]
	at java.base/jdk.internal.reflect.DirectConstructorHandleAccessor.newInstance(DirectConstructorHandleAccessor.java:62) ~[?:?]
	at java.base/java.lang.reflect.Constructor.newInstanceWithCaller(Constructor.java:502) ~[?:?]
	at java.base/java.lang.reflect.Constructor.newInstance(Constructor.java:486) ~[?:?]
	at MC-BOOTSTRAP/fml_loader@4.0.45/net.neoforged.fml.javafmlmod.FMLModContainer.constructMod(FMLModContainer.java:122) ~[loader-4.0.45.jar%2359!/:4.0]
	at MC-BOOTSTRAP/fml_loader@4.0.45/net.neoforged.fml.ModLoader.lambda$constructMods$3(ModLoader.java:153) ~[loader-4.0.45.jar%2359!/:4.0]
	at MC-BOOTSTRAP/fml_loader@4.0.45/net.neoforged.fml.ModLoader.lambda$dispatchParallelTask$7(ModLoader.java:231) ~[loader-4.0.45.jar%2359!/:4.0]
	at java.base/java.util.concurrent.CompletableFuture.uniHandle(CompletableFuture.java:934) [?:?]
	at java.base/java.util.concurrent.CompletableFuture$UniHandle.tryFire(CompletableFuture.java:911) [?:?]
	at java.base/java.util.concurrent.CompletableFuture$Completion.exec(CompletableFuture.java:483) [?:?]
	at java.base/java.util.concurrent.ForkJoinTask.doExec(ForkJoinTask.java:387) [?:?]
	at java.base/java.util.concurrent.ForkJoinPool$WorkQueue.topLevelExec(ForkJoinPool.java:1312) [?:?]
	at java.base/java.util.concurrent.ForkJoinPool.scan(ForkJoinPool.java:1843) [?:?]
	at java.base/java.util.concurrent.ForkJoinPool.runWorker(ForkJoinPool.java:1808) [?:?]
	at java.base/java.util.concurrent.ForkJoinWorkerThread.run(ForkJoinWorkerThread.java:188) [?:?]
Caused by: java.lang.ClassNotFoundException: net.minecraftforge.common.MinecraftForge
	at java.base/jdk.internal.loader.BuiltinClassLoader.loadClass(BuiltinClassLoader.java:641) ~[?:?]
	at java.base/java.lang.ClassLoader.loadClass(ClassLoader.java:526) ~[?:?]
	at cpw.mods.securejarhandler/cpw.mods.cl.ModuleClassLoader.loadClass(ModuleClassLoader.java:216) ~[securejarhandler-3.0.8.jar:?]
	at java.base/java.lang.ClassLoader.loadClass(ClassLoader.java:526) ~[?:?]
	at cpw.mods.securejarhandler/cpw.mods.cl.ModuleClassLoader.loadClass(ModuleClassLoader.java:216) ~[securejarhandler-3.0.8.jar:?]
	at java.base/java.lang.ClassLoader.loadClass(ClassLoader.java:526) ~[?:?]
	... 15 more
[1210月2026 00:32:35.007] [modloading-worker-0/INFO] [net.neoforged.neoforge.common.NeoForgeMod/NEOFORGE-MOD]: NeoForge mod loading, version 21.1.256, for MC 1.21.1
[1210月2026 00:32:35.123] [main/FATAL] [net.neoforged.fml.ModLoader/LOADING]: Failed to wait for future Mod Construction, 1 errors found
[1210月2026 00:32:35.247] [main/ERROR] [net.minecraft.CrashReport/]: Negative index in crash report handler (0/18)
[1210月2026 00:32:35.261] [main/FATAL] [net.neoforged.neoforge.common.NeoForgeMod/]: Preparing crash report with UUID 90875f65-b87d-4a6e-90fc-d27786b3115d
[1210月2026 00:32:35.262] [main/FATAL] [net.neoforged.neoforge.server.loading.ServerModLoader/]: Crash report saved to .\crash-reports\crash-2026-10-12_00.32.35-fml.txt
[1210月2026 00:32:35.263] [main/FATAL] [net.neoforged.neoforge.common.NeoForgeMod/]: Preparing crash report with UUID 5c3974f2-9510-4aff-a51b-7a77138c8bfa
[1210月2026 00:32:35.263] [main/ERROR] [net.minecraft.server.Main/FATAL]: Failed to start the minecraft server
net.neoforged.fml.ModLoadingException: Loading errors encountered:
	- Forge Probe (forgeprobe) has failed to load correctly
	  java.lang.NoClassDefFoundError: net/minecraftforge/common/MinecraftForge

	at MC-BOOTSTRAP/fml_loader@4.0.45/net.neoforged.fml.ModLoader.waitForFuture(ModLoader.java:281) ~[loader-4.0.45.jar%2359!/:4.0]
	at MC-BOOTSTRAP/fml_loader@4.0.45/net.neoforged.fml.ModLoader.dispatchParallelTask(ModLoader.java:244) ~[loader-4.0.45.jar%2359!/:4.0]
	at MC-BOOTSTRAP/fml_loader@4.0.45/net.neoforged.fml.ModLoader.constructMods(ModLoader.java:152) ~[loader-4.0.45.jar%2359!/:4.0]
	at MC-BOOTSTRAP/fml_loader@4.0.45/net.neoforged.fml.ModLoader.gatherAndInitializeMods(ModLoader.java:142) ~[loader-4.0.45.jar%2359!/:4.0]
	at TRANSFORMER/neoforge@21.1.256/net.neoforged.neoforge.internal.CommonModLoader.begin(CommonModLoader.java:49) ~[neoforge-21.1.256-universal.jar%23121!/:?]
	at TRANSFORMER/neoforge@21.1.256/net.neoforged.neoforge.server.loading.ServerModLoader.load(ServerModLoader.java:34) ~[neoforge-21.1.256-universal.jar%23121!/:?]
	at TRANSFORMER/minecraft@1.21.1/net.minecraft.server.Main.main(Main.java:124) ~[server-1.21.1-20240808.144430-srg.jar%23120!/:?]
	at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:103) ~[?:?]
	at java.base/java.lang.reflect.Method.invoke(Method.java:580) ~[?:?]
	at MC-BOOTSTRAP/fml_loader@4.0.45/net.neoforged.fml.loading.targets.CommonLaunchHandler.runTarget(CommonLaunchHandler.java:136) ~[loader-4.0.45.jar%2359!/:4.0]
	at MC-BOOTSTRAP/fml_loader@4.0.45/net.neoforged.fml.loading.targets.CommonLaunchHandler.serverService(CommonLaunchHandler.java:128) ~[loader-4.0.45.jar%2359!/:4.0]
	at MC-BOOTSTRAP/fml_loader@4.0.45/net.neoforged.fml.loading.targets.CommonServerLaunchHandler.runService(CommonServerLaunchHandler.java:32) ~[loader-4.0.45.jar%2359!/:4.0]
	at MC-BOOTSTRAP/fml_loader@4.0.45/net.neoforged.fml.loading.targets.CommonLaunchHandler.lambda$launchService$4(CommonLaunchHandler.java:118) ~[loader-4.0.45.jar%2359!/:4.0]
	at MC-BOOTSTRAP/cpw.mods.modlauncher@11.0.5/cpw.mods.modlauncher.LaunchServiceHandlerDecorator.launch(LaunchServiceHandlerDecorator.java:30) [modlauncher-11.0.5.jar%2364!/:?]
	at MC-BOOTSTRAP/cpw.mods.modlauncher@11.0.5/cpw.mods.modlauncher.LaunchServiceHandler.launch(LaunchServiceHandler.java:53) [modlauncher-11.0.5.jar%2364!/:?]
	at MC-BOOTSTRAP/cpw.mods.modlauncher@11.0.5/cpw.mods.modlauncher.LaunchServiceHandler.launch(LaunchServiceHandler.java:71) [modlauncher-11.0.5.jar%2364!/:?]
	at MC-BOOTSTRAP/cpw.mods.modlauncher@11.0.5/cpw.mods.modlauncher.Launcher.run(Launcher.java:103) [modlauncher-11.0.5.jar%2364!/:?]
	at MC-BOOTSTRAP/cpw.mods.modlauncher@11.0.5/cpw.mods.modlauncher.Launcher.main(Launcher.java:74) [modlauncher-11.0.5.jar%2364!/:?]
	at MC-BOOTSTRAP/cpw.mods.modlauncher@11.0.5/cpw.mods.modlauncher.BootstrapLaunchConsumer.accept(BootstrapLaunchConsumer.java:26) [modlauncher-11.0.5.jar%2364!/:?]
	at MC-BOOTSTRAP/cpw.mods.modlauncher@11.0.5/cpw.mods.modlauncher.BootstrapLaunchConsumer.accept(BootstrapLaunchConsumer.java:23) [modlauncher-11.0.5.jar%2364!/:?]
	at cpw.mods.bootstraplauncher@2.0.2/cpw.mods.bootstraplauncher.BootstrapLauncher.run(BootstrapLauncher.java:210) [bootstraplauncher-2.0.2.jar:?]
	at cpw.mods.bootstraplauncher@2.0.2/cpw.mods.bootstraplauncher.BootstrapLauncher.main(BootstrapLauncher.java:69) [bootstraplauncher-2.0.2.jar:?]
```

### 七.2 crash-report 完整原文


```text
---- Minecraft Crash Report ----
// Who set us up the TNT?

Time: 2026-10-12 00:32:35
Description: Mod loading failures have occurred; consult the issue messages for more details

net.neoforged.neoforge.logging.CrashReportExtender$ModLoadingCrashException: Mod loading has failed


A detailed walkthrough of the error, its code path and all known details is as follows:
---------------------------------------------------------------------------------------

-- Head --
Thread: main
Stacktrace:
	at java.base/jdk.internal.loader.BuiltinClassLoader.loadClass(BuiltinClassLoader.java:641) ~[?:?] {}
-- Mod loading issue for: forgeprobe --
Details:
	Caused by 0: java.lang.NoClassDefFoundError: net/minecraftforge/common/MinecraftForge
		at TRANSFORMER/forgeprobe/com.example.forgeprobe.ForgeProbe.<init>(ForgeProbe.java:14) ~[forgeprobe.jar%23122!/:?] {re:classloading,xf:forgecompat-transform:default}
		at java.base/jdk.internal.reflect.DirectConstructorHandleAccessor.newInstance(DirectConstructorHandleAccessor.java:62) ~[?:?] {}
		at java.base/java.lang.reflect.Constructor.newInstanceWithCaller(Constructor.java:502) ~[?:?] {}
		at java.base/java.lang.reflect.Constructor.newInstance(Constructor.java:486) ~[?:?] {}
		at MC-BOOTSTRAP/fml_loader@4.0.45/net.neoforged.fml.javafmlmod.FMLModContainer.constructMod(FMLModContainer.java:122) ~[loader-4.0.45.jar%2359!/:4.0] {}
		at MC-BOOTSTRAP/fml_loader@4.0.45/net.neoforged.fml.ModLoader.lambda$constructMods$3(ModLoader.java:153) ~[loader-4.0.45.jar%2359!/:4.0] {}
		at MC-BOOTSTRAP/fml_loader@4.0.45/net.neoforged.fml.ModLoader.lambda$dispatchParallelTask$7(ModLoader.java:231) ~[loader-4.0.45.jar%2359!/:4.0] {}
		at java.base/java.util.concurrent.CompletableFuture.uniHandle(CompletableFuture.java:934) ~[?:?] {}
		at java.base/java.util.concurrent.CompletableFuture$UniHandle.tryFire(CompletableFuture.java:911) ~[?:?] {}
		at java.base/java.util.concurrent.CompletableFuture$Completion.exec(CompletableFuture.java:483) ~[?:?] {}
		at java.base/java.util.concurrent.ForkJoinTask.doExec(ForkJoinTask.java:387) ~[?:?] {}
		at java.base/java.util.concurrent.ForkJoinPool$WorkQueue.topLevelExec(ForkJoinPool.java:1312) ~[?:?] {}
		at java.base/java.util.concurrent.ForkJoinPool.scan(ForkJoinPool.java:1843) ~[?:?] {}
		at java.base/java.util.concurrent.ForkJoinPool.runWorker(ForkJoinPool.java:1808) ~[?:?] {}
		at java.base/java.util.concurrent.ForkJoinWorkerThread.run(ForkJoinWorkerThread.java:188) ~[?:?] {}

	Mod file: /D:/DualForge/spike000/neoforge-server/mods/forgeprobe.jar
	Failure message: Forge Probe (forgeprobe) has failed to load correctly
		java.lang.NoClassDefFoundError: net/minecraftforge/common/MinecraftForge
	Mod version: 1.0.0
	Mod issues URL: <No issues URL found>
	Exception message: java.lang.ClassNotFoundException: net.minecraftforge.common.MinecraftForge
Stacktrace:
	at java.base/jdk.internal.loader.BuiltinClassLoader.loadClass(BuiltinClassLoader.java:641) ~[?:?] {}
	at java.base/java.lang.ClassLoader.loadClass(ClassLoader.java:526) ~[?:?] {}
	at cpw.mods.securejarhandler/cpw.mods.cl.ModuleClassLoader.loadClass(ModuleClassLoader.java:216) ~[securejarhandler-3.0.8.jar:?] {}
	at java.base/java.lang.ClassLoader.loadClass(ClassLoader.java:526) ~[?:?] {}
	at cpw.mods.securejarhandler/cpw.mods.cl.ModuleClassLoader.loadClass(ModuleClassLoader.java:216) ~[securejarhandler-3.0.8.jar:?] {}
	at java.base/java.lang.ClassLoader.loadClass(ClassLoader.java:526) ~[?:?] {}
	at TRANSFORMER/forgeprobe/com.example.forgeprobe.ForgeProbe.<init>(ForgeProbe.java:14) ~[forgeprobe.jar%23122!/:?] {re:classloading,xf:forgecompat-transform:default}
	at java.base/jdk.internal.reflect.DirectConstructorHandleAccessor.newInstance(DirectConstructorHandleAccessor.java:62) ~[?:?] {}
	at java.base/java.lang.reflect.Constructor.newInstanceWithCaller(Constructor.java:502) ~[?:?] {}
	at java.base/java.lang.reflect.Constructor.newInstance(Constructor.java:486) ~[?:?] {}
	at MC-BOOTSTRAP/fml_loader@4.0.45/net.neoforged.fml.javafmlmod.FMLModContainer.constructMod(FMLModContainer.java:122) ~[loader-4.0.45.jar%2359!/:4.0] {}
	at MC-BOOTSTRAP/fml_loader@4.0.45/net.neoforged.fml.ModLoader.lambda$constructMods$3(ModLoader.java:153) ~[loader-4.0.45.jar%2359!/:4.0] {}
	at MC-BOOTSTRAP/fml_loader@4.0.45/net.neoforged.fml.ModLoader.lambda$dispatchParallelTask$7(ModLoader.java:231) ~[loader-4.0.45.jar%2359!/:4.0] {}
	at java.base/java.util.concurrent.CompletableFuture.uniHandle(CompletableFuture.java:934) ~[?:?] {}
	at java.base/java.util.concurrent.CompletableFuture$UniHandle.tryFire(CompletableFuture.java:911) ~[?:?] {}
	at java.base/java.util.concurrent.CompletableFuture$Completion.exec(CompletableFuture.java:483) ~[?:?] {}
	at java.base/java.util.concurrent.ForkJoinTask.doExec(ForkJoinTask.java:387) ~[?:?] {}
	at java.base/java.util.concurrent.ForkJoinPool$WorkQueue.topLevelExec(ForkJoinPool.java:1312) ~[?:?] {}
	at java.base/java.util.concurrent.ForkJoinPool.scan(ForkJoinPool.java:1843) ~[?:?] {}
	at java.base/java.util.concurrent.ForkJoinPool.runWorker(ForkJoinPool.java:1808) ~[?:?] {}
	at java.base/java.util.concurrent.ForkJoinWorkerThread.run(ForkJoinWorkerThread.java:188) ~[?:?] {}


-- System Details --
Details:
	Minecraft Version: 1.21.1
	Minecraft Version ID: 1.21.1
	Operating System: Windows 11 (amd64) version 10.0
	Java Version: 21.0.10, Microsoft
	Java VM Version: OpenJDK 64-Bit Server VM (mixed mode, sharing), Microsoft
	Memory: 622509624 bytes (593 MiB) / 1073741824 bytes (1024 MiB) up to 3221225472 bytes (3072 MiB)
	CPUs: 24
	Processor Vendor: GenuineIntel
	Processor Name: Intel(R) Core(TM) Ultra 9 275HX
	Identifier: Intel64 Family 6 Model 198 Stepping 2
	Microarchitecture: unknown
	Frequency (GHz): 3.07
	Number of physical packages: 1
	Number of physical CPUs: 24
	Number of logical CPUs: 24
	Graphics card #0 name: Intel(R) Graphics
	Graphics card #0 vendor: Intel Corporation
	Graphics card #0 VRAM (MiB): 2048.00
	Graphics card #0 deviceId: VideoController1
	Graphics card #0 versionInfo: 32.0.101.8331
	Graphics card #1 name: NVIDIA GeForce RTX 5080 Laptop GPU
	Graphics card #1 vendor: NVIDIA
	Graphics card #1 VRAM (MiB): 16303.00
	Graphics card #1 deviceId: VideoController2
	Graphics card #1 versionInfo: 32.0.16.1656
	Memory slot #0 capacity (MiB): 16384.00
	Memory slot #0 clockSpeed (GHz): 6.40
	Memory slot #0 type: Unknown
	Memory slot #1 capacity (MiB): 16384.00
	Memory slot #1 clockSpeed (GHz): 6.40
	Memory slot #1 type: Unknown
	Virtual memory max (MiB): 64189.37
	Virtual memory used (MiB): 42401.83
	Swap memory total (MiB): 32000.00
	Swap memory used (MiB): 101.70
	Space in storage for jna.tmpdir (MiB): <path not set>
	Space in storage for org.lwjgl.system.SharedLibraryExtractPath (MiB): <path not set>
	Space in storage for io.netty.native.workdir (MiB): <path not set>
	Space in storage for java.io.tmpdir (MiB): available: 60741.25, total: 411282.00
	Space in storage for workdir (MiB): available: 176081.13, total: 563201.00
	JVM Flags: 2 total; -Xmx3G -Xms1G
	ModLauncher: 11.0.5+main.901c6ea8
	ModLauncher launch target: forgeserver
	ModLauncher services: 
		sponge-mixin-0.16.4+mixin.0.8.7.jar mixin PLUGINSERVICE 
		loader-4.0.45.jar slf4jfixer PLUGINSERVICE 
		loader-4.0.45.jar runtime_enum_extender PLUGINSERVICE 
		at-modlauncher-10.0.1.jar accesstransformer PLUGINSERVICE 
		loader-4.0.45.jar runtimedistcleaner PLUGINSERVICE 
		modlauncher-11.0.5.jar mixin TRANSFORMATIONSERVICE 
		modlauncher-11.0.5.jar forgecompat-transform TRANSFORMATIONSERVICE 
		modlauncher-11.0.5.jar fml TRANSFORMATIONSERVICE 
	FML Language Providers: 
		javafml@4.0
		lowcodefml@4.0
		minecraft@4.0
		javafml@null
	Mod List: 
		forgeprobe.jar                                    |Forge Probe                   |forgeprobe                    |1.0.0               |Manifest: NOSIGNATURE
		server-1.21.1-20240808.144430-srg.jar             |Minecraft                     |minecraft                     |1.21.1              |Manifest: NOSIGNATURE
		neoforge-21.1.256-universal.jar                   |NeoForge                      |neoforge                      |21.1.256            |Manifest: NOSIGNATURE
	Crash Report UUID: 90875f65-b87d-4a6e-90fc-d27786b3115d
	FML: 4.0.45
	NeoForge: 21.1.256
```

## 八、判据自评

自评 **VALID-002-0e-1**，独立裁判复核前不作为最终裁定。

- §7.1冻结输入初/终检一致、静态门通过；Run C全11项通过。
- Run T probe/委托/marker均命中；首确认失败P1语义精确匹配。
- ForgeProbe.java:14、LineNumberTable及变更前后/rewritten字节码把失败闭合到唯一新增语句；不伪称BCI级定位。
- 两轮dump已分别封存；meta、完整首失败/cause及ModLoadingException上报链存在，满足§7.2。
- 唯一内容差异符合预登记；捕获乱序是已登记限制。
- 没有触发其它首失败、输入漂移或观测不足STOP门。

结论上限：[E] 在本次固定compat新基线、descriptor重写与K1条件下，新增的首次Forge API语句在ForgeProbe.java:14以P1失败；未观测P2。不推导Forge API整体可用性/兼容层可行性/修复方向，也未证明register方法已执行。

## 九、事实标签

[F] 字节码/签名直接证据：四次println与super不变、唯一新增三指令、constructor descriptor与行号映射、EVENT_BUS/register签名、空壳IEventBus无成员。
[E] hash复测、构建exit0、K1运行、marker、P1/cause、完整上报链、进程终止元数据。
[H] stdout行乱序成因、具体错误发生的精确BCI、P2在不同输入下的条件、后续修复可行性仍未验证。
[D] 依冻结v1.0单变量/K1/首失败界限执行；自评交裁判，不决定下一Spike。
[F-j] 本轮未新增跨JDK规范事实。

## 十、与说明书的偏差及限制

1. 构建/静态取证在2026-10-11完成，因会话中断至2026-10-12恢复。运行前重新核对说明书SHA、两个probe、compat、harness及历史dump，不重复构建；两轮运行同一连续执行阶段完成。未改变冻结输入。
2. 新增工作目录内 evidence.ps1 / run-once.ps1 仅为hash门、构建命令/退出码记录及运行编排；实际实验harness只改两Label，已机械diff。报告/证据归档是许可输出，不是运行变量。
3. stdout捕获乱序保留，未排序、未推断成因。控制轮marker与分隔符次序变化；T部分stack顺序与latest不同。因冻结v1.0明确以控制流+帧+marker命中判断，此项不构成新增语义偏差。
4. 完整字节级原始日志为磁盘附件；报告嵌入引用保留文本，不替代带hash原文件。失败后附件只用于完整栈/上报/终止记录，不分析之后生命周期。
5. 本轮由独立实践代理执行，未由总指挥代行。执行边界无偏离；未改旧报告/冻结说明书/手册。

## 十一、边界自检

- [x] compat jar、四项前置、descriptor重写逻辑未改
- [x] 冻结probe jar与probe原源码/mods.toml未改
- [x] 工作副本仅1行import +1行语句
- [x] 新probe叶名forgeprobe.jar，7条目，仅一个class
- [x] 未修改NeoForge/FML/ModLauncher/Minecraft/Java或JVM参数
- [x] harness只有ValidateSet两Label变化，机械diff在附录C
- [x] 三件compile-only Forge依赖未进入运行mods或新jar
- [x] 首失败未修复、未进入方向3、未追加API
- [x] Control先于treatment，C硬门通过后T
- [x] 历史dump及C/T逐轮dump均已独立封存
- [x] 源码改前/改后完整归档，终检原始输入未变
- [x] Run T首失败后只采集完整附件，未复跑
- [x] 不删除本轮现场；server mods保留最后T运行副本，jar基线本体在原路径

## 十二、原始产物清单

原始运行目录：
- D:\DualForge\spike000\logs\spike002-0e-control（7文件）
- D:\DualForge\spike000\logs\spike002-0e-treatment（8文件，含crash-report）
- D:\DualForge\spike000\logs\spike002-0e-artifacts（83文件，命令/退出码/原始dump/复测与核对）
新probe及源码归档位于 D:\DualForge\spike000\probe-002-0e\archive；构建产物在build；所有引用使用已记录hash。

以下为报告生成前清点的98份证据/日志，包含零字节stderr：

| 文件 | 字节数 | SHA-256 |
|---|---|---|
| `D:\DualForge\spike000\logs\spike002-0e-artifacts\2Ea-entries-7.command.txt` | 185 | `EC06FC73D3F0B28E430C207B0F242E9DA27ED6376239D2889C11D35761A63937` |
| `D:\DualForge\spike000\logs\spike002-0e-artifacts\2Ea-entries-7.exit.txt` | 3 | `13BF7B3039C63BF5A50491FA3CFD8EB4E699D1BA1436315AEF9CBE5711530354` |
| `D:\DualForge\spike000\logs\spike002-0e-artifacts\2Ea-entries-7.txt` | 753 | `7AD0D40094B1A07BD5ECB6464CC907ED71C732108A79761C50AD6BC5FAB30B4C` |
| `D:\DualForge\spike000\logs\spike002-0e-artifacts\2Ea-entries-8.command.txt` | 157 | `0C5FE971BA675554DC4BFE08D739E212E417BBBE8E61266D157E1EAB18612958` |
| `D:\DualForge\spike000\logs\spike002-0e-artifacts\2Ea-entries-8.exit.txt` | 3 | `13BF7B3039C63BF5A50491FA3CFD8EB4E699D1BA1436315AEF9CBE5711530354` |
| `D:\DualForge\spike000\logs\spike002-0e-artifacts\2Ea-entries-8.txt` | 2332 | `22806656579286BA65D97CF5CB7AB550F994598BB3F5ED0B81DE1DE5A289CA44` |
| `D:\DualForge\spike000\logs\spike002-0e-artifacts\2Ea-entries-9.command.txt` | 175 | `7FBB9F8409DC121537C27B480912C9DA812F429930D48559647A94A224F95648` |
| `D:\DualForge\spike000\logs\spike002-0e-artifacts\2Ea-entries-9.exit.txt` | 3 | `13BF7B3039C63BF5A50491FA3CFD8EB4E699D1BA1436315AEF9CBE5711530354` |
| `D:\DualForge\spike000\logs\spike002-0e-artifacts\2Ea-entries-9.txt` | 123549 | `6F02E930096A964C441DF8C79D0BB6C89253AD7996FB487A3D8A901125CF86CA` |
| `D:\DualForge\spike000\logs\spike002-0e-artifacts\2Ea-frozen-refs-recheck.txt` | 3173 | `0F78AF79039D8A05C05C13E24AB342773F1C5BBE50CD144F3EE7FEEB258A6DB2` |
| `D:\DualForge\spike000\logs\spike002-0e-artifacts\2Ea-IEventBus-original.command.txt` | 216 | `EB01399D8BA407675BBD6A66673BA2F04716B9759B1AED8E78127EE68B48D73F` |
| `D:\DualForge\spike000\logs\spike002-0e-artifacts\2Ea-IEventBus-original.exit.txt` | 3 | `13BF7B3039C63BF5A50491FA3CFD8EB4E699D1BA1436315AEF9CBE5711530354` |
| `D:\DualForge\spike000\logs\spike002-0e-artifacts\2Ea-IEventBus-original.txt` | 2202 | `65FC3366E380224E9EBD19CC538C7613A0C25486D7C1F7472953654FD1966531` |
| `D:\DualForge\spike000\logs\spike002-0e-artifacts\2Ea-IEventBus-shim.command.txt` | 192 | `032D6A6428AFC6ED133AEFEFB0BAF5476B0537AFFB1CED02D3E281D941C91CD7` |
| `D:\DualForge\spike000\logs\spike002-0e-artifacts\2Ea-IEventBus-shim.exit.txt` | 3 | `13BF7B3039C63BF5A50491FA3CFD8EB4E699D1BA1436315AEF9CBE5711530354` |
| `D:\DualForge\spike000\logs\spike002-0e-artifacts\2Ea-IEventBus-shim.txt` | 97 | `3095F147CF148E73DE4BC4C2B655C829797CF48D735EDE6983AB40A6E6B97CA4` |
| `D:\DualForge\spike000\logs\spike002-0e-artifacts\2Ea-jar-version.command.txt` | 62 | `834D113CBAC27AA75EBDAE4279FBFB582A7F7AE54E820DA8122D7216EED799EC` |
| `D:\DualForge\spike000\logs\spike002-0e-artifacts\2Ea-jar-version.exit.txt` | 3 | `13BF7B3039C63BF5A50491FA3CFD8EB4E699D1BA1436315AEF9CBE5711530354` |
| `D:\DualForge\spike000\logs\spike002-0e-artifacts\2Ea-jar-version.txt` | 13 | `A4FB3A376936345A9D1C1869477620594B23CD170955EAA1FB016B588EB23634` |
| `D:\DualForge\spike000\logs\spike002-0e-artifacts\2Ea-javac-version.command.txt` | 63 | `3AB6E3CC3CD740AD0CC75B5BF22526A21ADAB0F365BEF9AF3F9EE29D08E86B26` |
| `D:\DualForge\spike000\logs\spike002-0e-artifacts\2Ea-javac-version.exit.txt` | 3 | `13BF7B3039C63BF5A50491FA3CFD8EB4E699D1BA1436315AEF9CBE5711530354` |
| `D:\DualForge\spike000\logs\spike002-0e-artifacts\2Ea-javac-version.txt` | 15 | `E3502B01AB6C5E9D017090C3DA055226F39CA5DD6705047FDAB9FDFDCD2B3335` |
| `D:\DualForge\spike000\logs\spike002-0e-artifacts\2Ea-javap-version.command.txt` | 63 | `141FF8DD1C5C97A069D61E5F8D3B65541F895CCB410D338A29ABCA001DEF25EF` |
| `D:\DualForge\spike000\logs\spike002-0e-artifacts\2Ea-javap-version.exit.txt` | 3 | `13BF7B3039C63BF5A50491FA3CFD8EB4E699D1BA1436315AEF9CBE5711530354` |
| `D:\DualForge\spike000\logs\spike002-0e-artifacts\2Ea-javap-version.txt` | 9 | `52DDD3BCFD1D4A9402B8C25D49F4D965B6965A0772BCE9A2933C72FAC74CC707` |
| `D:\DualForge\spike000\logs\spike002-0e-artifacts\2Ea-MinecraftForge.command.txt` | 233 | `CDD55B95AC89E2C55CDE2C2B09626E266B2783CA8FDA5A0F523AA8377229F53D` |
| `D:\DualForge\spike000\logs\spike002-0e-artifacts\2Ea-MinecraftForge.exit.txt` | 3 | `13BF7B3039C63BF5A50491FA3CFD8EB4E699D1BA1436315AEF9CBE5711530354` |
| `D:\DualForge\spike000\logs\spike002-0e-artifacts\2Ea-MinecraftForge.txt` | 1166 | `AEA674B741C83D22C3B30F4B4D773F43992ABA37F5732784786F7948BD4C7F60` |
| `D:\DualForge\spike000\logs\spike002-0e-artifacts\2Eb-harness-diff.command.txt` | 161 | `F9D7F999684DDF41B68C036681CC3C2488B8D1EF34606A4DC252B9F784ED48D9` |
| `D:\DualForge\spike000\logs\spike002-0e-artifacts\2Eb-harness-diff.exit.txt` | 3 | `F1B2F662800122BED0FF255693DF89C4487FBDCF453D3524A42D4EC20C3D9C04` |
| `D:\DualForge\spike000\logs\spike002-0e-artifacts\2Eb-harness-diff.txt` | 1336 | `7541EA9CFD343BF7E3857F983C4D0E16F20A097E9C2F61C82C755855AFBCDDC2` |
| `D:\DualForge\spike000\logs\spike002-0e-artifacts\2Eb-source-diff.command.txt` | 172 | `E184842E72CDBF1C4874C6FA4EDCBF73230C7728A95D9F338F4DB8BE23FC53A8` |
| `D:\DualForge\spike000\logs\spike002-0e-artifacts\2Eb-source-diff.exit.txt` | 3 | `F1B2F662800122BED0FF255693DF89C4487FBDCF453D3524A42D4EC20C3D9C04` |
| `D:\DualForge\spike000\logs\spike002-0e-artifacts\2Eb-source-diff.txt` | 989 | `ED8068C7AD3138F16C84194AB702AF68989EBAAFE8C4EC3261CBA76F3EABF821` |
| `D:\DualForge\spike000\logs\spike002-0e-artifacts\2Ec-jar.command.txt` | 279 | `DD802A119B008368A0EC4BFAA8AF65CC8B1490B527B72CF1888EC2FC6750FEA0` |
| `D:\DualForge\spike000\logs\spike002-0e-artifacts\2Ec-jar.exit.txt` | 3 | `13BF7B3039C63BF5A50491FA3CFD8EB4E699D1BA1436315AEF9CBE5711530354` |
| `D:\DualForge\spike000\logs\spike002-0e-artifacts\2Ec-jar.txt` | 2 | `7EB70257593DA06F682A3DDDA54A9D260D4FC514F645237F5CA74B08F8DA61A6` |
| `D:\DualForge\spike000\logs\spike002-0e-artifacts\2Ec-javac.command.txt` | 596 | `BEB0B0ACF51997078C694C569407BDB56BBA1AA60FFA4824B1070946F5C5BB8F` |
| `D:\DualForge\spike000\logs\spike002-0e-artifacts\2Ec-javac.exit.txt` | 3 | `13BF7B3039C63BF5A50491FA3CFD8EB4E699D1BA1436315AEF9CBE5711530354` |
| `D:\DualForge\spike000\logs\spike002-0e-artifacts\2Ec-javac.txt` | 2 | `7EB70257593DA06F682A3DDDA54A9D260D4FC514F645237F5CA74B08F8DA61A6` |
| `D:\DualForge\spike000\logs\spike002-0e-artifacts\2Ec-new-entries.command.txt` | 113 | `0593CDB4B431DEB30C061D5A0BE72D078329E1CAA42D18E16F7073E20AE8989D` |
| `D:\DualForge\spike000\logs\spike002-0e-artifacts\2Ec-new-entries.exit.txt` | 3 | `13BF7B3039C63BF5A50491FA3CFD8EB4E699D1BA1436315AEF9CBE5711530354` |
| `D:\DualForge\spike000\logs\spike002-0e-artifacts\2Ec-new-entries.txt` | 139 | `8AC3CDBD501A1477DEB8F6EEE292F78862E4509A63F74D583636BB9024F28E53` |
| `D:\DualForge\spike000\logs\spike002-0e-artifacts\2Ec-old-entries.command.txt` | 106 | `07E62ED2A357A70DFB8321B4347FF7AFC7DAC6C2AF4D5E1496B846E7D6EE1D25` |
| `D:\DualForge\spike000\logs\spike002-0e-artifacts\2Ec-old-entries.exit.txt` | 3 | `13BF7B3039C63BF5A50491FA3CFD8EB4E699D1BA1436315AEF9CBE5711530354` |
| `D:\DualForge\spike000\logs\spike002-0e-artifacts\2Ec-old-entries.txt` | 139 | `8AC3CDBD501A1477DEB8F6EEE292F78862E4509A63F74D583636BB9024F28E53` |
| `D:\DualForge\spike000\logs\spike002-0e-artifacts\2Ec-output-fingerprints.json` | 563 | `FF1B3A5DF2988C10E006CAA00AEE430C4723B65CAA0E35C96F7BEB78F0307C38` |
| `D:\DualForge\spike000\logs\spike002-0e-artifacts\2Ed-content-compare.json` | 1053 | `EA6E85DFA7AABB383B0D91DDAEA2BE9299545882ED5B09B06C424EFE231A87FA` |
| `D:\DualForge\spike000\logs\spike002-0e-artifacts\2Ed-new-javap.command.txt` | 182 | `248E952BBC19CCE567DCFF7D8BC950B0862FAE25123441F44773D130888E63D3` |
| `D:\DualForge\spike000\logs\spike002-0e-artifacts\2Ed-new-javap.exit.txt` | 3 | `13BF7B3039C63BF5A50491FA3CFD8EB4E699D1BA1436315AEF9CBE5711530354` |
| `D:\DualForge\spike000\logs\spike002-0e-artifacts\2Ed-new-javap.txt` | 1951 | `EBE04C878527B34EE4D9C43A0FE2562EC5EE2675E2874B74A159FA13F54AEECE` |
| `D:\DualForge\spike000\logs\spike002-0e-artifacts\2Ed-new-lines.command.txt` | 169 | `F25F5137E79A296DACCBD0950B952AE84A273006DFBC37F7B196C5BE156149A7` |
| `D:\DualForge\spike000\logs\spike002-0e-artifacts\2Ed-new-lines.exit.txt` | 3 | `13BF7B3039C63BF5A50491FA3CFD8EB4E699D1BA1436315AEF9CBE5711530354` |
| `D:\DualForge\spike000\logs\spike002-0e-artifacts\2Ed-new-lines.txt` | 326 | `BDB46AE757B1632AA804763B48882F1FBD995BFE263717D8B7BFA6AC5CCAE303` |
| `D:\DualForge\spike000\logs\spike002-0e-artifacts\2Ed-new-verbose.command.txt` | 169 | `D52E76D7CA328219B4C89A656B3C4DC2E05DCCE116DBB9DBFDA8982559CD9E4E` |
| `D:\DualForge\spike000\logs\spike002-0e-artifacts\2Ed-new-verbose.exit.txt` | 3 | `13BF7B3039C63BF5A50491FA3CFD8EB4E699D1BA1436315AEF9CBE5711530354` |
| `D:\DualForge\spike000\logs\spike002-0e-artifacts\2Ed-new-verbose.txt` | 8648 | `1E6E0B7E271201AEA575D7202906FCA98FECB80D207729F59E42752C11DDF065` |
| `D:\DualForge\spike000\logs\spike002-0e-artifacts\2Ed-old-javap.command.txt` | 175 | `DD4B1842240CD7D10EA904DAC8C8BFB95414BB172EB38CEE5F7E9E729D64911B` |
| `D:\DualForge\spike000\logs\spike002-0e-artifacts\2Ed-old-javap.exit.txt` | 3 | `13BF7B3039C63BF5A50491FA3CFD8EB4E699D1BA1436315AEF9CBE5711530354` |
| `D:\DualForge\spike000\logs\spike002-0e-artifacts\2Ed-old-javap.txt` | 1646 | `6457066683BA515F263B61D9E7A61277E2F3F9078E5F4469D46496E21C2D4D39` |
| `D:\DualForge\spike000\logs\spike002-0e-artifacts\2Ed-old-lines.command.txt` | 162 | `4CF708997AC69BDD2C6B59F1E15341773F82E27A528E90F5BCA0E4E6754A15ED` |
| `D:\DualForge\spike000\logs\spike002-0e-artifacts\2Ed-old-lines.exit.txt` | 3 | `13BF7B3039C63BF5A50491FA3CFD8EB4E699D1BA1436315AEF9CBE5711530354` |
| `D:\DualForge\spike000\logs\spike002-0e-artifacts\2Ed-old-lines.txt` | 306 | `2FCEDEE9CD6CBC6B5554E95ED2D5A65892C336093B344322188F329877551305` |
| `D:\DualForge\spike000\logs\spike002-0e-artifacts\2Ed-old-verbose.command.txt` | 162 | `F151B35B7F84889E23529270535CE043C9D288220D354F4C550C6666859B69D4` |
| `D:\DualForge\spike000\logs\spike002-0e-artifacts\2Ed-old-verbose.exit.txt` | 3 | `13BF7B3039C63BF5A50491FA3CFD8EB4E699D1BA1436315AEF9CBE5711530354` |
| `D:\DualForge\spike000\logs\spike002-0e-artifacts\2Ed-old-verbose.txt` | 7359 | `421B85BDEE27E7EBBB2983B25FE02B063C097646ECBA5A9B5408D1AAD0912F8D` |
| `D:\DualForge\spike000\logs\spike002-0e-artifacts\2Ed-static-gates.md` | 2304 | `9859B7B8378578B3D55361B98F50F821620D9C430CC5438542279486F9BF8E11` |
| `D:\DualForge\spike000\logs\spike002-0e-artifacts\2Ee-control-gate.md` | 1144 | `BD9DECB8B2CF8E25684A95345D81711F8BFAFB194F9D6828E487946C11AA5FE0` |
| `D:\DualForge\spike000\logs\spike002-0e-artifacts\2Ef-final-frozen-refs-recheck.json` | 3186 | `2E01138BE6D79AC650796897EAE612F9ADA49C829EDAA1A60CBBF8477BFAE4B1` |
| `D:\DualForge\spike000\logs\spike002-0e-artifacts\2Ef-treatment-dump-javap.command.txt` | 149 | `EAA7C7816BA727AECE71FBA68EAE8196C83B0E79DF599AEDE27A36FA3855E473` |
| `D:\DualForge\spike000\logs\spike002-0e-artifacts\2Ef-treatment-dump-javap.exit.txt` | 2 | `9A271F2A916B0B6EE6CECB2426F0B3206EF074578BE55D9BC94F6F3FE3AB86AA` |
| `D:\DualForge\spike000\logs\spike002-0e-artifacts\2Ef-treatment-dump-javap.txt` | 2060 | `9C9C4464C596C8127D9AAD071FEFC8A88E34DB4F07794B66857CC250E893C1D1` |
| `D:\DualForge\spike000\logs\spike002-0e-artifacts\historical-ForgeProbe.rewritten.class` | 1162 | `5E2F78DB2B0909D7368A185F618C3D080C00B732279B105CD5B8010FD66D9295` |
| `D:\DualForge\spike000\logs\spike002-0e-artifacts\spike002-0e-control-dump.json` | 197 | `77DB741DEAAB16FFBAE9324BAEC64715B134596C43FA4E025D585FA5CE8D1B27` |
| `D:\DualForge\spike000\logs\spike002-0e-artifacts\spike002-0e-control-runtime-mods.json` | 398 | `185933DB2D9B5F614122794F636E75A93D463F5D7ED4A489BDBE4954F2E9658B` |
| `D:\DualForge\spike000\logs\spike002-0e-artifacts\spike002-0e-control.command.txt` | 270 | `C362F1E8DB24C0744127A078E6E5B5F5DCABE5C1EF9CEC97EE08B499D6C8B316` |
| `D:\DualForge\spike000\logs\spike002-0e-artifacts\spike002-0e-control.harness-exit.txt` | 3 | `13BF7B3039C63BF5A50491FA3CFD8EB4E699D1BA1436315AEF9CBE5711530354` |
| `D:\DualForge\spike000\logs\spike002-0e-artifacts\spike002-0e-control.harness-output.txt` | 480 | `D7AF17A5404BF0BDF3750357F1B003D56345F9D8E75122A19B6FD757C590295A` |
| `D:\DualForge\spike000\logs\spike002-0e-artifacts\spike002-0e-treatment-dump.json` | 199 | `E6BA37725967E11B41218264CAF35AE44EA8C6407E43740BF701A7A5C96C8EFA` |
| `D:\DualForge\spike000\logs\spike002-0e-artifacts\spike002-0e-treatment-runtime-mods.json` | 398 | `D85B72B96A6DCD0F7B450F14DA178A41C9BAEA96765F87D5482E20C30B3B5FD1` |
| `D:\DualForge\spike000\logs\spike002-0e-artifacts\spike002-0e-treatment.command.txt` | 279 | `5AA541B1BA6A60CC39981BB43FFBF93047F4DA593FE5C894AE2FAE21AE0F284F` |
| `D:\DualForge\spike000\logs\spike002-0e-artifacts\spike002-0e-treatment.harness-exit.txt` | 3 | `13BF7B3039C63BF5A50491FA3CFD8EB4E699D1BA1436315AEF9CBE5711530354` |
| `D:\DualForge\spike000\logs\spike002-0e-artifacts\spike002-0e-treatment.harness-output.txt` | 345 | `01D4A51795BEE82367B528ACE859F1CB7A7DCE6449EF4468DD3F13A08EC1E050` |
| `D:\DualForge\spike000\logs\spike002-0e-control\ForgeProbe.rewritten.class` | 1162 | `5E2F78DB2B0909D7368A185F618C3D080C00B732279B105CD5B8010FD66D9295` |
| `D:\DualForge\spike000\logs\spike002-0e-control\meta.json` | 1396 | `E429FDF3D57EA15380E0C5AA06115F0161A7AFD35C20E69DDABC0361C3039526` |
| `D:\DualForge\spike000\logs\spike002-0e-control\spike002-0e-control.debug.log` | 81464 | `98CF16AB6D91B800EE284575532CA2159E01A03DA30572EE5B8B0CEE0C051A8B` |
| `D:\DualForge\spike000\logs\spike002-0e-control\spike002-0e-control.log` | 8316 | `6D500FFD8F5971AAC8CA1EB4D7CCC365DE63EEF10B99C18B3DCE0DAEE17B6E4D` |
| `D:\DualForge\spike000\logs\spike002-0e-control\spike002-0e-control.mods-list.txt` | 40 | `CC91CB12C66E954D3842CC4418968198DAECE280064536B274344945296AB0C9` |
| `D:\DualForge\spike000\logs\spike002-0e-control\spike002-0e-control.stderr.log` | 0 | `E3B0C44298FC1C149AFBF4C8996FB92427AE41E4649B934CA495991B7852B855` |
| `D:\DualForge\spike000\logs\spike002-0e-control\spike002-0e-control.stdout.log` | 10800 | `DD7CB1F35991CB2492A56BD43196B595DA966738B910882B1D272F0252CD15E2` |
| `D:\DualForge\spike000\logs\spike002-0e-treatment\crash-reports\crash-2026-10-12_00.32.35-fml.txt` | 8234 | `9C8BC96FC8F73A51847CD4BFF012487D9821D6E81AA8263E20BD822CB964F1DB` |
| `D:\DualForge\spike000\logs\spike002-0e-treatment\ForgeProbe.rewritten.class` | 1381 | `55D81709DC8C41A8FB25189BEF87D41F5F1920C721C02B2027F9EEE764924FE8` |
| `D:\DualForge\spike000\logs\spike002-0e-treatment\meta.json` | 1269 | `913AF25EF5F04E0DAE49E8097D779B9B705A4A1B1E54F8FB147D403CBFBA1056` |
| `D:\DualForge\spike000\logs\spike002-0e-treatment\spike002-0e-treatment.debug.log` | 79500 | `236D08BA776DE286FD6B7B4A8FC61A166D60C7AB1E975C96D40E4B6744374827` |
| `D:\DualForge\spike000\logs\spike002-0e-treatment\spike002-0e-treatment.log` | 10268 | `CB7837282D39348802ABCBC58FC2F986715DF2C7096B91AFC53C0F8E18EBC608` |
| `D:\DualForge\spike000\logs\spike002-0e-treatment\spike002-0e-treatment.mods-list.txt` | 40 | `CC91CB12C66E954D3842CC4418968198DAECE280064536B274344945296AB0C9` |
| `D:\DualForge\spike000\logs\spike002-0e-treatment\spike002-0e-treatment.stderr.log` | 0 | `E3B0C44298FC1C149AFBF4C8996FB92427AE41E4649B934CA495991B7852B855` |
| `D:\DualForge\spike000\logs\spike002-0e-treatment\spike002-0e-treatment.stdout.log` | 22190 | `C2B2C48ECDF3392AC13A1E41782F88471404FFEFC91420BB31C89C5FB7821461` |

## 附录 A：两轮 meta.json 原文

Control：

```json
{
  "label": "spike002-0e-control",
  "status": "DONE_STOPPED_GRACEFULLY",
  "exitCode": 0,
  "startedAt": "2026-10-12T00:31:21.0464813+08:00",
  "endedAt": "2026-10-12T00:31:45.9270559+08:00",
  "durationSeconds": 24.9,
  "doneLine": "[1210月2026 00:31:31.408] [Server thread/INFO] [net.minecraft.server.dedicated.DedicatedServer/]: Done (0.355s)! For help, type \"help\"\r",
  "modsDir": [
    "forgecompat-002-0d.jar",
    "forgeprobe.jar"
  ],
  "modsJar": "D:\\DualForge\\spike000\\probe\\build\\forgeprobe.jar",
  "extraModsJars": [
    "D:\\DualForge\\spike001a\\compat-002-0d\\build-treatment\\forgecompat-002-0d.jar"
  ],
  "commandLine": "C:\\Users\\Albert\\.jdks\\ms-21.0.10\\bin\\java.exe @user_jvm_args.txt @libraries/net/neoforged/neoforge/21.1.256/win_args.txt nogui",
  "workingDirectory": "D:\\DualForge\\spike000\\neoforge-server",
  "javaVersion": "openjdk version \"21.0.10\" 2026-01-20 LTS\r\nOpenJDK Runtime Environment Microsoft-13106404 (build 21.0.10+7-LTS)\r\nOpenJDK 64-Bit Server VM Microsoft-13106404 (build 21.0.10+7-LTS, mixed mode, sharing)",
  "stdoutLog": "D:\\DualForge\\spike000\\logs\\spike002-0e-control\\spike002-0e-control.stdout.log",
  "stderrLog": "D:\\DualForge\\spike000\\logs\\spike002-0e-control\\spike002-0e-control.stderr.log",
  "latestLog": "D:\\DualForge\\spike000\\logs\\spike002-0e-control\\spike002-0e-control.log"
}
```

Treatment：

```json
{
  "label": "spike002-0e-treatment",
  "status": "EXITED_EARLY",
  "exitCode": 0,
  "startedAt": "2026-10-12T00:32:26.7119280+08:00",
  "endedAt": "2026-10-12T00:32:38.8108937+08:00",
  "durationSeconds": 12.1,
  "doneLine": null,
  "modsDir": [
    "forgecompat-002-0d.jar",
    "forgeprobe.jar"
  ],
  "modsJar": "D:\\DualForge\\spike000\\probe-002-0e\\build\\forgeprobe.jar",
  "extraModsJars": [
    "D:\\DualForge\\spike001a\\compat-002-0d\\build-treatment\\forgecompat-002-0d.jar"
  ],
  "commandLine": "C:\\Users\\Albert\\.jdks\\ms-21.0.10\\bin\\java.exe @user_jvm_args.txt @libraries/net/neoforged/neoforge/21.1.256/win_args.txt nogui",
  "workingDirectory": "D:\\DualForge\\spike000\\neoforge-server",
  "javaVersion": "openjdk version \"21.0.10\" 2026-01-20 LTS\r\nOpenJDK Runtime Environment Microsoft-13106404 (build 21.0.10+7-LTS)\r\nOpenJDK 64-Bit Server VM Microsoft-13106404 (build 21.0.10+7-LTS, mixed mode, sharing)",
  "stdoutLog": "D:\\DualForge\\spike000\\logs\\spike002-0e-treatment\\spike002-0e-treatment.stdout.log",
  "stderrLog": "D:\\DualForge\\spike000\\logs\\spike002-0e-treatment\\spike002-0e-treatment.stderr.log",
  "latestLog": "D:\\DualForge\\spike000\\logs\\spike002-0e-treatment\\spike002-0e-treatment.log"
}
```

## 附录 B：新旧 javap 原文

旧probe：

```text
Compiled from "ForgeProbe.java"
public class com.example.forgeprobe.ForgeProbe {
  public com.example.forgeprobe.ForgeProbe(net.minecraftforge.eventbus.api.IEventBus);
    Code:
       0: aload_0
       1: invokespecial #1                  // Method java/lang/Object."<init>":()V
       4: getstatic     #7                  // Field java/lang/System.out:Ljava/io/PrintStream;
       7: ldc           #13                 // String =================================
       9: invokevirtual #15                 // Method java/io/PrintStream.println:(Ljava/lang/String;)V
      12: getstatic     #7                  // Field java/lang/System.out:Ljava/io/PrintStream;
      15: ldc           #21                 // String FORGE PROBE CONSTRUCTOR REACHED
      17: invokevirtual #15                 // Method java/io/PrintStream.println:(Ljava/lang/String;)V
      20: getstatic     #7                  // Field java/lang/System.out:Ljava/io/PrintStream;
      23: aload_1
      24: invokestatic  #23                 // Method java/lang/String.valueOf:(Ljava/lang/Object;)Ljava/lang/String;
      27: invokedynamic #29,  0             // InvokeDynamic #0:makeConcatWithConstants:(Ljava/lang/String;)Ljava/lang/String;
      32: invokevirtual #15                 // Method java/io/PrintStream.println:(Ljava/lang/String;)V
      35: getstatic     #7                  // Field java/lang/System.out:Ljava/io/PrintStream;
      38: ldc           #13                 // String =================================
      40: invokevirtual #15                 // Method java/io/PrintStream.println:(Ljava/lang/String;)V
      43: return
}
```

新probe（未rewrite）：

```text
Compiled from "ForgeProbe.java"
public class com.example.forgeprobe.ForgeProbe {
  public com.example.forgeprobe.ForgeProbe(net.minecraftforge.eventbus.api.IEventBus);
    Code:
       0: aload_0
       1: invokespecial #1                  // Method java/lang/Object."<init>":()V
       4: getstatic     #7                  // Field java/lang/System.out:Ljava/io/PrintStream;
       7: ldc           #13                 // String =================================
       9: invokevirtual #15                 // Method java/io/PrintStream.println:(Ljava/lang/String;)V
      12: getstatic     #7                  // Field java/lang/System.out:Ljava/io/PrintStream;
      15: ldc           #21                 // String FORGE PROBE CONSTRUCTOR REACHED
      17: invokevirtual #15                 // Method java/io/PrintStream.println:(Ljava/lang/String;)V
      20: getstatic     #7                  // Field java/lang/System.out:Ljava/io/PrintStream;
      23: aload_1
      24: invokestatic  #23                 // Method java/lang/String.valueOf:(Ljava/lang/Object;)Ljava/lang/String;
      27: invokedynamic #29,  0             // InvokeDynamic #0:makeConcatWithConstants:(Ljava/lang/String;)Ljava/lang/String;
      32: invokevirtual #15                 // Method java/io/PrintStream.println:(Ljava/lang/String;)V
      35: getstatic     #7                  // Field java/lang/System.out:Ljava/io/PrintStream;
      38: ldc           #13                 // String =================================
      40: invokevirtual #15                 // Method java/io/PrintStream.println:(Ljava/lang/String;)V
      43: getstatic     #33                 // Field net/minecraftforge/common/MinecraftForge.EVENT_BUS:Lnet/minecraftforge/eventbus/api/IEventBus;
      46: aload_0
      47: invokeinterface #39,  2           // InterfaceMethod net/minecraftforge/eventbus/api/IEventBus.register:(Ljava/lang/Object;)V
      52: return
}
```

## 附录 C：harness 机械 diff 原文


```diff
diff --git "a/D:\\DualForge\\spike000\\logs\\harness-baseline\\run-server.ps1" "b/D:\\DualForge\\spike000\\run-server.ps1"
index df8ca70..9bfbf97 100644
--- "a/D:\\DualForge\\spike000\\logs\\harness-baseline\\run-server.ps1"
+++ "b/D:\\DualForge\\spike000\\run-server.ps1"
@@ -13,7 +13,7 @@
     baseline run and the forgeprobe run is the -ModsJar argument.
 #>
 param(
-    [Parameter(Mandatory = $true)][ValidateSet('baseline', 'forgeprobe', 'spike001a-0', 'spike001b-1', 'spike001b-3', 'spike001b-3b', 'spike001b-5', 'spike001b-6', 'spike001c-0', 'spike001c-1', 'spike001c-2', 'spike002-0a', 'spike002-0b-2a-control', 'spike002-0b-2a', 'spike002-0b-2a-control-v1.1', 'spike002-0b-2a-v1.1', 'spike002-0c', 'spike002-0d-control', 'spike002-0d-treatment')][string]$Label,
+    [Parameter(Mandatory = $true)][ValidateSet('baseline', 'forgeprobe', 'spike001a-0', 'spike001b-1', 'spike001b-3', 'spike001b-3b', 'spike001b-5', 'spike001b-6', 'spike001c-0', 'spike001c-1', 'spike001c-2', 'spike002-0a', 'spike002-0b-2a-control', 'spike002-0b-2a', 'spike002-0b-2a-control-v1.1', 'spike002-0b-2a-v1.1', 'spike002-0c', 'spike002-0d-control', 'spike002-0d-treatment', 'spike002-0e-control', 'spike002-0e-treatment')][string]$Label,
     [string]$ModsJar = '',
     [string[]]$ExtraModsJars = @(),
     [string[]]$ExtraJvmArgs = @(),
```

## 交接

本报告与全部证据交 referee_oct11 独立核验，抄送总指挥。后续待裁判说明书，不自行修复P1或选择下一API。

