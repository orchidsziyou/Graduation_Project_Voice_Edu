import axios from 'axios'

// 创建题目 API 实例
const questionItemsApi = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器 - 自动添加 token
questionItemsApi.interceptors.request.use(
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
 * 保存题目到数据库
 */
export function saveQuestionItem(questionBody, questionType, questionAnswer, choosingAnswer = null, userid = null) {
  const data = {
    questionBody,
    questionType,      // 0=选择题，1=主观题
    questionAnswer
  }
  
  if (choosingAnswer != null) {
    data.choosingAnswer = choosingAnswer
  }
  
  if (userid != null) {
    data.userid = userid
  }
  
  return questionItemsApi.post('/question-items/save', data)
}

/**
 * 分页查询题目列表
 */
export function getQuestions(params) {
  return questionItemsApi.get('/question-items/list', { params })
}

export default questionItemsApi
