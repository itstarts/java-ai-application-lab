# 阶段 7：生产化与成本治理

> 本文档的工程权威版本位于 `java-ai-application-lab` 仓库。个人知识库或飞书中的副本仅用于阅读和导入。

本文对应学习路线中的阶段 7，目标是让前面已经完成的 Chat、结构化输出、RAG、Tool Calling、Agent 和评测体系具备上线运行能力。

阶段 7 的重点是治理：稳定性、成本、发布、限流、缓存、灰度、回滚和告警。业务能力仍沿用阶段 1 到阶段 6 的实现，不新增一套模型调用链路。

本文沿用 [`project-evolution-roadmap.md`](../roadmap/project-evolution-roadmap.md) 中的阶段演进约定。当前阶段之前已经生效的工程基线见 [`engineering-baseline.md`](../reference/engineering-baseline.md)。

本文中的数据库表字段使用 `snake_case`；API、日志和 trace 中沿用主路线图的 `camelCase` 字段名。

## 1. 阶段目标

完成一个 AI 应用治理后台：

- 统一管理模型路由和模型参数。
- 管理 Prompt 版本、发布记录和回滚点。
- 记录 token、费用、延迟和错误。
- 支持按用户、租户、功能和模型限流。
- 支持缓存策略和缓存风险控制。
- 支持异步任务、进度查询和失败重试。
- 支持灰度发布、回滚和告警。

阶段 7 的验收标准是：一次模型、Prompt、RAG、工具或 Agent 变更上线前有评测记录，上线后能按 `traceId`、`promptVersion`、`model` 和 `feature` 查到成本、耗时、错误和用户反馈；异常时能切回上一版配置。

## 2. 推荐技术组合

| 能力 | 推荐技术 |
|---|---|
| Web 框架 | Spring Boot 3.x |
| AI 抽象 | Spring AI `ChatModel`、`StreamingChatModel`、应用层 Provider |
| 配置管理 | 数据库配置表，后续可接 Nacos、Apollo 或 Spring Cloud Config |
| 指标 | Micrometer、Prometheus |
| Trace | OpenTelemetry |
| 看板和告警 | Grafana、Alertmanager 或云厂商监控 |
| 限流 | Bucket4j、Resilience4j、Redis 计数器 |
| 缓存 | Redis、Caffeine |
| 异步任务 | Spring Task、Quartz、消息队列 |
| 数据存储 | PostgreSQL |

Spring AI 提供统一的模型抽象和观测接入。生产治理仍建议在应用层保留 `ModelRouter`、`PromptRegistry`、`CostRecorder` 和 `AiTraceRecorder`，让业务代码保持在统一适配层。

## 3. 推荐模块结构

```text
ai-governance
├── controller
│   ├── ModelRouteController
│   ├── PromptReleaseController
│   ├── CostReportController
│   └── OpsRunbookController
├── routing
│   ├── ModelRoute
│   ├── ModelRouter
│   ├── ModelPolicy
│   └── RoutingDecisionRecorder
├── prompt
│   ├── PromptVersion
│   ├── PromptRelease
│   └── PromptRollbackService
├── cost
│   ├── CostEvent
│   ├── CostRecorder
│   └── CostReportService
├── quota
│   ├── RateLimitPolicy
│   ├── QuotaUsage
│   └── QuotaGuard
├── cache
│   ├── AiCachePolicy
│   ├── SemanticCacheService
│   └── RagCacheService
└── async
    ├── AiJob
    ├── AiJobRunner
    └── AiJobRetryPolicy
```

学习阶段可以把治理模块做在 `admin-api` 中。等项目复杂后，再拆成独立服务。

## 4. 模型路由

模型路由负责把一次 AI 请求映射到具体供应商、模型和参数。

### 4.1 路由输入

路由决策至少读取：

