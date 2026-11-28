package com.Blasphemeows.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Disposable;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Reusable animated UI banner for gameplay notifications such as checkpoint activations,
 * ability unlocks, and item pickups. Renders in UI camera space without blocking combat.
 */
public class NotificationPopup implements Disposable {
    private static final float FADE_IN_DURATION = 0.28f;
    private static final float HOLD_DURATION = 2.40f;
    private static final float FADE_OUT_DURATION = 0.45f;
    private static final float TOTAL_DURATION = FADE_IN_DURATION + HOLD_DURATION + FADE_OUT_DURATION;

    public static final class NotificationItem {
        final String title;
        final String subtitle;
        final Color accentColor;

        public NotificationItem(String title, String subtitle, Color accentColor) {
            this.title = title != null ? title : "";
            this.subtitle = subtitle != null ? subtitle : "";
            this.accentColor = accentColor != null ? new Color(accentColor) : new Color(Color.GOLD);
        }
    }

    private final Queue<NotificationItem> queue = new ArrayDeque<NotificationItem>();
    private final GlyphLayout layout = new GlyphLayout();
    private NotificationItem activeItem;
    private float elapsed = 0f;

    public void show(String title, String subtitle, Color accentColor) {
        queue.offer(new NotificationItem(title, subtitle, accentColor));
        if (activeItem == null) {
            advanceQueue();
        }
    }

    public void showCheckpointActivated(String checkpointName) {
        show("CHECKPOINT ACTIVATED", checkpointName, new Color(0.95f, 0.72f, 0.22f, 1f));
    }

    public void showAbilityUnlocked(String abilityName) {
        show("ABILITY UNLOCKED", abilityName, new Color(0.30f, 0.85f, 0.95f, 1f));
    }

    public void showItemPickup(String itemName) {
        show("ITEM ACQUIRED", itemName, new Color(0.45f, 0.90f, 0.45f, 1f));
    }

    public void update(float delta) {
        if (activeItem == null) {
            advanceQueue();
            return;
        }
        elapsed += delta;
        if (elapsed >= TOTAL_DURATION) {
            advanceQueue();
        }
    }

    public boolean isShowing() {
        return activeItem != null;
    }

    public void clear() {
        queue.clear();
        activeItem = null;
        elapsed = 0f;
    }

    private void advanceQueue() {
        activeItem = queue.poll();
        elapsed = 0f;
    }

    public void render(ShapeRenderer shapeRenderer, SpriteBatch batch, BitmapFont titleFont, BitmapFont bodyFont, OrthographicCamera uiCamera) {
        if (activeItem == null) {
            return;
        }

        float alpha;
        float slideOffset;
        if (elapsed < FADE_IN_DURATION) {
            float t = Math.min(1f, elapsed / FADE_IN_DURATION);
            alpha = t;
            slideOffset = (1f - t) * 14f;
        } else if (elapsed < FADE_IN_DURATION + HOLD_DURATION) {
            alpha = 1f;
            slideOffset = 0f;
        } else {
            float t = Math.min(1f, (elapsed - FADE_IN_DURATION - HOLD_DURATION) / FADE_OUT_DURATION);
            alpha = 1f - t;
            slideOffset = -t * 8f;
        }

        float screenW = uiCamera.viewportWidth;
        float screenH = uiCamera.viewportHeight;

        layout.setText(titleFont, activeItem.title);
        float titleW = layout.width;
        layout.setText(bodyFont, activeItem.subtitle);
        float subW = layout.width;

        float boxW = Math.max(340f, Math.max(titleW, subW) + 84f);
        float boxH = activeItem.subtitle.isEmpty() ? 56f : 76f;
        float boxX = (screenW - boxW) * 0.5f;
        float boxY = screenH * 0.72f - slideOffset;

        Color accent = activeItem.accentColor;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.setProjectionMatrix(uiCamera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.05f, 0.05f, 0.08f, 0.86f * alpha);
        shapeRenderer.rect(boxX, boxY, boxW, boxH);

        // Top and bottom ornamental accent bars
        float ruleWidth = boxW * (elapsed < FADE_IN_DURATION ? (elapsed / FADE_IN_DURATION) : 1f);
        float ruleX = boxX + (boxW - ruleWidth) * 0.5f;
        shapeRenderer.setColor(accent.r, accent.g, accent.b, 0.92f * alpha);
        shapeRenderer.rect(ruleX, boxY + boxH - 3f, ruleWidth, 3f);
        shapeRenderer.rect(ruleX, boxY, ruleWidth, 2f);
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(accent.r, accent.g, accent.b, 0.45f * alpha);
        shapeRenderer.rect(boxX, boxY, boxW, boxH);
        shapeRenderer.end();

        batch.setProjectionMatrix(uiCamera.combined);
        batch.begin();
        Color oldTitleColor = new Color(titleFont.getColor());
        Color oldBodyColor = new Color(bodyFont.getColor());

        titleFont.setColor(accent.r, accent.g, accent.b, alpha);
        layout.setText(titleFont, activeItem.title);
        float titleY = activeItem.subtitle.isEmpty()
                ? boxY + (boxH + layout.height) * 0.5f
                : boxY + boxH - 16f;
        titleFont.draw(batch, activeItem.title, boxX + (boxW - layout.width) * 0.5f, titleY);

        if (!activeItem.subtitle.isEmpty()) {
            bodyFont.setColor(0.92f, 0.92f, 0.95f, alpha);
            layout.setText(bodyFont, activeItem.subtitle);
            bodyFont.draw(batch, activeItem.subtitle, boxX + (boxW - layout.width) * 0.5f, boxY + 28f);
        }

        titleFont.setColor(oldTitleColor);
        bodyFont.setColor(oldBodyColor);
        batch.end();
    }

    @Override
    public void dispose() {
        clear();
    }
}
