package com.pal.dipesh.razorpay.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

/**
 * Enables Spring Data JPA Auditing so that {@code @CreatedDate},
 * {@code @LastModifiedDate}, {@code @CreatedBy} and {@code @LastModifiedBy}
 * fields on entities (e.g. {@code BaseTimeAuditEntity} / {@code BaseAuditEntity})
 * are populated automatically.
 *
 * <p>The current {@link AuditorAware} implementation always returns
 * {@code "system"}. Once a real user context (e.g. Spring Security) is wired in,
 * replace {@link #auditorAware()} with one that resolves the authenticated
 * principal and falls back to {@code "system"} for background / scheduled work.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaAuditingConfig {

    static final String SYSTEM_AUDITOR = "system";

    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> Optional.of(SYSTEM_AUDITOR);
    }
}