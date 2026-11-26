package com.Blasphemeows.score;

/**
 * Fixed client-side scoring rules for defeated enemies.
 */
public enum EnemyScoreType {
    NORMAL_REGIONAL(1000),
    REGIONAL_BOSS(30000),
    MINION(100),
    SPECIAL_REGIONAL(1500),
    FINAL_BOSS(50000),
    NONE(0);

    private final int points;

    EnemyScoreType(int points) {
        this.points = points;
    }

    public int getPoints() {
        return points;
    }
}
