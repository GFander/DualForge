# Spike-002-0i：原Forge空对象注册的有界JVM运行

v1.0冻结，2026-10-12，root串行。依据0h PASS与用户自动推进授权。本轮只取得原Forge eventbus6.2.27对零监听方法对象的运行事实，不验证MinecraftForge全局初始化或原ForgeProbe完整加载。使用独立夹具，不改宿主/probe/compat/harness，不启动服务端，不使用Neo总线，不下载依赖。

工作/证据：`D:\DualForge\spike000\logs\spike002-0i-artifacts`。同JDK21.0.10。只编译并运行下述夹具一次；运行class路径均来自既存Forge安装：

| JAR（forge-server/libraries相对路径） | SHA-256 |
|---|---|
| net/minecraftforge/eventbus/6.2.27/eventbus-6.2.27.jar | D2A7BCECB80B33F90854A003DC295960263D78435D9D6016C427D711510CB999 |
| net/minecraftforge/modlauncher/10.2.4/modlauncher-10.2.4.jar | FD5B1BEC9C1700052959DD2A3A934496C68CD7C0ECB18AED4D1FBA784122A134 |
| org/apache/logging/log4j/log4j-api/2.22.1/log4j-api-2.22.1.jar | 5D7BEAE7FF15D8516D6517121D7F12A79A6AC180DF64B5FCEC55D5BE21056E53 |
| org/apache/logging/log4j/log4j-core/2.22.1/log4j-core-2.22.1.jar | 46DCCECAC556623D8E2CE8648496824A82951D139062A4E61148AFF1A25ED18D |
| org/ow2/asm/asm/9.7.1/asm-9.7.1.jar | 8CADD43AC5EB6D09DE05FAECCA38B917A040BB9139C7EDEB4CC81C740B713281 |
| org/ow2/asm/asm-tree/9.7.1/asm-tree-9.7.1.jar | 9929881F59EB6B840E86D54570C77B59CE721D104E6DFD7A40978991C2D3B41F |
| net/jodah/typetools/0.6.3/typetools-0.6.3.jar | 408300867804BE05D1083CC0287DD3A21295A9E76090E288013BA5A4182DC96D |

先保存完整classpath、初/终检哈希、实际命令/原字节stdout/stderr/exit。javac --release21 -encodingUTF-8输出新classes；java -classpath classes+上述七件运行EmptyRegistrationFixture。不混入Neo库。源文件逐字如下：

```java
import java.lang.reflect.Method;
import net.minecraftforge.eventbus.api.BusBuilder;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class EmptyRegistrationFixture {
    public static final class BareTarget { }
    public static void main(String[] args) {
        BareTarget target = new BareTarget();
        int customPublic = 0;
        int subscribers = 0;
        for (Method method : target.getClass().getMethods()) {
            if (method.getDeclaringClass() != Object.class) customPublic++;
            if (method.isAnnotationPresent(SubscribeEvent.class)) subscribers++;
        }
        System.out.println("DECLARED_METHODS=" + target.getClass().getDeclaredMethods().length);
        System.out.println("CUSTOM_PUBLIC_METHODS=" + customPublic + " SUBSCRIBERS=" + subscribers);
        IEventBus bus = BusBuilder.builder().startShutdown().useModLauncher().build();
        System.out.println("BUS_CLASS=" + bus.getClass().getName());
        System.out.println("BEFORE_REGISTER");
        bus.register(target);
        System.out.println("AFTER_REGISTER");
    }
}
```

builder两选项与已取证Forge全局bus形成链相同；没有静默换成默认builder。factory初始化必要类型用原Forge安装版本。若需要Launcher/GameLayer实际启动或未冻结依赖才能继续，STOP记录完整失败，不删选项、不补依赖或复跑。

PASS-002-0i-1：编译0、运行0、DECLARED_METHODS0/CUSTOM_PUBLIC0/SUBSCRIBERS0、BUS_CLASS为Forge EventBus、BEFORE与AFTER_REGISTER均实际输出，无首失败，输入未变。原始异常存在则STOP，不以退出0单独替代判据。本实验不是K1因果必要条件证明，也不是原Forge Mod加载；只证该版本、builder选项、裸对象夹具的一次register正常返回。

实际PlainTarget与ForgeProbe不是同一class；连接到probe只能用0h结构/直接分支的限定推理，不能把夹具成功当作原ForgeProbe已运行。完成后顺序裁定再决定最小语义适配，不能把测试成功推广到有订阅方法对象。
