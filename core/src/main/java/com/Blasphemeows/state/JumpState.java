package com.Blasphemeows.state;

import com.Blasphemeows.player.Player;

/**
 * JumpState: Player is airborne after jumping.
 * Allows horizontal aerial control. Transitions back to Idle or Moving upon landing.
 */
public class JumpState implements PlayerState {
    @Override
    public void enter(Player player) {
    }

    @Override
    public void update(Player player, float delta) {
        if (player.isGrounded()) {
            if (Math.abs(player.getVelocityX()) > 10f) {
                player.getStateMachine().changeState(new MoveState());
            } else {
                player.getStateMachine().changeState(new IdleState());
            }
        }
    }

    @Override
    public void exit(Player player) {
    }

    @Override
    public String getName() {
        return "Airborne (Jump)";
    }
}
