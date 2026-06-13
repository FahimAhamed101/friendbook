const mongoose = require("mongoose");
const { CHAT_ROLE_VALUES } = require("../utils/chatRoles");

const chatConversationSchema = new mongoose.Schema(
  {
    participants: {
      type: [
        {
          type: mongoose.Schema.Types.ObjectId,
          ref: "User",
          required: true,
        },
      ],
      validate: {
        validator(value) {
          return Array.isArray(value) && value.length === 2;
        },
        message: "A conversation must include exactly two participants.",
      },
      required: true,
    },
    participantKey: {
      type: String,
      required: true,
      unique: true,
      index: true,
      trim: true,
    },
    participantRoleMap: {
      type: Map,
      of: {
        type: String,
        enum: CHAT_ROLE_VALUES,
      },
      default: {},
    },
    lastMessageText: {
      type: String,
      trim: true,
      default: null,
    },
    lastMessageSenderId: {
      type: mongoose.Schema.Types.ObjectId,
      ref: "User",
      default: null,
    },
    lastMessageSenderRole: {
      type: String,
      enum: CHAT_ROLE_VALUES,
      default: null,
    },
    lastMessageAt: {
      type: Date,
      default: null,
    },
  },
  {
    timestamps: true,
  }
);

chatConversationSchema.index({ participants: 1, lastMessageAt: -1 });

module.exports = mongoose.model("ChatConversation", chatConversationSchema);
