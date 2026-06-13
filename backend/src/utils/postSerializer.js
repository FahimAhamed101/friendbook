const toPublicUser = require("./toPublicUser");
const { getVideoPreview } = require("./videoPreview");

const DEFAULT_AVATAR_URL = "/images/resources/user.jpg";
const DEFAULT_POST_TYPE = "custom";
const ALLOWED_POST_TYPES = new Set(["custom", "article", "premium", "image", "album", "link", "video", "gif", "audio", "sponsor"]);
const REACTION_TYPES = ["like", "love", "haha", "wow", "sad"];

function getAuthorName(user) {
  if (!user) {
    return "Guest User";
  }

  const fullName = `${String(user.firstName || "").trim()} ${String(user.lastName || "").trim()}`.trim();
  return fullName || String(user.email || "Guest User");
}

function getAuthorHandle(user) {
  if (!user) {
    return "@guest";
  }

  const emailPrefix = String(user.email || "").split("@")[0]?.trim();
  if (emailPrefix) {
    return `@${emailPrefix}`;
  }

  const normalizedName = getAuthorName(user).toLowerCase().replace(/[^a-z0-9]+/g, "");
  return `@${normalizedName || "extremis"}`;
}

function toValidDate(value) {
  if (!value) {
    return null;
  }

  const parsed = value instanceof Date ? value : new Date(value);
  return Number.isNaN(parsed.getTime()) ? null : parsed;
}

function formatPublishedDate(value) {
  const parsed = toValidDate(value);
  if (!parsed) {
    return "Not available";
  }

  return new Intl.DateTimeFormat("en-US", {
    month: "short",
    day: "numeric",
    year: "numeric",
    hour: "numeric",
    minute: "2-digit",
  }).format(parsed);
}

function getAttachmentType(post) {
  const normalized = String(post?.attachmentType || "").trim().toLowerCase();
  if (normalized === "image" || normalized === "video" || normalized === "file") {
    return normalized;
  }

  return null;
}

function formatCommentTime(value) {
  const parsed = toValidDate(value);
  if (!parsed) {
    return "Just now";
  }

  return new Intl.DateTimeFormat("en-US", {
    month: "short",
    day: "numeric",
    year: "numeric",
    hour: "numeric",
    minute: "2-digit",
  }).format(parsed);
}

function serializeComment(comment) {
  const userSource = comment?.user && typeof comment.user === "object" ? comment.user : null;
  const user =
    userSource && typeof userSource === "object" && "firstName" in userSource
      ? toPublicUser(userSource)
      : null;

  return {
    id: String(comment?._id || ""),
    userId: user?.id || null,
    name: getAuthorName(user),
    image: String(user?.avatarUrl || DEFAULT_AVATAR_URL).trim() || DEFAULT_AVATAR_URL,
    time: formatCommentTime(comment?.createdAt),
    message: String(comment?.message || "").trim(),
  };
}

function getPostType(post) {
  const normalized = String(post?.postType || DEFAULT_POST_TYPE).trim().toLowerCase();
  return ALLOWED_POST_TYPES.has(normalized) ? normalized : DEFAULT_POST_TYPE;
}

function getDefaultActivity(postType, isScheduled) {
  if (isScheduled) {
    return "scheduled a post";
  }

  switch (postType) {
    case "article":
      return "shared a post";
    case "premium":
      return "shared a premium product";
    case "image":
      return "created a post";
    case "album":
      return "added an image album";
    case "link":
      return "shared a link";
    case "video":
      return "shared a video";
    case "gif":
      return "shared a gif";
    case "audio":
      return "posted audio";
    case "sponsor":
      return "shared sponsored items";
    default:
      return "created a post";
  }
}

function pickFirstNonEmpty(...values) {
  for (const value of values) {
    const normalized = String(value || "").trim();
    if (normalized) {
      return normalized;
    }
  }

  return null;
}

function serializeGalleryImages(post, attachmentType) {
  const galleryImages = Array.isArray(post?.galleryImages)
    ? post.galleryImages.map((entry) => String(entry || "").trim()).filter(Boolean)
    : [];

  if (galleryImages.length > 0) {
    return galleryImages;
  }

  if (getPostType(post) === "album" && attachmentType === "image" && post?.attachmentUrl) {
    return [String(post.attachmentUrl).trim()];
  }

  return [];
}

function serializeAudioSources(post, attachmentType) {
  const audioSources = Array.isArray(post?.audioSources)
    ? post.audioSources
        .map((entry) => {
          const url = String(entry?.url || "").trim();
          if (!url) {
            return null;
          }

          return {
            url,
            mimeType: String(entry?.mimeType || "").trim() || null,
          };
        })
        .filter(Boolean)
    : [];

  if (audioSources.length > 0) {
    return audioSources;
  }

  if (getPostType(post) === "audio" && attachmentType === "file" && post?.attachmentUrl) {
    return [
      {
        url: String(post.attachmentUrl).trim(),
        mimeType: null,
      },
    ];
  }

  return [];
}

function serializeSponsorItems(post) {
  if (!Array.isArray(post?.sponsorItems)) {
    return [];
  }

  return post.sponsorItems
    .map((item) => {
      const title = String(item?.title || "").trim();
      if (!title) {
        return null;
      }

      return {
        id: String(item?._id || title),
        title,
        image: String(item?.imageUrl || "").trim() || null,
        priceLabel: String(item?.priceLabel || "").trim() || null,
        href: String(item?.href || "").trim() || null,
        ctaLabel: String(item?.ctaLabel || "").trim() || "Shop Now",
        shareLabel: String(item?.shareLabel || "").trim() || null,
        likeLabel: String(item?.likeLabel || "").trim() || null,
      };
    })
    .filter(Boolean);
}

