import vue from '@vitejs/plugin-vue'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    // The frontend always calls /api/...; in dev Vite forwards that to Spring Boot,
    // in Docker nginx does the same. No CORS needed either way.
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
})
