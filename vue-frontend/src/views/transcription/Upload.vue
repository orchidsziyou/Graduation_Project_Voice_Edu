<template>
  <div class="upload-container">
    <el-card class="upload-card">
      <template #header>
        <div class="card-header">
          <span>上传音频文件</span>
        </div>
      </template>
      
      <el-upload
        ref="uploadRef"
        drag
        :auto-upload="false"
        :on-change="handleFileChange"
        :limit="1"
        accept=".mp3,.wav,.m4a,.flac"
        class="upload-area"
      >
        <el-icon class="el-icon--upload"><upload-filled /></el-icon>
        <div class="el-upload__text">
          将音频文件拖到此处，或<em>点击上传</em>
        </div>
        <template #tip>
          <div class="el-upload__tip">
            只支持 mp3/wav/m4a/flac 格式的文件
          </div>
        </template>
      </el-upload>
      
      <div v-if="selectedFile" class="file-info">
        <el-alert
          title="已选择文件"
          type="info"
          :closable="false"
          show-icon
        >
          <div class="file-detail">
            <p><strong>文件名:</strong> {{ selectedFile.name }}</p>
            <p><strong>文件大小:</strong> {{ formatFileSize(selectedFile.size) }}</p>
          </div>
        </el-alert>
        
        <!-- WAV 文件转写方式选择 -->
        <div v-if="isWavFile" class="transcription-mode-selector">
          <el-alert
            title="检测到 WAV 格式，可选择转写方式"
            type="success"
            :closable="false"
            show-icon
            style="margin-bottom: 15px;"
          >
            <template #default>
              <p style="margin: 5px 0;"><strong>本地转写 (Vosk)</strong>: 完全离线，速度快，无需网络</p>
              <p style="margin: 5px 0;"><strong>云端转写 (讯飞)</strong>: 识别精度高，需要网络</p>
            </template>
          </el-alert>
          
          <el-radio-group v-model="transcriptionMode" size="large" style="width: 100%;">
            <el-radio-button value="local">本地转写 (推荐)</el-radio-button>
            <el-radio-button value="cloud">云端转写</el-radio-button>
          </el-radio-group>
        </div>
      </div>
      
      <div class="upload-actions">
        <el-button
          type="primary"
          size="large"
          :loading="uploading"
          :disabled="!selectedFile"
          @click="handleUpload"
        >
          {{ uploading ? '转写中...' : '开始转写' }}
        </el-button>
        
        <el-button
          size="large"
          @click="handleReset"
        >
          重置
        </el-button>

        <el-button size="large" @click="returndashboard">
          返回主页
        </el-button>
      </div>
      
      <!-- 转写结果展示区域 -->
      <div v-if="transcriptionResult" class="result-section">
        <el-divider content-position="left">
          转写结果
        </el-divider>
        
        <div class="result-actions">
          <el-button
            type="primary"
            size="small"
            @click="handleCopyResult"
          >
            <el-icon><document-copy /></el-icon>
            一键复制内容
          </el-button>
          
          <el-button
            type="success"
            size="small"
            :loading="generatingQuestion"
            @click="handleGenerateQuestion"
          >
            <el-icon><magic-stick /></el-icon>
            {{ generatingQuestion ? 'AI 出题中...' : 'AI 智能出题' }}
          </el-button>
        </div>
        
        <!-- 题型选择 -->
        <div class="question-type-selector">
          <span class="selector-label">题目类型：</span>
          <el-radio-group v-model="questionType" size="small">
            <el-radio value="choice">选择题</el-radio>
            <el-radio value="short-answer">主观题</el-radio>
          </el-radio-group>
        </div>
        
        <el-input
          v-model="transcriptionResult"
          type="textarea"
          :rows="15"
          readonly
          class="result-textarea"
        />
        
        <!-- 自定义备注/提示词 -->
        <div class="remark-section">
          <el-divider content-position="left">
            自定义要求（可选）
          </el-divider>
          
          <el-input
            v-model="customRemark"
            type="textarea"
            :rows="3"
            placeholder="请输入您的自定义要求，例如：'请生成适合小学生的题目'、'重点考察听力理解能力'等。留空则使用默认提示词。"
            class="remark-textarea"
          />
        </div>
        
        <!-- AI 出题结果 -->
        <div v-if="generatedQuestion" class="question-section">
          <el-divider content-position="left">
            AI 生成的题目
          </el-divider>
          
          <div class="question-content">
            <el-input
              v-model="generatedQuestion"
              type="textarea"
              :rows="10"
              class="question-textarea"
            />
            
            <div class="question-actions">
              <el-button
                type="primary"
                size="small"
                @click="handleExtractStem"
                :disabled="!generatedQuestion"
              >
                <el-icon><scissor /></el-icon>
                提取题干
              </el-button>
              <el-button
                type="primary"
                size="small"
                @click="handleCopyQuestion"
              >
                <el-icon><document-copy /></el-icon>
                复制题目
              </el-button>
            </div>
          </div>
        </div>
        
        <!-- 提取的题干结果 -->
        <div v-if="extractedStem" class="extracted-stem-section">
          <el-divider content-position="left">
            提取的题干
          </el-divider>
          
          <div class="stem-card">
            <div class="stem-title">
              <el-icon><check /></el-icon>
              题干内容
            </div>
            <div class="stem-body">
              <p class="stem-text">{{ extractedStem }}</p>
              <div v-if="extractedOptions && extractedOptions.length > 0" class="options-list">
                <div v-for="(option, index) in extractedOptions" :key="index" class="option-item">
                  <span class="option-label">{{ String.fromCharCode(65 + index) }}.</span>
                  <span class="option-content">{{ option }}</span>
                </div>
              </div>
            </div>
            <div class="stem-footer">
              <el-button type="primary" size="small" @click="copyExtractedStem">
                <el-icon><document-copy /></el-icon>
                复制题干
              </el-button>
              <el-button 
                type="success" 
                size="small" 
                @click="handleSaveToDatabase"
                :loading="saving"
              >
                <el-icon><check /></el-icon>
                保存到题库
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled, DocumentCopy, MagicStick, Scissor, Check } from '@element-plus/icons-vue'
import { uploadAudio, transcribeLocal, saveLocalTranscriptionRecord } from '@/api/transcription'
import { sendChatMessage } from '@/api/ai'
import { saveAiQuestionRecord } from '@/api/aiQuestion'
import { saveQuestionItem } from '@/api/questionItems'
import { useUserStore } from '@/stores/user'
import { useRouter } from 'vue-router'

