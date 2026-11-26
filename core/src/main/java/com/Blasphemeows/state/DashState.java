package com.Blasphemeows.state;

import com.Blasphemeows.player.Player;

/**
 * DashState: Player dashes horizontally with invincibility frames (i-frames).
 * If Monk Dash is unlocked from The Forgotten Temple, the dash travels further and has longer i-frames.
 */
public class DashState implements PlayerState {
    private final boolean isMonkDashUpgraded;
    private final float duration;
    private final float dashSpeed;
    private float timer = 0f;

    public DashState(boolean isMonkDashUpgraded) {
        this.isMonkDashUpgraded = isMonkDashUpgraded;
        // Monk Dash has extended i-frames and distance
        this.duration = isMonkDashUpgraded ? 0.38f : 0.22f;
        this.dashSpeed = isMonkDashUpgraded ? 420.0f : 260.0f;
    }

    @Override
    public void enter(Player player) {
        player.setInvincible(true);
        if (isMonkDashUpgraded) {
            player.setVelocityY(0f);
        }
        float direction = player.isFacingRight() ? 1.0f : -1.0f;
        player.setVelocityX(direction * dashSpeed);
        player.getAttributes().notifyCombatLog(isMonkDashUpgraded ?
                "[Ability] Monk Dash activated! (Extended i-frame & reach)" :
                "[Action] Dodge roll activated!");
    }

    @Override
    public void update(Player player, float delta) {
        timer += delta;
        if (isMonkDashUpgraded) {
            player.setVelocityY(0f);
        }
        float direction = player.isFacingRight() ? 1.0f : -1.0f;
        player.setVelocityX(direction * dashSpeed * (1.0f - (timer / duration) * 0.4f));

        if (timer >= duration) {
            player.getStateMachine().changeState(new IdleState());
        }
    }

    @Override
    public void exit(Player player) {
        player.setInvincible(false);
        player.setVelocityX(0);
    }

    @Override
    public String getName() {
        return isMonkDashUpgraded ? "Monk Dash" : "Dodge";
    }

    public boolean isMonkDashUpgraded() {
        return isMonkDashUpgraded;
    }
}
