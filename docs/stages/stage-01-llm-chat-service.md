# 阶段 1：LLM 聊天服务实现手册

> 本文档的工程权威版本位于 `java-ai-application-lab` 仓库。个人知识库或飞书中的副本仅用于阅读和导入。

本文对应学习路线中的阶段 1，目标是把“一次模型调用”升级为一个可配置、可测试、可观测的 Java 后端聊天服务。

阶段 1 先聚焦 Chat API 本身：普通聊天、流式输出、多轮上下文、模型 Provider 抽象、错误处理、日志与 token 统计。RAG、Tool Calling、Agent 等能力放到后续阶段。

## 1. 阶段目标

完成一个最小但工程边界清晰的聊天后端：

- 提供普通聊天接口。
- 提供流式聊天接口。
- 支持会话和消息历史。
- 支持模型参数配置。
- 支持不同模型 Provider 的接入。
- 记录请求耗时、模型、会话、token 和错误。
- 在日志和 Prompt 中遵守阶段 0 的数据边界。

阶段 1 的验收标准是：前端或 Postman 能稳定调用聊天接口；长对话不会无限膨胀；模型异常能返回清晰错误；日志能支撑排查问题。

## 2. 推荐技术组合

| 能力 | 推荐技术 |
|---|---|
| Web 框架 | Spring Boot 3.x |
| Java 版本 | Java 21 |
| AI 抽象 | Spring AI ChatClient |
| 备选 AI 抽象 | LangChain4j AI Services |
| 普通接口 | Spring MVC |
| 流式输出 | SSE，必要时再考虑 WebFlux |
| 存储 | PostgreSQL |
| 配置 | Spring Boot Configuration Properties |
| 测试 | JUnit 5、MockWebServer 或 WireMock |

Spring AI 当前提供了面向 Spring 生态的 `ChatClient` 抽象，适合学习阶段先接入普通调用和流式调用。文档中建议优先依赖稳定抽象，避免业务代码直接绑定具体模型供应商类。

## 3. 推荐模块结构

可以先按下面结构组织：

```text
ai-chat-api
├── controller
│   └── ChatController
├── application
│   └── ChatService
├── provider
│   ├── ChatModelProvider
│   ├── ChatRequest
│   ├── ChatResult
│   └── SpringAiChatModelProvider
├── conversation
│   ├── ConversationService
│   ├── Conversation
│   └── Message
├── config
│   ├── AiProviderProperties
│   └── ChatProperties
└── observability
    ├── ChatTraceLogger
    └── TokenUsageRecorder
```

关键原则：

- Controller 只处理 HTTP 入参、鉴权上下文和响应格式。
- ChatService 负责编排会话、Prompt、Provider、日志和异常。
- Provider 封装模型供应商差异。
- ConversationService 管理会话和历史消息。
- Observability 组件记录可排查的信息。

## 4. API 设计

### 4.1 普通聊天接口

```http
POST /api/chat
Content-Type: application/json
```

请求示例：

```json
{
  "conversationId": "conv_001",
  "message": "帮我解释一下 RAG 的基本流程",
  "model": "default",
  "temperature": 0.2,
  "maxOutputTokens": 800
}
```

响应示例：

```json
{
  "conversationId": "conv_001",
  "messageId": "msg_002",
  "answer": "RAG 的基本流程是先检索资料，再把资料作为上下文交给模型回答。",
  "model": "default",
  "usage": {
    "inputTokens": 320,
    "outputTokens": 86,
    "totalTokens": 406
  },
  "latencyMs": 1280
}
```

### 4.2 流式聊天接口

```http
POST /api/chat/stream
Accept: text/event-stream
```

SSE 事件建议：

| 事件 | 说明 |
|---|---|
| `message_start` | 服务端已创建消息 |
| `delta` | 模型输出片段 |
| `message_end` | 输出结束，携带停止原因、耗时，以及可获得的 usage |
| `error` | 模型调用或业务处理失败 |

事件示例：

```text
event: delta
data: {"messageId":"msg_002","content":"RAG 的基本流程"}

event: message_end
data: {"messageId":"msg_002","finishReason":"stop","latencyMs":1280,"usageAvailable":false}
```

学习阶段优先用 SSE。它和 Spring MVC 配合直接，但前端实现要区分两种方式：

| 方式 | 适用场景 | 前端实现 |
|---|---|---|
| `GET /api/chat/stream` | 参数少、只做简单演示 | 浏览器原生 `EventSource` |
| `POST /api/chat/stream` | 需要请求体传 message、conversationId、模型参数 | `fetch` + `ReadableStream` 手动解析，或使用支持 POST 的 SSE 客户端库 |

浏览器原生 `EventSource` 只支持 GET；需要发送 POST 请求体时，前端实现使用 fetch streaming。阶段 1 如果希望接口和普通聊天一样使用 JSON 请求体，就把前端实现写成 fetch streaming；如果只做最小演示，可以先提供 GET 版本。

