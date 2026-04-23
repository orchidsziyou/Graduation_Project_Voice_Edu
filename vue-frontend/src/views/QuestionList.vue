<template>
  <div class="question-list-container">
    <el-card class="list-card">
      <template #header>
        <div class="card-header">
          <span>题库浏览</span>
          <el-button @click="handleRefresh" :loading="loading">
            🔄 刷新
          </el-button>
        </div>
      </template>

      <!-- 搜索栏 -->
      <div class="search-section">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索题干内容..."
          clearable
          style="width: 400px"
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <el-icon><search /></el-icon>
          </template>
        </el-input>
        <el-button type="primary" @click="handleSearch" :loading="searching">
          搜索
        </el-button>
        <el-button @click="handleReset">
          🔄 重置
        </el-button>
      </div>

      <!-- 题目列表 -->
      <div v-loading="loading" class="question-list">
        <div v-if="questions.length === 0 && !loading" class="empty-state">
          <el-empty description="暂无题目数据" />
        </div>

        <el-table 
          v-else 
          :data="questions" 
          stripe 
          style="width: 100%"
          :default-sort="{prop: 'id', order: 'descending'}"
        >
          <el-table-column prop="id" label="ID" width="80" sortable />
          
          <el-table-column label="题型" width="100">
            <template #default="{ row }">
              <el-tag :type="row.question_type === 0 ? 'success' : 'warning'">
                {{ row.question_type === 0 ? '选择题' : '主观题' }}
              </el-tag>
            </template>
          </el-table-column>
          
          <el-table-column prop="question_body" label="题干" min-width="300" show-overflow-tooltip />
          
          <el-table-column v-if="showAnswerColumn" label="选项" width="200" show-overflow-tooltip>
            <template #default="{ row }">
              <div v-if="row.choosing_answer" class="options-text">
                {{ formatOptions(row.choosing_answer) }}
              </div>
              <span v-else>-</span>
            </template>
          </el-table-column>
          
          <el-table-column v-if="showAnswerColumn" label="答案" min-width="200" show-overflow-tooltip>
            <template #default="{ row }">
              <div v-if="row.question_answer" class="answer-text">
                {{ row.question_answer }}
              </div>
              <span v-else>-</span>
            </template>
          </el-table-column>
          
          <el-table-column label="创建者" width="100">
            <template #default="{ row }">
              <span v-if="row.userid">用户 {{ row.userid }}</span>
              <span v-else>-</span>
            </template>
          </el-table-column>
          
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button 
                type="primary" 
                size="small" 
                @click="handleViewDetail(row)"
              >
                查看详情
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页 -->
        <div v-if="questions.length > 0" class="pagination-section">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :page-sizes="[15, 30, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            :total="total"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </div>
      </div>
    </el-card>
    
    <!-- 题目详情弹窗 -->
    <el-dialog 
      v-model="detailVisible" 
      title="题目详情" 
      width="700px"
      :close-on-click-modal="false"
    >
      <div v-if="currentQuestion" class="question-detail">
        <!-- 基本信息 -->
        <el-descriptions :column="2" border>
          <el-descriptions-item label="题目 ID">
            {{ currentQuestion.id }}
          </el-descriptions-item>
          <el-descriptions-item label="题型">
            <el-tag :type="currentQuestion.question_type === 0 ? 'success' : 'warning'">
              {{ currentQuestion.question_type === 0 ? '选择题' : '主观题' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="创建者" v-if="currentQuestion.userid">
            用户 {{ currentQuestion.userid }}
          </el-descriptions-item>
        </el-descriptions>
        
        <!-- 题干 -->
        <el-divider content-position="left">题干内容</el-divider>
        <div class="detail-section">
          <p class="question-body">{{ currentQuestion.question_body }}</p>
        </div>
        
        <!-- 选项（仅选择题） -->
        <div v-if="currentQuestion.question_type === 0 && currentQuestion.choosing_answer">
          <el-divider content-position="left">选项列表</el-divider>
          <div class="detail-section options-detail">
            <div 
              v-for="(option, index) in parseOptions(currentQuestion.choosing_answer)" 
              :key="index" 
              class="option-item"
            >
              <span class="option-label">{{ String.fromCharCode(65 + index) }}.</span>
              <span class="option-content">{{ option }}</span>
            </div>
          </div>
        </div>
        
        <!-- 答案（仅老师/管理员可见） -->
        <div v-if="showAnswerColumn">
          <el-divider content-position="left">参考答案</el-divider>
          <div class="detail-section answer-detail">
            <p v-if="currentQuestion.question_answer">
              {{ currentQuestion.question_answer }}
            </p>
            <p v-else class="no-answer">暂无答案</p>
          </div>
        </div>
      </div>
      
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getQuestions } from '@/api/questionItems'

const userStore = useUserStore()

// 检查是否是管理员或老师（userrole >= 3）
const isTeacherOrAdmin = computed(() => {
  const role = userStore.userInfo?.role || 0
  return role >= 3
})

// 是否显示答案列
const showAnswerColumn = computed(() => {
  return isTeacherOrAdmin.value
})

// 数据相关
const questions = ref([])
const loading = ref(false)
const searching = ref(false)
const searchKeyword = ref('')
const currentPage = ref(1)
const pageSize = ref(15)
const total = ref(0)

// 详情弹窗相关
const detailVisible = ref(false)
const currentQuestion = ref(null)

// 加载题目列表
const loadQuestions = async () => {
  loading.value = true
  
  try {
    console.log('=== 加载题目列表 ===')
    console.log('页码:', currentPage.value)
    console.log('每页数量:', pageSize.value)
    console.log('搜索关键词:', searchKeyword.value)
    console.log('当前用户 ID:', userStore.userId)
    console.log('当前用户角色:', userStore.userInfo?.role)
    console.log('是否显示答案:', showAnswerColumn.value)
    
    const response = await getQuestions({
      page: currentPage.value - 1, // 后端从 0 开始
      size: pageSize.value,
      keyword: searchKeyword.value.trim() || null
    })
    
    console.log('API 响应:', response.data)
    
    if (response.data.success) {
      // 后端返回格式：{success: true, total: 10, data: [...], currentPage: 0, pageSize: 15}
      questions.value = response.data.data || []
      total.value = response.data.total || 0
      console.log('加载了', questions.value.length, '条题目')
      console.log('总记录数:', total.value)
    } else {
      ElMessage.error(response.data.message || '加载失败')
    }
  } catch (error) {
    console.error('加载题目失败:', error)
    ElMessage.error(error.response?.data?.message || '加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = async () => {
  console.log('执行搜索，关键词:', searchKeyword.value)
  currentPage.value = 1 // 重置到第一页
  await loadQuestions()
}

// 重置搜索
const handleReset = () => {
  searchKeyword.value = ''
  currentPage.value = 1
  pageSize.value = 15
  loadQuestions()
}

// 刷新
const handleRefresh = () => {
  loadQuestions()
}

// 分页大小变化
const handleSizeChange = () => {
  currentPage.value = 1
  loadQuestions()
}

// 页码变化
const handleCurrentChange = () => {
  loadQuestions()
}

// 格式化选项显示
const formatOptions = (choosingAnswer) => {
  if (!choosingAnswer) return '-'
  // 将 "选项 1 | 选项 2 | 选项 3 | 选项 4" 格式化为更易读的形式
  return choosingAnswer.split(' | ').map((opt, index) => {
    const label = String.fromCharCode(65 + index) // A, B, C, D...
    return `${label}. ${opt.trim()}`
  }).join(' ')
}

// 查看题目详情
const handleViewDetail = (row) => {
  currentQuestion.value = row
  detailVisible.value = true
  console.log('查看题目详情:', row)
}

// 解析选项（将 | 分隔的字符串转为数组）
const parseOptions = (choosingAnswer) => {
  if (!choosingAnswer) return []
  // 支持两种分隔符：| 或 |
  return choosingAnswer.split('|').map(opt => opt.trim())
}

onMounted(() => {
  loadQuestions()
})
</script>

<style scoped>
.question-list-container {
  padding: 20px;
  min-height: calc(100vh - 60px);
  background-color: #f0f2f5;
}

.list-card {
  max-width: 1400px;
  margin: 0 auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-section {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
  padding: 15px;
  background-color: #f5f7fa;
  border-radius: 8px;
}

.question-list {
  min-height: 400px;
}

.empty-state {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
}

.options-text {
  font-size: 13px;
  line-height: 1.6;
  color: #606266;
}

.answer-text {
  font-size: 13px;
  line-height: 1.6;
  color: #67c23a;
  font-weight: 500;
}

.pagination-section {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}
</style>
