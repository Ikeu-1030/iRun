<script>
import { classifyError, isErrorType, ErrorType } from '@/utils/error'
import { showToast } from '@/utils/toast'

export default {
  onLaunch() {
    console.log('App Launch')
    this.setupGlobalErrorHandler()
  },
  onShow() {
    console.log('App Show')
  },
  onHide() {
    console.log('App Hide')
  },
  onError(err) {
    console.error('[App] Uncaught error:', err)
  },
  methods: {
    setupGlobalErrorHandler() {
      // 全局未捕获 Promise rejection 兜底
      if (typeof uni.onUnhandledRejection === 'function') {
        uni.onUnhandledRejection((res) => {
          const reason = res?.reason || res
          console.warn('[App] Unhandled rejection:', reason)
          const classified = classifyError(reason)
          if (classified.type === ErrorType.NETWORK || classified.type === ErrorType.SERVER_ERROR) {
            showToast(classified.message)
          }
        })
      }
    }
  }
}
</script>

<style lang="scss">
@import '@/uni_modules/uni-scss/index.scss';
@import '@/uni.scss';

page {
  /* ====== 暖珊瑚橙 + 青绿配色 ====== */
  --primary: #FF6B4A;
  --primary-focus: #FF8B72;
  --primary-on-dark: #FFB09C;
  --primary-dark: #E8553A;
  --primary-container: #FFF0ED;
  --on-primary: #FFFFFF;
  --on-primary-container: #7A2A15;

  /* ====== 辅助色：青绿 ====== */
  --secondary: #2EC4B6;
  --secondary-dark: #1FA89B;
  --secondary-container: #D4F5F0;
  --on-secondary: #FFFFFF;
  --on-secondary-container: #0B423C;

  /* ====== 强调色：金橙 ====== */
  --accent: #FFB347;
  --accent-container: #FFF7ED;

  /* ====== 语义色 ====== */
  --success: #34D399;
  --success-light: #D1FAE5;
  --success-container: #ECFDF5;

  --warning: #F59E0B;
  --warning-light: #FEF3C7;
  --warning-container: #FFFBEB;

  --error: #EF4444;
  --error-dark: #DC2626;
  --error-light: #FEE2E2;
  --error-container: #FEF2F2;

  --info: #3B82F6;
  --info-light: #DBEAFE;
  --info-container: #EFF6FF;

  /* ====== 暖白表面层级 ====== */
  --background: #FAFAF8;
  --surface: #F5F5F0;
  --surface-hover: #EEEDE8;
  --surface-raised: #FFFFFF;
  --surface-overlay: #FFFFFF;

  /* ====== 暖色文字 ====== */
  --text-primary: #1C1B1A;
  --text-secondary: #5E5D58;
  --text-tertiary: #8F8D88;
  --text-inverse: #FFFFFF;
  --text-link: var(--primary);

  /* ====== 暖色边框 ====== */
  --outline: #D4D2CC;
  --outline-light: #E8E6E0;
  --outline-strong: #A6A39E;

  /* ====== 兼容旧 token ====== */
  --on-background: var(--text-primary);
  --on-surface: var(--text-primary);
  --on-surface-variant: var(--text-secondary);
  --on-secondary-container: #0B423C;
  --surface-container-lowest: var(--surface-raised);
  --surface-container: var(--surface);
  --outline-variant: var(--outline-light);

  /* ====== Apple 字型尺寸 (17px body) ====== */
  --text-xs: 20rpx;
  --text-sm: 24rpx;
  --text-base: 34rpx;
  --text-lg: 36rpx;
  --text-xl: 42rpx;
  --text-2xl: 56rpx;
  --text-3xl: 68rpx;

  /* ====== 间距 ====== */
  --space-1: 8rpx;
  --space-2: 16rpx;
  --space-3: 24rpx;
  --space-4: 32rpx;
  --space-5: 40rpx;
  --space-6: 48rpx;
  --space-7: 64rpx;
  --space-8: 80rpx;

  /* ====== Apple 圆角体系 ====== */
  --radius-sm: 10rpx;
  --radius-md: 22rpx;
  --radius-lg: 36rpx;
  --radius-xl: 44rpx;
  --radius-card: 36rpx;
  --radius-full: 9999rpx;

  /* ====== Apple 阴影 — 仅产品图使用，卡片无阴影 ====== */
  --shadow-sm: none;
  --shadow-md: none;
  --shadow-lg: none;
  --shadow-xl: none;
  --shadow-primary: none;

  /* ====== 动效 ====== */
  --duration-fast: 0.12s;
  --duration-normal: 0.20s;
  --duration-slow: 0.30s;
  --duration-stagger: 0.06s;
  --duration-entrance: 0.35s;
  --easing-out: cubic-bezier(0.16, 1, 0.3, 1);
  --easing-in: cubic-bezier(0.4, 0, 1, 1);
  --easing-in-out: cubic-bezier(0.4, 0, 0.2, 1);
  --ease-spring: cubic-bezier(0.34, 1.56, 0.64, 1);
  --ease-bounce: cubic-bezier(0.25, 0.46, 0.45, 0.94);

  /* ====== Apple 基础样式：SF Pro 字体栈、17px body、负letter-spacing ====== */
  width: 100%;
  height: 100%;
  overflow-x: hidden;
  font-family: 'SF Pro Text', 'SF Pro Display', system-ui, -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Helvetica Neue', sans-serif;
  font-size: var(--text-base);
  font-weight: 400;
  line-height: 1.47;
  letter-spacing: -0.022em;
  color: var(--text-primary);
  background-color: var(--background);
  box-sizing: border-box;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

/* ====== 全局工具类 ====== */

.safe-area-bottom {
  padding-bottom: env(safe-area-inset-bottom);
}

/* Apple 风格卡片 — 无阴影，hairline 边框，表面色差替代层次 */
.card {
  background: var(--surface-raised);
  border-radius: var(--radius-lg);
  border: 1px solid var(--outline);
  padding: var(--space-4);
  transition: transform var(--duration-fast) var(--easing-out);
}
.card:active {
  transform: scale(0.98);
  background: var(--surface-hover);
}

/* Apple 风格状态徽章 */
.badge {
  display: inline-flex;
  align-items: center;
  padding: 4rpx 16rpx;
  border-radius: var(--radius-full);
  font-size: var(--text-xs);
  font-weight: 600;
  letter-spacing: -0.01em;
}
.badge--pending { background: var(--warning-light); color: #B06500; }
.badge--active  { background: var(--primary-container); color: var(--primary); }
.badge--success { background: var(--success-light); color: #1B7D32; }
.badge--error   { background: var(--error-light); color: var(--error); }
.badge--info    { background: var(--info-light); color: var(--primary); }
.badge--muted   { background: var(--surface); color: var(--text-tertiary); }

/* 入场动画 */
@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(16rpx); }
  to   { opacity: 1; transform: translateY(0); }
}
@keyframes fadeIn {
  from { opacity: 0; }
  to   { opacity: 1; }
}
@keyframes scaleIn {
  from { opacity: 0; transform: scale(0.95); }
  to   { opacity: 1; transform: scale(1); }
}
@keyframes fadeInScale {
  from { opacity: 0; transform: scale(0.92); }
  to   { opacity: 1; transform: scale(1); }
}
@keyframes slideInRight {
  from { opacity: 0; transform: translateX(20rpx); }
  to   { opacity: 1; transform: translateX(0); }
}
@keyframes bounceIn {
  0%   { opacity: 0; transform: scale(0.3); }
  50%  { opacity: 1; transform: scale(1.08); }
  70%  { transform: scale(0.95); }
  100% { opacity: 1; transform: scale(1); }
}
@keyframes pulseGlow {
  0%, 100% { box-shadow: 0 0 0 0 rgba(255, 107, 74, 0.35); }
  50%      { box-shadow: 0 0 0 8rpx rgba(255, 107, 74, 0.08); }
}
@keyframes statusBreathe {
  0%, 100% { opacity: 1; }
  50%      { opacity: 0.85; }
}

.animate-fade-in    { animation: fadeIn var(--duration-normal) var(--easing-out) backwards; }
.animate-fade-up    { animation: fadeInUp var(--duration-entrance) var(--easing-out) backwards; }
.animate-scale-in   { animation: scaleIn var(--duration-normal) var(--easing-out) backwards; }
.animate-scale-pop  { animation: fadeInScale var(--duration-entrance) var(--ease-spring) backwards; }
.animate-slide-right{ animation: slideInRight var(--duration-entrance) var(--easing-out) backwards; }
.animate-bounce-in  { animation: bounceIn 0.5s var(--ease-spring) backwards; }
.animate-pulse-glow { animation: pulseGlow 2s infinite; }

.delay-1  { animation-delay: 0.05s; }
.delay-2  { animation-delay: 0.10s; }
.delay-3  { animation-delay: 0.15s; }
.delay-4  { animation-delay: 0.20s; }
.delay-5  { animation-delay: 0.25s; }
.delay-6  { animation-delay: 0.30s; }
.delay-7  { animation-delay: 0.35s; }
.delay-8  { animation-delay: 0.40s; }

/* 旧工具类兼容 */
.mt-16 { margin-top: 16rpx; }
.mt-24 { margin-top: 24rpx; }
.mt-32 { margin-top: 32rpx; }
.mb-16 { margin-bottom: 16rpx; }
.mb-24 { margin-bottom: 24rpx; }
.p-24  { padding: 24rpx; }
.p-32  { padding: 32rpx; }
.px-32 { padding-left: 32rpx; padding-right: 32rpx; }
</style>
