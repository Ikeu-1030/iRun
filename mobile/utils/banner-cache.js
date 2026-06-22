/**
 * 轮播图模块级缓存 — 秒级渲染
 *
 * 设计：模块顶层变量在 JS 加载时初始化，常驻内存避免反复 JSON.parse。
 * App.vue onLaunch 预加载写入 storage，index.vue setup 直接同步读取。
 */
import { SERVER_ORIGIN } from '@/utils/config'

const STORAGE_KEY = 'rr_home_banner'

let _images = []
let _interval = 3
let _inited = false
let _pending = null

function _init() {
  try {
    const raw = uni.getStorageSync(STORAGE_KEY)
    if (raw) {
      const data = JSON.parse(raw)
      if (data.images?.length) {
        _images = data.images
        _interval = data.interval || 3
      }
    }
  } catch { /* ignore */ }
}

/**
 * 同步获取当前缓存（毫秒级，不发起网络请求）。
 * 页面 setup 中调用，直接赋值给 ref，实现首帧即渲染。
 */
export function getBannerSnapshot() {
  if (!_inited) { _init(); _inited = true }
  return { images: _images, interval: _interval }
}

/**
 * 后台静默刷新，自动去重防止并发请求。
 * App.vue onLaunch 和页面 onShow 都可以安全调用。
 */
export function refreshBanners() {
  if (_pending) return _pending
  _pending = new Promise((resolve) => {
    uni.request({
      url: SERVER_ORIGIN + '/api/common/banners',
      method: 'GET',
      success(res) {
        const body = res.data
        if (body?.code === 1 && body.data) {
          _images = body.data.images || []
          _interval = body.data.interval || 3
          try { uni.setStorageSync(STORAGE_KEY, JSON.stringify({ images: _images, interval: _interval })) } catch {}
        }
      },
      fail() {},
      complete() {
        _pending = null
        resolve()
      }
    })
  })
  return _pending
}
