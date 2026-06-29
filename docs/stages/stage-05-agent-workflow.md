# 阶段 5：Agent 与工作流设计

> 本文档的工程权威版本位于 `java-ai-application-lab` 仓库。个人知识库或飞书中的副本仅用于阅读和导入。

本文对应学习路线中的阶段 5，目标是做一个有状态、有步骤、有工具、有人工确认和失败处理的半自动业务助手。

阶段 4 已经把工具调用、权限、确认、幂等和审计做成后端受控能力。阶段 5 不重新定义工具边界，而是在这些工具之上增加任务状态、步骤编排、可暂停确认、失败重试和执行 trace。

本文沿用 [`project-evolution-roadmap.md`](../roadmap/project-evolution-roadmap.md) 中的阶段演进约定，并建立在阶段 4 的工具执行规则之上。当前阶段之前已经生效的工程基线见 [`engineering-baseline.md`](../reference/engineering-baseline.md)。

## 1. 阶段目标

完成一个需求分析 Agent：

- 输入一段产品需求。
- 提取业务目标。
- 拆分后端接口。
- 生成数据库表草案。
- 识别风险点。
- 生成测试用例。
- 等用户确认。
- 输出最终方案。

阶段 5 的验收标准是：任务能持久化；每一步状态可查看；高风险步骤能暂停等待确认；失败步骤可单独重试；trace 能串联模型调用、RAG 引用、工具调用和审计事件。

## 2. 推荐技术组合

| 能力 | 推荐技术 |
|---|---|
| Web 框架 | Spring Boot 3.x |
| Java 版本 | Java 21 |
| AI 抽象 | Spring AI ChatClient、Tool Calling |
| 备选 AI 抽象 | LangChain4j AI Services、Tools、Memory |
| 状态存储 | PostgreSQL |
| 短期状态缓存 | Redis，可选 |
| 状态编排 | 应用层状态机 |
| 复杂工作流备选 | Temporal、Camunda、Flowable |
| 测试 | JUnit 5、Testcontainers |

学习阶段先用应用层状态机和数据库表。只有当任务需要跨天运行、人工审批链复杂、补偿动作多、强恢复能力要求高时，再系统引入 Temporal、Camunda 或 Flowable。

## 3. Agent 的工程定义

在本路线中，Agent 按下面方式理解：

```text
Agent = 模型调用 + 工具调用 + 任务状态 + 步骤编排 + 人工确认 + 失败处理 + 审计
```

模型负责生成计划、分析文本和提出下一步候选。后端负责：

- 决定可用工具集合。
- 校验模型输出。
- 持久化任务和步骤。
- 控制状态流转。
- 执行工具和记录审计。
- 判断哪些步骤需要人工确认。

Agent 的输出进入后端校验或人工确认后再转成业务指令。模型生成的计划、SQL、接口设计、风险判断和工具参数都要经过后端边界。

## 4. 推荐模块结构

```text
ai-workflow-agent
├── controller
│   └── AgentTaskController
├── application
│   ├── AgentTaskService
│   ├── AgentPlanner
│   ├── AgentStepExecutor
│   ├── AgentRetryService
│   └── AgentConfirmationService
├── state
│   ├── AgentTask
│   ├── AgentStep
│   ├── AgentTaskStatus
│   └── AgentStepStatus
├── tool
│   └── AgentToolAdapter
├── trace
│   ├── AgentTraceRecorder
│   └── AgentTraceEvent
└── eval
    └── AgentEvalRunner
```

关键职责：

- `AgentPlanner` 生成步骤计划。
- `AgentStepExecutor` 执行单个步骤。
- `AgentConfirmationService` 处理用户确认。
- `AgentRetryService` 控制失败重试。
- `AgentToolAdapter` 复用阶段 4 的 ToolExecutionService。
- `AgentTraceRecorder` 记录任务、步骤、模型调用、工具调用和审计引用。

## 5. 数据模型

### 5.1 agent_task

| 字段 | 说明 |
|---|---|
| id | taskId |
| user_ref | 用户引用 |
| tenant_ref | 租户或团队引用 |
| title | 任务标题 |
| goal | 用户目标摘要 |
| status | 任务状态 |
| current_step_id | 当前步骤 |
| input_ref | 输入引用或脱敏摘要 |
| output_ref | 最终输出引用 |
| error_code | 错误码 |
| trace_id | traceId |
| version | 乐观锁版本 |
| created_at | 创建时间 |
| updated_at | 更新时间 |

