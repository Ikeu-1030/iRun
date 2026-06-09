import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  // 开发: / , 测试环境与后端 context-path /api 对齐
  base: '/api/',
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  server: {
    port: 3001,
    proxy: {
      '/api': {
        // 开发环境
        target: 'http://localhost:8080',
        // 测试环境 (Ubuntu 内网穿透)
        // target: 'https://sedative-squishy-worry.ngrok-free.dev',
        changeOrigin: true,
        configure: (proxy) => {
          proxy.on('proxyReq', (proxyReq, req) => {
            proxyReq.setHeader('ngrok-skip-browser-warning', 'true')
            proxyReq.setHeader('User-Agent', 'runningerrands-admin/1.0')
            const ip = req.socket.remoteAddress
            if (ip) {
              proxyReq.setHeader('X-Forwarded-For', ip)
              proxyReq.setHeader('X-Real-IP', ip)
            }
          })
        }
      }
    }
  }
})
