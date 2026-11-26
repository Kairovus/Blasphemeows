package com.Blasphemeows.score;

import com.Blasphemeows.enemy.Enemy;

/**
 * Accumulates run score from defeated enemies. Submit via ApiClient when a run ends.
 */
public final class ScoreTracker {
    private static final ScoreTracker INSTANCE = new ScoreTracker();

    private long currentScore;

    private ScoreTracker() {}

    public static ScoreTracker getInstance() {
        return INSTANCE;
    }

    public void addKill(Enemy enemy) {
        if (enemy == null) return;
        currentScore += enemy.getScoreType().getPoints();
    }

    public long getCurrentScore() {
        return currentScore;
    }

    public long takeScoreForSubmit() {
        long value = currentScore;
        currentScore = 0;
        return value;
    }

    public void reset() {
        currentScore = 0;
    }
}
