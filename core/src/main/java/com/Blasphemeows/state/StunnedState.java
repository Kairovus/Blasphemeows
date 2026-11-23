package com.Blasphemeows.state;

import com.Blasphemeows.player.Player;

/**
 * StunnedState: Player is incapacitated for a brief duration after receiving heavy blow.
 */
public class StunnedState implements PlayerState {
    private final float duration;
    private float timer = 0f;

    public StunnedState(float duration) {
        this.duration = duration;
    }

    @Override
    public void enter(Player player) {
        player.setVelocityX(0);
        player.getAttributes().notifyCombatLog("💫 Player is stunned for " + duration + "s!");
    }

    @Override
    public void update(Player player, float delta) {
        timer += delta;
        if (timer >= duration) {
            player.getStateMachine().changeState(new IdleState());
        }
    }

    @Override
    public void exit(Player player) {
    }

    @Override
    public String getName() {
        return "Stunned";
    }
}
