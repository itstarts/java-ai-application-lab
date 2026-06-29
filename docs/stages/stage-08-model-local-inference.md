# 阶段 8：模型进阶与本地推理入门

> 本文档的工程权威版本位于 `java-ai-application-lab` 仓库。个人知识库或飞书中的副本仅用于阅读和导入。

本文对应学习路线中的阶段 8，目标是理解模型、微调和本地推理的基本取舍，能用 Java 后端接入一个本地模型实验环境。

阶段 8 属于进阶理解，不作为阶段 1 到阶段 7 的前置条件。Java 后端主线仍以应用层能力为核心：模型调用、结构化输出、RAG、Tool Calling、Agent、评测和生产治理。本地模型用于补充模型选择能力、成本判断和私有化部署认知。

本文沿用 [`project-evolution-roadmap.md`](../roadmap/project-evolution-roadmap.md) 中的阶段演进约定。当前阶段之前已经生效的工程基线见 [`engineering-baseline.md`](../reference/engineering-baseline.md)。

## 1. 阶段目标

完成一个本地模型实验室：

- 能用 Ollama 启动本地聊天模型和 embedding 模型。
- 能用 Spring AI 或 OpenAI 兼容接口接入本地模型。
- 能用同一组评测样例比较云模型和本地模型。
- 理解 vLLM、llama.cpp 的适用场景。
- 理解量化、KV cache、batch、并发和吞吐的基本影响。
- 知道微调、LoRA、QLoRA 的适用边界。

阶段 8 的验收标准是：同一个问题集可以分别调用云模型、Ollama、vLLM 或 llama.cpp 服务，记录质量、延迟、token、硬件占用和错误；能基于评测结果说明某个场景是否适合本地模型。

## 2. 推荐技术组合

| 能力 | 推荐技术 |
|---|---|
| Java 接入 | Spring AI `ChatModel`、OpenAI 兼容客户端、应用层 Provider |
| 本地入门 | Ollama |
| 高吞吐服务 | vLLM |
| CPU / 边缘推理 | llama.cpp、GGUF 模型 |
| 微调理解 | Hugging Face Transformers、PEFT、LoRA、QLoRA |
| 评测 | 阶段 6 的 Eval Runner |
| 观测 | 阶段 7 的成本、延迟、错误和 trace 记录 |

Ollama 适合作为第一步，因为它提供本地模型管理、聊天、embedding 和 OpenAI 兼容接口。vLLM 更适合 GPU 服务和高吞吐推理。llama.cpp 更适合 GGUF 量化模型、CPU/边缘设备和轻量部署。

## 3. 推荐模块结构

```text
local-model-lab
├── controller
│   ├── LocalModelController
│   └── ModelBenchmarkController
├── provider
│   ├── LocalChatModelProvider
│   ├── OllamaProvider
│   ├── VllmProvider
│   └── LlamaCppProvider
├── benchmark
│   ├── ModelBenchmarkCase
│   ├── ModelBenchmarkRun
│   ├── ModelBenchmarkRunner
│   └── ModelComparisonReport
└── catalog
    ├── LocalModelCatalog
    └── ModelCapability
```

实验室模块只做模型比较和接入验证。业务模块继续依赖统一 Provider，不直接绑定 Ollama、vLLM 或 llama.cpp。

## 4. 必须理解的模型概念

| 概念 | 需要掌握到什么程度 |
|---|---|
| Base model | 基础模型，通常需要指令微调后才适合聊天 |
| Instruct model | 更适合按指令回答和多轮对话 |
| Tokenizer | 文本切成 token 的方式会影响长度、成本和截断 |
| Context window | 模型一次能处理的上下文长度 |
| Temperature | 控制生成随机性，结构化输出通常设低 |
| Embedding model | 生成向量，用于 RAG 检索 |
| Rerank model | 对候选 chunk 二次排序 |
| Quantization | 降低权重精度，减少显存或内存占用 |
| KV cache | 保存已计算的注意力缓存，影响长上下文和多轮性能 |
| Batch / continuous batching | 合并请求提升吞吐，影响延迟和资源利用率 |

这些概念的学习目标是能做工程取舍，不需要在这个阶段推导模型训练算法。

## 5. 本地推理工具取舍

### 5.1 Ollama

Ollama 适合本地入门和开发验证。

