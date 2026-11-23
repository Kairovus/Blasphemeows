package com.Blasphemeows.combat;

import com.Blasphemeows.player.Player;
import com.Blasphemeows.state.AttackState;

import java.util.ArrayList;
import java.util.List;

/**
 * ComboManager:
 * Manages combo sequences, input buffering, combo timeouts, and transitions.
 * Supports sequences such as L->L->L, H->H, and L->L->H.
 */
public class ComboManager {
    private final List<Character> currentSequence = new ArrayList<Character>();
    private Character bufferedInput = null;
    private float comboTimer = 0f;
    private static final float COMBO_WINDOW = 0.75f;
    private static final int MAX_COMBO_LENGTH = 5;

    public void update(float delta, Player player) {
        if (!currentSequence.isEmpty()) {
            comboTimer += delta;
            // If player does not continue attacking within combo window and is not in AttackState, reset
            if (comboTimer >= COMBO_WINDOW && !(player.getStateMachine().getCurrentState() instanceof AttackState)) {
                resetCombo();
            }
        }
    }

    public void bufferInput(char inputType) {
        this.bufferedInput = inputType;
    }

    public boolean hasBufferedInput() {
        return bufferedInput != null;
    }

    public Character consumeBufferedInput() {
        Character input = bufferedInput;
        bufferedInput = null;
        return input;
    }

    public void recordAttack(char attackType) {
        comboTimer = 0f;
        bufferedInput = null;
        if (currentSequence.size() >= MAX_COMBO_LENGTH) {
            currentSequence.clear();
        }
        currentSequence.add(attackType);
    }

    public void resetCombo() {
        currentSequence.clear();
        bufferedInput = null;
        comboTimer = 0f;
    }

    public List<Character> getCurrentSequence() {
        return currentSequence;
    }

    public String getSequenceString() {
        if (currentSequence.isEmpty()) return "None";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < currentSequence.size(); i++) {
            sb.append(currentSequence.get(i));
            if (i < currentSequence.size() - 1) sb.append(" -> ");
        }
        return sb.toString();
    }
}
