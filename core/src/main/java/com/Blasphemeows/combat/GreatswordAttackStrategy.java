package com.Blasphemeows.combat;

import com.Blasphemeows.player.Player;
import com.Blasphemeows.state.AttackState;

import java.util.List;

/**
 * Greatsword (Damage-focused):
 * Heavy, high damage swings with longer startup and recovery frames.
 * Heavy attacks break front shields during vulnerability windows.
 */
public class GreatswordAttackStrategy implements AttackStrategy {
    private final AttackData L1 = new AttackData("Greatsword L1 (Sweeping Arc)", false, 1.6f, 18f, 0.12f, 0.16f, 0.32f, 54f, 48f, false);
    private final AttackData L2 = new AttackData("Greatsword L2 (Upward Cleave)", false, 1.9f, 22f, 0.14f, 0.18f, 0.38f, 58f, 52f, false);
    private final AttackData H1 = new AttackData("Greatsword H1 (Crushing Chop)", true, 2.6f, 32f, 0.22f, 0.22f, 0.55f, 62f, 56f, true);
    private final AttackData H2 = new AttackData("Greatsword H2 (Cataclysmic Slam)", true, 3.4f, 40f, 0.30f, 0.25f, 0.70f, 72f, 64f, true);

    @Override
    public String getWeaponName() {
        return "Colossal Greatsword";
    }

    @Override
    public String getDescription() {
        return "Massive two-handed greatsword. Slower startup and longer recovery, but delivers colossal damage and breaks shields.";
    }

    @Override
    public void executeLightAttack(Player player) {
        if (player.getStateMachine().getCurrentState() instanceof AttackState) {
            AttackState currentAttack = (AttackState) player.getStateMachine().getCurrentState();
            if (currentAttack.canCancelIntoCombo()) {
                currentAttack.cancelAttack();
            } else {
                player.getComboManager().bufferInput('L');
                return;
            }
        }

        List<Character> seq = player.getComboManager().getCurrentSequence();
        AttackData attack = (!seq.isEmpty() && seq.get(seq.size() - 1) == 'L') ? L2 : L1;
        launchAttack(player, attack, 'L');
    }

    @Override
    public void executeHeavyAttack(Player player) {
        if (player.getStateMachine().getCurrentState() instanceof AttackState) {
            AttackState currentAttack = (AttackState) player.getStateMachine().getCurrentState();
            if (currentAttack.canCancelIntoCombo()) {
                currentAttack.cancelAttack();
            } else {
                player.getComboManager().bufferInput('H');
                return;
            }
        }

        List<Character> seq = player.getComboManager().getCurrentSequence();
        AttackData attack = (!seq.isEmpty() && seq.get(seq.size() - 1) == 'H') ? H2 : H1;
        launchAttack(player, attack, 'H');
    }

    private void launchAttack(Player player, AttackData attack, char inputType) {
        if (!player.getAttributes().consumeStamina(attack.getStaminaCost())) {
            player.getComboManager().resetCombo();
            return;
        }

        int dex = player.getAttributes().getDexterity();
        float baseDmg = 20.0f;
        float totalScaling = player.getAttributes().getPhysicalDamageScaling() + player.getAttributes().getDexterityDamageBonus();
        float finalDmg = baseDmg * attack.getDamageMultiplier() * totalScaling;

        Hitbox hitbox = player.spawnComboHitbox(
                finalDmg,
                attack.getHitboxWidth(),
                attack.getHitboxHeight(),
                attack.getStartupDuration(),
                attack.getActiveDuration(),
                attack.getAdjustedRecovery(dex),
                attack.canBreakShield()
        );

        player.getComboManager().recordAttack(inputType);
        player.getStateMachine().changeState(new AttackState(attack, hitbox, dex));
        player.getAttributes().notifyCombatLog("⚔️ [" + player.getComboManager().getSequenceString() + "] " +
                attack.getName() + " -> " + (int) finalDmg + " dmg! " + (attack.canBreakShield() ? "(Shield-Breaker)" : ""));
    }

    @Override
    public float getStaminaCost(boolean isHeavy) {
        return isHeavy ? 32f : 18f;
    }

    @Override
    public float getCooldown(boolean isHeavy) {
        return isHeavy ? 0.55f : 0.32f;
    }
}
