package com.Blasphemeows.combat;

import com.Blasphemeows.player.Player;
import com.Blasphemeows.state.AttackState;

import java.util.List;

/**
 * Sword (Longsword):
 * Balanced damage and speed with versatile combo chains:
 * 1. L -> L -> L (Slash -> Cross Cut -> Spin Finisher)
 * 2. H -> H     (Heavy Cleave -> Ground Breaker)
 * 3. L -> L -> H (Slash -> Cross Cut -> Piercing Heavy Thrust)
 */
public class SwordAttackStrategy implements AttackStrategy {
    // Attack frame definitions: (name, isHeavy, dmgMult, stamina, startup, active, recovery, width, height, canBreakShield)
    private final AttackData L1 = new AttackData("Sword L1 (Quick Slash)", false, 1.0f, 10f, 0.05f, 0.12f, 0.14f, 44f, 36f, false);
    private final AttackData L2 = new AttackData("Sword L2 (Cross Cut)", false, 1.25f, 12f, 0.05f, 0.12f, 0.14f, 48f, 38f, false);
    private final AttackData L3 = new AttackData("Sword L3 (Spin Finisher)", false, 1.70f, 16f, 0.08f, 0.16f, 0.22f, 56f, 42f, false);

    private final AttackData H1 = new AttackData("Sword H1 (Heavy Cleave)", true, 1.85f, 22f, 0.15f, 0.18f, 0.40f, 52f, 46f, false);
    private final AttackData H2 = new AttackData("Sword H2 (Ground Breaker)", true, 2.70f, 30f, 0.22f, 0.20f, 0.55f, 60f, 52f, true);

    private final AttackData LLH = new AttackData("Sword LLH (Piercing Thrust)", true, 2.30f, 26f, 0.12f, 0.18f, 0.44f, 70f, 32f, true);

    @Override
    public String getWeaponName() {
        return "Steel Longsword";
    }

    @Override
    public String getDescription() {
        return "Balanced blade with versatile light/heavy combo chains: L->L->L, H->H, and L->L->H.";
    }

    @Override
    public void executeLightAttack(Player player) {
        if (player.getStateMachine().getCurrentState() instanceof AttackState) {
            AttackState currentAttack = (AttackState) player.getStateMachine().getCurrentState();
            if (currentAttack.canCancelIntoCombo()) {
                // Fighting game cancel: Immediately cancel remaining recovery
                currentAttack.cancelAttack();
            } else {
                // Buffer input during early startup
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
            attack = L3; // Finisher LLL
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
                // Fighting game cancel: Immediately cancel remaining recovery
                currentAttack.cancelAttack();
            } else {
                // Buffer input during early startup
                player.getComboManager().bufferInput('H');
                return;
            }
        }

        List<Character> seq = player.getComboManager().getCurrentSequence();
        AttackData attack;

        if (seq.isEmpty()) {
            attack = H1;
        } else if (seq.size() == 1 && seq.get(0) == 'H') {
            attack = H2; // Finisher HH
        } else if (seq.size() == 2 && seq.get(0) == 'L' && seq.get(1) == 'L') {
            attack = LLH; // Finisher LLH
        } else {
            player.getComboManager().resetCombo();
            attack = H1;
        }

        launchAttack(player, attack, 'H');
    }

    private void launchAttack(Player player, AttackData attack, char inputType) {
        if (!player.getAttributes().consumeStamina(attack.getStaminaCost())) {
            player.getComboManager().resetCombo();
            return;
        }

        int dex = player.getAttributes().getDexterity();
        float baseDmg = 16.0f;
        float totalScaling = player.getAttributes().getPhysicalDamageScaling() + player.getAttributes().getDexterityDamageBonus();
        float finalDmg = baseDmg * attack.getDamageMultiplier() * totalScaling;

        // Spawn frame-governed hitbox
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
        player.getAttributes().notifyCombatLog("🗡️ [" + player.getComboManager().getSequenceString() + "] " +
                attack.getName() + " -> " + (int) finalDmg + " dmg!");
    }

    @Override
    public float getStaminaCost(boolean isHeavy) {
        return isHeavy ? 22f : 10f;
    }

    @Override
    public float getCooldown(boolean isHeavy) {
        return isHeavy ? 0.40f : 0.14f;
    }
}
