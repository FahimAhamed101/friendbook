const Post = require("../models/Post");
const mongoose = require("mongoose");
const { toFeedPost, toTimelinePost } = require("../utils/postSerializer");

const DEFAULT_FEED_SEED = [
  {
    postType: "custom",
    activityLabel: "is feeling happy",
    feeling: "happy",
    location: "New York, USA",
    title: "Project Milestone Reached!",
    content: "We just reached our first major milestone for the Myfriend project. Huge thanks to the team! #milestone #success",
    commentsOpen: true,
  },
  {
    postType: "image",
    activityLabel: "is traveling",
    feeling: "Traveling",
    location: "Paris, France",
    title: "Postcard from Paris",
    content: "The Eiffel Tower looks stunning today. #travel #paris",
    displayImageUrl: "https://picsum.photos/id/1018/1200/900",
    commentsOpen: true,
  },
  {
    postType: "video",
    activityLabel: "is watching",
    feeling: "Watching",
    location: "Home Cinema",
    title: "Nature's Beauty",
    content: "Short clip of the forest nearby. So peaceful.",
    attachmentUrl: "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
    attachmentType: "video",
    commentsOpen: true,
  },
  {
    postType: "bg",
    activityLabel: "is thinking",
    feeling: "Thinking",
    title: "Daily Motivation",
    content: "What inspires you the most in your daily work? #inspiration",
    displayImageUrl: "https://picsum.photos/id/1041/1200/900",
    commentsOpen: true,
  },
  {
    postType: "audio",
    activityLabel: "is listening to music",
    feeling: "Listening",
    title: "Late night beats",
    content: "Keeping the energy up for a coding marathon.",
    attachmentUrl: "http://codeskulptor-demos.commondatastorage.googleapis.com/pang/paws.mp3",
    attachmentType: "file",
    commentsOpen: true,
  }
];

async function ensureSeedFeedPostsForUser(userId) {
  const existingCount = await Post.countDocuments({
    author: userId,
    activityFeed: true,
  });

  if (existingCount > 0) {
    return;
  }

  const createdAtBase = Date.now();
  await Post.insertMany(
    DEFAULT_FEED_SEED.map((seed, index) => ({
      author: userId,
      postType: seed.postType,
      activityLabel: seed.activityLabel,
      feeling: seed.feeling || null,
      location: seed.location || null,
      title: seed.title,
      content: seed.content,
      displayImageUrl: seed.displayImageUrl || null,
      attachmentUrl: seed.attachmentUrl || null,
      attachmentType: seed.attachmentType || null,
      audience: "public",
      activityFeed: true,
      myStory: false,
      commentsOpen: seed.commentsOpen === true,
      viewCount: Math.floor(Math.random() * 500) + 50,
      createdAt: new Date(createdAtBase - index * 60 * 60 * 1000),
      updatedAt: new Date(createdAtBase - index * 60 * 60 * 1000),
    }))
  );
}

const ALLOWED_AUDIENCES = new Set(["public", "private", "specific-friend", "only-friends", "joined-groups"]);
const ALLOWED_POST_TYPES = new Set(["custom", "article", "premium", "image", "album", "link", "video", "gif", "audio", "sponsor", "party", "bg"]);
const ALLOWED_REACTION_TYPES = new Set(["like", "love", "haha", "wow", "sad", "angry"]);

function normalizeOptionalText(value) {
  const normalized = String(value || "").trim();
  return normalized || null;
}

function normalizeOptionalHref(value) {
  const normalized = normalizeOptionalText(value);
  if (!normalized) return null;
  return normalized;
}

function normalizeAudience(value) {
  const normalized = String(value || "").trim().toLowerCase();
  if (!normalized) return "public";
  return ALLOWED_AUDIENCES.has(normalized) ? normalized : "public";
}

function normalizeBoolean(value, fallback) {
  if (typeof value === "boolean") return value;
  if (typeof value === "string") {
    const n = value.trim().toLowerCase();
    if (n === "true") return true;
    if (n === "false") return false;
  }
  return fallback;
}

async function getFeedPosts(req, res, next) {
  try {
    const now = new Date();
    const query = {
      activityFeed: true,
      $or: [{ scheduledFor: null }, { scheduledFor: { $lte: now } }],
    };

    if (req.query.type) {
      query.postType = req.query.type;
    }

    let posts = await Post.find(query)
      .populate("author")
      .populate("comments.user")
      .sort({ createdAt: -1 })
      .limit(50);

    if (posts.length === 0 && !req.query.type) {
      await ensureSeedFeedPostsForUser(req.user._id);
      posts = await Post.find(query)
        .populate("author")
        .populate("comments.user")
        .sort({ createdAt: -1 })
        .limit(50);
    }

    const visiblePosts = posts.map((post) => toFeedPost(post, req.user._id));

    res.status(200).json({
      message: "Feed loaded successfully.",
      posts: visiblePosts,
    });
  } catch (error) {
    next(error);
  }
}

