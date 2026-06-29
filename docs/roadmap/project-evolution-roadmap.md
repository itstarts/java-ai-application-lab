# AI 应用开发实战项目路线

> 本文档的工程权威版本位于 `java-ai-application-lab` 仓库。个人知识库或飞书中的副本仅用于阅读和导入。

本文对应主路线图中的“推荐项目路线”，目标是把阶段 0 到阶段 8 串成一组可以连续演进的 Java 后端实战项目。

这份文档先固定项目主线和阶段演进关系。当前已经生效的共享工程契约以 [`engineering-baseline.md`](../reference/engineering-baseline.md) 为准；本文中涉及阶段 4 之后的工具调用、审计、评测和治理字段，进入对应阶段时再提升为代码硬约束。

## 1. 项目主线

推荐把所有练习项目做成同一个仓库中的连续演进，让每个阶段复用前一阶段的代码、数据和工程约定。

主线应用可以叫：

```text
java-ai-application-lab
```

核心业务场景：

```text
从 AI Chat API
-> 简历信息抽取器
-> 企业知识库问答
-> 智能客服助手
-> 业务办理助手
-> AI 应用管理后台
-> 本地模型实验室
```

每个项目都保留前一阶段的能力，并新增一组可验证功能。这样做的好处是：聊天、结构化输出、RAG、Tool Calling、Agent、评测和治理都能共享同一套模型调用链路、数据边界和观测字段。

## 2. 推荐仓库结构

沿用当前仓库的 `backend/` 主体结构即可。学习阶段不需要把每个项目拆成独立仓库。

```text
java-ai-application-lab
├── backend
│   ├── apps
│   │   ├── ai-chat-api
│   │   ├── resume-extractor
│   │   ├── knowledge-base
│   │   ├── customer-service-assistant
│   │   ├── workflow-agent
│   │   ├── admin-api
│   │   └── local-model-lab
│   ├── common
│   │   ├── ai-core
│   │   ├── ai-observability
│   │   ├── ai-security
│   │   └── test-support
│   └── pom.xml
├── docs
│   ├── roadmap
│   ├── reference
│   ├── stages
│   ├── iterations
│   └── notes
└── README.md
```

如果当前仓库暂时还没有这些模块，可以按项目推进逐步增加。模块跟随实际练习项目出现，空模块暂不创建。

## 3. 共享工程约定草案

本节描述阶段 4 之后会逐步采用的统一约定。当前阶段的生效契约只包含 `engineering-baseline.md` 中列出的数据边界、错误结构、基础 trace 字段、日志脱敏和 Provider 记录字段。

### 3.1 请求与 Trace

所有 AI 能力都应带上同一组请求追踪字段。

| 字段 | 说明 |
|---|---|
| `traceId` | 一次用户请求的全链路 id |
| `conversationId` | 聊天或任务会话 id，可为空 |
| `messageId` | 当前用户消息或模型消息 id |
| `userRef` | 用户引用标识，日志中不使用真实姓名、手机号、邮箱 |
| `tenantRef` | 租户或团队引用标识，学习阶段可用固定值 |
| `model` | 实际使用的模型标识 |
| `promptVersion` | Prompt 版本 |
| `feature` | `chat`、`extract`、`rag`、`tool`、`agent`、`eval`、`safety` 等 |

日志和评测中优先记录引用 id、hash、状态、耗时和错误码。完整 Prompt、完整工具返回、RAG 原文片段和真实用户隐私只进入受控调试存储。

模型调用也要从项目 1 开始保留 Provider 抽象。业务服务依赖统一的 `ChatModelProvider`、`EmbeddingProvider` 或应用层接口，不直接绑定某个模型供应商 SDK。项目 7 接入本地模型时，只扩展 Provider 实现，不回头改业务链路。

### 3.2 统一错误响应

普通 HTTP 接口使用统一错误结构：

```json
{
  "code": "AI_REQUEST_TIMEOUT",
  "message": "模型服务响应超时",
  "traceId": "trace_001"
}
```

