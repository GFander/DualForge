# Convergence-007C-4B Registry Helper与转换计划契约

日期：2026-10-15 UTC。生产实现提交 `24a66d3437898860183d84a3e63c069604c2f0a8`；最终测试源码提交 `2ff283086cee743f6ecd40cce87146237aad854c`。固定宿主 Minecraft 1.21.1 / NeoForge 21.1.256 / FML 4.0.45 / ModLauncher 11.0.5 / Java 21。

## 来源与选择

入口先由既有 `ForgeEntryProfile` 独立分类。旧 Item、Block、Event、Context 验证器不改接受范围。仅 Context 入口中真实存在静态 `(Lnet/minecraftforge/eventbus/api/IEventBus;)V` helper 调用，且入口没有未验证的 MinecraftForge 全局 Bus 字段引用时，尝试新计划。该候选判断不是准入许可。

`ForgeRegistryTransformationPlan.create` 重新使用官方已确认 Forge ModFile、单一 metadata owner、完成的 ScanResult、同 JAR 的真实 `@Mod`、官方 moduleName 和原 ClassFile。拒绝显式 module-info、外来 helper、缺失 class、归属冲突、native/platform 类和不支持结构。不依赖 fixture/modId/包/入口/helper 名称、发行 SHA 或现场绝对路径选择支持对象。

## 有界引用闭包

从入口的实际 helper 调用确定注册方法，独立验证每个 helper 的一个 typed Item DeferredRegister、至少一个 typed RegistryObject、Registry 来源、modId namespace、每条注册 ID / receiver / Supplier 数据流及赋值。支持多个 helper；注册 ID 在本 mod 的所有 Item helper 间检查重复。

按真实字段/方法及 Handle/BSM 引用反向扩展 Consumer，逐方法验证纯 Holder/Item accessor：同组字段、同组静态 getter、RegistryObject.get、Item CHECKCAST、精确返回 descriptor / 泛型 Signature。外来 API、字段逃逸、缺失 Consumer、递归环、未知 Handle/Bootstrap 整组拒绝。无实际引用边的普通类不因为同 JAR 或同 ABI 被改写。

当前 helper/clinit/attach/accessor 形态刻意限定，不支持任意 Java 控制流或业务逻辑。仅支持已取证的 ITEMS 工厂、普通 Item Supplier 和纯 getter；ResourceKey 工厂、捕获 Lambda、额外 Registry/Forge API 均未放行。attach 可以是直接官方挂接，或固定语义的 `!"false".equals(System.getenv(UPPERCASE_KEY))` 跳过条件；生产不匹配 DF4B 的特定名称。

## 不可变计划与发布

计划保存 modId/module/entry、真实 ModFile 来源、原 class 字节与 SHA、精确目标集合、注册字段/方法、规则版本和计划指纹。ModFile 路径只用于绑定该次计划身份，不能赋予准入权限。运行时重建计划并核对指纹，避免依赖 SERVICE/GAME 共享静态缓存。

整组先验证，在各 ClassNode 副本试转换并运行实际 ASM BasicVerifier/Analyzer，全部通过后发布 immutable Map 和 Targets。每个目标变换再次比对 ModLauncher 原始 SHA 及规范化输入节点；其他 Transformer 改变输入时拒绝。dump 失败也不污染原节点。

ModLauncher 的类名索引本身不带 ModFile，因此发布前额外检查其他已扫描 ModFile 的同名目标和旧 Profile 重叠，拒绝 `TRANSFORMATION_RULE_CONFLICT`。并发变换只读取已发布计划。

仅保证计划发布和单类克隆原子性，不提供 JVM 已定义类的跨类回滚。时序证据、第三方提前加载风险见本轮 FML 时序报告。不是对恶意 JVM 模组的沙箱。

## 直接验证入口

`tests/helper_registry/HelperRegistryContract.java` 使用真实 Reader、ModFile、ScanResult、LoadingModList；最终 110 项断言、35 个负例。包括两个 helper、全新身份、Consumer 返回/泛型闭包、无关类、native、外来同名类、多个 metadata owner、显式 JPMS、缺失引用、重复目标、规则冲突、输入 SHA 变化、Frame/stack/晚期 dump 失败和原字节保全。完整命令与逐项日志由独立证据索引定位。

独立 CI 只编译真实生产 `RegistryBytecodeVerifier` 和合成 ASM Contract，10 项断言；它不冒充原生 Forge 编译或游戏运行证据。

## 最终数据流补正

追加ASM负例证明，早期字符串类型标记可与人为构造的LDC常量碰撞（literal-registry-token被错误接受）。现改为独立enum Value，来源String字面量不能伪造内部Registry/Register/Supplier/Holder身份；计划版本提升v2，新增Registry/Supplier碰撞拒绝。修正前日志原样保留，所有最终动态和历史回归在新的干净验证worktree使用新同SHA JAR重跑；旧4d19414f…结果仅为前版候选证据，不进入最终PASS。
