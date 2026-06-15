package com.app.myfriend.backend;

public class BackendNotificationItem {
    public final String title;
    public final String message;
    public final String meta;
    public final String imageUrl;

    public BackendNotificationItem(String title, String message, String meta, String imageUrl) {
        this.title = title;
        this.message = message;
        this.meta = meta;
        this.imageUrl = imageUrl;
    }
}
