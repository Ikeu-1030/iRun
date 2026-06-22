<template>
  <div class="login-page">
    <!-- 全屏粒子画布 -->
    <canvas ref="particleCanvasRef" class="particle-canvas"></canvas>

    <!-- 渐变光晕装饰 -->
    <div class="glow-orb glow-orb--top"></div>
    <div class="glow-orb glow-orb--bottom"></div>

    <!-- 内容层 -->
    <div class="login-content">
      <!-- 左侧品牌 -->
      <div class="brand-side" ref="brandSideRef">
        <div class="brand-logo" ref="logoRef">
          <img class="logo-icon" src="/logo.svg" alt="小i跑腿" />
          <h1 class="brand-title">小i跑腿 · 管理端</h1>
        </div>
        <p class="brand-slogan" ref="sloganRef">让校园生活更高效</p>
      </div>

      <!-- 右侧登录卡 -->
      <div class="form-side" ref="formSideRef">
        <div class="login-card">
          <div class="login-header" ref="formHeaderRef">
            <h2>欢迎回来</h2>
            <p>请登录您的管理账号</p>
          </div>
          <el-form
            :model="form"
            :rules="rules"
            ref="formRef"
            class="login-form"
            @keyup.enter="handleLogin"
          >
            <div ref="formFieldsRef">
              <el-form-item prop="username">
                <el-input
                  v-model="form.username"
                  placeholder="用户名"
                  :prefix-icon="User"
                  size="large"
                />
              </el-form-item>
              <el-form-item prop="password">
                <el-input
                  v-model="form.password"
                  type="password"
                  placeholder="密码"
                  :prefix-icon="Lock"
                  size="large"
                  show-password
                />
              </el-form-item>
            </div>
            <div ref="formActionsRef">
              <el-form-item>
                <el-button
                  type="primary"
                  size="large"
                  class="login-btn"
                  :loading="loading"
                  @click="handleLogin"
                >
                  登 录
                </el-button>
              </el-form-item>
            </div>
          </el-form>
        </div>
      </div>
    </div>

    <!-- 底部版权 -->
    <div class="page-footer" ref="footerRef">
      Campus Errand Service Platform
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import gsap from 'gsap'
import { useAuthStore } from '@/stores/auth'
import { login as loginApi } from '@/api/auth'

const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)
const formRef = ref()
const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

// ---- refs ----
const particleCanvasRef = ref<HTMLCanvasElement>()
const brandSideRef = ref<HTMLElement>()
const logoRef = ref<HTMLElement>()
const sloganRef = ref<HTMLElement>()
const formSideRef = ref<HTMLElement>()
const formHeaderRef = ref<HTMLElement>()
const formFieldsRef = ref<HTMLElement>()
const formActionsRef = ref<HTMLElement>()
const footerRef = ref<HTMLElement>()

// ---- 全屏粒子系统 ----
type Particle = { x: number; y: number; vx: number; vy: number; r: number; alpha: number; alphaDir: number }

let _animId = 0
let _resizeHandler: (() => void) | null = null

onUnmounted(() => {
  if (_animId) cancelAnimationFrame(_animId)
  if (_resizeHandler) window.removeEventListener('resize', _resizeHandler)
})

function initParticles(particles: Particle[], w: number, h: number) {
  particles.length = 0
  const count = Math.min(80, Math.floor((w * h) / 12000))
  for (let i = 0; i < count; i++) {
    particles.push({
      x: Math.random() * w,
      y: Math.random() * h,
      vx: (Math.random() - 0.5) * 0.25,
      vy: (Math.random() - 0.5) * 0.25,
      r: Math.random() * 2.5 + 0.8,
      alpha: Math.random() * 0.35 + 0.08,
      alphaDir: (Math.random() - 0.5) * 0.003,
    })
  }
}

