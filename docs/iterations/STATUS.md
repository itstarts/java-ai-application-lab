# 当前状态

更新时间：2026-06-30

## 已完成

- 仓库结构已初始化。
- Java/Maven 工程已放在 `backend/` 下。
- 已创建首个后端应用：`backend/apps/ai-chat-api`。
- `ai-chat-api` 当前提供：
  - `GET /health`
  - `POST /api/chat`
  - 基础请求校验
  - 校验错误的 JSON Problem Detail 响应
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

## 验证记录

构建通过 Maven Toolchains 将编译和测试固定运行在 JDK 21。首次配置工具链：

```bash
backend/scripts/setup-toolchains.sh
```

在默认 shell 中直接运行测试：

```bash
cd backend
./mvnw test
```

结果：

```text
BUILD SUCCESS
Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
```

测试运行在 JDK 21，由日志 `Starting ChatControllerTest using Java 21.0.11` 确认。

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

1. 为 `ai-chat-api` 增加模型 Provider 抽象。
2. 明确默认 OpenAI-compatible Provider 的环境变量命名和错误处理策略。
3. 补充 Provider 配置缺失和 mock provider 的测试。

## 当前风险

- 首次 clone 后需运行 `backend/scripts/setup-toolchains.sh` 更新本机 `~/.m2/toolchains.xml`，脚本保留其他 JDK 版本条目，只新增或更新 JDK 21；构建依赖该文件定位 JDK 21。
- 当前 `/api/chat` 仍是 stub，不调用外部模型。
- 当前 CI 只覆盖 Maven 测试，后续需要随项目演进增加更完整的构建、集成测试和评测步骤。
- 阶段 6/7/8 已完成重映射：旧 `evaluation`、`security`、`observability` 不再作为独立阶段 slug；正文中出现这些词时按普通技术概念理解，阶段 taxonomy 以 `docs/roadmap/java-ai-learning-roadmap.md` 为准。
