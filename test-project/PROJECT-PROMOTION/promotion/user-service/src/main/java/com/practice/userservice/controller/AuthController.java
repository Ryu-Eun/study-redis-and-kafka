package com.practice.userservice.controller;

import com.practice.userservice.dto.UserDto;
import com.practice.userservice.entity.User;
import com.practice.userservice.service.JWTService;
import com.practice.userservice.service.UserService;
import io.jsonwebtoken.Claims;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
public class AuthController {

    private final JWTService jwtService;
    private final UserService userService;

    public AuthController(JWTService jwtService, UserService userService) {
        this.jwtService = jwtService;
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody UserDto.LoginRequest request) {
        // 로그인 검사
        User user = userService.authenticate(request.getEmail(), request.getPassword());

        // 토큰 발급
        String token = jwtService.generateToken(user);

        return ResponseEntity.ok(UserDto.LoginResponse.builder()
                .token(token)
                .user(UserDto.Response.from(user))
                .build());
    }

    @PostMapping("/validate-token")
    public ResponseEntity<?> validateToken(
            @RequestBody UserDto.TokenRequest request) {
        Claims claims = jwtService.validateToken(request.getToken());

        return ResponseEntity.ok(UserDto.TokenResponse.builder()
                .email(claims.getSubject())
                .valid(true)
                .role(claims.get("role", String.class))
                .build());
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<Map<String, String>> refreshToken(
            @RequestBody UserDto.TokenRequest tokenRequest) {
        String newToken = jwtService.refreshToken(tokenRequest.getToken());

        // singletonMap이 hashMap보다 가벼움. 단일 엔트리에 최적화
        return ResponseEntity.ok(Collections.singletonMap("token", newToken));
    }
}