字段命名约定：

| 场景 | 字段 | 说明 |
|---|---|---|
| HTTP 响应和 SSE `error` 事件 | `code` | 对前端和调用方暴露的错误码 |
| 日志、数据库、trace、工具结果、Agent step | `errorCode` | 内部排查和关联用错误码 |

两者值域相同，只是面向对象不同。后续文档写接口响应时使用 `code`，写内部记录时使用 `errorCode`。

推荐错误码分组：

| 分组 | 示例 | 说明 |
|---|---|---|
| `AI_*` | `AI_REQUEST_TIMEOUT` | 模型调用、限流、空响应 |
| `STRUCTURED_*` | `STRUCTURED_OUTPUT_INVALID` | JSON、DTO、字段校验 |
| `RAG_*` | `RAG_NO_EVIDENCE` | 检索、引用、拒答 |
| `TOOL_*` | `TOOL_ARGUMENT_INVALID` | 工具选择、参数、执行 |
| `AGENT_*` | `AGENT_STEP_FAILED` | Agent 状态、步骤、确认 |
| `SECURITY_*` | `SECURITY_ACCESS_DENIED` | 权限、数据边界、安全策略 |
| `EVAL_*` | `EVAL_CASE_FAILED` | 评测样例、指标计算 |

流式接口在响应开始前可以返回 HTTP 错误；响应已经开始后，通过 SSE `error` 事件返回同样的 `code`、`message`、`traceId` 和必要的 `messageId`。

`feature`、错误码分组和评测能力的对应关系：

| feature | 主要错误码分组 | 评测能力 |
|---|---|---|
| `chat` | `AI_*` | Chat |
| `extract` | `STRUCTURED_*` | Structured Output |
| `rag` | `RAG_*`、`AI_*` | RAG |
| `tool` | `TOOL_*`、`SECURITY_*` | Tool Calling |
| `agent` | `AGENT_*`、`TOOL_*`、`SECURITY_*` | Agent |
| `eval` | `EVAL_*` | Evaluation |
| `safety` | `SECURITY_*`、`EVAL_*` | Safety |

### 3.3 Tool Schema

工具调用统一按“工具定义、参数 DTO、执行结果、审计事件”四部分设计。

工具定义建议包含：

| 字段 | 说明 |
|---|---|
| `toolName` | 稳定英文标识，例如 `order.query_status` |
| `description` | 给模型看的简短能力说明 |
| `riskLevel` | `READ`、`WRITE_LOW`、`WRITE_HIGH` |
| `requiresConfirmation` | 是否需要用户二次确认 |
| `inputSchema` | 入参 DTO 或 JSON Schema |
| `outputSchema` | 给模型可见的返回结构 |
| `timeoutMs` | 工具执行超时 |
| `enabled` | 是否启用 |

风险等级执行策略：

| riskLevel | 执行策略 |
|---|---|
| `READ` | 可自动执行，仍需后端鉴权和参数校验 |
| `WRITE_LOW` | 低风险写操作，要求幂等键和操作日志；是否二次确认由业务配置决定 |
| `WRITE_HIGH` | 必须二次确认、幂等、审计，并复用原业务校验 |

工具参数规则：

- 用户身份、租户、权限范围从后端上下文获取。
- 模型只生成业务参数候选值。
- DTO 使用 Jakarta Validation 校验。
- 写操作必须有幂等键和二次确认记录。
- 工具返回给模型的内容只保留回答所需字段。

### 3.4 工具调用结果

工具执行结果统一表达为：

```json
{
  "toolCallId": "tool_call_001",
  "toolName": "order.query_status",
  "status": "SUCCEEDED",
  "idempotencyKey": null,
  "confirmationId": null,
  "safeResult": {
    "orderStatus": "SHIPPED",
    "deliveryDate": "2026-07-02"
  },
  "errorCode": null,
  "startedAt": "2026-06-29T10:00:00Z",
  "finishedAt": "2026-06-29T10:00:01Z",
  "traceId": "trace_001"
}
```