流式 token usage 也要按 Provider 能力处理。很多 OpenAI-compatible 流式接口默认不返回 usage，需要显式开启供应商支持的 usage 选项；部分 Provider 或框架版本拿不到流式 usage。拿不到时，`message_end` 记录 `usageAvailable=false`，并保留耗时、模型、finishReason；成本统计可以先用非流式接口、接口返回 usage 或离线估算补齐。

Spring MVC 使用 SSE 时仍然可以在内部使用 Reactor `Flux`。`Flux` 是流式数据类型，不等于必须使用 WebFlux 全栈。Controller 可以直接返回 `Flux<ServerSentEvent<?>>` 或 `Flux<ChatStreamEvent>` 并设置 `produces = text/event-stream`；也可以用 `SseEmitter` 手动订阅 Provider 的 Flux，注意处理 `onError`、`onComplete` 和连接关闭。等到需要高并发长连接、复杂背压或全链路响应式时，再系统学习 WebFlux。

流式接口的错误处理分两段看：如果错误发生在响应头和第一个事件发送前，可以返回对应 HTTP 状态码；如果已经开始发送 SSE，HTTP 状态通常已经是 200，后续超时或模型错误只能通过 `error` 事件携带业务错误码、messageId 和 traceId，再由前端结束本次展示。

## 5. 会话与消息模型

建议先设计两张表。

### 5.1 conversation

| 字段 | 说明 |
|---|---|
| id | 会话 id |
| user_id | 用户 id 或本地学习用户标识 |
| title | 会话标题 |
| status | ACTIVE / DELETED |
| created_at | 创建时间 |
| updated_at | 更新时间 |

### 5.2 message

| 字段 | 说明 |
|---|---|
| id | 消息 id |
| conversation_id | 会话 id |
| role | system / user / assistant |
| content | 消息内容 |
| model | 模型名称 |
| prompt_version | Prompt 版本 |
| input_tokens | 输入 token |
| output_tokens | 输出 token |
| latency_ms | 耗时 |
| error_code | 错误码 |
| created_at | 创建时间 |

消息内容保存要结合阶段 0 数据边界。学习阶段可以保存普通样例和公开问题；涉及真实隐私或业务数据时，优先保存脱敏内容、摘要或引用 id。

## 6. 多轮上下文策略

多轮对话的核心问题是控制上下文增长。

阶段 1 先实现三层策略：

1. 固定 system prompt。
2. 保留最近 N 轮对话。
3. 限制最大输入 token 预算。

推荐默认值：

| 参数 | 建议 |
|---|---:|
| 最近轮数 | 6-10 轮 |
| 最大输入 token | 根据模型上下文窗口预留输出空间 |
| 最大输出 token | 按场景配置，例如 800 或 1200 |

后续可以增加摘要策略：

```text
早期历史 -> 摘要
近期历史 -> 原文
当前问题 -> 原文
```

摘要也属于模型生成内容，作为候选上下文使用；如果涉及业务判断，仍需保留原始引用或可追踪消息 id。

## 7. Provider 抽象

阶段 1 建议先定义自己的 Provider 接口，避免 Controller 和业务服务直接依赖具体模型供应商。

示例接口：

```java
public interface ChatModelProvider {
    ChatResult chat(ChatRequest request);

    Flux<ChatStreamEvent> stream(ChatRequest request);
}
```

`ChatRequest` 建议包含：

| 字段 | 说明 |
|---|---|
| systemPrompt | 系统提示词 |
| messages | 多轮消息 |
| model | 模型名称 |
| temperature | 随机性参数 |
| maxOutputTokens | 最大输出 token |
| timeout | 超时时间策略 |
| metadata | traceId、userRef、conversationId |

阶段 1 先做全局模型请求超时，例如配置底层 HTTP 客户端的 connect/read timeout。每次请求单独传入 timeout 在不同 Provider 和 Spring AI 版本中的支持不一致；如果确实需要 per-request timeout，阻塞调用可以在业务层使用受控线程池和 `Future.get(timeout)`，流式调用可以对 `Flux` 增加 `timeout` 处理，并统一映射为 `AI_REQUEST_TIMEOUT`。

`Future.get(timeout)` 只代表调用方停止等待，底层 HTTP 请求不一定会被真正中止。使用这种方式时，还要配合底层 HTTP 客户端的 connect/read timeout、受控线程池容量和连接池释放策略，避免模型请求继续占用线程和连接。`future.cancel(true)` 只能辅助中断可响应中断的任务，阻塞式 socket read 通常仍依赖底层 HTTP 客户端的 read timeout 回收线程和连接。

`ChatResult` 建议包含：

| 字段 | 说明 |
|---|---|
| content | 模型回答 |
| finishReason | 停止原因 |
| inputTokens | 输入 token |
| outputTokens | 输出 token |
| rawProvider | 模型供应商 |
| rawModel | 实际模型名称 |

Provider 内部可以使用 Spring AI `ChatClient`。业务层只依赖自己的接口，这样后续切换 OpenAI-compatible、Ollama、Claude、通义、智谱等模型时，影响范围更小。

## 8. 配置设计

推荐使用环境变量和配置类。

`.env` 示例只放本地：

