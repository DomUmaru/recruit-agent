package com.recruit.agent.common.model;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 统一主键基类，使用字符串 UUID，便于 MySQL 与 Elasticsearch 共享同一业务标识。
 */
@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
public abstract class BaseAutoIdEntity implements Serializable {

    /**
     * 主键 ID，统一使用 UUID 字符串。
     */
    @Id
    @Column(name = "id", nullable = false, updatable = false, length = 36)
    private String id;

    @PrePersist
    protected void assignIdIfAbsent() {
        if (Objects.isNull(this.id) || this.id.isBlank()) {
            this.id = UUID.randomUUID().toString();
        }
    }

}
