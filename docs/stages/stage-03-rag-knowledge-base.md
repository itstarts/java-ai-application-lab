# 阶段 3：RAG 知识库设计

> 本文档的工程权威版本位于 `java-ai-application-lab` 仓库。个人知识库或飞书中的副本仅用于阅读和导入。

本文对应学习路线中的阶段 3，目标是把阶段 1 的聊天能力和阶段 2 的结构化输出能力，扩展成一个可以基于知识库资料回答问题的 Java 后端应用。

RAG 的核心价值是让模型回答时引用外部知识。企业场景中，模型本身通常不知道最新制度、内部文档、产品说明和项目资料，RAG 通过“先检索，再回答”的方式，把可追溯资料放入上下文，降低胡编答案的概率。

## 1. 阶段目标

完成一个最小但边界清晰的知识库问答系统：

- 支持上传文档并保存原始文件。
- 支持解析 PDF、Markdown、TXT、DOCX 等常见资料。
- 支持文本清洗、切分、embedding 和向量入库。
- 支持问题检索、候选召回、可选 rerank 和上下文拼装。
- 支持基于检索结果回答，并返回引用来源。
- 支持按知识库、文档状态、用户权限做过滤。
- 支持记录检索过程、命中 chunk、模型调用和回答质量。
- 支持构建一组 RAG 评测问题，持续验证召回和答案。

阶段 3 的验收标准是：上传一批资料后，用户能针对资料提问；回答能给出来源；找不到依据时能明确说明缺少资料；检索日志能解释一次回答用了哪些文档片段。

## 2. 推荐技术组合

| 能力 | 推荐技术 |
|---|---|
| Web 框架 | Spring Boot 3.x |
| Java 版本 | Java 21 |
| AI 抽象 | Spring AI ChatClient、EmbeddingModel、VectorStore、RAG Advisor |
| 备选 AI 抽象 | LangChain4j EmbeddingStore、RetrievalAugmentor |
| 向量库 | PostgreSQL + pgvector |
| 结构化存储 | PostgreSQL |
| 原始文件存储 | 本地文件、MinIO 或 S3 |
| 文档解析 | Apache Tika、PDFBox、docx4j，或 Spring AI Document Reader |
| 切分 | Spring AI TokenTextSplitter，必要时自定义切分器 |
| 异步任务 | Spring Task、Spring Batch 或消息队列 |
| 测试 | JUnit 5、Testcontainers |

学习阶段优先选择 PostgreSQL + pgvector。它和 Java 后端常见技术栈匹配度高，便于同时学习事务、元数据过滤、索引和向量检索。等数据量、并发和检索能力要求上升后，再评估 Milvus、Qdrant、Elasticsearch hybrid search 等专用方案。

Spring AI 当前提供 Document、EmbeddingModel、VectorStore、Document Reader、Text Splitter 和 RAG Advisor 等抽象。具体 API 会随版本演进，项目中应以锁定的 Spring AI 版本文档为准，业务代码尽量依赖自己的应用层接口。

## 3. 推荐模块结构

可以先按下面结构组织：

```text
ai-knowledge-api
├── controller
│   ├── KnowledgeBaseController
│   ├── DocumentController
│   └── KnowledgeChatController
├── application
│   ├── DocumentIngestionService
│   ├── KnowledgeRetrievalService
│   └── KnowledgeAnswerService
├── parser
│   ├── DocumentParser
│   ├── PdfDocumentParser
│   ├── MarkdownDocumentParser
│   └── DocxDocumentParser
├── chunk
│   ├── ChunkingService
│   └── ChunkingProperties
├── embedding
│   ├── EmbeddingService
│   └── EmbeddingModelProperties
├── retrieval
│   ├── Retriever
│   ├── RetrievalQuery
│   ├── RetrievalResult
│   └── RerankService
├── prompt
│   └── RagPromptBuilder
├── persistence
│   ├── KnowledgeBaseRepository
│   ├── DocumentRepository
│   ├── DocumentChunkRepository
│   └── IngestionJobRepository
└── observability
    ├── RagTraceLogger
    └── RetrievalMetricsRecorder
```

