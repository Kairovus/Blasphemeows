package com.Blasphemeows.command;

import com.Blasphemeows.player.Player;

public class MoveCommand implements Command {
    private final float direction;

    public MoveCommand(float direction) {
        this.direction = direction;
    }

    @Override
    public void execute(Player player) {
        player.move(direction);
    }
}
