package com.Blasphemeows.enemy;

import com.Blasphemeows.player.Player;

/**
 * TrainingDummy (Target Practice Station):
 * Located in the Main Hub.
 * Features two modes:
 * 1. Passive Mode: Remains stationary and auto-regenerates HP for combo practice.
 * 2. Aggressive Mode: Executes telegraphed practice strikes to test dodging, i-frames, and shield-breaking.
 */
public class TrainingDummy extends Enemy {
    private boolean aggressiveMode = false;
    private float regenTimer = 0f;
    private float attackTimer = 0f;
    private float windupTimer = 0f;
    private boolean isWindingUp = false;

    public TrainingDummy(float x, float y) {
        super("Target Practice Golem", x, y, 40f, 56f, 600, 0f);
        this.xpReward = 0; // Practice dummy yields no XP
    }

    public void toggleMode() {
        this.aggressiveMode = !this.aggressiveMode;
        this.isWindingUp = false;
        this.attackTimer = 0f;
        this.windupTimer = 0f;
        clearAttackHitbox();
        System.out.println("[Target Practice] Switched to: " + (aggressiveMode ? "AGGRESSIVE MODE" : "PASSIVE MODE"));
    }

    @Override
    public void update(float delta, Player player) {
        updateStatusEffects(delta);
        if (player == null) return;

        facingRight = player.getX() > x;

        // Auto-regenerate HP back to full over time
        if (hp < maxHp && !isStunnedOrFrozen()) {
            regenTimer += delta;
            if (regenTimer >= 0.8f) {
                regenTimer = 0f;
                hp = Math.min(maxHp, hp + 80);
            }
        }

        if (aggressiveMode && isAlive() && !isStunnedOrFrozen()) {
            float distToPlayer = Math.abs(player.getX() - x);

            if (isWindingUp) {
                windupTimer += delta;
                isVulnerableWindow = true; // Can be stunned by heavy attack during windup!

                if (currentAttackHitbox == null || currentAttackHitbox.isFinished()) {
                    float hbW = 60f;
                    float hbH = 44f;
                    float hbX = facingRight ? (x + width) : (x - hbW);
                    currentAttackHitbox = new com.Blasphemeows.combat.Hitbox(hbX, y + 6f, hbW, hbH, 12f, 0.8f, 0.25f, 0.2f, false);
                } else {
                    float hbX = facingRight ? (x + width) : (x - currentAttackHitbox.getWidth());
                    currentAttackHitbox.setPosition(hbX, y + 6f);
                    currentAttackHitbox.update(delta);
                }

                if (windupTimer >= 0.8f) {
                    isWindingUp = false;
                    isVulnerableWindow = false;
                    windupTimer = 0f;
                    // Practice strike
                    if (distToPlayer < 75f && !player.isInvincible()) {
                        player.takeDamage(12);
                        System.out.println("🤖💥 [Practice Golem] Landed practice strike! (12 dmg)");
                    }
                    clearAttackHitbox();
                }
                return;
            }

            attackTimer += delta;
            if (attackTimer >= 2.5f && distToPlayer < 90f) {
                attackTimer = 0f;
                isWindingUp = true;
                windupTimer = 0f;
                System.out.println("⚠️ [Practice Golem] Winding up practice strike! (Dodge now!)");
            }
        } else {
            isWindingUp = false;
            isVulnerableWindow = false;
            clearAttackHitbox();
        }
    }

    public boolean isAggressiveMode() {
        return aggressiveMode;
    }

    public void setAggressiveMode(boolean aggressiveMode) {
        this.aggressiveMode = aggressiveMode;
    }
}
