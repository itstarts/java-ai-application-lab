# 阶段 2：Prompt 与结构化输出实战

> 本文档的工程权威版本位于 `java-ai-application-lab` 仓库。个人知识库或飞书中的副本仅用于阅读和导入。

本文对应学习路线中的阶段 2，目标是让模型输出可以被 Java 后端稳定消费。

阶段 1 解决了“能调用模型并返回自然语言”。阶段 2 要解决的是“模型输出能进入程序流程”。很多 AI 应用需要模型返回 JSON、枚举、标签、列表、评分、风险项或字段抽取结果，然后由后端继续校验、存储、展示或触发后续流程。

## 1. 阶段目标

完成一个简历信息抽取器，并沉淀一套结构化输出工程方法：

- 能设计清晰的 System Prompt、User Prompt 和上下文 Prompt。
- 能让模型输出 JSON。
- 能把模型输出映射为 Java DTO。
- 能使用 Jackson 和 Jakarta Validation 做后端校验。
- 能处理 JSON 格式错误、字段缺失、类型错误和枚举越界。
- 能建立样例测试集和 Prompt 回归流程。
- 能区分模型抽取结果、原始文本和人工确认结果。

阶段 2 的重点是稳定性和可验证性。Prompt 只是第一层约束，后端校验、错误处理和测试集才是工程可控的基础。

## 2. 推荐技术组合

| 能力 | 推荐技术 |
|---|---|
| Prompt 编排 | Spring AI ChatClient |
| 结构化输出 | Spring AI Structured Output / `entity(...)` / Output Converter |
| JSON 处理 | Jackson |
| 字段校验 | Jakarta Validation |
| 测试 | JUnit 5 |
| 样例管理 | Markdown / JSONL / CSV |
| 版本管理 | Prompt 文件 + Git |

Spring AI 当前支持通过 ChatClient 将模型结果映射为 Java record/class，也支持通过结构化输出转换器和 JSON Schema 辅助约束输出。学习阶段可以先从 DTO + JSON + 后端校验入手，再逐步使用模型原生 structured output 能力。

## 3. Prompt 分层

一个可维护的 Prompt 通常分成三层：

| 层级 | 作用 | 示例 |
|---|---|---|
| System Prompt | 定义角色、任务、边界、输出格式 | “你是简历信息抽取助手，只输出 JSON” |
| Context Prompt | 放入可供模型参考的资料、字段定义、枚举说明 | 字段含义、输出 schema、示例 |
| User Prompt | 当前用户输入或待处理文本 | 简历原文 |

推荐结构：

```text
System:
你是一个信息抽取助手。请从输入文本中抽取结构化信息。
只输出 JSON，不输出解释。
缺失字段按字段说明处理：普通字段使用 null，数组字段使用空数组，枚举字段使用约定的 UNKNOWN 值。

Schema:
{字段说明和枚举说明}

Examples:
{少量输入输出示例}

User:
{待抽取文本}
```

Prompt 文件建议放在项目目录中，例如：

```text
prompts/
└── resume-extractor/
    ├── system-v1.md
    ├── schema-v1.json
    └── examples-v1.md
```

Prompt 放入版本库后，每次修改都能追踪原因和效果。

## 4. 输出 DTO 设计

以简历抽取为例，DTO 可以这样设计：

```java
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record ResumeExtractResult(
    @Size(max = 50)
    String name,

    @Min(0)
    @Max(60)
    Integer yearsOfExperience,

    @Size(max = 50)
    List<@Size(max = 50) String> skills,

    @Size(max = 100)
    String lastCompany,

    EducationLevel education,

    @Size(max = 20)
    List<@Size(max = 200) String> riskNotes
) {}

public enum EducationLevel {
    HIGH_SCHOOL,
    JUNIOR_COLLEGE,
    BACHELOR,
    MASTER,
    DOCTOR,
    UNKNOWN
}
```

设计 DTO 时遵循这些规则：

- 字段数量先少后多。
- 枚举值要明确。
- 缺失信息用 `null`、`UNKNOWN` 或空数组表达。
- 数字字段保持数字类型。
- 列表字段保持数组。
- 风险和不确定性单独放字段。

示例 JSON：

```json
{
  "name": "张三",
  "yearsOfExperience": 5,
  "skills": ["Java", "Spring Boot", "MySQL"],
  "lastCompany": "某科技公司",
  "education": "BACHELOR",
  "riskNotes": []
}
```

## 5. Schema 与字段说明

模型更容易遵守清晰的字段说明。

