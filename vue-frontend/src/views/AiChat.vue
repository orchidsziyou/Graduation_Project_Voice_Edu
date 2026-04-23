<template>
  <div class="chat-container">
    <el-card class="chat-card">
      <template #header>
        <div class="card-header">
          <span>AI 智能对话</span>
          <el-button @click="handleClearHistory" :disabled="chatHistory.length === 0">
            清除历史
          </el-button>
        </div>
      </template>
      
      <div class="chat-box" ref="chatBoxRef">
        <div
          v-for="(message, index) in chatHistory"
          :key="index"
          :class="['message', message.role]"
        >
          <div class="message-avatar">
            <el-avatar v-if="message.role === 'user'" :size="40">
              <el-icon><user /></el-icon>
            </el-avatar>
            <el-avatar v-else :size="40" style="background-color: #67C23A;">
              🤖
            </el-avatar>
          </div>
          
          <div class="message-content">
            <div class="message-bubble">
              {{ message.content }}
            </div>
            <div class="message-time">
              {{ message.time }}
            </div>
          </div>
        </div>
        
        <div v-if="loading" class="message ai">
          <div class="message-avatar">
            <el-avatar :size="40" style="background-color: #67C23A;">
              🤖
            </el-avatar>
          </div>
          <div class="message-content">
            <div class="message-bubble">
              <el-skeleton :rows="3" animated />
            </div>
          </div>
        </div>
      </div>
      
      <div class="input-area">
        <el-input
          v-model="inputMessage"
          type="textarea"
          :rows="3"
          placeholder="请输入您的问题..."
          :disabled="loading"
          @keyup.enter.exact="handleSendMessage"
        />
        <el-button
          type="primary"
          size="large"
          :loading="loading"
          :disabled="!inputMessage.trim()"
          @click="handleSendMessage"
        >
          发送
        </el-button>
      </div>
      
    </el-card>
  </div>
  <!-- 返回 Dashboard 按钮 - 居中显示 -->
      <div style="text-align: center; margin-top: 15px;">
        <el-button
          size="large"
          @click="returndashboard"
        >
          返回 Dashboard
        </el-button>
      </div>
</template>

<script setup>
import { ref, nextTick, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User } from '@element-plus/icons-vue'
import { sendChatMessage, getChatHistory, clearChatHistory as clearHistoryApi } from '@/api/ai'

const router = useRouter()
const chatBoxRef = ref(null)
const inputMessage = ref('')
const loading = ref(false)
const chatHistory = ref([])

// 格式化时间
const formatTime = (date) => {
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

// 滚动到底部
const scrollToBottom = async () => {
  await nextTick()
  if (chatBoxRef.value) {
    chatBoxRef.value.scrollTop = chatBoxRef.value.scrollHeight
  }
}

// 加载历史消息
const loadHistory = async () => {
  try {
    const response = await getChatHistory()
    
    if (response.data.success) {
      chatHistory.value = response.data.history || []
    }
  } catch (error) {
    console.error('加载历史失败:', error)
  }
}

// 发送消息
const handleSendMessage = async () => {
  if (!inputMessage.value.trim()) return
  
  const userMessage = inputMessage.value.trim()
  inputMessage.value = ''
  
  // 添加用户消息到历史
  chatHistory.value.push({
    role: 'user',
    content: userMessage,
    time: formatTime(new Date())
  })
  
  loading.value = true
  await scrollToBottom()
  
  try {
    const response = await sendChatMessage(userMessage)
    
    if (response.data.success) {
      // 添加 AI 回复到历史
      chatHistory.value.push({
        role: 'ai',
        content: response.data.response,
        time: formatTime(new Date())
      })
    } else {
      ElMessage.error(response.data.message || '发送失败')
    }
  } catch (error) {
    console.error('发送失败:', error)
    ElMessage.error(error.response?.data?.message || '发送失败，请检查网络连接')
  } finally {
    loading.value = false
    await scrollToBottom()
  }
}

// 清除历史
const handleClearHistory = async () => {
  try {
    await clearHistoryApi()
    chatHistory.value = []
    ElMessage.success('已清除对话历史')
  } catch (error) {
    console.error('清除历史失败:', error)
    ElMessage.error('清除历史失败')
  }
}

const returndashboard = () => {
  router.push('/')
}

onMounted(() => {
  loadHistory()
})
</script>

<style scoped>
.chat-container {
  padding: 20px;
}

.chat-card {
  max-width: 900px;
  margin: 0 auto;
  height: calc(100vh - 120px);
  display: flex;
  flex-direction: column;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chat-box {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background-color: #f5f7fa;
  border-radius: 8px;
  margin-bottom: 20px;
}

.message {
  display: flex;
  margin-bottom: 20px;
}

.message.user {
  flex-direction: row-reverse;
}

.message.ai {
  flex-direction: row;
}

.message-avatar {
  flex-shrink: 0;
}

.message-content {
  max-width: 70%;
  margin: 0 10px;
}

.message-bubble {
  padding: 12px 16px;
  border-radius: 12px;
  background-color: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  font-size: 14px;
  line-height: 1.6;
}

.message.user .message-bubble {
  background-color: #409EFF;
  color: white;
}

.message-time {
  font-size: 12px;
  color: #999;
  margin-top: 5px;
  text-align: right;
}

.input-area {
  display: flex;
  gap: 10px;
}

.input-area :deep(.el-textarea) {
  flex: 1;
}

:deep(.chat-box::-webkit-scrollbar) {
  width: 6px;
}

:deep(.chat-box::-webkit-scrollbar-thumb) {
  background-color: #ccc;
  border-radius: 3px;
}
</style>
