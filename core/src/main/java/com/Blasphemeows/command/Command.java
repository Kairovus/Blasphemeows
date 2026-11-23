package com.Blasphemeows.command;

import com.Blasphemeows.player.Player;

/**
 * Command Pattern: Command
 * Encapsulates an action request as an object, decoupling user input
 * from the execution on the Player entity.
 */
public interface Command {
    void execute(Player player);
}
