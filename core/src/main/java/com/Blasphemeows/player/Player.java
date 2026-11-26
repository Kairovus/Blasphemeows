package com.Blasphemeows.player;

import com.Blasphemeows.combat.AttackStrategy;
import com.Blasphemeows.combat.Hitbox;
import com.Blasphemeows.combat.Projectile;
import com.Blasphemeows.core.GameManager;
import com.Blasphemeows.enemy.Enemy;
import com.Blasphemeows.model.PlayerAttributes;
import com.Blasphemeows.state.AttackState;
import com.Blasphemeows.state.ComboRecoveryState;
import com.Blasphemeows.state.DashState;
import com.Blasphemeows.state.IdleState;
import com.Blasphemeows.state.JumpState;
import com.Blasphemeows.state.MoveState;
import com.Blasphemeows.state.PlayerStateMachine;
import com.Blasphemeows.state.RootedState;
import com.Blasphemeows.state.SeismicSlamState;

import java.util.ArrayList;
import java.util.List;

/**
 * Player Entity:
 * Central entity coordinating RPG attributes, weapon strategies, states,
 * jump mechanics, projectiles, and command executions.
 */
public class Player {
    private float x = 100f;
    private float y = 80f;
    private float groundY = 80f;
    private float velocityX = 0f;
    private float velocityY = 0f;
    private float width = 32f;
    private float height = 64f;
    private boolean facingRight = true;
    private boolean invincible = false;
    private boolean grounded = true;
    private boolean droppingThroughPlatform = false;
    private float dropThroughTimer = 0f;
    private float moveSpeed = 205f;
    private float jumpVelocity = 440f;
    private float gravity = -950f;

    private final PlayerAttributes attributes;
    private final PlayerStateMachine stateMachine;
    private final com.Blasphemeows.combat.ComboManager comboManager = new com.Blasphemeows.combat.ComboManager();
    private AttackStrategy attackStrategy;

    // Active hitboxes and projectiles
    private final List<Hitbox> activeHitboxes = new ArrayList<Hitbox>();
    private final List<Projectile> activeProjectiles = new ArrayList<Projectile>();

    public Player(PlayerAttributes attributes, AttackStrategy initialStrategy) {
        this.attributes = attributes != null ? attributes : new PlayerAttributes();
        this.attackStrategy = initialStrategy;
        this.stateMachine = new PlayerStateMachine(this);
        this.stateMachine.changeState(new IdleState());
    }

    public void update(float delta) {
        // State machine update
        stateMachine.update(delta);
        comboManager.update(delta, this);

        // Apply gravity unless executing Seismic Slam or hovering during Monk Dash
        boolean suppressGravity = stateMachine.getCurrentState() instanceof SeismicSlamState
                || (stateMachine.getCurrentState() instanceof DashState
                    && ((DashState) stateMachine.getCurrentState()).isMonkDashUpgraded());
        if (!suppressGravity) {
            if (y > groundY) {
                velocityY += gravity * delta;
                grounded = false;
            } else if (y <= groundY && velocityY <= 0) {
                y = groundY;
                velocityY = 0;
                grounded = true;
            }
        }

        // Integrate positions
        x += velocityX * delta;
        y += velocityY * delta;

        // Platform drop timer
        if (droppingThroughPlatform) {
            dropThroughTimer -= delta;
            if (dropThroughTimer <= 0f) {
                droppingThroughPlatform = false;
            }
        }

        // Ground constraint
        if (y < groundY && !(stateMachine.getCurrentState() instanceof SeismicSlamState)) {
            y = groundY;
            velocityY = 0;
            grounded = true;
        }

        // Update active hitboxes
        for (int i = activeHitboxes.size() - 1; i >= 0; i--) {
            Hitbox hitbox = activeHitboxes.get(i);
            hitbox.update(delta);
            if (!hitbox.isActive()) {
                activeHitboxes.remove(i);
            }
        }

        // Update active projectiles
        for (int i = activeProjectiles.size() - 1; i >= 0; i--) {
            Projectile projectile = activeProjectiles.get(i);
            projectile.update(delta, groundY);
            if (!projectile.isActive()) {
                activeProjectiles.remove(i);
            }
        }
    }

    public void jump() {
        if (!grounded || !canAct() || stateMachine.getCurrentState() instanceof RootedState) return;

        // Small stamina cost for jumping
        if (!attributes.consumeStamina(8.0f)) return;

        velocityY = jumpVelocity;
        grounded = false;
        stateMachine.changeState(new JumpState());
        attributes.notifyCombatLog("⬆️ Jumped into the air!");
    }

