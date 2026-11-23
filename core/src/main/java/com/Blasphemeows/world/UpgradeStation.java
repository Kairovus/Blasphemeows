package com.Blasphemeows.world;

import com.badlogic.gdx.graphics.Color;
import com.Blasphemeows.model.AttributeType;
import com.Blasphemeows.player.Player;

/**
 * Character Upgrade Station (Green Element):
 * Located in the Main Hub.
 * Players interact with this altar to spend accumulated attribute points
 * to improve Strength, Vitality, Endurance, or Dexterity.
 */
public class UpgradeStation {
    private final float x;
    private final float y;
    private final float width;
    private final float height;
    private boolean menuOpen = false;
    private final Color color = new Color(0.2f, 0.85f, 0.35f, 1f); // Vibrant Emerald Green

    public UpgradeStation(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public boolean isPlayerNearby(float playerX, float playerY) {
        return Math.abs(playerX - (x + width / 2f)) < 55f && Math.abs(playerY - y) < 65f;
    }

    public void toggleMenu() {
        this.menuOpen = !this.menuOpen;
    }

    public void closeMenu() {
        this.menuOpen = false;
    }

    public boolean isMenuOpen() {
        return menuOpen;
    }

    public boolean upgrade(Player player, AttributeType type) {
        return player.getAttributes().spendAttributePoint(type);
    }

    public String getPromptText() {
        return menuOpen ? "🌿 [F / ESC] Close Upgrade Altar" : "🌿 [F / ENTER] Character Upgrade Altar";
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public float getWidth() { return width; }
    public float getHeight() { return height; }
    public Color getColor() { return color; }
}
