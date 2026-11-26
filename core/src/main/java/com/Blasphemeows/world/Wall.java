package com.Blasphemeows.world;

import com.badlogic.gdx.graphics.Color;
import com.Blasphemeows.player.Player;

/** A solid architectural wall segment used for both rendering and collision. */
public class Wall {
    private final float x;
    private final float y;
    private final float width;
    private final float height;
    private final Color color;

    public Wall(float x, float y, float width, float height, Color color) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.color = color != null ? color : Color.DARK_GRAY;
    }

    public Wall(float x, float y, float width, float height) {
        this(x, y, width, height, new Color(0.02f, 0.02f, 0.025f, 1f));
    }

    /** Resolves solid AABB collision using the player's previous and current positions. */
    public boolean resolveCollision(Player player, float previousX, float previousY) {
        float left = x;
        float right = x + width;
        float bottom = y;
        float top = y + height;
        float playerWidth = player.getWidth();
        float playerHeight = player.getHeight();
        float currentX = player.getX();
        float currentY = player.getY();

        boolean verticalOverlap = currentY < top && currentY + playerHeight > bottom;
        if (verticalOverlap) {
            if (currentX > previousX && previousX + playerWidth <= left && currentX + playerWidth > left) {
                player.setX(left - playerWidth);
                player.setVelocityX(0f);
                return true;
            }
            if (currentX < previousX && previousX >= right && currentX < right) {
                player.setX(right);
                player.setVelocityX(0f);
                return true;
            }
        }

        boolean horizontalOverlap = currentX < right && currentX + playerWidth > left;
        if (horizontalOverlap) {
            if (currentY > previousY && previousY + playerHeight <= bottom
                    && currentY + playerHeight > bottom) {
                player.setY(bottom - playerHeight);
                player.setVelocityY(0f);
                return true;
            }
            if (currentY < previousY && previousY >= top && currentY < top
                    && player.getVelocityY() <= 0f) {
                player.setY(top);
                player.setVelocityY(0f);
                player.setGrounded(true);
                return true;
            }
        }

        // Recover safely if a spawn or a prior frame leaves the player inside a wall.
        if (horizontalOverlap && verticalOverlap) {
            float pushLeft = currentX + playerWidth - left;
            float pushRight = right - currentX;
            float pushDown = currentY + playerHeight - bottom;
            float pushUp = top - currentY;
            float horizontalPush = Math.min(pushLeft, pushRight);
            float verticalPush = Math.min(pushDown, pushUp);

            if (horizontalPush < verticalPush) {
                player.setX(pushLeft < pushRight ? left - playerWidth : right);
                player.setVelocityX(0f);
            } else if (pushDown < pushUp) {
                player.setY(bottom - playerHeight);
                player.setVelocityY(0f);
            } else {
                player.setY(top);
                player.setVelocityY(0f);
                player.setGrounded(true);
            }
            return true;
        }
        return false;
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public float getWidth() { return width; }
    public float getHeight() { return height; }
    public Color getColor() { return color; }
}
