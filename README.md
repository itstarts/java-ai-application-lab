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
│   ├── reference/
│   ├── stages/
│   ├── iterations/
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

## 学习路线和阶段命名

阶段命名和阶段顺序以 [`docs/roadmap/java-ai-learning-roadmap.md`](docs/roadmap/java-ai-learning-roadmap.md) 为准。文档阅读顺序见 [`docs/README.md`](docs/README.md)。

### 阶段与应用的关系

当前阶段只有一个后端应用：`backend/apps/ai-chat-api`。

阶段 01-02 会优先在 `ai-chat-api` 内迭代，先把聊天接口、流式输出、Prompt 管理和结构化输出做扎实。进入阶段 03 后，如果 RAG 知识库的文档解析、向量检索、引用来源和后台任务开始明显独立，再新增 `backend/apps/rag-knowledge-base`。阶段 04 的智能客服、阶段 05 的 Agent 工作流也遵循同样原则：只有当边界足够清晰、独立运行有价值时，才新增 app。

这样让当前结构保持轻量，同时保留后续演进为多应用实验仓库的空间。

## 本地开发

### 环境要求

- 已安装 JDK 21（用于编译和运行）
- Maven Wrapper 已随仓库提供，无需单独安装 Maven

构建通过 Maven Toolchains 把编译和测试固定运行在 JDK 21。Maven Wrapper 自身可由 JDK 11 或更高版本启动，因此本机默认 JDK 不需要切换到 21，也不需要为每个终端设置 `JAVA_HOME`。

### 首次配置 JDK 21 工具链

首次 clone 后运行一次，向本机 `~/.m2/toolchains.xml` 声明 JDK 21 的安装位置：

```bash
backend/scripts/setup-toolchains.sh
```

脚本只新增或更新 JDK 21 条目，保留文件中其他 JDK 版本的 toolchain。脚本自动探测 JDK 21 的安装路径，兼容 Intel 与 Apple Silicon 的 Homebrew 前缀。也可通过 `JDK21_HOME` 显式指定：

```bash
JDK21_HOME=/path/to/jdk-21 backend/scripts/setup-toolchains.sh
```

未安装 JDK 21 时，Homebrew 安装方式：

```bash
brew install openjdk@21
```

`~/.m2/toolchains.xml` 含本机绝对路径，属于机器本地配置，不进版本库。

### 运行测试

从仓库根目录执行：

```bash
backend/mvnw -f backend/pom.xml test
```

或进入后端工程目录执行：

```bash
cd backend
./mvnw test
```

### 启动第一个应用

进入后端工程目录执行：

```bash
cd backend
./mvnw -pl apps/ai-chat-api spring-boot:run
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

完整学习路线见 [`docs/roadmap/java-ai-learning-roadmap.md`](docs/roadmap/java-ai-learning-roadmap.md)。
项目演进路线见 [`docs/roadmap/project-evolution-roadmap.md`](docs/roadmap/project-evolution-roadmap.md)。

## AI 辅助开发规范

让 AI 助手修改本仓库前，先阅读：

```text
AGENTS.md
```

关键规则：

- 数据边界以 [`docs/reference/engineering-baseline.md`](docs/reference/engineering-baseline.md) 为准。
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
