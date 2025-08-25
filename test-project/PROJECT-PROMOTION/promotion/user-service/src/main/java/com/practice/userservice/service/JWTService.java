package com.practice.userservice.service;

import com.practice.userservice.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
@Slf4j
public class JWTService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration:3600000}") // 1시간
    private long tokenExpiration;

    // accessToken 생성
    public String generateToken(User user){
        long currentTimeMillis = System.currentTimeMillis();

        return Jwts.builder()
                .subject(user.getEmail())
                .claim("role","USER")
                .issuedAt(new Date(currentTimeMillis))
                .expiration(new Date(currentTimeMillis + tokenExpiration))
                .signWith(Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    // 토큰 검증 및 Claims 반환
    public Claims validateToken(String token) {
        try {
            Claims claims = parseJwtClaims(token);

            if(isTokenExpired(claims)){
                throw new JwtException("Token is expired");
            }

            return claims;
        } catch (JwtException e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid token: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected token validation error: ", e);
            throw new IllegalArgumentException("Token validation failed");
        }
    }

    // 토큰 파싱
    public Claims parseJwtClaims(String token) {
        return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // 기존 accessToken을 바탕으로 토큰시간 연장 (refreshToken을 발급받는 게 아님)
    public String refreshToken(String token) {
        Claims claims = parseJwtClaims(token);

        long currentTimeMillis = System.currentTimeMillis();
        return Jwts.builder()
                .subject(claims.getSubject())
                .claims(claims)
                .issuedAt(new Date(currentTimeMillis))
                .expiration(new Date(currentTimeMillis + tokenExpiration))
                .signWith(Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    // 토큰 만료 여부 확인
    public boolean isTokenExpired(Claims claims) {
        return claims.getExpiration().before(new Date());
    }

}