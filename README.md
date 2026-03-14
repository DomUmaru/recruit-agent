# Recruit-Agent：企业级智能招聘与面试辅助系统

![Java](https://img.shields.io/badge/Java-17-blue.svg)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.x-brightgreen.svg)
![Spring AI](https://img.shields.io/badge/Spring%20AI-Framework-orange.svg)
![Elasticsearch](https://img.shields.io/badge/Elasticsearch-8.x-yellow.svg)

**Recruit-Agent** 不是一个简单的“简历大模型对话壳”，而是一套面向 ToB 真实业务场景的 **Agentic Workflow（工作流智能体）** 后端系统。

本项目通过将大模型（LLM）的深度推理能力与传统检索引擎（ES）、确定性规则引擎深度融合，实现了从“非结构化简历解析”到“千岗千面检索”，再到“跨部门面试协同”的全链路招聘自动化。

---

## ✨ 核心技术亮点 (Technical Highlights)

- **混合意图路由架构 (Neuro-symbolic Routing)**：摒弃高延迟、易幻觉的纯 LLM 路由，采用“规则前置解析 (Fast-Path) + LLM 兜底推理 (Slow-Path)”的双层架构，实现对 `继续筛选 (Append)` 与 `重新筛选 (Replace)` 意图的精确分发。
- **工业级 RAG 检索底座**：
  - **动态上下文富化**：解决长文档“上下文孤儿”问题，基于 Parent-Child 切片与模块级元数据（Metadata）注入，提升语义命中率。
  - **三级漏斗检索**：实现 `Query 剥离去污` -> `ES BM25 & BGE-M3 混合召回` -> `BGE-Reranker 二次精排` 的完整检索流水线。
- **岗位感知打分模型 (Context-Aware Scoring)**：结合候选人的“语义相关性”、“JD 专属贴合度 (Priority Skills)”与“通用业务偏好 (学历/外包/年限)”，实现更符合 HR 录用直觉的推荐排序。
- **异构微服务解耦**：采用 Python Sidecar 模式独立部署 OCR、Embedding 与 Rerank 计算密集型服务，与 Java 业务主进程解耦，保障核心业务高可用。

---

## ⚙️ 核心业务模块 (Core Capabilities)

### 1. 简历摄入与多模态解析 (Ingestion Pipeline)

- `POST /api/resumes/upload`
- 支持原生 PDF 文本提取与 **PaddleOCR 动态降级容错**。
- 基于规则的逻辑段落识别与结构化切片，自动同步至 MySQL（业务库）与 Elasticsearch（检索库）。

### 2. 岗位绑定的上下文对话 (Contextual Chat Agent)

- `POST /api/chat/stream`（基于 SSE 的流式响应）
- Chat Session 强绑定 `PositionJD`，每轮对话自动注入岗位隐性硬约束（如：学历门槛、底线年限、城市要求）。

### 3. 多轮筛选与结果收敛 (Refinement)

- 将自然语言精准解析为 Residual Query（残差查询）与 Structured Filter（结构化过滤），支持：
  - **硬过滤维度**：学历、学校层级（985/211/C9 等）、年限、城市、职业阶段（校招/社招）、大厂/外包标签。
  - **状态机流转**：在上一轮候选人作用域（Scope）内持续缩小范围，或推翻重来。

### 4. 深度对比与协同交付 (Compare & Interview)

- 并非简单的关键词总结，而是 Agent 主动挂载工具：
  - **横向研判**：自动对比多位候选人，生成包含优劣势的 Markdown 分析报告。
  - **面试交接单**：为技术面试官自动生成包含“推荐理由、履历存疑点、STAR 法则追问建议”的交接提纲，赋能跨部门招聘协同。

---

## 🏗️ 架构与技术栈 (Tech Stack)

建议在此处补一张系统架构图，例如 `docs/architecture.png`。

- **Backend**: Java 17, Spring Boot 3.4.x, Spring AI
- **Storage**: MySQL 8, Elasticsearch 8
- **AI Models & Services** (Python 3.8+):
  - **OCR**: PaddleOCR
  - **Embedding**: BAAI/bge-m3
  - **Rerank**: BAAI/bge-reranker-v2-m3
- **Document Processing**: Apache PDFBox

---

## 🚀 快速启动 (Quick Start)

### 1. 启动基础设施

```bash
docker compose up -d
```

默认端口：

- MySQL：`3307`
- Elasticsearch：`9200`
- Kibana：`5601`

### 2. 启动本地 AI 推理服务 (Python 环境)

请确保本机已安装 Python 3.8+。

```powershell
# 1. 启动 OCR 服务
cd python/ocr-service
.\install.ps1
.\start.ps1

# 2. 启动 Embedding 服务
cd python/embedding-service
.\install.ps1
.\start.ps1

# 3. 启动 Rerank 服务
cd python/rerank-service
.\install.ps1
.\start.ps1
```

### 3. 启动 Spring Boot 应用

配置好大模型 API Key（即使当前不使用，也建议提供占位符以避免报错），以 `local` 环境启动：

```powershell
$env:OPENAI_API_KEY='your-api-key-here'
.\mvnw.cmd -gs global-settings.xml -s settings.xml spring-boot:run "-Dspring-boot.run.profiles=local"
```

### 4. 运行测试

```powershell
.\mvnw.cmd -gs global-settings.xml -s settings.xml test
```

---

## 🎯 推荐演示路线 (Demo Guide)

为了更好地展示本系统的架构设计，推荐按以下路线进行 API / UI 联调测试。

### 路线 1：底层检索引擎探底 (Search & Retrieval)

依次输入以下 Query，观察 `rerankScore` 与 `evidence chunk` 的精准度：

- `Java 后端`（测试基础召回）
- `搜索 推荐 Java`（测试 Query Normalization 剥离能力）
- `Go 微服务` / `C++`（测试跨语言边界置信度）
- `985 硕士 Java`（测试结构化 Filter 抽取与映射）

### 路线 2：Agent 业务工作流 (JD Context Chat)

绑定测试上下文：

- `positionId = JD-DEMO-001`

依次输入：

1. `帮我找 Java 后端候选人`（自动加载 JD 默认约束进行初筛）
2. `只看985硕士`（触发 `FILTER_REFINE`，追加过滤）
3. `继续只看北京的`（状态机继承，在上一轮池子内继续收敛）
4. `重新帮我筛选一次，这次按上海 Java`（触发重置，清空旧状态）
5. `帮我比较前两个候选人`（触发 `COMPARE` 工具，生成研判报告）
6. `给第 2 个候选人生成面试交接提纲`（触发 `INTERVIEW` 工具，交付最终业务价值）
