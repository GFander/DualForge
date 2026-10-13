> Codex回写说明：本文件以用户上传的验收审查报告为输入。报告内[E]日志/终端内容是用户提供的控制台摘录，Codex未取得或独立读取完整原始控制台、latest.log、debug.log、截图或诊断ZIP；[U]是用户报告中的明确人工确认。不得把本文件当作这些原始附件。
> 输入报告SHA-256：`e2a45d626225f696bd8cac207a422bf56cc9a8d145f075f43f5e2f3d8bed14fb`。起点HEAD：`6ea5b965ec15068d55dcfac452cf504a5f254e06`。本轮仅文档回写，未重新执行JVM或客户端。

# DualForge — Convergence-006A-C 客户端人工验收报告

**报告日期：** 2026-10-14（Asia/Shanghai，UTC+8）

**接收方：** Codex

**项目：** DualForge

**关联 PR：** https://github.com/GFander/DualForge/pull/9

**任务：** Convergence-006A — 受控 Forge 多 Item 注册

**本轮结论：006A-C 受控 macOS 客户端人工验收 PASS。**

本次验收针对 **Beta 贴图更新版 Forge fixture**，而不是原始 006A-S 冻结 fixture。必须分别保存两种制品的证据身份，不得混淆。

## 一、证据标签

- **[E]** 用户实际 Mac 客户端日志、终端命令及执行输出。
- **[U]** 用户亲自完成的游戏内人工检查及明确确认。
- **[S]** 仓库已有源码、制品变更记录及静态字节对照。
- **[D]** 本轮验收的支持边界和状态裁定。

原始输入包括：

1. 用户回传的 Prism Launcher 完整控制台文本。
2. 用户在 macOS 执行 `shasum -a 256` 的实际输出。
3. 用户明确确认剩余人工验收项目全部通过。
4. PR #9 的《Convergence-006A Beta 贴图更新》及原 006A 交付记录。

当前未提供独立截图文件、原生 latest.log/debug.log 或本地诊断 ZIP。不得虚构这些附件。

## 二、真实客户端环境

[E] 由 Prism Launcher 控制台确认：

| 组件 | 实测版本 |
|---|---|
| 操作系统 | macOS 27.0 |
| CPU | Apple M4 / arm64 |
| GPU | Apple M4 |
| Prism Launcher | 11.1.1 official |
| Minecraft | Java Edition 1.21.1 |
| NeoForge | 21.1.256 |
| FancyModLoader | 4.0.45 |
| ModLauncher | 11.0.5 |
| Java | Microsoft OpenJDK 21.0.7+6-LTS |
| Java 架构 | aarch64 |
| LWJGL | 3.3.3 |
| Launch Target | forgeclient |

实际测试实例名称为 `DualForge-005-Mac`，但该名称仅是 Prism 实例目录标识，**不代表本轮使用旧兼容层制品**。

实际加载：

- `dualforge.jar`
- `multi-registry-probe.jar`

未发现其他用户模组列于启动器的 Mods 清单。

实际 JVM 开关：

```text
-Ddualforge.2a.rewrite=true
-Ddualforge.0g.apiRewrite=true
-Ddualforge.0j.emptyRegistration=true
-Ddualforge.0l.subscriberRewrite=true
-DDF006_REGISTER=true
-Ddualforge.dump.path=dualforge-006a-evidence/rewritten.class
-Ddualforge.dump.profileDir=dualforge-006a-evidence/profile
```

测试时间约为 2026-10-14 05:25–05:27（UTC+8）。

## 三、实际制品身份

[E] 用户在 Mac 终端执行：

```bash
shasum -a 256 \
  "$MODS/dualforge.jar" \
  "$MODS/multi-registry-probe.jar"
```

得到：

**DualForge 兼容层**

```text
f03d86331830c57c55915c611143d864eaef6d2dbf25cc3cf1628c39015a3a1c
```

与 006A-S 最终生产兼容层 SHA 完全一致。

**原生 Forge 双 Item fixture**

```text
9094b4f1d83f5a6355bfb4bf7ae775964a8067d8663c16a292c9685da2ededd9
```

该值不同于原 006A-S 冻结 fixture：

```text
4099fb08fef19891eb95ca2fcce52300fb428f1faca4b562200dd081f562d904
```

但不是未知差异。

[S] PR #9 已有《Convergence-006A Beta 贴图更新》记录：

- 新 fixture SHA 恰好为 `9094b4f1…2ededd9`。
- 仅替换 `assets/multiregistryprobe/textures/item/beta_item.png`。
- Alpha 贴图与原 005 冻结资源保持不变。
- 所有 Java class、Forge metadata 和其他 JAR 成员原字节保持不变。
- 更新版未重新编译 Java，也未修改 DualForge 生产代码。
- 静态资源对照与 ZIP 校验已通过。

