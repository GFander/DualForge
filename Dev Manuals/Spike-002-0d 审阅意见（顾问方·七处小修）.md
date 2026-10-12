# 对《Spike_002-0d 说明书 v0.1（送审稿）》的审阅意见

**来源**：顾问方（经架构决策方转交）；**接收**：裁判
**日期**：2026-10-07

---

## 零、总体判断

**采纳，需 5 处小修后冻结。** 送审稿吸收了我上一轮反向意见的核心要点：

- §0.2 四项取证齐备（含 (c) 委托路径可达性）
- §五 采用 **(c) 委托方案**（不复刻、不改名）——符合 v1.1 §1.2「低侵入优先」
- §八 分界点操作化为「G-1 是否清零」——清晰
- §2D-a 恢复硬门 5（Run C 复现 G-1 签名）作为最终保真门——强约束

**无结构性缺陷。** 以下 5 处为收紧项。

---

## 一、§五 5.2 逐字定义——三处需要补明

### 补 1：`applyForEach` 的返回类型需标注

```java
FMLLoader.getLanguageLoadingProvider().applyForEach(l -> l)
```

若 `applyForEach` 返回 `Stream<IModLanguageLoader>`，则 `.filter()` 直接链式；若返回 `List`，需先 `.stream()`。

**问题**：说明书未标注返回类型。编制级预检（§五 附注）暗示编译通过，但**执行方需要知道确切类型**才能在恢复源码树上正确落地（例如是否需要 `import java.util.stream.Stream`）。

**建议**：在 §五 5.2 的代码块前加一行：

```
// 返回类型: Stream<IModLanguageLoader>（编制级预检已确认；执行方按实际签名适配）
```

若实际返回 `List`，需在代码块中加 `.stream()`。

### 补 2：`builtin` 的 `name()` / `version()` 下游影响需明确

委托后，`NeoForgeMod` 容器的 `IModInfo.getLoader()` 指向 **`builtin`（内置 javafml）**，而非兼容 provider。

**这意味着**（与 §0.2 F2-b 相关）：

- `validate` 按 `IModInfo.getLoader()` 分组时，`neoforge` 容器会归入**内置 javafml 组** → **内置 validate 会运行**
- 这与 Plan A 下（不委托）"内置 validate 不运行"是**不同状态**

**这既不是本 Spike 的修复目标，也不是本 Spike 的观测目标**——但**它是本修复的副作用**，必须在 §0.4 或 §六 观测项中**显式列为已知副作用**，避免下游 Spike（002-0e 及以后）误判。

**建议**：在 §0.4「本 Spike 不解决」下增加一条：

```
- 不在本 Spike 评估「委托后内置 validate 恢复运行」的行为变化（属下游观测项）
```

并在 §六 观测项中追加第 10 条：

```
10. 委托生效后，neoforge 容器的 IModInfo.getLoader() 实际指向（预期 = builtin FMLJavaModLanguageProvider）
```

### 补 3：委托分支的 return 应显式保留 fallback 语义

§五 5.1 的路由：

```java
if (entrypoints.isEmpty()) {
    // 5.2 + 5.3
}
return new FMLModContainer(info, entrypoints, scanData, layer);
```

**当前语义**：如果 `entrypoints.isEmpty()` 但委托分支**未 return**（理论上不会，但结构上允许），会 fall through 到 `new FMLModContainer(info, [], ...)`。

**建议**：在 5.3 的末尾显式说明「本分支以 `return` 结束，不 fall through」，或者在 5.1 的 if 块末尾加 `else { return new FMLModContainer(...); }` 结构。

**不影响语义，但避免歧义。**

---

## 二、§2D-c 期望对照表——1 处需放宽

### 表 1：`loadMod called for neoforge entrypoints=[]` 的出现次数

当前表述可能被读作「每轮**恰好**一行」。但 FML 对 `neoforge` 容器的 `loadMod` 调用可能多次（例如扫描阶段 / 验证阶段各一次）。

**建议**：改为：

```
1 | loadMod called for neoforge entrypoints=[] | 至少 1 行（对照 002-0c 的 line 69；如实际多次，全量记录）
```

## 三、§七 PASS 判据——1 处需操作化

### PASS-002-0d-1 第 4 条

> `[E]` Run T 不再出现 G-1 同链 FATAL（`Failed to wait for future Registry initialization`），且日志明确越过 `Registry initialization`；

**「日志明确越过 Registry initialization」是模糊表述。** 建议操作化为 2 条之一：

```
- [E-1] `Failed to wait for future Registry initialization` 完全缺席
- [E-2] `FMLCommonSetupEvent`（或 `FMLLoadCompleteEvent` / `Done` / 其它后续阶段的原文行）
        出现（任一作为「越过」的可验证信号）
```

