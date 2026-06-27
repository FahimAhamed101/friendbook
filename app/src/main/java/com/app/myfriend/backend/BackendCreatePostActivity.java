package com.app.myfriend.backend;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.app.myfriend.NightMode;
import com.app.myfriend.R;
import com.app.myfriend.reel.VideoEditActivity;
import com.app.myfriend.send.ImageEditingActivity;
import com.hendraanggrian.appcompat.widget.SocialEditText;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import de.hdodenhof.circleimageview.CircleImageView;

public class BackendCreatePostActivity extends AppCompatActivity {

    private static final int REQUEST_PICK_IMAGE = 301;
    private static final int REQUEST_PICK_VIDEO = 302;
    private static final int REQUEST_EDIT_IMAGE = 303;
    private static final int REQUEST_EDIT_VIDEO = 304;

    private BackendSessionManager sessionManager;
    private View progressBar;
    private EditText titleInput;
    private SocialEditText contentInput;
    private TextView locationLabel;
    private ImageView imagePreview;
    private View mediaPreviewContainer;
    private CircleImageView avatarView;
    private TextView authorNameView;

    private Uri pendingMediaUri;
    private String pendingMediaType = "";
    private String selectedFeeling = "";
    private String selectedLocation = "";
    private String initialPostType = "custom";
    private boolean isPresetApplied = false;
    private NightMode nightMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        nightMode = new NightMode(this);
        applyTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backend_create_post);

        sessionManager = new BackendSessionManager(this);
        progressBar = findViewById(R.id.createPostProgressBar);
        titleInput = findViewById(R.id.createPostTitle);
        contentInput = findViewById(R.id.createPostContent);
        locationLabel = findViewById(R.id.createPostLocationLabel);
        imagePreview = findViewById(R.id.createPostImagePreview);
        mediaPreviewContainer = findViewById(R.id.createPostMediaPreviewContainer);
        avatarView = findViewById(R.id.createPostAvatar);
        authorNameView = findViewById(R.id.createPostAuthorName);

        if (savedInstanceState != null) {
            isPresetApplied = savedInstanceState.getBoolean("isPresetApplied", false);
        }

        initialPostType = String.valueOf(getIntent().getStringExtra("initial_post_type")).trim().toLowerCase();
        if (initialPostType.isEmpty() || "null".equals(initialPostType)) {
            initialPostType = "custom";
        }

        setupUserHeader();

        findViewById(R.id.createPostBack).setOnClickListener(v -> finish());
        findViewById(R.id.createPostSubmit).setOnClickListener(v -> submitPost());
        findViewById(R.id.createPostPickImage).setOnClickListener(v -> openPicker("image/*", REQUEST_PICK_IMAGE));
        findViewById(R.id.createPostPickVideo).setOnClickListener(v -> openPicker("video/*", REQUEST_PICK_VIDEO));
        findViewById(R.id.createPostCancelMedia).setOnClickListener(v -> clearMedia());
        findViewById(R.id.createPostFeeling).setOnClickListener(v -> showFeelingDialog());
        findViewById(R.id.createPostLocationLabel).setOnClickListener(v -> showLocationDialog());

        if (!isPresetApplied) {
            applyComposerPreset();
            isPresetApplied = true;
        }
    }

    private void applyTheme() {
        String state = nightMode.loadNightModeState();
        if ("night".equalsIgnoreCase(state)) {
            setTheme(R.style.DarkTheme);
        } else if ("dim".equalsIgnoreCase(state)) {
            setTheme(R.style.DimTheme);
        } else {
            setTheme(R.style.AppTheme);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("isPresetApplied", isPresetApplied);
    }

    private void setupUserHeader() {
        JSONObject user = sessionManager.getUser();
        if (user != null) {
            String firstName = user.optString("firstName", "");
            String lastName = user.optString("lastName", "");
            authorNameView.setText(firstName + " " + lastName);
            String avatarUrl = BackendAuthApi.resolveUrl(user.optString("avatarUrl", ""));
            if (!avatarUrl.isEmpty()) {
                Picasso.get().load(avatarUrl).placeholder(R.drawable.avatar).into(avatarView);
            }
        }
    }

    private void showFeelingDialog() {
        String[] feelings = {"Happy", "Loved", "Sad", "Angry", "Excited", "Tired", "Blessed"};
        new AlertDialog.Builder(this)
                .setTitle("How are you feeling?")
                .setItems(feelings, (dialog, which) -> {
                    selectedFeeling = feelings[which];
                    Toast.makeText(this, "Feeling " + selectedFeeling, Toast.LENGTH_SHORT).show();
                }).show();
    }

    private void showLocationDialog() {
        EditText input = new EditText(this);
        input.setHint("Enter location");
        new AlertDialog.Builder(this)
                .setTitle("Add Location")
                .setView(input)
                .setPositiveButton("Add", (dialog, which) -> {
                    selectedLocation = input.getText().toString().trim();
                    locationLabel.setText(selectedLocation.isEmpty() ? "Add location" : selectedLocation);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void applyComposerPreset() {
        if ("image".equals(initialPostType)) {
            openPicker("image/*", REQUEST_PICK_IMAGE);
        } else if ("video".equals(initialPostType)) {
            openPicker("video/*", REQUEST_PICK_VIDEO);
        }
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
            if (requestCode == REQUEST_EDIT_IMAGE || requestCode == REQUEST_EDIT_VIDEO) {
                Toast.makeText(this, "Editor cancelled", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        if (requestCode == REQUEST_PICK_IMAGE || requestCode == REQUEST_PICK_VIDEO) {
            if (data == null) return;
            Uri selectedUri = data.getData();
            if (selectedUri == null) return;

            if (requestCode == REQUEST_PICK_IMAGE) {
                Intent editorIntent = new Intent(this, ImageEditingActivity.class);
                editorIntent.setData(selectedUri);
                editorIntent.putExtra("uri", selectedUri.toString());
                editorIntent.putExtra("return_result", true);
                editorIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivityForResult(editorIntent, REQUEST_EDIT_IMAGE);
            } else {
                Intent editorIntent = new Intent(this, VideoEditActivity.class);
                editorIntent.setData(selectedUri);
                editorIntent.putExtra("uri", selectedUri.toString());
                editorIntent.putExtra("return_result", true);
                editorIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivityForResult(editorIntent, REQUEST_EDIT_VIDEO);
            }
        } else if (requestCode == REQUEST_EDIT_IMAGE || requestCode == REQUEST_EDIT_VIDEO) {
            if (data == null) return;
            String editedUri = data.getStringExtra("edited_uri");
            if (editedUri != null) {
                pendingMediaUri = Uri.parse(editedUri);
                pendingMediaType = requestCode == REQUEST_EDIT_VIDEO ? "video" : "image";
                mediaPreviewContainer.setVisibility(View.VISIBLE);
                if ("image".equals(pendingMediaType)) {
                    Picasso.get().load(pendingMediaUri).into(imagePreview);
                } else {
                    imagePreview.setImageResource(R.drawable.cover); // Fallback for video thumbnail
                }
            }
        }
    }

    private void clearMedia() {
        pendingMediaUri = null;
        pendingMediaType = "";
        mediaPreviewContainer.setVisibility(View.GONE);
    }

    private void submitPost() {
        String token = sessionManager.getToken();
        String title = titleInput.getText().toString().trim();
        String content = contentInput.getText().toString().trim();

        if (title.isEmpty() && content.isEmpty() && pendingMediaUri == null) {
            Toast.makeText(this, "Add text or media before posting.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        if (pendingMediaUri != null) {
            BackendAuthApi.uploadFile(this, token, pendingMediaUri, "upload", new BackendAuthApi.AuthCallback() {
                @Override
                public void onSuccess(JSONObject responseJson) {
                    runOnUiThread(() -> publishPost(token, title, content, responseJson.optString("url", ""), pendingMediaType));
                }

                @Override
                public void onError(String message) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(BackendCreatePostActivity.this, message, Toast.LENGTH_SHORT).show();
                    });
                }
            });
        } else {
            publishPost(token, title, content, "", "custom");
        }
    }

    private void publishPost(String token, String title, String content, String mediaUrl, String mediaType) {
        JSONObject payload = new JSONObject();
        try {
            payload.put("postType", mediaType);
            payload.put("title", title);
            payload.put("content", content);
            payload.put("feeling", selectedFeeling);
            payload.put("location", selectedLocation);
            if (!mediaUrl.isEmpty()) {
                if ("video".equals(mediaType)) {
                    payload.put("attachmentUrl", mediaUrl);
                    payload.put("attachmentType", "video");
                } else {
                    payload.put("displayImageUrl", mediaUrl);
                    payload.put("attachmentUrl", mediaUrl);
                    payload.put("attachmentType", "image");
                }
            }
            payload.put("commentsOpen", true);
        } catch (JSONException e) {
            return;
        }

        BackendAuthApi.createPost(token, payload, new BackendAuthApi.AuthCallback() {
            @Override
            public void onSuccess(JSONObject responseJson) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(BackendCreatePostActivity.this, "Post published!", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(BackendCreatePostActivity.this, message, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}
