const express = require("express");
const {
  createGroup,
  getMyGroups,
  getDiscoverGroups,
  getGroupPosts,
  joinGroup,
} = require("../controllers/groupController");
const { protect } = require("../middleware/authMiddleware");

const router = express.Router();

router.post("/", protect, createGroup);
router.get("/me", protect, getMyGroups);
router.get("/discover", protect, getDiscoverGroups);
router.get("/posts", protect, getGroupPosts);
router.post("/:groupId/join", protect, joinGroup);

module.exports = router;
