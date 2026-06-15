package com.app.myfriend.backend;

public class BackendFeedPost {

    public final String id;
    public final String type;
    public final String authorName;
    public final String authorHandle;
    public final String authorImage;
    public final String activity;
    public final String published;
    public final String title;
    public final String content;
    public final String image;
    public final String attachmentUrl;
    public final String attachmentType;
    public final String linkUrl;
    public final String ctaLabel;
    public final int likeCount;
    public final int commentCount;
    public final int shareCount;
    public final int saveCount;
    public final boolean likedByViewer;
    public final boolean savedByViewer;
    public final boolean commentsOpen;

    public BackendFeedPost(
            String id,
            String type,
            String authorName,
            String authorHandle,
            String authorImage,
            String activity,
            String published,
            String title,
            String content,
            String image,
            String attachmentUrl,
            String attachmentType,
            String linkUrl,
            String ctaLabel,
            int likeCount,
            int commentCount,
            int shareCount,
            int saveCount,
            boolean likedByViewer,
            boolean savedByViewer,
            boolean commentsOpen
    ) {
        this.id = id;
        this.type = type;
        this.authorName = authorName;
        this.authorHandle = authorHandle;
        this.authorImage = authorImage;
        this.activity = activity;
        this.published = published;
        this.title = title;
        this.content = content;
        this.image = image;
        this.attachmentUrl = attachmentUrl;
        this.attachmentType = attachmentType;
        this.linkUrl = linkUrl;
        this.ctaLabel = ctaLabel;
        this.likeCount = likeCount;
        this.commentCount = commentCount;
        this.shareCount = shareCount;
        this.saveCount = saveCount;
        this.likedByViewer = likedByViewer;
        this.savedByViewer = savedByViewer;
        this.commentsOpen = commentsOpen;
    }
}
