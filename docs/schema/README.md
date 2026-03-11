# Step 1 Schema Freeze

本目录用于冻结 `Step 1` 的数据结构设计，当前以代码模型为准。

文件说明：

- `mysql-schema-v1.sql`
  - 对应当前 JPA Entity 的 MySQL DDL
  - 包含 `candidate`、`resume_document`、`position_jd`、`chat_session`、`chat_message`

- `elasticsearch-indexes-v1.json`
  - 对应当前 Elasticsearch Document 的索引 mapping
  - 包含 `resume_chunk`、`candidate_profile`

使用约束：

- 后续如果修改 Entity 或 Document 字段，必须同步更新本目录
- `dense_vector.dims` 当前固定为 `1024`
- 如果后续更换 embedding 模型，需要同步调整：
  - Java Document 注解
  - Elasticsearch mapping
  - 向量写入逻辑
