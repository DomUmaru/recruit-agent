# Role: 资深 Java 架构师 & Spring AI 智能体开发专家

# Background:
我正在从 0 到 1 开发一个名为《ToB 智能招聘与面试辅助 Agent 系统》的企业级后端项目。
你将作为我的结对编程架构师，协助我完成底层的数据库建模、核心业务逻辑开发、Agent 工作流编排以及 SSE 流式接口的开发。

# Tech Stack:
- Java 17, Spring Boot 3.x
- Spring AI (使用最新的 Function Calling / @Tool 规范)
- 关系型数据：MySQL 8.0 + Spring Data JPA
- 向量与全文检索：Elasticsearch 8.x + Spring AI Elasticsearch Vector Store
- 文档解析：Apache PDFBox + OCR 降级策略
- 接口通讯：Spring WebFlux (SSE / Server-Sent Events)

---
# Project PRD & Architecture Document:
（这里是项目的全局上下文，请仔细阅读并牢记，但不要急于写全部代码）

<PRD_Document>
# 新项目总结：ToB 智能招聘与面试辅助 Agent 系统

## 1. 项目定位
- 面向 B 端 HR、招聘专员、技术面试官
- 提供候选人搜索、筛选、对比、面试提纲生成能力
- 以自然语言交互为入口，以结构化检索和工具调用为执行核心
- 项目目标：
  - 从企业私有简历库中精准召回候选人
  - 支持多轮筛选条件追加
  - 支持多候选人横向分析
  - 支持基于简历和 JD 的面试辅助

---

## 2. 核心业务场景
- 候选人搜索
  - 输入 JD 或自然语言招聘需求
  - 返回匹配候选人 TopN、匹配理由、证据来源
- 条件追加筛选
  - 支持“只要985和211”“3年以上”“优先大厂”“排除外包”等条件
- 多候选人对比
  - 对比学历、技术栈、项目经历、岗位匹配度、风险点
- 面试提纲生成
  - 针对候选人和岗位要求生成分层问题、追问链、风险核验问题
- 聊天式招聘辅助
  - 通过统一 Chat 界面完成搜索、筛选、对比、面试题生成

---

## 3. 技术栈
- 后端：`Java 17`、`Spring Boot 3.x`
- AI：`Spring AI`
- 主数据库：`MySQL`
- 搜索引擎：`Elasticsearch`
- 文档处理：`PDFBox + OCR`
- 交互方式：`SSE`
- 可选增强：
  - ES 向量检索
  - Rerank
  - 结构化输出
  - 任务异步化

---

## 4. 数据存储设计
### MySQL
- 候选人主数据
- 候选人画像
- 简历文档信息
- JD 信息
- Chat 会话
- Chat 消息
- 搜索状态
- Trace 日志
- Eval 报告

### Elasticsearch
- 候选人搜索索引
- 简历 chunk 证据索引
- 可选 JD 索引
- 可选 dense vector 字段

---

## 5. 模块划分
- `candidate`
- `resume`
- `position`
- `search`
- `comparison`
- `interview`
- `chat`
- `agent`
- `rag`
- `ai`
- `observability`
- `eval`
- `common`

---

# 6. PDF 上传模块

## 6.1 模块目标
- 接收候选人简历 PDF
- 自动判断文本型 PDF 或扫描件 PDF
- 提取文本内容并完成标准化清洗
- 构建候选人画像基础数据
- 为后续索引、检索、面试生成提供原始输入

## 6.2 功能点
- 文件上传
- 候选人绑定
- 文档版本管理
- 文档状态跟踪
- 文本解析
- OCR fallback
- 文本清洗
- 结构分段
- 页面信息保留

## 6.3 处理流程
- 上传 PDF
- 创建 `resume_document`
- 判断是否可直接提取文本
- 文本型：
  - 走 `PDFBox`
- 扫描型：
  - 走 `OCR`
- 统一文本清洗：
  - 去噪
  - 去乱码
  - 段落恢复
  - 标题识别
- 输出标准化全文文本
- 准备进入 enrichment 和 chunking

## 6.4 产出内容
- 文档主记录
- 原始文本
- 按页文本
- 页面信息
- 文档状态
- 解析日志

---

