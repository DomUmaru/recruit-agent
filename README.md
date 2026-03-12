# recruit-agent

面向 ToB 招聘场景的智能招聘与面试辅助 Agent 后端系统。

## 当前状态

项目已经完成第一版可运行主链，当前不是“规划中”，而是一个可本地联调的后端 MVP。

当前已经打通的核心链路：

- 简历上传
- PDF 文本解析
- OCR fallback
- Resume Chunk 索引
- Candidate Profile 索引
- 候选人搜索
- refinement 多轮筛选
- 候选人对比
- 面试题生成
- Agent Router / Tool Calling
- Chat API / SSE
- 本地 PaddleOCR
- 本地 BGE-M3 embedding
- 本地 bge-reranker-v2-m3 rerank

## 技术栈

- Java 17
- Spring Boot 3.4.x
- Spring AI
- MySQL 8
- Elasticsearch 8
- PDFBox
- PaddleOCR 3.0
- BGE-M3
- bge-reranker-v2-m3
- SSE

## 模块结构

```text
src/main/java/com/recruit/agent
├─ agent
├─ candidate
├─ chat
├─ common
├─ comparison
├─ interview
├─ position
├─ rag
├─ resume
└─ search

python
├─ ocr-service
├─ embedding-service
└─ rerank-service
```

## 已完成能力

### 1. 简历摄入

- `POST /api/resumes/upload`
- 文件落盘与版本管理
- `resume_document` 状态流转
- PDFBox 文本提取
- OCR fallback 判定与接入
- 文本清洗
- Parent-Child Chunking
- `resume_chunk` 写入 Elasticsearch
- 规则式候选人画像抽取
- `candidate_profile` 写入 Elasticsearch

### 2. 搜索与 refinement

- `POST /api/search/candidates`
- `POST /api/search/candidates/refine`
- 候选人级 ES 检索
- 证据级 Chunk 召回
- 自然语言 filter parsing
- refinement merge
- scope candidate ids
- rerank 精排

当前支持的主要筛选维度：

- 学历
- 学校层级
- 年限
- 技术栈
- 城市
- 大厂
- 外包

### 3. Agent 主链

- Router 场景识别：
  - `SEARCH`
  - `FILTER_REFINE`
  - `COMPARE`
  - `INTERVIEW`
- 会话状态管理
- Deterministic execution
- Spring AI Tool Calling

当前正式 Tool：

- `searchCandidateByJDTool`
- `refineSearchFilterTool`
- `compareCandidatesTool`
- `generateInterviewQuestionsTool`

### 4. Chat / SSE

- `POST /api/chat`
- `POST /api/chat/stream`

当前 SSE 事件：

- `start`
- `router_decision`
- `tool_call`
- `tool_result`
- `state_update`
- `citation`
- `comparison`
- `token`
- `done`
- `error`

### 5. 对比与面试题

- `POST /api/comparison/candidates`
- `POST /api/interview/questions`

已支持：

- 候选人横向对比
- 差异点和风险点汇总
- 基于简历与目标查询的结构化面试题生成

## 本地模型能力

### OCR

- 本地服务目录：
  - [python/ocr-service/README.md](/C:/Users/Type-umr/Desktop/recruit-agent/python/ocr-service/README.md)
- Java 配置：
  - `app.ocr.provider=paddle`
- 已完成真实联调：
  - 扫描版 PDF 上传
  - OCR fallback
  - `parse_type=OCR_SCANNED`

### Embedding

- 本地服务目录：
  - [python/embedding-service/README.md](/C:/Users/Type-umr/Desktop/recruit-agent/python/embedding-service/README.md)
- Java 配置：
  - `app.embedding.provider=bge-m3`
- 已完成真实联调：
  - chunk embedding 写入 ES

### Rerank

- 本地服务目录：
  - [python/rerank-service/README.md](/C:/Users/Type-umr/Desktop/recruit-agent/python/rerank-service/README.md)
- Java 配置：
  - `app.rerank.provider=bge-reranker-v2-m3`
- 已完成真实联调：
  - search API 调用本地 rerank 服务
  - 返回 `rerankScore`

## 本地运行

### 1. 启动基础设施

```powershell
docker compose up -d
```

默认端口：

- MySQL: `3307`
- Elasticsearch: `9200`
- Kibana: `5601`

### 2. 启动本地模型服务

OCR:

```powershell
cd python/ocr-service
.\install.ps1
.\start.ps1
```

Embedding:

```powershell
cd python/embedding-service
.\install.ps1
.\start.ps1
```

Rerank:

```powershell
cd python/rerank-service
.\install.ps1
.\start.ps1
```

### 3. 启动应用

```powershell
$env:OPENAI_API_KEY='dummy'
.\mvnw.cmd -gs global-settings.xml -s settings.xml spring-boot:run "-Dspring-boot.run.profiles=local"
```

说明：

- 当前本地 profile 已默认接入 OCR / embedding / rerank
- `OPENAI_API_KEY` 目前即使不真正调用 OpenAI，也建议给一个占位值，否则部分 Spring AI 自动配置会拦启动

### 4. 运行测试

```powershell
.\mvnw.cmd -gs global-settings.xml -s settings.xml test
```

## 查看 Elasticsearch 数据

### 方式 1：Kibana

打开：

- `http://localhost:5601`

在 `Dev Tools` 中执行：

```http
GET resume_chunk/_search
{
  "size": 5,
  "sort": [
    { "indexedAt": "desc" }
  ]
}
```

查看候选人画像：

```http
GET candidate_profile/_search
{
  "size": 5,
  "sort": [
    { "indexedAt": "desc" }
  ]
}
```

### 方式 2：命令行

```powershell
docker exec recruit-agent-es curl -s "http://localhost:9200/candidate_profile/_search?size=5&sort=indexedAt:desc"
```

查看某个候选人的 chunk：

```powershell
docker exec recruit-agent-es curl -s "http://localhost:9200/resume_chunk/_search?q=candidateId:candidate-ocr-e2e-001&size=5&sort=indexedAt:desc"
```

检查 embedding 字段是否存在：

```powershell
docker exec recruit-agent-es curl -s "http://localhost:9200/resume_chunk/_count?q=candidateId:candidate-ocr-e2e-001%20AND%20_exists_:embedding"
```

## 最近提交

- `edc895f` `fix: normalize candidate search match scores`
- `eedc88d` `feat: add local rerank integration`
- `ed8b1b7` `feat: add local bge-m3 embedding integration`
- `7367d8e` `feat: add local paddle ocr integration`
- `2e0e5dc` `refactor: extract candidate match reason service`
- `6867430` `refactor: split search request normalization`
- `15f30d8` `feat: add natural language search filter parsing`
- `d8e3f4c` `feat: enhance candidate selection phrase parsing`

## 当前限制

- 候选人画像抽取仍然是规则式，不是 LLM enrichment
- 搜索虽已具备 ES + embedding + rerank 基础，但还没有真正的 hybrid retrieval 融合策略
- Chat token 事件仍然是 summary 分片，不是底层模型原生 streaming
- Router 仍以规则式判断为主
- compare / interview 仍然主要依赖规则组装，不是深度模型生成

## 下一步建议

当前最值得优先做的是：

- 收口文档与开发计划
- 再进入 hybrid retrieval
- 然后补更稳的 query understanding / router intelligence

不建议现在继续横向加新模块。
