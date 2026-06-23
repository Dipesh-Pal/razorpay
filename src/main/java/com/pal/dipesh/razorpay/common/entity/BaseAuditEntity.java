package com.pal.dipesh.razorpay.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import com.pal.dipesh.razorpay.common.config.JpaAuditingConfig;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Single base class for all auditable entities. Provides:
 * <ul>
 *   <li>UUID primary key ({@code id})</li>
 *   <li>Timestamp auditing: {@code createdAt}, {@code updatedAt}</li>
 *   <li>Principal auditing: {@code createdBy}, {@code updatedBy}</li>
 * </ul>
 *
 * <p>All four fields are populated by Spring Data JPA Auditing via
 * {@link AuditingEntityListener}. Auditing is enabled application-wide in
 * {@link JpaAuditingConfig}.
 *
 * <h2>Renaming an inherited column in a child entity</h2>
 * Use {@link jakarta.persistence.AttributeOverride} on the child class to
 * point an inherited field at a different column name, for example:
 * <pre>{@code
 * @Entity
 * @Table(name = "legacy_thing")
 * @AttributeOverride(name = "updatedAt",
 *     column = @Column(name = "modified_on", nullable = false))
 * public class LegacyThing extends BaseAuditEntity { ... }
 * }</pre>
 *
 * <h2>Omitting an inherited column</h2>
 * Standard JPA does not allow a child to "un-map" a field declared on a
 * {@code @MappedSuperclass}. If a specific table genuinely must not have one
 * of these columns, prefer one of:
 * <ol>
 *   <li>Leave the column nullable in that table (it costs almost nothing).</li>
 *   <li>Do not extend {@code BaseAuditEntity} for that one entity; declare
 *       only the fields it needs.</li>
 * </ol>
 */
@Getter
@Setter
@MappedSuperclass
@EqualsAndHashCode(of = "id")
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseAuditEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "updated_by")
    private String updatedBy;
}