package com.app.myfriend.backend;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;

public class BackendSessionManager {

    private static final String PREFS_NAME = "backend_auth";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_USER = "user";

    private final SharedPreferences preferences;

    public BackendSessionManager(Context context) {
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void saveSession(String token, JSONObject user) {
        preferences.edit()
                .putString(KEY_TOKEN, token)
                .putString(KEY_USER, user != null ? user.toString() : null)
                .apply();
    }

    public String getToken() {
        return preferences.getString(KEY_TOKEN, "");
    }

    public JSONObject getUser() {
        String rawUser = preferences.getString(KEY_USER, "");
        if (rawUser == null || rawUser.trim().isEmpty()) {
            return null;
        }

        try {
            return new JSONObject(rawUser);
        } catch (JSONException ignored) {
            return null;
        }
    }

    public boolean isLoggedIn() {
        return !getToken().trim().isEmpty();
    }

    public void clear() {
        preferences.edit().clear().apply();
    }
}
