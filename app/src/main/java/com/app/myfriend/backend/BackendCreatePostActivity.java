package com.app.myfriend.backend;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.app.myfriend.R;
import com.app.myfriend.reel.VideoEditActivity;
import com.app.myfriend.send.ImageEditingActivity;

import org.json.JSONException;
import org.json.JSONObject;

public class BackendCreatePostActivity extends AppCompatActivity {

    private static final int REQUEST_PICK_IMAGE = 301;
    private static final int REQUEST_PICK_VIDEO = 302;
    private static final int REQUEST_EDIT_IMAGE = 303;
    private static final int REQUEST_EDIT_VIDEO = 304;

    private BackendSessionManager sessionManager;
    private View progressBar;
    private EditText titleInput;
    private EditText contentInput;
    private EditText imageInput;
    private TextView mediaStatusView;
    private Button pickImageButton;
    private Button pickVideoButton;
    private Uri pendingMediaUri;
    private String pendingMediaType = "";
    private String initialPostType = "custom";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backend_create_post);

        sessionManager = new BackendSessionManager(this);
        progressBar = findViewById(R.id.createPostProgressBar);
        titleInput = findViewById(R.id.createPostTitle);
        contentInput = findViewById(R.id.createPostContent);
        imageInput = findViewById(R.id.createPostImageUrl);
        mediaStatusView = findViewById(R.id.createPostMediaStatus);
        pickImageButton = findViewById(R.id.createPostPickImage);
        pickVideoButton = findViewById(R.id.createPostPickVideo);
        initialPostType = String.valueOf(getIntent().getStringExtra("initial_post_type")).trim().toLowerCase();
        if (initialPostType.isEmpty() || "null".equals(initialPostType)) {
            initialPostType = "custom";
        }

        findViewById(R.id.createPostBack).setOnClickListener(v -> finish());
        findViewById(R.id.createPostSubmit).setOnClickListener(v -> submitPost());
        pickImageButton.setOnClickListener(v -> openPicker("image/*", REQUEST_PICK_IMAGE));
        pickVideoButton.setOnClickListener(v -> openPicker("video/*", REQUEST_PICK_VIDEO));

        BackendNavigationHelper.setup(this, R.id.nav_add);
        applyComposerPreset();
    }

    private void applyComposerPreset() {
        if ("image".equals(initialPostType)) {
            mediaStatusView.setVisibility(View.VISIBLE);
            mediaStatusView.setText("Choose an image to publish to the backend feed.");
            imageInput.setHint("Image URL or pick an image below");
            titleInput.post(() -> openPicker("image/*", REQUEST_PICK_IMAGE));
            return;
        }

        if ("video".equals(initialPostType)) {
            mediaStatusView.setVisibility(View.VISIBLE);
            mediaStatusView.setText("Choose a video to publish to the backend feed.");
            imageInput.setHint("Video URL is optional when you pick a file below");
            titleInput.post(() -> openPicker("video/*", REQUEST_PICK_VIDEO));
            return;
        }

        mediaStatusView.setVisibility(View.GONE);
    }

    private void openPicker(String mimeType, int requestCode) {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType(mimeType);
        startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) {
            return;
        }

        if ((requestCode == REQUEST_PICK_IMAGE || requestCode == REQUEST_PICK_VIDEO) && data != null && data.getData() != null) {
            Uri selectedUri = data.getData();
            if (selectedUri == null) {
                return;
            }

            if (requestCode == REQUEST_PICK_IMAGE) {
                Intent editorIntent = new Intent(this, ImageEditingActivity.class);
                editorIntent.putExtra("uri", selectedUri.toString());
                editorIntent.putExtra("return_result", true);
                startActivityForResult(editorIntent, REQUEST_EDIT_IMAGE);
            } else {
                Intent editorIntent = new Intent(this, VideoEditActivity.class);
                editorIntent.putExtra("uri", selectedUri.toString());
                editorIntent.putExtra("return_result", true);
                startActivityForResult(editorIntent, REQUEST_EDIT_VIDEO);
            }
            return;
        }

        if ((requestCode == REQUEST_EDIT_IMAGE || requestCode == REQUEST_EDIT_VIDEO) && data != null) {
            String editedUri = data.getStringExtra("edited_uri");
            if (editedUri == null || editedUri.trim().isEmpty()) {
                return;
            }

            pendingMediaUri = Uri.parse(editedUri);
            pendingMediaType = requestCode == REQUEST_EDIT_VIDEO ? "video" : "image";
            mediaStatusView.setVisibility(View.VISIBLE);
            mediaStatusView.setText("Selected " + pendingMediaType + " ready for upload.");
            if ("image".equals(pendingMediaType)) {
                imageInput.setHint("Edited image will be uploaded when you publish.");
            } else {
                imageInput.setHint("Edited video will be uploaded when you publish.");
            }
        }
    }

    private void submitPost() {
        String token = sessionManager.getToken();
        if (token.trim().isEmpty()) {
            finish();
            return;
        }

        String title = titleInput.getText().toString().trim();
        String content = contentInput.getText().toString().trim();
        String imageUrl = imageInput.getText().toString().trim();

        if (title.isEmpty() && content.isEmpty() && imageUrl.isEmpty() && pendingMediaUri == null) {
            Toast.makeText(this, "Add text or media before posting.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        if (pendingMediaUri != null) {
            BackendAuthApi.uploadFile(this, token, pendingMediaUri, "upload", new BackendAuthApi.AuthCallback() {
                @Override
                public void onSuccess(JSONObject responseJson) {
                    runOnUiThread(() -> {
                        String uploadedUrl = responseJson.optString("url", "");
                        if (uploadedUrl.isEmpty()) {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(BackendCreatePostActivity.this, "Upload failed.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        publishPost(token, title, content, uploadedUrl, pendingMediaType);
                    });
                }

                @Override
                public void onError(String message) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        TextView statusView = findViewById(R.id.createPostStatus);
                        statusView.setVisibility(View.VISIBLE);
                        statusView.setText(message);
                    });
                }
            });
            return;
        }

        publishPost(token, title, content, imageUrl, imageUrl.isEmpty() ? "custom" : "image");
    }

    private void publishPost(String token, String title, String content, String mediaUrl, String mediaType) {
        JSONObject payload = new JSONObject();
        try {
            String normalizedType = "video".equals(mediaType) ? "video" : mediaUrl.isEmpty() ? "custom" : "image";
            payload.put("postType", normalizedType);
            payload.put("title", title);
            payload.put("content", content);
            if (!mediaUrl.isEmpty()) {
                if ("video".equals(normalizedType)) {
                    payload.put("attachmentUrl", mediaUrl);
                    payload.put("attachmentType", "video");
                } else {
                    payload.put("displayImageUrl", mediaUrl);
                }
            }
            payload.put("commentsOpen", true);
            payload.put("activityFeed", true);
            payload.put("myStory", false);
        } catch (JSONException e) {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Could not prepare post.", Toast.LENGTH_SHORT).show();
            return;
        }

        BackendAuthApi.createPost(token, payload, new BackendAuthApi.AuthCallback() {
            @Override
            public void onSuccess(JSONObject responseJson) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(BackendCreatePostActivity.this, "Post published.", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    TextView statusView = findViewById(R.id.createPostStatus);
                    statusView.setVisibility(View.VISIBLE);
                    statusView.setText(message);
                });
            }
        });
    }
}
