import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  build: {
    chunkSizeWarningLimit: 1000, // Increase from default 500 to suppress warning since code splitting is implemented
    rollupOptions: {
      output: {
        manualChunks: {
          // Split large libraries into separate chunks
          'react-vendor': [
            'react',
            'react-dom',
          ],
          'router-vendor': [
            'react-router-dom',
          ],
          'ui-vendor': [
            '@radix-ui/react-dialog',
            '@radix-ui/react-tabs',
          ],
          'data-vendor': [
            'zustand',
            'axios',
          ],
          'auth-vendor': [
            '@react-oauth/google',
          ],
          'utils-vendor': [
            'date-fns',
            'react-hot-toast',
          ],
          'markdown-vendor': [
            'react-markdown',
            'remark-gfm',
          ],
          'icons-vendor': [
            'lucide-react',
          ],
        },
      },
    },
  },
  server: {
    proxy: {
      '/api': {
        target: process.env.VITE_API_TARGET || 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
