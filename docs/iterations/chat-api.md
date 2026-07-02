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
- 当前 `openai` Provider 骨架已落地配置校验：`AI_BASE_URL` 和 `AI_API_KEY` 缺失或空白时返回统一 `AI_PROVIDER_ERROR`；HTTP 请求构造和发送在后续任务实现。
- 接入真实 Provider 前不引入 Spring AI；后续引入 Spring AI 时仍保持业务层依赖项目自己的 Provider 接口。

## OpenAI-compatible Provider 接入契约

本节用于进入真实 Provider TDD 实现前固定配置、数据边界、错误映射和测试策略。当前仍保持 `mock` 为默认 Provider，不发起外部模型调用。

### Provider 标识和配置

首个真实 Provider 使用 `AI_PROVIDER=openai`，Provider 实现的 `providerName()` 返回 `openai`。`openai` 表示 OpenAI 官方 Provider 使用 Chat Completions 兼容协议访问模型服务；后续接入其他兼容服务时使用对应具体 Provider 标识，例如 `ollama`，不使用 `openai-compatible` 作为泛化 Provider 名称。

| 配置 | 环境变量 | Spring 配置键 | 规则 |
|---|---|---|---|
| Provider 标识 | `AI_PROVIDER` | `ai.provider` | 默认 `mock`；真实 OpenAI Provider 使用 `openai` |
| 模型名称 | `AI_CHAT_MODEL` | `ai.chat-model` | 必填；缺失时继续返回 `AI_MODEL_NOT_CONFIGURED` |
| Base URL | `AI_BASE_URL` | `ai.base-url` | `AI_PROVIDER=openai` 时必填；OpenAI 官方服务示例为 `https://api.openai.com/v1` |
| API Key | `AI_API_KEY` | `ai.api-key` | `AI_PROVIDER=openai` 时必填；只通过环境变量或本地密钥管理提供 |
| 请求超时 | `AI_REQUEST_TIMEOUT` | `ai.request-timeout` | 默认 `30s`；必须为正数，进入 Provider 请求契约 |

`AI_BASE_URL` 统一表示包含版本前缀的根地址，Provider 请求路径拼接为 `{AI_BASE_URL}/chat/completions`。实现时对尾部 `/` 做规范化，避免生成双斜杠路径。`AI_API_KEY` 不设置默认值，`.env.example` 只保留占位文本。

`AI_REQUEST_TIMEOUT` 表示单次 Provider HTTP 调用的最大等待时间。实现时用于 HTTP client 的连接、写入、读取或整体响应超时能力；任一超时异常以及外部 408/504 都统一映射为 `AI_REQUEST_TIMEOUT`。第一版不拆分 connect timeout 和 read timeout，后续确需拆分时先更新配置契约和测试。

### 请求和响应最小兼容面

第一版真实 Provider 只实现非流式文本聊天：

- 请求方法：`POST {AI_BASE_URL}/chat/completions`。
- 请求头：`Content-Type: application/json`，`Authorization: Bearer <AI_API_KEY>`。
- 请求体：至少包含 `model` 和 `messages`。
- `messages` 第一版只发送当前规范化后的用户消息：`[{ "role": "user", "content": "<message>" }]`。
- 成功响应读取 `choices[0].message.content` 作为 `ProviderChatResponse.content`。
- 响应 `choices` 为空、缺少 `message.content` 或内容为空白时，视为空响应。

Prompt 文件仍在 `prompts/chat/system-v1.md` 管理。当前真实 Provider 第一版不额外实现 Prompt 加载；进入 Prompt 版本加载任务时再把系统提示词纳入请求构造。

### 数据边界和日志脱敏

真实 Provider 调用会把用户消息发送到外部模型服务，默认只允许使用合成数据、公开资料或用户明确授权的数据。API Key、Authorization header、完整环境变量、完整 Prompt、完整用户输入、完整模型输出和外部错误响应 body 不进入普通日志。

普通模型调用日志继续只记录工程基线字段：`traceId`、`feature`、`provider`、`model`、`promptVersion`、`latencyMs`、`status`、`errorCode`。如果后续需要记录外部 `x-request-id` 等排查字段，先同步更新 `docs/reference/engineering-baseline.md`，再落地代码和测试。

