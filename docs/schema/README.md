# Schema Freeze

本目录用于保存当前阶段的数据结构快照，方便：
- 回看实体设计
- 对照数据库和索引结构
- 后续做 schema 变更审查

## 文件说明

### `mysql-schema-v1.sql`
- 当前 MySQL 结构快照
- 覆盖主要业务表，例如：
  - `candidate`
  - `resume_document`
  - `position_jd`
  - `chat_session`
  - `chat_message`

### `elasticsearch-indexes-v1.json`
- 当前 Elasticsearch 索引 mapping 快照
- 覆盖：
  - `resume_chunk`
  - `candidate_profile`

## 使用约束

- 如果后续修改了 Entity 或 Elasticsearch Document 字段，应同步更新本目录
- 当前向量维度以现有 embedding 模型为准
- 如果未来切换 embedding 模型，需要同步检查：
  - Java 侧索引模型
  - Elasticsearch mapping
  - 向量写入逻辑
