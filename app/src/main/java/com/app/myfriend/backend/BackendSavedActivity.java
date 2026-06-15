package com.app.myfriend.backend;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.myfriend.R;
import com.google.android.material.tabs.TabLayout;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class BackendSavedActivity extends AppCompatActivity {

    private BackendSessionManager sessionManager;
    private BackendFeedAdapter postsAdapter;
    private View progressBar;
    private TextView emptyView;
    private RecyclerView recyclerView;
    private List<BackendFeedPost> savedPosts = new ArrayList<>();
    private int selectedTab = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backend_saved);

        sessionManager = new BackendSessionManager(this);
        progressBar = findViewById(R.id.savedProgressBar);
        emptyView = findViewById(R.id.savedEmpty);
        recyclerView = findViewById(R.id.savedRecyclerView);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        postsAdapter = new BackendFeedAdapter(null);
        recyclerView.setAdapter(postsAdapter);

        findViewById(R.id.savedBack).setOnClickListener(v -> finish());

        TabLayout tabLayout = findViewById(R.id.savedTabLayout);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                selectedTab = tab != null ? tab.getPosition() : 0;
                renderCurrentTab();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        BackendNavigationHelper.setup(this, R.id.nav_user);
        loadSavedContent();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSavedContent();
    }

    private void loadSavedContent() {
        String token = sessionManager.getToken();
        if (token.trim().isEmpty()) {
            startActivity(new Intent(this, com.app.myfriend.welcome.IntroLast.class));
            finish();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);

        BackendAuthApi.getSavedPosts(token, new BackendAuthApi.AuthCallback() {
            @Override
            public void onSuccess(JSONObject responseJson) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    savedPosts = BackendFeedPostParser.parse(responseJson.optJSONArray("posts"));
                    renderCurrentTab();
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    emptyView.setText(message);
                    emptyView.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                });
            }
        });
    }

    private void renderCurrentTab() {
        if (selectedTab == 1) {
            postsAdapter.submitList(new ArrayList<>());
            recyclerView.setVisibility(View.GONE);
            emptyView.setText("No saved reels yet on the backend.");
            emptyView.setVisibility(View.VISIBLE);
            return;
        }

        if (savedPosts.isEmpty()) {
            postsAdapter.submitList(new ArrayList<>());
            recyclerView.setVisibility(View.GONE);
            emptyView.setText("No saved posts yet.");
            emptyView.setVisibility(View.VISIBLE);
            return;
        }

        postsAdapter.submitList(savedPosts);
        emptyView.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
    }

}
