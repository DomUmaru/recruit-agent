# Demo Playbook

## Demo 目标

用最少的步骤展示三件事：
- 系统能在简历库里找对人
- 系统能在岗位上下文里做更贴业务的排序
- 系统不仅能搜，还能继续比较和生成面试交接提纲

## 演示前准备

确保以下服务已启动：
- MySQL
- Elasticsearch
- OCR service
- embedding service
- rerank service
- Spring Boot application

建议使用：
- Apifox 或 Postman 调接口
- Kibana 只作为辅助查看，不作为主演示入口

## 主 Demo

### Demo 1：基础搜索

接口：
- `POST /api/search/candidates`

推荐 query：
- `Java 后端`

讲法：
- 先证明系统能做基础岗位搜索
- 返回的不只是名字，而是带有 evidence 的 topK 候选人
- 说明这不是纯关键词匹配，而是 keyword + profile vector + chunk vector + rerank 的融合结果

重点关注：
- top5 是否合理
- `matchReasons`
- `evidenceList`

### Demo 2：方向型搜索

推荐 query：
- `搜索 推荐 Java`
- 或 `Go 微服务`

讲法：
- 这条用来证明系统能区分方向，不是所有后端都混在一起
- 可以强调 search intent parsing 和 rerank 在起作用

### Demo 3：结构化过滤

推荐 query：
- `985 硕士 Java`

讲法：
- query 最终会保留成检索主题
- `985`、`硕士` 进入结构化 filter
- 说明系统支持“语义主题 + 结构化约束”的组合搜索

## JD 上下文 Demo

### Demo 4：岗位上下文 chat

接口：
- `POST /api/chat`

固定参数：
- `positionId = JD-DEMO-001`

推荐消息：
- `帮我找 Java 后端候选人`

讲法：
- chat 不是脱离业务上下文的自由问答
- 会话先绑定岗位
- 系统会先读取 `PositionJD`
- 再把岗位默认约束带进搜索
- 最终排序还会引入 `jdPreferenceScore`

### Demo 5：候选人对比

同一会话继续输入：
- `帮我比较前两个候选人`

讲法：
- compare 消费的是当前结果集，不需要重新搜
- 系统已经把结果集显式编号
- HR 可以直接引用“前两个”“第1、第2个”这类表达

### Demo 6：面试交接提纲

同一会话继续输入：
- `给第2个候选人生成面试交接提纲`

讲法：
- 这不是给 HR 出技术题
- 而是给技术面试官的交接材料
- evidence 不是整份简历，而是从 `项目经历 / 工作经验 / 实习经历 / 专业技能` 中按 query + JD 相关度选出的 Top-K chunk
- 当前输出包括：
  - `推荐理由`
  - `风险点/存疑点`
  - `追问建议`

## 多轮筛选 Demo

同一会话里继续演示：
1. `帮我找 Java 后端候选人`
2. `只看985硕士`
3. `继续只看北京的`
4. `重新帮我筛选一次，这次按上海 Java`

讲法：
- `继续筛` 会继承上一轮 query 和 filter
- `重新筛` 会保留会话入口，但替换上一轮筛选条件
- 这更符合真实 HR 的操作习惯

## 推荐讲述顺序

1. 基础搜索
2. 方向型搜索
3. 结构化过滤
4. JD 上下文 chat
5. compare
6. interview handoff brief
7. 多轮 refinement

## 演示时的边界说明

- `careerStage` 当前不作为 JD 默认硬过滤主条件，因为候选人侧还没有足够稳定的校招/社招真值
- compare 和 interview 已经可用，但它们属于扩展工作流，不是主搜索链
- 面试交接提纲当前是证据驱动的确定性摘要，重点是可解释和可控，而不是生成花哨文案
