package com.app.myfriend.backend;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.app.myfriend.R;
import com.app.myfriend.send.ImageEditingActivity;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import de.hdodenhof.circleimageview.CircleImageView;

public class BackendEditProfileActivity extends AppCompatActivity {

    private static final int REQUEST_PICK_AVATAR = 501;
    private static final int REQUEST_PICK_COVER = 502;
    private static final int REQUEST_EDIT_AVATAR = 503;
    private static final int REQUEST_EDIT_COVER = 504;

    private BackendSessionManager sessionManager;
    private View progressBar;
    private EditText nameInput;
    private EditText usernameInput;
    private EditText bioInput;
    private EditText locationInput;
    private EditText linkInput;
    private CircleImageView avatarPreview;
    private ImageView coverPreview;

    private Uri pendingAvatarUri;
    private Uri pendingCoverUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backend_edit_profile);

        sessionManager = new BackendSessionManager(this);
        progressBar = findViewById(R.id.editProfileProgressBar);
        nameInput = findViewById(R.id.editProfileName);
        usernameInput = findViewById(R.id.editProfileUsername);
        bioInput = findViewById(R.id.editProfileBio);
        locationInput = findViewById(R.id.editProfileLocation);
        linkInput = findViewById(R.id.editProfileLink);
        avatarPreview = findViewById(R.id.editProfileAvatar);
        coverPreview = findViewById(R.id.editProfileCover);

        findViewById(R.id.editProfileBack).setOnClickListener(v -> finish());
        findViewById(R.id.editProfileSave).setOnClickListener(v -> saveProfile());

        findViewById(R.id.editProfileChangeAvatar).setOnClickListener(v -> openPicker(REQUEST_PICK_AVATAR));
        findViewById(R.id.editProfileChangeCover).setOnClickListener(v -> openPicker(REQUEST_PICK_COVER));

        loadProfile();
    }

    private void openPicker(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) return;

        Uri selectedUri = data.getData();
        if (selectedUri == null) return;

        if (requestCode == REQUEST_PICK_AVATAR || requestCode == REQUEST_PICK_COVER) {
            Intent intent = new Intent(this, ImageEditingActivity.class);
            intent.setData(selectedUri);
            intent.putExtra("uri", selectedUri.toString());
            intent.putExtra("return_result", true);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(intent, requestCode == REQUEST_PICK_AVATAR ? REQUEST_EDIT_AVATAR : REQUEST_EDIT_COVER);
        } else if (requestCode == REQUEST_EDIT_AVATAR) {
            String editedUri = data.getStringExtra("edited_uri");
            if (editedUri != null) {
                pendingAvatarUri = Uri.parse(editedUri);
                Picasso.get().load(pendingAvatarUri).into(avatarPreview);
            }
        } else if (requestCode == REQUEST_EDIT_COVER) {
            String editedUri = data.getStringExtra("edited_uri");
            if (editedUri != null) {
                pendingCoverUri = Uri.parse(editedUri);
                Picasso.get().load(pendingCoverUri).into(coverPreview);
            }
        }
    }

    private void loadProfile() {
        String token = sessionManager.getToken();
        if (token.trim().isEmpty()) {
            finish();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        BackendAuthApi.getMyProfile(token, new BackendAuthApi.AuthCallback() {
            @Override
            public void onSuccess(JSONObject responseJson) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    JSONObject profile = responseJson.optJSONObject("profile");
                    if (profile == null) return;

                    JSONObject user = profile.optJSONObject("user");
                    String firstName = user != null ? user.optString("firstName", "") : "";
                    String lastName = user != null ? user.optString("lastName", "") : "";
                    nameInput.setText((firstName + " " + lastName).trim());
                    usernameInput.setText(user != null ? user.optString("username", "") : "");
                    bioInput.setText(profile.optString("bio", ""));
                    locationInput.setText(profile.optString("location", ""));

                    JSONObject contact = profile.optJSONObject("contact");
                    if (contact != null) {
                        linkInput.setText(contact.optString("website", ""));
                    }

                    String avatarUrl = BackendAuthApi.resolveUrl(profile.optString("avatarUrl", ""));
                    if (!avatarUrl.isEmpty()) {
                        Picasso.get().load(avatarUrl).placeholder(R.drawable.avatar).into(avatarPreview);
                    }

                    String coverUrl = BackendAuthApi.resolveUrl(profile.optString("coverUrl", ""));
                    if (!coverUrl.isEmpty()) {
                        Picasso.get().load(coverUrl).placeholder(R.drawable.cover).into(coverPreview);
                    }
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(BackendEditProfileActivity.this, message, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void saveProfile() {
        String token = sessionManager.getToken();
        String[] parts = nameInput.getText().toString().trim().split("\\s+", 2);
        String firstName = parts.length > 0 ? parts[0].trim() : "";
        String lastName = parts.length > 1 ? parts[1].trim() : "";

        progressBar.setVisibility(View.VISIBLE);

        if (pendingAvatarUri != null) {
            uploadAndSave(token, firstName, lastName, "avatar");
        } else if (pendingCoverUri != null) {
            uploadAndSave(token, firstName, lastName, "cover");
        } else {
            performUpdate(token, firstName, lastName, null, null);
        }
    }

    private void uploadAndSave(String token, String firstName, String lastName, String type) {
        Uri uploadUri = type.equals("avatar") ? pendingAvatarUri : pendingCoverUri;
        BackendAuthApi.uploadFile(this, token, uploadUri, type, new BackendAuthApi.AuthCallback() {
            @Override
            public void onSuccess(JSONObject responseJson) {
                String url = responseJson.optString("url", "");
                runOnUiThread(() -> {
                    if (type.equals("avatar")) {
                        pendingAvatarUri = null; // Mark as done
                        if (pendingCoverUri != null) {
                            uploadAndSave(token, firstName, lastName, "cover");
                        } else {
                            performUpdate(token, firstName, lastName, url, null);
                        }
                    } else {
                        pendingCoverUri = null; // Mark as done
                        performUpdate(token, firstName, lastName, null, url);
                    }
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(BackendEditProfileActivity.this, type + " upload failed: " + message, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void performUpdate(String token, String firstName, String lastName, String avatarUrl, String coverUrl) {
        JSONObject payload = new JSONObject();
        try {
            payload.put("firstName", firstName);
            payload.put("lastName", lastName);
            payload.put("username", usernameInput.getText().toString().trim());
            payload.put("bio", bioInput.getText().toString().trim());
            payload.put("location", locationInput.getText().toString().trim());
            payload.put("website", linkInput.getText().toString().trim());
            if (avatarUrl != null) payload.put("avatarUrl", avatarUrl);
            if (coverUrl != null) payload.put("coverUrl", coverUrl);
        } catch (JSONException e) {
            return;
        }

        BackendAuthApi.updateMyProfile(token, payload, new BackendAuthApi.AuthCallback() {
            @Override
            public void onSuccess(JSONObject responseJson) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(BackendEditProfileActivity.this, "Profile updated.", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(BackendEditProfileActivity.this, message, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}
