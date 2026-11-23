package com.Blasphemeows.command;

import com.Blasphemeows.combat.AttackStrategy;
import com.Blasphemeows.player.Player;

public class SwitchWeaponCommand implements Command {
    private final AttackStrategy newStrategy;

    public SwitchWeaponCommand(AttackStrategy newStrategy) {
        this.newStrategy = newStrategy;
    }

    @Override
    public void execute(Player player) {
        player.setWeapon(newStrategy);
    }
}
