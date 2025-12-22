package com.shop.core.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.Random;

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


    /**
     * Access Token 생성
     * @param memberId
     * @param email
     * @return
     */
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

    /**
     * RefreshToken 생성
     * @return
     */
    public String createRefreshToken() {
        Date now = new Date();
        Date validity = new Date(now.getTime() + refreshTokenValidityTime);

        return Jwts.builder()
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 토큰에서 memberId 추출
     * @param token
     * @return
     */
    public Long getMemberId(String token) {
        return Long.parseLong(
                Jwts.parserBuilder()
                        .setSigningKey(key)
                        .build()
                        .parseClaimsJws(token).getBody().getSubject()
        );
    }

    /**
     * 토큰에서 Email 추출
     * @param token
     * @return
     */

    public String getEmail(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody().get("email", String.class);
    }


    /**
     * 토큰 유효성 검증
     * @param token
     * @return
     */
    public boolean validateToken(String token) {
        Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
        try {
            return true;
        }catch (ExpiredJwtException e){
            log.error("만료된 JWT Token 입니다.");
            return false;
        }catch (UnsupportedJwtException e){
            log.error("지원하지 않는 JWT Token 입니다.");
            return false;
        }catch (MalformedJwtException e){
            log.error("잘못된 JWT 토큰입니다.");
            return false;
        }catch (SignatureException e){
            log.error("JWT 서명이 유효하지 않습니다.");
            return false;
        }
        catch (IllegalArgumentException e) {
            log.error("JWT 토큰이 비어있습니다.");
            return false;
        }
        catch (JwtException e) {
            log.error("Invalid JWT Token: {}", e.getMessage());
            return false;
        }
    }

    //



}
