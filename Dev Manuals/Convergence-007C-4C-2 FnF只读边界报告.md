# 4C-2 FnF只读边界

原始官方JAR保持128098 bytes，SHA-256 `bd415b04529c4ffc31e14757488f71dab6917e4560c61e236c3c4d0de6557958`。本轮使用真实发行ClassFile、javap/ASM，不用GitHub源码替代，不重包/定义/初始化FnF类。

真实Tab路径为ResourceKey CREATIVE_MODE_TAB → create(ResourceKey,String) → RegistryObject<CreativeModeTab>；Tab factory生成两个标准零捕获lambda：icon延迟读取LOGO Item Holder构造ItemStack，display延迟消费66个Item Holder并调用Output.accept(ItemLike)。类型、private static实现/Bootstrap/descriptor及所有引用逐项进入新计划准入，样本名字、SHA、1/58/8或66均未作为生产匹配。

新最终JAR SHA-256 `1afc82338206901c7057951aa2202c8dadd75cdd95d47fd112bb166d8faa6038` 的实际强制只读完整计划越过历史 `ITEM_FIELD_DESCRIPTOR`。新的首次拒绝：

```text
FNF_CONTRACT_VERSION=typed-v1
FNF_PLAN_ADMISSION=REJECTED reason=Registry helper rejected: GLOBAL_EVENT_BUS_UNSUPPORTED
FNF READ_ONLY ADMISSION PASS; FNF_RUNTIME=NOT RUN; no helper output published
```

位置为 `ForgeRegistryTransformationPlan` 构造器，在整组源引用准入后、输出克隆与发布前，检查实际Context入口统计的pendingGlobalCalls。该入口仍含原始 `MinecraftForge.EVENT_BUS.register(this)`；本轮明确拒绝，不删除调用、不返回no-op、不将其当Mod Bus。正常isCandidate依旧false，仍遵守旧Context分类边界。

这证明新的源类型/Tab/Holder闭包准入不再停于旧字段原因；它不证明FnF输出类、JVM链接或真实运行成功。整组计划仍拒绝，未发布FnF输出，更未执行67个Item注册、Food/Fuel、Tab客户端或持久化。FNF_RUNTIME=NOT RUN。

历史legacy-4b与supplier-v1收据和原始证据不改；typed-v1新增精确判定，其他原因/异常接受/附加文本/错误版本或冲突版本均失败。Java判定合约现为3正例/12负例；Python同时检查唯一精确版本、原因及NOT RUN。新拒绝不是把任意IllegalStateException判为PASS。

重跑使用tests/helper_registry/run_contract.py，显式传最终JAR、原4B fixture、固定原FnF、host oracle和 `--fnf-contract-version typed-v1`。诊断FnFBoundaryProbe只读首次观察与最终精确Contract分开保存。旧supplier-v1只用于其历史cohort预期，不能套用当前JAR。
