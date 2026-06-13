const mongoose = require("mongoose");
const ChatConversation = require("../models/ChatConversation");
const ChatMessage = require("../models/ChatMessage");
const User = require("../models/User");
const { normalizeChatRole } = require("../utils/chatRoles");
const {
  toChatConversation,
  toChatMessage,
  toChatParticipant,
} = require("../utils/chatSerializer");

const CHAT_USER_SELECT_FIELDS =
  "firstName lastName email researcherType avatarUrl location institute department phoneNumber skypeId localTime updatedAt createdAt";

function createHttpError(message, statusCode) {
  const error = new Error(message);
  error.statusCode = statusCode;
  return error;
}

function buildParticipantKey(firstUserId, secondUserId) {
  return [String(firstUserId), String(secondUserId)].sort().join(":");
}

function isValidObjectId(value) {
  return mongoose.Types.ObjectId.isValid(String(value || ""));
}

function normalizePaginationValue(value, fallback, max) {
  const parsed = Number.parseInt(String(value || ""), 10);
  if (!Number.isFinite(parsed) || parsed <= 0) {
    return fallback;
  }

  return Math.min(parsed, max);
}

function buildUserSearchQuery(search) {
  const normalized = String(search || "").trim();
  if (!normalized) {
    return {};
  }

  const searchRegex = new RegExp(normalized, "i");
  return {
    $or: [
      { firstName: searchRegex },
      { lastName: searchRegex },
      { email: searchRegex },
      { institute: searchRegex },
      { department: searchRegex },
      { position: searchRegex },
    ],
  };
}

function getRequestBody(req) {
  if (!req || !req.body || typeof req.body !== "object" || Buffer.isBuffer(req.body)) {
    return {};
  }

  return req.body;
}

async function getConversationUnreadCount(conversationId, viewerId) {
  return ChatMessage.countDocuments({
    conversationId,
    senderId: { $ne: viewerId },
    readBy: { $nin: [viewerId] },
  });
}

async function loadConversationForUser(conversationId, currentUserId) {
  if (!isValidObjectId(conversationId)) {
    throw createHttpError("Conversation not found.", 404);
  }

  const conversation = await ChatConversation.findById(conversationId).populate(
    "participants",
    CHAT_USER_SELECT_FIELDS
  );

  if (!conversation) {
    throw createHttpError("Conversation not found.", 404);
  }

  const hasAccess = conversation.participants.some(
    (participant) => String(participant?._id || "") === String(currentUserId)
  );

  if (!hasAccess) {
    throw createHttpError("You do not have access to this conversation.", 403);
  }

  return conversation;
}

async function findConversationForUsers(currentUser, recipientId) {
  const recipient = await findRecipientForConversation(currentUser, recipientId);
  const participantKey = buildParticipantKey(currentUser._id, recipient._id);

  const conversation = await ChatConversation.findOne({ participantKey }).populate(
    "participants",
    CHAT_USER_SELECT_FIELDS
  );

  return {
    conversation,
    recipient,
  };
}

async function findRecipientForConversation(currentUser, recipientId) {
  const normalizedRecipientId = String(recipientId || "").trim();
  const currentUserId = String(currentUser?._id || "");

  if (!isValidObjectId(normalizedRecipientId)) {
    throw createHttpError("A valid recipientId is required.", 400);
  }

  if (normalizedRecipientId === currentUserId) {
    throw createHttpError("You cannot start a conversation with yourself.", 400);
  }

  const recipient = await User.findById(normalizedRecipientId).select(
    CHAT_USER_SELECT_FIELDS
  );

  if (!recipient) {
    throw createHttpError("Recipient not found.", 404);
  }

  return recipient;
}

async function findOrCreateConversation(currentUser, recipientId) {
  const recipient = await findRecipientForConversation(currentUser, recipientId);
  const currentUserId = String(currentUser._id);
  const resolvedRecipientId = String(recipient._id);
  const participantKey = buildParticipantKey(currentUserId, resolvedRecipientId);

  let conversation = await ChatConversation.findOne({ participantKey });

  if (!conversation) {
    conversation = await ChatConversation.create({
      participants: [currentUser._id, recipient._id],
      participantKey,
      participantRoleMap: {
        [currentUserId]: normalizeChatRole(currentUser.researcherType),
        [resolvedRecipientId]: normalizeChatRole(recipient.researcherType),
      },
    });
  }

  return {
    conversation,
    recipient,
  };
}

async function getOrCreateConversation(req, res, next) {
  try {
    const body = getRequestBody(req);
    const { conversation, recipient } = await findOrCreateConversation(req.user, body.recipientId);

    res.status(200).json({
      message: "Conversation ready.",
      conversationId: String(conversation._id),
      participant: toChatParticipant(recipient, {
        lastActivityAt: conversation.lastMessageAt || recipient.updatedAt,
      }),
    });
  } catch (error) {
    next(error);
  }
}

