package com.Blasphemeows.command;

import com.Blasphemeows.player.Player;

public class DodgeOrDashCommand implements Command {
    @Override
    public void execute(Player player) {
        player.performDodgeOrDash();
    }
}
