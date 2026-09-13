import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
export default defineConfig({
  plugins: [react()],
  server: { proxy: { '/demo-api': { target: 'http://localhost:3100', changeOrigin: true, rewrite: (path) => path.replace(/^\/demo-api/, '') } } },
})
