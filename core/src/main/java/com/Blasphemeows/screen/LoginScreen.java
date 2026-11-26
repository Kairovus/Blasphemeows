package com.Blasphemeows.screen;

import com.Blasphemeows.Main;
import com.Blasphemeows.net.ApiClient;
import com.Blasphemeows.net.AuthSession;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;

/**
 * Login screen supporting username/password authentication, registration navigation,
 * and offline guest play.
 */
public class LoginScreen extends BaseMenuScreen {
    private final TextField usernameField;
    private final TextField passwordField;
    private final TextButton loginButton;
    private final TextButton registerButton;
    private final TextButton guestButton;
    private final Label statusLabel;

    public LoginScreen(Main game, String initialStatus) {
        super(game);

        Table panel = new Table(skin);
        panel.setBackground("window");
        panel.pad(28f);

        Label title = createTitleLabel("BLASPHEMEOWS");
        Label subtitle = createSubtitleLabel("Sanctuary Sign In");

        usernameField = new TextField("", skin);
        usernameField.setMessageText("Username (3-50 characters)");

        passwordField = new TextField("", skin);
        passwordField.setPasswordMode(true);
        passwordField.setPasswordCharacter('*');
        passwordField.setMessageText("Password (min 6 characters)");

        loginButton = new TextButton("Sign In", skin);
        registerButton = new TextButton("Create Account", skin);
        guestButton = new TextButton("Continue as Guest", skin);

        statusLabel = createStatusLabel();
        statusLabel.setAlignment(Align.center);
        if (initialStatus != null && !initialStatus.isEmpty()) {
            setStatus(statusLabel, initialStatus, Color.GOLD);
        }

        Label serverLabel = new Label("API: " + ApiClient.getInstance().getBaseUrl(), skin, "default");
        serverLabel.setColor(Color.LIGHT_GRAY);

        panel.add(title).padBottom(4f).row();
        panel.add(subtitle).padBottom(20f).row();

        panel.add(new Label("Username", skin, "white")).left().padBottom(4f).row();
        panel.add(usernameField).width(320f).height(34f).padBottom(12f).row();

        panel.add(new Label("Password", skin, "white")).left().padBottom(4f).row();
        panel.add(passwordField).width(320f).height(34f).padBottom(16f).row();

        panel.add(loginButton).width(320f).height(38f).padBottom(8f).row();
        panel.add(registerButton).width(320f).height(34f).padBottom(8f).row();
        panel.add(guestButton).width(320f).height(34f).padBottom(12f).row();
        panel.add(statusLabel).width(320f).minHeight(36f).padBottom(6f).row();
        panel.add(serverLabel).padTop(4f).row();

        rootTable.add(panel).center();

        wireListeners();
    }

    private void wireListeners() {
        loginButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                attemptLogin();
            }
        });

        registerButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.showRegisterScreen();
            }
        });

        guestButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                AuthSession.getInstance().enterGuestMode();
                game.showMainMenuScreen("Playing in Guest Mode (local progression saved; online scores disabled).");
            }
        });

        passwordField.addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (keycode == Input.Keys.ENTER) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });
    }

    private void attemptLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.length() < 3 || username.length() > 50) {
            setStatus(statusLabel, "Username must be between 3 and 50 characters.", Color.CORAL);
            return;
        }
        if (password.length() < 6 || password.length() > 100) {
            setStatus(statusLabel, "Password must be at least 6 characters.", Color.CORAL);
            return;
        }

        setBusy(true);
        setStatus(statusLabel, "Signing in...", Color.SKY);

        ApiClient.getInstance().login(username, password, new ApiClient.ApiCallback() {
            @Override
            public void onComplete(boolean success, String responseBody, String errorMessage) {
                setBusy(false);
                if (success) {
                    passwordField.setText("");
                    game.showMainMenuScreen("Signed in as " + AuthSession.getInstance().getUsername() + ".");
                } else {
                    setStatus(statusLabel, errorMessage != null ? errorMessage : "Sign in failed.", Color.CORAL);
                }
            }
        });
    }

    private void setBusy(boolean busy) {
        loginButton.setDisabled(busy);
        registerButton.setDisabled(busy);
        guestButton.setDisabled(busy);
        usernameField.setDisabled(busy);
        passwordField.setDisabled(busy);
    }

    @Override
    public void show() {
        super.show();
        stage.setKeyboardFocus(usernameField);
    }
}
