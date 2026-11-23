package com.Blasphemeows.state;

import com.Blasphemeows.player.Player;

/**
 * IdleState: Player is resting, regenerates stamina at standard rate.
 */
public class IdleState implements PlayerState {
    @Override
    public void enter(Player player) {
        player.setVelocityX(0);
    }

    @Override
    public void update(Player player, float delta) {
        player.getAttributes().regenerateStamina(delta);
    }

    @Override
    public void exit(Player player) {
    }

    @Override
    public String getName() {
        return "Idle";
    }
}
