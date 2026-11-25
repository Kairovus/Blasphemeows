package com.Blasphemeows.state;

import com.Blasphemeows.player.Player;

/**
 * MoveState: Player is moving horizontally.
 */
public class MoveState implements PlayerState {
    @Override
    public void enter(Player player) {
    }

    @Override
    public void update(Player player, float delta) {
        player.getAttributes().regenerateStamina(delta * 0.5f);
        if (Math.abs(player.getVelocityX()) < 0.01f) {
            player.getStateMachine().changeState(new IdleState());
        }
    }

    @Override
    public void exit(Player player) {
    }

    @Override
    public String getName() {
        return "Moving";
    }
}
