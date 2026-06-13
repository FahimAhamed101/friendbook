package com.app.myfriend.backend;

import com.app.myfriend.BuildConfig;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class BackendAuthApi {

    public interface AuthCallback {
        void onSuccess(JSONObject responseJson);
        void onError(String message);
    }

    private static final OkHttpClient CLIENT = new OkHttpClient();
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private BackendAuthApi() {
    }

    public static void signup(JSONObject payload, AuthCallback callback) {
        post("/auth/signup", payload, callback);
    }

    public static void login(JSONObject payload, AuthCallback callback) {
        post("/auth/login", payload, callback);
    }

    public static void getCurrentUser(String token, AuthCallback callback) {
        get("/auth/me", token, callback);
    }

    public static void getMyProfile(String token, AuthCallback callback) {
        get("/profile/me", token, callback);
    }

    public static void getFeedPosts(String token, AuthCallback callback) {
        get("/posts/feed", token, callback);
    }

    public static void getDiscoverPeople(String token, AuthCallback callback) {
        get("/profile/discover/people", token, callback);
    }

    public static void getConversations(String token, AuthCallback callback) {
        get("/chat/conversations", token, callback);
    }

    public static void getConversationMessages(String token, String conversationId, String recipientId, AuthCallback callback) {
        String path = "/chat/conversations/" + conversationId + "/messages";
        if (recipientId != null && !recipientId.trim().isEmpty()) {
            path = path + "?recipientId=" + recipientId.trim();
        }
        get(path, token, callback);
    }

    public static void openConversation(String token, String recipientId, AuthCallback callback) {
        JSONObject payload = new JSONObject();
        try {
            payload.put("recipientId", recipientId);
        } catch (JSONException e) {
            callback.onError("Could not prepare conversation request.");
            return;
        }
        post("/chat/conversations", token, payload, callback);
    }

    public static void sendMessage(String token, String conversationId, String recipientId, String content, AuthCallback callback) {
        JSONObject payload = new JSONObject();
        try {
            payload.put("content", content);
            if (recipientId != null && !recipientId.trim().isEmpty()) {
                payload.put("recipientId", recipientId.trim());
            }
        } catch (JSONException e) {
            callback.onError("Could not prepare message.");
            return;
        }
        post("/chat/conversations/" + conversationId + "/messages", token, payload, callback);
    }

    public static void markConversationRead(String token, String conversationId, AuthCallback callback) {
        JSONObject payload = new JSONObject();
        post("/chat/conversations/" + conversationId + "/read", token, payload, callback);
    }

    public static String resolveUrl(String rawUrl) {
        String normalized = String.valueOf(rawUrl == null ? "" : rawUrl).trim();
        if (normalized.isEmpty()) {
            return "";
        }

        if (normalized.startsWith("http://") || normalized.startsWith("https://")) {
            return normalized;
        }

        if (!normalized.startsWith("/")) {
            return normalized;
        }

        String baseUrl = getBaseUrls().get(0);
        int apiIndex = baseUrl.indexOf("/api");
        String origin = apiIndex >= 0 ? baseUrl.substring(0, apiIndex) : baseUrl;
        return origin + normalized;
    }

    private static void post(String path, JSONObject payload, AuthCallback callback) {
        List<String> baseUrls = getBaseUrls();
        attemptPost(baseUrls, 0, path, null, payload, callback);
    }

    private static void post(String path, String token, JSONObject payload, AuthCallback callback) {
        List<String> baseUrls = getBaseUrls();
        attemptPost(baseUrls, 0, path, token, payload, callback);
    }

    private static void get(String path, String token, AuthCallback callback) {
        List<String> baseUrls = getBaseUrls();
        attemptGet(baseUrls, 0, path, token, callback);
    }

    private static void attemptPost(List<String> baseUrls, int index, String path, String token, JSONObject payload, AuthCallback callback) {
        if (index >= baseUrls.size()) {
            callback.onError("Could not reach backend. Start the backend server and set BACKEND_BASE_URL if needed.");
            return;
        }

        RequestBody body = RequestBody.create(payload.toString(), JSON);
        Request.Builder builder = new Request.Builder()
                .url(baseUrls.get(index) + path)
                .post(body);
        if (token != null && !token.trim().isEmpty()) {
            builder.addHeader("Authorization", "Bearer " + token);
        }
        Request request = builder.build();

        CLIENT.newCall(request).enqueue(new RetryingJsonCallback(baseUrls, index, path, payload, token, callback, true));
    }

    private static void attemptGet(List<String> baseUrls, int index, String path, String token, AuthCallback callback) {
        if (index >= baseUrls.size()) {
            callback.onError("Could not reach backend. Start the backend server and set BACKEND_BASE_URL if needed.");
            return;
        }

        Request request = new Request.Builder()
                .url(baseUrls.get(index) + path)
                .get()
                .addHeader("Authorization", "Bearer " + token)
                .build();

        CLIENT.newCall(request).enqueue(new RetryingJsonCallback(baseUrls, index, path, null, token, callback, false));
    }

    private static List<String> getBaseUrls() {
        Set<String> urls = new LinkedHashSet<>();
        addUrl(urls, BuildConfig.BACKEND_BASE_URL);
        addUrl(urls, "http://10.0.2.2:4000/api");
        addUrl(urls, "http://127.0.0.1:4000/api");
        addUrl(urls, "http://localhost:4000/api");
        return new ArrayList<>(urls);
    }

    private static void addUrl(Set<String> urls, String url) {
        if (url == null) {
            return;
        }

        String normalized = url.trim();
        if (normalized.isEmpty()) {
            return;
        }

        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        urls.add(normalized);
    }

    private static class RetryingJsonCallback implements Callback {
        private final AuthCallback callback;
        private final List<String> baseUrls;
        private final int index;
        private final String path;
        private final JSONObject payload;
        private final String token;
        private final boolean isPost;

        RetryingJsonCallback(List<String> baseUrls, int index, String path, JSONObject payload, String token, AuthCallback callback, boolean isPost) {
            this.baseUrls = baseUrls;
            this.index = index;
            this.path = path;
            this.payload = payload;
            this.token = token;
            this.callback = callback;
            this.isPost = isPost;
        }

        @Override
        public void onFailure(Call call, IOException e) {
            if (index + 1 < baseUrls.size()) {
                if (isPost) {
                    attemptPost(baseUrls, index + 1, path, token, payload, callback);
                } else {
                    attemptGet(baseUrls, index + 1, path, token, callback);
                }
                return;
            }

            callback.onError("Could not reach backend. Start the backend server and set BACKEND_BASE_URL if needed.");
        }

        @Override
        public void onResponse(Call call, Response response) throws IOException {
            String body = response.body() != null ? response.body().string() : "";
            try {
                JSONObject jsonObject = body.isEmpty() ? new JSONObject() : new JSONObject(body);
                if (response.isSuccessful()) {
                    callback.onSuccess(jsonObject);
                    return;
                }

                callback.onError(jsonObject.optString("message", "Request failed."));
            } catch (JSONException e) {
                callback.onError("Received an invalid response from the backend.");
            } finally {
                response.close();
            }
        }
    }
}
