package com.app.myfriend.backend;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.myfriend.R;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONObject;

import de.hdodenhof.circleimageview.CircleImageView;

public class BackendUserProfileActivity extends AppCompatActivity {

    private BackendSessionManager sessionManager;
    private BackendFeedAdapter timelineAdapter;
    private View progressBar;
    private TextView nameView;
    private TextView handleView;
    private TextView headlineView;
    private TextView bioView;
    private TextView actionView;
    private TextView emptyView;
    private CircleImageView avatarView;
    private String userId;
    private boolean following;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backend_user_profile);

        userId = getIntent().getStringExtra("userId");
        sessionManager = new BackendSessionManager(this);
        progressBar = findViewById(R.id.userProfileProgressBar);
        nameView = findViewById(R.id.userProfileName);
        handleView = findViewById(R.id.userProfileHandle);
        headlineView = findViewById(R.id.userProfileHeadline);
        bioView = findViewById(R.id.userProfileBio);
        actionView = findViewById(R.id.userProfileAction);
        emptyView = findViewById(R.id.userProfileEmpty);
        avatarView = findViewById(R.id.userProfileAvatar);

        RecyclerView recyclerView = findViewById(R.id.userProfileTimelineRecycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        timelineAdapter = new BackendFeedAdapter(null);
        recyclerView.setAdapter(timelineAdapter);

        findViewById(R.id.userProfileBack).setOnClickListener(v -> finish());
        findViewById(R.id.userProfileMessage).setOnClickListener(v -> openConversation());
        actionView.setOnClickListener(v -> toggleFollow());

        loadProfile();
    }

    private void loadProfile() {
        String token = sessionManager.getToken();
        if (token.trim().isEmpty() || userId == null || userId.trim().isEmpty()) {
            finish();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        BackendAuthApi.getProfileById(token, userId, new BackendAuthApi.AuthCallback() {
            @Override
            public void onSuccess(JSONObject responseJson) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    JSONObject profile = responseJson.optJSONObject("profile");
                    if (profile == null) {
                        emptyView.setVisibility(View.VISIBLE);
                        return;
                    }

                    JSONObject network = responseJson.optJSONObject("network");
                    following = isFollowing(network);
                    actionView.setText(following ? "Following" : "Follow");

                    nameView.setText(profile.optString("fullName", "Profile"));
                    handleView.setText(profile.optString("handle", ""));
                    headlineView.setText(profile.optString("headline", ""));
                    bioView.setText(profile.optString("bio", ""));

                    String avatarUrl = BackendAuthApi.resolveUrl(profile.optString("avatarUrl", ""));
                    if (!avatarUrl.isEmpty() && avatarUrl.startsWith("http")) {
                        Picasso.get().load(avatarUrl).placeholder(R.drawable.avatar).error(R.drawable.avatar).into(avatarView);
                    } else {
                        avatarView.setImageResource(R.drawable.avatar);
                    }

                    JSONArray timeline = responseJson.optJSONArray("timeline");
                    timelineAdapter.submitList(BackendFeedPostParser.parse(timeline));
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

    private boolean isFollowing(JSONObject network) {
        if (network == null) {
            return false;
        }
        JSONArray followers = network.optJSONArray("followers");
        if (followers == null) {
            return false;
        }
        for (int i = 0; i < followers.length(); i++) {
            JSONObject follower = followers.optJSONObject(i);
            if (follower != null && "You".equalsIgnoreCase(follower.optString("actionLabel", ""))) {
                return true;
            }
        }
        return false;
    }

    private void toggleFollow() {
        BackendAuthApi.toggleFollowUser(sessionManager.getToken(), userId, new BackendAuthApi.AuthCallback() {
            @Override
            public void onSuccess(JSONObject responseJson) {
                runOnUiThread(() -> {
                    following = responseJson.optBoolean("isFollowing", !following);
                    actionView.setText(following ? "Following" : "Follow");
                    Toast.makeText(BackendUserProfileActivity.this, responseJson.optString("message", "Updated."), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> Toast.makeText(BackendUserProfileActivity.this, message, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void openConversation() {
        BackendAuthApi.openConversation(sessionManager.getToken(), userId, new BackendAuthApi.AuthCallback() {
            @Override
            public void onSuccess(JSONObject responseJson) {
                runOnUiThread(() -> {
                    Intent intent = new Intent(BackendUserProfileActivity.this, BackendChatThreadActivity.class);
                    intent.putExtra("conversationId", responseJson.optString("conversationId", ""));
                    intent.putExtra("recipientId", userId);
                    intent.putExtra("title", nameView.getText().toString());
                    startActivity(intent);
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> Toast.makeText(BackendUserProfileActivity.this, message, Toast.LENGTH_SHORT).show());
            }
        });
    }
}
