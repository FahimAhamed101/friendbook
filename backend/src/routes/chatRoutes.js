const express = require("express");
const {
  getOrCreateConversation,
  listConversations,
  listContacts,
  listMessages,
  sendMessage,
  markRead,
} = require("../controllers/chatController");
const { protect } = require("../middleware/authMiddleware");

const router = express.Router();

router.post("/conversations", protect, getOrCreateConversation);
router.get("/conversations", protect, listConversations);
router.get("/contacts", protect, listContacts);
router.get("/conversations/:conversationId/messages", protect, listMessages);
router.post("/conversations/:conversationId/messages", protect, sendMessage);
router.post("/conversations/:conversationId/read", protect, markRead);

module.exports = router;
