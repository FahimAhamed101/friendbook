package com.app.myfriend;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.app.myfriend.backend.BackendSessionManager;
import com.app.myfriend.welcome.IntroLast;

public class Check extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
        BackendSessionManager backendSessionManager = new BackendSessionManager(this);
        if (backendSessionManager.isLoggedIn()) {
            startActivity(new Intent(Check.this, MainActivity.class));
            finish();
            return;
        }

        startActivity(new Intent(Check.this, IntroLast.class));
        finish();
    }

}
