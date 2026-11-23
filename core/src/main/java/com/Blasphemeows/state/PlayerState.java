package com.Blasphemeows.state;

import com.Blasphemeows.player.Player;

/**
 * State Pattern: PlayerState
 * Interface representing a specific state of the Player.
 */
public interface PlayerState {
    void enter(Player player);
    void update(Player player, float delta);
    void exit(Player player);
    String getName();
}
