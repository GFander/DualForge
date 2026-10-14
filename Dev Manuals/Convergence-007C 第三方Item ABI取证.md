# Convergence-007C：真实发行字节码与首失败

取证对象为冻结原始 Food N' Fuel SHA `bd415b04529c4ffc31e14757488f71dab6917e4560c61e236c3c4d0de6557958`。
五个 class 均由原 JAR 的 `javap -v -p -c` 和 ASM ClassNode 读取；没有加载/改写发行 class 来制造输入。

## 007B normal profile 对照

| 结构 | 007B 已验证契约 | 原发行 JAR 实际输出 |
|---|---|---|
| 入口与构造 | public final，单 `(Lnet/minecraftforge/eventbus/api/IEventBus;)V` | public 非 final `FoodNFuel`，`<init>()V` |
| Mod Bus 来源 | 参数 ALOAD1 | `FMLJavaModLoadingContext.get()L…FMLJavaModLoadingContext;` → `getModEventBus()L…IEventBus;`，存 local1 |
| 挂接 | 同入口 owned GETSTATIC / register | local1 分别传 `ItemRegistries.register(IEventBus)V` 与 `FnFCreativeModeTabs.register(IEventBus)V`；两类内各挂接一次 |
| Item 字段 | 入口内 DR/RegistryObject typed 字段与 get-only | 独立未标 @Mod 的 ItemRegistries；67 个 `RegistryObject<Item>` 字段 |
| create | `create(IForgeRegistry,String)DeferredRegister` | Item 使用同描述符；Tab 使用 `create(ResourceKey,String)DeferredRegister` |
| register | `(String,Supplier)RegistryObject` | 两类实际调用该描述符；Item 67 次，Tab 1 次 |
| BSM | 零捕获 LambdaMetafactory / 同入口静态 Item 工厂 | 实际零捕获 lambda 指向 helper 类私有静态工厂；另有食物效果和 Tab 回调的独立 BSM；完整 Handle/参数保留 |
| Supplier | 原同类纯 `new Item(new Properties())` | 普通 Logo、58 个 food Properties、8 个 `new FuelItem(Properties,int)`；不满足纯工厂模板 |
| Holder.get | 同入口 get-only | Tab icon/displayItems 的 helper lambda 中调用 `RegistryObject.get()Object` 并 cast ItemLike/Item |
| Tab | 未支持 | `CreativeModeTab` builder、title/icon/displayItems、真实 `fnf:fnf_item_tab` |
| 全局总线 | 原严格受控 listener/empty route | `MinecraftForge.EVENT_BUS:IEventBus` → `IEventBus.register(Object)V`；入口没有实际 SubscribeEvent 方法 |
| 自定义 Item | 未支持 | FuelItem extends Item；ctor `(Item$Properties,int)V`；`getBurnTime(ItemStack,RecipeType)I` 返回保存的 int |
| Minecraft 命名 | 受控工厂使用 Mojang 成员 | 发行字节码含 SRG `m_…` / `f_…` 成员，如 Properties 的 food/stack 调用及食物/Tab builder |

上表是 RELEASE_BYTECODE 与静态契约，不是已经观察到的 NeoForge 后续失败。
纯 Forge debug 实际枚举 `srgtomcp` / MCPNamingService 并加载 method/field mappings。映射事实独立于 Registry 查询；不能假定 NeoForge 具有相同映射链，也不能删除 food/效果调用来取得启动成功。

## 真正首次失败：发现准入

N0（空 NeoForge）、N1（仅本轮 DualForge）、N2（同 DualForge + 同原发行 JAR）均以标准 run.sh 和原默认参数串行运行。
N2 实际安装 SHA：compat `36d84c40e1a10eea7921ed31c967af197b7b43cabf65937d75f27132ec67ac44`，Food N' Fuel `bd415b04…57958`。

```text
[DUALFORGE] CALLED ForgeModReader.read for …/mods/fnf-forge-1.3-1.21.x.jar
Skipping jar. File …/fnf-forge-1.3-1.21.x.jar is for Minecraft Forge or an older version of NeoForge, and cannot be loaded
… Done …
Unknown item 'fnf:logo'
```

当前 `ForgeModLocator.isForgeCandidate` 因三引号字符串直接 false；共享 Reader 因同 guard 返回 null。LanguageLoader 仅给 neoforge 内置委托，没有 fnf 的 metadata/entry/targets/构造/转换链。
**最后成功是容器被遍历并交给 Reader；首失败是候选准入，当前等级 L0。** 不把后续宿主 Done / exit0 当作第三方兼容。
原 `[0,)` 同时包含 Forge52.1.0 与宿主 FML4.0.45；行尾注释也超出现有正则/扫描器子集。它们是额外静态边界，不是删除三引号后实测得到的下一异常。

## 只读取证 Contract 与可行性边界

`ReleaseBoundaryTest` 实际使用宿主已有 NightConfig TOML3.8.3/core3.8.3 成功解析原 TOML，包括 description、modId、两个 required 依赖及原 range。
同时实际断言：候选 false、Reader null、没有单 bus ctor 候选；原 normal profile 对原 ClassNode 明确拒绝 class structure，验证前后 ClassNode 及 JAR 字节相同。
这是 **边界 Contract PASS / 兼容未实现**。Class/profile 拒绝是直接静态调用，未冒充 N2 的构造器运行失败。
诊断 JVM 的 classpath/add-opens/add-exports 只用于离线 Contract；N0/N1/N2 真实 JVM 命令没有这些附加项。
早期 ASM 工具 classpath 选到旧 ASM、边界工具缺 Log4j 或需要的 opens 的失败均保留，修正后的独立输出才记 Contract PASS。

完整链需要分别证明完整 TOML/依赖语义、无参上下文生命周期、跨类字段/调用归属、SRG 成员映射、Tab builder、Fuel 等机制。
原 `forge [0,)` required 约束必须保持真实语义，不能把 `forge` 字符串替换成 `neoforge` 或虚构完整 Forge 运行时。
目前原生功能基线尚为 PARTIAL，生产修改强制门槛未满足；不在本 PR 同时引入多个未验证子系统。
后续最小方向为先补齐原生玩家功能，再独立设计 metadata/宿主能力契约；第二阶段才是扫描与数据依赖限定的上下文/跨类注册可行性。两项均未自动实施。

生产源码、targets、Parser/Reader、依赖、ABI、模块层和 API 支持集合本轮 **全部未改**；没有第二个身份白名单、Supplier 业务改写或游戏类全局转换。
