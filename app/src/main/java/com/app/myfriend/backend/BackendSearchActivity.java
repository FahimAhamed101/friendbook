package com.app.myfriend.backend;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.myfriend.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class BackendSearchActivity extends AppCompatActivity {

    private BackendSessionManager sessionManager;
    private BackendPeopleAdapter peopleAdapter;
    private BackendFeedAdapter postAdapter;
    private View progressBar;
    private TextView emptyView;
    private EditText searchInput;
    private RecyclerView recyclerView;
    private String currentCategory = "people"; // Default category

    private TextView[] categories;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backend_search);

        sessionManager = new BackendSessionManager(this);
        progressBar = findViewById(R.id.searchProgressBar);
        emptyView = findViewById(R.id.searchEmpty);
        searchInput = findViewById(R.id.searchInput);

        recyclerView = findViewById(R.id.searchRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        peopleAdapter = new BackendPeopleAdapter(new BackendPeopleAdapter.OnPersonClickListener() {
            @Override
            public void onPersonClicked(BackendPerson person) {
                Intent intent = new Intent(BackendSearchActivity.this, BackendUserProfileActivity.class);
                intent.putExtra("userId", person.id);
                startActivity(intent);
            }

            @Override
            public void onActionClicked(BackendPerson person) {
                toggleFollow(person);
            }
        });

        postAdapter = new BackendFeedAdapter(new BackendFeedAdapter.FeedActionListener() {
            @Override public void onLike(BackendFeedPost post, String type) {}
            @Override public void onComment(BackendFeedPost post) {}
            @Override public void onShare(BackendFeedPost post) {}
            @Override public void onSave(BackendFeedPost post) {}
            @Override public void onAuthorClick(String authorId) {
                Intent intent = new Intent(BackendSearchActivity.this, BackendUserProfileActivity.class);
                intent.putExtra("userId", authorId);
                startActivity(intent);
            }
        });

        categories = new TextView[]{
                findViewById(R.id.categoryPeople),
                findViewById(R.id.categoryPosts),
                findViewById(R.id.categoryImages),
                findViewById(R.id.categoryVideos)
        };

        findViewById(R.id.searchBack).setOnClickListener(v -> finish());

        setupCategory(findViewById(R.id.categoryPeople), "people");
        setupCategory(findViewById(R.id.categoryPosts), "post");
        setupCategory(findViewById(R.id.categoryImages), "image");
        setupCategory(findViewById(R.id.categoryVideos), "video");

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                loadResults(s == null ? "" : s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        updateCategoryUI();
        loadResults("");
    }

    private void setupCategory(TextView view, String cat) {
        view.setOnClickListener(v -> {
            currentCategory = cat;
            updateCategoryUI();
            loadResults(searchInput.getText().toString());
        });
    }

    private void updateCategoryUI() {
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(R.attr.texttwocolor, typedValue, true);
        int textColor = typedValue.data;

        for (TextView catView : categories) {
            catView.setBackgroundTintList(null);
            catView.setBackgroundResource(R.drawable.btn_round);
            catView.setTextColor(textColor);
        }

        TextView active = null;
        if ("people".equals(currentCategory)) active = findViewById(R.id.categoryPeople);
        else if ("post".equals(currentCategory)) active = findViewById(R.id.categoryPosts);
        else if ("image".equals(currentCategory)) active = findViewById(R.id.categoryImages);
        else if ("video".equals(currentCategory)) active = findViewById(R.id.categoryVideos);

        if (active != null) {
            active.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.colorPrimary));
            active.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        }
    }

    private void loadResults(String query) {
        String token = sessionManager.getToken();
        if (token.trim().isEmpty()) return;

        progressBar.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);

        if ("people".equals(currentCategory)) {
            recyclerView.setAdapter(peopleAdapter);
            BackendAuthApi.searchPeople(token, query, new BackendAuthApi.AuthCallback() {
                @Override
                public void onSuccess(JSONObject responseJson) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        JSONArray users = responseJson.optJSONArray("users");
                        List<BackendPerson> items = new ArrayList<>();
                        if (users != null) {
                            for (int i = 0; i < users.length(); i++) {
                                JSONObject user = users.optJSONObject(i);
                                if (user == null) continue;
                                items.add(new BackendPerson(
                                        user.optString("id", ""),
                                        user.optString("name", "User"),
                                        user.optString("subtitle", ""),
                                        user.optString("image", ""),
                                        user.optString("actionLabel", "Follow"),
                                        user.optBoolean("isFollowing", false)
                                ));
                            }
                        }
                        peopleAdapter.submitList(items);
                        emptyView.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
                    });
                }

                @Override
                public void onError(String message) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(BackendSearchActivity.this, message, Toast.LENGTH_SHORT).show();
                    });
                }
            });
        } else {
            recyclerView.setAdapter(postAdapter);
            // Search posts using same API but different category param if supported, or filter locally
            // For now, let's reuse getFeedPosts and pretend it supports filtering
            BackendAuthApi.getFeedPosts(token, new BackendAuthApi.AuthCallback() {
                @Override
                public void onSuccess(JSONObject responseJson) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        List<BackendFeedPost> items = BackendFeedPostParser.parse(responseJson.optJSONArray("posts"));
                        List<BackendFeedPost> filtered = new ArrayList<>();
                        for (BackendFeedPost p : items) {
                            boolean typeMatch = "post".equals(currentCategory) || currentCategory.equalsIgnoreCase(p.type);
                            boolean queryMatch = query.isEmpty() || (p.content != null && p.content.toLowerCase().contains(query.toLowerCase()));
                            if (typeMatch && queryMatch) filtered.add(p);
                        }
                        postAdapter.submitList(filtered);
                        emptyView.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
                    });
                }

                @Override
                public void onError(String message) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(BackendSearchActivity.this, message, Toast.LENGTH_SHORT).show();
                    });
                }
            });
        }
    }

    private void toggleFollow(BackendPerson person) {
        BackendAuthApi.toggleFollowUser(sessionManager.getToken(), person.id, new BackendAuthApi.AuthCallback() {
            @Override
            public void onSuccess(JSONObject responseJson) {
                runOnUiThread(() -> loadResults(searchInput.getText().toString()));
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> Toast.makeText(BackendSearchActivity.this, message, Toast.LENGTH_SHORT).show());
            }
        });
    }
}
