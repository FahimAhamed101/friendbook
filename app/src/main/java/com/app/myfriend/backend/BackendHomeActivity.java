package com.app.myfriend.backend;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.myfriend.R;
import com.app.myfriend.emailAuth.LoginActivity;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class BackendHomeActivity extends AppCompatActivity implements BackendFeedAdapter.FeedActionListener {

    private static final String[] STORY_BADGES = {"Live", "Podcast", "Story", "Update"};

    private BackendSessionManager sessionManager;
    private BackendFeedAdapter feedAdapter;
    private BackendHomeStoryAdapter storyAdapter;
    private View progressBar;
    private TextView titleView;
    private TextView subtitleView;
    private TextView detailsView;
    private TextView emptyView;
    private CircleImageView avatarView;

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
        avatarView = findViewById(R.id.homeAvatar);

        RecyclerView storiesRecyclerView = findViewById(R.id.storiesRecyclerView);
        storiesRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        storyAdapter = new BackendHomeStoryAdapter(this::openStoryPerson);
        storiesRecyclerView.setAdapter(storyAdapter);

        RecyclerView recyclerView = findViewById(R.id.feedRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setNestedScrollingEnabled(false);
        feedAdapter = new BackendFeedAdapter(this);
        recyclerView.setAdapter(feedAdapter);

        findViewById(R.id.refreshButton).setOnClickListener(v -> loadBackendHome());
        findViewById(R.id.profileButton).setOnClickListener(v -> startActivity(new Intent(this, BackendProfileActivity.class)));
        findViewById(R.id.chatButton).setOnClickListener(v -> startActivity(new Intent(this, BackendNotificationsActivity.class)));
        findViewById(R.id.peopleButton).setOnClickListener(v -> startActivity(new Intent(this, BackendDiscoverActivity.class)));
        findViewById(R.id.searchButton).setOnClickListener(v -> startActivity(new Intent(this, BackendSearchActivity.class)));
        findViewById(R.id.logoutButton).setOnClickListener(v -> {
            sessionManager.clear();
            redirectToLogin();
        });

        findViewById(R.id.createPostCard).setOnClickListener(v -> openCreatePostComposer("custom"));
        findViewById(R.id.imagePostButton).setOnClickListener(v -> openCreatePostComposer("image"));
        findViewById(R.id.videoPostButton).setOnClickListener(v -> openCreatePostComposer("video"));
        findViewById(R.id.livePostButton).setOnClickListener(v -> openCreatePostComposer("custom"));

        BackendNavigationHelper.setup(this, R.id.nav_home);
        loadBackendHome();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!sessionManager.getToken().trim().isEmpty()) {
            loadBackendHome();
        }
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
        loadStories(token);
        loadNotificationCount(token);
        loadFeed(token);
    }

    private void loadNotificationCount(String token) {
        TextView countView = findViewById(R.id.chatButton);
        BackendAuthApi.getConversations(token, new BackendAuthApi.AuthCallback() {
            @Override
            public void onSuccess(JSONObject responseJson) {
                runOnUiThread(() -> {
                    JSONArray data = responseJson.optJSONArray("data");
                    int unreadCount = 0;
                    if (data != null) {
                        for (int i = 0; i < data.length(); i++) {
                            JSONObject conversation = data.optJSONObject(i);
                            if (conversation != null) {
                                unreadCount += conversation.optInt("unreadCount", 0);
                            }
                        }
                    }
                    countView.setText(String.valueOf(Math.max(0, unreadCount)));
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> countView.setText("0"));
            }
        });
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

                    String displayName = profile.optString("fullName", "").trim();
                    titleView.setText(displayName.isEmpty() ? getString(R.string.app_name) : getString(R.string.app_name));
                    subtitleView.setText(profile.optString("handle", ""));

                    String bio = profile.optString("bio", "").trim();
                    if (bio.isEmpty()) {
                        detailsView.setVisibility(View.GONE);
                    } else {
                        detailsView.setVisibility(View.VISIBLE);
                        detailsView.setText(bio);
                    }

                    String avatarUrl = BackendAuthApi.resolveUrl(profile.optString("avatarUrl", ""));
                    if (!avatarUrl.isEmpty() && avatarUrl.startsWith("http")) {
                        Picasso.get().load(avatarUrl).placeholder(R.drawable.avatar).error(R.drawable.avatar).into(avatarView);
                    } else {
                        avatarView.setImageResource(R.drawable.avatar);
                    }
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

    private void loadStories(String token) {
        BackendAuthApi.getDiscoverPeople(token, new BackendAuthApi.AuthCallback() {
            @Override
            public void onSuccess(JSONObject responseJson) {
                runOnUiThread(() -> {
                    JSONArray users = responseJson.optJSONArray("users");
                    List<BackendHomeStory> stories = new ArrayList<>();
                    if (users != null) {
                        for (int i = 0; i < users.length() && i < 8; i++) {
                            JSONObject user = users.optJSONObject(i);
                            if (user == null) {
                                continue;
                            }
                            stories.add(new BackendHomeStory(
                                    user.optString("id", ""),
                                    user.optString("name", "User"),
                                    user.optString("image", ""),
                                    STORY_BADGES[i % STORY_BADGES.length]
                            ));
                        }
                    }
                    storyAdapter.submitList(stories);
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> storyAdapter.submitList(new ArrayList<>()));
            }
        });
    }

    private void loadFeed(String token) {
        BackendAuthApi.getFeedPosts(token, new BackendAuthApi.AuthCallback() {
            @Override
            public void onSuccess(JSONObject responseJson) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    List<BackendFeedPost> items = BackendFeedPostParser.parse(responseJson.optJSONArray("posts"));
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

    private void openCreatePostComposer(String postType) {
        Intent intent = new Intent(this, BackendCreatePostActivity.class);
        intent.putExtra("initial_post_type", postType);
        startActivity(intent);
    }

    private void openStoryPerson(BackendHomeStory story) {
        if (story == null || story.id == null || story.id.trim().isEmpty()) {
            return;
        }

        BackendAuthApi.openConversation(sessionManager.getToken(), story.id, new BackendAuthApi.AuthCallback() {
            @Override
            public void onSuccess(JSONObject responseJson) {
                runOnUiThread(() -> {
                    Intent intent = new Intent(BackendHomeActivity.this, BackendChatThreadActivity.class);
                    intent.putExtra("conversationId", responseJson.optString("conversationId", ""));
                    intent.putExtra("recipientId", story.id);
                    intent.putExtra("title", story.name);
                    startActivity(intent);
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> Toast.makeText(BackendHomeActivity.this, message, Toast.LENGTH_SHORT).show());
            }
        });
    }

    @Override
    public void onLike(BackendFeedPost post) {
        BackendAuthApi.likePost(sessionManager.getToken(), post.id, new FeedRefreshCallback("Post updated."));
    }

    @Override
    public void onComment(BackendFeedPost post) {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setHint("Write a comment");

        new AlertDialog.Builder(this)
                .setTitle("Add comment")
                .setView(input)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Post", (dialog, which) -> {
                    String message = input.getText().toString().trim();
                    if (message.isEmpty()) {
                        Toast.makeText(this, "Comment cannot be empty.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    BackendAuthApi.commentOnPost(sessionManager.getToken(), post.id, message, new FeedRefreshCallback("Comment added."));
                })
                .show();
    }

    @Override
    public void onShare(BackendFeedPost post) {
        BackendAuthApi.sharePost(sessionManager.getToken(), post.id, new BackendAuthApi.AuthCallback() {
            @Override
            public void onSuccess(JSONObject responseJson) {
                runOnUiThread(() -> {
                    Toast.makeText(BackendHomeActivity.this, "Post shared.", Toast.LENGTH_SHORT).show();
                    loadFeed(sessionManager.getToken());
                    String shareUrl = BackendAuthApi.resolveUrl(!post.linkUrl.trim().isEmpty() ? post.linkUrl : post.attachmentUrl);
                    String shareText = post.title != null && !post.title.trim().isEmpty()
                            ? post.title
                            : post.content;
                    openShareSheet(shareText, shareUrl);
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> Toast.makeText(BackendHomeActivity.this, message, Toast.LENGTH_SHORT).show());
            }
        });
    }

    @Override
    public void onSave(BackendFeedPost post) {
        BackendAuthApi.savePost(sessionManager.getToken(), post.id, new FeedRefreshCallback(post.savedByViewer ? "Post removed from saved items." : "Post saved."));
    }

    private void openShareSheet(String shareText, String shareUrl) {
        String normalizedText = String.valueOf(shareText == null ? "" : shareText).trim();
        String normalizedUrl = String.valueOf(shareUrl == null ? "" : shareUrl).trim();
        StringBuilder payload = new StringBuilder();
        if (!normalizedText.isEmpty()) {
            payload.append(normalizedText);
        }
        if (!normalizedUrl.isEmpty()) {
            if (payload.length() > 0) {
                payload.append("\n");
            }
            payload.append(normalizedUrl);
        }

        if (payload.length() == 0) {
            payload.append(getString(R.string.app_name));
        }

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, payload.toString());
        startActivity(Intent.createChooser(shareIntent, "Share post"));
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private class FeedRefreshCallback implements BackendAuthApi.AuthCallback {
        private final String successMessage;

        FeedRefreshCallback(String successMessage) {
            this.successMessage = successMessage;
        }

        @Override
        public void onSuccess(JSONObject responseJson) {
            runOnUiThread(() -> {
                Toast.makeText(BackendHomeActivity.this, successMessage, Toast.LENGTH_SHORT).show();
                loadFeed(sessionManager.getToken());
            });
        }

        @Override
        public void onError(String message) {
            runOnUiThread(() -> Toast.makeText(BackendHomeActivity.this, message, Toast.LENGTH_SHORT).show());
        }
    }
}