典型能力：

- `ollama pull` 拉取模型。
- `ollama run` 交互运行模型。
- HTTP API 支持生成、聊天、embedding 和模型管理。
- 提供 OpenAI 兼容的 `/v1/chat/completions`。
- 默认本地服务端口常见为 `11434`。

适用场景：

| 场景 | 说明 |
|---|---|
| 本地开发 | 快速验证 Prompt、结构化输出和简单 RAG |
| 隐私实验 | 数据不离开本机，但仍要控制日志和存储 |
| 低并发工具 | 面向开发者或内部小规模使用 |
| embedding 实验 | 用本地 embedding 模型比较召回效果 |

接入 Java 后端时，可以把 Ollama 当作一个 OpenAI 兼容服务：

```properties
spring.ai.openai.base-url=http://localhost:11434
spring.ai.openai.api-key=ollama
spring.ai.openai.chat.options.model=qwen3:8b
```

这里使用 OpenAI 兼容接口，`api-key` 是客户端需要的占位值。也可以使用 Spring AI 原生 Ollama 配置；学习项目中建议先统一到 OpenAI 兼容接口，便于和云模型、vLLM、llama.cpp 使用同一套 Provider 进行对比。

具体模型名称以本机已拉取的模型为准。模型变更要写入阶段 7 的模型路由配置，并跑阶段 6 的回归评测。

### 5.2 vLLM

vLLM 适合 GPU 推理服务和高并发场景。

典型能力：

- 使用 `vllm serve <model>` 启动 OpenAI 兼容 API 服务。
- 支持高吞吐推理和连续批处理。
- 支持张量并行等多 GPU 推理方式。
- 支持多种量化方式，具体能力取决于模型和硬件。

适用场景：

| 场景 | 说明 |
|---|---|
| GPU 服务 | 多用户共享模型服务 |
| 高并发推理 | 批处理提升吞吐 |
| 私有化部署 | 需要统一 API 网关、鉴权、监控和限流 |
| 离线批处理 | 大量生成或抽取任务 |

vLLM 的运维复杂度高于 Ollama。进入生产前要补齐鉴权、网络隔离、限流、观测、模型热更新策略和容量压测。

### 5.3 llama.cpp

llama.cpp 适合 GGUF 量化模型、CPU 推理和边缘设备。

典型能力：

- 使用 GGUF 模型文件。
- 支持 F16 和多种量化模型。
- `llama-server` 提供 HTTP 服务、Web UI 和 OpenAI 兼容接口。
- 可在 CPU、Apple Silicon、CUDA、Metal 等多种后端上运行，具体效果取决于构建和硬件。

适用场景：

| 场景 | 说明 |
|---|---|
| CPU / 边缘 | 没有独立 GPU 或部署资源有限 |
| 离线应用 | 网络不可用或必须本机运行 |
| 量化实验 | 比较不同 GGUF 量化等级的质量和速度 |
| 桌面工具 | 内部工具或个人知识库 |

llama.cpp 的优势是轻量和可移植。模型质量、上下文长度、吞吐和延迟要用同一套评测集实际测量。

## 6. Java 后端接入方式

保留统一模型 Provider：

```java
public interface ChatModelProvider {
    ChatResult chat(ChatRequest request);
    Stream<ChatChunk> stream(ChatRequest request);
}
```

不同模型服务只做 Provider 实现：

| Provider | 接入方式 |
|---|---|
| 云模型 | 供应商 SDK 或 OpenAI 兼容接口 |
| Ollama | Ollama API 或 OpenAI 兼容接口 |
| vLLM | OpenAI 兼容接口 |
| llama.cpp | `llama-server` OpenAI 兼容接口 |

Provider 返回结果仍要写入阶段 6/7 的记录：

- `traceId`
- `feature`
- `provider`
- `model`
- `promptVersion`
- `inputTokens`
- `outputTokens`
- `latencyMs`
- `errorCode`
- `evalRunId`

本地模型请求也要经过阶段 0 的数据边界检查。模型在本机运行并不自动满足合规要求，仍要管理日志、缓存、模型文件来源、网络暴露面和访问权限。

## 7. 本地模型实验数据模型

数据库字段使用 `snake_case`。

### 7.1 local_model_catalog

