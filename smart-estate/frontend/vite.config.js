import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Islands build: a single JS + CSS bundle with fixed names, emitted into Spring's
// static resources so Thymeleaf templates can reference them directly.
export default defineConfig({
  plugins: [react()],
  base: '/app/',
  server: { port: 5173, strictPort: true },
  build: {
    outDir: '../src/main/resources/static/app',
    emptyOutDir: true,
    manifest: false,
    cssCodeSplit: false,
    rollupOptions: {
      input: 'src/main.jsx',
      output: {
        entryFileNames: 'smart-estate.js',
        chunkFileNames: 'smart-estate-[name].js',
        assetFileNames: 'smart-estate.[ext]',
      },
    },
  },
})
