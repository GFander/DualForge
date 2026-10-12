# Spike-002-0i-1：恢复原Forge空注册夹具的精确前置

v1.0冻结，2026-10-12，root顺序执行/复核。0i STOP成立，register未运行；本轮补前置，不回改0i记录。

复用0i已编译的两个class（入口3EDD9A21A53B0206349F30853D72F7A075E1AC850D67E75779C50642F6BA4B46，BareTarget5525399924814351FBD166A7FDCF6CD085180C73F7DBAB092824EE93963C40BD），不重编译。原七件classpath、源和builder startShutdown/useModLauncher保持。新增的固定前置为：

- Forge安装 `net/minecraftforge/securemodules/2.2.21/securemodules-2.2.21.jar`，A8254AC61485C95F652D1B3A92395FCEE375FA7D12AE996EFD672ED8BEABB3A0。
- 同安装 `net/minecraftforge/unsafe/0.9.2/unsafe-0.9.2.jar`，F5035AB2DD1C02AA354F948A28DAF2FF4215956194A98634F076AD71610FE3A4；按securemodules模块声明补齐，不声称经负对照证明此件是空注册必要条件。夹具不调用Unsafe，库自身依赖不等于助手用Unsafe取实例。

只读确认模块声明和缺类条目归属后，java -classpath 0i/classes+原七件+新增两件 EmptyRegistrationFixture，一次。没有K1必要条件证明，不把旧STOP当作完整七库与九库单变量语义实验。环境是恢复共同前提；唯一观测目标仍为该裸对象register是否返回。

目录 `spike000\logs\spike002-0i-1-artifacts`，原命令/原字节stdout/stderr/exit与前后SHA全量保存。原0i完整日志不覆盖。PASS必须编译产物未变、依赖哈希匹配、运行0、五个0i marker完整；缺marker/首异常/输入漂移STOP，禁止继续临时增依赖或修改builder选项。

结论仅原Forge eventbus6.2.27、该固定裸对象及builder选项的运行返回，不声称原ForgeProbe全局类初始化或完整加载。执行后决定是否具备限定空对象语义适配资格。