| 字段 | 说明 |
|---|---|
| `id` | 模型记录 id |
| `provider` | `ollama`、`vllm`、`llama_cpp` |
| `model` | 模型名称或路径 |
| `model_family` | Qwen、Llama、Mistral 等 |
| `parameter_size` | 参数规模 |
| `quantization` | 量化方式，可为空 |
| `context_window` | 上下文长度 |
| `embedding_supported` | 是否支持 embedding |
| `tool_calling_supported` | 是否支持工具调用 |
| `license_ref` | 许可证引用 |
| `enabled` | 是否启用 |

### 7.2 model_benchmark_run

| 字段 | 说明 |
|---|---|
| `id` | runId |
| `model_catalog_id` | 模型记录 |
| `dataset_id` | 阶段 6 评测集 |
| `dataset_version` | 数据集版本 |
| `feature` | 能力类型 |
| `hardware_profile` | CPU、GPU、内存、显存摘要 |
| `runtime_config` | 上下文、温度、批大小等配置摘要 |
| `metrics_json` | 质量、延迟、吞吐、错误率 |
| `started_at` | 开始时间 |
| `finished_at` | 完成时间 |

### 7.3 model_benchmark_case_result

| 字段 | 说明 |
|---|---|
| `id` | 结果 id |
| `run_id` | benchmark run |
| `case_id` | 评测样例 |
| `passed` | 是否通过 |
| `score` | 分数 |
| `latency_ms` | 延迟 |
| `input_tokens` | 输入 token |
| `output_tokens` | 输出 token |
| `error_code` | 错误码 |
| `actual_ref` | 输出摘要引用 |

## 8. 评测方法

本地模型比较必须使用阶段 6 的固定评测集。

建议至少比较：

| 维度 | 说明 |
|---|---|
| 正确性 | 是否覆盖答案点、字段、工具选择或 Agent step |
| 格式稳定性 | JSON 是否可解析，schema 是否符合 |
| 拒答能力 | 无依据、越权、敏感问题是否拒答 |
| RAG 效果 | embedding 和 rerank 组合是否召回正确 chunk |
| 延迟 | 平均、P95、P99 |
| 吞吐 | 每秒请求数或每秒 token |
| 硬件占用 | CPU、内存、GPU、显存 |
| 成本 | 云模型费用或本地硬件折算成本 |

同一个模型至少跑三类样例：

- 简单聊天和分类。
- 结构化输出。
- RAG 问答。

如果要用于 Tool Calling 或 Agent，需要额外跑阶段 4 和阶段 5 的工具选择、确认、重试和安全样例。

## 9. 什么时候考虑本地模型

优先考虑本地模型的场景：

| 场景 | 判断方式 |
|---|---|
| 数据边界严格 | 阶段 0 判定仅可使用私有化或本地模型 |
| 大批量稳定任务 | 本地吞吐能摊薄硬件成本 |
| 低延迟内网应用 | 网络往返成为主要瓶颈 |
| 离线环境 | 运行环境仅访问本地模型服务 |
| 可控模型版本 | 需要固定模型和推理参数 |

继续使用云模型的常见原因：

| 场景 | 判断方式 |
|---|---|
| 质量差距明显 | 本地模型评测未达通过阈值 |
| 运维成本过高 | GPU、部署、监控和升级成本超过收益 |
| 峰值流量不稳定 | 自建容量利用率过低 |
| 多模态或复杂推理 | 云模型能力明显领先 |
| 合规已有供应商方案 | 企业已完成供应商评审和数据协议 |

最终选择看评测数据、数据边界、成本和运维能力。本机启动成功只代表具备候选资格。

## 10. 微调、LoRA 和 QLoRA

微调用于让模型稳定学习任务模式、输出风格或分类边界。知识类内容优先放入 RAG，业务动作优先通过 Tool Calling 接入后端服务。

### 10.1 微调前置条件

考虑微调前，先确认：

- 已经跑过 Prompt 优化。
- 结构化输出已用 schema 和 DTO 校验约束。
- RAG 的文档质量、切分、embedding、rerank 已调整。
- 工具调用已经补齐业务接口。
- 阶段 6 评测集能稳定衡量效果。
- 有足够高质量、可授权使用的训练样本。
- 有训练、评测、回滚和模型版本管理流程。

### 10.2 LoRA

