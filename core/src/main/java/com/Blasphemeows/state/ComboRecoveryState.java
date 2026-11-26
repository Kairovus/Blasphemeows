package com.Blasphemeows.state;

import com.Blasphemeows.player.Player;

/** Locks player actions after a completed sword combo route. */
public class ComboRecoveryState implements PlayerState {
    private float remaining;

    public ComboRecoveryState(float duration) {
        this.remaining = Math.max(0f, duration);
    }

    @Override
    public void enter(Player player) {
        player.setVelocityX(0f);
    }

    @Override
    public void update(Player player, float delta) {
        player.setVelocityX(0f);
        remaining -= delta;
        if (remaining <= 0f) {
            player.getComboManager().resetCombo();
            player.getStateMachine().changeState(new IdleState());
        }
    }

    @Override
    public void exit(Player player) {
        player.setVelocityX(0f);
    }

    @Override
    public String getName() {
        return "Combo Recovery";
    }
}