因此确认：

**本轮 Mac 实测是已记录的 Beta 贴图资源更新 cohort，且兼容层与 006A-S 是相同二进制。**

[D] 原 006A-S PASS 仍属于原冻结 fixture。Beta 资源更新版获得的是本次独立客户端实际运行证据，不得将原服务端实验记录修改成新版 fixture 的原生 C/T 执行记录。

## 四、真实 ModLauncher / FML 加载证据

[E] 控制台记录：

```text
[DUALFORGE-XFORM] onLoad() called, otherServices=[mixin, fml, dualforge-transform]
[DUALFORGE-XFORM] initialize() called, name=dualforge-transform
```

说明新的 DualForge TransformationService 在真实 macOS 客户端中被加载并初始化。

Forge Locator 实际发现：

```text
ForgeModLocator: adding candidate .../multi-registry-probe.jar
ForgeModReader: found META-INF/mods.toml -> building SecureJar + IModFile
```

ModLauncher / FML 列出：

```text
Multi Registry Probe 1.0.0 (multiregistryprobe)
```

语言加载器实际执行：

```text
[DUALFORGE-LANG] loadMod called for multiregistryprobe
entrypoints=[com.example.multiregistrycheck.MultiRegistryProbe]
```

实际 Transformer 命中：

```text
[DUALFORGE-XFORM] offered: com/example/multiregistrycheck/MultiRegistryProbe

[DUALFORGE-XFORM] REGISTRY_PROFILE
createHits=1
supplierHits=2
modBusAttachHits=1
holderGetHits=2
supplierImplementationHits=2
```

转写后的类成功输出：

```text
[DUALFORGE-XFORM] PROFILE dump OK
path=dualforge-006a-evidence/profile/MultiRegistryProbe.class
bytes=3168
```

**裁定：客户端实际执行了受控双 Item 字节码转换，而不是只发现了一个 Forge JAR。**

## 五、实际 Forge 构造器、Mod Bus 与 Supplier

[E] 控制台记录：

```text
[DF006] CONSTRUCTOR REACHED
[DF006] INJECTED MODBUS=1980433688
[DF006] ATTACH RETURNED

[DF006] ALPHA SUPPLIER REACHED
[DF006] BETA SUPPLIER REACHED
```

这些记录证明：

1. 原生 Forge 模组入口构造器在真实客户端执行。
2. 构造器收到注入的事件总线参数。
3. `DeferredRegister.register(modBus)` 挂接路径返回。
4. Alpha 和 Beta 两个 Supplier 均实际执行。

注意，客户端日志没有服务端独立查询 Driver 的完整调用栈观测；真实 RegisterEvent/FML 调用栈的完整证明仍属于 006A-S 原始服务器证据。

不能把客户端 `System.out` 标记冒称为完整官方注册栈。

## 六、游戏内实际物品验证

[E] 客户端成功启动单人世界：

```text
Starting integrated minecraft server version 1.21.1
```

游戏实际确认 Alpha 物品发放成功：

```text
已将1个[DualForge Alpha 物品]给予玩家
```

Beta 物品也实际发放成功：

```text
已将1个[DualForge Beta 物品]给予玩家
```

用户的验证命令为：

```mcfunction
/give @p multiregistryprobe:alpha_item 1
/give @p multiregistryprobe:beta_item 1
```

其中 Alpha 的命令输入与发放结果均可在控制台定位；Beta 的发放成功结果可在控制台定位，但本次文本未完整保留 Beta 的输入命令行。

[U] 用户另外明确确认以下人工检查全部通过：

- 两个不同物品均成功获取。
- Alpha 和 Beta 可以同时出现在背包中，互不覆盖。
- 两个物品的背包图标实际正确。
- 两个物品的手持显示实际正确。
- 没有观察到紫黑缺失纹理。
- 英文名称均显示正确：
  - `DualForge Alpha Item`
  - `DualForge Beta Item`
- 中文名称均显示正确：
  - `DualForge Alpha 物品`
  - `DualForge Beta 物品`

**证据性质：** 中文物品发放名称有控制台直接支持；背包、手持、双物品共存与英文显示为用户明确人工确认。没有收到独立截图文件，禁止将这些项目标记为“已通过截图独立复核”。

## 七、退出和错误分析

[E] 实际启动后可以进入世界、保存游戏并正常结束进程：

```text
Stopping server
Saving players
Saving worlds
All dimensions are saved
Stopping!
进程退出，代码：0。
```

本轮控制台存在一条需要保留的 ERROR：

