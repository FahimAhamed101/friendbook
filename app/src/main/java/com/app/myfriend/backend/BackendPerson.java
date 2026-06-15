package com.app.myfriend.backend;

public class BackendPerson {

    public final String id;
    public final String name;
    public final String subtitle;
    public final String image;
    public final String actionLabel;
    public final boolean isFollowing;

    public BackendPerson(String id, String name, String subtitle, String image, String actionLabel, boolean isFollowing) {
        this.id = id;
        this.name = name;
        this.subtitle = subtitle;
        this.image = image;
        this.actionLabel = actionLabel;
        this.isFollowing = isFollowing;
    }
}