# 7. RAG 模块

## 7.1 模块目标
- 将简历和 JD 转化为可检索、可引用、可解释的知识资产
- 支撑候选人搜索、候选人对比、面试题生成

## 7.2 模块组成
- 文档清洗
- 结构识别
- Chunk 切分
- 标签 enrichment
- 索引构建
- 检索
- 排序
- 上下文压缩
- 引用生成

## 7.3 文档结构化
- 识别简历区块：
  - 个人简介
  - 教育经历
  - 工作经历
  - 项目经历
  - 技能栈
  - 其他
- 识别 JD 区块：
  - 岗位职责
  - 任职要求
  - 加分项
  - 学历要求
  - 年限要求

## 7.4 Chunking
- 采用 Parent-Child Chunk
- Parent：
  - 保留完整上下文块
- Child：
  - 用于细粒度召回
- 保留：
  - `candidateId`
  - `docId`
  - `section`
  - `page`
  - `parentId`
  - `content`

## 7.5 Enrichment
- 从简历中提取：
  - 学校名
  - 学历层级
  - 工作年限
  - 技术栈
  - 行业标签
  - 公司标签
  - 项目标签
  - 隐性语义标签
- 从 JD 中提取：
  - 核心技能
  - 学历要求
  - 经验要求
  - 岗位方向
  - 加分项
  - 查询扩展词

## 7.6 RAG 输出能力
- 为搜索提供召回索引
- 为候选人匹配提供证据
- 为对比报告提供原始事实依据
- 为面试问题生成提供亮点与风险点证据

---

# 8. 召回与检索模块

## 8.1 模块目标
- 从 ES 中高效召回候选人和相关证据
- 支持自然语言需求与结构化过滤结合
- 支持候选人级粗召回和证据级细召回

## 8.2 检索层次
### 第一层：候选人级召回
- 检索对象：候选人聚合索引
- 用途：筛出候选人池
- 能力：
  - 全文检索
  - 多字段检索
  - filter 条件约束
  - 向量召回
  - 混合检索

### 第二层：证据级召回
- 检索对象：简历 chunk 索引
- 用途：解释候选人为什么匹配
- 能力：
  - chunk 搜索
  - section 定位
  - 页面定位
  - 引用生成

## 8.3 支持的查询类型
- JD 检索
- 自然语言检索
- 技术关键词检索
- 条件筛选检索
- 多轮条件 refinement
- 组合检索：
  - query + filter
  - filter only
  - query + candidate scope

## 8.4 支持的过滤条件
- 学历层级
- 学校名称
- 工作年限
- 技术栈
- 公司标签
- 项目标签
- 行业标签
- 城市
- 是否大厂
- 是否外包
- 候选人来源
- 其他扩展标签

## 8.5 查询理解
- Query Rewrite
- 技术术语扩展
- 同义词归一
- JD 术语扩展
- 可选 HyDE
- 自然语言条件解析

## 8.6 排序策略
- BM25
- Bool filter
- ES vector score
- 混合检索融合
- rerank
- MMR 去重
- 候选人级聚合打分

## 8.7 检索结果输出
- 候选人列表
- 匹配分
- 命中字段
- 证据片段
- 匹配原因
- 可解释引用

---

# 9. Agent 编排模块

## 9.1 模块目标
- 理解 HR 当前要完成的招聘任务
- 决定当前应该调用哪个 Tool
- 管理多轮招聘会话状态
- 在可控边界内完成任务编排

## 9.2 Agent 核心职责
- 意图识别
- 任务路由
- 状态读取
- Tool 选择
- Tool 调用
- 结果拼装
- 状态更新

## 9.3 Router
- 识别当前场景：
  - `SEARCH`
  - `FILTER_REFINE`
  - `COMPARE`
  - `INTERVIEW`
  - `CHAT_QA`
- 判断是否依赖历史状态
- 限定允许调用的 Tool 集合
- 控制业务边界

## 9.4 State
- 保存会话级上下文
- 主要字段：
  - 当前 query
  - 当前 filters
  - 上轮候选人列表
  - 当前选中的候选人
  - 当前场景
  - 排序方式
- 支持：
  - 新搜索
  - 追加筛选
  - 覆盖筛选
  - 候选人选择
  - 任务切换

