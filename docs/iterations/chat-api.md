# Chat API 迭代计划

对应路线图阶段：阶段 1，LLM API 与聊天服务。

## 目标

把 `backend/apps/ai-chat-api` 从 stub 聊天接口演进为可配置、可测试、可逐步接入真实模型的 Chat API。

## 当前范围

本阶段优先完成：

- 保留 `GET /health`。
- 完善 `POST /api/chat` 的请求和响应结构。
- 抽象模型 Provider 接口，先让应用代码不直接绑定具体厂商。
- 先实现 mock/stub Provider，并覆盖正常返回、配置缺失和错误分支测试。
- 在 mock/stub Provider 稳定后再接入 OpenAI-compatible Provider。
- 通过环境变量读取 Provider 配置。
- 增加基础错误处理。
- 增加模型调用日志，包括 provider、model、耗时和错误信息。
- 保持 API Key 不进入日志和版本库。

## 非目标

本阶段边界：

- 流式输出。
- 会话历史持久化。
- RAG。
- Tool Calling。
- Agent 工作流。
- 多模型路由。
- 前端页面。
- 当前阶段不引入 Spring AI；先保留应用层 Provider 抽象，Spring AI 在后续阶段作为 Provider 实现或增强项接入。

## 交付物

- 代码：
  - `backend/apps/ai-chat-api` 内的 Chat API 实现。
  - Provider 抽象和 mock/stub Provider 实现。
  - OpenAI-compatible Provider 在 mock/stub Provider 和错误分支测试稳定后实现。
  - 配置类和错误处理。
- 文档：
  - 更新 `README.md` 中的本地启动和环境变量说明。
  - 必要时更新本文件的状态。
- Prompt：
  - 继续使用 `prompts/chat/system-v1.md`。
- 测试或评测：
  - Controller 测试。
  - Provider 配置缺失时的错误测试。
  - Provider stub 或 mock 测试。

## 验收标准

- `GET /health` 正常返回。
- `POST /api/chat` 在 stub 或 mock provider 下可稳定返回。
- 配置缺失时返回明确错误，不暴露密钥。
- API Key 只通过环境变量提供，不出现在代码、README 示例真实值或日志中。
- Maven 测试通过。

## 当前实现决策

- Provider 抽象使用项目自己的应用层接口，Controller 只依赖应用服务，不直接依赖具体模型 SDK。
- `ai-chat-api` Java 根包使用 `io.github.itstarts.aialab.chat`，Maven `groupId` 使用 `io.github.itstarts.aialab`。
- HTTP 请求、响应和错误 DTO 放在 `api.dto`；Provider 公共契约放在 `provider`，配置放在 `provider.config`，异常分类放在 `provider.error`，mock 实现放在 `provider.mock`。
- 公共 Provider 契约 DTO 使用 `ProviderChatRequest` / `ProviderChatResponse`；后续具体厂商请求响应类放入对应 Provider 子包，例如 `provider.openai.dto`。
- 当前阶段默认 Provider 为 `mock`，用于本地开发和 CI 稳定测试。
- `AI_PROVIDER` 表示供应商或适配器标识，例如 `mock`、`openai`、`ollama`，不使用泛化的协议名表达具体供应商。
- 聊天模型环境变量统一使用 `AI_CHAT_MODEL`。
- OpenAI-compatible 是接入协议形态；具体实现应记录实际 `provider` 和 `model`。
- 真实模型调用必须在 mock/stub Provider、配置缺失和错误映射测试通过后再接入。
- 接入真实 Provider 前不引入 Spring AI；后续引入 Spring AI 时仍保持业务层依赖项目自己的 Provider 接口。

## 验证命令

首次配置 JDK 21 工具链：

该脚本需要本机已安装 `python3`，用于安全合并 `~/.m2/toolchains.xml`。

```bash
backend/scripts/setup-toolchains.sh
```

运行测试：

```bash
cd backend
./mvnw test
```

启动应用：

```bash
cd backend
./mvnw -pl apps/ai-chat-api spring-boot:run
```

健康检查：

```bash
curl http://localhost:8080/health
```

聊天请求：

```bash
curl -X POST http://localhost:8080/api/chat \
  -H 'Content-Type: application/json' \
  -d '{"message":"hello"}'
```

## 风险与注意事项

- Provider 选择放在配置和模型调用层，Controller 只处理 HTTP 交互。
- 日志记录 provider、model、状态、耗时和错误摘要，Authorization header、API Key 和完整环境变量保持脱敏。
- OpenAI-compatible 是协议约定，不直接作为默认 `AI_PROVIDER` 值；默认 `AI_PROVIDER=mock`，真实供应商使用具体标识。
- 真实模型接入前，应先用 mock/stub 覆盖错误分支。
- 真实模型调用会产生费用和数据外发风险，接入前需要显式确认模型服务、数据边界、超时、错误映射和日志脱敏。

## 后续入口

完成基础 Chat API 后，下一步进入：

- 流式输出。
- Prompt 版本加载。
- 结构化输出实验。

是否直接进入独立阶段，以路线图和当时的需求为准。
