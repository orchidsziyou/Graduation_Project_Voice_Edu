<template>
  <div class="answer-container">
    <el-card class="answer-card">
      <template #header>
        <div class="card-header">
          <span>在线答题</span>
          <el-button type="primary" @click="handleRefresh">
            🔄 刷新
          </el-button>
        </div>
      </template>

      <!-- 班级推送题目区域 -->
      <div class="pending-section">
        <el-divider content-position="left">班级推送题目</el-divider>
        
        <!-- 查询所有推送题目按钮 -->
        <div style="margin-bottom: 15px; text-align: right;">
          <el-button 
            type="primary" 
            @click="showAllAssignmentsDialog = true"
            :loading="queryingAssignments"
          >
              查询所有推送题目
          </el-button>
        </div>
        
        <!-- 空状态提示 -->
        <el-empty 
          v-if="pendingAssignments.length === 0 && !queryingAssignments" 
          description="暂无待完成任务"
        />
        
        <!-- 显示待完成任务 -->
        <el-collapse v-else v-model="activeCollapse" accordion>
          <el-collapse-item
            v-for="task in pendingAssignments"
            :key="task.recordId"
            :name="task.recordId"
          >
            <template #title>
              <div class="pending-task-title">
                <el-tag :type="task.status === 0 ? 'danger' : 'success'" size="small">
                  {{ task.status === 0 ? '未完成' : '已完成' }}
                </el-tag>
                <span class="task-name">{{ task.assignmentTitle }}</span>
                <span v-if="task.deadline" class="task-deadline">
                  截止：{{ formatDate(task.deadline) }}
                </span>
              </div>
            </template>
            
            <div class="pending-task-content">
              <el-descriptions :column="1" border>
                <el-descriptions-item label="推送标题">
                  {{ task.assignmentTitle }}
                </el-descriptions-item>
                <el-descriptions-item label="作答要求" v-if="task.requirement">
                  {{ task.requirement }}
                </el-descriptions-item>
                <el-descriptions-item label="截止时间">
                  {{ task.deadline ? formatDate(task.deadline) : '无截止时间' }}
                </el-descriptions-item>
                <el-descriptions-item label="题目类型">
                  <el-tag :type="task.questionType === 0 ? 'success' : 'warning'" size="small">
                    {{ task.questionType === 0 ? '选择题' : '填空题' }}
                  </el-tag>
                </el-descriptions-item>
              </el-descriptions>
              
              <!-- 题目内容 -->
              <div v-if="task.questionBody" class="task-question-body">
                <h4>题目内容</h4>
                <p>{{ task.questionBody }}</p>
              </div>
              
              <!-- 选择题选项 -->
              <div v-if="task.questionType === 0 && task.choosingAnswer" class="task-options">
                <h4>选项</h4>
                <div v-for="(option, index) in parseOptions(task.choosingAnswer)" :key="index" class="option-item">
                  <strong>{{ String.fromCharCode(65 + index)}}.</strong> {{ option }}
                </div>
              </div>
              
              <!-- 答题操作 -->
              <div class="task-actions">
                <el-button type="primary" @click="handleAnswerPendingTask(task)">
                  开始答题
                </el-button>
              </div>
            </div>
          </el-collapse-item>
        </el-collapse>
      </div>
      <div class="question-section">
        <el-button 
          type="success" 
          size="large" 
          @click="handleGetRandomQuestion"
          :loading="gettingQuestion"
          style="width: 100%; margin-bottom: 20px;"
        >
          随机抽取一道题目
        </el-button>

        <div v-if="currentQuestion" class="question-display">
          <el-divider content-position="left">当前题目</el-divider>
          
          <div class="question-info">
            <el-tag :type="currentQuestion.question_type === 0 ? 'success' : 'warning'" size="large">
              {{ currentQuestion.question_type === 0 ? '选择题' : '填空题' }}
            </el-tag>
            <span class="question-id">题目 ID: {{ currentQuestion.id }}</span>
          </div>

          <div class="question-body">
            <h3>{{ currentQuestion.question_body }}</h3>
          </div>

          <!-- 选择题选项 -->
          <div v-if="currentQuestion.question_type === 0 && currentQuestion.choosing_answer" class="options-section">
            <el-radio-group v-model="userAnswer" class="options-list">
              <div v-for="(option, index) in parseOptions(currentQuestion.choosing_answer)" :key="index">
                <el-radio :label="String.fromCharCode(65 + index)" border size="large" style="display: block; margin: 10px 0; padding: 15px;">
                  <span class="option-label">{{ String.fromCharCode(65 + index )}}.</span>
                  <span class="option-content">{{ option }}</span>
                </el-radio>
              </div>
            </el-radio-group>
          </div>

          <!-- 填空题输入框 -->
          <div v-else-if="currentQuestion.question_type === 1" class="answer-input-section">
            <el-input
              v-model="userAnswer"
              type="textarea"
              :rows="3"
              placeholder="请输入你的答案..."
              style="font-size: 16px;"
            />
          </div>

          <!-- 提交按钮 -->
          <div class="submit-actions">
            <el-button 
              type="primary" 
              size="large" 
              @click="handleSubmitAnswer"
              :loading="submitting"
              :disabled="!userAnswer || !currentQuestion"
            >
              提交答案
            </el-button>
            <el-button 
              size="large" 
              @click="handleClearAnswer"
            >
              清空答案
            </el-button>
          </div>
        </div>

        <el-empty v-else description="点击上方按钮随机抽取一道题目" />
      </div>

      <!-- 答题记录 -->
      <div class="records-section">
        <el-divider content-position="left">我的答题记录</el-divider>
        
        <el-table 
          v-loading="loadingRecords"
          :data="answerRecords" 
          stripe 
          style="width: 100%"
        >
          <el-table-column prop="id" label="记录 ID" width="80" />
          <el-table-column label="题目 ID" width="100">
            <template #default="{ row }">
              {{ row.questionid }}
            </template>
          </el-table-column>
          <el-table-column label="题型" width="100">
            <template #default="{ row }">
              <el-tag :type="row.question_type === 0 ? 'success' : 'warning'">
                {{ row.question_type === 0 ? '选择题' : '填空题' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="question_answer" label="你的答案" min-width="200" show-overflow-tooltip />
          <el-table-column label="答题时间" width="180">
            <template #default="{ row }">
              {{ formatDate(row.createdAt) }}
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页 -->
        <div v-if="answerRecords.length > 0" class="pagination-section">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next, jumper"
            :total="totalRecords"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </div>
      </div>
    </el-card>
    
    <!-- 所有推送题目弹窗 -->
    <el-dialog 
      v-model="showAllAssignmentsDialog" 
      title="所有班级推送题目" 
      width="80%"
      :close-on-click-modal="false"
      class="all-assignments-dialog"
    >
      <div v-loading="queryingAssignments" class="dialog-content">
        <!-- 筛选栏 -->
        <div class="filter-bar">
          <el-radio-group v-model="filterStatus" @change="handleFilterChange">
            <el-radio-button label="all">全部</el-radio-button>
            <el-radio-button label="pending">未完成</el-radio-button>
            <el-radio-button label="completed">已完成</el-radio-button>
          </el-radio-group>
          
          <el-tag type="info" style="margin-left: 15px;">
            共 {{ filteredAssignments.length }} 个题目
          </el-tag>
          
          <el-button 
            type="primary" 
            size="small" 
            @click="loadAllAssignments"
            :loading="queryingAssignments"
            style="margin-left: auto;"
          >
            🔄 刷新
          </el-button>
        </div>
        
        <!-- 空状态 -->
        <el-empty 
          v-if="filteredAssignments.length === 0 && !queryingAssignments" 
          description="暂无推送题目"
        />
        
        <!-- 题目列表 -->
        <el-collapse v-else v-model="activeAssignmentCollapse" accordion class="assignments-collapse">
          <el-collapse-item
            v-for="task in filteredAssignments"
            :key="task.recordId"
            :name="task.recordId"
          >
            <template #title>
              <div class="assignment-title">
                <el-tag :type="task.status === 0 ? 'danger' : 'success'" size="small">
                  {{ task.status === 0 ? '未完成' : '已完成' }}
                </el-tag>
                <span class="title-text">{{ task.assignmentTitle }}</span>
                <span v-if="task.deadline" class="deadline-text">
                  截止：{{ formatDate(task.deadline) }}
                </span>
              </div>
            </template>
            
            <div class="assignment-content">
              <el-descriptions :column="2" border>
                <el-descriptions-item label="推送标题">
                  {{ task.assignmentTitle }}
                </el-descriptions-item>
                <el-descriptions-item label="作答要求" v-if="task.requirement">
                  {{ task.requirement }}
                </el-descriptions-item>
                <el-descriptions-item label="截止时间">
                  {{ task.deadline ? formatDate(task.deadline) : '无截止时间' }}
                </el-descriptions-item>
                <el-descriptions-item label="题目类型">
                  <el-tag :type="task.questionType === 0 ? 'success' : 'warning'" size="small">
                    {{ task.questionType === 0 ? '选择题' : '填空题' }}
                  </el-tag>
                </el-descriptions-item>
              </el-descriptions>
              
              <!-- 题目内容 -->
              <div v-if="task.questionBody" class="question-body-section">
                <h4>题目内容</h4>
                <p>{{ task.questionBody }}</p>
              </div>
              
              <!-- 选择题选项 -->
              <div v-if="task.questionType === 0 && task.choosingAnswer" class="options-section">
                <h4>选项</h4>
                <div v-for="(option, index) in parseOptions(task.choosingAnswer)" :key="index" class="option-item">
                  <strong>{{ String.fromCharCode(65 + index)}}.</strong> {{ option }}
                </div>
              </div>
              
              <!-- 答题按钮 -->
              <div class="action-buttons">
                <el-button 
                  v-if="task.status === 0"
                  type="primary" 
                  @click="handleAnswerFromDialog(task)"
                >
                  开始答题
                </el-button>
                <el-tag v-else type="success">✅ 已完成</el-tag>
              </div>
            </div>
          </el-collapse-item>
        </el-collapse>
      </div>
      
      <template #footer>
        <el-button @click="showAllAssignmentsDialog = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getRandomQuestion, submitAnswer, getAnswerRecords } from '@/api/answerQuestion'
import { getPendingAssignments, getAllAssignments, submitAnswer as submitAssignmentAnswer } from '@/api/assignment'

const userStore = useUserStore()

// 题目相关
const currentQuestion = ref(null)
const gettingQuestion = ref(false)
const userAnswer = ref('')
const submitting = ref(false)

// 记录相关
const answerRecords = ref([])
const loadingRecords = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const totalRecords = ref(0)

// 待完成任务相关
const pendingAssignments = ref([])
const allAssignments = ref([]) // 所有推送题目
const activeCollapse = ref(null)
const currentRecordId = ref(null) // 记录当前待办任务的 ID
const queryingAssignments = ref(false)
const showAllAssignmentsDialog = ref(false) // 显示弹窗
const filterStatus = ref('all') // 筛选状态：all/pending/completed
const activeAssignmentCollapse = ref(null) // 弹窗中的折叠面板

// 计算属性：根据筛选条件过滤题目
const filteredAssignments = computed(() => {
  if (filterStatus.value === 'all') {
    return allAssignments.value
  } else if (filterStatus.value === 'pending') {
    return allAssignments.value.filter(task => task.status === 0)
  } else {
    return allAssignments.value.filter(task => task.status === 1)
  }
})

// 解析选项
const parseOptions = (choosingAnswer) => {
  if (!choosingAnswer) return []
  return choosingAnswer.split('|').map(opt => opt.trim())
}

// 获取随机题目
const handleGetRandomQuestion = async () => {
  gettingQuestion.value = true
  currentRecordId.value = null // 切换为随机抽题，清空待办标记
  
  try {
    const response = await getRandomQuestion()
    
    if (response.data.success) {
      currentQuestion.value = response.data.data
      userAnswer.value = ''
      ElMessage.success('题目抽取成功，请作答')
      console.log('当前题目:', currentQuestion.value)
    } else {
      ElMessage.error(response.data.message || '获取题目失败')
    }
  } catch (error) {
    console.error('获取题目失败:', error)
    ElMessage.error(error.response?.data?.message || '获取题目失败，请稍后重试')
  } finally {
    gettingQuestion.value = false
  }
}

// 提交答案
const handleSubmitAnswer = async () => {
  if (!userAnswer.value || !currentQuestion.value) {
    ElMessage.warning('请先作答')
    return
  }
  
  // 检查用户是否登录
  if (!userStore.isLoggedIn) {
    ElMessage.error('请先登录')
    return
  }
  
  // 如果没有用户信息，先获取
  let userId = userStore.userId
  if (!userId) {
    console.log('用户信息为空，尝试获取...')
    await userStore.fetchUserInfo()
    userId = userStore.userId
  }
  
  if (!userId) {
    ElMessage.error('无法获取用户信息，请重新登录')
    return
  }
  
  submitting.value = true
  
  try {
    let response;
    
    // 区分对待办任务和随机抽题的提交逻辑
    if (currentRecordId.value) {
      console.log('提交待办任务答案, recordId:', currentRecordId.value)
      response = await submitAssignmentAnswer(currentRecordId.value, userAnswer.value)
    } else {
      console.log('提交随机抽题答案')
      response = await submitAnswer(
        userId,
        currentQuestion.value.id,
        userAnswer.value,
        currentQuestion.value.question_type
      )
    }
    
    if (response.data.success) {
      ElMessage.success('答题成功！答案已保存')
      console.log('答题记录 ID:', response.data.recordId)
      
      // 清空当前题目
      currentQuestion.value = null
      userAnswer.value = ''
      currentRecordId.value = null // 清空标记
      
      // 重新加载记录和待完成任务
      loadAnswerRecords()
      loadPendingAssignments()
    } else {
      ElMessage.error(response.data.message || '提交失败')
    }
  } catch (error) {
    console.error('提交答案失败:', error)
    ElMessage.error(error.response?.data?.message || '提交失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

// 清空答案
const handleClearAnswer = () => {
  currentRecordId.value = null
  userAnswer.value = ''
  ElMessage.info('已清空答案')
}

// 加载答题记录
const loadAnswerRecords = async () => {
  if (!userStore.userId) {
    console.warn('用户未登录')
    return
  }
  
  loadingRecords.value = true
  
  try {
    const response = await getAnswerRecords(userStore.userId, currentPage.value - 1, pageSize.value)
    
    if (response.data.success) {
      answerRecords.value = response.data.data.content || []
      totalRecords.value = response.data.data.totalElements || 0
      console.log('加载了', answerRecords.value.length, '条答题记录')
    } else {
      ElMessage.error(response.data.message || '加载记录失败')
    }
  } catch (error) {
    console.error('加载记录失败:', error)
    ElMessage.error(error.response?.data?.message || '加载失败，请稍后重试')
  } finally {
    loadingRecords.value = false
  }
}

// 加载待完成任务
const loadPendingAssignments = async () => {
  if (!userStore.userId) {
    console.warn('用户未登录')
    return
  }
  
  try {
    const response = await getPendingAssignments(userStore.userId)
    
    if (response.data.success) {
      pendingAssignments.value = response.data.data || []
      console.log('加载了', pendingAssignments.value.length, '个待完成任务')
    } else {
      ElMessage.error(response.data.message || '加载待完成任务失败')
    }
  } catch (error) {
    console.error('加载待完成任务失败:', error)
    ElMessage.error(error.response?.data?.message || '加载失败，请稍后重试')
  }
}

// 查询所有推送题目（包括已完成和未完成）
const loadAllAssignments = async () => {
  if (!userStore.userId) {
    ElMessage.warning('请先登录')
    return
  }
  
  queryingAssignments.value = true
  
  try {
    console.log('查询所有推送题目...')
    const response = await getAllAssignments(userStore.userId)
    
    if (response.data.success) {
      allAssignments.value = response.data.data || []
      ElMessage.success(`查询到 ${allAssignments.value.length} 个推送题目`)
      console.log('所有推送题目:', allAssignments.value)
    } else {
      ElMessage.error(response.data.message || '查询失败')
    }
  } catch (error) {
    console.error('查询所有推送题目失败:', error)
    ElMessage.error(error.response?.data?.message || '查询失败，请稍后重试')
  } finally {
    queryingAssignments.value = false
  }
}

// 切换为仅显示未完成
const handleShowPendingOnly = () => {
  allAssignments.value = []
  ElMessage.info('已切换为仅显示未完成题目')
}

// 开始回答待完成的任务
const handleAnswerPendingTask = (task) => {
  // 设置当前题目为待完成任务的题目
  currentQuestion.value = {
    id: task.questionId,
    question_body: task.questionBody,
    question_type: task.questionType,
    choosing_answer: task.choosingAnswer
  }
  
  // 核心修改：记录当前待办任务的记录 ID
  currentRecordId.value = task.recordId
  
  userAnswer.value = ''
  
  // 滚动到题目区域
  setTimeout(() => {
    const questionSection = document.querySelector('.question-section')
    if (questionSection) {
      questionSection.scrollIntoView({ behavior: 'smooth', block: 'start' })
    }
  }, 100)
  
  ElMessage.success('已加载题目，请作答')
}

// 格式化日期
const formatDate = (dateString) => {
  if (!dateString) return '-'
  const date = new Date(dateString)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 刷新
const handleRefresh = () => {
  loadAnswerRecords()
}

// 分页大小变化
const handleSizeChange = () => {
  currentPage.value = 1
  loadAnswerRecords()
}

// 页码变化
const handleCurrentChange = () => {
  loadAnswerRecords()
}

onMounted(() => {
  // console.log('=== AnswerQuestion 组件已挂载 ===')
  // console.log('当前用户登录状态:', userStore.isLoggedIn)
  // console.log('当前用户信息:', userStore.userInfo)
  // console.log('当前用户 ID:', userStore.userId)
  
  // 如果已登录但没有用户信息，尝试获取
  if (userStore.isLoggedIn && !userStore.userInfo) {
    console.log('已登录但无用户信息，尝试获取...')
    userStore.fetchUserInfo().then(() => {
      console.log('用户信息获取完成:', userStore.userInfo)
      console.log('用户 ID:', userStore.userId)
    })
  }
  
  if (userStore.isLoggedIn) {
    loadAnswerRecords()
    loadPendingAssignments()
  }
})
</script>

<style scoped>
.answer-container {
  padding: 20px;
  min-height: calc(100vh - 60px);
  background-color: #f0f2f5;
}

.answer-card {
  max-width: 900px;
  margin: 0 auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.question-section {
  margin-bottom: 30px;
}

.question-display {
  margin-top: 20px;
}

.question-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  padding: 10px;
  background-color: #f5f7fa;
  border-radius: 6px;
}

.question-id {
  color: #909399;
  font-size: 14px;
}

.question-body {
  padding: 20px;
  background-color: white;
  border-radius: 8px;
  border: 2px solid #e4e7ed;
  margin-bottom: 20px;
}

.question-body h3 {
  margin: 0;
  font-size: 18px;
  line-height: 1.6;
  color: #303133;
}

.options-section {
  margin-bottom: 20px;
}

.options-list {
  width: 100%;
}

.option-label {
  font-weight: bold;
  color: #409EFF;
  margin-right: 10px;
  font-size: 16px;
}

.option-content {
  font-size: 15px;
  color: #606266;
}

.answer-input-section {
  margin-bottom: 20px;
}

.submit-actions {
  display: flex;
  gap: 15px;
  justify-content: center;
  padding: 20px;
  background-color: #f5f7fa;
  border-radius: 8px;
}

.records-section {
  margin-top: 30px;
}

.pagination-section {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

/* 待完成任务样式 */
.pending-section {
  margin-bottom: 30px;
}

.pending-task-title {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
}

.task-name {
  font-weight: bold;
  font-size: 15px;
  color: #303133;
}

.task-deadline {
  color: #f56c6c;
  font-size: 13px;
  margin-left: auto;
}

.pending-task-content {
  padding: 10px 0;
}

.task-question-body {
  margin-top: 15px;
  padding: 15px;
  background-color: #f5f7fa;
  border-radius: 8px;
  border-left: 4px solid #409EFF;
}

.task-question-body h4 {
  margin: 0 0 10px 0;
  color: #303133;
  font-size: 16px;
}

.task-question-body p {
  margin: 0;
  color: #606266;
  font-size: 15px;
  line-height: 1.6;
}

.task-options {
  margin-top: 15px;
}

.task-options h4 {
  margin: 0 0 10px 0;
  color: #303133;
  font-size: 16px;
}

.option-item {
  padding: 8px 12px;
  margin: 5px 0;
  background-color: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  color: #606266;
}

.option-item strong {
  color: #409EFF;
  margin-right: 8px;
}

.task-actions {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}
</style>
