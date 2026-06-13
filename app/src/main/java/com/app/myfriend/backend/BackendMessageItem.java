package com.app.myfriend.backend;

public class BackendMessageItem {

    public final String id;
    public final String senderId;
    public final String content;
    public final String createdAt;
    public final boolean sentByViewer;

    public BackendMessageItem(String id, String senderId, String content, String createdAt, boolean sentByViewer) {
        this.id = id;
        this.senderId = senderId;
        this.content = content;
        this.createdAt = createdAt;
        this.sentByViewer = sentByViewer;
    }
}
