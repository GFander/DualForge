# Spike-002-0j：固定裸探针的空注册语义保真

v1.0冻结，2026-10-12，root串行。依据0i-1 PASS、0h条件字节码与0g S1；用户自动推进授权。仅验证固定probe的零方法形态，不做通用事件系统、监听器登记或分发。

共同输入：0e probe D61224F3014F0ADFF85D10EEDEA414763C231367F08EDB99CE99DD2C8935F1C9/1848B；0g src对archive/src-after复核后复制为compat-002-0j工作副本；0g原jar F0D51E46…B4C739、逐轮T dump A9ABBB94…05EF1保持历史参照。新jar两轮相同。共同JVM：2a.rewrite=true、0g.apiRewrite=true；唯一变量 `forgecompat.0j.emptyRegistration=false`（C）/true（T）。两者必须显式true/false。

## 精确定义

在0g transformer只新增private final boolean emptyRegistrationEnabled；构造器末尾按0g同样true/false校验读取0j属性并log emptyRegistrationEnabled；在既有apiRewriteEnabled三引用变换块之后追加：

```java
if (emptyRegistrationEnabled) {
    if (!apiRewriteEnabled) throw new IllegalStateException("0j requires apiRewrite=true");
    call.setOpcode(Opcodes.INVOKESTATIC);
    call.owner = "com/example/forgecompat/ForgeEmptyRegistration";
    call.desc = "(Lnet/neoforged/bus/api/IEventBus;Ljava/lang/Object;)V";
    call.itf = false;
}
log("EMPTY_ROUTE enabled=" + emptyRegistrationEnabled + " hits=" + (emptyRegistrationEnabled ? 1 : 0));
```

新增类ForgeEmptyRegistration.java逐字：

```java
package com.example.forgecompat;

import java.lang.reflect.Method;
import net.neoforged.bus.api.IEventBus;

public final class ForgeEmptyRegistration {
    private ForgeEmptyRegistration() { }
    public static void register(IEventBus bus, Object target) {
        if (target != null) {
            Class<?> type = target.getClass();
            if ("com.example.forgeprobe.ForgeProbe".equals(type.getName())
                    && type.getSuperclass() == Object.class
                    && type.getDeclaredMethods().length == 0) {
                int customPublic = 0;
                for (Method method : type.getMethods()) {
                    if (method.getDeclaringClass() != Object.class) customPublic++;
                }
                if (customPublic == 0) {
                    System.out.println("[FORGECOMPAT-EMPTY] legacy-empty-noop target=" + type.getName()
                            + " declaredMethods=0 customPublic=0");
                    return;
                }
            }
        }
        System.out.println("[FORGECOMPAT-EMPTY] delegate to NeoForge register");
        bus.register(target);
    }
}
```

只改该transformer、新增helper，前后全量归档；不catch任何注册异常，不加监听器/注解，不改probe或宿主。不满足profile时走原Neo调用。固定probe由哈希确保，未来该局部规则不能默认为任意mod的语义兼容。

## 构建及K1门

与0g相同编译流程，classpath仅追加现存Neo bus8.0.5（705E24F3…62593A）用于helper签名，非运行新增依赖。原有7依赖前后记录，实际javac/jar命令及exit原字节留存。新jar应26条目/17内容文件：0g其余15内容文件逐字节不变，仅transformer改变、helper新增。两个源变化符合定义；sources-after与归档固定。

harness现态FBA0003C…9C8C先归档，只追加spike002-0j-control/treatment两个label，单步与累计机械diff，不改其它字节。新jar/harness哈希构建后登记，两轮不变。文件名forgeprobe.jar保持。日志/证据目录对应spike002-0j-*，运行label存在即STOP；清理前旧0g现场完整归档、无Java/reparse风险。

C：成员重写true、空路由false。必须复现S1、Neo EventBus.register帧/ForgeProbe line14；dump与0g T逐字节同值1359B。offered/ctor REWRITE1/API1/EMPTY_ROUTEfalse0/marker/BUS/委托都命中，才T。

T：EMPTY_ROUTEtrue1、post-check为INVOKESTATIC helper(Object/NeoBus)契约，运行dump对C仅offset47调用的opcode/owner/descriptor/itf变化；指令数量不变、旧返回offset52变50为invokeinterface5bytes→invokestatic3bytes的布局变化，四次println/ctor参数/字段引用不变，行号位置逻辑保持。helper运行日志必须精确declared0/custom0/固定target，且无delegate日志；这验证目标profile实际成立和空路径执行，不伪称Neo register调用或监听器登记成功。

PASS-002-0j-1：C保真S1且T准确helper空路径、S1缺席，完整元数据/新终点记录。若T到Done且正常stop：VALID-002-0j-1（局部零监听路径生命周期通过）。其它首异常完整记录STOP；T仍S1为FAIL；缺profile日志、出现helper delegate、输入漂移、class访问失败、字节码越界也是STOP，不吞异常或修宿主。首失败界限与完整原日志采集沿用0g。

归档C/T逐轮dump（shared路径运行输出立即另存）、全量日志/meta/crash、完整命令/exit、源码前后/hash。执行报告与顺序裁定另存；同一智能体审查限制披露。结论绝不超过“固定零订阅方法对象的这句API路径正常返回/生命周期通过”。
