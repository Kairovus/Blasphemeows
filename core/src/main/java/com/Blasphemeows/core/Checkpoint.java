package com.Blasphemeows.core;

import com.badlogic.gdx.graphics.Color;
import com.Blasphemeows.player.Player;

/**
 * Checkpoint Entity:
 * Placed in the Main Hub and across regions.
 * Interacting with a checkpoint saves player progression, restores HP/SP/Flasks,
 * and sets the respawn location upon death.
 */
public class Checkpoint {
    private final String id;
    private final String name;
    private final GameManager.Region region;
    private final float x;
    private final float y;
    private final float width;
    private final float height;
    private float respawnX;
    private float respawnY;
    private boolean activated = false;

    public Checkpoint(String id, String name, GameManager.Region region, float x, float y, float width, float height) {
        this.id = id;
        this.name = name;
        this.region = region;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.respawnX = x + 20f;
        this.respawnY = y;
    }

    public void setRespawnPosition(float respawnX, float respawnY) {
        this.respawnX = respawnX;
        this.respawnY = respawnY;
    }

    public boolean isPlayerNearby(float playerX, float playerY) {
        return Math.abs(playerX - (x + width / 2f)) < (width / 2f + 45f) && Math.abs(playerY - y) < (height + 40f);
    }

    public void activate(Player player) {
        this.activated = true;
        player.getAttributes().restoreFullHealthAndStamina();
        SaveManager.getInstance().saveGame(player, this);
        player.getAttributes().notifyCombatLog("🔥 Checkpoint Activated: " + name + " (Progression Saved & Restored!)");
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public GameManager.Region getRegion() { return region; }
    public float getX() { return x; }
    public float getY() { return y; }
    public float getWidth() { return width; }
    public float getHeight() { return height; }
    public float getRespawnX() { return respawnX; }
    public float getRespawnY() { return respawnY; }
    public boolean isActivated() { return activated; }
    public void setActivated(boolean activated) { this.activated = activated; }
    public Color getColor() { return activated ? Color.FIREBRICK : Color.LIGHT_GRAY; }
}
