package com.Blasphemeows.net;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.net.HttpRequestBuilder;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

/**
 * Async HTTP client for the Blasphemeows Spring Boot API.
 * All requests run off the render thread via Gdx.net.
 */
public final class ApiClient {
    private static final ApiClient INSTANCE = new ApiClient();

    /** Override with -Dblasphemeows.apiBaseUrl=https://your-host */
    private String baseUrl = System.getProperty("blasphemeows.apiBaseUrl", "http://localhost:8080");

    private final JsonReader jsonReader = new JsonReader();

    private ApiClient() {}

    public static ApiClient getInstance() {
        return INSTANCE;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void register(String username, String password, ApiCallback callback) {
        postAuth("/api/auth/register", username, password, callback);
    }

    public void login(String username, String password, ApiCallback callback) {
        postAuth("/api/auth/login", username, password, callback);
    }

    /**
     * Validates a restored JWT token against an authenticated endpoint (/api/scores/me).
     * Clears the session if the server rejects the token (401/403).
     * Network errors leave the session intact so offline/retry handling remains possible.
     */
    public void validateSession(SessionValidationCallback callback) {
        if (!AuthSession.getInstance().isLoggedIn()) {
            runOnGameThread(new Runnable() {
                @Override
                public void run() {
                    if (callback != null) {
                        callback.onValidated(false, false, "No saved session");
                    }
                }
            });
            return;
        }
        send("GET", "/api/scores/me", null, true, new RawResponseCallback() {
            @Override
            public void onComplete(boolean success, int statusCode, String responseBody, String errorMessage) {
                if (success) {
                    if (callback != null) {
                        callback.onValidated(true, false, null);
                    }
                } else if (statusCode == 401 || statusCode == 403) {
                    AuthSession.getInstance().clear();
                    if (callback != null) {
                        callback.onValidated(false, false, "Session expired. Please sign in again.");
                    }
                } else {
                    if (callback != null) {
                        callback.onValidated(false, true, errorMessage);
                    }
                }
            }
        });
    }

    public void submitScore(long score, ApiCallback callback) {
        if (!AuthSession.getInstance().isLoggedIn()) {
            failOnGameThread(callback, "Not logged in");
            return;
        }
        String body = "{\"score\":" + score + "}";
        send("POST", "/api/scores", body, true, wrapCallback(callback, true));
    }

    public void myScores(ApiCallback callback) {
        if (!AuthSession.getInstance().isLoggedIn()) {
            failOnGameThread(callback, "Not logged in");
            return;
        }
        send("GET", "/api/scores/me", null, true, wrapCallback(callback, true));
    }

    public void leaderboard(ApiCallback callback) {
        send("GET", "/api/leaderboard?limit=10", null, false, wrapCallback(callback, false));
    }

    private void postAuth(String path, final String username, String password, final ApiCallback callback) {
        String body = "{\"username\":\"" + escapeJson(username) + "\",\"password\":\"" + escapeJson(password) + "\"}";
        send("POST", path, body, false, new RawResponseCallback() {
            @Override
            public void onComplete(boolean ok, int statusCode, String response, String error) {
                if (ok) {
                    try {
                        JsonValue root = jsonReader.parse(response);
                        String token = root.getString("token", null);
                        String user = root.getString("username", username);
                        if (token != null && !token.isEmpty()) {
                            AuthSession.getInstance().set(token, user);
                        } else {
                            if (callback != null) {
                                callback.onComplete(false, response, "Missing auth token in server response");
                            }
                            return;
                        }
                    } catch (Exception e) {
                        if (callback != null) {
                            callback.onComplete(false, response, "Bad auth response");
                        }
                        return;
                    }
                }
                if (callback != null) {
                    callback.onComplete(ok, response, error);
                }
            }
        });
    }

    private RawResponseCallback wrapCallback(final ApiCallback callback, final boolean clearOnUnauthorized) {
        return new RawResponseCallback() {
            @Override
            public void onComplete(boolean success, int statusCode, String responseBody, String errorMessage) {
                if (!success && clearOnUnauthorized && (statusCode == 401 || statusCode == 403)) {
                    AuthSession.getInstance().clear();
                    errorMessage = "Session expired or invalid. Please log in again.";
                }
                if (callback != null) {
                    callback.onComplete(success, responseBody, errorMessage);
                }
            }
        };
    }

    private void send(String method, String path, String body, boolean auth, final RawResponseCallback callback) {
        HttpRequestBuilder builder = new HttpRequestBuilder();
        Net.HttpRequest request = builder
                .newRequest()
                .method(method)
                .url(baseUrl + path)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .timeout(10000)
                .build();

        if (body != null) {
            request.setContent(body);
        }
        if (auth) {
            request.setHeader("Authorization", "Bearer " + AuthSession.getInstance().getToken());
        }

        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                final int status = httpResponse.getStatus().getStatusCode();
                final String response = httpResponse.getResultAsString();
                final boolean ok = status >= 200 && status < 300;
                final String error = ok ? null : extractMessage(response, "HTTP " + status);
                runOnGameThread(new Runnable() {
                    @Override
                    public void run() {
                        if (callback != null) {
                            callback.onComplete(ok, status, response, error);
                        }
                    }
                });
            }

            @Override
            public void failed(Throwable t) {
                final String msg = (t != null && t.getMessage() != null && !t.getMessage().isEmpty())
                        ? t.getMessage()
                        : "Unable to reach server";
                runOnGameThread(new Runnable() {
                    @Override
                    public void run() {
                        if (callback != null) {
                            callback.onComplete(false, 0, null, msg);
                        }
                    }
                });
            }

            @Override
            public void cancelled() {
                runOnGameThread(new Runnable() {
                    @Override
                    public void run() {
                        if (callback != null) {
                            callback.onComplete(false, 0, null, "Request cancelled");
                        }
                    }
                });
            }
        });
    }

    private static String extractMessage(String response, String fallback) {
        if (response == null || response.isEmpty()) return fallback;
        try {
            JsonValue root = new JsonReader().parse(response);
            return root.getString("message", fallback);
        } catch (Exception e) {
            return fallback;
        }
    }

    private static String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static void failOnGameThread(final ApiCallback callback, final String error) {
        runOnGameThread(new Runnable() {
            @Override
            public void run() {
                if (callback != null) {
                    callback.onComplete(false, null, error);
                }
            }
        });
    }

    private static void runOnGameThread(Runnable runnable) {
        if (Gdx.app != null) {
            Gdx.app.postRunnable(runnable);
        } else {
            runnable.run();
        }
    }

    private interface RawResponseCallback {
        void onComplete(boolean success, int statusCode, String responseBody, String errorMessage);
    }

    public interface ApiCallback {
        void onComplete(boolean success, String responseBody, String errorMessage);
    }

    public interface SessionValidationCallback {
        void onValidated(boolean valid, boolean networkError, String message);
    }
}
