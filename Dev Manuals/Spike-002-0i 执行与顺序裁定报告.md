# Spike-002-0i 执行与顺序裁定报告

2026-10-12。root串行。裁定 **STOP-002-0i-1（夹具前置缺件）**，不是S1修复失败或原Forge注册行为结论。

javac退出0；Java应用只运行一次，退出1。实际输出DECLARED_METHODS=0、CUSTOM_PUBLIC_METHODS=0 SUBSCRIBERS=0，随后在创建bus的日志初始化阶段失败；BUS_CLASS/BEFORE_REGISTER/AFTER_REGISTER均未出现，register未调用。

原始完整stderr：`spike000\logs\spike002-0i-artifacts\java.stderr.txt`，1718B。完整首栈如下：

```text
Exception in thread "main" java.lang.NoClassDefFoundError: net/minecraftforge/securemodules/SecureModuleClassLoader
	at cpw.mods.modlauncher.log.MLClassLoaderContextSelector.toContextMapKey(MLClassLoaderContextSelector.java:20)
	at org.apache.logging.log4j.core.selector.ClassLoaderContextSelector.locateContext(ClassLoaderContextSelector.java:187)
	at org.apache.logging.log4j.core.selector.ClassLoaderContextSelector.getContext(ClassLoaderContextSelector.java:140)
	at org.apache.logging.log4j.core.selector.ClassLoaderContextSelector.getContext(ClassLoaderContextSelector.java:123)
	at org.apache.logging.log4j.core.selector.ClassLoaderContextSelector.getContext(ClassLoaderContextSelector.java:117)
	at org.apache.logging.log4j.core.impl.Log4jContextFactory.getContext(Log4jContextFactory.java:149)
	at org.apache.logging.log4j.core.impl.Log4jContextFactory.getContext(Log4jContextFactory.java:46)
	at org.apache.logging.log4j.LogManager.getContext(LogManager.java:197)
	at org.apache.logging.log4j.LogManager.getLogger(LogManager.java:611)
	at org.apache.logging.log4j.LogManager.getLogger(LogManager.java:597)
	at net.minecraftforge.eventbus.EventBus.<clinit>(EventBus.java:25)
	at net.minecraftforge.eventbus.BusBuilderImpl.build(BusBuilderImpl.java:62)
	at EmptyRegistrationFixture.main(EmptyRegistrationFixture.java:18)
Caused by: java.lang.ClassNotFoundException: net.minecraftforge.securemodules.SecureModuleClassLoader
	at java.base/jdk.internal.loader.BuiltinClassLoader.loadClass(BuiltinClassLoader.java:641)
	at java.base/jdk.internal.loader.ClassLoaders$AppClassLoader.loadClass(ClassLoaders.java:188)
	at java.base/java.lang.ClassLoader.loadClass(ClassLoader.java:526)
	... 13 more
```

工具首次启动前另有采集脚本路径括号错误，未编译/启动应用，已修正并保留过程记录。javac stderr是本地化注解处理提示，原字节保留，不能称0B；它不是编译失败。

七件classpath未改变，夹具源和两个class保留。两个class哈希：3EDD9A21A53B0206349F30853D72F7A075E1AC850D67E75779C50642F6BA4B46（入口）/5525399924814351FBD166A7FDCF6CD085180C73F7DBAB092824EE93963C40BD（BareTarget）。未知原register返回值保持UNKNOWN。

后续只读定位：现有Forge安装securemodules2.2.21含缺类，ModLauncher模块声明requires cpw.mods.securejarhandler；该securemodules模块名正为此名，声明requires net.minecraftforge.unsafe（现有0.9.2）。这是前置补件依据，不是从正例反推每件依赖必要。原七件class路径没有这两项。

裁定下一轮002-0i-1：保留此STOP，另冻新增securemodules及其声明unsafe依赖的夹具环境；同一两个class不重编译、源码/builder选项不变。只运行一次，失败不继续临时追加classpath。不会改NeoForge宿主或兼容层。
