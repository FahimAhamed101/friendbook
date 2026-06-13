const jwt = require("jsonwebtoken");

function createConfigError(message) {
  const error = new Error(message);
  error.statusCode = 500;
  error.expose = true;
  return error;
}

function getJwtSecret() {
  const secret = process.env.JWT_SECRET;
  if (!secret) {
    throw createConfigError("JWT_SECRET is missing. Add it to your environment variables.");
  }

  return secret;
}

function generateToken(userId) {
  const secret = getJwtSecret();

  return jwt.sign({ sub: userId }, secret, {
    expiresIn: process.env.JWT_EXPIRES_IN || "7d",
  });
}

module.exports = generateToken;
module.exports.getJwtSecret = getJwtSecret;