const uploadRef = ref(null)
const selectedFile = ref(null)
const uploading = ref(false)
const transcribing = ref(false)
const generatingQuestion = ref(false)
const transcriptionResult = ref('')
const generatedQuestion = ref('')
const customRemark = ref('')
const questionType = ref('choice') // 默认选择题
const extractedStem = ref('') // 提取的题干
const extractedOptions = ref([]) // 提取的选项
const durationLoaded = ref(false) // 时长是否已加载
const transcriptionMode = ref('local') // 转写模式：local(本地) 或 cloud(云端)，默认本地
const saving = ref(false) // 保存状态

const userStore = useUserStore()

const router = useRouter()

// 计算属性：判断是否为 WAV 文件
const isWavFile = computed(() => {
  if (!selectedFile.value) return false
  const fileName = selectedFile.value.name.toLowerCase()
  return fileName.endsWith('.wav')
})

// 回到主页
const returndashboard = () => {
  router.push('/')
}

// 处理文件选择
const handleFileChange = (file) => {
  selectedFile.value = file.raw
  durationLoaded.value = false // 重置时长加载状态
  
  // 使用浏览器 API 获取音频时长
  if (file.raw) {
    const audio = new Audio()
    const objectURL = URL.createObjectURL(file.raw)
    
    audio.addEventListener('loadedmetadata', () => {
      const duration = audio.duration * 1000 // 转换为毫秒
      // console.log('音频真实时长:', duration, '毫秒')
      // console.log('音频时长:', formatDuration(duration))
      
      // 将时长信息附加到文件对象上
      file.raw.duration = Math.round(duration)
      durationLoaded.value = true // 标记时长已加载
      // console.log('durationLoaded 已设置为 true')
      
      URL.revokeObjectURL(objectURL)
    })
    
    audio.addEventListener('error', (e) => {
      console.error('音频时长加载失败:', e)
      URL.revokeObjectURL(objectURL)
    })
    
    audio.src = objectURL
  }
}

// 格式化文件大小
const formatFileSize = (bytes) => {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(2) + ' MB'
}

// 格式化音频时长（毫秒转为分：秒格式）
const formatDuration = (ms) => {
  if (!ms) return '0:00'
  const totalSeconds = Math.floor(ms / 1000)
  const minutes = Math.floor(totalSeconds / 60)
  const seconds = totalSeconds % 60
  return `${minutes}:${seconds.toString().padStart(2, '0')}`
}

