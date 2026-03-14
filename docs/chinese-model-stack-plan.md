# 中文模型能力接入方案

## 目标

基于当前 `recruit-agent` 的后端主链，在不破坏既有确定性执行链的前提下，逐步接入以下模型能力：

- `LLM`：Qwen3 API
- `Embedding`：BGE-M3，本地部署
- `Rerank`：`bge-reranker-v2-m3`，本地部署
- `OCR`：PaddleOCR 3.0，本地部署

这套方案的核心原则：

- 中文场景优先
- 成本优先可控
- 模型负责理解、生成、排序辅助
- 代码负责状态、规则、candidateId 最终决策
- 不一次性并行接四条链，按阶段落地

## 当前系统现状

当前代码已经具备以下基础：

- 简历上传、PDF 解析、OCR fallback 骨架
- `ResumeChunk` / `CandidateProfileIndex` 索引链
- Elasticsearch 检索、refinement、多轮 chat
- compare / interview tool 主链
- Spring AI Tool Calling 基础

当前仍缺的模型能力：

- 真实 OCR
- 向量 embedding 生成与向量召回
- rerank 精排
- 更强的 LLM 生成与复杂语义理解

## 推荐接入顺序

推荐顺序：

1. `OCR`
2. `Embedding`
3. `Rerank`
4. `LLM`

原因：

- `OCR` 是当前摄入链的真实缺口，优先补齐数据入口
- `Embedding` 是后续 hybrid retrieval 的底座
- `Rerank` 建立在召回之上，适合在已有搜索主链稳定后接入
- `LLM` 虽然收益高，但当前系统已经有确定性 Agent 主链，最后接入更稳

如果以 demo 效果为先，也可以把 `LLM` 提前到第一位，但不建议在 OCR 和检索底座缺失的情况下过度依赖 LLM。

## 总体架构原则

所有模型能力都必须先抽象 provider 层，不允许直接把模型 SDK 写进业务服务。

建议新增四类抽象：

- `OcrService`
- `EmbeddingService`
- `RerankService`
- `LlmGenerationService`

这样后续即使替换模型，也不需要改：

- `resume`
- `rag`
- `search`
- `agent`
- `chat`

这些业务模块的核心逻辑。

## 一、OCR 接入方案

### 目标

替换当前占位实现 `NoOpOcrEngine`，使扫描版 PDF、图片简历、弱文本 PDF 可以进入既有摄入链。

### 建议接口

```java
public interface OcrService {
    OcrResult recognize(byte[] content, String filename, String contentType);
}
```

### 建议模块位置

- `src/main/java/com/recruit/agent/resume/parser/ocr`

建议新增：

- `OcrService`
- `PaddleOcrServiceImpl`
- `OcrResult`
- `OcrClient`

### 接入方式

建议将 PaddleOCR 作为本地独立服务运行，Java 后端通过 HTTP 调用，而不是直接在 JVM 内嵌 Python。

原因：

- 降低 Java 与 Python 混部复杂度
- 便于本地调试和后续部署拆分
- 便于做超时、重试、熔断

### 调用时机

沿用当前链路：

- 先走 PDFBox
- 由 detector 判断是否需要 OCR fallback
- 再调用 `OcrService`

### 验证样本

必须优先准备真实中文简历样本：

- 扫描版 PDF
- 双栏 PDF
- 中英混排简历
- 含表格/时间轴/图标的简历

### 风险

- OCR 通了不等于结构好，版面顺序可能仍乱
- 不能直接假设 OCR 输出适合 chunking，需要观察清洗后文本质量

## 二、Embedding 接入方案

### 目标

让 `ResumeChunk` 和 query 具备向量表示，为后续 hybrid retrieval 提供基础。

### 模型

- `BGE-M3`

### 建议接口

```java
public interface EmbeddingService {
    List<List<Float>> embedDocuments(List<String> texts);
    List<Float> embedQuery(String query);
}
```

### 建议模块位置

- `src/main/java/com/recruit/agent/rag/embedding`

建议新增：

- `EmbeddingService`
- `BgeM3EmbeddingServiceImpl`
- `EmbeddingClient`

### 接入点

1. 简历摄入后，为 `ResumeChunk` 生成 embedding
2. 写入 Elasticsearch 向量字段
3. 搜索时对 query 生成 embedding
4. 执行 topK 向量召回

### 落地策略

第一阶段只处理 `ResumeChunk`，不要一开始就给 `CandidateProfileIndex` 也上向量。

原因：

- `ResumeChunk` 是最直接的证据载体
- 更适合做语义召回
- 对 compare / interview 证据质量提升更直接

