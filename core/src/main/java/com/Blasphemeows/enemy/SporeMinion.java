package com.Blasphemeows.enemy;

import com.Blasphemeows.player.Player;

/**
 * Branch 3: Overgrown Spire - Minion
 * SporeMinion: Small, low HP, fast minion spawned by Spore Summoner.
 */
public class SporeMinion extends Enemy {
    private float biteWindup;

    public SporeMinion(float x, float y) {
        super("Spore Minion", x, y, 20f, 20f, 25, 130f);
        this.xpReward = 1;
        this.scoreType = com.Blasphemeows.score.EnemyScoreType.MINION;
    }

    @Override
    public void update(float delta, Player player) {
        updateStatusEffects(delta);
        if (isStunnedOrFrozen() || !isAlive() || player == null) return;

        facingRight = player.getX() > x;
        float distToPlayer = Math.abs(player.getX() - x);

        // Swarms directly towards player
        if (distToPlayer > 24f) {
            telegraphingAttack = false;
            x += (facingRight ? 1f : -1f) * moveSpeed * delta;
        } else {
            telegraphingAttack = true;
            biteWindup += delta;
            if (biteWindup < 0.3f) return;
            telegraphingAttack = false;
            player.takeDamage(8);
            hp = 0; // Sacrifices itself on contact
            System.out.println("🍄💥 [Spore Minion] Detonated on player for 8 damage!");
        }
    }
}
