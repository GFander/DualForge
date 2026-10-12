# Spike-002-0b-1b 取证报告（静态链段 ④⑤⑥ + `targets()` 匹配规则）

> **结论（先行）**：**002-0b-1b 整体 PASS（PASS-a ~ PASS-g + PASS-cleanup 全部达成）**
>
> **已证 `[F]`（本轮核心结果）**：
> - **④ 类加载触发点**：`FMLModContainer.<init>` 中 `Class.forName(Module, name)`（entrypoint `Class<?>` 获取），
>   其中的 `Module` 来自 `FMLLoader.getGameLayer()`（= ModLauncher **GAME 层**）。
> - **⑤ class bytes 获取点**：GAME 层的 `ModuleClassLoader`（实例 = `TransformingClassLoader`）
>   经 `loadClass → findClass → loadFromModule → readerToClass → getClassBytes(ModuleReader, …)`
>   取得 `byte[]`，并在 `readerToClass` 内以 `invokevirtual maybeTransformClassBytes` 进入转换链。
> - **⑥ 静态调用点**：
>   通道 A = `ClassTransformer.performVote` 内
>   `invokeinterface cpw.mods.modlauncher.api.ITransformer.transform:(Ljava/lang/Object;Lcpw/mods/modlauncher/api/ITransformerVotingContext;)Ljava/lang/Object;`；
>   通道 B = `LaunchPluginHandler.offerClassNodeToPlugins` 内
>   `invokeinterface cpw.mods.modlauncher.serviceapi.ILaunchPluginService.processClassWithFlags:(…;Lorg/objectweb/asm/tree/ClassNode;Lorg/objectweb/asm/Type;Ljava/lang/String;)I`。
> - **④→⑤→⑥ 连续链 = YES（同一静态调用路径）**：全部连接边为**同一对象的 `this` 分派**或
>   **同一 GAME 层对象身份**（ModuleLayer → Module → ClassLoader），无孤立证据拼接；
>   两处 JDK / VM 边界（`Class.forName(Module,String)` 内部、`defineClass`）已按 §三 边界规则标注。
> - **`targets()` / `TargetType` 匹配规则**：**精确匹配**（ASM `Type` 归一化 + 名称/描述符 `equals`）；
>   `TransformStore.classNeedsTransforming` 存 **internal name**；四个 `TargetType`
>   （PRE_CLASS / CLASS / METHOD / FIELD）各自独立子表。
> - **ForgeProbe 静态资格**：FQCN `com.example.forgeprobe.ForgeProbe` → **YES（满足 `targets()` 匹配前提）**；
>   前提条件见 §七（必须存在声明该精确 target 的已注册 transformer，且其 `castVote` 返回 YES）。
> - **通道归属**：entrypoint 类加载路径汇入**同一 `ClassTransformer` 转换点**，A / B 两条通道都在该点被查询；
>   但 **A 的 provider 来源 = SERVICE 层（白名单可达第三方）**，
>   **B 的 provider 来源 = BOOT 层（`Launcher` 构造期 `ServiceLoader`，第三方 `mods/` JAR 静态不可达）**。
> - **静态时序**：⑥ 的注册（`Launcher.run#initialiseServiceTransformers`）**先于** GAME 层构建、
>   更先于 ④ 的 entrypoint 类加载。
>
> **未证（本 Spike 不产生 `[E]`）**：ForgeProbe 本次运行中是否真的进入 transform / 层 d 是否可被解除 /
> 层 e 是否进入 —— **均未观测（本 Spike 不运行服务端）**。
>
> **按 §九 属情形 A**：完整静态链 ④→⑤→⑥（同一路径）+ `targets()` 规则明确 + 通道归属明确 + 实际 `T` 明确
> → **交回架构决策方裁定 002-0b Step 2-A 最小实现范围**。
> 同时记录**情形 C 的限制条件**：通道 B 对第三方 `mods/` JAR 静态不可达（§八）。
> **未进入 002-0b Step 2-A**，未引入 ASM，未新增源文件，未运行服务端。

---

## 一、分析对象

| 工件 | SHA-256（本轮实测） |
|---|---|
| `loader-4.0.45.jar` | `8390398e67df23e72d85ed809685be2752c8ec545b7cbb6ad4b70fdc81e4dcd4` |
| `modlauncher-11.0.5.jar` | `fd9b9fc7cf043d2264ea0113eac6b258f0a71fece5b00e831bfbdf9726c78a24` |
| `bootstraplauncher-2.0.2.jar` | `d3f29309140540570fd6c709509706aa8fb133c1c8aef24491688f1a3f4e1d49` |
| `securejarhandler-3.0.8.jar` | `945c63d6deafc821616b0380c23867d9b8c2852438f8a6df72732ab933fc587d` |

**手段**：`jar tf` / 解包读 `META-INF/services/` + `javap -p -c -constants`（本机 JDK 24.0.2 的 `javap`
仅作**反汇编器**读取目标 JAR 的 class 文件，不改变目标字节码事实）。
**未使用 current/main 分支源码**；**未引入 ASM**；**未新增任何源文件**；**未运行服务端**。

精确环境（冻结，与 002-0a/002-0b 一致）：

```
Minecraft 1.21.1 / NeoForge 21.1.256 / FancyModLoader 4.0.45
Java 21（OpenJDK 21.0.10，Microsoft Build，C:\Users\Albert\.jdks\ms-21.0.10）/ Windows 11 amd64
```

（Java 版本依据：`spike000/logs/{baseline,spike002-0a}/meta.json` 的 `commandLine` 与 `javaVersion` 字段。）

---

## 二、通道 A · B 的 discovery · layer · classloader 条件

### 2.1 Layer 枚举与父子关系（`IModuleLayerManager$Layer`，`modlauncher-11.0.5`）

```
BOOT     parent = []
SERVICE  parent = [BOOT]
PLUGIN   parent = [BOOT]
GAME     parent = [PLUGIN, SERVICE]
```

（依据：`Layer` 的 `static {}` 中 `anewarray` 逐项放置 + 私有构造 `<init>(Layer...)` 保存 `parent` 数组，
及 `public Layer[] getParent()`。）

`ModuleLayerHandler` 的 `BOOT` 条目在 `Launcher` 侧被登记为
**ModLauncher 自身所在层**（构造器：`this.getClass().getClassLoader()`，若为 `ModuleClassLoader` 则直接复用；
否则以 `ModuleLayer.boot()` 的 configuration 新建 `ModuleClassLoader("BOOT", …)`），
即 bootstraplauncher 引导出来的 `MC-BOOTSTRAP` 层。

### 2.2 通道 A —— `ITransformationService`

| 项 | 结果（均为 `[F]`） |
|---|---|
| `META-INF/services` 声明位置 | `META-INF/services/cpw.mods.modlauncher.api.ITransformationService`（`loader-4.0.45.jar` 自身声明 `net.neoforged.fml.loading.FMLServiceProvider`） |
| 是否在 §3.6 白名单 | **YES** |
| 白名单内容（原文） | `TransformerDiscovererConstants.SERVICES = Set.of(ITransformationService, IModFileCandidateLocator, IModFileReader, IDependencyLocator, GraphicsBootstrapper, ImmediateWindowProvider)` |
| 提升后 Layer | **SERVICE 层** |
| 第三方 `mods/` JAR 可见性 | **YES（静态可达）** |

**白名单判定逻辑（`ModDirTransformerDiscoverer` + `TransformerDiscovererConstants`，javap 原文节选）**：

```
  private static boolean shouldLoadInServiceLayer(java.nio.file.Path);
        51: aload_0
        52: invokestatic  #203   // Method net/neoforged/fml/loading/TransformerDiscovererConstants.shouldLoadInServiceLayer:(Ljava/nio/file/Path;)Z
        55: ireturn

  public static boolean shouldLoadInServiceLayer(cpw.mods.jarhandling.JarContents);
         5: aload_1
         6: invokeinterface #50,  1   // InterfaceMethod cpw/mods/jarhandling/JarMetadata.providers:()Ljava/util/List;
        11: invokeinterface #54,  1   // InterfaceMethod java/util/List.stream:()Ljava/util/Stream;
        16: invokedynamic #58,  0     // InvokeDynamic #0:apply …（provider 名映射）
        26: getstatic     #68         // Field SERVICES:Ljava/util/Set;
        39: invokeinterface #82,  2   // InterfaceMethod java/util/stream/Stream.anyMatch:(Ljava/util/function/Predicate;)Z
        44: ireturn
```

