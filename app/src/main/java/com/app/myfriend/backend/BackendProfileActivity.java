package com.app.myfriend.backend;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.myfriend.R;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class BackendProfileActivity extends AppCompatActivity {

    private BackendSessionManager sessionManager;
    private BackendFeedAdapter timelineAdapter;
    private View progressBar;
    private TextView nameView;
    private TextView handleView;
    private TextView headlineView;
    private TextView bioView;
    private TextView followersView;
    private TextView followingView;
    private TextView emptyView;
    private CircleImageView avatarView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backend_profile);

        sessionManager = new BackendSessionManager(this);
        progressBar = findViewById(R.id.profileProgressBar);
        nameView = findViewById(R.id.profileName);
        handleView = findViewById(R.id.profileHandle);
        headlineView = findViewById(R.id.profileHeadline);
        bioView = findViewById(R.id.profileBio);
        followersView = findViewById(R.id.profileFollowers);
        followingView = findViewById(R.id.profileFollowing);
        emptyView = findViewById(R.id.profileEmpty);
        avatarView = findViewById(R.id.profileAvatar);

        RecyclerView timelineRecycler = findViewById(R.id.profileTimelineRecycler);
        timelineRecycler.setLayoutManager(new LinearLayoutManager(this));
        timelineAdapter = new BackendFeedAdapter(null);
        timelineRecycler.setAdapter(timelineAdapter);

        findViewById(R.id.profileBack).setOnClickListener(v -> finish());
        findViewById(R.id.profileEdit).setOnClickListener(v -> startActivity(new Intent(this, BackendEditProfileActivity.class)));
        findViewById(R.id.profileHome).setOnClickListener(v -> {
            startActivity(new Intent(this, BackendHomeActivity.class));
            finish();
        });
        findViewById(R.id.profileChats).setOnClickListener(v -> startActivity(new Intent(this, BackendChatListActivity.class)));
        findViewById(R.id.profileDiscover).setOnClickListener(v -> startActivity(new Intent(this, BackendDiscoverActivity.class)));

        BackendNavigationHelper.setup(this, R.id.nav_user);
        loadProfile();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProfile();
    }

    private void loadProfile() {
        String token = sessionManager.getToken();
        if (token.trim().isEmpty()) {
            finish();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        BackendAuthApi.getMyProfile(token, new BackendAuthApi.AuthCallback() {
            @Override
            public void onSuccess(JSONObject responseJson) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    JSONObject profile = responseJson.optJSONObject("profile");
                    if (profile == null) {
                        emptyView.setVisibility(View.VISIBLE);
                        return;
                    }

                    JSONObject user = profile.optJSONObject("user");
                    if (user != null) {
                        sessionManager.saveSession(token, user);
                    }

                    nameView.setText(profile.optString("fullName", "Profile"));
                    handleView.setText(profile.optString("handle", ""));
                    headlineView.setText(profile.optString("headline", ""));
                    bioView.setText(profile.optString("bio", ""));
                    JSONObject analytics = profile.optJSONObject("analytics");
                    followersView.setText(String.valueOf(analytics != null ? analytics.optInt("followerCount", 0) : 0));
                    followingView.setText(String.valueOf(analytics != null ? analytics.optInt("followingCount", 0) : 0));

                    String avatarUrl = BackendAuthApi.resolveUrl(profile.optString("avatarUrl", ""));
                    if (!avatarUrl.isEmpty() && avatarUrl.startsWith("http")) {
                        Picasso.get().load(avatarUrl).placeholder(R.drawable.avatar).error(R.drawable.avatar).into(avatarView);
                    } else {
                        avatarView.setImageResource(R.drawable.avatar);
                    }

                    JSONArray timeline = responseJson.optJSONArray("timeline");
                    List<BackendFeedPost> items = BackendFeedPostParser.parse(timeline);
                    timelineAdapter.submitList(items);
                    emptyView.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    emptyView.setVisibility(View.VISIBLE);
                    emptyView.setText(message);
                });
            }
        });
    }
}
