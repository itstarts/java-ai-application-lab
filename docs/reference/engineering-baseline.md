# 工程基线

本文记录当前已经生效、跨阶段共用的工程契约。阶段 4-8 指南中的 Tool Calling、Agent、评测、成本治理和本地推理字段，进入对应阶段并落地到代码和测试后，再提升到本文。

## 数据边界

- API Key、Token、密码、私钥证书、真实客户数据、真实简历、真实订单、合同和内部文档不得提交到版本库。
- 模型服务凭证通过环境变量或本地密钥管理方式提供。
- `.env` 保持未跟踪状态，只有 `.env.example` 可以提交。
- 样例数据使用合成数据或明确脱敏后的数据。
- 未经明确授权，不把敏感本地文件或业务数据发送给外部模型服务。

## 错误响应

HTTP 接口对外使用统一错误结构：

```json
{
  "code": "AI_REQUEST_TIMEOUT",
  "message": "模型服务响应超时",
  "traceId": "trace_001"
}
```

字段约定：

| 场景 | 字段 | 说明 |
|---|---|---|
| HTTP 响应和 SSE `error` 事件 | `code` | 对前端和调用方暴露的错误码 |
| 日志、数据库、trace 和内部记录 | `errorCode` | 内部排查和关联用错误码 |

## Trace 字段

当前阶段优先保留这些字段：

| 字段 | 说明 |
|---|---|
| `traceId` | 一次请求的全链路 id |
| `conversationId` | 会话 id，可为空 |
| `messageId` | 当前用户消息或模型消息 id |
| `model` | 实际使用的模型标识 |
| `promptVersion` | Prompt 版本 |
| `feature` | 能力类型，例如 `chat` |

后续阶段新增 `toolCallId`、`taskId`、`stepId`、`evalRunId` 等字段时，先在对应阶段文档中验证，再同步到本文。

## 日志脱敏

普通日志记录：

- `traceId`
- `feature`
- `provider`
- `model`
- `promptVersion`
- `latencyMs`
- `status`
- `errorCode`

普通日志不记录：

- 完整 Prompt。
- 完整用户输入。
- API Key、Authorization header、Token。
- 真实姓名、手机号、邮箱、身份证、银行卡等隐私原文。
- 真实订单、合同、简历或内部文档原文。

确需排查完整输入输出时，进入受控调试存储，并设置访问权限和保留期限。

## Provider 记录字段

接入真实模型 Provider 时，模型调用记录至少包含：

| 字段 | 说明 |
|---|---|
| `traceId` | 请求链路 |
| `provider` | 模型服务供应商或协议适配器 |
| `model` | 模型名称 |
| `feature` | 能力类型 |
| `promptVersion` | Prompt 版本 |
| `latencyMs` | 调用耗时 |
| `status` | 成功、失败、超时等状态 |
| `errorCode` | 内部错误码，可为空 |

Controller 只处理 HTTP 交互，业务代码通过 Provider 抽象访问具体模型服务。

## 变更规则

- 涉及当前生效字段时，先对齐本文。
- 修改共享字段时，同步更新 README、AGENTS、相关迭代文档和测试。
- 未来阶段指南中的字段不自动成为当前阶段硬约束；进入对应阶段并落地实现后再提升到本文。