建议为每个字段写清楚：

| 字段 | 类型 | 规则 |
|---|---|---|
| `name` | string/null | 只抽取候选人姓名，无法判断时为 null |
| `yearsOfExperience` | integer/null | 只返回数字，无法判断时为 null |
| `skills` | string[] | 技术栈数组，去重后返回 |
| `lastCompany` | string/null | 最近一家公司，无法判断时为 null |
| `education` | enum | 只能取给定枚举，无法判断时返回 UNKNOWN |
| `riskNotes` | string[] | 记录不确定、冲突、明显缺失的信息 |

如果模型供应商支持原生 JSON Schema，可以把 Java DTO 或 JSON Schema 传给模型作为响应格式约束。若使用 Spring AI，可关注 `entity(...)`、Structured Output Converter、BeanOutputConverter 等能力；具体 API 以项目使用的 Spring AI 版本为准。

结构化抽取建议使用低 temperature，通常设置为 `0` 或接近 `0`。这样同一输入的输出更稳定，JSON 合法率、字段准确率和回归评测结果才有可比性。调整 temperature 后需要重新跑测试集。

## 6. 后端校验

结构化输出需要 Prompt 约束和后端校验共同保证。

至少做三层校验：

1. JSON 可解析。
2. DTO 可反序列化。
3. 字段满足业务规则。

示例规则：

| 字段 | 校验 |
|---|---|
| `yearsOfExperience` | 0-60 之间 |
| `skills` | 最多 50 个，每个长度不超过 50 |
| `education` | 强类型枚举在 Jackson 反序列化阶段校验；无法判断时使用 UNKNOWN |
| `riskNotes` | 最多 20 条 |

模型返回内容有两条常见处理路径。

使用 Spring AI `entity(...)`、BeanOutputConverter 或模型原生 structured output 时：

```text
模型响应
-> Spring AI / Output Converter 转换为 Java 对象
-> 捕获转换异常
-> Jakarta Validation
-> 业务规则校验
-> 保存结果和原始输出引用
```

自管 ChatClient 文本输出时：

```text
模型原始输出
-> 提取 JSON
-> Jackson 反序列化
-> Jakarta Validation
-> 业务规则校验
-> 保存结果和原始输出引用
```

这两条路径保持单一。使用 `entity(...)` 或 Output Converter 时，JSON 提取和对象映射通常由转换器完成；只有直接拿到模型文本时，才需要自己提取 JSON 并调用 Jackson。

Bean Validation 需要显式触发。反序列化成功不代表字段已经校验。可以在 Service 中注入 `jakarta.validation.Validator`：

```java
Set<ConstraintViolation<ResumeExtractResult>> violations = validator.validate(result);
if (!violations.isEmpty()) {
    throw new StructuredOutputValidationException(violations);
}
```

枚举字段要提前设计失败策略。使用强类型 enum 时，非法枚举值通常在 Jackson 反序列化阶段失败，适合归入 DTO 反序列化失败并按重试策略处理。若希望把未知值映射为 `UNKNOWN`，需要显式配置 Jackson，例如使用默认枚举值策略；或者先用 `String education` 接收，再在业务层校验和归一化。

默认枚举值策略要谨慎使用。它会把模型输出的非法枚举、拼写错误或不在枚举表中的真实值静默归并为 `UNKNOWN`，从而丢失“模型输出非法值”的故障信号。只有业务明确接受“无法判断即缺失”的语义时才使用；更保守的做法是让强类型 enum 反序列化失败，然后进入有限重试或失败状态。

校验失败时保留异常和错误结果，返回明确状态，例如 `EXTRACT_FAILED`、`JSON_INVALID`、`FIELD_VALIDATION_FAILED`。

## 7. 重试策略

结构化输出失败时可以重试，但要限制次数和原因。

适合重试的场景：

- JSON 少了右括号。
- 多输出了说明文字。
- 字段名轻微偏离。
- 枚举大小写错误。

不适合盲目重试的场景：

- 输入文本缺少信息。
- 模型抽取结果与原文冲突。
- 数据涉及敏感边界。
- 连续两次结构化失败。

推荐策略：

| 失败类型 | 处理 |
|---|---|
| JSON 解析失败 | 最多重试 1 次，附带错误原因 |
| DTO 反序列化失败 | 最多重试 1 次，强调字段类型 |
| 字段业务校验失败 | 记录错误，进入人工复核 |
| 关键字段缺失 | 返回缺失状态，不强行编造 |

重试 Prompt 可以包含：

