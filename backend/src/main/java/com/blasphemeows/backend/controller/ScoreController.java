package com.blasphemeows.backend.controller;

import com.blasphemeows.backend.dto.ScoreRequest;
import com.blasphemeows.backend.dto.ScoreResponse;
import com.blasphemeows.backend.model.User;
import com.blasphemeows.backend.service.ScoreService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/scores")
public class ScoreController {

    private final ScoreService scoreService;

    public ScoreController(ScoreService scoreService) {
        this.scoreService = scoreService;
    }

    @PostMapping
    public ScoreResponse submit(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ScoreRequest request) {
        return scoreService.submit(user, request);
    }

    @GetMapping("/me")
    public List<ScoreResponse> myScores(@AuthenticationPrincipal User user) {
        return scoreService.myScores(user);
    }
}
