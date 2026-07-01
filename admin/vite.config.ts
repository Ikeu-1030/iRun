import { defineConfig, type Plugin } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

function securityPlugin(): Plugin {
  return {
    name: 'security-headers',
    transformIndexHtml(html, ctx) {
      // 生产构建时注入 CSP meta，开发环境不注入（避免阻断 Vite HMR）
      if (ctx.server) return html
      const csp = [
        "default-src 'self'",
        "script-src 'self'",
        "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com",
        "font-src 'self' https://fonts.gstatic.com",
        "img-src 'self' data: blob: https://*.aliyuncs.com",
        "connect-src 'self' ws: wss:",
        "frame-ancestors 'none'",
        "base-uri 'self'",
        "form-action 'self'",
      ].join('; ')
      return html.replace('<!-- __CSP_META__ -->',
        `<meta http-equiv="Content-Security-Policy" content="${csp}">`)
    },
  }
}

export default defineConfig({
  base: '/api/',
  plugins: [vue(), securityPlugin()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  server: {
    port: 3001,
    headers: {
      'X-Content-Type-Options': 'nosniff',
      'X-Frame-Options': 'DENY',
      'Referrer-Policy': 'strict-origin-when-cross-origin',
      'Permissions-Policy': 'camera=(), microphone=(), geolocation=()',
    },
    proxy: {
      '^/api/(admin|user/|common|ws|imgs)': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        configure: (proxy) => {
          proxy.on('proxyReq', (proxyReq, req) => {
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
