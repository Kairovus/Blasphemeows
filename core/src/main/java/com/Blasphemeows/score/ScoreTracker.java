package com.Blasphemeows.score;

import com.Blasphemeows.enemy.Enemy;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

/**
 * Accumulates run score from defeated enemies and tracks unsubmitted/pending scores safely.
 * Scores are preserved across guest runs and failed submissions until successfully submitted
 * or explicitly discarded (e.g. New Game).
 */
public final class ScoreTracker {
    private static final ScoreTracker INSTANCE = new ScoreTracker();
    private static final String PREFERENCES_NAME = "com.blasphemeows.score.state";
    private static final String KEY_PENDING_SCORE = "pendingScore";

    private long currentScore = 0L;
    private long pendingScore = 0L;
    private boolean submissionInFlight = false;
    private boolean initialized = false;

    private ScoreTracker() {}

    public static ScoreTracker getInstance() {
        return INSTANCE;
    }

    private void ensureLoaded() {
        if (initialized) {
            return;
        }
        initialized = true;
        if (Gdx.app != null) {
            Preferences prefs = Gdx.app.getPreferences(PREFERENCES_NAME);
            pendingScore = Math.max(0L, prefs.getLong(KEY_PENDING_SCORE, 0L));
        }
    }

    public synchronized void addKill(Enemy enemy) {
        if (enemy == null) return;
        ensureLoaded();
        currentScore += enemy.getScoreType().getPoints();
    }

    public synchronized long getCurrentScore() {
        ensureLoaded();
        return currentScore;
    }

    public synchronized long getPendingScore() {
        ensureLoaded();
        return pendingScore;
    }

    /** Total unsubmitted score (active run + any pending score from prior failed/guest runs). */
    public synchronized long getTotalUnsubmittedScore() {
        ensureLoaded();
        return currentScore + pendingScore;
    }

    public synchronized boolean isSubmissionInFlight() {
        return submissionInFlight;
    }

    /**
     * Moves the active run's score into {@code pendingScore} at run end without erasing it,
     * ensuring guest or offline scores are preserved until submitted or discarded.
     */
    public synchronized long finalizeRunScore() {
        ensureLoaded();
        if (currentScore > 0L) {
            pendingScore += currentScore;
            currentScore = 0L;
            persistPending();
        }
        return pendingScore;
    }

    /**
     * Attempts to begin an async submission for the current/pending score.
     * Returns the positive score snapshot to submit, or 0 if nothing is available
     * or a submission is already in flight (preventing duplicate submissions).
     */
    public synchronized long beginSubmission() {
        ensureLoaded();
        if (submissionInFlight) {
            return 0L;
        }
        long total = finalizeRunScore();
        if (total <= 0L) {
            return 0L;
        }
        submissionInFlight = true;
        return total;
    }

    /**
     * Marks a submission of {@code submittedAmount} as succeeded, deducting only the amount
     * actually accepted by the backend so any newer kills are preserved.
     */
    public synchronized void confirmSubmissionSuccess(long submittedAmount) {
        ensureLoaded();
        pendingScore = Math.max(0L, pendingScore - Math.max(0L, submittedAmount));
        submissionInFlight = false;
        persistPending();
    }

    /**
     * Marks a failed submission without discarding the pending score so it can be retried later.
     */
    public synchronized void markSubmissionFailed() {
        submissionInFlight = false;
        persistPending();
    }

    /**
     * Deprecated unsafe helper retained for compatibility: delegates to {@link #finalizeRunScore()}
     * without silently destroying unsubmitted score.
     */
    public synchronized long takeScoreForSubmit() {
        return finalizeRunScore();
    }

    /** Clears active and pending local scores (e.g. when starting a brand new game). */
    public synchronized void reset() {
        currentScore = 0L;
        pendingScore = 0L;
        submissionInFlight = false;
        initialized = true;
        persistPending();
    }

    private void persistPending() {
        if (Gdx.app == null) {
            return;
        }
        Preferences prefs = Gdx.app.getPreferences(PREFERENCES_NAME);
        if (pendingScore > 0L) {
            prefs.putLong(KEY_PENDING_SCORE, pendingScore);
        } else {
            prefs.remove(KEY_PENDING_SCORE);
        }
        prefs.flush();
    }
}
