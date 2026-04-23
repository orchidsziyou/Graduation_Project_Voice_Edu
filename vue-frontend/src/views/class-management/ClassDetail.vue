<template>
  <div class="class-detail-container">
    <el-card class="class-detail-card">
      <template #header>
        <div class="card-header">
          <div>
            <h2>🏫 {{ classInfo.classname || '班级详情' }}</h2>
            <p v-if="classInfo.classcode" class="class-code">
              <el-icon><key /></el-icon>
              班级码：{{ classInfo.classcode }}
            </p>
          </div>
          <div style="display: flex; gap: 10px;">
            <el-button type="success" @click="handleAssignQuestion" :disabled="!canManageMembers">
              <el-icon><promotion /></el-icon>
              推送题目
            </el-button>
            <el-button type="primary" @click="handleViewAssignments">
              <el-icon><document /></el-icon>
              查看推送记录
            </el-button>
            <el-button @click="handleBack">
              <el-icon><back /></el-icon>
              返回
            </el-button>
            <el-button type="primary" @click="handleRefresh">
              <el-icon><refresh /></el-icon>
              刷新
            </el-button>
          </div>
        </div>
      </template>

      <!-- 班级统计信息 -->
      <div class="class-stats">
        <el-row :gutter="20">
          <el-col :span="8">
            <el-statistic title="班级总人数" :value="members.length">
              <template #prefix>
                <el-icon><user /></el-icon>
              </template>
            </el-statistic>
          </el-col>
          <el-col :span="8">
            <el-statistic title="老师数量" :value="teacherCount">
              <template #prefix>
                <el-icon><user-filled /></el-icon>
              </template>
            </el-statistic>
          </el-col>
          <el-col :span="8">
            <el-statistic title="学生数量" :value="studentCount">
              <template #prefix>
                <el-icon><avatar /></el-icon>
              </template>
            </el-statistic>
          </el-col>
        </el-row>
      </div>

      <!-- 成员列表 -->
      <div v-loading="loading" element-loading-text="正在加载成员列表...">
        <div v-if="members.length === 0 && !loading" class="empty-state">
          <el-empty description="暂无成员" />
        </div>

        <el-table
          v-else
          :data="members"
          stripe
          style="width: 100%"
          :default-sort="{ prop: 'joinAt', order: 'descending' }"
        >
          <el-table-column type="index" label="#" width="60" align="center" />
          
          <el-table-column prop="username" label="用户名" width="150">
            <template #default="{ row }">
              <strong>{{ row.username }}</strong>
            </template>
          </el-table-column>
          
          <el-table-column prop="email" label="邮箱" min-width="200" show-overflow-tooltip />
          
          <el-table-column label="身份" width="120" align="center">
            <template #default="{ row }">
              <el-tag :type="row.userrole === 5 ? 'danger' : 'primary'" size="large">
                {{ row.userrole === 5 ? '老师' : '学生' }}
              </el-tag>
            </template>
          </el-table-column>
          
          <el-table-column label="加入时间" width="180" sortable prop="joinAt">
            <template #default="{ row }">
              {{ formatDate(row.joinAt) }}
            </template>
          </el-table-column>
          
          <el-table-column label="操作" width="150" fixed="right" align="center">
            <template #default="{ row }">
              <el-button
                type="danger"
                size="small"
                @click="handleRemoveMember(row)"
                :disabled="!canManageMembers"
                :loading="removingUserId === row.userid"
              >
                <el-icon><delete /></el-icon>
                移除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 提示信息 -->
      <el-alert
        v-if="!canManageMembers && members.length > 0"
        title="提示：只有老师或班级创建者才能管理成员"
        type="info"
        :closable="false"
        style="margin-top: 20px"
      />
    </el-card>

    <!-- 推送记录对话框 -->
    <el-dialog
      v-model="showAssignmentsDialog"
      title="班级推送记录"
      width="90%"
      :close-on-click-modal="false"
    >
      <div v-loading="assignmentsLoading" element-loading-text="正在加载推送记录...">
        <div v-if="assignments.length === 0 && !assignmentsLoading" class="empty-state">
          <el-empty description="暂无推送记录" />
        </div>

        <el-table
          v-else
          :data="assignments"
          stripe
          style="width: 100%"
        >
          <el-table-column type="index" label="#" width="60" align="center" />
          
          <el-table-column prop="title" label="推送标题" min-width="200" show-overflow-tooltip>
            <template #default="{ row }">
              <strong>{{ row.title }}</strong>
            </template>
          </el-table-column>
          
          <el-table-column label="题目类型" width="120" align="center">
            <template #default="{ row }">
              <el-tag :type="getQuestionTypeColor(row.questionType)" size="small">
                {{ getQuestionTypeText(row.questionType) }}
              </el-tag>
            </template>
          </el-table-column>
          
          <el-table-column label="作答统计" width="200" align="center">
            <template #default="{ row }">
              <div style="display: flex; gap: 10px; justify-content: center;">
                <el-tag type="success" size="small">
                  已答: {{ row.answeredCount }}
                </el-tag>
                <el-tag type="warning" size="small">
                  未答: {{ row.unansweredCount }}
                </el-tag>
              </div>
            </template>
          </el-table-column>
          
          <el-table-column label="正确率" width="120" align="center">
            <template #default="{ row }">
              <el-progress
                :percentage="row.correctRate || 0"
                :color="getProgressColor(row.correctRate)"
                :stroke-width="8"
              />
            </template>
          </el-table-column>
          
          <el-table-column label="截止时间" width="180" sortable prop="deadline">
            <template #default="{ row }">
              {{ formatDate(row.deadline) }}
            </template>
          </el-table-column>
          
          <el-table-column label="操作" width="150" fixed="right" align="center">
            <template #default="{ row }">
              <el-button
                type="primary"
                size="small"
                @click="handleViewAnswerDetail(row)"
              >
                <el-icon><document /></el-icon>
                查看详情
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>

    <!-- 作答详情对话框 -->
    <el-dialog
      v-model="showAnswerDetailDialog"
      :title="`作答详情 - ${currentAssignment?.title || ''}`"
      width="80%"
      :close-on-click-modal="false"
    >
      <div v-loading="answerDetailLoading" element-loading-text="正在加载作答记录...">
        <div v-if="answerRecords.length === 0 && !answerDetailLoading" class="empty-state">
          <el-empty description="暂无作答记录" />
        </div>

        <!-- 选择题：显示选项统计 -->
        <div v-else-if="isChoiceQuestion" class="choice-stats-container">
          <h3 style="margin-bottom: 20px; color: #303133;">选项分布统计</h3>
          <el-row :gutter="20" style="margin-bottom: 30px;">
            <el-col :span="6" v-for="option in ['A', 'B', 'C', 'D']" :key="option">
              <el-card shadow="hover" class="option-stat-card">
                <div class="option-stat-content">
                  <div class="option-label">
                    <el-tag :type="getOptionColor(option)" size="large" effect="dark">
                      选项 {{ option }}
                    </el-tag>
                  </div>
                  <div class="option-count">
                    <span class="count-number">{{ choiceOptionStats[option] }}</span>
                    <span class="count-unit">人选择</span>
                  </div>
                  <el-progress
                    :percentage="getChoicePercentage(option)"
                    :color="getOptionColor(option)"
                    :stroke-width="12"
                    :show-text="true"
                  />
                </div>
              </el-card>
            </el-col>
          </el-row>

          <h3 style="margin-bottom: 20px; color: #303133;">学生作答明细</h3>
          <el-table
            :data="answerRecords"
            stripe
            style="width: 100%"
          >
            <el-table-column type="index" label="#" width="60" align="center" />
            
            <el-table-column prop="studentName" label="学生姓名" width="150">
              <template #default="{ row }">
                <strong>{{ row.studentName }}</strong>
              </template>
            </el-table-column>
            
            <el-table-column label="选择答案" width="120" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.answer && row.status === 1" :type="getOptionColor(row.answer.trim().toUpperCase())" size="large" effect="plain">
                  {{ row.answer.trim().toUpperCase() }}
                </el-tag>
                <span v-else style="color: #909399;">未作答</span>
              </template>
            </el-table-column>
            
            <el-table-column label="是否正确" width="120" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.isCorrect !== null" :type="row.isCorrect ? 'success' : 'danger'" size="small">
                  {{ row.isCorrect ? '正确' : '错误' }}
                </el-tag>
                <span v-else>-</span>
              </template>
            </el-table-column>
            
            <el-table-column label="作答状态" width="120" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                  {{ row.status === 1 ? '已提交' : '未提交' }}
                </el-tag>
              </template>
            </el-table-column>
            
            <el-table-column label="作答时间" width="180" sortable prop="answerTime">
              <template #default="{ row }">
                {{ formatDate(row.answerTime) }}
              </template>
            </el-table-column>
          </el-table>
        </div>

        <!-- 主观题：显示各学生答案 -->
        <div v-else class="subjective-answers-container">
          <h3 style="margin-bottom: 20px; color: #303133;">学生答案列表</h3>
          <el-table
            :data="answerRecords"
            stripe
            style="width: 100%"
          >
            <el-table-column type="index" label="#" width="60" align="center" />
            
            <el-table-column prop="studentName" label="学生姓名" width="150">
              <template #default="{ row }">
                <strong>{{ row.studentName }}</strong>
              </template>
            </el-table-column>
            
            <el-table-column label="作答状态" width="120" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                  {{ row.status === 1 ? '已提交' : '未提交' }}
                </el-tag>
              </template>
            </el-table-column>
            
            <el-table-column label="是否正确" width="120" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.isCorrect !== null" :type="row.isCorrect ? 'success' : 'danger'" size="small">
                  {{ row.isCorrect ? '正确' : '错误' }}
                </el-tag>
                <span v-else>-</span>
              </template>
            </el-table-column>
            
            <el-table-column prop="answer" label="答案内容" min-width="400">
              <template #default="{ row }">
                <div class="answer-content">
                  {{ row.answer || '未作答' }}
                </div>
              </template>
            </el-table-column>
            
            <el-table-column label="作答时间" width="180" sortable prop="answerTime">
              <template #default="{ row }">
                {{ formatDate(row.answerTime) }}
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Key, User, UserFilled, Avatar, Back, Refresh, Delete, Promotion, Document } from '@element-plus/icons-vue'
import { getClassMembersWithUsers, removeClassMember, getClassById } from '@/api/class'
import { getClassAssignmentsWithStats, getAssignmentAnswerDetails } from '@/api/assignment'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const members = ref([])
const classInfo = ref({})
const removingUserId = ref(null)