## 9.5 Tool 体系
### 必做 Tool
- `searchCandidateByJDTool`
- `compareCandidatesTool`
- `generateInterviewQuestionsTool`
- `refineSearchFilterTool`

### 可选 Tool
- `parseJDTool`
- `retrieveCandidateEvidenceTool`
- `extractCandidateProfileTool`

## 9.6 Orchestrator
- 串联：
  - Router
  - State
  - Tool
  - RAG
  - LLM
  - Answer Assembler
- 控制执行顺序
- 保证结果统一输出
- 保证 trace 可记录

## 9.7 Agent 的体现
- 不是固定死流程
- 不是单步检索
- 是根据任务类型动态选 Tool
- 是根据状态进行多轮任务推进
- 是围绕招聘业务目标完成执行

---

# 10. Chat 模块

## 10.1 模块目标
- 提供统一招聘对话入口
- 支持流式结果返回
- 支持聊天驱动搜索、筛选、对比、面试题生成
- 支持多轮上下文状态延续

## 10.2 入口能力
- 统一聊天接口
- 聊天历史记录
- 多轮上下文延续
- SSE 实时事件输出

## 10.3 支持的对话类型
- 搜索型对话
- 条件 refinement 对话
- 对比型对话
- 面试辅助型对话
- 普通解释型对话

## 10.4 SSE 事件设计
- `start`
- `router_decision`
- `state_update`
- `tool_call`
- `tool_result`
- `token`
- `citation`
- `done`
- `error`

## 10.5 Chat 处理流程
- 接收 HR 输入
- 读取 session state
- Router 判断场景
- 调用 Orchestrator
- 选择 Tool
- 执行业务动作
- 生成自然语言结果
- 流式返回
- 更新 state
- 保存消息与 trace

## 10.6 多轮能力示例
- 第一轮：
  - “找做推荐系统的候选人”
- 第二轮：
  - “只要985和211”
- 第三轮：
  - “再加3年以上经验”
- 第四轮：
  - “把前3个对比一下”
- 第五轮：
  - “给第2个生成面试题”

Chat 模块负责统一承接整个任务链。

---

# 11. 候选人画像模块

## 11.1 模块目标
- 将非结构化简历转为结构化候选人画像
- 支撑筛选条件过滤和候选人级搜索

## 11.2 画像内容
- 最高学历
- 学校名称
- 学校层级
- 工作年限
- 技术栈
- 行业标签
- 公司标签
- 项目标签
- 候选人摘要
- 扩展 metadata

## 11.3 用途
- 搜索过滤
- 候选人聚合召回
- 结果解释
- 对比分析
- 面试题生成

---

# 12. JD 模块

## 12.1 模块目标
- 管理岗位需求
- 结构化解析岗位要求
- 为搜索和面试生成提供标准输入

## 12.2 功能点
- 新建 JD
- 存储原文
- 提取结构化字段
- 技能词扩展
- 检索 query 构建
- 岗位要求摘要

## 12.3 解析结果
- 岗位名称
- 关键技能
- 学历要求
- 年限要求
- 核心职责
- 加分项
- query 扩展词

---

# 13. Comparison 模块

## 13.1 模块目标
- 对多个候选人生成结构化对比报告

## 13.2 对比维度
- 学历
- 学校层级
- 工作年限
- 技术栈
- 项目经验
- 岗位契合点
- 风险点
- 综合建议

## 13.3 数据来源
- CandidateProfile
- ResumeChunk evidence
- JD 结构化结果
- RAG 检索结果

---

# 14. Interview 模块

## 14.1 模块目标
- 针对岗位与候选人生成定制化面试问题

## 14.2 生成内容
- 基础核验题
- 项目深挖题
- 技术追问题
- 风险点问题
- 与 JD 相关的定向问题
- 连环追问链

## 14.3 依赖输入
- candidateId
- jdId 或岗位要求
- 候选人亮点
- 候选人模糊点
- 候选人与岗位差距点

---

# 15. Observability 模块

## 15.1 模块目标
- 记录系统行为和检索轨迹
- 支持问题排查和效果分析

