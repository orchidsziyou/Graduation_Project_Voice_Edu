<template>
  <div class="create-class-container">
    <el-card class="create-class-card">
      <template #header>
        <div class="card-header">
          <span>🏫 创建新班级</span>
        </div>
      </template>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
        label-position="left"
        style="max-width: 600px; margin: 0 auto;"
      >
        <el-form-item label="班级名称" prop="classname">
          <el-input
            v-model="form.classname"
            placeholder="例如：三年二班、高一 (1) 班"
            clearable
            maxlength="50"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="班级编号" prop="classcode">
          <el-input
            v-model="form.classcode"
            placeholder="例如：202401、CLASS001"
            clearable
            maxlength="20"
            show-word-limit
          />
          <div class="form-tip">用于唯一标识班级的编号</div>
        </el-form-item>

        <el-form-item label="班级人数" prop="classnum">
          <el-input-number
            v-model="form.classnum"
            :min="1"
            :max="100"
            :step="1"
            controls-position="right"
            style="width: 100%;"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSubmit" :loading="submitting" size="large">
            {{ submitting ? '创建中...' : '立即创建' }}
          </el-button>
          <el-button @click="handleReset" size="large">
            重置
          </el-button>
          <el-button @click="returntodashboard" size = "large">
            返回主页
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createClass } from '@/api/class'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref(null)
const submitting = ref(false)

// 表单数据
const form = reactive({
  classname: '',
  classcode: '',
  classnum: 30
})

// 表单验证规则
const rules = {
  classname: [
    { required: true, message: '请输入班级名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  classcode: [
    { required: true, message: '请输入班级编号', trigger: 'blur' },
    { min: 2, max: 20, message: '长度在 2 到 20 个字符', trigger: 'blur' }
  ],
  classnum: [
    { required: true, message: '请输入班级人数', trigger: 'change' }
  ]
}

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true
      
      try {
        console.log('提交表单数据:', form)
        console.log('当前用户 ID:', userStore.userId) // 调试输出
        
        // 调用后端 API 创建班级，传递当前用户 ID
        const response = await createClass(
          form.classname, 
          form.classcode, 
          form.classnum,
          userStore.userId  // 传递当前登录用户的 ID
        )
        
        if (response.data.success) {
          ElMessage.success(response.data.message || '班级创建成功！')
          console.log('创建的班级 ID:', response.data.classid)
          
          // 重置表单
          handleReset()
          
          // 可以选择跳转到其他页面
          // router.push('/class-management/list')
        } else {
          ElMessage.error(response.data.message || '创建失败')
        }
        
      } catch (error) {
        console.error('创建班级失败:', error)
        ElMessage.error(error.response?.data?.message || '创建失败，请稍后重试')
      } finally {
        submitting.value = false
      }
    } else {
      ElMessage.warning('请检查表单填写是否正确')
      return false
    }
  })
}

// 重置表单
const handleReset = () => {
  if (!formRef.value) return
  
  formRef.value.resetFields()
  form.classnum = 30
}

const returntodashboard = () =>{
  router.push('/')
}


</script>

<style scoped>
.create-class-container {
  padding: 20px;
  min-height: calc(100vh - 60px);
  background-color: #f0f2f5;
}

.create-class-card {
  max-width: 800px;
  margin: 0 auto;
  border-radius: 8px;
}

.card-header {
  font-size: 18px;
  font-weight: bold;
  color: #303133;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
}

:deep(.el-form-item__label) {
  font-weight: 500;
  color: #606266;
}

:deep(.el-input-number) {
  width: 100%;
}
</style>
