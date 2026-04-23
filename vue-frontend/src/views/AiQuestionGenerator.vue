<template>
  <div class="question-container">
    <el-card class="question-card">
      <template #header>
        <div class="card-header">
          <span>AI 智能出题</span>
          <el-button @click="handleClear" :disabled="!content.trim()">
            清空内容
          </el-button>
        </div>
      </template>
      
      <div class="form-content">
        <!-- 内容输入框 -->
        <div class="form-item">
          <label class="form-label">题目素材：</label>
          <el-input
            v-model="content"
            type="textarea"
            :rows="8"
            placeholder="请输入用于生成题目的内容，例如：一段文本、一个知识点描述等..."
            :disabled="loading"
          />
        </div>
        
        <!-- 关键信息抽取 -->
        <div v-if="content.trim()" class="form-item">
          <el-card class="analysis-card">
            <template #header>
              <div class="card-header-actions">
                <span>智能分析</span>
                <div class="header-buttons">
                  <el-button 
                    type="success" 
                    size="small"
                    :loading="extractingKeywords"
                    @click="handleExtractKeywords"
                  >
                    {{ extractingKeywords ? '提取中...' : '提取关键词' }}
                  </el-button>
                  <el-button 
                    type="primary" 
                    size="small"
                    :loading="analyzing"
                    @click="handleAnalyzeContent"
                  >
                    {{ analyzing ? '分析中...' : '提取关键点' }}
                  </el-button>
                </div>
              </div>
            </template>
            
            <div v-if="keywordsResult && keywordsResult.length > 0" class="keywords-result">
              <el-divider content-position="left">关键词</el-divider>
              <div class="keywords-tags">
                <el-tag
                  v-for="(item, index) in keywordsResult"
                  :key="index"
                  type="success"
                  effect="plain"
                  size="large"
                  class="keyword-tag"
                >
                  {{ item.keyword }}
                  <span class="keyword-weight">({{ (item.weight * 100).toFixed(0) }}%)</span>
                </el-tag>
              </div>
              <div class="keywords-actions">
                <el-button 
                  type="primary" 
                  size="small" 
                  @click="copyKeywords"
                >
                  <el-icon><document-copy /></el-icon>
                  复制关键词
                </el-button>
                <el-button 
                  type="warning" 
                  size="small" 
                  @click="useKeywordsAsRemark"
                >
                  <el-icon><edit /></el-icon>
                  用作出题要求
                </el-button>
              </div>
            </div>
            
            <div v-if="analysisResult && analysisResult.trim()" class="analysis-result">
              <el-divider content-position="left">关键信息</el-divider>
              <el-input
                v-model="analysisResult"
                type="textarea"
                :rows="6"
                readonly
                class="analysis-textarea"
              />
              <div class="analysis-actions">
                <el-button 
                  type="success" 
                  size="small" 
                  @click="useAnalysisAsContent"
                >
                  <el-icon><document-copy /></el-icon>
                  使用分析结果作为素材
                </el-button>
                <el-button 
                  type="primary" 
                  size="small" 
                  @click="copyAnalysisResult"
                >
                  <el-icon><document-copy /></el-icon>
                  复制分析结果
                </el-button>
              </div>
            </div>
            
            <div v-else-if="content.trim()" class="analysis-tips">
              <el-alert
                title="点击「提取关键点」按钮，AI 将自动分析素材内容，提取关键信息和知识点"
                type="info"
                :closable="false"
                show-icon
              />
            </div>
          </el-card>
        </div>
        
        <!-- 题型选择 -->
        <div class="form-item">
          <label class="form-label">题型选择：</label>
          <el-radio-group v-model="questionType" :disabled="loading">
            <el-radio label="choice">选择题</el-radio>
            <el-radio label="short-answer">简答题</el-radio>
          </el-radio-group>
        </div>
        
        <!-- 备注输入 -->
        <div class="form-item">
          <label class="form-label">出题要求（可选）：</label>
          <el-input
            v-model="remark"
            type="textarea"
            :rows="2"
            placeholder="请输入对题目的特殊要求，例如：难度、题量、考察重点等..."
            :disabled="loading"
          />
        </div>
        
        <!-- 操作按钮 -->
        <div class="button-group">
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            :disabled="!content.trim()"
            @click="handleGenerateQuestion"
          >
            生成题目
          </el-button>
          <el-button
            size="large"
            @click="returndashboard"
          >
            返回 Dashboard
          </el-button>
        </div>
        
        <!-- 生成的题目显示 -->
        <div v-if="generatedQuestion" class="result-section">
          <el-divider content-position="left">
            生成的题目
          </el-divider>
          <div class="result-box">
            <div class="result-type">
              <el-tag :type="questionType === 'choice' ? 'success' : 'warning'">
                {{ questionType === 'choice' ? '选择题' : '简答题' }}
              </el-tag>
            </div>
            <div class="result-content">
              <el-input
                v-model="generatedQuestion"
                type="textarea"
                :rows="8"
                class="editable-question-textarea"
              />
            </div>
            <div v-if="remark" class="result-remark">
              <strong>出题要求：</strong>{{ remark }}
            </div>
            <div class="result-time">
              生成时间：{{ currentTime }}
            </div>
            
            <!-- 提取题干按钮 -->
            <div class="result-actions">
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
                type="success"
                size="small"
                @click="copyGeneratedQuestion"
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
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { DocumentCopy, Scissor, Check, Edit } from '@element-plus/icons-vue'
import { sendChatMessage } from '@/api/ai'
import { saveAiQuestionRecord } from '@/api/aiQuestion'
import { saveQuestionItem } from '@/api/questionItems'
import { extractKeywords } from '@/api/keyword'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

