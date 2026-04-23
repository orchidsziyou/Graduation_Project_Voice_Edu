<template>
  <div class="vosk-test-container">
    <el-card class="test-card">
      <template #header>
        <div class="card-header">
          <span>Vosk 本地语音识别测试</span>
          <el-button type="text" @click="goBack">
            <el-icon><arrow-left /></el-icon>
            返回
          </el-button>
        </div>
      </template>

      <!-- 说明区域 -->
      <el-alert
        title="Vosk 离线语音识别"
        type="info"
        :closable="false"
        show-icon
        class="info-alert"
      >
        <template #default>
          <p>✅ 完全离线运行，无需网络连接</p>
          <p>✅ 支持中文识别</p>
          <p>✅ 自动将 WAV 转换为 16kHz 单声道格式</p>
          <p>⚠️ 首次使用需要下载中文模型 (~2GB)</p>
        </template>
      </el-alert>

      <!-- 上传区域 -->
      <div class="upload-section">
        <el-upload
          ref="uploadRef"
          drag
          :auto-upload="false"
          :on-change="handleFileChange"
          :limit="1"
          accept=".wav"
          class="upload-area"
        >
          <el-icon class="el-icon--upload"><upload-filled /></el-icon>
          <div class="el-upload__text">
            将音频文件拖到此处，或<em>点击上传</em>
          </div>
          <template #tip>
            <div class="el-upload__tip">
              ⚠️ 仅支持 WAV 格式<br>
              💡 推荐: 16kHz, 单声道, 16-bit<br>
            </div>
          </template>
        </el-upload>
      </div>

      <!-- 文件信息 -->
      <div v-if="selectedFile" class="file-info">
        <el-alert
          title="已选择文件"
          type="success"
          :closable="false"
          show-icon
        >
          <div class="file-detail">
            <p><strong>文件名:</strong> {{ selectedFile.name }}</p>
            <p><strong>文件大小:</strong> {{ formatFileSize(selectedFile.size) }}</p>
          </div>
        </el-alert>
      </div>

      <!-- 控制按钮 -->
      <div class="action-buttons">
        <el-button
          type="primary"
          size="large"
          :loading="recognizing"
          :disabled="!selectedFile"
          @click="handleRecognize"
        >
          {{ recognizing ? '识别中...' : '开始识别' }}
        </el-button>
        
        <el-button
          size="large"
          @click="handleReset"
        >
          重置
        </el-button>
      </div>

      <!-- 识别结果 -->
      <div v-if="recognitionResult" class="result-section">
        <el-divider content-position="left">
          识别结果
        </el-divider>
        
        <div class="result-actions">
          <el-button
            type="primary"
            size="small"
            @click="handleCopyResult"
          >
            <el-icon><document-copy /></el-icon>
            复制结果
          </el-button>
          
          <el-button
            type="success"
            size="small"
            :loading="saving"
            @click="handleSaveToRecords"
          >
            <el-icon><folder-add /></el-icon>
            保存到转写记录
          </el-button>
        </div>
        
        <el-input
          v-model="recognitionResult"
          type="textarea"
          :rows="10"
          readonly
          class="result-textarea"
        />
        
        <!-- 识别信息 -->
        <div class="recognition-info">
          <el-tag type="success">识别模式: 本地 Vosk</el-tag>
          <el-tag type="info" style="margin-left: 10px;">
            耗时: {{ recognitionTime }}ms
          </el-tag>
        </div>
      </div>

      <!-- 错误提示 -->
      <el-alert
        v-if="errorMessage"
        :title="errorMessage"
        type="error"
        :closable="true"
        show-icon
        @close="errorMessage = ''"
        class="error-alert"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { UploadFilled, ArrowLeft, DocumentCopy, FolderAdd } from '@element-plus/icons-vue'
import { transcribeLocal, saveLocalTranscriptionRecord } from '@/api/transcription'

const router = useRouter()
const uploadRef = ref(null)
const selectedFile = ref(null)
const recognizing = ref(false)
const recognitionResult = ref('')
const recognitionTime = ref(0)
const errorMessage = ref('')
const saving = ref(false)  // 保存状态

// 返回上一页
const goBack = () => {
  router.back()
}

// 处理文件选择
const handleFileChange = (file) => {
  selectedFile.value = file.raw
  errorMessage.value = ''
  recognitionResult.value = ''
}