**提升目标是 SERVICE 层（javap 原文，`TransformationServicesHandler.lambda$discoverServices$16`）**：

```
  private void lambda$discoverServices$16(cpw.mods.modlauncher.api.NamedPath);
         0: aload_0
         1: getfield      #13   // Field layerHandler:Lcpw/mods/modlauncher/ModuleLayerHandler;
         4: getstatic     #268  // Field cpw/mods/modlauncher/api/IModuleLayerManager$Layer.SERVICE:…
         7: aload_1
         8: invokevirtual #415  // Method cpw/mods/modlauncher/ModuleLayerHandler.addToLayer:(IModuleLayerManager$Layer;Lcpw/mods/modlauncher/api/NamedPath;)V
```

**SERVICE 层构建与实际 ServiceLoader（javap 原文）**：

```
  // TransformationServicesHandler.discoverServices 内：
       125: ... ModuleLayerHandler.buildLayer:(IModuleLayerManager$Layer;)Lcpw/mods/modlauncher/ModuleLayerHandler$LayerInfo;   // buildLayer(SERVICE)
       149: aload_0
       150: aload         5
       152: invokedynamic #277  // get:(LayerInfo) -> Supplier
       162: invokestatic  #247  // Method cpw/mods/modlauncher/util/ServiceLoaderUtils.streamServiceLoader:(Supplier;Consumer;)Ljava/util/Stream;
       165: … toMap → putfield serviceLookup

  private static java.util.ServiceLoader lambda$discoverServices$18(cpw.mods.modlauncher.ModuleLayerHandler$LayerInfo);
         0: aload_0
         1: invokevirtual #387  // Method cpw/mods/modlauncher/ModuleLayerHandler$LayerInfo.layer:()Lcpw/mods/modlauncher/ModuleLayer;
         4: ldc_w         #391  // class cpw/mods/modlauncher/api/ITransformationService
         7: invokestatic  #393  // Method java/util/ServiceLoader.load:(Ljava/lang/ModuleLayer;Ljava/lang/Class;)Ljava/util/ServiceLoader;
        10: areturn
```

SERVICE 层自身的 ClassLoader = **普通 `cpw.mods.cl.ModuleClassLoader`**（`buildLayer(SERVICE)` 默认 BiFunction
`lambda$buildLayer$6`：`new ModuleClassLoader(layer.name()+"…", configuration, parents)`），
即 **SERVICE 层不经过转换**；这满足"服务实现类必须先能被普通加载"的前提。

### 2.3 通道 B —— `ILaunchPluginService`

| 项 | 结果（均为 `[F]`） |
|---|---|
| `META-INF/services` 声明位置 | `META-INF/services/cpw.mods.modlauncher.serviceapi.ILaunchPluginService`（`loader-4.0.45.jar` 自身声明 3 个实现：`SLF4JFixerLaunchPluginService` / `RuntimeDistCleaner` / `RuntimeEnumExtender`） |
| 是否在 §3.6 白名单 | **NO** |
| 不被白名单提升时的实际发现机制 | `ServiceLoader.load(BOOT 层, ILaunchPluginService.class)` |
| 实际所在 Layer | **BOOT 层** |
| 发现时机 | **`Launcher.<init>`**（早于 `run()`，即早于任何 mod 发现/层提升） |
| 第三方 `mods/` JAR 可见性 | **NO（静态不可达，见下）** |
| 是否存在其它提升通道 | **未发现**（枚举 modlauncher 11.0.5 全部 `addToLayer` 调用点） |

**发现机制（javap 原文）**：

```
  // cpw.mods.modlauncher.Launcher.<init>（私有构造器，run() 之前）：
       236: aload_0
       237: new           #128   // class cpw/mods/modlauncher/LaunchPluginHandler
       240: dup
       241: aload_0
       242: getfield      #47    // Field moduleLayerHandler:Lcpw/mods/modlauncher/ModuleLayerHandler;
       245: invokespecial #130   // Method cpw/mods/modlauncher/LaunchPluginHandler."<init>":(Lcpw/mods/modlauncher/ModuleLayerHandler;)V
       248: putfield      #131   // Field launchPlugins:Lcpw/mods/modlauncher/LaunchPluginHandler;

  // cpw.mods.modlauncher.LaunchPluginHandler：
  private static java.util.ServiceLoader lambda$new$0(cpw.mods.modlauncher.ModuleLayerHandler);
         0: aload_0
         1: getstatic     #336   // Field cpw/mods/modlauncher/api/IModuleLayerManager$Layer.BOOT:…
         4: invokevirtual #342   // Method cpw/mods/modlauncher/ModuleLayerHandler.getLayer:(…Layer;)Ljava/util/Optional;
         7: invokevirtual #348   // Method java/util/Optional.orElseThrow:()Ljava/lang/Object;
        10: checkcast     #351   // class java/lang/ModuleLayer
        13: ldc           #140   // class cpw/mods/modlauncher/serviceapi/ILaunchPluginService
        15: invokestatic  #353   // Method java/util/ServiceLoader.load:(Ljava/lang/ModuleLayer;Ljava/lang/Class;)Ljava/util/ServiceLoader;
        18: areturn
```

**"静态不可达"的完整依据**（§2.2 要求：不得由"不在白名单"直接推断）：

1. 唯一发现点是 **BOOT 层**（上引字节码）；且发生在 `Launcher` 构造期。
2. BOOT 层在引导期固定（bootstraplauncher `ModuleLayer.defineModules` 于 `MC-BOOTSTRAP` 层）；`mods/` 目录不在引导模块路径中。
3. `ModuleLayerHandler.addToLayer` 的全部调用点（modlauncher 11.0.5 全量枚举）只出现于：
   - `Launcher`（PLUGIN 资源 → PLUGIN 层；GAME 资源 → GAME 层，`lambda$run$10/11/13/14`）；
   - `TransformationServicesHandler`（`ITransformerDiscoveryService` 候选 → **SERVICE 层**，`lambda$discoverServices$16`）。
   **无一以 BOOT 层为目标**；且 `addToLayer` 对已构建层直接抛 `IllegalStateException("Layer already populated")`。
4. `mods/` JAR 的唯一层提升入口 = `ModDirTransformerDiscoverer.candidates()` →
   `TransformerDiscovererConstants.shouldLoadInServiceLayer(path)` → 命中白名单 → **SERVICE 层**（§2.2）；
   白名单不含 `ILaunchPluginService`，且该路径的终点固定是 SERVICE 层。

⇒ **结论**：第三方 `mods/` JAR 通过通道 B 注册 `ILaunchPluginService` 在静态链上**不可达**。
（**注意**：这是"发现源 = BOOT 层 + 无 BOOT 目标提升路径"的字节码结论，不是"不在白名单"的直接外推。）

---

## 三、④ 类加载触发点取证

### 3.1 调用方链（Loader 侧，javap 原文）

```
  // net.neoforged.fml.ModLoader#buildModContainerFromTOML(IModInfo, ModFileScanData)
         1: invokeinterface #467,  1   // InterfaceMethod net/neoforged/neoforgespi/language/IModInfo.getLoader:()Lnet/neoforged/neoforgespi/language/IModLanguageLoader;
         6: aload_0
         7: aload_1
         8: invokestatic  #473        // Method net/neoforged/fml/loading/FMLLoader.getGameLayer:()Ljava/lang/ModuleLayer;
        11: invokeinterface #477,  4   // InterfaceMethod net/neoforged/neoforgespi/language/IModLanguageLoader.loadMod:(IModInfo;ModFileScanData;Ljava/lang/ModuleLayer;)Lnet/neoforged/fml/ModContainer;
```

```
  // net.neoforged.fml.javafmlmod.FMLJavaModLanguageProvider#loadMod(IModInfo, ModFileScanData, ModuleLayer)
        60: new           #62   // class net/neoforged/fml/javafmlmod/FMLModContainer
        63: dup
        64: aload_1
        65: aload         4
        67: aload_2
        68: aload_3
        69: invokespecial #64   // Method FMLModContainer."<init>":(IModInfo;Ljava/util/List;ModFileScanData;Ljava/lang/ModuleLayer;)V
```

