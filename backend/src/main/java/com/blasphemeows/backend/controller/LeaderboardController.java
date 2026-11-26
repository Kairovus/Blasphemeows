package com.blasphemeows.backend.controller;

import com.blasphemeows.backend.dto.ScoreResponse;
import com.blasphemeows.backend.service.ScoreService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/leaderboard")
public class LeaderboardController {

    private final ScoreService scoreService;

    public LeaderboardController(ScoreService scoreService) {
        this.scoreService = scoreService;
    }

    @GetMapping
    public List<ScoreResponse> leaderboard(@RequestParam(defaultValue = "10") int limit) {
        return scoreService.leaderboard(limit);
    }
}