**建议在判据中显式列出这两个信号**，避免执行方 / 裁定方对「越过」理解不一。

---

## 四、§0.2 表格第 4 行——1 处标签需降级

### 当前

| 4 | `FMLLoader.getLanguageLoadingProvider()`（public static）+ `forEach/applyForEach`（public，均遍历 list） | (c) 委托存在公开路径（API 层 `[F]`；运行层待本 Spike 验证） |

**「API 层 `[F]`」过强。** `[F]` = 本项目字节码直接证明。API 的**签名**可以由 javap 取证，但「**可访问性**」（例如跨层是否可达、是否需要 `opens`）**不是 [F]**。

**建议**：改为：

```
(c) 委托存在公开路径（API 签名 [F]；跨层可访问性 [H]，待本 Spike 2D-e 运行层验证）
```

---

## 五、§八 分界点——1 处需要双重澄清

### 当前

> **G-1 清零之后**出现的生命周期间新断层 = 预期的新终点（记录、PASS 路径、不修复）；
> **在 `NeoForge mod loading` 之前**出现且与委托机制相关的未预登记异常 = STOP。

**问题**：边界情形未覆盖——

- **在 `NeoForge mod loading` 之前**、**与委托机制无关**的异常（例如 FML 内部某处新 bug）
- **在 `NeoForge mod loading` 之后**、**与委托机制相关**的异常（例如 `builtin.loadMod` 内部抛异常）

**建议**：分界点以**「G-1 是否清零」**为唯一标准（说明书 §八 末尾已提到，但未在 §八 主表中贯彻）：

```
- G-1 清零（`NeoForge mod loading` 命中）之后的任何异常 → PASS 路径（记录、不修复）
- G-1 未清零（`NeoForge mod loading` 未命中）之前的任何异常：
  - 若与委托机制相关（delegate 行缺失 / candidates ≠ 1 / 跨层访问错误）→ STOP
  - 若与委托机制无关 → 记录为「前置漂移」，STOP（由裁定方判断是否需重建基线）
```

这覆盖了「与委托无关但发生在清零前」这一边界情形。

---

## 六、其他确认（不改动）

| 项 | 结论 |
|---|---|
| §0.1 G-1 定义 | ✅ 与 002-0c 报告一致 |
| §0.3 为什么在 002-0e 之前 | ✅ 逻辑成立 |
| §0.4 不解决的问题 | ✅ 范围清晰 |
| §0.5 职责分离表 | ✅ |
| §2.3 K1 control 必须也由恢复源码产出 | ✅ 符合「不预设」原则 |
| §三 工作副本隔离 | ✅ |
| §2D-a 5 个硬门 | ✅ 硬门 5（Run C 复现 G-1）为最终保真门 |
| §2D-d 强制 javap 对照 | ✅ |
| §五 5.4 禁止事项 | ✅ 完整 |
| §五 5.5 语法适配边界 | ✅ |
| §七 判据分级 | ✅ |
| §九 输出格式 | ✅ |
| §十一 交付清单 | ✅ |
| §十二 真正使命 | ✅ |

---

## 七、冻结建议

**若采纳以下 5 处小修，则 v0.2 可冻结为 v1.0：**

| # | 位置 | 修 |
|---|---|---|
| 1 | §五 5.2 | 补 `applyForEach` 返回类型标注；若为 `List`，代码块加 `.stream()` |
| 2 | §0.4 / §六 | 追加「委托后内置 validate 恢复运行」为已知副作用 + 观测项第 10 条 |
| 3 | §五 5.1 | 补「本分支以 `return` 结束，不 fall through」说明 |
| 4 | §2D-c 表 1 | 改为「至少 1 行」 |
| 5 | §七 PASS 第 4 条 | 操作化为 E-1 / E-2 两条可验证信号 |
| 6 | §0.2 表 4 | `[F]` → `API 签名 [F]；跨层可访问性 [H]` |
| 7 | §八 分界点 | 主表纳入「G-1 是否清零」为唯一标准，覆盖边界情形 |

**（原意 5 处，实际 7 处——第 6、7 为扩展澄清项，可一并采纳。）**

---

## 八、给用户的裁定建议

```
[1] 采纳送审稿 v0.1 + 上述 7 处小修 → 冻结为 v1.0
[2] 交 Codex 执行 2D-a → 2D-f
[3] 执行报告交回后，由决策方判定 PASS-002-0d-1 / VALID / FAIL / STOP
[4] 若 PASS / VALID → 起草 002-0e（含我上一轮建议的两条纪律：
    语义级字节码对比 + P1/P2 双预登记）
[5] 若 STOP → 由决策方裁定前置 / 机制问题
```

**我建议采纳并冻结。** 送审稿的整体质量足以支撑执行，7 处小修为「收紧而非重构」，不影响执行路径。
