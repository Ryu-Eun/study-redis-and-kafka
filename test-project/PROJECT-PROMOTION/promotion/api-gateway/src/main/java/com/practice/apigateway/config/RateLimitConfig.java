package com.practice.apigateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimitConfig {

    // redisRateLimiter: API Gateway로 오는 모든 요청을 Redis를 사용해서 제한
    @Bean
    public RedisRateLimiter redisRateLimiter() {
        // ReplenishRate: 초당 허용되는 요청수, BurstCapacity: 최대 누적 가능한 요청수
        return new RedisRateLimiter(10,20);
    }

    @Bean
    public KeyResolver userKeyResolver(){
        // 유저별로 반복되는 요청을 제한.
        // X-USER-ID가 있으면 그걸 토대로 같은 유저인지 파악하고, 없으면 IP주소로 제한
        return exchange -> Mono.just(
                exchange.getRequest().getHeaders().getFirst("X-User-ID") != null ?
                        exchange.getRequest().getHeaders().getFirst("X-User-ID") :
                        exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
        );
    }

}
