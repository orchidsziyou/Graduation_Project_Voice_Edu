import axios from 'axios'

// 创建 AI API 实例
const aiApi = axios.create({
  baseURL: '/api',
  timeout: 60000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器 - 自动添加 token
aiApi.interceptors.request.use(
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
 * 发送聊天消息
 */
export function sendChatMessage(message) {
  return aiApi.post('/chat/send', { message })
}

/**
 * 获取对话历史
 */
export function getChatHistory() {
  return aiApi.get('/chat/history')
}

/**
 * 清除对话历史
 */
export function clearChatHistory() {
  return aiApi.post('/chat/clear')
}

/**
 * 获取语音识别配置
 */
export function getSpeechRecognitionConfig() {
  return aiApi.get('/config/speech-recognition')
}

export default aiApi
