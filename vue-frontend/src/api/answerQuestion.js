import axios from 'axios'

// 创建答题 API 实例
const answerQuestionApi = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器 - 自动添加 token
answerQuestionApi.interceptors.request.use(
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
 * 随机获取一道题目
 */
export function getRandomQuestion() {
  return answerQuestionApi.get('/answer-question/random')
}

/**
 * 提交答案
 */
export function submitAnswer(userid, questionid, answer, questionType) {
  return answerQuestionApi.post('/answer-question/submit', {
    userid,
    questionid,
    answer,
    questionType
  })
}

/**
 * 查询用户的答题记录
 */
export function getAnswerRecords(userid, page = 0, size = 10) {
  return answerQuestionApi.get(`/answer-question/records/${userid}`, {
    params: { page, size }
  })
}

export default answerQuestionApi