function drawParticles(particles: Particle[], ctx: CanvasRenderingContext2D, w: number, h: number) {
  ctx.clearRect(0, 0, w, h)
  for (const p of particles) {
    p.x += p.vx
    p.y += p.vy
    p.alpha += p.alphaDir
    if (p.alpha <= 0.04 || p.alpha >= 0.4) p.alphaDir *= -1
    if (p.x < -10) p.x = w + 10
    if (p.x > w + 10) p.x = -10
    if (p.y < -10) p.y = h + 10
    if (p.y > h + 10) p.y = -10

    // 连线 — 近距离粒子之间画半透明线
    for (let j = 0; j < particles.length; j++) {
      const q = particles[j]
      const dx = p.x - q.x
      const dy = p.y - q.y
      const dist = Math.sqrt(dx * dx + dy * dy)
      if (dist < 120) {
        ctx.beginPath()
        ctx.moveTo(p.x, p.y)
        ctx.lineTo(q.x, q.y)
        ctx.strokeStyle = `rgba(255,255,255,${0.04 * (1 - dist / 120)})`
        ctx.lineWidth = 0.5
        ctx.stroke()
      }
    }

    ctx.beginPath()
    ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2)
    ctx.fillStyle = `rgba(255,255,255,${p.alpha})`
    ctx.fill()
  }
}

function startCanvas() {
  const canvas = particleCanvasRef.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  if (!ctx) return

  const particles: Particle[] = []

  const resize = () => {
    const w = window.innerWidth
    const h = window.innerHeight
    canvas.width = w * devicePixelRatio
    canvas.height = h * devicePixelRatio
    canvas.style.width = w + 'px'
    canvas.style.height = h + 'px'
    ctx.setTransform(devicePixelRatio, 0, 0, devicePixelRatio, 0, 0)
    initParticles(particles, w, h)
  }
  resize()
  window.addEventListener('resize', resize)
  _resizeHandler = resize

  const loop = () => {
    const w = canvas.width / devicePixelRatio
    const h = canvas.height / devicePixelRatio
    drawParticles(particles, ctx, w, h)
    _animId = requestAnimationFrame(loop)
  }
  loop()
}

// ---- 文字拆分 ----
function splitText(el: HTMLElement) {
  const text = el.textContent || ''
  el.textContent = ''
  return [...text].map((ch) => {
    const span = document.createElement('span')
    span.textContent = ch
    span.style.display = 'inline-block'
    el.appendChild(span)
    return span
  })
}

// ---- GSAP Timeline ----
async function runEntranceAnimation() {
  await nextTick()

  const tl = gsap.timeline({ defaults: { ease: 'power3.out' } })

  // 品牌区淡入
  if (brandSideRef.value) {
    tl.from(brandSideRef.value, { opacity: 0, duration: 0.6 }, 0.15)
  }

  // Logo 弹性上浮
  if (logoRef.value) {
    tl.from(logoRef.value, {
      y: 30, opacity: 0, duration: 0.9, ease: 'back.out(1.6)',
    }, 0.3)
  }

  // Slogan 逐字
  if (sloganRef.value) {
    const chars = splitText(sloganRef.value)
    tl.from(chars, {
      y: 16, opacity: 0, duration: 0.5, stagger: 0.045, ease: 'power3.out',
    }, '-=0.35')
  }

  // 毛玻璃卡片从右滑入 + 淡入
  if (formSideRef.value) {
    tl.from(formSideRef.value, {
      x: 60, opacity: 0, duration: 1, ease: 'power4.out',
    }, '-=0.5')
  }

  // 表单头部
  if (formHeaderRef.value) {
    tl.from(formHeaderRef.value, {
      y: 12, opacity: 0, duration: 0.5,
    }, '-=0.3')
  }

  // 表单字段 stagger
  if (formFieldsRef.value) {
    const children = Array.from(formFieldsRef.value.children) as HTMLElement[]
    tl.from(children, {
      y: 10, opacity: 0, duration: 0.45, stagger: 0.1, ease: 'power3.out',
    }, '-=0.1')
  }

  // 按钮
  if (formActionsRef.value) {
    tl.from(formActionsRef.value, {
      y: 6, opacity: 0, duration: 0.45,
    }, '-=0.05')
  }

  // Footer
  if (footerRef.value) {
    tl.from(footerRef.value, {
      opacity: 0, duration: 0.6,
    }, '-=0.2')
  }
}

