# 对《Spike-002-0c 裁判审查与裁定报告》的反向意见

**来源**：顾问方（经人类转交）；**接收**：裁判
**日期**：2026-10-07

---

## 零、总体判断

审查质量高。 13 项独立核验 + 三条关键发现 + 四项裁定，纪律严格。F2 的 G-1 归因链闭合，且把 001b-3 遗留的 [E]（同名覆盖 = 情况 A）升级为 [D]（前置缺陷认定），这是本次审查最有价值的动作。

F1 是本次审查最重要的发现：v1.1 已含同一 Registry 失败——把 002-0c 从"发现新断层"降级为"确定性复现"。这改变了下游结论的语义（详见 §一）。

但有 3 处需要补充或修正。

---

## 一、F1 的语义影响——报告没有推到底

F1 承认"该失败并非 002-0c 首次产生，而是两轮确定性复现"，但未说透：

```
002-0b-2A v1.1：FML 止步于 Registry initialization（未分析）
        ↓
002-0c：同一失败（正式纳入判定）
        ↓
结论：002-0c 没有推进新边界——它把 v1.1 的"沉默失败"正式承认为"生命周期终点"
```

这意味着：

· 002-0c 的 PASS 成立（报告维持 PASS 正确）
· 但 002-0c 的实际贡献是"暴露 G-1"——而非"观测到生命周期终点"
· 真正的下一步不是方向 2，而是 002-0d（修复 G-1）——报告裁定 2 的处置正确

建议在报告中明确：

002-0c 的核心产出 = 承认 G-1 存在于 v1.1 已发生的运行中；其"生命周期事实基线"仅对"存在 G-1 的基线"成立。修复 G-1 后需重新建立零变量基线（002-0d 的 PASS 判据已含此）。

---

## 二、F2 的归因链——需要补一条取证

报告的 F2 逐环标注清楚，但前提未取证：

缺环：neoforge-21.1.256-universal.jar 的 mods.toml / neoforge.mods.toml 是否声明 modLoader="javafml"？

· 若为 YES → neoforge 容器确实走 javafml → 走兼容 provider → 归因成立 ✓
· 若为 NO（例如 NeoForge 自身用其它 loader） → 归因链断裂，需重新审视

我预期是 YES（因为 languageProviderMap["javafml"] 覆盖后，所有 javafml 容器都受影响），但这是证据纪律——不能靠预期。建议 002-0d 说明书的第一项取证就是读 universal.jar 的 metadata。

另一条需要补的：FMLJavaModLanguageProvider.loadMod 的完整行为——报告假定它"只筛选注解"，但这是未取证的 [H]：

