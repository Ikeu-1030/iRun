/**
 * WeChat Mini Program safe toast wrapper.
 * wx.showToast truncates title to ~7 Chinese characters when an icon is present.
 * This utility auto-detects long messages and falls back to icon:'none' which
 * allows more text, or shows a modal for very long messages.
 */
export function showToast(msg, opts = {}) {
  if (!msg) return
  msg = String(msg)
  const icon = opts.icon || 'none'
  if (msg.length <= 7) {
    uni.showToast({ title: msg, icon, duration: opts.duration || 2000 })
  } else if (msg.length <= 14) {
    uni.showToast({ title: msg, icon: 'none', duration: opts.duration || 2500 })
  } else {
    uni.showModal({
      title: '提示',
      content: msg,
      showCancel: false,
      confirmText: '知道了'
    })
  }
}
