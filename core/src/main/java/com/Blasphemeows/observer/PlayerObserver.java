package com.Blasphemeows.observer;

import com.Blasphemeows.core.GameManager;

/**
 * Observer Pattern: PlayerObserver
 * Decouples player state and combat events from UI/HUD and game systems.
 */
public interface PlayerObserver {
    void onHealthChanged(int currentHP, int maxHP);
    void onStaminaChanged(float currentStamina, float maxStamina);
    void onLevelChanged(int level, int currentXP, int nextLevelXP, int attributePoints);
    void onAbilityUnlocked(GameManager.Ability ability);
    void onStateChanged(String stateName);
    void onCombatLog(String message);
}