async function listConversations(req, res, next) {
  try {
    const page = normalizePaginationValue(req.query.page, 1, 1000);
    const limit = normalizePaginationValue(req.query.limit, 20, 100);

    const [conversations, total] = await Promise.all([
      ChatConversation.find({ participants: req.user._id })
        .populate(
          "participants",
          CHAT_USER_SELECT_FIELDS
        )
        .sort({ lastMessageAt: -1, updatedAt: -1 })
        .skip((page - 1) * limit)
        .limit(limit),
      ChatConversation.countDocuments({ participants: req.user._id }),
    ]);

    const unreadCounts = await Promise.all(
      conversations.map((conversation) =>
        getConversationUnreadCount(conversation._id, req.user._id)
      )
    );

    const serializedConversations = conversations
      .map((conversation, index) =>
        toChatConversation(conversation, req.user._id, unreadCounts[index])
      )
      .filter(Boolean);

    res.status(200).json({
      total,
      page,
      limit,
      data: serializedConversations,
    });
  } catch (error) {
    next(error);
  }
}

async function listContacts(req, res, next) {
  try {
    const searchQuery = buildUserSearchQuery(req.query.search);
    const users = await User.find({
      _id: { $ne: req.user._id },
      ...searchQuery,
    })
      .select(
        CHAT_USER_SELECT_FIELDS
      )
      .sort({ updatedAt: -1, createdAt: -1 })
      .limit(50);

    const participantKeys = users.map((user) => buildParticipantKey(req.user._id, user._id));
    const existingConversations = await ChatConversation.find({
      participantKey: { $in: participantKeys },
    }).select("participantKey");

    const conversationMap = new Map(
      existingConversations.map((conversation) => [
        conversation.participantKey,
        String(conversation._id),
      ])
    );

    res.status(200).json({
      data: users.map((user) => ({
        ...toChatParticipant(user),
        conversationId: conversationMap.get(buildParticipantKey(req.user._id, user._id)) || null,
      })),
    });
  } catch (error) {
    next(error);
  }
}

async function listMessages(req, res, next) {
  try {
    const recipientId = String(req.query.recipientId || "").trim();
    let conversation;

    try {
      conversation = await loadConversationForUser(req.params.conversationId, req.user._id);
    } catch (error) {
      if ((error?.statusCode === 404 || error?.statusCode === 403) && recipientId) {
        ({ conversation } = await findConversationForUsers(req.user, recipientId));
      } else {
        throw error;
      }
    }

    const limit = normalizePaginationValue(req.query.limit, 200, 500);

    if (!conversation) {
      res.status(200).json({
        conversationId: null,
        data: [],
        limit,
      });
      return;
    }

    const before = String(req.query.before || "").trim();

    const match = { conversationId: conversation._id };
    if (before) {
      const parsedBefore = new Date(before);
      if (Number.isNaN(parsedBefore.getTime())) {
        throw createHttpError("The before query parameter must be a valid date.", 400);
      }

      match.createdAt = { $lt: parsedBefore };
    }

    const messages = await ChatMessage.find(match).sort({ createdAt: 1 }).limit(limit);

    res.status(200).json({
      conversationId: String(conversation._id),
      data: messages.map((message) => toChatMessage(message, req.user._id)),
      limit,
    });
  } catch (error) {
    next(error);
  }
}

async function sendMessage(req, res, next) {
  try {
    const body = getRequestBody(req);
    const content = String(body.content || "").trim();

    if (!content) {
      throw createHttpError("Message content is required.", 400);
    }

    if (content.length > 2000) {
      throw createHttpError("Message content cannot exceed 2000 characters.", 400);
    }

    let conversation;

    try {
      conversation = await loadConversationForUser(req.params.conversationId, req.user._id);
    } catch (error) {
      if ((error?.statusCode === 404 || error?.statusCode === 403) && body.recipientId) {
        ({ conversation } = await findOrCreateConversation(req.user, body.recipientId));
      } else {
        throw error;
      }
    }

    const senderRole = normalizeChatRole(req.user.researcherType);
    const message = await ChatMessage.create({
      conversationId: conversation._id,
      senderId: req.user._id,
      senderRole,
      content,
      readBy: [req.user._id],
    });

    conversation.lastMessageText = content;
    conversation.lastMessageSenderId = req.user._id;
    conversation.lastMessageSenderRole = senderRole;
    conversation.lastMessageAt = message.createdAt;
    conversation.participantRoleMap = {
      ...Object.fromEntries(
        conversation.participantRoleMap && typeof conversation.participantRoleMap.entries === "function"
          ? Array.from(conversation.participantRoleMap.entries())
          : Object.entries(conversation.participantRoleMap || {})
      ),
      [String(req.user._id)]: senderRole,
    };
    await conversation.save();

    res.status(201).json(toChatMessage(message, req.user._id));
  } catch (error) {
    next(error);
  }
}

async function markRead(req, res, next) {
  try {
    const conversation = await loadConversationForUser(req.params.conversationId, req.user._id);

    await ChatMessage.updateMany(
      {
        conversationId: conversation._id,
        senderId: { $ne: req.user._id },
        readBy: { $nin: [req.user._id] },
      },
      {
        $addToSet: {
          readBy: req.user._id,
        },
      }
    );

    res.status(200).json({
      message: "Messages marked as read.",
    });
  } catch (error) {
    next(error);
  }
}

module.exports = {
  getOrCreateConversation,
  listConversations,
  listContacts,
  listMessages,
  sendMessage,
  markRead,
};
