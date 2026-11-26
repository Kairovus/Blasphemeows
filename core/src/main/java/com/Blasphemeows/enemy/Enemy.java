package com.Blasphemeows.enemy;

import com.Blasphemeows.player.Player;
import com.Blasphemeows.score.EnemyScoreType;

/**
 * Base abstract class for all Enemies.
 * Encapsulates health, position, movement, state flags, and combat interaction.
 */
public abstract class Enemy {
    protected String name;
    protected float x;
    protected float y;
    protected float width;
    protected float height;
    protected float velocityX;
    protected float velocityY;
    protected boolean facingRight = false;

    protected int hp;
    protected int maxHp;
    protected float moveSpeed;
    protected int xpReward = 3; // Regular enemy awards 3 XP by default
    protected EnemyScoreType scoreType = EnemyScoreType.NORMAL_REGIONAL;

    // Defense and stun mechanics
    protected boolean hasFrontShield = false;
    protected boolean isVulnerableWindow = false;
    protected float stunTimer = 0f;
    protected boolean isFrozen = false;

    // Projectile emission
    protected final java.util.List<com.Blasphemeows.combat.Projectile> pendingProjectiles = new java.util.ArrayList<com.Blasphemeows.combat.Projectile>();

    // Active attack hitbox / danger zone
    protected com.Blasphemeows.combat.Hitbox currentAttackHitbox = null;
    protected boolean telegraphingAttack;

    public Enemy(String name, float x, float y, float width, float height, int maxHp, float moveSpeed) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.maxHp = maxHp;
        this.hp = maxHp;
        this.moveSpeed = moveSpeed;
    }

    public abstract void update(float delta, Player player);

    public void updateStatusEffects(float delta) {
        if (stunTimer > 0f) {
            stunTimer -= delta;
            if (stunTimer <= 0f) {
                stunTimer = 0f;
                isFrozen = false;
                System.out.println("[Enemy] " + name + " recovered from stun/freeze!");
            }
        }
    }

    public boolean isStunnedOrFrozen() {
        return stunTimer > 0f;
    }

    public void stun(float duration) {
        this.stunTimer = Math.max(this.stunTimer, duration);
        this.velocityX = 0f;
        this.velocityY = 0f;
        this.telegraphingAttack = false;
        System.out.println("[Enemy] " + name + " STUNNED for " + duration + "s!");
    }

    public void freeze(float duration) {
        this.isFrozen = true;
        stun(duration);
        System.out.println("[Enemy] " + name + " FROZEN in ice for " + duration + "s!");
    }

    /**
     * Hit resolution with shield logic.
     * @param damage Raw damage amount
     * @param canBreakShield Whether this attack carries shield-breaking capability (e.g. Greatsword Heavy)
     * @param attackerX X position of attacker to determine front or back hit
     */
    public boolean takeHit(float damage, boolean canBreakShield, float attackerX) {
        if (!isAlive()) return false;

        boolean hitFromFront = (facingRight && attackerX > x) || (!facingRight && attackerX < x);

        // Shield Bulwark Frontal Defense Logic
        if (hasFrontShield && hitFromFront && !isStunnedOrFrozen()) {
            if (isVulnerableWindow && canBreakShield) {
                // Break shield window: Stun enemy for 1 second!
                stun(1.0f);
                int actualDmg = (int) damage;
                hp = Math.max(0, hp - actualDmg);
                System.out.println("⚡ [COUNTER] " + name + "'s guard was shattered by Heavy Attack! Stunned for 1.0s! Took " + actualDmg + " dmg.");
                return true;
            } else {
                System.out.println("🛡️ [BLOCKED] " + name + "'s shield deflected the attack from the front!");
                return false;
            }
        }

        // Standard or back hit
        int actualDmg = (int) damage;
        hp = Math.max(0, hp - actualDmg);
        System.out.println("💥 [HIT] " + name + " took " + actualDmg + " damage! (" + hp + "/" + maxHp + " HP remaining)");
        return true;
    }

    public boolean isAlive() {
        return hp > 0;
    }

    public void spawnProjectile(com.Blasphemeows.combat.Projectile projectile) {
        if (projectile != null) {
            pendingProjectiles.add(projectile);
        }
    }

    public void drainPendingProjectiles(java.util.List<com.Blasphemeows.combat.Projectile> out) {
        if (!pendingProjectiles.isEmpty()) {
            out.addAll(pendingProjectiles);
            pendingProjectiles.clear();
        }
    }

    // Getters and Setters
    public String getName() { return name; }
    public float getX() { return x; }
    public void setX(float x) { this.x = x; }
    public float getY() { return y; }
    public void setY(float y) { this.y = y; }
    public float getWidth() { return width; }
    public float getHeight() { return height; }
    public int getHp() { return hp; }
    public int getMaxHp() { return maxHp; }
    public boolean isFacingRight() { return facingRight; }
    public boolean hasFrontShield() { return hasFrontShield; }
    public boolean isVulnerableWindow() { return isVulnerableWindow; }
    public boolean isFrozen() { return isFrozen; }
    public boolean isTelegraphingAttack() { return telegraphingAttack; }
    public int getXpReward() { return xpReward; }
    public EnemyScoreType getScoreType() { return scoreType; }

    public com.Blasphemeows.combat.Hitbox getCurrentAttackHitbox() {
        return currentAttackHitbox;
    }

    public void clearAttackHitbox() {
        if (currentAttackHitbox != null) {
            currentAttackHitbox.deactivate();
            currentAttackHitbox = null;
        }
    }
}
