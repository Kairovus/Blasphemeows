package com.blasphemeows.backend.dto;

import java.time.Instant;

public class ScoreResponse {

    private Long id;
    private String username;
    private long score;
    private Instant createdAt;

    public ScoreResponse(Long id, String username, long score, Instant createdAt) {
        this.id = id;
        this.username = username;
        this.score = score;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public long getScore() {
        return score;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