### 注意点

- embedding 生成要支持批量
- 要有失败降级策略
- 索引重建和增量更新策略必须先设计好

## 三、Rerank 接入方案

### 目标

提升搜索结果排序质量，但不重构当前主检索。

### 模型

- `bge-reranker-v2-m3`

### 建议接口

```java
public interface RerankService {
    List<RerankItem> rerank(String query, List<RerankDocument> documents);
}
```

### 建议模块位置

- `src/main/java/com/recruit/agent/search/rerank`

建议新增：

- `RerankService`
- `BgeRerankServiceImpl`
- `RerankDocument`
- `RerankItem`

### 推荐接法

不要上来就改候选人主检索逻辑，先做后置精排：

1. 先用现有 ES 检索召回 candidate 或 chunk
2. 对 topK 结果调用 rerank
3. 用 rerank 分数重排

### 推荐优先级

先 rerank `ResumeChunk`，再聚合 candidate。

原因：

- chunk 是细粒度证据
- 更容易体现语义差异
- 后续 compare / interview 也能复用更准的证据

### 风险

- rerank 不能替代召回
- topK 设太大成本会上升
- 如果候选集本身召回不全，rerank 无法补救

## 四、LLM 接入方案

### 目标

在保留当前确定性 tool execution 的前提下，用 Qwen3 API 提升理解和生成质量。

### 模型用途

优先用于：

- chat summary
- compare 总结
- interview 题目润色
- router 辅助理解
- 复杂自然语言解析的受限 JSON 输出

不建议直接用于：

- 直接决定 `candidateId`
- 直接跳过 search / refinement / selection 的确定性执行
- 直接生成不可审计的对比结论

### 建议接口

如果不完全依赖 Spring AI，建议加一层业务抽象：

```java
public interface LlmGenerationService {
    String generateText(LlmPrompt prompt);
    <T> T generateStructured(LlmPrompt prompt, Class<T> responseType);
}
```

### 建议模块位置

- `src/main/java/com/recruit/agent/llm`

### 与现有 Spring AI 的关系

当前已经有 Spring AI Tool Calling，可以继续保留。

推荐策略：

- Tool Calling 继续由 Spring AI 负责
- 业务摘要、compare 总结、interview 题目润色通过 `LlmGenerationService` 统一封装

这样不会让所有模型调用都绑死在 chat 流里。

## 分阶段实施计划

### Phase 1：OCR

目标：

- 打通真实 OCR fallback

改造范围：

- `resume/parser/ocr`
- `resume/parser`
- 配置文件

完成标准：

- 扫描版简历可解析
- 进入现有 chunk / profile / index 主链

### Phase 2：Embedding

目标：

- 为 `ResumeChunk` 建立向量表示

改造范围：

- `rag`
- `resume ingestion`
- Elasticsearch mapping

完成标准：

- chunk embedding 可生成
- query embedding 可生成
- 支持基础向量召回

### Phase 3：Rerank

目标：

- 提升 topK 排序质量

改造范围：

- `search`
- `comparison`
- `interview`

完成标准：

- 搜索结果在现有 ES 召回基础上支持 rerank
- compare / interview 证据质量可见提升

### Phase 4：LLM

目标：

- 提升 Agent 生成与理解质量

改造范围：

- `agent`
- `chat`
- `comparison`
- `interview`

完成标准：

- compare summary 更自然
- interview 题目更像真实面试官提问
- chat summary 质量提升

## 配置建议

建议新增统一模型配置前缀：

- `app.model.ocr.*`
- `app.model.embedding.*`
- `app.model.rerank.*`
- `app.model.llm.*`

建议不要把模型 URL、token、batch size、timeout 散落在各模块。

## 边界约束

必须坚持以下边界：

- `candidateId` 的最终确定必须走代码逻辑
- LLM 只输出文本或受限结构，不直接控制状态
- rerank 只重排已有结果，不负责全量召回
- embedding 先服务 `ResumeChunk`，不要一开始铺满全域索引
- OCR 服务失败时，摄入链要有明确失败状态和日志

## 当前最推荐的下一步

如果要正式开始实施，建议从 `OCR` 开始。

原因：

- 当前真实缺口最明确
- 改造面最集中
- 对简历摄入链收益最直接
- 不会牵动 chat / agent 主链

推荐实施顺序：

1. 先把 `OcrService` provider 抽象建出来
2. 用 PaddleOCR 本地服务替换 `NoOpOcrEngine`
3. 用真实中文简历样本验证解析质量
4. 再开始 embedding / rerank

