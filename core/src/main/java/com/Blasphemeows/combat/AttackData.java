package com.Blasphemeows.combat;

/**
 * Encapsulates frame data, stamina, dimensions, and damage multipliers for an attack in a combo.
 */
public class AttackData {
    private final String name;
    private final boolean isHeavy;
    private final float damageMultiplier;
    private final float staminaCost;
    private final float startupDuration;
    private final float activeDuration;
    private final float baseRecoveryDuration;
    private final float hitboxWidth;
    private final float hitboxHeight;
    private final boolean canBreakShield;

    public AttackData(String name, boolean isHeavy, float damageMultiplier, float staminaCost,
                      float startupDuration, float activeDuration, float baseRecoveryDuration,
                      float hitboxWidth, float hitboxHeight, boolean canBreakShield) {
        this.name = name;
        this.isHeavy = isHeavy;
        this.damageMultiplier = damageMultiplier;
        this.staminaCost = staminaCost;
        this.startupDuration = startupDuration;
        this.activeDuration = activeDuration;
        this.baseRecoveryDuration = baseRecoveryDuration;
        this.hitboxWidth = hitboxWidth;
        this.hitboxHeight = hitboxHeight;
        this.canBreakShield = canBreakShield;
    }

    public String getName() { return name; }
    public boolean isHeavy() { return isHeavy; }
    public float getDamageMultiplier() { return damageMultiplier; }
    public float getStaminaCost() { return staminaCost; }
    public float getStartupDuration() { return startupDuration; }
    public float getActiveDuration() { return activeDuration; }
    public float getBaseRecoveryDuration() { return baseRecoveryDuration; }

    /**
     * Recovery frames are reduced by Dexterity:
     * recovery = baseRecovery / (1.0 + dexterity * 0.04)
     */
    public float getAdjustedRecovery(int dexterity) {
        float recoveryScale = 1.0f + (Math.max(0, dexterity - 10) * 0.04f);
        return Math.max(0.04f, baseRecoveryDuration / recoveryScale);
    }

    public float getTotalDuration(int dexterity) {
        return startupDuration + activeDuration + getAdjustedRecovery(dexterity);
    }

    public float getHitboxWidth() { return hitboxWidth; }
    public float getHitboxHeight() { return hitboxHeight; }
    public boolean canBreakShield() { return canBreakShield; }
}
