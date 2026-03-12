# recruit-agent

面向 ToB 招聘场景的智能招聘与面试辅助 Agent 后端系统。

## 项目目标

项目面向 HR、招聘专员和技术面试官，目标是构建一套可对话的招聘工作台，覆盖这些核心场景：

- 候选人简历上传、解析与结构化入库
- 基于 JD 或自然语言的候选人搜索
- 条件筛选、多轮 refinement 和候选人对比
- 基于简历和 JD 生成面试问题与追问建议
- 通过统一 chat 入口完成搜索、筛选、对比和面试题生成

当前技术路线：

- MySQL：主数据存储
- Elasticsearch：检索索引与候选人画像索引
- PDFBox / OCR：简历解析
- Spring Boot：后端服务
- Spring AI：Agent 与 Tool Calling 编排
- SSE：流式对话输出

## 当前进度

当前仓库已经完成 `Step 1`、`Step 2`、`Step 3`、`Step 4` 和 `Step 5` 的 MVP 主链，并补齐了候选人对比与面试题生成功能。

### Step 1 已完成

- Spring Boot + Java 17 工程骨架
- Maven Wrapper、本仓库内 Maven settings
- 核心 JPA Entity 建模
- Elasticsearch Document 建模
- Repository 接口
- MySQL DDL 与 Elasticsearch mapping 文档冻结

### Step 2 已完成

- 简历上传接口
- 本地文件落盘
- `resume_document` 落库与版本管理
- PDFBox 文本型 PDF 解析
- OCR fallback 编排骨架
- 文本清洗
- Parent-Child Chunking
- `resume_chunk` 写入 Elasticsearch
- 规则式候选人画像抽取
- `candidate_profile` 写入 Elasticsearch

### Step 3 已完成的基础能力

- 候选人搜索模块 `search`
- 基于 `CandidateProfileIndex` 的候选人级检索
- 基于 `ResumeChunk` 的证据级召回
- 结构化过滤：
  - 学历
  - 学校层级
  - 年限
  - 技术栈
  - 城市
  - 大厂
  - 外包
- refinement 请求模型与合并策略
- 候选人范围限定 `scopeCandidateIds`
- 搜索 API：
  - `POST /api/search/candidates`
  - `POST /api/search/candidates/refine`

### Step 4 已完成的基础能力

- `SEARCH` / `FILTER_REFINE` / `COMPARE` / `INTERVIEW` 路由决策
- Router 决策对象与上下文模型
- Tool 风格的服务封装
- 会话状态装配：
  - `filtersJson`
  - `lastCandidateIdsJson`
  - `selectedCandidateIdsJson`
- Agent Orchestrator MVP
- Spring AI Tool Calling 接入
- 正式 Tool：
  - `searchCandidateByJDTool`
  - `refineSearchFilterTool`
  - `compareCandidatesTool`
  - `generateInterviewQuestionsTool`
- `ChatClient` 配置与无模型回退执行策略

### Step 5 已完成的 MVP

- 普通 chat 接口：
  - `POST /api/chat`
- SSE 流式接口：
  - `POST /api/chat/stream`
- 当前 SSE 事件协议：
  - `start`
  - `router_decision`
  - `tool_call`
  - `tool_result`
  - `state_update`
  - `citation`
  - `comparison`
  - `interview`
  - `token`
  - `done`
  - `error`

### 已补齐的业务能力

- `comparison` 模块
  - 候选人横向对比
  - 输出亮点、风险点、证据片段和总结
  - 对外接口：`POST /api/comparison/candidates`
- `interview` 模块
  - 基于候选人画像与简历证据生成结构化面试题
  - 输出候选人级题集、问题分类、提问理由和证据
  - 对外接口：`POST /api/interview/questions`

## 最近提交

- `5de734c` `feat: add interview question generation flow`
- `6906c47` `feat: enrich comparison chat payloads`
- `55059c8` `feat: add candidate comparison tool flow`
- `16cbc78` `feat: add chat api and sse event streaming`
- `cd08d35` `feat: add spring ai tool calling and agent orchestrator`
- `bfa371a` `feat: add search retrieval and agent routing foundation`
- `e7ca4c0` `feat: complete step2 resume ingestion mvp`
- `76f96ea` `feat: complete step1 domain modeling baseline`

## 已验证链路

当前已经通过测试与本地联调验证的链路包括：

- MySQL 容器启动并自动执行 schema
- Elasticsearch 与 Kibana 可用
- 简历上传成功
- PDF 正文提取成功
- `ResumeChunk` 索引写入成功
- `CandidateProfileIndex` 索引写入成功
- 英文年限表达如 `5 years` 可抽取为 `5.0`
- 搜索服务、refinement 服务、Agent Router、Agent Orchestrator 单元测试通过
- comparison 与 interview 服务单元测试通过
- chat API 与 SSE 事件流测试通过

说明：

- PowerShell 终端查看 Elasticsearch 返回值时，中文可能显示为乱码
- 经原始字节检查，ES 中保存的 UTF-8 数据是正确的
- 更推荐在 Kibana 或浏览器中查看中文字段

## 当前模块结构

```text
src/main/java/com/recruit/agent
├── agent
├── candidate
├── chat
├── common
├── comparison
├── interview
├── position
├── rag
├── resume
└── search

docs/schema
├── mysql-schema-v1.sql
└── elasticsearch-indexes-v1.json
```

## 核心模型

### MySQL 实体