    public void move(float direction) {
        // Cannot move while rooted, attacking, or dashing
        if (stateMachine.getCurrentState() instanceof RootedState
                || stateMachine.getCurrentState() instanceof AttackState
                || stateMachine.getCurrentState() instanceof ComboRecoveryState
                || stateMachine.getCurrentState() instanceof DashState
                || stateMachine.getCurrentState() instanceof SeismicSlamState) {
            return;
        }

        boolean isAirborne = stateMachine.getCurrentState() instanceof JumpState;

        if (direction != 0) {
            facingRight = direction > 0;
            velocityX = direction * moveSpeed;
            if (!isAirborne && !(stateMachine.getCurrentState() instanceof MoveState)) {
                stateMachine.changeState(new MoveState());
            }
        } else {
            velocityX = 0;
            if (stateMachine.getCurrentState() instanceof MoveState) {
                stateMachine.changeState(new IdleState());
            }
        }
    }

    public void performLightAttack() {
        if (attributes.isDead() || stateMachine.getCurrentState() instanceof ComboRecoveryState
                || stateMachine.getCurrentState() instanceof RootedState
                || stateMachine.getCurrentState() instanceof DashState
                || stateMachine.getCurrentState() instanceof SeismicSlamState) {
            return;
        }
        if (attackStrategy != null) {
            attackStrategy.executeLightAttack(this);
        }
    }

    public void performHeavyAttack() {
        if (attributes.isDead() || stateMachine.getCurrentState() instanceof ComboRecoveryState
                || stateMachine.getCurrentState() instanceof RootedState
                || stateMachine.getCurrentState() instanceof DashState
                || stateMachine.getCurrentState() instanceof SeismicSlamState) {
            return;
        }
        if (attackStrategy != null) {
            attackStrategy.executeHeavyAttack(this);
        }
    }

    public void dropThroughPlatform() {
        if (stateMachine.getCurrentState() instanceof ComboRecoveryState) return;
        // Can only drop through if currently above the base world ground
        if (y > groundY + 4f && grounded) {
            droppingThroughPlatform = true;
            dropThroughTimer = 0.28f;
            grounded = false;
            velocityY = -75f;
            if (stateMachine.getCurrentState() instanceof IdleState || stateMachine.getCurrentState() instanceof MoveState) {
                stateMachine.changeState(new JumpState());
            }
            attributes.notifyCombatLog("⬇️ Dropped through platform!");
        }
    }

    public boolean isDroppingThroughPlatform() {
        return droppingThroughPlatform;
    }

    public void heal() {
        if (!canAct() || stateMachine.getCurrentState() instanceof RootedState) return;
        attributes.useHealCharge();
    }

    public void performDodgeOrDash() {
        if (!canAct()) return;

        float staminaCost = 14f;
        if (!attributes.consumeStamina(staminaCost)) {
            return;
        }

        // Check if Monk Dash (Branch 1 upgrade) has been acquired
        boolean hasMonkDash = GameManager.getInstance().hasAbility(GameManager.Ability.MONK_DASH);
        stateMachine.changeState(new DashState(hasMonkDash));
    }

    public void performSeismicSlam() {
        if (!canAct()) return;

        // Check if Seismic Slam (Branch 2 upgrade) has been acquired
        if (!GameManager.getInstance().hasAbility(GameManager.Ability.SEISMIC_SLAM)) {
            attributes.notifyCombatLog("❌ Seismic Slam is locked! Complete Ashen Foundry to unlock.");
            return;
        }

        float staminaCost = 35f;
        if (!attributes.consumeStamina(staminaCost)) {
            return;
        }

        stateMachine.changeState(new SeismicSlamState());
    }

    public void performGlacialOrb(Enemy target) {
        if (!canAct()) return;

        // Check if Glacial Orb (Branch 3 upgrade) has been acquired
        if (!GameManager.getInstance().hasAbility(GameManager.Ability.GLACIAL_ORB)) {
            attributes.notifyCombatLog("❌ Glacial Orb is locked! Complete Overgrown Spire to unlock.");
            return;
        }

        float staminaCost = 25f;
        if (!attributes.consumeStamina(staminaCost)) {
            return;
        }

        // Launch an actual flying Glacial Orb projectile!
        float orbSpeed = facingRight ? 340f : -340f;
        float spawnX = facingRight ? (x + width) : (x - 24f);
        Projectile orb = new Projectile(spawnX, y + 14f, orbSpeed, 0f, 25.0f, 3.0f, false, Projectile.Type.GLACIAL_ORB);
        activeProjectiles.add(orb);

        attributes.notifyCombatLog("❄️ Glacial Orb launched! Flying forward to freeze enemies for 1.5s!");
    }