// 推送记录相关
const showAssignmentsDialog = ref(false)
const assignments = ref([])
const assignmentsLoading = ref(false)

// 作答详情相关
const showAnswerDetailDialog = ref(false)
const currentAssignment = ref(null)
const answerRecords = ref([])
const answerDetailLoading = ref(false)
const currentQuestionInfo = ref(null) // 存储当前题目信息

// 计算属性
const teacherCount = computed(() => {
  return members.value.filter(m => m.userrole === 5).length
})

const studentCount = computed(() => {
  return members.value.filter(m => m.userrole === 1).length
})

// 判断当前用户是否有权限管理成员（老师角色）
const canManageMembers = computed(() => {
  if (!userStore.userId) return false
  
  // 检查当前用户是否是老师
  const currentUser = members.value.find(m => m.userid === userStore.userId)
  return currentUser && currentUser.userrole === 5
})

// 加载班级信息
const loadClassInfo = async () => {
  const classid = route.params.classid
  if (!classid) {
    ElMessage.error('缺少班级ID参数')
    return
  }

  try {
    const response = await getClassById(classid)
    if (response.data.success) {
      classInfo.value = response.data.data
    }
  } catch (error) {
    console.error('加载班级信息失败:', error)
  }
}

// 加载成员列表
const loadMembers = async () => {
  const classid = route.params.classid
  if (!classid) {
    ElMessage.error('缺少班级ID参数')
    return
  }

  loading.value = true

  try {
    console.log('=== 加载班级成员 ===')
    console.log('班级 ID:', classid)

    const response = await getClassMembersWithUsers(classid)

    console.log('后端返回的数据:', response.data)

    if (response.data.success) {
      members.value = response.data.data || []
      console.log('加载成功，成员数:', members.value.length)
    } else {
      console.error('后端返回失败:', response.data.message)
      ElMessage.error(response.data.message || '加载成员失败')
    }
  } catch (error) {
    console.error('加载成员失败:', error)
    console.error('错误详情:', error.response?.data)
    ElMessage.error(error.response?.data?.message || '加载失败，请检查网络连接')
  } finally {
    loading.value = false
  }
}

