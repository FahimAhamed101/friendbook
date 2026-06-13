const { formatChatRole, normalizeChatRole } = require("./chatRoles");

function getDisplayName(user) {
  const firstName = String(user?.firstName || "").trim();
  const lastName = String(user?.lastName || "").trim();
  const fullName = `${firstName} ${lastName}`.trim();
  return fullName || String(user?.email || "Unknown user");
}

function getPresenceStatus(referenceDate) {
  if (!referenceDate) {
    return "offline";
  }

  const timestamp = new Date(referenceDate).getTime();
  if (Number.isNaN(timestamp)) {
    return "offline";
  }

  const diffMinutes = Math.floor((Date.now() - timestamp) / (1000 * 60));
  if (diffMinutes <= 30) {
    return "online";
  }

  if (diffMinutes <= 60 * 24) {
    return "away";
  }

  return "offline";
}

function toChatParticipant(user, options = {}) {
  const role = normalizeChatRole(user?.researcherType);

  return {
    id: String(user?._id || user?.id || ""),
    name: getDisplayName(user),
    email: String(user?.email || ""),
    avatarUrl: String(user?.avatarUrl || "").trim() || "/images/resources/user.jpg",
    status: getPresenceStatus(options.lastActivityAt || user?.updatedAt || user?.createdAt),
    role,
    roleLabel: formatChatRole(role),
    location: user?.location || user?.institute || null,
    institute: user?.institute || null,
    department: user?.department || null,
    phoneNumber: user?.phoneNumber || null,
    skypeId: user?.skypeId || null,
    localTime: user?.localTime || null,
  };
}

function toChatConversation(conversation, currentUserId, unreadCount) {
  const normalizedCurrentUserId = String(currentUserId || "");
  const otherParticipant =
    Array.isArray(conversation?.participants) &&
    conversation.participants.find((participant) => {
      const participantId = String(participant?._id || participant?.id || participant || "");
      return participantId && participantId !== normalizedCurrentUserId;
    });

  if (!otherParticipant) {
    return null;
  }

  const participant = toChatParticipant(otherParticipant || {}, {
    lastActivityAt: conversation?.lastMessageAt,
  });

  return {
    conversationId: String(conversation?._id || ""),
    lastMessageText: conversation?.lastMessageText || null,
    lastMessageSenderRole: conversation?.lastMessageSenderRole
      ? normalizeChatRole(conversation.lastMessageSenderRole)
      : null,
    lastMessageAt: conversation?.lastMessageAt || null,
    unreadCount: Number(unreadCount || 0),
    participant,
  };
}

function toChatMessage(message, currentUserId) {
  const readBy = Array.isArray(message?.readBy)
    ? message.readBy.map((value) => String(value))
    : [];

  return {
    id: String(message?._id || ""),
    conversationId: String(message?.conversationId || ""),
    senderId: String(message?.senderId || ""),
    senderRole: normalizeChatRole(message?.senderRole),
    content: String(message?.content || ""),
    readByViewer: readBy.includes(String(currentUserId || "")),
    createdAt: message?.createdAt || null,
    updatedAt: message?.updatedAt || null,
  };
}

module.exports = {
  toChatParticipant,
  toChatConversation,
  toChatMessage,
};
