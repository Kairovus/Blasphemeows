package com.Blasphemeows.enemy.factory;

import com.Blasphemeows.enemy.Enemy;
import com.Blasphemeows.enemy.TrainingDummy;

/**
 * Concrete Factory for the Tutorial Hub Sanctuary.
 * Spawns non-hostile practice training dummies for safe experimentation.
 */
public class TutorialHubEnemyFactory extends EnemyFactory {
    @Override
    public Enemy createMeleeEnemy(float x, float y) {
        return new TrainingDummy(x, y);
    }

    @Override
    public Enemy createRangedEnemy(float x, float y) {
        return new TrainingDummy(x, y);
    }

    @Override
    public Enemy createSpecialEnemy(float x, float y) {
        return new TrainingDummy(x, y);
    }

    @Override
    public String getRegionName() {
        return "Tutorial Hub (Sanctuary Grounds)";
    }
}
