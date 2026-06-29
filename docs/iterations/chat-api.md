# Chat API 迭代计划

对应路线图阶段：阶段 1，LLM API 与聊天服务。

## 目标

把 `backend/apps/ai-chat-api` 从 stub 聊天接口演进为可配置、可测试、可逐步接入真实模型的 Chat API。

## 当前范围

本阶段优先完成：

- 保留 `GET /health`。
- 完善 `POST /api/chat` 的请求和响应结构。
- 抽象模型 Provider 接口，先让应用代码不直接绑定具体厂商。
- 接入 OpenAI-compatible Provider。
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

## 交付物

- 代码：
  - `backend/apps/ai-chat-api` 内的 Chat API 实现。
  - Provider 抽象和 OpenAI-compatible 实现。
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

## 验证命令

当前终端可显式指定 JDK 21：

```bash
export JAVA_HOME=/usr/local/opt/openjdk@21
export PATH="$JAVA_HOME/bin:$PATH"
```

运行测试：

```bash
mvn -f backend/pom.xml test
```

启动应用：

```bash
mvn -f backend/pom.xml -pl apps/ai-chat-api spring-boot:run
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
- OpenAI-compatible 是默认协议约定，仍保留其他兼容服务或本地模型的接入空间。
- 真实模型接入前，应先用 mock/stub 覆盖错误分支。

## 后续入口

完成基础 Chat API 后，下一步进入：

- 流式输出。
- Prompt 版本加载。
- 结构化输出实验。

是否直接进入独立阶段，以路线图和当时的需求为准。
