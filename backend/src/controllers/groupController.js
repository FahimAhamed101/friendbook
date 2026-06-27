const Group = require("../models/Group");
const Post = require("../models/Post");
const { toFeedPost } = require("../utils/postSerializer");

async function createGroup(req, res, next) {
  try {
    const { name, description, category, isPrivate, iconUrl, coverUrl } = req.body;

    if (!name) {
      return res.status(400).json({ message: "Group name is required." });
    }

    const group = await Group.create({
      name,
      description,
      category,
      isPrivate: isPrivate === true || isPrivate === "true",
      iconUrl,
      coverUrl,
      creator: req.user._id,
      members: [req.user._id],
    });

    res.status(201).json({
      message: "Group created successfully.",
      group,
    });
  } catch (error) {
    next(error);
  }
}

async function getMyGroups(req, res, next) {
  try {
    const groups = await Group.find({ members: req.user._id })
      .populate("creator", "firstName lastName avatarUrl")
      .sort({ updatedAt: -1 });

    res.status(200).json({
      message: "Groups loaded successfully.",
      groups,
    });
  } catch (error) {
    next(error);
  }
}

async function getDiscoverGroups(req, res, next) {
  try {
    const groups = await Group.find({
      isPrivate: false,
      members: { $ne: req.user._id }
    }).limit(20);

    res.status(200).json({
      message: "Discover groups loaded.",
      groups,
    });
  } catch (error) {
    next(error);
  }
}

async function getGroupPosts(req, res, next) {
  try {
    // For simplicity, get all posts from groups user is a member of
    const myGroups = await Group.find({ members: req.user._id }).select("_id");
    const groupIds = myGroups.map(g => g._id);

    const posts = await Post.find({ group: { $in: groupIds } })
      .populate("author")
      .populate("group")
      .sort({ createdAt: -1 })
      .limit(50);

    res.status(200).json({
      message: "Group posts loaded.",
      posts: posts.map(p => toFeedPost(p, req.user._id)),
    });
  } catch (error) {
    next(error);
  }
}

async function joinGroup(req, res, next) {
  try {
    const group = await Group.findById(req.params.groupId);
    if (!group) {
      return res.status(404).json({ message: "Group not found." });
    }

    if (group.members.includes(req.user._id)) {
      return res.status(400).json({ message: "Already a member." });
    }

    group.members.push(req.user._id);
    await group.save();

    res.status(200).json({ message: "Joined group successfully." });
  } catch (error) {
    next(error);
  }
}

module.exports = {
  createGroup,
  getMyGroups,
  getDiscoverGroups,
  getGroupPosts,
  joinGroup,
};
