# Forge-NeoForge API差异报告

日期：2026-10-15（Asia/Shanghai）。独立 Draft [PR #22](https://github.com/GFander/DualForge/pull/22)，base 为未合并 PR #21 分支。任务起点 `29893683ea584b26208eb205b819c3f3376147be`，实际被测工具源码 `25571c40671d6d80ca10a4dcec77db84440ff555`。生产目录与起点逐文件相同。

扫描105708条ClassFile记录，含同名跨artifact、MR及共享Java输入。该数不是唯一类数量，也不是公共API数。选定服务端视图统计如下；dependencies含JDK和第三方，**不计入platform API覆盖率**。

| 平台 | 类别 | 类 | 字段 | 方法 | 公共/保护类 | 公共/保护字段 | 公共/保护方法 |
|---|---|---|---|---|---|---|---|
| forge | dependencies | 31850 | 104506 | 356158 | 14631 | 36383 | 181581 |
| forge | minecraft | 6482 | 31012 | 62571 | 4970 | 15417 | 38630 |
| forge | platform | 1603 | 4388 | 11444 | 1322 | 1377 | 7384 |
| neoforge | dependencies | 33250 | 105993 | 362585 | 15546 | 36744 | 185506 |
| neoforge | minecraft | 6528 | 31196 | 63135 | 5011 | 15561 | 39040 |
| neoforge | platform | 1925 | 4874 | 13155 | 1568 | 1647 | 8780 |

API发现保留所有可观察内部字段/方法、构造、默认/静态/桥接方法及Generic Signature。Descriptor用于JVM身份，Signature单独保留；解析以ASM9.10.1为核心，非正则ClassFile解析。稳定符号为owner、owner.field:desc、owner.method(desc)。继承解析保守处理默认接口冲突，private/构造器边界不继承。缺类或多候选不靠相似度选结果。

17435个来源平台符号，按不同单位统计，不能把类/字段/方法或使用点混成一个百分比：

| 单位 | 单一目标待语义审查 | 歧义 | 未解析 |
|---|---|---|---|
| CLASS | 902 | 0 | 701 |
| FIELD | 2427 | 186 | 1775 |
| METHOD | 5236 | 204 | 6004 |

按策略的实际计数：

| 单位 | Exact | Class rename | Field rewrite | Method rewrite | Descriptor rewrite | Runtime shim | Unresolved策略 |
|---|---|---|---|---|---|---|---|
| CLASS | 0 | 892 | 0 | 0 | 0 | 55 | 656 |
| FIELD | 0 | 0 | 1980 | 0 | 433 | 123 | 1852 |
| METHOD | 0 | 0 | 0 | 3778 | 1417 | 351 | 5898 |

策略与决策状态是两轴；Unresolved策略可包含歧义，Shim策略可尚无可靠目标，不能再次累加两表。共享第三方同名API和MC另表，不塞进平台Exact计数。本次平台Exact计数为0，不代表所有API都不兼容；相同owner/name/descriptor/必要flags的匹配分支仍存在。模糊名称只形成候选，成员实际存在性来自固定官方二进制；调用kind、继承、签名、生命周期仍独立审查。所有17435条均通过版本化JSON Schema，productionEnabled/runtimeVerified全为false。

服务端MC：Forge6482类，NeoForge6528类；629类有声明ABI差异。与同一官方安装器Mojang命名的原版服务端基线相比，Forge807类、NeoForge963类发生声明变化。原版/补丁对照含接口、父类、access、字段/方法/泛型签名；不是行为等价证明。实际补丁关联引用包括CreativeModeTab.builder()和Item继承的getBurnTime，两宿主都能静态解析相应ABI，功能仍未重测。

两平台提供的服务端成员均为Mojang名称。FnF本轮原始JAR也无m_/f_引用；不能把旧报告中的SRG描述转成伪造的API差异。命名工具仍能按精确obf descriptor连接官方MCP与Mojang数据（含重载负边界），只是未用于本FnF改写。

105708条扫描两次SHA相同；15份规范化输出也逐一相同。完整原始扫描SHA `cef48473c5474df918e4edc446c181f3e07c4d92448ffc692b19858e4fa62334`；规范化api-diff SHA `2fafd40137374d09e8f4826d0bf3618385ec5fe2af934a30a02398dfea5ca340`。数据不含工具插入的工作区路径或随机时间。客户端完整patch缺失、运行时注入形状、反射/动态生成和保守继承解析均为明确盲区；本门禁总体PARTIAL。
