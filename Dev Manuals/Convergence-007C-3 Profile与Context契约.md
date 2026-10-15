# Convergence-007C-3 Profile与Context契约

先行契约，不预写未运行结果。

1. 只处理共享Reader确认的Forge来源、真实FML @Mod owner。新增明确BLOCK/NORMAL_ITEM/LEGACY_ITEM/EVENT/CONTEXT分类；每类独立candidate/validation/plan/audit。没有匹配UNSUPPORTED，冲突AMBIGUOUS；不catch旧validator失败后试新Profile。旧Item/Block/事件验证和指令转换保持原边界。
2. Context仅public Object子类、非final或final均可；唯一public无参构造，唯一同JAR匹配Forge @Mod，无额外入口/metadata owner。只允许Object构造、唯一直接get/getModEventBus链、一个受控局部Bus。reject额外ctor、未知Context/get形态、第二来源、分支/异常处理、捕获/indy/MethodHandle及未知副作用。
3. 构造体只接受固定字符串marker打印、把已取Bus存为本类Object观察字段、已证明同JAR唯一owner的静态单Bus helper调用、原已取证全局EVENT_BUS.register(this)形态。静态字段只支持modId常量/已验证LogUtils logger初始化；不得新增白名单modId/包/类/JAR/SHA。
4. helper只读核对同JAR class/实际调用目标、public static单ForgeBus→void、唯一owned DeferredRegister字段到原register调用的直接委托。它不是Registry验证/转换，不宣称其clinit/Supplier/Tab已支持，不把helper加入transform targets。任何其他bus escape或未知副作用拒绝。
5. Context转换只改两个已验证MethodInsn的owner及get返回Context类型，保留()V、Forge IEventBus、栈/局部/Frame与所有业务调用。整ClassNode先克隆验证再改，dump失败也不污染调用者。明确两个helper与全局调用尚未适配；不为FnF清除或跳过任何行为。
6. shim每次从宿主ThreadLocal实际container取得Bus，验证当前调用者在其Forge @Mod入口构造方法、module与metadata一致、loader属于DualForge；拒绝minecraft fallback/native/foreign/outside constructor。getter检查active/container/caller一致。无额外ThreadLocal或全局Bus缓存；不声称支持异步/跨模组重入。
7. 视图的delegate必须等于实际FMLModContainer.getEventBus，owner等于当前container，不能是全局Bus；两个模组的delegate必须不同。来源IEventBus旧空壳保持原字节，不新增Event API方法；该视图只验证上下文关联，任何一般Bus操作不在支持范围。
8. Fixture必须原Forge52.1.0干净编译，先在原Forge验收，NeoForge使用同一原JAR；观察器独立、仅读，不进入FnF最终双JAR运行。零业务JVM参数，所有JVM串行；健康组与当轮空宿主基线比较。
9. 负例覆盖归属、歧义、未知副作用、Context来源、原生路径、Bootstrap/Handle、显式JPMS和部分失败。旧007B13负例与JPMS原样复跑，新增检查旧nonfinal不能被新Context绕过；最终同一JAR执行002–007C-3。
