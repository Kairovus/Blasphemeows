package com.Blasphemeows.player;

import com.Blasphemeows.combat.AttackData;
import com.Blasphemeows.state.AttackState;
import com.Blasphemeows.state.DashState;
import com.Blasphemeows.state.PlayerState;
import com.Blasphemeows.state.RootedState;
import com.Blasphemeows.state.SeismicSlamState;
import com.Blasphemeows.state.StunnedState;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;

import java.util.HashMap;
import java.util.Map;

/**
 * PlayerAnimator: draws the player sprite based on the player's CURRENT STATE.
 *
 * It only READS the player (position, facing, current state), so no game logic
 * changes. Gameplay (Player, states, combos, hitboxes) stays exactly as it was.
 *
 * Assets (put in assets/player/):
 *  - cat_idle.png   : 6 x 2 cells of 256 x 272 px (12 frames), idle loop
 *  - cat_attack.png : 5 x 5 cells of 320 x 320 px (25 frames), see ATTACK_CLIPS
 *
 * Both sheets are drawn facing RIGHT and flipped when the player faces left.
 */
public class PlayerAnimator implements Disposable {

    // ---- Sheet layout ------------------------------------------------------
    private static final String IDLE_PATH = "player/cat_idle.png";
    private static final int IDLE_COLS = 6, IDLE_ROWS = 2, IDLE_W = 256, IDLE_H = 272;
    private static final float IDLE_FEET_GAP = 26f;    // px between feet and cell bottom

    private static final String ATTACK_PATH = "player/cat_attack.png";
    private static final int ATTACK_COLS = 5, ATTACK_ROWS = 5, ATTACK_W = 320, ATTACK_H = 320;
    private static final float ATTACK_FEET_GAP = 33f;

    // ---- On-screen size ----------------------------------------------------
    // The two sheets were drawn at different sizes, so each has its own scale
    // to make the cat the same height (~64 px = Player.height) in both.
    private static final float IDLE_SCALE = 0.36f;
    private static final float ATTACK_SCALE = 0.85f;

    private static final float IDLE_FRAME_TIME = 0.10f;

    /**
     * Which frames of cat_attack.png each attack uses (DRAFT - adjust freely).
     * Frame numbers = row * 5 + column (0..24). The clip is stretched over the
     * attack's real duration (startup + active + recovery), so animation and
     * hitbox stay in sync automatically.
     */
    private static final Map<String, int[]> ATTACK_CLIPS = new HashMap<String, int[]>();
    static {
        ATTACK_CLIPS.put("L1",  new int[]{1, 2, 6, 9});          // Quick Slash
        ATTACK_CLIPS.put("L2",  new int[]{8, 7, 6, 9});          // Cross Cut
        ATTACK_CLIPS.put("L3",  new int[]{3, 4, 5, 9});          // Spin Finisher
        ATTACK_CLIPS.put("H1",  new int[]{10, 11, 13, 14});      // Heavy Cleave
        ATTACK_CLIPS.put("H2",  new int[]{12, 13, 14, 15, 16});  // Ground Breaker
        ATTACK_CLIPS.put("LLH", new int[]{16, 17, 18, 19});      // Piercing Thrust
    }
    private static final int SLAM_FRAME = 15;                    // Seismic Slam (static for now)

    private final Texture idleTexture;
    private final Texture attackTexture;
    private final TextureRegion[] idleFrames;
    private final TextureRegion[] attackFrames;

    private PlayerState lastState;
    private float stateTime = 0f;

    public PlayerAnimator() {
        idleTexture = new Texture(Gdx.files.internal(IDLE_PATH));
        attackTexture = new Texture(Gdx.files.internal(ATTACK_PATH));
        // Linear because the sprites are scaled down; use Nearest for crisp pixel art at integer scales.
        idleTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        attackTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        idleFrames = slice(idleTexture, IDLE_COLS, IDLE_ROWS, IDLE_W, IDLE_H);
        attackFrames = slice(attackTexture, ATTACK_COLS, ATTACK_ROWS, ATTACK_W, ATTACK_H);
    }

