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

    public void submitScore(long score, ApiCallback callback) {
        if (!AuthSession.getInstance().isLoggedIn()) {
            failOnGameThread(callback, "Not logged in");
            return;
        }
        String body = "{\"score\":" + score + "}";
        send("POST", "/api/scores", body, true, callback);
    }

    public void myScores(ApiCallback callback) {
        if (!AuthSession.getInstance().isLoggedIn()) {
            failOnGameThread(callback, "Not logged in");
            return;
        }
        send("GET", "/api/scores/me", null, true, callback);
    }

    public void leaderboard(ApiCallback callback) {
        send("GET", "/api/leaderboard?limit=10", null, false, callback);
    }

    private void postAuth(String path, String username, String password, ApiCallback callback) {
        String body = "{\"username\":\"" + escapeJson(username) + "\",\"password\":\"" + escapeJson(password) + "\"}";
        send("POST", path, body, false, (ok, response, error) -> {
            if (ok) {
                try {
                    JsonValue root = jsonReader.parse(response);
                    String token = root.getString("token", null);
                    String user = root.getString("username", username);
                    if (token != null) {
                        AuthSession.getInstance().set(token, user);
                    }
                } catch (Exception e) {
                    failOnGameThread(callback, "Bad auth response");
                    return;
                }
            }
            if (callback != null) {
                callback.onComplete(ok, response, error);
            }
        });
    }

    private void send(String method, String path, String body, boolean auth, ApiCallback callback) {
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
                int status = httpResponse.getStatus().getStatusCode();
                String response = httpResponse.getResultAsString();
                boolean ok = status >= 200 && status < 300;
                String error = ok ? null : extractMessage(response, "HTTP " + status);
                runOnGameThread(() -> {
                    if (callback != null) {
                        callback.onComplete(ok, response, error);
                    }
                });
            }

            @Override
            public void failed(Throwable t) {
                failOnGameThread(callback, t.getMessage() != null ? t.getMessage() : "Network error");
            }

            @Override
            public void cancelled() {
                failOnGameThread(callback, "Request cancelled");
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

    private static void failOnGameThread(ApiCallback callback, String error) {
        runOnGameThread(() -> {
            if (callback != null) {
                callback.onComplete(false, null, error);
            }
        });
    }

    private static void runOnGameThread(Runnable runnable) {
        Gdx.app.postRunnable(runnable);
    }

    public interface ApiCallback {
        void onComplete(boolean success, String responseBody, String errorMessage);
    }
}
