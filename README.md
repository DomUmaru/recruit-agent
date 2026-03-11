# recruit-agent

ToB 智能招聘与面试辅助 Agent 系统。

## 项目简介

本项目面向 B 端 HR、招聘专员和技术面试官，目标是提供：

- 候选人搜索
- 条件筛选与多轮 refinement
- 多候选人对比
- 基于候选人简历和 JD 的面试提纲生成

系统整体方向基于：

- MySQL 主数据存储
- Elasticsearch 检索与向量索引
- OCR / PDF 解析
- Spring AI Tool Calling
- SSE 流式交互

## 当前进度

当前仓库已完成 `Step 1: 核心领域建模` 的基础工作：

- Spring Boot 3 + Java 17 工程骨架
- Maven Wrapper 与仓库内 Maven settings
- 核心 JPA Entity 建模
- Elasticsearch Document 建模
- Repository 仓储接口
- MySQL DDL 与 Elasticsearch mapping 冻结文档

## 目录结构

```text
src/main/java/com/recruit/agent
├─ candidate
├─ chat
├─ common
├─ position
├─ rag
└─ resume

docs/schema
├─ mysql-schema-v1.sql
├─ elasticsearch-indexes-v1.json
└─ README.md
```

## 本地构建

推荐使用仓库内的 Maven Wrapper，并显式指定当前仓库配置：

```powershell
.\mvnw.cmd -gs global-settings.xml -s settings.xml -DskipTests compile
```

## 当前建模范围

MySQL 实体：

- `Candidate`
- `ResumeDocument`
- `PositionJD`
- `ChatSession`
- `ChatMessage`

Elasticsearch 索引文档：

- `ResumeChunk`
- `CandidateProfileIndex`

## 后续计划

下一阶段将进入 `Step 2`：

- PDF 上传
- 文档落库
- PDFBox / OCR 双轨解析
- 文本清洗
- Chunking 与索引写入
