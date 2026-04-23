<template>
  <div class="records-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>转写记录</span>
          <el-button type="primary" @click="handleRefresh">
            <el-icon><refresh /></el-icon>
            刷新
          </el-button>

          <el-button type="primary" @click="returndashboard">
            返回主页
          </el-button>
        </div>
      </template>
      
      <el-table
        v-loading="loading"
        :data="records"
        stripe
        style="width: 100%"
      >
        <el-table-column prop="fileName" label="文件名" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="转写时间" width="180" />
        <el-table-column prop="duration" label="音频时长" width="100" />
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button
              type="primary"
              size="small"
              @click="handleViewDetail(row)"
            >
              查看详情
            </el-button>
            <el-button
              v-if="row.orderId && (!row.transcriptionText || row.transcriptionText.trim() === '')"
              type="success"
              size="small"
              :loading="row.fetchingResult"
              @click="handleFetchResult(row)"
            >
              {{ row.fetchingResult ? '获取中...' : '获取结果' }}
            </el-button>
            <el-button
              type="danger"
              size="small"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 详情对话框 -->
      <el-dialog
        v-model="detailDialogVisible"
        title="转写详情"
        width="80%"
      >
        <div v-if="currentRecord">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="文件名">
              {{ currentRecord.fileName }}
            </el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="getStatusType(currentRecord.status)">
                {{ getStatusText(currentRecord.status) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="转写时间">
              {{ currentRecord.createdAt }}
            </el-descriptions-item>
            <el-descriptions-item label="音频时长">
              {{ currentRecord.duration }}
            </el-descriptions-item>
          </el-descriptions>
          
          <el-divider>转写内容</el-divider>
          
          <div v-if="!currentRecord.transcriptionText || currentRecord.transcriptionText.trim() === ''" style="text-align: center; padding: 20px;">
            <el-alert
              title="提示"
              type="info"
              :closable="false"
              show-icon
            >
              {{ currentRecord.orderId ? '当前记录暂无转写内容，请点击下方按钮获取' : '本地转写记录，内容为空' }}
            </el-alert>
            <div style="margin-top: 20px;">
              <el-button
                v-if="currentRecord.orderId"
                type="success"
                size="large"
                :loading="fetchingDetailResult"
                @click="handleFetchResultFromDetail"
              >
                {{ fetchingDetailResult ? '获取中...' : '🔄 获取转写结果' }}
              </el-button>
            </div>
          </div>
          
          <el-input
            v-else
            v-model="currentRecord.transcriptionText"
            type="textarea"
            :rows="15"
            readonly
          />
        </div>
        
        <template #footer>
          <el-button @click="detailDialogVisible = false">关闭</el-button>
          <el-button 
            v-if="currentRecord?.transcriptionText && currentRecord.transcriptionText.trim() !== ''" 
            type="success" 
            @click="handleGenerateQuestionFromRecord"
          >
             AI 出题
          </el-button>
          <el-button type="primary" @click="handleCopyContent">
            复制内容
          </el-button>
        </template>
      </el-dialog>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { getTranscriptionRecords, getTranscriptionResultByOrderId } from '@/api/transcription'
import { useUserStore } from '@/stores/user'
import { useRouter } from 'vue-router'

const userStore = useUserStore()
const loading = ref(false)
const records = ref([])
const detailDialogVisible = ref(false)
const currentRecord = ref(null)
const fetchingDetailResult = ref(false)
const router = useRouter()


const returndashboard = () => {
  router.push('/')
}

// 加载转写记录
const loadRecords = async () => {
  loading.value = true
  
  // console.log('开始加载转写记录...')
  // console.log('当前用户登录状态:', userStore.isLoggedIn)
  // console.log('当前用户名:', userStore.username)
  // console.log('Token:', localStorage.getItem('token'))
  
  try {
    const response = await getTranscriptionRecords()
    // console.log('API 响应:', response)
    
    if (response.data.success) {
      records.value = response.data.records || []
      console.log('加载成功，记录数:', records.value.length)
    } else {
      console.error('后端返回失败:', response.data.message)
      ElMessage.error(response.data.message || '加载记录失败')
    }
  } catch (error) {
    console.error('加载记录失败:', error)
    console.error('错误详情:', error.response?.data)
    ElMessage.error(error.response?.data?.message || '加载失败，请检查网络连接')
  } finally {
    loading.value = false
  }
}

// 刷新
const handleRefresh = () => {
  loadRecords()
}

// 获取转写结果
const handleFetchResult = async (row) => {
  if (!row.orderId) {
    ElMessage.warning('该记录没有订单号，无法获取转写结果')
    return
  }
  
  // 设置加载状态
  row.fetchingResult = true
  
  try {
    console.log('开始获取转写结果，订单 ID:', row.orderId)
    
    const response = await getTranscriptionResultByOrderId(row.orderId)
    
    console.log('获取转写结果响应:', response)
    
    if (response.data.success) {
      // 更新记录的 content
      row.content = response.data.result || ''
      row.status = 'completed'
      
      ElMessage.success('获取转写结果成功！')
      
      // 刷新列表显示
      loadRecords()
    } else {
      ElMessage.error(response.data.message || '获取转写结果失败')
    }
  } catch (error) {
    console.error('获取转写结果失败:', error)
    console.error('错误详情:', error.response?.data)
    
    const errorMsg = error.response?.data?.message || error.message || '获取失败，请检查网络连接'
    ElMessage.error(errorMsg)
  } finally {
    row.fetchingResult = false
  }
}

// 查看详情
const handleViewDetail = (row) => {
  currentRecord.value = { ...row }
  detailDialogVisible.value = true
}

// 删除记录
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除 "${row.fileName}" 吗？`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    // TODO: 实现删除 API
    ElMessage.info('删除功能开发中...')
  } catch (error) {
    // 用户取消删除
  }
}

// 从详情对话框获取结果
const handleFetchResultFromDetail = async () => {
  if (!currentRecord.value || !currentRecord.value.orderId) {
    ElMessage.warning('该记录没有订单号，无法获取转写结果')
    return
  }
  
  fetchingDetailResult.value = true
  
  try {
    console.log('开始获取转写结果，订单 ID:', currentRecord.value.orderId)
    
    const response = await getTranscriptionResultByOrderId(currentRecord.value.orderId)
    
    console.log('获取转写结果响应:', response)
    
    if (response.data.success) {
      // 更新当前记录的 transcriptionText
      currentRecord.value.transcriptionText = response.data.result || ''
      currentRecord.value.status = 'completed'
      
      ElMessage.success('获取转写结果成功！')
      
      // 刷新列表数据
      loadRecords()
    } else {
      ElMessage.error(response.data.message || '获取转写结果失败')
    }
  } catch (error) {
    console.error('获取转写结果失败:', error)
    console.error('错误详情:', error.response?.data)
    
    const errorMsg = error.response?.data?.message || error.message || '获取失败，请检查网络连接'
    ElMessage.error(errorMsg)
  } finally {
    fetchingDetailResult.value = false
  }
}

// 复制内容
const handleCopyContent = () => {
  if (currentRecord.value?.transcriptionText) {
    navigator.clipboard.writeText(currentRecord.value.transcriptionText)
    ElMessage.success('复制成功')
  }
}

// 跳转到 AI 出题页面并填充内容
const handleGenerateQuestionFromRecord = () => {
  console.log('=== 点击 AI 出题按钮 ===')
  console.log('当前记录:', currentRecord.value)
  console.log('记录内容:', currentRecord.value?.transcriptionText)
  
  if (!currentRecord.value?.transcriptionText) {
    ElMessage.warning('没有可出题的内容')
    return
  }
  
  // 将转写内容编码后通过 query 参数传递
  const encodedContent = encodeURIComponent(currentRecord.value.transcriptionText)
  console.log('编码后的内容:', encodedContent.substring(0, 50) + '...')
  console.log('跳转路由:', `/ai-question/generate?content=${encodedContent}`)
  
  router.push(`/ai-question/generate?content=${encodedContent}`)
}

// 获取状态类型
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

onMounted(() => {
  loadRecords()
})
</script>

<style scoped>
.records-container {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

:deep(.el-table) {
  font-size: 13px;
}

:deep(.el-table th) {
  background-color: #f5f7fa;
  color: #606266;
}
</style>