### 5.2 agent_step

| 字段 | 说明 |
|---|---|
| id | stepId |
| task_id | 所属任务 |
| trace_id | traceId |
| step_index | 执行顺序 |
| step_type | 步骤类型 |
| status | 步骤状态 |
| input_ref | 输入引用 |
| output_ref | 输出引用 |
| prompt_version | Prompt 版本 |
| model | 模型 |
| tool_name | 工具名称，可为空 |
| tool_call_id | 工具调用 id，可为空 |
| confirmation_id | 确认请求 id，可为空 |
| idempotency_key | 写工具幂等键，可为空 |
| retryable | 当前失败是否可重试 |
| failure_type | 失败类型，可为空 |
| retry_count | 重试次数 |
| latency_ms | 耗时 |
| error_code | 错误码 |
| version | 乐观锁版本 |
| started_at | 开始时间 |
| finished_at | 结束时间 |

### 5.3 agent_trace_event

| 字段 | 说明 |
|---|---|
| id | trace 事件 id |
| trace_id | traceId |
| task_id | taskId |
| step_id | stepId |
| event_type | 事件类型 |
| ref_type | 引用类型 |
| ref_id | 引用 id |
| summary | 脱敏摘要 |
| created_at | 时间 |

trace 事件记录引用，不记录完整 Prompt、完整工具返回和用户隐私原文。

常见 `event_type`：

| event_type | 说明 |
|---|---|
| `MODEL_CALL` | 模型调用 |
| `RAG_RETRIEVAL` | RAG 检索 |
| `TOOL_CALL` | 工具调用 |
| `CONFIRMATION` | 人工确认 |
| `AUDIT` | 审计事件 |
| `STEP_OUTPUT` | 步骤输出 |

常见 `ref_type`：

| ref_type | 说明 |
|---|---|
| `message` | 消息 |
| `rag_chunk` | RAG chunk 引用 |
| `tool_result` | 工具结果 |
| `confirmation` | 确认请求 |
| `audit_event` | 审计事件 |
| `artifact` | 生成物 |

## 6. 状态设计

任务状态：

| 状态 | 可进入条件 |
|---|---|
| `CREATED` | 用户创建任务 |
| `PLANNING` | 正在生成步骤计划 |
| `WAITING_CONFIRMATION` | 有步骤等待用户确认 |
| `EXECUTING` | 正在执行步骤 |
| `FAILED` | 任务不可继续或重试耗尽 |
| `CANCELLED` | 用户取消 |
| `COMPLETED` | 所有步骤完成 |

步骤状态：

| 状态 | 可进入条件 |
|---|---|
| `PENDING` | 已生成但未执行 |
| `RUNNING` | 正在执行 |
| `WAITING_CONFIRMATION` | 等用户确认 |
| `SUCCEEDED` | 执行成功 |
| `FAILED` | 执行失败 |
| `SKIPPED` | 被用户或规则跳过 |
| `CANCELLED` | 运行中步骤因所属任务取消而停止推进 |

状态流转由后端控制。模型只建议下一步，状态修改由应用层完成。

`FAILED` 必须同时记录 `retryable` 和 `failure_type`。模型超时、临时工具失败等技术失败可以设置 `retryable=true`；用户拒绝、确认过期、权限拒绝、业务状态不允许等终态失败设置 `retryable=false`。

状态更新使用条件更新或乐观锁，例如：

```sql
update agent_step
set status = 'RUNNING', version = version + 1
where id = :stepId and status = 'PENDING' and version = :version
```

确认回调、重试入口和异步执行入口都按期望状态更新。更新不到行时说明步骤已被其他请求处理，本次请求直接返回当前状态。

任务和步骤的状态映射：

- 必选步骤 `FAILED` 且 `retryable=false` 时，任务进入 `FAILED`。
- 可跳过步骤由用户或规则明确标记为 `SKIPPED` 后，任务继续执行后续步骤。
- 步骤重试耗尽后设置 `retryable=false`，任务进入 `FAILED`。
- 用户取消任务时，任务进入 `CANCELLED`；`PENDING` 和 `WAITING_CONFIRMATION` 步骤标记为 `SKIPPED`，待确认请求标记为 `CANCELLED`；`RUNNING` 步骤标记为 `CANCELLED`，不再派发后续动作，已发出的外部工具调用按阶段 4 的幂等和审计规则收敛并记录 trace。

