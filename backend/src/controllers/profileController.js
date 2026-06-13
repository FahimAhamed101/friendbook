const mongoose = require("mongoose");
const User = require("../models/User");
const toPublicUser = require("../utils/toPublicUser");
const Post = require("../models/Post");
const { toTimelinePost } = require("../utils/postSerializer");
const {
  videos,
  comments,
  timeline,
  researchImages,
  events,
} = require("../utils/profileFixtures");

function normalizeOptionalText(value) {
  const normalized = String(value || "").trim();
  return normalized || null;
}

function normalizeOptionalUrl(value) {
  const normalized = String(value || "").trim();
  if (!normalized) {
    return null;
  }

  try {
    return new URL(normalized).toString();
  } catch {
    return null;
  }
}

function normalizeStringArray(value) {
  const entries = Array.isArray(value)
    ? value
    : typeof value === "string"
      ? value.split(/[,\n]/g)
      : [];

  return Array.from(
    new Set(
      entries
        .map((entry) => String(entry || "").trim())
        .filter(Boolean)
        .slice(0, 12)
    )
  );
}

function getFullName(user) {
  return `${String(user.firstName || "").trim()} ${String(user.lastName || "").trim()}`.trim() || user.email;
}

function getHandle(user) {
  const username = String(user.username || "").trim();
  if (username) {
    return `@${username}`;
  }

  const emailPrefix = String(user.email || "").split("@")[0]?.trim();
  if (emailPrefix) {
    return `@${emailPrefix}`;
  }

  return `@${getFullName(user).toLowerCase().replace(/[^a-z0-9]+/g, "") || "researcher"}`;
}

function formatDate(value) {
  if (!value) {
    return "Not available";
  }

  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return "Not available";
  }

  return new Intl.DateTimeFormat("en-US", {
    month: "short",
    day: "numeric",
    year: "numeric",
  }).format(parsed);
}

function getCompletion(user) {
  const publicUser =
    user && typeof user === "object" && "id" in user && !("_id" in user)
      ? user
      : toPublicUser(user);
  const fields = [
    publicUser.firstName,
    publicUser.lastName,
    publicUser.email,
    publicUser.researcherType,
    publicUser.institute,
    publicUser.department,
    publicUser.position,
    publicUser.gender,
    publicUser.avatarUrl,
    publicUser.coverImageUrl,
    publicUser.bio,
    publicUser.location,
    publicUser.website,
    publicUser.disciplines.length ? "disciplines" : "",
    publicUser.skills.length ? "skills" : "",
  ];

  const completed = fields.filter((field) => String(field || "").trim()).length;
  return Math.round((completed / fields.length) * 100);
}

function getObjectIdStrings(value) {
  if (!Array.isArray(value)) {
    return [];
  }

  return Array.from(
    new Set(
      value
        .map((entry) => {
          if (!entry) {
            return null;
          }

          if (typeof entry === "string") {
            return entry;
          }

          if (typeof entry === "object" && "_id" in entry) {
            return String(entry._id);
          }

          return String(entry);
        })
        .filter(Boolean)
    )
  );
}

function parsePositiveInteger(value, fallback, max = 60) {
  const parsed = Number.parseInt(String(value || ""), 10);

  if (!Number.isFinite(parsed) || parsed <= 0) {
    return fallback;
  }

  return Math.min(parsed, max);
}

function getPersonSubtitle(publicUser) {
  return (
    publicUser.department ||
    publicUser.position ||
    publicUser.institute ||
    publicUser.researcherType ||
    "Researcher"
  );
}

function buildPersonCard(user, viewerFollowingSet, viewerId) {
  const publicUser = toPublicUser(user);
  const userId = publicUser.id;
  const isViewer = viewerId ? viewerId === userId : false;
  const isFollowing = !isViewer && viewerFollowingSet.has(userId);

  return {
    id: userId,
    profileHref: `/profile/${userId}`,
    name: getFullName(publicUser),
    subtitle: getPersonSubtitle(publicUser),
    image: publicUser.avatarUrl || "/images/resources/user.jpg",
    actionLabel: isViewer ? "You" : isFollowing ? "Following" : "Follow",
    isFollowing,
    canFollow: !isViewer,
  };
}

