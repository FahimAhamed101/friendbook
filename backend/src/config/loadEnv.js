const fs = require("fs");
const path = require("path");
const dotenv = require("dotenv");

let loaded = false;

function loadEnv() {
  if (loaded) {
    return;
  }

  const envCandidates = [
    path.resolve(process.cwd(), ".env"),
    path.resolve(process.cwd(), "backend/.env"),
    path.resolve(__dirname, "../../.env"),
    path.resolve(__dirname, "../../../.env"),
    path.resolve(__dirname, "../.env"),
  ];

  // Load every discovered env file so missing keys in one file can be
  // fulfilled by another (for example backend/.env after root .env).
  // Existing process env values are preserved by dotenv default behavior.
  for (const envPath of [...new Set(envCandidates)]) {
    if (fs.existsSync(envPath)) {
      dotenv.config({ path: envPath, quiet: true });
    }
  }

  loaded = true;
}

module.exports = loadEnv;
