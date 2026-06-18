import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      // Forward API calls to the Spring Boot backend, avoiding CORS in dev.
      '/api': 'http://localhost:8080',
    },
  },
})
