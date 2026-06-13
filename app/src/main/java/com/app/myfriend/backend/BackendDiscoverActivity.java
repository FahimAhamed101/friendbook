package com.app.myfriend.backend;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.myfriend.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class BackendDiscoverActivity extends AppCompatActivity {

    private BackendSessionManager sessionManager;
    private BackendPeopleAdapter peopleAdapter;
    private View progressBar;
    private TextView emptyView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backend_discover);

        sessionManager = new BackendSessionManager(this);
        progressBar = findViewById(R.id.discoverProgressBar);
        emptyView = findViewById(R.id.discoverEmpty);

        RecyclerView recyclerView = findViewById(R.id.discoverRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        peopleAdapter = new BackendPeopleAdapter(this::openConversationForPerson);
        recyclerView.setAdapter(peopleAdapter);

        findViewById(R.id.discoverBack).setOnClickListener(v -> finish());
        loadPeople();
    }

    private void loadPeople() {
        String token = sessionManager.getToken();
        if (token.trim().isEmpty()) {
            finish();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        BackendAuthApi.getDiscoverPeople(token, new BackendAuthApi.AuthCallback() {
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
                                    "Message"
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
                    emptyView.setVisibility(View.VISIBLE);
                    emptyView.setText(message);
                });
            }
        });
    }

    private void openConversationForPerson(BackendPerson person) {
        String token = sessionManager.getToken();
        progressBar.setVisibility(View.VISIBLE);
        BackendAuthApi.openConversation(token, person.id, new BackendAuthApi.AuthCallback() {
            @Override
            public void onSuccess(JSONObject responseJson) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Intent intent = new Intent(BackendDiscoverActivity.this, BackendChatThreadActivity.class);
                    intent.putExtra("conversationId", responseJson.optString("conversationId", ""));
                    intent.putExtra("recipientId", person.id);
                    intent.putExtra("title", person.name);
                    startActivity(intent);
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
