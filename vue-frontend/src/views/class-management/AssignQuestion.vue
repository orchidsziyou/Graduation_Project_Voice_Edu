<template>
  <div class="assign-question-container">
    <el-card class="assign-question-card">
      <template #header>
        <div class="card-header">
          <div>
            <h2>推送题目到班级</h2>
            <p v-if="classInfo.classname" class="class-name">
              目标班级：{{ classInfo.classname }}（{{ classInfo.classcode }}）
            </p>
          </div>
          <el-button @click="handleBack">
            <el-icon><back /></el-icon>
            返回
          </el-button>
        </div>
      </template>

      <!-- 题目搜索区域 -->
      <div class="search-section">
        <el-form :inline="true" class="search-form">
          <el-form-item label="关键词">
            <el-input
              v-model="searchKeyword"
              placeholder="输入题目内容或题干进行搜索"
              clearable
              style="width: 300px"
              @keyup.enter="handleSearch"
            />
          </el-form-item>
          
          <el-form-item label="题型">
            <el-select v-model="questionType" placeholder="全部" clearable style="width: 150px">
              <el-option label="选择题" :value="0" />
              <el-option label="主观题" :value="1" />
            </el-select>
          </el-form-item>

          <el-form-item>
            <el-button type="primary" @click="handleSearch" :loading="searching">
              搜索
            </el-button>
            <el-button @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 题目列表 -->
      <div v-loading="loading" element-loading-text="正在加载题目...">
        <div v-if="questions.length === 0 && !loading" class="empty-state">
          <el-empty description="暂无题目，请先在题库中添加题目" />
        </div>

        <el-table
          v-else
          :data="questions"
          stripe
          style="width: 100%"
          highlight-current-row
          @current-change="handleSelectQuestion"
        >
          <el-table-column type="index" label="#" width="60" align="center" />
          
          <el-table-column label="题型" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="getQuestionTypeColor(row.question_type)" size="small">
                {{ getQuestionTypeText(row.question_type) }}
              </el-tag>
            </template>
          </el-table-column>
          
          <el-table-column label="题目内容" min-width="300" show-overflow-tooltip>
            <template #default="{ row }">
              <strong>{{ row.question_body || '无题干' }}</strong>
            </template>
          </el-table-column>
          
          <el-table-column label="选项" min-width="200" show-overflow-tooltip>
            <template #default="{ row }">
              <span v-if="row.question_answer" style="color: #606266;">{{ row.question_answer }}</span>
              <span v-else style="color: #909399;">-</span>
            </template>
          </el-table-column>
          
          <el-table-column label="答案" width="100" align="center">
            <template #default="{ row }">
              <span v-if="row.choosing_answer" style="color: #67c23a; font-weight: bold;">
                {{ row.choosing_answer }}
              </span>
              <span v-else style="color: #909399;">-</span>
            </template>
          </el-table-column>
          
          <el-table-column label="操作" width="120" fixed="right" align="center">
            <template #default="{ row }">
              <el-button
                type="success"
                size="small"
                @click="handleSelectAndAssign(row)"
              >
                选择并推送
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页 -->
        <div v-if="total > 0" class="pagination">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :total="total"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handlePageChange"
          />
        </div>
      </div>

      <!-- 推送配置对话框 -->
      <el-dialog
        v-model="showAssignDialog"
        title="推送题目配置"
        width="600px"
        :close-on-click-modal="false"
      >
        <el-form :model="assignForm" label-width="100px">
          <el-form-item label="题目信息">
            <el-descriptions :column="1" border>
              <el-descriptions-item label="题型">
                <el-tag :type="getQuestionTypeColor(selectedQuestion?.question_type)" size="small">
                  {{ getQuestionTypeText(selectedQuestion?.question_type) }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="题干">
                {{ selectedQuestion?.question_body || '无题干' }}
              </el-descriptions-item>
              <el-descriptions-item label="答案">
                <span v-if="selectedQuestion?.choosing_answer" style="color: #67c23a; font-weight: bold;">
                  {{ selectedQuestion.choosing_answer }}
                </span>
                <span v-else-if="selectedQuestion?.question_answer" style="color: #67c23a; font-weight: bold;">
                  {{ selectedQuestion.question_answer }}
                </span>
                <span v-else style="color: #909399;">-</span>
              </el-descriptions-item>
            </el-descriptions>
          </el-form-item>

          <el-form-item label="推送标题" required>
            <el-input
              v-model="assignForm.title"
              placeholder="请输入推送标题，例如：第一章练习题"
              maxlength="100"
              show-word-limit
            />
          </el-form-item>

          <el-form-item label="推送要求">
            <el-input
              v-model="assignForm.requirement"
              type="textarea"
              :rows="4"
              placeholder="请输入作答要求或说明（选填）"
              maxlength="500"
              show-word-limit
            />
          </el-form-item>

          <el-form-item label="截止时间">
            <el-date-picker
              v-model="assignForm.deadline"
              type="datetime"
              placeholder="选择截止时间（选填）"
              style="width: 100%"
              format="YYYY-MM-DD HH:mm:ss"
            />
          </el-form-item>

          <el-alert
            title="提示：推送后，班级所有学生将收到该题目并可以作答"
            type="info"
            :closable="false"
            show-icon
          />
        </el-form>

        <template #footer>
          <el-button @click="showAssignDialog = false">取消</el-button>
          <el-button type="primary" @click="handleSubmitAssignment" :loading="submitting">
            确认推送
          </el-button>
        </template>
      </el-dialog>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Back } from '@element-plus/icons-vue'
