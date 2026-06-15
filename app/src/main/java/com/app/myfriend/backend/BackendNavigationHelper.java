package com.app.myfriend.backend;

import android.app.Activity;
import android.content.Intent;

import com.app.myfriend.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

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
            if (itemId == selectedItemId) {
                return true;
            }

            Intent intent = null;
            if (itemId == R.id.nav_home) {
                intent = new Intent(activity, BackendHomeActivity.class);
            } else if (itemId == R.id.nav_reels) {
                intent = new Intent(activity, BackendDiscoverActivity.class);
            } else if (itemId == R.id.nav_add) {
                intent = new Intent(activity, BackendCreatePostActivity.class);
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
}
