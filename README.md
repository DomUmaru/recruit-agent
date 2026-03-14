# recruit-agent

面向招聘场景的智能简历检索与候选人筛选后端系统。

当前版本已经完成一条可本地联调的主链路：
- 简历上传与解析
- OCR / 文本提取
- 结构化切片与索引
- 候选人搜索与多轮 refinement
- JD 绑定的岗位上下文搜索
- 候选人对比
- 面试交接提纲生成

## 核心能力

### 1. 简历摄入与索引
- `POST /api/resumes/upload`
- 支持 PDF 文本解析与 OCR
- 基于规则的结构化切片
- 写入 Elasticsearch
  - `resume_chunk`
  - `candidate_profile`

### 2. 搜索与 refinement
- `POST /api/search/candidates`
- `POST /api/search/candidates/refine`
- 支持自然语言 query 拆解
  - query residual
  - 结构化 filter
- 当前支持的过滤维度
  - 学历
  - 学校层级
  - 年限
  - 技术栈
  - 城市
  - 大厂
  - 外包
  - career stage

### 3. Hybrid retrieval + rerank
- 关键词召回
- `candidate_profile` 向量召回
- `resume_chunk` 向量召回
- rerank 精排
- 支持 JD-aware rerank
  - `title`
  - `prioritySkills`
  - `bonusSkills`

### 4. Chat Agent
- `POST /api/chat`
- `POST /api/chat/stream`
- 支持场景
  - `SEARCH`
  - `FILTER_REFINE`
  - `COMPARE`
  - `INTERVIEW`
- chat session 可绑定 `positionId`
- 每轮搜索会先加载对应 `PositionJD` 的默认约束
- 支持两类筛选语义
  - `继续筛`：继承上一轮结果，追加 filter
  - `重新筛`：重开一轮，替换上一轮 filter

### 5. 候选人对比与面试交接提纲
- `POST /api/comparison/candidates`
- `POST /api/interview/questions`

对比能力：
- 支持按搜索结果序号选人
- 支持
  - `第1个和第2个`
  - `前三个`
  - `最后三个`
  - `第1、4、5、8个`

面试交接提纲能力：
- 不再定位为“HR 出题”
- 输出面向技术面试官的交接材料
  - `推荐理由`
  - `风险点/存疑点`
  - `追问建议`
- evidence 来自候选人 `resume_chunk` 中与岗位和 query 最相关的 Top-K 片段

## 技术栈

- Java 17
- Spring Boot 3.4.x
- Spring AI
- MySQL 8
- Elasticsearch 8
- PDFBox
- PaddleOCR
- BGE-M3 embedding
- bge-reranker-v2-m3

## 项目结构

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

python
├── ocr-service
├── embedding-service
└── rerank-service
```

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
- `local` profile 默认接入 OCR / embedding / rerank
- 即使当前不走真实 OpenAI，也建议提供占位 `OPENAI_API_KEY`

### 4. 测试

```powershell
.\mvnw.cmd -gs global-settings.xml -s settings.xml test
```

## 推荐 Demo 路线

### 路线 1：搜索主链
1. `Java 后端`
2. `搜索 推荐 Java`
3. `Go 微服务`
4. `前端 React`
5. `985 硕士 Java`

关注点：
- topK 是否合理
- evidence 是否能解释命中
- refinement 是否能逐轮收敛

### 路线 2：JD 上下文 chat
绑定：
- `positionId = JD-DEMO-001`

示例消息：
- `帮我找 Java 后端候选人`
- `帮我比较前两个候选人`
- `给第2个候选人生成面试交接提纲`

关注点：
- JD 默认约束是否生效
- `jdPreferenceScore` 是否参与排序
- compare / interview 是否能消费会话内结果集

## 项目定位

主线：
- OCR / 结构化切片
- hybrid retrieval
- rerank
- JD-aware 搜索
- compare

扩展能力：
- chat 工具编排
- 面试交接提纲

当前更适合做的不是继续加功能，而是：
- 固定 demo
- 准备面试讲法
- 保持文档和运行链路一致