| 字段 | 说明 |
|---|---|
| `feature` | `chat`、`extract`、`rag`、`tool`、`agent`、`eval`、`safety` |
| `taskType` | 分类、抽取、问答、工具选择、Agent step 等 |
| `dataLevel` | 阶段 0 定义的数据等级 |
| `riskLevel` | 工具或任务风险等级 |
| `userTier` | 用户套餐或内部等级 |
| `latencyClass` | 实时、准实时、离线 |
| `budgetClass` | 成本优先、质量优先、平衡 |

### 4.2 路由表

```text
model_route
├── id
├── route_key
├── feature
├── task_type
├── data_level
├── provider
├── model
├── temperature
├── max_tokens
├── timeout_ms
├── retry_policy
├── cost_policy
├── enabled
├── release_status
├── traffic_percent
├── prompt_version
├── eval_run_id
├── created_at
└── updated_at
```

`eval_run_id` 关联阶段 6 的评测运行记录。模型路由上线前，应能看到对应评测结果和人工备注。

### 4.3 路由流程

```text
接收请求
-> 识别 feature / taskType / dataLevel / riskLevel
-> 读取启用中的路由配置
-> 检查限流和配额
-> 选择模型和参数
-> 记录 routingDecision
-> 调用模型
-> 记录 token、成本、延迟和错误
```

路由失败返回明确错误码，例如 `AI_ROUTE_NOT_FOUND`、`AI_ROUTE_DISABLED`、`AI_QUOTA_EXCEEDED`。如果配置了备用路由，响应和日志必须记录实际使用的 `provider`、`model` 和 `routingDecisionId`。

备用路由必须通过同样的 `dataLevel`、`riskLevel`、权限和阶段 6 评测门禁。缺少可用的合规备用路由时，返回明确错误；切换到备用模型必须经过审批。

## 5. Prompt 版本和发布

Prompt 需要像代码一样有版本、有发布、有回滚点。

### 5.1 Prompt 版本表

| 字段 | 说明 |
|---|---|
| `prompt_version` | 稳定版本号，例如 `rag-answer-v3` |
| `feature` | 所属能力 |
| `content_ref` | Prompt 内容引用 |
| `schema_ref` | 结构化输出 schema，可为空 |
| `change_reason` | 修改原因 |
| `eval_run_id` | 上线前评测记录 |
| `status` | `DRAFT`、`RELEASED`、`ARCHIVED` |
| `created_by` | 创建人引用 |
| `created_at` | 创建时间 |

Prompt 内容可以保存在数据库或版本库中。生产环境使用不可变版本，线上请求只引用 `promptVersion`，不直接引用可编辑草稿。

### 5.2 发布流程

```text
创建 Prompt 草稿
-> 本地样例验证
-> 跑阶段 6 回归评测
-> 记录 evalRunId
-> 小流量灰度
-> 观察错误率、延迟、成本和用户反馈
-> 扩大流量或回滚
```

回滚只切换路由或发布指针，不删除旧 Prompt。旧版本保留到审计和问题复盘窗口结束。

## 6. 超时、重试和限流

### 6.1 超时

每次 AI 请求要有总超时预算：

```text
HTTP 请求总预算
= 模型调用预算
+ RAG 检索预算
+ rerank 预算
+ 工具调用预算
+ Agent step 预算
```

超时错误统一记录 `AI_REQUEST_TIMEOUT`、`RAG_RETRIEVAL_TIMEOUT`、`TOOL_TIMEOUT` 或 `AGENT_STEP_TIMEOUT`。日志中记录发生超时的阶段，不记录完整输入输出。

### 6.2 重试

| 场景 | 策略 |
|---|---|
| 模型临时超时 | 可按指数退避加抖动重试，限制次数 |
| 结构化输出解析失败 | 可让模型修正一次，再走 DTO 校验 |
| RAG 检索失败 | 记录错误并返回可解释失败 |
| READ 工具临时失败 | 可重试，仍需幂等和权限检查 |
| WRITE 工具失败 | 依赖阶段 4 的 `idempotencyKey`，不自动重复提交业务动作 |
| Agent step 失败 | 依赖阶段 5 的 `retryable` 和 `failure_type` |

