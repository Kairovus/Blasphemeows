package com.Blasphemeows.command;

import com.Blasphemeows.player.Player;

public class LightAttackCommand implements Command {
    @Override
    public void execute(Player player) {
        player.performLightAttack();
    }
}
