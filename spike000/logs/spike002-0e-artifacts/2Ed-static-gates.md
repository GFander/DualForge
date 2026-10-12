# 2E-a 至 2E-d 执行门核对（2026-10-11）

冻结说明书 SHA-256：072ADCCB6A063E1C037836769058958E08D376E807F774EC7C69F052AB7C6F2A。

- 2Ea-frozen-refs-recheck.txt 全部匹配；工具版本均 21.0.10。
- 原 server/latest.log SHA-256 F59F43C9615F2BCC91F54801FF634CF25893D875976D0732F8481EB1B0EA41FC 与已归档 002-0d treatment 相同；debug.log 2CF844BF907F71A9EE2021B84C68E9F6E961DA63B53AA4C124F617AC7B3B69CF 同归档；crash-reports 不存在；无 java 进程。server mods 两件为旧冻结 probe 与 002-0d compat 运行副本。0e 两运行 Label 目录不存在。首轮前仅两份 server log，将由既有 harness 清理。
- 历史共享 dump 已另存 historical-ForgeProbe.rewritten.class，原哈希 5E2F78DB2B0909D7368A185F618C3D080C00B732279B105CD5B8010FD66D9295。
- 原源码 3 文件与 src-before 一致；src-after 仅 ForgeProbe.java 增加 import 与 register 语句两行；行级 diff 原文在 2Eb-source-diff.txt。
- harness 仅 ValidateSet 增加两个 Label。变后 SHA-256 84C7E2C06BF0A1D2AC0D0E2D5AD20482F1357668EC8807DAD642DA55944F77D0，精确符合冻结预期。
- javac / jar 退出码 0，实际命令、原始输出、退出码分别留存。新探针 D61224F3014F0ADFF85D10EEDEA414763C231367F08EDB99CE99DD2C8935F1C9 / 1848 B / 7 条。
- 原/新 jar 条目顺序完全相同。MANIFEST 与 mods.toml 逐字节相同，唯一差异是 ForgeProbe.class（1172→1391 B）。只打包一类，不含编译依赖。
- 构造函数 descriptor 均 (Lnet/minecraftforge/eventbus/api/IEventBus;)V；0至40 offsets 的 super 与四次 println 指令逐条完全相同。invokedynamic BootstrapMethods 仍 StringConcatFactory.makeConcatWithConstants，参数 BUS = \u0001 相同；注解与类结构不变。
- 唯一新增 43:getstatic MinecraftForge.EVENT_BUS、46:aload_0、47:invokeinterface IEventBus.register(Object):void；原43:return移至52:return，无其它指令变化。
- 两者 verbose 无 Exception table；均 interfaces=0 fields=0 methods=1。LineNumberTable 原 8:0、9:4、10:12、11:20、12:35、13:43；新 9:0、10:4、11:12、12:20、13:35、14:43、15:52。前五项源码行号+1，新语句line14与return line15另行映射。

结论：静态门通过，可执行 Run C；Run C 判据通过并封存 dump 后方可 Run T。