## 7. 步骤类型

需求分析 Agent 可以先定义这些步骤：

| stepType | 输入 | 输出 |
|---|---|---|
| `ANALYZE_REQUIREMENT` | 用户需求 | 业务目标、范围、约束 |
| `DESIGN_API` | 业务目标 | API 草案 |
| `DESIGN_SCHEMA` | API 草案 | 表结构草案 |
| `IDENTIFY_RISK` | 需求和设计草案 | 风险点 |
| `GENERATE_TESTS` | API 和风险点 | 测试用例 |
| `EXPORT_PLAN` | 已确认结果 | 最终方案 |

确认作为某个步骤进入 `WAITING_CONFIRMATION` 状态来表达。每个步骤有固定输入输出结构。阶段 2 的结构化输出方法在这里继续使用：模型输出 DTO，后端做 JSON 解析、字段校验和业务规则校验。

## 8. 执行流程

推荐流程：

```text
创建任务
-> 保存原始输入引用
-> 生成步骤计划
-> 持久化 agent_step
-> 执行第一个 PENDING step
-> 记录模型调用和输出引用
-> 如需工具，走阶段 4 ToolExecutionService
-> 如需确认，当前 step 进入 WAITING_CONFIRMATION
-> 用户确认后继续执行
-> 某步失败则记录 errorCode
-> 支持单步重试或取消任务
-> 全部成功后生成最终输出
```

Agent 不需要一次性把所有步骤都交给模型自由执行。学习阶段推荐“后端固定步骤 + 模型填充内容”的方式，便于测试和排查。

## 9. 人工确认

确认节点用于控制高风险步骤。

需要确认的场景：

- 写操作工具调用。
- 输出最终方案前。
- 生成数据库变更建议。
- 生成对外接口变更建议。
- 需要使用 L3 数据或更高风险数据。

确认请求包含：

| 字段 | 说明 |
|---|---|
| `confirmationId` | 确认 id |
| `taskId` | 任务 id |
| `stepId` | 步骤 id |
| `toolCallId` | 工具调用 id，可为空 |
| `idempotencyKey` | 写工具幂等键，可为空 |
| `summary` | 给用户看的确认摘要 |
| `riskLevel` | 风险等级 |
| `status` | `PENDING`、`CONFIRMED`、`REJECTED`、`EXPIRED`、`CANCELLED` |
| `expiresAt` | 过期时间 |

需要写工具确认时，`idempotencyKey` 在创建确认请求时生成，并与 `confirmationId`、`stepId`、`toolCallId`、参数摘要一起持久化。用户确认后读取已有幂等键，不重新生成。

用户确认后，后端继续执行；用户拒绝时，步骤进入 `FAILED` 且 `retryable=false`，`failure_type=USER_REJECTED`。确认过期时，步骤进入 `FAILED` 且 `retryable=false`，`failure_type=CONFIRMATION_EXPIRED`。过期可以由定时任务扫描，也可以在用户下次查询或确认时惰性判定。

任务取消导致确认请求进入 `CANCELLED` 时，步骤按任务取消规则进入 `SKIPPED`，不走用户拒绝或过期的失败路径。

## 10. 失败与重试

失败分三类：

| 类型 | 示例 | 处理 |
|---|---|---|
| 可重试技术失败 | 模型超时、工具临时失败 | 按次数重试 |
| 可修正输入失败 | JSON 校验失败、参数缺失 | 让模型修正一次或等待用户补充 |
| 不可继续失败 | 权限拒绝、确认过期、业务状态不允许 | 停止当前步骤 |

重试规则：

- 每个 step 有 `retry_count` 和最大重试次数。
- 只有 `retryable=true` 的失败步骤可以重试。
- 重试只重跑当前步骤，不重跑已成功步骤。
- 重试前保留上一次失败输出引用。
- 写操作工具重试必须复用 `agent_step.idempotency_key`。
- 用户拒绝、确认过期、权限拒绝和业务状态不允许不进入自动重试。

## 11. Tool Calling 接入

Agent 调工具时复用阶段 4：

```text
AgentStepExecutor
-> AgentToolAdapter
-> ToolExecutionService
-> ToolRegistry
-> 权限校验
-> DTO 校验
-> 确认和幂等
-> 审计事件
```

