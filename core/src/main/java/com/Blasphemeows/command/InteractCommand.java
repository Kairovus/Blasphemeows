package com.Blasphemeows.command;

import com.Blasphemeows.player.Player;

/**
 * Command Pattern: InteractCommand
 * Handles interaction with portals, recovery shrines, and environmental objects.
 */
public class InteractCommand implements Command {
    public interface InteractionHandler {
        void onInteract(Player player);
    }

    private final InteractionHandler handler;

    public InteractCommand(InteractionHandler handler) {
        this.handler = handler;
    }

    @Override
    public void execute(Player player) {
        if (handler != null) {
            handler.onInteract(player);
        }
    }
}
