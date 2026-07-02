# Backlog

本文件记录跨阶段待办；已经定稿的技术决策写入 `decisions.md`。

## P0：近期必须处理

- [ ] 接入真实模型 Provider 前，按 `docs/reference/engineering-baseline.md` 落地基础 trace、错误响应、日志脱敏和 Provider 记录字段。
- [x] 接入真实模型 Provider 前，确认默认 Provider、环境变量命名和错误处理策略。
- [ ] 为 `ai-chat-api` 增加 Provider 抽象，让 Controller 通过模型调用接口访问具体服务。
- [ ] README 中在真实模型接入后补充最小可运行配置示例。

说明：当前已确认阶段 1 默认 `AI_PROVIDER=mock`，聊天模型字段统一为 `AI_CHAT_MODEL`。OpenAI-compatible 作为真实 Provider 的协议形态，待 mock/stub Provider 和错误分支测试稳定后接入。

## P1：阶段内需要处理

- [ ] 增加流式输出方案设计，优先评估 SSE。
- [ ] 增加 Prompt 加载和版本管理代码。
- [ ] 为结构化输出阶段准备样例数据格式。
- [ ] 建立最小 evals 目录，但只在进入评测阶段或已有用例时创建。
- [ ] 进入阶段 4 时，把实际采用的 Tool schema、`riskLevel`、`toolCallId`、`idempotencyKey` 和 `confirmationId` 同步提升到 `docs/reference/engineering-baseline.md`。
- [ ] 进入阶段 6 时创建 `evals/`，并把实际评测 JSONL 格式同步提升到 `docs/reference/engineering-baseline.md`。

## P2：后续增强

- [ ] 引入 Spring AI。
- [ ] 增加会话历史持久化。
- [ ] 增加 RAG 知识库 app。
- [ ] 增加 Tool Calling 智能客服 app。
- [ ] 增加 Agent 工作流 app。
- [ ] 增加前端管理页面。
- [ ] 进入阶段 7 时再创建 runbook 或运维手册目录，记录模型路由、成本治理、灰度和回滚流程。
