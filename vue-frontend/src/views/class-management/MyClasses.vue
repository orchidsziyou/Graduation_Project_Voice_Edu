<template>
  <div class="my-classes-container">
    <el-card class="my-classes-card">
      <template #header>
        <div class="card-header">
          <span>我的班级</span>
          <div style="display: flex; gap: 10px;">
            <el-tag v-if="userStore.isLoggedIn" type="success">
              👤 {{ userStore.username }}
            </el-tag>
            <el-button type="primary" @click="handleRefresh">
              <el-icon><refresh /></el-icon>
              刷新
            </el-button>
            <el-button type="success" @click="router.push('/class-management/create')">
              <el-icon><plus /></el-icon>
              创建班级
            </el-button>
            <el-button @click="router.push('/')">
              回到主页
            </el-button>
          </div>
        </div>
      </template>

      <!-- 统计信息 -->
      <div class="statistics">
        <el-row :gutter="20">
          <el-col :span="6">
            <el-statistic title="我的班级总数" :value="classes.length" />
          </el-col>
          <el-col :span="6">
            <el-statistic title="我管理的班级" :value="managedClasses.length" />
          </el-col>
          <el-col :span="6">
            <el-statistic title="我是老师的班级" :value="teacherClasses.length" />
          </el-col>
          <el-col :span="6">
            <el-statistic title="我是学生的班级" :value="studentClasses.length" />
          </el-col>
        </el-row>
      </div>

      <!-- 班级列表 -->
      <div v-loading="loading" element-loading-text="正在加载班级列表...">
        <div v-if="classes.length === 0 && !loading" class="empty-state">
          <el-empty description="暂无班级，快去创建或加入班级吧！" />
          <div style="text-align: center; margin-top: 20px;">
            <el-button type="primary" @click="router.push('/class-management/create')">
              创建班级
            </el-button>
            <el-button type="success" @click="router.push('/class-management/members')">
              加入班级
            </el-button>
          </div>
        </div>

        <div v-else class="classes-grid">
          <el-card
            v-for="cls in classes"
            :key="cls.id"
            class="class-card"
            shadow="hover"
            @click="handleViewClass(cls)"
          >
            <div class="class-header">
              <el-tag :type="cls.userrole === 5 ? 'danger' : 'primary'" size="large">
                {{ cls.userrole === 5 ? '老师' : '学生' }}
              </el-tag>
              <el-tag v-if="cls.isCreator" type="warning" size="small">
                创建者
              </el-tag>
            </div>

            <div class="class-info">
              <h3>{{ cls.classname }}</h3>
              <p class="class-code">
                <el-icon><key /></el-icon>
                班级码：{{ cls.classcode }}
              </p>
              <p class="class-num">
                <el-icon><user /></el-icon>
                班级人数：{{ cls.classnum }} 人
              </p>
              <p class="join-time">
                <el-icon><clock /></el-icon>
                加入时间：{{ formatDate(cls.joinAt) }}
              </p>
            </div>

            <div class="class-actions">
              <el-button type="primary" size="small" @click.stop="handleViewClass(cls)">
                查看详情
              </el-button>
              <el-button 
                v-if="cls.userrole === 5" 
                type="success" 
                size="small" 
                @click.stop="handleManageClass(cls)"
              >
                管理班级
              </el-button>
            </div>
          </el-card>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh, Plus, Key, User, Clock } from '@element-plus/icons-vue'
import { getMyClasses } from '@/api/class'
import { useUserStore } from '@/stores/user'
import { useRouter } from 'vue-router'

const userStore = useUserStore()
const router = useRouter()

const loading = ref(false)
const classes = ref([])

// 计算属性：按角色分类
const managedClasses = computed(() => {
  return classes.value.filter(cls => cls.isCreator)
})

const teacherClasses = computed(() => {
  return classes.value.filter(cls => cls.userrole === 5)
})

const studentClasses = computed(() => {
  return classes.value.filter(cls => cls.userrole === 1)
})

// 加载班级列表
const loadClasses = async () => {
  if (!userStore.userId) {
    console.warn('用户未登录')
    return
  }

  loading.value = true

  try {
    console.log('=== 开始加载我的班级 ===')
    console.log('用户 ID:', userStore.userId)

    const response = await getMyClasses(userStore.userId)

    console.log('后端返回的数据:', response.data)

    if (response.data.success) {
      classes.value = response.data.data || []
      console.log('加载成功，班级数:', classes.value.length)
      console.log('班级详情:', classes.value)
    } else {
      console.error('后端返回失败:', response.data.message)
      ElMessage.error(response.data.message || '加载班级失败')
    }
  } catch (error) {
    console.error('加载班级失败:', error)
    ElMessage.error(error.response?.data?.message || '加载失败，请检查网络连接')
  } finally {
    loading.value = false
  }
}

// 刷新
const handleRefresh = () => {
  loadClasses()
}

// 查看班级详情
const handleViewClass = (cls) => {
  console.log('查看班级:', cls)
  router.push(`/class-management/class-detail/${cls.classid}`)
}

// 管理班级（仅老师）
const handleManageClass = (cls) => {
  console.log('管理班级:', cls)
  router.push(`/class-management/class-detail/${cls.classid}`)
}

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

onMounted(() => {
  console.log('=== MyClasses 组件已挂载 ===')
  console.log('当前用户登录状态:', userStore.isLoggedIn)
  console.log('当前用户 ID:', userStore.userId)
  loadClasses()
})
</script>

<style scoped>
.my-classes-container {
  padding: 20px;
}

.my-classes-card {
  max-width: 1200px;
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
  padding: 20px;
  background-color: #f5f7fa;
  border-radius: 8px;
}

.empty-state {
  padding: 40px 0;
  text-align: center;
}

.classes-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
  margin-top: 20px;
}

.class-card {
  cursor: pointer;
  transition: all 0.3s;
}

.class-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.class-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.class-info h3 {
  margin: 0 0 10px 0;
  color: #303133;
  font-size: 18px;
}

.class-info p {
  margin: 8px 0;
  color: #606266;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.class-actions {
  display: flex;
  gap: 10px;
  margin-top: 15px;
  padding-top: 15px;
  border-top: 1px solid #ebeef5;
}
</style>
