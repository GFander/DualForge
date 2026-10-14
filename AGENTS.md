# Git 工作规范

## 1. 开始任务

- 阅读适用 AGENTS.md。
- 检查当前分支、HEAD、git status、remote 和已有未提交变化。
- 记录本轮起点提交 SHA。
- 获取远端状态；联网失败时明确记录，不声称基线已更新。
- 默认从最新已验收 main 创建 codex/<任务名> 分支。
- 若用户指定了其他基线，按用户指定执行。
- 工作区有其他任务改动时，优先使用独立 worktree。
- 不覆盖、丢弃或混入用户已有改动。
- 不对用户改动自行执行 stash、reset --hard 或 clean。

## 2. 控制修改和提交范围

- 一个任务分支围绕一个明确目标。
- 只修改授权范围，不顺带重构、迁移或整理其他组件。
- 提交前检查完整 diff、新增文件及暂存区。
- 按路径选择暂存文件，不盲目执行 git add .。
- 检查误提交的凭据、依赖、构建产物、世界、缓存和日志。
- 不自行添加许可证。
- 历史冻结源码与证据保持原样。

## 3. 提交要求

- 每个 commit 表达一个完整、可解释的变化。
- 使用简明标题，例如：

  ```text
  fix: preserve Forge optional dependency metadata
  feat: derive controlled Forge entries from FML scan
  test: verify unclaimed classes remain unchanged
  docs: record Convergence-003b validation
  ```

- 不用 update、changes、fix stuff 等含糊标题。
- 复现、实现和文档可分开提交，避免人为拆成无意义的小提交。
- 沿用用户 Git 身份；缺失时询问，不伪造。
- 不擅自 amend、rebase 或改写已推送的提交。
- 未经明确授权，不强推。

## 4. 验证和证据

- 对最终代码运行与修改范围相符的检查和回归。
- 记录被测源码提交 SHA、实际命令、环境和制品 SHA-256。
- 如测试时源码尚未提交，记录源码指纹，并在提交后核对一致性。
- 区分本轮实际运行、静态检查及历史验证。
- 失败、NOT RUN、PARTIAL 如实报告，不写成 PASS。
- 最终生产代码修改后，先前验证不能直接沿用；
  应运行受影响的验证，或明确说明尚未验证。
- 报告和证据索引可以入 Git；
  完整运行附件按项目既有外部归档方式保存。

## 5. 推送和 PR

- 完成本地提交后，推送到专用任务分支。
- 核验远端分支 SHA 与本地 HEAD 一致。
- 默认一个任务一个 PR；已有对应 PR 时更新，不重复创建。
- PR 写清：
  问题及修复后的行为、修改范围、验证结果、剩余限制。
- PR 以最终实现为准，不沿用过时标题和描述。
- 验证未完成或存在阻塞时，使用 Draft PR 并明确原因。
- 用户授权合并后才合并；不自动删除分支或创建发布。
- 需要叠加任务时，明确 PR 基线和依赖关系，
  不将前置任务改动声称为本轮新增。

## 6. 交付

最终回复必须包含：

- 分支和提交 SHA
- PR 链接及状态
- 本轮修改摘要
- 实际验证结果及限制
- 是否已推送、是否已合并
- 剩余阻塞；没有则明确说明

不得把“已提交”说成“已上传”，
不得把“已创建 PR”说成“已合并”。

## 仓库结构与历史归档

- 开始修改前阅读根目录 STRUCTURE.md 和 tests/README.md，确认目录状态及实际入口。
- 唯一生产源码为 dualforge/src/main/java 和 src/main/resources；不得把历史副本当作生产版本。
- tools/entry/ 存放 zsh 入口，tools/lib/ 存放 Python 内部实现，tools/harness/run-server.zsh 是活跃独立 harness。
- tests/dependencies、tests/discovery、tests/identity、tests/events、tests/registry、tests/project_identity、tests/multi_registry、tests/block_registry 是当前测试；测试文件的位置保持不变。Registry 阶段和受控范围见 tests/registry/README.md，资源静态检查不代表真实客户端渲染。
- history/ 是历史迭代、冻结源码、夹具及记录的归档，不作为生产构建输入。原 Probe 是历史冻结回归输入，只使用指纹匹配的原制品，不重建替代。
- history/early-spikes/ 保存三个早期 Spike 的原字节，不是活跃实现。旧 Gradle/DEPRECATED 命令仅为历史记录，当前 zsh 入口不使用它们。
- 移动文件使用 git mv，冻结记录保持原字节；历史命名迁移的允许差异限于身份；后续功能按用户明确任务范围实现和验证；同步更新活跃脚本、文档和忽略规则。
- 冻结报告、索引及 Java 中的历史现场绝对路径保持原样；新旧仓库路径映射见 STRUCTURE.md，不进行全局字符串替换。
- 运行附件、依赖、制品和世界继续外部保存或存入被忽略的 out/；所有 JVM 验证串行执行。

