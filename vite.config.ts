import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';
import { VitePWA } from 'vite-plugin-pwa';

export default defineConfig({
  plugins: [
    react(),
    tailwindcss(),
    VitePWA({
      registerType: 'autoUpdate',
      includeAssets: ['icon.svg'],
      manifest: {
        id: '/',
        name: 'ICMAP Smart Tutor Portal',
        short_name: 'ICMAP Tutor',
        description:
          'Adaptive study portal PWA for laptop and mobile: OM textbook, past papers, Pomodoro timer, roadmap, flashcards, and free AI tutor.',
        start_url: '/',
        scope: '/',
        display: 'standalone',
        orientation: 'any',
        theme_color: '#07111f',
        background_color: '#050b14',
        categories: ['education', 'productivity'],
        icons: [
          {
            src: '/icon.svg',
            sizes: '192x192 512x512 any',
            type: 'image/svg+xml',
            purpose: 'any maskable'
          }
        ]
      },
      workbox: {
        globPatterns: ['**/*.{js,css,html,svg,png,ico,woff2}'],
        maximumFileSizeToCacheInBytes: 15 * 1024 * 1024,
        runtimeCaching: [
          {
            urlPattern: /\/assets\/aistudio\/.*\.pdf$/i,
            handler: 'CacheFirst',
            options: {
              cacheName: 'study-pdfs-cache',
              expiration: {
                maxEntries: 10,
                maxAgeSeconds: 60 * 60 * 24 * 30
              }
            }
          }
        ]
      }
    })
  ]
});