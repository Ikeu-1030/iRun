<template>
  <div class="login-page" @mousemove="onMouseMove">
    <!-- 背景纹理层 -->
    <div class="bg-texture"></div>

    <!-- 全屏粒子画布 -->
    <canvas ref="particleCanvasRef" class="particle-canvas"></canvas>

    <!-- 渐变光晕装饰 -->
    <div class="glow-orb glow-orb--top"></div>
    <div class="glow-orb glow-orb--mid"></div>
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
        <div class="brand-deco" ref="brandDecoRef">
          <span class="deco-dot"></span>
          <span class="deco-line"></span>
          <span class="deco-dot"></span>
        </div>
      </div>

      <!-- 右侧登录卡 -->
      <div class="form-side" ref="formSideRef">
        <div class="login-card-wrapper" ref="cardWrapperRef">
          <!-- 动画边框 -->
          <div class="card-border-glow"></div>
          <div class="login-card">
            <!-- 顶部彩色 accent -->
            <div class="card-accent"></div>

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
                    <span v-if="!loading">登 录</span>
                  </el-button>
                </el-form-item>
              </div>
            </el-form>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部版权 -->
    <div class="page-footer" ref="footerRef">
      <span>Campus Errand Service Platform</span>
      <span class="footer-dot">·</span>
      <span>小i跑腿 v1.0</span>
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
const brandDecoRef = ref<HTMLElement>()
const formSideRef = ref<HTMLElement>()
const cardWrapperRef = ref<HTMLElement>()
const formHeaderRef = ref<HTMLElement>()
const formFieldsRef = ref<HTMLElement>()
const formActionsRef = ref<HTMLElement>()
const footerRef = ref<HTMLElement>()

// ---- 鼠标视差 ----
const mouseX = ref(0)
const mouseY = ref(0)

function onMouseMove(e: MouseEvent) {
  mouseX.value = (e.clientX / window.innerWidth - 0.5) * 2  // -1 ~ 1
  mouseY.value = (e.clientY / window.innerHeight - 0.5) * 2
}

onMounted(() => {
  window.addEventListener('mousemove', onMouseMove, { passive: true })
})

onUnmounted(() => {
  window.removeEventListener('mousemove', onMouseMove)
  if (_animId) cancelAnimationFrame(_animId)
  if (_resizeHandler) window.removeEventListener('resize', _resizeHandler)
})

// ---- 全屏粒子系统 ----
type Particle = {
  x: number; y: number; vx: number; vy: number
  r: number; alpha: number; alphaDir: number
  hue: number
}

let _animId = 0
let _resizeHandler: (() => void) | null = null

function initParticles(particles: Particle[], w: number, h: number) {
  particles.length = 0
  const count = Math.min(100, Math.floor((w * h) / 10000))
  for (let i = 0; i < count; i++) {
    particles.push({
      x: Math.random() * w,
      y: Math.random() * h,
      vx: (Math.random() - 0.5) * 0.3,
      vy: (Math.random() - 0.5) * 0.3,
      r: Math.random() * 3 + 0.6,
      alpha: Math.random() * 0.4 + 0.06,
      alphaDir: (Math.random() - 0.5) * 0.004,
      hue: Math.random() < 0.15 ? 15 + Math.random() * 20 : 220 + Math.random() * 30,
    })
  }
}

