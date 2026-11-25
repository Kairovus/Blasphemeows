package com.Blasphemeows.enemy;

import com.Blasphemeows.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Branch 3: Overgrown Spire - Summoner
 * SporeSummoner: Conjures aggressive spore minions into battle.
 */
public class SporeSummoner extends Enemy {
    private float summonTimer = 0f;
    private final float summonInterval = 5.0f;
    private final List<SporeMinion> spawnedMinions = new ArrayList<SporeMinion>();

    public SporeSummoner(float x, float y) {
        super("Spore Summoner", x, y, 32f, 52f, 85, 60f);
    }

    @Override
    public void update(float delta, Player player) {
        updateStatusEffects(delta);
        if (isStunnedOrFrozen() || !isAlive() || player == null) {
            updateMinions(delta, player);
            return;
        }

        facingRight = player.getX() > x;
        float distToPlayer = Math.abs(player.getX() - x);

        // Keep distance
        if (distToPlayer < 160f) {
            x += (facingRight ? -1f : 1f) * moveSpeed * delta;
        }

        summonTimer += delta;
        if (summonTimer >= summonInterval) {
            summonTimer = 0f;
            spawnMinion();
        }

        updateMinions(delta, player);
    }

    private void spawnMinion() {
        if (spawnedMinions.size() < 4) {
            SporeMinion minion = new SporeMinion(x + (facingRight ? 24f : -24f), y);
            spawnedMinions.add(minion);
            System.out.println("🍄✨ [Spore Summoner] Summoned a toxic Spore Minion!");
        }
    }

    private void updateMinions(float delta, Player player) {
        for (int i = spawnedMinions.size() - 1; i >= 0; i--) {
            SporeMinion minion = spawnedMinions.get(i);
            minion.update(delta, player);
            if (!minion.isAlive()) {
                spawnedMinions.remove(i);
            }
        }
    }

    public List<SporeMinion> getSpawnedMinions() {
        return spawnedMinions;
    }
}
