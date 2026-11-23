package com.Blasphemeows.command;

import com.Blasphemeows.player.Player;

/**
 * QTEMashCommand:
 * Sent during RootedState to mash Left or Right keys to break free from Root Weaver's vines.
 */
public class QTEMashCommand implements Command {
    private final boolean isLeftKey;

    public QTEMashCommand(boolean isLeftKey) {
        this.isLeftKey = isLeftKey;
    }

    @Override
    public void execute(Player player) {
        player.inputQTEBreak(isLeftKey);
    }

    public boolean isLeftKey() {
        return isLeftKey;
    }
}
