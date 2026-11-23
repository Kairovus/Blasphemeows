package com.Blasphemeows.combat;

import com.Blasphemeows.player.Player;

/**
 * Strategy Pattern: AttackStrategy
 * Encapsulates weapon attack behavior: hitbox sizes, damage multipliers,
 * stamina costs, cooldowns, and special properties (such as Greatsword shield breaking).
 */
public interface AttackStrategy {
    String getWeaponName();
    String getDescription();
    void executeLightAttack(Player player);
    void executeHeavyAttack(Player player);
    float getStaminaCost(boolean isHeavy);
    float getCooldown(boolean isHeavy);
}
