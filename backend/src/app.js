const cors = require("cors");
const express = require("express");
const authRoutes = require("./routes/authRoutes");
const chatRoutes = require("./routes/chatRoutes");
const postRoutes = require("./routes/postRoutes");
const profileRoutes = require("./routes/profileRoutes");
const uploadRoutes = require("./routes/uploadRoutes");
const groupRoutes = require("./routes/groupRoutes");
const { notFound, errorHandler } = require("./middleware/errorHandler");

function getAllowedOrigins() {
  const configuredOrigins = String(process.env.CLIENT_ORIGIN || "")
    .split(",")
    .map((origin) => origin.trim())
    .filter(Boolean);

  if (process.env.NODE_ENV === "production") {
    return configuredOrigins;
  }

  return [...new Set([...configuredOrigins, "http://localhost:3000"])];
}

const allowedOrigins = getAllowedOrigins();

const app = express();
const apiRouter = express.Router();
app.use(
  cors({
    origin(origin, callback) {
      if (!origin || allowedOrigins.length === 0 || allowedOrigins.includes(origin)) {
        callback(null, true);
        return;
      }

      callback(new Error("Origin is not allowed by CORS."));
    },
    credentials: true,
  })
);

app.use(express.json());
app.use(express.urlencoded({ extended: true }));
app.use((req, res, next) => {
  if (req.body == null) {
    req.body = {};
    next();
    return;
  }

  if (Buffer.isBuffer(req.body)) {
    try {
      req.body = JSON.parse(req.body.toString("utf8"));
    } catch {
      req.body = {};
    }

    next();
    return;
  }

  if (typeof req.body === "string") {
    const rawBody = req.body.trim();
    if (!rawBody) {
      req.body = {};
      next();
      return;
    }

    try {
      req.body = JSON.parse(rawBody);
    } catch {
      req.body = {};
    }
  }

  next();
});

apiRouter.get("/health", (req, res) => {
  res.status(200).json({
    ok: true,
    service: "auth-api",
    timestamp: new Date().toISOString(),
  });
});

apiRouter.use("/auth", authRoutes);
apiRouter.use("/chat", chatRoutes);
apiRouter.use("/posts", postRoutes);
apiRouter.use("/profile", profileRoutes);
apiRouter.use("/uploads", uploadRoutes);
apiRouter.use("/groups", groupRoutes);

// The route modules above remain the source of truth for every backend endpoint.
// Vercel forwards /api/* into this app through a single catch-all function, while
// local backend development still uses the direct /api/* mount below.
app.use("/api", apiRouter);
app.use(apiRouter);

app.use(notFound);
app.use(errorHandler);

module.exports = app;
