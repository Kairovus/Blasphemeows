package com.Blasphemeows.enemy.factory;

import com.Blasphemeows.enemy.Enemy;
import com.Blasphemeows.enemy.Pyromancer;
import com.Blasphemeows.enemy.RootWeaver;
import com.Blasphemeows.enemy.SporeSummoner;

/**
 * Concrete Factory for Branch 3: Overgrown Spire.
 * Mystic nature and spellcasting theme with pyromancy, minion summoners, and root crowd control.
 */
public class OvergrownSpireEnemyFactory extends EnemyFactory {
    @Override
    public Enemy createMeleeEnemy(float x, float y) {
        // In the Spire, Summoners conjure minions directly on the field
        return new SporeSummoner(x, y);
    }

    @Override
    public Enemy createRangedEnemy(float x, float y) {
        return new Pyromancer(x, y);
    }

    @Override
    public Enemy createSpecialEnemy(float x, float y) {
        return new RootWeaver(x, y);
    }

    @Override
    public String getRegionName() {
        return "Overgrown Spire";
    }
}
