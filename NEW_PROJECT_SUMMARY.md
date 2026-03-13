# NEW_PROJECT_SUMMARY

## 项目定位

这是一个面向 ToB 招聘场景的智能招聘与面试辅助 Agent 后端系统。

目标不是做单点搜索，而是覆盖完整招聘工作流：

- 简历摄入
- 候选人检索
- 多轮 refinement
- 候选人对比
- 面试题生成
- Chat / SSE 统一交互

## 当前已落地的主链

### 1. 数据与索引

- MySQL：业务主数据
- Elasticsearch：检索索引与证据索引

核心索引：

- `resume_chunk`
- `candidate_profile`

### 2. 摄入链

- PDF 上传
- PDFBox 文本提取
- OCR fallback
- 文本清洗
- Parent-Child Chunking
- Candidate Profile 抽取
- MySQL + ES 双写

### 3. 搜索链

- 候选人级关键词召回
- `candidate_profile` 向量召回
- `resume_chunk` 向量召回
- hybrid retrieval 融合
- 证据级 Chunk 召回
- 自然语言 filter parsing
- refinement merge
- candidate scope
- rerank

### 4. Agent 链

- Router
- State
- Tool Calling
- Orchestrator
- Chat API
- SSE

### 5. 业务能力

- 搜索
- refinement
- compare
- interview

## 当前本地模型方案

### LLM

- Qwen3 API
- 当前项目里仍以 Spring AI Tool Calling 作为上层编排入口

### OCR

- PaddleOCR 3.0
- 本地服务目录：
  - [python/ocr-service/README.md](/C:/Users/Type-umr/Desktop/recruit-agent/python/ocr-service/README.md)

### Embedding

- BGE-M3
- 本地服务目录：
  - [python/embedding-service/README.md](/C:/Users/Type-umr/Desktop/recruit-agent/python/embedding-service/README.md)

### Rerank

- `bge-reranker-v2-m3`
- 本地服务目录：
  - [python/rerank-service/README.md](/C:/Users/Type-umr/Desktop/recruit-agent/python/rerank-service/README.md)

## 当前阶段判断

项目已经完成 MVP 主链，不再处于“从 0 到 1 的纯规划阶段”。

更准确地说，当前处于：

- 核心链路已闭环
- 中文本地模型栈已接入并打通
- 开发重点开始从“有能力”转向“提质量、稳体验”

## 当前最重要的工程结论

- 不要再继续横向堆模块
- 应优先做质量增强和策略收口
- 文档、运行方式、模型服务协议必须保持同步

## 下一阶段建议

建议顺序：

1. 做真实样本联调，验证 hybrid + rerank 效果
2. 增强 query understanding
3. 增强 router 智能度
4. 再接 Qwen3 API 到 summary / compare / interview / router
5. 最后再考虑 observability / eval

## 一句话总结

这个项目现在已经是一个具备真实本地 OCR、embedding、rerank 能力的招聘 Agent 后端 MVP，下一阶段重点不是继续加模块，而是提高检索与编排质量。