### 3.2 entrypoint `Class<?>` 获取 API（javap 原文）

```
  // net.neoforged.fml.javafmlmod.FMLModContainer#<init>
  private final java.lang.Module layer;      // ← 字段类型是 java.lang.Module（层内解析结果）

        59: aload_0
        60: aload         4                       // 构造器第 4 参数：java.lang.ModuleLayer
        62: aload_1
        63: invokeinterface #60,  1   // IModInfo.getOwningFile:()Lnet/neoforged/neoforgespi/language/IModFileInfo;
        68: invokeinterface #66,  1   // IModFileInfo.moduleName:()Ljava/lang/String;
        73: invokevirtual #72         // Method java/lang/ModuleLayer.findModule:(Ljava/lang/String;)Ljava/util/Optional;
        76: invokevirtual #78         // Method java/util/Optional.orElseThrow:()Ljava/lang/Object;
        79: checkcast     #84         // class java/lang/Module
        82: putfield      #86         // Field layer:Ljava/lang/Module;
        …
       137: aload_0
       138: getfield      #86         // Field layer:Ljava/lang/Module;
       141: aload         7           // entrypoint 字符串
       143: invokestatic  #126        // Method java/lang/Class.forName:(Ljava/lang/Module;Ljava/lang/String;)Ljava/lang/Class;
       146: astore        8
```

**记录（④ 字段逐项）**：

| 要求 | 结果 |
|---|---|
| entrypoint `Class<?>` 获取 API | `java.lang.Class.forName(java.lang.Module, java.lang.String)`（`initialize=false`，Java 9+ Module 版） |
| Module 的来源 | `ModuleLayer.findModule(modFileInfo.moduleName()).orElseThrow()`；ModuleLayer 实参 = `ModLoader` 传入的 `FMLLoader.getGameLayer()` |
| 实际 ClassLoader 类型 / 实例来源 | **该 Module 所属 GAME 层的 `ModuleClassLoader`，实例 = `cpw.mods.modlauncher.TransformingClassLoader`**（见 3.3 / 3.4） |
| 类定义触发路径 |（JDK 边界）`Class.forName(Module,String)` → JVM 以 `module.getClassLoader()` 调 `ClassLoader.loadClass(String)` → `loadClass(String,boolean)`；**Loader 侧再入帧 = `cpw.mods.cl.ModuleClassLoader.loadClass(String, boolean)`**（字节码，见 §四） |
| 进入 JDK / VM 边界前**最后一个 Java-visible 调用点** | `invokestatic java/lang/Class.forName:(Ljava/lang/Module;Ljava/lang/String;)Ljava/lang/Class;`（`FMLModContainer.<init>` offset 143 附近，javap 原文如上） |
| 是否经过 `TransformingClassLoader` | **YES**（GAME 层 loader 即 `TransformingClassLoader`，见 3.3/3.4） |
| 是否经过 ModuleLayer | **YES**（GAME 层；`Module`/`defineModules` 语义，见下） |

（本 Spike 不反推 JDK/VM 内部实现细节；仅记录边界两侧的精确字节码依据。）

### 3.3 `FMLLoader.getGameLayer()` 与 ModLauncher GAME 层的同一性（javap 原文）

```
  // net.neoforged.fml.loading.FMLLoader
  private static java.lang.ModuleLayer gameLayer;
  public static void beforeStart(java.lang.ModuleLayer);
         0: aload_0
         1: putstatic     #376   // Field gameLayer:Ljava/lang/ModuleLayer;

  // net.neoforged.fml.loading.targets.CommonLaunchHandler
  public cpw.mods.modlauncher.api.ServiceRunner launchService(java.lang.String[], java.lang.ModuleLayer);
         0: aload_2
         1: invokestatic  #261   // Method net/neoforged/fml/loading/FMLLoader.beforeStart:(Ljava/lang/ModuleLayer;)V
```

```
  // cpw.mods.modlauncher.Launcher#run（关键顺序，javap 原文节选）
       240: invokevirtual #283   // Method cpw/mods/modlauncher/TransformationServicesHandler.buildTransformingClassLoader:(LaunchPluginHandler;Environment;ModuleLayerHandler;)Lcpw/mods/modlauncher/TransformingClassLoader;
       243: putfield      #287   // Field classLoader:Lcpw/mods/modlauncher/TransformingClassLoader;
       …
       268: getstatic     #301   // Field cpw/mods/modlauncher/api/IModuleLayerManager$Layer.GAME:…
       271: invokevirtual #304   // Method cpw/mods/modlauncher/ModuleLayerHandler.getLayer:(…Layer;)Ljava/util/Optional;
       274: invokevirtual #308   // Method java/util/Optional.orElseThrow:()Ljava/lang/Object;
       277: checkcast     #313   // class java/lang/ModuleLayer
       280: aload_0
       281: getfield      #287   // Field classLoader:Lcpw/mods/modlauncher/TransformingClassLoader;
       288: invokevirtual #315   // Method cpw/mods/modlauncher/LaunchServiceHandler.launch:(ArgumentHandler;ModuleLayer;TransformingClassLoader;LaunchPluginHandler;)V
```

⇒ `FMLLoader.gameLayer` 即 `ModuleLayerHandler.getLayer(GAME)` 返回的 GAME 层。

### 3.4 GAME 层的 ClassLoader = `TransformingClassLoader`（javap 原文）

```
  // cpw.mods.modlauncher.TransformationServicesHandler#buildTransformingClassLoader
         0: aload_3
         1: getstatic     #35   // Field cpw/mods/modlauncher/api/IModuleLayerManager$Layer.GAME:…
         4: aload_0 / 5: aload_1 / 6: aload_2
         7: invokedynamic #41   // BiFunction（下引 lambda$buildTransformingClassLoader$0）
        12: invokevirtual #45   // Method cpw/mods/modlauncher/ModuleLayerHandler.buildLayer:(Layer;BiFunction;)L…LayerInfo;
        31: aload         4
        33: invokevirtual #62   // LayerInfo.cl:()Lcpw/mods/cl/ModuleClassLoader;
        36: checkcast     #68   // class cpw/mods/modlauncher/TransformingClassLoader
        39: areturn

  private cpw.mods.cl.ModuleClassLoader lambda$buildTransformingClassLoader$0(LaunchPluginHandler, Environment, Configuration, List);
         0: new           #68   // class cpw/mods/modlauncher/TransformingClassLoader
        13: invokespecial #497  // Method TransformingClassLoader."<init>":(TransformStore;LaunchPluginHandler;Environment;Ljava/lang/module/Configuration;Ljava/util/List;)V
        16: areturn
```

```
  // cpw.mods.modlauncher.ModuleLayerHandler#buildLayer(Layer, BiFunction)：单 loader 定义整层
       254: aload         7
       256: invokedynamic #201  // Function（返回该 loader）
       261: invokestatic  #204  // Method java/lang/ModuleLayer.defineModules:(Ljava/lang/module/Configuration;Ljava/util/List;Ljava/util/function/Function;)Ljava/lang/ModuleLayer$Controller;
```

⇒ GAME 层内所有模块（含 `mods/` JAR 对应模块）由**同一个 `TransformingClassLoader` 实例**定义
（`Module.getClassLoader()` 即该实例）。`ModValidator.getModResources()` 亦以
`new ITransformationService$Resource(GAME, <mod files + GAMELIBRARY>)` 形式把 mod 文件送入 GAME 层
（javap 原文：`53: getstatic #191 // Field …Layer.GAME`）。

**PASS-b ✅**

---

## 四、⑤ class bytes 获取点与实际 `T` 取证

### 4.1 字节读取位置与进入转换链的调用（`securejarhandler-3.0.8`，javap 原文）

