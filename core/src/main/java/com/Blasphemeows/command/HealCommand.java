package com.Blasphemeows.command;

import com.Blasphemeows.player.Player;

/**
 * Command Pattern: HealCommand
 * Consumes one of the player's 3 healing flask charges to restore health.
 */
public class HealCommand implements Command {
    @Override
    public void execute(Player player) {
        player.heal();
    }
}
