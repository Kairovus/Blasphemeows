package com.Blasphemeows;

import com.Blasphemeows.core.GameManager;
import com.Blasphemeows.core.SaveManager;
import com.Blasphemeows.net.ApiClient;
import com.Blasphemeows.net.AuthSession;
import com.Blasphemeows.score.ScoreTracker;
import com.Blasphemeows.screen.GameScreen;
import com.Blasphemeows.screen.LeaderboardScreen;
import com.Blasphemeows.screen.LoginScreen;
import com.Blasphemeows.screen.MainMenuScreen;
import com.Blasphemeows.screen.RegisterScreen;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

/**
 * Root libGDX Game coordinator managing shared rendering resources,
 * startup session restoration/validation, and screen transitions.
 */
public class Main extends Game {
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private BitmapFont font;
    private Skin skin;

    @Override
    public void create() {
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        font = new BitmapFont();
        skin = new Skin(Gdx.files.internal("ui/uiskin.json"));

        boolean hasSavedSession = AuthSession.getInstance().restore();
        if (hasSavedSession) {
            final MainMenuScreen menu = new MainMenuScreen(this, "Validating saved session...");
            switchScreen(menu);
            ApiClient.getInstance().validateSession(new ApiClient.SessionValidationCallback() {
                @Override
                public void onValidated(boolean valid, boolean networkError, String message) {
                    if (getScreen() != menu) {
                        return;
                    }
                    if (valid) {
                        menu.setStatusMessage("Session verified for " + AuthSession.getInstance().getUsername() + ".", Color.LIME);
                    } else if (!networkError) {
                        showLoginScreen(message != null ? message : "Session expired. Please sign in again.");
                    } else {
                        menu.setStatusMessage("Server unreachable; offline mode active (" + message + ").", Color.GOLD);
                    }
                }
            });
        } else {
            showLoginScreen(null);
        }
    }

    private void switchScreen(Screen nextScreen) {
        Screen previous = getScreen();
        setScreen(nextScreen);
        if (previous != null && previous != nextScreen) {
            previous.dispose();
        }
    }

    public void showLoginScreen(String statusMessage) {
        switchScreen(new LoginScreen(this, statusMessage));
    }

    public void showRegisterScreen() {
        switchScreen(new RegisterScreen(this));
    }

    public void showMainMenuScreen(String statusMessage) {
        switchScreen(new MainMenuScreen(this, statusMessage));
    }

    public void showLeaderboardScreen() {
        switchScreen(new LeaderboardScreen(this));
    }

    public void continueGame() {
        switchScreen(new GameScreen(this, true));
    }

    public void startNewGame() {
        GameManager.getInstance().resetProgress();
        SaveManager.getInstance().clearSave();
        ScoreTracker.getInstance().reset();
        switchScreen(new GameScreen(this, false));
    }

    public SpriteBatch getBatch() {
        return batch;
    }

    public ShapeRenderer getShapeRenderer() {
        return shapeRenderer;
    }

    public BitmapFont getFont() {
        return font;
    }

    public Skin getSkin() {
        return skin;
    }

    @Override
    public void dispose() {
        Screen current = getScreen();
        setScreen(null);
        if (current != null) {
            current.dispose();
        }
        if (skin != null) {
            skin.dispose();
            skin = null;
        }
        if (font != null) {
            font.dispose();
            font = null;
        }
        if (shapeRenderer != null) {
            shapeRenderer.dispose();
            shapeRenderer = null;
        }
        if (batch != null) {
            batch.dispose();
            batch = null;
        }
    }
}
