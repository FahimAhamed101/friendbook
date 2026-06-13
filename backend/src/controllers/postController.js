const Post = require("../models/Post");
const mongoose = require("mongoose");
const { toFeedPost, toTimelinePost } = require("../utils/postSerializer");

const ALLOWED_AUDIENCES = new Set(["public", "private", "specific-friend", "only-friends", "joined-groups"]);
const ALLOWED_ATTACHMENT_TYPES = new Set(["image", "video", "file"]);
const ALLOWED_POST_TYPES = new Set(["custom", "article", "premium", "image", "album", "link", "video", "gif", "audio", "sponsor"]);
const ALLOWED_REACTION_TYPES = new Set(["like", "love", "haha", "wow", "sad"]);
const TEMPLATE_RELATIVE_PATTERN = /^(?:\.{0,2}\/)?[\w./-]+\.(?:html?|php|asp|aspx)(?:[?#].*)?$/i;

function normalizeOptionalText(value) {
  const normalized = String(value || "").trim();
  return normalized || null;
}

function normalizeOptionalHref(value, fieldName) {
  const normalized = normalizeOptionalText(value);
  if (!normalized) {
    return null;
  }

  if (
    normalized === "#" ||
    normalized.startsWith("/") ||
    normalized.startsWith("./") ||
    normalized.startsWith("../") ||
    TEMPLATE_RELATIVE_PATTERN.test(normalized)
  ) {
    return normalized;
  }

  try {
    return new URL(normalized).toString();
  } catch {
    const error = new Error(`${fieldName} must be a valid URL or app path.`);
    error.statusCode = 400;
    throw error;
  }
}

function normalizeAudience(value) {
  const normalized = String(value || "").trim().toLowerCase();
  if (!normalized) {
    return "joined-groups";
  }

  if (!ALLOWED_AUDIENCES.has(normalized)) {
    const error = new Error("Audience selection is invalid.");
    error.statusCode = 400;
    throw error;
  }

  return normalized;
}

function normalizeAttachmentType(value) {
  const normalized = String(value || "").trim().toLowerCase();
  if (!normalized) {
    return null;
  }

  if (!ALLOWED_ATTACHMENT_TYPES.has(normalized)) {
    const error = new Error("Attachment type is invalid.");
    error.statusCode = 400;
    throw error;
  }

  return normalized;
}

function normalizePostType(value) {
  const normalized = String(value || "").trim().toLowerCase();
  if (!normalized) {
    return "custom";
  }

  if (!ALLOWED_POST_TYPES.has(normalized)) {
    const error = new Error("Post type is invalid.");
    error.statusCode = 400;
    throw error;
  }

  return normalized;
}

function normalizeReactionType(value) {
  const normalized = String(value || "").trim().toLowerCase();
  if (!normalized || !ALLOWED_REACTION_TYPES.has(normalized)) {
    const error = new Error("Reaction type is invalid.");
    error.statusCode = 400;
    throw error;
  }

  return normalized;
}

function normalizeBoolean(value, fallback) {
  if (typeof value === "boolean") {
    return value;
  }

  if (typeof value === "string") {
    const normalized = value.trim().toLowerCase();
    if (normalized === "true") {
      return true;
    }

    if (normalized === "false") {
      return false;
    }
  }

  return fallback;
}

function normalizeScheduledFor(value) {
  const normalized = normalizeOptionalText(value);
  if (!normalized) {
    return null;
  }

  const parsed = new Date(normalized);
  if (Number.isNaN(parsed.getTime())) {
    const error = new Error("Schedule date is invalid.");
    error.statusCode = 400;
    throw error;
  }

  return parsed;
}

function normalizeNonNegativeInteger(value, fallback = 0) {
  if (value == null || value === "") {
    return fallback;
  }

  const parsed = Number.parseInt(String(value), 10);
  if (!Number.isFinite(parsed) || parsed < 0) {
    const error = new Error("A numeric value is invalid.");
    error.statusCode = 400;
    throw error;
  }

  return parsed;
}

function normalizeHrefList(value, fieldName) {
  if (value == null || value === "") {
    return [];
  }

  const entries = Array.isArray(value) ? value : [value];

  return entries
    .map((entry) => normalizeOptionalHref(entry, fieldName))
    .filter(Boolean)
    .slice(0, 10);
}

function normalizeAudioSources(value, fallbackUrl, fallbackMimeType) {
  const normalizedEntries = [];
  const entries = Array.isArray(value) ? value : value ? [value] : [];

  entries.forEach((entry) => {
    if (typeof entry === "string") {
      const url = normalizeOptionalHref(entry, "Audio source URL");
      if (url) {
        normalizedEntries.push({ url, mimeType: null });
      }

      return;
    }

    if (!entry || typeof entry !== "object") {
      return;
    }

    const url = normalizeOptionalHref(entry.url, "Audio source URL");
    const mimeType = normalizeOptionalText(entry.mimeType);
    if (url) {
      normalizedEntries.push({ url, mimeType });
    }
  });

  if (normalizedEntries.length > 0) {
    return normalizedEntries.slice(0, 4);
  }

  const fallbackNormalizedUrl = normalizeOptionalHref(fallbackUrl, "Audio source URL");
  if (!fallbackNormalizedUrl) {
    return [];
  }

  return [
    {
      url: fallbackNormalizedUrl,
      mimeType: normalizeOptionalText(fallbackMimeType),
    },
  ];
}

function normalizeSponsorItems(value) {
  if (!Array.isArray(value)) {
    return [];
  }

  return value
    .map((item) => {
      if (!item || typeof item !== "object") {
        return null;
      }

      const title = normalizeOptionalText(item.title);
      if (!title) {
        return null;
      }

      return {
        title,
        imageUrl: normalizeOptionalHref(item.imageUrl || item.image, "Sponsor image URL"),
        priceLabel: normalizeOptionalText(item.priceLabel || item.price),
        href: normalizeOptionalHref(item.href, "Sponsor item URL"),
        ctaLabel: normalizeOptionalText(item.ctaLabel) || "Shop Now",
        shareLabel: normalizeOptionalText(item.shareLabel),
        likeLabel: normalizeOptionalText(item.likeLabel),
      };
    })
    .filter(Boolean)
    .slice(0, 12);
}

function hasRenderableContent(payload) {
  return Boolean(
    payload.title ||
      payload.content ||
      payload.linkUrl ||
      payload.attachmentUrl ||
      payload.displayImageUrl ||
      payload.galleryImages.length > 0 ||
      payload.audioSources.length > 0 ||
      payload.gifPreviewUrl ||
      payload.gifDataUrl ||
      payload.sponsorItems.length > 0
  );
}

function canUserViewPost(post, userId) {
  if (!post) {
    return false;
  }

  if (String(post.audience || "").trim().toLowerCase() !== "private") {
    return true;
  }

  const authorId =
    post.author && typeof post.author === "object" && post.author._id
      ? String(post.author._id)
      : String(post.author || "");

  return authorId === userId;
}

async function findPostForViewer(postId, userId) {
  const normalizedPostId = String(postId || "").trim();
  if (!mongoose.Types.ObjectId.isValid(normalizedPostId)) {
    return null;
  }

  const post = await Post.findById(postId)
    .populate("author")
    .populate("comments.user");

  if (!post || !canUserViewPost(post, userId)) {
    return null;
  }

  return post;
}

function getReactionUserId(reaction) {
  if (reaction?.user && typeof reaction.user === "object" && reaction.user._id) {
    return String(reaction.user._id);
  }

  return String(reaction?.user || "").trim();
}

function getMutableReactions(post) {
  if (Array.isArray(post.reactions) && post.reactions.length > 0) {
    return post.reactions;
  }

  post.reactions = Array.isArray(post.likes)
    ? post.likes
        .map((userId) => {
          const normalizedUserId = String(userId || "").trim();
          if (!normalizedUserId) {
            return null;
          }

          return {
            user: userId,
            type: "like",
          };
        })
        .filter(Boolean)
    : [];

  return post.reactions;
}

function normalizeReactionUserId(value) {
  const normalized = String(value || "").trim();
  if (!normalized || !mongoose.Types.ObjectId.isValid(normalized)) {
    return null;
  }

  return normalized;
}

function sanitizePostReactions(post) {
  const sourceReactions = getMutableReactions(post);
  const sanitizedReactions = [];
  const seenUsers = new Set();

  sourceReactions.forEach((reaction) => {
    const userId = normalizeReactionUserId(getReactionUserId(reaction));
    const type = String(reaction?.type || "").trim().toLowerCase();

    if (!userId || !ALLOWED_REACTION_TYPES.has(type) || seenUsers.has(userId)) {
      return;
    }

    seenUsers.add(userId);
    sanitizedReactions.push({
      user: new mongoose.Types.ObjectId(userId),
      type,
    });
  });

  post.reactions = sanitizedReactions;
}

function syncLegacyLikes(post) {
  const reactions = Array.isArray(post.reactions) ? post.reactions : [];
  post.likes = reactions
    .filter((reaction) => String(reaction?.type || "").trim().toLowerCase() === "like")
    .map((reaction) => reaction.user);
}

async function getFeedPosts(req, res, next) {
  try {
    const now = new Date();
    const posts = await Post.find({
      activityFeed: true,
      $or: [{ scheduledFor: null }, { scheduledFor: { $lte: now } }],
    })
      .populate("author")
      .populate("comments.user")
      .sort({ createdAt: -1 })
      .limit(50);

    const visiblePosts = posts
      .filter((post) => canUserViewPost(post, String(req.user._id)))
      .map((post) => toFeedPost(post, req.user._id));

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
    const attachmentUrl = normalizeOptionalHref(req.body.attachmentUrl, "Attachment URL");
    const attachmentType = normalizeAttachmentType(req.body.attachmentType);
    const payload = {
      postType: normalizePostType(req.body.postType || req.body.type),
      activityLabel: normalizeOptionalText(req.body.activityLabel || req.body.activity),
      title: normalizeOptionalText(req.body.title),
      content: normalizeOptionalText(req.body.content || req.body.description) || "",
      attachmentUrl,
      attachmentType,
      attachmentName: normalizeOptionalText(req.body.attachmentName),
      displayImageUrl: normalizeOptionalHref(req.body.displayImageUrl || req.body.image, "Image URL"),
      galleryImages: normalizeHrefList(req.body.galleryImages || req.body.images, "Gallery image URL"),
      morePhotosCount: normalizeNonNegativeInteger(req.body.morePhotosCount, 0),
      linkUrl: normalizeOptionalHref(req.body.linkUrl || req.body.href, "Link URL"),
      ctaLabel: normalizeOptionalText(req.body.ctaLabel),
      ctaHref: normalizeOptionalHref(req.body.ctaHref, "Call to action URL"),
      fetchedImageLabel: normalizeOptionalText(req.body.fetchedImageLabel),
      gifPreviewUrl: normalizeOptionalHref(req.body.gifPreviewUrl || req.body.gifPreview, "GIF preview URL"),
      gifDataUrl: normalizeOptionalHref(req.body.gifDataUrl, "GIF data URL"),
      audioSources: normalizeAudioSources(
        req.body.audioSources,
        req.body.audioUrl || (attachmentType === "file" ? attachmentUrl : null),
        req.body.audioMimeType
      ),
      sponsorItems: normalizeSponsorItems(req.body.sponsorItems),
      audience: normalizeAudience(req.body.audience),
      activityFeed: normalizeBoolean(req.body.activityFeed, true),
      myStory: normalizeBoolean(req.body.myStory, true),
      commentsOpen: normalizeBoolean(req.body.commentsOpen, false),
      scheduledFor: normalizeScheduledFor(req.body.scheduledFor),
    };

    if (!hasRenderableContent(payload)) {
      res.status(400).json({
        message: "Add a title, write something, attach media, or provide variant content before publishing.",
      });
      return;
    }

    if (payload.attachmentUrl && !payload.attachmentType) {
      res.status(400).json({ message: "Attachment type is required when an attachment URL is provided." });
      return;
    }

    const createdPost = await Post.create({
      author: req.user._id,
      postType: payload.postType,
      activityLabel: payload.activityLabel,
      title: payload.title,
      content: payload.content,
      attachmentUrl: payload.attachmentUrl,
      attachmentType: payload.attachmentType,
      attachmentName: payload.attachmentName,
      displayImageUrl: payload.displayImageUrl,
      galleryImages: payload.galleryImages,
      morePhotosCount: payload.morePhotosCount,
      linkUrl: payload.linkUrl,
      ctaLabel: payload.ctaLabel,
      ctaHref: payload.ctaHref,
      fetchedImageLabel: payload.fetchedImageLabel,
      gifPreviewUrl: payload.gifPreviewUrl,
      gifDataUrl: payload.gifDataUrl,
      audioSources: payload.audioSources,
      sponsorItems: payload.sponsorItems,
      audience: payload.audience,
      activityFeed: payload.activityFeed,
      myStory: payload.myStory,
      commentsOpen: payload.commentsOpen,
      scheduledFor: payload.scheduledFor,
    });

    const post = await Post.findById(createdPost._id).populate("author");
    const feedPost = toFeedPost(post, req.user._id);

    res.status(201).json({
      message: feedPost.status === "scheduled" ? "Post scheduled successfully." : "Post published successfully.",
      post: feedPost,
      timelinePost: toTimelinePost(post, req.user._id),
    });
  } catch (error) {
    next(error);
  }
}

async function getPostById(req, res, next) {
  try {
    const post = await findPostForViewer(req.params.postId, String(req.user._id));
    if (!post) {
      res.status(404).json({ message: "Post not found." });
      return;
    }

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
    const post = await findPostForViewer(req.params.postId, String(req.user._id));
    if (!post) {
      res.status(404).json({ message: "Post not found." });
      return;
    }

    const reactionType = normalizeReactionType(req.body.reactionType);
    const viewerId = String(req.user._id);
    sanitizePostReactions(post);
    const reactions = getMutableReactions(post);
    const existingReactionIndex = reactions.findIndex(
      (reaction) => getReactionUserId(reaction) === viewerId
    );
    const existingReaction =
      existingReactionIndex >= 0 ? String(reactions[existingReactionIndex]?.type || "").trim().toLowerCase() : null;
    let message = "Reaction saved.";

    if (existingReactionIndex >= 0 && existingReaction === reactionType) {
      post.reactions.splice(existingReactionIndex, 1);
      message = "Reaction removed.";
    } else if (existingReactionIndex >= 0) {
      post.reactions[existingReactionIndex].type = reactionType;
      message = "Reaction updated.";
    } else {
      post.reactions.push({
        user: req.user._id,
        type: reactionType,
      });
      message = "Reaction added.";
    }

    syncLegacyLikes(post);

    await post.save();
    await post.populate("author");
    await post.populate("comments.user");

    res.status(200).json({
      message,
      post: toFeedPost(post, req.user._id),
    });
  } catch (error) {
    next(error);
  }
}

async function togglePostLike(req, res, next) {
  req.body = {
    ...req.body,
    reactionType: "like",
  };

  return reactToPost(req, res, next);
}

async function addPostComment(req, res, next) {
  try {
    const post = await findPostForViewer(req.params.postId, String(req.user._id));
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
    await post.populate("author");
    await post.populate("comments.user");

    res.status(201).json({
      message: "Comment added successfully.",
      post: toFeedPost(post, req.user._id),
    });
  } catch (error) {
    next(error);
  }
}

async function sharePost(req, res, next) {
  try {
    const post = await findPostForViewer(req.params.postId, String(req.user._id));
    if (!post) {
      res.status(404).json({ message: "Post not found." });
      return;
    }

    post.shareCount += 1;
    await post.save();
    await post.populate("author");
    await post.populate("comments.user");

    res.status(200).json({
      message: "Post shared successfully.",
      post: toFeedPost(post, req.user._id),
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
  togglePostLike,
  addPostComment,
  sharePost,
};
