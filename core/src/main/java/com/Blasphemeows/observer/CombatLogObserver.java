package com.Blasphemeows.observer;

import com.Blasphemeows.core.GameManager;

/**
 * Concrete Observer that logs combat events, state transitions, and health updates.
 * Can be connected to in-game dialogue, console, or HUD elements.
 */
public class CombatLogObserver implements PlayerObserver {
    @Override
    public void onHealthChanged(int currentHP, int maxHP) {
        System.out.println("[HUD/Observer] HP: " + currentHP + " / " + maxHP);
    }

    @Override
    public void onStaminaChanged(float currentStamina, float maxStamina) {
        // Formatted to 1 decimal place
        System.out.printf("[HUD/Observer] Stamina: %.1f / %.1f%n", currentStamina, maxStamina);
    }

    @Override
    public void onLevelChanged(int level, int currentXP, int nextLevelXP, int attributePoints) {
        System.out.println("[HUD/Observer] Level: " + level + " | XP: " + currentXP + "/" + nextLevelXP + " | Points: " + attributePoints);
    }

    @Override
    public void onAbilityUnlocked(GameManager.Ability ability) {
        System.out.println("[HUD/Observer] New Ability Unlocked: " + ability.getAbilityName() + "!");
    }

    @Override
    public void onStateChanged(String stateName) {
        System.out.println("[Player/State] Transitioned to: " + stateName);
    }

    @Override
    public void onCombatLog(String message) {
        System.out.println("[CombatLog] " + message);
    }
}
