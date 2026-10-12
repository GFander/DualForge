# Run C 最终保真门（2026-10-12）

检索本轮全量 stdout / latest / debug 的判据信号与错误；完整原始文件归档，meta 与运行副本哈希已核对。

1. stdout L64 forgeprobe entrypoints 原文命中一次。
2. L65 offered 原文一次。
3. L66 REWRITE hits=1，描述符一处转换。
4. L67 post-check ctorCount=1 oldAbsent=true newPresent=true。
5. L68 dump bytes=1162；已立即归档本轮目录，SHA-256 5E2F78DB2B0909D7368A185F618C3D080C00B732279B105CD5B8010FD66D9295。
6. L69 neoforge entrypoints=[]，一次。
7. L70 delegate -> builtin javafml for neoforge，一次。
8. marker/BUS/两侧分隔符全命中；原始捕获顺序为L71 marker，L72分隔符，L73 BUS，L74分隔符，与源码次序不同。如实保留，不推测原因。
9. L75 NeoForge mod loading 命中。
10. L95 Done (0.355s)! 命中。
11. meta DONE_STOPPED_GRACEFULLY / exitCode=0 /24.9s；stderr=0 B，无crash-report。latest/debug无ERROR或FATAL。

本轮 mods 恰为 compat 4CD8195F…2FEF + 冻结probe A93A823C…74E22。控制门通过，可以启动唯一Run T；源码、新探针、compat、harness不变。
