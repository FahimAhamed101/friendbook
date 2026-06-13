const mongoose = require("mongoose");

let connectionPromise = null;
const serverSelectionTimeoutMS = Number(process.env.MONGODB_SERVER_SELECTION_TIMEOUT_MS || 10000);

function getMongooseOptions() {
  return {
    dbName: process.env.MONGODB_DB || undefined,
    serverSelectionTimeoutMS,
  };
}

function createConfigError(message) {
  const error = new Error(message);
  error.statusCode = 500;
  error.expose = true;
  return error;
}

function isSrvLookupError(error, uri) {
  return (
    uri &&
    uri.startsWith("mongodb+srv") &&
    error &&
    error.syscall === "querySrv"
  );
}

function createSrvLookupError(error) {
  const message = [
    `MongoDB SRV DNS lookup failed for ${error.hostname || "the Atlas cluster"}.`,
    "Use a non-SRV MongoDB URI locally, or add MONGODB_LOCAL_URI as a fallback.",
    "Example: mongodb://user:pass@host1:27017,host2:27017,host3:27017/db?tls=true&authSource=admin&retryWrites=true&w=majority",
  ].join(" ");

  const configError = createConfigError(message);
  configError.cause = error;
  configError.code = error.code;
  return configError;
}

async function connectDB() {
  const uri = process.env.MONGODB_URI;
  if (!uri) {
    throw createConfigError("MONGODB_URI is missing. Add it to your environment variables.");
  }

  if (mongoose.connection.readyState === 1) {
    return mongoose.connection;
  }

  if (mongoose.connection.readyState === 2 && connectionPromise) {
    return connectionPromise;
  }

  if (!connectionPromise) {
    connectionPromise = (async () => {
      try {
        return await mongoose.connect(uri, getMongooseOptions());
      } catch (error) {
        // Reset so future attempts can retry
        connectionPromise = null;

        // If the error looks like a DNS SRV lookup failure for Atlas,
        // offer a helpful message and attempt an optional local fallback
        // when `MONGODB_LOCAL_URI` is provided in the environment.
        const isSrvError = isSrvLookupError(error, uri);
        const localUri = process.env.MONGODB_LOCAL_URI;

        if (isSrvError && localUri) {
          // Try local fallback
          // eslint-disable-next-line no-console
          console.warn(
            'MongoDB SRV lookup failed. Attempting fallback to MONGODB_LOCAL_URI.'
          );
          try {
            return await mongoose.connect(localUri, getMongooseOptions());
          } catch (localErr) {
            // If fallback fails, throw the original error to preserve context
            throw error;
          }
        }

        throw isSrvError ? createSrvLookupError(error) : error;
      }
    })();
  }

  await connectionPromise;
  return mongoose.connection;
}

module.exports = connectDB;
