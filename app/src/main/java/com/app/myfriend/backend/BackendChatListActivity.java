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

public class BackendChatListActivity extends AppCompatActivity {

    private BackendSessionManager sessionManager;
    private BackendConversationAdapter conversationAdapter;
    private View progressBar;
    private TextView emptyView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backend_chat_list);

        sessionManager = new BackendSessionManager(this);
        progressBar = findViewById(R.id.chatListProgressBar);
        emptyView = findViewById(R.id.chatListEmpty);

        RecyclerView recyclerView = findViewById(R.id.chatListRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        conversationAdapter = new BackendConversationAdapter(this::openConversation);
        recyclerView.setAdapter(conversationAdapter);

        findViewById(R.id.chatListBack).setOnClickListener(v -> finish());
        findViewById(R.id.chatListNewChat).setOnClickListener(v -> startActivity(new Intent(this, BackendDiscoverActivity.class)));

        loadConversations();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadConversations();
    }

    private void loadConversations() {
        String token = sessionManager.getToken();
        if (token.trim().isEmpty()) {
            finish();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        BackendAuthApi.getConversations(token, new BackendAuthApi.AuthCallback() {
            @Override
            public void onSuccess(JSONObject responseJson) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    JSONArray data = responseJson.optJSONArray("data");
                    List<BackendConversationItem> items = new ArrayList<>();
                    if (data != null) {
                        for (int i = 0; i < data.length(); i++) {
                            JSONObject conversation = data.optJSONObject(i);
                            if (conversation == null) {
                                continue;
                            }
                            JSONObject participant = conversation.optJSONObject("participant");
                            items.add(new BackendConversationItem(
                                    conversation.optString("conversationId", ""),
                                    participant != null ? participant.optString("id", "") : "",
                                    participant != null ? participant.optString("name", "User") : "User",
                                    participant != null ? participant.optString("avatarUrl", "") : "",
                                    participant != null ? participant.optString("roleLabel", "") : "",
                                    conversation.optString("lastMessageText", ""),
                                    conversation.optString("lastMessageAt", ""),
                                    conversation.optInt("unreadCount", 0)
                            ));
                        }
                    }
                    conversationAdapter.submitList(items);
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

    private void openConversation(BackendConversationItem item) {
        Intent intent = new Intent(this, BackendChatThreadActivity.class);
        intent.putExtra("conversationId", item.conversationId);
        intent.putExtra("recipientId", item.participantId);
        intent.putExtra("title", item.participantName);
        startActivity(intent);
    }
}