// 刷新
const handleRefresh = () => {
  loadClassInfo()
  loadMembers()
}

// 返回
const handleBack = () => {
  router.push('/class-management/my-classes')
}

// 推送题目
const handleAssignQuestion = () => {
  router.push(`/class-management/assign-question/${route.params.classid}`)
}

// 查看推送记录
const handleViewAssignments = async () => {
  showAssignmentsDialog.value = true
  await loadAssignments()
}

// 加载推送记录
const loadAssignments = async () => {
  const classid = route.params.classid
  if (!classid) return
  
  assignmentsLoading.value = true
  try {
    console.log('=== 加载班级推送记录 ===')
    console.log('班级 ID:', classid)
    
    const response = await getClassAssignmentsWithStats(classid)
    console.log('后端返回的推送记录:', response.data)
    
    if (response.data.success) {
      assignments.value = response.data.data || []
      console.log('加载成功，推送记录数:', assignments.value.length)
    } else {
      console.error('后端返回失败:', response.data.message)
      ElMessage.error(response.data.message || '加载推送记录失败')
    }
  } catch (error) {
    console.error('加载推送记录失败:', error)
    ElMessage.error(error.response?.data?.message || '加载推送记录失败')
  } finally {
    assignmentsLoading.value = false
  }
}

// 查看作答详情
const handleViewAnswerDetail = async (assignment) => {
  console.log('=== 查看作答详情 ===')
  console.log('当前推送信息:', assignment)
  console.log('题目类型:', assignment.questionType)
  console.log('题目类型类型:', typeof assignment.questionType)
  
  currentAssignment.value = assignment
  showAnswerDetailDialog.value = true
  await loadAnswerRecords(assignment.id)
}

