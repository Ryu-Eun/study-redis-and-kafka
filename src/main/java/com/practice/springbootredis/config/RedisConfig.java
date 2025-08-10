package com.practice.springbootredis.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(ObjectMapper mapper, RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();

        // 연결 설정 (application.yml의 Redis 설정 정보)
        template.setConnectionFactory(redisConnectionFactory);

        // key Serializer 설정 - 문자열 그대로 Redis에 저장
        template.setKeySerializer(new StringRedisSerializer());

        // value Serializer 설정 - Java객체를 JSON으로 변환해서 Redis에 저장
        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(mapper, Object.class);
        template.setValueSerializer(serializer);

        // Hash 자료구조의 value도 JSON으로 직렬화
        template.setHashValueSerializer(serializer);

        return template;
    }

    // Caused by: org.springframework.data.redis.serializer.SerializationException: Could not write JSON: Java 8 date/time type `java.time.LocalDateTime` not supported by default: add Module "com.fasterxml.jackson.datatype:jackson-datatype-jsr310" to enable handling (or disable `MapperFeature.REQUIRE_HANDLERS_FOR_JAVA8_TIMES`) (through reference chain: com.practice.springbootredis.domain.User["createdAt"])
    // 즉,Jackson이 LocalDateTime을 기본적으로 처리를 못함
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Java8 시간 API(LocalDateTime, LocalDate 등) 지원 모듈 등록
        mapper.registerModule(new JavaTimeModule());

        // 날짜를 숫자대신 문자열로 저장
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 모든 접근자, 모든 접근레벨에 Jackson 허용
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        return mapper;
    }

}