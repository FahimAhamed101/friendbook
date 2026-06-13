function notFound(req, res) {
  res.status(404).json({ message: "Endpoint not found." });
}

function getDuplicateKeyMessage(err) {
  const duplicateField = Object.keys(err?.keyPattern || err?.keyValue || {})[0];

  if (duplicateField === "email") {
    return "An account with this email already exists.";
  }

  return "A record with the same unique value already exists.";
}

function errorHandler(err, req, res, next) {
  if (res.headersSent) {
    next(err);
    return;
  }

  console.error(err);
  if (err?.code === 11000) {
    res.status(409).json({
      message: getDuplicateKeyMessage(err),
    });
    return;
  }

  if (err?.name === "ValidationError") {
    const firstMessage = Object.values(err.errors || {})[0]?.message;
    res.status(400).json({
      message: firstMessage || "Validation failed.",
    });
    return;
  }

  const explicitStatus = Number(err?.statusCode || err?.status);
  const statusCode =
    Number.isInteger(explicitStatus) && explicitStatus >= 400
      ? explicitStatus
      : res.statusCode >= 400
      ? res.statusCode
      : 500;

  res.status(statusCode).json({
    message:
      statusCode === 500 && !err?.expose ? "Internal server error." : err.message || "Request failed.",
  });
}

module.exports = {
  notFound,
  errorHandler,
};