### 错误映射策略

| 场景 | 对外错误码 | HTTP 状态 | 处理规则 |
|---|---|---:|---|
| 未找到 Provider Bean | `AI_PROVIDER_NOT_FOUND` | 503 | 保持现有逻辑 |
| `AI_CHAT_MODEL` 为空 | `AI_MODEL_NOT_CONFIGURED` | 503 | 保持现有逻辑 |
| `openai` Provider 缺少 `AI_BASE_URL` 或 `AI_API_KEY` | `AI_PROVIDER_ERROR` | 502 | 返回统一错误结构，日志记录配置项名称和 `errorCode`，不记录配置值 |
| 本地请求超时、连接超时、外部 408/504 | `AI_REQUEST_TIMEOUT` | 504 | 映射为 `ChatProviderErrorType.REQUEST_TIMEOUT` |
| 外部 429 | `AI_RATE_LIMITED` | 429 | 映射为 `ChatProviderErrorType.RATE_LIMITED` |
| 外部 2xx 但内容为空或响应结构缺少可用文本 | `AI_EMPTY_RESPONSE` | 502 | 映射为 `ChatProviderErrorType.EMPTY_RESPONSE` |
| 外部 400/401/403/404、除 504 外的 5xx、非 JSON 响应、JSON 解析失败、其他 IO 异常 | `AI_PROVIDER_ERROR` | 502 | 保留 cause 链，普通日志不记录外部响应 body |

后续如果需要把配置缺失拆成独立错误码，先补充工程基线、API 错误契约和测试，再修改现有映射。

### 测试策略

真实 Provider 实现继续使用 TDD，先写失败测试，再写最小实现：

- 配置测试：覆盖 `AI_PROVIDER=openai` 下 `AI_BASE_URL`、`AI_API_KEY`、`AI_CHAT_MODEL`、`AI_REQUEST_TIMEOUT` 的缺失、空白、默认值和非法值。
- Provider 单元测试：使用本地 stub HTTP 服务或可控 HTTP 客户端替身，不访问外部网络。
- 成功路径测试：断言请求路径、请求头脱敏边界、`model/messages` 请求体，以及 `choices[0].message.content` 到 `ProviderChatResponse.content` 的映射。
- 错误路径测试：覆盖超时、429、空响应、非 JSON、外部 4xx、除 504 外的 5xx、API Key 缺失和 Base URL 缺失；408/429/504 按上表专门分类断言。
- Controller 集成测试：覆盖 `AI_PROVIDER=openai` 时成功响应和统一错误响应结构，继续断言 `code`、`message`、`traceId`。
- 日志相关测试或人工检查：确认普通日志不包含 API Key、Authorization header、完整用户输入和外部响应 body。

当前已完成配置契约第一片：

- `AI_BASE_URL` / `AI_API_KEY` 已进入 `ChatProviderProperties` 和 `application.yml` 绑定。
- `AI_PROVIDER=openai` 下 Base URL / API Key 缺失或空白的 Provider 单元测试、Service 映射测试和 Controller 统一错误响应测试已覆盖。
- 日志捕获测试已覆盖缺配置分支，确认普通日志不包含测试 API Key、Authorization header、Base URL 和完整用户输入。

下一片实现从本地 stub HTTP 服务或可控 HTTP 客户端替身开始，覆盖非流式成功路径，请求路径、请求头、请求体和响应解析。

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
- 当前 openai Provider 已覆盖缺配置错误分支；真实 HTTP 调用接入前继续使用 mock/stub 覆盖外部错误分支。
- 真实模型调用会产生费用和数据外发风险，接入前需要显式确认模型服务、数据边界、超时、错误映射和日志脱敏。

## 后续入口

完成基础 Chat API 后，下一步进入：

- OpenAI-compatible Provider 非流式 HTTP 调用成功路径。
- OpenAI-compatible Provider 外部错误映射。
- 流式输出。
- Prompt 版本加载。
- 结构化输出实验。

是否直接进入独立阶段，以路线图和当时的需求为准。
