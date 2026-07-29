import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";

const apiTarget = process.env.CAMPUSLINK_VITE_API_TARGET || "http://127.0.0.1:8080";
const websocketTarget = process.env.CAMPUSLINK_VITE_WS_TARGET || "ws://127.0.0.1:8080";

export default defineConfig({
  plugins: [vue()],
  server: {
    host: "127.0.0.1",
    port: 5180,
    strictPort: true,
    proxy: {
      "/api": apiTarget,
      "/ws": {
        target: websocketTarget,
        ws: true
      }
    }
  },
  test: {
    environment: "node"
  }
});
