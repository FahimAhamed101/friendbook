const express = require("express");
const { uploadFile } = require("../controllers/uploadController");
const { protect } = require("../middleware/authMiddleware");

const router = express.Router();

router.post("/", protect, uploadFile);

module.exports = router;