重试要计入成本和 trace。用户看到的错误结果应能关联到所有重试尝试。

### 6.3 限流和配额

限流维度：

- 用户。
- 租户。
- `feature`。
- 模型。
- API key 或客户端。
- 单次请求 token 上限。
- 每日费用上限。

配额拒绝使用 `AI_QUOTA_EXCEEDED` 或 `AI_RATE_LIMITED`。高风险工具调用仍按阶段 4 的权限、确认和审计执行，配额系统必须继续遵守业务校验。

## 7. 成本台账

成本治理的基础是每次调用都能落账。

```text
ai_cost_event
├── id
├── trace_id
├── feature
├── provider
├── model
├── prompt_version
├── input_tokens
├── output_tokens
├── total_tokens
├── unit_price_ref
├── cost_amount
├── currency
├── latency_ms
├── status
├── error_code
├── user_ref
├── tenant_ref
└── created_at
```

统计口径：

| 维度 | 用途 |
|---|---|
| `feature` | 看哪个能力最花钱 |
| `model` | 比较模型成本和质量 |
| `promptVersion` | 判断 Prompt 变更是否增加 token |
| `tenantRef` | 租户账单和预算 |
| `userRef` | 排查滥用和异常调用 |
| `errorCode` | 识别失败成本 |

价格表要带生效时间。模型供应商价格变化时，新价格只影响生效时间之后的成本计算，历史成本保留原口径。

## 8. 用户反馈

用户反馈用于灰度判断、Prompt 改进和问题复盘。反馈记录只保存摘要、标签和引用，不保存完整隐私原文。

```text
ai_feedback_event
├── id
├── trace_id
├── feature
├── model
├── prompt_version
├── feedback_type
├── rating
├── comment_ref
├── user_ref
├── tenant_ref
└── created_at
```

`feedback_type` 可以是 `LIKE`、`DISLIKE`、`CORRECTION`、`REPORT_UNSAFE`。若用户提交了纠错文本，正文进入受控存储，普通反馈表只保存 `comment_ref` 和脱敏摘要。

## 9. 缓存策略

| 缓存类型 | 适用场景 | 关键边界 |
|---|---|---|
| 精确请求缓存 | 完全相同的低风险问答 | key 包含 `tenantRef`、`feature`、`promptVersion`、`model` |
| 语义缓存 | 高频公开知识问答 | 需要相似度阈值、权限隔离和人工抽检 |
| Prompt 缓存 | 模型服务支持上下文缓存时 | 记录缓存命中、过期和成本节省 |
| RAG 检索缓存 | 相同知识库和权限集合下的检索结果 | key 包含知识库版本、权限摘要和检索参数 |
| 工具结果缓存 | READ 工具的短时查询 | 只缓存非敏感摘要，遵守业务时效 |

缓存命中也要记录 `traceId`、`feature`、`cachePolicy` 和 `cacheHit`。涉及 L3/L4 数据、高风险写操作、权限敏感结果时，优先不做语义缓存。

## 10. 异步任务

长耗时 AI 任务使用异步模型：

```text
提交任务
-> 创建 ai_job
-> 返回 jobId
-> 后台执行
-> 写进度和 trace
-> 用户轮询或接收通知
-> 完成后读取结果引用
```

`ai_job` 建议字段：

| 字段 | 说明 |
|---|---|
| `job_id` | 任务 id |
| `trace_id` | 请求链路 |
| `task_id` | Agent task id，可为空 |
| `feature` | 能力类型 |
| `user_ref` | 用户引用 |
| `tenant_ref` | 租户引用 |
| `status` | `PENDING`、`RUNNING`、`SUCCEEDED`、`FAILED`、`CANCELLED` |
| `progress` | 进度百分比或阶段 |
| `input_ref` | 输入引用 |
| `output_ref` | 输出引用 |
| `idempotency_key` | 幂等提交键 |
| `error_code` | 错误码 |
| `retry_count` | 重试次数 |
| `cost_amount` | 任务成本 |
| `created_at` | 创建时间 |
| `finished_at` | 完成时间 |

