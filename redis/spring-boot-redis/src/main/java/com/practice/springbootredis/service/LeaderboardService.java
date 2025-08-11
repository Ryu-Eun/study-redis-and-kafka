package com.practice.springbootredis.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;


/**
 * ZSET 생긴형태 - 하나의 키에 여러 member-score
 *Key: "game:leaderboard"
 * Members: {
 *     "user123": 1500,
 *     "user456": 2000,
 *     "user789": 1200
 * }
 *
 */
@Service
@Slf4j
public class LeaderboardService {
    private final RedisTemplate<String, String> redisTemplate;
    private static final String LEADERBOARD_KEY = "game:leaderboard";

    @Autowired
    public LeaderboardService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // 유저 점수 추가
    public void addScore(String userId, double score) {
        redisTemplate.opsForZSet().add(LEADERBOARD_KEY, userId, score);
        log.info("Score added for user: {} with score: {}", userId, score);
    }

    // 오름차순 플레이어 전체 등수 조회
    public List<String> getTopPlayers(int count) {
        Set<String> topScores = redisTemplate.opsForZSet().reverseRange(LEADERBOARD_KEY, 0, count - 1);
        return new ArrayList<>(topScores != null ? topScores : Collections.EMPTY_SET);
    }

    // 해당 유저 등수 조회
    public Long getUserRank(String userId) {
        return redisTemplate.opsForZSet().reverseRank(LEADERBOARD_KEY, userId);
    }

    // 해당 유저 점수 조회
    public Double getUserScore(String userId) {
        return redisTemplate.opsForZSet().score(LEADERBOARD_KEY, userId);
    }

}