- tests/registry/mac/ 面向新 dualforge.jar；tests/registry/mac-legacy/ 是固定旧 cohort 的显式 Legacy 工具，保持原字节。新制品不能继承旧 Mac PASS。
- 当前生产 package 为 io.github.gfander.dualforge，模块 dualforge，服务 dualforge-transform，属性前缀 dualforge.；LanguageLoader 的 javafml / 9999 契约不改。
- 命名迁移审计、逐路径豁免与保全指纹见 Dev Manuals/DualForge 命名审计.json 和 DualForge 命名统一迁移索引.json；不得以目录级豁免掩盖活跃旧引用。

- 006A 的 tests/multi_registry/ 使用独立原生Forge双Item输入和NeoForge查询driver；原005输入及贴图冻结不变。验证器按逐调用/字段数据依赖匹配，禁止新增fixture身份白名单或通用命名空间替换。
- tests/project_identity/check_identity.py 的生产规范化对照验证冻结命名迁移提交；当前命名扫描和逐路径保护继续严格。新的兼容功能必须用自己的最终新JAR重新动态验证。
- 006A-S与006A-C分别交付；服务端证据不继承客户端PASS，不能以资源静态或打包代替实机验收。

- 默认服务端输入为 runtime/neoforge-server/；只跟踪三个原配置。运行库、mods、defaultconfigs 和 spike000world 外置或忽略；复制迁移时保留原数据并核对 SHA，禁止覆盖已有不同字节文件。
- docs/README.md 是按功能导航；Dev Manuals 中冻结事实及旧命令不改写。历史路径迁移由 tests/project_identity/structure_relocation.json 精确记录，旧命名索引不改；保护全部374项，禁止目录级豁免。

- 006B通过独立Block+BlockItem profile验证两个typed Registry及精确Holder依赖，原006A Item规则不放宽。原fixture/JAR/报告不变；006B-C当前为用户Mac实机人工PASS，本轮截图WAIVED，详见Dev Manuals/Convergence-006B-C macOS 客户端人工验收报告.md；历史NOT RUN不改写，回归绑定同一已测JAR。
- 最新客户端对象统一输入在tests/client_objects/：实际ID ice_ether/raw_ice_ether/ice_ether_block与Java声明/资源同名；原005/006A/006B输入保留作冻结回归，不覆盖旧结果。两角色分开安装，当前仅Block组获用户人工PASS，新版items-test仍NOT RUN；不能以旧名称/语言覆盖或历史006A-C替代新组实机结论。截图豁免仅限本轮，Mac日志原件/实际compat SHA独立核验和原始证据持久化限制分别记录。

- 007A的tests/bootstrap使用原版启动文件，不允许隐式注入生产/业务JVM参数、Agent或额外模块参数。四生产开关默认true、诊断默认关闭；历史C/T显式条件和全部严格拒绝断言保留。旧首次预检快照不得重置来制造保全PASS，修改后采用固定提交的新验证worktree。

- 007B的tests/dropin_items保存无业务开关的原生Forge Item输入和独立查询观察器；原Forge JAR不预转换或修改。经真实失败取证，只对准入Forge automatic module名与FML metadata名不匹配使用官方ModJarMetadata；匹配旧descriptor保持委托，显式JPMS名不匹配拒绝。Reader识别guard、Parser及依赖语义不改；不新建Layer。普通profile单IEventBus ABI、API调用点/字段/数据依赖、拒绝原子性严格限定，真实第三方与Mac门槛分别交付。

- 007C的tests/real_mods/food_n_fuel使用正式未改字节Forge发行制品；先做纯Forge基线，再记录NeoForge实际首失败。run_stock的生命周期PASS不代表模组功能；原生查询、实际玩家行为和客户端渲染分别验收。当前原生基线PARTIAL、目标发现FAIL，生产零修改；不得把未来TOML/上下文/跨类支持或旧客户端PASS说成已完成，详见docs/COMPATIBILITY.md及007C报告。