异步任务要支持幂等提交。相同 `idempotencyKey` 的重复提交返回同一个 `jobId`，减少用户刷新页面导致的重复扣费。建议对 `tenant_ref`、`user_ref`、`feature`、`idempotency_key` 建唯一约束。

`jobId` 是异步执行容器。异步任务内部启动阶段 5 的 Agent 时，`ai_job.task_id` 指向 `agent_task.id`，trace 查询可以从 `jobId` 进入 `taskId` 和 `stepId`。

## 11. 灰度、回滚和告警

### 11.1 灰度

灰度对象包括：

- 模型路由。
- Prompt 版本。
- RAG 切分策略。
- embedding 或 rerank 模型。
- 工具 schema。
- Agent step 编排。

灰度维度包括租户、用户白名单、流量百分比和功能开关。灰度期间必须对比基线：错误率、延迟、token、成本、用户反馈和阶段 6 评测指标。

### 11.2 回滚

回滚配置至少保存：

| 字段 | 说明 |
|---|---|
| `release_id` | 发布 id |
| `previous_release_id` | 上一稳定版本 |
| `rollback_reason` | 回滚原因 |
| `operator_ref` | 操作人 |
| `rolled_back_at` | 时间 |

回滚动作要写审计事件。涉及工具、权限或数据边界的发布，回滚后还要抽查 trace，确认请求已经切回旧配置。

### 11.3 告警

基础告警：

| 告警 | 触发条件 |
|---|---|
| 可用性下降 | AI 请求成功率低于阈值 |
| 延迟升高 | P95/P99 超过阈值 |
| 成本异常 | 单小时或单日费用超过预算 |
| token 异常 | 输入或输出 token 暴涨 |
| 安全拦截增加 | `SECURITY_*` 明显上升 |
| 工具失败增加 | `TOOL_*` 错误率上升 |
| Agent 卡住 | `WAITING_CONFIRMATION` 或 `RUNNING` 超时 |
| 队列积压 | 异步任务堆积超过阈值 |

告警内容只放摘要字段和跳转链接。排查人员通过 `traceId` 进入受控详情页。

## 12. 数据保留和运维手册

保留策略建议：

| 数据 | 保留策略 |
|---|---|
| 普通日志 | 短期保留，便于排障 |
| trace | 覆盖主要问题复盘周期 |
| 成本事件 | 覆盖账单和预算周期 |
| 审计事件 | 按业务合规要求保留 |
| 评测集和评测结果 | 长期保留，支撑回归对比 |
| 完整输入输出 | 受控存储，设置访问权限和保留期限 |

运维手册至少包含：

- 模型供应商故障时如何切换路由。
- 成本异常时如何定位高费用请求。
- Prompt 版本如何回滚。
- RAG 召回异常如何定位知识库版本。
- Tool Calling 越权告警如何排查。
- Agent 卡住如何取消任务和清理待确认请求。

## 13. 验收清单

- 模型路由能按 `feature`、`taskType`、`dataLevel` 和 `budgetClass` 选择模型。
- 每次调用都有 token、成本、延迟、错误码和 `traceId`。
- Prompt 发布关联阶段 6 的 `evalRunId`。
- 限流和配额能阻止超预算调用。
- 缓存 key 包含权限和版本边界。
- 异步任务支持进度、取消、幂等和重试。
- 灰度发布能按流量比例切换。
- 回滚能恢复上一版模型路由和 Prompt。
- 告警能覆盖可用性、延迟、成本、安全、工具和 Agent 卡住。
- 运维手册能指导一次真实问题排查。

完成阶段 7 后，你的项目已经具备上线前治理能力。后续阶段 8 再学习本地模型和推理优化，用来理解模型选择和部署成本。
