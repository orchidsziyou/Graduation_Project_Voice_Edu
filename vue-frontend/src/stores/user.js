import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, logout as logoutApi, getCurrentUser } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
  // 状态
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(null)
  
  // 计算属性
  const isLoggedIn = computed(() => !!token.value)
  const username = computed(() => userInfo.value?.username || '')
  const userId = computed(() => userInfo.value?.id || null) // 用户 ID
  const userRole = computed(() => userInfo.value?.role || 0) // 0:普通用户，5:管理员
  const isAdmin = computed(() => userRole.value === 5)
  
  // 方法
  async function login(username, password) {
    try {
      console.log('调用登录 API...', { username })
      const response = await loginApi(username, password)
      console.log('API 响应:', response)
      
      if (response.data.success) {
        // 保存 token 到本地存储
        token.value = response.data.token
        localStorage.setItem('token', response.data.token)
        console.log('Token 已保存:', response.data.token)
        
        // 保存用户信息
        userInfo.value = response.data.user
        console.log('用户信息已保存:', response.data.user)
        
        return { success: true }
      } else {
        console.error('登录失败 - 后端返回错误:', response.data.message)
        return { 
          success: false, 
          message: response.data.message || '登录失败' 
        }
      }
    } catch (error) {
      console.error('登录异常:', error)
      console.error('错误详情:', error.response?.data)
      return { 
        success: false, 
        message: error.response?.data?.message || '登录失败，请检查网络连接' 
      }
    }
  }
  
  async function fetchUserInfo() {
    try {
      const response = await getCurrentUser()
      
      if (response.data.success) {
        userInfo.value = response.data.user
        return { success: true }
      } else {
        return { success: false }
      }
    } catch (error) {
      console.error('获取用户信息失败:', error)
      return { success: false }
    }
  }
  
  async function logout() {
    try {
      await logoutApi()
    } catch (error) {
      console.error('退出登录失败:', error)
    } finally {
      // 清除本地状态
      token.value = ''
      userInfo.value = null
      localStorage.removeItem('token')
    }
  }
  
  return {
    token,
    userInfo,
    isLoggedIn,
    username,
    userId,
    userRole,
    isAdmin,
    login,
    logout,
    fetchUserInfo
  }
})
