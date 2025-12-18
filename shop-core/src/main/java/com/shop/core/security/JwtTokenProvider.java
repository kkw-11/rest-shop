package com.shop.core.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenProvider {
    private final Key key;
    private final long accessTokenValidityTime;
    private final long refreshTokenValidityTime;

    public JwtTokenProvider(@Value("${jwt.secret}")String secret,
                            @Value("${jwt.access-token-validity}") long accessTokenValidityTime,
                            @Value("${jwt.rfresh-token-validity}") long refreshTokenValidityTime
                            ){
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.accessTokenValidityTime = accessTokenValidityTime;
        this.refreshTokenValidityTime = refreshTokenValidityTime;
    }

    // Access Token 생성
    public String createAccessToken(Long memberId, String email) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + accessTokenValidityTime);

        return Jwts.builder()
                .setSubject(String.valueOf(memberId))
                .claim("email", email)
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // Refresh Token 생성
    public Long getMemberId(String token) {
        return Long.parseLong(
                Jwts.parserBuilder()
                        .setSigningKey(key)
                        .build()
                        .parseClaimsJws(token).getBody().getSubject()
        );
    }

    // 토큰 유효성 검증
    public boolean validateToken(String token) {
        Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
        try {
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Invalid JWT Token: {}", e.getMessage());
            return false;
        }
    }

}
