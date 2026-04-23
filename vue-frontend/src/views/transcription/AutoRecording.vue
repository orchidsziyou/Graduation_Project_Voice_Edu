<template>
  <div class="recording-container">
    <el-card class="recording-card">
      <template #header>
        <div class="card-header">
          <span>自动录音</span>
          <el-button type="text" @click="goBack">
            <el-icon><arrow-left /></el-icon>
            返回
          </el-button>
        </div>
      </template>

      <!-- 录音状态展示区 -->
      <div class="recording-status-section">
        <div class="status-indicator" :class="{ 'is-recording': isRecording }">
          <div class="pulse-ring" v-if="isRecording"></div>
          <el-icon class="mic-icon" :size="80">
            <microphone />
          </el-icon>
        </div>
        
        <div class="status-text">
          <h2 v-if="isRecording">正在录音...</h2>
          <h2 v-else>准备就绪</h2>
          <p class="recording-time" v-if="isRecording">
            {{ formatTime(recordingDuration) }}
          </p>
        </div>
      </div>

      <!-- 音频可视化 -->
      <div class="audio-visualizer" v-if="isRecording">
        <div 
          v-for="(bar, index) in visualizerBars" 
          :key="index"
          class="visualizer-bar"
          :style="{ height: bar.height + '%' }"
        ></div>
      </div>

      <!-- 控制按钮区域 -->
      <div class="control-buttons">
        <el-button
          v-if="!isRecording"
          type="danger"
          size="large"
          :icon="Microphone"
          @click="startRecording"
          :loading="isStarting"
          class="start-btn"
        >
          开始录音
        </el-button>

        <el-button
          v-else
          type="primary"
          size="large"
          :icon="VideoPause"
          @click="stopRecording"
          :loading="isStopping"
          class="stop-btn"
        >
          停止录音
        </el-button>
      </div>

      <!-- 录音设置 -->
      <el-divider content-position="left">录音设置</el-divider>
      
      <div class="settings-section">
        <el-form :model="settings" label-width="120px">
          <el-form-item label="分段时长">
            <el-select v-model="settings.segmentDuration" placeholder="选择分段时长">
              <el-option label="5 分钟" :value="5 * 60 * 1000" />
              <el-option label="10 分钟" :value="10 * 60 * 1000" />
              <el-option label="15 分钟" :value="15 * 60 * 1000" />
              <el-option label="20 分钟" :value="20 * 60 * 1000" />
              <el-option label="30 分钟" :value="30 * 60 * 1000" />
            </el-select>
            <div class="form-tip">录音将按此时间间隔自动分段并上传</div>
          </el-form-item>

          <el-form-item label="音频格式">
            <el-radio-group v-model="settings.audioFormat">
              <el-radio label="wav">WAV (无损)</el-radio>
              <el-radio label="webm">WebM (压缩)</el-radio>
            </el-radio-group>
            <div class="form-tip">WebM 格式文件更小，WAV 格式音质更好</div>
          </el-form-item>

          <el-form-item label="自动上传">
            <el-switch v-model="settings.autoUpload" />
            <div class="form-tip">开启后，每个片段录制完成会自动上传到服务器</div>
          </el-form-item>

          <el-form-item label="转写方式">
            <el-radio-group v-model="settings.transcriptionMode">
              <el-radio label="local">本地转写 (Vosk)</el-radio>
              <el-radio label="cloud">云端转写 (讯飞)</el-radio>
            </el-radio-group>
            <div class="form-tip">
              本地转写：使用 Vosk 离线引擎，无需网络，保护隐私<br>
              云端转写：使用讯飞在线 API，识别准确率更高
            </div>
          </el-form-item>
        </el-form>
      </div>

      <!-- 录音历史列表 -->
      <el-divider content-position="left">本次录音片段</el-divider>
      
      <div class="segments-list">
        <el-empty v-if="recordedSegments.length === 0" description="暂无录音片段" />
        
        <el-table v-else :data="recordedSegments" stripe style="width: 100%">
          <el-table-column prop="index" label="序号" width="80" />
          <el-table-column prop="duration" label="时长" width="120">
            <template #default="{ row }">
              {{ formatTime(row.duration) }}
            </template>
          </el-table-column>
          <el-table-column prop="size" label="文件大小" width="120">
            <template #default="{ row }">
              {{ formatFileSize(row.size) }}
            </template>
          </el-table-column>
          <el-table-column prop="uploadStatus" label="上传状态" width="120">
            <template #default="{ row }">
              <el-tag :type="getUploadStatusType(row.uploadStatus)">
                {{ getUploadStatusText(row.uploadStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作">
            <template #default="{ row }">
              <el-button
                type="primary"
                size="small"
                link
                @click="playSegment(row)"
              >
                播放
              </el-button>
              <el-button
                type="warning"
                size="small"
                link
                @click="uploadSegment(row)"
                :disabled="row.uploadStatus === 'uploaded'"
              >
                上传
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 进度提示 -->
      <el-alert
        v-if="isRecording"
        title="录音进行中"
        type="info"
        :closable="false"
        show-icon
        class="progress-alert"
      >
        <template #default>
          <div class="alert-content">
            <p>已录制 {{ recordedSegments.length }} 个片段</p>
            <p v-if="nextSegmentTime > 0">
              距离下次自动分段: {{ formatTime(nextSegmentTime) }}
            </p>
            <p class="tip-text">提示：您可以随时点击“停止录音”按钮结束录音</p>
          </div>
        </template>
      </el-alert>
      
      <!-- 使用说明 -->
      <el-alert
        v-if="!isRecording && recordedSegments.length === 0"
        title="使用说明"
        type="success"
        :closable="false"
        show-icon
        class="instruction-alert"
      >
        <template #default>
          <div class="instruction-content">
            <p>1️⃣ 点击“开始录音”按钮，允许浏览器访问麦克风</p>
            <p>2️⃣ 录音会按照设置的时长自动分段（默认 10 分钟）</p>
            <p>3️⃣ 每个片段会自动上传到服务器进行语音转写</p>
            <p>4️⃣ 您可以随时停止录音，所有已录制的片段都会保存</p>
            <p>5️⃣ 支持手动播放和重新上传录音片段</p>
          </div>
        </template>
      </el-alert>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Microphone, VideoPause, ArrowLeft } from '@element-plus/icons-vue'
import { uploadAudio, transcribeLocal } from '@/api/transcription'

const router = useRouter()

// 录音状态
const isRecording = ref(false)
const isStarting = ref(false)
const isStopping = ref(false)
const recordingDuration = ref(0)
let recordingTimer = null

// 录音设置
const settings = ref({
  segmentDuration: 10 * 60 * 1000, // 默认 10 分钟
  audioFormat: 'wav',
  autoUpload: true,
  transcriptionMode: 'local' // 默认使用本地转写
})

// 录音片段列表
const recordedSegments = ref([])

// 音频可视化条
const visualizerBars = ref(Array.from({ length: 20 }, () => ({ height: 10 })))

// 下次分段时间
const nextSegmentTime = ref(0)

// MediaRecorder 相关
let mediaRecorder = null
let audioChunks = []
let currentSegmentStartTime = 0
let audioContext = null
let analyser = null
let dataArray = null
let animationFrameId = null

// 返回上一页
const goBack = () => {
  if (isRecording.value) {
    ElMessage.warning('请先停止录音')
    return
  }
  router.back()
}

// 开始录音
const startRecording = async () => {
  isStarting.value = true
  
  try {
    // 请求麦克风权限
    const stream = await navigator.mediaDevices.getUserMedia({ 
      audio: {
        echoCancellation: true,
        noiseSuppression: true,
        sampleRate: 44100
      } 
    })
    
    console.log('获取麦克风流成功')
    
    // 创建 MediaRecorder
    const mimeType = getSupportedMimeType()
    mediaRecorder = new MediaRecorder(stream, { mimeType })
    
    console.log('使用音频格式:', mimeType)
    
    // 初始化音频上下文用于可视化
    initAudioVisualizer(stream)
    
    // 存储音频块
    audioChunks = []
    currentSegmentStartTime = Date.now()
    
    mediaRecorder.ondataavailable = (event) => {
      if (event.data.size > 0) {
        audioChunks.push(event.data)
      }
    }
    
    mediaRecorder.onstop = async () => {
      console.log('录音停止，处理音频数据...')
      await handleSegmentComplete()
      
      // 停止所有音轨
      stream.getTracks().forEach(track => track.stop())
      console.log('音轨已停止')
    }
    
    // 开始录音
    mediaRecorder.start(1000) // 每秒触发一次 dataavailable
    console.log('录音已开始')
    
    isRecording.value = true
    recordingDuration.value = 0
    recordedSegments.value = []
    nextSegmentTime.value = settings.value.segmentDuration
    
    // 启动计时器
    recordingTimer = setInterval(() => {
      recordingDuration.value += 1000
      
      // 更新下次分段时间
      const elapsed = recordingDuration.value % settings.value.segmentDuration
      nextSegmentTime.value = settings.value.segmentDuration - elapsed
      
      // 检查是否需要分段
      if (elapsed === 0 && recordingDuration.value > 0) {
        console.log('达到分段时长，自动分段...')
        handleAutoSegment()
      }
    }, 1000)
    
    ElMessage.success('录音已开始')
  } catch (error) {
    console.error('启动录音失败:', error)
    if (error.name === 'NotAllowedError') {
      ElMessage.error('麦克风权限被拒绝，请允许访问麦克风')
    } else if (error.name === 'NotFoundError') {
      ElMessage.error('未找到麦克风设备')
    } else {
      ElMessage.error('启动录音失败: ' + error.message)
    }
  } finally {
    isStarting.value = false
  }
}

// 停止录音
const stopRecording = async () => {
  isStopping.value = true
  
  try {
    console.log('停止录音...')
    
    // 清除计时器
    if (recordingTimer) {
      clearInterval(recordingTimer)
      recordingTimer = null
    }
    
    // 停止音频可视化
    stopAudioVisualizer()
    
    // 停止 MediaRecorder
    if (mediaRecorder && mediaRecorder.state !== 'inactive') {
      mediaRecorder.stop()
      console.log('MediaRecorder 已停止')
    }
    
    isRecording.value = false
    
    ElMessage.success(`录音已停止，共录制 ${recordedSegments.value.length} 个片段`)
  } catch (error) {
    console.error('停止录音失败:', error)
    ElMessage.error('停止录音失败: ' + error.message)
  } finally {
    isStopping.value = false
  }
}

// 自动分段处理
const handleAutoSegment = () => {
  if (mediaRecorder && mediaRecorder.state === 'recording') {
    console.log('触发自动分段...')
    // 请求新的数据块，这会触发 ondataavailable
    mediaRecorder.requestData()
    // 处理当前片段
    handleSegmentComplete()
    // 重置音频块数组，开始新片段
    audioChunks = []
    currentSegmentStartTime = Date.now()
  }
}

// 处理片段完成
const handleSegmentComplete = async () => {
  if (audioChunks.length === 0) {
    console.warn('没有音频数据')
    return
  }
  
  console.log('处理音频片段，数据块数量:', audioChunks.length)
  
  // 创建 Blob
  const mimeType = getSupportedMimeType()
  const blob = new Blob(audioChunks, { type: mimeType })
  const duration = Date.now() - currentSegmentStartTime
  
  console.log('片段大小:', blob.size, 'bytes')
  console.log('片段时长:', duration, 'ms')
  
  const segment = {
    index: recordedSegments.value.length + 1,
    duration: duration,
    size: blob.size,
    blob: blob,
    uploadStatus: 'pending'
  }
  
  recordedSegments.value.push(segment)
  console.log('片段', segment.index, '已添加到列表')
  
  // 如果开启自动上传，则上传该片段
  if (settings.value.autoUpload) {
    await uploadSegmentAuto(segment)
  }
}

// 自动上传片段
const uploadSegmentAuto = async (segment) => {
  try {
    segment.uploadStatus = 'uploading'
    console.log('自动上传片段:', segment.index, '转写方式:', settings.value.transcriptionMode)
    
    // 将 Blob 转换为 File
    const fileExtension = settings.value.audioFormat === 'mp3' ? 'mp3' : 'wav'
    const fileName = `recording_segment_${segment.index}_${Date.now()}.${fileExtension}`
    const file = new File([segment.blob], fileName, { 
      type: segment.blob.type,
      lastModified: Date.now()
    })
    
    console.log('文件名:', fileName)
    console.log('文件大小:', file.size, 'bytes')
    
    let response
    // 根据转写方式调用不同的 API
    if (settings.value.transcriptionMode === 'local') {
      // 本地转写 (Vosk)
      console.log('使用本地转写 (Vosk)')
      response = await transcribeLocal(file)
    } else {
      // 云端转写 (讯飞)
      console.log('使用云端转写 (讯飞)')
      response = await uploadAudio(file, segment.duration)
    }
    
    if (response.data && response.data.success) {
      segment.uploadStatus = 'uploaded'
      console.log('片段', segment.index, '上传成功')
      ElMessage.success(`片段 ${segment.index} 上传成功`)
    } else {
      segment.uploadStatus = 'failed'
      console.error('上传失败:', response.data?.message)
      ElMessage.error(`片段 ${segment.index} 上传失败: ${response.data?.message}`)
    }
  } catch (error) {
    console.error('上传失败:', error)
    segment.uploadStatus = 'failed'
    ElMessage.error(`片段 ${segment.index} 上传失败: ${error.message}`)
  }
}

// 手动上传片段
const uploadSegment = async (segment) => {
  try {
    segment.uploadStatus = 'uploading'
    // console.log('手动上传片段:', segment.index)
    
    // 将 Blob 转换为 File
    const fileExtension = settings.value.audioFormat === 'mp3' ? 'mp3' : 'wav'
    const fileName = `recording_segment_${segment.index}_${Date.now()}.${fileExtension}`
    const file = new File([segment.blob], fileName, { 
      type: segment.blob.type,
      lastModified: Date.now()
    })
    
    // 调用上传 API
    const response = await uploadAudio(file, segment.duration)
    
    if (response.data && response.data.success) {
      segment.uploadStatus = 'uploaded'
      // console.log('上传成功')
      ElMessage.success('上传成功')
    } else {
      segment.uploadStatus = 'failed'
      console.error('上传失败:', response.data?.message)
      ElMessage.error('上传失败: ' + response.data?.message)
    }
  } catch (error) {
    console.error('上传失败:', error)
    segment.uploadStatus = 'failed'
    ElMessage.error('上传失败: ' + error.message)
  }
}

// 播放片段
const playSegment = (segment) => {
  if (!segment.blob) {
    ElMessage.warning('片段数据不存在')
    return
  }
  
  try {
    // 创建音频 URL
    const audioUrl = URL.createObjectURL(segment.blob)
    const audio = new Audio(audioUrl)
    
    audio.play().then(() => {
      // console.log('开始播放片段', segment.index)
      ElMessage.info(`正在播放片段 ${segment.index}`)
    }).catch(error => {
      console.error('播放失败:', error)
      ElMessage.error('播放失败: ' + error.message)
    })
    
    // 播放结束后释放 URL
    audio.onended = () => {
      URL.revokeObjectURL(audioUrl)
      console.log('⏹️ 播放结束')
    }
  } catch (error) {
    console.error('播放出错:', error)
    ElMessage.error('播放出错: ' + error.message)
  }
}

// 获取支持的 MIME 类型
const getSupportedMimeType = () => {
  const types = [
    'audio/webm;codecs=opus',
    'audio/webm',
    'audio/mp4',
    'audio/wav'
  ]
  
  for (const type of types) {
    if (MediaRecorder.isTypeSupported(type)) {
      console.log('支持的音频格式:', type)
      return type
    }
  }
  
  console.warn('使用默认格式')
  return ''
}

// 初始化音频可视化
const initAudioVisualizer = (stream) => {
  try {
    audioContext = new (window.AudioContext || window.webkitAudioContext)()
    const source = audioContext.createMediaStreamSource(stream)
    analyser = audioContext.createAnalyser()
    analyser.fftSize = 64
    
    source.connect(analyser)
    
    dataArray = new Uint8Array(analyser.frequencyBinCount)
    
    // 开始可视化动画
    updateVisualizer()
  } catch (error) {
    console.error('初始化音频可视化失败:', error)
  }
}

// 停止音频可视化
const stopAudioVisualizer = () => {
  if (animationFrameId) {
    cancelAnimationFrame(animationFrameId)
    animationFrameId = null
  }
  
  if (audioContext) {
    audioContext.close()
    audioContext = null
  }
}

// 更新音频可视化
const updateVisualizer = () => {
  if (!analyser || !dataArray) {
    // 如果没有分析器，使用模拟数据
    visualizerBars.value = visualizerBars.value.map(() => ({
      height: Math.random() * 80 + 20
    }))
    
    if (isRecording.value) {
      animationFrameId = requestAnimationFrame(updateVisualizer)
    }
    return
  }
  
  // 获取真实的音频数据
  analyser.getByteFrequencyData(dataArray)
  
  // 更新可视化条
  visualizerBars.value = visualizerBars.value.map((_, index) => {
    const value = dataArray[index] || 0
    return { height: (value / 255) * 100 }
  })
  
  if (isRecording.value) {
    animationFrameId = requestAnimationFrame(updateVisualizer)
  }
}

// 格式化时间
const formatTime = (ms) => {
  if (!ms) return '00:00'
  const totalSeconds = Math.floor(ms / 1000)
  const hours = Math.floor(totalSeconds / 3600)
  const minutes = Math.floor((totalSeconds % 3600) / 60)
  const seconds = totalSeconds % 60
  
  if (hours > 0) {
    return `${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`
  }
  return `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`
}

// 格式化文件大小
const formatFileSize = (bytes) => {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(2) + ' MB'
}

// 获取上传状态类型
const getUploadStatusType = (status) => {
  const types = {
    pending: 'info',
    uploading: 'warning',
    uploaded: 'success',
    failed: 'danger'
  }
  return types[status] || 'info'
}

// 获取上传状态文本
const getUploadStatusText = (status) => {
  const texts = {
    pending: '待上传',
    uploading: '上传中',
    uploaded: '已上传',
    failed: '失败'
  }
  return texts[status] || status
}

// 组件卸载时清理
onUnmounted(() => {
  if (recordingTimer) {
    clearInterval(recordingTimer)
  }
})
</script>

<style scoped>
.recording-container {
  padding: 20px;
  min-height: calc(100vh - 60px);
  background-color: #f0f2f5;
}

.recording-card {
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

/* 录音状态展示区 */
.recording-status-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 12px;
  margin-bottom: 30px;
  color: white;
}

.status-indicator {
  position: relative;
  width: 120px;
  height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;
}

.pulse-ring {
  position: absolute;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.3);
  animation: pulse 2s ease-out infinite;
}

@keyframes pulse {
  0% {
    transform: scale(1);
    opacity: 1;
  }
  100% {
    transform: scale(1.5);
    opacity: 0;
  }
}

.mic-icon {
  z-index: 1;
  filter: drop-shadow(0 4px 8px rgba(0, 0, 0, 0.2));
}

.status-text {
  text-align: center;
}

.status-text h2 {
  margin: 0 0 10px 0;
  font-size: 28px;
  font-weight: bold;
}

.recording-time {
  margin: 0;
  font-size: 36px;
  font-weight: bold;
  font-family: 'Courier New', monospace;
  letter-spacing: 2px;
}

/* 音频可视化 */
.audio-visualizer {
  display: flex;
  align-items: flex-end;
  justify-content: center;
  gap: 4px;
  height: 80px;
  margin: 30px 0;
  padding: 0 20px;
}

.visualizer-bar {
  width: 8px;
  background: linear-gradient(to top, #667eea, #764ba2);
  border-radius: 4px;
  transition: height 0.1s ease;
  min-height: 10%;
}

/* 控制按钮 */
.control-buttons {
  display: flex;
  justify-content: center;
  margin: 30px 0;
}

.start-btn,
.stop-btn {
  padding: 20px 60px;
  font-size: 18px;
  border-radius: 50px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  transition: all 0.3s;
}

.start-btn:hover,
.stop-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.2);
}

/* 设置区域 */
.settings-section {
  padding: 20px;
  background-color: #f9fafb;
  border-radius: 8px;
  margin-bottom: 20px;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
  line-height: 1.5;
}

/* 片段列表 */
.segments-list {
  margin-top: 20px;
  min-height: 200px;
}

/* 进度提示 */
.progress-alert {
  margin-top: 20px;
}

.alert-content p {
  margin: 5px 0;
  font-size: 14px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .recording-container {
    padding: 10px;
  }
  
  .recording-status-section {
    padding: 30px 15px;
  }
  
  .status-indicator {
    width: 100px;
    height: 100px;
  }
  
  .status-text h2 {
    font-size: 24px;
  }
  
  .recording-time {
    font-size: 28px;
  }
  
  .start-btn,
  .stop-btn {
    padding: 15px 40px;
    font-size: 16px;
  }
}
</style>
