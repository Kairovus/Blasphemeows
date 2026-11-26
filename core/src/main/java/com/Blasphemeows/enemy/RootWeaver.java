package com.Blasphemeows.enemy;

import com.Blasphemeows.player.Player;

/**
 * Branch 3: Overgrown Spire - Special / Controller Enemy
 * RootWeaver:
 * Channels nature sorcery to sprout clutching thorny roots beneath the player's feet,
 * triggering the Player's RootedState.
 * While rooted, the player is immobilized and must alternate A/D or Left/Right keys to break free!
 */
public class RootWeaver extends Enemy {
    private float channelTimer = 0f;
    private final float rootCooldown = 6.0f;
    private float cooldownTimer = 3.0f; // first cast ready after 3s
    private boolean isChanneling = false;

    public RootWeaver(float x, float y) {
        super("Root Weaver", x, y, 34f, 54f, 105, 65f);
        this.xpReward = 5; // Special enemy reward
        this.scoreType = com.Blasphemeows.score.EnemyScoreType.SPECIAL_REGIONAL;
    }

    @Override
    public void update(float delta, Player player) {
        updateStatusEffects(delta);
        if (isStunnedOrFrozen() || !isAlive() || player == null) {
            isChanneling = false;
            telegraphingAttack = false;
            return;
        }

        facingRight = player.getX() > x;
        float distToPlayer = Math.abs(player.getX() - x);

        if (isChanneling) {
            telegraphingAttack = true;
            channelTimer += delta;
            // Telegraphing root eruption
            if (channelTimer >= 1.0f) {
                isChanneling = false;
                telegraphingAttack = false;
                channelTimer = 0f;
                // Trap the player in roots if within range and not dodging
                if (distToPlayer < 280f && !player.isInvincible()) {
                    System.out.println("🌿🕸️ [Root Weaver] THORNY ROOTS ERUPT BENEATH THE PLAYER!");
                    player.applyRootTrap();
                } else {
                    System.out.println("🌿 [Root Weaver] Roots missed! Player avoided the eruption zone.");
                }
            }
            return;
        }

        cooldownTimer += delta;
        if (cooldownTimer >= rootCooldown && distToPlayer < 260f) {
            cooldownTimer = 0f;
            isChanneling = true;
            telegraphingAttack = true;
            channelTimer = 0f;
            System.out.println("⚠️ [Root Weaver] Chanting ritual: sprouting roots under player's feet (Telegraph: 1.0s)!");
            return;
        }

        // Maintain distance
        if (distToPlayer < 120f) {
            x += (facingRight ? -1f : 1f) * moveSpeed * delta;
        } else if (distToPlayer > 200f) {
            x += (facingRight ? 1f : -1f) * moveSpeed * delta;
        }
    }

    public boolean isChanneling() {
        return isChanneling;
    }
}