Agent 不直接调用业务服务。这样可以保证普通客服助手和 Agent 使用同一套工具边界、确认规则和审计格式。

## 12. Prompt 与计划约束

Planner Prompt 应让模型输出结构化计划，例如：

```json
{
  "steps": [
    {"stepType": "ANALYZE_REQUIREMENT", "reason": "先明确目标"},
    {"stepType": "DESIGN_API", "reason": "根据目标设计接口"},
    {"stepType": "DESIGN_SCHEMA", "reason": "接口需要数据结构支撑"}
  ],
  "riskNotes": []
}
```

后端只接受白名单内的 `stepType`。模型可选范围限定为白名单中的状态、工具和 SQL 执行步骤。

## 13. 观测与 Trace

每次 Agent 任务至少记录：

| 信息 | 用途 |
|---|---|
| `traceId` | 串联用户请求 |
| `taskId` | 串联任务 |
| `stepId` | 定位步骤 |
| `model` | 定位模型版本 |
| `promptVersion` | 定位 Prompt |
| `toolName` | 定位工具 |
| `toolCallId` | 关联工具结果 |
| `ragChunkIds` | 关联 RAG 召回片段 |
| `latencyMs` | 分析耗时 |
| `errorCode` | 排查失败 |

管理后台应能按 `traceId` 查看：用户输入摘要、步骤列表、模型调用、RAG 引用、工具调用、确认记录、审计事件和最终输出引用。

## 14. 评测集设计

至少准备 30 条 Agent 流程样例：

| 类型 | 数量 | 目标 |
|---|---:|---|
| 简单需求 | 8 | 基础步骤完整 |
| 复杂需求 | 8 | 多步骤输出稳定 |
| 信息不足 | 5 | 能请求补充或标注风险 |
| 高风险变更 | 5 | 能进入确认 |
| 失败和重试 | 4 | 能记录失败和重试 |

JSONL 示例：

```json
{"caseId":"agent_001","feature":"agent","input":{"requirement":"做一个订单取消接口"},"expected":{"steps":["ANALYZE_REQUIREMENT","DESIGN_API","IDENTIFY_RISK","GENERATE_TESTS","EXPORT_PLAN"],"confirmationPoints":["EXPORT_PLAN"],"finalOutputPoints":["接口路径","权限校验","幂等"]},"tags":["api-design"]}
```

核心指标：

| 指标 | 说明 |
|---|---|
| Step Coverage | 是否生成必要步骤 |
| Step Order Accuracy | 步骤顺序是否合理 |
| Confirmation Accuracy | 高风险节点是否确认 |
| Retry Correctness | 失败后是否只重试当前步骤 |
| Trace Completeness | trace 是否能串联 task、step、tool |

## 15. 测试建议

### 15.1 单元测试

- 状态机只允许合法流转。
- 模型输出未知 stepType 时拒绝。
- 步骤失败只影响当前 step。
- 用户拒绝确认时状态正确。
- 确认过期时步骤不可重试。
- 并发确认只有一个请求能推进状态。
- 任务取消后 RUNNING 步骤能收敛为 CANCELLED。
- 任务取消后待确认请求能收敛为 CANCELLED。
- 重试不会重复执行已成功步骤。
- 写操作工具重试复用幂等键。

### 15.2 集成测试

- 创建任务后能生成步骤。
- 执行到确认节点时暂停。
- 用户确认后继续执行。
- 工具调用能产生 toolCallId 和审计事件。
- 管理后台能按 traceId 查看完整链路。

模型调用测试优先使用 stub 响应。真实模型效果放到评测集和手动验收中。

## 16. 阶段完成标准

完成阶段 5 时，应能做到：

- 能创建并持久化 Agent 任务。
- 能生成并保存步骤计划。
- 能按步骤执行和更新状态。
- 能在确认节点暂停和继续。
- 能对失败步骤单独重试。
- 能复用阶段 4 的工具调用、确认、幂等和审计。
- 能按 `traceId` 串联模型调用、RAG 引用、步骤、工具和审计。
- 能用固定样例评测步骤完整性、确认准确性和 trace 完整性。

阶段 5 完成后，阶段 6 可以开始系统化评测、安全测试和可观测性建设。没有状态、步骤和 trace 的 Agent，很难进入生产治理。