· 若它只筛选 net.neoforged.fml.common.Mod → 修复 (a') 只需扩展注解筛选
· 若它还包含 mixin 配置 / 事件总线注册 / 其它副作用 → "复刻"(a) 需复制全部逻辑，侵入度高

这直接决定 002-0d 的修复方向，必须在 002-0d 说明书中作为前置取证给出。

## 三、裁定 2 的修复方向——报告漏了一个候选

报告给出 (a) 复刻 / (b) Plan B 改名。我认为需要评估 (c) 与 (a')：

| 方案 | 机制 | 侵入度 | 障碍 |
|---|---|---|---|
| (a) 复刻 | 兼容 provider 重新实现内置 javafml 对 neoforge 容器的处理 | 高（"复刻"= 重新实现） | 违反 v1.1 §1.2"低侵入优先" |
| (a') 扩展注解筛选 | 兼容 provider 同时筛选 Forge @Mod + NeoForge @Mod | 中 | 若内置 loadMod 不止筛选注解，(a') 不完整 |
| (b) Plan B 改名 | 兼容 provider 用 forge_compat，ForgeModFileInfoParser 重写 modLoader | 中（跨组件） | 改动面大；影响 IModInfo.getLoader().name() |
| (c) 委托 | 兼容 provider 在 loadMod 中委托给内置 javafml 实例 | 低 | 内置实例是否可达？ |

方案 (c) 的关键技术问题：

LanguageProviderLoader.languageProviders 是 List<IModLanguageLoader>（列表），languageProviderMap 是 Map<String, ModLanguageWrapper>（Map）。

· Map.put 覆盖的是 map
· 内置 javafml 实例仍留在 list 里

若此推断成立：兼容 provider 可通过（公开或反射）访问 list，找到内置实例，在"非 Forge 命名空间容器"上委托给它。这是最干净的路径——避免"复刻"，符合"低侵入优先"。

建议 002-0d 说明书前置取证项：

1. neoforge-21.1.256-universal.jar 的 modLoader 声明值
2. FMLJavaModLanguageProvider.loadMod 的完整行为（是否只有注解筛选）
3. LanguageProviderLoader.languageProviders 是否含内置 javafml 实例（列表 vs map）
4. 兼容 provider 访问 list 的公开路径（若有）

基于四项取证结果，再决定 (a) / (a') / (b) / (c)。

报告裁定 2 的修复方向"预登记"为 (a) 之前，需要先做这四项取证——这是关键遗漏。

---

## 四、裁定 3 的 002-0e 探针——两个漏洞

**漏洞 1：单变量不严格——"源码 + 重建"改变了整个 jar**

报告说"仅 forgeprobe.jar（源码 + 重建）变化"。但重新编译会：

· 改变所有 class 文件的常量池索引
· 可能改变其它方法体的字节码（即使逻辑未变）
· 可能导致整个 jar 的 hash 变化

这与"单变量"的严格定义有偏差。

建议：

1. 002-0e 说明书中明确"源码级唯一变量 = 新增一条语句"
2. 报告 §五 强制要求对比新旧字节码——确认：
   · marker 三行的 System.out.println 序列指令级未变
   · 唯一新增的是 MinecraftForge.EVENT_BUS.register(this) 对应的 3–4 条指令
3. 用 javap -p -c -constants dump 对比作为强制字段

**漏洞 2：预期失败类型是 [H]，不是 [F]**

报告预登记预期 NoClassDefFoundError: net.minecraftforge.common.MinecraftForge，依据是"转换器仅重写 <init> 描述符"。这个依据成立，但首个失败点可能不是 NCDFE：

候选失败点：

```
① Class loading: MinecraftForge.class 加载失败
   → NoClassDefFoundError: net/minecraftforge/common/MinecraftForge   [预期]

② Field resolution: EVENT_BUS 字段解析失败
   → 若 MinecraftForge 类存在但字段缺失（不太可能）
   → NoSuchFieldError: EVENT_BUS

③ Return type resolution: EVENT_BUS 返回类型解析
   → 若空壳 IEventBus 存在（前置 4），无问题
   → 若空壳 IEventBus 不存在，NCDFE

④ Method resolution: register 方法解析
   → 若 EVENT_BUS 是空壳 IEventBus（无 register 方法），
     则 invokevirtual 会 NoSuchMethodError
```

关键观察：前置 4 的空壳 IEventBus 无任何方法。所以若成功加载 MinecraftForge 并取得 EVENT_BUS 字段，则 register 调用会必然抛 NoSuchMethodError。

因此首个失败可能是：

· NoClassDefFoundError: net.minecraftforge/common/MinecraftForge（首选，若类不存在）
· NoSuchMethodError: net.minecraftforge/eventbus/api/IEventBus.register（若类存在但方法缺失）

报告应预登记两种可能，并接受任一为 PASS-002-0e-1 的形态，只要它能确定一个新的命名空间缺失坐标。

另一个细节：报告说"位置在 marker 三行打印之后"——这意味着 BUS = ... 已经打印。这是正确的——marker 命中后再触发 NCDFE，与 002-0e 的观测点一致。

## 五、对报告 §七 三点的正式回答

**问题 1：G-1 归因链是否存在反例？**

归因链闭合，无反例。

但需补一条取证：neoforge-21.1.256-universal.jar 的 metadata modLoader 声明。

**问题 2：修复方向 (a) 是否优于 (b)？**

(a) 优于 (b) 成立，但报告漏了 (a') 和 (c)：

· (c) 委托（若技术可行）优于 (a) —— 符合"低侵入优先"
· (a') 扩展注解筛选居中 —— 需要先确认内置 loadMod 是否只筛选注解
· (a) 复刻是次优 —— 违反低侵入原则
· (b) Plan B 是最后选项 —— 改动面大

建议 002-0d 说明书先做四项取证（见 §三），再决定方向。不预设 (a)。

**问题 3：002-0e 探针契约是否有漏洞？**

有 2 处：

1. 单变量不严格 —— 需要字节码级"仅新增一条语句"的验证
2. 预期失败类型是 [H] 而非 [F] —— 应预登记 NCDFE: MinecraftForge 与 NoSuchMethodError: IEventBus.register 两种可能

---

## 六、给裁判的具体建议

**建议 1：002-0c 报告不回改，但在 002-0d 说明书中明确 F1 的语义**

```
002-0c 的 PASS 成立，但贡献是"承认 G-1"，
而非"观测到生命周期终点"。
零变量基线（生命周期事实基线）在 G-1 修复前不成立。
```

**建议 2：002-0d 说明书以"四项取证"开头**

```
取证 1: neoforge-universal.jar 的 modLoader 声明值
取证 2: FMLJavaModLanguageProvider.loadMod 的完整行为
取证 3: LanguageProviderLoader.languageProviders 是否含内置 javafml 实例
取证 4: 兼容 provider 访问 list 的公开路径（若有）

→ 基于取证结果选择 (a) / (a') / (b) / (c)
```

**建议 3：002-0e 说明书补两条纪律**

```
纪律 1: 源码级"仅新增一条语句"必须由 javap 字节码对比验证
纪律 2: 首个失败点预登记两种类型（NCDFE / NoSuchMethodError），
        任一为 PASS-002-0e-1 的合格形态
```

---

## 七、总结

| 项 | 报告结论 | 我的反向意见 |
|---|---|---|
| F1 | 确定性复现 | ✅ 成立，但需推到底：002-0c 贡献 = "承认 G-1"，非"终点" |
| F2 | G-1 归因 | ✅ 闭合，需补一条取证（universal.jar 的 modLoader 声明） |
| 裁定 2 修复方向 | (a) vs (b) | 报告漏 (a') / (c)；应先做四项取证再选 |
| 裁定 3 探针 | 单变量 | 源码重建改变整 jar；需字节码对比验证 |
| 裁定 3 预期 | NCDFE | 应预登记两种失败类型（NCDFE / NoSuchMethodError） |
| F3 三注 | 显示值 vs 功能值 | ✅ 全部正确，尤其 F3.2 |

核心结论：报告方向正确，维持 PASS-002-0c-1 与 002-0d → 002-0e 的路径成立。但 002-0d 的修复方向未充分取证，002-0e 的单变量与预期需要收紧。

建议：不立即冻结 002-0d 说明书——先做四项取证（universal.jar metadata / FMLJavaModLanguageProvider.loadMod 行为 / languageProviders 列表构成 / 委托路径可达性），基于取证结果决定修复方向。
