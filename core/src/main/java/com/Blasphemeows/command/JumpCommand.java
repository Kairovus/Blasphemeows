package com.Blasphemeows.command;

import com.Blasphemeows.player.Player;

/**
 * JumpCommand:
 * Dispatches jump action to the Player entity.
 */
public class JumpCommand implements Command {
    @Override
    public void execute(Player player) {
        player.jump();
    }
}
