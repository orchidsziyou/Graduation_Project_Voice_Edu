<template>
  <div class="dashboard-container">
    <!-- 顶部导航栏 -->
    <el-header class="header">
      <div class="header-left">
        <h1 class="logo">🎙️ 智能随堂测验生成系统</h1>
      </div>
      
      <div class="header-right">
        <!-- 未登录时显示登录按钮 -->
        <el-button 
          v-if="!userStore.isLoggedIn" 
          type="success" 
          size="large"
          @click="handleLogin"
          style="margin-right: 15px;"
        >
          立即登录
        </el-button>
        
        <!-- 已登录时显示用户信息 -->
        <el-dropdown v-else @command="handleCommand">
          <span class="user-info">
            <el-avatar :size="32" style="margin-right: 8px;">
              <el-icon><user /></el-icon>
            </el-avatar>
            {{ userStore.username }}
            <el-icon class="el-icon--right"><arrow-down /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人中心</el-dropdown-item>
              <el-dropdown-item command="settings">系统设置</el-dropdown-item>
              <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-header>
    
    <el-container class="main-container">
      <!-- 左侧边栏 -->
      <el-aside width="220px" class="sidebar">
        <el-menu
          :default-active="activeMenu"
          background-color="#304156"
          text-color="#bfcbd9"
          active-text-color="#409EFF"
          router
        >
          <el-menu-item index="/">
            <el-icon><office-building /></el-icon>
            <span>工作台</span>
          </el-menu-item>
          
          <el-sub-menu index="transcription">
            <template #title>
              <el-icon><microphone /></el-icon>
              <span>语音转写</span>
            </template>
            <el-menu-item index="/transcription/upload">
              上传音频
            </el-menu-item>
            <el-menu-item index="/transcription/auto-recording">
              自动录音
            </el-menu-item>
            <el-menu-item index="/transcription/records">
              转写记录
            </el-menu-item>
          </el-sub-menu>

          <el-menu-item index="/ai-chat">
            <el-icon><chat-dot-round /></el-icon>
            <span>AI 对话</span>
          </el-menu-item>
          
          <el-sub-menu index="ai-question">
            <template #title>
              <el-icon><edit /></el-icon>
              <span>AI 智能出题</span>
            </template>
            <el-menu-item index="/ai-question/generate">
              <el-icon><magic-stick /></el-icon>
              <span>智能出题</span>
            </el-menu-item>
            <el-menu-item index="/ai-question/records">
              <el-icon><collection /></el-icon>
              <span>出题记录</span>
            </el-menu-item>
          </el-sub-menu>

          <el-sub-menu index="question">
            <template #title>
              <el-icon><edit /></el-icon>
              <span>题目</span>
            </template>
            <el-menu-item index="/answer-question">
              <el-icon><check /></el-icon>
              <span>答题</span>
            </el-menu-item>
            <el-menu-item index="/question-list">
              <el-icon><reading /></el-icon>
              <span>题库浏览</span>
            </el-menu-item>

          </el-sub-menu>

          

          <el-sub-menu index="class-management">
            <template #title>
              <el-icon><school /></el-icon>
              <span>班级管理</span>
            </template>
            <el-menu-item index="/class-management/my-classes">
              我的班级
            </el-menu-item>
            <el-menu-item index="/class-management/create">
              创建班级
            </el-menu-item>
            <el-menu-item index="/class-management/members">
              加入班级
            </el-menu-item>
          </el-sub-menu>

          <el-sub-menu index="adminPage">
            <template #title>
              <el-icon><school /></el-icon>
              <span>管理员界面</span>
            </template>
            <el-menu-item index="/sql-query">
            <el-icon><DataLine /></el-icon>
              <span>SQL 查询</span>
            </el-menu-item>
            <el-menu-item index="/admin/files">
            <el-icon><DataLine /></el-icon>
              <span>后台文件管理</span>
            </el-menu-item>

          </el-sub-menu>
          
          <el-divider direction="vertical" style="width: 100%; border-top: 1px solid #4a5568;" />
          
          <el-menu-item index="/settings">
            <el-icon><setting /></el-icon>
            <span>系统设置</span>
          </el-menu-item>
        </el-menu>
      </el-aside>
      
      <!-- 主内容区 -->
      <el-main class="main-content">
        <!-- 统计卡片 -->
        <el-row :gutter="20" class="stats-cards">
          <el-col :span="6">
            <el-card shadow="hover" class="stat-card">
              <div class="stat-content">
                <div class="stat-icon blue">
                  <el-icon><document /></el-icon>
                </div>
                <div class="stat-info">
                  <div class="stat-value">12</div>
                  <div class="stat-label">我的转写</div>
                </div>
              </div>
            </el-card>
          </el-col>
          
          <el-col :span="6">
            <el-card shadow="hover" class="stat-card">
              <div class="stat-content">
                <div class="stat-icon green">
                  <el-icon><finished /></el-icon>
                </div>
                <div class="stat-info">
                  <div class="stat-value">8</div>
                  <div class="stat-label">已完成</div>
                </div>
              </div>
            </el-card>
          </el-col>
          
          <el-col :span="6">
            <el-card shadow="hover" class="stat-card">
              <div class="stat-content">
                <div class="stat-icon orange">
                  <el-icon><clock /></el-icon>
                </div>
                <div class="stat-info">
                  <div class="stat-value">3</div>
                  <div class="stat-label">处理中</div>
                </div>
              </div>
            </el-card>
          </el-col>
          
          <el-col :span="6">
            <el-card shadow="hover" class="stat-card">
              <div class="stat-content">
                <div class="stat-icon purple">
                  <el-icon><chat-line-square /></el-icon>
                </div>
                <div class="stat-info">
                  <div class="stat-value">25</div>
                  <div class="stat-label">对话次数</div>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>
        
        <!-- 快捷操作 -->
        <el-card class="quick-actions">
          <template #header>
            <div class="card-header">
              <span>快捷操作</span>
            </div>
          </template>
          
          <el-row :gutter="20">
            <el-col :span="8">
              <el-button
                type="primary"
                size="large"
                icon="upload"
                style="width: 100%"
                @click="handleQuickUpload"
              >
                上传音频
              </el-button>
            </el-col>
            
            <el-col :span="8">
              <el-button
                type="success"
                size="large"
                icon="chat-dot-round"
                style="width: 100%"
                @click="handleQuickChat"
              >
                AI 对话
              </el-button>
            </el-col>
            
            <el-col :span="8">
              <el-button
                type="warning"
                size="large"
                icon="document"
                style="width: 100%"
                @click="handleQuickRecords"
              >
                查看记录
              </el-button>
            </el-col>
          </el-row>
          
          <el-row :gutter="20" style="margin-top: 20px;">
            <el-col :span="8">
              <el-button
                type="info"
                size="large"
                icon="collection"
                style="width: 100%"
                @click="handleQuickAiQuestion"
              >
                AI 出题记录
              </el-button>
            </el-col>
            
            <el-col :span="8">
              <el-button
                type="success"
                size="large"
                icon="edit"
                style="width: 100%"
                @click="handleQuickAiGenerate"
              >
                AI 智能出题
              </el-button>
            </el-col>
            
            <el-col :span="8">
              <el-button
                type="primary"
                size="large"
                icon="search"
                style="width: 100%"
                @click="handleQuickSql"
              >
                SQL 查询
              </el-button>
            </el-col>
          </el-row>
          
          <el-row :gutter="20" style="margin-top: 20px;">
            <el-col :span="8">
              <el-button
                type="danger"
                size="large"
                icon="microphone"
                style="width: 100%"
                @click="handleQuickAutoRecording"
              >
                自动录音
              </el-button>
            </el-col>
          </el-row>
        </el-card>
        
        <!-- 最近转写记录 -->
        <el-card class="recent-records">
          <template #header>
            <div class="card-header">
              <span>最近转写记录</span>
              <el-button type="primary" link @click="handleViewAll">
                查看全部
              </el-button>
            </div>
          </template>
          
          <el-table :data="recentRecords" stripe style="width: 100%">
            <el-table-column prop="fileName" label="文件名" />
            <el-table-column prop="status" label="状态">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)">
                  {{ getStatusText(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="转写时间" />
            <el-table-column prop="duration" label="音频时长" />
            <el-table-column label="操作" width="150">
              <template #default="{ row }">
                <el-button
                  type="primary"
                  size="small"
                  link
                  @click="handleViewDetail(row)"
                >
                  查看详情
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-main>
    </el-container>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  User,
  ArrowDown,
  OfficeBuilding,
  Microphone,
  ChatDotRound,
  DataLine,
  Setting,
  Document,
  Finished,
  Clock,
  ChatLineSquare,
  School,
  Reading,
  Edit,
  Search
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getTranscriptionRecords } from '@/api/transcription'

const router = useRouter()
const userStore = useUserStore()

// 当前激活的菜单
const activeMenu = ref('/')

// 最近转写记录
const recentRecords = ref([])
const recordsLoading = ref(false)

// 加载最近转写记录
const loadRecentRecords = async () => {
  recordsLoading.value = true
  
  try {
    const response = await getTranscriptionRecords()
    
    if (response.data.success) {
      // 获取最近的 5 条记录
      recentRecords.value = (response.data.records || []).slice(0, 5)
      // console.log('Dashboard 加载转写记录成功:', recentRecords.value.length, '条')
    } else {
      console.error('后端返回失败:', response.data.message)
      ElMessage.error(response.data.message || '加载记录失败')
    }
  } catch (error) {
    console.error('加载记录失败:', error)
    console.error('错误详情:', error.response?.data)
    ElMessage.error(error.response?.data?.message || '加载失败，请检查网络连接')
  } finally {
    recordsLoading.value = false
  }
}

// 页面加载时获取数据
onMounted(() => {
  loadRecentRecords()
})

// 获取状态标签类型
const getStatusType = (status) => {
  const types = {
    completed: 'success',
    processing: 'warning',
    failed: 'danger'
  }
  return types[status] || 'info'
}

// 获取状态文本
const getStatusText = (status) => {
  const texts = {
    completed: '已完成',
    processing: '处理中',
    failed: '失败'
  }
  return texts[status] || status
}

// 下拉菜单处理
const handleCommand = async (command) => {
  switch (command) {
    case 'profile':
      router.push('/profile')
      break
    case 'settings':
      ElMessage.info('系统设置功能开发中...')
      // router.push('/settings')
      break
    case 'logout':
      try {
        await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        await userStore.logout()
        ElMessage.success('已退出登录')
        router.push('/login')
      } catch (error) {
        // 用户取消
      }
      break
  }
}

// 快捷操作
const handleQuickUpload = () => {
  router.push('/transcription/upload')
}

const handleQuickChat = () => {
  router.push('/ai-chat')
}

const handleQuickRecords = () => {
  router.push('/transcription/records')
}

const handleQuickAiQuestion = () => {
  router.push('/ai-question/records')
}

const handleQuickAiGenerate = () => {
  router.push('/ai-question/generate')
}

const handleQuickSql = () => {
  router.push('/sql-query')
}

const handleQuickAutoRecording = () => {
  router.push('/transcription/auto-recording')
}

const handleViewAll = () => {
  router.push('/transcription/records')
}

const handleViewDetail = (record) => {
  ElMessage.info(`查看 ${record.fileName} 的详情`)
  // TODO: 实现查看详情逻辑
}
</script>

<style scoped>
.dashboard-container {
  min-height: 100vh;
  background-color: #f0f2f5;
}

.header {
  background-color: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  z-index: 100;
}

.header-left .logo {
  margin: 0;
  font-size: 20px;
  color: #409EFF;
}

.header-right .user-info {
  display: flex;
  align-items: center;
  cursor: pointer;
  color: #606266;
  font-size: 14px;
}

.main-container {
  height: calc(100vh - 60px);
}

.sidebar {
  background-color: #304156;
  overflow-x: hidden;
}

:deep(.el-menu) {
  border-right: none;
}

:deep(.el-menu-item.is-active) {
  background-color: #409EFF !important;
}

.main-content {
  padding: 20px;
}

.stats-cards {
  margin-bottom: 20px;
}

.stat-card {
  border-radius: 8px;
}

.stat-content {
  display: flex;
  align-items: center;
  padding: 10px;
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  color: white;
  margin-right: 15px;
}

.stat-icon.blue {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.stat-icon.green {
  background: linear-gradient(135deg, #56ab2f 0%, #a8e063 100%);
}

.stat-icon.orange {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.stat-icon.purple {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #333;
  margin-bottom: 5px;
}

.stat-label {
  font-size: 13px;
  color: #999;
}

.quick-actions,
.recent-records {
  border-radius: 8px;
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: bold;
}

:deep(.el-table) {
  font-size: 13px;
}

:deep(.el-table th) {
  background-color: #f5f7fa;
  color: #606266;
}
</style>
