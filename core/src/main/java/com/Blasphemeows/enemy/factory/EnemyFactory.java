package com.Blasphemeows.enemy.factory;

import com.Blasphemeows.enemy.Enemy;

/**
 * Factory Pattern: EnemyFactory
 * Abstract Factory defining the creation of region-specific enemies:
 * Melee, Ranged, and Special archetype units.
 */
public abstract class EnemyFactory {
    public abstract Enemy createMeleeEnemy(float x, float y);
    public abstract Enemy createRangedEnemy(float x, float y);
    public abstract Enemy createSpecialEnemy(float x, float y);
    public abstract String getRegionName();
}
