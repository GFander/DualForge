# 仓库协作约定

每轮修改都必须完成版本控制后再交付：

- 核对本轮 diff，保留用户已有改动，不把无关的既有变化混入本轮提交。
- 将本轮授权改动提交到 Git，并记录实际提交 SHA。
- 推送到专用分支，创建或更新对应 PR，在最终回复中给出 PR 链接。
- 明确区分本地提交、远端推送和 PR 状态；未经授权不合并 PR。

## 仓库结构与历史归档

- 开始修改前阅读根目录 STRUCTURE.md 和 tests/README.md，确认目录状态及实际入口。
- 唯一生产源码为 spike001a/compat-002-0l/src/main/java 和 src/main/resources；不得把历史副本当作生产版本。
- tools/entry/ 存放 zsh 入口，tools/lib/ 存放 Python 内部实现，tools/harness/run-server.zsh 是活跃独立 harness。
- tests/dependencies、tests/discovery、tests/identity 是当前测试；测试文件的位置保持不变。
- history/ 是历史迭代、冻结源码、夹具及记录的归档，不作为生产构建输入。原 Probe 是历史冻结回归输入，只使用指纹匹配的原制品，不重建替代。
- spike001a/compat/ 和 spike000/probe/ 是保留原位置的遗留目录，不是活跃实现。Gradle 记录已弃用，说明见 spike001a/compat/DEPRECATED.md。
- 移动文件使用 git mv，保持 Java 内容及冻结记录原字节；同步更新活跃脚本、文档和忽略规则。
- 冻结报告、索引及 Java 中的历史现场绝对路径保持原样；新旧仓库路径映射见 STRUCTURE.md，不进行全局字符串替换。
- 运行附件、依赖、制品和世界继续外部保存或存入被忽略的 out/；所有 JVM 验证串行执行。
