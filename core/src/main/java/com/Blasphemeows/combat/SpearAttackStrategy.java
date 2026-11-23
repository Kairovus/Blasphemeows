package com.Blasphemeows.combat;

import com.Blasphemeows.player.Player;
import com.Blasphemeows.state.AttackState;

import java.util.List;

/**
 * Spear (Range-focused):
 * Long reach with fast linear thrusts and distance management.
 */
public class SpearAttackStrategy implements AttackStrategy {
    private final AttackData L1 = new AttackData("Spear L1 (Quick Poke)", false, 1.1f, 10f, 0.05f, 0.10f, 0.16f, 75f, 24f, false);
    private final AttackData L2 = new AttackData("Spear L2 (Double Jab)", false, 1.35f, 12f, 0.06f, 0.12f, 0.18f, 82f, 26f, false);
    private final AttackData L3 = new AttackData("Spear L3 (Deep Thrust)", false, 1.75f, 16f, 0.08f, 0.14f, 0.24f, 92f, 28f, false);
    private final AttackData H1 = new AttackData("Spear H1 (Sweeping Halberd)", true, 1.90f, 24f, 0.16f, 0.18f, 0.42f, 65f, 44f, false);
    private final AttackData H2 = new AttackData("Spear H2 (Drilling Lunge)", true, 2.50f, 28f, 0.18f, 0.20f, 0.48f, 105f, 30f, true);

    @Override
    public String getWeaponName() {
        return "Silver Spear";
    }

    @Override
    public String getDescription() {
        return "Long piercing polearm. Extended forward reach to punish enemies safely from distance.";
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
        AttackData attack;
        if (seq.isEmpty()) {
            attack = L1;
        } else if (seq.size() == 1 && seq.get(0) == 'L') {
            attack = L2;
        } else if (seq.size() == 2 && seq.get(0) == 'L' && seq.get(1) == 'L') {
            attack = L3;
        } else {
            player.getComboManager().resetCombo();
            attack = L1;
        }

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
        float baseDmg = 15.0f;
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
        player.getAttributes().notifyCombatLog("🔱 [" + player.getComboManager().getSequenceString() + "] " +
                attack.getName() + " -> " + (int) finalDmg + " dmg!");
    }

    @Override
    public float getStaminaCost(boolean isHeavy) {
        return isHeavy ? 24f : 10f;
    }

    @Override
    public float getCooldown(boolean isHeavy) {
        return isHeavy ? 0.42f : 0.16f;
    }
}
