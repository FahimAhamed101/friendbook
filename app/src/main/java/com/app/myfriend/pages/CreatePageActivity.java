package com.app.myfriend.pages;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.app.myfriend.NightMode;
import com.app.myfriend.R;

import java.util.HashMap;

public class CreatePageActivity extends AppCompatActivity {

    private String selectedItem = "";
    private NightMode sharedPref;
    private CircularProgressIndicator progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        sharedPref = new NightMode(this);
        applyTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_page);

        progressBar = findViewById(R.id.progressBar);
        findViewById(R.id.imageView).setOnClickListener(view -> onBackPressed());

        TextView code = findViewById(R.id.code);

        findViewById(R.id.showCat).setOnClickListener(view -> {
            final String[] items = {"Business/Industry", "Entertainment", "News/Media", "Travel", "Food/Restaurants", "Shopping/Retail", "Health/Wellness", "Sports/Fitness", "Technology/Science", "Personal/Lifestyle"};

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Select a category");

            builder.setItems(items, (dialog, which) -> {
                selectedItem = items[which];
                code.setText(selectedItem);
                dialog.dismiss();
            });

            builder.create().show();
        });

        EditText name = findViewById(R.id.email);
        EditText bio = findViewById(R.id.pass);
        EditText link = findViewById(R.id.name);
        EditText username = findViewById(R.id.username);

        findViewById(R.id.signUp).setOnClickListener(view -> {
            String pageName = name.getText().toString().trim();
            String pageUsername = username.getText().toString().trim();

            if (pageName.isEmpty() || pageUsername.isEmpty()){
                Snackbar.make(view, "Enter name & username", Snackbar.LENGTH_LONG).show();
                return;
            }

            if (selectedItem.isEmpty()) {
                Snackbar.make(view, "Please select a category", Snackbar.LENGTH_LONG).show();
                return;
            }

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) {
                Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
                return;
            }

            progressBar.setVisibility(View.VISIBLE);
            view.setEnabled(false);

            String timeStamp = String.valueOf(System.currentTimeMillis());
            HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put("id", user.getUid());
            hashMap.put("pId", timeStamp);
            hashMap.put("name", pageName);
            hashMap.put("bio", bio.getText().toString().trim());
            hashMap.put("link", link.getText().toString().trim());
            hashMap.put("username", pageUsername);
            hashMap.put("photo", "");
            hashMap.put("cover", "");
            hashMap.put("cat", selectedItem);

            FirebaseDatabase.getInstance().getReference().child("Pages").child(timeStamp)
                    .setValue(hashMap)
                    .addOnSuccessListener(aVoid -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(CreatePageActivity.this, "Page Created Successfully", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(CreatePageActivity.this, PagesProfileActivity.class);
                        intent.putExtra("id", timeStamp);
                        startActivity(intent);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        progressBar.setVisibility(View.GONE);
                        view.setEnabled(true);
                        Toast.makeText(CreatePageActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });
    }

    private void applyTheme() {
        String state = sharedPref.loadNightModeState();
        if ("night".equals(state)){
            setTheme(R.style.DarkTheme);
        } else if ("dim".equals(state)){
            setTheme(R.style.DimTheme);
        } else {
            setTheme(R.style.AppTheme);
        }
    }
}
