package com.app.myfriend.emailAuth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.app.myfriend.R;
import com.app.myfriend.backend.BackendAuthApi;
import com.app.myfriend.backend.BackendHomeActivity;
import com.app.myfriend.backend.BackendSessionManager;
import com.app.myfriend.menu.PrivacyActivity;
import com.app.myfriend.menu.TermsActivity;

import org.json.JSONException;
import org.json.JSONObject;

@SuppressWarnings("ALL")
public class SignUpActivity extends AppCompatActivity {

    private BackendSessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        sessionManager = new BackendSessionManager(this);

        //Back
        findViewById(R.id.imageView).setOnClickListener(v -> onBackPressed());

        //Text
        findViewById(R.id.login).setOnClickListener(v -> startActivity(new Intent(SignUpActivity.this, LoginActivity.class)));

        TextView terms = findViewById(R.id.terms);
        TextView privacy = findViewById(R.id.privacy);

        privacy.setOnClickListener(v -> startActivity(new Intent(SignUpActivity.this, PrivacyActivity.class)));

        terms.setOnClickListener(v -> startActivity(new Intent(SignUpActivity.this, TermsActivity.class)));

        //EditText
        EditText email = findViewById(R.id.email);
        EditText pass = findViewById(R.id.pass);
        EditText name = findViewById(R.id.name);
        EditText username = findViewById(R.id.username);
        CheckBox checkBox = findViewById(R.id.checkbox);
        EditText code = findViewById(R.id.code);

        //Button
        findViewById(R.id.signUp).setOnClickListener(v -> {
            findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
            String mEmail = email.getText().toString().trim();
            String mPassword = pass.getText().toString().trim();
            String mName = name.getText().toString().trim();
            String mUsername = username.getText().toString().trim();

            if(!checkBox.isChecked()){
                Snackbar.make(v,"Agree to privacy policy & Terms & Conditions", Snackbar.LENGTH_LONG).show();
                findViewById(R.id.progressBar).setVisibility(View.INVISIBLE);
            }else
            if (mEmail.isEmpty()){
                Snackbar.make(v,"Enter your email", Snackbar.LENGTH_LONG).show();
                findViewById(R.id.progressBar).setVisibility(View.INVISIBLE);
            }else if(mPassword.isEmpty()){
                Snackbar.make(v,"Enter your password", Snackbar.LENGTH_LONG).show();
                findViewById(R.id.progressBar).setVisibility(View.INVISIBLE);
            }else if(mName.isEmpty()){
                Snackbar.make(v,"Enter your Name", Snackbar.LENGTH_LONG).show();
                findViewById(R.id.progressBar).setVisibility(View.INVISIBLE);
            }else if(mUsername.isEmpty()){
                Snackbar.make(v,"Enter your Username", Snackbar.LENGTH_LONG).show();
                findViewById(R.id.progressBar).setVisibility(View.INVISIBLE);
            } else if (mPassword.length()<6){
                Snackbar.make(v,"Password should have minimum 6 characters", Snackbar.LENGTH_LONG).show();
                findViewById(R.id.progressBar).setVisibility(View.INVISIBLE);
            }else {
                register(mEmail,mPassword,mName,mUsername);
            }

        });

    }

    private void register(String mEmail, String mPassword, String mName, String mUsername) {
        String[] parts = mName.trim().split("\\s+", 2);
        String firstName = parts.length > 0 ? parts[0].trim() : "";
        String lastName = parts.length > 1 ? parts[1].trim() : "";

        JSONObject payload = new JSONObject();
        try {
            payload.put("name", mName);
            payload.put("firstName", firstName);
            if (!lastName.isEmpty()) {
                payload.put("lastName", lastName);
            }
            payload.put("email", mEmail);
            payload.put("password", mPassword);
            payload.put("username", mUsername);
            payload.put("termsAccepted", true);
        } catch (JSONException e) {
            findViewById(R.id.progressBar).setVisibility(View.INVISIBLE);
            Snackbar.make(findViewById(R.id.signUp), "Could not prepare signup request.", Snackbar.LENGTH_LONG).show();
            return;
        }

        BackendAuthApi.signup(payload, new BackendAuthApi.AuthCallback() {
            @Override
            public void onSuccess(JSONObject responseJson) {
                runOnUiThread(() -> {
                    findViewById(R.id.progressBar).setVisibility(View.INVISIBLE);
                    String token = responseJson.optString("token", "");
                    JSONObject user = responseJson.optJSONObject("user");
                    if (token.trim().isEmpty() || user == null) {
                        Snackbar.make(findViewById(R.id.signUp), "Backend signup succeeded but response was incomplete.", Snackbar.LENGTH_LONG).show();
                        return;
                    }

                    sessionManager.saveSession(token, user);
                    Intent intent = new Intent(SignUpActivity.this, BackendHomeActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK| Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    findViewById(R.id.progressBar).setVisibility(View.INVISIBLE);
                    Snackbar.make(findViewById(R.id.signUp), message, Snackbar.LENGTH_LONG).show();
                });
            }
        });

    }
}
