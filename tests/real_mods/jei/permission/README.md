# 010G：原始权限谓词与服务端来源约束

整体 PARTIAL。原始 JEI 删除包的权限函数及四个相关方法/clinit 已通过有限只读证明；完整 process 在下一项玩家库存读取处继续整组拒绝。原 Packet、权限函数、拒绝分支及反馈发送没有被改写。没有通用 Forge 权限 API、固定授权值、OP 授予、假 ServerPlayer 或 NoOp。

`ForgePermissionPlan` 验证原调用顺序、分支目标、三个当前配置 getter、原生 Player/CommandSourceStack/服务器接口、Optional 和两个精确 Lambda bootstrap。配置角色来自实际数据流，不由方法名推断。独立夹具沿用既有三个 getter：enabled→creative、secondary→op、cancelDeath→give；原JEI对应其三个原始getter，生产无名称白名单。

原函数先处理 creative；随后创建 CommandSourceStack。op配置启用且服务器非null时，直接返回 `hasPermission(server.getOperatorUserPermissionLevel())`，false也不回退give。否则按give配置查询当前服务器dispatcher的`give`节点并调用实际`canUse`，缺失节点返回false。不是固定OP等级，也不是对true/false做替换。原函数允许creative分支在查询服务器前返回；隔离的null-server案例仅证明原函数行为。获准网络回调在调用该函数前必须通过严格服务器/连接身份护栏，不能以该布尔值替代身份验证。

新增计数器业务Profile只接受SERVER配置、全部serverbound定义及原`sender→config→enqueue→process`数据流。当前宿主约束为固定NeoForge专服，13个MC宿主二进制绑定。原生ServerPlayer与GameListener须为宿主真实Class、相互引用同一玩家/连接、连接有效、服务器等于实际调度loop；排队执行前再次检查来源和服务器线程。Integrated Server、未知子类、客户端声明和未验证连接保持拒绝。旧E/F路径不启用新guard，原enqueue异常算法保留。

独立输入gatealpha/gatebeta仅用Forge SDK编译一次，双方使用同SHA。各3个serverbound派生Payload，总6个官方定义；22个来源类只转换8类，14类只读依赖（包含权限utility）保持原字节。全部22类参加fingerprint和foreign冲突检查。G utility只读、原JEIutility只证明可达权限方法；未验证的其它方法不因此获准。

## 证据层

- `collect_abi.py`：重新核对923个原Forge发行ClassFile及原始权限/Context/删除包/网络，保留完整javap；双宿主固定补丁SDK，包含Entity继承方法、原生权限、命令和连接依赖。
- `compare_semantics.py`：比较选定真实宿主方法的规范化指令；不把局部相等当作所有动态玩家/OP服务已证明。
- `PermissionContract.java`：两个完整来源计划、29类必须拒绝的静态/owner映射、全组源SHA和只读依赖、固定host ABI及原JEI权限后续库存门禁。
- `PermissionModels.java.in`：原始未改JEI函数与独立函数在同一普通Player组件、真实native CommandSourceStack/命令节点、真正当前已注册配置上对照；8配置×2creative×5level×2模拟server引用×2mod=320案例，每次110允许/210拒绝。安全counter执行原process，拒绝不增加protectedCalls。缺失命令、null与原配置异常有实际证据，临时状态恢复，既有局部计数不回滚。
- `IdentityModels.java.in` / `OwnerModels.java.in`：真实FML owner下隔离的Module/Loader/Container/authority/source/thread拒绝；普通Player测试对象本身也不能通过ServerPlayer guard。
- `run_live.py`：真实Forge/Neo FML first/restart；复制已停机world/config后仅在新副本修改alpha配置，对照当前getter。普通Player模型没有加入玩家列表或建立连接；没有创建ServerPlayer。REAL_SERVER_PLAYER/REAL_CONNECTED_PLAYER/REAL_PACKET_DELIVERY=NOT_RUN。
- `run_f_contract.py` / `run_boundary.py` / `run_e_boundary.py`：保留原E/F冻结测试，新的适配副本只迁移原JEI已被证明的旧首失败预期为新库存门禁；其它负例保留。真正未知权限方法仍有G独立变异拒绝。
- `run_remaining.py`：最终JAR的B/C/D/E/F、Mouse、Helper、静态及原JEI真实单因素首断点；旧输入按SHA复用，结果重新运行。

原函数组件使用隔离URLClassLoader及原接口的配置代理；不等价原JEI GAME层构造。ModelPlayer是明确标注的未连接普通Player组件，仅其权限输入由模型控制，没有假ServerPlayer，不用于生产身份取得。正反布尔结果不是REAL_PLAYER_PERMISSION证据。源码、完整指纹和最终选定运行以交付报告及SSD收据为准。

## 执行

所有本地JVM串行，输出必须是SSD下的新目录；官方启动文件、不增加业务JVM参数或Agent。示例路径变量均由调用者替换：

```bash
python3 -B tests/real_mods/jei/permission/collect_abi.py --output SSD_FRESH
python3 -B tests/real_mods/jei/permission/compare_semantics.py --abi ABI --output SSD_FRESH
python3 -B tests/real_mods/jei/permission/build_fixture.py --output SSD_FRESH
python3 -B tests/real_mods/jei/permission/build_observer.py --output SSD_FRESH
python3 -B tests/real_mods/jei/permission/run_contract.py --jar FINAL --fixtures INPUTS --abi ABI --output SSD_FRESH
python3 -B tests/real_mods/jei/permission/run_live.py --jar FINAL --fixtures INPUTS --observers OBSERVERS --output SSD_FRESH
python3 -B tests/real_mods/jei/permission/run_remaining.py --jar FINAL --output SSD_FRESH
python3 -B tests/real_mods/mouse_tweaks/run_regression.py --jar FINAL --inputs-008a /home/albert/Env/DualForge/runs/convergence-008a-20261017 --output SSD_FRESH
```

`retain_evidence.py`只回收本G任务已完成且每文件SHA与完整canonical SDK相同的重复server/libraries；保留mods、world、config、命令、日志及恢复清单，不删除旧阶段。真实玩家/库存/响应发送及客户端仍未交付。`010G_JEI_CLIENT_ZIP=NOT_PRODUCED`。
