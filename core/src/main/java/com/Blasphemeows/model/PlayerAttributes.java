package com.Blasphemeows.model;

import com.Blasphemeows.core.GameManager;
import com.Blasphemeows.observer.PlayerObserver;

import java.util.ArrayList;
import java.util.List;

/**
 * PlayerAttributes (Observer Subject)
 * Stores and manages player stats: Vitality, Strength, Endurance, Dexterity.
 * Also manages Level, Experience (XP), and Attribute Points.
 * Broadcasts changes to registered PlayerObserver instances.
 */
public class PlayerAttributes {
    private final List<PlayerObserver> observers = new ArrayList<PlayerObserver>();

    // RPG Attributes
    private int vitality = 10;   // HP Max = vitality * 12
    private int strength = 10;   // Primary Physical damage scaling = 1.0 + (strength * 0.06)
    private int endurance = 10;  // Max stamina = endurance * 10
    private int dexterity = 10;  // Recovery reduction and secondary weapon damage scaling

    // Level & Experience Progression
    private int level = 1;
    private int currentXP = 0;
    private int attributePoints = 0;

    private int currentHP;
    private float currentStamina;
    private float staminaRegenRate = 18.0f; // stamina per second

    // Healing charges (max 3 times)
    private int maxHealCharges = 3;
    private int healCharges = 3;
    private int healAmount = 45;

    public PlayerAttributes() {
        this.currentHP = getMaxHP();
        this.currentStamina = getMaxStamina();
    }

    public PlayerAttributes(int vitality, int strength, int endurance, int dexterity) {
        this.vitality = vitality;
        this.strength = strength;
        this.endurance = endurance;
        this.dexterity = dexterity;
        this.currentHP = getMaxHP();
        this.currentStamina = getMaxStamina();
    }

    // Observer management
    public void registerObserver(PlayerObserver observer) {
        if (observer != null && !observers.contains(observer)) {
            observers.add(observer);
            observer.onHealthChanged(currentHP, getMaxHP());
            observer.onStaminaChanged(currentStamina, getMaxStamina());
            observer.onLevelChanged(level, currentXP, getNextLevelXP(), attributePoints);
        }
    }

    public void removeObserver(PlayerObserver observer) {
        observers.remove(observer);
    }

    public void notifyCombatLog(String message) {
        for (PlayerObserver observer : observers) {
            observer.onCombatLog(message);
        }
    }

    public void notifyAbilityUnlocked(GameManager.Ability ability) {
        for (PlayerObserver observer : observers) {
            observer.onAbilityUnlocked(ability);
        }
    }

    public void notifyStateChanged(String stateName) {
        for (PlayerObserver observer : observers) {
            observer.onStateChanged(stateName);
        }
    }

    public void notifyLevelChanged() {
        for (PlayerObserver observer : observers) {
            observer.onLevelChanged(level, currentXP, getNextLevelXP(), attributePoints);
        }
    }

    // Experience and Leveling
    public int getNextLevelXP() {
        return 15 + (level * 10);
    }

    public void addXP(int amount) {
        if (amount <= 0) return;
        currentXP += amount;
        notifyCombatLog("✨ Gained +" + amount + " XP!");

        while (currentXP >= getNextLevelXP()) {
            currentXP -= getNextLevelXP();
            level++;
            attributePoints++;
            notifyCombatLog("🌟 LEVEL UP! Reached Level " + level + "! (Earned 1 Attribute Point)");
        }
        notifyLevelChanged();
    }

    public boolean spendAttributePoint(AttributeType type) {
        if (attributePoints <= 0) {
            notifyCombatLog("❌ No attribute points available! Level up to earn points.");
            return false;
        }
        attributePoints--;
        investPoint(type);
        notifyLevelChanged();
        return true;
    }

    // Health operations
    public void takeDamage(int dmg) {
        if (dmg <= 0) return;
        this.currentHP = Math.max(0, this.currentHP - dmg);
        for (PlayerObserver observer : observers) {
            observer.onHealthChanged(this.currentHP, getMaxHP());
        }
    }

    public void heal(int amount) {
        if (amount <= 0) return;
        this.currentHP = Math.min(getMaxHP(), this.currentHP + amount);
        for (PlayerObserver observer : observers) {
            observer.onHealthChanged(this.currentHP, getMaxHP());
        }
    }

