import request from '@/utils/request'

export function listTasks(params: { status?: number; page?: number; size?: number }) {
  return request({ url: '/admin/tasks', method: 'get', params })
}

export function getTaskDetail(taskId: number) {
  return request({ url: `/admin/tasks/${taskId}`, method: 'get' })
}

export function updateTaskStatus(taskId: number, status: number) {
  return request({ url: `/admin/tasks/${taskId}/status`, method: 'put', params: { status } })
}
