# Convergence-007C-3 构造器桥接方案比较

在生产修改前依据实际Forge52.1.0/FML4.0.45取证选择。新的Context Profile不能成为旧Item验证失败后的兜底；旧Profile candidate与validator不放宽，无法归属或冲突明确拒绝。

| 方案 | 可证明部分 | 边界和决定 |
|---|---|---|
| A 注入NeoForge Bus并改写无参构造器 | 对纯Context fixture，能够将唯一get/getModEventBus链替换为参数，需重排原local1和栈/帧 | FnF仍把旧Forge Bus传给两个旧helper；直接给NeoForge Bus不满足来源接口/描述符契约。修复该边界会继续触及helper的DeferredRegister调用/初始化，不在本轮做。不是只改ctor签名即可。未作为生产方案 |
| B 隔离Context API shim | 宿主本来支持无参；在已验证唯一ctor调用点只映射两个Context方法owner与返回context类型。保留ForgeBus、local、helper描述符；视图按当前真实container绑定native delegate | 选择受控实验。新类在DualForge命名空间，不给宿主或Forge包注入同名Context。没有全局Bus、额外Layer或自建生命周期。必须验证caller构造方法、所属metadata/module和active container一致；getter再次验证归属。视图不是一个新建Bus，也不等于返回对象原样就是NeoForge Bus，不实现register/post/listener等新API |
| C 严格拒绝 | 不产生未经证明的ABI桥接 | 若B不能维持归属/正确Bus/严格回归，保留准确Context拒绝，交付PARTIAL；不能吞掉未知类或把不转换当成功 |

B仅证明上下文→所属真实ModBus的关联。运行时从source ABI视图读取其private delegate/owner的检查属于测试观察，不是对一般Forge Event API的兼容声明。FnF的全局总线调用及两个Registry helper业务保持原字节；下一实际缺类/方法/注册失败必须保留，不在本轮补齐。