## 15.2 记录内容
- 原始 query
- 重写 query
- filter 条件
- Router 决策
- Tool 调用记录
- 检索命中结果
- citations
- latency
- guardrail 结果

---

# 16. Eval 模块

## 16.1 模块目标
- 做招聘检索和结果生成的离线评估

## 16.2 评测方向
- 候选人召回效果
- 筛选条件准确性
- 匹配解释质量
- 对比报告质量
- 面试题相关性
- citation 覆盖率

---

# 17. 第一版开发重点

## 必须完成
- PDF 上传
- PDFBox + OCR 解析
- 候选人画像抽取
- MySQL 主数据建模
- ES 检索索引
- 候选人搜索
- 条件 refinement
- Chat + SSE
- Router + State
- 4 个核心 Tool

## 暂不做
- 完整多租户
- 复杂权限系统
- MQ 异步链路
- 多 Agent 协作
- 超复杂前端
- 自动评分体系

---

# 18. 一句话项目定义
一个基于 `MySQL + Elasticsearch + OCR + Spring AI Tool Calling + SSE` 的 ToB 智能招聘与面试辅助 Agent 系统，支持候选人搜索、筛选、对比和定制化面试提纲生成。

</PRD_Document>

---
# Development Rules (开发核心原则):
为了保证项目的高质量与代码可维护性，请你务必遵守以下原则：
1. **绝对不要一次性生成所有代码**：我们的项目很大，必须严格按照我规定的步骤（Step-by-Step）进行开发。每次我只让你做一个 Step。
2. **遵守大厂规范**：代码必须遵循高内聚低耦合，Service 层要写接口和实现类，要有清晰的 DTO/VO 划分，核心方法必须加 JavaDoc 注释。
3. **拥抱最新的 Spring AI 规范**：大模型调用请使用 `ChatClient` Fluent API；工具调用请优先使用最新的 `@Tool` 注解或 `@Bean` + `@Description` 的标准做法。
4. **注意异构数据双写**：业务状态存在 MySQL，用于检索的文档 Chunk 和 Vector 存入 Elasticsearch。

---
# Execution Roadmap (分步开发计划):
我们将会按照以下 5 个阶段来完成第一版（MVP）的开发：

- **Step 1: 核心领域建模 (Domain Modeling)**。根据 PRD 编写 MySQL 的 JPA Entity 实体类（如 Candidate, PositionJD, ChatSession 等），以及 Elasticsearch 的 Document 模型（如 ResumeChunk, CandidateProfile 等）。
- **Step 2: 文档摄入与解析流水线 (Ingestion Pipeline)**。开发 PDFBox+OCR 的双轨解析逻辑，实现文本清理、大模型提取隐性标签（Enrichment），以及 Parent-Child Chunking 切分逻辑，并将其存入 MySQL 和 ES。
- **Step 3: 高阶检索增强层 (RAG & Retrieval Service)**。利用 ES 实现候选人维度的粗召回和 Chunk 维度的细召回，支持“Query（全文/向量） + Filter（精确条件过滤）”的混合检索逻辑。
- **Step 4: Agent 编排与 Tool 注册 (Agent Core)**。设计多轮会话状态（State），编写 Router 逻辑，并注册核心的 4 个 Tool（searchCandidate, refineSearch, compareCandidates, generateInterview）。
- **Step 5: 聊天交互与流式网关 (Chat & SSE API)**。开发暴露给前端的 `/api/chat/stream` 接口，接收 HR 的自然语言输入，串联 Agent Router，并将思考过程与 Tool 调用结果通过 SSE 流式推送到前端。

---
# Initial Command (本次任务):
你是否已经完全理解了本项目的设计文档和我们的分步开发计划？
如果理解，**请不要生成其他步骤的代码，现在立即开始执行【Step 1】**：
1. 请给出 MySQL 核心业务表的 Spring Data JPA Entity 类代码（包含 Candidate, ResumeDocument, PositionJD, ChatSession）。
2. 请给出对应 Elasticsearch 索引的 Document 类代码（包含 ResumeChunk, CandidateProfile_Index，需注意包含 dense_vector 和 Metadata 字段）。
3. 请为上述模型生成合理的字段、关联关系（OneToMany等）以及必要的 JPA 注解。