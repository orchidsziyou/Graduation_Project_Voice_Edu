import axios from 'axios'

const request = axios.create({
  baseURL: '/api',
  timeout: 30000
})

// 请求拦截器
request.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  response => response,
  error => {
    console.error('API 请求错误:', error)
    return Promise.reject(error)
  }
)

/**
 * 提取关键词
 * @param {string} text - 要提取关键词的文本
 * @param {number} topN - 返回前 N 个关键词，默认 10
 */
export function extractKeywords(text, topN = 10) {
  return request({
    url: '/keyword/extract',
    method: 'post',
    data: {
      text,
      topN
    }
  })
}

/**
 * 提取关键短语
 * @param {string} text - 要提取关键短语的文本
 * @param {number} topN - 返回前 N 个关键短语，默认 5
 */
export function extractKeyPhrases(text, topN = 5) {
  return request({
    url: '/keyword/extract-phrases',
    method: 'post',
    data: {
      text,
      topN
    }
  })
}

/**
 * 文本分词
 * @param {string} text - 要分词的文本
 */
export function segmentText(text) {
  return request({
    url: '/keyword/segment',
    method: 'post',
    data: {
      text
    }
  })
}