```text
[05:27:39] [Sound engine/ERROR] [mojang/OpenAlUtil]:
Stop: Invalid name parameter.
```

同时存在普通资源、声音和 Shader WARN。

该 ERROR 位于退出阶段，来源是 OpenAL 音频子系统，当前日志没有显示它与 DualForge Registry 转换之间的因果关系。

因此：

- 不得填写“客户端无任何 ERROR”。
- 可以填写“未观察到 DualForge Registry/FML 加载失败或客户端崩溃”。
- 进程退出代码为 0，世界保存成功。
- 不需要仅凭该音频错误修改兼容层生产代码。
- 如未来持续复现，可单独进行纯 NeoForge 对照，不纳入本次 Registry 修复范围。

## 八、006A-C 最终裁定

| 验收项目 | 状态 | 证据 |
|---|---|---|
| 新 DualForge 兼容层 SHA | PASS | [E] Mac shasum |
| Beta 资源更新 fixture 身份 | PASS | [E] shasum + [S] 更新记录 |
| macOS 原生客户端加载 | PASS | [E] Prism 控制台 |
| Forge Locator / Reader / FML | PASS | [E] 实际日志 |
| 双 Item Transformer 命中 | PASS | [E] create=1 / supplier=2 / holder=2 |
| 构造器和注册总线挂接 | PASS | [E] System.out |
| Alpha/Beta Supplier 执行 | PASS | [E] System.out |
| 两个 Item 实际获得 | PASS | [E] 游戏发放结果 |
| 两 Item 同时存在 | PASS | [U] 人工确认 |
| 背包与手持贴图 | PASS | [U] 人工确认 |
| 中英文名称 | PASS | [E] 中文日志 + [U] 双语言确认 |
| 保存及正常退出 | PASS | [E] 退出代码 0 |
| 独立截图文件归档 | 未提供 | 不影响已明确的人工确认，但限制第三方视觉复核 |
| 新资源版独立服务端 C/T | NOT RUN | 原 006A-S 属于原冻结 fixture |
| Intel Mac | NOT RUN | 本轮仅 Apple M4 / arm64 |

**[D] 结论：Convergence-006A-C 在实际 macOS arm64 / Java 21 / NeoForge 21.1.256 / Beta 资源更新版双 Item fixture 上受控人工验收 PASS。**

该结论不代表任意 Forge 模组、任意 Supplier、Block 注册、一般 Registry API 或其他平台获得支持。

## 九、Codex 回写指令

收到本报告后，请按以下原则更新 PR #9：

1. 核对当前分支和 HEAD，阅读原 006A 报告、证据索引与《Beta 贴图更新》记录。
2. 新增独立文档：

   `Dev Manuals/Convergence-006A-C macOS 客户端人工验收报告.md`

3. 以本报告为输入，明确记录两份实际 SHA、完整客户端范围、用户人工确认、加载日志标记及 OpenAL ERROR。
4. 在当前 README、测试 README、PR 正文中更新**新资源变体的客户端状态**。
5. 不修改已冻结的原 006A-S 报告或历史输入，不用新版 fixture SHA 覆盖原服务端 SHA。
6. 原《Beta 贴图更新》中的 `client NOT RUN` 属于当时尚未执行的历史状态；通过新增后续验收报告说明状态已经变化，不追溯改写历史证据。
7. 如果本报告文本以外的原始控制台文件在 Codex 工作区不可访问，明确标记为“用户提供的控制台摘录/审查报告”，不要伪造原始附件的本地路径或 SHA。
8. 不得生成虚构截图、`latest.log`、`debug.log`、客户端测试 Driver 日志或新的服务端 C/T 结果。
9. 不修改 DualForge 生产 Java、Forge fixture class、其他资源或已经验证的 JAR。
10. 仅完成文档、索引及必要的状态回写；检查 Git diff、旧证据保护和工作区状态。
11. 更新现有 PR #9，不额外创建同目标 PR。
12. 不自动合并、不创建 Release、不进入 006B。

## 十、交付要求

最终向用户汇报：

- 新增或更新的确切文件路径。
- Git commit SHA 和 PR 最新 HEAD。
- 两份客户端实测 JAR SHA。
- 006A-S 原冻结 fixture 与 006A-C 更新 fixture 的区分。
- 新 006A-C PASS 的证据标签和边界。
- OpenAL 音频 ERROR 的记录方式。
- 仍未取得的原始附件或平台测试。
- 是否已经推送。
- PR 是否仍为 Draft。
- 是否存在需要用户决策的合并阻塞。

**本任务仅为客户端验收证据回写，不授权修改兼容实现、不自动合并 PR #9。**