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
import com.google.android.material.tabs.TabLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class BackendGroupsActivity extends AppCompatActivity {

    private BackendSessionManager sessionManager;
    private BackendGroupsAdapter groupsAdapter;
    private BackendFeedAdapter feedAdapter;
    private View progressBar;
    private TextView emptyView;
    private RecyclerView recyclerView;
    private int currentTab = 0; // 0: My Groups, 1: Feed, 2: Discover

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backend_groups);

        sessionManager = new BackendSessionManager(this);
        progressBar = findViewById(R.id.groupsProgressBar);
        emptyView = findViewById(R.id.groupsEmpty);
        recyclerView = findViewById(R.id.groupsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        groupsAdapter = new BackendGroupsAdapter(new BackendGroupsAdapter.OnGroupClickListener() {
            @Override
            public void onGroupClicked(BackendGroup group) {
                // Open Group Details/Chat - to be implemented
                Toast.makeText(BackendGroupsActivity.this, "Opening " + group.name, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onJoinClicked(BackendGroup group) {
                joinGroup(group);
            }
        });

        feedAdapter = new BackendFeedAdapter(new BackendFeedAdapter.FeedActionListener() {
            @Override public void onLike(BackendFeedPost post, String type) {}
            @Override public void onComment(BackendFeedPost post) {}
            @Override public void onShare(BackendFeedPost post) {}
            @Override public void onSave(BackendFeedPost post) {}
            @Override public void onAuthorClick(String authorId) {
                Intent intent = new Intent(BackendGroupsActivity.this, BackendUserProfileActivity.class);
                intent.putExtra("userId", authorId);
                startActivity(intent);
            }
        });

        TabLayout tabLayout = findViewById(R.id.groupsTabLayout);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                loadData();
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        findViewById(R.id.groupsBack).setOnClickListener(v -> finish());
        findViewById(R.id.groupsCreate).setOnClickListener(v -> {
            startActivity(new Intent(this, BackendCreateGroupActivity.class));
        });

        BackendNavigationHelper.setup(this, -1); // Groups usually from menu
        loadData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData(); // Refresh groups after coming back from creation
    }

    private void loadData() {
        String token = sessionManager.getToken();
        if (token.isEmpty()) return;

        progressBar.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);

        if (currentTab == 1) { // Group Feed
            recyclerView.setAdapter(feedAdapter);
            BackendAuthApi.getGroupPosts(token, new BackendAuthApi.AuthCallback() {
                @Override
                public void onSuccess(JSONObject responseJson) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        List<BackendFeedPost> posts = BackendFeedPostParser.parse(responseJson.optJSONArray("posts"));
                        feedAdapter.submitList(posts);
                        emptyView.setVisibility(posts.isEmpty() ? View.VISIBLE : View.GONE);
                    });
                }
                @Override
                public void onError(String message) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(BackendGroupsActivity.this, message, Toast.LENGTH_SHORT).show();
                    });
                }
            });
        } else { // My Groups or Discover
            recyclerView.setAdapter(groupsAdapter);
            if (currentTab == 0) {
                BackendAuthApi.getMyGroups(token, new BackendAuthApi.AuthCallback() {
                    @Override
                    public void onSuccess(JSONObject responseJson) {
                        runOnUiThread(() -> handleGroupsResponse(responseJson));
                    }
                    @Override
                    public void onError(String message) {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(BackendGroupsActivity.this, message, Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            } else {
                BackendAuthApi.getDiscoverGroups(token, new BackendAuthApi.AuthCallback() {
                    @Override
                    public void onSuccess(JSONObject responseJson) {
                        runOnUiThread(() -> handleGroupsResponse(responseJson));
                    }
                    @Override
                    public void onError(String message) {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(BackendGroupsActivity.this, message, Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            }
        }
    }

    private void handleGroupsResponse(JSONObject responseJson) {
        progressBar.setVisibility(View.GONE);
        JSONArray groupsJson = responseJson.optJSONArray("groups");
        List<BackendGroup> items = new ArrayList<>();
        if (groupsJson != null) {
            for (int i = 0; i < groupsJson.length(); i++) {
                JSONObject g = groupsJson.optJSONObject(i);
                if (g == null) continue;
                items.add(new BackendGroup(
                        g.optString("id", g.optString("_id")),
                        g.optString("name", ""),
                        g.optString("description", ""),
                        g.optString("category", ""),
                        g.optString("iconUrl", ""),
                        g.optString("coverUrl", ""),
                        g.optString("creator", ""),
                        g.optJSONArray("members") != null ? g.optJSONArray("members").length() : 0,
                        true, // Simplified
                        g.optBoolean("isPrivate", false)
                ));
            }
        }
        groupsAdapter.submitList(items);
        emptyView.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void joinGroup(BackendGroup group) {
        String token = sessionManager.getToken();
        progressBar.setVisibility(View.VISIBLE);
        BackendAuthApi.joinGroup(token, group.id, new BackendAuthApi.AuthCallback() {
            @Override
            public void onSuccess(JSONObject responseJson) {
                runOnUiThread(() -> {
                    Toast.makeText(BackendGroupsActivity.this, "Joined successfully", Toast.LENGTH_SHORT).show();
                    loadData();
                });
            }
            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(BackendGroupsActivity.this, message, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}
