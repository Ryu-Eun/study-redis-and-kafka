package com.practice.springbootredis.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class User implements Serializable { // Serializable 인터페이스 추가 필수
    private Long id;
    private String username;
    private String email;
    private LocalDateTime createdAt;
}
