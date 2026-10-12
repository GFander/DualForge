# Spike-002-0i-1 执行与顺序裁定报告

2026-10-12，root串行。**PASS-002-0i-1**：同一两个class未重编译，七原依赖不变，补securemodules2.2.21及其声明unsafe依赖后，唯一Java应用运行退出0；stderr0B。

stdout完整原文在0i-1-artifacts/java.stdout.txt（包含4行Log4j包扫描弃用WARN，非异常）；判据信号按原文选录：

```text
DECLARED_METHODS=0
CUSTOM_PUBLIC_METHODS=0 SUBSCRIBERS=0
BUS_CLASS=net.minecraftforge.eventbus.EventBus
BEFORE_REGISTER
AFTER_REGISTER
```

准确结论：[E]在原Forge eventbus6.2.27、该固定裸对象、startShutdown/useModLauncher选项和冻结九库classpath下，register正常返回。这个夹具不是ForgeProbe同一class，也不是MinecraftForge全局实例或原Forge宿主加载；不推一般注册兼容。

[F]+[E]限定连接：0h固定probe没有普通声明方法、直接继承Object；原registerObject在没有非Object公共候选/SubscribeEvent方法时正常return。夹具运行已测这个裸对象形态。因此下轮可以为**固定probe同形态**试验空注册保真；不能未经验证用于有方法/监听器/其它对象。

0i旧STOP保留为前置缺件，不升格PASS。新增两件是按已测缺类及模块声明恢复前提，不声称负对照证明unsafe必要。没有改builder、不临时追加其它库、不重新编译；0i-1应用只运行一次。

采集脚本复用0g原字节Native函数，明确记录这项工具复用，没有复跑0g。初/终九项输入相同；javac来自旧0i，不称本轮编译。原源与两个class哈希保持。完整命令、classpath、模块描述、条目清单、stdout/stderr/exit均在spike002-0i-1-artifacts。

顺序裁定：准入002-0j限定空对象语义适配K1，继续固定0e probe和0g成员映射，不吞异常、不添加监听器。仅当对象类名精确ForgeProbe、直接父类Object、声明方法0且公共非Object方法0时返回；不满足则原样调用Neo register。运行事实仍须K1；不称这段适配为通用Forge IEventBus实现。
