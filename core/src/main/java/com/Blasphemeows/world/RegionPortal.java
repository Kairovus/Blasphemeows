package com.Blasphemeows.world;

import com.badlogic.gdx.graphics.Color;
import com.Blasphemeows.core.GameManager;

/**
 * Represents an interactive travel gateway / portal in the game world.
 * Allows transitioning between the central Tutorial Hub and regional branches.
 */
public class RegionPortal {
    private final float x;
    private final float y;
    private final float width;
    private final float height;
    private final GameManager.Region destination;
    private final Color color;
    private final String label;

    public RegionPortal(float x, float y, float width, float height, GameManager.Region destination, Color color, String label) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.destination = destination;
        this.color = color;
        this.label = label;
    }

    public boolean isPlayerNearby(float playerX, float playerY) {
        return Math.abs(playerX - (x + width / 2f)) < 45f && Math.abs(playerY - y) < 60f;
    }

    public boolean isLocked() {
        if (destination == GameManager.Region.HOLLOW_ALTARS) {
            return !GameManager.getInstance().isFinalRegionUnlocked();
        }
        return false;
    }

    public String getPromptText() {
        if (isLocked()) {
            return "🔒 [LOCKED] " + label + " (Requires all 3 branch regions cleared!)";
        }
        return "✨ [Press F / ENTER] Enter " + label;
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public float getWidth() { return width; }
    public float getHeight() { return height; }
    public GameManager.Region getDestination() { return destination; }
    public Color getColor() { return color; }
    public String getLabel() { return label; }
}
