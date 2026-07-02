# 当前状态

更新时间：2026-07-02

## 已完成

- 仓库结构已初始化。
- Java/Maven 工程已放在 `backend/` 下。
- 已创建首个后端应用：`backend/apps/ai-chat-api`。
- `ai-chat-api` 当前提供：
  - `GET /health`
  - `POST /api/chat`
  - 基础请求校验
  - 统一错误响应：`code`、`message`、`traceId`
  - 应用层 Provider 抽象和 `mock` Provider
  - 模型调用日志字段：`traceId`、`feature`、`provider`、`model`、`promptVersion`、`latencyMs`、`status`、`errorCode`
- 当前仓库已存在 `ai-chat-api` 本地 mock Provider 代码和 Controller 测试；后续开发是在现有骨架上迭代，不是从空项目开始。
- 已创建中文 `README.md`。
- 已创建中文 `AGENTS.md`。
- 已创建 GitHub Actions 构建配置。
- 已创建 Prompt 示例：`prompts/chat/system-v1.md`。
- 已导入完整学习路线：`docs/roadmap/java-ai-learning-roadmap.md`。
- 已明确 `AGENTS.md` 与 `docs/` 的分工：硬约束写入 `AGENTS.md`，路线、模板、状态、待办和决策记录写入 `docs/`。
- 已用显式 JDK 21 验证 Maven 测试通过。
- 已通过 Maven Wrapper 和 Toolchains 固定 JDK 21 编译和测试，本机默认 JDK 无需切换。
- 已将 AI 应用开发补充文档纳入仓库：
  - `docs/roadmap/project-evolution-roadmap.md`
  - `docs/reference/engineering-baseline.md`
  - `docs/stages/stage-00-ai-application-basics.md`
  - `docs/stages/stage-00-data-boundary.md`
  - `docs/stages/stage-01-llm-chat-service.md`
  - `docs/stages/stage-02-prompt-structured-output.md`
  - `docs/stages/stage-03-rag-knowledge-base.md`
  - `docs/stages/stage-04-tool-calling.md`
  - `docs/stages/stage-05-agent-workflow.md`
  - `docs/stages/stage-06-evaluation-security-observability.md`
  - `docs/stages/stage-07-production-cost-governance.md`
  - `docs/stages/stage-08-model-local-inference.md`
- 已明确仓库内 `docs/` 是工程文档权威来源；个人知识库或飞书副本只用于阅读和导入。
- 已明确阶段 1 前置实现决策：
  - 先实现项目自己的应用层 Provider 抽象和 mock/stub Provider。
  - 当前阶段不引入 Spring AI，后续引入时作为 Provider 实现或增强项接入。
  - 默认 `AI_PROVIDER=mock`，聊天模型环境变量统一使用 `AI_CHAT_MODEL`。
  - 真实模型调用在 mock/stub Provider、配置缺失和错误映射测试稳定后再接入。
- 已为 `ai-chat-api` 落地阶段 1 mock-first Provider 基础：
  - Controller 通过 `ChatService` 访问应用层 Provider 接口，不直接绑定具体模型 SDK。
  - Maven `groupId` 统一为 `io.github.itstarts.aialab`，`ai-chat-api` Java 根包统一为 `io.github.itstarts.aialab.chat`。
  - HTTP 请求、响应和错误 DTO 已从 Controller/Handler 拆入 `api.dto` 包。
  - Provider 包已按应用层契约、配置、错误和 mock 实现分包；公共契约 DTO 使用 `ProviderChatRequest` / `ProviderChatResponse`，具体厂商 DTO 后续放入各自 Provider 子包。
  - `MockChatProvider` 默认返回本地 echo 响应，不调用外部模型。
  - `application.yml` 通过 `AI_PROVIDER`、`AI_CHAT_MODEL`、`AI_REQUEST_TIMEOUT` 读取模型配置，默认 `mock` / `mock-chat`。
  - Provider 未找到返回 `AI_PROVIDER_NOT_FOUND`，模型名缺失返回 `AI_MODEL_NOT_CONFIGURED`，Provider 错误映射为 `AI_PROVIDER_ERROR`。
  - Provider 异常可携带错误分类，已覆盖 `AI_REQUEST_TIMEOUT`、`AI_RATE_LIMITED`、`AI_EMPTY_RESPONSE` 的应用层错误映射。
  - `backend/pom.xml` 统一提供 Lombok、Apache Commons Lang 和 Spring Boot configuration processor 等通用依赖，当前模块继承使用。
