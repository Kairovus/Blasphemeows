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
 * Account registration screen validating inputs against backend constraints
 * (username 3-50 chars, password 6-100 chars, matching confirmation).
 */
public class RegisterScreen extends BaseMenuScreen {
    private final TextField usernameField;
    private final TextField passwordField;
    private final TextField confirmPasswordField;
    private final TextButton registerButton;
    private final TextButton backButton;
    private final Label statusLabel;

    public RegisterScreen(Main game) {
        super(game);

        Table panel = new Table(skin);
        panel.setBackground("window");
        panel.pad(28f);

        Label title = createTitleLabel("BLASPHEMEOWS");
        Label subtitle = createSubtitleLabel("Create Sanctuary Account");

        usernameField = new TextField("", skin);
        usernameField.setMessageText("Username (3-50 characters)");

        passwordField = new TextField("", skin);
        passwordField.setPasswordMode(true);
        passwordField.setPasswordCharacter('*');
        passwordField.setMessageText("Password (6-100 characters)");

        confirmPasswordField = new TextField("", skin);
        confirmPasswordField.setPasswordMode(true);
        confirmPasswordField.setPasswordCharacter('*');
        confirmPasswordField.setMessageText("Confirm Password");

        registerButton = new TextButton("Register", skin);
        backButton = new TextButton("Back to Sign In", skin);

        statusLabel = createStatusLabel();
        statusLabel.setAlignment(Align.center);

        panel.add(title).padBottom(4f).row();
        panel.add(subtitle).padBottom(20f).row();

        panel.add(new Label("Username", skin, "white")).left().padBottom(4f).row();
        panel.add(usernameField).width(320f).height(34f).padBottom(12f).row();

        panel.add(new Label("Password", skin, "white")).left().padBottom(4f).row();
        panel.add(passwordField).width(320f).height(34f).padBottom(12f).row();

        panel.add(new Label("Confirm Password", skin, "white")).left().padBottom(4f).row();
        panel.add(confirmPasswordField).width(320f).height(34f).padBottom(16f).row();

        panel.add(registerButton).width(320f).height(38f).padBottom(8f).row();
        panel.add(backButton).width(320f).height(34f).padBottom(12f).row();
        panel.add(statusLabel).width(320f).minHeight(36f).row();

        rootTable.add(panel).center();

        wireListeners();
    }

    private void wireListeners() {
        registerButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                attemptRegister();
            }
        });

        backButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.showLoginScreen(null);
            }
        });

        confirmPasswordField.addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (keycode == Input.Keys.ENTER) {
                    attemptRegister();
                    return true;
                }
                return false;
            }
        });
    }

    private void attemptRegister() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String confirm = confirmPasswordField.getText();

        if (username.isEmpty() || username.length() < 3 || username.length() > 50) {
            setStatus(statusLabel, "Username must be 3 to 50 non-blank characters.", Color.CORAL);
            return;
        }
        if (password.trim().isEmpty() || password.length() < 6 || password.length() > 100) {
            setStatus(statusLabel, "Password must be 6 to 100 characters.", Color.CORAL);
            return;
        }
        if (!password.equals(confirm)) {
            setStatus(statusLabel, "Passwords do not match.", Color.CORAL);
            return;
        }

        setBusy(true);
        setStatus(statusLabel, "Creating account...", Color.SKY);

        ApiClient.getInstance().register(username, password, new ApiClient.ApiCallback() {
            @Override
            public void onComplete(boolean success, String responseBody, String errorMessage) {
                setBusy(false);
                if (success) {
                    passwordField.setText("");
                    confirmPasswordField.setText("");
                    game.showMainMenuScreen("Account created! Signed in as " + AuthSession.getInstance().getUsername() + ".");
                } else {
                    setStatus(statusLabel, errorMessage != null ? errorMessage : "Registration failed.", Color.CORAL);
                }
            }
        });
    }

    private void setBusy(boolean busy) {
        registerButton.setDisabled(busy);
        backButton.setDisabled(busy);
        usernameField.setDisabled(busy);
        passwordField.setDisabled(busy);
        confirmPasswordField.setDisabled(busy);
    }

    @Override
    public void show() {
        super.show();
        stage.setKeyboardFocus(usernameField);
    }
}
