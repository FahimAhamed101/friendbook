package com.app.myfriend.backend;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;

import com.app.myfriend.BuildConfig;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
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

    public static void updateMyProfile(String token, JSONObject payload, AuthCallback callback) {
        patch("/profile/me", token, payload, callback);
    }

    public static void updateCurrentUserMedia(String token, JSONObject payload, AuthCallback callback) {
        patch("/auth/me", token, payload, callback);
    }

    public static void getProfileById(String token, String userId, AuthCallback callback) {
        get("/profile/" + userId, token, callback);
    }

    public static void toggleFollowUser(String token, String userId, AuthCallback callback) {
        post("/profile/" + userId + "/follow", token, new JSONObject(), callback);
    }

    public static void getFeedPosts(String token, AuthCallback callback) {
        get("/posts/feed", token, callback);
    }

    public static void getReels(String token, AuthCallback callback) {
        get("/posts/feed?type=video", token, callback);
    }

    public static void getSavedPosts(String token, AuthCallback callback) {
        get("/posts/saved", token, callback);
    }

    public static void getPostById(String token, String postId, AuthCallback callback) {
        get("/posts/" + postId, token, callback);
    }

    public static void createPost(String token, JSONObject payload, AuthCallback callback) {
        post("/posts", token, payload, callback);
    }

    public static void uploadFile(Context context, String token, Uri fileUri, String kind, AuthCallback callback) {
        if (context == null || fileUri == null) {
            callback.onError("A file is required.");
            return;
        }

        String mimeType = context.getContentResolver().getType(fileUri);
        if (mimeType == null || mimeType.trim().isEmpty()) {
            String extension = MimeTypeMap.getFileExtensionFromUrl(fileUri.toString());
            if (extension != null) {
                mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            }
        }
        if (mimeType == null || mimeType.trim().isEmpty()) {
            mimeType = "application/octet-stream";
        }

        byte[] fileBytes;
        try {
            fileBytes = readBytes(context, fileUri);
        } catch (IOException e) {
            callback.onError("Could not read the selected file.");
            return;
        }

        String fileName = getFileName(context, fileUri);
        List<String> baseUrls = getBaseUrls();
        attemptUpload(baseUrls, 0, token, fileBytes, fileName, mimeType, kind, callback);
    }

    public static void likePost(String token, String postId, AuthCallback callback) {
        post("/posts/" + postId + "/like", token, new JSONObject(), callback);
    }

    public static void reactToPost(String token, String postId, String reactionType, AuthCallback callback) {
        JSONObject payload = new JSONObject();
        try {
            payload.put("reactionType", reactionType);
        } catch (JSONException e) {
            callback.onError("Could not prepare reaction.");
            return;
        }
        post("/posts/" + postId + "/reactions", token, payload, callback);
    }

    public static void sharePost(String token, String postId, AuthCallback callback) {
        post("/posts/" + postId + "/share", token, new JSONObject(), callback);
    }

    public static void savePost(String token, String postId, AuthCallback callback) {
        post("/posts/" + postId + "/save", token, new JSONObject(), callback);
    }

    public static void commentOnPost(String token, String postId, String message, AuthCallback callback) {
        JSONObject payload = new JSONObject();
        try {
            payload.put("message", message);
        } catch (JSONException e) {
            callback.onError("Could not prepare comment.");
            return;
        }
        post("/posts/" + postId + "/comments", token, payload, callback);
    }

    public static void getDiscoverPeople(String token, AuthCallback callback) {
        get("/profile/discover/people", token, callback);
    }

    public static void searchPeople(String token, String query, AuthCallback callback) {
        String normalized = String.valueOf(query == null ? "" : query).trim();
        if (normalized.isEmpty()) {
            get("/profile/discover/people", token, callback);
            return;
        }

        get("/profile/discover/people?q=" + Uri.encode(normalized), token, callback);
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

    public static String getVideoThumbnail(String videoUrl) {
        if (videoUrl == null || videoUrl.isEmpty()) return "";
        String resolved = resolveUrl(videoUrl);
        if (resolved.contains("cloudinary.com") && resolved.contains("/video/upload/")) {
            return resolved.replace("/video/upload/", "/video/upload/so_0/").replace(".mp4", ".jpg").replace(".mkv", ".jpg").replace(".mov", ".jpg");
        }
        return resolved;
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

    private static void patch(String path, String token, JSONObject payload, AuthCallback callback) {
        List<String> baseUrls = getBaseUrls();
        attemptPatch(baseUrls, 0, path, token, payload, callback);
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

    private static void attemptPatch(List<String> baseUrls, int index, String path, String token, JSONObject payload, AuthCallback callback) {
        if (index >= baseUrls.size()) {
            callback.onError("Could not reach backend. Start the backend server and set BACKEND_BASE_URL if needed.");
            return;
        }

        RequestBody body = RequestBody.create(payload.toString(), JSON);
        Request request = new Request.Builder()
                .url(baseUrls.get(index) + path)
                .patch(body)
                .addHeader("Authorization", "Bearer " + token)
                .build();

        CLIENT.newCall(request).enqueue(new RetryingJsonCallback(baseUrls, index, path, payload, token, callback, true, true));
    }

    private static void attemptUpload(List<String> baseUrls, int index, String token, byte[] fileBytes, String fileName, String mimeType, String kind, AuthCallback callback) {
        if (index >= baseUrls.size()) {
            callback.onError("Could not reach backend. Start the backend server and set BACKEND_BASE_URL if needed.");
            return;
        }

        RequestBody fileBody = RequestBody.create(fileBytes, MediaType.get(mimeType));
        MultipartBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("kind", kind == null || kind.trim().isEmpty() ? "upload" : kind.trim())
                .addFormDataPart("file", fileName, fileBody)
                .build();

        Request request = new Request.Builder()
                .url(baseUrls.get(index) + "/uploads")
                .post(requestBody)
                .addHeader("Authorization", "Bearer " + token)
                .build();

        CLIENT.newCall(request).enqueue(new UploadRetryingCallback(baseUrls, index, token, fileBytes, fileName, mimeType, kind, callback));
    }

    private static byte[] readBytes(Context context, Uri fileUri) throws IOException {
        try (InputStream inputStream = context.getContentResolver().openInputStream(fileUri);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            if (inputStream == null) {
                throw new IOException("Missing file stream.");
            }

            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            return outputStream.toByteArray();
        }
    }

    private static String getFileName(Context context, Uri fileUri) {
        Cursor cursor = context.getContentResolver().query(fileUri, null, null, null, null);
        if (cursor != null) {
            try {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0 && cursor.moveToFirst()) {
                    String name = cursor.getString(index);
                    if (name != null && !name.trim().isEmpty()) {
                        return name.trim();
                    }
                }
            } finally {
                cursor.close();
            }
        }

        String fallback = fileUri.getLastPathSegment();
        return fallback == null || fallback.trim().isEmpty() ? "upload" : fallback.trim();
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
        private final boolean isPatch;

        RetryingJsonCallback(List<String> baseUrls, int index, String path, JSONObject payload, String token, AuthCallback callback, boolean isPost) {
            this(baseUrls, index, path, payload, token, callback, isPost, false);
        }

        RetryingJsonCallback(List<String> baseUrls, int index, String path, JSONObject payload, String token, AuthCallback callback, boolean isPost, boolean isPatch) {
            this.baseUrls = baseUrls;
            this.index = index;
            this.path = path;
            this.payload = payload;
            this.token = token;
            this.callback = callback;
            this.isPost = isPost;
            this.isPatch = isPatch;
        }

        @Override
        public void onFailure(Call call, IOException e) {
            if (index + 1 < baseUrls.size()) {
                if (isPatch) {
                    attemptPatch(baseUrls, index + 1, path, token, payload, callback);
                } else if (isPost) {
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

    private static class UploadRetryingCallback implements Callback {
        private final AuthCallback callback;
        private final List<String> baseUrls;
        private final int index;
        private final String token;
        private final byte[] fileBytes;
        private final String fileName;
        private final String mimeType;
        private final String kind;

        UploadRetryingCallback(List<String> baseUrls, int index, String token, byte[] fileBytes, String fileName, String mimeType, String kind, AuthCallback callback) {
            this.baseUrls = baseUrls;
            this.index = index;
            this.token = token;
            this.fileBytes = fileBytes;
            this.fileName = fileName;
            this.mimeType = mimeType;
            this.kind = kind;
            this.callback = callback;
        }

        @Override
        public void onFailure(Call call, IOException e) {
            if (index + 1 < baseUrls.size()) {
                attemptUpload(baseUrls, index + 1, token, fileBytes, fileName, mimeType, kind, callback);
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

                callback.onError(jsonObject.optString("message", "Upload failed."));
            } catch (JSONException e) {
                callback.onError("Received an invalid response from the backend.");
            } finally {
                response.close();
            }
        }
    }
}
