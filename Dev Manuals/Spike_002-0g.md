# Spike-002-0g：限定单语句的 EVENT_BUS 成员引用变换

状态：v1.0 冻结版。日期：2026-10-12。root 顺序编制、审查及执行；依据用户「不要分子代理」「有问题自己修复，自动推进」和002-0f PASS裁定。旧报告及制品保留。

## 目标与唯一变量

只回答：将0e probe唯一API语句的三个二进制引用协同变换为已取证NeoForge契约，能否越过P1并实际进入NeoForge EventBus.register？

同一新compat jar、同一0e probe（D61224F3014F0ADFF85D10EEDEA414763C231367F08EDB99CE99DD2C8935F1C9 /1848B），两轮唯一实验变量为 `-Dforgecompat.0g.apiRewrite=false`（C）/`true`（T）。共同前提 `-Dforgecompat.2a.rewrite=true`。C先复现0e P1且dump为55D81709…4924FE8/1381B，才运行T。三个操作数是同一限定语句规则的耦合变换，不把它们伪称为三个独立受控变量。

## 冻结输入与工作布局

- compat原基线 `spike001a\compat-002-0d\build-treatment\forgecompat-002-0d.jar`：4CD8195F7AA03B8B595F646CDEAC16454D03D5E3862B5E2F388C820CF2FA2FEF /21180B。
- 源树 `spike001a\compat-002-0d\src`，先对照src-fixed归档逐文件核验，再完整复制与前后归档。仅新工作副本 `spike001a\compat-002-0g\src` 的 ForgeProbeConstructorTransformer.java允许修改。
- probe复用0e制品，原零API probe A93A…、mods.toml B32C…、0d/0e归档、0f冻结输入不修改。0e Run T逐轮dump是历史参照，共享dump路径仅为运行输出，首轮前另存现态，每轮后立即另存。
- 编译固定ms-21.0.10；classpath为compat\build\cp.txt原五件+现存asm/asm-tree 9.10.1，两ASM文件初检记录完整SHA。只使用现存依赖，不下载、不混入Forge库。完整命令/退出码保存。
- 构建新 `compat-002-0g\build\forgecompat-002-0g.jar`，4service+manifest保持，25条目；除transformer.class其它15个内容文件须与0d基线逐字节一致。jar新SHA构建后登记并在两轮固定。
- harness先归档现态84C7E2C06BF0A1D2AC0D0E2D5AD20482F1357668EC8807DAD642DA55944F77D0，只ValidateSet追加spike002-0g-control/treatment，机械diff对现态及原harness-baseline分别保存，其余字节不变。
- 日志：spike000\logs\spike002-0g-control、spike002-0g-treatment、spike002-0g-artifacts。运行目录存在即STOP，不覆盖；清理前确认server现态为已归档0e运行副本、无java进程、无reparse目标。

## 逐字实现定义

仅在原 transformer 中新增 ASM `Opcodes`、`AbstractInsnNode`、`FieldInsnNode`、`MethodInsnNode` imports，以及 `private final boolean apiRewriteEnabled;`。

原构造器在既有rewriteEnabled日志后追加（两个合法字符串必须显式提供）：

```java
String apiValue = System.getProperty("forgecompat.0g.apiRewrite");
if (!"true".equals(apiValue) && !"false".equals(apiValue)) {
    throw new IllegalStateException("[FORGECOMPAT-XFORM] required forgecompat.0g.apiRewrite must be true or false");
}
this.apiRewriteEnabled = Boolean.parseBoolean(apiValue);
log("apiRewriteEnabled=" + apiRewriteEnabled);
```

在原transform的既有post-check日志之后、dump(input)之前唯一插入 `rewriteEventBusCall(input);`。既有构造描述符重写、vote、targets、dump路径、错误记录与服务实现保持。类注释相应注明局部规则，不改变行为。

新增方法逐字如下：

