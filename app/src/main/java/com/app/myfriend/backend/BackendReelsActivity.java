package com.app.myfriend.backend;

import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.app.myfriend.R;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class BackendReelsActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private BackendReelAdapter adapter;
    private BackendSessionManager sessionManager;
    private View progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Full screen for reels
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.fragment_reel);

        sessionManager = new BackendSessionManager(this);
        viewPager = findViewById(R.id.videoPager);
        progressBar = findViewById(R.id.no); // Reusing the 'no' view as a loading indicator or just hide it

        adapter = new BackendReelAdapter(this);
        viewPager.setAdapter(adapter);
        viewPager.setOrientation(ViewPager2.ORIENTATION_VERTICAL);

        findViewById(R.id.tabLayout).setVisibility(View.GONE); // Simplify for now

        BackendNavigationHelper.setup(this, R.id.nav_reels);
        loadReels();
    }

    private void loadReels() {
        String token = sessionManager.getToken();
        if (token.isEmpty()) return;

        BackendAuthApi.getReels(token, new BackendAuthApi.AuthCallback() {
            @Override
            public void onSuccess(JSONObject responseJson) {
                runOnUiThread(() -> {
                    List<BackendFeedPost> posts = BackendFeedPostParser.parse(responseJson.optJSONArray("posts"));
                    List<BackendFeedPost> videoPosts = new ArrayList<>();
                    for (BackendFeedPost post : posts) {
                        if ("video".equalsIgnoreCase(post.type) || post.attachmentUrl != null && post.attachmentUrl.contains(".mp4")) {
                            videoPosts.add(post);
                        }
                    }
                    adapter.submitList(videoPosts);
                    progressBar.setVisibility(videoPosts.isEmpty() ? View.VISIBLE : View.GONE);
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    Toast.makeText(BackendReelsActivity.this, message, Toast.LENGTH_SHORT).show();
                    progressBar.setVisibility(View.VISIBLE);
                });
            }
        });
    }
}