- 已在 `AGENTS.md` 增加通用 Git 协作规则：Agent 可以建议提交或推送时机，执行 `commit`、`push`、`tag`、`release` 仍需用户明确指令。
- 已在 `AGENTS.md` 补充学习仓库的 PR/MR 使用边界：多数阶段 checkpoint 可直接提交到 `main` 并打 tag，远端评审、跨模块、数据模型或高回滚成本变更再建议开发分支和合并请求。
- 已重写 `AGENTS.md` 的 `Git 协作` 小节，明确本仓库以 `main + 阶段 tag` 为主策略，并补充阶段 tag 建议和跨 Agent 接手阅读要求。

## 验证记录

构建通过 Maven Toolchains 将编译和测试固定运行在 JDK 21。

该脚本需要本机已安装 `python3`，用于安全合并 `~/.m2/toolchains.xml`。

2026-07-01 验证：

先运行工具链配置：

```bash
backend/scripts/setup-toolchains.sh
```

结果：

```text
已更新本机 ~/.m2/toolchains.xml
已检测到 JDK 21 安装路径
```

再运行测试：

```bash
cd backend
./mvnw test
```

结果：

```text
BUILD SUCCESS
Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
```

测试运行在 JDK 21，由日志 `Starting ChatControllerTest using Java 21.0.11` 和 `Toolchain in maven-surefire-plugin` 确认；具体 JDK 安装路径因本机环境而异，不写入状态文档。

2026-07-02 文档检查：

```bash
git diff --check
```

结果：无输出。

2026-07-02 `AGENTS.md` Git 协作规则复审：

- Claude 最终复审结论：P0/P1/P2/P3 问题均为 0。
- 已确认 `AGENTS.md`、`README.md` 的 `main + 阶段 tag` 策略和跨 Agent 接手规则自洽。

2026-07-02 `ai-chat-api` Provider 抽象和 mock Provider 验证：

```bash
cd backend
./mvnw test
```

结果：

```text
BUILD SUCCESS
Tests run: 25, Failures: 0, Errors: 0, Skipped: 0
```

本次验证包含父级 Maven POM 统一通用依赖后的编译、注解处理和测试运行。

覆盖范围：

- `GET /health` 正常返回。
- `POST /api/chat` 在 `mock` Provider 下返回 `provider`、`model`、`content`、`traceId`。
- 请求校验错误返回统一 `CHAT_MESSAGE_INVALID` 结构。
- 仅包含 Unicode 空白字符的消息会被拒绝，Unicode 空白包裹的有效消息会在进入 Provider 前规范化。
- 畸形 JSON 请求体返回统一 `CHAT_MESSAGE_INVALID` 结构。
- Spring MVC 404、405 和 415 框架异常返回统一错误结构，并保留对应 HTTP 状态。
- 恰好 4000 字符的消息可正常通过。
- 超过 4000 字符的消息返回统一 `CHAT_MESSAGE_INVALID` 结构。
- `AI_PROVIDER` 指向未实现 Provider 时返回 `AI_PROVIDER_NOT_FOUND`。
- `AI_CHAT_MODEL` 缺失时返回 `AI_MODEL_NOT_CONFIGURED`。
- 非正数 `AI_REQUEST_TIMEOUT` 配置通过 Bean Validation 校验拒绝。
- `AI_PROVIDER`、`AI_REQUEST_TIMEOUT` 和 `AI_CHAT_MODEL` 的默认值和缺失语义有单元测试覆盖。
- mock Provider 模拟错误时返回 `AI_PROVIDER_ERROR`。
- Provider 异常携带具体分类时可映射为对应 API 错误，例如 `AI_REQUEST_TIMEOUT`。
- Provider 异常映射保留原始 cause 链。
- 未预期异常保留同一 `traceId`，并返回统一 `INTERNAL_SERVER_ERROR` 结构。
- `AI_REQUEST_TIMEOUT` 可进入 Provider 请求契约。