// ---- 登录 ----
async function handleLogin() {
  if (!form.username || !form.password) {
    ElMessage.error('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    const data = await loginApi(form)
    authStore.setAuth(data.token, data.refreshToken, {
      adminId: data.adminId,
      username: data.username,
      name: data.name,
      role: data.role,
    })
    ElMessage.success('登录成功')
    router.replace('/dashboard')
  } catch {
    ElMessage.error('登录失败，请检查用户名和密码')
  } finally {
    loading.value = false
  }
}

onMounted(startCanvas)
onMounted(runEntranceAnimation)
</script>

<style scoped>
/* ===== 全屏容器 ===== */
.login-page {
  position: relative;
  width: 100vw;
  height: 100vh;
  background: linear-gradient(140deg, #0f0c1d 0%, #16132a 30%, #111b33 60%, #0a1628 100%);
  overflow: hidden;
}

/* ===== 全屏粒子 ===== */
.particle-canvas {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
}

/* ===== 光晕装饰 ===== */
.glow-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(120px);
  pointer-events: none;
  z-index: 0;
}

.glow-orb--top {
  width: 500px;
  height: 500px;
  background: rgba(255, 107, 74, 0.12);
  top: -180px;
  right: -100px;
}

.glow-orb--bottom {
  width: 400px;
  height: 400px;
  background: rgba(46, 196, 182, 0.08);
  bottom: -140px;
  left: -80px;
}

/* ===== 内容层 ===== */
.login-content {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 60px;
  height: 100%;
  padding: 0 60px;
}

/* ===== 左侧品牌 ===== */
.brand-side {
  text-align: left;
  color: #fff;
  flex: 0 0 auto;
}

.brand-logo {
  display: flex;
  align-items: center;
  gap: 18px;
}

.logo-icon {
  width: 56px;
  height: 56px;
  flex-shrink: 0;
}

.brand-title {
  font-size: 30px;
  font-weight: 700;
  letter-spacing: 2px;
  margin: 0;
  background: linear-gradient(135deg, #FF6B4A, #FFB347);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.brand-slogan {
  margin-top: 20px;
  font-size: 17px;
  color: rgba(255, 255, 255, 0.55);
  letter-spacing: 4px;
}

/* ===== 右侧毛玻璃卡片 ===== */
.form-side {
  flex: 0 0 auto;
}

.login-card {
  width: 400px;
  padding: 44px 40px 36px;
  background: rgba(255, 255, 255, 0.06);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 20px;
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.3);
}

.login-header {
  margin-bottom: 32px;
}

.login-header h2 {
  font-size: 24px;
  font-weight: 700;
  color: #fff;
  margin: 0 0 6px;
}

.login-header p {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.45);
  margin: 0;
}

/* 输入框 — 暗色主题 */
.login-form :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 10px;
  box-shadow: none;
  transition: border-color 0.2s, background 0.2s;
}

.login-form :deep(.el-input__wrapper:hover) {
  border-color: rgba(255, 107, 74, 0.5);
  background: rgba(255, 255, 255, 0.1);
}

.login-form :deep(.el-input__wrapper.is-focus) {
  border-color: #FF6B4A;
  background: rgba(255, 255, 255, 0.1);
  box-shadow: 0 0 0 3px rgba(255, 107, 74, 0.15);
}

.login-form :deep(.el-input__inner) {
  color: #fff;
}

.login-form :deep(.el-input__inner::placeholder) {
  color: rgba(255, 255, 255, 0.3);
}

.login-form :deep(.el-input__prefix) {
  color: rgba(255, 255, 255, 0.35);
}

.login-btn {
  margin-top: 4px;
  width: 100%;
  border-radius: 10px;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 4px;
  height: 46px;
  transition: transform 0.2s;
}

.login-btn:hover {
  transform: scale(1.02);
}

.login-btn:active {
  transform: scale(0.98);
}

/* ===== 底部版权 ===== */
.page-footer {
  position: absolute;
  bottom: 28px;
  left: 0;
  right: 0;
  text-align: center;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.2);
  letter-spacing: 1px;
  z-index: 1;
}

/* ===== 响应式 ===== */
@media (max-width: 860px) {
  .login-content {
    flex-direction: column;
    gap: 32px;
    padding: 40px 24px;
  }

  .brand-side {
    text-align: center;
  }

  .brand-logo {
    flex-direction: column;
    gap: 12px;
  }

  .brand-title {
    font-size: 24px;
  }

  .brand-slogan {
    font-size: 15px;
    letter-spacing: 2px;
  }

  .login-card {
    width: 100%;
    max-width: 400px;
    padding: 32px 24px 28px;
  }
}
</style>
