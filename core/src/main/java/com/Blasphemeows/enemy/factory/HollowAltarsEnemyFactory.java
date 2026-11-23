package com.Blasphemeows.enemy.factory;

import com.Blasphemeows.core.GameManager;
import com.Blasphemeows.enemy.DivingHarpy;
import com.Blasphemeows.enemy.Enemy;
import com.Blasphemeows.enemy.RootWeaver;
import com.Blasphemeows.enemy.ShieldBulwark;
import com.Blasphemeows.enemy.TempleMonk;

/**
 * Concrete Factory for the Final Region:
 * The Hollow Altars of the Crooked Saints.
 * Contains elite representatives from all three branches.
 * Automatically unlocked once all 3 initial branches are cleared.
 */
public class HollowAltarsEnemyFactory extends EnemyFactory {

    public HollowAltarsEnemyFactory() {
        if (!GameManager.getInstance().isFinalRegionUnlocked()) {
            System.out.println("⚠️ Warning: Accessing HollowAltarsEnemyFactory before clearing all 3 branch regions!");
        }
    }

    @Override
    public Enemy createMeleeEnemy(float x, float y) {
        // Elite temple monk
        return new TempleMonk(x, y);
    }

    @Override
    public Enemy createRangedEnemy(float x, float y) {
        // High-altitude diving harpy or spellcaster
        return new DivingHarpy(x, y);
    }

    @Override
    public Enemy createSpecialEnemy(float x, float y) {
        // Elite shielded bulwark guarding the crooked saints altar
        return new ShieldBulwark(x, y);
    }

    public Enemy createRootController(float x, float y) {
        return new RootWeaver(x, y);
    }

    @Override
    public String getRegionName() {
        return "The Hollow Altars of the Crooked Saints";
    }
}
