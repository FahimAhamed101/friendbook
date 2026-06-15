package com.app.myfriend.backend;

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

public class BackendNotificationsActivity extends AppCompatActivity {

    private BackendSessionManager sessionManager;
    private BackendNotificationAdapter adapter;
    private View progressBar;
    private TextView emptyView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backend_notifications);

        sessionManager = new BackendSessionManager(this);
        progressBar = findViewById(R.id.notificationsProgressBar);
        emptyView = findViewById(R.id.notificationsEmpty);

        RecyclerView recyclerView = findViewById(R.id.notificationsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new BackendNotificationAdapter();
        recyclerView.setAdapter(adapter);

        findViewById(R.id.notificationsBack).setOnClickListener(v -> finish());

        loadNotifications();
    }

    private void loadNotifications() {
        String token = sessionManager.getToken();
        if (token.trim().isEmpty()) {
            finish();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        BackendAuthApi.getMyProfile(token, new BackendAuthApi.AuthCallback() {
            @Override
            public void onSuccess(JSONObject responseJson) {
                BackendAuthApi.getConversations(token, new BackendAuthApi.AuthCallback() {
                    @Override
                    public void onSuccess(JSONObject chatJson) {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            List<BackendNotificationItem> items = new ArrayList<>();

                            JSONArray comments = responseJson.optJSONArray("comments");
                            if (comments != null) {
                                for (int i = 0; i < comments.length(); i++) {
                                    JSONObject comment = comments.optJSONObject(i);
                                    if (comment == null) {
                                        continue;
                                    }
                                    items.add(new BackendNotificationItem(
                                            comment.optString("name", "Comment"),
                                            comment.optString("message", ""),
                                            comment.optString("time", ""),
                                            comment.optString("image", "")
                                    ));
                                }
                            }

                            JSONArray conversations = chatJson.optJSONArray("data");
                            if (conversations != null) {
                                for (int i = 0; i < conversations.length(); i++) {
                                    JSONObject conversation = conversations.optJSONObject(i);
                                    if (conversation == null) {
                                        continue;
                                    }
                                    JSONObject participant = conversation.optJSONObject("participant");
                                    int unreadCount = conversation.optInt("unreadCount", 0);
                                    if (unreadCount <= 0) {
                                        continue;
                                    }
                                    items.add(new BackendNotificationItem(
                                            participant != null ? participant.optString("name", "New message") : "New message",
                                            conversation.optString("lastMessageText", "You have unread messages."),
                                            unreadCount + " unread messages",
                                            participant != null ? participant.optString("avatarUrl", "") : ""
                                    ));
                                }
                            }

                            adapter.submitList(items);
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
