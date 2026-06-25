package com.app.myfriend.backend;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import com.app.myfriend.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;

public final class BackendNavigationHelper {

    private BackendNavigationHelper() {
    }

    public static void setup(Activity activity, int selectedItemId) {
        BottomNavigationView bottomNavigationView = activity.findViewById(R.id.backendBottomNavigation);
        if (bottomNavigationView == null) {
            return;
        }

        bottomNavigationView.setSelectedItemId(selectedItemId);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_add) {
                showAddOptions(activity);
                return false;
            }

            if (itemId == selectedItemId) {
                return true;
            }

            Intent intent = null;
            if (itemId == R.id.nav_home) {
                intent = new Intent(activity, BackendHomeActivity.class);
            } else if (itemId == R.id.nav_reels) {
                intent = new Intent(activity, BackendReelsActivity.class);
            } else if (itemId == R.id.nav_chat) {
                intent = new Intent(activity, BackendChatListActivity.class);
            } else if (itemId == R.id.nav_user) {
                intent = new Intent(activity, BackendMenuActivity.class);
            }

            if (intent == null) {
                return false;
            }

            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            activity.startActivity(intent);
            activity.overridePendingTransition(0, 0);
            return true;
        });
    }

    public static void showAddOptions(Activity activity) {
        BottomSheetDialog more = new BottomSheetDialog(activity);
        @SuppressLint("InflateParams")
        View view = LayoutInflater.from(activity).inflate(R.layout.add_bottom, null);

        view.findViewById(R.id.post).setOnClickListener(v -> {
            more.dismiss();
            openComposer(activity, "custom");
        });

        view.findViewById(R.id.reel).setOnClickListener(v -> {
            more.dismiss();
            openComposer(activity, "video");
        });

        view.findViewById(R.id.stories).setOnClickListener(v -> {
            more.dismiss();
            openComposer(activity, "custom");
        });

        view.findViewById(R.id.live).setOnClickListener(v -> {
            more.dismiss();
            openComposer(activity, "custom");
        });

        View.OnClickListener comingSoon = v -> Toast.makeText(activity, "Coming soon to backend...", Toast.LENGTH_SHORT).show();

        view.findViewById(R.id.party).setOnClickListener(comingSoon);
        view.findViewById(R.id.pages).setOnClickListener(comingSoon);
        view.findViewById(R.id.translation).setOnClickListener(comingSoon);
        view.findViewById(R.id.referral).setOnClickListener(comingSoon);
        view.findViewById(R.id.camera).setOnClickListener(comingSoon);
        view.findViewById(R.id.meeting).setOnClickListener(comingSoon);
        view.findViewById(R.id.sell).setOnClickListener(comingSoon);
        view.findViewById(R.id.podcast).setOnClickListener(comingSoon);

        more.setContentView(view);
        more.show();
    }

    private static void openComposer(Activity activity, String type) {
        Intent intent = new Intent(activity, BackendCreatePostActivity.class);
        intent.putExtra("initial_post_type", type);
        activity.startActivity(intent);
    }
}