`safeResult` 是给模型继续生成答案使用的安全结果，不等于原始业务对象。原始对象留在业务系统内部，必要时只在受控审计系统中记录引用 id。

工具结果状态：

| status | 说明 |
|---|---|
| `SUCCEEDED` | 执行成功 |
| `FAILED` | 业务失败或系统异常 |
| `TIMEOUT` | 工具执行超时 |
| `DENIED` | 权限或安全策略拒绝 |
| `WAITING_CONFIRMATION` | 需要用户确认后再执行 |

### 3.5 Agent 状态与步骤

Agent 任务统一拆成 `task` 和 `step`。

任务状态：

| 状态 | 说明 |
|---|---|
| `CREATED` | 已创建 |
| `PLANNING` | 正在分析和生成计划 |
| `WAITING_CONFIRMATION` | 等待用户确认 |
| `EXECUTING` | 正在执行工具或步骤 |
| `FAILED` | 任务失败 |
| `CANCELLED` | 用户取消 |
| `COMPLETED` | 完成 |

步骤状态：

| 状态 | 说明 |
|---|---|
| `PENDING` | 等待执行 |
| `RUNNING` | 执行中 |
| `WAITING_CONFIRMATION` | 等待确认 |
| `SUCCEEDED` | 成功 |
| `FAILED` | 失败 |
| `SKIPPED` | 被跳过 |
| `CANCELLED` | 运行中步骤因所属任务取消而停止推进 |

每个 step 至少记录：`stepId`、`taskId`、`stepType`、`status`、`inputRef`、`outputRef`、`toolName`、`toolCallId`、`latencyMs`、`errorCode`、`startedAt`、`finishedAt`、`traceId`。

`toolCallId` 可为空；当 step 调用工具时必须填充，用于关联工具结果和审计事件。

### 3.6 审计事件

高风险操作和工具调用记录审计事件。

| 字段 | 说明 |
|---|---|
| `auditId` | 审计事件 id |
| `traceId` | 关联请求 |
| `toolCallId` | 关联工具调用，可为空 |
| `toolName` | 工具名称，可为空 |
| `idempotencyKey` | 写操作幂等键，可为空 |
| `confirmationId` | 用户确认记录 id，可为空 |
| `actorRef` | 操作人引用 |
| `action` | 操作类型 |
| `resourceType` | 资源类型 |
| `resourceRef` | 资源引用 |
| `riskLevel` | 风险等级 |
| `decision` | `AUTO_ALLOWED`、`CONFIRMATION_REQUESTED`、`USER_CONFIRMED`、`USER_REJECTED`、`CANCELLED`、`DENIED` |
| `result` | `SUCCEEDED`、`FAILED`、`NOT_EXECUTED` |
| `createdAt` | 时间 |

审计日志不保存完整 Prompt、完整工具返回和用户隐私原文。

当 `decision=DENIED` 时，`result` 使用 `NOT_EXECUTED`，表示操作被策略或权限拦截，没有进入业务执行。

工具 `TIMEOUT` 归入审计 `result=FAILED`。工具进入 `WAITING_CONFIRMATION` 时先记录确认请求，使用 `decision=CONFIRMATION_REQUESTED`、`result=NOT_EXECUTED`；用户确认后再以 `decision=USER_CONFIRMED` 记录实际执行审计事件；用户明确拒绝时使用 `decision=USER_REJECTED`、`result=NOT_EXECUTED`；任务取消或用户撤销待确认操作时使用 `decision=CANCELLED`、`result=NOT_EXECUTED`。

### 3.7 评测样例格式

评测集统一使用 JSONL，便于版本管理和增量追加。

```json
{"caseId":"rag_001","feature":"rag","input":{"question":"报销需要几天内提交？"},"expected":{"documentIds":["doc_policy_001"],"chunkIds":["chunk_policy_003"],"answerPoints":["7 个工作日内提交"],"shouldRefuse":false},"tags":["policy","single-doc"]}
```

