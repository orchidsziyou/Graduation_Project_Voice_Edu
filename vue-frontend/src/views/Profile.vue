<template>
  <div class="profile-container">
    <el-card class="profile-card">
      <template #header>
        <div class="card-header">
          <span>个人中心</span>
          <el-button type="primary" @click="handleBack">
            返回主页
          </el-button>
        </div>
      </template>
      
      <div v-loading="loading" element-loading-text="加载中...">
        <!-- 用户基本信息 -->
        <div class="user-info-section">
          <div class="avatar-section">
            <el-avatar :size="100" :src="avatarUrl">
              <el-icon :size="50"><user /></el-icon>
            </el-avatar>
            <h2 class="username">{{ userInfo.username }}</h2>
            <el-tag v-if="userInfo.role === 5" type="danger" effect="dark" style="margin-top: 10px;">
              管理员
            </el-tag>
            <el-tag v-else type="success" effect="dark" style="margin-top: 10px;">
              普通用户
            </el-tag>
          </div>
          
          <el-divider />
          
          <!-- 详细信息 -->
          <el-descriptions title="账户信息" :column="1" border>
            <el-descriptions-item label="用户名">
              <el-icon><user /></el-icon>
              {{ userInfo.username }}
            </el-descriptions-item>
            <el-descriptions-item label="邮箱地址">
              <el-icon><message /></el-icon>
              {{ userInfo.email }}
              <el-button type="primary" link size="small" @click="showEmailDialog = true" style="margin-left: 10px;">
                修改邮箱
              </el-button>
            </el-descriptions-item>
            <el-descriptions-item label="用户 ID">
              <el-icon><id-card /></el-icon>
              {{ userInfo.id }}
            </el-descriptions-item>
            <el-descriptions-item label="注册时间">
              <el-icon><calendar /></el-icon>
              {{ formatDateTime(userInfo.createdAt) }}
            </el-descriptions-item>
            <el-descriptions-item label="最后登录">
              <el-icon><clock /></el-icon>
              {{ formatDateTime(userInfo.lastLogin) || '暂无记录' }}
            </el-descriptions-item>
            <el-descriptions-item label="账户角色">
              <el-tag v-if="userInfo.role === 5" type="danger">管理员</el-tag>
              <el-tag v-else type="success">普通用户</el-tag>
            </el-descriptions-item>
          </el-descriptions>
          
          <!-- 安全设置 -->
          <el-divider content-position="left">安全设置</el-divider>
          <div style="text-align: center; display: flex; gap: 15px; justify-content: center; flex-wrap: wrap;">
            <el-button type="warning" @click="showPasswordDialog = true">
              <el-icon><lock /></el-icon>
              修改密码
            </el-button>
            <!-- 管理员专用：更新用户角色 -->
            <el-button 
              v-if="userInfo.role === 5" 
              type="danger" 
              @click="showRoleDialog = true"
            >
              <el-icon><user-filled /></el-icon>
              更新用户角色
            </el-button>
          </div>
          
          <!-- 统计信息 -->
          <el-divider content-position="left">数据统计</el-divider>
          
          <el-row :gutter="20" class="stats-row">
            <el-col :span="8">
              <el-statistic title="转写次数" :value="transcriptionCount">
                <template #prefix>
                  <el-icon><document /></el-icon>
                </template>
              </el-statistic>
            </el-col>
            <el-col :span="8">
              <el-statistic title="AI 对话次数" :value="chatCount">
                <template #prefix>
                  <el-icon><chat-dot-square /></el-icon>
                </template>
              </el-statistic>
            </el-col>
            <el-col :span="8">
              <el-statistic title="生成题目数" :value="questionCount">
                <template #prefix>
                  <el-icon><collection /></el-icon>
                </template>
              </el-statistic>
            </el-col>
          </el-row>
        </div>
      </div>
    </el-card>

    <!-- 修改邮箱对话框 -->
    <el-dialog
      v-model="showEmailDialog"
      title="修改邮箱"
      width="450px"
      :close-on-click-modal="false"
    >
      <el-form :model="emailForm" :rules="emailRules" ref="emailFormRef" label-width="80px">
        <el-form-item label="当前邮箱">
          <el-input v-model="userInfo.email" disabled />
        </el-form-item>
        <el-form-item label="新邮箱" prop="newEmail">
          <el-input v-model="emailForm.newEmail" placeholder="请输入新邮箱地址" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEmailDialog = false">取消</el-button>
        <el-button type="primary" @click="handleUpdateEmail" :loading="emailLoading">
          确认修改
        </el-button>
      </template>
    </el-dialog>

    <!-- 修改密码对话框 -->
    <el-dialog
      v-model="showPasswordDialog"
      title="修改密码"
      width="450px"
      :close-on-click-modal="false"
    >
      <el-form :model="passwordForm" :rules="passwordRules" ref="passwordFormRef" label-width="90px">
        <el-form-item label="原密码" prop="oldPassword">
          <el-input
            v-model="passwordForm.oldPassword"
            type="password"
            placeholder="请输入原密码"
            show-password
          />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="passwordForm.newPassword"
            type="password"
            placeholder="请输入新密码（至少6位）"
            show-password
          />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="passwordForm.confirmPassword"
            type="password"
            placeholder="请再次输入新密码"
            show-password
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showPasswordDialog = false">取消</el-button>
        <el-button type="primary" @click="handleUpdatePassword" :loading="passwordLoading">
          确认修改
        </el-button>
      </template>
    </el-dialog>

    <!-- 更新用户角色对话框（仅管理员可见） -->
    <el-dialog
      v-model="showRoleDialog"
      title="更新用户角色"
      width="450px"
      :close-on-click-modal="false"
    >
      <el-alert
        title="注意：此操作仅管理员可用"
        type="warning"
        :closable="false"
        style="margin-bottom: 20px;"
      />
      <el-form :model="roleForm" :rules="roleRules" ref="roleFormRef" label-width="100px">
        <el-form-item label="目标账号" prop="username">
          <el-input v-model="roleForm.username" placeholder="请输入要修改的用户账号" />
        </el-form-item>
        <el-form-item label="新角色" prop="role">
          <el-radio-group v-model="roleForm.role">
            <el-radio :label="3">老师</el-radio>
            <el-radio :label="5">管理员</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showRoleDialog = false">取消</el-button>
        <el-button type="danger" @click="handleUpdateRole" :loading="roleLoading">
          确认更新
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  User,
  Message,
  UserFilled,
  Calendar,
  Clock,
  Document,
  ChatDotSquare,
  Collection,
  Lock
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { updateEmail, updatePassword, updateUserRole } from '@/api/auth'
import axios from 'axios'

