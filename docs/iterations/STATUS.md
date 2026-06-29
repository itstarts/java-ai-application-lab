# 当前状态

更新时间：2026-06-29

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

## 验证记录

验证时显式指定 Homebrew JDK 21：

```bash
JAVA_HOME=/usr/local/opt/openjdk@21 PATH="/usr/local/opt/openjdk@21/bin:$PATH" mvn -f backend/pom.xml test
```

结果：

```text
BUILD SUCCESS
Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
```

## 尚未完成

- 尚未接入真实模型 Provider。
- 尚未引入 Spring AI。
- 尚未实现流式输出。
- 尚未持久化会话历史。
- 尚未建立 `evals/` 评测目录。
- 尚未创建 RAG、Tool Calling、Agent 等后续 app。

## 接手入口

继续当前阶段时，优先阅读：

1. `AGENTS.md`
2. `docs/iterations/chat-api.md`
3. `docs/iterations/backlog.md`
4. `docs/roadmap/java-ai-learning-roadmap.md`

下一步建议：

1. 为 `ai-chat-api` 增加模型 Provider 抽象。
2. 明确默认 OpenAI-compatible Provider 的环境变量命名和错误处理策略。
3. 补充 Provider 配置缺失和 mock provider 的测试。

## 当前风险

- 运行 Maven 前需要显式设置 `JAVA_HOME` 指向 JDK 21。
- 当前 `/api/chat` 仍是 stub，不调用外部模型。
- 当前 CI 只覆盖 Maven 测试，后续需要随项目演进增加更完整的构建、集成测试和评测步骤。
