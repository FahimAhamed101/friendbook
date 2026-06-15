const express = require("express");
const {
  createPost,
  getFeedPosts,
  getPostById,
  reactToPost,
  togglePostLike,
  addPostComment,
  sharePost,
  toggleSavedPost,
  getSavedPosts,
} = require("../controllers/postController");
const { protect } = require("../middleware/authMiddleware");

const router = express.Router();

router.get("/feed", protect, getFeedPosts);
router.get("/saved", protect, getSavedPosts);
router.post("/", protect, createPost);
router.get("/:postId", protect, getPostById);
router.post("/:postId/reactions", protect, reactToPost);
router.post("/:postId/like", protect, togglePostLike);
router.post("/:postId/comments", protect, addPostComment);
router.post("/:postId/share", protect, sharePost);
router.post("/:postId/save", protect, toggleSavedPost);

module.exports = router;
