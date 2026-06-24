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
    private TextView chatBadge;
    private CircleImageView avatarView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backend_home);

        sessionManager = new BackendSessionManager(this);
        progressBar = findViewById(R.id.progressBar);
        titleView = findViewById(R.id.backendTitle);
        chatBadge = findViewById(R.id.chatBadgeCount);
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
        findViewById(R.id.notificationButton).setOnClickListener(v -> startActivity(new Intent(this, BackendNotificationsActivity.class)));
        findViewById(R.id.peopleButton).setOnClickListener(v -> startActivity(new Intent(this, BackendDiscoverActivity.class)));
        findViewById(R.id.searchButton).setOnClickListener(v -> startActivity(new Intent(this, BackendSearchActivity.class)));

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
        if (sessionManager.isLoggedIn()) {
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
        loadProfile(token);
        loadStories(token);
        loadNotificationCount(token);
        loadFeed(token);
    }

    private void loadNotificationCount(String token) {
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
                    if (unreadCount > 0) {
                        chatBadge.setVisibility(View.VISIBLE);
                        chatBadge.setText(String.valueOf(unreadCount));
                    } else {
                        chatBadge.setVisibility(View.GONE);
                    }
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> chatBadge.setVisibility(View.GONE));
            }
        });
    }

    private void loadProfile(String token) {
        BackendAuthApi.getMyProfile(token, new BackendAuthApi.AuthCallback() {
            @Override
            public void onSuccess(JSONObject responseJson) {
                runOnUiThread(() -> {
                    JSONObject profile = responseJson.optJSONObject("profile");
                    if (profile == null) return;

                    JSONObject user = profile.optJSONObject("user");
                    if (user != null) {
                        sessionManager.saveSession(token, user);
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
                    }
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
                        for (int i = 0; i < users.length() && i < 10; i++) {
                            JSONObject user = users.optJSONObject(i);
                            if (user == null) continue;
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
                    findViewById(R.id.emptyState).setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    findViewById(R.id.emptyState).setVisibility(View.VISIBLE);
                    ((TextView)findViewById(R.id.emptyState)).setText(message);
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
        if (story == null || story.id == null || story.id.trim().isEmpty()) return;

        BackendAuthApi.openConversation(sessionManager.getToken(), story.id, new BackendAuthApi.AuthCallback() {
            @Override
            public void onSuccess(JSONObject responseJson) {
                runOnUiThread(() -> {
                    Intent intent = new Intent(BackendHomeActivity.this, BackendChatThreadActivity.class);
                    intent.putExtra("conversationId", responseJson.optString("conversationId", ""));
                    intent.putExtra("recipientId", story.id);
                    intent.putExtra("title", story.name);
                    intent.putExtra("avatar", story.imageUrl);
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
    public void onLike(BackendFeedPost post, String type) {
        BackendAuthApi.reactToPost(sessionManager.getToken(), post.id, type, new FeedRefreshCallback("Reaction updated."));
    }

    @Override
    public void onComment(BackendFeedPost post) {
        Intent intent = new Intent(this, BackendCommentActivity.class);
        intent.putExtra("postId", post.id);
        startActivity(intent);
    }

    @Override
    public void onShare(BackendFeedPost post) {
        BackendAuthApi.sharePost(sessionManager.getToken(), post.id, new BackendAuthApi.AuthCallback() {
            @Override
            public void onSuccess(JSONObject responseJson) {
                runOnUiThread(() -> {
                    Toast.makeText(BackendHomeActivity.this, "Post shared.", Toast.LENGTH_SHORT).show();
                    loadFeed(sessionManager.getToken());
                    openShareSheet(post.title != null ? post.title : post.content, BackendAuthApi.resolveUrl(post.linkUrl));
                });
            }
            @Override public void onError(String message) {}
        });
    }

    @Override
    public void onSave(BackendFeedPost post) {
        BackendAuthApi.savePost(sessionManager.getToken(), post.id, new FeedRefreshCallback(post.savedByViewer ? "Unsaved" : "Saved"));
    }

    @Override
    public void onAuthorClick(String authorId) {
        Intent intent = new Intent(this, BackendUserProfileActivity.class);
        intent.putExtra("userId", authorId);
        startActivity(intent);
    }

    private void openShareSheet(String text, String url) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, text + (url != null ? "\n" + url : ""));
        startActivity(Intent.createChooser(intent, "Share via"));
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
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
                if (successMessage != null) {
                    Toast.makeText(BackendHomeActivity.this, successMessage, Toast.LENGTH_SHORT).show();
                }
                loadFeed(sessionManager.getToken());
            });
        }

        @Override
        public void onError(String message) {
            runOnUiThread(() -> Toast.makeText(BackendHomeActivity.this, message, Toast.LENGTH_SHORT).show());
        }
    }
}