// 加载作答记录
const loadAnswerRecords = async (assignmentId) => {
  answerDetailLoading.value = true
  try {
    console.log('=== 加载作答详情 ===')
    console.log('推送 ID:', assignmentId)
    
    const response = await getAssignmentAnswerDetails(assignmentId)
    console.log('后端返回的作答记录:', response.data)
    
    if (response.data.success) {
      answerRecords.value = response.data.data || []
      console.log('加载成功，作答记录数:', answerRecords.value.length)
      
      // 更新当前推送的题目类型信息
      if (response.data.questionType !== undefined) {
        console.log('后端返回的题目类型:', response.data.questionType)
        currentAssignment.value = {
          ...currentAssignment.value,
          questionType: response.data.questionType
        }
      }
    } else {
      console.error('后端返回失败:', response.data.message)
      ElMessage.error(response.data.message || '加载作答记录失败')
    }
  } catch (error) {
    console.error('加载作答记录失败:', error)
    ElMessage.error(error.response?.data?.message || '加载作答记录失败')
  } finally {
    answerDetailLoading.value = false
  }
}

// 加载题目信息
const loadQuestionInfo = async (questionId) => {
  try {
    // TODO: 调用后端 API 获取题目详情
    // const response = await getQuestionById(questionId)
    // currentQuestionInfo.value = response.data.data
    console.log('加载题目信息, questionId:', questionId)
  } catch (error) {
    console.error('加载题目信息失败:', error)
  }
}

