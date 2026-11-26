package com.Blasphemeows.enemy;

import com.Blasphemeows.player.Player;

/**
 * Branch 2: Ashen Foundry - Ranged Enemy
 * SlagHurler: Uses tongs to hurl molten slag and crucible fire from afar.
 */
public class SlagHurler extends Enemy {
    private float tossTimer = 0f;
    private float tossWindup = 0f;
    private final float tossInterval = 3.2f;

    public SlagHurler(float x, float y) {
        super("Slag Hurler", x, y, 36f, 50f, 100, 40f);
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

        // Keep distance
        if (distToPlayer < 120f) {
            x += (facingRight ? -1f : 1f) * moveSpeed * delta;
        } else if (distToPlayer > 280f) {
            x += (facingRight ? 1f : -1f) * moveSpeed * delta;
        }

        if (telegraphingAttack) {
            tossWindup += delta;
            if (tossWindup < 0.55f) return;
            telegraphingAttack = false;
            tossWindup = 0f;
            tossTimer = 0f;
            float spawnX = facingRight ? (x + width) : (x - 18f);
            float vx = (player.getX() - x) * 1.15f;
            float vy = 280f; // Lob upward in an arc!
            spawnProjectile(new com.Blasphemeows.combat.Projectile(
                    spawnX, y + 30f, vx, vy, 22f, 3.0f, true, com.Blasphemeows.combat.Projectile.Type.MOLTEN_SLAG
            ));
            System.out.println("🔥 [Slag Hurler] Hurled an arcing glob of molten slag!");
            return;
        }
        tossTimer += delta;
        if (tossTimer >= tossInterval) {
            telegraphingAttack = true;
            tossWindup = 0f;
        }
    }
}
