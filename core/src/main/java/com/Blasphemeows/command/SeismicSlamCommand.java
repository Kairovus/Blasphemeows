package com.Blasphemeows.command;

import com.Blasphemeows.player.Player;

public class SeismicSlamCommand implements Command {
    @Override
    public void execute(Player player) {
        player.performSeismicSlam();
    }
}
