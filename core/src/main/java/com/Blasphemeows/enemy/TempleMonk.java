package com.Blasphemeows.enemy;

import com.Blasphemeows.player.Player;

/**
 * Branch 1: The Forgotten Temple - Melee Enemy
 * TempleMonk: Fast, agile martial artist with active dash strikes.
 */
public class TempleMonk extends Enemy {
    private float dashCooldown = 2.5f;
    private float dashTimer = 0f;
    private boolean isDashing = false;
    private float dashDuration = 0.25f;
    private float currentDashTime = 0f;
    private boolean preparingDash;
    private float dashWindup;
    private boolean hasHitPlayer;
    private float meleeTimer;
    private float meleeWindup;

    public TempleMonk(float x, float y) {
        super("Temple Monk", x, y, 32f, 48f, 65, 110f);
    }

    @Override
    public void update(float delta, Player player) {
        updateStatusEffects(delta);
        if (isStunnedOrFrozen() || !isAlive() || player == null) {
            isDashing = false;
            preparingDash = false;
            telegraphingAttack = false;
            clearAttackHitbox();
            return;
        }

        facingRight = player.getX() > x;
        float distToPlayer = Math.abs(player.getX() - x);

        if (preparingDash) {
            telegraphingAttack = true;
            dashWindup += delta;
            if (dashWindup >= 0.45f) {
                preparingDash = false;
                isDashing = true;
                currentDashTime = 0f;
                hasHitPlayer = false;
                telegraphingAttack = false;
            }
            return;
        }

        if (!isDashing && distToPlayer <= 40f) {
            meleeTimer += delta;
            if (meleeTimer >= 1.2f) {
                if (meleeWindup == 0f) telegraphingAttack = true;
                meleeWindup += delta;
                if (meleeWindup >= 0.4f) {
                    if (Math.abs(player.getY() - y) < 40f) player.takeDamage(10);
                    meleeTimer = 0f;
                    meleeWindup = 0f;
                    telegraphingAttack = false;
                    System.out.println("👊 [Temple Monk] Landed a monastery strike!");
                }
            }
            return;
        }

        if (meleeWindup > 0f) {
            meleeWindup = 0f;
            telegraphingAttack = false;
        }

        if (isDashing) {
            currentDashTime += delta;
            x += (facingRight ? 1f : -1f) * 320f * delta;

            if (currentAttackHitbox == null || currentAttackHitbox.isFinished()) {
                float hbW = 44f;
                float hbH = 40f;
                float hbX = facingRight ? (x + width) : (x - hbW);
                currentAttackHitbox = new com.Blasphemeows.combat.Hitbox(hbX, y + 4f, hbW, hbH, 14f, 0.01f, dashDuration, 0.1f, false);
            } else {
                float hbX = facingRight ? (x + width) : (x - currentAttackHitbox.getWidth());
                currentAttackHitbox.setPosition(hbX, y + 4f);
                currentAttackHitbox.update(delta);
            }

            // Deal contact damage on dash
            if (!hasHitPlayer && distToPlayer < 36f && Math.abs(player.getY() - y) < 40f) {
                player.takeDamage(14);
                hasHitPlayer = true;
            }

            if (currentDashTime >= dashDuration) {
                isDashing = false;
                telegraphingAttack = false;
                dashTimer = 0f;
                clearAttackHitbox();
            }
            return;
        }

        dashTimer += delta;
        if (dashTimer >= dashCooldown && distToPlayer < 240f && distToPlayer > 50f) {
            preparingDash = true;
            dashWindup = 0f;
            telegraphingAttack = true;
            System.out.println("🥋 [Temple Monk] Palm Dash towards player!");
            return;
        }

        // Standard movement towards player
        if (distToPlayer > 40f) {
            x += (facingRight ? 1f : -1f) * moveSpeed * delta;
        }
    }
}