    private static TextureRegion[] slice(Texture tex, int cols, int rows, int cw, int ch) {
        TextureRegion[] out = new TextureRegion[cols * rows];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                out[r * cols + c] = new TextureRegion(tex, c * cw, r * ch, cw, ch);
            }
        }
        return out;
    }

    /** Call once per frame (after player.update). */
    public void update(float delta, Player player) {
        PlayerState current = player.getStateMachine().getCurrentState();
        if (current != lastState) {   // states are re-created on every change, so identity works
            lastState = current;
            stateTime = 0f;
        }
        stateTime += delta;
    }

    /** Call between batch.begin() and batch.end(), with the WORLD camera projection. */
    public void draw(SpriteBatch batch, Player player) {
        PlayerState state = player.getStateMachine().getCurrentState();

        TextureRegion frame;
        float scale;
        float feetGap;

        if (state instanceof AttackState) {
            AttackState attack = (AttackState) state;
            int[] clip = resolveClip(attack.getAttackData());
            float total = attack.getTotalDuration();
            float progress = total > 0f ? Math.min(0.999f, attack.getElapsed() / total) : 0f;
            frame = attackFrames[clip[(int) (progress * clip.length)]];
            scale = ATTACK_SCALE;
            feetGap = ATTACK_FEET_GAP;
        } else if (state instanceof SeismicSlamState) {
            frame = attackFrames[SLAM_FRAME];
            scale = ATTACK_SCALE;
            feetGap = ATTACK_FEET_GAP;
        } else {
            // Idle / Move / Jump / Dash / Rooted / Stunned: idle loop as placeholder
            // until those sheets exist (see README note in the chat).
            int index = (int) (stateTime / IDLE_FRAME_TIME) % idleFrames.length;
            frame = idleFrames[index];
            scale = IDLE_SCALE;
            feetGap = IDLE_FEET_GAP;
        }

        float w = frame.getRegionWidth() * scale;
        float h = frame.getRegionHeight() * scale;
        float footX = player.getX() + player.getWidth() / 2f;   // sprite is centered on the hitbox
        float x = footX - w / 2f;
        float y = player.getY() - feetGap * scale;              // feet stand on the hitbox bottom

        applyTint(batch, player, state);
        if (player.isFacingRight()) {
            batch.draw(frame, x, y, w, h);
        } else {
            batch.draw(frame, x + w, y, -w, h);                 // negative width = mirrored
        }
        batch.setColor(Color.WHITE);
    }

    /** Replaces the old debug colors (cyan = i-frames, brown = rooted). */
    private void applyTint(SpriteBatch batch, Player player, PlayerState state) {
        if (player.isInvincible() || state instanceof DashState) {
            batch.setColor(1f, 1f, 1f, 0.55f);
        } else if (state instanceof RootedState) {
            batch.setColor(0.75f, 0.55f, 0.35f, 1f);
        } else if (state instanceof StunnedState) {
            batch.setColor(1f, 0.6f, 0.6f, 1f);
        } else {
            batch.setColor(Color.WHITE);
        }
    }

    /** Works for every weapon: names look like "Sword L1 (Quick Slash)", "Spear H2 (...)". */
    private static int[] resolveClip(AttackData data) {
        String name = data.getName();
        String key;
        if (name.contains(" LLH")) key = "LLH";
        else if (name.contains(" L1")) key = "L1";
        else if (name.contains(" L2")) key = "L2";
        else if (name.contains(" L3")) key = "L3";
        else if (name.contains(" H1")) key = "H1";
        else if (name.contains(" H2")) key = "H2";
        else key = data.isHeavy() ? "H1" : "L1";
        return ATTACK_CLIPS.get(key);
    }

    @Override
    public void dispose() {
        idleTexture.dispose();
        attackTexture.dispose();
    }
}
