# 4C-3 FnF 完整只读计划

固定原始官方JAR SHA `bd415b04529c4ffc31e14757488f71dab6917e4560c61e236c3c4d0de6557958`，128098 bytes；未重包、定义、初始化或执行发行类。

最终生产提交 `9fc9cde97888a2d7894252dc59702d8548d5a5e5`，新兼容层 SHA `a26893598d1ffdb73078373b007a4d5d54dddd1b774cb2a0fa0dd90fe5602bf7`。真实Reader/FML metadata/ScanResult下，正常入口分类仍为Context，组合计划候选现在为true；不是按fnf、FoodNFuel或输入SHA生产匹配。测试中的固定SHA只确认实际发行样本身份。

强制完整只读create成功，返回以下单一计划的三份已验证输出：

- net/natevg/fnf/FoodNFuel：两Context调用、两个已验证Mod Bus helper及唯一Global register(this)共同进入一个克隆构造器。
- net/natevg/fnf/item/ItemRegistries：原67个Supplier业务不改，注册/API/Holder边界映射闭合。
- net/natevg/fnf/item/FnFCreativeModeTabs：typed Tab、icon/display Consumer闭包保持。

FnFFoodProperties/FuelItem是已验证业务依赖，不新变换为Target。每个目标保存原ClassFile SHA、原始字节SHA核对、克隆before/after、输出SHA和ASM结果。未留下未映射的Forge Registry调用或MinecraftForge全局字段。整个输出计划才返回，没有部分结果。

实际精确收据：

```text
FNF_CONTRACT_VERSION=global-v1
FNF_PLAN_ADMISSION=ACCEPTED targets=3 candidate=COMPOSED_CONTEXT globalListeners=0
FNF READ_ONLY COMPLETE PLAN PASS; FNF_RUNTIME=NOT RUN; source classes never defined or initialized
```

本轮无下一道静态拒绝。该结论表示只读计划三目标全部生成并通过校验、正常粗选规则相符；没有启动FnF的ModLauncher/Game运行或将FnF类定义到JVM，不能称为FnF实际加载、构造或注册成功。独立受控模组的动态发布验证了共享生产发布路径，不替代FnF首次游戏验收。

历史legacy-4b/supplier-v1/typed-v1的拒绝收据与原始附件保持；这些旧版本只适用于原cohort。新run_fnf.py仅接受唯一global-v1、唯一三目标完整准入、三条不同完整SHA证明及唯一NOT RUN收据。错误版本、拒绝/重复/缺少结果、错误SHA格式、缺目标、任何例外失败均不能冒充成功。旧Context测试直接复核未变校验器，不再宣称FnF生产只转入口；原typed blanket-Global拒绝按本轮授权换成不支持post的负例，其余断言不放宽。

重跑：tests/global_bus/run_fnf.py，参数见同目录README。完整命令、退出、原始stdout/stderr、目标字节和输入/输出指纹位于out/convergence-007c4c3/fnf-accepted/。必要FnF前后ClassFile仅作为只读边界证据归档，不是可运行发行包。客户端、食物/燃料实际行为、创造栏UI、持久化及完整FnF游戏全部NOT RUN；4A历史PARTIAL不改。
