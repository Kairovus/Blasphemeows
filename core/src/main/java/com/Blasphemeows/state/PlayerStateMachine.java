package com.Blasphemeows.state;

import com.Blasphemeows.player.Player;

/**
 * State machine managing transitions and updates between Player states.
 */
public class PlayerStateMachine {
    private final Player player;
    private PlayerState currentState;

    public PlayerStateMachine(Player player) {
        this.player = player;
    }

    public void changeState(PlayerState newState) {
        if (newState == null) return;
        if (currentState != null) {
            currentState.exit(player);
        }
        currentState = newState;
        currentState.enter(player);
        player.getAttributes().notifyStateChanged(currentState.getName());
    }

    public void update(float delta) {
        if (currentState != null) {
            currentState.update(player, delta);
        }
    }

    public PlayerState getCurrentState() {
        return currentState;
    }

    public String getCurrentStateName() {
        return currentState != null ? currentState.getName() : "None";
    }
}
