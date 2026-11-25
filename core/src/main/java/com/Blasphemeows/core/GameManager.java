package com.Blasphemeows.core;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Singleton Pattern: GameManager
 * Central game coordinator managing global game state:
 * - Unlocked abilities per region
 * - Completed regions & final region unlock check
 * - Global session state
 */
public class GameManager {
    private static volatile GameManager instance;

    public enum Region {
        TUTORIAL_HUB("Tutorial Hub (Sanctuary Grounds)", "Peaceful central hub with region portals, practice dummies, and recovery shrine"),
        FORGOTTEN_TEMPLE("The Forgotten Temple", "Martial arts temple guarded by swift monks and harpies"),
        ASHEN_FOUNDRY("Ashen Foundry", "Industrial forge ruled by tanky brutes and shielded bulwarks"),
        OVERGROWN_SPIRE("Overgrown Spire", "Mystic nature spire inhabited by spellcasters and root weavers"),
        HOLLOW_ALTARS("The Hollow Altars of the Crooked Saints", "The sacred final pinnacle housing elite guardians");

        private final String displayName;
        private final String description;

        Region(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }
    }

    public enum Ability {
        MONK_DASH("Monk Dash", "Upgraded dodge from Forgotten Temple: longer travel distance and extended invincibility frames."),
        SEISMIC_SLAM("Seismic Slam", "Heavy slam attack from Ashen Foundry: slams the ground creating a damaging shockwave."),
        GLACIAL_ORB("Glacial Orb", "Mystic ice ability from Overgrown Spire: launches an ice orb that freezes/stuns an enemy for 1.5s.");

        private final String abilityName;
        private final String description;

        Ability(String abilityName, String description) {
            this.abilityName = abilityName;
            this.description = description;
        }

        public String getAbilityName() {
            return abilityName;
        }

        public String getDescription() {
            return description;
        }
    }

    private final Set<Region> completedRegions = new HashSet<Region>();
    private final Set<Ability> unlockedAbilities = new HashSet<Ability>();
    private Region currentRegion = Region.TUTORIAL_HUB;

    private GameManager() {}

    public static GameManager getInstance() {
        if (instance == null) {
            synchronized (GameManager.class) {
                if (instance == null) {
                    instance = new GameManager();
                }
            }
        }
        return instance;
    }

    public void completeRegion(Region region) {
        completedRegions.add(region);
        System.out.println("[GameManager] Region completed: " + region.getDisplayName());
    }

    public boolean isRegionCompleted(Region region) {
        return completedRegions.contains(region);
    }

    /**
     * Requirement: Final region unlock does not require complex traversal,
     * simply checks if the 3 initial branches have been completed.
     */
    public boolean isFinalRegionUnlocked() {
        return completedRegions.contains(Region.FORGOTTEN_TEMPLE)
                && completedRegions.contains(Region.ASHEN_FOUNDRY)
                && completedRegions.contains(Region.OVERGROWN_SPIRE);
    }

    public void unlockAbility(Ability ability) {
        unlockedAbilities.add(ability);
        System.out.println("[GameManager] Ability unlocked: " + ability.getAbilityName());
    }

    public boolean hasAbility(Ability ability) {
        return unlockedAbilities.contains(ability);
    }

    public Set<Region> getCompletedRegions() {
        return Collections.unmodifiableSet(completedRegions);
    }

    public Set<Ability> getUnlockedAbilities() {
        return Collections.unmodifiableSet(unlockedAbilities);
    }

    public Region getCurrentRegion() {
        return currentRegion;
    }

    public void setCurrentRegion(Region currentRegion) {
        if (currentRegion == Region.HOLLOW_ALTARS && !isFinalRegionUnlocked()) {
            throw new IllegalStateException("Cannot enter " + Region.HOLLOW_ALTARS.getDisplayName() + " until all 3 regions are completed!");
        }
        this.currentRegion = currentRegion;
    }

    public void resetProgress() {
        completedRegions.clear();
        unlockedAbilities.clear();
        currentRegion = Region.TUTORIAL_HUB;
    }
}