关键职责：

- `DocumentIngestionService` 负责编排上传后的入库流程。
- `KnowledgeRetrievalService` 负责召回、过滤和 rerank。
- `KnowledgeAnswerService` 负责拼装 Prompt、调用模型和生成引用。
- `RagPromptBuilder` 只处理上下文和问题的组装。
- Repository 只处理数据读写，不混入模型调用。
- Observability 组件记录可回放的检索链路。

## 4. 核心数据模型

### 4.1 knowledge_base

| 字段 | 说明 |
|---|---|
| id | 知识库 id |
| name | 知识库名称 |
| owner_id | 创建者或所属团队 |
| visibility | PRIVATE / TEAM / PUBLIC |
| status | ACTIVE / ARCHIVED |
| created_at | 创建时间 |
| updated_at | 更新时间 |

### 4.2 document

| 字段 | 说明 |
|---|---|
| id | 文档 id |
| knowledge_base_id | 所属知识库 |
| title | 文档标题 |
| source_type | UPLOAD / URL / MANUAL |
| file_uri | 原始文件位置 |
| content_hash | 原始内容 hash，用于去重和版本判断 |
| parser_type | 解析器类型 |
| status | UPLOADED / PARSING / INDEXING / READY / FAILED |
| error_code | 入库失败错误码 |
| created_by | 上传人 |
| created_at | 创建时间 |
| updated_at | 更新时间 |

### 4.3 document_chunk

| 字段 | 说明 |
|---|---|
| id | chunk id |
| document_id | 所属文档 |
| knowledge_base_id | 所属知识库 |
| chunk_index | 文档内序号 |
| content | 切分后的文本 |
| token_count | 估算 token 数 |
| page_number | 页码，无法识别时为空 |
| heading_path | 标题路径 |
| content_hash | chunk 内容 hash |
| embedding_model | embedding 模型名称 |
| embedding_dimension | 向量维度 |
| vector_id | 向量存储 id |
| status | READY / FAILED |
| created_at | 创建时间 |

如果使用 pgvector，也可以把向量字段直接放在 `document_chunk` 表中，例如 `embedding vector(1024)`。学习阶段推荐先让 chunk 元数据和向量在同一张表中，便于理解和调试。后续如果需要多模型、多索引或冷热分层，可以再拆成 `chunk_embedding` 表。

### 4.4 ingestion_job

| 字段 | 说明 |
|---|---|
| id | 入库任务 id |
| document_id | 文档 id |
| stage | PARSE / CHUNK / EMBED / INDEX |
| status | PENDING / RUNNING / SUCCEEDED / FAILED |
| retry_count | 重试次数 |
| error_message | 简短错误信息 |
| started_at | 开始时间 |
| finished_at | 结束时间 |

入库任务要能重试和排查。文档解析、embedding 调用、向量写入都可能失败，任务状态比单个同步接口更适合承载这类流程。

## 5. 文档入库流程

推荐流程：

```text
上传文件
-> 保存原始文件
-> 创建 document 和 ingestion_job
-> 解析文本
-> 清洗文本
-> 切分 chunk
-> 生成 embedding
-> 写入向量库和 chunk 元数据
-> 标记文档 READY
```

### 5.1 上传与原始文件保存

上传接口只负责接收文件、基础校验和创建入库任务。

需要记录：

- 文件名、大小、MIME 类型。
- 上传人、知识库 id、权限范围。
- 原始文件 URI。
- 内容 hash。
- 当前入库状态。

原始文件建议保留。原因是解析器、切分规则、embedding 模型后续会调整，保留原始文件才能重建索引。

### 5.2 解析与清洗

解析阶段的目标是得到可检索文本和元数据。

建议至少保留：

- 文档标题。
- 页码或段落位置。
- 标题层级。
- 表格文本。
- 图片 OCR 结果，如果项目需要处理扫描件。