LoRA 的核心思路是冻结基础模型权重，只训练少量低秩适配器参数。它能降低训练成本，也方便为不同任务保留多个轻量适配器。

工程上要关注：

| 项目 | 说明 |
|---|---|
| 基座模型 | 基座能力决定上限 |
| 数据质量 | 小而准的数据通常比大量噪声数据更有价值 |
| 评测集 | 微调前后必须用同一套数据对比 |
| 合并策略 | 适配器可以单独加载，也可以合并到基座模型 |
| 版本管理 | 记录基座模型、adapter、训练数据和超参 |

### 10.3 QLoRA

QLoRA 把量化和 LoRA 结合起来，常见做法是在 4-bit 量化基础模型上训练 LoRA 适配器，从而降低显存需求。

学习阶段只需要理解它的用途和限制：

- 训练通常使用 Python 生态，例如 Transformers、PEFT、bitsandbytes。
- Java 后端负责调用训练后的推理服务，不承担训练流程。
- 量化会改变精度和资源占用，必须重新跑评测。
- 微调样本涉及真实数据时，要遵守阶段 0 的数据分级和授权要求。

## 11. 量化和推理成本

量化的目标是减少模型权重占用，让模型能在更小显存或内存中运行。

| 量化方向 | 影响 |
|---|---|
| 8-bit | 通常比 4-bit 更稳，节省一部分显存 |
| 4-bit | 显存占用更低，质量波动需要评测 |
| GGUF 量化 | 常用于 llama.cpp |
| GPTQ / AWQ | 常用于 GPU 推理生态 |
| KV cache 量化 | 降低长上下文和并发下的缓存占用 |

成本评估需要覆盖模型单次运行和配套资源。至少要算：

- 模型加载时间。
- 单请求延迟。
- 并发吞吐。
- 显存和内存占用。
- GPU 利用率。
- 运维人力。
- 失败重试带来的额外成本。
- 评测和回滚成本。

## 12. 安全和运维边界

本地推理服务要按生产服务管理：

- 默认只监听本机或内网地址。
- 暴露给其他系统前先接 API 网关、鉴权、限流和审计。
- 模型文件来源要登记，记录许可证和 hash。
- 不在普通日志保存完整 Prompt、RAG 原文和隐私字段。
- 本地 embedding 和向量库也要遵守租户和权限隔离。
- 接入工具调用前，仍按阶段 4 执行白名单、权限、确认和审计。
- 用于 Agent 前，仍按阶段 5 执行状态机、重试和人工确认。

本地部署减少了外部调用链路，但没有消除 Prompt Injection、越权、敏感信息泄露和成本攻击。

## 13. 实验步骤

建议按这个顺序完成：

1. 用 Ollama 启动一个聊天模型。
2. 用 Java 后端通过 OpenAI 兼容接口调用本地模型。
3. 用阶段 2 的结构化输出样例测试 JSON 稳定性。
4. 用本地 embedding 模型跑阶段 3 的 RAG 评测。
5. 用同一批样例比较云模型和本地模型。
6. 记录延迟、token、硬件占用和错误码。
7. 尝试用 vLLM 或 llama.cpp 提供同样的 OpenAI 兼容接口。
8. 更新阶段 7 的模型路由，让本地模型成为一个受控路由选项。

每一步都保留 `traceId` 和 `model`，便于在阶段 6/7 的评测和成本记录中对比。

## 14. 验收清单

- Ollama 模型能被 Java 后端调用。
- 本地模型 Provider 和云模型 Provider 使用同一接口。
- 至少一个本地聊天模型和一个 embedding 模型进入 `local_model_catalog`。
- 同一套评测集能比较云模型和本地模型。
- RAG 评测能显示本地 embedding 的召回效果。
- 记录了延迟、token、硬件占用和错误码。
- 能说明 Ollama、vLLM、llama.cpp 的适用场景。
- 能说明 LoRA、QLoRA 和量化的基本边界。
- 本地推理服务没有裸露到不受控网络。
- 本地模型接入仍遵守阶段 0 到阶段 7 的数据、安全、评测和治理要求。

完成阶段 8 后，你不需要成为模型训练工程师，但应该能和模型、算法、平台团队讨论本地推理方案，并把模型服务纳入 Java 后端的统一工程治理。
