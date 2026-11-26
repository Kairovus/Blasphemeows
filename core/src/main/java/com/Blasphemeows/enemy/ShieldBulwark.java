package com.Blasphemeows.enemy;

import com.Blasphemeows.player.Player;

/**
 * Branch 2: Ashen Foundry - Special Enemy
 * ShieldBulwark:
 * Wields a colossal iron tower shield that completely negates all attacks from the front.
 * Mechanics:
 * - Impervious to frontal strikes while guarded.
 * - When winding up to attack, a vulnerability window opens ('isVulnerableWindow = true').
 * - If struck by a Heavy Attack (e.g. Greatsword Heavy) during this window,
 *   his stance is shattered and he is STUNNED for 1.0 second, leaving him wide open for free damage!
 */
public class ShieldBulwark extends Enemy {
    private enum ActionPhase { GUARDING, WINDUP, BASH, RECOVERY }
    private ActionPhase phase = ActionPhase.GUARDING;
    private float phaseTimer = 0f;
    private final float bashCooldown = 3.5f;
    private float cooldownTimer = 0f;

    public ShieldBulwark(float x, float y) {
        super("Shield Bulwark", x, y, 44f, 56f, 180, 50f);
        this.hasFrontShield = true;
        this.xpReward = 5; // Special enemy reward
        this.scoreType = com.Blasphemeows.score.EnemyScoreType.SPECIAL_REGIONAL;
    }

    @Override
    public void update(float delta, Player player) {
        updateStatusEffects(delta);
        if (!isAlive() || player == null) return;

        // If stunned or frozen, vulnerability window and shield are deactivated for free damage!
        if (isStunnedOrFrozen()) {
            isVulnerableWindow = false;
            telegraphingAttack = false;
            phase = ActionPhase.GUARDING;
            phaseTimer = 0f;
            clearAttackHitbox();
            return;
        }

        facingRight = player.getX() > x;
        float distToPlayer = Math.abs(player.getX() - x);
        phaseTimer += delta;

        switch (phase) {
            case GUARDING:
                isVulnerableWindow = false;
                hasFrontShield = true;
                cooldownTimer += delta;
                clearAttackHitbox();

                // Move slowly towards player with shield raised
                if (distToPlayer > 50f) {
                    x += (facingRight ? 1f : -1f) * moveSpeed * delta;
                } else if (cooldownTimer >= bashCooldown) {
                    // Enter attack window!
                    phase = ActionPhase.WINDUP;
                    phaseTimer = 0f;
                    cooldownTimer = 0f;
                    isVulnerableWindow = true; // Staggerable by heavy attack!

                    float hbW = 60f;
                    float hbH = 50f;
                    float hbX = facingRight ? (x + width) : (x - hbW);
                    currentAttackHitbox = new com.Blasphemeows.combat.Hitbox(hbX, y, hbW, hbH, 26f, 0.8f, 0.35f, 0.5f, false);
                    System.out.println("⚠️ [Shield Bulwark] LOWERING SHIELD FOR BASH! (Vulnerable Window OPEN: Hit with Heavy Attack!)");
                }
                break;

            case WINDUP:
                // Vulnerability window lasts 0.8 seconds
                isVulnerableWindow = true;
                if (currentAttackHitbox != null) {
                    float hbX = facingRight ? (x + width) : (x - currentAttackHitbox.getWidth());
                    currentAttackHitbox.setPosition(hbX, y);
                    currentAttackHitbox.update(delta);
                }
                if (phaseTimer >= 0.8f) {
                    isVulnerableWindow = false;
                    phase = ActionPhase.BASH;
                    phaseTimer = 0f;
                    System.out.println("🛡️💥 [Shield Bulwark] SHIELD BASH EXECUTED!");
                    if (distToPlayer < 65f && !player.isInvincible()) {
                        player.takeDamage(26);
                    }
                }
                break;

            case BASH:
                if (currentAttackHitbox != null) {
                    currentAttackHitbox.update(delta);
                }
                if (phaseTimer >= 0.35f) {
                    phase = ActionPhase.RECOVERY;
                    phaseTimer = 0f;
                    clearAttackHitbox();
                }
                break;

            case RECOVERY:
                if (phaseTimer >= 0.5f) {
                    phase = ActionPhase.GUARDING;
                    phaseTimer = 0f;
                    hasFrontShield = true;
                }
                break;
        }
        telegraphingAttack = phase == ActionPhase.WINDUP;
    }

    public boolean isInVulnerableWindow() {
        return isVulnerableWindow;
    }
}