```
  // cpw.mods.cl.ModuleClassLoader
  protected java.lang.Class<?> loadClass(java.lang.String, boolean) throws java.lang.ClassNotFoundException;
  …     42: getfield      #52   // Field packageLookup:Ljava/util/Map;
        46: aload         6
        48: invokeinterface #395	// Map.containsKey
        56: aload_0
        57: aload_0
        58: getfield      #52   // Field packageLookup:…
        71: invokevirtual #398  // Method java/lang/module/ResolvedModule.name:()Ljava/lang/String;
        75: invokevirtual #399  // Method findClass:(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/Class;

  protected java.lang.Class<?> findClass(java.lang.String) throws java.lang.ClassNotFoundException;
         0: aload_0
         1: aload_1
         2: invokevirtual #418  // Method classNameToModuleName:(Ljava/lang/String;)Ljava/lang/String;
        10: aload_0
        11: aload_2
        12: aload_1
        13: invokevirtual #399  // Method findClass:(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/Class;

  protected java.lang.Class<?> findClass(java.lang.String moduleName, java.lang.String className);
         0: aload_0
         1: aload_1
         2: aload_0
         3: aload_2
         4: invokedynamic #496  // BiFunction（lambda$findClass$20）
         9: invokevirtual #449  // Method loadFromModule:(Ljava/lang/String;Ljava/util/function/BiFunction;)Ljava/lang/Object;

  private java.lang.Class<?> readerToClass(java.lang.module.ModuleReader, java.lang.module.ModuleReference, java.lang.String);
         0: aload_0
         1: aload_0
         2: aload_1
         3: aload_2
         4: aload_3
         5: invokevirtual #308  // Method getClassBytes:(Ljava/lang/module/ModuleReader;Ljava/lang/module/ModuleReference;Ljava/lang/String;)[B
         8: aload_3
         9: aconst_null
        10: invokevirtual #312  // Method maybeTransformClassBytes:([BLjava/lang/String;Ljava/lang/String;)[B
        13: astore        4
        …
       114: aload_0
       115: aload_3
       116: aload         4
       118: iconst_0
       119: aload         4
       121: arraylength
       122: aload         7
       124: aload_0
       125: invokestatic  #357  // Method cpw/mods/cl/ProtectionDomainHelper.createProtectionDomain:(…)Ljava/security/ProtectionDomain;
       128: invokevirtual #361  // Method defineClass:(Ljava/lang/String;[BIILjava/security/ProtectionDomain;)Ljava/lang/Class;
```

`getClassBytes`（源码名，字节码读取位置）= 从模块 `ModuleReader` 读
`className.replace('.','/') + ".class"`（`ModuleReader.find(...)` → `open()` → `readAllBytes`，
来源即 `SecureJar$ModuleDataProvider` 的模块读取器）。

### 4.2 `TransformingClassLoader` 覆写点与转换入口（javap 原文）

```
  // cpw.mods.modlauncher.TransformingClassLoader
  protected byte[] maybeTransformClassBytes(byte[], java.lang.String, java.lang.String);
         0: aload_0
         1: getfield      #47   // Field classTransformer:Lcpw/mods/modlauncher/ClassTransformer;
         4: aload_1
         5: aload_2
         6: aload_3
         7: ifnull        14
        10: aload_3
        11: goto          16
        14: ldc           #92   // String classloading
        16: invokevirtual #94   // Method cpw/mods/modlauncher/ClassTransformer.transform:([BLjava/lang/String;Ljava/lang/String;)[B
        19: areturn
```

`cpw.mods.cl.ModuleClassLoader.maybeTransformClassBytes` 基类实现为 `return bytes;`（恒等），
`TransformingClassLoader` 覆写并转发到 `ClassTransformer.transform(byte[], name, "classloading")`
（`readerToClass` 传入 `pkg = null` ⇒ 第三实参固定为字符串 `"classloading"`）。

### 4.3 转换链入口类型与实际 `T`（`ClassTransformer.transform`，javap 原文）

```
  byte[] transform(byte[], java.lang.String, java.lang.String);
         0: aload_2
         1: bipush        46
         3: bipush        47
         5: invokevirtual #42   // Method java/lang/String.replace:(CC)Ljava/lang/String;   // '.' -> '/'
        12: invokestatic  #48   // Method org/objectweb/asm/Type.getObjectType:(Ljava/lang/String;)Lorg/objectweb/asm/Type;
        43: aload_0
        44: getfield      #26   // Field transformers:Lcpw/mods/modlauncher/TransformStore;
        47: aload         4
        49: invokevirtual #60   // Method cpw/mods/modlauncher/TransformStore.needsTransforming:(Ljava/lang/String;)Z
        54: iload         7
        56: ifne          69
        59: aload         6
        61: invokevirtual #66   // Method java/util/EnumMap.isEmpty:()Z
        64: ifeq          69
        67: aload_1
        68: areturn                                  // ← 无目标且无插件：原样返回，不解析 ASM
        69: new           #72   // class org/objectweb/asm/tree/ClassNode
        73: ldc           #76   // int 589824          （ASM9 = 9 << 16）
        75: invokespecial #77   // ClassNode."<init>":(I)V
        85: new           #80   // class org/objectweb/asm/ClassReader
        89: aload_1
        90: invokespecial #82   // ClassReader."<init>":([B)V
        97: aload         8
        99: bipush        8                              （EXPAND_FRAMES）
       101: invokevirtual #85   // Method org/objectweb/asm/ClassReader.accept:(Lorg/objectweb/asm/ClassVisitor;I)V
```

| 要求 | 结果 |
|---|---|
| 字节读取位置 | `ModuleClassLoader.getClassBytes(ModuleReader, ModuleReference, name)`（SecureJar 模块读取器） |
| 进入转换链时的实际类型 | **`byte[]`**（`maybeTransformClassBytes:([BLjava/lang/String;Ljava/lang/String;)[B`） |
| 转换链入口类型 | `byte[]` → ASM 解析 → **`ClassNode`** |
| ASM 解析发生的方法 | `cpw.mods.modlauncher.ClassTransformer.transform`：`new ClassReader(bytes).accept(node, 8)` |
| 实际 transformer 实现类 | 本轮不判定（第三方实现；机制层面为 `TransformerHolder` 包装） |
| **实际 `T`** | **`org.objectweb.asm.tree.ClassNode`**（PRE_CLASS / CLASS）、`MethodNode`（METHOD）、`FieldNode`（FIELD）——由 `TargetType.getNodeType()` 字节码确定（见 §七） |
| 调用点接收参数的字节码类型 | `maybeTransformClassBytes:([BLjava/lang/String;Ljava/lang/String;)[B`；`ITransformer.transform:(Ljava/lang/Object;Lcpw/mods/modlauncher/api/ITransformerVotingContext;)Ljava/lang/Object;`（泛型擦除） |
| 输出 | `byte[]`（`ClassWriter.toByteArray()`；`TransformerClassWriter.createClassWriter(int, ClassTransformer, ClassNode)`） |

另一条进入点（供 launch plugin 通过 `ITransformerLoader` 取类）：
`TransformingClassLoader.buildTransformedClassNodeFor(name, pkg)` → `ModuleClassLoader.getMaybeTransformedClassBytes`
→ offset 167-174 `invokevirtual maybeTransformClassBytes`（同一覆写点）。

**PASS-c ✅**

---

## 五、⑥ `transform` / `processClass` 静态调用点取证

### 5.1 通道 A：`ITransformer.transform` 调用点（javap 原文）

```
  // cpw.mods.modlauncher.ClassTransformer#performVote
       155: aload         5
       157: getstatic     #392  // Field cpw/mods/modlauncher/api/TransformerVoteResult.YES:…
       168: iconst_0
       169: invokeinterface #395,  2  // List.get
       174: checkcast     #398        // class cpw/mods/modlauncher/TransformerVote
       177: invokevirtual #400        // Method cpw/mods/modlauncher/TransformerVote.getTransformer:()Lcpw/mods/modlauncher/api/ITransformer;
       180: astore        6
       182: aload         6
       184: aload_2
       185: aload_3
       186: invokeinterface #404,  3  // InterfaceMethod cpw/mods/modlauncher/api/ITransformer.transform:(Ljava/lang/Object;Lcpw/mods/modlauncher/api/ITransformerVotingContext;)Ljava/lang/Object;
       191: astore_2
```