文档和空白检查：

```bash
git diff --check
```

结果：无输出。

2026-07-02 `ai-chat-api` 包名和 Provider/API DTO 分包整理验证：

```bash
cd backend
./mvnw test
```

结果：

```text
BUILD SUCCESS
Tests run: 25, Failures: 0, Errors: 0, Skipped: 0
```

本次验证包含 Maven 坐标改为 `io.github.itstarts.aialab`、Java 根包改为 `io.github.itstarts.aialab.chat`、API DTO 拆分和 Provider 分包整理后的编译与测试运行。

2026-07-02 `ai-chat-api` Provider 错误映射补充分支验证：

```bash
cd backend
./mvnw test
```

结果：

```text
BUILD SUCCESS
Tests run: 27, Failures: 0, Errors: 0, Skipped: 0
```

本次验证补齐 `AI_RATE_LIMITED` 和 `AI_EMPTY_RESPONSE` 的 mock Provider 触发分支，并通过 `POST /api/chat` 覆盖统一错误响应结构、HTTP 状态和 `traceId`。

文档和空白检查：

```bash
git diff --check
```

结果：无输出。

独立评审：

- Codex 子代理 Wegener 基于最新代码 diff 完成只读评审。
- 评审结论：Ready to merge? Yes；Critical、Important、Minor 问题均为 0。
- 评审确认本次变更未接入真实模型、未引入 Spring AI，保持 Provider 抽象边界和日志脱敏路径。

## 尚未完成

- 尚未接入真实模型 Provider。
- 尚未引入 Spring AI。
- 尚未实现流式输出。
- 尚未持久化会话历史。
- 尚未建立 `evals/` 评测目录。
- 尚未创建 RAG、Tool Calling、Agent 等后续 app。
- 阶段 0-8 补充文档已入仓库，其中阶段 2-8 的对应能力尚未进入代码实现；当前生效工程契约以 `docs/reference/engineering-baseline.md` 为准。

## 接手入口

继续当前阶段时，先按 `docs/README.md` 的阅读顺序进入；当前阶段的执行文档是 `docs/iterations/chat-api.md`。

下一步建议：

1. 明确真实模型服务、数据边界、超时和错误映射后，再实现 OpenAI-compatible Provider。
2. 真实 Provider 接入后，在 `README.md` 补充最小可运行配置示例。
3. 继续保持当前阶段不引入 Spring AI；后续引入时作为 Provider 实现或增强项接入。

## 当前风险

- 首次 clone 后需运行 `backend/scripts/setup-toolchains.sh` 更新本机 `~/.m2/toolchains.xml`，脚本保留其他 JDK 版本条目，只新增或更新 JDK 21；构建依赖该文件定位 JDK 21。
- 当前 `/api/chat` 仍使用本地 `mock` Provider，不调用外部模型。
- 当前 CI 只覆盖 Maven 测试，后续需要随项目演进增加更完整的构建、集成测试和评测步骤。
- 阶段 6/7/8 已完成重映射：旧 `evaluation`、`security`、`observability` 不再作为独立阶段 slug；正文中出现这些词时按普通技术概念理解，阶段 taxonomy 以 `docs/roadmap/java-ai-learning-roadmap.md` 为准。
- Codex worktree 环境不会自动携带本机 `.env`，首次构建也可能需要下载 Maven 分发版和依赖；使用 worktree 开发前应先完成 JDK 21 toolchain 和依赖准备。
