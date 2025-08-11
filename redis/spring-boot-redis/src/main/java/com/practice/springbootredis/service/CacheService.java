package com.practice.springbootredis.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;


@Service
@Slf4j
public class CacheService {
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public CacheService(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    // set cache data
    public void setCacheData(String key, Object data, long timeoutSeconds){
        try{
            redisTemplate.opsForValue().set(key, data, timeoutSeconds, TimeUnit.SECONDS);
            log.info("Data cached successfully for key: {}", key);
        } catch (Exception e){
            log.error("Error caching data:{ }", e.getMessage());
            throw new RuntimeException("Cache operation failed", e);
        }

    }

    // get cache data
    public <T> Optional<T> getCacheData(String key, Class<T> clazz){
        try{
            Object data = redisTemplate.opsForValue().get(key);
            if(data == null){
                return Optional.empty();
            }
            // 읽어올때 LinkedHashMap형태로 불러오게 되는데, convertValue가 원하는 Java Object로 변환해줌
            return Optional.of(objectMapper.convertValue(data, clazz)); // convertValue(원본데이터, 변환할타입)
        } catch (Exception e){
            log.error("Error retrieving cached data for: {} ", e.getMessage());
            return Optional.empty();
        }
    }

    // delete cache data
    public void deleteCachedData(String key){
        try{
            redisTemplate.delete(key);
            log.info("Data cached deleted successfully for key: {}", key);
        } catch (Exception e){
            log.error("Error deleting cached data for: {} ", e.getMessage());
            throw new RuntimeException("Cache operation failed", e);
        }
    }

}