| 要求 | 结果 |
|---|---|
| 调用点类/方法 | `cpw.mods.modlauncher.ClassTransformer#performVote` |
| 调用指令 | `invokeinterface`（#404） |
| 被调用接口/方法 | `cpw.mods.modlauncher.api.ITransformer.transform(T, ITransformerVotingContext)` |
| 输入类型 | 实际 `T` = `ClassNode` / `MethodNode` / `FieldNode`（按调用来源列表） |
| 输出类型 | 同类节点 `T`（`astore_2` 覆盖后继续投票循环） |
| 调用点所属 Layer | 该 `ClassTransformer` 由 `TransformingClassLoader`（GAME 层 loader）构造（1:1，见 §三 3.4） |
| 静态前置条件 | (a) `needsTransforming(internalName)=true`；(b) 对应 `TargetType` 子表存在匹配 label；(c) `castVote` 返回 `YES`（`performVote` 按 `TransformerVoteResult` 分组：`REJECT`→抛 `VoteRejectedException`；`NO`→移出列表；`YES`→取首个执行 `transform`；`DEFER`→抛 `VoteDeadlockException`） |

`performVote` 的四个调用来源（同一方法内，javap 原文偏移）：

```
       264–294: TransformStore.getTransformersFor(name, TargetType.PRE_CLASS)  → performVote(...)
       352–392: TransformStore.getTransformersFor(name, FieldNode)            → performVote(...)（逐字段）
       448–491: TransformStore.getTransformersFor(name, MethodNode)           → performVote(...)（逐方法）
       505–538: TransformStore.getTransformersFor(name, TargetType.CLASS)     → performVote(...)
```

（顺序：PRE_CLASS → 字段 → 方法 → CLASS → AFTER 插件。）

**唯一性核查**：`modlauncher-11.0.5` 中 `ITransformer.transform` 的 `invokeinterface` 仅出现于
`ClassTransformer`；`TransformerHolder` 仅实现/委托该接口；`loader-4.0.45.jar` 无该调用点。

### 5.2 通道 B：`ILaunchPluginService` 调用点（javap 原文）

```
  // cpw.mods.modlauncher.LaunchPluginHandler#computeLaunchPluginTransformerSet
        58: aload         8
        60: aload_1
        61: iload_2
        62: aload_3
        63: invokeinterface #175,  4  // InterfaceMethod ILaunchPluginService.handlesClass:(Lorg/objectweb/asm/Type;ZLjava/lang/String;)Ljava/util/EnumSet;

  // cpw.mods.modlauncher.LaunchPluginHandler#offerClassNodeToPlugins
        58: aload         9
        60: aload_1
        61: aload_3
        62: aload         4
        64: aload         6
        66: invokeinterface #231,  5  // InterfaceMethod ILaunchPluginService.processClassWithFlags:(Lcpw/mods/modlauncher/serviceapi/ILaunchPluginService$Phase;Lorg/objectweb/asm/tree/ClassNode;Lorg/objectweb/asm/Type;Ljava/lang/String;)I
```

| 要求 | 结果 |
|---|---|
| 调用点类/方法 | `LaunchPluginHandler#offerClassNodeToPlugins`（由 `ClassTransformer.transform` 于 BEFORE/AFTER 两处调用） |
| 调用指令 | `invokeinterface`（#231） |
| 被调用接口/方法 | `ILaunchPluginService.processClassWithFlags(Phase, ClassNode, Type, String)`（默认方法，可回退到 `processClass(...)` 族） |
| 输入类型 | `ClassNode`（已解析）+ `org.objectweb.asm.Type` |
| 输出类型 | `int`（ComputeFlags 位） |
| 调用点所属 Layer | 同上：GAME 层 `TransformingClassLoader` → `ClassTransformer` |
| 静态前置条件 | `handlesClass(Type, isEmpty, pkg)` 返回非空 `EnumSet<Phase>`（`computeLaunchPluginTransformerSet`；`isEmpty = (bytes.length == 0)`，由 `ClassTransformer.transform` offset 23-29 计算），且 plugin 来源为 BOOT 层（§2.3） |

> **调用点静态存在 ≠ 运行时一定被触发**（本 Spike 不运行服务端）。

**PASS-d ✅**

---

## 六、④→⑤→⑥ 连续链与静态时序

### 6.1 连接点 1（④→⑤）：`Module` → 该模块的 ClassLoader → `ModuleClassLoader.loadClass`

```
④ FMLModContainer.<init>
   └─ ModuleLayer.findModule(info.getOwningFile().moduleName())        （javap 原文，offset 59–82）
   └─ Class.forName(Module, entrypointName)                            （javap 原文，offset 137–146）
==================== JDK 边界（记录，不反推实现）====================
   JDK 契约：Class.forName(Module,String) 以 module.getClassLoader() 为加载器；
   JVM 经 ClassLoader.loadClass(String) → loadClass(String,boolean) 分派
==================== 再入点（字节码）====================
⑤ cpw.mods.cl.ModuleClassLoader.loadClass(String, boolean)            （javap 原文，§四 4.1）
   └─ findClass(String) → findClass(String moduleName, String className)
   └─ loadFromModule(moduleName, lambda$findClass$20) → readerToClass
   └─ getClassBytes(ModuleReader, …)                                    （byte[] 形成点）
   └─ invokevirtual maybeTransformClassBytes(bytes, name, null)         （javap 原文，§四 4.1）
```

**同一对象的可证明性**：

- `FMLModContainer.<init>` 的 `ModuleLayer` 实参来自 `ModLoader` 的 `FMLLoader.getGameLayer()`；
- `FMLLoader.gameLayer` 由 `FMLLoader.beforeStart(ModuleLayer)` 写入，其调用方
  `CommonLaunchHandler.launchService(String[], ModuleLayer)` 的层实参来自
  `Launcher.run` → `LaunchServiceHandler.launch(..., moduleLayerHandler.getLayer(GAME).orElseThrow(), ...)`；
- 该 GAME 层由 `TransformationServicesHandler.buildTransformingClassLoader` 经
  `ModuleLayerHandler.buildLayer(GAME, lambda$buildTransformingClassLoader$0)` 建立，
  `ModuleLayer.defineModules(configuration, parents, cl)` 以**同一个** `TransformingClassLoader` 实例定义整层模块；
- 因此 `Class.forName(Module, name)` 中 `Module` 的加载器**就是**该 `TransformingClassLoader` 实例；
  `readerToClass` 中的 `invokevirtual maybeTransformClassBytes` 以 `this` 分派到该实例的覆写实现。

### 6.2 连接点 2（⑤→⑥）：同一 loader 的覆写方法 → `ClassTransformer` → ⑥

```
⑤ this.maybeTransformClassBytes(bytes, name, null)                     （ModuleClassLoader.readerToClass）
   └─（动态分派，this = GAME 层 TransformingClassLoader 实例）
⑥ cpw.mods.modlauncher.TransformingClassLoader.maybeTransformClassBytes（javap 原文，§四 4.2）
   └─ classTransformer.transform(bytes, name, "classloading")          （同一实例字段 classTransformer）
       ├─ TransformStore.getTransformersFor(name, PRE_CLASS/CLASS/FieldNode/MethodNode)
       │    └─ performVote → invokeinterface ITransformer.transform(T, ctx)   ← 通道 A 的 ⑥
       └─ LaunchPluginHandler.computeLaunchPluginTransformerSet(…) / offerClassNodeToPlugins(…)
            └─ invokeinterface ILaunchPluginService.processClassWithFlags(…)  ← 通道 B 的 ⑥
   └─ ClassWriter.toByteArray() → defineClass（JDK/VM 边界，返回 Class<?>）
```

**同一静态调用路径判定：`YES`。**
全部连接边为**同一对象的 `this` 分派**（`loader.loadClass` → `loader.readerToClass` → `loader.maybeTransformClassBytes`
→ `loader.classTransformer.transform` → `performVote`）或**同一 GAME 层身份**（ModuleLayer → Module → ClassLoader），
不存在"分别找到调用点后拼接"的情况。两处 JDK / VM 边界（`Class.forName0`、`defineClass`）按 §三 要求显式标注，
未跨越边界反推实现。

### 6.3 与 `FMLModContainer.<init>` 的静态时序

`Launcher.run` 的字节码顺序（javap 原文摘录）：

```
        14: TransformationServicesHandler.discoverServices:(ArgumentHandler$DiscoveryData)V          （SERVICE 层建立并加载服务）
       114: TransformationServicesHandler.triggerScanCompletion:(IModuleLayerManager;)Ljava/util/List;（FML completeScan → 资源）
       ~140–191: 资源入层（GAME / PLUGIN，addToLayer）
       200: TransformationServicesHandler.initialiseServiceTransformers:()V                          （⑥ 的注册：gatherTransformers）
       207: LaunchPluginHandler.offerScanResultsToPlugins:(Ljava/util/List;)V
       240: TransformationServicesHandler.buildTransformingClassLoader:(…)L…TransformingClassLoader;（GAME 层建立）
       288: LaunchServiceHandler.launch:(ArgumentHandler;ModuleLayer;TransformingClassLoader;LaunchPluginHandler;)V
```

