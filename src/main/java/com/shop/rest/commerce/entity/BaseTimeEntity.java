package com.shop.rest.commerce.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class BaseTimeEntity {

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime regTime;

    @UpdateTimestamp
    @Column(insertable = false)
    private LocalDateTime updateTime;
}
