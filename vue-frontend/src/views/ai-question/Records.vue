<template>
  <div class="records-container">
    <el-card class="records-card">
      <template #header>
        <div class="card-header">
          <span>AI 出题历史记录</span>
          <div style="display: flex; gap: 10px;">
            <el-tag v-if="userStore.isLoggedIn" type="success">
              👤 {{ userStore.username }}
            </el-tag>
            <el-button type="primary" @click="handleRefresh">
              <el-icon><refresh /></el-icon>
              刷新
            </el-button>

            <el-button type = "primary" @click ="returndashboard">
              回到主页
            </el-button>
          </div>
        </div>
      </template>
      
      <!-- 统计信息 -->
      <div class="statistics">
        <el-statistic title="总记录数" :value="totalCount" />
      </div>
      
      <!-- 记录列表 -->
      <div v-loading="loading" element-loading-text="正在加载记录...">
        <div v-if="records.length === 0 && !loading" class="empty-state">
          <el-empty description="暂无出题记录，快去生成第一道题吧！" />
        </div>
        
        <div v-else class="records-list">
          <el-collapse accordion v-model="activeNames">
            <el-collapse-item
              v-for="record in records"
              :key="record.id"
              :name="record.id"
            >
              <template #title>
                <div class="record-title">
                  <el-tag size="small" type="info">
                    📅 {{ formatDate(record.createAt) }}
                  </el-tag>
                  <span class="preview-text">
                    {{ getPreview(record.transcriptionContent) }}
                  </span>
                </div>
              </template>
              
              <div class="record-detail">
                  <!-- 转写内容 -->
                  <div class="detail-section">
                    <h4>转写内容</h4>
                    <el-input
                      type="textarea"
                      :rows="6"
                      :model-value="record.transcriptionContent"
                      readonly
                      class="content-textarea"
                    />
                  </div>
                  
                  <!-- 自定义备注 -->
                  <div v-if="record.customRemark" class="detail-section">
                    <h4>自定义要求</h4>
                    <el-tag type="warning">{{ record.customRemark }}</el-tag>
                  </div>
                  
                  <!-- AI 生成的题目 -->
                  <div class="detail-section">
                    <h4>AI 生成的题目</h4>
                    <el-input
                      type="textarea"
                      :rows="8"
                      :model-value="record.generatedQuestion"
                      readonly
                      class="question-textarea"
                    />
                    
                    <!-- 操作按钮 -->
                    <div class="question-actions">
                      <el-button
                        type="primary"
                        size="small"
                        @click="handleCopyQuestion(record.generatedQuestion)"
                      >
                        <el-icon><document-copy /></el-icon>
                        复制题目
                      </el-button>
                      
                      <el-button
                        type="success"
                        size="small"
                        @click="handleExtractStem(record.generatedQuestion, record.id)"
                        :loading="extractingId === record.id"
                      >
                        <el-icon><scissor /></el-icon>
                        提取题干
                      </el-button>
                    </div>
                  </div>
                  
                  <!-- 提取的题干结果 -->
                  <div v-if="extractedStems[record.id]" class="detail-section extracted-stem-section">
                    <el-divider content-position="left">
                      提取的题干
                    </el-divider>
                    
                    <div class="stem-card">
                      <div class="stem-title">
                        <el-icon><check /></el-icon>
                        题干内容
                      </div>
                      <div class="stem-body">
                        <p class="stem-text">{{ extractedStems[record.id].stem }}</p>
                        <div v-if="extractedStems[record.id].options && extractedStems[record.id].options.length > 0" class="options-list">
                          <div v-for="(option, index) in extractedStems[record.id].options" :key="index" class="option-item">
                            <span class="option-label">{{ String.fromCharCode(65 + index) }}.</span>
                            <span class="option-content">{{ option }}</span>
                          </div>
                        </div>
                      </div>
                      <div class="stem-footer">
                        <el-button type="primary" size="small" @click="copyExtractedStem(record.id)">
                          <el-icon><document-copy /></el-icon>
                          复制题干
                        </el-button>
                        <el-button 
                          type="success" 
                          size="small" 
                          @click="handleSaveToDatabase(record.id)"
                          :loading="savingId === record.id"
                        >
                          <el-icon><check /></el-icon>
                          保存到题库
                        </el-button>
                      </div>
                    </div>
                  </div>
                  
                  <!-- 操作按钮 -->
                  <div class="record-actions">
                    <el-button
                      type="danger"
                      size="small"
                      @click="handleDelete(record.id)"
                    >
                      <el-icon><delete /></el-icon>
                      删除记录
                    </el-button>
                  </div>
                </div>
              </el-collapse-item>
          </el-collapse>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, DocumentCopy, Delete, Scissor, Check } from '@element-plus/icons-vue'
