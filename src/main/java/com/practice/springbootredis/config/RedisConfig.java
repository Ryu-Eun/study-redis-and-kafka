package com.practice.springbootredis.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();

        // 연결 설정 (application.yml의 Redis 설정 정보)
        template.setConnectionFactory(redisConnectionFactory);

        // key Serializer 설정 - 문자열 그대로 Redis에 저장
        template.setKeySerializer(new StringRedisSerializer());

        // value Serializer 설정 - Java객체를 JSON으로 변환해서 Redis에 저장
        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(Object.class);
        template.setValueSerializer(serializer);

        // Hash 자료구조의 value도 JSON으로 직렬화
        template.setHashValueSerializer(serializer);

        return template;
    }

}
