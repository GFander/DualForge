# Spike-002-0h 静态取证报告

2026-10-12。root执行及顺序复核，无子代理。依据冻结说明书SHA `F745576DAF91C255482C44BF49835E94AAA0313B1A9DF59664E44B1E5E385BF1`。自评并顺序确认 **PASS-002-0h-1**，仅静态条件事实。

输入7项前后哈希匹配；复用0f的两侧EventBus code、probe有效verbose、两工具版本及0g T完整latest，共6项逐一核对原索引SHA。未重新运行工具，未构建/启动JVM应用；复用来源不冒称本轮新实验。

## 固定结构与直接路径

probe原class：super_class java/lang/Object、interfaces0、fields0、methods1；唯一方法是构造函数，class注解Forge @Mod。它没有声明普通监听方法。源证据为probe-verbose-raw，非初次有损文件。

Forge EventBus.register(Object) 实例分支offset34调用registerObject，offset37返回。registerObject完整原文已从原始code无改写选出，存 `spike002-0h-artifacts\forge-registerObject-full-excerpt.txt`，SHA `4E1D6FE1C646DA8C093E648EC40C4BB510484181DAE1B26377FCC21873932C6B`。

它offset4调用Class.getMethods；新HashMap收集候选；static方法跳过；直接SubscribeEvent命中则调registerListener；未命中且declaringClass为Object时跳过；其它未命中方法进入候选map。

零候选分支原文：

```text
     117: aload_3
     118: invokevirtual #140                // Method java/util/HashMap.isEmpty:()Z
     121: ifeq          125
     124: return
```

这是[F]条件事实：该map.isEmpty调用返回true时，正常返回；未出现Neo那种显式「没有SubscribeEvent方法」计数异常。此分支前的反射调用或注解解析仍可能失败，不能由一个return断言整个原Forge调用永不失败。

Neo完整register选录存 `neo-register-full-excerpt.txt`；offset258/260检测本类声明方法匹配计数，零则offset263–284抛IllegalArgumentException，285为非零分支return。0g已实际到达该异常及Neo EventBus.register帧，这是[E]，并非0h新运行。

## 可裁定与未知

已证明：[F]两侧零候选相关的直接终止分支不同；原Forge空map分支有正常return，Neo零计数分支有显式异常。[E]当前局部映射probe实际命中Neo异常。

尚未证明：[H]固定ForgeProbe在**原Forge实际环境**执行时，反射返回的完整公共方法集合及注解查询是否使map必为空、是否遇到先行失败；Class.getMethods、Method.isAnnotationPresent、Object声明方法属于JDK/运行边界，本轮没有跨越它们编造项目[F]。原Forge完整MinecraftForge初始化或probe加载也没测试。

因此不能立即把吞异常或空操作当成已证明的语义修复。也不能给probe添加原生Neo监听器后宣称原Forge零监听路径已兼容。结论限定结构、条件分支和现有实测终点。

## 判据、归档与顺序裁定

PASS：所有指定结构/直接路径/零候选终点完整且来源准确，未知项明确，不变性通过。7输入与6复用文件均匹配；新增证据清单在 `spike000\logs\spike002-0h-artifacts\evidence-index.json`。无生产文件修改、无新服务端运行、无实现。

同一智能体顺序复核限制保留；报告未冒称独立代理审查。后续最小步骤应补**原Forge受控空对象注册的运行事实**，先冻结其真实所需依赖与入口；当前没有授予吞异常适配或添加监听器的实现。

结论上限：可以定位“链接问题之后暴露注册语义边界”；不能称注册成功、Forge全局总线已通或通用兼容成立。
