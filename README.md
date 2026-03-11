# recruit-agent

面向 ToB 招聘场景的智能招聘与面试辅助 Agent 系统。

## 项目目标

本项目面向 HR、招聘专员和技术面试官，目标是构建一套可对话的招聘工作台，覆盖这些核心场景：

- 候选人简历上传、解析与结构化入库
- 基于 JD 或自然语言的候选人搜索
- 条件筛选、多轮 refinement 和候选人对比
- 基于简历和 JD 生成面试问题与追问建议

当前技术路线：

- MySQL：主数据存储
- Elasticsearch：检索索引与候选人画像索引
- PDFBox / OCR：简历解析
- Spring Boot：后端服务
- Spring AI：后续 Agent 与 Tool Calling 编排
- SSE：后续流式对话输出

## 当前进度

当前仓库已经完成 `Step 1`，并实现了 `Step 2` 的 MVP 链路。

### Step 1 已完成

- Spring Boot 3 + Java 17 工程骨架
- Maven Wrapper、本仓库内 Maven settings
- 核心 JPA Entity 建模
- Elasticsearch Document 建模
- Repository 仓储接口
- MySQL DDL 与 Elasticsearch mapping 文档冻结

### Step 2 当前已实现

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
- 基础单元测试与真实联调验证

## 已验证链路

当前已经完成一次真实本地联调，验证通过的链路包括：

- MySQL 容器启动并自动执行 schema
- Elasticsearch 与 Kibana 可用
- 简历上传成功
- PDF 正文提取成功
- `ResumeChunk` 索引写入成功
- `CandidateProfileIndex` 索引写入成功
- 英文年限表达如 `5 years` 可抽取为 `5.0`

说明：

- PowerShell 终端查看 Elasticsearch 返回值时，中文可能显示为乱码
- 经原始字节检查，ES 中保存的 UTF-8 数据是正确的
- 更推荐在 Kibana 或浏览器中查看中文字段

## 当前模块结构

```text
src/main/java/com/recruit/agent
├── candidate
├── chat
├── common
├── position
├── rag
└── resume

docs/schema
├── mysql-schema-v1.sql
├── elasticsearch-indexes-v1.json
└── README.md
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

### 1. 编译

推荐使用仓库内的 Maven Wrapper，并显式指定当前仓库配置：

```powershell
.\mvnw.cmd -gs global-settings.xml -s settings.xml -DskipTests compile
```

### 2. 启动基础设施

先复制环境变量模板：

```powershell
Copy-Item .env.example .env
```

启动本地容器：

```powershell
docker compose up -d
```

当前默认端口：

- MySQL: `localhost:3307`
- Elasticsearch: `http://localhost:9200`
- Kibana: `http://localhost:5601`

MySQL 启动时会自动执行：

- [mysql-schema-v1.sql](C:/Users/Type-umr/Desktop/recruit-agent/docs/schema/mysql-schema-v1.sql)

### 3. 启动应用

使用本地 profile 启动：

```powershell
.\mvnw.cmd -gs global-settings.xml -s settings.xml spring-boot:run "-Dspring-boot.run.profiles=local"
```

如果本机 Maven Wrapper 后台启动不稳定，也可以先打包再运行：

```powershell
.\mvnw.cmd -gs global-settings.xml -s settings.xml -DskipTests package
java -jar target/recruit-agent-0.0.1-SNAPSHOT.jar --spring.profiles.active=local
```

### 4. 运行测试

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

成功后会返回：

- `documentId`
- `candidateId`
- `versionNo`
- `status`
- `fileStorageKey`
- `createdAt`

## 文档与配置

- 项目规划文档：`NEW_PROJECT_SUMMARY.md`
- MySQL DDL：[docs/schema/mysql-schema-v1.sql](C:/Users/Type-umr/Desktop/recruit-agent/docs/schema/mysql-schema-v1.sql)
- Elasticsearch mapping：[docs/schema/elasticsearch-indexes-v1.json](C:/Users/Type-umr/Desktop/recruit-agent/docs/schema/elasticsearch-indexes-v1.json)
- 本地 Docker 环境：[docker-compose.yml](C:/Users/Type-umr/Desktop/recruit-agent/docker-compose.yml)
- 本地应用配置：[application-local.yml](C:/Users/Type-umr/Desktop/recruit-agent/src/main/resources/application-local.yml)

## 当前限制

- OCR 目前只有 fallback 接口与编排骨架，尚未接入真实识别引擎
- 向量字段已预留，但还未接真实 embedding 生成
- 候选人画像抽取当前为规则式实现，不是 LLM enrichment
- Section 识别与 chunk 切分规则仍偏 MVP
- 搜索服务、Agent Router、SSE 对话接口尚未开始

## 下一步

建议下一阶段进入 `Step 3`：

- 基于 `CandidateProfileIndex` 的候选人聚合检索
- 基于 `ResumeChunk` 的证据级召回
- 条件过滤与多轮 refinement
- 为后续 Agent Tool 编排准备统一搜索入口