- `Candidate`
- `ResumeDocument`
- `PositionJD`
- `ChatSession`
- `ChatMessage`

### Elasticsearch 索引文档

- `ResumeChunk`
- `CandidateProfileIndex`

## 本地开发

### 1. 环境变量

先复制环境变量模板：

```powershell
Copy-Item .env.example .env
```

如果要启用 Spring AI 的 OpenAI 模型调用，请在 `.env` 中配置：

```powershell
OPENAI_API_KEY=your_api_key
```

默认配置为：

- `spring.ai.model.chat=none`

也就是说，不配置模型时应用仍可启动，并走确定性回退链路。

### 2. 编译

推荐使用仓库内的 Maven Wrapper，并显式指定当前仓库配置：

```powershell
.\mvnw.cmd -gs global-settings.xml -s settings.xml -DskipTests compile
```

### 3. 启动基础设施

```powershell
docker compose up -d
```

当前默认端口：

- MySQL: `localhost:3307`
- Elasticsearch: `http://localhost:9200`
- Kibana: `http://localhost:5601`

MySQL 启动时会自动执行：

- [mysql-schema-v1.sql](/C:/Users/Type-umr/Desktop/recruit-agent/docs/schema/mysql-schema-v1.sql)

### 4. 启动应用

使用本地 profile 启动：

```powershell
.\mvnw.cmd -gs global-settings.xml -s settings.xml spring-boot:run "-Dspring-boot.run.profiles=local"
```

如果本机 Maven Wrapper 后台启动不稳定，也可以先打包再运行：

```powershell
.\mvnw.cmd -gs global-settings.xml -s settings.xml -DskipTests package
java -jar target/recruit-agent-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

### 5. 运行测试

```powershell
.\mvnw.cmd -gs global-settings.xml -s settings.xml test
```

## 主要接口

### 简历上传

`POST /api/resumes/upload`

请求类型：

- `multipart/form-data`

表单字段：

- `candidateId`
- `file`

### 候选人搜索

`POST /api/search/candidates`

请求体示例：

```json
{
  "query": "推荐系统 Java",
  "filter": {
    "highestDegrees": ["BACHELOR"],
    "schoolTiers": ["PROJECT_985"],
    "minYearsOfExperience": 3.0,
    "technicalSkills": ["Java"],
    "bigTech": true,
    "outsourcing": false
  },
  "limit": 10,
  "evidenceLimit": 3
}
```

### 候选人 refinement 搜索

`POST /api/search/candidates/refine`

请求体示例：

```json
{
  "baseRequest": {
    "query": "推荐系统",
    "scopeCandidateIds": ["candidate-1", "candidate-2"]
  },
  "refinementQuery": "只要985和211，再加3年以上",
  "mergeMode": "APPEND"
}
```

### 候选人对比

`POST /api/comparison/candidates`

请求体示例：

```json
{
  "candidateIds": ["candidate-1", "candidate-2"],
  "targetQuery": "推荐系统"
}
```

### 面试题生成

`POST /api/interview/questions`

请求体示例：

```json
{
  "candidateIds": ["candidate-1"],
  "targetQuery": "推荐系统"
}
```

### 普通聊天

`POST /api/chat`

请求体示例：

```json
{
  "sessionNo": "session-1",
  "userId": "user-1",
  "message": "找做推荐系统的候选人"
}
```

### SSE 流式聊天

`POST /api/chat/stream`

返回 `text/event-stream`，当前会输出结构化事件序列。

## 文档与配置

- 项目规划文档：[NEW_PROJECT_SUMMARY.md](/C:/Users/Type-umr/Desktop/recruit-agent/NEW_PROJECT_SUMMARY.md)
- MySQL DDL：[mysql-schema-v1.sql](/C:/Users/Type-umr/Desktop/recruit-agent/docs/schema/mysql-schema-v1.sql)
- Elasticsearch mapping：[elasticsearch-indexes-v1.json](/C:/Users/Type-umr/Desktop/recruit-agent/docs/schema/elasticsearch-indexes-v1.json)
- 本地 Docker 环境：[docker-compose.yml](/C:/Users/Type-umr/Desktop/recruit-agent/docker-compose.yml)
- 本地应用配置：[application-local.yml](/C:/Users/Type-umr/Desktop/recruit-agent/src/main/resources/application-local.yml)

## 当前限制

- OCR 当前只有 fallback 接口与编排骨架，尚未接入真实识别引擎
- 向量字段已预留，但还未接入真实 embedding 生成
- 候选人画像抽取当前仍是规则式实现，不是 LLM enrichment
- Search 当前已有 ES 查询与证据召回，但 ranking、query rewrite、vector/hybrid retrieval 仍未完善
- Router 当前仍以规则式判断 `SEARCH` / `FILTER_REFINE` / `COMPARE` / `INTERVIEW` 为主
- comparison 与 interview 当前是规则式组装，不是基于 LLM 深度生成
- 当前 token 事件是基于 summary 分片的伪流式输出，不是底层模型的原生 token streaming
- SSE 当前使用 `SseEmitter`，尚未迁移到 WebFlux

## 下一步

建议下一阶段优先从以下方向中选择其一：

- 升级当前 search / router，让模型更深度参与 query 理解、refinement 解析和候选人选择
- 为 comparison / interview 增加更稳定的前端展示 payload 与引用协议
- 将当前 SSE 从 summary 分片流升级为真正的模型 token 流
- 继续增强检索质量，包括 rerank、query rewrite、vector/hybrid retrieval