    public void inputQTEBreak(boolean isLeftKey) {
        if (stateMachine.getCurrentState() instanceof RootedState) {
            ((RootedState) stateMachine.getCurrentState()).onMashInput(this, isLeftKey);
        }
    }

    public void applyRootTrap() {
        if (!invincible && !(stateMachine.getCurrentState() instanceof RootedState)) {
            comboManager.resetCombo();
            stateMachine.changeState(new RootedState());
        }
    }

    public void spawnHitbox(float damage, float width, float height, float lifetime, boolean canBreakShield) {
        float spawnX = facingRight ? (x + this.width) : (x - width);
        Hitbox hitbox = new Hitbox(spawnX, y + 8f, width, height, damage, lifetime, canBreakShield);
        activeHitboxes.add(hitbox);
    }

    public void spawnShockwave(float damage, float radius, float height) {
        float spawnX = x - (radius / 2f) + (this.width / 2f);
        Hitbox shockwave = new Hitbox(spawnX, y, radius, height, damage, 0.35f, true);
        activeHitboxes.add(shockwave);
    }

    public void spawnProjectile(Projectile projectile) {
        if (projectile != null) {
            activeProjectiles.add(projectile);
        }
    }

    public void takeDamage(int dmg) {
        if (invincible) {
            attributes.notifyCombatLog("🛡️ Attack dodged via invincibility frame (i-frame)!");
            return;
        }
        attributes.takeDamage(dmg);
        attributes.notifyCombatLog("💔 Player took " + dmg + " damage! Remaining HP: " + attributes.getCurrentHP());
    }

    public boolean canAct() {
        return !(stateMachine.getCurrentState() instanceof RootedState)
                && !(stateMachine.getCurrentState() instanceof DashState)
                && !(stateMachine.getCurrentState() instanceof SeismicSlamState)
                && !(stateMachine.getCurrentState() instanceof AttackState)
                && !(stateMachine.getCurrentState() instanceof ComboRecoveryState)
                && !attributes.isDead();
    }

    public void setWeapon(AttackStrategy newStrategy) {
        this.attackStrategy = newStrategy;
        attributes.notifyCombatLog("🗡️ Switched weapon to: " + newStrategy.getWeaponName());
    }

    // Getters and Setters
    public float getX() { return x; }
    public void setX(float x) { this.x = x; }
    public float getY() { return y; }
    public void setY(float y) { this.y = y; }
    public float getGroundY() { return groundY; }
    public void setGroundY(float groundY) { this.groundY = groundY; }
    public float getVelocityX() { return velocityX; }
    public void setVelocityX(float vx) { this.velocityX = vx; }
    public float getVelocityY() { return velocityY; }
    public void setVelocityY(float vy) { this.velocityY = vy; }
    public float getWidth() { return width; }
    public float getHeight() { return height; }
    public boolean isFacingRight() { return facingRight; }
    public void setFacingRight(boolean facingRight) { this.facingRight = facingRight; }
    public boolean isInvincible() { return invincible; }
    public Hitbox spawnComboHitbox(float damage, float width, float height,
                                   float startup, float active, float recovery, boolean canBreakShield) {
        float spawnX = facingRight ? (x + this.width) : (x - width);
        Hitbox hitbox = new Hitbox(spawnX, y + 8f, width, height, damage, startup, active, recovery, canBreakShield);
        activeHitboxes.add(hitbox);
        return hitbox;
    }

    public void respawn(float spawnX, float spawnY) {
        this.x = spawnX;
        this.y = spawnY;
        this.velocityX = 0f;
        this.velocityY = 0f;
        this.grounded = true;
        this.attributes.restoreFullHealthAndStamina();
        this.stateMachine.changeState(new IdleState());
        this.activeHitboxes.clear();
        this.activeProjectiles.clear();
        this.comboManager.resetCombo();
        this.attributes.notifyCombatLog("✨ Respawned at active checkpoint!");
    }

    public com.Blasphemeows.combat.ComboManager getComboManager() {
        return comboManager;
    }

    public void setGrounded(boolean grounded) {
        this.grounded = grounded;
    }

    public void setInvincible(boolean invincible) { this.invincible = invincible; }
    public boolean isGrounded() { return grounded; }
    public PlayerAttributes getAttributes() { return attributes; }
    public PlayerStateMachine getStateMachine() { return stateMachine; }
    public AttackStrategy getAttackStrategy() { return attackStrategy; }
    public List<Hitbox> getActiveHitboxes() { return activeHitboxes; }
    public List<Projectile> getActiveProjectiles() { return activeProjectiles; }
}
