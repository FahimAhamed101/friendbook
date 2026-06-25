package com.app.myfriend.backend;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class BackendFeedPostParser {

    private BackendFeedPostParser() {
    }

    public static List<BackendFeedPost> parse(JSONArray posts) {
        List<BackendFeedPost> items = new ArrayList<>();
        if (posts == null) {
            return items;
        }

        for (int i = 0; i < posts.length(); i++) {
            JSONObject post = posts.optJSONObject(i);
            if (post == null) {
                continue;
            }

            JSONObject stats = post.optJSONObject("stats");

            JSONArray galleryJson = post.optJSONArray("images");
            List<String> galleryImages = new ArrayList<>();
            if (galleryJson != null) {
                for (int j = 0; j < galleryJson.length(); j++) {
                    galleryImages.add(galleryJson.optString(j));
                }
            }

            JSONArray audioJson = post.optJSONArray("audioSources");
            List<BackendFeedPost.AudioSource> audioSources = new ArrayList<>();
            if (audioJson != null) {
                for (int j = 0; j < audioJson.length(); j++) {
                    JSONObject audio = audioJson.optJSONObject(j);
                    if (audio != null) {
                        audioSources.add(new BackendFeedPost.AudioSource(
                                audio.optString("url"),
                                audio.optString("mimeType")
                        ));
                    }
                }
            }

            JSONArray topReactionsJson = stats != null ? stats.optJSONArray("topReactions") : null;
            List<String> topReactions = new ArrayList<>();
            if (topReactionsJson != null) {
                for (int j = 0; j < topReactionsJson.length(); j++) {
                    topReactions.add(topReactionsJson.optString(j));
                }
            }

            items.add(new BackendFeedPost(
                    post.optString("id", ""),
                    post.optString("type", "custom"),
                    post.optString("authorId", ""),
                    post.optString("authorName", "Unknown author"),
                    post.optString("authorHandle", ""),
                    post.optString("authorImage", ""),
                    post.optString("activity", "posted"),
                    post.optString("feeling", ""),
                    post.optString("location", ""),
                    post.optString("published", ""),
                    post.optString("title", ""),
                    post.optString("content", ""),
                    post.optString("image", ""),
                    galleryImages,
                    audioSources,
                    post.optString("attachmentUrl", ""),
                    post.optString("attachmentType", ""),
                    post.optString("linkUrl", ""),
                    post.optString("ctaLabel", ""),
                    stats != null ? stats.optInt("likeCount", 0) : 0,
                    stats != null ? stats.optInt("commentCount", 0) : 0,
                    stats != null ? stats.optInt("shareCount", 0) : 0,
                    stats != null ? stats.optInt("saveCount", 0) : 0,
                    stats != null ? stats.optInt("viewCount", 0) : 0,
                    stats != null && stats.optBoolean("likedByViewer", false),
                    stats != null && stats.optBoolean("savedByViewer", false),
                    post.optBoolean("commentsOpen", false),
                    stats != null ? stats.optString("viewerReaction", null) : null,
                    topReactions
            ));
        }

        return items;
    }
}
