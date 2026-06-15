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
            items.add(new BackendFeedPost(
                    post.optString("id", ""),
                    post.optString("type", "custom"),
                    post.optString("authorName", "Unknown author"),
                    post.optString("authorHandle", ""),
                    post.optString("authorImage", ""),
                    post.optString("activity", "posted"),
                    post.optString("published", ""),
                    post.optString("title", ""),
                    post.optString("content", ""),
                    post.optString("image", ""),
                    post.optString("attachmentUrl", ""),
                    post.optString("attachmentType", ""),
                    post.optString("linkUrl", ""),
                    post.optString("ctaLabel", ""),
                    stats != null ? stats.optInt("likeCount", 0) : 0,
                    stats != null ? stats.optInt("commentCount", 0) : 0,
                    stats != null ? stats.optInt("shareCount", 0) : 0,
                    stats != null ? stats.optInt("saveCount", 0) : 0,
                    stats != null && stats.optBoolean("likedByViewer", false),
                    stats != null && stats.optBoolean("savedByViewer", false),
                    post.optBoolean("commentsOpen", false)
            ));
        }

        return items;
    }
}