清洗要做得克制。优先清理页眉页脚、重复空行、乱码、无意义目录页和版权页；对正文内容保持可追溯，避免为了“干净”而丢掉关键信息。

### 5.3 文本切分

切分质量直接影响召回质量。

推荐先从下面规则开始：

| 项 | 建议 |
|---|---|
| chunk 大小 | 300-800 中文字，或按 token 控制 |
| overlap | 50-150 中文字，避免跨段信息断裂 |
| 切分边界 | 标题、段落、列表项优先 |
| 元数据 | documentId、chunkIndex、pageNumber、headingPath |
| 超长段落 | 按句子或 token 继续拆分 |

切分要兼顾两个目标：

- chunk 足够小，便于检索命中具体证据。
- chunk 足够完整，能让模型理解上下文。

对 FAQ、制度条款、产品说明这类资料，可以按自然条目切分；对长篇 PDF，可以按标题和段落切分后再做 token 限制。

### 5.4 Embedding 与向量写入

embedding 模型负责把文本转换成向量。向量库负责按相似度召回。

需要明确这些边界：

- 同一个向量索引中，embedding 模型和维度必须一致。
- 更换 embedding 模型后，需要对相关 chunk 重新生成向量，并基于新向量重建索引。
- chunk 文本、embedding 模型、模型版本、维度要一起记录。
- 向量写入和 chunk 元数据写入要能保持一致状态。

使用 Spring AI `VectorStore.add(documents)` 一类封装时，框架通常会在写入前调用 embedding 模型生成向量。应用层仍然要记录使用了哪个模型、哪种切分规则和哪次入库任务，方便后续排查。

## 6. 检索问答流程

推荐流程：

```text
用户问题
-> 权限与知识库范围校验
-> 可选问题改写
-> 生成问题 embedding
-> 带知识库、文档状态、权限过滤做向量召回 topK
-> 可选关键词召回合并，同样带过滤条件
-> 可选 rerank
-> 拼装上下文
-> 调用聊天模型
-> 返回答案和引用
-> 记录检索 trace
```

### 6.1 检索接口

可以先实现一个内部检索接口：

```java
public record RetrievalQuery(
    String knowledgeBaseId,
    String userId,
    String question,
    int topK,
    double minSimilarity,
    Map<String, Object> filters
) {}

public record RetrievalResult(
    String chunkId,
    String documentId,
    String title,
    String content,
    Integer pageNumber,
    String headingPath,
    Double similarity,
    Double distance
) {}
```

`topK` 可以从 5 或 8 开始。`minSimilarity` 需要结合当前 embedding 模型、资料类型和问题集调参。

这里要区分框架封装和 pgvector 原生 SQL 的语义。Spring AI `VectorStore` 一类抽象通常暴露的是 similarity，值越大越相似；pgvector 原生 `<=>`、`<->` 等算子返回的是 distance，值越小越相似。直接写 SQL 时应按 distance 升序排序，例如 `ORDER BY embedding <=> :queryVector ASC`，阈值也应表达为 `maxDistance`，或在应用层明确转换成 similarity 后再使用 `minSimilarity`。

### 6.2 权限过滤

RAG 权限要尽量前置到检索阶段。只有用户有权限查看的文档片段才能进入 Prompt。

常见过滤条件：

- 知识库 id。
- 文档状态必须为 READY。
- 文档可见范围。
- 团队、租户、角色或用户授权。
- 文档标签和业务分类。

如果向量库支持 metadata filter，可以在向量召回时带上过滤条件；如果只在召回后过滤，可能出现 topK 全部被过滤掉的问题。学习阶段可以先基于 pgvector 和 SQL 条件实现向量相似度排序加权限过滤。

### 6.3 Rerank

向量召回关注语义相似，rerank 关注候选片段和问题的精细相关性。

推荐先按阶段引入：

