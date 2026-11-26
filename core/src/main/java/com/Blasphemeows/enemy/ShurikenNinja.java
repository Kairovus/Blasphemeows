package com.Blasphemeows.enemy;

import com.Blasphemeows.player.Player;

/**
 * Branch 1: The Forgotten Temple - Ranged Enemy
 * ShurikenNinja: Keeps distance, throws shurikens, and executes evasive dashes when pressured.
 */
public class ShurikenNinja extends Enemy {
    private float throwTimer = 0f;
    private float throwWindup = 0f;
    private final float throwInterval = 2.0f;
    private float escapeDashTimer = 0f;

    public ShurikenNinja(float x, float y) {
        super("Shuriken Ninja", x, y, 28f, 44f, 45, 95f);
    }

    @Override
    public void update(float delta, Player player) {
        updateStatusEffects(delta);
        if (isStunnedOrFrozen() || !isAlive() || player == null) {
            telegraphingAttack = false;
            return;
        }

        facingRight = player.getX() > x;
        float distToPlayer = Math.abs(player.getX() - x);

        escapeDashTimer += delta;
        // Evasive dash if player gets within close quarters
        if (distToPlayer < 70f && escapeDashTimer >= 3.0f) {
            escapeDashTimer = 0f;
            x += (facingRight ? -1f : 1f) * 120f; // Evasive leap backwards
            System.out.println("💨 [Shuriken Ninja] Evasive smoke dash backwards!");
            return;
        }

        // Maintain distance (around 150-250px)
        if (distToPlayer < 140f) {
            x += (facingRight ? -1f : 1f) * moveSpeed * delta;
        } else if (distToPlayer > 260f) {
            x += (facingRight ? 1f : -1f) * moveSpeed * delta;
        }

        // Throw shuriken
        if (telegraphingAttack) {
            throwWindup += delta;
            if (throwWindup < 0.4f) return;
            telegraphingAttack = false;
            throwWindup = 0f;
            throwTimer = 0f;
            float spawnX = facingRight ? (x + width) : (x - 14f);
            float vx = facingRight ? 290f : -290f;
            spawnProjectile(new com.Blasphemeows.combat.Projectile(
                    spawnX, y + 20f, vx, 0f, 12f, 2.5f, true, com.Blasphemeows.combat.Projectile.Type.SHURIKEN
            ));
            System.out.println("🗡️ [Shuriken Ninja] Threw a razor shuriken projectile!");
            return;
        }
        throwTimer += delta;
        if (throwTimer >= throwInterval) {
            telegraphingAttack = true;
            throwWindup = 0f;
        }
    }
}
