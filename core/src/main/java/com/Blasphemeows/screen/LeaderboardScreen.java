package com.Blasphemeows.screen;

import com.Blasphemeows.Main;
import com.Blasphemeows.net.ApiClient;
import com.Blasphemeows.net.AuthSession;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

/**
 * Displays live global leaderboard rankings (/api/leaderboard) and personal score
 * history (/api/scores/me) fetched asynchronously from the Spring Boot backend.
 */
public class LeaderboardScreen extends BaseMenuScreen {
    private final TextButton globalTabButton;
    private final TextButton myScoresTabButton;
    private final TextButton refreshButton;
    private final TextButton backButton;
    private final Label statusLabel;
    private final Table rowsTable;

    private boolean showingMyScores = false;

    public LeaderboardScreen(Main game) {
        super(game);

        Table panel = new Table(skin);
        panel.setBackground("window");
        panel.pad(24f);

        Label title = createTitleLabel("LEADERBOARD");
        Label subtitle = createSubtitleLabel("Account: " + AuthSession.getInstance().getDisplayIdentity());

        globalTabButton = new TextButton("Global Top 10", skin);
        myScoresTabButton = new TextButton("My Score History", skin);
        refreshButton = new TextButton("Refresh", skin);
        backButton = new TextButton("Back to Menu", skin);

        Table tabBar = new Table(skin);
        tabBar.add(globalTabButton).width(160f).height(32f).padRight(8f);
        tabBar.add(myScoresTabButton).width(160f).height(32f).padRight(8f);
        tabBar.add(refreshButton).width(110f).height(32f);

        rowsTable = new Table(skin);
        rowsTable.top().left();

        ScrollPane scrollPane = new ScrollPane(rowsTable, skin, "background");
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false);

        statusLabel = createStatusLabel();
        statusLabel.setAlignment(Align.center);

        panel.add(title).padBottom(4f).row();
        panel.add(subtitle).padBottom(12f).row();
        panel.add(tabBar).padBottom(12f).row();
        panel.add(scrollPane).width(520f).height(250f).padBottom(10f).row();
        panel.add(statusLabel).width(520f).minHeight(28f).padBottom(10f).row();
        panel.add(backButton).width(220f).height(36f).row();

        rootTable.add(panel).center();

        globalTabButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                showingMyScores = false;
                loadScores();
            }
        });

        myScoresTabButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                showingMyScores = true;
                loadScores();
            }
        });

        refreshButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                loadScores();
            }
        });

        backButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.showMainMenuScreen(null);
            }
        });
    }

    @Override
    public void show() {
        super.show();
        loadScores();
    }

    private void loadScores() {
        rowsTable.clearChildren();
        globalTabButton.setColor(showingMyScores ? Color.LIGHT_GRAY : Color.GOLD);
        myScoresTabButton.setColor(showingMyScores ? Color.GOLD : Color.LIGHT_GRAY);

        if (showingMyScores && !AuthSession.getInstance().isLoggedIn()) {
            setStatus(statusLabel, "Sign in to view your personal score history (/api/scores/me).", Color.GOLD);
            Label guestInfo = new Label("You are currently playing in Guest Mode.", skin, "white");
            guestInfo.setAlignment(Align.center);
            rowsTable.add(guestInfo).width(500f).padTop(40f).center();
            return;
        }

        refreshButton.setDisabled(true);
        setStatus(statusLabel, showingMyScores ? "Loading your scores..." : "Loading global leaderboard...", Color.SKY);

        ApiClient.ApiCallback callback = new ApiClient.ApiCallback() {
            @Override
            public void onComplete(boolean success, String responseBody, String errorMessage) {
                refreshButton.setDisabled(false);
                if (!success) {
                    setStatus(statusLabel, "Failed to load scores: " + (errorMessage != null ? errorMessage : "Network error"), Color.CORAL);
                    Label errLabel = new Label("Could not reach leaderboard service. Check connection and try Refresh.", skin, "white");
                    errLabel.setWrap(true);
                    errLabel.setAlignment(Align.center);
                    rowsTable.add(errLabel).width(480f).padTop(40f).center();
                    return;
                }
                populateRows(responseBody);
            }
        };

        if (showingMyScores) {
            ApiClient.getInstance().myScores(callback);
        } else {
            ApiClient.getInstance().leaderboard(callback);
        }
    }

    private void populateRows(String jsonBody) {
        rowsTable.clearChildren();
        try {
            JsonValue root = new JsonReader().parse(jsonBody);
            if (root == null || root.size == 0) {
                setStatus(statusLabel, "No scores recorded yet.", Color.LIGHT_GRAY);
                Label empty = new Label("No runs submitted yet. Defeat enemies and complete a run!", skin, "white");
                empty.setWrap(true);
                empty.setAlignment(Align.center);
                rowsTable.add(empty).width(480f).padTop(40f).center();
                return;
            }

            // Column headers
            Label rankHdr = new Label("#", skin, "white");
            rankHdr.setColor(Color.GOLD);
            Label userHdr = new Label("Player", skin, "white");
            userHdr.setColor(Color.GOLD);
            Label scoreHdr = new Label("Score", skin, "white");
            scoreHdr.setColor(Color.GOLD);
            Label dateHdr = new Label("Date (UTC)", skin, "white");
            dateHdr.setColor(Color.GOLD);

            rowsTable.add(rankHdr).width(40f).left().pad(4f);
            rowsTable.add(userHdr).width(170f).left().pad(4f);
            rowsTable.add(scoreHdr).width(110f).right().pad(4f);
            rowsTable.add(dateHdr).width(160f).right().pad(4f).row();

            int rank = 1;
            String currentUser = AuthSession.getInstance().getUsername();
            for (JsonValue entry = root.child; entry != null; entry = entry.next) {
                String username = entry.getString("username", "unknown");
                long score = entry.getLong("score", 0L);
                String createdAt = formatTimestamp(entry.getString("createdAt", ""));

                boolean isSelf = currentUser != null && currentUser.equals(username);
                Color rowColor = isSelf ? Color.LIME : Color.WHITE;

                Label rankLbl = new Label(String.valueOf(rank), skin, "white");
                rankLbl.setColor(rank <= 3 ? Color.GOLD : rowColor);
                Label userLbl = new Label(username, skin, "white");
                userLbl.setColor(rowColor);
                Label scoreLbl = new Label(String.valueOf(score), skin, "white");
                scoreLbl.setColor(rowColor);
                Label dateLbl = new Label(createdAt, skin, "white");
                dateLbl.setColor(Color.LIGHT_GRAY);

                rowsTable.add(rankLbl).width(40f).left().pad(4f);
                rowsTable.add(userLbl).width(170f).left().pad(4f);
                rowsTable.add(scoreLbl).width(110f).right().pad(4f);
                rowsTable.add(dateLbl).width(160f).right().pad(4f).row();
                rank++;
            }

            setStatus(statusLabel, "Loaded " + (rank - 1) + " entries.", Color.LIME);
        } catch (Exception e) {
            setStatus(statusLabel, "Failed to parse leaderboard response.", Color.CORAL);
        }
    }

    private static String formatTimestamp(String iso) {
        if (iso == null || iso.isEmpty()) return "-";
        String cleaned = iso.replace('T', ' ');
        int dot = cleaned.indexOf('.');
        if (dot > 0) {
            cleaned = cleaned.substring(0, dot);
        } else if (cleaned.endsWith("Z")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }
        return cleaned.length() > 16 ? cleaned.substring(0, 16) : cleaned;
    }
}
