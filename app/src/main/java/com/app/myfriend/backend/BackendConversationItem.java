package com.app.myfriend.backend;

public class BackendConversationItem {

    public final String conversationId;
    public final String participantId;
    public final String participantName;
    public final String participantAvatarUrl;
    public final String participantSubtitle;
    public final String lastMessageText;
    public final String lastMessageAt;
    public final int unreadCount;

    public BackendConversationItem(
            String conversationId,
            String participantId,
            String participantName,
            String participantAvatarUrl,
            String participantSubtitle,
            String lastMessageText,
            String lastMessageAt,
            int unreadCount
    ) {
        this.conversationId = conversationId;
        this.participantId = participantId;
        this.participantName = participantName;
        this.participantAvatarUrl = participantAvatarUrl;
        this.participantSubtitle = participantSubtitle;
        this.lastMessageText = lastMessageText;
        this.lastMessageAt = lastMessageAt;
        this.unreadCount = unreadCount;
    }
}
