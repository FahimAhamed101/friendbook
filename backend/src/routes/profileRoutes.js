const express = require("express");
const {
  getDiscoverPeople,
  getMyProfile,
  getProfileById,
  toggleFollowUser,
  updateMyProfile,
} = require("../controllers/profileController");
const { protect } = require("../middleware/authMiddleware");

const router = express.Router();

router.get("/me", protect, getMyProfile);
router.patch("/me", protect, updateMyProfile);
router.get("/discover/people", protect, getDiscoverPeople);
router.post("/:userId/follow", protect, toggleFollowUser);
router.get("/:userId", protect, getProfileById);

module.exports = router;
