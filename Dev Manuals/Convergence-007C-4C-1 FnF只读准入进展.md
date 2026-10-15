# 4C-1 FnF 只读准入进展

输入仍为官方原始 `fnf-forge-1.3-1.21.x.jar`，SHA-256 `bd415b04529c4ffc31e14757488f71dab6917e4560c61e236c3c4d0de6557958`。本轮没有启动 FnF；FNF_RUNTIME=NOT RUN。

最终兼容层 SHA-256 `8fd97a0a4e811aed4e8e084f172341c1b808837e202a22986705e9f0c33360c2`。新的只读 Supplier Contract 对真实发行 JAR 的67个零捕获 Item Supplier逐个验证，全部通过；其 FoodProperties、自定义 Item 子类、构造器、字段、override及必要静态依赖通过实际固定 NeoForge ABI oracle 的类型/链接检查。原输入和所有 ClassNode 不变，不产生游戏运行证据或注册结果。

原 4B 历史精确拒绝 `Registry helper rejected: SUPPLIER_BUSINESS_UNSUPPORTED` 的原始证据不修改。本轮 `supplier-v1` Contract 在强制只读完整计划中实际观察并精确断言：

```text
FNF_CONTRACT_VERSION=supplier-v1
FNF_PLAN_ADMISSION=REJECTED reason=Registry helper rejected: ITEM_FIELD_DESCRIPTOR
FNF READ_ONLY ADMISSION PASS; FNF_RUNTIME=NOT RUN; no helper output published
```

下一处为 `FnFCreativeModeTabs.FNF_ITEM_TAB` 的 `RegistryObject<CreativeModeTab>` 字段，超出既有 Item helper 字段类型契约。记录的是强制计划诊断：正常 `isCandidate` 仍因入口包含未验证全局 Event Bus 而排除 Helper Profile，仍选择旧 Context；未扩大正常生产候选，也没有发布 FnF helper 输出。

`FnFAdmissionExpectation` 同时保留 `legacy-4b` 精确旧原因并新增 `supplier-v1` 精确新原因。Java 2 正例/9 负例检查意外接受、错误异常、附加文本及跨版本原因不能通过；Python 收据检查还验证唯一版本、唯一原因、NOT RUN，不能用任意 IllegalStateException 冒充预期拒绝。

67个独立 Supplier 静态通过，与完整计划被拒绝是两个层次。Tab、全局 Event Bus、真实 Food/Fuel 行为、客户端和 FnF 完整兼容均未实施或未运行。本轮未进入4C-2，不能仅依据这次首拒绝推定下一阶段只需处理 Tab。

重跑见 `tests/supplier_boundary/README.md` 和 `tests/helper_registry/README.md`。最终只读原始收据：`out/convergence-007c4b/contract/20261015-163637-3c11f10f/`，对应 SHA、完整命令和归档映射见本轮证据索引。
