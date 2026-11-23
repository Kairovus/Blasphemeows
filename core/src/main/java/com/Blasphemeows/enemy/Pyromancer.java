package com.Blasphemeows.enemy;

import com.Blasphemeows.player.Player;

/**
 * Branch 3: Overgrown Spire - Magic Caster
 * Pyromancer: Chants nature pyromancy, casting damaging mystic fireballs at the player.
 */
public class Pyromancer extends Enemy {
    private float castTimer = 0f;
    private final float castInterval = 2.6f;

    public Pyromancer(float x, float y) {
        super("Spire Pyromancer", x, y, 30f, 48f, 70, 75f);
    }

    @Override
    public void update(float delta, Player player) {
        updateStatusEffects(delta);
        if (isStunnedOrFrozen() || !isAlive() || player == null) return;

        facingRight = player.getX() > x;
        float distToPlayer = Math.abs(player.getX() - x);

        // Keep distance
        if (distToPlayer < 140f) {
            x += (facingRight ? -1f : 1f) * moveSpeed * delta;
        } else if (distToPlayer > 260f) {
            x += (facingRight ? 1f : -1f) * moveSpeed * delta;
        }

        castTimer += delta;
        if (castTimer >= castInterval) {
            castTimer = 0f;
            float spawnX = facingRight ? (x + width) : (x - 20f);
            float vx = facingRight ? 260f : -260f;
            spawnProjectile(new com.Blasphemeows.combat.Projectile(
                    spawnX, y + 24f, vx, 0f, 18f, 3.0f, true, com.Blasphemeows.combat.Projectile.Type.FIREBALL
            ));
            System.out.println("🔥✨ [Pyromancer] Casts a blazing Mystic Fireball projectile!");
        }
    }
}