// 处理上传
const handleUpload = async () => {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择音频文件')
    return
  }
  
  // 等待时长加载完成（最多等待 5 秒）
  if (!durationLoaded.value && transcriptionMode.value === 'cloud') {
    // console.log('等待音频时长加载...')
    let waitCount = 0
    const maxWaitCount = 50 // 5 秒（50 * 100ms）
    while (!durationLoaded.value && waitCount < maxWaitCount) {
      await new Promise(resolve => setTimeout(resolve, 100))
      waitCount++
      if (waitCount % 10 === 0) {
        // console.log(`已等待 ${waitCount * 0.1} 秒...`)
      }
    }
    console.log('时长加载状态:', durationLoaded.value ? ' 已完成' : '超时')
  }
  
  uploading.value = true
  // console.log('开始上传文件:', selectedFile.value.name)
  // console.log('文件大小:', selectedFile.value.size, 'bytes')
  // console.log('转写模式:', transcriptionMode.value)
  
  try {
    let response
    
    // 根据选择的模式调用不同的 API
    if (isWavFile.value && transcriptionMode.value === 'local') {
      // 本地 Vosk 转写
      console.log('使用本地 Vosk 转写...')
      response = await transcribeLocal(selectedFile.value)
    } else {
      // 云端讯飞转写
      console.log('使用云端讯飞转写...')
      const duration = selectedFile.value.duration || null
      if (duration) {
        console.log('音频时长:', duration, '毫秒 (', formatDuration(duration), ')')
      } else {
        console.log('未获取到音频时长，后端将自动计算')
      }
      response = await uploadAudio(selectedFile.value, duration)
    }
    
    // console.log('API 响应:', response)
    // console.log('响应数据:', response.data)
    
    // 检查响应数据的结构
    if (typeof response.data === 'string') {
      console.log('后端返回的是字符串（旧格式）')
      ElMessage.success('转写成功！')
      transcriptionResult.value = response.data
    } else if (response.data && typeof response.data === 'object') {
      console.log('后端返回的是 JSON 对象（新格式）')
      if (response.data.success) {
        console.log('转写成功，结果:', response.data.result)
        if (response.data.duration) {
          console.log('使用的时长:', response.data.duration, 'ms')
        }
        ElMessage.success('转写成功！')
        transcriptionResult.value = response.data.result
        
        // 如果是本地转写，手动保存转写记录
        if (isWavFile.value && transcriptionMode.value === 'local') {
          console.log('检测到本地转写模式，开始保存记录...')
          await saveLocalTranscriptionRecordManually()
        }
      } else {
        console.error('后端返回失败:', response.data.message)
        ElMessage.error(response.data.message || '转写失败')
      }
    } else {
      console.error('未知的响应格式:', response.data)
      ElMessage.error('转写失败：未知的响应格式')
    }
  } catch (error) {
    console.error('上传异常:', error)
    console.error('错误详情:', error.response?.data)
    ElMessage.error(error.response?.data?.message || '上传失败，请检查网络连接')
  } finally {
    uploading.value = false
  }
}

// 重置
const handleReset = () => {
  if (uploadRef.value) {
    uploadRef.value.clearFiles()
  }
  selectedFile.value = null
  transcriptionResult.value = ''
}

// 复制结果
const handleCopyResult = () => {
  navigator.clipboard.writeText(transcriptionResult.value)
  ElMessage.success('复制成功')
}

// 手动保存本地转写记录
const saveLocalTranscriptionRecordManually = async () => {
  try {
    // console.log('开始保存本地转写记录...')
    
    const response = await saveLocalTranscriptionRecord({
      transcriptionText: transcriptionResult.value,
      fileName: selectedFile.value?.name || 'upload.wav',
      fileSize: selectedFile.value?.size || 0
    })
    
    if (response.data.success) {
      console.log('本地转写记录保存成功')
    } else {
      console.error('保存转写记录失败:', response.data.message)
    }
  } catch (error) {
    console.error('保存转写记录异常:', error)
    // 不显示错误提示，避免影响用户体验
  }
}