function drawParticles(particles: Particle[], ctx: CanvasRenderingContext2D, w: number, h: number) {
  ctx.clearRect(0, 0, w, h)

  // 鼠标对粒子施加微弱引力
  const mx = (mouseX.value * 0.5 + 0.5) * w
  const my = (mouseY.value * 0.5 + 0.5) * h

  for (const p of particles) {
    // 微弱引力
    p.vx += (mx - p.x) * 0.00003
    p.vy += (my - p.y) * 0.00003
    // 阻尼
    p.vx *= 0.9995
    p.vy *= 0.9995

    p.x += p.vx
    p.y += p.vy
    p.alpha += p.alphaDir
    if (p.alpha <= 0.03 || p.alpha >= 0.5) p.alphaDir *= -1
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
      if (dist < 130) {
        ctx.beginPath()
        ctx.moveTo(p.x, p.y)
        ctx.lineTo(q.x, q.y)
        const alpha = 0.05 * (1 - dist / 130)
        ctx.strokeStyle = `rgba(255,255,255,${alpha})`
        ctx.lineWidth = 0.4
        ctx.stroke()
      }
    }

    // 粒子带微弱颜色
    ctx.beginPath()
    ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2)
    const hsl = `hsla(${p.hue}, 60%, 75%, ${p.alpha})`
    ctx.fillStyle = hsl
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
    tl.from(brandSideRef.value, { opacity: 0, duration: 0.6 }, 0.1)
  }

  // Logo 弹性上浮
  if (logoRef.value) {
    tl.from(logoRef.value, {
      y: 36, opacity: 0, duration: 1, ease: 'back.out(1.7)',
    }, 0.25)
  }

  // Slogan 逐字
  if (sloganRef.value) {
    const chars = splitText(sloganRef.value)
    tl.from(chars, {
      y: 18, opacity: 0, duration: 0.5, stagger: 0.04, ease: 'power3.out',
    }, '-=0.3')
  }

  // 装饰点线
  if (brandDecoRef.value) {
    tl.from(brandDecoRef.value, {
      scaleX: 0, opacity: 0, duration: 0.7, ease: 'power3.inOut',
    }, '-=0.2')
  }

  // 毛玻璃卡片从右滑入 + 淡入
  if (formSideRef.value) {
    tl.from(formSideRef.value, {
      x: 70, opacity: 0, duration: 1.1, ease: 'power4.out',
    }, '-=0.45')
  }

  // 表单头部
  if (formHeaderRef.value) {
    tl.from(formHeaderRef.value, {
      y: 14, opacity: 0, duration: 0.5,
    }, '-=0.25')
  }

  // 表单字段 stagger
  if (formFieldsRef.value) {
    const children = Array.from(formFieldsRef.value.children) as HTMLElement[]
    tl.from(children, {
      y: 12, opacity: 0, duration: 0.5, stagger: 0.1, ease: 'power3.out',
    }, '-=0.08')
  }

  // 按钮
  if (formActionsRef.value) {
    tl.from(formActionsRef.value, {
      y: 8, opacity: 0, duration: 0.5,
    }, '-=0.05')
  }

  // Footer
  if (footerRef.value) {
    tl.from(footerRef.value, {
      opacity: 0, duration: 0.7,
    }, '-=0.15')
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
/* ===== CSS 变量 ===== */
.login-page {
  --brand-coral: #FF6B4A;
  --brand-gold: #FFB347;
  --brand-teal: #2EC4B6;
}

/* ===== 全屏容器 ===== */
.login-page {
  position: relative;
  width: 100vw;
  height: 100vh;
  background: linear-gradient(140deg, #0f0c1d 0%, #16132a 30%, #111b33 60%, #0a1628 100%);
  overflow: hidden;
}

/* ===== 背景纹理 — 微点阵 ===== */
.bg-texture {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  opacity: 0.25;
  background-image: radial-gradient(circle, rgba(255,255,255,0.15) 1px, transparent 1px);
  background-size: 40px 40px;
}

/* ===== 全屏粒子 ===== */
.particle-canvas {
  position: absolute;
  inset: 0;
  z-index: 1;
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
  width: 520px;
  height: 520px;
  background: rgba(255, 107, 74, 0.14);
  top: -200px;
  right: -120px;
  animation: orbBreathe 8s ease-in-out infinite, orbDrift 16s ease-in-out infinite;
}

.glow-orb--mid {
  width: 300px;
  height: 300px;
  background: rgba(46, 196, 182, 0.07);
  top: 40%;
  left: 30%;
  animation: orbBreathe 10s ease-in-out 3s infinite, orbDrift2 20s ease-in-out infinite;
}

.glow-orb--bottom {
  width: 420px;
  height: 420px;
  background: rgba(255, 179, 71, 0.08);
  bottom: -160px;
  left: -100px;
  animation: orbBreathe 9s ease-in-out 1.5s infinite, orbDrift 18s ease-in-out 2s infinite;
}

@keyframes orbBreathe {
  0%, 100% { transform: scale(1); opacity: 1; }
  50% { transform: scale(1.12); opacity: 0.75; }
}

@keyframes orbDrift {
  0%, 100% { translate: 0 0; }
  25% { translate: 30px -20px; }
  50% { translate: -10px 15px; }
  75% { translate: -25px -10px; }
}

@keyframes orbDrift2 {
  0%, 100% { translate: 0 0; }
  33% { translate: -20px -30px; }
  66% { translate: 25px 20px; }
}

/* ===== 内容层 ===== */
.login-content {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 72px;
  height: 100%;
  padding: 0 64px;
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
  width: 60px;
  height: 60px;
  flex-shrink: 0;
  filter: drop-shadow(0 8px 16px rgba(255, 107, 74, 0.3));
}

.brand-title {
  font-size: 32px;
  font-weight: 700;
  letter-spacing: 2px;
  margin: 0;
  background: linear-gradient(135deg, #FF6B4A 0%, #FFB347 60%, #FF8C6B 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  text-shadow: none;
}

.brand-slogan {
  margin-top: 24px;
  font-size: 17px;
  color: rgba(255, 255, 255, 0.5);
  letter-spacing: 5px;
  font-weight: 300;
}

/* 品牌装饰线 */
.brand-deco {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 28px;
  height: 2px;
}

.deco-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--brand-coral);
  flex-shrink: 0;
}

.deco-line {
  flex: 1;
  max-width: 120px;
  height: 1px;
  background: linear-gradient(90deg, rgba(255,107,74,0.6), rgba(255,107,74,0.05));
}

/* ===== 右侧毛玻璃卡片 ===== */
.form-side {
  flex: 0 0 auto;
  perspective: 800px;
}

.login-card-wrapper {
  position: relative;
  border-radius: 22px;
}

/* 卡片动画边框 — 伪元素遮罩镂空实现 */
.card-border-glow {
  position: absolute;
  inset: -2px;
  border-radius: 22px;
  z-index: 0;
  pointer-events: none;
  background: linear-gradient(
    135deg,
    rgba(255, 107, 74, 0.55),
    rgba(255, 179, 71, 0.35),
    rgba(46, 196, 182, 0.35),
    rgba(255, 107, 74, 0.55)
  );
  background-size: 300% 300%;
  animation: borderShimmer 6s ease-in-out infinite;
}

/* 用内层遮罩挖掉中心，只留边框 */
.card-border-glow::after {
  content: '';
  position: absolute;
  inset: 2px;
  border-radius: 20px;
  background: #16132a;
}

@keyframes borderShimmer {
  0%, 100% { background-position: 0% 50%; }
  25% { background-position: 100% 0%; }
  50% { background-position: 100% 100%; }
  75% { background-position: 0% 100%; }
}

.login-card {
  position: relative;
  z-index: 1;
  width: 400px;
  padding: 44px 40px 36px;
  background: rgba(255, 255, 255, 0.065);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 20px;
  box-shadow:
    0 8px 32px rgba(0, 0, 0, 0.25),
    0 1px 0 rgba(255, 255, 255, 0.08) inset;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

/* 卡片 hover 微抬 */
.login-card:hover {
  transform: translateY(-2px);
  box-shadow:
    0 12px 40px rgba(0, 0, 0, 0.35),
    0 1px 0 rgba(255, 255, 255, 0.1) inset,
    0 0 80px rgba(255, 107, 74, 0.06);
}

/* 卡片顶部彩色 accent */
.card-accent {
  position: absolute;
  top: 0;
  left: 50%;
  translate: -50% 0;
  width: 60px;
  height: 3px;
  border-radius: 0 0 3px 3px;
  background: linear-gradient(90deg, var(--brand-coral), var(--brand-gold));
}

.login-header {
  margin-bottom: 34px;
}

.login-header h2 {
  font-size: 26px;
  font-weight: 700;
  color: #fff;
  margin: 0 0 6px;
  letter-spacing: 1px;
}

.login-header p {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.4);
  margin: 0;
}

/* 输入框 — 暗色主题 */
.login-form :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 10px;
  box-shadow: none;
  transition: border-color 0.25s, background 0.25s, box-shadow 0.25s;
}

