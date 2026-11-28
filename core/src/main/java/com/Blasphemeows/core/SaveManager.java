package com.Blasphemeows.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import com.Blasphemeows.combat.GreatswordAttackStrategy;
import com.Blasphemeows.combat.SpearAttackStrategy;
import com.Blasphemeows.combat.SwordAttackStrategy;
import com.Blasphemeows.model.PlayerAttributes;
import com.Blasphemeows.player.Player;

/**
 * Singleton Pattern: SaveManager
 * Manages per-user local progression using the platform's preferences store.
 */
public class SaveManager {
    private static volatile SaveManager instance;
    private static final String PREFERENCES_NAME = "com.blasphemeows.game.save";
    private static final String SAVE_KEY = "saveData";
    private static final String LEGACY_SAVE_FILE_NAME = "blasphemeows_save.json";

    private SaveData currentSaveData = new SaveData();
    private final Json json = new Json(JsonWriter.OutputType.json);

    private SaveManager() {}

    public static SaveManager getInstance() {
        if (instance == null) {
            synchronized (SaveManager.class) {
                if (instance == null) {
                    instance = new SaveManager();
                }
            }
        }
        return instance;
    }

    public synchronized boolean saveGame(Player player, Checkpoint checkpoint) {
        if (player == null) return false;

        try {
            PlayerAttributes attr = player.getAttributes();
            currentSaveData.level = attr.getLevel();
            currentSaveData.currentXP = attr.getCurrentXP();
            currentSaveData.attributePoints = attr.getAttributePoints();

            currentSaveData.vitality = attr.getVitality();
            currentSaveData.strength = attr.getStrength();
            currentSaveData.endurance = attr.getEndurance();
            currentSaveData.dexterity = attr.getDexterity();

            // Persist weapon
            if (player.getAttackStrategy() instanceof GreatswordAttackStrategy) {
                currentSaveData.equippedWeapon = "GREATSWORD";
            } else if (player.getAttackStrategy() instanceof SpearAttackStrategy) {
                currentSaveData.equippedWeapon = "SPEAR";
            } else {
                currentSaveData.equippedWeapon = "SWORD";
            }

            // Persist abilities
            currentSaveData.unlockedAbilities.clear();
            for (GameManager.Ability ability : GameManager.getInstance().getUnlockedAbilities()) {
                currentSaveData.unlockedAbilities.add(ability.name());
            }

            // Persist completed regions
            currentSaveData.completedRegions.clear();
            for (GameManager.Region reg : GameManager.getInstance().getCompletedRegions()) {
                currentSaveData.completedRegions.add(reg.name());
            }

            // Persist regional checkpoint info (Main Hub checkpoint must not overwrite regional history)
            if (checkpoint != null && checkpoint.getRegion() != GameManager.Region.TUTORIAL_HUB) {
                currentSaveData.lastCheckpointId = checkpoint.getId();
                currentSaveData.lastCheckpointRegion = checkpoint.getRegion().name();
                currentSaveData.respawnX = checkpoint.getRespawnX();
                currentSaveData.respawnY = checkpoint.getRespawnY();
            }

            Preferences preferences = Gdx.app.getPreferences(PREFERENCES_NAME);
            preferences.putString(SAVE_KEY, json.toJson(currentSaveData));
            preferences.flush();
            System.out.println("[SaveManager] Progression saved to local user preferences.");
            return true;
        } catch (Exception e) {
            System.err.println("[SaveManager] Failed to save game progression: " + e.getMessage());
            return false;
        }
    }