不同能力的 `expected` 字段不同：

| feature | 能力 | expected 重点 |
|---|---|---|
| `chat` | Chat | `answerPoints`、`shouldRefuse` |
| `extract` | Structured Output | `fields`、`enums`、`validationStatus` |
| `rag` | RAG | `documentIds`、`chunkIds`、`answerPoints`、`shouldRefuse` |
| `tool` | Tool Calling | `toolName`、`arguments`、`requiresConfirmation` |
| `agent` | Agent | `steps`、`confirmationPoints`、`finalOutputPoints` |
| `eval` | Evaluation | `targetFeature`、`metricName`、`threshold`、`expectedStatus` |
| `safety` | Safety | `blocked`、`redactedFields`、`reason` |

评测集遵守阶段 0 数据边界。可提交到仓库的样例优先使用模拟数据。

## 4. 项目 1：AI Chat API

目标：掌握模型调用、流式输出、多轮对话、异常处理和基础日志。

输入输出：

| 能力 | 接口 |
|---|---|
| 普通聊天 | `POST /api/chat` |
| 流式聊天 | `POST /api/chat/stream` |
| 会话列表 | `GET /api/conversations` |
| 消息历史 | `GET /api/conversations/{conversationId}/messages` |

最小数据表：

- `conversation`
- `message`
- `ai_request_log`

验收：

- 普通聊天和流式聊天可用。
- 业务服务通过统一 Provider 接口调用模型。
- 长对话有上下文截断策略。
- 模型错误能映射为统一错误码。
- 日志能查到 `traceId`、模型、耗时和 token。

## 5. 项目 2：简历信息抽取器

目标：掌握 Prompt 分层、结构化输出、DTO 校验和样例回归。

核心流程：

```text
输入简历文本
-> Prompt + Schema
-> 模型输出 JSON
-> Jackson 反序列化
-> Jakarta Validation
-> 保存抽取结果和原始输出引用
-> 评测集回归
```

最小数据表：

- `extract_job`
- `extract_result`
- `prompt_version`
- `eval_case`

验收：

- 能稳定输出合法 JSON。
- 枚举、数字、数组字段可校验。
- 失败有明确错误码和重试上限。
- 至少 20 个模拟样例可回归。

## 6. 项目 3：企业知识库问答

目标：掌握文档入库、embedding、pgvector、RAG、引用和拒答。

核心流程：

```text
上传文档
-> 解析
-> 切分
-> embedding
-> 写入 pgvector
-> 问题检索
-> 可选 rerank
-> Prompt 拼装
-> 答案和引用
```

最小数据表：

- `knowledge_base`
- `document`
- `document_chunk`
- `ingestion_job`
- `rag_trace`

验收：

- 支持至少 3 种文档格式。
- 检索阶段带权限过滤。
- 回答返回引用来源。
- 无依据时拒答。
- 至少 50 个 RAG 问题可评测。

## 7. 项目 4：智能客服助手

目标：掌握 RAG + Tool Calling 的业务接口编排。

核心工具：

| 工具 | 风险等级 | 执行策略 |
|---|---|---|
| `order.query_status` | `READ` | 自动执行 |
| `refund.query_status` | `READ` | 自动执行 |
| `inventory.query_stock` | `READ` | 自动执行 |
| `notification.update_preference` | `WRITE_LOW` | 幂等 + 操作日志，可按配置确认 |
| `order.cancel` | `WRITE_HIGH` | 二次确认 + 幂等 + 审计 |
| `address.update` | `WRITE_HIGH` | 身份校验 + 二次确认 + 幂等 + 审计 |

验收：

- 用户问订单时能选择正确工具。
- 工具参数来自用户输入和后端上下文，不由模型生成权限字段。
- 查询类工具只返回必要字段。
- 写操作必须先确认。
- 每次工具调用有审计事件。

## 8. 项目 5：业务办理助手

目标：掌握 Agent 状态管理、步骤编排、人工确认和失败重试。

推荐任务：需求分析 Agent。

