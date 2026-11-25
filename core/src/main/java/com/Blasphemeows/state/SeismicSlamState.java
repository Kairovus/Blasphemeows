package com.Blasphemeows.state;

import com.Blasphemeows.player.Player;

/**
 * SeismicSlamState: Unlocked from Ashen Foundry.
 * Player leaps upward and slams downward violently, causing a devastating
 * ground shockwave that damages enemies in a wide radius.
 */
public class SeismicSlamState implements PlayerState {
    private enum Phase { ASCENT, DESCENT, IMPACT }
    private Phase phase = Phase.ASCENT;
    private float phaseTimer = 0f;

    @Override
    public void enter(Player player) {
        phase = Phase.ASCENT;
        phaseTimer = 0f;
        player.setVelocityX(0);
        player.setVelocityY(280.0f); // Launch upward
        player.setInvincible(true);
        player.getAttributes().notifyCombatLog("[Ability] Seismic Slam initiated!");
    }

    @Override
    public void update(Player player, float delta) {
        phaseTimer += delta;

        switch (phase) {
            case ASCENT:
                if (phaseTimer >= 0.22f) {
                    phase = Phase.DESCENT;
                    phaseTimer = 0f;
                    player.setVelocityY(-450.0f); // Slam downward
                }
                break;

            case DESCENT:
                if (phaseTimer >= 0.25f || player.getY() <= 0) {
                    phase = Phase.IMPACT;
                    phaseTimer = 0f;
                    player.setVelocityY(0);
                    player.setY(0);

                    // Unleash ground shockwave hitbox
                    float shockwaveDmg = 45.0f * player.getAttributes().getPhysicalDamageScaling();
                    player.spawnShockwave(shockwaveDmg, 160.0f, 40.0f);
                    player.getAttributes().notifyCombatLog("💥 Seismic Slam IMPACT! Ground shockwave triggered!");
                }
                break;

            case IMPACT:
                if (phaseTimer >= 0.25f) {
                    player.getStateMachine().changeState(new IdleState());
                }
                break;
        }
    }

    @Override
    public void exit(Player player) {
        player.setInvincible(false);
        player.setVelocityY(0);
    }

    @Override
    public String getName() {
        return "Seismic Slam";
    }
}
