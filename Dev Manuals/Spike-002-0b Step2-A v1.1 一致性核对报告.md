# Spike-002-0b Step 2-A v1.1 一致性核对报告（STOP：前置 3 材料缺口）

**接收方**：架构决策方（原样交回）
**核对依据**：v1.1 冻结交付说明（2026-10-07）· 纪律 3（"执行时若发现 v1.1 描述与 001b-6 / 002-0a 材料不一致，停止并交回——不得自行'合理化'"）
**核对日期**：2026-10-07
**核对性质**：纯只读（javap / 报告比对 / 源码比对）——**未冻结 v1.1、未修改任何工程文件、未构建、未运行**

**总体结果**：**STOP —— 前置 3（"真实 ModFileInfo"路径）存在材料级缺口，无法按 v1.1 现有文本直接实现；另有"4 处替换文本"的投递缺口待确认。**

---

## 一、核对范围与方法

按纪律 3 执行冻结前材料一致性核对。材料源：

- 《Spike-001b-6 实验报告》§四 / §五（"001b-6 已验证路径"）
- 《Spike-001c-1 实验报告》§二（前置配置确认）
- 《Spike-002-0a 实验报告》与 002-0a 运行日志
- 当前基线源码（`spike001a\compat`，= 001b-1 基线）
- 冻结工件 javap（`loader-4.0.45.jar` / `modlauncher-11.0.5.jar`）

---

## 二、逐项核对结果

| # | 前置 | 与材料一致性 | 当前可实施性 | 结论 |
|---|---|---|---|---|
| 1 | ForgeCompatLanguageLoader（name/version/loadMod 形态） | 一致（001b-6 line 136：name=javafml / version=9999；002-0a `FMLModContainer constructed` 证据） | 可实施（接口签名已复核，见 §2.1） | ✅ |
| 2 | IModLanguageLoader service 声明 | 一致（001b-6 line 199 清理项佐证） | 可实施（单行文件） | ✅ |
| 3 | ForgeModFileInfoParser → 真实 ModFileInfo | 调用点与材料一致；但**依赖项定义缺失**（见 §2.3） | **无法直接实施** | ⛔ |
| 4 | 空壳 IEventBus | 一致 | **已具备**（v1 探针在构建产物中，javap 无成员） | ✅ |

### 2.1 前置 1 —— ✅ 一致，可直接实施

- `name()` = `"javafml"`、`version()` = `"9999"` 与材料一致（001b-6 §五 line 136；001c-1 §二）。
- 接口签名已从 `loader-4.0.45.jar` 复核：
  `IModLanguageLoader.loadMod(IModInfo, ModFileScanData, ModuleLayer) → net.neoforged.fml.ModContainer`（可抛 `ModLoadingException`，本实现可不抛）；
  `FMLModContainer` 构造器 `public (IModInfo, List<String>, ModFileScanData, ModuleLayer)`、类 `extends ModContainer`；
  `ModFileScanData.AnnotationData.annotationType()` / `clazz()`（均为 `org.objectweb.asm.Type`）。
- 冻结文本的 5 步 `loadMod` 可实现，且"正常返回、不得抛异常"与 001b-6 的教训一致（001b-6 的观测桩曾抛 `UnsupportedOperationException("Observation only")`，属反面案例）。

### 2.2 前置 2 —— ✅ 一致

单行 service 文件（`com.example.forgecompat.ForgeCompatLanguageLoader` + 换行）。

### 2.3 前置 3 —— ⛔ 材料级缺口（本次 STOP 的直接原因）

v1.1 冻结文本：

```java
// build(IModFile file) 的返回值：
return new ModFileInfo(modFile, ForgeModFileInfo.configOf(parsed), info -> { });
```

**缺口 1（阻断）**：`ForgeModFileInfo.configOf` 的**定义**不存在于任何现存材料：

1. 调用点与材料一致 —— 001b-6 §五 line 128–129、001c-1 §二 line 28 均为此调用；
2. 但 001b-6 §五自述该实现为"**临时，已撤销**"改动，且两份报告只记录了**调用行**，未记录 `configOf` 的方法体；
3. 全盘检索（`D:\DualForge`，.java / .txt / .patch）：**没有任何文件包含 `configOf` 的定义**；
4. 该时期的 forgecompat jar 快照不存在（现存仅 `001a0` / `001b1` / `002-0b-2a`），无法从产物反查；
5. 当前基线源码（001b-1）的 `ForgeModFileInfo` **没有**该方法：其文件级 config 为实例字段 `IConfigurable config = new MapConfigurable(parsedToml)`，`MapConfigurable` 为**私有静态嵌套类**。

