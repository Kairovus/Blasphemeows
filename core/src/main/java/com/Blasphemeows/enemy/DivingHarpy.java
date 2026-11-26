package com.Blasphemeows.enemy;

import com.Blasphemeows.player.Player;

/**
 * Branch 1: The Forgotten Temple - Special / Air Enemy
 * DivingHarpy: Hovers in the sky, tracks player, and dashes directly downward at high speed.
 */
public class DivingHarpy extends Enemy {
    private enum State { HOVER, WINDUP, DIVE, RECOVER }
    private State state = State.HOVER;
    private float stateTimer = 0f;
    private float targetX;
    private float targetY;
    private boolean hasHitPlayer;
    private final float hoverAltitude = 120f;

    public DivingHarpy(float x, float y) {
        super("Diving Harpy", x, y + 120f, 36f, 36f, 65, 85f);
        this.y = y + hoverAltitude;
        this.xpReward = 5; // Special enemy reward
        this.scoreType = com.Blasphemeows.score.EnemyScoreType.SPECIAL_REGIONAL;
    }

    @Override
    public void update(float delta, Player player) {
        updateStatusEffects(delta);
        if (isStunnedOrFrozen() || !isAlive() || player == null) {
            telegraphingAttack = false;
            // If stunned or frozen, fall to ground
            if (isStunnedOrFrozen() && y > 0) {
                y = Math.max(0, y - 200f * delta);
            }
            return;
        }

        facingRight = player.getX() > x;
        stateTimer += delta;

        switch (state) {
            case HOVER:
                // Move towards player horizontally above their head
                float targetHoverX = player.getX() + (facingRight ? -40f : 40f);
                x += (targetHoverX > x ? 1f : -1f) * moveSpeed * delta;
                y = hoverAltitude + (float) Math.sin(stateTimer * 4f) * 15f;

                if (stateTimer >= 2.5f) {
                    state = State.WINDUP;
                    stateTimer = 0f;
                    targetX = player.getX();
                    targetY = player.getY();
                    hasHitPlayer = false;
                    telegraphingAttack = true;
                    System.out.println("🦅 [Diving Harpy] Locks onto the player before diving!");
                }
                break;

            case WINDUP:
                if (stateTimer >= 0.55f) {
                    state = State.DIVE;
                    stateTimer = 0f;
                    telegraphingAttack = false;
                }
                break;

            case DIVE:
                // High-speed dive dash towards targeted player position
                float dx = targetX - x;
                float dy = targetY - y;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);

                if (dist > 10f && stateTimer < 0.6f) {
                    x += (dx / dist) * 360f * delta;
                    y += (dy / dist) * 360f * delta;

                    // Contact damage during dive
                    if (!hasHitPlayer && Math.abs(player.getX() - x) < 32f && Math.abs(player.getY() - y) < 32f) {
                        player.takeDamage(18);
                        hasHitPlayer = true;
                    }
                } else {
                    state = State.RECOVER;
                    stateTimer = 0f;
                    telegraphingAttack = false;
                }
                break;

            case RECOVER:
                // Ascent back into the sky
                y += 140f * delta;
                if (y >= hoverAltitude || stateTimer >= 1.2f) {
                    y = hoverAltitude;
                    state = State.HOVER;
                    stateTimer = 0f;
                }
                break;
        }
    }
}