```bash
AI_PROVIDER=openai-compatible
AI_BASE_URL=https://api.example.com/v1
AI_API_KEY=replace-with-local-secret
AI_CHAT_MODEL=example-chat-model
AI_REQUEST_TIMEOUT=30s
```

`application.yml` 示例：

```yaml
ai:
  provider: ${AI_PROVIDER:mock}
  base-url: ${AI_BASE_URL:}
  api-key: ${AI_API_KEY:}
  chat-model: ${AI_CHAT_MODEL:}
  request-timeout: ${AI_REQUEST_TIMEOUT:30s}
chat:
  max-history-rounds: 8
  max-input-tokens: 6000
  max-output-tokens: 1000
```

规则：

- API Key 只通过环境变量或密钥管理系统注入。
- `.env` 不提交版本库。
- 日志不打印完整配置。
- 启动时检查必要配置，缺失时给出明确错误。

## 9. 错误处理

阶段 1 至少定义这些错误码：

| 错误码 | HTTP 状态 | 场景 |
|---|---:|---|
| `AI_PROVIDER_NOT_CONFIGURED` | 503 | 模型配置缺失或当前环境未启用真实 Provider |
| `AI_REQUEST_TIMEOUT` | 504 | 模型调用超时 |
| `AI_RATE_LIMITED` | 429 | 模型服务限流 |
| `AI_EMPTY_RESPONSE` | 502 | 模型返回空内容 |
| `AI_PROVIDER_ERROR` | 502 | 模型服务返回错误 |
| `CHAT_CONTEXT_TOO_LONG` | 400 | 上下文超过预算 |
| `CHAT_MESSAGE_INVALID` | 400 | 用户输入为空或过长 |

HTTP 响应保持业务可读：

```json
{
  "code": "AI_REQUEST_TIMEOUT",
  "message": "模型服务响应超时，请稍后重试",
  "traceId": "trace_001"
}
```

日志中记录技术细节，响应中只返回用户可理解的信息和 traceId。

建议使用 `@ControllerAdvice` 和 `@ExceptionHandler` 统一把内部异常映射为业务错误码、HTTP 状态码和响应体，避免不同 Controller 返回不一致。

上表适用于普通接口，以及流式接口在响应尚未开始前发生的错误。SSE 已经开始输出后，HTTP 状态码通常已经固定；此时通过 `error` 事件返回同样的业务错误码和 traceId，例如：

```text
event: error
data: {"messageId":"msg_002","code":"AI_REQUEST_TIMEOUT","message":"模型服务响应超时","traceId":"trace_001"}
```

## 10. 日志与可观测性

建议每次模型调用记录：

- traceId。
- conversationId。
- messageId。
- provider。
- model。
- promptVersion。
- inputTokens。
- outputTokens。
- latencyMs。
- finishReason。
- errorCode。

日志中避免记录：

- API Key。
- 完整 Prompt。
- 完整用户隐私。
- 完整模型响应。
- 完整工具返回结果。

阶段 1 先用应用日志即可；阶段 6 再接 OpenTelemetry、Prometheus、Grafana 和完整评测面板。

## 11. 测试策略

阶段 1 测试重点是后端工程逻辑，模型回答质量放到后续评测体系中衡量。

| 测试类型 | 验证内容 |
|---|---|
| Controller 测试 | 请求参数、响应结构、错误码 |
| ChatService 测试 | 会话编排、历史截断、异常映射 |
| Provider Mock 测试 | 模型正常返回、空响应、超时、限流 |
| SSE 测试 | 事件顺序、错误事件、结束事件 |
| 配置测试 | 缺少 API Key、缺少模型名、mock provider |

建议先提供一个 `mock` provider：

- 固定返回一段文本。
- 支持模拟延迟。
- 支持模拟异常。
- 支持模拟流式分片。

这样在没有真实 API Key 或 CI 环境中，也能验证业务流程。

## 12. 验收清单

阶段 1 完成时，检查下面这些项：

| 检查项 | 状态 |
|---|---|
| `/api/chat` 可调用 |  |
| `/api/chat/stream` 可流式输出 |  |
| 会话和消息能保存 |  |
| 历史消息有截断策略 |  |
| Provider 抽象已隔离供应商差异 |  |
| API Key 未写入代码和日志 |  |
| 模型异常能映射为业务错误码 |  |
| 日志能查到 traceId、模型、耗时和 token |  |
| mock provider 可用于测试 |  |
| 至少有 Controller、Service、Provider 相关测试 |  |

## 13. 后续演进

阶段 1 做完后，后续阶段这样接上：

- 阶段 2：在 ChatService 上增加结构化输出和 Prompt 版本管理。
- 阶段 3：在请求前加入 RAG 检索上下文。
- 阶段 4：让模型通过 Tool Calling 调用后端业务接口。
- 阶段 5：把单次聊天升级为有状态、多步骤的 Agent。
- 阶段 6：补齐评测、安全和可观测性。

阶段 1 的核心产物是一条稳定的模型调用链路。只要这条链路的配置、错误、日志、测试和数据边界清楚，后面的 RAG、Tool Calling 和 Agent 才有可靠基础。
