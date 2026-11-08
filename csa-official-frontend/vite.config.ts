import { fileURLToPath } from 'node:url'
import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const target = env.API_PROXY_TARGET || 'http://127.0.0.1:8080'
  return {
    plugins: [vue()],
    resolve: { alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) } },
    server: { strictPort: true, proxy: { '/api': { target }, '/files': { target } } },
    preview: { strictPort: true, proxy: { '/api': { target }, '/files': { target } } },
    build: { sourcemap: false },
  }
})
