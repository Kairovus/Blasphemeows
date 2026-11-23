package com.Blasphemeows.model;

/**
 * RPG Attributes:
 * - Vitality: Maximum HP
 * - Strength: Physical damage scaling
 * - Endurance: Maximum stamina
 * - Dexterity: Attack speed and weapon recovery scaling
 */
public enum AttributeType {
    VITALITY("Vitality", "Increases maximum health points"),
    STRENGTH("Strength", "Scales physical attack damage"),
    ENDURANCE("Endurance", "Increases maximum stamina capacity"),
    DEXTERITY("Dexterity", "Increases attack and action recovery speed");

    private final String label;
    private final String description;

    AttributeType(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }
}