// AI 智能出题
const handleGenerateQuestion = async () => {
  if (!transcriptionResult.value) {
    ElMessage.warning('请先获取转写结果')
    return
  }
  
  generatingQuestion.value = true
  generatedQuestion.value = ''
  
  try {
    // 根据选择的题型构建提示词
    const questionTypeText = questionType.value === 'choice' ? '选择题' : '简答题';
    let prompt = `请根据以下内容，生成 1 道${questionTypeText}（包括题干、选项（如果是选择题）和参考答案）。\n\n内容：${transcriptionResult.value}`
    
    // 如果有自定义备注，添加到提示词中
    if (customRemark.value && customRemark.value.trim()) {
      prompt += `\n\n【自定义要求】${customRemark.value.trim()}`
      console.log('使用自定义提示词:', customRemark.value)
    } else {
      console.log('使用默认提示词')
    }

    prompt +=`生成的格式严格按照如下所示：第一行简单简单描述一下出题的大纲，第二行是题目的题干，如果是选择题，第三行就是 4 个选项，每个选项之间用空格隔开,单个选项里面不允许出现空格，并且每个选项之前加上 A/B/C/D 这样的前缀，前缀与对应的选项之间不要有空格，用一个逗号来隔开，比如:A,12 B,21 C,31 D,41，严格按照这个格式来出题;如果是简答题，这一行空着。第四行使这个题目的答案以及解析。此外，输出的题目里面不允许有任何的比如加粗加黑的内容，和其他要求以外的内容。`
    
    console.log('调用 AI 出题 API...')
    console.log('题目类型:', questionType.value)
    const response = await sendChatMessage(prompt)
    
    if (response.data.success) {
      generatedQuestion.value = response.data.response
      ElMessage.success('AI 出题成功！')
      
      // 保存到数据库
      await saveToDatabase()
    } else {
      ElMessage.error(response.data.message || 'AI 出题失败')
    }
  } catch (error) {
    console.error('AI 出题失败:', error)
    ElMessage.error(error.response?.data?.message || 'AI 出题失败，请检查网络连接')
  } finally {
    generatingQuestion.value = false
  }
}

// 保存到数据库
const saveToDatabase = async () => {
  try {
    console.log('开始保存 AI 出题记录到数据库...')
    
    const saveData = {
      transcriptionContent: transcriptionResult.value,
      customRemark: customRemark.value ? customRemark.value.trim() : '',
      generatedQuestion: generatedQuestion.value
    }
    
    console.log('保存数据:', saveData)
    
    const response = await saveAiQuestionRecord(saveData)
    
    if (response.data.success) {
      console.log('AI 出题记录保存成功，记录 ID:', response.data.recordId)
    } else {
      console.error('保存失败:', response.data.message)
    }
  } catch (error) {
    console.error('保存 AI 出题记录失败:', error)
    // 不显示错误提示，避免影响用户体验
  }
}

// 复制提取的题干
const copyExtractedStem = () => {
  navigator.clipboard.writeText(extractedStem.value)
  ElMessage.success('题干复制成功')
}

// 复制题目
const handleCopyQuestion = () => {
  navigator.clipboard.writeText(generatedQuestion.value)
  ElMessage.success('复制成功')
}

// 提取题干
const handleExtractStem = () => {
  console.log('=== 开始提取题干 ===')
  console.log('generatedQuestion.value:', generatedQuestion.value)
  
  if (!generatedQuestion.value || !generatedQuestion.value.trim()) {
    console.warn('题目内容为空')
    ElMessage.warning('没有可提取的题目内容')
    return
  }
  
  try {
    const lines = generatedQuestion.value.split('\n').filter(line => line.trim())
    console.log('分割后的行数:', lines.length)
    console.log('所有行内容:', lines)
    
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
    } else if (lines.length === 1) {
      // 只有一行，可能是题干和选项混合
      const onlyLine = lines[0].trim()
      stem = onlyLine
      console.log('唯一一行作为题干:', stem)
    }
    
    // 进一步清理：去除题干开头的选项标记
    if (stem.match(/^[A-D]\.\s/)) {
      stem = stem.replace(/^[A-D]\.\s/, '')
      console.log('清理选项标记后的题干:', stem)
    }
    
    console.log('最终提取的题干:', stem)
    console.log('提取到的选项:', options)
    
    if (stem) {
      extractedStem.value = stem
      extractedOptions.value = options
      console.log('设置 extractedStem.value:', extractedStem.value)
      console.log('设置 extractedOptions:', extractedOptions.value)
      ElMessage.success('题干提取成功！共提取 ' + options.length + ' 个选项')
      
      // 自动滚动到提取结果处
      setTimeout(() => {
        const element = document.querySelector('.extracted-stem-section')
        console.log('找到的元素:', element)
        if (element) {
          element.scrollIntoView({ behavior: 'smooth', block: 'center' })
        }
      }, 100)
    } else {
      console.warn('未能提取到题干')
      ElMessage.warning('未能提取到题干，请手动检查')
    }
  } catch (error) {
    console.error('提取题干失败:', error)
    ElMessage.error('提取失败：' + error.message)
  }
}

