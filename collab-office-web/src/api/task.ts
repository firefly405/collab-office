import request from '@/utils/request'

export function createTask(title: string, projectId?: string) {
  return request.post('/task/create', null, { params: { title, projectId } })
}

export function getTaskList(projectId?: string, status?: number) {
  return request.get('/task/list', { params: { projectId, status } })
}

export function getTaskDetail(taskId: string) {
  return request.get(`/task/${taskId}`)
}

export function updateTask(taskId: string, data: any) {
  return request.put(`/task/${taskId}`, data)
}

export function updateTaskStatus(taskId: string, status: number) {
  return request.put(`/task/${taskId}/status`, null, { params: { status } })
}

export function deleteTask(taskId: string) {
  return request.delete(`/task/${taskId}`)
}

export function createProject(name: string, spaceId: number) {
  return request.post('/task/project/create', null, { params: { name, spaceId } })
}

export function getProjectList(spaceId: number) {
  return request.get('/task/project/list', { params: { spaceId } })
}
