const DIRECT_VIDEO_EXTENSION_PATTERN = /\.(mp4|webm|ogg|mov|m4v)(\?.*)?$/i;

function safeParseUrl(value) {
  const normalized = String(value || "").trim();
  if (!normalized) {
    return null;
  }

  try {
    return new URL(normalized);
  } catch {
    return null;
  }
}

function getYouTubeId(url) {
  if (!url) {
    return null;
  }

  const hostname = url.hostname.replace(/^www\./i, "").toLowerCase();
  if (hostname === "youtu.be") {
    const id = url.pathname.replace(/^\/+/g, "").split("/")[0];
    return id || null;
  }

  if (hostname === "youtube.com" || hostname === "m.youtube.com") {
    if (url.pathname === "/watch") {
      return url.searchParams.get("v");
    }

    if (url.pathname.startsWith("/embed/") || url.pathname.startsWith("/shorts/")) {
      const id = url.pathname.split("/")[2];
      return id || null;
    }
  }

  return null;
}

function getVimeoId(url) {
  if (!url) {
    return null;
  }

  const hostname = url.hostname.replace(/^www\./i, "").toLowerCase();
  if (hostname !== "vimeo.com" && hostname !== "player.vimeo.com") {
    return null;
  }

  const match = url.pathname.match(/\/(\d+)(?:$|\/)/);
  return match ? match[1] : null;
}

function getVideoPreview(linkUrl) {
  const normalized = String(linkUrl || "").trim();
  if (!normalized) {
    return {
      embedUrl: null,
      videoUrl: null,
    };
  }

  if (DIRECT_VIDEO_EXTENSION_PATTERN.test(normalized)) {
    return {
      embedUrl: null,
      videoUrl: normalized,
    };
  }

  const url = safeParseUrl(linkUrl);
  if (!url) {
    return {
      embedUrl: null,
      videoUrl: null,
    };
  }

  const youTubeId = getYouTubeId(url);
  if (youTubeId) {
    return {
      embedUrl: `https://www.youtube.com/embed/${youTubeId}`,
      videoUrl: null,
    };
  }

  const vimeoId = getVimeoId(url);
  if (vimeoId) {
    return {
      embedUrl: `https://player.vimeo.com/video/${vimeoId}`,
      videoUrl: null,
    };
  }

  if (DIRECT_VIDEO_EXTENSION_PATTERN.test(url.pathname)) {
    return {
      embedUrl: null,
      videoUrl: url.toString(),
    };
  }

  return {
    embedUrl: null,
    videoUrl: null,
  };
}

module.exports = {
  getVideoPreview,
};
