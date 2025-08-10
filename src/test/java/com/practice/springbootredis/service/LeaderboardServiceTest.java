package com.practice.springbootredis.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class LeaderboardServiceTest {

    @Autowired
    private LeaderboardService leaderboardService;

    @Test
    void 값을_추가한_후에_해당_유저_순위_조회(){
        //given
        String userId = "testUser";
        double score = 100.0;

        //when
        leaderboardService.addScore(userId, score);
        List<String> topPlayers = leaderboardService.getTopPlayers(1); // 1명만 조회
        Long rank = leaderboardService.getUserRank(userId);

        //then
        assertFalse(topPlayers.isEmpty());
        assertEquals(userId, topPlayers.get(0));
        assertEquals(0L, rank);
    }

}