// 移除成员
const handleRemoveMember = async (member) => {
  if (!canManageMembers.value) {
    ElMessage.warning('您没有权限执行此操作')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定要将 "${member.username}" 从班级中移除吗？`,
      '确认移除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    removingUserId.value = member.userid

    const response = await removeClassMember(route.params.classid, member.userid)

    if (response.data.success) {
      ElMessage.success(`已成功移除成员：${member.username}`)
      // 重新加载成员列表
      await loadMembers()
    } else {
      ElMessage.error(response.data.message || '移除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('移除成员失败:', error)
      ElMessage.error(error.response?.data?.message || '移除失败，请稍后重试')
    }
  } finally {
    removingUserId.value = null
  }
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

// 获取题目类型文本
const getQuestionTypeText = (type) => {
  if (type === null || type === undefined) return '未知'
  
  // 根据后端定义: 0=选择题, 1=主观题
  const typeMap = {
    '0': '选择题',
    '1': '主观题',
    'choice': '选择题',
    'subjective': '主观题'
  }
  
  const key = String(type).trim()
  return typeMap[key] || `类型${type}`
}

// 获取题目类型标签颜色
const getQuestionTypeColor = (type) => {
  if (type === null || type === undefined) return ''
  
  // 根据后端定义: 0=选择题, 1=主观题
  const colorMap = {
    '0': 'primary',
    '1': 'success',
    'choice': 'primary',
    'subjective': 'success'
  }
  
  const key = String(type).trim()
  return colorMap[key] || ''
}

// 获取进度条颜色
const getProgressColor = (percentage) => {
  if (percentage >= 80) return '#67c23a'
  if (percentage >= 60) return '#e6a23c'
  return '#f56c6c'
}

// 判断是否为选择题
const isChoiceQuestion = computed(() => {
  const questionType = currentAssignment.value?.questionType
  console.log('判断题型 - questionType:', questionType, '类型:', typeof questionType)
  
  if (questionType === null || questionType === undefined) {
    console.log('题型为空,返回 false')
    return false
  }
  
  // 支持数字类型: 0 表示选择题
  // 支持字符串类型: 'choice', '选择题', '0', 'Choice'
  const type = String(questionType).toLowerCase().trim()
  
  const isChoice = type === '0' || type === 'choice' || type === '选择题'
  
  console.log('转换后的类型:', type, '是否为选择题:', isChoice)
  return isChoice
})

// 统计选择题各选项的选择人数
const choiceOptionStats = computed(() => {
  if (!isChoiceQuestion.value) return {}
  
  const stats = { A: 0, B: 0, C: 0, D: 0 }
  answerRecords.value.forEach(record => {
    if (record.answer && record.status === 1) {
      const answer = record.answer.trim().toUpperCase()
      if (stats.hasOwnProperty(answer)) {
        stats[answer]++
      }
    }
  })
  
  return stats
})

// 获取选项标签颜色
const getOptionColor = (option) => {
  const colors = {
    A: 'primary',
    B: 'success',
    C: 'warning',
    D: 'danger'
  }
  return colors[option] || ''
}

// 计算选择题各选项的百分比
const getChoicePercentage = (option) => {
  if (!isChoiceQuestion.value) return 0
  
  const totalAnswered = answerRecords.value.filter(r => r.status === 1 && r.answer).length
  if (totalAnswered === 0) return 0
  
  const count = choiceOptionStats.value[option] || 0
  return Math.round((count / totalAnswered) * 100)
}

onMounted(() => {
  console.log('=== ClassDetail 组件已挂载 ===')
  console.log('班级 ID:', route.params.classid)
  loadClassInfo()
  loadMembers()
})
</script>

<style scoped>
.class-detail-container {
  padding: 20px;
}

.class-detail-card {
  max-width: 1400px;
  margin: 0 auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h2 {
  margin: 0 0 10px 0;
  color: #303133;
  font-size: 24px;
}

.class-code {
  margin: 0;
  color: #606266;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 5px;
}

.class-stats {
  margin-bottom: 20px;
  padding: 20px;
  background-color: #f5f7fa;
  border-radius: 8px;
}

.empty-state {
  padding: 40px 0;
  text-align: center;
}

:deep(.el-table) {
  font-size: 14px;
}

:deep(.el-table th) {
  background-color: #f5f7fa;
  color: #606266;
}

/* 选择题选项统计卡片样式 */
.option-stat-card {
  text-align: center;
  transition: all 0.3s;
}

.option-stat-card:hover {
  transform: translateY(-5px);
}

.option-stat-content {
  padding: 10px 0;
}

.option-label {
  margin-bottom: 15px;
}

.option-count {
  margin: 15px 0;
}

.count-number {
  font-size: 36px;
  font-weight: bold;
  color: #303133;
  display: block;
}

.count-unit {
  font-size: 14px;
  color: #909399;
  margin-top: 5px;
  display: block;
}

/* 主观题答案内容样式 */
.answer-content {
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.6;
  padding: 8px;
  background-color: #f5f7fa;
  border-radius: 4px;
  max-height: 200px;
  overflow-y: auto;
}
</style>
