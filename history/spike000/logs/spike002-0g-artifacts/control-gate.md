# 002-0g Run C 门

2026-10-12。C实际服务端只运行一次；前一次wrapper失败发生于启动前哈希检查，不是复跑。

offered/ctor REWRITE hits=1/post-check均在；API original-pattern hits=1；apiRewriteEnabled=false、API_REWRITE enabled=false hits=0，post-check保留完整Forge字段与调用契约。marker/BUS与neoforge委托命中。首失败NCDFE MinecraftForge，构造帧line14，与0e P1一致；没有transformer ERROR。

dump已封存并逐字节确认55D81709DC8C41A8FB25189BEF87D41F5F1920C721C02B2027F9EEE764924FE8/1381B，与0e Run T一致。运行mods恰固定probe D612…与新compat F0D51…；meta EXITED_EARLY/0/10.1s，无Done。控制门通过，准入唯一T。
