# NEW_PROJECT_SUMMARY

## 项目定位

这是一个面向招聘场景的智能简历检索与候选人筛选后端系统。

目标不是做一个简单的关键词搜索接口，而是覆盖一条更完整的招聘工作流：
- 简历摄入
- 候选人搜索
- 多轮 refinement
- 候选人对比
- 面试交接提纲
- chat 统一交互

## 当前已经完成的主链

### 1. 数据与索引
- MySQL：业务主数据
- Elasticsearch：检索与证据索引

核心索引：
- `resume_chunk`
- `candidate_profile`

### 2. 摄入链
- PDF 上传
- PDFBox 文本提取
- OCR fallback / OCR 主路径
- 文本清洗
- 结构化切片
- Candidate Profile 抽取
- MySQL + Elasticsearch 双写

### 3. 搜索链
- 关键词召回
- `candidate_profile` 向量召回
- `resume_chunk` 向量召回
- hybrid retrieval 融合
- evidence chunk 召回
- 自然语言 filter parsing
- refinement merge
- rerank

### 4. Agent 链
- Router
- State
- Tool Calling
- Orchestrator
- Chat API
- SSE

### 5. 业务能力
- search
- refinement
- compare
- interview handoff brief

## 当前本地模型栈

### OCR
- PaddleOCR

### Embedding
- BGE-M3

### Rerank
- `bge-reranker-v2-m3`

### LLM
- Qwen / Spring AI tool orchestration

## 当前阶段判断

项目已经完成从 0 到 1 的主能力建设，当前不再是“功能缺失”的阶段，而是“质量收口与讲述固化”的阶段。

更准确地说，当前状态是：
- 核心链路已闭环
- 本地 OCR / embedding / rerank 已接通
- 搜索、对比、岗位上下文 chat、面试交接提纲都已跑通
- 后续重点应该从“继续堆功能”转向“提高稳定性、可讲性和演示质量”

## 当前最重要的工程结论

- 不建议继续横向扩功能
- 应优先做 demo 固化、面试讲法和少量文档清理
- 文档、运行方式、样本与模型服务协议要保持同步

## 推荐讲法

一句话版本：

这是一个把简历解析、结构化切片、混合检索、重排和岗位上下文结合起来的招聘检索系统，目标是帮助 HR 和用人部门更快找到更匹配的候选人，并给出可解释证据。

## 下一阶段建议

如果后面还要继续做，不建议再新增主能力，优先级应是：
1. 固定 demo 流程
2. 固定 query / chat 话术
3. 准备面试讲稿
4. 视情况补小范围 eval 或 observability
