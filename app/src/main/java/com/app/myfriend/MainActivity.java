package com.app.myfriend;

import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;

import androidx.appcompat.app.AppCompatActivity;

import com.app.myfriend.backend.BackendChatListActivity;
import com.app.myfriend.backend.BackendDiscoverActivity;
import com.app.myfriend.backend.BackendHomeActivity;
import com.app.myfriend.backend.BackendSavedActivity;
import com.app.myfriend.backend.BackendSessionManager;
import com.app.myfriend.backend.BackendUserProfileActivity;
import com.app.myfriend.welcome.IntroLast;

import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private NightMode nightMode;
    private SharedMode sharedMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        nightMode = new NightMode(this);
        applyTheme();
        sharedMode = new SharedMode(this);
        String locale = sharedMode.loadNightModeState();
        if (!locale.isEmpty()) {
            setApplicationLocale(locale);
        }

        super.onCreate(savedInstanceState);

        BackendSessionManager sessionManager = new BackendSessionManager(this);
        if (!sessionManager.isLoggedIn()) {
            Intent intent = new Intent(this, IntroLast.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
            return;
        }

        startActivity(resolveDestination(getIntent() != null ? getIntent().getData() : null));
        finish();
    }

    private Intent resolveDestination(Uri uri) {
        Intent fallback = new Intent(this, BackendHomeActivity.class);
        fallback.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

        if (uri == null) {
            return fallback;
        }

        List<String> segments = uri.getPathSegments();
        if (segments == null || segments.isEmpty()) {
            return fallback;
        }

        String type = segments.size() >= 2 ? segments.get(segments.size() - 2) : "";
        String id = segments.get(segments.size() - 1);

        if ("user".equalsIgnoreCase(type) && !id.trim().isEmpty()) {
            Intent intent = new Intent(this, BackendUserProfileActivity.class);
            intent.putExtra("userId", id);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            return intent;
        }

        if ("chat".equalsIgnoreCase(type)) {
            Intent intent = new Intent(this, BackendChatListActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            return intent;
        }

        if ("discover".equalsIgnoreCase(type) || "group".equalsIgnoreCase(type) || "reel".equalsIgnoreCase(type)) {
            Intent intent = new Intent(this, BackendDiscoverActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            return intent;
        }

        if ("saved".equalsIgnoreCase(type) || "product".equalsIgnoreCase(type)) {
            Intent intent = new Intent(this, BackendSavedActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            return intent;
        }

        return fallback;
    }

    private void applyTheme() {
        String state = nightMode.loadNightModeState();
        if ("night".equalsIgnoreCase(state)) {
            setTheme(R.style.DarkTheme);
        } else if ("dim".equalsIgnoreCase(state)) {
            setTheme(R.style.DimTheme);
        } else {
            setTheme(R.style.AppTheme);
        }
    }

    private void setApplicationLocale(String locale) {
        Resources resources = getResources();
        DisplayMetrics dm = resources.getDisplayMetrics();
        Configuration config = resources.getConfiguration();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            config.setLocale(new Locale(locale.toLowerCase()));
        } else {
            config.locale = new Locale(locale.toLowerCase());
        }
        resources.updateConfiguration(config, dm);
    }
}
