# 006B-C：冰以太方块 macOS 人工验收

本包使用用户提供的原始16×16冰以太方块贴图，方块六面共用它；对应BlockItem继承同一立方体模型。
注册ID保持 `blockregistryprobe:sample_block`；英文名 **Ice Ether Block**，中文名 **冰之以太块**。
PNG的RGBA及半透明像素保持原字节；本轮沿用普通Block渲染路径，请记录实机的实际视觉效果。
状态是 **CLIENT_READY / CLIENT NOT RUN**，静态资源及打包PASS不代表已经实际渲染。

## 安装到独立Prism实例

1. 解压ZIP，进入 `dualforge-006b-mac-test-kit`；运行 `shasum -a 256 -c SHA256SUMS`，须全部OK。
2. 在原生Prism新建 `DualForge-006B-Mac`：Minecraft **1.21.1**、NeoForge **21.1.256**、同CPU架构的Java **21**。
3. 打开实例文件夹，将本包 `mods/` 中两个JAR复制到该新实例 `.minecraft/mods/`。使用本次两个文件，避免混装旧006A/005夹具或其他兼容层。
4. Prism实例 Settings → Java → 启用实例JVM参数覆盖；将 `JVM-ARGS.txt` 的七行作为以空格分隔的JVM参数粘贴。不要填入Minecraft命令参数，也无需修改全局设置。
5. 启动前在实例mods中执行 `shasum -a 256 dualforge.jar block-registry-probe.jar`，将两项SHA与本包manifest及CHECKLIST一并保存。
6. 本包另附 `resourcepacks/dualforge-ice-ether-names.zip`。复制到实例 `.minecraft/resourcepacks/`，在游戏 Options → Resource Packs 中启用，作为已有受控物品的中英文名称覆盖；方块JAR自身也带正确名称。此语言包只含名称，不添加注册对象，原005/006A模组无需修改。

此包包含原兼容层和新增资源版原生Forge输入，所有旧JAR成员（含class、Manifest、TOML）原字节保留。
资源版fixture具有新的JAR SHA；服务端PASS属于原冻结fixture，不把它冒充本包客户端PASS。
不包含查询Driver、Minecraft/NeoForge/Java依赖、账号、世界或旧日志；不使用旧005安装脚本，因为其fixture校验不同。

名称映射如下；已有注册ID不改。语言包也可单独用于原005或006A独立实例，不需要为核对名称把多个Forge fixture同时安装。

|已有对象|中文|英文|
|---|---|---|
|005 registryprobe:ice_ether；006A multiregistryprobe:alpha_item|冰之以太|Ice Ether|
|006A multiregistryprobe:beta_item（粗矿贴图）|冰之以太粗矿|Raw Ice Ether|
|006B blockregistryprobe:sample_block|冰之以太块|Ice Ether Block|

## 实机验收

启动前在实例外保存阶段标记：`touch "$HOME/Desktop/dualforge-006b-client-start.marker"`。
启用作弊的新单人世界中执行：

```text
/give @p blockregistryprobe:sample_block 16
```

依次观察背包中的立方体图标、第一人称及第三人称手持显示，再右键实际放置多个方块。
检查各面均显示给定蓝色纹理，无紫黑缺图；绕方块观察并截图。保存退出后重新进入世界，确认方块仍存在。
分别将语言切换到English (US)与简体中文，将鼠标悬停物品，核对上述名称并截图。
正常保存退出至桌面，检查本次latest.log/debug.log，无新增ERROR/FATAL、dump FAIL或crash。
日志中的构造marker、`ATTACH RETURNED`和两个Supplier marker可以辅助判断加载；单凭日志不能判断渲染成功。

## 返回验收材料

填写CHECKLIST.md中的实际版本、两JAR SHA和各项实际结果。返回本次 `.minecraft/logs/latest.log`、`debug.log`、必要crash（如有）以及背包/手持/实际放置/中英文名称截图。
日志和截图分享前检查用户名、token、聊天、账号信息、私有地址和个人路径；只处理分享副本，保留本地原件。
不要上传整个实例、世界或依赖；模板里的关键词不是运行日志证据。
用户人工核验和原始材料复核完成后才能更新006B-C结论，不继承006A-C。

回滚仅从这次新建实例移走已核验SHA的两个JAR，保留世界和其他个人文件。不自动合并PR或进入006C。
