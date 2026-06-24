package com.app.myfriend.backend;

public class BackendCommentItem {
    public final String id;
    public final String userId;
    public final String name;
    public final String image;
    public final String time;
    public final String message;

    public BackendCommentItem(String id, String userId, String name, String image, String time, String message) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.image = image;
        this.time = time;
        this.message = message;
    }
}
