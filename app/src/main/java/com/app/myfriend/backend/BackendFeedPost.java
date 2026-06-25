package com.app.myfriend.backend;

import java.util.List;

public class BackendFeedPost {

    public final String id;
    public final String type;
    public final String authorId;
    public final String authorName;
    public final String authorHandle;
    public final String authorImage;
    public final String activity;
    public final String feeling;
    public final String location;
    public final String published;
    public final String title;
    public final String content;
    public final String image;
    public final List<String> galleryImages;
    public final List<AudioSource> audioSources;
    public final String attachmentUrl;
    public final String attachmentType;
    public final String linkUrl;
    public final String ctaLabel;
    public final int likeCount;
    public final int commentCount;
    public final int shareCount;
    public final int saveCount;
    public final int viewCount;
    public final boolean likedByViewer;
    public final boolean savedByViewer;
    public final boolean commentsOpen;
    public final String viewerReaction;
    public final List<String> topReactions;

    public BackendFeedPost(
            String id,
            String type,
            String authorId,
            String authorName,
            String authorHandle,
            String authorImage,
            String activity,
            String feeling,
            String location,
            String published,
            String title,
            String content,
            String image,
            List<String> galleryImages,
            List<AudioSource> audioSources,
            String attachmentUrl,
            String attachmentType,
            String linkUrl,
            String ctaLabel,
            int likeCount,
            int commentCount,
            int shareCount,
            int saveCount,
            int viewCount,
            boolean likedByViewer,
            boolean savedByViewer,
            boolean commentsOpen,
            String viewerReaction,
            List<String> topReactions
    ) {
        this.id = id;
        this.type = type;
        this.authorId = authorId;
        this.authorName = authorName;
        this.authorHandle = authorHandle;
        this.authorImage = authorImage;
        this.activity = activity;
        this.feeling = feeling;
        this.location = location;
        this.published = published;
        this.title = title;
        this.content = content;
        this.image = image;
        this.galleryImages = galleryImages;
        this.audioSources = audioSources;
        this.attachmentUrl = attachmentUrl;
        this.attachmentType = attachmentType;
        this.linkUrl = linkUrl;
        this.ctaLabel = ctaLabel;
        this.likeCount = likeCount;
        this.commentCount = commentCount;
        this.shareCount = shareCount;
        this.saveCount = saveCount;
        this.viewCount = viewCount;
        this.likedByViewer = likedByViewer;
        this.savedByViewer = savedByViewer;
        this.commentsOpen = commentsOpen;
        this.viewerReaction = viewerReaction;
        this.topReactions = topReactions;
    }

    public static class AudioSource {
        public final String url;
        public final String mimeType;

        public AudioSource(String url, String mimeType) {
            this.url = url;
            this.mimeType = mimeType;
        }
    }
}
