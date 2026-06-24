package com.app.myfriend.backend;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.myfriend.R;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

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

    private CircleImageView avatarView;
    private TextView titleView;
    private TextView statusView;
    private ImageView onlineIndicator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backend_chat_thread);

        sessionManager = new BackendSessionManager(this);
        progressBar = findViewById(R.id.chatThreadProgressBar);
        emptyView = findViewById(R.id.chatThreadEmpty);
        inputView = findViewById(R.id.chatThreadInput);
        recyclerView = findViewById(R.id.chatThreadRecyclerView);

        avatarView = findViewById(R.id.chatThreadAvatar);
        titleView = findViewById(R.id.chatThreadTitle);
        statusView = findViewById(R.id.chatThreadStatus);
        onlineIndicator = findViewById(R.id.chatThreadOnlineIndicator);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        messageAdapter = new BackendMessageAdapter();
        recyclerView.setAdapter(messageAdapter);

        conversationId = getIntent().getStringExtra("conversationId");
        recipientId = getIntent().getStringExtra("recipientId");
        String initialTitle = getIntent().getStringExtra("title");
        String initialAvatar = getIntent().getStringExtra("avatar");

        titleView.setText(initialTitle != null ? initialTitle : "Chat");
        if (initialAvatar != null && !initialAvatar.isEmpty()) {
            Picasso.get().load(BackendAuthApi.resolveUrl(initialAvatar)).placeholder(R.drawable.avatar).into(avatarView);
        }

        viewerId = sessionManager.getUser() != null ? sessionManager.getUser().optString("id", "") : "";

        findViewById(R.id.chatThreadBack).setOnClickListener(v -> finish());
        findViewById(R.id.chatThreadSend).setOnClickListener(v -> sendMessage());

        findViewById(R.id.chatThreadAudioCall).setOnClickListener(v -> Toast.makeText(this, "Audio call coming soon", Toast.LENGTH_SHORT).show());
        findViewById(R.id.chatThreadVideoCall).setOnClickListener(v -> Toast.makeText(this, "Video call coming soon", Toast.LENGTH_SHORT).show());

        loadMessages();
        loadRecipientProfile();
    }

    private void loadRecipientProfile() {
        if (recipientId == null || recipientId.isEmpty()) return;

        BackendAuthApi.getProfileById(sessionManager.getToken(), recipientId, new BackendAuthApi.AuthCallback() {
            @Override
            public void onSuccess(JSONObject responseJson) {
                runOnUiThread(() -> {
                    JSONObject profile = responseJson.optJSONObject("profile");
                    if (profile != null) {
                        titleView.setText(profile.optString("fullName", titleView.getText().toString()));
                        String avatarUrl = BackendAuthApi.resolveUrl(profile.optString("avatarUrl", ""));
                        if (!avatarUrl.isEmpty()) {
                            Picasso.get().load(avatarUrl).placeholder(R.drawable.avatar).into(avatarView);
                        }
                        statusView.setText("Active now");
                        onlineIndicator.setVisibility(View.VISIBLE);
                    }
                });
            }

            @Override
            public void onError(String message) {}
        });
    }

    private void loadMessages() {
        String token = sessionManager.getToken();
        if (token.trim().isEmpty()) {
            finish();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        String resolvedConversationId = (conversationId == null || conversationId.trim().isEmpty() || "null".equals(conversationId)) ? "new" : conversationId;

        BackendAuthApi.getConversationMessages(token, resolvedConversationId, recipientId, new BackendAuthApi.AuthCallback() {
            @Override
            public void onSuccess(JSONObject responseJson) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    String newConvId = responseJson.optString("conversationId", "");
                    if (!newConvId.isEmpty()) {
                        conversationId = newConvId;
                    }

                    JSONArray data = responseJson.optJSONArray("data");
                    List<BackendMessageItem> items = new ArrayList<>();
                    if (data != null) {
                        for (int i = 0; i < data.length(); i++) {
                            JSONObject message = data.optJSONObject(i);
                            if (message == null) continue;
                            String sId = message.optString("senderId", "");
                            items.add(new BackendMessageItem(
                                    message.optString("id", ""),
                                    sId,
                                    message.optString("content", ""),
                                    message.optString("createdAt", ""),
                                    sId.equals(viewerId)
                            ));
                        }
                    }
                    messageAdapter.submitList(items);
                    emptyView.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
                    if (!items.isEmpty()) {
                        recyclerView.scrollToPosition(items.size() - 1);
                    }

                    if (conversationId != null && !conversationId.isEmpty() && !"null".equals(conversationId)) {
                        BackendAuthApi.markConversationRead(token, conversationId, new BackendAuthApi.AuthCallback() {
                            @Override public void onSuccess(JSONObject responseJson) {}
                            @Override public void onError(String message) {}
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
        if (content.isEmpty()) return;

        String token = sessionManager.getToken();
        String resolvedConversationId = (conversationId == null || conversationId.trim().isEmpty() || "null".equals(conversationId)) ? "new" : conversationId;

        BackendAuthApi.sendMessage(token, resolvedConversationId, recipientId, content, new BackendAuthApi.AuthCallback() {
            @Override
            public void onSuccess(JSONObject responseJson) {
                runOnUiThread(() -> {
                    inputView.setText("");
                    if (conversationId == null || conversationId.trim().isEmpty() || "null".equals(conversationId)) {
                        loadOrCreateConversationIdThenRefresh();
                    } else {
                        loadMessages();
                    }
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> Toast.makeText(BackendChatThreadActivity.this, message, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void loadOrCreateConversationIdThenRefresh() {
        BackendAuthApi.openConversation(sessionManager.getToken(), recipientId, new BackendAuthApi.AuthCallback() {
            @Override
            public void onSuccess(JSONObject responseJson) {
                runOnUiThread(() -> {
                    conversationId = responseJson.optString("conversationId", "");
                    loadMessages();
                });
            }
            @Override public void onError(String message) {}
        });
    }
}