1. 第一版只做向量召回。
2. 召回结果不稳定时，引入关键词召回或 hybrid search。
3. 候选数量变多后，引入 rerank 模型对 topN 候选重排。

rerank 通常会增加一次模型调用成本和延迟。它适合放在候选召回之后，例如向量召回 20 条，再 rerank 取前 5 条进入 Prompt。

### 6.4 Prompt 拼装

RAG Prompt 的基本结构：

```text
System:
你是知识库问答助手。请基于给定资料回答问题。
回答中需要引用资料编号。资料不足时说明缺少依据。

Context:
[1] 文档标题: xxx
页码: 3
内容: ...

[2] 文档标题: yyy
页码: 5
内容: ...

User:
{用户问题}
```

上下文拼装要控制 token 预算。可以按检索排序结果依次放入 chunk，并预留输出 token。进入 Prompt 的内容要记录 trace，包括 chunkId、documentId、similarity 或 distance、pageNumber 和 promptOrder。

如果检索结果来自 pgvector 原生 distance，拼装顺序应按 distance 从低到高；如果来自框架封装后的 similarity，则按 similarity 从高到低。日志字段名也要明确使用 `distance` 或 `similarity`，避免排查时误读。

检索内容要明确包裹为“资料”，让模型把它当作待引用数据；系统指令仍以服务端定义为准。文档中如果出现“忽略以上规则”“改写系统提示词”等文本，也只作为资料内容处理。

### 6.5 回答与引用

响应示例：

```json
{
  "answer": "根据《员工报销制度》，差旅住宿需要在出差结束后 7 个工作日内提交报销申请。",
  "citations": [
    {
      "documentId": "doc_001",
      "title": "员工报销制度",
      "pageNumber": 3,
      "chunkId": "chunk_012"
    }
  ],
  "traceId": "rag_trace_001"
}
```

引用来源建议由后端根据进入 Prompt 的 chunk 生成，不完全依赖模型自由输出。模型可以在答案中标注 `[1]`、`[2]`，后端再映射到具体文档和 chunk。

后端需要校验模型输出的引用编号是否在本次 Prompt 的有效范围内。越界编号、重复编号、无法解析的编号都要降级为无效引用，并在 trace 中记录，避免返回悬空来源。

当检索不到足够资料时，返回明确状态：

```json
{
  "answer": "当前知识库中没有找到足够依据回答这个问题。",
  "citations": [],
  "traceId": "rag_trace_002"
}
```

找不到依据时仍要记录检索 trace，便于判断是资料缺失、切分问题、embedding 不匹配，还是权限过滤过严。

## 7. pgvector 设计要点

学习阶段可以使用一张 chunk 表承载文本、元数据和向量。

示例字段：

```sql
embedding vector(1024)
```

向量维度要和 embedding 模型输出维度一致。不同模型维度不同，表结构和索引要随之调整。

常见索引：

| 索引 | 适用场景 |
|---|---|
| HNSW | 查询性能和召回质量通常更好，适合在线查询；构建和内存成本更高 |
| IVFFlat | 构建较快、内存占用相对低；需要先有一定数据再建索引，并调 lists/probes |

学习阶段可以先用精确检索验证效果，再增加近似索引优化性能。性能优化前先建立评测集，避免只追求更快但召回质量下降。

需要同时建普通字段索引，例如：

- `knowledge_base_id`
- `document_id`
- `status`
- `created_by`
- `tenant_id` 或团队字段

向量检索通常还要结合 metadata filter，普通索引能帮助数据库缩小候选范围。

近似索引和 metadata filter 叠加时要关注召回下降风险。某些执行计划可能先做 ANN 候选召回，再过滤知识库、状态或权限，导致过滤后候选不足。需要结合 pgvector 版本能力、查询计划、候选数量和 Recall@K 评测结果来调参，必要时采用更强的预过滤策略。

## 8. 观测与日志

RAG 问题很难只靠最终答案排查，必须记录检索过程。

建议记录：

