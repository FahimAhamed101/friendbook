package com.app.myfriend.backend;

public class BackendGroup {
    public final String id;
    public final String name;
    public final String description;
    public final String category;
    public final String iconUrl;
    public final String coverUrl;
    public final String creatorId;
    public final int memberCount;
    public final boolean isMember;
    public final boolean isPrivate;

    public BackendGroup(String id, String name, String description, String category, String iconUrl, String coverUrl, String creatorId, int memberCount, boolean isMember, boolean isPrivate) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.iconUrl = iconUrl;
        this.coverUrl = coverUrl;
        this.creatorId = creatorId;
        this.memberCount = memberCount;
        this.isMember = isMember;
        this.isPrivate = isPrivate;
    }
}
