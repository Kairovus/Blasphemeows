package com.Blasphemeows.world;

import com.badlogic.gdx.graphics.Color;
import com.Blasphemeows.player.Player;

/**
 * Represents a physical platform in the world for verticality and multi-elevation navigation.
 * Supports one-way top landing collision.
 */
public class Platform {
    private final float x;
    private final float y;
    private final float width;
    private final float height;
    private final Color color;

    public Platform(float x, float y, float width, float height, Color color) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.color = color != null ? color : Color.DARK_GRAY;
    }

    public Platform(float x, float y, float width, float height) {
        this(x, y, width, height, new Color(0.18f, 0.18f, 0.20f, 1f));
    }

    /**
     * Resolves player vertical landing on top of this platform.
     */
    public boolean resolveCollision(Player player, float previousY) {
        if (player.isDroppingThroughPlatform()) {
            return false;
        }

        float px = player.getX();
        float py = player.getY();
        float pw = player.getWidth();

        // Check horizontal overlap
        if (px + pw > x && px < x + width) {
            float topY = y + height;
            // If player was falling and crosses from above platform top to below
            if (previousY >= topY - 8f && py <= topY && player.getVelocityY() <= 0) {
                player.setY(topY);
                player.setVelocityY(0f);
                player.setGrounded(true);
                return true;
            }
        }
        return false;
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public float getWidth() { return width; }
    public float getHeight() { return height; }
    public Color getColor() { return color; }
}