// 表单数据
const content = ref('')
const questionType = ref('choice') // 'choice' 或 'short-answer'
const remark = ref('')

// 生成结果
const generatedQuestion = ref('')
const loading = ref(false)
const currentTime = ref('')
const extractedStem = ref('') // 提取的题干
const extractedOptions = ref([]) // 提取的选项
const saving = ref(false) // 保存状态

// 分析相关
const analyzing = ref(false)
const analysisResult = ref('')

// 关键词提取相关
const extractingKeywords = ref(false)
const keywordsResult = ref([])

// 格式化时间
const formatTime = (date) => {
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

// 返回 Dashboard
const returndashboard = () => {
  router.push('/')
}

// 清空内容
const handleClear = () => {
  content.value = ''
  remark.value = ''
  generatedQuestion.value = ''
  analysisResult.value = ''
  ElMessage.success('已清空')
}

// 分析内容 - 提取关键点
const handleAnalyzeContent = async () => {
  if (!content.value.trim()) {
    ElMessage.warning('请先输入题目素材')
    return
  }
  
  analyzing.value = true
  
  try {
    // console.log('=== 开始调用 AI 分析内容 ===')
    // console.log('素材内容长度:', content.value.length)
    // console.log('素材内容预览:', content.value.substring(0, 50) + '...')
    
    // 构建提示词：对转写文本进行关键信息抽取、知识点划分与语义建模
    const analysisPrompt = `请对以下内容进行深度分析，提取关键信息和知识点：

【分析要求】
1. 提取核心概念和关键术语
2. 识别主要知识点并进行层次划分
3. 总结内容的逻辑结构和重点
4. 标注重要的定义、公式或结论

【内容】
${content.value}

【输出格式】
请用清晰的条列式格式输出分析结果，包括：
- 核心主题
- 关键概念（3-5 个）
- 主要知识点（按重要性排序）
- 逻辑结构总结
- 考察重点建议`
    
    // console.log('提示词长度:', analysisPrompt.length)
    // console.log(analysisPrompt)
    
    console.log('调用 sendChatMessage API...')
    const response = await sendChatMessage(analysisPrompt)
    
    // console.log('AI 分析响应状态:', response.status)
    // console.log('AI 分析响应数据:', response.data)
    
    if (response.data.success) {
      analysisResult.value = response.data.response || ''
      console.log('分析结果:', analysisResult.value.substring(0, 100) + '...')
      ElMessage.success('内容分析完成！')
    } else {
      console.error('API 返回失败:', response.data)
      ElMessage.error(response.data.message || '分析失败')
    }
  } catch (error) {
    console.error('错误状态码:', error.response?.status)
    console.error('错误响应数据:', error.response?.data)
    console.error('错误消息:', error.message)
    console.error('完整错误对象:', error)
    
    let errorMsg = '分析失败'
    if (error.response) {
      // 服务器返回了错误响应
      errorMsg = error.response.data?.message || `服务器错误 (${error.response.status})`
      console.error('服务器返回错误:', errorMsg)
    } else if (error.request) {
      // 请求已发送但没有收到响应
      errorMsg = '网络连接失败，请检查后端服务是否启动（端口 8081）'
      console.error('网络错误，请求已发送但未收到响应')
    } else {
      // 其他错误
      errorMsg = error.message || '生成失败，请检查网络连接'
      console.error('其他错误:', errorMsg)
    }
    
    ElMessage.error(errorMsg)
  } finally {
    analyzing.value = false
  }
}

// 使用分析结果作为素材
const useAnalysisAsContent = () => {
  if (!analysisResult.value) {
    ElMessage.warning('没有分析结果')
    return
  }
  
  content.value = analysisResult.value
  ElMessage.success('已将分析结果设置为题目素材')
  
  // 滚动到顶部
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

// 复制分析结果
const copyAnalysisResult = () => {
  if (!analysisResult.value) {
    ElMessage.warning('没有分析结果')
    return
  }
  
  navigator.clipboard.writeText(analysisResult.value)
  ElMessage.success('复制成功')
}

// 提取关键词
const handleExtractKeywords = async () => {
  if (!content.value.trim()) {
    ElMessage.warning('请先输入题目素材')
    return
  }
  
  extractingKeywords.value = true
  
  try {
    console.log('=== 开始提取关键词 ===')
    console.log('素材内容长度:', content.value.length)
    
    const response = await extractKeywords(content.value, 10)
    
    console.log('关键词提取响应:', response.data)
    
    if (response.data.success) {
      keywordsResult.value = response.data.keywords || []
      console.log('提取到关键词数量:', keywordsResult.value.length)
      ElMessage.success(`成功提取 ${keywordsResult.value.length} 个关键词`)
    } else {
      console.error('API 返回失败:', response.data)
      ElMessage.error(response.data.message || '关键词提取失败')
    }
  } catch (error) {
    console.error('=== 关键词提取出错 ===')
    console.error('错误详情:', error)
    
    let errorMsg = '关键词提取失败'
    if (error.response) {
      errorMsg = error.response.data?.message || `服务器错误 (${error.response.status})`
    } else if (error.request) {
      errorMsg = '网络连接失败，请检查后端服务是否启动'
    } else {
      errorMsg = error.message || '提取失败，请检查网络连接'
    }
    
    ElMessage.error(errorMsg)
  } finally {
    extractingKeywords.value = false
  }
}

// 复制关键词
const copyKeywords = () => {
  if (!keywordsResult.value || keywordsResult.value.length === 0) {
    ElMessage.warning('没有关键词')
    return
  }
  
  const keywordsText = keywordsResult.value.map(item => item.keyword).join('、')
  navigator.clipboard.writeText(keywordsText)
  ElMessage.success('关键词已复制')
}

// 使用关键词作为出题要求
const useKeywordsAsRemark = () => {
  if (!keywordsResult.value || keywordsResult.value.length === 0) {
    ElMessage.warning('没有关键词')
    return
  }
  
  const keywordsText = keywordsResult.value.map(item => item.keyword).join('、')
  remark.value = `请围绕以下关键词出题：${keywordsText}`
  ElMessage.success('已将关键词设置为出题要求')
}

// 生成题目
const handleGenerateQuestion = async () => {
  if (!content.value.trim()) {
    ElMessage.warning('请输入题目素材')
    return
  }
  
  loading.value = true
  
  try {
    console.log('=== 开始调用 AI 生成题目接口 ===')
    console.log('题目类型:', questionType.value)
    
    // 根据选择的题型构建提示词
    const questionTypeText = questionType.value === 'choice' ? '选择题' : '简答题';
    let prompt = `请根据以下内容，生成 1 道${questionTypeText}（包括题干、选项（如果是选择题）和参考答案）。\n\n内容：${content.value}`
    
    // 如果有自定义备注，添加到提示词中
    if (remark.value && remark.value.trim()) {
      prompt += `\n\n【自定义要求】${remark.value.trim()}`
      console.log('使用自定义提示词:', remark.value)
    } else {
      console.log('使用默认提示词')
    }

    prompt += `生成的格式严格按照如下所示：第一行简单简单描述一下出题的大纲，第二行是题目的题干，如果是选择题，第三行就是 4 个选项，每个选项之间用空格隔开,单个选项里面不允许出现空格，并且每个选项之前加上 A/B/C/D 这样的前缀，前缀与对应的选项之间不要有空格，用一个逗号来隔开，比如:A,12 B,21 C,31 D,41，严格按照这个格式来出题;如果是简答题，这一行空着。第四行使这个题目的答案以及解析。此外，输出的题目里面不允许有任何的比如加粗加黑的内容，和其他要求以外的内容。`
    
    console.log('调用 sendChatMessage API...')
    const response = await sendChatMessage(prompt)
    
    console.log('接口响应:', response)
    console.log('响应数据:', response.data)
    
    if (response.data.success) {
      generatedQuestion.value = response.data.response
      currentTime.value = formatTime(new Date())
      
      ElMessage.success('题目生成成功！')
      
      // 保存到数据库
      await saveToDatabase()
    } else {
      ElMessage.error(response.data.message || '生成失败')
    }
  } catch (error) {
    console.error('生成失败:', error)
    console.error('错误状态码:', error.response?.status)
    console.error('错误信息:', error.response?.data)
    console.error('完整错误:', error)
    
    let errorMsg = '生成失败'
    if (error.response) {
      // 服务器返回了错误响应
      errorMsg = error.response.data?.message || `服务器错误 (${error.response.status})`
    } else if (error.request) {
      // 请求已发送但没有收到响应
      errorMsg = '网络连接失败，请检查后端服务是否启动（端口 8081）'
    } else {
      // 其他错误
      errorMsg = error.message || '生成失败，请检查网络连接'
    }
    
    ElMessage.error(errorMsg)
  } finally {
    loading.value = false
  }
}

// 保存到数据库
const saveToDatabase = async () => {
  try {
    console.log('开始保存 AI 出题记录到数据库...')
    
    const saveData = {
      transcriptionContent: content.value,
      customRemark: remark.value ? remark.value.trim() : '',
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

// 复制题目
const copyGeneratedQuestion = () => {
  navigator.clipboard.writeText(generatedQuestion.value)
  ElMessage.success('复制成功')
}

// 提取题干
const handleExtractStem = () => {
  // console.log('generatedQuestion.value:', generatedQuestion.value)
  
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

// 复制提取的题干
const copyExtractedStem = () => {
  navigator.clipboard.writeText(extractedStem.value)
  ElMessage.success('题干复制成功')
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

onMounted(() => {
  // 检查是否有路由参数传递的内容
  const contentParam = route.query.content
  if (contentParam) {
    try {
      // 解码 URL 编码的内容
      const decodedContent = decodeURIComponent(contentParam)
      content.value = decodedContent
      console.log('自动填充转写内容:', decodedContent.substring(0, 50) + '...')
      ElMessage.success('已自动填充转写内容')
    } catch (error) {
      console.error('解码内容失败:', error)
      ElMessage.error('内容加载失败')
    }
  }
})
</script>

<style scoped>
.question-container {
  padding: 20px;
}

.question-card {
  max-width: 900px;
  margin: 0 auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.form-content {
  padding: 10px 0;
}

.form-item {
  margin-bottom: 25px;
}

.card-header-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-buttons {
  display: flex;
  gap: 10px;
}

/* 关键词结果样式 */
.keywords-result {
  margin-bottom: 20px;
  padding: 15px;
  background-color: #f0f9ff;
  border-radius: 8px;
  border: 1px solid #d9ecff;
}

.keywords-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin: 15px 0;
}

.keyword-tag {
  font-size: 14px;
  padding: 8px 12px;
  transition: all 0.3s;
}

.keyword-tag:hover {
  transform: translateY(-2px);
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.3);
}

.keyword-weight {
  font-size: 12px;
  color: #909399;
  margin-left: 5px;
}

.keywords-actions {
  display: flex;
  gap: 10px;
  margin-top: 15px;
}

.analysis-card {
  background: linear-gradient(135deg, #f0f9ff 0%, #e6fffa 100%);
  border: 2px solid #b3e5fc;
  box-shadow: 0 4px 12px rgba(0, 162, 255, 0.15);
}

.analysis-result {
  margin-top: 15px;
}

.analysis-textarea {
  font-family: 'Courier New', Courier, monospace;
  font-size: 14px;
  line-height: 1.8;
  background-color: #fff;
}

.analysis-actions {
  display: flex;
  gap: 10px;
  margin-top: 15px;
  justify-content: flex-end;
}

.analysis-tips {
  padding: 10px 0;
}

.form-label {
  display: block;
  font-weight: 600;
  margin-bottom: 10px;
  color: #303133;
  font-size: 14px;
}

.button-group {
  display: flex;
  gap: 15px;
  margin: 25px 0;
}

.result-section {
  margin-top: 30px;
}

.result-box {
  background-color: #f5f7fa;
  padding: 20px;
  border-radius: 8px;
  border: 1px solid #e4e7ed;
  margin-top: 15px;
}

.result-type {
  margin-bottom: 15px;
}

.result-content {
  font-size: 15px;
  line-height: 1.8;
  color: #303133;
  white-space: pre-wrap;
  background-color: white;
  padding: 15px;
  border-radius: 6px;
  border-left: 4px solid #409EFF;
  margin-bottom: 15px;
}

.result-remark {
  font-size: 13px;
  color: #606266;
  padding: 10px;
  background-color: #fff7e6;
  border-radius: 4px;
  margin-bottom: 10px;
}

.result-time {
  font-size: 12px;
  color: #909399;
  text-align: right;
}

.result-actions {
  display: flex;
  gap: 10px;
  margin-top: 15px;
  justify-content: flex-end;
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

:deep(.el-radio-group) {
  width: 100%;
}

:deep(.el-radio) {
  margin-right: 20px;
}
</style>