**缺口 2（次要）**：冻结文本中的标识符 `modFile` 需要 `net.neoforged.fml.loading.moddiscovery.ModFile` 类型的声明行；001b-6 §五仅说明"`IModFile.create(secureJar, parser)` 的返回值本身就是 `ModFile`（`instanceof` 运行期成立）"，**未给出该 cast 行的原文**。

**影响**：按 v1.1 现有文本无法直接编译实现——补齐需要执行方"自写"缺失成员（违反"不得自行解释前置"），或由决策方补发定义。

**连带影响**：v1.1 §五 执行账目"修改 2 源文件（build.gradle + parser）"在该缺口下需要重算：如恢复 `configOf`，则至少 **+1 个被修改文件**（`ForgeModFileInfo.java`），且 §八 清理项需相应增加。

### 2.4 前置 4 —— ✅ 已具备

v1 探针已包含 `net.minecraftforge.eventbus.api.IEventBus` 空壳（javap 验证无成员），且构建产物在 `forgecompat-002-0b-2a.jar` 中。

### 2.5 "4 处替换文本"投递核对 —— ⚠ 待确认

- 本轮说明称"四处替换的具体文本已在上轮交付（§四 替换 1–4）"；**实际上一轮的 §四 替换 1–6 是 v1 的文本**（已应用并冻结于 v1）。
- 本会话中可**逐字采用**的 v1.1 内容：§0.3 四项前置精确形态、纪律 2 的三阻断点文字、基线归属说明。
- 未收到逐字原文的部分：§2A-c"弹性预检"措辞、§八 新增 3 项、附录 A.2 第 9/10 项（仅有定位级描述）。
- 待确认：**补发原文**，或**授权按本轮定位表构造**（构造稿可先交回供核，不先冻结）。

---

## 三、STOP 判定

- 依据：纪律 3（"发现不一致 → 停止并交回，不得自行'合理化'"）+ "不得自行解释前置"。
- 结果：**停止**。未冻结 v1.1；未执行"加入四项共同前置 / 重构建 / Run C / Run T"的任何一步；v1 现场（含 `logs\spike002-0b-2a-control\`）原样保留。

---

## 四、最小修复选项（供裁定）

**前置 3 缺口**：

- **选项 A（拟稿，待你确认或替换）**：授权执行方按下稿补齐（修改文件账目 2 → 3）：

```java
// ForgeModFileInfo.java（新增；语义由现有 MapConfigurable 唯一确定）
static IConfigurable configOf(Map<String, Object> parsed) {
    return new MapConfigurable(parsed);
}
```

```java
// ForgeModFileInfoParser.build() 内（cast 行；import net.neoforged.fml.loading.moddiscovery.ModFile）
ModFile modFile = (ModFile) file;
return new ModFileInfo(modFile, ForgeModFileInfo.configOf(parsed), info -> { });
```

- **选项 B**：补发 001b-6 原始 `configOf` 定义（若仍可取得）——执行时逐字采用。

**替换文本（§2.5）**：

- 选项 ①：补发四处替换的逐字原文；
- 选项 ②：授权按本轮定位表构造（构造稿先交回供核）。

---

## 五、未改动声明与现场

- 本次仅做只读核对（javap / 报告与源码比对）；**未修改任何文件**（v1 探针 4 文件、`build.gradle`、`run-server.ps1`、构建产物均维持 v1 STOP 时的状态）。
- 证据存档：`logs\spike002-0b-2a-artifacts\2A-v1.1-precheck-evidence.txt`（含材料摘录、全盘检索结果、javap 签名）。

---

## 六、待裁定清单

1. **前置 3 缺口**：选项 A（授权拟稿 + 账目 2 → 3）/ 选项 B（补发原始定义）/ 其它；
2. **替换文本投递**：补发原文 / 授权按定位表构造；
3. 其余 v1.1 元素（前置 1 / 2 / 4、判定纪律、执行清单框架）均已核对通过——第 1、2 项裁定后即可冻结 v1.1 并执行 Run C → Run T。

---

## 七、事实标签

**[F]**：`ForgeModFileInfo.configOf` 的定义不存在于任何现存材料（检索范围：全部 Dev Manuals + 全盘 .java/.txt/.patch + 全部现存 jar 快照）。
**[F]**：`IModLanguageLoader` / `FMLModContainer` / `ModFileInfo` / `AnnotationData` 签名与 v1.1 前置 1 / 3 的使用方式兼容（javap 原文见证据文件）。
**[E]**：无（本次未运行）。
**[H]**：无新增。
**[D]**：STOP 判定；待裁定项见 §六。
