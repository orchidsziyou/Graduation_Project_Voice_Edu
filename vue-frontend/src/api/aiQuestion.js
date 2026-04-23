import axios from 'axios'

// 创建 AI 出题 API 实例
const aiQuestionApi = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器 - 自动添加 token
aiQuestionApi.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

/**
 * 保存 AI 出题记录
 */
export function saveAiQuestionRecord(data) {
  return aiQuestionApi.post('/ai-question/save', data)
}

/**
 * 获取用户的出题记录列表
 */
export function getMyRecords() {
  return aiQuestionApi.get('/ai-question/records')
}

/**
 * 删除单条记录
 */
export function deleteRecord(id) {
  return aiQuestionApi.delete(`/ai-question/${id}`)
}

export default aiQuestionApi
