CREATE DATABASE IF NOT EXISTS `recruit_agent`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE `recruit_agent`;

CREATE TABLE IF NOT EXISTS `candidate`
(
    `id`                         VARCHAR(36)    NOT NULL COMMENT '主键 ID',
    `created_at`                 DATETIME       NOT NULL COMMENT '创建时间',
    `updated_at`                 DATETIME       NOT NULL COMMENT '更新时间',
    `candidate_no`               VARCHAR(64)    NOT NULL COMMENT '候选人编号',
    `full_name`                  VARCHAR(128)   NOT NULL COMMENT '候选人姓名',
    `phone`                      VARCHAR(32)    NULL COMMENT '联系电话',
    `email`                      VARCHAR(128)   NULL COMMENT '邮箱地址',
    `current_city`               VARCHAR(64)    NULL COMMENT '当前所在城市',
    `current_company`            VARCHAR(128)   NULL COMMENT '当前公司名称',
    `current_title`              VARCHAR(128)   NULL COMMENT '当前岗位名称',
    `total_years_of_experience`  DECIMAL(5, 1)  NULL COMMENT '总工作年限',
    `highest_degree`             VARCHAR(32)    NULL COMMENT '最高学历',
    `school_name`                VARCHAR(256)   NULL COMMENT '毕业院校名称',
    `school_tier`                VARCHAR(32)    NULL COMMENT '学校层级标签',
    `source`                     VARCHAR(32)    NOT NULL COMMENT '候选人来源渠道',
    `status`                     VARCHAR(32)    NOT NULL COMMENT '候选人状态',
    `is_big_tech`                BIT(1)         NOT NULL COMMENT '是否具备大厂背景',
    `is_outsourcing`             BIT(1)         NOT NULL COMMENT '是否为外包经历候选人',
    `summary`                    VARCHAR(2000)  NULL COMMENT '候选人摘要',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_candidate_candidate_no` (`candidate_no`),
    KEY `idx_candidate_status` (`status`),
    KEY `idx_candidate_source` (`source`),
    KEY `idx_candidate_city` (`current_city`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='候选人主数据表';

CREATE TABLE IF NOT EXISTS `resume_document`
(
    `id`                VARCHAR(36)   NOT NULL COMMENT '主键 ID',
    `created_at`        DATETIME      NOT NULL COMMENT '创建时间',
    `updated_at`        DATETIME      NOT NULL COMMENT '更新时间',
    `candidate_id`      VARCHAR(36)   NOT NULL COMMENT '候选人 ID',
    `file_name`         VARCHAR(255)  NOT NULL COMMENT '原始文件名',
    `file_storage_key`  VARCHAR(512)  NOT NULL COMMENT '文件存储路径或对象存储 Key',
    `file_checksum`     VARCHAR(128)  NULL COMMENT '文件校验值',
    `version_no`        INT           NOT NULL COMMENT '简历版本号',
    `page_count`        INT           NULL COMMENT '文档总页数',
    `status`            VARCHAR(32)   NOT NULL COMMENT '简历处理状态',
    `parse_type`        VARCHAR(32)   NULL COMMENT '解析方式',
    `active_version`    BIT(1)        NOT NULL COMMENT '是否为当前生效版本',
    `raw_text`          LONGTEXT      NULL COMMENT '原始提取文本',
    `cleaned_text`      LONGTEXT      NULL COMMENT '清洗后的标准化文本',
    `page_text_json`    LONGTEXT      NULL COMMENT '按页文本 JSON',
    `parser_log`        LONGTEXT      NULL COMMENT '解析过程日志',
    PRIMARY KEY (`id`),
    KEY `idx_resume_document_candidate_id` (`candidate_id`),
    KEY `idx_resume_document_status` (`status`),
    KEY `idx_resume_document_candidate_active` (`candidate_id`, `active_version`),
    CONSTRAINT `fk_resume_document_candidate`
        FOREIGN KEY (`candidate_id`) REFERENCES `candidate` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='简历文档表';

CREATE TABLE IF NOT EXISTS `position_jd`
(
    `id`                       VARCHAR(36)   NOT NULL COMMENT '主键 ID',
    `created_at`               DATETIME      NOT NULL COMMENT '创建时间',
    `updated_at`               DATETIME      NOT NULL COMMENT '更新时间',
    `jd_no`                    VARCHAR(64)   NOT NULL COMMENT 'JD 编号',
    `title`                    VARCHAR(128)  NOT NULL COMMENT '岗位名称',
    `department`               VARCHAR(128)  NULL COMMENT '所属部门',
    `location`                 VARCHAR(128)  NULL COMMENT '工作地点',
    `min_years_of_experience`  INT           NULL COMMENT '最低经验要求',
    `max_years_of_experience`  INT           NULL COMMENT '最高经验要求',
    `required_degree`          VARCHAR(64)   NULL COMMENT '学历要求',
    `priority_skills`          VARCHAR(1000) NULL COMMENT '核心技能要求',
    `bonus_skills`             VARCHAR(1000) NULL COMMENT '加分项技能',
    `raw_jd_text`              LONGTEXT      NOT NULL COMMENT 'JD 原始文本',
    `structured_jd_json`       LONGTEXT      NULL COMMENT '结构化 JD 结果 JSON',
    `query_expansion_json`     LONGTEXT      NULL COMMENT '查询扩展词 JSON',
    `status`                   VARCHAR(32)   NOT NULL COMMENT 'JD 状态',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_position_jd_jd_no` (`jd_no`),
    KEY `idx_position_jd_status` (`status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='岗位 JD 表';

CREATE TABLE IF NOT EXISTS `chat_session`
(
    `id`                          VARCHAR(36)   NOT NULL COMMENT '主键 ID',
    `created_at`                  DATETIME      NOT NULL COMMENT '创建时间',
    `updated_at`                  DATETIME      NOT NULL COMMENT '更新时间',
    `session_no`                  VARCHAR(64)   NOT NULL COMMENT '会话编号',
    `user_id`                     VARCHAR(64)   NOT NULL COMMENT '发起会话的用户 ID',
    `current_scene`               VARCHAR(32)   NOT NULL COMMENT '当前会话场景',
    `status`                      VARCHAR(32)   NOT NULL COMMENT '会话状态',
    `current_query`               VARCHAR(1000) NULL COMMENT '当前轮主查询语句',
    `filters_json`                LONGTEXT      NULL COMMENT '当前筛选条件 JSON',
    `last_candidate_ids_json`     LONGTEXT      NULL COMMENT '上一轮候选人结果集 ID JSON',
    `selected_candidate_ids_json` LONGTEXT      NULL COMMENT '当前选中候选人 ID JSON',
    `sort_mode`                   VARCHAR(64)   NULL COMMENT '当前排序方式',
    `session_state_json`          LONGTEXT      NULL COMMENT '完整会话状态快照 JSON',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_chat_session_session_no` (`session_no`),
    KEY `idx_chat_session_user_status` (`user_id`, `status`),
    KEY `idx_chat_session_updated_at` (`updated_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='聊天会话表';

CREATE TABLE IF NOT EXISTS `chat_message`
(
    `id`                VARCHAR(36) NOT NULL COMMENT '主键 ID',
    `created_at`        DATETIME    NOT NULL COMMENT '创建时间',
    `updated_at`        DATETIME    NOT NULL COMMENT '更新时间',
    `session_id`        VARCHAR(36) NOT NULL COMMENT '会话 ID',
    `role`              VARCHAR(32) NOT NULL COMMENT '消息角色',
    `sequence_no`       INT         NOT NULL COMMENT '消息顺序号',
    `content`           LONGTEXT    NOT NULL COMMENT '消息正文',
    `citations_json`    LONGTEXT    NULL COMMENT '引用信息 JSON',
    `tool_payload_json` LONGTEXT    NULL COMMENT '工具调用载荷 JSON',
    PRIMARY KEY (`id`),
    KEY `idx_chat_message_session_sequence` (`session_id`, `sequence_no`),
    CONSTRAINT `fk_chat_message_session`
        FOREIGN KEY (`session_id`) REFERENCES `chat_session` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='聊天消息表';
