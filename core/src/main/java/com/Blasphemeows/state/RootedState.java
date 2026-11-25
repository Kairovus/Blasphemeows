package com.Blasphemeows.state;

import com.Blasphemeows.player.Player;

/**
 * RootedState: Triggered by Overgrown Spire's Root Weaver enemy.
 * Entangles the player in choking mystic roots, immobilizing them.
 * The player must mash alternating Left and Right keys (e.g. A and D) to break free!
 * Sustains tick damage over time if not broken promptly.
 */
public class RootedState implements PlayerState {
    private int mashCounter = 0;
    private final int MASH_REQUIRED = 6;
    private Boolean lastKeyWasLeft = null;
    private float tickTimer = 0f;
    private final float DAMAGE_INTERVAL = 1.0f;
    private final int TICK_DAMAGE = 8;

    @Override
    public void enter(Player player) {
        player.setVelocityX(0);
        mashCounter = 0;
        lastKeyWasLeft = null;
        player.getAttributes().notifyCombatLog("⚠️ [Rooted] Entangled by roots! Mash LEFT and RIGHT repeatedly to break free!");
    }

    public void onMashInput(Player player, boolean isLeft) {
        // Player must alternate left and right inputs
        if (lastKeyWasLeft == null || lastKeyWasLeft != isLeft) {
            lastKeyWasLeft = isLeft;
            mashCounter++;
            player.getAttributes().notifyCombatLog("[QTE] Breaking roots: (" + mashCounter + " / " + MASH_REQUIRED + ")");
            if (mashCounter >= MASH_REQUIRED) {
                player.getAttributes().notifyCombatLog("✨ Roots shattered! Regained freedom of movement.");
                player.getStateMachine().changeState(new IdleState());
            }
        }
    }

    @Override
    public void update(Player player, float delta) {
        player.setVelocityX(0);
        tickTimer += delta;
        if (tickTimer >= DAMAGE_INTERVAL) {
            tickTimer = 0f;
            player.takeDamage(TICK_DAMAGE);
            player.getAttributes().notifyCombatLog("💥 Root strangulation deals " + TICK_DAMAGE + " damage!");
        }
    }

    @Override
    public void exit(Player player) {
    }

    @Override
    public String getName() {
        return "Rooted (Mash A/D to escape: " + mashCounter + "/" + MASH_REQUIRED + ")";
    }

    public int getMashCounter() {
        return mashCounter;
    }

    public int getMashRequired() {
        return MASH_REQUIRED;
    }
}