而 ④ 的类加载发生在 launch 之后的 FML 启动流程（`ModLoader.buildModContainerFromTOML` → `FMLModContainer.<init>`）。

**结论**：⑥ 的注册（通道 A 的 `TransformStore` 填充、通道 B 的插件集合固定）**先于** ④ 的 entrypoint 类加载
（静态时序 = **注册在前**）。**PASS-e ✅**

（边界声明：静态可达 ≠ 本次运行一定发生；本 Spike 不运行服务端。）

---

## 七、`targets()` / `TargetType` 匹配规则

### 7.1 元素类型与静态工厂（javap 原文）

```
public interface cpw.mods.modlauncher.api.ITransformer<T> {
  public abstract T transform(T, cpw.mods.modlauncher.api.ITransformerVotingContext);
  public abstract cpw.mods.modlauncher.api.TransformerVoteResult castVote(ITransformerVotingContext);
  public abstract java.util.Set<cpw.mods.modlauncher.api.ITransformer$Target<T>> targets();
  public abstract cpw.mods.modlauncher.api.TargetType<T> getTargetType();
  public default java.lang.String[] labels();          // DEFAULT_LABEL = ["default"]
}

public final class cpw.mods.modlauncher.api.ITransformer$Target<T> extends java.lang.Record {
  private final java.lang.String className;
  private final java.lang.String elementName;
  private final java.lang.String elementDescriptor;
  private final cpw.mods.modlauncher.api.TargetType<T> targetType;

  public static Target<ClassNode> targetClass(java.lang.String);                  // → Target(name, "", "", TargetType.CLASS)
  public static Target<ClassNode> targetPreClass(java.lang.String);               // → Target(name, "", "", TargetType.PRE_CLASS)
  public static Target<MethodNode> targetMethod(java.lang.String, java.lang.String, java.lang.String); // → Target(name, m, d, METHOD)
  public static Target<FieldNode> targetField(java.lang.String, java.lang.String); // → Target(name, f, "", FIELD)
}
```

```
public final class cpw.mods.modlauncher.api.TargetType<T> {
  public static final TargetType<org.objectweb.asm.tree.ClassNode> PRE_CLASS;   // name="PRE_CLASS"
  public static final TargetType<org.objectweb.asm.tree.ClassNode> CLASS;       // name="CLASS"
  public static final TargetType<org.objectweb.asm.tree.MethodNode> METHOD;     // name="METHOD"
  public static final TargetType<org.objectweb.asm.tree.FieldNode> FIELD;       // name="FIELD"
  public static final TargetType<?>[] VALUES;                                   // {PRE_CLASS, CLASS, METHOD, FIELD}
}
```

### 7.2 注册规则（`TransformationServiceDecorator.lambda$gatherTransformers$1`，javap 原文）

```
        14: aload_2
        15: invokeinterface #118  // ITransformer.targets:()Ljava/util/Set;
        20: astore        4
        22: aload         4
        24: invokeinterface #122  // java/util/Set.isEmpty:()Z
        29: ifne          154       // ← targets() 为空：整条 transformer 被跳过（不注册）
        …
        49: invokedynamic #142 …     // 按 Target.targetType() 分组（groupingBy）
        67: aload         5
        69: invokeinterface #155  // Map.keySet
        74: invokeinterface #158  // Set.size
        79: iconst_1
        80: if_icmpgt     94
        83: aload         5
        85: aload_3
        86: invokeinterface #162  // Map.containsKey(transformer.getTargetType())
        91: ifne          119
        94: … IllegalArgumentException("The transformer contains invalid targets")   // 多目标类型 / 与 getTargetType() 不符 → 抛错
       119: … flatMap + forEach(lambda$gatherTransformers$0)
  private void lambda$gatherTransformers$0(TransformStore, ITransformer, TransformTargetLabel);
         1: aload_3
         2: aload_2
         3: aload_0
         4: getfield      #7   // Field service:…
         7: invokevirtual #190  // Method cpw/mods/modlauncher/TransformStore.addTransformer:(Lcpw/mods/modlauncher/TransformTargetLabel;Lcpw/mods/modlauncher/api/ITransformer;Lcpw/mods/modlauncher/api/ITransformationService;)V
```

### 7.3 存储与匹配算法（`TransformStore` / `TransformTargetLabel` / `TransformList`，javap 原文）

```
  // cpw.mods.modlauncher.TransformStore#addTransformer
        35: aload_0
        36: getfield      #10   // Field classNeedsTransforming:Ljava/util/Set;
        39: aload_1
        40: invokevirtual #108  // TransformTargetLabel.getClassName:()Lorg/objectweb/asm/Type;
        43: invokevirtual #112  // org/objectweb/asm/Type.getInternalName:()Ljava/lang/String;
        46: invokeinterface #118 // Set.add   ← classNeedsTransforming 存 internal name
        …
        56: aload_1
        57: invokevirtual #124  // TransformTargetLabel.getTargetType:()Lcpw/mods/modlauncher/api/TargetType;
        60: invokeinterface #128 // Map.get
        70–82: TransformList.addTransformer(label, new TransformerHolder(t, service))

  // cpw.mods.modlauncher.TransformTargetLabel 私有构造器（归一化）
         4: aload_1
         6: bipush 46 / 8: bipush 47
        10: invokevirtual #28   // String.replace('.', '/')       ← FQCN 或 internal 均可
        13: invokestatic  #34   // org/objectweb/asm/Type.getObjectType
        24: aload_3
        26: invokevirtual #46   // String.length
        29: ifle 39 / 33: Type.getMethodType / 39: Type.VOID_TYPE   ← 方法描述符（METHOD）或 VOID（CLASS/FIELD）

  // getTransformersFor(String, TargetType)  — ClassTransformer 的 CLASS/PRE_CLASS 查询
         0: new           #44   // TransformTargetLabel(String name, TargetType)
        10: aload_2
        11: aload_0
        12: getfield      #19   // Map<TargetType, TransformList>
        15: invokevirtual #59   // TargetType.get(Map)
        22: aload_3
        23: invokevirtual #63   // TransformList.getTransformersForLabel:(TransformTargetLabel)

  // getTransformersFor(String, FieldNode) → label(name, field.name)；getTransformersFor(String, MethodNode) → label(name, m.name, m.desc)

  // cpw.mods.modlauncher.TransformList#getTransformersForLabel → ConcurrentHashMap.computeIfAbsent(label)
  // cpw.mods.modlauncher.TransformTargetLabel#equals → Objects.equals(className, elementName, elementDescriptor)
```

| 检查项 | 结果 |
|---|---|
| 匹配算法位置 | `ClassTransformer.transform`（取出候选）+ `TransformStore.getTransformersFor`（构键）+ `TransformList.getTransformersForLabel`（查表）+ `TransformTargetLabel.equals/hashCode`（键比较） |
| 匹配方式 | **精确匹配**：ASM `Type` 内部名 + `String` 元素名 + 描述符 `Type` 的 `Objects.equals` |
| 可赋值匹配 | **无**（未使用 `isAssignableFrom` / 前缀 / 通配） |
| 名称表示法 | 接受 **FQCN（`.`）或 internal（`/`）**，统一归一化为 ASM `Type.getObjectType`（internal name）比较 |
| 是否区分方法/字段 | **YES**：METHOD 需 `name + desc` 精确匹配；FIELD 按 `name`（desc 固定 `VOID_TYPE`）；CLASS/PRE_CLASS 按类名 |
| `classNeedsTransforming` 语义 | 该集合存 **internal name**；`ClassTransformer.transform` 在进入 ASM 解析前以 `needsTransforming(internalName)` 作**第一层门** |
| 目标类型约束 | 同一 transformer 的所有 target 必须属于**同一个** `targetType`，且等于 `getTargetType()`，否则 `IllegalArgumentException` |
| `targets()` 为空 | transformer **不会被注册**（静默跳过） |

### 7.4 ForgeProbe 静态资格判定