async function createPost(req, res, next) {
  try {
    const payload = {
      author: req.user._id,
      postType: req.body.postType || "custom",
      activityLabel: normalizeOptionalText(req.body.activityLabel),
      title: normalizeOptionalText(req.body.title),
      content: normalizeOptionalText(req.body.content) || "",
      feeling: normalizeOptionalText(req.body.feeling),
      location: normalizeOptionalText(req.body.location),
      attachmentUrl: normalizeOptionalHref(req.body.attachmentUrl),
      attachmentType: req.body.attachmentType,
      displayImageUrl: normalizeOptionalHref(req.body.displayImageUrl),
      linkUrl: normalizeOptionalHref(req.body.linkUrl),
      ctaLabel: normalizeOptionalText(req.body.ctaLabel),
      commentsOpen: normalizeBoolean(req.body.commentsOpen, true),
      activityFeed: normalizeBoolean(req.body.activityFeed, true),
      myStory: normalizeBoolean(req.body.myStory, false),
      audience: normalizeAudience(req.body.audience),
      viewCount: 0
    };

    const createdPost = await Post.create(payload);
    const post = await Post.findById(createdPost._id).populate("author");

    res.status(201).json({
      message: "Post published successfully.",
      post: toFeedPost(post, req.user._id),
    });
  } catch (error) {
    next(error);
  }
}

async function getPostById(req, res, next) {
  try {
    const post = await Post.findById(req.params.postId)
      .populate("author")
      .populate("comments.user");

    if (!post) {
      res.status(404).json({ message: "Post not found." });
      return;
    }

    post.viewCount = (post.viewCount || 0) + 1;
    await post.save();

    res.status(200).json({
      message: "Post loaded successfully.",
      post: toFeedPost(post, req.user._id),
    });
  } catch (error) {
    next(error);
  }
}

async function reactToPost(req, res, next) {
  try {
    const post = await Post.findById(req.params.postId);
    if (!post) {
      res.status(404).json({ message: "Post not found." });
      return;
    }

    const reactionType = String(req.body.reactionType || req.body.type || "like").toLowerCase();
    if (!ALLOWED_REACTION_TYPES.has(reactionType)) {
      res.status(400).json({ message: "Invalid reaction type." });
      return;
    }

    const viewerId = String(req.user._id);
    const existingReactionIndex = post.reactions.findIndex(
      (reaction) => String(reaction.user) === viewerId
    );

    if (existingReactionIndex >= 0) {
      if (post.reactions[existingReactionIndex].type === reactionType) {
        post.reactions.splice(existingReactionIndex, 1);
      } else {
        post.reactions[existingReactionIndex].type = reactionType;
      }
    } else {
      post.reactions.push({
        user: req.user._id,
        type: reactionType,
      });
    }

    await post.save();
    const updatedPost = await Post.findById(post._id).populate("author").populate("comments.user");

    res.status(200).json({
      message: "Reaction saved.",
      post: toFeedPost(updatedPost, req.user._id),
    });
  } catch (error) {
    next(error);
  }
}

async function addPostComment(req, res, next) {
  try {
    const post = await Post.findById(req.params.postId);
    if (!post) {
      res.status(404).json({ message: "Post not found." });
      return;
    }

    const message = normalizeOptionalText(req.body.message);
    if (!message) {
      res.status(400).json({ message: "Comment message is required." });
      return;
    }

    post.comments.push({
      user: req.user._id,
      message,
    });

    await post.save();
    const updatedPost = await Post.findById(post._id).populate("author").populate("comments.user");

    res.status(201).json({
      message: "Comment added successfully.",
      post: toFeedPost(updatedPost, req.user._id),
    });
  } catch (error) {
    next(error);
  }
}

async function sharePost(req, res, next) {
  try {
    const post = await Post.findById(req.params.postId);
    if (!post) {
      res.status(404).json({ message: "Post not found." });
      return;
    }

    post.shareCount = (post.shareCount || 0) + 1;
    await post.save();

    res.status(200).json({
      message: "Post shared successfully.",
      post: toFeedPost(post, req.user._id),
    });
  } catch (error) {
    next(error);
  }
}

async function toggleSavedPost(req, res, next) {
  try {
    const post = await Post.findById(req.params.postId);
    if (!post) {
      res.status(404).json({ message: "Post not found." });
      return;
    }

    const viewerId = String(req.user._id);
    const savedByIds = post.savedBy.map(id => String(id));
    const isSaved = savedByIds.includes(viewerId);

    if (isSaved) {
      post.savedBy = post.savedBy.filter((userId) => String(userId) !== viewerId);
    } else {
      post.savedBy.push(req.user._id);
    }

    await post.save();
    const updatedPost = await Post.findById(post._id).populate("author").populate("comments.user");

    res.status(200).json({
      message: isSaved ? "Post removed from saved items." : "Post saved successfully.",
      post: toFeedPost(updatedPost, req.user._id),
    });
  } catch (error) {
    next(error);
  }
}

async function getSavedPosts(req, res, next) {
  try {
    const posts = await Post.find({ savedBy: req.user._id })
      .populate("author")
      .populate("comments.user")
      .sort({ updatedAt: -1 })
      .limit(50);

    res.status(200).json({
      message: "Saved posts loaded successfully.",
      posts: posts.map((post) => toFeedPost(post, req.user._id)),
    });
  } catch (error) {
    next(error);
  }
}

module.exports = {
  getFeedPosts,
  getPostById,
  createPost,
  reactToPost,
  addPostComment,
  sharePost,
  toggleSavedPost,
  getSavedPosts,
};
