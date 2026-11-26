package com.Blasphemeows.net;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

/**
 * Manages JWT authentication and guest state in memory and libGDX Preferences.
 * Persists only the JWT token and username (never passwords), kept separate from game saves.
 */
public final class AuthSession {
    private static final AuthSession INSTANCE = new AuthSession();
    private static final String PREFERENCES_NAME = "com.blasphemeows.auth.session";
    private static final String KEY_TOKEN = "jwtToken";
    private static final String KEY_USERNAME = "username";

    private String token;
    private String username;
    private boolean guestMode = false;

    private AuthSession() {}

    public static AuthSession getInstance() {
        return INSTANCE;
    }

    /** Restores any previously persisted JWT token and username from Preferences on startup. */
    public synchronized boolean restore() {
        if (Gdx.app == null) {
            return false;
        }
        Preferences prefs = Gdx.app.getPreferences(PREFERENCES_NAME);
        String savedToken = prefs.getString(KEY_TOKEN, null);
        String savedUsername = prefs.getString(KEY_USERNAME, null);
        if (savedToken != null && !savedToken.trim().isEmpty()
                && savedUsername != null && !savedUsername.trim().isEmpty()) {
            this.token = savedToken.trim();
            this.username = savedUsername.trim();
            this.guestMode = false;
            return true;
        }
        this.token = null;
        this.username = null;
        return false;
    }

    /** Stores an authenticated JWT session in memory and persists it to Preferences. */
    public synchronized void set(String token, String username) {
        this.token = token;
        this.username = username;
        this.guestMode = false;
        persist();
    }

    /** Activates offline/guest play without altering local game saves. */
    public synchronized void enterGuestMode() {
        this.token = null;
        this.username = null;
        this.guestMode = true;
        clearPersisted();
    }

    /** Clears both in-memory and persisted auth session (never touches game saves). */
    public synchronized void clear() {
        this.token = null;
        this.username = null;
        this.guestMode = false;
        clearPersisted();
    }

    public synchronized boolean isLoggedIn() {
        return token != null && !token.isEmpty();
    }

    public synchronized boolean isGuest() {
        return !isLoggedIn() && guestMode;
    }

    public synchronized String getToken() {
        return token;
    }

    public synchronized String getUsername() {
        return username;
    }

    public synchronized String getDisplayIdentity() {
        if (isLoggedIn()) {
            return username;
        }
        return "Guest";
    }

    private void persist() {
        if (Gdx.app == null) {
            return;
        }
        Preferences prefs = Gdx.app.getPreferences(PREFERENCES_NAME);
        if (token != null && !token.isEmpty()) {
            prefs.putString(KEY_TOKEN, token);
            prefs.putString(KEY_USERNAME, username != null ? username : "");
        } else {
            prefs.remove(KEY_TOKEN);
            prefs.remove(KEY_USERNAME);
        }
        prefs.flush();
    }

    private void clearPersisted() {
        if (Gdx.app == null) {
            return;
        }
        Preferences prefs = Gdx.app.getPreferences(PREFERENCES_NAME);
        prefs.remove(KEY_TOKEN);
        prefs.remove(KEY_USERNAME);
        prefs.flush();
    }
}
