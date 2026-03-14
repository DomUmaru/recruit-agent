# Interview Talk Track

## 1. 项目一句话

这是一个面向招聘场景的智能简历检索系统，核心目标是把简历解析、结构化索引、混合检索、岗位上下文和可解释排序结合起来，帮助 HR 和用人部门更快找到更匹配的候选人。

## 2. 为什么做这个项目

真实招聘场景里有几个典型问题：

- 简历来源复杂，PDF 文本质量不稳定
- 只靠关键词搜索，召回和排序都不够准
- HR 和用人部门之间存在信息交接成本
- 搜到人以后，还要继续 refinement、比较和交接

所以这个项目不是单点搜索接口，而是一条完整链路。

## 3. 系统怎么做

### 摄入

- 简历上传后先做 PDF 文本解析和 OCR
- 再做结构化切片
- 最终写入两个核心索引：
  - `resume_chunk`
  - `candidate_profile`

### 搜索

- 搜索不是单路关键词
- 先做：
  - 关键词召回
  - `candidate_profile` 向量召回
  - `resume_chunk` 向量召回
- 再做 hybrid fusion 和 rerank

### 岗位上下文

- chat 会话可以绑定 `positionId`
- 每次搜索前先读取 `PositionJD`
- 把岗位默认约束注入 filter
- rerank 时再叠加 `JD-aware` 偏好分

## 4. 做过哪些关键工程优化

### 简历切片

- 早期切分太粗，后面重写成基于 section 的规则化切片
- child chunk 会带板块和子项富化信息，提升检索质量

### 中文 query parsing

- 解决过中文编码、LLM residual 过度压缩、`985/硕士` 过滤误判等问题
- 现在 search intent parsing 已经支持 query residual 和结构化 filter 分离

### compare / interview

- compare 支持按结果集编号选人
- interview 从“出题”重构成“面试交接提纲”
- evidence 不再取简历前几个 chunk，而是按岗位和 query 相关度从关键 section 里选 Top-K

### 多轮 refinement

- 支持“继续筛”和“重新筛”两种语义
- `继续筛` 追加 filter
- `重新筛` 替换上一轮 filter

## 5. 最终效果

现在这套系统已经支持：

- search
- refinement
- JD 上下文 chat
- compare
- interview handoff brief

重点不是“功能有了”，而是主链都跑通了，有真实样本和 synthetic 样本验证。

## 6. 你在里面负责什么

推荐答法：

- 我主导的是后端主链设计和落地，包括简历摄入、索引结构、检索链路、rerank、chat 工具编排，以及后续的质量收口。
- 项目里很多问题不是单点 bug，而是链路问题，比如 OCR 文本质量、切片粒度、query parsing、JD 约束和 compare/interview 的证据对齐，我是按链路逐步收口的。

## 7. 如果继续做下一步

- 不会再横向加功能
- 更可能做的是：
  - eval
  - observability
  - 更稳定的岗位偏好建模
  - application / candidate / position 的更完整业务闭环