const router = useRouter()
const userStore = useUserStore()

// 用户信息
const loading = ref(false)
const userInfo = ref({
  id: null,
  username: '',
  email: '',
  role: 0,
  createdAt: null,
  lastLogin: null
})

// 统计数据
const transcriptionCount = ref(0)
const chatCount = ref(0)
const questionCount = ref(0)

// 默认头像
const avatarUrl = ref('')

// 邮箱修改相关
const showEmailDialog = ref(false)
const emailLoading = ref(false)
const emailFormRef = ref(null)
const emailForm = ref({
  newEmail: ''
})

// 邮箱验证规则
const emailRules = {
  newEmail: [
    { required: true, message: '请输入新邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ]
}

// 密码修改相关
const showPasswordDialog = ref(false)
const passwordLoading = ref(false)
const passwordFormRef = ref(null)
const passwordForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 密码验证规则
const validateConfirmPassword = (rule, value, callback) => {
  if (value !== passwordForm.value.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const passwordRules = {
  oldPassword: [
    { required: true, message: '请输入原密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '新密码长度不能少于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

// 用户角色修改相关
const showRoleDialog = ref(false)
const roleLoading = ref(false)
const roleFormRef = ref(null)
const roleForm = ref({
  username: '',
  role: 3
})

const roleRules = {
  username: [
    { required: true, message: '请输入目标用户账号', trigger: 'blur' }
  ],
  role: [
    { required: true, message: '请选择角色', trigger: 'change' }
  ]
}

// 格式化日期时间
const formatDateTime = (dateTime) => {
  if (!dateTime) return ''
  const date = new Date(dateTime)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

// 加载用户信息
const loadUserInfo = async () => {
  loading.value = true
  
  try {
    // 从 store 获取用户信息
    if (userStore.userInfo) {
      userInfo.value = {
        id: userStore.userInfo.id || userStore.userInfo.userId,
        username: userStore.userInfo.username,
        email: userStore.userInfo.email || '未设置',
        role: userStore.userInfo.role || 0,
        createdAt: userStore.userInfo.createdAt,
        lastLogin: userStore.userInfo.lastLogin
      }
      
      console.log('用户信息:', userInfo.value)
    } else {
      // 如果 store 中没有，尝试从后端获取
      const response = await axios.get('/api/auth/me', {
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        }
      })
      
      if (response.data.success) {
        userInfo.value = response.data.user
      }
    }
    
    // 模拟统计数据
    transcriptionCount.value = 12
    chatCount.value = 25
    questionCount.value = 8
    
  } catch (error) {
    console.error('加载用户信息失败:', error)
    ElMessage.error('加载用户信息失败，请重试')
  } finally {
    loading.value = false
  }
}

// 修改邮箱
const handleUpdateEmail = async () => {
  if (!emailFormRef.value) return
  
  await emailFormRef.value.validate(async (valid) => {
    if (!valid) return
    
    emailLoading.value = true
    try {
      const response = await updateEmail(emailForm.value.newEmail)
      
      if (response.data.success) {
        ElMessage.success('邮箱修改成功')
        // 更新本地用户信息
        userInfo.value.email = emailForm.value.newEmail
        userStore.setUserInfo({ ...userStore.userInfo, email: emailForm.value.newEmail })
        showEmailDialog.value = false
        emailForm.value.newEmail = ''
      } else {
        ElMessage.error(response.data.message || '修改失败')
      }
    } catch (error) {
      console.error('修改邮箱失败:', error)
      ElMessage.error(error.response?.data?.message || '修改失败，请稍后重试')
    } finally {
      emailLoading.value = false
    }
  })
}

// 修改密码
const handleUpdatePassword = async () => {
  if (!passwordFormRef.value) return
  
  await passwordFormRef.value.validate(async (valid) => {
    if (!valid) return
    
    passwordLoading.value = true
    try {
      const response = await updatePassword(
        passwordForm.value.oldPassword,
        passwordForm.value.newPassword
      )
      
      if (response.data.success) {
        ElMessage.success('密码修改成功，请重新登录')
        showPasswordDialog.value = false
        passwordForm.value = {
          oldPassword: '',
          newPassword: '',
          confirmPassword: ''
        }
        // 延迟退出登录
        setTimeout(() => {
          userStore.logout()
          router.push('/login')
        }, 1500)
      } else {
        ElMessage.error(response.data.message || '修改失败')
      }
    } catch (error) {
      console.error('修改密码失败:', error)
      ElMessage.error(error.response?.data?.message || '修改失败，请稍后重试')
    } finally {
      passwordLoading.value = false
    }
  })
}

// 更新用户角色（管理员专用）
const handleUpdateRole = async () => {
  if (!roleFormRef.value) return
  
  await roleFormRef.value.validate(async (valid) => {
    if (!valid) return
    
    roleLoading.value = true
    try {
      const response = await updateUserRole(
        roleForm.value.username,
        roleForm.value.role
      )
      
      if (response.data.success) {
        const roleName = roleForm.value.role === 5 ? '管理员' : '老师'
        ElMessage.success(`用户 "${roleForm.value.username}" 的角色已更新为${roleName}`)
        showRoleDialog.value = false
        roleForm.value = {
          username: '',
          role: 3
        }
      } else {
        ElMessage.error(response.data.message || '更新失败')
      }
    } catch (error) {
      console.error('更新用户角色失败:', error)
      ElMessage.error(error.response?.data?.message || '更新失败，请稍后重试')
    } finally {
      roleLoading.value = false
    }
  })
}

// 返回主页
const handleBack = () => {
  router.push('/')
}

onMounted(() => {
  loadUserInfo()
})
</script>

<style scoped>
.profile-container {
  padding: 20px;
  min-height: calc(100vh - 60px);
  background-color: #f0f2f5;
}

.profile-card {
  max-width: 900px;
  margin: 0 auto;
  border-radius: 12px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 18px;
  font-weight: bold;
}

.user-info-section {
  padding: 20px 0;
}

.avatar-section {
  text-align: center;
  margin-bottom: 30px;
}

.username {
  margin: 15px 0 10px;
  font-size: 24px;
  color: #333;
  font-weight: 600;
}

:deep(.el-descriptions__label) {
  width: 120px;
  font-weight: 600;
  color: #606266;
}

:deep(.el-descriptions__content) {
  color: #333;
}

:deep(.el-descriptions-item__container) {
  align-items: center;
}

:deep(.el-descriptions-item__container .el-icon) {
  margin-right: 5px;
  color: #409EFF;
}

.stats-row {
  margin-top: 20px;
  text-align: center;
}

:deep(.el-statistic) {
  padding: 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 8px;
  color: white;
}

:deep(.el-statistic__title) {
  color: rgba(255, 255, 255, 0.9);
  font-size: 14px;
}

:deep(.el-statistic__value) {
  color: white;
  font-size: 32px;
  font-weight: bold;
}

:deep(.el-statistic__prefix) {
  color: white;
  margin-right: 8px;
}

/* 响应式布局 */
@media (max-width: 768px) {
  .profile-container {
    padding: 10px;
  }
  
  .stats-row {
    flex-direction: column;
  }
  
  :deep(.el-col-8) {
    width: 100%;
    margin-bottom: 15px;
  }
}
</style>
