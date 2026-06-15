package com.app.myfriend.backend;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
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
    private View progressBar;
    private TextView emptyView;
    private EditText searchInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backend_search);

        sessionManager = new BackendSessionManager(this);
        progressBar = findViewById(R.id.searchProgressBar);
        emptyView = findViewById(R.id.searchEmpty);
        searchInput = findViewById(R.id.searchInput);

        RecyclerView recyclerView = findViewById(R.id.searchRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        peopleAdapter = new BackendPeopleAdapter(new BackendPeopleAdapter.OnPersonClickListener() {
            @Override
            public void onPersonClicked(BackendPerson person) {
                Intent intent = new Intent(BackendSearchActivity.this, BackendUserProfileActivity.class);
                intent.putExtra("userId", person.id);
                intent.putExtra("title", person.name);
                startActivity(intent);
            }

            @Override
            public void onActionClicked(BackendPerson person) {
                toggleFollow(person);
            }
        });
        recyclerView.setAdapter(peopleAdapter);

        findViewById(R.id.searchBack).setOnClickListener(v -> finish());
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                loadResults(s == null ? "" : s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        loadResults("");
    }

    private void loadResults(String query) {
        String token = sessionManager.getToken();
        if (token.trim().isEmpty()) {
            finish();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
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
                            if (user == null) {
                                continue;
                            }

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
                    if (items.isEmpty()) {
                        emptyView.setText("No people found.");
                    }
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

    private void toggleFollow(BackendPerson person) {
        if (person == null || person.id == null || person.id.trim().isEmpty()) {
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        BackendAuthApi.toggleFollowUser(sessionManager.getToken(), person.id, new BackendAuthApi.AuthCallback() {
            @Override
            public void onSuccess(JSONObject responseJson) {
                runOnUiThread(() -> loadResults(searchInput.getText().toString()));
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