// 格式化文件大小
const formatFileSize = (bytes) => {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(2) + ' MB'
}

// 开始识别
const handleRecognize = async () => {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择音频文件')
    return
  }

  recognizing.value = true
  errorMessage.value = ''
  recognitionResult.value = ''
  
  const startTime = Date.now()

  try {
    console.log('🎤 开始本地 Vosk 识别...')
    const response = await transcribeLocal(selectedFile.value)
    
    console.log('✅ 识别响应:', response.data)
    
    if (response.data.success) {
      recognitionResult.value = response.data.result || '(无内容)'
      recognitionTime.value = Date.now() - startTime
      
      ElMessage.success(`识别成功！耗时 ${recognitionTime.value}ms`)
    } else {
      errorMessage.value = response.data.message || '识别失败'
      ElMessage.error(errorMessage.value)
    }
  } catch (error) {
    console.error('❌ 识别失败:', error)
    errorMessage.value = error.response?.data?.message || '识别失败，请检查后端服务是否启动'
    ElMessage.error(errorMessage.value)
  } finally {
    recognizing.value = false
  }
}

// 重置
const handleReset = () => {
  if (uploadRef.value) {
    uploadRef.value.clearFiles()
  }
  selectedFile.value = null
  recognitionResult.value = ''
  errorMessage.value = ''
  recognitionTime.value = 0
}

// 复制结果
const handleCopyResult = () => {
  navigator.clipboard.writeText(recognitionResult.value)
  ElMessage.success('复制成功')
}

// 保存到转写记录
const handleSaveToRecords = async () => {
  console.log('🔵 点击保存按钮')
  
  if (!recognitionResult.value || recognitionResult.value === '(无内容)') {
    console.log('⚠️ 没有可保存的内容')
    ElMessage.warning('没有可保存的识别结果')
    return
  }
  
  console.log('💾 开始保存，内容长度:', recognitionResult.value.length)
  saving.value = true
  
  try {
    // 调用后端 API 保存转写记录
    const response = await saveLocalTranscriptionRecord({
      transcriptionText: recognitionResult.value,
      fileName: selectedFile.value?.name || 'vosk_test.wav',
      fileSize: selectedFile.value?.size || 0
    })
    
    console.log('✅ 保存响应:', response.data)
    
    if (response.data.success) {
      ElMessage.success('✅ 已保存到转写记录')
    } else {
      ElMessage.error(response.data.message || '保存失败')
    }
  } catch (error) {
    console.error('❌ 保存失败:', error)
    console.error('错误详情:', error.response?.data)
    ElMessage.error(error.response?.data?.message || '保存失败，请检查是否登录')
  } finally {
    saving.value = false
    console.log('🔴 保存操作完成')
  }
}
</script>

<style scoped>
.vosk-test-container {
  padding: 20px;
  min-height: calc(100vh - 60px);
  background-color: #f0f2f5;
}

.test-card {
  max-width: 900px;
  margin: 0 auto;
  border-radius: 12px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: bold;
  font-size: 18px;
}

.info-alert {
  margin-bottom: 20px;
}

.info-alert p {
  margin: 5px 0;
  font-size: 14px;
  line-height: 1.6;
}

.upload-section {
  margin: 20px 0;
}

.upload-area {
  margin: 20px 0;
}

.file-info {
  margin: 20px 0;
}

.file-detail p {
  margin: 5px 0;
  font-size: 14px;
}

.action-buttons {
  display: flex;
  gap: 15px;
  justify-content: center;
  margin: 30px 0;
}

.result-section {
  margin-top: 30px;
  padding: 20px;
  background-color: #f5f7fa;
  border-radius: 8px;
}

.result-actions {
  margin-bottom: 15px;
}

.result-textarea {
  font-family: 'Courier New', Courier, monospace;
  font-size: 14px;
  line-height: 1.6;
}

.recognition-info {
  margin-top: 15px;
  display: flex;
  align-items: center;
}

.error-alert {
  margin-top: 20px;
}

:deep(.el-upload-dragger) {
  padding: 40px 20px;
}

:deep(.el-upload__text) {
  font-size: 14px;
  color: #606266;
}

:deep(.el-upload__text em) {
  color: #409EFF;
  font-style: normal;
}
</style>