import { getMyRecords, deleteRecord } from '@/api/aiQuestion'
import { useUserStore } from '@/stores/user'
import { useRouter } from 'vue-router'
import { saveQuestionItem } from '@/api/questionItems'

const userStore = useUserStore()
const loading = ref(false)
const records = ref([])
const totalCount = ref(0)
const router = useRouter()

// 存储提取的题干 { [recordId]: { stem, options } }
const extractedStems = ref({})
// 正在提取的记录 ID
const extractingId = ref(null)
// 正在保存的记录 ID
const savingId = ref(null)

// 加载记录
const loadRecords = async () => {
  loading.value = true
  
  try {
    // console.log('=== 开始加载 AI 出题记录 ===')
    const response = await getMyRecords()
    
    // console.log('后端返回的数据:', response.data)
    
    if (response.data.success) {
      records.value = response.data.records || []
      totalCount.value = response.data.totalCount || 0
      console.log('加载成功，记录数:', records.value.length)
      console.log('记录详情:', records.value)
      
      // 检查每条记录的数据结构
      records.value.forEach((record, index) => {
        console.log(`记录 ${index + 1}:`, {
          id: record.id,
          transcriptionContent: record.transcriptionContent ? '有内容' : '空',
          generatedQuestion: record.generatedQuestion ? '有内容' : '空',
          customRemark: record.customRemark
        })
      })
    } else {
      console.error('后端返回失败:', response.data.message)
      ElMessage.error(response.data.message || '加载记录失败')
    }
  } catch (error) {
    console.error('加载记录失败:', error)
    ElMessage.error(error.response?.data?.message || '加载失败，请检查网络连接')
  } finally {
    loading.value = false
  }
}

// 刷新
const handleRefresh = () => {
  loadRecords()
}

// 回到主页
const returndashboard = () => {
  router.push('/')
}

