import axios from 'axios'

// 创建转写 API 实例
const transcriptionApi = axios.create({
  baseURL: '/api',
  timeout: 60000, // 转写可能需要较长时间
  headers: {
    'Content-Type': 'multipart/form-data'
  }
})

// 请求拦截器 - 自动添加 token
transcriptionApi.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
      // console.log('添加 Token:', token.substring(0, 20) + '...')
    } else {
      // console.log('未找到 Token，使用匿名方式上传')
    }
    // console.log('请求配置:', config)
    return config
  },
  error => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
transcriptionApi.interceptors.response.use(
  response => {
    // console.log('API 响应成功:', response)
    return response
  },
  error => {
    console.error('API 响应失败:', error)
    console.error('错误详情:', error.response?.data)
    return Promise.reject(error)
  }
)

/**
 * 上传音频文件进行转写
 */
export function uploadAudio(file, duration = null) {
  const formData = new FormData()
  formData.append('file', file)
  
  // 如果有时长信息，也传递给后端
  if (duration) {
    formData.append('duration', duration)
  }
  
  return transcriptionApi.post('/audio/transcribe', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 获取用户的转写记录列表
 */
export function getTranscriptionRecords() {
  return transcriptionApi.get('/transcription/records')
}

/**
 * 获取单个转写记录详情
 */
export function getTranscriptionRecordDetail(id) {
  return transcriptionApi.get(`/transcription/records/${id}`)
}

/**
 * 获取转写结果
 */
export function getTranscriptionResult(orderId) {
  return transcriptionApi.get(`/audio/result/${orderId}`)
}

/**
 * 根据订单号获取转写结果
 */
export function getTranscriptionResultByOrderId(orderId) {
  return transcriptionApi.post('/transcription/get-result-by-orderid', {
    orderId: orderId
  }, {
    headers: {
      'Content-Type': 'application/json'
    }
  })
}

/**
 * 本地 Vosk 语音识别
 * @param {File} file - 音频文件 (WAV)
 */
export function transcribeLocal(file) {
  const formData = new FormData()
  formData.append('file', file)
  
  return transcriptionApi.post('/speech/transcribe-local', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 保存本地转写记录（用于 VoskTest 页面手动保存）
 * @param {Object} data - 转写数据
 * @param {string} data.transcriptionText - 转写文本内容
 * @param {string} data.fileName - 文件名
 * @param {number} data.fileSize - 文件大小
 */
export function saveLocalTranscriptionRecord(data) {
  return transcriptionApi.post('/transcription/save-local', data, {
    headers: {
      'Content-Type': 'application/json'
    }
  })
}

export default transcriptionApi
