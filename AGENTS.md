# AGENTS.md

本仓库用于学习和构建偏生产化的 Java AI 应用。AI 助手可以协助实现、评审、文档和测试，但必须遵守以下规则。

## 文档路由

- 本文件只放面向 AI 助手和协作者的硬约束；约束必须写到可直接执行，链接只用于补充细节。
- 路线、解释、模板、阶段记录和长期决策放在 `docs/`；本文件仅引用必要路径，详细说明保留在对应文档中。
- 学习路线和阶段顺序以 `docs/roadmap/java-ai-learning-roadmap.md` 为准，不自行发明新的阶段体系。
- 涉及阶段推进、计划调整、跨会话接手、文档治理或较大功能变更时，先按 `docs/README.md` 的阅读顺序进入；纯局部小改可只阅读相关上下文。
- 影响当前状态、下一步入口、验证记录或重要风险时，必须同步更新 `docs/iterations/STATUS.md`。
- 详细执行计划在进入对应阶段后创建，并基于 `docs/iterations/_template.md` 编写。
- 涉及 AI 请求、模型 Provider、错误响应、trace、日志脱敏或数据边界时，必须对齐 `docs/reference/engineering-baseline.md`。阶段 4-8 指南中的未来字段仅作为后续阶段参考，进入对应阶段并落地实现后再提升到工程基线。

## 沟通

- 默认使用中文沟通和生成文档；技术标识符、命令、API、字段、协议和文件名可保留英文。
- 代码变更后说明受影响文件、验证命令、验证结果和已知风险。
- 没有验证证据时，不得声称功能“已完成”“已修复”或“测试通过”。

## 安全与数据边界

- 具体数据边界以 `docs/reference/engineering-baseline.md` 为准。
- 处理凭证、真实业务数据、隐私数据、内部文档和样例数据时，必须先对齐工程基线。
- 新增或修改数据边界约束时，同步更新工程基线和本文件中的引用关系。

## AI 应用规则

- Prompt 变更必须进入 `prompts/` 目录进行版本管理，并在变更说明中说明影响。
- 结构化输出变更必须包含校验逻辑或测试。
- RAG 相关变更在已有评测数据时，必须记录召回、拒答和引用准确性。
- Tool Calling 的写操作必须保留后端权限校验、幂等、审计日志和人工确认。
- Agent 工作流必须持久化任务状态，并暴露失败和重试行为。
- 为保证可复现性，模型服务、模型名称、temperature 等关键请求参数应记录在日志或文档中。

## 工程规则

- 优先做小范围、阶段聚焦的变更，按学习阶段逐步扩展能力。
- 未来模块在进入对应阶段时创建，保持当前目录只包含正在使用的内容。
- 新增实现前优先遵循现有项目约定。
- Maven 模块名称应与阶段命名保持一致。
- 仓库根目录是学习工作区根；Java/Maven 构建根位于 `backend/`。
- Java 服务使用 Java 21 和 Spring Boot。
- 首次 clone 后必须运行 `backend/scripts/setup-toolchains.sh`，向本机 `~/.m2/toolchains.xml` 写入 JDK 21。未运行脚本或本机没有 JDK 21 时，Maven Toolchains 会找不到匹配 JDK 21，构建会失败。正常配置后，本机默认 `JAVA_HOME` 不需要切到 JDK 21；编译和测试由 Maven Toolchains 固定使用 JDK 21。

## 验证

- Java 代码变更后，从 `backend/` 运行最相关的 Maven Wrapper 命令，通常是 `./mvnw test` 或 `./mvnw -pl <module> test`。
- 也可以从仓库根目录运行 `backend/mvnw -f backend/pom.xml test`。
- Prompt 或 RAG 变更在已有评测集时，应更新或运行相关 eval case。
- 仅文档变更时，至少人工检查链接、路径和命令是否一致。