import axios from 'axios'
import { createAssignment } from '@/api/assignment'
import { getClassById } from '@/api/class'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const searching = ref(false)
const submitting = ref(false)

const questions = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const searchKeyword = ref('')
const questionType = ref(null)

const classInfo = ref({})
const showAssignDialog = ref(false)
const selectedQuestion = ref(null)
const assignForm = ref({
  title: '',
  requirement: '',
  deadline: null
})

// 获取题目类型文本
const getQuestionTypeText = (type) => {
  const types = {
    0: '选择题',
    1: '填空题',
    2: '判断题'
  }
  return types[type] || '未知'
}

// 获取题目类型颜色
const getQuestionTypeColor = (type) => {
  const colors = {
    0: 'primary',
    1: 'success',
    2: 'warning'
  }
  return colors[type] || 'info'
}

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

// 加载题目列表
const loadQuestions = async () => {
  loading.value = true

  try {
    const params = {
      page: currentPage.value - 1,
      size: pageSize.value
    }

    if (searchKeyword.value.trim()) {
      params.keyword = searchKeyword.value.trim()
    }

    if (questionType.value !== null && questionType.value !== undefined) {
      params.type = questionType.value
    }

    const response = await axios.get('/api/question-items/list', { params })

    if (response.data.success) {
      questions.value = response.data.data || []
      total.value = response.data.total || 0
      console.log('加载题目成功:', questions.value.length, '条')
    } else {
      ElMessage.error(response.data.message || '加载题目失败')
    }
  } catch (error) {
    console.error('加载题目失败:', error)
    ElMessage.error(error.response?.data?.message || '加载失败，请检查网络连接')
  } finally {
    loading.value = false
  }
}

// 搜索题目
const handleSearch = () => {
  currentPage.value = 1
  loadQuestions()
}

// 重置搜索
const handleReset = () => {
  searchKeyword.value = ''
  questionType.value = null
  currentPage.value = 1
  loadQuestions()
}

// 分页大小变化
const handleSizeChange = (val) => {
  pageSize.value = val
  currentPage.value = 1
  loadQuestions()
}

// 页码变化
const handlePageChange = (val) => {
  currentPage.value = val
  loadQuestions()
}

// 选择题目（表格行点击）
const handleSelectQuestion = (row) => {
  selectedQuestion.value = row
}

// 选择并推送
const handleSelectAndAssign = (question) => {
  selectedQuestion.value = question
  assignForm.value = {
    title: '',
    requirement: '',
    deadline: null
  }
  showAssignDialog.value = true
}

// 提交推送
const handleSubmitAssignment = async () => {
  if (!assignForm.value.title.trim()) {
    ElMessage.warning('请输入推送标题')
    return
  }

  if (!userStore.userId) {
    ElMessage.warning('请先登录')
    return
  }

  submitting.value = true

  try {
    const data = {
      questionId: selectedQuestion.value.id,
      classId: route.params.classid,
      teacherId: userStore.userId,
      title: assignForm.value.title.trim(),
      requirement: assignForm.value.requirement?.trim() || '',
      deadline: assignForm.value.deadline ? new Date(assignForm.value.deadline).toISOString() : null
    }

    console.log('推送数据:', data)

    const response = await createAssignment(data)

    if (response.data.success) {
      ElMessage.success(`推送成功！已发送给班级所有学生`)
      showAssignDialog.value = false
      // 可选：返回到班级详情页
      // router.push(`/class-management/class-detail/${route.params.classid}`)
    } else {
      ElMessage.error(response.data.message || '推送失败')
    }
  } catch (error) {
    console.error('推送失败:', error)
    ElMessage.error(error.response?.data?.message || '推送失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

// 返回
const handleBack = () => {
  router.push(`/class-management/class-detail/${route.params.classid}`)
}

onMounted(() => {
  console.log('=== AssignQuestion 组件已挂载 ===')
  console.log('班级 ID:', route.params.classid)
  loadClassInfo()
  loadQuestions()
})
</script>

<style scoped>
.assign-question-container {
  padding: 20px;
}

.assign-question-card {
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

.class-name {
  margin: 0;
  color: #606266;
  font-size: 14px;
}

.search-section {
  margin-bottom: 20px;
  padding: 20px;
  background-color: #f5f7fa;
  border-radius: 8px;
}

.search-form {
  display: flex;
  flex-wrap: wrap;
  gap: 15px;
}

.empty-state {
  padding: 40px 0;
  text-align: center;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

:deep(.el-table) {
  font-size: 14px;
}

:deep(.el-table th) {
  background-color: #f5f7fa;
  color: #606266;
}

:deep(.el-descriptions__label) {
  width: 100px;
}
</style>
