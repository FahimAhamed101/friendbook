package com.app.myfriend.backend;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.app.myfriend.R;

import org.json.JSONException;
import org.json.JSONObject;

public class BackendEditProfileActivity extends AppCompatActivity {

    private BackendSessionManager sessionManager;
    private View progressBar;
    private EditText nameInput;
    private EditText usernameInput;
    private EditText bioInput;
    private EditText locationInput;
    private EditText linkInput;

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

        findViewById(R.id.editProfileBack).setOnClickListener(v -> finish());
        findViewById(R.id.editProfileSave).setOnClickListener(v -> saveProfile());

        loadProfile();
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
                    if (profile == null) {
                        return;
                    }
                    JSONObject user = profile.optJSONObject("user");
                    String firstName = user != null ? user.optString("firstName", "") : "";
                    String lastName = user != null ? user.optString("lastName", "") : "";
                    nameInput.setText((firstName + " " + lastName).trim());
                    usernameInput.setText(user != null ? user.optString("username", "") : "");
                    bioInput.setText(profile.optString("bio", ""));
                    locationInput.setText(profile.optString("location", ""));
                    JSONObject contact = profile.optJSONObject("contact");
                    linkInput.setText(contact != null ? contact.optString("website", "") : "");
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    TextView statusView = findViewById(R.id.editProfileStatus);
                    statusView.setVisibility(View.VISIBLE);
                    statusView.setText(message);
                });
            }
        });
    }

    private void saveProfile() {
        String token = sessionManager.getToken();
        String[] parts = nameInput.getText().toString().trim().split("\\s+", 2);
        String firstName = parts.length > 0 ? parts[0].trim() : "";
        String lastName = parts.length > 1 ? parts[1].trim() : "";

        JSONObject payload = new JSONObject();
        try {
            payload.put("firstName", firstName);
            payload.put("lastName", lastName);
            payload.put("username", usernameInput.getText().toString().trim());
            payload.put("bio", bioInput.getText().toString().trim());
            payload.put("location", locationInput.getText().toString().trim());
            payload.put("website", linkInput.getText().toString().trim());
        } catch (JSONException e) {
            Toast.makeText(this, "Could not prepare profile update.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
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
                    TextView statusView = findViewById(R.id.editProfileStatus);
                    statusView.setVisibility(View.VISIBLE);
                    statusView.setText(message);
                });
            }
        });
    }
}
