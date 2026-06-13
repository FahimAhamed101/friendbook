package com.app.myfriend.backend;

public class BackendFeedPost {

    public final String id;
    public final String authorName;
    public final String authorHandle;
    public final String authorImage;
    public final String activity;
    public final String published;
    public final String content;
    public final String image;
    public final int likeCount;
    public final int commentCount;
    public final int shareCount;

    public BackendFeedPost(
            String id,
            String authorName,
            String authorHandle,
            String authorImage,
            String activity,
            String published,
            String content,
            String image,
            int likeCount,
            int commentCount,
            int shareCount
    ) {
        this.id = id;
        this.authorName = authorName;
        this.authorHandle = authorHandle;
        this.authorImage = authorImage;
        this.activity = activity;
        this.published = published;
        this.content = content;
        this.image = image;
        this.likeCount = likeCount;
        this.commentCount = commentCount;
        this.shareCount = shareCount;
    }
}