// 保存到题库
const handleSaveToDatabase = async () => {
  if (!extractedStem.value) {
    ElMessage.warning('请先提取题干')
    return
  }
  
  saving.value = true
  
  try {
    // 准备数据
    const questionBody = extractedStem.value
    const questionTypeValue = questionType.value === 'choice' ? 0 : 1  // 0=选择题，1=主观题
    
    // 提取选项（如果是选择题）
    let choosingAnswer = null
    if (questionTypeValue === 0 && extractedOptions.value.length > 0) {
      choosingAnswer = extractedOptions.value.join(' | ')  // 用分隔符连接选项
    }
    
    // 从生成的题目中提取答案
    let questionAnswer = '参考答案见原生成内容'
    const lines = generatedQuestion.value.split('\n').map(line => line.trim())
    for (let i = lines.length - 1; i >= 0; i--) {
      const line = lines[i]
      if (line.includes('答案') || line.includes('解析')) {
        questionAnswer = line
        break
      }
    }
    
    // 获取当前登录用户 ID
    const userid = userStore.userId
    console.log('当前用户 ID:', userid)
    
    console.log('保存参数:', {
      questionBody,
      questionType: questionTypeValue,
      questionAnswer,
      choosingAnswer,
      userid
    })
    
    // 调用 API 保存
    const response = await saveQuestionItem(questionBody, questionTypeValue, questionAnswer, choosingAnswer, userid)
    
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
    saving.value = false
  }
}
</script>

<style scoped>
.upload-container {
  padding: 20px;
}

.upload-card {
  max-width: 800px;
  margin: 0 auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: bold;
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

.upload-actions {
  display: flex;
  gap: 10px;
  justify-content: center;
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

.result-section {
  margin-top: 30px;
  padding: 20px;
  background-color: #f5f7fa;
  border-radius: 8px;
}

.result-actions {
  display: flex;
  gap: 10px;
  margin-bottom: 15px;
}

.question-type-selector {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background-color: #f0f9ff;
  border-radius: 6px;
  border: 1px solid #d9ecff;
  margin-bottom: 15px;
}

.selector-label {
  font-weight: bold;
  color: #606266;
  font-size: 14px;
  white-space: nowrap;
}

.result-textarea {
  font-family: 'Courier New', Courier, monospace;
  font-size: 14px;
  line-height: 1.6;
}

.remark-section {
  margin-top: 20px;
  padding: 15px;
  background-color: #fff7e6;
  border-radius: 6px;
  border: 1px solid #ffe4b5;
}

.remark-textarea {
  font-family: inherit;
  font-size: 13px;
  line-height: 1.6;
}

.question-section {
  margin-top: 30px;
  padding-top: 20px;
  border-top: 2px solid #e4e7ed;
}

.question-content {
  margin-top: 15px;
}

.question-textarea {
  font-family: 'Courier New', Courier, monospace;
  font-size: 14px;
  line-height: 1.8;
  background-color: #fff;
}

.question-actions {
  margin-top: 15px;
  text-align: right;
}

.extracted-stem-section {
  margin-top: 25px;
  padding: 20px;
  background: linear-gradient(135deg, #f0fff4 0%, #e6fffa 100%);
  border-radius: 8px;
  border: 2px solid #c6f6d5;
  animation: slideIn 0.3s ease-out;
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

.stem-card {
  background: white;
  border-radius: 6px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.stem-title {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: linear-gradient(135deg, #48bb78 0%, #38a169 100%);
  color: white;
  font-weight: bold;
  font-size: 15px;
}

.stem-body {
  padding: 20px;
}

.stem-text {
  font-size: 16px;
  line-height: 1.8;
  color: #2d3748;
  font-weight: 500;
  margin-bottom: 20px;
  padding-left: 12px;
  border-left: 4px solid #48bb78;
  background-color: #f7fafc;
  padding: 15px;
  border-radius: 4px;
}

.options-list {
  margin-top: 15px;
  display: flex;
  flex-direction: column;
  gap: 10px;
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
  font-weight: bold;
  color: #38a169;
  min-width: 25px;
  font-size: 15px;
}

.option-content {
  flex: 1;
  color: #4a5568;
  line-height: 1.6;
  font-size: 15px;
}

.stem-footer {
  padding: 12px 20px;
  background-color: #f7fafc;
  border-top: 1px solid #e2e8f0;
  text-align: right;
}
</style>
