# Java AI Application Lab

面向 Java 后端程序员的 AI 应用开发学习与实战仓库。

这个仓库按阶段组织 AI 应用开发学习和实战内容，逐步积累：

- 学习路线和阶段笔记
- 可运行的 Java/Spring Boot 示例应用
- Prompt 版本管理
- 评测集和评测报告
- AI 辅助开发规范
- 安全、合规、可观测性和成本治理实践

## 仓库定位

本仓库的主定位是 **学习 + 实验**。后续可以选择 1-2 个成熟应用作为作品集重点展示，例如：

- `rag-knowledge-base`：企业知识库问答
- `ai-customer-service`：带工具调用的智能客服

当前阶段保留最小可运行骨架，后续按阶段新增模块。

## 技术主线

| 类别 | 选择 |
|---|---|
| 语言 | Java 21 |
| 后端框架 | Spring Boot |
| AI 框架 | Spring AI，后续按阶段引入 |
| 构建工具 | Maven |
| 首个应用 | `backend/apps/ai-chat-api` |
| CI | GitHub Actions |

默认模型服务约定为 OpenAI-compatible API，本地实验可使用 Ollama。当前 `ai-chat-api` 仍是 stub 实现，只返回本地响应。

## 目录结构

```text
java-ai-application-lab/
├── README.md
├── AGENTS.md
├── LICENSE
├── .env.example
├── .gitignore
├── .github/
│   └── workflows/
│       └── build.yml
├── backend/
│   ├── pom.xml
│   └── apps/
│       └── ai-chat-api/
├── docs/
│   ├── roadmap/
│   └── notes/
└── prompts/
    └── chat/
```

### 为什么 Maven 工程放在 `backend/`

仓库根目录承载学习路线、Prompt、评测、AI 协作规则和未来前端/基础设施内容。Java/Maven 工程放在 `backend/` 下，让根目录保持学习工作区定位。

后续如果增加前端或基础设施，可以自然扩展为：

```text
frontend/
infra/
evals/
data/
```

## 阶段命名

所有阶段在应用、文档、Prompt、评测、tag 中统一命名。

| 阶段 | 名称 | 主要交付物 |
|---|---|---|
| 01 | `chat` | Chat API 骨架和流式模型接入 |
| 02 | `structured-output` | JSON 抽取、结构化输出和校验 |
| 03 | `rag` | 基于 pgvector 的知识库问答和引用来源 |
| 04 | `tool-calling` | 带权限、审计和确认机制的工具调用 |
| 05 | `agent-workflow` | 有状态 Agent 工作流和人工检查点 |
| 06 | `evaluation` | 评测集、指标和回归报告 |
| 07 | `security` | Prompt injection、数据边界和访问控制 |
| 08 | `observability` | 模型调用链路、成本、延迟和监控 |

### 阶段与应用的关系

当前阶段只有一个后端应用：`backend/apps/ai-chat-api`。

阶段 01-02 会优先在 `ai-chat-api` 内迭代，先把聊天接口、流式输出、Prompt 管理和结构化输出做扎实。进入阶段 03 后，如果 RAG 知识库的文档解析、向量检索、引用来源和后台任务开始明显独立，再新增 `backend/apps/rag-knowledge-base`。阶段 04 的智能客服、阶段 05 的 Agent 工作流也遵循同样原则：只有当边界足够清晰、独立运行有价值时，才新增 app。

这样让当前结构保持轻量，同时保留后续演进为多应用实验仓库的空间。

## 本地开发

### 环境要求

- JDK 21
- Maven 3.9+

当前本机如果仍是 JDK 11，执行 Maven 会报：

```text
错误: 不支持发行版本 21
```

需要先安装并切换到 JDK 21。

当前终端可显式指定已安装的 JDK 21。

Homebrew 安装的 JDK 21：

```bash
export JAVA_HOME=/usr/local/opt/openjdk@21
export PATH="$JAVA_HOME/bin:$PATH"
java -version
```

如果 JDK 21 已注册到 macOS JavaVirtualMachines：

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export PATH="$JAVA_HOME/bin:$PATH"
java -version
```

### 运行测试

从仓库根目录执行：

```bash
mvn -f backend/pom.xml test
```

或进入后端工程目录执行：

```bash
cd backend
mvn test
```

### 启动第一个应用

从仓库根目录执行：

```bash
mvn -f backend/pom.xml -pl apps/ai-chat-api spring-boot:run
```

或进入后端工程目录执行：

```bash
cd backend
mvn -pl apps/ai-chat-api spring-boot:run
```

健康检查：

```bash
curl http://localhost:8080/health
```

Stub 聊天请求：

```bash
curl -X POST http://localhost:8080/api/chat \
  -H 'Content-Type: application/json' \
  -d '{"message":"hello"}'
```

## 环境变量

复制 `.env.example` 到 `.env` 后再填写本地密钥：

```bash
cp .env.example .env
```

`.env` 只保留在本地，不提交到版本库。当前阶段只用 `.env.example` 统一后续模型配置的命名约定。

## 学习路线

完整学习路线见：

```text
docs/roadmap/java-ai-learning-roadmap.md
```

## AI 辅助开发规范

让 AI 助手修改本仓库前，先阅读：

```text
AGENTS.md
```

关键规则：

- API Key、Token、真实用户数据、真实简历、真实订单或内部文档只保留在本地或受控密钥系统中。
- Prompt 修改必须进入 `prompts/` 并说明版本变化。
- RAG 变更需要记录召回率、拒答率和引用准确率。
- Tool Calling 的写操作必须保留权限校验、幂等、审计和人工确认。
- 声称功能完成或测试通过前，必须给出验证证据。

## Git 建议

学习仓库建议使用 `main + 阶段 tag`：

```text
v0.1-chat
v0.2-structured-output
v0.3-rag-baseline
v0.4-tool-calling
v0.5-agent-workflow
```

阶段交付优先使用 tag 标记。长期阶段分支容易形成无人维护的历史分支。