```
FQCN:  com.example.forgeprobe.ForgeProbe   （forgeprobe.jar 内实存：com/example/forgeprobe/ForgeProbe.class）
```

| 项 | 判定 | 依据 |
|---|---|---|
| 是否满足 `targets()` 的**匹配前提** | **YES** | (1) 精确匹配规则 = 归一化类名 `equals`（§7.3）；(2) ForgeProbe 类经 GAME 层 `TransformingClassLoader` 进入 `ClassTransformer.transform`（§三–§五链）；(3) 声明 `Target.targetClass("com.example.forgeprobe.ForgeProbe")`（或 internal 形式）即可命中 `classNeedsTransforming` 与 CLASS 子表 |

**前提条件（必须同时成立，本轮均未触发）**：

1. 存在**已注册**的第三方 transformer（`ITransformationService.transformers()` 提供且 `targets()` 非空）；
2. 其 `targets()` 精确声明该目标，且 `getTargetType()` 与目标类型一致；
3. 该 transformer 的 `castVote` 对该类返回 `YES`（否则按 REJECT/NO/DEFER 分支处理）；
4. ForgeProbe 本次运行**实际被加载**（本 Spike 不运行 → 运行时状态 UNKNOWN / N/A）。

**PASS-f ✅**

---

## 八、通道归属判定

**问题**：`FMLModContainer.<init>` 的 entrypoint 类加载路径实际关联的是通道 A（`ITransformationService.transformers()`）
还是通道 B（`ILaunchPluginService`），或两者？

**判定：两者汇入同一转换点（`ClassTransformer.transform`），但 provider 来源 Layer 与可被调用的接口方法不同：**

| | 通道 A | 通道 B |
|---|---|---|
| 接口 | `ITransformer` | `ILaunchPluginService` |
| provider 来源 Layer | **SERVICE 层**（`ITransformationService` 经白名单提升；`ServiceLoader.load(SERVICE 层, …)`） | **BOOT 层**（`ServiceLoader.load(BOOT 层, …)`，`Launcher` 构造期） |
| 实际被调用接口方法（⑥） | `ITransformer.transform(T, ITransformerVotingContext)`（`ClassTransformer.performVote`，`invokeinterface #404`） | `ILaunchPluginService.processClassWithFlags(Phase, ClassNode, Type, String)`（`LaunchPluginHandler.offerClassNodeToPlugins`，`invokeinterface #231`） |
| 实际参数 `T` | `ClassNode` / `MethodNode` / `FieldNode`（按 `TargetType`） | `ClassNode`（已解析） |
| 第三方 `mods/` JAR 可达性 | **静态可达**（白名单 = YES → SERVICE 层 → `transformers()`） | **静态不可达**（发现源 BOOT 层；`mods/` JAR 无进入 BOOT 层的路径；`addToLayer` 无 BOOT 目标） |

**依据**（反引段落，均为精确字节码）：

1. `ClassTransformer.transform` 同时引用 `TransformStore`（`Field transformers`，offset 44/269）与
   `LaunchPluginHandler`（`Field pluginHandler`，offset 18/168/541）——二者在同一方法内被查询；
2. `TransformingClassLoader.<init>` 以 `(TransformStore, LaunchPluginHandler, Environment, Configuration, List)` 构造
   `ClassTransformer`（javap 原文，§三 3.4）；
3. `LaunchPluginHandler.lambda$new$0` 的 BOOT 层加载（§2.3）；
4. `TransformationServicesHandler.lambda$discoverServices$16` 的 SERVICE 层提升（§2.2）。

> **情形 C 记录**：通道 B 的注册条件证明第三方 `mods/` JAR **不可注册**（限制条件，交决策方裁定）。
> 该结论**不是**由"不在白名单"直接推出，而是由"发现源 = BOOT 层 + 无 BOOT 目标提升路径"的完整字节码链条得出（§2.3）。

**PASS-g ✅**

---

## 九、清理确认

本 Spike 为**纯取证**：

- [x] 兼容层工程未修改（`spike001a/compat`、`spike001b5` 未触碰）
- [x] 无新增源文件 / 无新增依赖（未引入 ASM；分析全程未编译任何 Java 代码）
- [x] `forgecompat.jar` 未重新构建
- [x] `mods/` 未修改（`forgeprobe.jar` / `forgecompat.jar` 仅只读读取）
- [x] 未运行服务端、无新日志（`logs/` 无新增）
- [x] 分析用临时目录（`.scratch-002-0b-1b/`，解包 + javap 文本）已在报告定稿前删除
- [x] 仓库内唯一新增文件 = 本报告（`Dev Manuals/Spike-002-0b-1b 取证报告.md`）

**PASS-cleanup ✅**

---

## 十、事实标签

### [F] Source Fact

1. `[F]` `FMLModContainer.<init>` 以 `ModuleLayer.findModule(IModFileInfo.moduleName()).orElseThrow()` 解析 `java.lang.Module` 字段，
   并以 `Class.forName(Module, String)` 获取 entrypoint `Class<?>`。
2. `[F]` `ModLoader.buildModContainerFromTOML` 传入 `FMLLoader.getGameLayer()` 作为 `IModLanguageLoader.loadMod(…, ModuleLayer)` 的层实参。
3. `[F]` `FMLLoader.gameLayer` 由 `CommonLaunchHandler.launchService(String[], ModuleLayer)` 经 `FMLLoader.beforeStart` 写入；
   该层实参来自 `Launcher.run → LaunchServiceHandler.launch(…, ModuleLayerHandler.getLayer(GAME).orElseThrow(), …)`。
4. `[F]` GAME 层由 `TransformationServicesHandler.buildTransformingClassLoader` 经
   `ModuleLayerHandler.buildLayer(GAME, BiFunction)` 建立；BiFunction = `new TransformingClassLoader(transformStore, pluginHandler, environment, configuration, parents)`；
   `ModuleLayer.defineModules(configuration, parents, cl)` 以单 loader 定义整层。
5. `[F]` `cpw.mods.cl.ModuleClassLoader.loadClass` → `findClass` → `loadFromModule` → `readerToClass` 中
   `getClassBytes(ModuleReader, …)` 读取 `byte[]`，随后 `invokevirtual maybeTransformClassBytes`（
   `TransformingClassLoader` 覆写为 `ClassTransformer.transform(bytes, name, "classloading")`）。
6. `[F]` `ClassTransformer.transform` 输入 `byte[]`，以 `new ClassNode(589824)`（ASM9）+ `ClassReader.accept(node, 8)`（EXPAND_FRAMES）形成实际 `T`；
   无目标且无插件时**原样返回字节**（offset 54–68）。
7. `[F]` 通道 A 的 ⑥ 调用点 = `ClassTransformer.performVote` 的
   `invokeinterface ITransformer.transform:(Ljava/lang/Object;LITransformerVotingContext;)Ljava/lang/Object;`；
   其候选列表来自 `TransformStore.getTransformersFor(name, PRE_CLASS / FieldNode / MethodNode / CLASS)`。
8. `[F]` 通道 B 的 ⑥ 调用点 = `LaunchPluginHandler.offerClassNodeToPlugins` 的
   `invokeinterface ILaunchPluginService.processClassWithFlags(Phase, ClassNode, Type, String)`；
   候选来自 `computeLaunchPluginTransformerSet → handlesClass(Type, isEmpty, pkg)`。
9. `[F]` 通道 A 的注册 = `TransformationServiceDecorator.gatherTransformers`：
   `targets()` 为空 → 跳过；按 `targetType` 分组并校验等于 `getTargetType()`；
   `new TransformTargetLabel(target)` → `TransformStore.addTransformer(label, transformer, service)`（`TransformerHolder` 包装）。
10. `[F]` `TransformStore.addTransformer` 将 `label.getClassName().getInternalName()` 加入 `classNeedsTransforming`；
    按 `TargetType` 分桶 `TransformList`。
11. `[F]` `TransformTargetLabel` 将类名 `replace('.', '/')` 后转 `ASM Type`；
    方法描述符非空时用 `Type.getMethodType`，否则 `Type.VOID_TYPE`；`equals/hashCode` 只比较
    `className + elementName + elementDescriptor`（`labelType` 由子表隔离）。
