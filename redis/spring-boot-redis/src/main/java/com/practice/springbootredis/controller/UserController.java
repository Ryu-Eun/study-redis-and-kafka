package com.practice.springbootredis.controller;

import com.practice.springbootredis.domain.User;
import com.practice.springbootredis.service.CacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/users")
@Slf4j
public class UserController {
    private final CacheService cacheService;

    @Autowired
    public UserController(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(@PathVariable Long id){
        return cacheService.getCacheData("user:" + id, User.class)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user){
        user.setCreatedAt(LocalDateTime.now());
        cacheService.setCacheData("user:" + user.getId(), user, 3600);
        return ResponseEntity.ok(user);
    }

}
