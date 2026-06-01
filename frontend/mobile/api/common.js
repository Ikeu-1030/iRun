/**
 * 通用模块 API
 */
import { SERVER_ORIGIN } from '@/utils/config'
import { getToken } from '@/utils/request'
import { showToast } from '@/utils/toast'

const UPLOAD_URL = SERVER_ORIGIN + '/api/common/upload'

/**
 * 文件上传
 * @param {string} filePath - 本地文件路径
 * @returns {Promise<string>} 返回 OSS 文件 URL
 */
export function uploadFile(filePath) {
  return new Promise((resolve, reject) => {
    uni.showLoading({ title: '上传中…', mask: true })

    uni.uploadFile({
      url: UPLOAD_URL,
      filePath,
      name: 'file',
      header: { authentication: getToken() },
      success(res) {
        uni.hideLoading()
        try {
          // uni.uploadFile 在不同平台 res.data 可能是字符串或已解析对象
          const body = typeof res.data === 'string' ? JSON.parse(res.data) : res.data
          if (body.code === 1) {
            resolve(body.data)
          } else {
            showToast(body.msg || '上传失败')
            reject(new Error(body.msg))
          }
        } catch (e) {
          console.error('uploadFile 解析响应失败:', e, 'res.data:', res.data)
          uni.showToast({ title: '上传异常，请重试', icon: 'none' })
          reject(e)
        }
      },
      fail(err) {
        uni.hideLoading()
        console.error('uploadFile 网络失败:', err)
        uni.showToast({ title: '上传失败，请重试', icon: 'none' })
        reject(err)
      }
    })
  })
}

/**
 * 多文件上传 (顺序上传)
 * @param {string[]} filePaths
 * @returns {Promise<string[]>}
 */
export async function uploadFiles(filePaths) {
  const urls = []
  for (const fp of filePaths) {
    const url = await uploadFile(fp)
    urls.push(url)
  }
  return urls
}
