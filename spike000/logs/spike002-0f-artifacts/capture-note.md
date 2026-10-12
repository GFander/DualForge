# 采集记录与编码修正

2026-10-12。仅静态工具，无服务端运行或制品修改。

第一阶段 collect.ps1 把 native stdout 强制按 UTF-8 解码，发现 jar 自动模块中文提示及 javap verbose 的本地化日期出现替换字符。该采集方式有损，故保留这些旧文件但不使用它们作为完整原文证据。

已改用 StandardOutput/StandardError.BaseStream → MemoryStream → WriteAllBytes，原样保存进程输出字节。四份 `*-module-raw.stdout.txt` 与七份 `*-verbose-raw.stdout.txt` 是同参数的补采证据，命令、stderr、退出码分别保存；它们取代无 `-raw` 的对应旧捕获。extra 阶段六组 builder/implementation 的 code/verbose 从一开始用修正后的字节采集。

本机模块提示原字节按 code page 936 显示为「找不到模块描述符。已派生自动模块。」；同一输出中的英文模块声明是 ASCII。后续阅读本地化原始输出使用 `[Text.Encoding]::GetEncoding(936)`。哈希计算始终对原字节，不对展示转码文本。

初次 core 的 code/entries/version 输出经扫描均为 ASCII，不受该 UTF-8 解码问题影响。manifest 为 ZIP 条目 UTF-8 文本读取，另有冻结 JAR 本体可核验。旧有损捕获、补采输出和修正脚本一并列入清单，不能把旧有损文件冒称原始字节。

两次临时查看 BusBuilder 的命令返回内容仅辅助选择同一冻结 bus JAR 内的 BusBuilderImpl；正式存档仍由 extra 阶段完整保存对应 API、实现 builder 及 EventBus。未展开反射扫描/分发/其它事件 API；没有初始化任何被调查类。
