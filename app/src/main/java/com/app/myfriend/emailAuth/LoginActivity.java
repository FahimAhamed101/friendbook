package com.app.myfriend.emailAuth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.app.myfriend.MainActivity;
import com.app.myfriend.R;
import com.app.myfriend.backend.BackendAuthApi;
import com.app.myfriend.backend.BackendSessionManager;

import org.json.JSONException;
import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {

    private BackendSessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        sessionManager = new BackendSessionManager(this);

        //Back
        findViewById(R.id.imageView).setOnClickListener(v -> onBackPressed());

        //OnClick
        findViewById(R.id.signUP).setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, SignUpActivity.class)));

        findViewById(R.id.forgot).setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class)));

        //EditText
        EditText email = findViewById(R.id.email);
        EditText pass = findViewById(R.id.pass);

        //Button
        findViewById(R.id.login).setOnClickListener(v -> {
            findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
            String mEmail = email.getText().toString().trim();
            String mPassword = pass.getText().toString().trim();
            if (mEmail.isEmpty()){
                Snackbar.make(v,"Enter your email", Snackbar.LENGTH_LONG).show();
                findViewById(R.id.progressBar).setVisibility(View.INVISIBLE);
            }else if(mPassword.isEmpty()){
                Snackbar.make(v,"Enter your password", Snackbar.LENGTH_LONG).show();
                findViewById(R.id.progressBar).setVisibility(View.INVISIBLE);
            }else {
                loginWithBackend(mEmail, mPassword);
            }
        });

    }

    private void loginWithBackend(String email, String password) {
        JSONObject payload = new JSONObject();
        try {
            payload.put("email", email);
            payload.put("password", password);
        } catch (JSONException e) {
            Snackbar.make(findViewById(R.id.login), "Could not prepare login request.", Snackbar.LENGTH_LONG).show();
            findViewById(R.id.progressBar).setVisibility(View.INVISIBLE);
            return;
        }

        BackendAuthApi.login(payload, new BackendAuthApi.AuthCallback() {
            @Override
            public void onSuccess(JSONObject responseJson) {
                runOnUiThread(() -> {
                    findViewById(R.id.progressBar).setVisibility(View.INVISIBLE);
                    String token = responseJson.optString("token", "");
                    JSONObject user = responseJson.optJSONObject("user");
                    if (token.trim().isEmpty() || user == null) {
                        Snackbar.make(findViewById(R.id.login), "Backend login succeeded but response was incomplete.", Snackbar.LENGTH_LONG).show();
                        return;
                    }

                    sessionManager.saveSession(token, user);
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    findViewById(R.id.progressBar).setVisibility(View.INVISIBLE);
                    Snackbar.make(findViewById(R.id.login), message, Snackbar.LENGTH_LONG).show();
                });
            }
        });
    }
}
