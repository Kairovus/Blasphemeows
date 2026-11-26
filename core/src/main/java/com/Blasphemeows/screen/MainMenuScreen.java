package com.Blasphemeows.screen;

import com.Blasphemeows.Main;
import com.Blasphemeows.core.SaveData;
import com.Blasphemeows.core.SaveManager;
import com.Blasphemeows.net.ApiClient;
import com.Blasphemeows.net.AuthSession;
import com.Blasphemeows.score.ScoreTracker;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;

/**
 * Main Menu providing Continue, New Game (with overwrite confirmation),
 * Leaderboard, Account/Profile overview, and Logout/Sign-In navigation.
 */
public class MainMenuScreen extends BaseMenuScreen {
    private final Label identityLabel;
    private final Label statusLabel;
    private final Label pendingScoreLabel;
    private final TextButton continueButton;
    private final TextButton newGameButton;
    private final TextButton leaderboardButton;
    private final TextButton profileButton;
    private final TextButton authActionButton;
    private final TextButton submitPendingButton;

    public MainMenuScreen(Main game, String initialStatus) {
        super(game);

        Table panel = new Table(skin);
        panel.setBackground("window");
        panel.pad(28f);

        Label title = createTitleLabel("BLASPHEMEOWS");
        Label subtitle = createSubtitleLabel("Sanctuary Main Menu");

        identityLabel = new Label("", skin, "white");
        identityLabel.setAlignment(Align.center);

        pendingScoreLabel = new Label("", skin, "default");
        pendingScoreLabel.setAlignment(Align.center);

        submitPendingButton = new TextButton("Submit Pending Score", skin);

        continueButton = new TextButton("Continue", skin);
        newGameButton = new TextButton("New Game", skin);
        leaderboardButton = new TextButton("Leaderboard", skin);
        profileButton = new TextButton("Account / Profile", skin);
        authActionButton = new TextButton("Logout", skin);
        TextButton exitButton = new TextButton("Quit", skin);

        statusLabel = createStatusLabel();
        statusLabel.setAlignment(Align.center);
        if (initialStatus != null && !initialStatus.isEmpty()) {
            setStatus(statusLabel, initialStatus, Color.GOLD);
        }

        panel.add(title).padBottom(4f).row();
        panel.add(subtitle).padBottom(10f).row();
        panel.add(identityLabel).padBottom(8f).row();
        panel.add(pendingScoreLabel).padBottom(6f).row();
        panel.add(submitPendingButton).width(280f).height(30f).padBottom(12f).row();

        panel.add(continueButton).width(280f).height(38f).padBottom(8f).row();
        panel.add(newGameButton).width(280f).height(38f).padBottom(8f).row();
        panel.add(leaderboardButton).width(280f).height(36f).padBottom(8f).row();
        panel.add(profileButton).width(280f).height(36f).padBottom(8f).row();
        panel.add(authActionButton).width(280f).height(36f).padBottom(8f).row();
        panel.add(exitButton).width(280f).height(32f).padBottom(10f).row();
        panel.add(statusLabel).width(320f).minHeight(34f).row();

        rootTable.add(panel).center();

        continueButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (!continueButton.isDisabled()) {
                    game.continueGame();
                }
            }
        });

        newGameButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                handleNewGameClicked();
            }
        });

        leaderboardButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.showLeaderboardScreen();
            }
        });

        profileButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                showProfileDialog();
            }
        });

        authActionButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (AuthSession.getInstance().isLoggedIn()) {
                    AuthSession.getInstance().clear();
                    game.showLoginScreen("Logged out. Local save progression remains intact.");
                } else {
                    game.showLoginScreen(null);
                }
            }
        });

        submitPendingButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                submitPendingScore();
            }
        });

        exitButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Gdx.app.exit();
            }
        });

        refreshMenuState();
    }

    public void setStatusMessage(String message, Color color) {
        setStatus(statusLabel, message, color);
        refreshMenuState();
    }

    public void refreshMenuState() {
        AuthSession session = AuthSession.getInstance();
        if (session.isLoggedIn()) {
            identityLabel.setText("Account: " + session.getUsername() + " (Authenticated)");
            identityLabel.setColor(Color.LIME);
            authActionButton.setText("Logout");
        } else {
            identityLabel.setText("Account: Guest Mode (Offline)");
            identityLabel.setColor(Color.LIGHT_GRAY);
            authActionButton.setText("Sign In / Register");
        }

        boolean hasSave = SaveManager.getInstance().hasSaveFile();
        continueButton.setDisabled(!hasSave);
        continueButton.setText(hasSave ? "Continue" : "Continue (No Save)");

        long unsubmitted = ScoreTracker.getInstance().getTotalUnsubmittedScore();
        if (unsubmitted > 0L) {
            pendingScoreLabel.setText("Unsubmitted Score: " + unsubmitted);
            pendingScoreLabel.setColor(Color.GOLD);
            pendingScoreLabel.setVisible(true);
            submitPendingButton.setVisible(session.isLoggedIn());
            submitPendingButton.setDisabled(ScoreTracker.getInstance().isSubmissionInFlight());
        } else {
            pendingScoreLabel.setText("");
            pendingScoreLabel.setVisible(false);
            submitPendingButton.setVisible(false);
        }
    }

    private void handleNewGameClicked() {
        if (!SaveManager.getInstance().hasSaveFile()) {
            game.startNewGame();
            return;
        }

        Dialog confirmDialog = new Dialog("Overwrite Existing Save?", skin, "dialog") {
            @Override
            protected void result(Object object) {
                if (Boolean.TRUE.equals(object)) {
                    game.startNewGame();
                }
            }
        };
        confirmDialog.getContentTable().pad(16f);
        Label prompt = new Label("Starting a new game will erase your saved checkpoint and progression.\nProceed?", skin, "white");
        prompt.setAlignment(Align.center);
        confirmDialog.text(prompt);
        confirmDialog.button("Start New Game", Boolean.TRUE);
        confirmDialog.button("Cancel", Boolean.FALSE);
        confirmDialog.show(stage);
    }

    private void showProfileDialog() {
        AuthSession session = AuthSession.getInstance();
        SaveManager saveManager = SaveManager.getInstance();
        boolean hasSave = saveManager.hasSaveFile();
        SaveData save = saveManager.getCurrentSaveData();

        StringBuilder info = new StringBuilder();
        if (session.isLoggedIn()) {
            info.append("Username: ").append(session.getUsername()).append("\n");
            info.append("Status: Authenticated (JWT active)\n");
        } else {
            info.append("Username: Guest\n");
            info.append("Status: Offline Guest Mode\n");
        }

        info.append("Unsubmitted Score: ").append(ScoreTracker.getInstance().getTotalUnsubmittedScore()).append("\n");
        if (hasSave && save != null) {
            info.append("Local Save: Level ").append(save.level)
                .append(" | Checkpoint: ").append(save.lastCheckpointRegion);
        } else {
            info.append("Local Save: None");
        }

        Dialog profileDialog = new Dialog("Account & Profile", skin, "dialog") {
            @Override
            protected void result(Object object) {
                if ("leaderboard".equals(object)) {
                    game.showLeaderboardScreen();
                } else if ("login".equals(object)) {
                    game.showLoginScreen(null);
                }
            }
        };
        profileDialog.getContentTable().pad(16f);
        Label body = new Label(info.toString(), skin, "white");
        body.setAlignment(Align.left);
        profileDialog.text(body);
        if (session.isLoggedIn()) {
            profileDialog.button("My Scores", "leaderboard");
        } else {
            profileDialog.button("Sign In", "login");
        }
        profileDialog.button("Close", "close");
        profileDialog.show(stage);
    }

    private void submitPendingScore() {
        final long scoreToSubmit = ScoreTracker.getInstance().beginSubmission();
        if (scoreToSubmit <= 0L) {
            return;
        }
        submitPendingButton.setDisabled(true);
        setStatus(statusLabel, "Submitting score " + scoreToSubmit + "...", Color.SKY);
        ApiClient.getInstance().submitScore(scoreToSubmit, new ApiClient.ApiCallback() {
            @Override
            public void onComplete(boolean success, String responseBody, String errorMessage) {
                if (success) {
                    ScoreTracker.getInstance().confirmSubmissionSuccess(scoreToSubmit);
                    setStatus(statusLabel, "Submitted score " + scoreToSubmit + " to leaderboard!", Color.LIME);
                } else {
                    ScoreTracker.getInstance().markSubmissionFailed();
                    setStatus(statusLabel, "Score preserved locally (upload failed: " + errorMessage + ")", Color.CORAL);
                }
                refreshMenuState();
            }
        });
    }

    @Override
    public void show() {
        super.show();
        refreshMenuState();
    }
}
