<template>
  <div class="class-members-container">
    <el-card class="members-card">
      <template #header>
        <div class="card-header">
          <span>班级管理</span>
        </div>
      </template>

      <!-- 搜索区域 -->
      <div class="search-section">
        <h3>搜索班级</h3>
        <el-form :inline="true" class="search-form">
          <el-form-item label="搜索方式">
            <el-select v-model="searchType" placeholder="请选择" style="width: 150px">
              <el-option label="按班级名称" value="name" />
              <el-option label="按班级编号" value="code" />
            </el-select>
          </el-form-item>

          <el-form-item>
            <el-input
              v-model="searchKeyword"
              :placeholder="searchType === 'name' ? '请输入班级名称' : '请输入班级编号'"
              clearable
              style="width: 300px"
              @keyup.enter="handleSearch"
            />
          </el-form-item>

          <el-form-item>
            <el-button type="primary" @click="handleSearch" :loading="searching">
              搜索
            </el-button>
            <el-button @click="handleReset">重置</el-button>
            <el-button type="success" @click="handleLoadMyClasses" :loading="loadingMyClasses">
              我的班级
            </el-button>
            <el-button @click="returntodashboard">返回主页</el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 我的班级列表 -->
      <div v-if="showMyClasses" class="my-classes-section">
        <h3>我的班级（共 {{ myClasses.length }} 个）</h3>
        <el-table :data="myClasses" stripe style="width: 100%">
          <el-table-column prop="classname" label="班级名称" width="150" />
          <el-table-column prop="classcode" label="班级编号" width="120" />
          <el-table-column prop="classnum" label="班级人数" width="100" align="center" />
          <el-table-column label="创建时间" width="180">
            <template #default="{ row }">
              {{ formatDate(row.createdAt) }}
            </template>
          </el-table-column>
          <el-table-column label="我的角色" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="row.userrole === 5 ? 'danger' : 'primary'">
                {{ row.userrole === 5 ? '老师' : '学生' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="加入时间" width="180">
            <template #default="{ row }">
              {{ formatDate(row.joinAt) }}
            </template>
          </el-table-column>
        </el-table>
      </div>


      <!-- 搜索结果 -->
      <div v-if="searchResults.length > 0" class="results-section">
        <h3>搜索结果（共 {{ searchResults.length }} 条）</h3>
        <el-table :data="searchResults" stripe style="width: 100%">
          <el-table-column prop="classname" label="班级名称" width="150" />
          <el-table-column prop="classcode" label="班级编号" width="120" />
          <el-table-column prop="classnum" label="班级人数" width="100" align="center" />
          <el-table-column label="创建时间" width="180">
            <template #default="{ row }">
              {{ formatDate(row.createdAt) }}
            </template>
          </el-table-column>
          <el-table-column prop="description" label="描述" show-overflow-tooltip />
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button
                type="success"
                size="small"
                @click="handleJoinClass(row)"
                :loading="joiningClassId === row.classid"
              >
                加入班级
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 空状态 -->
      <el-empty v-else-if="hasSearched" description="未找到符合条件的班级" />

      <!-- 初始提示 -->
      <div v-else class="placeholder-content">
        <el-icon :size="80" color="#909399"><school /></el-icon>
        <h2>搜索并加入班级</h2>
        <p>请输入班级名称或班级编号进行搜索</p>
        <p style="margin-top: 20px; font-size: 14px; color: #909399;">
          支持模糊匹配，可以快速找到目标班级
        </p>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { School } from '@element-plus/icons-vue'
import { searchClasses, getMyClasses, joinClass } from '@/api/class'
import { useUserStore } from '@/stores/user'
import { useRouter } from 'vue-router'

const userStore = useUserStore()
const router = useRouter()

// 搜索相关
const searchType = ref('name') // 'name' 或 'code'
const searchKeyword = ref('')
const searching = ref(false)
const searchResults = ref([])
const hasSearched = ref(false)

// 我的班级相关
const myClasses = ref([])
const showMyClasses = ref(false)
const loadingMyClasses = ref(false)


// 加入班级相关
const joiningClassId = ref(null)

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

// 搜索班级
const handleSearch = async () => {
  if (!searchKeyword.value.trim()) {
    ElMessage.warning('请输入搜索关键词')
    return
  }

  searching.value = true
  hasSearched.value = true
  searchResults.value = []

  try {
    const response = await searchClasses(searchKeyword.value.trim())

    if (response.data.success) {
      searchResults.value = response.data.data || []
      console.log('搜索结果:', searchResults.value.length, '条')

      if (searchResults.value.length === 0) {
        ElMessage.info('未找到符合条件的班级')
      } else {
        ElMessage.success(`找到 ${searchResults.value.length} 个班级`)
      }
    } else {
      ElMessage.error(response.data.message || '搜索失败')
    }
  } catch (error) {
    console.error('搜索失败:', error)
    ElMessage.error(error.response?.data?.message || '搜索失败，请检查网络连接')
  } finally {
    searching.value = false
  }
}

// 重置搜索
const handleReset = () => {
  searchKeyword.value = ''
  searchResults.value = []
  hasSearched.value = false
}

const returntodashboard = () =>{
  router.push('/')
}

// 加载我的班级列表
const handleLoadMyClasses = async () => {
  if (!userStore.userId) {
    ElMessage.warning('请先登录')
    return
  }

  loadingMyClasses.value = true
  showMyClasses.value = false

  try {
    console.log('获取用户', userStore.userId, '的班级列表')

    // 调用真实后端 API
    const response = await getMyClasses(userStore.userId)

    if (response.data.success) {
      myClasses.value = response.data.data || []
      console.log('我的班级:', myClasses.value)
      console.log('共找到', myClasses.value.length, '个班级')

      showMyClasses.value = true
      ElMessage.success(`加载了 ${myClasses.value.length} 个班级`)
    } else {
      ElMessage.error(response.data.message || '加载失败')
    }
  } catch (error) {
    console.error('加载班级失败:', error)
    ElMessage.error(error.response?.data?.message || '加载失败，请稍后重试')
  } finally {
    loadingMyClasses.value = false
  }
}


// 加入班级
const handleJoinClass = async (classItem) => {
  if (!userStore.userId) {
    ElMessage.warning('请先登录')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定要加入班级“${classItem.classname}”吗？`,
      '确认加入',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'info'
      }
    )

    joiningClassId.value = classItem.classid

    console.log('申请加入班级:', {
      userid: userStore.userId,
      classid: classItem.classid,
      userrole: 1 // 1=学生
    })

    // 调用真实后端 API 加入班级
    const response = await joinClass(userStore.userId, classItem.classid, 1)

    if (response.data.success) {
      ElMessage.success(`成功加入班级：${classItem.classname}`)

      // 从搜索结果中移除该班级
      searchResults.value = searchResults.value.filter(item => item.classid !== classItem.classid)

      // 刷新班级人数
      classItem.classnum = (classItem.classnum || 0) + 1
    } else {
      ElMessage.error(response.data.message || '加入失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('加入班级失败:', error)
      ElMessage.error(error.response?.data?.message || '加入失败，请稍后重试')
    }
  } finally {
    joiningClassId.value = null
  }
}
</script>

<style scoped>
.class-members-container {
  padding: 20px;
  min-height: calc(100vh - 60px);
  background-color: #f0f2f5;
}

.placeholder-card {
  max-width: 600px;
  margin: 0 auto;
  border-radius: 8px;
}

.card-header {
  font-size: 18px;
  font-weight: bold;
  color: #303133;
}

.placeholder-content {
  text-align: center;
  padding: 40px 20px;
}

.placeholder-content h2 {
  margin: 20px 0 10px;
  color: #606266;
}

.placeholder-content p {
  color: #909399;
  line-height: 1.8;
}

.my-classes-section {
  margin-top: 20px;
  padding: 20px;
  background-color: #f0f9ff;
  border-radius: 8px;
  border: 1px solid #d6e9f7;
}

.my-classes-section h3 {
  margin-bottom: 15px;
  color: #606266;
  font-size: 16px;
}

.search-section {
  margin-bottom: 20px;
  padding: 20px;
  background-color: #f5f7fa;
  border-radius: 8px;
}

.search-section h3 {
  margin-bottom: 15px;
  color: #606266;
  font-size: 16px;
}

.search-form {
  display: flex;
  flex-wrap: wrap;
  gap: 15px;
}

.results-section {
  margin-top: 20px;
}

.results-section h3 {
  margin-bottom: 15px;
  color: #606266;
  font-size: 16px;
}

:deep(.el-table) {
  font-size: 14px;
}

:deep(.el-table th) {
  background-color: #f5f7fa;
  color: #606266;
}


</style>
