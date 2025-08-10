package com.practice.springbootredis.controller;

import com.practice.springbootredis.service.LeaderboardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leaderboard")
@Slf4j
public class LeaderboardController {
    private final LeaderboardService leaderboardService;

    @Autowired
    public LeaderboardController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    // 추가
    @PostMapping("/scores")
    public ResponseEntity<Void> addScore(
            @RequestParam String userId,
            @RequestParam double score) {
        leaderboardService.addScore(userId, score);
        return ResponseEntity.ok().build();
    }

    // 상위 n명
    @GetMapping("/top/{count}")
    public ResponseEntity<List<String>> getTopPlayers(@PathVariable int count) {
        return ResponseEntity.ok(leaderboardService.getTopPlayers(count));
    }

    // 특정 유저 순위
    @GetMapping("/rank/{userId}")
    public ResponseEntity<Long> getUserRank(@PathVariable String userId) {
        Long rank = leaderboardService.getUserRank(userId);
        return rank != null ? ResponseEntity.ok(rank + 1) : ResponseEntity.notFound().build();
    }

}