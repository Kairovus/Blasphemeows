package com.Blasphemeows.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import com.Blasphemeows.combat.GreatswordAttackStrategy;
import com.Blasphemeows.combat.SpearAttackStrategy;
import com.Blasphemeows.combat.SwordAttackStrategy;
import com.Blasphemeows.model.PlayerAttributes;
import com.Blasphemeows.player.Player;

/**
 * Singleton Pattern: SaveManager
 * Manages JSON persistence and loading for player progression, checkpoints, and attributes.
 * Uses libGDX's local FileHandle system to ensure cross-platform compatibility.
 */
public class SaveManager {
    private static volatile SaveManager instance;
    private static final String SAVE_FILE_NAME = "blasphemeows_save.json";

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

            // Persist checkpoint info
            if (checkpoint != null) {
                currentSaveData.lastCheckpointId = checkpoint.getId();
                currentSaveData.lastCheckpointRegion = checkpoint.getRegion().name();
                currentSaveData.respawnX = checkpoint.getX() + 20f;
                currentSaveData.respawnY = checkpoint.getY();
            }

            FileHandle file = Gdx.files.local(SAVE_FILE_NAME);
            file.writeString(json.prettyPrint(currentSaveData), false);
            System.out.println("[SaveManager] Progression saved successfully to " + SAVE_FILE_NAME);
            return true;
        } catch (Exception e) {
            System.err.println("[SaveManager] Failed to save game progression: " + e.getMessage());
            return false;
        }
    }

    public synchronized boolean loadGame(Player player) {
        if (player == null) return false;

        try {
            FileHandle file = Gdx.files.local(SAVE_FILE_NAME);
            if (!file.exists()) {
                System.out.println("[SaveManager] No existing save file found. Starting fresh.");
                return false;
            }

            String content = file.readString();
            currentSaveData = json.fromJson(SaveData.class, content);
            if (currentSaveData == null) {
                currentSaveData = new SaveData();
                return false;
            }

            // Apply attributes
            PlayerAttributes attr = player.getAttributes();
            attr.setLevel(currentSaveData.level);
            attr.setCurrentXP(currentSaveData.currentXP);
            attr.setAttributePoints(currentSaveData.attributePoints);
            attr.setVitality(currentSaveData.vitality);
            attr.setStrength(currentSaveData.strength);
            attr.setEndurance(currentSaveData.endurance);
            attr.setDexterity(currentSaveData.dexterity);
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

            // Apply abilities
            GameManager gm = GameManager.getInstance();
            for (String abName : currentSaveData.unlockedAbilities) {
                try {
                    gm.unlockAbility(GameManager.Ability.valueOf(abName));
                } catch (IllegalArgumentException ignored) {}
            }

            // Apply completed regions
            for (String regName : currentSaveData.completedRegions) {
                try {
                    gm.completeRegion(GameManager.Region.valueOf(regName));
                } catch (IllegalArgumentException ignored) {}
            }

            System.out.println("[SaveManager] Game progression loaded successfully! Level " + currentSaveData.level);
            return true;
        } catch (Exception e) {
            System.err.println("[SaveManager] Corrupted or invalid save file. Starting with default stats. Error: " + e.getMessage());
            currentSaveData = new SaveData();
            return false;
        }
    }

    public boolean hasSaveFile() {
        return Gdx.files.local(SAVE_FILE_NAME).exists();
    }

    public SaveData getCurrentSaveData() {
        return currentSaveData;
    }
}
