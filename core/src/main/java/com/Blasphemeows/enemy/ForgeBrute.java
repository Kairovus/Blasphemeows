package com.Blasphemeows.enemy;

import com.Blasphemeows.player.Player;

/**
 * Branch 2: Ashen Foundry - Melee Enemy
 * ForgeBrute: Slow, tanky behemoth wielding an iron forge hammer. Heavy damage.
 */
public class ForgeBrute extends Enemy {
    private float swingTimer = 0f;
    private final float swingCooldown = 2.8f;
    private boolean isWindingUp = false;
    private float windupTimer = 0f;

    public ForgeBrute(float x, float y) {
        super("Forge Brute", x, y, 48f, 60f, 160, 45f); // High HP (160), slow speed (45)
    }

    @Override
    public void update(float delta, Player player) {
        updateStatusEffects(delta);
        if (isStunnedOrFrozen() || !isAlive() || player == null) {
            isWindingUp = false;
            telegraphingAttack = false;
            clearAttackHitbox();
            return;
        }

        facingRight = player.getX() > x;
        float distToPlayer = Math.abs(player.getX() - x);

        if (isWindingUp) {
            telegraphingAttack = true;
            windupTimer += delta;

            if (currentAttackHitbox == null || currentAttackHitbox.isFinished()) {
                float hbW = 72f;
                float hbH = 50f;
                float hbX = facingRight ? (x + width) : (x - hbW);
                currentAttackHitbox = new com.Blasphemeows.combat.Hitbox(hbX, y, hbW, hbH, 32f, 0.8f, 0.25f, 0.3f, true);
            } else {
                float hbX = facingRight ? (x + width) : (x - currentAttackHitbox.getWidth());
                currentAttackHitbox.setPosition(hbX, y);
                currentAttackHitbox.update(delta);
            }

            // Visual telegraph: heavy hammer raised
            if (windupTimer >= 0.8f) {
                isWindingUp = false;
                telegraphingAttack = false;
                windupTimer = 0f;
                // Deliver hammer smash
                if (distToPlayer < 70f && Math.abs(player.getY() - y) < 40f) {
                    player.takeDamage(32); // Heavy damage!
                    System.out.println("🔨💥 [Forge Brute] HEAVY FORGE SMASH! Crushed player for 32 damage!");
                }
                clearAttackHitbox();
            }
            return;
        }

        swingTimer += delta;
        if (distToPlayer < 60f && swingTimer >= swingCooldown) {
            swingTimer = 0f;
            isWindingUp = true;
            telegraphingAttack = true;
            windupTimer = 0f;
            System.out.println("⚠️ [Forge Brute] Raising heavy forge hammer (windup)...");
            return;
        }

        if (distToPlayer >= 50f) {
            x += (facingRight ? 1f : -1f) * moveSpeed * delta;
        }
    }
}
