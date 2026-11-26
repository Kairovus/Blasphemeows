package com.Blasphemeows.net;

/**
 * In-memory JWT for the current game session.
 */
public final class AuthSession {
    private static final AuthSession INSTANCE = new AuthSession();

    private String token;
    private String username;

    private AuthSession() {}

    public static AuthSession getInstance() {
        return INSTANCE;
    }

    public void set(String token, String username) {
        this.token = token;
        this.username = username;
    }

    public void clear() {
        token = null;
        username = null;
    }

    public boolean isLoggedIn() {
        return token != null && !token.isEmpty();
    }

    public String getToken() {
        return token;
    }

    public String getUsername() {
        return username;
    }
}
