package com.Blasphemeows.world;

import com.badlogic.gdx.graphics.Color;
import com.Blasphemeows.combat.AttackStrategy;
import com.Blasphemeows.player.Player;

/**
 * Weapon Station (Blue Element):
 * Located in the Main Hub.
 * Players interact with this station to equip a specific weapon (Sword, Greatsword, or Spear).
 * Replaces unrestricted mid-combat weapon switching.
 */
public class WeaponStation {
    private final float x;
    private final float y;
    private final float width;
    private final float height;
    private final AttackStrategy weaponStrategy;
    private final Color color = new Color(0.2f, 0.55f, 0.95f, 1f); // Vibrant Blue

    public WeaponStation(float x, float y, float width, float height, AttackStrategy weaponStrategy) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.weaponStrategy = weaponStrategy;
    }

    public boolean isPlayerNearby(float playerX, float playerY) {
        return Math.abs(playerX - (x + width / 2f)) < 45f && Math.abs(playerY - y) < 60f;
    }

    public void interact(Player player) {
        player.setWeapon(weaponStrategy);
        player.getAttributes().notifyCombatLog("⚔️ Equipped: " + weaponStrategy.getWeaponName());
    }

    public String getPromptText() {
        return "⚔️ [F / ENTER] Equip " + weaponStrategy.getWeaponName();
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public float getWidth() { return width; }
    public float getHeight() { return height; }
    public AttackStrategy getWeaponStrategy() { return weaponStrategy; }
    public Color getColor() { return color; }
}
