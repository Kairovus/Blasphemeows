package com.Blasphemeows.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Data Transfer Object (DTO) for persisting player progression.
 */
public class SaveData {
    public int level = 1;
    public int currentXP = 0;
    public int attributePoints = 0;

    public int vitality = 10;
    public int strength = 10;
    public int endurance = 10;
    public int dexterity = 10;

    public String equippedWeapon = "SWORD";
    public List<String> unlockedAbilities = new ArrayList<String>();
    public List<String> completedRegions = new ArrayList<String>();

    public String lastCheckpointId = "hub_central";
    public String lastCheckpointRegion = "TUTORIAL_HUB";
    public float respawnX = 140f;
    public float respawnY = 80f;

    public SaveData() {}
}
