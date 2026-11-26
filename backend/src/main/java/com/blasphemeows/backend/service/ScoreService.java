package com.blasphemeows.backend.service;

import com.blasphemeows.backend.dto.ScoreRequest;
import com.blasphemeows.backend.dto.ScoreResponse;
import com.blasphemeows.backend.model.Score;
import com.blasphemeows.backend.model.User;
import com.blasphemeows.backend.repository.ScoreRepository;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScoreService {

    private final ScoreRepository scoreRepository;

    public ScoreService(ScoreRepository scoreRepository) {
        this.scoreRepository = scoreRepository;
    }

    @Transactional
    public ScoreResponse submit(User user, ScoreRequest request) {
        Score score = new Score();
        score.setUser(user);
        score.setScore(request.getScore());
        scoreRepository.save(score);
        return toResponse(score);
    }

    @Transactional(readOnly = true)
    public List<ScoreResponse> myScores(User user) {
        return scoreRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ScoreResponse> leaderboard(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        return scoreRepository.findLeaderboard(PageRequest.of(0, safeLimit)).stream()
                .map(this::toResponse)
                .toList();
    }

    private ScoreResponse toResponse(Score score) {
        return new ScoreResponse(
                score.getId(),
                score.getUser().getUsername(),
                score.getScore(),
                score.getCreatedAt());
    }
}
