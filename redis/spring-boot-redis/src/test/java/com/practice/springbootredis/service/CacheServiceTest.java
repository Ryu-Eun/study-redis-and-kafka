package com.practice.springbootredis.service;

import com.practice.springbootredis.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CacheServiceTest {

    @Autowired
    private CacheService cacheService;

    @Test
    void 캐시에_데이터_넣기(){
        //given
        User user = new User(1L,"testUser", "test@example.com", LocalDateTime.now());

        //when
        cacheService.setCacheData("user:1", user, 60);
        Optional<User> cachedUser = cacheService.getCacheData("user:1", User.class);

        //then
        assertTrue(cachedUser.isPresent());
        assertEquals(user.getUsername(), cachedUser.get().getUsername());

    }

}