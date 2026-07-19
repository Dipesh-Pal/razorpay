package com.pal.dipesh.razorpay.merchant.entity;

import com.pal.dipesh.razorpay.common.entity.BaseAuditEntity;
import com.pal.dipesh.razorpay.common.enums.UserRole;

import jakarta.persistence.*;

import lombok.*;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Dashboard/portal user belonging to a {@link Merchant}.
 *
 * <p>Extends {@link BaseAuditEntity}: account creation, role changes, and
 * password resets are all human-driven mutations where the actor matters
 * (e.g. "who promoted user X to ADMIN?"). Generic last-modified auditing
 * provides that out of the box.
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "app_user",
        indexes = {
                @Index(name = "idx_app_user_merchant_id", columnList = "merchant_id")
        }
)
@EqualsAndHashCode(callSuper = true)
public class AppUser extends BaseAuditEntity implements UserDetails {

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "merchant_id", nullable = false)
	private Merchant merchant;

	@Column(name = "email", nullable = false, length = 80, unique = true)
	private String email;

	@Column(name = "password_hash", nullable = false)
	private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 50)
    private UserRole role;

	@Override
	@NonNull
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority("ROLE_" + role));
	}

	@Override
	public @Nullable String getPassword() {
		return getPasswordHash();
	}

	@Override
	@NonNull
	public String getUsername() {
		return getEmail();
	}
}