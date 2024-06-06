import { defineConfig } from "vite";
import scalaJSPlugin from "@scala-js/vite-plugin-scalajs";

export default defineConfig({
  plugins: [
    scalaJSPlugin({
      cwd: "../../",
      projectID: "frontend",
    }),
  ],
  server: {
    proxy: {
      // For requests to /api/**, drop the prefix and proxy the rest to the backend.
      "/api": {
        target: "http://localhost:8080",
        rewrite: (path) => path.replace(/^\/api/, ""),
      },
    },
  },
});
