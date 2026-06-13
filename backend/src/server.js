const loadEnv = require("./config/loadEnv");
loadEnv();

const connectDB = require("./config/db");
const app = require("./app");

const parsedPort = Number(process.env.AUTH_PORT || process.env.PORT || 4000);
const PORT = Number.isFinite(parsedPort) && parsedPort > 0 ? parsedPort : 4000;

async function bootstrap() {
  try {
    await connectDB();
    app.listen(PORT, () => {
      console.log(`Auth API running on http://localhost:${PORT}`);
    });
  } catch (error) {
    console.error(`Failed to start auth API: ${error.message}`);
    if (error.cause) {
      console.error(`Cause: ${error.cause.code || "ERROR"} ${error.cause.hostname || ""}`.trim());
    }
    process.exit(1);
  }
}

bootstrap();
