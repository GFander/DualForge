# Spike-002-0n：隔离总线上的固定订阅方法实际送达

v1.0 冻结，2026-10-12。root 单智能体串行执行、复核、裁定。依据0l VALID和0m PASS。仅验证隔离夹具，**不证明FML全局bus送达，也不实现Forge post API适配**。

## 一、唯一问题与K1

从Forge默认SubscribeEvent编译的单个非static具体事件订阅方法，经限定注解转换后，能否在正常启动的独立Neo EventBus上被一次post实际调用一次？K1唯一运行变量 `-Dforgecompat.0n.post=false/true`。两轮同一runtime jar、依赖、driver、listener、event类，先C后T，每轮一次；观察字段/marker共同存在，不烧入开关。

## 二、固定输入与边界

事件类原件：0l T `D:/DualForge/spike000/logs/spike002-0l-treatment/ProbeEvent.class`，SHA `15089EC00BC3EA3867E4746DF6FDEB612CBE6C899D52F725BDD8313A7FEAD6AD`，228B；只复制不改字节（已经继承Neo Event）。

编译原注解listener的classpath仅0l新probe jar（A774D2…9AE8）和Forge eventbus6.2.27（D2A7BC…10CB999）。转换工具只对固定 `com/example/forgeprobe/SubscriberFixture.onProbe(Lcom/example/forgeprobe/ProbeEvent;)V` 的一个默认RuntimeVisible Forge SubscribeEvent descriptor换为Neo；其它方法/field/opcode完全保留。前后javap code语义相同、verbose仅此注解变化，工具exit/实际参数/全量原字节输出保存。

listener源码精确profile：Object父类，public实例int hits（初值0），默认Object构造器；一个public非static方法：

```java
@net.minecraftforge.eventbus.api.SubscribeEvent
public void onProbe(ProbeEvent event) {
    hits++;
    System.out.println("[SUBSCRIBER-FIXTURE] HANDLER REACHED hits=" + hits);
}
```

driver使用Neo API（观测器不是Forge Mod）：严格读取post属性；`BusBuilder.builder().build()`不调用startShutdown、不接触宿主总线；new SubscriberFixture、new已翻译ProbeEvent、register(listener)，打印注册返回/hits=0；C不post，T只调用一次`bus.post(event)`并检查返回对象==event；两轮结尾检查hits分别为0/1，否则throw并非吞异常。禁止反射人工调用onProbe、手动加监听器或修改hits。

运行依赖仅既存 Neo bus8.0.5、ASM9.10.1、log4j-api/core2.22.1、typetools0.6.3；ASM-tree仅离线转换工具使用，允许作为共同依赖但不允许运行classpath含Forge bus/universal/0l compat/probe jar。每件完整hash在构建前记录、执行前后核对。未安装依赖不下载，若缺件STOP；工具脚本可修复，必须与实验输入分离。

独立工作目录 `spike000/subscriber-002-0n`，证据 `logs/spike002-0n-artifacts`；原制品/服务器现场/harness均不动，不安装新Mod，不启动服务器、不读写world。

## 三、执行与判据

1. 保存固定输入/依赖前指纹、冻结说明书hash；归档原listener源码与编译class、注解转换后class及javap；driver实际编译命令/exit保存。最终runtime jar包含driver/translated listener/固定event；C/T只用这个同hash jar。
2. C：register返回、hits=0、无HANDLER REACHED、最终0；否则STOP，T不运行。
3. T：register返回/hits0→一次HANDLER REACHED hits=1→post返回sameEvent=true→最终hits1，exit0。任何新异常记录完整栈/首失败，不修复。
4. 强制执行前/后hash一致，原C/Tstdout/stderr、真实命令/exit、依赖列表/源码/每class反汇编及SHA索引全部归档。原0l event类字节必须仍完全一致。

PASS-002-0n-1：K1完整，C0/T1以及marker明确证明经bus路径调用（不是反射/人工invoke），全部输入不变。FAIL：C正确而T无新失败但未命中/计数不符。STOP：缺前置、转换模式不符、无关工具/环境失败或新异常阻断；原样报告，另轮裁定。

输出：结构化执行与顺序裁定报告（环境/输入/编译与转换/硬门/C与T原文/首次失败/完整附件/五标签/偏差/边界）。结论只限普通不取消的自定义事件、默认priority和当前独立bus；不证明原ForgeProbe的空onProbe已经invoke、FML全局bus送达、Forge post返回值兼容、取消/MONITOR/线程/业务事件。完成后再选择宿主真实送达实验，不能扩大本轮。
