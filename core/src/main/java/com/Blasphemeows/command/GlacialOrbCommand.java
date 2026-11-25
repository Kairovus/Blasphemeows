package com.Blasphemeows.command;

import com.Blasphemeows.enemy.Enemy;
import com.Blasphemeows.player.Player;

public class GlacialOrbCommand implements Command {
    private final Enemy target;

    public GlacialOrbCommand(Enemy target) {
        this.target = target;
    }

    public GlacialOrbCommand() {
        this(null);
    }

    @Override
    public void execute(Player player) {
        player.performGlacialOrb(target);
    }
}
