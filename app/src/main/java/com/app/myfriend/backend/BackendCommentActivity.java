package com.app.myfriend.backend;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.myfriend.R;
import com.app.myfriend.search.SearchActivity;
import com.hendraanggrian.appcompat.widget.SocialEditText;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class BackendCommentActivity extends AppCompatActivity implements BackendFeedAdapter.FeedActionListener {

    private BackendSessionManager sessionManager;
    private String postId;
    private BackendFeedAdapter postAdapter;
    private BackendCommentAdapter commentAdapter;
    private View progressBar;
    private SocialEditText commentInput;
    private TextView topName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backend_comment);

        sessionManager = new BackendSessionManager(this);
        postId = getIntent().getStringExtra("postId");

        progressBar = findViewById(R.id.commentProgressBar);
        commentInput = findViewById(R.id.commentInput);
        topName = findViewById(R.id.commentTopName);

        RecyclerView commentRecyclerView = findViewById(R.id.commentRecyclerView);
        commentRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        commentAdapter = new BackendCommentAdapter();
        commentRecyclerView.setAdapter(commentAdapter);

        // We use the same adapter for the single post view at the top
        // or we could just manually bind it. Let's manually bind to the included layout.

        findViewById(R.id.commentBack).setOnClickListener(v -> finish());
        findViewById(R.id.commentSubmit).setOnClickListener(v -> postComment());

        findViewById(R.id.commentAddMedia).setOnClickListener(v -> Toast.makeText(this, "Media comments coming soon", Toast.LENGTH_SHORT).show());

        loadPostAndComments();
    }

    private void loadPostAndComments() {
        String token = sessionManager.getToken();
        progressBar.setVisibility(View.VISIBLE);

        BackendAuthApi.getPostById(token, postId, new BackendAuthApi.AuthCallback() {
            @Override
            public void onSuccess(JSONObject responseJson) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    JSONObject postJson = responseJson.optJSONObject("post");
                    if (postJson != null) {
                        List<BackendFeedPost> singlePostList = BackendFeedPostParser.parse(new JSONArray().put(postJson));
                        if (!singlePostList.isEmpty()) {
                            bindPost(singlePostList.get(0));
                        }

                        JSONArray commentsArray = postJson.optJSONArray("comments");
                        List<BackendCommentItem> comments = parseComments(commentsArray);
                        commentAdapter.submitList(comments);
                    }
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(BackendCommentActivity.this, message, Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }

    private void bindPost(BackendFeedPost post) {
        View postView = findViewById(R.id.commentPostContainer);
        topName.setText(post.authorName + "'s Post");

        // Manual binding logic similar to Adapter
        ((TextView)postView.findViewById(R.id.feedAuthorName)).setText(post.authorName);
        ((TextView)postView.findViewById(R.id.feedAuthorHandle)).setText(post.authorHandle);
        ((TextView)postView.findViewById(R.id.feedTime)).setText(post.published);

        com.tylersuehr.socialtextview.SocialTextView content = postView.findViewById(R.id.feedContent);
        content.setLinkText(post.content);

        ImageView authorImg = postView.findViewById(R.id.feedAuthorImage);
        if (!post.authorImage.isEmpty()) {
            Picasso.get().load(BackendAuthApi.resolveUrl(post.authorImage)).placeholder(R.drawable.avatar).into(authorImg);
        }

        ImageView postImg = postView.findViewById(R.id.feedImage);
        if (!post.image.isEmpty()) {
            postImg.setVisibility(View.VISIBLE);
            Picasso.get().load(BackendAuthApi.resolveUrl(post.image)).placeholder(R.drawable.cover).into(postImg);
        } else {
            postImg.setVisibility(View.GONE);
        }

        ((TextView)postView.findViewById(R.id.feedLikeCount)).setText(String.valueOf(post.likeCount));
        ((TextView)postView.findViewById(R.id.feedCommentCount)).setText(String.valueOf(post.commentCount));
        ((TextView)postView.findViewById(R.id.feedViewCount)).setText(String.valueOf(post.viewCount));

        // Disable interaction buttons in the header of comment activity to avoid recursion
        postView.findViewById(R.id.feedCommentButton).setAlpha(0.5f);
        postView.findViewById(R.id.feedCommentButton).setOnClickListener(null);
    }

    private List<BackendCommentItem> parseComments(JSONArray array) {
        List<BackendCommentItem> items = new ArrayList<>();
        if (array == null) return items;
        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.optJSONObject(i);
            if (obj == null) continue;
            items.add(new BackendCommentItem(
                    obj.optString("id"),
                    obj.optString("userId"),
                    obj.optString("name"),
                    obj.optString("image"),
                    obj.optString("time"),
                    obj.optString("message")
            ));
        }
        return items;
    }

    private void postComment() {
        String message = commentInput.getText().toString().trim();
        if (message.isEmpty()) return;

        progressBar.setVisibility(View.VISIBLE);
        BackendAuthApi.commentOnPost(sessionManager.getToken(), postId, message, new BackendAuthApi.AuthCallback() {
            @Override
            public void onSuccess(JSONObject responseJson) {
                runOnUiThread(() -> {
                    commentInput.setText("");
                    loadPostAndComments();
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(BackendCommentActivity.this, message, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    @Override public void onLike(BackendFeedPost post, String type) {}
    @Override public void onComment(BackendFeedPost post) {}
    @Override public void onShare(BackendFeedPost post) {}
    @Override public void onSave(BackendFeedPost post) {}
    @Override public void onAuthorClick(String authorId) {}
}
