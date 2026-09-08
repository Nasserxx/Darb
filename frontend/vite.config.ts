import path from "node:path";
import tailwindcss from "@tailwindcss/vite";
import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  // Base path the SPA is served under. Set VITE_BASE_PATH (e.g. "/darb/")
  // when deploying behind a reverse-proxy subpath; defaults to "/".
  base: process.env.VITE_BASE_PATH || "/",
  server: {
    port: 3000,
  },
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
});
