package com.app.myfriend.backend;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.myfriend.R;
import com.app.myfriend.emailAuth.LoginActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class BackendHomeActivity extends AppCompatActivity {

    private BackendSessionManager sessionManager;
    private BackendFeedAdapter feedAdapter;
    private View progressBar;
    private TextView titleView;
    private TextView subtitleView;
    private TextView detailsView;
    private TextView emptyView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backend_home);

        sessionManager = new BackendSessionManager(this);
        progressBar = findViewById(R.id.progressBar);
        titleView = findViewById(R.id.backendTitle);
        subtitleView = findViewById(R.id.backendSubtitle);
        detailsView = findViewById(R.id.backendDetails);
        emptyView = findViewById(R.id.emptyState);

        RecyclerView recyclerView = findViewById(R.id.feedRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        feedAdapter = new BackendFeedAdapter();
        recyclerView.setAdapter(feedAdapter);

        findViewById(R.id.refreshButton).setOnClickListener(v -> loadBackendHome());
        findViewById(R.id.logoutButton).setOnClickListener(v -> {
            sessionManager.clear();
            redirectToLogin();
        });

        loadBackendHome();
    }

    private void loadBackendHome() {
        String token = sessionManager.getToken();
        if (token.trim().isEmpty()) {
            redirectToLogin();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
        loadProfile(token);
        loadFeed(token);
    }

    private void loadProfile(String token) {
        BackendAuthApi.getMyProfile(token, new BackendAuthApi.AuthCallback() {
            @Override
            public void onSuccess(JSONObject responseJson) {
                runOnUiThread(() -> {
                    JSONObject profile = responseJson.optJSONObject("profile");
                    if (profile == null) {
                        return;
                    }

                    JSONObject user = profile.optJSONObject("user");
                    if (user != null) {
                        sessionManager.saveSession(token, user);
                    }

                    titleView.setText(profile.optString("fullName", "Backend Feed"));
                    subtitleView.setText(profile.optString("handle", "Signed in"));
                    String headline = profile.optString("headline", "").trim();
                    String bio = profile.optString("bio", "").trim();
                    String details = headline;
                    if (!bio.isEmpty()) {
                        details = details.isEmpty() ? bio : details + "\n\n" + bio;
                    }
                    if (details.isEmpty()) {
                        details = "Signed in with backend auth. Feed data below is loaded from the backend, not Firebase.";
                    }
                    detailsView.setText(details);
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    if (message.toLowerCase().contains("token") || message.toLowerCase().contains("unauthorized")) {
                        sessionManager.clear();
                        redirectToLogin();
                        return;
                    }
                    subtitleView.setText(message);
                });
            }
        });
    }

    private void loadFeed(String token) {
        BackendAuthApi.getFeedPosts(token, new BackendAuthApi.AuthCallback() {
            @Override
            public void onSuccess(JSONObject responseJson) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    JSONArray posts = responseJson.optJSONArray("posts");
                    List<BackendFeedPost> items = new ArrayList<>();
                    if (posts != null) {
                        for (int i = 0; i < posts.length(); i++) {
                            JSONObject post = posts.optJSONObject(i);
                            if (post == null) {
                                continue;
                            }

                            JSONObject stats = post.optJSONObject("stats");
                            items.add(new BackendFeedPost(
                                    post.optString("id", ""),
                                    post.optString("authorName", "Unknown author"),
                                    post.optString("authorHandle", ""),
                                    post.optString("authorImage", ""),
                                    post.optString("activity", "posted"),
                                    post.optString("published", ""),
                                    post.optString("content", ""),
                                    post.optString("image", ""),
                                    stats != null ? stats.optInt("likeCount", 0) : 0,
                                    stats != null ? stats.optInt("commentCount", 0) : 0,
                                    stats != null ? stats.optInt("shareCount", 0) : 0
                            ));
                        }
                    }

                    feedAdapter.submitList(items);
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

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
