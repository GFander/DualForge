# Spike-002-0c G-1 四项前置取证（执行方完成，待裁判采纳）

**性质**：只读取证（javap / jar metadata），**未修改任何文件**
**依据**：《Spike-002-0c 反向意见（顾问方）.md》§三 / §六（"先做四项取证，再决定修复方向"）
**执行方**：Codex（执行方角色；因裁判通道本轮两次未处理该任务，先行产出 [F] 证据供裁定）
**日期**：2026-10-07

---

## 取证 1：neoforge 系统容器的 modLoader 声明 —— `[F]`

- 制品：`D:\DualForge\spike000\neoforge-server\libraries\net\neoforged\neoforge\21.1.256\neoforge-21.1.256-universal.jar`
- `META-INF/neoforge.mods.toml` 原文（摘录）：

```
modLoader="javafml"
loaderVersion="[3,]"
...
[[mods]]
    modId="neoforge"
```

- **结论：YES** —— `neoforge` 容器声明 `modLoader="javafml"`（版本要求 `[3,]`：兼容 provider 的 `9999` 与内置 `4.0.45` 均满足）。
  G-1 归因链的**前提成立**：同名 `javafml` 覆盖后，`neoforge` 容器确实走兼容 provider。
- 对方向选择的影响：无（前提成立）；若为 NO 才需重审归因链。

---

## 取证 2：内置 javafml `loadMod` 完整行为 —— `[F]`

- 类：`net.neoforged.fml.javafmlmod.FMLJavaModLanguageProvider extends net.neoforged.fml.loading.BuiltInLanguageLoader`
- `loadMod(IModInfo, ModFileScanData, ModuleLayer)` 字节码逐段（javap 原文存档 `G1-F2-FMLJavaModLanguageProvider.txt`）：

```
scanData.getAnnotatedBy(net.neoforged.fml.common.Mod.class, ElementType.TYPE)
  .filter(ad -> ad.annotationData().get("value").equals(info.getModId()))                 // lambda$loadMod$0
  .filter(ad -> AutomaticEventSubscriber.getSides(ad.annotationData().get("dist"))
                  .contains(FMLLoader.getDist()))                                          // lambda$loadMod$1
  .sorted(Comparator.comparingInt(ad -> -getSides(ad.annotationData().get("dist")).size()))// lambda$loadMod$2
  .map(ad -> ad.clazz().getClassName())                                                    // lambda$loadMod$3
  .toList()
new FMLModContainer(info, list, scanData, layer)
```

- **结论：`loadMod` 本身 = 纯注解筛选链 + 容器构造；无 mixin / 事件总线 / 其它副作用。**
- 对方向选择的影响：**"复刻"（a）与"扩展注解筛选"（a'）的差别缩小** —— 需要复刻的仅是上述 5 步筛选链；
  顾问方"若还含其它副作用则复刻侵入度高"的顾虑被 `[F]` 排除。

---

## 取证 3：`languageProviders`（List） vs `languageProviderMap`（Map） —— `[F]`

- 类：`net.neoforged.fml.loading.LanguageProviderLoader`（javap 原文存档 `G1-F3F4-LanguageProviderLoader.txt`）
- 字段：
  - `private final List<IModLanguageLoader> languageProviders;` —— ctor 中 `ServiceLoaderUtil.loadServices(ctx, IModLanguageLoader.class)` 一次装载（全部发现者）；
  - `private final Map<String, ModLanguageWrapper> languageProviderMap;` —— `HashMap`。
- 装载路径：`languageProviders.forEach(this::lambda$new$0)`；lambda 内为
  **`languageProviderMap.put(loader.name(), new ModLanguageWrapper(loader, version))`** —— 纯 `Map.put`，**list 不删元素**。
- 解析路径：`findLanguage(...)` = `languageProviderMap.get(name)` 取当前覆盖者 → 版本矩阵校验 → 返回其 `modLanguageProvider()`。
- 附注 `[F]`：ctor 对每个 provider 的 `version()` 为 null/空白时抛 `RuntimeException`（"Found unversioned language provider"）——
  兼容 provider 的 `"9999"` 满足；`BuiltInLanguageLoader.version()` 取自其 jar 的 Implementation-Version（显示层另有 `Package.getImplementationVersion()` 问题，见裁判报告 F3.2）。
- **结论：内置 javafml 实例仍保留在 `languageProviders` list 中；map 仅决定 `findLanguage` 的胜者。**

---

## 取证 4：兼容 provider 访问该 list 的公开路径 —— `[F]`

- `LanguageProviderLoader` 的 **public** 方法：
  - `public void forEach(Consumer<IModLanguageLoader>)`
  - `public <T> Stream<T> applyForEach(Function<IModLanguageLoader, T>)`
  （二者均直接遍历 `languageProviders`）
- 实例获取：`net.neoforged.fml.loading.FMLLoader`：
  - 字段：`private static LanguageProviderLoader languageProviderLoader;`
  - 访问器：**`public static LanguageProviderLoader getLanguageLoadingProvider();`**
  （javap 原文存档 `G1-F4-FMLLoader.txt`）
- **结论：存在公开路径** —— `FMLLoader.getLanguageLoadingProvider().applyForEach(...)`（或 `forEach`）
  可遍历到内置 javafml 实例（按 `name().equals("javafml")` 且排除自身）。
- 时序：provider loader 在 FMLLoader 初始化期构建；`loadMod` 在 mod 构造期被调用——对象已就绪
  （此"时序前提"标 `[F]`-支撑的实现前提，002-0d 实现时复核）。

---

## 四项取证对修复方向 (a) / (a') / (b) / (c) 的影响（执行方汇总，供裁判裁定）

| 方案 | 取证后评估 |
|---|---|
| **(c) 委托** | **技术可行**（取证 4）：对"非 Forge 命名空间容器"调用内置实例 `loadMod`；无逻辑复制、无改名；符合"低侵入优先"——建议优先评估 |
| **(a') 扩展注解筛选** | **可行且范围已量化**（取证 2）：只需按内置语义复刻 5 步筛选链的 NeoForge 侧处理 |
| **(a) 复刻** | 与 (a') 实为同一件事的两种描述（取证 2 证明 `loadMod` 无其它副作用）；"侵入度高"的顾虑不成立 |
| **(b) Plan B 改名** | 仍为最后选项（跨组件、语义面大） |

---

## 证据文件

```
logs\spike002-0c-artifacts\
  ├ G1-F2-FMLJavaModLanguageProvider.txt     （javap -p -c -constants 原文）
  ├ G1-F3F4-LanguageProviderLoader.txt       （javap 原文）
  ├ G1-F3-BuiltInLanguageLoader.txt          （javap 原文）
  ├ G1-F4-FMLLoader.txt                      （javap 原文）
  └ G1-F1-neoforge.mods.toml.txt             （universal.jar 内 metadata 原文）
```

## 纪律声明

- 只读取证；未修改任何探针 / 日志 / 说明书 / 实验报告；
- 本文件为**执行方证据**，待裁判采纳；**未冻结 002-0d**（遵守顾问方建议）。