function serializeReactions(post) {
  const rawReactions = Array.isArray(post?.reactions) ? post.reactions : [];
  if (rawReactions.length > 0) {
    return rawReactions.reduce((entries, reaction) => {
      const userId =
        reaction?.user && typeof reaction.user === "object" && reaction.user._id
          ? String(reaction.user._id)
          : String(reaction?.user || "").trim();
      const type = String(reaction?.type || "").trim().toLowerCase();

      if (!userId || !REACTION_TYPES.includes(type)) {
        return entries;
      }

      entries.push({ userId, type });
      return entries;
    }, []);
  }

  return Array.isArray(post?.likes)
    ? post.likes.reduce((entries, user) => {
        const userId =
          user && typeof user === "object" && user._id
            ? String(user._id)
            : String(user || "").trim();

        if (!userId) {
          return entries;
        }

        entries.push({ userId, type: "like" });
        return entries;
      }, [])
    : [];
}

function serializePost(post, viewerId) {
  const authorSource = post?.author && typeof post.author === "object" ? post.author : null;
  const author =
    authorSource && typeof authorSource === "object" && "firstName" in authorSource
      ? toPublicUser(authorSource)
      : null;
  const createdAt = toValidDate(post?.createdAt) || new Date();
  const scheduledFor = toValidDate(post?.scheduledFor);
  const isScheduled = Boolean(scheduledFor && scheduledFor.getTime() > Date.now());
  const publishedAt = isScheduled ? scheduledFor : createdAt;
  const attachmentType = getAttachmentType(post);
  const postType = getPostType(post);
  const serializedComments = Array.isArray(post?.comments) ? post.comments.map((comment) => serializeComment(comment)) : [];
  const { embedUrl, videoUrl } = getVideoPreview(post?.linkUrl);
  const serializedReactions = serializeReactions(post);
  const reactionCounts = serializedReactions.reduce(
    (counts, reaction) => {
      counts[reaction.type] += 1;
      return counts;
    },
    {
      like: 0,
      love: 0,
      haha: 0,
      wow: 0,
      sad: 0,
    }
  );
  const topReactions = REACTION_TYPES
    .filter((type) => reactionCounts[type] > 0)
    .sort((left, right) => reactionCounts[right] - reactionCounts[left])
    .slice(0, 3);
  const viewerReaction =
    serializedReactions.find((reaction) => reaction.userId === String(viewerId || ""))?.type || null;
  const likeCount = serializedReactions.length;
  const commentCount = serializedComments.length;
  const shareCount = Number(post?.shareCount || 0);
  const title = String(post?.title || "").trim() || null;
  const description = String(post?.content || "").trim();
  const image = pickFirstNonEmpty(post?.displayImageUrl, attachmentType === "image" ? post?.attachmentUrl : null);
  const galleryImages = serializeGalleryImages(post, attachmentType);
  const audioSources = serializeAudioSources(post, attachmentType);
  const sponsorItems = serializeSponsorItems(post);
  const href = pickFirstNonEmpty(post?.linkUrl, post?.ctaHref, "#") || "#";

  return {
    id: String(post?._id || post?.id || ""),
    type: postType,
    authorId: author?.id || null,
    authorName: getAuthorName(author),
    authorHandle: getAuthorHandle(author),
    authorImage: String(author?.avatarUrl || DEFAULT_AVATAR_URL).trim() || DEFAULT_AVATAR_URL,
    activity: String(post?.activityLabel || "").trim() || getDefaultActivity(postType, isScheduled),
    published: formatPublishedDate(publishedAt),
    title,
    content: description,
    description,
    href,
    image,
    images: galleryImages,
    morePhotosCount: Number(post?.morePhotosCount || 0),
    attachmentUrl: String(post?.attachmentUrl || "").trim() || null,
    attachmentType,
    attachmentName: String(post?.attachmentName || "").trim() || null,
    linkUrl: String(post?.linkUrl || "").trim() || null,
    ctaLabel: String(post?.ctaLabel || "").trim() || null,
    ctaHref: String(post?.ctaHref || "").trim() || null,
    fetchedImageLabel: String(post?.fetchedImageLabel || "").trim() || null,
    gifPreview: String(post?.gifPreviewUrl || "").trim() || null,
    gifDataUrl: String(post?.gifDataUrl || "").trim() || null,
    audioSources,
    sponsorItems,
    commentsOpen: post?.commentsOpen === true,
    audience: String(post?.audience || "joined-groups").trim() || "joined-groups",
    activityFeed: post?.activityFeed !== false,
    myStory: post?.myStory !== false,
    scheduledFor: scheduledFor ? scheduledFor.toISOString() : null,
    createdAt: createdAt.toISOString(),
    status: isScheduled ? "scheduled" : "published",
    embedUrl,
    videoUrl,
    comments: serializedComments,
    stats: {
      viewCount: Math.max(1, likeCount + commentCount + shareCount + 1),
      likeCount,
      commentCount,
      shareCount,
      likedByViewer: Boolean(viewerReaction),
      viewerReaction,
      reactionCounts,
      topReactions,
    },
  };
}

function toFeedPost(post, viewerId) {
  return serializePost(post, viewerId);
}

function toTimelinePost(post, viewerId) {
  return serializePost(post, viewerId);
}

module.exports = {
  toFeedPost,
  toTimelinePost,
};
