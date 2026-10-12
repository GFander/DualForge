# 运行编排修正

首次尝试control在run-once.ps1输入哈希检查阶段失败：PowerShell数组项中的 `$base + 路径, 哈希` 缺少连接表达式括号，使两项被拼成一个文件名，Get-FileHash报找不到文件。

当时尚未记录harness命令、未调用run-server.ps1、未生成control目录或启动Java。修改仅为给两处路径连接加括号，正确生成路径/hash二元数组；没有修改实验输入、JVM参数或harness。失败输出在该次工具记录中；此文件为明确的过程记录，不冒充原始进程日志。修正后重新做同一launch的预检，不计为服务端复跑。
