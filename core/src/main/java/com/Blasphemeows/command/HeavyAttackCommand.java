package com.Blasphemeows.command;

import com.Blasphemeows.player.Player;

public class HeavyAttackCommand implements Command {
    @Override
    public void execute(Player player) {
        player.performHeavyAttack();
    }
}
