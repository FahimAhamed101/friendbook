package com.app.myfriend.backend;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.myfriend.R;
import com.google.android.material.tabs.TabLayout;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class BackendProfileActivity extends AppCompatActivity implements BackendFeedAdapter.FeedActionListener {

    private BackendSessionManager sessionManager;
    private BackendFeedAdapter timelineAdapter;
    private View progressBar;
    private TextView titleNameView;
    private TextView nameView;
    private TextView handleView;
    private TextView headlineView;
    private TextView bioView;
    private TextView postCountView;
    private TextView followersView;
    private TextView followingView;
    private TextView emptyView;
    private CircleImageView avatarView;
    private ImageView coverView;
    private ImageView verifyIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backend_profile);

        sessionManager = new BackendSessionManager(this);
        progressBar = findViewById(R.id.profileProgressBar);
        titleNameView = findViewById(R.id.profileTitleName);
        nameView = findViewById(R.id.profileName);
        handleView = findViewById(R.id.profileHandle);
        headlineView = findViewById(R.id.profileHeadline);
        bioView = findViewById(R.id.profileBio);
        postCountView = findViewById(R.id.profilePostCount);
        followersView = findViewById(R.id.profileFollowers);
        followingView = findViewById(R.id.profileFollowing);
        emptyView = findViewById(R.id.profileEmpty);
        avatarView = findViewById(R.id.profileAvatar);
        coverView = findViewById(R.id.profileCover);
        verifyIcon = findViewById(R.id.profileVerifyIcon);

        RecyclerView timelineRecycler = findViewById(R.id.profileTimelineRecycler);
        timelineRecycler.setLayoutManager(new LinearLayoutManager(this));
        timelineRecycler.setNestedScrollingEnabled(false);
        timelineAdapter = new BackendFeedAdapter(this);
        timelineRecycler.setAdapter(timelineAdapter);

        findViewById(R.id.profileBack).setOnClickListener(v -> finish());
        findViewById(R.id.profileEdit).setOnClickListener(v -> startActivity(new Intent(this, BackendEditProfileActivity.class)));
        findViewById(R.id.profileSavedItems).setOnClickListener(v -> startActivity(new Intent(this, BackendSavedActivity.class)));
        findViewById(R.id.profileMenu).setOnClickListener(v -> startActivity(new Intent(this, BackendMenuActivity.class)));

        TabLayout tabLayout = findViewById(R.id.profileTabLayout);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 2) { // Saved tab
                    loadSavedPosts();
                } else {
                    loadProfile();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

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

                    String fullName = profile.optString("fullName", "Profile");
                    nameView.setText(fullName);
                    titleNameView.setText(fullName);
                    handleView.setText(profile.optString("handle", ""));
                    headlineView.setText(profile.optString("headline", ""));
                    bioView.setText(profile.optString("bio", ""));

                    JSONObject analytics = profile.optJSONObject("analytics");
                    followersView.setText(String.valueOf(analytics != null ? analytics.optInt("followerCount", 0) : 0));
                    followingView.setText(String.valueOf(analytics != null ? analytics.optInt("followingCount", 0) : 0));
                    postCountView.setText(String.valueOf(analytics != null ? analytics.optInt("postCount", 0) : 0));

                    String avatarUrl = BackendAuthApi.resolveUrl(profile.optString("avatarUrl", ""));
                    if (!avatarUrl.isEmpty() && avatarUrl.startsWith("http")) {
                        Picasso.get().load(avatarUrl).placeholder(R.drawable.avatar).error(R.drawable.avatar).into(avatarView);
                    } else {
                        avatarView.setImageResource(R.drawable.avatar);
                    }

                    String coverUrl = BackendAuthApi.resolveUrl(profile.optString("coverImageUrl", ""));
                    if (!coverUrl.isEmpty() && coverUrl.startsWith("http")) {
                        Picasso.get().load(coverUrl).placeholder(R.drawable.cover).error(R.drawable.cover).into(coverView);
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

    private void loadSavedPosts() {
        progressBar.setVisibility(View.VISIBLE);
        BackendAuthApi.getSavedPosts(sessionManager.getToken(), new BackendAuthApi.AuthCallback() {
            @Override
            public void onSuccess(JSONObject responseJson) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    List<BackendFeedPost> items = BackendFeedPostParser.parse(responseJson.optJSONArray("posts"));
                    timelineAdapter.submitList(items);
                    emptyView.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(BackendProfileActivity.this, message, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    @Override
    public void onLike(BackendFeedPost post, String type) {
        BackendAuthApi.reactToPost(sessionManager.getToken(), post.id, type, new BackendAuthApi.AuthCallback() {
            @Override
            public void onSuccess(JSONObject responseJson) {
                runOnUiThread(() -> loadProfile());
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> Toast.makeText(BackendProfileActivity.this, message, Toast.LENGTH_SHORT).show());
            }
        });
    }

    @Override
    public void onComment(BackendFeedPost post) {
        // Simple redirection to Chat for now or show comment dialog
    }

    @Override
    public void onShare(BackendFeedPost post) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, post.content);
        startActivity(Intent.createChooser(shareIntent, "Share post"));
    }

    @Override
    public void onSave(BackendFeedPost post) {
        BackendAuthApi.savePost(sessionManager.getToken(), post.id, new BackendAuthApi.AuthCallback() {
            @Override
            public void onSuccess(JSONObject responseJson) {
                loadProfile();
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> Toast.makeText(BackendProfileActivity.this, message, Toast.LENGTH_SHORT).show());
            }
        });
    }

    @Override
    public void onAuthorClick(String authorId) {
        // Already on my profile
    }
}
