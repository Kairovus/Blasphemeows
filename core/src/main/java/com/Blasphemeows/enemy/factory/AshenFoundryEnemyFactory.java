package com.Blasphemeows.enemy.factory;

import com.Blasphemeows.enemy.Enemy;
import com.Blasphemeows.enemy.ForgeBrute;
import com.Blasphemeows.enemy.ShieldBulwark;
import com.Blasphemeows.enemy.SlagHurler;

/**
 * Concrete Factory for Branch 2: Ashen Foundry.
 * Heavy industrial forge theme with slow, tanky, hard-hitting foes and shielded defense.
 */
public class AshenFoundryEnemyFactory extends EnemyFactory {
    @Override
    public Enemy createMeleeEnemy(float x, float y) {
        return new ForgeBrute(x, y);
    }

    @Override
    public Enemy createRangedEnemy(float x, float y) {
        return new SlagHurler(x, y);
    }

    @Override
    public Enemy createSpecialEnemy(float x, float y) {
        return new ShieldBulwark(x, y);
    }

    @Override
    public String getRegionName() {
        return "Ashen Foundry";
    }
}