async function buildNetworkPayload(profileUser, viewerUser) {
  const profileUserId = String(profileUser._id);
  const viewerUserId = String(viewerUser._id);
  const profileFollowingIds = getObjectIdStrings(profileUser.following);
  const viewerFollowingIds = getObjectIdStrings(viewerUser.following);
  const viewerFollowingSet = new Set(viewerFollowingIds);
  const suggestionExcludedIds = Array.from(
    new Set([viewerUserId, profileUserId, ...viewerFollowingIds])
  );

  const [followerCount, followerDocs, followingDocs, suggestionDocs] = await Promise.all([
    User.countDocuments({ following: profileUser._id }),
    User.find({ following: profileUser._id }).sort({ createdAt: -1 }).limit(24),
    profileFollowingIds.length
      ? User.find({ _id: { $in: profileFollowingIds } })
      : Promise.resolve([]),
    User.find({ _id: { $nin: suggestionExcludedIds } }).sort({ createdAt: -1 }).limit(8),
  ]);

  const followingDocMap = new Map(
    followingDocs.map((user) => [String(user._id), user])
  );

  const orderedFollowingDocs = profileFollowingIds
    .map((userId) => followingDocMap.get(userId))
    .filter(Boolean);

  const followerCards = followerDocs.map((user) =>
    buildPersonCard(user, viewerFollowingSet, viewerUserId)
  );
  const followingCards = orderedFollowingDocs.map((user) =>
    buildPersonCard(user, viewerFollowingSet, viewerUserId)
  );
  const suggestionCards = suggestionDocs.map((user) =>
    buildPersonCard(user, viewerFollowingSet, viewerUserId)
  );

  return {
    followers: followerCards,
    following: followingCards,
    suggestions: suggestionCards,
    whoIsFollowing: followerCards.slice(0, 5),
    stats: {
      followerCount,
      followingCount: profileFollowingIds.length,
    },
  };
}

function buildProfilePayload(user, stats = {}) {
  const publicUser = toPublicUser(user);
  const fullName = getFullName(publicUser);
  const handle = getHandle(publicUser);
  const institute = publicUser.institute || "Oxford University";
  const department = publicUser.department || "Department not added";
  const position = publicUser.position || "Professor Associate";
  const researcherType = publicUser.researcherType || "Educational leadership";
  const gender = publicUser.gender || "Not specified";
  const avatarUrl = publicUser.avatarUrl || "/images/resources/user.jpg";
  const coverImageUrl = publicUser.coverImageUrl || "/images/resources/top-bg.jpg";
  const location = publicUser.location || [department, institute].filter(Boolean).join(", ");
  const completion = getCompletion(user);
  const disciplines =
    publicUser.disciplines.length > 0
      ? publicUser.disciplines
      : [
          researcherType,
          department,
          "Educational assessment",
          "Educational management",
          "Social Psychology",
          "Qualitative social research",
        ];
  const skills =
    publicUser.skills.length > 0
      ? publicUser.skills
      : [
          position,
          institute,
          "Research collaboration",
          "Mentoring",
          "Conference speaking",
          `Profile completion ${completion}%`,
        ];

  return {
    user: publicUser,
    fullName,
    handle,
    institute,
    department,
    position,
    researcherType,
    gender,
    avatarUrl,
    coverImageUrl,
    location,
    joined: formatDate(publicUser.createdAt),
    completion,
    disciplines: Array.from(new Set(disciplines.filter(Boolean))),
    skills: Array.from(new Set(skills.filter(Boolean))),
    bio:
      publicUser.bio ||
      `${fullName} is building research collaborations, sharing field notes, and contributing to academic conversations across the Extremis network.`,
    headline: `${position} at ${institute}`,
    contact: {
      emailAddress: publicUser.email,
      phoneNumber: publicUser.phoneNumber || "Not added",
      skypeId: publicUser.skypeId || "Not added",
      website: publicUser.website || "Not added",
      localTime: publicUser.localTime || "3:40AM",
    },
    analytics: {
      profileCompletion: completion,
      researcherType,
      institute,
      joined: formatDate(publicUser.createdAt),
      followerCount: Number.isFinite(stats.followerCount) ? stats.followerCount : 0,
      followingCount: Number.isFinite(stats.followingCount) ? stats.followingCount : 0,
    },
  };
}

async function loadProfileTimeline(profileUserId, viewerId = profileUserId) {
  const userPosts = await Post.find({ author: profileUserId })
    .populate("author")
    .populate("comments.user")
    .sort({ createdAt: -1 })
    .limit(20);

  return [...userPosts.map((post) => toTimelinePost(post, viewerId)), ...timeline];
}

async function getMyProfile(req, res, next) {
  try {
    const [profileTimeline, network] = await Promise.all([
      loadProfileTimeline(req.user._id),
      buildNetworkPayload(req.user, req.user),
    ]);

    res.status(200).json({
      message: "Profile loaded successfully.",
      profile: buildProfilePayload(req.user, network.stats),
      timeline: profileTimeline,
      network,
      media: {
        videos,
        researchImages,
      },
      events,
      comments,
    });
  } catch (error) {
    next(error);
  }
}

async function getProfileById(req, res, next) {
  try {
    const { userId } = req.params;

    if (!mongoose.Types.ObjectId.isValid(userId)) {
      res.status(404).json({ message: "Profile not found." });
      return;
    }

    const profileUser = await User.findById(userId);

    if (!profileUser) {
      res.status(404).json({ message: "Profile not found." });
      return;
    }

    const [profileTimeline, network] = await Promise.all([
      loadProfileTimeline(profileUser._id, req.user._id),
      buildNetworkPayload(profileUser, req.user),
    ]);

    res.status(200).json({
      message: "Profile loaded successfully.",
      profile: buildProfilePayload(profileUser, network.stats),
      timeline: profileTimeline,
      network,
      media: {
        videos,
        researchImages,
      },
      events,
      comments,
    });
  } catch (error) {
    next(error);
  }
}

