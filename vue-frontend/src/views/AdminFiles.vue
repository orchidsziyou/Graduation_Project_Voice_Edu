<template>
  <div class="admin-files-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>管理员界面 - 文件管理</span>
          <el-button type="success" size="small" @click="loadFiles">
            <el-icon><refresh /></el-icon>
            刷新列表
          </el-button>
        </div>
      </template>

      <el-alert
        title="提示"
        type="warning"
        :closable="false"
        show-icon
        style="margin-bottom: 20px"
      >
        此页面用于管理本地上传的音频文件。删除操作不可恢复，请谨慎操作！
      </el-alert>

      <el-table
        v-loading="loading"
        :data="fileList"
        stripe
        border
        style="width: 100%"
      >
        <el-table-column prop="name" label="文件名" min-width="200" />
        <el-table-column prop="sizeFormatted" label="文件大小" width="120" />
        <el-table-column prop="lastModified" label="最后修改时间" width="180" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-popconfirm
              title="确定要删除该文件吗？此操作不可恢复！"
              confirm-button-text="确定删除"
              cancel-button-text="取消"
              confirm-button-type="danger"
              @confirm="handleDelete(row.name)"
            >
              <template #reference>
                <el-button type="danger" size="small" :loading="deleting === row.name">
                  <el-icon><delete /></el-icon>
                  删除
                </el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="fileList.length === 0 && !loading" class="empty-state">
        <el-empty description="暂无上传文件" />
      </div>

      <div style="margin-top: 20px; text-align: right">
        <el-button @click="returndashboard">回到主页</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const router = useRouter()
const loading = ref(false)
const deleting = ref('')
const fileList = ref([])

const api = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' }
})

api.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

const loadFiles = async () => {
  loading.value = true
  try {
    const response = await api.get('/admin/files/list')
    if (response.data.success) {
      fileList.value = response.data.data || []
    } else {
      ElMessage.error(response.data.message || '加载失败')
    }
  } catch (error) {
    console.error('加载文件列表失败:', error)
    ElMessage.error('加载文件列表失败')
  } finally {
    loading.value = false
  }
}

const handleDelete = async (fileName) => {
  deleting.value = fileName
  try {
    const response = await api.delete('/admin/files/delete', { params: { fileName } })
    if (response.data.success) {
      ElMessage.success('删除成功')
      await loadFiles()
    } else {
      ElMessage.error(response.data.message || '删除失败')
    }
  } catch (error) {
    console.error('删除文件失败:', error)
    ElMessage.error('删除文件失败')
  } finally {
    deleting.value = ''
  }
}

const returndashboard = () => router.push('/')

onMounted(() => loadFiles())
</script>

<style scoped>
.admin-files-container { padding: 20px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.empty-state { padding: 40px 0; }
</style>