// 格式化日期
const formatDate = (dateString) => {
  if (!dateString) return ''
  const date = new Date(dateString)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 获取预览文本
const getPreview = (text) => {
  if (!text) return '无内容'
  const cleanText = text.replace(/\n/g, ' ')
  return cleanText.length > 50 ? cleanText.substring(0, 50) + '...' : cleanText
}

// 复制题目
const handleCopyQuestion = (question) => {
  navigator.clipboard.writeText(question)
  ElMessage.success('复制成功')
}

// 提取题干
const handleExtractStem = (generatedQuestion, recordId) => {
  // console.log('=== 开始提取题干 ===')
  // console.log('记录 ID:', recordId)
  // console.log('题目内容:', generatedQuestion)
  // console.log('题目内容类型:', typeof generatedQuestion)
  // console.log('题目内容是否为空:', !generatedQuestion)
  
  if (!generatedQuestion || !generatedQuestion.trim()) {
    console.warn('题目内容为空或只有空白字符')
    ElMessage.warning('没有可提取的题目内容')
    return
  }
  
  extractingId.value = recordId
  
  try {
    const lines = generatedQuestion.split('\n').filter(line => line.trim())
    // console.log('分割后的行数:', lines.length)
    // console.log('所有行内容:', lines)
    
    let stem = ''
    let options = []
    
    // 根据提示词的格式要求提取：
    // 第一行：大纲描述
    // 第二行：题干
    // 第三行：选项（选择题，4 个选项用空格隔开）
    // 第四行：答案及解析
    
    if (lines.length >= 2) {
      // 跳过第一行（大纲描述），从第二行开始提取题干
      stem = lines[1].trim()
      console.log('从第二行提取题干:', stem)
      
      // 如果是选择题，尝试从第三行提取选项
      if (lines.length >= 3) {
        const thirdLine = lines[2].trim()
        console.log('第三行内容:', thirdLine)
        
        // 检查是否是答案行（包含"答案"、"解析"等关键词）
        const isAnswerLine = thirdLine.includes('答案') || thirdLine.includes('解析')
        
        if (!isAnswerLine) {
          // 第三行不是答案行，可能是选项行
          // 尝试按空格分割选项
          const potentialOptions = thirdLine.split(/\s+/)
          console.log('分割的选项:', potentialOptions)
          
          // 验证是否是选项格式（A. B. C. D.或 A,B,C,D）
          const hasOptionPattern = potentialOptions.some(opt => 
            /^[A-D][.、]/.test(opt) || /^[A-D]$/.test(opt) || /^[A-D],/.test(opt)
          )
          
          if (hasOptionPattern && potentialOptions.length >= 4) {
            // 清理选项标记，只保留选项内容
            options = potentialOptions.map(opt => {
              // 移除 A. B. C. D.或 A, B,等标记
              return opt.replace(/^[A-D][.,、]\s*/, '').trim()
            }).filter(opt => opt.length > 0)
            console.log('提取到的选项:', options)
          }
        }
        
        // 如果第三行是答案行，尝试从后续行提取选项
        if (isAnswerLine || options.length === 0) {
          for (let i = 3; i < lines.length; i++) {
            const line = lines[i].trim()
            // 如果已经遇到答案行，后面的选项也要提取
            for (let j = i; j < lines.length; j++) {
              const optionLine = lines[j].trim()
              // 支持多种格式：A. B. C. D. 或 A, B, C, D,
              const optionMatch = optionLine.match(/^([A-D])\.\s*(.+)$/) || 
                                 optionLine.match(/^([A-D]),\s*(.+)$/) ||
                                 optionLine.match(/^([A-D]),(.+)$/)
              if (optionMatch) {
                options.push(optionMatch[2])
                console.log('从第', j+1, '行提取到选项:', optionMatch[2])
              } else if (optionLine.includes('答案') || optionLine.includes('解析')) {
                break // 遇到答案行停止
              }
            }
            break
          }
        }
      }
    }
    
    // 如果提取到了题干，保存结果
    if (stem) {
      extractedStems.value[recordId] = {
        stem: stem,
        options: options
      }
      ElMessage.success('题干提取成功！共提取 ' + options.length + ' 个选项')
      console.log('最终提取结果:', { stem, options })
    } else {
      ElMessage.warning('未能提取到题干内容')
    }
  } catch (error) {
    console.error('提取失败:', error)
    ElMessage.error('提取失败：' + error.message)
  } finally {
    extractingId.value = null
  }
}

// 复制提取的题干
const copyExtractedStem = (recordId) => {
  const stemData = extractedStems.value[recordId]
  if (stemData && stemData.stem) {
    let textToCopy = stemData.stem
    if (stemData.options && stemData.options.length > 0) {
      textToCopy += '\n\n'
      stemData.options.forEach((opt, idx) => {
        textToCopy += `${String.fromCharCode(65 + idx)}. ${opt}\n`
      })
    }
    navigator.clipboard.writeText(textToCopy)
    ElMessage.success('题干复制成功')
  }
}

// 保存到题库
const handleSaveToDatabase = async (recordId) => {
  const stemData = extractedStems.value[recordId]
  
  if (!stemData || !stemData.stem) {
    ElMessage.warning('请先提取题干')
    return
  }
  
  savingId.value = recordId
  
  try {
    console.log('=== 开始保存题目到题库 ===')
    console.log('记录 ID:', recordId)
    
    // 准备数据
    const questionBody = stemData.stem
    const questionType = stemData.options && stemData.options.length > 0 ? 0 : 1  // 有选项=选择题，否则=主观题
    
    // 提取选项（如果是选择题）
    let choosingAnswer = null
    if (questionType === 0 && stemData.options.length > 0) {
      choosingAnswer = stemData.options.join(' | ')  // 用分隔符连接选项
    }
    
    // 从原始题目中提取答案
    const record = records.value.find(r => r.id === recordId)
    let questionAnswer = '参考答案见原生成内容'
    if (record && record.generatedQuestion) {
      const lines = record.generatedQuestion.split('\n').map(line => line.trim())
      for (let i = lines.length - 1; i >= 0; i--) {
        const line = lines[i]
        if (line.includes('答案') || line.includes('解析')) {
          questionAnswer = line
          break
        }
      }
    }
    
    console.log('保存参数:', {
      questionBody,
      questionType,
      questionAnswer,
      choosingAnswer,
      userid: userStore.userId
    })
    
    // 调用 API 保存
    const response = await saveQuestionItem(questionBody, questionType, questionAnswer, choosingAnswer, userStore.userId)
    
    if (response.data.success) {
      console.log('题目保存成功，ID:', response.data.id)
      ElMessage.success('题目保存成功！已添加到题库')
    } else {
      console.error('保存失败:', response.data.message)
      ElMessage.error(response.data.message || '保存失败')
    }
    
  } catch (error) {
    console.error('保存题目失败:', error)
    ElMessage.error(error.response?.data?.message || '保存失败，请稍后重试')
  } finally {
    savingId.value = null
  }
}

// 删除记录
const handleDelete = (id) => {
  ElMessageBox.confirm(
    '确定要删除这条记录吗？此操作不可恢复。',
    '确认删除',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      const response = await deleteRecord(id)
      
      if (response.data.success) {
        ElMessage.success('删除成功')
        // 重新加载记录
        await loadRecords()
      } else {
        ElMessage.error(response.data.message || '删除失败')
      }
    } catch (error) {
      console.error('删除失败:', error)
      ElMessage.error(error.response?.data?.message || '删除失败')
    }
  }).catch(() => {
    // 用户取消
  })
}

