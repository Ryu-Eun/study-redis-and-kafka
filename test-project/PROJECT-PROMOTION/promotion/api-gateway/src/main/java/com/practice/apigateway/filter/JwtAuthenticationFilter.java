package com.practice.apigateway.filter;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerExchangeFilterFunction;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    @LoadBalanced
    private final WebClient webClient;

    // ReactorLoadBalancerExchangeFilterFunction이 WebClient의 모든 요청을 가로채서 서비스 이름을 실제 주소로 변환하고 선택된 서버로 요청 전달
    public JwtAuthenticationFilter(ReactorLoadBalancerExchangeFilterFunction lbFunction) {
        super(Config.class);
        this.webClient = WebClient.builder()
                .filter(lbFunction) // 로드밸런싱 기능 추가
                .baseUrl("http://user-service") // 기본 url 설정
                .build();
    }

    @Override
    public GatewayFilter apply(Config config) {
        // exchange: 현재 HTTP 요청/응답의 모든 정보, chain: 다음 필터로 넘어가는 체인
        return (exchange, chain) -> {
            // request의 Authorization 헤더 추출
            String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);

                return validateToken(token)
                        .flatMap(userId -> proceedWithUserId(userId, exchange, chain))
                        .switchIfEmpty(chain.filter(exchange)) // validateToken이 빈 결과물을 반환하면 (토큰이 유효하지 않다면) 헤더 추가없이 다음 필터로
                        .onErrorResume(e -> handleAuthenticationError(exchange, e)); // 어떤 단계에서든 에러가 발생하면 401 에러
            }

            return chain.filter(exchange);
        };
    }

    private Mono<Void> handleAuthenticationError(ServerWebExchange exchange, Throwable e) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    private Mono<Long> validateToken(String token) {
        return webClient.post()
                .uri("/api/v1/users/validate-token")
                .bodyValue("{\"token\":\"" + token + "\"}")
                .header("Content-Type", "application/json")
                .retrieve() // 비동기적으로 실제 HTTP 요청 실행
                .bodyToMono(Map.class)// 서버에서 온 JSON 응답을 Map으로 변환
                .map(response -> Long.valueOf(response.get("id").toString()) );
    }

    private Mono<Void> proceedWithUserId(Long userId, ServerWebExchange exchange, GatewayFilterChain chain) {
        // 새로운 요청 객체 생성
        ServerHttpRequest newRequest = exchange.getRequest()
                .mutate()
                .header("X-USER-ID", String.valueOf(userId))
                .build();

        // 새로운 exchange 객체 생성
        ServerWebExchange newExchange = exchange.mutate()
                .request(newRequest)
                .build();

        return chain.filter(newExchange);
    }

    public static class Config{
        // 필터 구성을 위한 설정 클래스
        // 지금은 항상 같은 방식으로 동작하는 필터라 빈 값이어도 괜찮다
    }

}
