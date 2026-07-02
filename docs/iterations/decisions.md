# 决策记录

本文件记录影响仓库长期结构或技术方向的决策。待办事项写入 `backlog.md`。

## 2026-06-29：仓库定位为学习 + 实验

决策：本仓库主定位是 Java AI 应用开发的学习与实验仓库，按阶段演进成熟应用。

原因：

- 当前目标是按路线逐步学习和积累能力。
- 过早平台化会制造大量空模块和维护成本。
- 后续可以选择 1-2 个成熟 app 作为作品集重点展示。

## 2026-06-29：Maven 工程放在 backend/

决策：仓库根目录作为学习工作区根，Java/Maven 工程放在 `backend/`。

原因：

- 根目录需要同时承载 README、AGENTS、docs、prompts、CI，以及未来可能出现的 evals、infra、frontend。
- 根目录直接作为 Maven 工程根时，仓库会更接近单一 Java 后端项目，弱化长期学习实验仓库定位。
- `backend/` 结构为未来多应用和前端/基础设施扩展保留空间。

## 2026-06-29：远期模块按阶段创建

决策：只在进入对应阶段时新增 app、evals、infra、data 等目录。

原因：

- 空目录和空模块会增加维护负担。
- 当前阶段只需要 `ai-chat-api`。
- 远期阶段的实现边界可能随着学习和实践变化。

## 2026-06-29：默认模型协议采用 OpenAI-compatible

决策：默认模型服务配置使用 OpenAI-compatible 约定，同时保留 Ollama 等本地模型实验空间。

原因：

- OpenAI-compatible 协议覆盖面广，便于切换不同模型服务。
- Java 后端应用可以先围绕稳定 HTTP API 和配置管理建立工程能力。
- 本地模型适合后续成本、隐私和离线实验；第一阶段以 OpenAI-compatible HTTP API 为主。

## 2026-07-01：阶段 1 采用 mock-first Provider 策略

决策：阶段 1 先实现项目自己的应用层 Provider 抽象和 mock/stub Provider，默认 `AI_PROVIDER=mock`，聊天模型字段统一为 `AI_CHAT_MODEL`。真实 OpenAI-compatible Provider 在 mock/stub Provider、配置缺失和错误映射测试稳定后接入。当前阶段不直接引入 Spring AI；后续引入时作为 Provider 实现或增强项接入，业务层仍依赖项目自己的 Provider 接口。

原因：

- mock/stub Provider 能让本地开发和 CI 不依赖真实 API Key、外部网络、模型额度和供应商稳定性。
- 先稳定配置、错误结构、trace 字段和日志脱敏，再接入真实模型，可以降低费用和数据外发风险。
- `AI_PROVIDER` 使用具体供应商或适配器标识，避免把 OpenAI-compatible 协议形态误当成具体 Provider。
- 保留 Spring AI 接入空间，同时避免当前阶段过早引入框架依赖和版本选择成本。

## 2026-07-02：Java 包名和 Provider 包结构

决策：个人学习项目的 Maven `groupId` 使用 `io.github.itstarts.aialab`，`ai-chat-api` Java 根包使用 `io.github.itstarts.aialab.chat`。Provider 相关代码按应用层契约、配置、错误和具体实现分包；公共契约 DTO 命名为 `ProviderChatRequest` / `ProviderChatResponse`，具体厂商请求响应类放入对应 Provider 子包。

原因：

- `io.github.itstarts` 能表达个人 GitHub 命名空间，`aialab` 保留当前学习仓库的项目域。
- 应用层 Provider 契约需要和具体厂商协议 DTO 分开，后续新增 OpenAI、Ollama 等 Provider 时不混淆。
- Controller 只保留 HTTP 调度逻辑，API DTO 独立放置后更适合作为对外契约演进。

## 2026-06-29：仓库文档作为工程权威来源

决策：`java-ai-application-lab` 仓库内的 `docs/` 是工程文档权威来源；个人知识库或飞书中的副本只用于阅读和导入。

原因：

- 路线、阶段指南、工程契约和代码需要一起演进。
- 只保留一个工程权威来源，保证个人知识库和仓库文档职责清晰。
- AI 接手时应从仓库内文档恢复上下文，不跨目录猜测最新版本。

## 2026-06-29：阶段指南和工程基线分离

决策：阶段 4-8 的长文档放在 `docs/stages/`，当前已经生效的跨阶段工程契约放在 `docs/reference/engineering-baseline.md`。

原因：

- 阶段 4-8 文档保留为后续阶段指南，进入对应阶段并落地实现后再提升为当前代码硬约束。
- 当前阶段只需要数据边界、错误响应、基础 trace、日志脱敏和 Provider 记录字段。
- 进入对应阶段并落地实现后，再把实际采用的字段提升到工程基线、代码和测试。

## 2026-06-29：继续使用 docs/iterations/decisions.md 记录决策

决策：不新增 `docs/decisions/` 目录，继续使用 `docs/iterations/decisions.md` 记录关键技术和仓库设计决策。

原因：

- 当前仓库已有决策记录入口。
- 新增 `docs/decisions/` 会造成两个决策入口并存。
- 以后如果 ADR 数量明显增加，再单独迁移为目录结构。
