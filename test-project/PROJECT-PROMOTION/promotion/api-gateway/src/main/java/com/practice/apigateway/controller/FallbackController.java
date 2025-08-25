package com.practice.apigateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/users")
    public Mono<ResponseEntity<Map<String, Object>>> userFallback(){
        Map<String, Object> response = Map.of(
                "status", "service_unavailable",
                "message", "사용자 서비스가 일시 중단되었습니다. 잠시 . 다시 시도해주세요.",
                "timestamp", System.currentTimeMillis(),
                "retry_after_seconds", 30
        );

        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response)); // 503 ERROR
    }
}
