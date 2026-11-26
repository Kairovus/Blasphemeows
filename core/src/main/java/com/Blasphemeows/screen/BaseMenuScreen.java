package com.Blasphemeows.screen;

import com.Blasphemeows.Main;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

/**
 * Shared base for Scene2D menu/authentication screens.
 * Uses the game's shared SpriteBatch and Skin without disposing shared resources.
 */
public abstract class BaseMenuScreen extends ScreenAdapter {
    protected final Main game;
    protected final Skin skin;
    protected final Stage stage;
    protected final Table rootTable;

    protected BaseMenuScreen(Main game) {
        this.game = game;
        this.skin = game.getSkin();
        this.stage = new Stage(new ScreenViewport(), game.getBatch());
        this.rootTable = new Table(skin);
        this.rootTable.setFillParent(true);
        this.rootTable.pad(24f);
        this.stage.addActor(this.rootTable);
    }

    protected Label createTitleLabel(String text) {
        Label.LabelStyle style = new Label.LabelStyle(skin.getFont("window"), new Color(0.95f, 0.78f, 0.30f, 1f));
        return new Label(text, style);
    }

    protected Label createSubtitleLabel(String text) {
        Label.LabelStyle style = new Label.LabelStyle(skin.getFont("subtitle"), new Color(0.82f, 0.84f, 0.88f, 1f));
        return new Label(text, style);
    }

    protected Label createStatusLabel() {
        Label label = new Label("", skin, "white");
        label.setWrap(true);
        return label;
    }

    protected void setStatus(Label label, String message, Color color) {
        if (label == null) return;
        label.setText(message != null ? message : "");
        label.setColor(color != null ? color : Color.WHITE);
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0.07f, 0.08f, 0.12f, 1f);
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        if (width <= 0 || height <= 0) return;
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void hide() {
        if (Gdx.input.getInputProcessor() == stage) {
            Gdx.input.setInputProcessor(null);
        }
    }

    @Override
    public void dispose() {
        stage.dispose();
    }
}
