import axios from 'axios'

// 创建认证 API 实例
const authApi = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器 - 自动添加 token
authApi.interceptors.request.use(
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

// 响应拦截器 - 处理错误
authApi.interceptors.response.use(
  response => response,
  error => {
    if (error.response?.status === 401) {
      // token 无效或过期，清除本地存储并跳转到登录页
      localStorage.removeItem('token')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

/**
 * 用户登录
 */
export function login(username, password) {
  return authApi.post('/auth/login', { username, password })
}

/**
 * 用户注册
 */
export function register(username, password, email) {
  return authApi.post('/auth/register', { username, password, email })
}

/**
 * 获取当前用户信息
 */
export function getCurrentUser() {
  return authApi.get('/auth/me')
}

/**
 * 退出登录
 */
export function logout() {
  return authApi.post('/auth/logout')
}

/**
 * 修改用户邮箱
 */
export function updateEmail(email) {
  return authApi.put('/auth/update-email', { email })
}

/**
 * 修改用户密码
 */
export function updatePassword(oldPassword, newPassword) {
  return authApi.put('/auth/update-password', { oldPassword, newPassword })
}

/**
 * 管理员更新用户角色
 */
export function updateUserRole(username, role) {
  return authApi.put('/auth/update-user-role', { username, role })
}

export default authApi