12. `[F]` `TargetType` 常量及 node 类型：`PRE_CLASS/CLASS → ClassNode`，`METHOD → MethodNode`，`FIELD → FieldNode`；`VALUES` 顺序同上。
13. `[F]` `Target` 静态工厂：`targetClass(name) = (name, "", "", CLASS)`；`targetPreClass(name) = (name, "", "", PRE_CLASS)`；
    `targetMethod(name, m, d) = (name, m, d, METHOD)`；`targetField(name, f) = (name, f, "", FIELD)`。
14. `[F]` 通道 B 的发现 = `ServiceLoader.load(BOOT 层, ILaunchPluginService.class)`（`LaunchPluginHandler.lambda$new$0`），
    发生在 `Launcher.<init>`（run() 之前）。
15. `[F]` `addToLayer` 的全部调用点（modlauncher 11.0.5）：`Launcher`（GAME / PLUGIN 资源）、
    `TransformationServicesHandler`（SERVICE 层提升）；**无一以 BOOT 层为目标**。
16. `[F]` `TransformerDiscovererConstants.SERVICES`（6 项）含 `ITransformationService`，不含 `ILaunchPluginService`；
    `ModDirTransformerDiscoverer` 仅将命中白名单的 `mods/` JAR 提升进 **SERVICE 层**。
17. `[F]` `Layer` 枚举父子：`BOOT=[]`，`SERVICE=[BOOT]`，`PLUGIN=[BOOT]`，`GAME=[PLUGIN, SERVICE]`。
18. `[F]` ForgeProbe JAR 内实存 `com/example/forgeprobe/ForgeProbe.class`（`mods/forgeprobe.jar` 目录清单）。

### [E] Experimental Fact

**无。** 本 Spike 为纯取证，未运行服务端，不产生 `[E]`。

### [H] Hypothesis

1. `[H]` PRE_CLASS → FIELD → METHOD → CLASS 的执行顺序（javap 偏移顺序）可能对应"类级预转换 →
   元素级 → 类级后转换"的设计语义 —— 本轮仅证明**调用顺序**，未证明设计意图。
2. `[H]` 通道 A 的第三方 transformer 命中 ForgeProbe 后，是否达成层 d 解除取决于 transformer 自身重写逻辑；
   本 Spike 未设计、未实现、未运行，不能推断。

### [D] Design Decision（交决策方）

1. `[D]` 002-0b-1b 整体 PASS（PASS-a ~ g + cleanup），符合 §九 情形 A。
2. `[D]` 建议把"通道 B 对第三方 `mods/` JAR 静态不可达"作为**限制条件**纳入 Step 2-A 的实现范围裁定（§九 情形 C）。
3. `[D]` 手册 §10 T5 / R-CLASSPROCESSOR 的 `ClassProcessorProvider` 名称更正仍待决策方回写（002-0b-1 的 [D]1 未变）。

---

## 十一、结论

| 判据 | 结果 |
|---|---|
| PASS-a 通道 A / B 的 discovery / layer / classloader 条件 | ✅ |
| PASS-b ④ 类加载触发点（含 JDK 边界前最后 Java-visible 调用点） | ✅ |
| PASS-c ⑤ class bytes 获取点 + 实际 `T` | ✅ |
| PASS-d ⑥ 静态调用点 + 实际接口方法 + 实际 `T` | ✅ |
| **PASS-e ④→⑤→⑥ 同一静态调用路径（YES）+ entrypoint 静态时序** | ✅（注册在前） |
| PASS-f `targets()` / `TargetType` 匹配规则 + ForgeProbe 静态资格 | ✅（精确匹配；ForgeProbe = YES） |
| PASS-g 通道归属有字节码级依据 + 实际被调用接口方法明确 | ✅（A+B 汇入同一转换点；A 可达 / B 对第三方不可达） |
| PASS-cleanup 无副作用 | ✅ |

**Runtime Observation: N/A** —— 本 Spike 明确禁止运行服务端，因此不产生 `[E]`。

**结论上限（禁止超出）**：

> `[F]` ④ = `Class.forName(Module, String)`（Module 来自 `FMLLoader.getGameLayer()`）；
> ⑤ = `ModuleClassLoader.getClassBytes` + `maybeTransformClassBytes`，实际 `T` = ASM `ClassNode`（PRE_CLASS/CLASS）/
> `MethodNode`（METHOD）/ `FieldNode`（FIELD）；
> ⑥ = `ITransformer.transform`（`ClassTransformer.performVote`）与
> `ILaunchPluginService.processClassWithFlags`（`LaunchPluginHandler.offerClassNodeToPlugins`）；
> 三者构成**同一静态调用路径的连续链**，且 ⑥ 的注册**先于** ④ 的类加载。
> `targets()` 为**精确匹配**（ASM Type 归一化 + 名称/描述符 equals）；
> ForgeProbe FQCN `com.example.forgeprobe.ForgeProbe` **满足静态目标条件**。
> 通道归属 = A + B 汇入同一 `ClassTransformer`；**A 对第三方静态可达，B 对第三方静态不可达**。

**证据边界（必读）**：

| 本 Spike 能证明 | 本 Spike **不能**证明 |
|---|---|
| ④⑤⑥ 静态调用点与连续链 | ForgeProbe 本次运行真的进入 transform |
| 实际 `T`（ClassNode 等）与 `targets()` 精确匹配规则 | 改写真的解除层 d |
| ForgeProbe 静态资格（匹配前提） | 层 e 真的进入 |
| 通道归属与实际被调用接口方法 | 任何第三方 transformer 的实际改写结果 |

**禁止从本报告推导**：ASM 重写可行 / 层 d 可被解除 / 全局 Forge → NeoForge 重写策略已确定 /
ForgeProbe 的字节码一定会被 transform / 002-0b Step 2-A 可执行（该裁定属架构决策方）。

**Confidence**: **HIGH**（全部结论均为精确字节码直接证据；未证部分不设置信度）

**Evidence**: §二–§八 的 `javap -p -c -constants` 原文与 SHA-256 清单。

**Hypotheses still unverified**: §十 `[H]` 1–2。

---

## 十二、边界确认

- [x] 未预设 ⑤ 的形态（由实际调用链得出 `byte[] → ClassNode`，而非预先假定）
- [x] 未因"不在白名单"直接推结论（通道 B 由 discovery / layer / `addToLayer` 全量字节码判定）
- [x] 判定链连续、可连接（④→⑤→⑥ 为同一对象的 `this` 分派 / 同一层身份）
- [x] 两处 JDK / VM 边界按 §三 记录（`Class.forName0`、`defineClass`），未跨边界反推实现细节
- [x] 未引入 ASM（分析未编译任何代码）
- [x] 未新增任何源文件到兼容层工程
- [x] 未运行服务端
- [x] 未修改任何工件（JAR / 工程 / `mods/` / 日志）
- [x] 未声称任何运行时执行状态
- [x] 未进入 002-0b Step 2-A
- [x] 未为推进 Step 2-A 补充推断

---

## 十三、禁止事项确认

- [x] 未修改 NeoForge / FML 核心
- [x] 未使用 current/main 分支源码
- [x] 未使用 IDE 自动重构功能
- [x] 未提出实现方案 / 未设计 ClassProcessor
- [x] 未改版本、未重建 JAR、未修改 `mods/`
- [x] 未进入 002-0b Step 2-A
- [x] 未进入 002-0c
- [x] 未判定 Step 2-A 可执行（属架构决策方裁定）

---

## 附录：交接建议

按 §九 属**情形 A**：

> `[D]` 002-0b-1b 完整静态链 ④→⑤→⑥（同一路径）+ `targets()` 规则明确 + 通道归属明确 + 实际 `T` 明确
> → **交回架构决策方裁定 002-0b Step 2-A 的最小实现范围**。

并附**情形 C 限制条件**（供裁定参考）：

> `[D]` 通道 B（`ILaunchPluginService`）的发现源固定为 **BOOT 层**，发生在 `Launcher` 构造期；
> 第三方 `mods/` JAR 无路径进入 BOOT 层（`addToLayer` 无 BOOT 目标；`ModDirTransformerDiscoverer` 仅提升到 SERVICE 层）。
> ⇒ 第三方实现应在通道 A（`ITransformationService#transformers()`）内提供。
> 该限制条件为静态字节码结论，**未运行验证**。

> **002-0b-1b 完成后停在此处。**
