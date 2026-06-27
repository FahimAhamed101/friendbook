package com.app.myfriend.backend;

import android.content.Intent;
import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.app.myfriend.NightMode;
import com.app.myfriend.R;
import com.app.myfriend.marketPlace.MarketPlaceActivity;
import com.app.myfriend.meeting.MeetingActivity;
import com.app.myfriend.watchParty.StartWatchPartyActivity;
import com.app.myfriend.podcast.PodcastActivity;

public class BackendMenuActivity extends AppCompatActivity {

    private NightMode nightMode;
    private boolean applyingTheme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        nightMode = new NightMode(this);
        applyTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backend_menu);

        TextView backView = findViewById(R.id.menuBack);
        backView.setOnClickListener(v -> finish());

        findViewById(R.id.menuProfile).setOnClickListener(v -> startActivity(new Intent(this, BackendProfileActivity.class)));
        findViewById(R.id.menuMarketplace).setOnClickListener(v -> startActivity(new Intent(this, MarketPlaceActivity.class)));
        findViewById(R.id.menuGroups).setOnClickListener(v -> startActivity(new Intent(this, BackendGroupsActivity.class)));
        findViewById(R.id.menuWatchParty).setOnClickListener(v -> startActivity(new Intent(this, StartWatchPartyActivity.class)));
        findViewById(R.id.menuMeetings).setOnClickListener(v -> startActivity(new Intent(this, MeetingActivity.class)));
        findViewById(R.id.menuPodcast).setOnClickListener(v -> startActivity(new Intent(this, PodcastActivity.class)));
        findViewById(R.id.menuSaved).setOnClickListener(v -> startActivity(new Intent(this, BackendSavedActivity.class)));
        findViewById(R.id.menuLive).setOnClickListener(v -> startActivity(new Intent(this, BackendCreatePostActivity.class)));
        findViewById(R.id.menuLogout).setOnClickListener(v -> {
            new BackendSessionManager(this).clear();
            Intent intent = new Intent(this, com.app.myfriend.welcome.IntroLast.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        SwitchCompat themeSwitch = findViewById(R.id.menuThemeSwitch);
        themeSwitch.setChecked("night".equalsIgnoreCase(nightMode.loadNightModeState()));
        themeSwitch.setOnCheckedChangeListener(this::toggleNightMode);

        findViewById(R.id.menuThemeCard).setOnClickListener(v -> themeSwitch.toggle());

        BackendNavigationHelper.setup(this, R.id.nav_user);
    }

    private void toggleNightMode(CompoundButton buttonView, boolean isChecked) {
        if (applyingTheme) {
            return;
        }
        applyingTheme = true;
        nightMode.setNightModeState(isChecked ? "night" : "day");
        Toast.makeText(this, isChecked ? "Night mode enabled" : "Night mode disabled", Toast.LENGTH_SHORT).show();
        recreate();
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
}