    public boolean useHealCharge() {
        if (healCharges <= 0) {
            notifyCombatLog("❌ No healing charges remaining! (0/" + maxHealCharges + ")");
            return false;
        }
        if (currentHP >= getMaxHP()) {
            notifyCombatLog("HP already full! (" + currentHP + "/" + getMaxHP() + ")");
            return false;
        }
        healCharges--;
        heal(healAmount);
        notifyCombatLog("🧪 Used Healing Flask: +" + healAmount + " HP! (Remaining: " + healCharges + "/" + maxHealCharges + ")");
        return true;
    }

    public void refillHealCharges() {
        this.healCharges = maxHealCharges;
        notifyCombatLog("✨ Healing flasks replenished (" + maxHealCharges + "/" + maxHealCharges + ")");
    }

    public void restoreFullHealthAndStamina() {
        this.currentHP = getMaxHP();
        this.currentStamina = getMaxStamina();
        this.healCharges = maxHealCharges;
        for (PlayerObserver observer : observers) {
            observer.onHealthChanged(this.currentHP, getMaxHP());
            observer.onStaminaChanged(this.currentStamina, getMaxStamina());
        }
    }

    // Stamina operations
    public boolean consumeStamina(float cost) {
        if (currentStamina >= cost) {
            currentStamina -= cost;
            for (PlayerObserver observer : observers) {
                observer.onStaminaChanged(currentStamina, getMaxStamina());
            }
            return true;
        }
        notifyCombatLog("Not enough stamina! (Required: " + (int) cost + ", Available: " + (int) currentStamina + ")");
        return false;
    }

    public void regenerateStamina(float delta) {
        if (currentStamina < getMaxStamina()) {
            currentStamina = Math.min(getMaxStamina(), currentStamina + (staminaRegenRate * delta));
            for (PlayerObserver observer : observers) {
                observer.onStaminaChanged(currentStamina, getMaxStamina());
            }
        }
    }

    // Stat investment
    public void investPoint(AttributeType type) {
        switch (type) {
            case VITALITY:
                vitality++;
                currentHP = Math.min(getMaxHP(), currentHP + 12);
                break;
            case STRENGTH:
                strength++;
                break;
            case ENDURANCE:
                endurance++;
                currentStamina = Math.min(getMaxStamina(), currentStamina + 10f);
                break;
            case DEXTERITY:
                dexterity++;
                break;
        }
        for (PlayerObserver observer : observers) {
            observer.onHealthChanged(currentHP, getMaxHP());
            observer.onStaminaChanged(currentStamina, getMaxStamina());
        }
        notifyCombatLog("Upgraded " + type.getLabel() + "! (Current: " + getAttributeValue(type) + ")");
    }

    public int getAttributeValue(AttributeType type) {
        switch (type) {
            case VITALITY: return vitality;
            case STRENGTH: return strength;
            case ENDURANCE: return endurance;
            case DEXTERITY: return dexterity;
            default: return 10;
        }
    }

    // Scaling values
    public int getMaxHP() {
        return vitality * 12;
    }

    public float getMaxStamina() {
        return endurance * 10.0f;
    }

    public float getPhysicalDamageScaling() {
        return 1.0f + (strength * 0.06f);
    }

    public float getDexterityDamageBonus() {
        return Math.max(0, (dexterity - 10) * 0.02f);
    }

    public float getRecoverySpeedMultiplier() {
        return 1.0f + (Math.max(0, dexterity - 10) * 0.04f);
    }

    // Getters and Setters
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public int getCurrentXP() { return currentXP; }
    public void setCurrentXP(int currentXP) { this.currentXP = currentXP; }
    public int getAttributePoints() { return attributePoints; }
    public void setAttributePoints(int attributePoints) { this.attributePoints = attributePoints; }

    public int getVitality() { return vitality; }
    public void setVitality(int vitality) { this.vitality = vitality; }
    public int getStrength() { return strength; }
    public void setStrength(int strength) { this.strength = strength; }
    public int getEndurance() { return endurance; }
    public void setEndurance(int endurance) { this.endurance = endurance; }
    public int getDexterity() { return dexterity; }
    public void setDexterity(int dexterity) { this.dexterity = dexterity; }

    public int getCurrentHP() { return currentHP; }
    public void setCurrentHP(int currentHP) { this.currentHP = currentHP; }
    public float getCurrentStamina() { return currentStamina; }
    public void setCurrentStamina(float currentStamina) { this.currentStamina = currentStamina; }

    public int getHealCharges() { return healCharges; }
    public void setHealCharges(int healCharges) { this.healCharges = healCharges; }
    public int getMaxHealCharges() { return maxHealCharges; }
    public boolean isDead() { return currentHP <= 0; }
}