| 信息 | 用途 |
|---|---|
| traceId | 串联一次问答 |
| questionHash | 避免直接记录敏感问题全文 |
| knowledgeBaseId | 定位知识库 |
| filters | 排查权限和范围 |
| retrievedChunkIds | 查看召回结果 |
| similarity / distance | 判断召回排序和阈值 |
| rerankScores | 判断重排效果 |
| promptTokenEstimate | 控制上下文预算 |
| model | 定位模型版本 |
| latencyMs | 排查性能 |
| errorCode | 排查失败原因 |

日志要遵守阶段 0 的数据边界。生产环境优先记录引用 id、hash、分数、耗时和错误码；真实文档内容、用户问题全文和完整 Prompt 只在明确允许的调试环境中保存，并设置保留期限。

## 9. 评测集设计

RAG 必须用问题集评测。只靠人工随手问几个问题，无法判断改动是否让系统变好。

建议先准备 50 个问题：

| 类型 | 数量 | 目标 |
|---|---:|---|
| 单文档事实题 | 15 | 验证基础召回 |
| 多段综合题 | 10 | 验证上下文组合 |
| 同义表达题 | 10 | 验证 embedding 语义召回 |
| 无答案题 | 10 | 验证拒答 |
| 权限边界题 | 5 | 验证过滤 |

每个问题至少记录：

- question。
- expectedDocumentIds。
- expectedChunkIds，可选。
- expectedAnswerPoints。
- shouldRefuse。
- allowedKnowledgeBaseIds。

核心指标：

| 指标 | 说明 |
|---|---|
| Recall@K | 正确资料是否出现在前 K 个召回结果中 |
| Citation Accuracy | 引用是否对应真实依据 |
| Answer Correctness | 答案是否覆盖关键点 |
| Refusal Accuracy | 无资料时是否拒答 |
| Latency | 检索和模型调用耗时 |
| Cost | embedding、rerank、chat 调用成本 |

阶段 3 初期重点看 Recall@K、引用准确率和拒答准确率。答案质量后续可以结合人工评审和 LLM-as-judge，但最终仍要保留人工抽检。

## 10. 常见失败与定位

| 现象 | 可能原因 | 排查方式 |
|---|---|---|
| 找不到相关资料 | 切分太粗、embedding 不适合中文、权限过滤过严 | 查看 topK、过滤条件和 chunk 内容 |
| 找到资料但回答错误 | Prompt 没约束依据、上下文太长、模型忽略引用 | 查看最终 Prompt 和 citation 映射 |
| 引用不准确 | 完全依赖模型生成引用 | 后端维护 chunk 编号和引用映射 |
| 同类问题表现不稳定 | temperature 偏高、rerank 缺失、问题改写不稳定 | 固定参数并跑评测集 |
| 上传后查询不到 | 入库任务失败、document 未 READY、向量维度不匹配 | 查看 ingestion_job 和 vector 写入日志 |
| 响应慢 | topK 过大、rerank 候选过多、索引缺失 | 分段记录 parse、retrieval、rerank、chat 耗时 |

定位 RAG 问题时，先判断失败发生在哪一层：资料是否存在，chunk 是否合理，检索是否命中，rerank 是否保留，Prompt 是否放入，模型是否按资料回答。

## 11. 数据边界与安全

RAG 会把内部文档变成模型上下文，因此要比普通聊天更重视数据边界。

需要落实：

- 文档入库前确认资料等级和可用范围。
- embedding 调用也是外部模型调用，按阶段 0 的数据分级处理。
- Prompt 只接收用户有权访问的 chunk。
- 把文档内容视为不可信输入，防范间接 Prompt 注入；检索内容只作为资料，不作为指令。
- 检索日志默认记录 id、hash、分数和耗时。
- 对外展示引用时，只展示用户有权访问的文档标题、页码和片段。
- 删除或归档文档后，要同步处理 chunk、向量和缓存。
- 更换 embedding 模型时，重新生成向量并重建索引，不混用旧向量。