```text
上一次输出无法解析为合法 JSON。
错误原因：{error}
请只重新输出符合 schema 的 JSON。
只输出 JSON。
```

## 8. 可信度与人工复核

模型抽取结果是候选结果，事实来源仍是原始输入和业务系统记录。

建议保存三类信息：

| 信息 | 用途 |
|---|---|
| 原始输入引用 | 追溯来源 |
| 模型原始输出 | 排查格式和模型问题 |
| 校验后的 DTO | 供业务系统读取 |

涉及招聘、合同、财务、风控等场景时，抽取结果进入业务流程前需要人工复核或规则校验。

可增加字段：

```json
{
  "confidence": "MEDIUM",
  "riskNotes": ["工作年限由多段经历推算，可能不准确"],
  "needsReview": true
}
```

`confidence` 适合作为参考信号，后端校验和人工判断仍然保留。

## 9. 测试集设计

阶段 2 至少准备 20 份样例。

| 类型 | 数量 | 目的 |
|---|---:|---|
| 标准简历 | 6 | 验证基本字段 |
| 信息缺失 | 4 | 验证 null 和空数组 |
| 格式混乱 | 4 | 验证鲁棒性 |
| 多公司经历 | 3 | 验证最近公司和年限 |
| 技能很多 | 2 | 验证数组和去重 |
| 干扰文本 | 1 | 验证边界 |

样例格式可以用 JSONL：

```jsonl
{"id":"case-001","input":"...简历文本...","expected":{"education":"BACHELOR","skills":["Java"]}}
{"id":"case-002","input":"...简历文本...","expected":{"education":"UNKNOWN","skills":[]}}
```

评测集遵守阶段 0 数据边界。学习阶段优先使用模拟简历，避免真实姓名、手机号、邮箱、学校、公司和项目经历进入仓库或外部模型。

## 10. 回归评测

每次修改下面内容后，都应跑一遍回归：

- System Prompt。
- 字段说明。
- Few-shot 示例。
- 模型名称。
- temperature。
- JSON Schema。
- DTO 字段。

建议统计这些指标：

| 指标 | 说明 |
|---|---|
| JSON 合法率 | 输出能被解析的比例 |
| DTO 映射成功率 | 能反序列化为 DTO 的比例 |
| 字段准确率 | 关键字段是否符合 expected |
| 缺失处理准确率 | 不知道时是否返回 null/UNKNOWN |
| 重试率 | 首次失败后需要重试的比例 |
| 人工复核率 | 需要人工确认的比例 |

结构化输出的评估重点是能否被程序稳定解析和校验。

## 11. 常见失败与处理

| 失败 | 原因 | 处理 |
|---|---|---|
| 输出 Markdown 包裹 JSON | Prompt 没强调只输出 JSON | 在系统提示词中明确禁止额外文字，并做 JSON 提取 |
| 数字变自然语言 | 字段说明不清 | 指定 integer/null |
| 枚举值乱写 | 枚举约束弱 | Prompt 中列出所有枚举，后端校验 |
| 缺失字段被编造 | 未说明缺失规则 | 明确无法判断时返回 null/UNKNOWN |
| 长文本抽取不全 | 输入太长或结构混乱 | 分段抽取后合并，或先做文本清洗 |
| 校验失败仍入库 | 缺少后端状态 | 引入 FAILED/NEEDS_REVIEW 状态 |

## 12. 验收清单

| 检查项 | 状态 |
|---|---|
| Prompt 文件独立管理 |  |
| DTO 和枚举已定义 |  |
| JSON 能被 Jackson 反序列化 |  |
| 字段有 Jakarta Validation 或业务校验 |  |
| JSON 解析失败有明确错误码 |  |
| 重试次数有限制 |  |
| 样例测试集不少于 20 条 |  |
| 修改 Prompt 后能跑回归 |  |
| 真实敏感数据未进入测试集 |  |
| 结果进入业务前有复核或校验策略 |  |

## 13. 后续演进

阶段 2 做完后，后续能力会自然接上：

- 阶段 3：把结构化输出用于 RAG 的引用、答案来源和拒答原因。
- 阶段 4：把结构化输出用于 Tool Calling 参数候选。
- 阶段 5：把结构化输出用于 Agent 的计划、步骤和状态。
- 阶段 6：把结构化输出纳入评测和回归体系。

阶段 2 的核心产物是一套“Prompt + DTO + 校验 + 测试集”的闭环。只要模型输出能稳定进入 Java 类型系统，后续 RAG、工具调用和 Agent 编排就会更可靠。