```java
private void rewriteEventBusCall(ClassNode node) {
    if (!"com/example/forgeprobe/ForgeProbe".equals(node.name)) {
        throw new IllegalStateException("0g unexpected target " + node.name);
    }
    FieldInsnNode field = null;
    MethodInsnNode call = null;
    int matches = 0;
    for (MethodNode method : node.methods) {
        if (!"<init>".equals(method.name) || !NEW_DESC.equals(method.desc)) continue;
        for (AbstractInsnNode insn : method.instructions) {
            if (!(insn instanceof FieldInsnNode f)) continue;
            if (f.getOpcode() != Opcodes.GETSTATIC
                    || !"net/minecraftforge/common/MinecraftForge".equals(f.owner)
                    || !"EVENT_BUS".equals(f.name)
                    || !"Lnet/minecraftforge/eventbus/api/IEventBus;".equals(f.desc)) continue;
            AbstractInsnNode receiver = nextOpcode(insn);
            AbstractInsnNode invocation = receiver == null ? null : nextOpcode(receiver);
            if (receiver == null || receiver.getOpcode() != Opcodes.ALOAD
                    || !(receiver instanceof org.objectweb.asm.tree.VarInsnNode v) || v.var != 0
                    || !(invocation instanceof MethodInsnNode m)
                    || m.getOpcode() != Opcodes.INVOKEINTERFACE || !m.itf
                    || !"net/minecraftforge/eventbus/api/IEventBus".equals(m.owner)
                    || !"register".equals(m.name) || !"(Ljava/lang/Object;)V".equals(m.desc)) {
                throw new IllegalStateException("0g member sequence mismatch");
            }
            field = f;
            call = m;
            matches++;
        }
    }
    if (matches != 1) throw new IllegalStateException("0g expected one member sequence, found " + matches);
    log("API original-pattern hits=" + matches);
    if (apiRewriteEnabled) {
        field.owner = "net/neoforged/neoforge/common/NeoForge";
        field.desc = "Lnet/neoforged/bus/api/IEventBus;";
        call.owner = "net/neoforged/bus/api/IEventBus";
    }
    log("API_REWRITE enabled=" + apiRewriteEnabled + " hits=" + (apiRewriteEnabled ? 1 : 0));
    log("API post-check field=" + field.owner + "." + field.name + ":" + field.desc
            + " call=" + call.owner + "." + call.name + call.desc + " itf=" + call.itf);
}

private static AbstractInsnNode nextOpcode(AbstractInsnNode node) {
    AbstractInsnNode next = node.getNext();
    while (next != null && next.getOpcode() < 0) next = next.getNext();
    return next;
}
```

原transform异常捕获策略未改：ERROR日志出现即STOP，不把返回input解释为成功。规则关时不写任何API节点；规则开时只写上述三操作数，不改方法desc、opcode、itf、指令数量、行号/异常表/注解/打印。不得扩大到其它类或其它成员。

## 执行步骤与硬门

1. 2G-a：完整读0e/0f结论、本契约；输入/hash、源码对归档、版本、现态归档；冻结本说明书SHA。
2. 2G-b：复制隔离工作树，按逐字定义修改唯一源，前后全量归档；harness仅两Label；编译/装配/新jar登记。
3. 2G-c：原基线与新jar条目/内容逐项比较，15/16不变；transformer javap只允许上述属性检查/规则/辅助方法/常量差异。其余前置尤其委托不变。
4. 2G-d：C运行，记录offered、ctor REWRITE1、API original-pattern1、API_REWRITEfalse hits0、marker/BUS、delegate、P1完整栈与meta；dump须与0eT逐字节同值1381B。任何不符STOP。runtime mods恰probe+新compat，哈希固定。
5. 2G-e：T运行，API_REWRITEtrue hits1及post-check准确；dump语义对C只允许getstatic owner+desc、invokeinterface owner三处差异（忽略CP索引），constructor已共同Neo型、四次打印/行号/控制流不变。定位首个新终点，完整栈保留，不修复。
6. 2G-f：每轮dump即时归档，全文日志/meta/crash/命令/exit留存，冻结输入与新jar/source后核验，报告→顺序复核→裁定，不回改历史。

运行由既有run-server.ps1完成，C/T均固定0eprobe+新compat，JVM除唯一apiRewrite开关外相同（2a=true、Xmx3G、Xms1G）。首失败锁定分析边界，固定harness完成完整异常链/终止采集；后续只附件。不按stdout捕获顺序代替执行控制流；无精确BCI不伪称精确指令。

## 预登记与判据

`[H] S1`：Neo EventBus.register 因无SubscribeEvent方法抛IllegalArgumentException。若T出现该消息、Neo EventBus.register帧与ForgeProbe line14，并且C P1/三引用字节码/输入门均通过：**PASS-002-0g-1（P1越过、register入口已实际进入，新终点S1）**。这不等于监听器登记成功或总线语义兼容。

T到Done且无首失败可记VALID-002-0g-1（本路径生命周期通过），但无后置调用观测时不推register操作成功。T仍P1为FAIL（局部规则未解除），其它首失败为STOP完整交回；缺marker/规则/输入漂移/字节码越界/ERROR/dump缺失也是STOP。预登记S1原文语义匹配、栈与调用组共同限定位置。未来其它语义修复必须另立说明书，不添加SubscribeEvent探针。

## 交付格式

`Dev Manuals\Spike-002-0g 执行报告.md`：环境与所有初/终检SHA；前后源码/构建命令/exit；15/16内容比较；C全部门与完整首栈；T全部信号与完整首栈；dump逐指令差异；meta与完整文件索引；自评及[F]/[E]/[H]/[D]；偏差和顺序审查限制。复核文书另存，不自称独立代理审查。

范围：局部Spike规则不构成通用mapping或正式兼容层；不修改宿主、0eprobe、四前置、mod metadata；不下载库、不加第二API、不加监听器、不修复S1。
