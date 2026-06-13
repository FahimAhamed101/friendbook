package com.app.myfriend.backend;

import android.os.Bundle;
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

public class BackendChatThreadActivity extends AppCompatActivity {

    private BackendSessionManager sessionManager;
    private BackendMessageAdapter messageAdapter;
    private View progressBar;
    private TextView emptyView;
    private EditText inputView;
    private String conversationId;
    private String recipientId;
    private String viewerId;
    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backend_chat_thread);

        sessionManager = new BackendSessionManager(this);
        progressBar = findViewById(R.id.chatThreadProgressBar);
        emptyView = findViewById(R.id.chatThreadEmpty);
        inputView = findViewById(R.id.chatThreadInput);
        recyclerView = findViewById(R.id.chatThreadRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        messageAdapter = new BackendMessageAdapter();
        recyclerView.setAdapter(messageAdapter);

        conversationId = getIntent().getStringExtra("conversationId");
        recipientId = getIntent().getStringExtra("recipientId");
        viewerId = sessionManager.getUser() != null ? sessionManager.getUser().optString("id", "") : "";

        ((TextView) findViewById(R.id.chatThreadTitle)).setText(getIntent().getStringExtra("title"));
        findViewById(R.id.chatThreadBack).setOnClickListener(v -> finish());
        findViewById(R.id.chatThreadSend).setOnClickListener(v -> sendMessage());

        loadMessages();
    }

    private void loadMessages() {
        String token = sessionManager.getToken();
        if (token.trim().isEmpty()) {
            finish();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        String resolvedConversationId = conversationId == null || conversationId.trim().isEmpty() ? "new" : conversationId;
        BackendAuthApi.getConversationMessages(token, resolvedConversationId, recipientId, new BackendAuthApi.AuthCallback() {
            @Override
            public void onSuccess(JSONObject responseJson) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    conversationId = responseJson.optString("conversationId", conversationId == null ? "" : conversationId);
                    JSONArray data = responseJson.optJSONArray("data");
                    List<BackendMessageItem> items = new ArrayList<>();
                    if (data != null) {
                        for (int i = 0; i < data.length(); i++) {
                            JSONObject message = data.optJSONObject(i);
                            if (message == null) {
                                continue;
                            }
                            String senderId = message.optString("senderId", "");
                            items.add(new BackendMessageItem(
                                    message.optString("id", ""),
                                    senderId,
                                    message.optString("content", ""),
                                    message.optString("createdAt", ""),
                                    senderId.equals(viewerId)
                            ));
                        }
                    }
                    messageAdapter.submitList(items);
                    emptyView.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
                    if (!items.isEmpty()) {
                        recyclerView.scrollToPosition(items.size() - 1);
                    }
                    if (conversationId != null && !conversationId.trim().isEmpty()) {
                        BackendAuthApi.markConversationRead(token, conversationId, new BackendAuthApi.AuthCallback() {
                            @Override
                            public void onSuccess(JSONObject responseJson) {
                            }

                            @Override
                            public void onError(String message) {
                            }
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

    private void sendMessage() {
        String content = inputView.getText().toString().trim();
        if (content.isEmpty()) {
            return;
        }

        String token = sessionManager.getToken();
        progressBar.setVisibility(View.VISIBLE);
        String resolvedConversationId = conversationId == null || conversationId.trim().isEmpty() ? "new" : conversationId;
        BackendAuthApi.sendMessage(token, resolvedConversationId, recipientId, content, new BackendAuthApi.AuthCallback() {
            @Override
            public void onSuccess(JSONObject responseJson) {
                runOnUiThread(() -> {
                    inputView.setText("");
                    if (conversationId == null || conversationId.trim().isEmpty()) {
                        loadOrCreateConversationIdThenRefresh();
                    } else {
                        loadMessages();
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

    private void loadOrCreateConversationIdThenRefresh() {
        String token = sessionManager.getToken();
        BackendAuthApi.openConversation(token, recipientId, new BackendAuthApi.AuthCallback() {
            @Override
            public void onSuccess(JSONObject responseJson) {
                runOnUiThread(() -> {
                    conversationId = responseJson.optString("conversationId", "");
                    loadMessages();
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