如果资料属于阶段 0 定义的 L3 或更高风险数据，先使用模拟数据或脱敏样例完成学习项目；真实业务接入需要结合权限、审计、脱敏、供应商协议和公司安全要求单独设计。

## 12. 推荐实现顺序

按下面顺序推进：

1. 建 knowledge_base、document、document_chunk、ingestion_job 表。
2. 实现文档上传和原始文件保存。
3. 实现 Markdown/TXT 解析，先跑通最简单文档。
4. 增加 PDF/DOCX 解析。
5. 实现切分服务和 chunk 元数据保存。
6. 接入 embedding 模型和 pgvector。
7. 实现向量检索接口。
8. 接入知识库问答 Prompt。
9. 返回引用来源。
10. 增加权限过滤和文档状态过滤。
11. 增加 RAG trace 日志。
12. 建立 50 条评测问题并跑回归。
13. 根据评测结果再调整 chunk、topK、rerank 和 Prompt。

## 13. 最小可行接口

### 13.1 创建知识库

```http
POST /api/knowledge-bases
Content-Type: application/json
```

```json
{
  "name": "AI 应用开发资料库",
  "visibility": "PRIVATE"
}
```

### 13.2 上传文档

```http
POST /api/knowledge-bases/{knowledgeBaseId}/documents
Content-Type: multipart/form-data
```

响应：

```json
{
  "documentId": "doc_001",
  "status": "UPLOADED",
  "ingestionJobId": "job_001"
}
```

### 13.3 查询入库状态

```http
GET /api/documents/{documentId}/ingestion-status
```

```json
{
  "documentId": "doc_001",
  "status": "READY",
  "chunkCount": 42,
  "errorCode": null
}
```

### 13.4 知识库问答

```http
POST /api/knowledge-bases/{knowledgeBaseId}/chat
Content-Type: application/json
```

```json
{
  "question": "阶段 2 为什么要做结构化输出校验？",
  "topK": 5
}
```

## 14. 测试建议

### 14.1 单元测试

- 文档解析器能返回文本和元数据。
- 切分器不会生成空 chunk。
- chunk 序号稳定。
- Prompt Builder 能按 token 预算截断。
- citation 映射能从 `[1]` 映射到正确 chunk。
- citation 映射能识别越界编号和非法编号。
- 权限过滤条件能正确生成。

### 14.2 集成测试

使用 Testcontainers 启动 PostgreSQL + pgvector，验证：

- chunk 和向量能写入。
- 指定知识库能检索到对应 chunk。
- 只有 READY 状态的文档会被召回。
- 无权限文档不会被召回。
- 删除文档后不会继续出现在结果中。

embedding 模型调用可以先用 stub 或固定向量，避免测试依赖外部服务。需要验证真实模型效果时，单独放到手动评测或 nightly 评测中。

### 14.3 评测回归

每次调整这些内容后跑评测集：

- chunk 大小和 overlap。
- embedding 模型。
- topK、minSimilarity 或 maxDistance。
- hybrid search 权重。
- rerank 模型。
- RAG Prompt。

记录每次评测结果，至少包括 Recall@K、引用准确率、拒答准确率、平均耗时和备注。

## 15. 阶段完成标准

完成阶段 3 时，应能做到：

- 能上传至少 3 种格式的文档。
- 能查看文档入库状态和失败原因。
- 能基于知识库资料回答问题。
- 能返回文档标题、页码或 chunk 引用。
- 能在资料不足时拒答。
- 能按知识库和文档状态过滤召回结果。
- 能记录一次问答的检索 trace。
- 能用一组固定问题评估召回和引用质量。
- 能说明 embedding、chunk、topK、rerank、Prompt 对 RAG 效果的影响。

阶段 3 完成后，再进入 Tool Calling 和 Agent 阶段会更稳。RAG 解决“模型参考什么资料”，Tool Calling 解决“模型调用什么能力”，两者结合后才适合做更复杂的业务助手。
