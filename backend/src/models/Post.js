const mongoose = require("mongoose");

const postCommentSchema = new mongoose.Schema(
  {
    user: {
      type: mongoose.Schema.Types.ObjectId,
      ref: "User",
      required: true,
    },
    message: {
      type: String,
      required: true,
      trim: true,
      maxlength: 2000,
    },
  },
  {
    _id: true,
    timestamps: true,
  }
);

const postReactionSchema = new mongoose.Schema(
  {
    user: {
      type: mongoose.Schema.Types.ObjectId,
      ref: "User",
      required: true,
    },
    type: {
      type: String,
      enum: ["like", "love", "haha", "wow", "sad"],
      required: true,
      default: "like",
    },
  },
  {
    _id: true,
    timestamps: true,
  }
);

const postAudioSourceSchema = new mongoose.Schema(
  {
    url: {
      type: String,
      required: true,
    },
    mimeType: {
      type: String,
      default: null,
      trim: true,
    },
  },
  {
    _id: false,
  }
);

const sponsorItemSchema = new mongoose.Schema(
  {
    title: {
      type: String,
      required: true,
      trim: true,
      maxlength: 200,
    },
    imageUrl: {
      type: String,
      default: null,
    },
    priceLabel: {
      type: String,
      default: null,
      trim: true,
      maxlength: 60,
    },
    href: {
      type: String,
      default: null,
    },
    ctaLabel: {
      type: String,
      default: "Shop Now",
      trim: true,
      maxlength: 80,
    },
    shareLabel: {
      type: String,
      default: null,
      trim: true,
      maxlength: 80,
    },
    likeLabel: {
      type: String,
      default: null,
      trim: true,
      maxlength: 80,
    },
  },
  {
    _id: true,
  }
);

const postSchema = new mongoose.Schema(
  {
    author: {
      type: mongoose.Schema.Types.ObjectId,
      ref: "User",
      required: true,
      index: true,
    },
    postType: {
      type: String,
      enum: ["custom", "article", "premium", "image", "album", "link", "video", "gif", "audio", "sponsor"],
      default: "custom",
      index: true,
    },
    activityLabel: {
      type: String,
      default: null,
      trim: true,
      maxlength: 120,
    },
    title: {
      type: String,
      default: null,
      trim: true,
      maxlength: 300,
    },
    content: {
      type: String,
      default: "",
      trim: true,
      maxlength: 4000,
    },
    attachmentUrl: {
      type: String,
      default: null,
    },
    attachmentType: {
      type: String,
      default: null,
      validate(value) {
        return value == null || value === "image" || value === "video" || value === "file";
      },
    },
    attachmentName: {
      type: String,
      default: null,
      trim: true,
    },
    displayImageUrl: {
      type: String,
      default: null,
    },
    galleryImages: {
      type: [String],
      default: [],
    },
    morePhotosCount: {
      type: Number,
      default: 0,
      min: 0,
    },
    linkUrl: {
      type: String,
      default: null,
    },
    ctaLabel: {
      type: String,
      default: null,
      trim: true,
      maxlength: 120,
    },
    ctaHref: {
      type: String,
      default: null,
    },
    fetchedImageLabel: {
      type: String,
      default: null,
      trim: true,
      maxlength: 120,
    },
    gifPreviewUrl: {
      type: String,
      default: null,
    },
    gifDataUrl: {
      type: String,
      default: null,
    },
    audioSources: {
      type: [postAudioSourceSchema],
      default: [],
    },
    sponsorItems: {
      type: [sponsorItemSchema],
      default: [],
    },
    audience: {
      type: String,
      enum: ["public", "private", "specific-friend", "only-friends", "joined-groups"],
      default: "joined-groups",
    },
    activityFeed: {
      type: Boolean,
      default: true,
    },
    myStory: {
      type: Boolean,
      default: true,
    },
    commentsOpen: {
      type: Boolean,
      default: false,
    },
    scheduledFor: {
      type: Date,
      default: null,
    },
    likes: {
      type: [
        {
          type: mongoose.Schema.Types.ObjectId,
          ref: "User",
        },
      ],
      default: [],
    },
    reactions: {
      type: [postReactionSchema],
      default: [],
    },
    comments: {
      type: [postCommentSchema],
      default: [],
    },
    shareCount: {
      type: Number,
      default: 0,
      min: 0,
    },
  },
  {
    timestamps: true,
  }
);

module.exports = mongoose.model("Post", postSchema);
