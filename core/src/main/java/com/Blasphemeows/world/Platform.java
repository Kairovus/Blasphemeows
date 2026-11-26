package com.Blasphemeows.world;

import com.badlogic.gdx.graphics.Color;
import com.Blasphemeows.player.Player;

/**
 * One-way walkable ledge: solid on the top and sides, open on the underside
 * so the player can jump up through it and drop down with S/DOWN.
 */
public class Platform {
    private final float x;
    private final float y;
    private final float width;
    private final float height;
    private final Color color;
    private final boolean showTopRim;

    public Platform(float x, float y, float width, float height, Color color) {
        this(x, y, width, height, color, true);
    }

    public Platform(float x, float y, float width, float height, Color color, boolean showTopRim) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.color = color != null ? color : Color.DARK_GRAY;
        this.showTopRim = showTopRim;
    }

    public Platform(float x, float y, float width, float height) {
        this(x, y, width, height, new Color(0.18f, 0.18f, 0.20f, 1f));
    }

    /** Lands the player on the top surface when falling onto it. */
    public boolean resolveCollision(Player player, float previousY) {
        if (player.isDroppingThroughPlatform()) {
            return false;
        }

        float px = player.getX();
        float py = player.getY();
        float pw = player.getWidth();
        float topY = y + height;

        if (px + pw > x && px < x + width) {
            if (previousY >= topY - 8f && py <= topY && player.getVelocityY() <= 0) {
                player.setY(topY);
                player.setVelocityY(0f);
                player.setGrounded(true);
                return true;
            }
        }
        return false;
    }

    /**
     * Top + side hitboxes only. No underside collision, so platforms can be
     * jumped through from below and dropped through with S/DOWN.
     */
    public boolean resolveSolidCollision(Player player, float previousX, float previousY) {
        if (resolveCollision(player, previousY)) return true;
        if (player.isDroppingThroughPlatform()) return false;

        float px = player.getX();
        float py = player.getY();
        float pw = player.getWidth();
        float ph = player.getHeight();
        float left = x;
        float right = x + width;
        float bottom = y;
        float top = y + height;

        // Standing on (or above) the top — never side-block; that is what sticks the player.
        if (py >= top - 1f) return false;
        if (px >= right || px + pw <= left || py + ph <= bottom) return false;

        // Side hits only when the body actually overlaps the platform slab.
        boolean verticalOverlap = py < top && py + ph > bottom;
        if (!verticalOverlap) return false;

        float oldRight = previousX + pw;
        if (px > previousX && oldRight <= left && px + pw > left) {
            player.setX(left - pw);
            player.setVelocityX(0f);
            return true;
        }
        if (px < previousX && previousX >= right && px < right) {
            player.setX(right);
            player.setVelocityX(0f);
            return true;
        }
        return false;
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public float getWidth() { return width; }
    public float getHeight() { return height; }
    public Color getColor() { return color; }
    public boolean hasTopRim() { return showTopRim; }
}