步骤示例：

| stepType | 说明 |
|---|---|
| `ANALYZE_REQUIREMENT` | 提取业务目标 |
| `DESIGN_API` | 生成接口草案 |
| `DESIGN_SCHEMA` | 生成表结构草案 |
| `IDENTIFY_RISK` | 识别风险点 |
| `GENERATE_TESTS` | 生成测试用例 |
| `EXPORT_PLAN` | 输出最终方案 |

人工确认通过步骤进入 `WAITING_CONFIRMATION` 状态表达，不作为独立 `stepType`。

验收：

- 页面或接口能查看任务状态。
- 每一步输入输出有引用。
- 失败步骤可单独重试。
- 高风险步骤等待用户确认。
- Agent trace 能串联模型调用和工具调用。

## 9. 项目 6：AI 应用管理后台

目标：掌握评测、安全、可观测性、成本和治理。

后台功能：

| 模块 | 内容 |
|---|---|
| 调用记录 | 模型、耗时、token、错误码 |
| Prompt 管理 | 版本、适用功能、变更说明 |
| RAG 观测 | 召回 chunk、similarity/distance、引用 |
| 工具审计 | 工具名、参数摘要、确认记录、结果 |
| Agent 追踪 | task、step、状态、错误 |
| 评测面板 | 样例集、指标、回归结果 |
| 成本统计 | 模型费用、embedding、rerank、token |
| 告警 | 错误率、延迟、成本异常、安全事件 |

验收：

- 能按 `traceId` 查询一次完整调用链。
- 能看到模型、RAG、工具、Agent 的核心指标。
- 能对 Prompt 或模型变更跑回归评测。
- 能发现成本和错误率异常。

## 10. 项目 7：本地模型实验室

目标：理解本地模型、API 模型、embedding 和 rerank 的效果与成本差异。

建议只做实验室，不作为前面项目的前置条件。

实验内容：

| 实验 | 目标 |
|---|---|
| Ollama Chat | 跑通本地聊天模型 |
| Ollama Embedding | 比较本地 embedding 效果 |
| vLLM 服务 | 了解 OpenAI-compatible 服务形态 |
| llama.cpp | 了解轻量本地推理和量化模型 |
| 模型对比 | 比较延迟、成本、输出质量 |

验收：

- 能通过统一 Provider 接口切换 API 模型和本地模型。
- 能记录延迟、token、显存或内存占用。
- 能说明本地模型适用场景和限制。

## 11. 推荐推进顺序

| 顺序 | 项目 | 主要产物 |
|---|---|---|
| 1 | AI Chat API | 稳定模型调用链路 |
| 2 | 简历信息抽取器 | Prompt + DTO + 校验 + 回归 |
| 3 | 企业知识库问答 | RAG 入库、检索、引用 |
| 4 | 智能客服助手 | Tool Calling、权限、审计 |
| 5 | 业务办理助手 | Agent 状态机、人工确认 |
| 6 | AI 应用管理后台 | 评测、观测、安全、成本 |
| 7 | 本地模型实验室 | 模型选型和本地推理实验 |

每完成一个项目，都要补齐：

- README 使用说明。
- API 示例。
- 数据表说明。
- 最小测试。
- 评测样例。
- 已知限制。

## 12. 与阶段文档的关系

| 阶段 | 对应项目 |
|---|---|
| 阶段 1 | 项目 1：AI Chat API |
| 阶段 2 | 项目 2：简历信息抽取器 |
| 阶段 3 | 项目 3：企业知识库问答 |
| 阶段 4 | 项目 4：智能客服助手 |
| 阶段 5 | 项目 5：业务办理助手 |
| 阶段 6 | 项目 6：AI 应用管理后台 |
| 阶段 7 | 项目 6 的生产化治理 |
| 阶段 8 | 项目 7：本地模型实验室 |

后续阶段文档写到接口、状态、错误、审计、评测时，优先引用本文的共享约定。确需新增字段时，先说明新增原因和影响范围，再同步更新本文。
