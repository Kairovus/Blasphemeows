package com.Blasphemeows.state;

import com.Blasphemeows.combat.AttackData;
import com.Blasphemeows.combat.Hitbox;
import com.Blasphemeows.player.Player;

/**
 * AttackState:
 * Governs active attack execution, startup, active frames, and recovery.
 * Supports fighting game combo cancels:
 * - Once active frames begin (startup finished), pressing the next combo attack
 *   immediately cancels the remaining recovery frames with ZERO delay.
 * - If inputs are buffered during startup, they trigger immediately once active begins.
 * - If the player whiffs or does not continue the combo, full recovery frames play out.
 */
public class AttackState implements PlayerState {
    private final AttackData attackData;
    private final Hitbox hitbox;
    private final float totalDuration;
    private float timer = 0f;
    private boolean chained = false;

    public AttackState(AttackData attackData, Hitbox hitbox, int dexterity) {
        this.attackData = attackData;
        this.hitbox = hitbox;
        this.totalDuration = attackData.getTotalDuration(dexterity);
    }

    @Override
    public void enter(Player player) {
        // Slight forward step on attack
        float step = attackData.isHeavy() ? 35f : 20f;
        player.setVelocityX((player.isFacingRight() ? 1f : -1f) * step);
    }

    @Override
    public void update(Player player, float delta) {
        timer += delta;
        player.setVelocityX(player.getVelocityX() * 0.7f);

        // Keep hitbox aligned with player
        if (hitbox != null && !hitbox.isFinished()) {
            float spawnX = player.isFacingRight() ? (player.getX() + player.getWidth()) : (player.getX() - hitbox.getWidth());
            hitbox.setPosition(spawnX, player.getY() + 8f);
        }

        // Fighting game combo cancel:
        // As soon as startup completes (hitbox active), any buffered combo input
        // instantly cancels the recovery and chains into the next attack!
        if (canCancelIntoCombo() && !chained && player.getComboManager().hasBufferedInput()) {
            chained = true;
            char nextInput = player.getComboManager().consumeBufferedInput();
            cancelAttack();
            if (nextInput == 'L') {
                player.performLightAttack();
            } else if (nextInput == 'H') {
                player.performHeavyAttack();
            }
            return;
        }

        if (timer >= totalDuration) {
            player.getStateMachine().changeState(new IdleState());
        }
    }

    /**
     * In fighting game combo rules, an attack can cancel into the next attack
     * of a combo chain the moment startup completes and active damage frames begin.
     */
    public boolean canCancelIntoCombo() {
        return timer >= attackData.getStartupDuration();
    }

    /**
     * Cancels the current attack's recovery immediately.
     */
    public void cancelAttack() {
        this.chained = true;
        if (hitbox != null) {
            hitbox.deactivate();
        }
    }

    @Override
    public void exit(Player player) {
        if (hitbox != null && !chained) {
            hitbox.deactivate();
        }
        player.setVelocityX(0f);
    }

    @Override
    public String getName() {
        return attackData != null ? attackData.getName() : "Attacking";
    }

    public AttackData getAttackData() {
        return attackData;
    }

    /** Seconds since this attack began (used by PlayerAnimator to sync the sprite). */
    public float getElapsed() {
        return timer;
    }

    /** Startup + active + recovery, in seconds. */
    public float getTotalDuration() {
        return totalDuration;
    }
}
