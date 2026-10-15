# Convergence-007C-4B 历史回归与负例

最终生产JAR SHA-256：`13e1b35bdb21667e2e1093e2a1e8ae28044faf2b51dbd8fce094eefcfe1d3a3f`。
本轮最终验证源码提交2ff2830，生产补正24a66d3。所有下列受影响游戏/ABI检查实际使用同一个新JAR；没有使用前版4d19414f…的运行结果替代。

| 类别 | 本轮实际结果 |
|---|---|
| 新Helper/FML/ASM | 110断言，35拒绝，PASS；原JAR/原ClassNode不污染，整组失败不发布计划 |
| 内部Bus桥接 | 8个运行时拒绝，PASS；独立白盒诊断，不混入正式C/T |
| 独立生产栈验证器 | 10个合成ASM断言，PASS；不是游戏证据 |
| 007B | 原13个严格拒绝及1个显式JPMS拒绝，PASS |
| 007C-3 | 原80断言/22拒绝，PASS；原FnF Context-only Target契约保持 |
| 007C-1 / C-2 | metadata 93断言、平台依赖91断言，PASS |
| 007A | 原4默认/8覆盖/20非法值、可选dump及I/O失败Contract，PASS；正式stock启动另有真实证据 |
| 002–005 | 原Probe smoke、官方Layer/加载、003a发现/边界、003b身份/Scope、002 metadata/CDEF、004事件C/T、005 Registry C/T/bytecode/scope/atomicity/assets，整组PASS |
| 006A / 006B | 各自原Contract、Control、Treatment及运行时字节码断言，全部PASS |
| Context真实共存 | N0、单Context、双Context+原生模组，PASS；真实独立Bus、foreign/native拒绝、Done/stop/exit0 |
| 平台动态矩阵 | 18个原案例PASS：每个健康输入与同轮N0/N1错误基线比较；负例核对具体FML原因 |
| FnF N2 | NOT RUN：Gate8只读闭包拒绝；不是第19项PASS，完整历史19-case矩阵标注PARTIAL |
| 4A离线研究回归 | 原Scanner/Prototype/拒绝/独立JVM/候选隔离PASS；4A整体PARTIAL及其历史FnF运行结果不升级 |

Python单元测试共 **74** 项PASS（7个目录，其中新cohort 11项）；zsh语法25脚本PASS；374项冻结保护PASS。原源码/配置/世界及原Fixture/JAR保全、无Java/javac进程和端口释放另有最终收据。未修改历史Java、旧断言、旧报告或冻结输入。

上述“历史回归PASS”限定为本轮要求且允许执行的旧受控功能与拒绝契约；FnF新运行不满足Gate8准入而明确NOT RUN。未将历史PASS当成新运行，也未将原生NeoForge驱动的API用法当作Forge兼容范围。

## 拒绝原因与复现

每个新静态负例保留真实Reader/FML扫描、原始/改写ClassFile、stdout/stderr和精确原因。覆盖错误来源/owner/module、native、外来同名目标、缺失helper/consumer、重复target/ID、unknownRegistry/descriptor、捕获/foreign Handle/Bootstrap、Supplier业务、字段/返回/泛型/cast、stack/frame、计划中途和dump后期失败。

追加碰撞负例先在旧生产JAR复现 **Accepted literal-registry-token**，再以enum内部值修正，Registry/Supplier字符串均不能伪造来源。该旧版完整运行虽通过当时契约，也从最终验收排除。新生产补正之后全部受影响动态与旧回归已在新的干净worktree重跑。

运行时精确拒绝包括unrecognized source BusView、foreign/escaped context、foreign helper module、outside owned entry constructor、DUPLICATE_REGISTRY_ATTACHMENT；跨模组使用真实contextpeer视图。防御性的全部receiver分支没有逐一故障注入，不能声称全面恶意JVM安全证明。

## 原始证据与命令

`/workspace/DualForge-007C4B-validation/out/convergence-007c4b/final-validation.py` 保存串行入口；每个阶段有独立command/exit/stdout/stderr，子runner保留版本、安装清单、实际Java命令、debug、assertions和input/output SHA。报告索引精确列出路径、文件大小和SHA。

原历史harness使用已有云网络适配、物理复制原世界；本轮空stock宿主/正式C/T不添加业务参数或Agent，两种模式分开报告。stock日志确有基线Settings/网络ERROR；仅断言相对本轮空宿主无新增，不声称严格零ERROR。
