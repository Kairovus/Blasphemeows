package com.Blasphemeows.combat;

import com.badlogic.gdx.graphics.Color;
import com.Blasphemeows.enemy.Enemy;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents an attack hitbox spawned in the world.
 * Supports distinct Startup, Active (damage-dealing), and Recovery phases.
 * Always renders debug visualization while only dealing damage during active attack frames.
 */
public class Hitbox {
    public enum Phase {
        STARTUP(Color.YELLOW),     // Telegraph window - no damage
        ACTIVE(Color.RED),         // Active damage window - deals damage
        RECOVERY(Color.CYAN);      // Wind-down window - no damage

        private final Color debugColor;

        Phase(Color debugColor) {
            this.debugColor = debugColor;
        }

        public Color getDebugColor() {
            return debugColor;
        }
    }

    private float x;
    private float y;
    private float width;
    private float height;
    private float damage;
    private boolean canBreakShield;

    private float startupDuration;
    private float activeDuration;
    private float recoveryDuration;
    private float elapsed = 0f;
    private Phase currentPhase = Phase.STARTUP;

    private boolean finished = false;
    private final Set<Enemy> hitEnemies = new HashSet<Enemy>();

    public Hitbox(float x, float y, float width, float height, float damage,
                  float startupDuration, float activeDuration, float recoveryDuration,
                  boolean canBreakShield) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.damage = damage;
        this.startupDuration = Math.max(0.01f, startupDuration);
        this.activeDuration = Math.max(0.05f, activeDuration);
        this.recoveryDuration = Math.max(0.01f, recoveryDuration);
        this.canBreakShield = canBreakShield;
        this.currentPhase = (startupDuration <= 0.01f) ? Phase.ACTIVE : Phase.STARTUP;
    }

    // Simplified constructor for legacy shockwaves/traps
    public Hitbox(float x, float y, float width, float height, float damage, float lifetime, boolean canBreakShield) {
        this(x, y, width, height, damage, 0.02f, lifetime * 0.7f, lifetime * 0.3f, canBreakShield);
    }

    public void update(float delta) {
        if (finished) return;
        elapsed += delta;

        if (elapsed < startupDuration) {
            currentPhase = Phase.STARTUP;
        } else if (elapsed < startupDuration + activeDuration) {
            currentPhase = Phase.ACTIVE;
        } else if (elapsed < startupDuration + activeDuration + recoveryDuration) {
            currentPhase = Phase.RECOVERY;
        } else {
            finished = true;
        }
    }

    public void setPosition(float newX, float newY) {
        this.x = newX;
        this.y = newY;
    }

    public boolean overlaps(float targetX, float targetY, float targetW, float targetH) {
        if (!isDamageActive()) return false;
        return x < targetX + targetW &&
                x + width > targetX &&
                y < targetY + targetH &&
                y + height > targetY;
    }

    /**
     * Checks if this enemy has already been hit during this specific attack swing.
     */
    public boolean canHitEnemy(Enemy enemy) {
        return isDamageActive() && !hitEnemies.contains(enemy);
    }

    public void registerHit(Enemy enemy) {
        hitEnemies.add(enemy);
    }

    public boolean isDamageActive() {
        return !finished && currentPhase == Phase.ACTIVE;
    }

    public boolean isFinished() {
        return finished;
    }

    public boolean isActive() {
        return !finished;
    }

    public void deactivate() {
        this.finished = true;
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public float getWidth() { return width; }
    public float getHeight() { return height; }
    public float getDamage() { return damage; }
    public boolean canBreakShield() { return canBreakShield; }
    public Phase getCurrentPhase() { return currentPhase; }
    public Color getDebugColor() { return currentPhase.getDebugColor(); }
}