    public synchronized boolean loadGame(Player player) {
        if (player == null) return false;

        try {
            Preferences preferences = Gdx.app.getPreferences(PREFERENCES_NAME);
            String content = preferences.getString(SAVE_KEY, null);
            if (content == null || content.trim().isEmpty()) {
                content = readLegacySave();
                if (content == null) {
                    currentSaveData = new SaveData();
                    System.out.println("[SaveManager] No existing save found. Starting fresh.");
                    return false;
                }
            }
            currentSaveData = json.fromJson(SaveData.class, content);
            if (currentSaveData == null) {
                currentSaveData = new SaveData();
                return false;
            }

            // Apply attributes (clamped against corrupted/outdated values)
            PlayerAttributes attr = player.getAttributes();
            attr.setLevel(Math.max(1, currentSaveData.level));
            attr.setCurrentXP(Math.max(0, currentSaveData.currentXP));
            attr.setAttributePoints(Math.max(0, currentSaveData.attributePoints));
            attr.setVitality(Math.max(1, currentSaveData.vitality));
            attr.setStrength(Math.max(1, currentSaveData.strength));
            attr.setEndurance(Math.max(1, currentSaveData.endurance));
            attr.setDexterity(Math.max(1, currentSaveData.dexterity));
            attr.restoreFullHealthAndStamina();
            attr.notifyLevelChanged();

            // Apply weapon
            if ("GREATSWORD".equalsIgnoreCase(currentSaveData.equippedWeapon)) {
                player.setWeapon(new GreatswordAttackStrategy());
            } else if ("SPEAR".equalsIgnoreCase(currentSaveData.equippedWeapon)) {
                player.setWeapon(new SpearAttackStrategy());
            } else {
                player.setWeapon(new SwordAttackStrategy());
            }

            // Apply abilities and completed regions cleanly
            GameManager gm = GameManager.getInstance();
            gm.resetProgress();
            if (currentSaveData.unlockedAbilities != null) {
                for (String abName : currentSaveData.unlockedAbilities) {
                    if (abName == null) continue;
                    try {
                        gm.unlockAbility(GameManager.Ability.valueOf(abName));
                    } catch (IllegalArgumentException ignored) {}
                }
            }

            if (currentSaveData.completedRegions != null) {
                for (String regName : currentSaveData.completedRegions) {
                    if (regName == null) continue;
                    try {
                        gm.completeRegion(GameManager.Region.valueOf(regName));
                    } catch (IllegalArgumentException ignored) {}
                }
            }

            // Import legacy JSON once so this user's existing progression is preserved.
            if (!preferences.contains(SAVE_KEY)) {
                preferences.putString(SAVE_KEY, json.toJson(currentSaveData));
                preferences.flush();
            }

            System.out.println("[SaveManager] Game progression loaded successfully! Level " + currentSaveData.level);
            return true;
        } catch (Exception e) {
            System.err.println("[SaveManager] Corrupted or invalid save. Starting with default stats. Error: " + e.getMessage());
            currentSaveData = new SaveData();
            return false;
        }
    }

    public synchronized boolean hasSaveFile() {
        if (Gdx.app == null) {
            return false;
        }
        try {
            Preferences preferences = Gdx.app.getPreferences(PREFERENCES_NAME);
            String content = preferences.getString(SAVE_KEY, null);
            if (content == null || content.trim().isEmpty()) {
                content = readLegacySave();
            }
            if (content == null || content.trim().isEmpty()) {
                return false;
            }
            SaveData parsed = json.fromJson(SaveData.class, content);
            return parsed != null && parsed.level >= 1;
        } catch (Exception e) {
            return false;
        }
    }

    /** Clears local campaign progression (used only on explicit New Game confirmation). */
    public synchronized void clearSave() {
        currentSaveData = new SaveData();
        if (Gdx.app == null) {
            return;
        }
        Preferences preferences = Gdx.app.getPreferences(PREFERENCES_NAME);
        preferences.remove(SAVE_KEY);
        preferences.flush();

        FileHandle legacyFile = Gdx.files.local(LEGACY_SAVE_FILE_NAME);
        if (legacyFile.exists()) {
            legacyFile.delete();
        }
        FileHandle projectRootSave = Gdx.files.local("../" + LEGACY_SAVE_FILE_NAME);
        if (projectRootSave.exists()) {
            projectRootSave.delete();
        }
    }

    private String readLegacySave() {
        FileHandle legacyFile = Gdx.files.local(LEGACY_SAVE_FILE_NAME);
        if (legacyFile.exists()) return legacyFile.readString();

        // The Gradle desktop run task starts in assets/, while older launches may have saved at the project root.
        FileHandle projectRootSave = Gdx.files.local("../" + LEGACY_SAVE_FILE_NAME);
        return projectRootSave.exists() ? projectRootSave.readString() : null;
    }

    public SaveData getCurrentSaveData() {
        return currentSaveData;
    }
}
