package com.Blasphemeows.enemy.factory;

import com.Blasphemeows.enemy.DivingHarpy;
import com.Blasphemeows.enemy.Enemy;
import com.Blasphemeows.enemy.ShurikenNinja;
import com.Blasphemeows.enemy.TempleMonk;

/**
 * Concrete Factory for Branch 1: The Forgotten Temple.
 * Martial arts shaolin temple theme with active, dash-heavy enemies.
 */
public class ForgottenTempleEnemyFactory extends EnemyFactory {
    @Override
    public Enemy createMeleeEnemy(float x, float y) {
        return new TempleMonk(x, y);
    }

    @Override
    public Enemy createRangedEnemy(float x, float y) {
        return new ShurikenNinja(x, y);
    }

    @Override
    public Enemy createSpecialEnemy(float x, float y) {
        return new DivingHarpy(x, y);
    }

    @Override
    public String getRegionName() {
        return "The Forgotten Temple";
    }
}