.login-form :deep(.el-input__wrapper:hover) {
  border-color: rgba(255, 107, 74, 0.45);
  background: rgba(255, 255, 255, 0.09);
}

.login-form :deep(.el-input__wrapper.is-focus) {
  border-color: var(--brand-coral);
  background: rgba(255, 255, 255, 0.1);
  box-shadow: 0 0 0 4px rgba(255, 107, 74, 0.1);
}

.login-form :deep(.el-input__inner) {
  color: #fff;
}

.login-form :deep(.el-input__inner::placeholder) {
  color: rgba(255, 255, 255, 0.28);
}

.login-form :deep(.el-input__prefix) {
  color: rgba(255, 255, 255, 0.32);
}

.login-form :deep(.el-input__prefix.is-focus) {
  color: var(--brand-coral);
}

.login-form :deep(.el-input__suffix) {
  color: rgba(255, 255, 255, 0.32);
}

/* 登录按钮 */
.login-btn {
  margin-top: 6px;
  width: 100%;
  height: 48px;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 700;
  letter-spacing: 6px;
  border: none;
  background: linear-gradient(135deg, #FF6B4A 0%, #FF8A5C 50%, #FFB347 100%);
  background-size: 200% 100%;
  transition: all 0.3s ease;
  box-shadow: 0 4px 20px rgba(255, 107, 74, 0.3);
}

.login-btn:hover {
  background-position: 100% 0;
  transform: translateY(-1px);
  box-shadow: 0 8px 30px rgba(255, 107, 74, 0.45);
}

.login-btn:active {
  transform: scale(0.98) translateY(0);
  box-shadow: 0 2px 10px rgba(255, 107, 74, 0.2);
}

.login-btn.is-loading {
  background: rgba(255, 255, 255, 0.1);
  box-shadow: none;
}

/* ===== 底部版权 ===== */
.page-footer {
  position: absolute;
  bottom: 30px;
  left: 0;
  right: 0;
  text-align: center;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.18);
  letter-spacing: 1px;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.footer-dot {
  color: rgba(255, 255, 255, 0.1);
  font-weight: 700;
}

/* ===== 响应式 ===== */
@media (max-width: 860px) {
  .login-content {
    flex-direction: column;
    gap: 36px;
    padding: 40px 24px;
  }

  .brand-side {
    text-align: center;
  }

  .brand-logo {
    flex-direction: column;
    gap: 12px;
  }

  .logo-icon {
    width: 52px;
    height: 52px;
  }

  .brand-title {
    font-size: 26px;
  }

  .brand-slogan {
    font-size: 15px;
    letter-spacing: 3px;
  }

  .brand-deco {
    justify-content: center;
  }

  .deco-line {
    max-width: 80px;
  }

  .login-card {
    width: 100%;
    max-width: 400px;
    padding: 34px 26px 28px;
  }
}
</style>
