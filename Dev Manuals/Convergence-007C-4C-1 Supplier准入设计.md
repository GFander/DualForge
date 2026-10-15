# 4C-1 Supplier API/类型/依赖准入设计裁定

起点 main `47693225ce71c7241a0bc8579c62b062df15f30c`；已 fetch 并核对其包含已合并 PR25 的 `fc0ad24165d3bc046c5f2a9594e1542ab1d3fa12`。此文件在生产修改前先提交。原 4B/4C-0 报告和原始输入保持原字节。

## 裁定

以不改变 Supplier 方法指令为前提，保留现有精确 LambdaMetafactory/零捕获/同 ModFile 实现方法绑定及 Item 注册数据流，新增一个小型只读类型/依赖验证器。它验证的是链接与已声明兼容边界，不是 Java 安全沙箱，不证明任意业务无副作用。不得用 1/58/8 分类、modId、包/类/JAR 名或 FnF SHA 选择生产规则。

1. **业务保持**：Supplier 内不需要变更的 Minecraft/JDK API 与同文件业务依赖原样执行。改写仍只由 4B Registry 边界负责，逐方法比较规范化 ASM 指令、常量、Handle/BSM、局部/Frame、异常表。允许整个 ClassFile 因重新序列化改变常量池，不以 class 总 SHA 相同替代语义对照。
2. **未知 Forge 链接**：遍历实际方法 descriptor、NEW/cast、字段/方法 owner 和 descriptor、被访问的同文件声明/父类/初始化体。除已独立处理的 root Registry 初始化外，业务依赖遇到任何未映射 Forge API 即拒绝；不进行包名全局替换。运行期反射、类加载、复杂 Handle/indy/condy 和不支持的控制流/异常处理拒绝。
3. **Bootstrap**：沿用精确 BSM owner/name/tag/descriptor、三参数、SAM/instantiated type、零捕获调用点、同 owner 的 private static Item 工厂。新增方法体准入不会放开捕获/外来 Handle 或其他旧字段和注册规则。
4. **类型**：用 ASM Analyzer + 不反射加载类的 SimpleVerifier 子类，依据实际 owned/host ClassNode 层次检查栈、locals、参数和返回。每个正常返回须是非 null、可赋给 Item 的具体引用类型；NEW 不能是接口/抽象类，构造器/父类/方法与字段必须确切存在且满足 static、opcode、可访问性。未知层次不退化成 Object。
5. **前向依赖**：对同 ModFile 的被调用业务方法、构造器、静态字段类型及必要 clinit 递归读取并验证，处理自定义 Item 子类/override 与独立静态 FoodProperties 工厂；记录循环/缺失/不支持结构，保证有限终止。原始代码只读，不运行 Supplier/构造器/clinit。外部类只查固定宿主 ABI，不递归执行或改写其方法体。
6. **ABI 来源**：生产首先从实际 FML LoadingModList 中的 minecraft/neoforge ModFile 读取合并资源字节，并从 JDK 模块只读资源获取 java 类；GAME 尚未建立时也不得 Class.forName 来源类。缺少、冲突或不可解析的宿主声明拒绝，不猜测版本。只读 Contract 可以显式提供按固定官方输入选择/核对过的 ClassFile oracle；这是离线测试依赖，不是生产开关或替代运行证明。
7. **依赖不等于 Target**：计划区分需要 Registry 改写的 entry/helper/Holder consumer 与仅前向验证的业务数据/子类。后者不加入转换 Targets；原字节随已有 ModFile 全类快照及计划 SHA 绑定。必要 host ABI 指纹纳入新计划身份，运行桥接重建应得到相同身份。业务依赖若反向引用不允许的 Registry 边界则拒绝，不能借此扩大 Consumer。
8. **整组发布**：所有 Supplier、业务依赖、原 Registry/Consumer、输入/归属和克隆后的 ASM 检查都成功才发布 Targets；任一失败不发布部分计划。沿用初始 class SHA、输入节点对照、目标冲突、caller/owner Mod Bus 和 ITEM Registry 身份校验。不声称回滚已定义 JVM 类。

## 明确支持的初始控制范围

先支持可验证的直线方法、精确字段/方法链接、原生 Item 和 owned Item 子类、独立数据/工厂与副作用原样保留。未知分支、异常表、monitor、jsr/ret、反射/动态类加载、复杂 Handle 和业务体额外 invokedynamic 不默认允许。旧 Profile 不动，4B 的其他严格负例保留；合法的新业务形态与旧错误预期的迁移必须版本化记录，不删除整套测试。

## 验证顺序

先构建不同新身份的 A 普通、B 独立 Food 数据、C 自定义 Item/额外 ctor 参数/真实 override 三个原生 Forge case；先官方 Forge，再同原 JAR NeoForge C/T。查询真实 Registry、精确 Supplier 次数、Holder/对象/owner Bus、官方 RegisterEvent、native 共存与未认领类不改。新负例逐项记录准确原因、Target 发布和原节点保全。

最终同一新 compat JAR 复跑原 4B C/T、相关 007B/Context/Registry/Block/Event 静态契约、ASM verifier、Python 与374冻结保护。FnF 只读逐 Supplier 验证及强制计划新拒绝位置分别报告；正常 isCandidate 对全局 Bus 的排除不变，FNF_RUNTIME=NOT RUN。Tab/全局 Bus/客户端/4C-2 不在本次实现范围。
