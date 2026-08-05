package com.pal.dipesh.razorpay.merchant.security;

import com.pal.dipesh.razorpay.common.enums.TokenType;
import com.pal.dipesh.razorpay.merchant.security.rsa.RsaKeyProperties;
import com.pal.dipesh.razorpay.merchant.security.rsa.TokenPair;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final TokenBlockListService blockListService;
    private final RsaKeyProperties rsaKeyProperties;
    private final RSAPrivateKey rsaPrivateKey;
    private final RSAPublicKey rsaPublicKey;

    private record IssuedToken(String token, String jti, Instant expiresAt) {}

    /*----------------------- Token generation -----------------------*/

    public TokenPair generateTokenPair(String email, UUID merchantId, List<String> role) {
        IssuedToken refreshToken = buildRefreshToken(email, merchantId);
        IssuedToken accessToken  = buildAccessToken(email, merchantId, role, refreshToken.jti());

        return new TokenPair(
                accessToken.token(),
                refreshToken.token(),
                accessToken.expiresAt(),
                refreshToken.expiresAt()
        );
    }

    private IssuedToken buildAccessToken(String email, UUID merchantId, List<String> role, String refreshJti) {
        Instant now = Instant.now();
        Date exp = Date.from(now.plusMillis(rsaKeyProperties.accessTokenExpiryMs()));
        String jti = UUID.randomUUID().toString();

        String token = Jwts.builder()
                .header()
                .type("JWT")
                .keyId("rsa-key-1")
                .and()
                .id(jti)
                .subject(email)
                .issuer(rsaKeyProperties.issuer())
                .issuedAt(Date.from(now))
                .expiration(exp)
                .claim("merchant_id", merchantId.toString())
                .claim("role", role)
                .claim("type", TokenType.ACCESS.name())
                .claim("rti", refreshJti)               // paired refresh JTI — for logout revocation
                .signWith(rsaPrivateKey, Jwts.SIG.RS256)
                .compact();

        return new IssuedToken(token, jti, exp.toInstant());
    }

    private IssuedToken buildRefreshToken(String email, UUID merchantId) {
        Instant now = Instant.now();
        Date exp = Date.from(now.plusMillis(rsaKeyProperties.refreshTokenExpiryMs()));
        String jti = UUID.randomUUID().toString();

        String token = Jwts.builder()
                .header()
                .type("JWT")
                .keyId("rsa-key-1")
                .and()
                .id(jti)
                .subject(email)
                .issuer(rsaKeyProperties.issuer())
                .issuedAt(Date.from(now))
                .expiration(exp)
                .claim("merchant_id", merchantId.toString())
                .claim("type", TokenType.REFRESH.name())
                .signWith(rsaPrivateKey, Jwts.SIG.RS256)
                .compact();

        return new IssuedToken(token, jti, exp.toInstant());
    }

    /*---------------------- Token Validation ------------------------*/

    public Claims validateAccessToken(String accessToken) {
        Claims claims = parseClaims(accessToken);

        if (!TokenType.ACCESS.name().equals(claims.get("type", String.class))) {
            throw new JwtException("Refresh token cannot be used for API access");
        }

        if (blockListService.isBlockListed(extractJti(claims))) {
            throw new JwtException("Token has been revoked");
        }

        return claims;
    }

    public Claims validateRefreshToken(String refreshToken) {
        Claims claims = parseClaims(refreshToken);

        if (!TokenType.REFRESH.name().equals(claims.get("type", String.class))) {
            throw new JwtException("Not a refresh token");
        }

        if (blockListService.isBlockListed(extractJti(claims))) {
            throw new JwtException("Refresh token has been revoked");
        }

        return claims;
    }

    /**
     * Parse a refresh token for the logout flow: signature + type checks only, no blocklist
     * lookup (an already-blocklisted token still needs to be handled idempotently). Callers
     * must catch {@link JwtException} and fall back to the {@code rti} claim of the access
     * token when parsing fails.
     */
    public Claims parseRefreshForLogout(String refreshToken) {
        Claims claims = parseClaims(refreshToken);

        if (!TokenType.REFRESH.name().equals(claims.get("type", String.class))) {
            throw new JwtException("Not a refresh token");
        }

        return claims;
    }

    public void invalidateTokens(@Nullable Claims accessTokenClaims, @Nonnull Claims refreshTokenClaims) {
        String accessJti = accessTokenClaims != null ? extractJti(accessTokenClaims) : null;
        Instant accessExpiresAt = accessTokenClaims != null ? extractExpiry(accessTokenClaims): null;

        String refreshJti = extractJti(refreshTokenClaims);
        Instant refreshExpiresAt = extractExpiry(refreshTokenClaims);

        blockListService.blockListPair(accessJti, accessExpiresAt, refreshJti, refreshExpiresAt);
    }

    private Claims parseClaims(String token) {
        try{
            return Jwts.parser()
                    .verifyWith(rsaPublicKey)
                    .requireIssuer(rsaKeyProperties.issuer())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new JwtException("Token has expired", e);
        } catch (SignatureException e) {
            throw new JwtException("Invalid token signature", e);
        } catch (MalformedJwtException e) {
            throw new JwtException("Malformed token", e);
        } catch (JwtException e) {
            throw new JwtException("Token validation failed", e);
        }
    }

    /*--------------------- Helpers ---------------------------------*/

    public String extractJti(Claims claims) {
        return claims.getId();
    }

    public Instant extractExpiry(Claims claims) {
        return claims.getExpiration().toInstant();
    }

    public List<? extends GrantedAuthority> extractRoles(Claims claims) {
        Object raw = claims.get("role");

        if (raw instanceof List<?> roles) {
            return roles.stream()
                    .filter(String.class::isInstance)
                    .map(role -> new SimpleGrantedAuthority((String) role))
                    .toList();
        }

        return Collections.emptyList();
    }

    public UUID extractMerchantId(Claims claims) {
        return UUID.fromString(claims.get("merchant_id", String.class));
    }

    public String extractSubject(Claims claims) {
        return claims.getSubject();
    }

    @Nullable
    public String extractRefreshJti(Claims accessClaims) {
        return accessClaims.get("rti", String.class);
    }
}