async function getDiscoverPeople(req, res, next) {
  try {
    const viewerUserId = String(req.user._id);
    const viewerFollowingIds = getObjectIdStrings(req.user.following);
    const viewerFollowingSet = new Set(viewerFollowingIds);
    const limit = parsePositiveInteger(req.query.limit, 24);

    const users = await User.find({ _id: { $ne: req.user._id } })
      .sort({ createdAt: -1 })
      .limit(limit);

    res.status(200).json({
      message: "People loaded successfully.",
      users: users.map((user) => buildPersonCard(user, viewerFollowingSet, viewerUserId)),
    });
  } catch (error) {
    next(error);
  }
}

async function updateMyProfile(req, res, next) {
  try {
    const updatableFields = [
      "firstName",
      "lastName",
      "researcherType",
      "institute",
      "department",
      "position",
      "gender",
      "bio",
      "location",
      "phoneNumber",
      "skypeId",
      "localTime",
    ];

    let didUpdate = false;

    updatableFields.forEach((field) => {
      if (!Object.prototype.hasOwnProperty.call(req.body, field)) {
        return;
      }

      const value = normalizeOptionalText(req.body[field]);
      if ((field === "firstName" || field === "lastName") && !value) {
        return;
      }

      req.user[field] = value;
      didUpdate = true;
    });

    if (Object.prototype.hasOwnProperty.call(req.body, "website")) {
      const website = normalizeOptionalUrl(req.body.website);
      if (req.body.website && !website) {
        res.status(400).json({ message: "Website must be a valid URL." });
        return;
      }

      req.user.website = website;
      didUpdate = true;
    }

    if (Object.prototype.hasOwnProperty.call(req.body, "avatarUrl")) {
      const avatarUrl = normalizeOptionalUrl(req.body.avatarUrl);
      if (req.body.avatarUrl && !avatarUrl) {
        res.status(400).json({ message: "Avatar URL must be a valid URL." });
        return;
      }

      req.user.avatarUrl = avatarUrl;
      didUpdate = true;
    }

    if (Object.prototype.hasOwnProperty.call(req.body, "coverImageUrl")) {
      const coverImageUrl = normalizeOptionalUrl(req.body.coverImageUrl);
      if (req.body.coverImageUrl && !coverImageUrl) {
        res.status(400).json({ message: "Cover image URL must be a valid URL." });
        return;
      }

      req.user.coverImageUrl = coverImageUrl;
      didUpdate = true;
    }

    if (Object.prototype.hasOwnProperty.call(req.body, "disciplines")) {
      req.user.disciplines = normalizeStringArray(req.body.disciplines);
      didUpdate = true;
    }

    if (Object.prototype.hasOwnProperty.call(req.body, "skills")) {
      req.user.skills = normalizeStringArray(req.body.skills);
      didUpdate = true;
    }

    if (!didUpdate) {
      res.status(400).json({ message: "No profile fields were provided." });
      return;
    }

    if (!String(req.user.firstName || "").trim() || !String(req.user.lastName || "").trim()) {
      res.status(400).json({ message: "First name and last name cannot be empty." });
      return;
    }

    await req.user.save();
    const [profileTimeline, network] = await Promise.all([
      loadProfileTimeline(req.user._id),
      buildNetworkPayload(req.user, req.user),
    ]);

    res.status(200).json({
      message: "Profile updated successfully.",
      profile: buildProfilePayload(req.user, network.stats),
      timeline: profileTimeline,
      network,
      media: {
        videos,
        researchImages,
      },
      events,
      comments,
    });
  } catch (error) {
    next(error);
  }
}

async function toggleFollowUser(req, res, next) {
  try {
    const { userId } = req.params;

    if (!mongoose.Types.ObjectId.isValid(userId)) {
      res.status(404).json({ message: "User not found." });
      return;
    }

    if (String(req.user._id) === userId) {
      res.status(400).json({ message: "You cannot follow yourself." });
      return;
    }

    const targetUser = await User.findById(userId);

    if (!targetUser) {
      res.status(404).json({ message: "User not found." });
      return;
    }

    const currentFollowingIds = getObjectIdStrings(req.user.following);
    const isFollowing = currentFollowingIds.includes(userId);

    if (isFollowing) {
      req.user.following = currentFollowingIds.filter((followedUserId) => followedUserId !== userId);
    } else {
      req.user.following = [...currentFollowingIds, userId];
    }

    await req.user.save();

    res.status(200).json({
      message: isFollowing ? "User unfollowed successfully." : "User followed successfully.",
      targetUserId: userId,
      isFollowing: !isFollowing,
    });
  } catch (error) {
    next(error);
  }
}

module.exports = {
  getDiscoverPeople,
  getProfileById,
  getMyProfile,
  toggleFollowUser,
  updateMyProfile,
};