// 组件挂载时加载数据
onMounted(() => {
  console.log('=== Records 组件已挂载 ===')
  console.log('当前用户登录状态:', userStore.isLoggedIn)
  console.log('当前用户名:', userStore.username)
  loadRecords()
})
</script>

<style scoped>
.records-container {
  padding: 20px;
}

.records-card {
  max-width: 1000px;
  margin: 0 auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: bold;
}

.statistics {
  margin-bottom: 20px;
  padding: 15px;
  background-color: #f5f7fa;
  border-radius: 8px;
}

.empty-state {
  padding: 40px 0;
}

.records-list {
  margin-top: 20px;
}

.record-title {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
}

.preview-text {
  color: #606266;
  font-size: 14px;
  flex: 1;
}

.record-detail {
  padding: 10px;
}

.detail-section {
  margin-bottom: 20px;
}

.detail-section h4 {
  margin: 0 0 10px 0;
  color: #303133;
  font-size: 14px;
}

.content-textarea {
  font-family: 'Courier New', Courier, monospace;
  font-size: 13px;
  line-height: 1.6;
}

.question-textarea {
  font-family: 'Courier New', Courier, monospace;
  font-size: 14px;
  line-height: 1.8;
}

.record-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  margin-top: 15px;
  padding-top: 15px;
  border-top: 1px solid #e4e7ed;
}

.question-actions {
  display: flex;
  gap: 10px;
  margin-top: 15px;
  justify-content: flex-start;
}

.extracted-stem-section {
  margin-top: 25px;
  padding: 20px;
  background: linear-gradient(135deg, #f0fff4 0%, #e6fffa 100%);
  border-radius: 8px;
  border: 2px solid #c6f6d5;
  animation: slideIn 0.3s ease-out;
}

.stem-card {
  background-color: white;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.stem-title {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 15px;
  background: linear-gradient(135deg, #67c23a 0%, #52c41a 100%);
  color: white;
  font-weight: bold;
  font-size: 15px;
}

.stem-body {
  padding: 20px;
}

.stem-text {
  margin: 0 0 20px 0;
  padding: 15px;
  background-color: #f5f7fa;
  border-left: 4px solid #67c23a;
  border-radius: 4px;
  font-size: 15px;
  line-height: 1.8;
  color: #303133;
}

.options-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.option-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px 15px;
  background-color: #f0fff4;
  border-radius: 6px;
  border: 1px solid #c6f6d5;
  transition: all 0.2s;
}

.option-item:hover {
  background-color: #e6fffa;
  border-color: #81e6d9;
  transform: translateX(5px);
}

.option-label {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 24px;
  height: 24px;
  padding: 0 8px;
  background: linear-gradient(135deg, #67c23a 0%, #52c41a 100%);
  color: white;
  border-radius: 4px;
  font-weight: bold;
  font-size: 13px;
  flex-shrink: 0;
}

.option-content {
  flex: 1;
  color: #606266;
  font-size: 14px;
  line-height: 1.6;
}

.stem-footer {
  display: flex;
  justify-content: flex-end;
  padding: 12px 15px;
  background-color: #fafafa;
  border-top: 1px solid #ebeef5;
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

:deep(.el-collapse-item__header) {
  font-weight: 500;
  background-color: #fafafa;
  padding: 15px 20px;
}

:deep(.el-collapse-item__content) {
  padding: 20px;
  display: block !important;
  visibility: visible !important;
  opacity: 1 !important;
}
</style>
