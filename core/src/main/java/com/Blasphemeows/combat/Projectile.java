package com.Blasphemeows.combat;

import com.badlogic.gdx.graphics.Color;
import com.Blasphemeows.enemy.Enemy;
import com.Blasphemeows.player.Player;

/**
 * Projectile Entity:
 * Represents flying projectiles in combat (Fireballs, Molten Slag, Shurikens, Glacial Orbs).
 * Encapsulates trajectory, gravity effects, lifetime, and hit resolution with status effects.
 */
public class Projectile {
    public enum Type {
        SHURIKEN(Color.LIGHT_GRAY, 14f, 14f, false, "Shuriken"),
        MOLTEN_SLAG(Color.ORANGE, 18f, 18f, true, "Molten Slag"),
        FIREBALL(Color.FIREBRICK, 20f, 20f, false, "Fireball"),
        GLACIAL_ORB(Color.CYAN, 22f, 22f, false, "Glacial Orb");

        private final Color color;
        private final float width;
        private final float height;
        private final boolean affectedByGravity;
        private final String displayName;

        Type(Color color, float width, float height, boolean affectedByGravity, String displayName) {
            this.color = color;
            this.width = width;
            this.height = height;
            this.affectedByGravity = affectedByGravity;
            this.displayName = displayName;
        }

        public Color getColor() { return color; }
        public float getWidth() { return width; }
        public float getHeight() { return height; }
        public boolean isAffectedByGravity() { return affectedByGravity; }
        public String getDisplayName() { return displayName; }
    }

    private float x;
    private float y;
    private float velocityX;
    private float velocityY;
    private float width;
    private float height;
    private float damage;
    private final boolean isHostile; // true = damages player, false = damages enemies
    private final Type type;
    private float lifetime;
    private boolean active = true;

    private static final float GRAVITY = -650f;

    public Projectile(float x, float y, float vx, float vy, float damage, float lifetime, boolean isHostile, Type type) {
        this.x = x;
        this.y = y;
        this.velocityX = vx;
        this.velocityY = vy;
        this.damage = damage;
        this.lifetime = lifetime;
        this.isHostile = isHostile;
        this.type = type;
        this.width = type.getWidth();
        this.height = type.getHeight();
    }

    public void update(float delta, float groundY) {
        if (!active) return;

        if (type.isAffectedByGravity()) {
            velocityY += GRAVITY * delta;
        }

        x += velocityX * delta;
        y += velocityY * delta;

        // Ground collision for arcing projectiles
        if (y <= groundY) {
            active = false;
        }

        lifetime -= delta;
        if (lifetime <= 0) {
            active = false;
        }
    }

    public boolean overlaps(float ox, float oy, float ow, float oh) {
        if (!active) return false;
        return x < ox + ow &&
                x + width > ox &&
                y < oy + oh &&
                y + height > oy;
    }

    /**
     * Resolves collision with the player.
     */
    public boolean onHitPlayer(Player player) {
        if (!active || !isHostile) return false;
        if (player.isInvincible()) {
            player.getAttributes().notifyCombatLog("🛡️ Dodged " + type.getDisplayName() + " with i-frame!");
            active = false;
            return true;
        }

        player.takeDamage((int) damage);
        active = false;
        return true;
    }

    /**
     * Resolves collision with an enemy.
     */
    public boolean onHitEnemy(Enemy enemy) {
        if (!active || isHostile || !enemy.isAlive()) return false;

        // Special Glacial Orb effect: freeze/stun enemy for 1.5s
        if (type == Type.GLACIAL_ORB) {
            enemy.freeze(1.5f);
        }

        enemy.takeHit(damage, false, x);
        active = false;
        return true;
    }

    // Getters
    public float getX() { return x; }
    public float getY() { return y; }
    public float getWidth() { return width; }
    public float getHeight() { return height; }
    public float getDamage() { return damage; }
    public boolean isHostile() { return isHostile; }
    public Type getType() { return type; }
    public boolean isActive() { return active; }
    public void deactivate() { this.active = false; }
}
