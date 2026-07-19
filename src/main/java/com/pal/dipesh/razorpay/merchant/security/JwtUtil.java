package com.pal.dipesh.razorpay.merchant.security;

import com.pal.dipesh.razorpay.common.enums.TokenType;
import com.pal.dipesh.razorpay.merchant.entity.AppUser;
import com.pal.dipesh.razorpay.merchant.security.rsa.RsaKeyProperties;
import com.pal.dipesh.razorpay.merchant.security.rsa.TokenPair;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
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

    private final RsaKeyProperties rsaKeyProperties;
    private final RSAPrivateKey rsaPrivateKey;
    private final RSAPublicKey rsaPublicKey;
    // private final TokenBlocklistService blocklistService; // TODO: To be implemented later using Redis to block expired tokens

    private record IssuedToken(String token, Instant expiresAt) {}

    /*----------------------- Token generation -----------------------*/
    public TokenPair generateTokenPair(String email, UUID merchantId, List<String> role) {
        IssuedToken accessToken = buildToken(email, merchantId, role, TokenType.ACCESS);
        IssuedToken refreshToken = buildToken(email, merchantId, role, TokenType.REFRESH);

        return new TokenPair(accessToken.token(), refreshToken.token(), accessToken.expiresAt(), refreshToken.expiresAt());
    }

    private IssuedToken buildToken(String email, UUID merchantId, List<String> role, TokenType type) {
        Instant now = Instant.now();

        long expiryMs = switch (type) {
            case ACCESS -> rsaKeyProperties.accessTokenExpiryMs();
            case REFRESH -> rsaKeyProperties.refreshTokenExpiryMs();
        };

        Date exp = Date.from(now.plusMillis(expiryMs));

        String token = Jwts.builder()
                .header()
                .type("JWT")
                .keyId("rsa-key-1")                 // for RSA key rotation support
                .and()
                .id(UUID.randomUUID().toString())       // jti — for blocklist
                .subject(email)
                .issuer(rsaKeyProperties.issuer())
                .issuedAt(Date.from(now))
                .expiration(exp)
                .claim("merchant_id", merchantId.toString())
                .claim("role", role)
                .claim("type", type.name())
                .signWith(rsaPrivateKey, Jwts.SIG.RS256)
                .compact();

        return new IssuedToken(token, exp.toInstant());
    }

    /*---------------------- Token Validation ------------------------*/
    public Claims validateAccessToken(String accessToken) {
        Claims claims = parseClaims(accessToken);

        if (!TokenType.ACCESS.name().equals(claims.get("type", String.class))) {
            throw new JwtException("Refresh token cannot be used for API access");
        }

        // Check blocklist (Redis lookup)
//        if (blocklistService.isBlocklisted(claims.getId())) {
//            throw new JwtException("Token has been revoked");
//        }

        return claims;
    }

    public Claims validateRefreshToken(String refreshToken) {
        Claims claims = parseClaims(refreshToken);

        if (!TokenType.REFRESH.name().equals(claims.get("type", String.class))) {
            throw new JwtException("Not a refresh token");
        }

//        if (blocklistService.isBlocklisted(claims.getId())) {
//            throw new JwtException("Refresh token has been revoked");
//        }

        return claims;
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
}
