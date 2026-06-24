package com.app.myfriend.welcome;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import com.app.myfriend.R;
import com.app.myfriend.emailAuth.LoginActivity;
import com.app.myfriend.menu.PrivacyActivity;
import com.app.myfriend.menu.TermsActivity;
import com.app.myfriend.phoneAuth.GenerateOTPActivity;

@SuppressWarnings("ALL")
public class IntroLast extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro_last);

        Button email = findViewById(R.id.next);
        Button phone = findViewById(R.id.phone);

        email.setOnClickListener(v -> startActivity( new Intent(getApplicationContext(), LoginActivity.class )));

        phone.setOnClickListener(v -> startActivity(new Intent(getApplicationContext(), GenerateOTPActivity.class )));

        TextView terms = findViewById(R.id.terms);
        TextView privacy = findViewById(R.id.privacy);

        privacy.setOnClickListener(v -> {
            startActivity(new Intent(IntroLast.this, PrivacyActivity.class));
        });

        terms.setOnClickListener(v -> {
            startActivity(new Intent(IntroLast.this, TermsActivity.class));
        });

    }
}