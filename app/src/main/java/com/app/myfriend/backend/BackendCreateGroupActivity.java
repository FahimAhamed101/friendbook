package com.app.myfriend.backend;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.app.myfriend.NightMode;
import com.app.myfriend.R;
import com.app.myfriend.send.ImageEditingActivity;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

public class BackendCreateGroupActivity extends AppCompatActivity {

    private static final int REQUEST_PICK_ICON = 401;
    private static final int REQUEST_EDIT_ICON = 402;

    private BackendSessionManager sessionManager;
    private View progressBar;
    private EditText nameInput, descriptionInput, categoryInput;
    private ImageView iconPreview;
    private Uri pendingIconUri;
    private NightMode nightMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        nightMode = new NightMode(this);
        applyTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backend_create_group);

        sessionManager = new BackendSessionManager(this);
        progressBar = findViewById(R.id.createGroupProgressBar);
        nameInput = findViewById(R.id.createGroupName);
        descriptionInput = findViewById(R.id.createGroupDescription);
        categoryInput = findViewById(R.id.createGroupCategory);
        iconPreview = findViewById(R.id.createGroupIconPreview);

        findViewById(R.id.createGroupBack).setOnClickListener(v -> finish());
        findViewById(R.id.createGroupIconCard).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, REQUEST_PICK_ICON);
        });

        findViewById(R.id.createGroupSubmit).setOnClickListener(v -> createGroup());
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
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) return;

        if (requestCode == REQUEST_PICK_ICON) {
            Uri selectedUri = data.getData();
            if (selectedUri != null) {
                Intent intent = new Intent(this, ImageEditingActivity.class);
                intent.setData(selectedUri);
                intent.putExtra("uri", selectedUri.toString());
                intent.putExtra("return_result", true);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                startActivityForResult(intent, REQUEST_EDIT_ICON);
            }
        } else if (requestCode == REQUEST_EDIT_ICON) {
            String editedUri = data.getStringExtra("edited_uri");
            if (editedUri != null) {
                pendingIconUri = Uri.parse(editedUri);
                Picasso.get().load(pendingIconUri).into(iconPreview);
            }
        }
    }

    private void createGroup() {
        String name = nameInput.getText().toString().trim();
        String description = descriptionInput.getText().toString().trim();
        String category = categoryInput.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(this, "Group name is required", Toast.LENGTH_SHORT).show();
            return;
        }

        String token = sessionManager.getToken();
        progressBar.setVisibility(View.VISIBLE);

        if (pendingIconUri != null) {
            BackendAuthApi.uploadFile(this, token, pendingIconUri, "group_icon", new BackendAuthApi.AuthCallback() {
                @Override
                public void onSuccess(JSONObject responseJson) {
                    runOnUiThread(() -> submitGroup(token, name, description, category, responseJson.optString("url", "")));
                }

                @Override
                public void onError(String message) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(BackendCreateGroupActivity.this, "Icon upload failed: " + message, Toast.LENGTH_SHORT).show();
                    });
                }
            });
        } else {
            submitGroup(token, name, description, category, "");
        }
    }

    private void submitGroup(String token, String name, String description, String category, String iconUrl) {
        JSONObject payload = new JSONObject();
        try {
            payload.put("name", name);
            payload.put("description", description);
            payload.put("category", category);
            if (!iconUrl.isEmpty()) {
                payload.put("iconUrl", iconUrl);
            }
        } catch (JSONException e) {
            return;
        }

        BackendAuthApi.createGroup(token, payload, new BackendAuthApi.AuthCallback() {
            @Override
            public void onSuccess(JSONObject responseJson) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(BackendCreateGroupActivity.this, "Group created!", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(BackendCreateGroupActivity.this, message, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}
